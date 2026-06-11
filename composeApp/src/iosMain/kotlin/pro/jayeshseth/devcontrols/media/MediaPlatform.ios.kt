package pro.jayeshseth.devcontrols.media

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import pro.jayeshseth.media.MediaController

/**
 * iOS [MediaController] — placeholder. Real playback will mirror web: a hidden **WKWebView** running the
 * YouTube IFrame Player, played by video id (so [canPlaySourceUrls] = true, [playSource] takes the id).
 * That needs AVFoundation/WebKit cinterop + an iOS app shell, which must be built/verified on macOS;
 * authored here as a no-op so the iOS target is structurally complete.
 */
private class IosMediaController : MediaController {
    override val playing = MutableStateFlow(false)
    override val positionMs = MutableStateFlow(0L)
    override val durationMs = MutableStateFlow(0L)
    override val buffering: StateFlow<Boolean> = MutableStateFlow(false)
    override val error: StateFlow<String?> = MutableStateFlow("iOS playback not implemented yet")
    override val canPlaySourceUrls = true
    override fun playSource(youtubeUrl: String) {}
    override fun play(url: String) {}
    override fun pause() {}
    override fun resume() {}
    override fun stop() {}
    override fun seekTo(ms: Long) {}
    override fun release() {}
}

@Composable
actual fun rememberMediaController(): MediaController = remember { IosMediaController() }
