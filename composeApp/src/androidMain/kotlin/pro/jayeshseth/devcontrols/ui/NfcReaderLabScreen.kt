package pro.jayeshseth.devcontrols.ui

import android.app.Activity
import android.nfc.NdefMessage
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.Ndef
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp

/** NFC Reader — reader-mode tag scanning + NDEF record parsing. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NfcReaderLabScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val activity = context as? Activity
    val adapter = remember { NfcAdapter.getDefaultAdapter(context) }
    var report by remember { mutableStateOf("Hold an NFC tag to the back of the phone…") }

    DisposableEffect(adapter, activity) {
        if (adapter == null || activity == null) {
            report = "No NFC adapter on this device."
            return@DisposableEffect onDispose { }
        }
        if (!adapter.isEnabled) report = "NFC is turned off — enable it in Settings."
        val callback = NfcAdapter.ReaderCallback { tag: Tag -> report = describeTag(tag) }
        val flags = NfcAdapter.FLAG_READER_NFC_A or NfcAdapter.FLAG_READER_NFC_B or
            NfcAdapter.FLAG_READER_NFC_F or NfcAdapter.FLAG_READER_NFC_V or
            NfcAdapter.FLAG_READER_NO_PLATFORM_SOUNDS
        runCatching { adapter.enableReaderMode(activity, callback, flags, null) }
        onDispose { runCatching { adapter.disableReaderMode(activity) } }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("NFC Reader") }, navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }) },
        modifier = Modifier.fillMaxSize()
    ) { inner ->
        Column(Modifier.padding(inner).fillMaxSize().padding(16.dp)) {
            Card(Modifier.fillMaxSize()) {
                Text(report, Modifier.padding(16.dp), fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

private fun describeTag(tag: Tag): String = buildString {
    appendLine("UID: " + tag.id.joinToString(":") { "%02X".format(it) })
    appendLine("Tech: " + tag.techList.joinToString(", ") { it.substringAfterLast('.') })
    val ndef = Ndef.get(tag)
    if (ndef != null) {
        appendLine("Type: ${ndef.type}")
        appendLine("Max size: ${ndef.maxSize} bytes, writable: ${ndef.isWritable}")
        val msg: NdefMessage? = ndef.cachedNdefMessage
        msg?.records?.forEachIndexed { i, rec ->
            appendLine("Record $i: tnf=${rec.tnf} type=${String(rec.type)}")
            val payload = rec.payload
            // text record: [status][lang][text]
            val text = runCatching {
                if (rec.tnf.toInt() == 1 && String(rec.type) == "T") {
                    val langLen = payload[0].toInt() and 0x3F
                    String(payload, 1 + langLen, payload.size - 1 - langLen)
                } else String(payload)
            }.getOrDefault("(${payload.size} bytes)")
            appendLine("  payload: $text")
        }
    } else appendLine("No NDEF data.")
}
