package pro.jayeshseth.devcontrols.platform

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import pro.jayeshseth.devcontrols.ui.present.Deck

// Browser Fullscreen API bridge — js() is target-specific, so expect/actual (identical js + wasmJs actuals).
internal expect fun enterFullscreen()
internal expect fun exitFullscreen()

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

// Enter browser fullscreen while the deck is presenting; restore on exit. (Fullscreen needs a recent
// user gesture — entering present mode from a tap provides it.)
@Composable
actual fun PresentationMode() {
    DisposableEffect(Unit) {
        enterFullscreen()
        onDispose { exitFullscreen() }
    }
}

// TODO when wasm is re-enabled: WebGL/Skiko RuntimeEffect. Falls back to the gradient for now.
@Composable
actual fun ShaderBackground(source: String, modifier: Modifier, fallback: @Composable () -> Unit) {
    Box(modifier) { fallback() }
}
