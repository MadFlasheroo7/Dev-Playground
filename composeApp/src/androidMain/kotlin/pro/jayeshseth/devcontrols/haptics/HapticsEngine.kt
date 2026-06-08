package pro.jayeshseth.devcontrols.haptics

import android.content.Context
import android.os.Build
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log

/**
 * Thin facade over `Vibrator` / `VibratorManager` that:
 * - Probes device capabilities (multi-actuator, amplitude control, supported
 *   predefined effects, supported primitives).
 * - Exposes one-shot, waveform, predefined-effect, and primitive-composition
 *   playback.
 * - Lets the caller select the [VibrationAttributes] usage so the system
 *   knows whether this is haptic feedback (always on), a touch confirmation
 *   (subject to "Touch feedback" toggle), an alarm-style buzz (uses ringtone
 *   profile), etc.
 *
 * All public API operates on the default vibrator returned by
 * `VibratorManager.getDefaultVibrator()`; the per-actuator path is exposed
 * separately via [vibratorIds] / [vibrateOnActuator] for multi-rotor devices.
 */
class HapticsEngine(context: Context) {

    private val ctx = context.applicationContext
    private val manager: VibratorManager =
        ctx.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
    private val default: Vibrator = manager.defaultVibrator

    // ---- Capability probe ----

    val hasVibrator: Boolean get() = default.hasVibrator()
    val hasAmplitudeControl: Boolean get() = default.hasAmplitudeControl()

    /** All vibrator actuator IDs on this device. Most phones have one. */
    val vibratorIds: IntArray get() = manager.vibratorIds

    /** Per-effect support map. true / false / unknown (older API). */
    fun predefinedEffectSupport(): Map<Int, Support> {
        val effects = intArrayOf(
            VibrationEffect.EFFECT_TICK,
            VibrationEffect.EFFECT_CLICK,
            VibrationEffect.EFFECT_DOUBLE_CLICK,
            VibrationEffect.EFFECT_HEAVY_CLICK,
        )
        val supported = default.areEffectsSupported(*effects)
        return effects.mapIndexed { i, id ->
            id to when (supported[i]) {
                Vibrator.VIBRATION_EFFECT_SUPPORT_YES -> Support.YES
                Vibrator.VIBRATION_EFFECT_SUPPORT_NO -> Support.NO
                else -> Support.UNKNOWN
            }
        }.toMap()
    }

    /** Per-primitive support map for the composition API. */
    fun primitiveSupport(): Map<Int, Boolean> {
        val primitives = mutableListOf(
            VibrationEffect.Composition.PRIMITIVE_CLICK,
            VibrationEffect.Composition.PRIMITIVE_TICK,
            VibrationEffect.Composition.PRIMITIVE_QUICK_RISE,
            VibrationEffect.Composition.PRIMITIVE_SLOW_RISE,
            VibrationEffect.Composition.PRIMITIVE_QUICK_FALL,
        )
        if (Build.VERSION.SDK_INT >= 31) {
            primitives += listOf(
                VibrationEffect.Composition.PRIMITIVE_LOW_TICK,
                VibrationEffect.Composition.PRIMITIVE_THUD,
                VibrationEffect.Composition.PRIMITIVE_SPIN,
            )
        }
        val arr = primitives.toIntArray()
        val supportedArr = default.arePrimitivesSupported(*arr)
        return arr.mapIndexed { i, id -> id to supportedArr[i] }.toMap()
    }

    // ---- Playback ----

    /** Stops any currently-playing vibration on the default actuator. */
    fun cancel() {
        runCatching { default.cancel() }
    }

    /** Stops vibrations on every actuator. */
    fun cancelAll() {
        runCatching { manager.cancel() }
    }

    /**
     * One-shot vibration of [durationMs] at [amplitude]
     * (1..255 or [VibrationEffect.DEFAULT_AMPLITUDE]).
     */
    fun oneShot(durationMs: Long, amplitude: Int, usage: Int = UsageDefaults.TOUCH) {
        if (durationMs <= 0) return
        val effect = VibrationEffect.createOneShot(durationMs, amplitude.coerceIn(1, 255))
        play(effect, usage)
    }

    /**
     * Waveform: alternating off/on for the durations in [timings] starting
     * with off, optionally repeating from [repeat] (-1 = no repeat).
     * If [amplitudes] is non-null its length must equal [timings].
     */
    fun waveform(
        timings: LongArray,
        amplitudes: IntArray? = null,
        repeat: Int = -1,
        usage: Int = UsageDefaults.TOUCH,
    ) {
        if (timings.isEmpty()) return
        val effect = if (amplitudes != null) {
            require(amplitudes.size == timings.size) { "amplitudes size must match timings" }
            VibrationEffect.createWaveform(timings, amplitudes, repeat)
        } else {
            VibrationEffect.createWaveform(timings, repeat)
        }
        play(effect, usage)
    }

    /** Predefined effect ([VibrationEffect.EFFECT_TICK] / CLICK / DOUBLE_CLICK / HEAVY_CLICK). */
    fun predefined(effectId: Int, usage: Int = UsageDefaults.TOUCH) {
        val effect = VibrationEffect.createPredefined(effectId)
        play(effect, usage)
    }

