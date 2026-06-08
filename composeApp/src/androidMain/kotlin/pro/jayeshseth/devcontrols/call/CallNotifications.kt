package pro.jayeshseth.devcontrols.call

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat

object CallNotifications {

    const val CHANNEL_ID = "focus_calls"
    const val NOTIF_ID_ACTIVE = 4242
    const val NOTIF_ID_RINGING = 4243

    const val ACTION_ANSWER = "pro.jayeshseth.devcontrols.call.ACTION_ANSWER"
    const val ACTION_DECLINE = "pro.jayeshseth.devcontrols.call.ACTION_DECLINE"
    const val ACTION_HANGUP = "pro.jayeshseth.devcontrols.call.ACTION_HANGUP"

    fun ensureChannel(context: Context) {
        val nm = ContextCompat.getSystemService(context, NotificationManager::class.java) ?: return
        if (nm.getNotificationChannel(CHANNEL_ID) == null) {
            nm.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    "Focus calls",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Heads-up notifications for incoming/active calls when Focus Calls is on"
                    setShowBadge(false)
                }
            )
        }
    }

    fun ringingNotification(context: Context, callerLabel: String): android.app.Notification {
        ensureChannel(context)
        val answer = pi(context, ACTION_ANSWER, NOTIF_ID_RINGING)
        val decline = pi(context, ACTION_DECLINE, NOTIF_ID_RINGING)
        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.sym_call_incoming)
            .setContentTitle("Incoming call")
            .setContentText(callerLabel)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .addAction(android.R.drawable.sym_action_call, "Answer", answer)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Decline", decline)
            .setOngoing(true)
            .setAutoCancel(false)
            .build()
    }

    fun activeCallNotification(context: Context, callerLabel: String): android.app.Notification {
        ensureChannel(context)
        val hangup = pi(context, ACTION_HANGUP, NOTIF_ID_ACTIVE)
        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.sym_call_outgoing)
            .setContentTitle("In call")
            .setContentText(callerLabel)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Hang up", hangup)
            .setOngoing(true)
            .setAutoCancel(false)
            .build()
    }

    private fun pi(context: Context, action: String, requestCode: Int): PendingIntent {
        val intent = Intent(context, FocusInCallService::class.java).setAction(action)
        return PendingIntent.getService(
            context, requestCode, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
