package pro.jayeshseth.bugrush.audio

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin
import kotlin.random.Random

/**
 * Pure procedural 8-bit chiptune audio synthesizer.
 * Generates raw 16-bit PCM little-endian byte arrays at 44.1 kHz mono.
 * Zero external assets or file dependencies.
 */
object ChiptuneSynthesizer {

    const val SAMPLE_RATE = 44100
    private const val MAX_AMP = 26000.0

    fun square(phase: Double, duty: Double = 0.5): Double =
        if ((phase % 1.0) < duty) 1.0 else -1.0

    fun triangle(phase: Double): Double =
        4.0 * abs((phase % 1.0) - 0.5) - 1.0

    fun sine(phase: Double): Double =
        sin(phase * 2.0 * PI)

    fun noise(): Double =
        Random.nextDouble(-1.0, 1.0)

    private fun writeSample(buffer: ByteArray, sampleIndex: Int, value: Double) {
        val clamped = value.coerceIn(-1.0, 1.0)
        val shortVal = (clamped * MAX_AMP).toInt().coerceIn(-32768, 32767)
        val byteIndex = sampleIndex * 2
        buffer[byteIndex] = (shortVal and 0xFF).toByte()
        buffer[byteIndex + 1] = ((shortVal shr 8) and 0xFF).toByte()
    }

    fun synthesizeSfx(sfx: SoundEffect): ByteArray {
        return when (sfx) {
            SoundEffect.LASER_PEW -> synthesizeLaserPew()
            SoundEffect.RAILGUN_BLAST -> synthesizeRailgunBlast()
            SoundEffect.FLAK_BURST -> synthesizeFlakBurst()
            SoundEffect.DASH_WHOOSH -> synthesizeDashWhoosh()
            SoundEffect.GEM_CHIME -> synthesizeGemChime()
            SoundEffect.BUG_POP -> synthesizeBugPop()
            SoundEffect.PLAYER_HIT -> synthesizePlayerHit()
            SoundEffect.TRIVIA_CORRECT -> synthesizeTriviaCorrect()
            SoundEffect.TRIVIA_WRONG -> synthesizeTriviaWrong()
            SoundEffect.BOSS_SPAWN_SIREN -> synthesizeBossSiren()
            SoundEffect.KERNEL_PANIC -> synthesizeKernelPanic()
            SoundEffect.UPGRADE_UNLOCKED -> synthesizeUpgradeUnlocked()
            SoundEffect.DEFLECT -> synthesizeDeflect()
        }
    }

    private fun synthesizeLaserPew(): ByteArray {
        val durationSec = 0.12
        val totalSamples = (SAMPLE_RATE * durationSec).toInt()
        val buffer = ByteArray(totalSamples * 2)
        var phase = 0.0

        for (i in 0 until totalSamples) {
            val t = i.toDouble() / totalSamples
            // Downward pitch sweep from 920 Hz to 180 Hz
            val freq = 920.0 - 740.0 * (t * t)
            phase += freq / SAMPLE_RATE
            val envelope = 1.0 - t
            val sample = square(phase, 0.5) * envelope
            writeSample(buffer, i, sample)
        }
        return buffer
    }

    private fun synthesizeRailgunBlast(): ByteArray {
        val durationSec = 0.28
        val totalSamples = (SAMPLE_RATE * durationSec).toInt()
        val buffer = ByteArray(totalSamples * 2)
        var phase = 0.0

        for (i in 0 until totalSamples) {
            val t = i.toDouble() / totalSamples
            // Deep sub-bass drop 180 Hz to 35 Hz + initial noise crunch
            val freq = 180.0 * (1.0 - t * 0.8)
            phase += freq / SAMPLE_RATE
            val noisePart = if (t < 0.35) noise() * (1.0 - t / 0.35) * 0.6 else 0.0
            val subBass = square(phase, 0.3) * 0.7
            val envelope = (1.0 - t) * (1.0 - t)
            val sample = (subBass + noisePart) * envelope
            writeSample(buffer, i, sample)
        }
        return buffer
    }

