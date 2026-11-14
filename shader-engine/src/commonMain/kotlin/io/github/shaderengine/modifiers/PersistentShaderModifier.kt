package io.github.shaderengine.modifiers

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.graphicsLayer
import io.github.shaderengine.core.ShaderController
import io.github.shaderengine.core.UniformBuffer

/**
 * Double-buffered ping-pong shader canvas.
 *
 * Maintains two offscreen hardware-accelerated [ImageBitmap] buffers swapped per frame,
 * allowing shaders to sample [uniform shader uPreviousFrame] to produce persistent
 * wake trails, liquid advection, and decaying fluid effects.
 */
fun Modifier.persistentShaderCanvas(
    controller: ShaderController,
    uniforms: (UniformBuffer.(controller: ShaderController) -> Unit)? = null
): Modifier = this
    .graphicsLayer {
        // Continuous hardware layer invalidation hooked to the 60/120 FPS clock
        val t = controller.timeSeconds
    }
    .drawWithCache {
        var frontBuffer: ImageBitmap? = null
        var backBuffer: ImageBitmap? = null
        var frontCanvas: Canvas? = null
        var backCanvas: Canvas? = null
        val drawScope = CanvasDrawScope()

        onDrawBehind {
            val w = size.width.toInt().coerceAtLeast(1)
            val h = size.height.toInt().coerceAtLeast(1)

            if (frontBuffer == null || frontBuffer?.width != w || frontBuffer?.height != h) {
                frontBuffer = ImageBitmap(w, h)
                backBuffer = ImageBitmap(w, h)
                frontCanvas = Canvas(frontBuffer!!)
                backCanvas = Canvas(backBuffer!!)
            }

            val currentFront = frontBuffer!!
            val currentBack = backBuffer!!
            val canvas = backCanvas!!

            val buffer = controller.uniformBuffer

            // Inject standard geometric and dynamic uniforms
            buffer.vec2("uResolution", size.width, size.height)
            buffer.vec2("resolution", size.width, size.height)

            buffer.float("uTime", controller.timeSeconds)
            buffer.float("time", controller.timeSeconds)

            buffer.vec2("uTouch", controller.smoothPointer)
            buffer.vec2("pointer", controller.smoothPointer)

            buffer.vec2("uVelocity", controller.pointerVelocity)
            buffer.vec2("velocity", controller.pointerVelocity)

            buffer.float("uStrength", controller.pointerStrength)
            buffer.float("pointerStrength", controller.pointerStrength)

            buffer.vec2("uTilt", controller.tilt)
            buffer.vec2("tilt", controller.tilt)

            buffer.vec2("uGyro", controller.gyroVector)
            buffer.vec2("gyro", controller.gyroVector)

            buffer.float("uSpecularBoost", 1.0f)

            // Feed previous frame as uPreviousFrame input shader
            buffer.shader("uPreviousFrame", currentFront)

            // User uniforms
            uniforms?.invoke(buffer, controller)

            // Render current frame into backBuffer with direct source replacement
            val brush = controller.platformShader.toBrush()
            drawScope.draw(
                density = this,
                layoutDirection = layoutDirection,
                canvas = canvas,
                size = size
            ) {
                drawRect(brush = brush, blendMode = BlendMode.Src)
            }

            // Draw current rendered frame to screen
            drawImage(currentBack)

            // Ping-pong swap buffers and canvases
            frontBuffer = currentBack
            backBuffer = currentFront
            val tempCanvas = frontCanvas
            frontCanvas = backCanvas
            backCanvas = tempCanvas
        }
    }
