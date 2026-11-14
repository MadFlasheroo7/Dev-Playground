package io.github.shaderengine.camera

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.PaintingStyle
import io.github.shaderengine.core.PlatformRuntimeShader
import io.github.shaderengine.core.ShaderController
import kotlin.math.sin

/**
 * Multiplatform video & camera viewfinder texture sampler.
 * Bridges external camera frames, video player surfaces, or synthetic test patterns
 * into [PlatformRuntimeShader] samplers with 0 recompositions.
 */
@Stable
class CameraStreamSampler(
    val width: Int = 320,
    val height: Int = 240
) {
    /** Latest video/viewfinder frame snapshot. */
    var currentFrame by mutableStateOf<ImageBitmap?>(null)
        private set

    private val canvasBitmap = ImageBitmap(width, height)
    private val drawCanvas = Canvas(canvasBitmap)
    private val paint = Paint()

    /**
     * Updates the stream with an external hardware [ImageBitmap] frame.
     */
    fun updateFrame(frame: ImageBitmap) {
        currentFrame = frame
    }

    /**
     * Binds the latest frame snapshot into the shader's sampler uniform (e.g. "cameraStream" or "content").
     */
    fun bindToShader(shader: PlatformRuntimeShader, uniformName: String = "cameraStream") {
        val frame = currentFrame
        if (frame != null) {
            shader.setInputBitmap(uniformName, frame)
        }
    }

    /**
     * Binds the latest frame snapshot into the shader controller's sampler uniform (e.g. "cameraStream" or "content").
     */
    fun bindToShader(controller: ShaderController, uniformName: String = "cameraStream") {
        val frame = currentFrame
        if (frame != null) {
            controller.uniformBuffer.bitmap(uniformName, frame)
        }
    }

    /**
     * Generates a dynamic synthetic camera test pattern (SMPTE color bars + sweeping crosshairs).
     * Provides a realistic live camera texture for instant multiplatform verification
     * on desktop JVM and emulators without physical camera permissions.
     */
    fun generateSyntheticFrame(timeSeconds: Float) {
        val barColors = listOf(
            Color(0xFFE5E5E5), // Light Gray
            Color(0xFFC0C000), // Yellow
            Color(0xFF00C0C0), // Cyan
            Color(0xFF00C000), // Green
            Color(0xFFC000C0), // Magenta
            Color(0xFFC00000), // Red
            Color(0xFF0000C0)  // Blue
        )

        val barW = width.toFloat() / barColors.size
        for (i in barColors.indices) {
            paint.color = barColors[i]
            paint.style = PaintingStyle.Fill
            drawCanvas.drawRect(
                left = i * barW,
                top = 0f,
                right = (i + 1) * barW,
                bottom = height * 0.75f,
                paint = paint
            )
        }

        // Bottom gradient strip
        val bottomH = height * 0.25f
        paint.color = Color(0xFF111827)
        drawCanvas.drawRect(0f, height * 0.75f, width.toFloat(), height.toFloat(), paint)

        // Sweeping radar/viewfinder scanline
        val scanX = ((sin(timeSeconds * 2.0) * 0.5 + 0.5) * width).toFloat()
        paint.color = Color(0xFF00FF66)
        paint.strokeWidth = 3f
        drawCanvas.drawLine(
            p1 = Offset(scanX, 0f),
            p2 = Offset(scanX, height.toFloat()),
            paint = paint
        )

        currentFrame = canvasBitmap
    }
}

/**
 * Remember an active [CameraStreamSampler] with optional synthetic pattern generator.
 */
@Composable
fun rememberCameraStreamSampler(
    width: Int = 320,
    height: Int = 240,
    generateSynthetic: Boolean = true
): CameraStreamSampler {
    val sampler = remember(width, height) { CameraStreamSampler(width, height) }
    if (generateSynthetic) {
        LaunchedEffect(sampler) {
            var startNanos = -1L
            while (true) {
                withFrameNanos { frameNanos ->
                    if (startNanos < 0L) startNanos = frameNanos
                    val t = (frameNanos - startNanos) / 1_000_000_000f
                    sampler.generateSyntheticFrame(t)
                }
            }
        }
    }
    return sampler
}
