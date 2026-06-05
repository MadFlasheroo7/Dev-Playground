package pro.jayeshseth.devcontrols.ui.notif

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.RemoteInput

private const val ACTION = "pro.jayeshseth.devcontrols.NOTIF_ACTION"
private const val KEY_REPLY = "reply"

/** Notification actions — buttons + inline reply (RemoteInput) handled by a runtime receiver. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationActionsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var lastEvent by remember { mutableStateOf("—") }
    val ch = remember { ensureChannel(context, "actions", "Actions", NotificationManager.IMPORTANCE_HIGH) }

    DisposableEffect(Unit) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context?, intent: Intent?) {
                val reply = intent?.let { RemoteInput.getResultsFromIntent(it) }?.getCharSequence(KEY_REPLY)
                lastEvent = if (reply != null) "Reply: \"$reply\"" else "Action: ${intent?.getStringExtra("which")}"
                NotificationManagerCompat.from(context).cancel(40)
            }
        }
        ContextCompatRegister(context, receiver)
        onDispose { runCatching { context.unregisterReceiver(receiver) } }
    }

    fun pi(which: String, mutable: Boolean): PendingIntent {
        val flags = (if (mutable) PendingIntent.FLAG_MUTABLE else PendingIntent.FLAG_IMMUTABLE) or PendingIntent.FLAG_UPDATE_CURRENT
        return PendingIntent.getBroadcast(context, which.hashCode(), Intent(ACTION).setPackage(context.packageName).putExtra("which", which), flags)
    }

    fun postIt() {
        val remoteInput = RemoteInput.Builder(KEY_REPLY).setLabel("Reply…").build()
        val replyAction = NotificationCompat.Action.Builder(SMALL_ICON, "Reply", pi("reply", true)).addRemoteInput(remoteInput).build()
        post(context, 40, NotificationCompat.Builder(context, ch).setSmallIcon(SMALL_ICON)
            .setContentTitle("Actionable").setContentText("Try the buttons or inline reply")
            .addAction(SMALL_ICON, "Like", pi("like", false))
            .addAction(SMALL_ICON, "Archive", pi("archive", false))
            .addAction(replyAction))
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Notification Actions") }, navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }) },
        modifier = Modifier.fillMaxSize()
    ) { inner ->
        Column(Modifier.padding(inner).fillMaxSize().padding(16.dp)) {
            Button(onClick = { postIt() }, modifier = Modifier.fillMaxWidth()) { Text("Post actionable notification") }
            Spacer(Modifier.height(16.dp))
            Text("Last event", style = MaterialTheme.typography.labelMedium)
            Text(lastEvent, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(8.dp))
            Text("Tap an action button or type a reply in the shade — it shows up here.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private fun ContextCompatRegister(context: Context, receiver: BroadcastReceiver) {
    if (android.os.Build.VERSION.SDK_INT >= 33) context.registerReceiver(receiver, IntentFilter(ACTION), Context.RECEIVER_NOT_EXPORTED)
    else @Suppress("UnspecifiedRegisterReceiverFlag") context.registerReceiver(receiver, IntentFilter(ACTION))
}
