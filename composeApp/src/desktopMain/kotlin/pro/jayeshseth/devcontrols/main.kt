package pro.jayeshseth.devcontrols

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application

/** Desktop (JVM) entry point. Replaced by `AppRoot()` once the common nav shell lands (step 3). */
fun main() = application {
    Window(onCloseRequest = ::exitApplication, title = "DevControls") {
        MaterialTheme {
            Text("DevControls desktop — toolchain OK")
        }
    }
}
