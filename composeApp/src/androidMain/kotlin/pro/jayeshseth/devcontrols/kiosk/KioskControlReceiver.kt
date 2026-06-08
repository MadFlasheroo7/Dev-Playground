package pro.jayeshseth.devcontrols.kiosk

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.glance.appwidget.updateAll
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import pro.jayeshseth.devcontrols.data.KioskPrefs
import pro.jayeshseth.devcontrols.widget.DevControlsWidget

class KioskControlReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != KioskNotification.ACTION_STOP) return
        scope.launch {
            KioskPrefs.get(context).setKioskActive(false)
            KioskNotification.cancel(context)
            AnimationScales.restore(context)
            runCatching { DevControlsWidget().updateAll(context) }
        }
    }

    companion object {
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    }
}
