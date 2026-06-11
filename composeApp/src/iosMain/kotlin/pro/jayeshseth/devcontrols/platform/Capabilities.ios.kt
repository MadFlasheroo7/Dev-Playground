package pro.jayeshseth.devcontrols.platform

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier

// Deck JSON stored uncompressed (no java.util.zip on iOS); DeckJson.read() handles the raw-JSON branch.
actual object DeckArchive {
    actual fun pack(json: String): ByteArray = json.encodeToByteArray()
    actual fun unpack(bytes: ByteArray): String = bytes.decodeToString()
}

@Composable
actual fun rememberDeckFileIo(): DeckFileIo = remember {
    object : DeckFileIo {
        override fun save(suggestedName: String, bytes: ByteArray) { println("[DevControls] iOS save TODO: $suggestedName") }
        override fun open(onLoaded: (ByteArray?) -> Unit) { onLoaded(null) }
    }
}

@Composable
actual fun rememberDeckExporter(): DeckExporter? = null

@Composable
actual fun rememberToaster(): (String) -> Unit = remember { { msg: String -> println("[DevControls] $msg") } }

// TODO (Mac): UIWindowScene fullscreen / orientation lock.
@Composable
actual fun PresentationMode() { /* no-op until iOS app shell exists */ }

// TODO (Mac): Skiko RuntimeEffect for shaders. Falls back to the gradient for now.
@Composable
actual fun ShaderBackground(source: String, modifier: Modifier, fallback: @Composable () -> Unit) {
    Box(modifier) { fallback() }
}
