package pro.jayeshseth.media

import android.content.Context
import android.net.Uri
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import androidx.media3.common.MediaItem as Media3Item

/**
 * [MediaController] backed by Media3 / ExoPlayer. YouTube `googlevideo` URLs can't be streamed directly
 * (open-ended ranges → 403; range bursts get rate-limited), so a googlevideo URL is first **downloaded
 * to a temp file** via [ChunkedDownloader] and ExoPlayer plays that local file — playback can't stall.
 */
@UnstableApi
class AndroidMediaController(context: Context) : MediaController {
    private val appContext = context.applicationContext
    private val player = ExoPlayer.Builder(appContext).build()
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    @Volatile private var tempFile: File? = null

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

    // We resolve watch URLs ourselves via yt-dlp (uncapped), like mpv on desktop.
    override val canPlaySourceUrls = true

    init {
        player.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) { _playing.value = isPlaying }
            override fun onPlaybackStateChanged(state: Int) {
                if (state != Player.STATE_BUFFERING) _buffering.value = false
            }
            override fun onPlayerError(e: androidx.media3.common.PlaybackException) {
                _error.value = e.errorCodeName + ": " + (e.message ?: "playback error")
                _buffering.value = false
            }
        })
        scope.launch {
            while (isActive) {
                _positionMs.value = player.currentPosition.coerceAtLeast(0)
                _durationMs.value = player.duration.takeIf { it > 0 } ?: 0
                delay(400)
            }
        }
    }

    /** Resolve a YouTube watch URL to an uncapped stream via yt-dlp, then stream it with ExoPlayer. */
    override fun playSource(youtubeUrl: String) {
        _error.value = null; _buffering.value = true
        _positionMs.value = 0; _durationMs.value = 0
        scope.launch {
            val streamUrl = try {
                withContext(Dispatchers.IO) { YtDlp.resolveStreamUrl(appContext, youtubeUrl) }
            } catch (e: Exception) {
                _error.value = "Resolve failed: ${e.message}"; _buffering.value = false
                return@launch
            }
            player.setMediaItem(Media3Item.fromUri(streamUrl)); player.prepare(); player.playWhenReady = true
        }
    }

    override fun play(url: String) {
        _error.value = null; _buffering.value = true
        _positionMs.value = 0; _durationMs.value = 0
        scope.launch {
            val uri = try {
                if (url.contains("googlevideo.com")) {
                    val f = File.createTempFile("dc-exo-", ".media", appContext.cacheDir)
                    tempFile?.delete(); tempFile = f
                    withContext(Dispatchers.IO) { ChunkedDownloader.download(url, f) }
                    Uri.fromFile(f)
                } else Uri.parse(url)
            } catch (e: Exception) {
                _error.value = "Download failed: ${e.message}"; _buffering.value = false
                return@launch
            }
            player.setMediaItem(Media3Item.fromUri(uri)); player.prepare(); player.playWhenReady = true
        }
    }

    override fun pause() { player.playWhenReady = false }
    override fun resume() { player.playWhenReady = true }
    override fun stop() { player.stop() }
    override fun seekTo(ms: Long) { player.seekTo(ms) }
    override fun release() { scope.cancel(); runCatching { tempFile?.delete() }; player.release() }

    /** The underlying ExoPlayer, for an optional video surface (PlayerView) in the UI layer. */
    fun exoPlayer(): ExoPlayer = player
}
