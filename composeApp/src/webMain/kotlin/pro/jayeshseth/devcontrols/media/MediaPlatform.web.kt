package pro.jayeshseth.devcontrols.media

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import pro.jayeshseth.media.MediaController

// JS bridge to the YouTube IFrame Player (window.__yt, defined in index.html). The js() intrinsic is
// target-specific, so these are expect/actual (identical actuals for the js and wasmJs targets).
internal expect fun ytLoad(id: String)
internal expect fun ytPlay()
internal expect fun ytPause()
internal expect fun ytSeek(sec: Double)
internal expect fun ytTimeMs(): Double
internal expect fun ytDurationMs(): Double
internal expect fun ytState(): Double

/**
 * Web [MediaController] backed by the **YouTube IFrame Player API** (a hidden offscreen iframe managed by
 * the `window.__yt` helper in index.html). Plays by video id — no stream extraction, so no PoToken/CORS
 * issues. State is polled from the player (YT states: 1=playing, 3=buffering). Shared by js + wasmJs.
 */
private class WebMediaController : MediaController {
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val _playing = MutableStateFlow(false)
    private val _positionMs = MutableStateFlow(0L)
    private val _durationMs = MutableStateFlow(0L)
    private val _buffering = MutableStateFlow(false)
    private val _error = MutableStateFlow<String?>(null)
    override val playing: StateFlow<Boolean> = _playing
    override val positionMs: StateFlow<Long> = _positionMs
    override val durationMs: StateFlow<Long> = _durationMs
    override val buffering: StateFlow<Boolean> = _buffering
    override val error: StateFlow<String?> = _error

    override val canPlaySourceUrls = true

    init {
        scope.launch {
            while (isActive) {
                val state = ytState().toInt()
                _playing.value = state == 1
                _buffering.value = state == 3
                _positionMs.value = ytTimeMs().toLong()
                _durationMs.value = ytDurationMs().toLong()
                delay(400)
            }
        }
    }

    /** [youtubeUrl] is actually the bare video id (from WebMediaSource). */
    override fun playSource(youtubeUrl: String) {
        _error.value = null; _buffering.value = true; _positionMs.value = 0; _durationMs.value = 0
        ytLoad(youtubeUrl)
    }

    override fun play(url: String) = playSource(url)
    override fun pause() = ytPause()
    override fun resume() = ytPlay()
    override fun stop() = ytPause()
    override fun seekTo(ms: Long) = ytSeek(ms / 1000.0)
    override fun release() { scope.cancel() }
}

@Composable
actual fun rememberMediaController(): MediaController = remember { WebMediaController() }
