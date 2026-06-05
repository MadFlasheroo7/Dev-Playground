package pro.jayeshseth.devcontrols.ui.notif

import android.app.NotificationManager
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationCompat
import androidx.core.app.Person

/** Notification styles — post BigText, Inbox, Messaging, Media-style, and grouped notifications. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationStylesScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val ch = remember { ensureChannel(context, "styles", "Styles", NotificationManager.IMPORTANCE_DEFAULT) }

    fun base() = NotificationCompat.Builder(context, ch).setSmallIcon(SMALL_ICON).setAutoCancel(true)

    Scaffold(
        topBar = { TopAppBar(title = { Text("Notification Styles") }, navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }) },
        modifier = Modifier.fillMaxSize()
    ) { inner ->
        Column(Modifier.padding(inner).fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState())) {
            Text("Each button posts a notification — pull down the shade to see it.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(12.dp))
            Btn("BigText") {
                post(context, 1, base().setContentTitle("Big Text").setContentText("Tap to expand")
                    .setStyle(NotificationCompat.BigTextStyle().bigText("This is a long expanded body. ".repeat(8))))
            }
            Btn("Inbox") {
                post(context, 2, base().setContentTitle("Inbox · 5 new").setContentText("Multiple lines")
                    .setStyle(NotificationCompat.InboxStyle().also { s -> (1..5).forEach { s.addLine("Line item $it") } }))
            }
            Btn("Messaging") {
                val me = Person.Builder().setName("You").build()
                val them = Person.Builder().setName("Ada").build()
                post(context, 3, base().setStyle(
                    NotificationCompat.MessagingStyle(me)
                        .addMessage("Hey, you up?", System.currentTimeMillis() - 60000, them)
                        .addMessage("Building Dev Controls 😄", System.currentTimeMillis(), me)
                ))
            }
            Btn("Big Picture") {
                val bmp = android.graphics.Bitmap.createBitmap(600, 300, android.graphics.Bitmap.Config.ARGB_8888).also {
                    android.graphics.Canvas(it).drawColor(0xFF3D5AFE.toInt())
                }
                post(context, 4, base().setContentTitle("Big Picture").setContentText("Expanded image")
                    .setStyle(NotificationCompat.BigPictureStyle().bigPicture(bmp)))
            }
            Btn("High-importance (heads-up)") {
                val hch = ensureChannel(context, "headsup", "Heads-up", NotificationManager.IMPORTANCE_HIGH)
                post(context, 5, NotificationCompat.Builder(context, hch).setSmallIcon(SMALL_ICON).setAutoCancel(true)
                    .setPriority(NotificationCompat.PRIORITY_HIGH).setContentTitle("Heads-up!").setContentText("Pops over the top"))
            }
            Btn("Grouped (+ summary)") {
                (10..12).forEach { i ->
                    post(context, i, base().setGroup("grp").setContentTitle("Item $i").setContentText("In a group"))
                }
                post(context, 19, base().setGroup("grp").setGroupSummary(true).setContentTitle("3 items"))
            }
        }
    }
}

@Composable
private fun Btn(label: String, onClick: () -> Unit) {
    Button(onClick = onClick, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) { Text(label) }
}
