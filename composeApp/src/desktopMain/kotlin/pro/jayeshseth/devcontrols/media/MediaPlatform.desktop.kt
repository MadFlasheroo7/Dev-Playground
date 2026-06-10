package pro.jayeshseth.devcontrols.media

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import pro.jayeshseth.media.MediaController
import pro.jayeshseth.media.createDesktopMediaController

@Composable
actual fun rememberMediaController(): MediaController = remember { createDesktopMediaController() }
