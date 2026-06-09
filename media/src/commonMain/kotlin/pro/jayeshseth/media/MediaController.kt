package pro.jayeshseth.media

import kotlinx.coroutines.flow.StateFlow

/**
 * Audio/video playback control over a resolved [Track.url]. Audio-first (the primary use is music),
 * though the underlying platform players also decode video.
 *
 * Implementations are concrete per platform (so the UI layer can construct them with whatever the
 * platform needs — e.g. an Android `Context`): [pro.jayeshseth.media] ships `AndroidMediaController`
 * (Media3/ExoPlayer) and `DesktopMediaController` (VLCJ); iOS (AVPlayer) and Web (HTML5) follow.
 * State is exposed as [StateFlow]s so a Compose UI can observe play state and scrub position.
 */
interface MediaController {
    val playing: StateFlow<Boolean>
    val positionMs: StateFlow<Long>
    val durationMs: StateFlow<Long>

    /** Load [url] and start playback. */
    fun play(url: String)
    fun pause()
    fun resume()
    fun stop()
    fun seekTo(ms: Long)
    /** Free the underlying player. Call from the UI's onDispose. */
    fun release()
}
