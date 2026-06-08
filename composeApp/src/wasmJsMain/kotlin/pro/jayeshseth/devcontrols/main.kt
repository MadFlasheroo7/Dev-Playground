package pro.jayeshseth.devcontrols

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.CanvasBasedWindow

/** Web (Wasm) entry point — renders into the <canvas id="ComposeTarget"> in index.html. */
@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    CanvasBasedWindow(canvasElementId = "ComposeTarget") {
        AppRoot()
    }
}
