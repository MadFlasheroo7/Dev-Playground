package io.github.shaderengine.core

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.geometry.Offset
import io.github.shaderengine.sensor.PlatformMotionManager
import io.github.shaderengine.sensor.rememberPlatformMotionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Coordinates frame-accurate uniforms, physics animations, and sensory drivers.
 * All states are backed by Compose snapshot primitives so that reading them inside
 * draw/layer phases triggers NO recomposition cycles.
 */
@Stable
class ShaderController(
    val shaderSource: String,
    private val scope: CoroutineScope,
    val motionManager: PlatformMotionManager? = null
) {
    val platformShader = PlatformRuntimeShader(shaderSource)
    val uniformBuffer = UniformBuffer(platformShader)

    /** Continuous animation clock in seconds. Reading this in graphicsLayer {} causes 0 recompositions. */
    var timeSeconds by mutableFloatStateOf(0f)
        private set

    /** Raw pointer touch coordinate in local pixel space. */
    var rawPointer by mutableStateOf(Offset.Zero)
        private set

    /** Instantaneous velocity vector of the pointer in px/s. */
    var pointerVelocity by mutableStateOf(Offset.Zero)
        private set

    /** Gyroscope / tilt sensor vector (e.g. pitch, roll). */
    var gyroVector by mutableStateOf(Offset.Zero)
        private set

    /** Hardware motion / device tilt in radians (pitch, roll). */
    var tilt by mutableStateOf(Offset.Zero)
        private set

    /** Spring-smoothed pointer position for organic fluid / ferrofluid inertia. */
    private val smoothPointerAnim = Animatable(Offset.Zero, Offset.VectorConverter)
    val smoothPointer: Offset get() = smoothPointerAnim.value

    /** Spring-animated strength/pressure factor (0.0 = released, 1.0 = pressed). */
    private val strengthAnim = Animatable(0f)
    val pointerStrength: Float get() = strengthAnim.value

    fun updatePointer(
        position: Offset,
        velocity: Offset,
        isPressed: Boolean = true,
        bounds: Offset = Offset.Zero
    ) {
        rawPointer = position
        pointerVelocity = velocity
        if (bounds != Offset.Zero) {
            motionManager?.onPointerParallax(position, bounds)
        }
        scope.launch {
            strengthAnim.animateTo(
                targetValue = if (isPressed) 1f else 0f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )
            )
        }
        scope.launch {
            smoothPointerAnim.animateTo(
                targetValue = position,
                animationSpec = spring(
                    dampingRatio = 0.65f,
                    stiffness = 380f
                )
            )
        }
    }

    fun onPointerUp() {
        scope.launch {
            strengthAnim.animateTo(
                targetValue = 0f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioHighBouncy,
                    stiffness = Spring.StiffnessLow
                )
            )
        }
    }

    fun updateGyro(roll: Float, pitch: Float) {
        val o = Offset(roll, pitch)
        gyroVector = o
        tilt = o
    }

    fun updateTilt(roll: Float, pitch: Float) {
        val o = Offset(roll, pitch)
        tilt = o
        gyroVector = o
    }

    /**
     * Continuous frame loop that drives [timeSeconds] via [withFrameNanos].
     */
    suspend fun runClockLoop() {
        var startNanos = -1L
        while (true) {
            withFrameNanos { frameNanos ->
                if (startNanos < 0L) startNanos = frameNanos
                timeSeconds = (frameNanos - startNanos) / 1_000_000_000f
                motionManager?.let {
                    tilt = it.tilt
                    gyroVector = it.tilt
                }
            }
        }
    }
}

/**
 * Remembers a [ShaderController] tied to the Composable lifecycle.
 */
@Composable
fun rememberShaderController(
    shaderSource: String,
    autoRunClock: Boolean = true,
    motionManager: PlatformMotionManager? = rememberPlatformMotionManager()
): ShaderController {
    val scope = rememberCoroutineScope()
    val controller = remember(shaderSource, motionManager) { ShaderController(shaderSource, scope, motionManager) }
    if (autoRunClock) {
        LaunchedEffect(controller) {
            controller.runClockLoop()
        }
    }
    return controller
}
