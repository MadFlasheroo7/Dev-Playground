package io.github.shaderengine.sensor

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset

/**
 * Skia / Desktop actual implementation of [PlatformMotionManager].
 *
 * In the absence of physical gyroscopes, synthesizes virtual pitch and roll tilt
 * from cursor parallax offset relative to the window or composable container center,
 * filtered with an exponential low-pass filter (alpha = 0.12).
 */
actual class PlatformMotionManager {
    private var _tilt by mutableStateOf(Offset.Zero)
    actual val tilt: Offset get() = _tilt

    private var smoothedPitch = 0f
    private var smoothedRoll = 0f
    private val alpha = 0.12f
    private var running = false

    actual fun start() {
        running = true
    }

    actual fun stop() {
        running = false
        smoothedPitch = 0f
        smoothedRoll = 0f
        _tilt = Offset.Zero
    }

    actual fun onPointerParallax(position: Offset, bounds: Offset) {
        if (!running) return
        if (bounds.x > 0f && bounds.y > 0f) {
            val cx = bounds.x / 2f
            val cy = bounds.y / 2f
            // Virtual tilt normalized to +/- 1.2 radians
            val targetRoll = ((position.x - cx) / cx).coerceIn(-1.2f, 1.2f)
            val targetPitch = ((position.y - cy) / cy).coerceIn(-1.2f, 1.2f)
            smoothedPitch += alpha * (targetPitch - smoothedPitch)
            smoothedRoll += alpha * (targetRoll - smoothedRoll)
            _tilt = Offset(smoothedRoll, smoothedPitch)
        }
    }

    fun updateTilt(pitch: Float, roll: Float) {
        smoothedPitch = pitch
        smoothedRoll = roll
        _tilt = Offset(roll, pitch)
    }
}

@Composable
actual fun rememberPlatformMotionManager(): PlatformMotionManager {
    val manager = remember { PlatformMotionManager() }
    DisposableEffect(manager) {
        manager.start()
        onDispose {
            manager.stop()
        }
    }
    return manager
}
