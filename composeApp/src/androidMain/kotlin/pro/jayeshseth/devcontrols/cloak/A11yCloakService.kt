package pro.jayeshseth.devcontrols.cloak

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import pro.jayeshseth.devcontrols.MainActivity
import pro.jayeshseth.devcontrols.data.A11yCloakPrefs
import pro.jayeshseth.devcontrols.data.DevSettingsRepository
import pro.jayeshseth.devcontrols.util.ForegroundAppDetector

/**
 * The Accessibility Cloak's independent "eyes".
 *
 * Hiding our accessibility service from a detector means removing it from
 * [enabled_accessibility_services], which *unbinds* it — so the service itself
 * can't be the thing that decides when to rebind. This foreground service is
 * that decider: it polls [UsageStatsManager] (no accessibility needed) for the
 * current foreground app, and uses WRITE_SECURE_SETTINGS — the "hands" — to
 * cloak/uncloak around the target window.
 *
 * While cloaked:
 *  - our component is stripped from `enabled_accessibility_services` (the service
 *    unbinds; Kiosk / Hide Dev / Force Dark pause for the duration)
 *  - if `alsoHideDev`, developer-options + USB debugging are zeroed too, since the
 *    a11y-based Hide Dev can't run while we're unbound.
 * On leaving the target every overwritten value is restored byte-for-byte.
 */
class A11yCloakService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var pollJob: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        ensureChannel(this)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(NOTIF_ID, buildNotification(), ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(NOTIF_ID, buildNotification())
        }
        if (pollJob == null) {
            scope.launch { A11yCloakPrefs.get(applicationContext).setServiceRunning(true) }
            pollJob = scope.launch { pollLoop() }
        }
        return START_STICKY
    }

    private suspend fun pollLoop() {
        val prefs = A11yCloakPrefs.get(applicationContext)
        val repo = DevSettingsRepository.get(applicationContext)
        val resolver = applicationContext.contentResolver
        val ownPrefix = "$packageName/"

        var lastForeground: String? = null
        while (scope.isActive) {
            val fg = ForegroundAppDetector.currentForegroundPackage(applicationContext)
            // Skip our own UI and unknown readings — don't uncloak just because the
            // poller momentarily can't see the target.
            if (fg != null && fg != packageName && fg != lastForeground) {
                lastForeground = fg
                val targets = prefs.targetsOnce()
                val isTarget = fg in targets
                val esc = prefs.readEscrow()
                if (isTarget && !esc.active) {
                    cloak(prefs, repo, resolver, ownPrefix)
                } else if (!isTarget && esc.active) {
                    uncloak(prefs, repo, resolver, esc)
                }
            }
            delay(POLL_MS)
        }
    }

    private suspend fun cloak(
        prefs: A11yCloakPrefs,
        repo: DevSettingsRepository,
        resolver: android.content.ContentResolver,
        ownPrefix: String,
    ) {
        val current = Settings.Secure.getString(resolver, ENABLED_SERVICES) ?: ""
        val accessEnabled = Settings.Secure.getInt(resolver, ACCESSIBILITY_ENABLED, 0)
        val also = prefs.alsoHideDevOnce()
        val dev = repo.currentDeveloperOptions()
        val adb = repo.currentUsbDebugging()

        prefs.snapshotEscrow(current, accessEnabled, dev, adb)

        val trimmed = current.split(':')
            .filter { it.isNotBlank() && !it.startsWith(ownPrefix) }
            .joinToString(":")
        Log.d(TAG, "cloak: stripping own service. '$current' -> '$trimmed'")
        Settings.Secure.putString(resolver, ENABLED_SERVICES, trimmed)
        if (trimmed.isEmpty()) {
            // No services left — turn the subsystem off so the master flag reads clean too.
            Settings.Secure.putInt(resolver, ACCESSIBILITY_ENABLED, 0)
        }
        if (also) {
            if (dev) repo.setDeveloperOptions(false)
            if (adb) repo.setUsbDebugging(false)
        }
        updateNotification()
    }

    private suspend fun uncloak(
        prefs: A11yCloakPrefs,
        repo: DevSettingsRepository,
        resolver: android.content.ContentResolver,
        esc: A11yCloakPrefs.Escrow,
    ) {
        Log.d(TAG, "uncloak: restoring services '${esc.savedServices}'")
        Settings.Secure.putString(resolver, ENABLED_SERVICES, esc.savedServices)
        Settings.Secure.putInt(resolver, ACCESSIBILITY_ENABLED, esc.savedAccessibilityEnabled)
        if (prefs.alsoHideDevOnce()) {
            repo.setDeveloperOptions(esc.savedDev)
            repo.setUsbDebugging(esc.savedAdb)
        }
        prefs.clearEscrow()
        updateNotification()
    }

    override fun onDestroy() {
        pollJob?.cancel()
        // Best-effort un-cloak so we never leave the service stripped after a kill.
        val prefs = A11yCloakPrefs.get(applicationContext)
        val repo = DevSettingsRepository.get(applicationContext)
        val resolver = applicationContext.contentResolver
        scope.launch {
            val esc = prefs.readEscrow()
            if (esc.active) uncloak(prefs, repo, resolver, esc)
            prefs.setServiceRunning(false)
        }
        super.onDestroy()
    }

    private fun updateNotification() {
        val nm = ContextCompat.getSystemService(this, NotificationManager::class.java) ?: return
        nm.notify(NOTIF_ID, buildNotification())
    }

    private fun buildNotification(): Notification {
        val tap = android.app.PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            android.app.PendingIntent.FLAG_IMMUTABLE or android.app.PendingIntent.FLAG_UPDATE_CURRENT
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Accessibility Cloak active")
            .setContentText("Watching foreground apps to hide the Dev Controls a11y service.")
            .setSmallIcon(android.R.drawable.ic_lock_idle_lock)
            .setOngoing(true)
            .setContentIntent(tap)
            .build()
    }

    companion object {
        private const val TAG = "A11yCloak"
        private const val NOTIF_ID = 0x0C10
        private const val POLL_MS = 400L
        const val CHANNEL_ID = "a11y_cloak"

        // Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES is @hide; the string is stable.
        const val ENABLED_SERVICES = "enabled_accessibility_services"
        const val ACCESSIBILITY_ENABLED = "accessibility_enabled"

        fun start(context: Context) {
            ensureChannel(context)
            ContextCompat.startForegroundService(context, Intent(context, A11yCloakService::class.java))
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, A11yCloakService::class.java))
        }

        private fun ensureChannel(context: Context) {
            val nm = ContextCompat.getSystemService(context, NotificationManager::class.java) ?: return
            if (nm.getNotificationChannel(CHANNEL_ID) == null) {
                nm.createNotificationChannel(
                    NotificationChannel(
                        CHANNEL_ID, "Accessibility Cloak", NotificationManager.IMPORTANCE_LOW
                    )
                )
            }
        }
    }
}
