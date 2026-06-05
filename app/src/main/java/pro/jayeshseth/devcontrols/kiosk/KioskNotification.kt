package pro.jayeshseth.devcontrols.kiosk

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat

object KioskNotification {

    const val CHANNEL_ID = "kiosk_mode"
    const val NOTIF_ID = 4244
    const val ACTION_STOP = "pro.jayeshseth.devcontrols.kiosk.ACTION_STOP"

    fun ensureChannel(context: Context) {
        val nm = ContextCompat.getSystemService(context, NotificationManager::class.java) ?: return
        if (nm.getNotificationChannel(CHANNEL_ID) == null) {
            nm.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    "Kiosk mode",
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = "Persistent indicator while Kiosk Mode is active"
                    setShowBadge(false)
                }
            )
        }
    }

    fun show(context: Context, targetLabel: String) {
        ensureChannel(context)
        val stopIntent = Intent(context, KioskControlReceiver::class.java).setAction(ACTION_STOP)
        val stopPi = PendingIntent.getBroadcast(
            context, 0, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val n = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setContentTitle("Kiosk Mode active")
            .setContentText("Locked to $targetLabel — tap Stop to exit")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setAutoCancel(false)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop kiosk", stopPi)
            .build()

        val nm = ContextCompat.getSystemService(context, NotificationManager::class.java) ?: return
        nm.notify(NOTIF_ID, n)
    }

    fun cancel(context: Context) {
        val nm = ContextCompat.getSystemService(context, NotificationManager::class.java) ?: return
        nm.cancel(NOTIF_ID)
    }
}
