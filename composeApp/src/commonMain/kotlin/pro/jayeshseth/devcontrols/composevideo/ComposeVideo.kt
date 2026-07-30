package pro.jayeshseth.devcontrols.composevideo

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier

// ─────────────────────────────────────────────────────────────────────────────
// VideoEffect
// ─────────────────────────────────────────────────────────────────────────────

sealed class VideoEffect {
    data class Brightness(val factor: Float = 1f) : VideoEffect()
    data class Contrast(val factor: Float = 1f) : VideoEffect()
    data class Saturation(val factor: Float = 1f) : VideoEffect()
    data class Blur(val radius: Int = 4) : VideoEffect()
    data class Trim(val startMs: Long = 0L, val endMs: Long = Long.MAX_VALUE) : VideoEffect()
    data class SpeedRamp(val startMs: Long, val endMs: Long, val factor: Float) : VideoEffect()
}

// ─────────────────────────────────────────────────────────────────────────────
// VideoResolution
// ─────────────────────────────────────────────────────────────────────────────

enum class VideoResolution(val width: Int, val height: Int, val label: String) {
    P720(1280, 720, "720p HD"),
    P1080(1920, 1080, "1080p Full HD"),
    P1440(2560, 1440, "1440p QHD"),
    SQUARE_1080(1080, 1080, "1080×1080 Square"),
    SQUARE_720(720, 720, "720×720 Square"),
    PREVIEW(480, 480, "480p Preview"),
}

// ─────────────────────────────────────────────────────────────────────────────
// ComposeVideoController — holds all export & playback state persistently
// ─────────────────────────────────────────────────────────────────────────────

@Stable
class ComposeVideoController(
    fps: Int = 30,
    durationMs: Long = 3000L,
    resolution: VideoResolution = VideoResolution.P1080,
) {
    var fps: Int by mutableIntStateOf(fps)
    var durationMs: Long by mutableStateOf(durationMs)
    var resolution: VideoResolution by mutableStateOf(resolution)

    // Audio settings
    var audioUriString: String? by mutableStateOf(null)
    var audioOffsetMs: Long by mutableStateOf(0L)
    var audioVolume: Float by mutableFloatStateOf(1f)

    // Effect settings — persistent across tab changes
    var brightness: Float by mutableFloatStateOf(1f)
    var contrast: Float by mutableFloatStateOf(1f)
    var saturation: Float by mutableFloatStateOf(1f)
    var blurEnabled: Boolean by mutableStateOf(false)
    var blurRadius: Int by mutableIntStateOf(6)
    var trimEnabled: Boolean by mutableStateOf(false)
    var trimStart: Float by mutableFloatStateOf(0f)
    var trimEnd: Float by mutableFloatStateOf(1f)

    // Playback state
    var isPlaying: Boolean by mutableStateOf(false)
    var currentTimeMs: Long by mutableStateOf(0L)
    var isLooping: Boolean by mutableStateOf(true)

    // Export state
    var isExporting: Boolean by mutableStateOf(false)
    var exportProgress: Float by mutableFloatStateOf(0f)
    var exportPhase: String by mutableStateOf("")
    var outputFileName: String by mutableStateOf("compose_video")

    /** Platform-side frame capture hook. */
    var onCaptureFrame: (suspend (frameIndex: Int) -> Any)? = null

    val totalFrames: Int get() = ((durationMs / 1000f) * fps).toInt().coerceAtLeast(1)
    val currentProgress: Float
        get() = if (durationMs <= 0) 0f else (currentTimeMs.toFloat() / durationMs).coerceIn(0f, 1f)

    /** Dynamically computes active VideoEffects from persistent settings. */
    val effects: List<VideoEffect>
        get() {
            val list = mutableListOf<VideoEffect>()
            if (brightness != 1f) list.add(VideoEffect.Brightness(brightness))
            if (contrast != 1f) list.add(VideoEffect.Contrast(contrast))
            if (saturation != 1f) list.add(VideoEffect.Saturation(saturation))
            if (blurEnabled) list.add(VideoEffect.Blur(blurRadius))
            if (trimEnabled) {
                list.add(
                    VideoEffect.Trim(
                        (trimStart * durationMs).toLong(),
                        (trimEnd * durationMs).toLong()
                    )
                )
            }
            return list
        }

    fun reset() {
        isPlaying = false
        currentTimeMs = 0L
        isExporting = false
        exportProgress = 0f
        exportPhase = ""
    }
}

@Composable
fun rememberComposeVideoController(
    fps: Int = 30,
    durationMs: Long = 3000L,
    resolution: VideoResolution = VideoResolution.P1080,
): ComposeVideoController = remember { ComposeVideoController(fps, durationMs, resolution) }

// ─────────────────────────────────────────────────────────────────────────────
// ComposeVideoScope
// ─────────────────────────────────────────────────────────────────────────────

@Stable
class ComposeVideoScope(
    val progress: Float,
    val timeMs: Long,
    val fps: Int,
    val durationMs: Long,
) {
    val frameIndex: Int get() = ((progress * ((durationMs / 1000f) * fps)).toInt()).coerceAtLeast(0)
}

// ─────────────────────────────────────────────────────────────────────────────
// ComposeVideo — expect composable
// ─────────────────────────────────────────────────────────────────────────────

@Composable
expect fun ComposeVideo(
    controller: ComposeVideoController,
    modifier: Modifier = Modifier,
    content: @Composable ComposeVideoScope.() -> Unit,
)
