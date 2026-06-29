package pro.jayeshseth.devcontrols.service

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class ActiveNotification(
    val key: String,
    val packageName: String,
    val title: String,
    val text: String,
    val postTime: Long,
    val isClearable: Boolean
)

class CustomNotificationListener : NotificationListenerService() {

    override fun onListenerConnected() {
        super.onListenerConnected()
        instance = this
        updateNotificationList()
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        instance = null
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        updateNotificationList()
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)
        updateNotificationList()
    }

    private fun updateNotificationList() {
        val activeSbns = runCatching { activeNotifications }.getOrNull() ?: return
        val mappedList = activeSbns.mapNotNull { sbn ->
            val extras = sbn.notification.extras
            val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: ""
            val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""
            
            // Filter out empty or system-ongoing notifications that aren't useful
            if (title.isEmpty() && text.isEmpty()) return@mapNotNull null

            ActiveNotification(
                key = sbn.key,
                packageName = sbn.packageName,
                title = title,
                text = text,
                postTime = sbn.postTime,
                isClearable = sbn.isClearable
            )
        }
        _notifications.value = mappedList
    }

    companion object {
        private var instance: CustomNotificationListener? = null

        private val _notifications = MutableStateFlow<List<ActiveNotification>>(emptyList())
        val notifications: StateFlow<List<ActiveNotification>> = _notifications

        fun dismissNotification(key: String) {
            instance?.cancelNotification(key)
        }

        fun dismissAllNotifications() {
            instance?.cancelAllNotifications()
        }
    }
}