    /**
     * Plays a composition (sequence of primitives with per-step scale 0..1
     * and delay in ms).
     */
    fun composition(steps: List<CompositionStep>, usage: Int = UsageDefaults.TOUCH) {
        if (steps.isEmpty()) return
        val builder = VibrationEffect.startComposition()
        steps.forEach { step ->
            builder.addPrimitive(
                step.primitiveId,
                step.scale.coerceIn(0f, 1f),
                step.delayMs.coerceAtLeast(0)
            )
        }
        play(builder.compose(), usage)
    }

    /** Fire on a specific actuator (multi-rotor devices). */
    fun vibrateOnActuator(actuatorId: Int, effect: VibrationEffect, usage: Int = UsageDefaults.TOUCH) {
        val v = manager.getVibrator(actuatorId)
        if (!v.hasVibrator()) return
        val attrs = VibrationAttributes.Builder().setUsage(usage).build()
        runCatching { v.vibrate(effect, attrs) }
            .onFailure { Log.w(TAG, "actuator $actuatorId vibrate failed", it) }
    }

    private fun play(effect: VibrationEffect, usage: Int) {
        val attrs = VibrationAttributes.Builder().setUsage(usage).build()
        runCatching { default.vibrate(effect, attrs) }
            .onFailure { Log.w(TAG, "vibrate failed", it) }
    }

    enum class Support { YES, NO, UNKNOWN }

    data class CompositionStep(
        val primitiveId: Int,
        val scale: Float,
        val delayMs: Int,
    )

    /** Convenience name → ID maps. */
    object UsageDefaults {
        const val TOUCH = VibrationAttributes.USAGE_TOUCH
        const val NOTIFICATION = VibrationAttributes.USAGE_NOTIFICATION
        const val RINGTONE = VibrationAttributes.USAGE_RINGTONE
        const val ALARM = VibrationAttributes.USAGE_ALARM
        const val HARDWARE_FEEDBACK = VibrationAttributes.USAGE_HARDWARE_FEEDBACK
        const val MEDIA = VibrationAttributes.USAGE_MEDIA
        const val PHYSICAL_EMULATION = VibrationAttributes.USAGE_PHYSICAL_EMULATION
        const val ACCESSIBILITY = VibrationAttributes.USAGE_ACCESSIBILITY
        const val COMMUNICATION_REQUEST = VibrationAttributes.USAGE_COMMUNICATION_REQUEST
        const val UNKNOWN = VibrationAttributes.USAGE_UNKNOWN

        val all: List<Pair<String, Int>> = listOf(
            "TOUCH (haptic feedback — always on)" to TOUCH,
            "NOTIFICATION (subject to notif vibration)" to NOTIFICATION,
            "RINGTONE (uses ring profile)" to RINGTONE,
            "ALARM (alarm-style, bypasses DND)" to ALARM,
            "HARDWARE_FEEDBACK (e.g. fingerprint reader)" to HARDWARE_FEEDBACK,
            "MEDIA (synced w/ media)" to MEDIA,
            "PHYSICAL_EMULATION (game controllers)" to PHYSICAL_EMULATION,
            "ACCESSIBILITY (a11y service)" to ACCESSIBILITY,
            "COMMUNICATION_REQUEST" to COMMUNICATION_REQUEST,
            "UNKNOWN" to UNKNOWN,
        )
    }

    object Effects {
        val all: List<Pair<String, Int>> = listOf(
            "EFFECT_TICK" to VibrationEffect.EFFECT_TICK,
            "EFFECT_CLICK" to VibrationEffect.EFFECT_CLICK,
            "EFFECT_DOUBLE_CLICK" to VibrationEffect.EFFECT_DOUBLE_CLICK,
            "EFFECT_HEAVY_CLICK" to VibrationEffect.EFFECT_HEAVY_CLICK,
        )
    }

    object Primitives {
        val all: List<Pair<String, Int>> get() {
            val base = mutableListOf(
                "PRIMITIVE_CLICK" to VibrationEffect.Composition.PRIMITIVE_CLICK,
                "PRIMITIVE_TICK" to VibrationEffect.Composition.PRIMITIVE_TICK,
                "PRIMITIVE_QUICK_RISE" to VibrationEffect.Composition.PRIMITIVE_QUICK_RISE,
                "PRIMITIVE_SLOW_RISE" to VibrationEffect.Composition.PRIMITIVE_SLOW_RISE,
                "PRIMITIVE_QUICK_FALL" to VibrationEffect.Composition.PRIMITIVE_QUICK_FALL,
            )
            if (Build.VERSION.SDK_INT >= 31) {
                base += listOf(
                    "PRIMITIVE_LOW_TICK" to VibrationEffect.Composition.PRIMITIVE_LOW_TICK,
                    "PRIMITIVE_THUD" to VibrationEffect.Composition.PRIMITIVE_THUD,
                    "PRIMITIVE_SPIN" to VibrationEffect.Composition.PRIMITIVE_SPIN,
                )
            }
            return base
        }
    }

    companion object {
        private const val TAG = "HapticsEngine"
    }
}
