package pro.jayeshseth.devcontrols.ui.notif

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

internal const val SMALL_ICON = android.R.drawable.ic_dialog_info

internal fun ensureChannel(context: Context, id: String, name: String, importance: Int): String {
    val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    if (nm.getNotificationChannel(id) == null) nm.createNotificationChannel(NotificationChannel(id, name, importance))
    return id
}

internal fun post(context: Context, id: Int, builder: NotificationCompat.Builder) {
    runCatching { NotificationManagerCompat.from(context).notify(id, builder.build()) }
}
