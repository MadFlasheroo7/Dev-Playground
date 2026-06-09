package pro.jayeshseth.devcontrols

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import pro.jayeshseth.devcontrols.platform.LocalWindowState

/** Desktop (JVM) entry point. Exposes the WindowState so present mode can go fullscreen. */
fun main() = application {
    val windowState = rememberWindowState()
    Window(onCloseRequest = ::exitApplication, state = windowState, title = "DevControls") {
        CompositionLocalProvider(LocalWindowState provides windowState) {
            AppRoot()
        }
    }
}
