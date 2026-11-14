package io.github.shaderengine.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
import kotlin.math.atan2
import kotlin.math.sqrt

/**
 * Android implementation of [PlatformMotionManager].
 *
 * Utilizes [Sensor.TYPE_ROTATION_VECTOR] for high-precision orientation without gimbal lock,
 * falling back gracefully to [Sensor.TYPE_GRAVITY] or [Sensor.TYPE_ACCELEROMETER].
 * An exponential low-pass filter (alpha = 0.12) is applied to eliminate sensor jitter.
 */
actual class PlatformMotionManager(private val context: Context) : SensorEventListener {
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val sensor = sensorManager?.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        ?: sensorManager?.getDefaultSensor(Sensor.TYPE_GRAVITY)
        ?: sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    private var _tilt by mutableStateOf(Offset.Zero)
    actual val tilt: Offset get() = _tilt

    private val rotationMatrix = FloatArray(9)
    private val orientationAngles = FloatArray(3)
    private var smoothedPitch = 0f
    private var smoothedRoll = 0f
    private val alpha = 0.12f
    private var running = false

    actual fun start() {
        if (running || sensor == null) return
        running = true
        sensorManager?.registerListener(this, sensor, SensorManager.SENSOR_DELAY_GAME)
    }

    actual fun stop() {
        if (!running) return
        running = false
        sensorManager?.unregisterListener(this)
    }

    actual fun onPointerParallax(position: Offset, bounds: Offset) {
        // Physical hardware sensor takes precedence on Android.
    }

    private fun angularDiff(target: Float, current: Float): Float {
        val pi = kotlin.math.PI.toFloat()
        var diff = (target - current) % (2f * pi)
        if (diff > pi) diff -= 2f * pi
        if (diff < -pi) diff += 2f * pi
        return diff
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return
        when (event.sensor.type) {
            Sensor.TYPE_ROTATION_VECTOR -> {
                SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
                SensorManager.getOrientation(rotationMatrix, orientationAngles)
                // orientationAngles[1] = pitch (rotation around X), orientationAngles[2] = roll (rotation around Y)
                val pitch = orientationAngles[1]
                val roll = orientationAngles[2]
                smoothedPitch += alpha * angularDiff(pitch, smoothedPitch)
                smoothedRoll += alpha * angularDiff(roll, smoothedRoll)
                _tilt = Offset(smoothedRoll, smoothedPitch)
            }
            Sensor.TYPE_GRAVITY, Sensor.TYPE_ACCELEROMETER -> {
                val ax = event.values[0]
                val ay = event.values[1]
                val az = event.values[2]
                val pitch = atan2(ay, az)
                val roll = atan2(-ax, sqrt(ay * ay + az * az))
                smoothedPitch += alpha * angularDiff(pitch, smoothedPitch)
                smoothedRoll += alpha * angularDiff(roll, smoothedRoll)
                _tilt = Offset(smoothedRoll, smoothedPitch)
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}

@Composable
actual fun rememberPlatformMotionManager(): PlatformMotionManager {
    val context = LocalContext.current.applicationContext
    val manager = remember(context) { PlatformMotionManager(context) }
    DisposableEffect(manager) {
        manager.start()
        onDispose {
            manager.stop()
        }
    }
    return manager
}
