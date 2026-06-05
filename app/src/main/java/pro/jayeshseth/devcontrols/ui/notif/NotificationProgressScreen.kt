package pro.jayeshseth.devcontrols.ui.notif

import android.app.NotificationManager
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Notification progress — determinate download, indeterminate, and an ongoing notification. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationProgressScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val ch = remember { ensureChannel(context, "progress", "Progress", NotificationManager.IMPORTANCE_LOW) }

    fun base() = NotificationCompat.Builder(context, ch).setSmallIcon(SMALL_ICON).setOnlyAlertOnce(true)

    Scaffold(
        topBar = { TopAppBar(title = { Text("Notification Progress") }, navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }) },
        modifier = Modifier.fillMaxSize()
    ) { inner ->
        Column(Modifier.padding(inner).fillMaxSize().padding(16.dp)) {
            Button(onClick = {
                scope.launch {
                    for (p in 0..100 step 5) {
                        post(context, 50, base().setContentTitle("Downloading…").setContentText("$p%").setProgress(100, p, false).setOngoing(true))
                        delay(250)
                    }
                    post(context, 50, base().setContentTitle("Complete").setContentText("Done").setProgress(0, 0, false).setOngoing(false).setAutoCancel(true))
                }
            }, modifier = Modifier.fillMaxWidth()) { Text("Determinate download") }
            Spacer(Modifier.height(8.dp))
            Button(onClick = {
                post(context, 51, base().setContentTitle("Working…").setContentText("Please wait").setProgress(0, 0, true).setOngoing(true))
            }, modifier = Modifier.fillMaxWidth()) { Text("Indeterminate (spinner)") }
            Spacer(Modifier.height(8.dp))
            Button(onClick = {
                post(context, 52, base().setContentTitle("Ongoing").setContentText("Can't be swiped away").setOngoing(true))
            }, modifier = Modifier.fillMaxWidth()) { Text("Ongoing (sticky)") }
            Spacer(Modifier.height(16.dp))
            OutlinedButton(onClick = { NotificationManagerCompat.from(context).cancelAll() }, modifier = Modifier.fillMaxWidth()) { Text("Clear all") }
            Spacer(Modifier.height(8.dp))
            Text("Importance LOW = silent in the shade. The download animates 0→100%.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
