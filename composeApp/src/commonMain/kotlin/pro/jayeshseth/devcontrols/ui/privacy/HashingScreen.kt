package pro.jayeshseth.devcontrols.ui.privacy

import pro.jayeshseth.devcontrols.util.fmt
import pro.jayeshseth.devcontrols.util.hex2
import pro.jayeshseth.devcontrols.util.pad
import pro.jayeshseth.devcontrols.util.nowMillis

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
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
import java.security.MessageDigest
import java.util.zip.CRC32

/** Hashing — live MD5 / SHA-1 / SHA-256 / SHA-512 / CRC32 of any text. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HashingScreen(onBack: () -> Unit) {
    var input by remember { mutableStateOf("hello world") }
    fun digest(algo: String) = runCatching {
        MessageDigest.getInstance(algo).digest(input.toByteArray()).joinToString("") { hex2(it) }
    }.getOrDefault("—")
    val crc = remember(input) { CRC32().apply { update(input.toByteArray()) }.value.toString(16) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Hashing") }, navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }) },
        modifier = Modifier.fillMaxSize()
    ) { inner ->
        Column(Modifier.padding(inner).fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState())) {
            OutlinedTextField(input, { input = it }, label = { Text("Input text") }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(16.dp))
            listOf("MD5" to digest("MD5"), "SHA-1" to digest("SHA-1"), "SHA-256" to digest("SHA-256"), "SHA-512" to digest("SHA-512"), "CRC32" to crc).forEach { (name, h) ->
                Text(name, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                Text(h, style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace)
                Spacer(Modifier.height(12.dp))
            }
        }
    }
}
