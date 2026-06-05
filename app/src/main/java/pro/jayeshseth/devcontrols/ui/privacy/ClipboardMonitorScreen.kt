package pro.jayeshseth.devcontrols.ui.privacy

import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily

/** Clipboard monitor — logs clipboard changes while focused; flags secret-looking content. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClipboardMonitorScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val cm = remember { context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager }
    val log = remember { mutableStateListOf<Pair<String, Boolean>>() }

    fun read() {
        val text = cm.primaryClip?.getItemAt(0)?.coerceToText(context)?.toString() ?: return
        if (log.firstOrNull()?.first == text) return
        val sensitive = Regex("\\d{13,19}").containsMatchIn(text) || text.contains("password", true) || Regex("[A-Za-z0-9+/]{20,}={0,2}").matches(text.trim())
        log.add(0, text to sensitive)
    }

    DisposableEffect(Unit) {
        val l = ClipboardManager.OnPrimaryClipChangedListener { read() }
        cm.addPrimaryClipChangedListener(l)
        read()
        onDispose { cm.removePrimaryClipChangedListener(l) }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Clipboard Monitor") }, navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }) },
        modifier = Modifier.fillMaxSize()
    ) { inner ->
        Column(Modifier.padding(inner).fillMaxSize().padding(12.dp)) {
            Text("Copy text in other apps and return here. Android 12+ only lets us read while focused (and shows a copy toast).",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(8.dp))
            Button(onClick = { read() }) { Text("Read clipboard now") }
            Spacer(Modifier.height(8.dp))
            LazyColumn(Modifier.fillMaxSize()) {
                items(log) { (text, sensitive) ->
                    Column(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                        if (sensitive) Text("⚠ looks sensitive", color = Color(0xFFE53935), style = MaterialTheme.typography.labelSmall)
                        Text(text.take(200), fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodyMedium)
                    }
                    HorizontalDivider()
                }
            }
        }
    }
}
