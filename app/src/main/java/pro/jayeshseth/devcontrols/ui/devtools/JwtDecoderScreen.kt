package pro.jayeshseth.devcontrols.ui.devtools

import android.util.Base64
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import org.json.JSONObject

/** JWT decoder — splits a token and base64url-decodes the header + payload. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JwtDecoderScreen(onBack: () -> Unit) {
    var token by remember { mutableStateOf("eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjMiLCJuYW1lIjoiQWRhIiwiaWF0IjoxNTE2MjM5MDIyfQ.sig") }

    fun decode(part: String) = runCatching { JSONObject(String(Base64.decode(part, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP))).toString(2) }.getOrDefault("(undecodable)")
    val parts = token.trim().split(".")
    val header = if (parts.size >= 1 && parts[0].isNotEmpty()) decode(parts[0]) else ""
    val payload = if (parts.size >= 2) decode(parts[1]) else ""

    Scaffold(
        topBar = { TopAppBar(title = { Text("JWT Decoder") }, navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }) },
        modifier = Modifier.fillMaxSize()
    ) { inner ->
        Column(Modifier.padding(inner).fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState())) {
            OutlinedTextField(token, { token = it }, label = { Text("JWT") }, modifier = Modifier.fillMaxWidth().height(140.dp))
            Spacer(Modifier.height(12.dp))
            Text("Header", style = MaterialTheme.typography.labelMedium, color = Color(0xFFFF5252))
            Text(header, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(12.dp))
            Text("Payload", style = MaterialTheme.typography.labelMedium, color = Color(0xFFAB47BC))
            Text(payload, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(12.dp))
            Text("Signature not verified (no key) — this only decodes.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
