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
    override val playing: StateFlow<Boolean> = _playing
    override val positionMs: StateFlow<Long> = _positionMs
    override val durationMs: StateFlow<Long> = _durationMs

    /** False if native libVLC could not be loaded (playback unavailable, extraction still works). */
    val available: Boolean

    private val component: AudioPlayerComponent? = runCatching { AudioPlayerComponent() }.getOrNull()

    init {
        available = component != null
        if (component != null) {
            scope.launch {
                while (isActive) {
                    val mp = component.mediaPlayer()
                    _playing.value = mp.status().isPlaying
                    _positionMs.value = mp.status().time().coerceAtLeast(0)
                    _durationMs.value = mp.status().length().coerceAtLeast(0)
                    delay(400)
                }
            }
        }
    }

    override fun play(url: String) { component?.mediaPlayer()?.media()?.play(url) }
    override fun pause() { component?.mediaPlayer()?.controls()?.setPause(true) }
    override fun resume() { component?.mediaPlayer()?.controls()?.setPause(false) }
    override fun stop() { component?.mediaPlayer()?.controls()?.stop() }
    override fun seekTo(ms: Long) { component?.mediaPlayer()?.controls()?.setTime(ms) }
    override fun release() { scope.cancel(); component?.release() }
}
