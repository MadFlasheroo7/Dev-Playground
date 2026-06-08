package pro.jayeshseth.devcontrols.ui

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlin.coroutines.cancellation.CancellationException
import kotlin.math.PI
import kotlin.math.sin

private class SynthState {
    @Volatile var freq: Double = 0.0
    @Volatile var wave: Int = 0   // 0 sine, 1 square, 2 saw
}

/** Synth keyboard — generates tones live with [AudioTrack]; pick the waveform, hold keys. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SynthKeyboardLabScreen(onBack: () -> Unit) {
    val synth = remember { SynthState() }
    var wave by remember { mutableIntStateOf(0) }

    DisposableEffect(Unit) {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        scope.launch {
            val sr = 44100
            val minBuf = AudioTrack.getMinBufferSize(sr, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT)
            val track = AudioTrack(
                AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build(),
                AudioFormat.Builder().setSampleRate(sr).setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT).build(),
                maxOf(minBuf, 2048), AudioTrack.MODE_STREAM, AudioManager.AUDIO_SESSION_ID_GENERATE
            )
            track.play()
            val n = 512
            val buf = ShortArray(n)
            var phase = 0.0
            try {
                while (isActive) {
                    val f = synth.freq
                    for (i in 0 until n) {
                        val s = if (f <= 0.0) 0.0 else when (synth.wave) {
                            1 -> if (sin(phase) >= 0) 1.0 else -1.0
                            2 -> (phase / (2 * PI)) * 2.0 - 1.0
                            else -> sin(phase)
                        }
                        buf[i] = (s * 0.28 * Short.MAX_VALUE).toInt().toShort()
                        if (f > 0.0) { phase += 2 * PI * f / sr; if (phase > 2 * PI) phase -= 2 * PI }
                    }
                    track.write(buf, 0, n)
                }
            } catch (_: CancellationException) {
            } finally {
                runCatching { track.stop() }; runCatching { track.release() }
            }
        }
        onDispose { scope.cancel() }
    }

    // C4..B4 + C5
    val notes = listOf("C" to 261.63, "C#" to 277.18, "D" to 293.66, "D#" to 311.13, "E" to 329.63,
        "F" to 349.23, "F#" to 369.99, "G" to 392.0, "G#" to 415.30, "A" to 440.0, "A#" to 466.16, "B" to 493.88, "C" to 523.25)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Synth Keyboard") },
                navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }
            )
        },
        modifier = Modifier.fillMaxSize()
    ) { inner ->
        Column(Modifier.padding(inner).fillMaxSize().padding(12.dp)) {
            Text("Waveform", style = MaterialTheme.typography.labelMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Sine", "Square", "Saw").forEachIndexed { i, w ->
                    FilterChip(wave == i, { wave = i; synth.wave = i }, { Text(w) })
                }
            }
            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth().height(360.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                notes.forEachIndexed { i, (name, f) ->
                    val sharp = name.contains("#")
                    Box(
                        Modifier.weight(1f).fillMaxHeight()
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (sharp) Color(0xFF222230) else Color(0xFFEFEFF5))
                            .pointerInput(i) {
                                detectTapGestures(onPress = {
                                    synth.freq = f
                                    tryAwaitRelease()
                                    synth.freq = 0.0
                                })
                            },
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        Text(name, color = if (sharp) Color.White else Color.Black,
                            modifier = Modifier.padding(bottom = 8.dp), style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
            Text("Hold a key to sustain the tone.", style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp))
        }
    }
}
