package pro.jayeshseth.devcontrols.media

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import pro.jayeshseth.media.DesktopMediaController
import pro.jayeshseth.media.MediaController

@Composable
actual fun rememberMediaController(): MediaController = remember { DesktopMediaController() }
