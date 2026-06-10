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

// --- JS bridge to the YouTube IFrame Player (window.__yt is defined in index.html) ---
private fun ytLoad(id: String) { js("window.__yt && window.__yt.load(id)") }
private fun ytPlay() { js("window.__yt && window.__yt.play()") }
private fun ytPause() { js("window.__yt && window.__yt.pause()") }
private fun ytSeek(sec: Double) { js("window.__yt && window.__yt.seek(sec)") }
private fun ytTimeMs(): Double = js("(window.__yt && window.__yt.timeMs()) || 0")
private fun ytDurationMs(): Double = js("(window.__yt && window.__yt.durationMs()) || 0")
private fun ytState(): Double = js("(window.__yt && window.__yt.state()) ?? -1")

/**
 * Web [MediaController] backed by the **YouTube IFrame Player API** (a hidden 1×1 iframe managed by the
 * `window.__yt` helper in index.html). Plays by video id — no stream extraction, so no PoToken/CORS
 * issues. State is polled from the player (YT states: 1=playing, 3=buffering).
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
