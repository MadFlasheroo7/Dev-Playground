package pro.jayeshseth.devcontrols.ui.devtools

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.clickable
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import java.util.UUID

private const val LOREM = "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris."

/** Generators — UUID, timestamps, and lorem ipsum; tap a value to copy. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GeneratorsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var uuid by remember { mutableStateOf(UUID.randomUUID().toString()) }
    val now = remember { System.currentTimeMillis() }
    fun copy(s: String) { (context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(ClipData.newPlainText("x", s)); Toast.makeText(context, "Copied", Toast.LENGTH_SHORT).show() }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Generators") }, navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }) },
        modifier = Modifier.fillMaxSize()
    ) { inner ->
        Column(Modifier.padding(inner).fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState())) {
            Text("Tap any value to copy.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(12.dp))
            Field("UUID v4", uuid) { copy(uuid) }
            Button(onClick = { uuid = UUID.randomUUID().toString() }) { Text("New UUID") }
            Spacer(Modifier.height(12.dp))
            Field("Unix (ms)", "$now") { copy("$now") }
            Field("Unix (s)", "${now / 1000}") { copy("${now / 1000}") }
            Field("ISO-8601", java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", java.util.Locale.US).format(java.util.Date(now))) { }
            Spacer(Modifier.height(12.dp))
            Field("Lorem ipsum", LOREM) { copy(LOREM) }
        }
    }
}

@Composable
private fun Field(label: String, value: String, onClick: () -> Unit) {
    Column(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 8.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
        Text(value, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodyMedium)
    }
}