    private fun synthesizeFlakBurst(): ByteArray {
        val durationSec = 0.20
        val totalSamples = (SAMPLE_RATE * durationSec).toInt()
        val buffer = ByteArray(totalSamples * 2)
        var phase = 0.0

        for (i in 0 until totalSamples) {
            val t = i.toDouble() / totalSamples
            val freq = 140.0 * (1.0 - t)
            phase += freq / SAMPLE_RATE
            val noisePart = noise() * 0.65
            val punch = triangle(phase) * 0.35
            val envelope = (1.0 - t) * (1.0 - t)
            writeSample(buffer, i, (noisePart + punch) * envelope)
        }
        return buffer
    }

    private fun synthesizeDashWhoosh(): ByteArray {
        val durationSec = 0.18
        val totalSamples = (SAMPLE_RATE * durationSec).toInt()
        val buffer = ByteArray(totalSamples * 2)
        var phase = 0.0

        for (i in 0 until totalSamples) {
            val t = i.toDouble() / totalSamples
            // Bandpass-like sweep up then down
            val sweep = sin(t * PI)
            val freq = 300.0 + sweep * 700.0
            phase += freq / SAMPLE_RATE
            val sample = (noise() * 0.45 + triangle(phase) * 0.55) * sweep
            writeSample(buffer, i, sample)
        }
        return buffer
    }

    private fun synthesizeGemChime(): ByteArray {
        // C6 (1046.5 Hz) -> E6 (1318.5 Hz) -> G6 (1568 Hz)
        val noteDur = 0.07
        val totalSamples = (SAMPLE_RATE * noteDur * 3).toInt()
        val buffer = ByteArray(totalSamples * 2)
        val notes = doubleArrayOf(1046.5, 1318.5, 1567.98)
        val samplesPerNote = (SAMPLE_RATE * noteDur).toInt()
        var phase = 0.0

        for (noteIdx in 0..2) {
            val freq = notes[noteIdx]
            for (i in 0 until samplesPerNote) {
                val globalIdx = noteIdx * samplesPerNote + i
                val t = i.toDouble() / samplesPerNote
                phase += freq / SAMPLE_RATE
                val envelope = 1.0 - t * 0.8
                val sample = (sine(phase) * 0.7 + triangle(phase) * 0.3) * envelope
                writeSample(buffer, globalIdx, sample)
            }
        }
        return buffer
    }

    private fun synthesizeBugPop(): ByteArray {
        val durationSec = 0.09
        val totalSamples = (SAMPLE_RATE * durationSec).toInt()
        val buffer = ByteArray(totalSamples * 2)
        var phase = 0.0

        for (i in 0 until totalSamples) {
            val t = i.toDouble() / totalSamples
            val freq = 550.0 - 450.0 * t
            phase += freq / SAMPLE_RATE
            val click = if (i < 40) noise() * 0.4 else 0.0
            val envelope = (1.0 - t) * (1.0 - t)
            val sample = (square(phase, 0.4) * 0.7 + click) * envelope
            writeSample(buffer, i, sample)
        }
        return buffer
    }

    private fun synthesizePlayerHit(): ByteArray {
        val durationSec = 0.16
        val totalSamples = (SAMPLE_RATE * durationSec).toInt()
        val buffer = ByteArray(totalSamples * 2)
        var phase = 0.0

        for (i in 0 until totalSamples) {
            val t = i.toDouble() / totalSamples
            val freq = 120.0 - 80.0 * t
            phase += freq / SAMPLE_RATE
            val envelope = (1.0 - t)
            val sample = (noise() * 0.6 + square(phase, 0.25) * 0.4) * envelope
            writeSample(buffer, i, sample)
        }
        return buffer
    }

    private fun synthesizeTriviaCorrect(): ByteArray {
        // Triumphant fanfare: C5 (523) -> E5 (659) -> G5 (784) -> C6 (1046)
        val noteDur = 0.08
        val notes = doubleArrayOf(523.25, 659.25, 783.99, 1046.50)
        val samplesPerNote = (SAMPLE_RATE * noteDur).toInt()
        val totalSamples = samplesPerNote * notes.size
        val buffer = ByteArray(totalSamples * 2)
        var phase = 0.0

        for (noteIdx in notes.indices) {
            val freq = notes[noteIdx]
            for (i in 0 until samplesPerNote) {
                val globalIdx = noteIdx * samplesPerNote + i
                val t = i.toDouble() / samplesPerNote
                phase += freq / SAMPLE_RATE
                val envelope = 1.0 - t * 0.5
                val sample = square(phase, 0.35) * envelope * 0.7
                writeSample(buffer, globalIdx, sample)
            }
        }
        return buffer
    }

