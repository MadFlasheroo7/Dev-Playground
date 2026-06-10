package pro.jayeshseth.devcontrols

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import kotlinx.browser.document

/** Web (Wasm) entry point. ComposeViewport replaced CanvasBasedWindow in Compose Multiplatform 1.8+. */
@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    ComposeViewport(document.body!!) {
        AppRoot()
    }
}
