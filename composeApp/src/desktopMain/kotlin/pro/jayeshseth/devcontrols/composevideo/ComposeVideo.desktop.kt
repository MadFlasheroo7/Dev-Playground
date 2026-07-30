package pro.jayeshseth.devcontrols.composevideo

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.asComposeCanvas
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.skia.Surface
import pro.jayeshseth.devcontrols.ui.composevideo.renderTemplateAnimation
import pro.jayeshseth.devcontrols.ui.composevideo.selectedAnimationTemplate
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.File
import javax.imageio.ImageIO

// ─────────────────────────────────────────────────────────────────────────────
// ComposeVideo — Desktop actual
// ─────────────────────────────────────────────────────────────────────────────

@Composable
actual fun ComposeVideo(
    controller: ComposeVideoController,
    modifier: Modifier,
    content: @Composable ComposeVideoScope.() -> Unit,
) {
    val graphicsLayer = rememberGraphicsLayer()

    Box(
        modifier = modifier.drawWithContent {
            graphicsLayer.record { this@drawWithContent.drawContent() }
            drawLayer(graphicsLayer)
        }
    ) {
        ComposeVideoScope(
            progress = controller.currentProgress,
            timeMs = controller.currentTimeMs,
            fps = controller.fps,
            durationMs = controller.durationMs,
        ).content()
    }

    // Live Playback Engine
    LaunchedEffect(controller.isPlaying) {
        if (controller.isPlaying) {
            val frameMs = 1000L / controller.fps
            while (controller.isPlaying) {
                withFrameNanos { }
                controller.currentTimeMs += frameMs
                if (controller.currentTimeMs >= controller.durationMs) {
                    if (controller.isLooping) controller.currentTimeMs = 0L
                    else { controller.isPlaying = false; controller.currentTimeMs = controller.durationMs }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Desktop export — Uses Skia offscreen Surface raster rendering (100% reliable,
// zero black screen) + FFmpeg MP4 encoding matching exact duration.
// ─────────────────────────────────────────────────────────────────────────────

suspend fun exportComposeVideoDesktop(
    controller: ComposeVideoController,
    onProgress: (Float) -> Unit,
    onComplete: (String) -> Unit,
    onError: (String) -> Unit,
) = withContext(Dispatchers.IO) {

    val w = controller.resolution.width
    val h = controller.resolution.height
    val fps = controller.fps
    val totalFrames = controller.totalFrames

    val trimEffect = controller.effects.filterIsInstance<VideoEffect.Trim>().lastOrNull()
    val trimStartMs = trimEffect?.startMs ?: 0L
    val trimEndMs = trimEffect?.endMs ?: controller.durationMs

    val activeDurationMs = (trimEndMs - trimStartMs).coerceAtLeast(500L)
    val activeDurationSec = activeDurationMs / 1000f

    try {
        val tempDir = File.createTempFile("cvid_desktop_", "_frames")
        tempDir.delete()
        tempDir.mkdirs()

        controller.exportPhase = "Rendering frames via Skia Engine…"

        // Skia offscreen surface for synchronous raster rendering
        val surface = Surface.makeRasterN32Premul(w, h)
        val skiaCanvas = surface.canvas
        val composeCanvas = skiaCanvas.asComposeCanvas()
        val drawScope = CanvasDrawScope()

        val template = selectedAnimationTemplate
        var savedFrames = 0

        for (frameIdx in 0 until totalFrames) {
            val frameTimeMs = (frameIdx * 1000L) / fps
            if (frameTimeMs < trimStartMs || frameTimeMs > trimEndMs) continue

            val progress = (frameTimeMs.toFloat() / controller.durationMs).coerceIn(0f, 1f)

            // Render frame onto Skia canvas offscreen
            skiaCanvas.clear(0)
            drawScope.draw(
                density = Density(1f),
                layoutDirection = LayoutDirection.Ltr,
                canvas = composeCanvas,
                size = androidx.compose.ui.geometry.Size(w.toFloat(), h.toFloat())
            ) {
                renderTemplateAnimation(template, progress, frameTimeMs)
            }

            val imageSnapshot = surface.makeImageSnapshot()
            val pngBytes = imageSnapshot.encodeToData()?.bytes
                ?: throw RuntimeException("Failed to encode Skia snapshot for frame $frameIdx")

            val bufferedImage = ImageIO.read(ByteArrayInputStream(pngBytes))
            val file = File(tempDir, "frame_%04d.png".format(savedFrames++))
            ImageIO.write(bufferedImage, "PNG", file)

            onProgress((frameIdx + 1).toFloat() / totalFrames * 0.7f)
        }

        if (savedFrames == 0) {
            onError("No frames captured. Check trim range.")
            tempDir.deleteRecursively()
            return@withContext
        }

        // Target: ~/Movies/ComposeVideo/<outputFileName>.mp4
        val moviesDir = File(System.getProperty("user.home"), "Movies/ComposeVideo")
        moviesDir.mkdirs()
        val outputFile = File(moviesDir, "${controller.outputFileName}.mp4")

        controller.exportPhase = "Encoding MP4 video via FFmpeg…"
        onProgress(0.85f)

        val audioPath = controller.audioUriString
        val audioFile = audioPath?.let { File(it) }
        val hasAudio = audioFile != null && audioFile.exists() && audioFile.length() > 0

        // FFmpeg command using -framerate before input and explicit -t for exact video duration
        val ffmpegCmd = mutableListOf(
            "ffmpeg", "-y",
            "-framerate", "$fps",
            "-i", File(tempDir, "frame_%04d.png").absolutePath
        )

        if (hasAudio) {
            val offsetSec = (controller.audioOffsetMs / 1000f).coerceAtLeast(0f)
            ffmpegCmd.addAll(
                listOf(
                    "-ss", "$offsetSec",
                    "-i", audioFile!!.absolutePath,
                    "-map", "0:v:0",
                    "-map", "1:a:0?",
                    "-c:a", "aac",
                    "-filter:a", "volume=${controller.audioVolume}",
                    "-t", "$activeDurationSec"
                )
            )
        } else {
            ffmpegCmd.addAll(
                listOf(
                    "-map", "0:v:0",
                    "-t", "$activeDurationSec"
                )
            )
        }

        ffmpegCmd.addAll(
            listOf(
                "-c:v", "libx264",
                "-pix_fmt", "yuv420p",
                "-vf", "scale=trunc(iw/2)*2:trunc(ih/2)*2",
                outputFile.absolutePath
            )
        )

        val process = ProcessBuilder(ffmpegCmd)
            .redirectErrorStream(true)
            .start()

        val exitCode = process.waitFor()
        tempDir.deleteRecursively()

        if (exitCode == 0 && outputFile.exists() && outputFile.length() > 0) {
            onProgress(1f)
            onComplete(outputFile.absolutePath)
        } else {
            val output = process.inputStream.bufferedReader().readText().takeLast(400)
            onError("FFmpeg encoding failed (code $exitCode):\n$output")
        }

    } catch (e: Exception) {
        onError(e.message ?: "Desktop export failed")
    }
}