    private fun synthesizeTriviaWrong(): ByteArray {
        val durationSec = 0.28
        val totalSamples = (SAMPLE_RATE * durationSec).toInt()
        val buffer = ByteArray(totalSamples * 2)
        var phase1 = 0.0
        var phase2 = 0.0

        for (i in 0 until totalSamples) {
            val t = i.toDouble() / totalSamples
            // Dissonant beating buzz (110 Hz and 117 Hz)
            phase1 += 110.0 / SAMPLE_RATE
            phase2 += 117.0 / SAMPLE_RATE
            val envelope = 1.0 - t * 0.6
            val sample = (square(phase1, 0.5) * 0.5 + square(phase2, 0.5) * 0.5) * envelope
            writeSample(buffer, i, sample)
        }
        return buffer
    }

    private fun synthesizeBossSiren(): ByteArray {
        // Alternating dual-frequency alarm: 440 Hz and 587 Hz
        val sectionDur = 0.14
        val sections = doubleArrayOf(440.0, 587.33, 440.0, 587.33)
        val samplesPerSection = (SAMPLE_RATE * sectionDur).toInt()
        val totalSamples = samplesPerSection * sections.size
        val buffer = ByteArray(totalSamples * 2)
        var phase = 0.0

        for (secIdx in sections.indices) {
            val freq = sections[secIdx]
            for (i in 0 until samplesPerSection) {
                val globalIdx = secIdx * samplesPerSection + i
                phase += freq / SAMPLE_RATE
                val sample = square(phase, 0.5) * 0.65
                writeSample(buffer, globalIdx, sample)
            }
        }
        return buffer
    }

    private fun synthesizeKernelPanic(): ByteArray {
        // Rapid 880 Hz stutter pulses (8 pulses of 45ms each)
        val pulseDur = 0.045
        val pulses = 8
        val samplesPerPulse = (SAMPLE_RATE * pulseDur).toInt()
        val totalSamples = samplesPerPulse * pulses
        val buffer = ByteArray(totalSamples * 2)
        var phase = 0.0

        for (p in 0 until pulses) {
            val isAudio = p % 2 == 0
            for (i in 0 until samplesPerPulse) {
                val globalIdx = p * samplesPerPulse + i
                phase += 880.0 / SAMPLE_RATE
                val sample = if (isAudio) square(phase, 0.5) * 0.8 else 0.0
                writeSample(buffer, globalIdx, sample)
            }
        }
        return buffer
    }

    private fun synthesizeUpgradeUnlocked(): ByteArray {
        // Sparkling ascending synth run: G4 -> C5 -> E5 -> G5 -> C6
        val noteDur = 0.07
        val notes = doubleArrayOf(392.0, 523.25, 659.25, 783.99, 1046.50)
        val samplesPerNote = (SAMPLE_RATE * noteDur).toInt()
        val totalSamples = samplesPerNote * notes.size
        val buffer = ByteArray(totalSamples * 2)
        var phase = 0.0

        for (noteIdx in notes.indices) {
            val freq = notes[noteIdx]
            for (i in 0 until samplesPerNote) {
                val globalIdx = noteIdx * samplesPerNote + i
                val t = i.toDouble() / samplesPerNote
                phase += freq / SAMPLE_RATE
                val envelope = 1.0 - t * 0.4
                val sample = (square(phase, 0.25) * 0.6 + sine(phase * 2.0) * 0.3) * envelope
                writeSample(buffer, globalIdx, sample)
            }
        }
        return buffer
    }

    private fun synthesizeDeflect(): ByteArray {
        val durationSec = 0.10
        val totalSamples = (SAMPLE_RATE * durationSec).toInt()
        val buffer = ByteArray(totalSamples * 2)
        var phase = 0.0

        for (i in 0 until totalSamples) {
            val t = i.toDouble() / totalSamples
            // Metallic high ping: 1200 Hz down to 600 Hz with fast decay
            val freq = 1200.0 - 600.0 * t
            phase += freq / SAMPLE_RATE
            val envelope = (1.0 - t) * (1.0 - t)
            val sample = (triangle(phase) * 0.7 + noise() * 0.3) * envelope
            writeSample(buffer, i, sample)
        }
        return buffer
    }

