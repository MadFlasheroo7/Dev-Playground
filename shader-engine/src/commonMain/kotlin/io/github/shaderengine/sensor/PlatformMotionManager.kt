package io.github.shaderengine.sensor

import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Offset

/**
 * Cross-platform hardware motion and virtual tilt manager.
 *
 * Provides smoothed pitch and roll angles (in radians or normalized parallax units).
 * On Android, binds to [android.hardware.SensorManager] rotation vector / gravity sensors.
 * On Skia / Desktop, derives smooth virtual tilt from cursor offset relative to window center.
 */
expect class PlatformMotionManager {
    /** Smoothed device tilt / parallax offset (x = roll / horizontal, y = pitch / vertical). */
    val tilt: Offset

    /** Starts sensor listeners or motion tracking. Safe to call multiple times. */
    fun start()

    /** Stops sensor listeners to ensure 0% battery/CPU drain when inactive. */
    fun stop()

    /** Updates virtual tilt on platforms that support pointer parallax. */
    fun onPointerParallax(position: Offset, bounds: Offset)
}

/**
 * Remembers a lifecycle-bound [PlatformMotionManager] instance.
 * Automatically starts listening when entering composition and unregisters when disposed.
 */
@Composable
expect fun rememberPlatformMotionManager(): PlatformMotionManager
