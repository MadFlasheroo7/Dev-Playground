package pro.jayeshseth.devcontrols.ui.threed

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

/**
 * Tactile feedback engine: rich haptics (composition primitives — quick-rise on press,
 * quick-fall on release, ticks at detents) paired with a synthesized "thock" click whose
 * pitch tracks depth. Together they make a flat screen feel like it has physical depth.
 */
class Tactile(context: Context) {
    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= 31)
        (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
    else @Suppress("DEPRECATION") (context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator)

    private val synth = ClickSynth().also { it.start() }

    private fun supports(vararg p: Int): Boolean =
        runCatching { vibrator?.areAllPrimitivesSupported(*p) == true }.getOrDefault(false)

    /** Pressing into the surface — a rise that ramps then clicks at the bottom. */
    fun pressDown(depth: Float) {
        val d = depth.coerceIn(0f, 1f)
        if (supports(VibrationEffect.Composition.PRIMITIVE_QUICK_RISE, VibrationEffect.Composition.PRIMITIVE_CLICK)) {
            val comp = VibrationEffect.startComposition()
                .addPrimitive(VibrationEffect.Composition.PRIMITIVE_QUICK_RISE, 0.3f + 0.6f * d)
                .addPrimitive(VibrationEffect.Composition.PRIMITIVE_CLICK, 0.5f + 0.5f * d)
            runCatching { vibrator?.vibrate(comp.compose()) }
        } else predefined(VibrationEffect.EFFECT_CLICK)
        synth.click(freq = 220f - 120f * d, amp = 0.5f + 0.4f * d, decay = 30f)
    }

    /** Releasing — a quick fall + light tick as it pops back up. */
    fun release() {
        if (supports(VibrationEffect.Composition.PRIMITIVE_QUICK_FALL, VibrationEffect.Composition.PRIMITIVE_TICK)) {
            val comp = VibrationEffect.startComposition()
                .addPrimitive(VibrationEffect.Composition.PRIMITIVE_QUICK_FALL, 0.6f)
                .addPrimitive(VibrationEffect.Composition.PRIMITIVE_TICK, 0.5f)
            runCatching { vibrator?.vibrate(comp.compose()) }
        } else predefined(VibrationEffect.EFFECT_TICK)
        synth.click(freq = 520f, amp = 0.3f, decay = 55f)
    }

    /** A crisp detent — for knob notches / slider steps. */
    fun detent() {
        if (supports(VibrationEffect.Composition.PRIMITIVE_TICK)) {
            runCatching { vibrator?.vibrate(VibrationEffect.startComposition().addPrimitive(VibrationEffect.Composition.PRIMITIVE_TICK, 0.7f).compose()) }
        } else predefined(VibrationEffect.EFFECT_TICK)
        synth.click(freq = 900f, amp = 0.22f, decay = 70f)
    }

    /** A heavy bottom-out thud. */
    fun thud() {
        if (Build.VERSION.SDK_INT >= 31 && supports(VibrationEffect.Composition.PRIMITIVE_THUD)) {
            runCatching { vibrator?.vibrate(VibrationEffect.startComposition().addPrimitive(VibrationEffect.Composition.PRIMITIVE_THUD, 1f).compose()) }
        } else predefined(VibrationEffect.EFFECT_HEAVY_CLICK)
        synth.click(freq = 90f, amp = 0.8f, decay = 18f)
    }

    private fun predefined(effect: Int) {
        runCatching {
            if (Build.VERSION.SDK_INT >= 29) vibrator?.vibrate(VibrationEffect.createPredefined(effect))
            else @Suppress("DEPRECATION") vibrator?.vibrate(20)
        }
    }

    fun close() { synth.stop() }
}

/** Streaming click synth — mixes short decaying sine "thocks" with near-zero latency. */
private class ClickSynth {
    private class Voice(var phase: Double, val freq: Float, val amp: Float, val decay: Float, var age: Float = 0f)
    private val voices = java.util.concurrent.CopyOnWriteArrayList<Voice>()
    @Volatile private var running = false
    private var thread: Thread? = null

    fun click(freq: Float, amp: Float, decay: Float) {
        if (voices.size < 8) voices.add(Voice(0.0, freq, amp, decay))
    }

    fun start() {
        if (running) return
        running = true
        thread = Thread {
            val sr = 44100
            val minBuf = AudioTrack.getMinBufferSize(sr, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT)
            val track = runCatching {
                AudioTrack(
                    AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build(),
                    AudioFormat.Builder().setSampleRate(sr).setChannelMask(AudioFormat.CHANNEL_OUT_MONO).setEncoding(AudioFormat.ENCODING_PCM_16BIT).build(),
                    maxOf(minBuf, 1024), AudioTrack.MODE_STREAM, AudioManager.AUDIO_SESSION_ID_GENERATE
                )
            }.getOrNull() ?: return@Thread
            track.play()
            val n = 256; val buf = ShortArray(n)
            while (running) {
                for (i in 0 until n) {
                    var s = 0.0
                    val it = voices.iterator()
                    for (v in it) {
                        val env = exp(-v.age * v.decay)
                        if (env < 0.001) { voices.remove(v); continue }
                        s += sin(v.phase) * v.amp * env
                        v.phase += 2 * PI * v.freq / sr; if (v.phase > 2 * PI) v.phase -= 2 * PI
                        v.age += 1f / sr
                    }
                    buf[i] = (s.coerceIn(-1.0, 1.0) * Short.MAX_VALUE * 0.7).toInt().toShort()
                }
                track.write(buf, 0, n)
            }
            runCatching { track.stop(); track.release() }
        }.also { it.start() }
    }

    fun stop() { running = false }
}

@Composable
fun rememberTactile(): Tactile {
    val context = LocalContext.current
    val tactile = remember { Tactile(context.applicationContext) }
    DisposableEffect(Unit) { onDispose { tactile.close() } }
    return tactile
}