    fun synthesizeBgm(): ByteArray {
        // 4-bar 130 BPM upbeat retro chiptune loop (~3.69 seconds)
        val bpm = 130.0
        val sixteenthSec = (60.0 / bpm) / 4.0
        val totalSteps = 32 // 2 bars of 16 steps
        val samplesPerStep = (SAMPLE_RATE * sixteenthSec).toInt()
        val totalSamples = samplesPerStep * totalSteps
        val buffer = ByteArray(totalSamples * 2)

        // Bass frequencies (A2, C3, D3, F2)
        val bassFreqs = doubleArrayOf(
            110.0, 110.0, 110.0, 110.0, 130.81, 130.81, 130.81, 130.81,
            146.83, 146.83, 146.83, 146.83, 87.31, 87.31, 110.0, 130.81,
            110.0, 110.0, 110.0, 110.0, 130.81, 130.81, 130.81, 130.81,
            146.83, 146.83, 146.83, 146.83, 164.81, 164.81, 146.83, 130.81
        )

        // Lead melody frequencies (A4, C5, E5, G5, etc.)
        val leadFreqs = doubleArrayOf(
            440.0, 0.0, 523.25, 0.0, 659.25, 0.0, 523.25, 659.25,
            587.33, 0.0, 440.0, 0.0, 392.0, 440.0, 523.25, 0.0,
            440.0, 0.0, 659.25, 0.0, 783.99, 0.0, 659.25, 523.25,
            587.33, 659.25, 587.33, 523.25, 440.0, 0.0, 0.0, 0.0
        )

        var bassPhase = 0.0
        var leadPhase = 0.0

        for (step in 0 until totalSteps) {
            val bFreq = bassFreqs[step]
            val lFreq = leadFreqs[step]

            for (i in 0 until samplesPerStep) {
                val globalIdx = step * samplesPerStep + i
                val t = i.toDouble() / samplesPerStep

                bassPhase += bFreq / SAMPLE_RATE
                val bassSample = triangle(bassPhase) * (1.0 - t * 0.3) * 0.45

                val leadSample = if (lFreq > 0.0) {
                    leadPhase += lFreq / SAMPLE_RATE
                    square(leadPhase, 0.25) * (1.0 - t * 0.5) * 0.35
                } else 0.0

                val hiHat = if (step % 2 == 1 && i < samplesPerStep / 4) noise() * 0.15 * (1.0 - (i.toDouble() / (samplesPerStep / 4))) else 0.0

                val mixed = (bassSample + leadSample + hiHat).coerceIn(-1.0, 1.0)
                writeSample(buffer, globalIdx, mixed)
            }
        }
        return buffer
    }

