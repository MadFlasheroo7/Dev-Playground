package pro.jayeshseth.devcontrols.media

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import pro.jayeshseth.media.AndroidMediaController
import pro.jayeshseth.media.MediaController

@Composable
actual fun rememberMediaController(): MediaController {
    val context = LocalContext.current
    return remember { AndroidMediaController(context) }
}
