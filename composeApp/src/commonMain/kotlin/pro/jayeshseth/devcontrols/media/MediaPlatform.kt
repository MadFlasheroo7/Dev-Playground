package pro.jayeshseth.devcontrols.media

import androidx.compose.runtime.Composable
import pro.jayeshseth.media.MediaController

/**
 * Constructs the platform [MediaController] for the UI (the only piece that needs platform context —
 * e.g. an Android `Context` — which is why this lives in the app layer, not the Compose-free `:media`).
 * Android → Media3/ExoPlayer; Desktop → VLCJ.
 */
@Composable
expect fun rememberMediaController(): MediaController
