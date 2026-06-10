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
    /** True while the player is loading/buffering (resolving, opening the stream, or refilling its cache). */
    val buffering: StateFlow<Boolean>
    /** Last fatal playback error message, or null. Lets the UI distinguish "still loading" from "broken". */
    val error: StateFlow<String?>

    /**
     * True if this controller can play a YouTube **watch URL** directly — resolving the stream itself
     * (e.g. mpv via its yt-dlp hook). When true, the UI calls [playSource] with the watch URL and skips
     * its own [MediaSource.resolve], which matters because NewPipeExtractor's stream URLs are currently
     * PoToken-capped whereas yt-dlp's are not.
     */
    val canPlaySourceUrls: Boolean get() = false

    /** Play a YouTube watch/source URL directly. Only meaningful when [canPlaySourceUrls] is true. */
    fun playSource(youtubeUrl: String) = play(youtubeUrl)

    /** Load [url] (a direct, already-resolved stream URL) and start playback. */
    fun play(url: String)
    fun pause()
    fun resume()
    fun stop()
    fun seekTo(ms: Long)
    /** Free the underlying player. Call from the UI's onDispose. */
    fun release()
}
