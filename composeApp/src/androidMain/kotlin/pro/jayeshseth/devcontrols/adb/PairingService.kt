package pro.jayeshseth.devcontrols.adb

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.RemoteInput
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import pro.jayeshseth.devcontrols.data.AdbConsolePrefs

/**
 * Short-running foreground service that drives the ADB pairing flow via a pinned
 * notification. NSD discovers the pairing port; the user types the 6-digit code
 * directly into the notification via RemoteInput; on submit we run pair() and
 * update the notification accordingly.
 */
class PairingService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val state = MutableStateFlow(PairingState.WaitingForService)
    private var lastEndpoint: PairingEndpoint? = null
    private var lastError: String? = null
    private var discoveryJob: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        ensureChannel(this)
        val startType = if (Build.VERSION.SDK_INT >= 34) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
        } else 0
        if (Build.VERSION.SDK_INT >= 29) {
            startForeground(NOTIF_ID, buildNotification(), startType)
        } else {
            @Suppress("DEPRECATION")
            startForeground(NOTIF_ID, buildNotification())
        }
        startDiscovery()
        observeState()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_SUBMIT_CODE -> handleSubmit(intent)
            ACTION_CANCEL -> { stop() }
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        discoveryJob?.cancel()
        scope.cancel()
        super.onDestroy()
    }

    private fun startDiscovery() {
        val discoverer = AdbPairingDiscoverer(applicationContext)
        discoveryJob = scope.launch {
            discoverer.endpoint.collectLatest { ep ->
                lastEndpoint = ep
                state.value = when {
                    state.value == PairingState.Pairing -> PairingState.Pairing
                    state.value == PairingState.Success -> PairingState.Success
                    state.value == PairingState.Error -> PairingState.Error
                    ep != null -> PairingState.Ready
                    else -> PairingState.WaitingForService
                }
            }
        }
    }

    private fun observeState() {
        scope.launch {
            state.collectLatest { repost() }
        }
    }

    private fun handleSubmit(intent: Intent) {
        val results = RemoteInput.getResultsFromIntent(intent)
        if (results == null) {
            Log.w(TAG, "handleSubmit: no RemoteInput results in intent")
            lastError = "No code submitted."
            state.value = PairingState.Error
            return
        }
        val code = results.getCharSequence(KEY_CODE)?.toString()?.trim().orEmpty()
        Log.d(TAG, "handleSubmit: codeLen=${code.length}")
        if (code.length != 6 || !code.all(Char::isDigit)) {
            lastError = "Code must be 6 digits."
            state.value = PairingState.Error
            return
        }
        val endpoint = lastEndpoint
        if (endpoint == null) {
            lastError = "Pairing dialog on phone is not open."
            state.value = PairingState.Error
            return
        }
        Log.d(TAG, "handleSubmit: pairing to ${endpoint.host}:${endpoint.port}")
        state.value = PairingState.Pairing
        scope.launch {
            val client = AdbClient.get(applicationContext)
            val res = client.pair(endpoint.host, endpoint.port, code)
            Log.d(TAG, "handleSubmit: pair result success=${res.isSuccess} err=${res.exceptionOrNull()?.message}")
            if (res.isSuccess) {
                AdbConsolePrefs.get(applicationContext).markPaired(true)
                val connectRes = client.connect()
                Log.d(TAG, "handleSubmit: connect result success=${connectRes.isSuccess} err=${connectRes.exceptionOrNull()?.message}")
                state.value = PairingState.Success
                kotlinx.coroutines.delay(2_500L)
                stop()
            } else {
                lastError = res.exceptionOrNull()?.message ?: "Pairing failed"
                state.value = PairingState.Error
            }
        }
    }

    private fun stop() {
        if (Build.VERSION.SDK_INT >= 24) stopForeground(STOP_FOREGROUND_REMOVE)
        else @Suppress("DEPRECATION") stopForeground(true)
        stopSelf()
    }

    private fun repost() {
        val nm = ContextCompat.getSystemService(this, NotificationManager::class.java) ?: return
        nm.notify(NOTIF_ID, buildNotification())
    }

    private fun buildNotification(): Notification {
        val state = this.state.value
        val ep = lastEndpoint
        val title = when (state) {
            PairingState.WaitingForService -> "Pair Dev Controls — waiting"
            PairingState.Ready -> "Pair Dev Controls — enter code"
            PairingState.Pairing -> "Pairing…"
            PairingState.Success -> "Paired"
            PairingState.Error -> "Pairing failed"
        }
        val text = when (state) {
            PairingState.WaitingForService ->
                "On phone: Settings → Developer options → Wireless debugging → Pair device with pairing code"
            PairingState.Ready ->
                "Service found at ${ep?.host}:${ep?.port}. Tap 'Enter code' below and type the 6-digit code."
            PairingState.Pairing -> "Connecting to ${ep?.host}:${ep?.port}…"
            PairingState.Success -> "ADB shell is now available in adb_c."
            PairingState.Error -> lastError ?: "Unknown error. Tap 'Enter code' to retry."
        }

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_data_bluetooth)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setOngoing(state != PairingState.Success)
            .setOnlyAlertOnce(true)
            .setSilent(state != PairingState.Error)
            .setPriority(NotificationCompat.PRIORITY_HIGH)

        if (state == PairingState.Ready || state == PairingState.Error) {
            val remoteInput = RemoteInput.Builder(KEY_CODE)
                .setLabel("6-digit code")
                .build()
            val submitIntent = Intent(this, PairingService::class.java)
                .setAction(ACTION_SUBMIT_CODE)
            val submitPi = PendingIntent.getService(
                this, REQ_SUBMIT, submitIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
            )
            val action = NotificationCompat.Action.Builder(
                android.R.drawable.ic_menu_send,
                if (state == PairingState.Error) "Retry — enter code" else "Enter code",
                submitPi
            )
                .addRemoteInput(remoteInput)
                .setAllowGeneratedReplies(false)
                .build()
            builder.addAction(action)
        }

        if (state != PairingState.Success) {
            val cancelIntent = Intent(this, PairingService::class.java)
                .setAction(ACTION_CANCEL)
            val cancelPi = PendingIntent.getService(
                this, REQ_CANCEL, cancelIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            builder.addAction(
                android.R.drawable.ic_menu_close_clear_cancel, "Cancel", cancelPi
            )
        }

        return builder.build()
    }

    enum class PairingState { WaitingForService, Ready, Pairing, Success, Error }

    companion object {
        const val CHANNEL_ID = "adb_pairing"
        const val NOTIF_ID = 4711
        const val KEY_CODE = "key_code"

        const val ACTION_START = "pro.jayeshseth.devcontrols.adb.ACTION_START_PAIRING"
        const val ACTION_SUBMIT_CODE = "pro.jayeshseth.devcontrols.adb.ACTION_SUBMIT_CODE"
        const val ACTION_CANCEL = "pro.jayeshseth.devcontrols.adb.ACTION_CANCEL_PAIRING"

        private const val REQ_SUBMIT = 1001
        private const val REQ_CANCEL = 1002
        private const val TAG = "PairingService"

        fun start(context: Context) {
            val intent = Intent(context, PairingService::class.java).setAction(ACTION_START)
            try {
                ContextCompat.startForegroundService(context, intent)
            } catch (t: Throwable) {
                Log.w(TAG, "startForegroundService failed", t)
            }
        }

        fun ensureChannel(context: Context) {
            val nm = ContextCompat.getSystemService(context, NotificationManager::class.java)
                ?: return
            if (nm.getNotificationChannel(CHANNEL_ID) == null) {
                nm.createNotificationChannel(
                    NotificationChannel(
                        CHANNEL_ID,
                        "ADB pairing",
                        NotificationManager.IMPORTANCE_HIGH
                    ).apply {
                        description = "Pinned notification used to pair the embedded ADB client"
                        setShowBadge(false)
                    }
                )
            }
        }
    }
}
