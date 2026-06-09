package pro.jayeshseth.devcontrols.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import pro.jayeshseth.devcontrols.ui.present.Deck

// No java.util.zip on wasm — store the deck JSON uncompressed. DeckJson.read() handles the
// '{' (raw JSON) branch, so these files still open on JVM platforms too.
actual object DeckArchive {
    actual fun pack(json: String): ByteArray = json.encodeToByteArray()
    actual fun unpack(bytes: ByteArray): String = bytes.decodeToString()
}

@Composable
actual fun rememberDeckFileIo(): DeckFileIo = remember {
    object : DeckFileIo {
        override fun save(suggestedName: String, bytes: ByteArray) { println("[DevControls] save not yet supported on web: $suggestedName") }
        override fun open(onLoaded: (ByteArray?) -> Unit) { onLoaded(null) }
    }
}

@Composable
actual fun rememberDeckExporter(): DeckExporter? = null

@Composable
actual fun rememberToaster(): (String) -> Unit = remember { { msg: String -> println("[DevControls] $msg") } }

// TODO when wasm is re-enabled: document.documentElement.requestFullscreen() +
// screen.orientation.lock("landscape") via kotlinx-browser. No-op for now.
@Composable
actual fun PresentationMode() { /* no-op on web until wasm target returns */ }