    fun synthesizeTitleTheme(): ByteArray {
        // 4-bar 130 BPM upbeat retro chiptune title anthem loop (~7.38 seconds, 64 steps)
        val bpm = 130.0
        val sixteenthSec = (60.0 / bpm) / 4.0
        val totalSteps = 64
        val samplesPerStep = (SAMPLE_RATE * sixteenthSec).toInt()
        val totalSamples = samplesPerStep * totalSteps
        val buffer = ByteArray(totalSamples * 2)

        // Bassline: Energetic retro progression (C - Am - F - G)
        val bassFreqs = doubleArrayOf(
            // Bar 1: C Major
            130.81, 130.81, 196.00, 130.81, 164.81, 130.81, 196.00, 130.81,
            130.81, 130.81, 196.00, 130.81, 164.81, 130.81, 196.00, 220.00,
            // Bar 2: A Minor
            110.00, 110.00, 164.81, 110.00, 130.81, 110.00, 164.81, 110.00,
            110.00, 110.00, 164.81, 110.00, 130.81, 110.00, 146.83, 164.81,
            // Bar 3: F Major
            87.31, 87.31, 130.81, 87.31, 110.00, 87.31, 130.81, 87.31,
            98.00, 98.00, 146.83, 98.00, 123.47, 98.00, 146.83, 98.00,
            // Bar 4: G Major / Cadence
            98.00, 98.00, 146.83, 98.00, 196.00, 146.83, 123.47, 98.00,
            110.00, 123.47, 130.81, 146.83, 164.81, 174.61, 196.00, 246.94
        )

        // Lead Melody: Heroic, memorable arcade title anthem
        val leadFreqs = doubleArrayOf(
            // Bar 1: Fanfare opening
            523.25, 0.0, 523.25, 659.25, 783.99, 0.0, 1046.50, 0.0,
            987.77, 783.99, 880.00, 0.0, 783.99, 659.25, 523.25, 0.0,
            // Bar 2: Minor escalation
            440.00, 0.0, 523.25, 659.25, 880.00, 0.0, 1046.50, 0.0,
            1174.66, 1046.50, 987.77, 783.99, 880.00, 0.0, 0.0, 0.0,
            // Bar 3: Climactic ascension
            698.46, 0.0, 880.00, 1046.50, 1318.51, 0.0, 1174.66, 1046.50,
            987.77, 0.0, 1046.50, 1174.66, 1318.51, 0.0, 1174.66, 0.0,
            // Bar 4: Triumphant resolution
            987.77, 0.0, 783.99, 880.00, 987.77, 1046.50, 1174.66, 0.0,
            1046.50, 0.0, 783.99, 0.0, 1046.50, 0.0, 0.0, 0.0
        )

        // Arpeggiated shimmer counter-melody (16th note chiptune arpeggio)
        val arpFreqs = doubleArrayOf(
            261.63, 329.63, 392.00, 523.25, 261.63, 329.63, 392.00, 523.25,
            261.63, 329.63, 392.00, 523.25, 261.63, 329.63, 392.00, 523.25,
            220.00, 261.63, 329.63, 440.00, 220.00, 261.63, 329.63, 440.00,
            220.00, 261.63, 329.63, 440.00, 220.00, 261.63, 329.63, 440.00,
            174.61, 220.00, 261.63, 349.23, 174.61, 220.00, 261.63, 349.23,
            196.00, 246.94, 293.66, 392.00, 196.00, 246.94, 293.66, 392.00,
            196.00, 246.94, 293.66, 392.00, 196.00, 246.94, 293.66, 392.00,
            246.94, 293.66, 349.23, 392.00, 261.63, 329.63, 392.00, 523.25
        )

        var bassPhase = 0.0
        var leadPhase = 0.0
        var arpPhase = 0.0

        for (step in 0 until totalSteps) {
            val bFreq = bassFreqs[step]
            val lFreq = leadFreqs[step]
            val aFreq = arpFreqs[step]
            val isKickStep = (step % 4 == 0)
            val isSnareStep = (step % 4 == 2)

            for (i in 0 until samplesPerStep) {
                val globalIdx = step * samplesPerStep + i
                val t = i.toDouble() / samplesPerStep

                // Bass Channel
                bassPhase += bFreq / SAMPLE_RATE
                val bassSample = (triangle(bassPhase) * 0.4 + square(bassPhase, 0.5) * 0.15) * (1.0 - t * 0.25)

                // Lead Melody Channel
                val leadSample = if (lFreq > 0.0) {
                    leadPhase += lFreq / SAMPLE_RATE
                    square(leadPhase, 0.25) * (1.0 - t * 0.4) * 0.38
                } else 0.0

                // Arpeggio Shimmer Channel
                arpPhase += aFreq / SAMPLE_RATE
                val arpSample = square(arpPhase, 0.125) * (1.0 - t * 0.6) * 0.15

                // 8-bit Percussion
                val kick = if (isKickStep && i < samplesPerStep / 2) {
                    val kickT = i.toDouble() / (samplesPerStep / 2)
                    val kickFreq = 140.0 * (1.0 - kickT * 0.75)
                    triangle(kickFreq * i / SAMPLE_RATE) * (1.0 - kickT) * 0.35
                } else 0.0

                val snare = if (isSnareStep && i < samplesPerStep / 2) {
                    val snareT = i.toDouble() / (samplesPerStep / 2)
                    noise() * (1.0 - snareT) * 0.25
                } else 0.0

                val hiHat = if (step % 2 == 1 && i < samplesPerStep / 5) {
                    val hatT = i.toDouble() / (samplesPerStep / 5)
                    noise() * (1.0 - hatT) * 0.10
                } else 0.0

                val mixed = (bassSample + leadSample + arpSample + kick + snare + hiHat).coerceIn(-1.0, 1.0)
                writeSample(buffer, globalIdx, mixed)
            }
        }
        return buffer
    }
}

