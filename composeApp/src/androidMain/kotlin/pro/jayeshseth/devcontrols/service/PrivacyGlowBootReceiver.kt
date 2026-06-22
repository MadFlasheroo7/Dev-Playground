package pro.jayeshseth.devcontrols.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import pro.jayeshseth.devcontrols.data.PrivacyGlowPrefs

class PrivacyGlowBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        MainScope().launch {
            val prefs = PrivacyGlowPrefs.get(context)
            if (prefs.hudEnabled.first() && Settings.canDrawOverlays(context)) {
                val serviceIntent = Intent(context, PrivacyGlowService::class.java)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(serviceIntent)
                } else {
                    context.startService(serviceIntent)
                }
            }
        }
    }
}
