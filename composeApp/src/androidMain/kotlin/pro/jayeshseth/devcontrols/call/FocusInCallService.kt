package pro.jayeshseth.devcontrols.call

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.telecom.Call
import android.telecom.InCallService
import android.util.Log
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import pro.jayeshseth.devcontrols.data.FocusCallsPrefs

/**
 * Minimal InCallService used when the user opts into Arch B (default dialer mode).
 * Never starts a full-screen activity — calls are surfaced only via heads-up
 * notifications managed by [CallNotifications]. The system uses our service in
 * place of the previous dialer's, so no other in-call UI is shown.
 */
class FocusInCallService : InCallService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var currentCall: Call? = null

    private val callCallback = object : Call.Callback() {
        override fun onStateChanged(call: Call, state: Int) {
            Log.d(TAG, "onStateChanged $state")
            updateForState(call, state)
        }
    }

    override fun onCallAdded(call: Call) {
        super.onCallAdded(call)
        Log.d(TAG, "onCallAdded ${call.details}")
        currentCall = call
        call.registerCallback(callCallback)
        updateForState(call, call.details.state)
    }

    override fun onCallRemoved(call: Call) {
        super.onCallRemoved(call)
        Log.d(TAG, "onCallRemoved")
        call.unregisterCallback(callCallback)
        if (currentCall === call) currentCall = null
        cancelAllNotifications()
        scope.launch {
            FocusCallsPrefs.get(applicationContext).setPendingReturnPackage(null)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            CallNotifications.ACTION_ANSWER -> currentCall?.answer(0)
            CallNotifications.ACTION_DECLINE -> currentCall?.reject(false, null)
            CallNotifications.ACTION_HANGUP -> currentCall?.disconnect()
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        scope.cancel()
        cancelAllNotifications()
        super.onDestroy()
    }

    private fun updateForState(call: Call, state: Int) {
        val nm = ContextCompat.getSystemService(this, NotificationManager::class.java) ?: return
        val handle = call.details.handle?.schemeSpecificPart ?: "Unknown"
        when (state) {
            Call.STATE_RINGING -> {
                nm.notify(
                    CallNotifications.NOTIF_ID_RINGING,
                    CallNotifications.ringingNotification(this, handle)
                )
                scope.launch { maybeAutoAnswer(call) }
            }
            Call.STATE_ACTIVE, Call.STATE_DIALING -> {
                nm.cancel(CallNotifications.NOTIF_ID_RINGING)
                nm.notify(
                    CallNotifications.NOTIF_ID_ACTIVE,
                    CallNotifications.activeCallNotification(this, handle)
                )
            }
            Call.STATE_DISCONNECTED, Call.STATE_DISCONNECTING -> {
                cancelAllNotifications()
            }
        }
    }

    private suspend fun maybeAutoAnswer(call: Call) {
        val prefs = FocusCallsPrefs.get(applicationContext).readSnapshot()
        if (!prefs.enabled || !prefs.autoAnswer) return
        val foreground = pro.jayeshseth.devcontrols.util.ForegroundAppDetector
            .currentForegroundPackage(applicationContext)
        if (foreground != null && foreground in prefs.allowedPackages) {
            call.answer(0)
        }
    }

    private fun cancelAllNotifications() {
        val nm = ContextCompat.getSystemService(this, NotificationManager::class.java) ?: return
        nm.cancel(CallNotifications.NOTIF_ID_RINGING)
        nm.cancel(CallNotifications.NOTIF_ID_ACTIVE)
    }

    companion object {
        private const val TAG = "FocusInCallService"
    }
}
