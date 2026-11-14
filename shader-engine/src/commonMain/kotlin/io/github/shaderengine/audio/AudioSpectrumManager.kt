package io.github.shaderengine.audio

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import io.github.shaderengine.core.ShaderController
import kotlin.math.cos
import kotlin.math.sin

/**
 * Real-time audio spectrum telemetry manager.
 * Holds normalized frequency bands:
 * - [bass]: 20 Hz - 250 Hz (sub-bass & kick drums)
 * - [mid]: 250 Hz - 4000 Hz (vocals & harmonic instruments)
 * - [treble]: 4000 Hz - 16000 Hz (cymbals & high frequencies)
 * - [energy]: Overall RMS amplitude / sound pressure level
 *
 * All properties are backed by Compose snapshot primitives so reading them inside
 * draw/layer phases incurs 0 recompositions.
 */
@Stable
class AudioSpectrumManager {
    /** Normalized bass amplitude [0.0 .. 1.0]. */
    var bass by mutableFloatStateOf(0f)
        private set

    /** Normalized mid-range amplitude [0.0 .. 1.0]. */
    var mid by mutableFloatStateOf(0f)
        private set

    /** Normalized treble/high-frequency amplitude [0.0 .. 1.0]. */
    var treble by mutableFloatStateOf(0f)
        private set

    /** Normalized aggregate audio energy [0.0 .. 1.0]. */
    var energy by mutableFloatStateOf(0f)
        private set

    /** Normalized instantaneous time-domain waveform amplitude [-1.0 .. 1.0]. */
    var waveform by mutableFloatStateOf(0f)
        private set

    /**
     * Updates telemetry with fresh FFT analysis data.
     */
    fun update(
        bassVal: Float,
        midVal: Float,
        trebleVal: Float,
        energyVal: Float = (bassVal * 0.5f + midVal * 0.35f + trebleVal * 0.15f),
        waveformVal: Float = 0f
    ) {
        bass = bassVal.coerceIn(0f, 1f)
        mid = midVal.coerceIn(0f, 1f)
        treble = trebleVal.coerceIn(0f, 1f)
        energy = energyVal.coerceIn(0f, 1f)
        waveform = waveformVal.coerceIn(-1f, 1f)
    }

    /**
     * Automatically binds audio uniforms into a [ShaderController]'s uniform buffer
     * on every frame with 0 recompositions.
     */
    fun bindToShader(controller: ShaderController) {
        controller.uniformBuffer.float("uBass", bass)
        controller.uniformBuffer.float("uMid", mid)
        controller.uniformBuffer.float("uTreble", treble)
        controller.uniformBuffer.float("uAudioEnergy", energy)
        controller.uniformBuffer.float("uAudioWaveform", waveform)
    }

    /**
     * Simulates continuous rhythmic acoustic modulation using harmonic Fourier series.
     * Useful for standalone testing and previewing when live microphone or audio playback is inactive.
     */
    fun simulate(timeSeconds: Float) {
        val b = (sin(timeSeconds * 4.2) * 0.5 + 0.5) * (cos(timeSeconds * 1.8) * 0.3 + 0.7)
        val m = (sin(timeSeconds * 8.5 + 1.2) * 0.5 + 0.5) * 0.85
        val tr = (sin(timeSeconds * 16.0 + 2.5) * 0.5 + 0.5) * 0.65
        val en = (b * 0.55 + m * 0.30 + tr * 0.15)
        val wf = sin(timeSeconds * 36.0) * 0.5
        update(b.toFloat(), m.toFloat(), tr.toFloat(), en.toFloat(), wf.toFloat())
    }
}

/**
 * Remember a stable [AudioSpectrumManager] with an optional continuous simulation driver.
 */
@Composable
fun rememberAudioSpectrumManager(simulatePlayback: Boolean = true): AudioSpectrumManager {
    val manager = remember { AudioSpectrumManager() }
    if (simulatePlayback) {
        LaunchedEffect(manager) {
            var startNanos = -1L
            while (true) {
                withFrameNanos { frameNanos ->
                    if (startNanos < 0L) startNanos = frameNanos
                    val t = (frameNanos - startNanos) / 1_000_000_000f
                    manager.simulate(t)
                }
            }
        }
    }
    return manager
}
