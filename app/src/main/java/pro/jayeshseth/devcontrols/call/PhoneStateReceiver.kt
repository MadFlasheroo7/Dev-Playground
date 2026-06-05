package pro.jayeshseth.devcontrols.call

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telecom.TelecomManager
import android.telephony.TelephonyManager
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import pro.jayeshseth.devcontrols.data.FocusCallsPrefs
import pro.jayeshseth.devcontrols.util.ForegroundAppDetector

class PhoneStateReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != TelephonyManager.ACTION_PHONE_STATE_CHANGED) return
        val state = intent.getStringExtra(TelephonyManager.EXTRA_STATE) ?: return

        // Run async so we don't block the main thread; receiver lifecycle goAsync isn't
        // needed since we don't read from the BroadcastReceiver after returning.
        scope.launch {
            when (state) {
                TelephonyManager.EXTRA_STATE_RINGING -> handleRinging(context)
                TelephonyManager.EXTRA_STATE_OFFHOOK -> handleOffhook(context)
                TelephonyManager.EXTRA_STATE_IDLE -> handleIdle(context)
            }
        }
    }

    private suspend fun handleRinging(context: Context) {
        val prefs = FocusCallsPrefs.get(context).readSnapshot()
        if (!prefs.enabled) return

        val foreground = ForegroundAppDetector.currentForegroundPackage(context)
        Log.d(TAG, "ringing — foreground=$foreground allowed=${prefs.allowedPackages}")
        if (foreground == null || foreground !in prefs.allowedPackages) return

        FocusCallsPrefs.get(context).setPendingReturnPackage(foreground)

        if (prefs.autoAnswer) {
            answerCall(context)
        }
    }

    private suspend fun handleOffhook(context: Context) {
        val prefs = FocusCallsPrefs.get(context).readSnapshot()
        if (!prefs.enabled || !prefs.useRelaunch) return
        val pkg = prefs.pendingReturnPackage ?: return
        relaunch(context, pkg)
        FocusCallsPrefs.get(context).setPendingReturnPackage(null)
    }

    private suspend fun handleIdle(context: Context) {
        FocusCallsPrefs.get(context).setPendingReturnPackage(null)
    }

    private fun answerCall(context: Context) {
        try {
            val tm = context.getSystemService(Context.TELECOM_SERVICE) as TelecomManager
            tm.acceptRingingCall()
            Log.d(TAG, "acceptRingingCall invoked")
        } catch (se: SecurityException) {
            Log.w(TAG, "ANSWER_PHONE_CALLS not granted", se)
        } catch (t: Throwable) {
            Log.w(TAG, "acceptRingingCall failed", t)
        }
    }

    private fun relaunch(context: Context, pkg: String) {
        try {
            val launch = context.packageManager.getLaunchIntentForPackage(pkg) ?: run {
                Log.w(TAG, "no launch intent for $pkg")
                return
            }
            launch.addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or
                    Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
            )
            context.startActivity(launch)
            Log.d(TAG, "relaunched $pkg")
        } catch (t: Throwable) {
            Log.w(TAG, "relaunch failed", t)
        }
    }

    companion object {
        private const val TAG = "PhoneStateReceiver"
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    }
}
