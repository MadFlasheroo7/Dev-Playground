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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import java.net.URLDecoder
import java.net.URLEncoder

/** Encoder — live Base64, Hex, and URL encode/decode of any text. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EncoderScreen(onBack: () -> Unit) {
    var input by remember { mutableStateOf("Dev Controls ⚙") }
    val bytes = input.toByteArray()
    val b64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
    val hex = bytes.joinToString(" ") { "%02x".format(it) }
    val url = runCatching { URLEncoder.encode(input, "UTF-8") }.getOrDefault("")
    val b64decode = runCatching { String(Base64.decode(input.trim(), Base64.DEFAULT)) }.getOrDefault("(not valid base64)")
    val urldecode = runCatching { URLDecoder.decode(input, "UTF-8") }.getOrDefault("(not valid)")

    Scaffold(
        topBar = { TopAppBar(title = { Text("Encoder") }, navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }) },
        modifier = Modifier.fillMaxSize()
    ) { inner ->
        Column(Modifier.padding(inner).fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState())) {
            OutlinedTextField(input, { input = it }, label = { Text("Input") }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(12.dp))
            listOf("Base64" to b64, "Hex" to hex, "URL-encoded" to url, "Base64 → decoded" to b64decode, "URL → decoded" to urldecode).forEach { (k, v) ->
                Text(k, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                Text(v, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(10.dp))
            }
        }
    }
}
