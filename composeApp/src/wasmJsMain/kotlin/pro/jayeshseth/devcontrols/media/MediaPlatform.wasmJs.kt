package pro.jayeshseth.devcontrols.media

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import pro.jayeshseth.media.MediaController

/**
 * Web [MediaController] stub so the wasm target compiles. Real web playback (YouTube IFrame Player API)
 * is wired up next. Currently a no-op that surfaces an explanatory error.
 */
private class WebMediaController : MediaController {
    override val playing = MutableStateFlow(false)
    override val positionMs = MutableStateFlow(0L)
    override val durationMs = MutableStateFlow(0L)
    override val buffering: StateFlow<Boolean> = MutableStateFlow(false)
    override val error: StateFlow<String?> = MutableStateFlow("Web playback not implemented yet")
    override fun play(url: String) {}
    override fun pause() {}
    override fun resume() {}
    override fun stop() {}
    override fun seekTo(ms: Long) {}
    override fun release() {}
}

@Composable
actual fun rememberMediaController(): MediaController = remember { WebMediaController() }
