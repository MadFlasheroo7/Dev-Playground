package pro.jayeshseth.media

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import uk.co.caprica.vlcj.player.component.AudioPlayerComponent

/**
 * [MediaController] backed by VLCJ. VLCJ is a JNI binding to native **libVLC**, which must be
 * installed on the desktop (e.g. the VLC app). The component is created lazily and guarded, so a
 * machine without libVLC still runs everything else (extraction, search) — playback just no-ops with
 * [available] = false instead of crashing.
 */
class DesktopMediaController : MediaController {
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

    /** False if native libVLC could not be loaded (playback unavailable, extraction still works). */
    val available: Boolean

    private val component: AudioPlayerComponent? = runCatching { AudioPlayerComponent() }.getOrNull()

    /** Bridges YouTube's bounded-range requirement to VLC (which sends open-ended ranges → 403). */
    private val proxy = YoutubeStreamProxy()

    init {
        available = component != null
        if (component == null) {
            _error.value = "libVLC not found — install VLC, or mpv is used when available"
        } else {
            scope.launch {
                while (isActive) {
                    val mp = component.mediaPlayer()
                    val playing = mp.status().isPlaying
                    val pos = mp.status().time().coerceAtLeast(0)
                    _playing.value = playing
                    _positionMs.value = pos
                    _durationMs.value = mp.status().length().coerceAtLeast(0)
                    _buffering.value = !playing && _wantPlaying && pos <= 0
                    delay(400)
                }
            }
        }
    }

    @Volatile private var _wantPlaying = false

    override fun play(url: String) {
        _error.value = null; _wantPlaying = true; _buffering.value = true
        // Route googlevideo URLs through the local range-proxy; play anything else (HLS/DASH) directly.
        val playUrl = if (url.contains("googlevideo.com")) runCatching { proxy.start(url) }.getOrDefault(url) else url
        component?.mediaPlayer()?.media()?.play(playUrl)
    }
    override fun pause() { component?.mediaPlayer()?.controls()?.setPause(true) }
    override fun resume() { component?.mediaPlayer()?.controls()?.setPause(false) }
    override fun stop() { component?.mediaPlayer()?.controls()?.stop() }
    override fun seekTo(ms: Long) { component?.mediaPlayer()?.controls()?.setTime(ms) }
    override fun release() { scope.cancel(); proxy.stop(); component?.release() }
}
