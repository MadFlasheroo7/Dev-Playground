package pro.jayeshseth.devcontrols.ui

import android.nfc.NfcAdapter
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/** NFC status — adapter presence/enabled + a note on reader-mode (live tag reads need hardware). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NfcStatusLabScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val adapter = remember { NfcAdapter.getDefaultAdapter(context) }
    val present = adapter != null
    val enabled = adapter?.isEnabled == true

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("NFC Status") },
                navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }
            )
        },
        modifier = Modifier.fillMaxSize()
    ) { inner ->
        Column(Modifier.padding(inner).fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(Modifier.height(32.dp))
            val (color, label) = when {
                !present -> Color(0xFF888888) to "No NFC hardware"
                enabled -> Color(0xFF69F0AE) to "NFC enabled"
                else -> Color(0xFFFFB300) to "NFC present, disabled"
            }
            Box(Modifier.size(120.dp).clip(CircleShape).background(color.copy(alpha = 0.2f)), contentAlignment = Alignment.Center) {
                Box(Modifier.size(70.dp).clip(CircleShape).background(color))
            }
            Spacer(Modifier.height(16.dp))
            Text(label, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(24.dp))
            Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surfaceVariant).padding(16.dp)) {
                Column {
                    Text("adapter present = $present", fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodySmall)
                    Text("adapter enabled = $enabled", fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Live tag reading needs reader-mode (enableReaderMode) wired to the Activity + a physical tag. " +
                            "This screen reports adapter capabilities only.",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
