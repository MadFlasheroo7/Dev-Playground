package io.github.shaderengine.modifiers

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.RenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import io.github.shaderengine.core.ShaderController
import io.github.shaderengine.core.UniformBuffer

/**
 * Applies a hardware-accelerated shader as a post-processing filter over any live Compose
 * Composable hierarchy using [graphicsLayer].
 *
 * All state reads happen strictly within [graphicsLayer], ensuring 0 recompositions at 60/120 FPS.
 */
fun Modifier.shaderRenderEffect(
    controller: ShaderController,
    contentUniformName: String = "content",
    innerEffect: RenderEffect? = null,
    uniforms: (UniformBuffer.(controller: ShaderController) -> Unit)? = null
): Modifier = this.graphicsLayer {
    val buffer = controller.uniformBuffer

    // Inject standard geometric and dynamic uniforms (both canonical u* and standard names)
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

    buffer.float("uSpecularBoost", 1.0f)

    // User-defined uniforms
    uniforms?.invoke(buffer, controller)

    // Construct and assign render effect
    val effect = controller.platformShader.toRenderEffect(
        contentUniformName = contentUniformName,
        innerEffect = innerEffect
    )
    if (effect != null) {
        renderEffect = effect
    }
}

/**
 * Draws a standalone procedural shader background behind or inside a composable.
 * Driven via [graphicsLayer] invalidation and dynamic per-frame brush evaluation.
 * Zero recomposition cost during frame ticks.
 */
fun Modifier.generativeShaderCanvas(
    controller: ShaderController,
    uniforms: (UniformBuffer.(controller: ShaderController) -> Unit)? = null
): Modifier = this
    .graphicsLayer {
        // Continuous 60/120 FPS hardware layer invalidation hooked to the state clock
        val t = controller.timeSeconds
    }
    .drawBehind {
        val buffer = controller.uniformBuffer

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

        buffer.float("uSpecularBoost", 1.0f)

        uniforms?.invoke(buffer, controller)

        val brush = controller.platformShader.toBrush()
        drawRect(brush = brush)
    }

/**
 * Interactive touch and instantaneous velocity gesture listener that drives the physics
 * of a [ShaderController].
 */
fun Modifier.shaderPointerInput(controller: ShaderController): Modifier =
    this.pointerInput(controller) {
        val velocityTracker = VelocityTracker()
        awaitPointerEventScope {
            while (true) {
                val event = awaitPointerEvent()
                val change = event.changes.firstOrNull() ?: continue
                val bounds = Offset(size.width.toFloat(), size.height.toFloat())

                if (change.pressed) {
                    velocityTracker.addPosition(change.uptimeMillis, change.position)
                    val v = velocityTracker.calculateVelocity()
                    controller.updatePointer(
                        position = change.position,
                        velocity = Offset(v.x, v.y),
                        isPressed = true,
                        bounds = bounds
                    )
                } else if (change.previousPressed && !change.pressed) {
                    controller.onPointerUp()
                    velocityTracker.resetTracking()
                } else {
                    controller.motionManager?.onPointerParallax(change.position, bounds)
                }
            }
        }
    }
