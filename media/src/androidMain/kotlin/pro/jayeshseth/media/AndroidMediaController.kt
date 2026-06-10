package pro.jayeshseth.media

import android.content.Context
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import okhttp3.ConnectionPool
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import androidx.media3.common.MediaItem as Media3Item

/**
 * [MediaController] backed by Media3 / ExoPlayer. Plays the resolved progressive audio/video URL via a
 * [YoutubeRangeDataSource] so YouTube's bounded-range requirement is satisfied (a plain ExoPlayer would
 * get HTTP 403 on its open-ended range requests).
 */
@UnstableApi
class AndroidMediaController(context: Context) : MediaController {
    // Zero-size pool → a fresh TCP connection per range request (resets YouTube's per-connection throttle).
    private val httpClient = OkHttpClient.Builder()
        .connectionPool(ConnectionPool(0, 1, TimeUnit.MILLISECONDS))
        .build()
    private val player = ExoPlayer.Builder(context.applicationContext)
        .setMediaSourceFactory(DefaultMediaSourceFactory(YoutubeRangeDataSource.Factory(httpClient)))
        .build()
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private val _playing = MutableStateFlow(false)
    private val _positionMs = MutableStateFlow(0L)
    private val _durationMs = MutableStateFlow(0L)
    override val playing: StateFlow<Boolean> = _playing
    override val positionMs: StateFlow<Long> = _positionMs
    override val durationMs: StateFlow<Long> = _durationMs

    init {
        player.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) { _playing.value = isPlaying }
        })
        scope.launch {
            while (isActive) {
                _positionMs.value = player.currentPosition.coerceAtLeast(0)
                _durationMs.value = player.duration.takeIf { it > 0 } ?: 0
                delay(400)
            }
        }
    }

    override fun play(url: String) {
        player.setMediaItem(Media3Item.fromUri(url)); player.prepare(); player.playWhenReady = true
    }
    override fun pause() { player.playWhenReady = false }
    override fun resume() { player.playWhenReady = true }
    override fun stop() { player.stop() }
    override fun seekTo(ms: Long) { player.seekTo(ms) }
    override fun release() { scope.cancel(); player.release() }

    /** The underlying ExoPlayer, for an optional video surface (PlayerView) in the UI layer. */
    fun exoPlayer(): ExoPlayer = player
}
