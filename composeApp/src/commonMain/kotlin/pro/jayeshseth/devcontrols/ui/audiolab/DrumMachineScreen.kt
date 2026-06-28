package pro.jayeshseth.devcontrols.ui.audiolab

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import pro.jayeshseth.devcontrols.platform.AudioPlayer
import kotlin.math.PI
import kotlin.math.sin
import kotlin.math.exp

private val TRACKS = listOf("Kick", "Snare", "Hat", "Clap")
private const val STEPS = 16

/** Drum machine — a 4×16 step sequencer using custom PCM mixed drum samples. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DrumMachineScreen(onBack: () -> Unit) {
    val grid = remember { Array(TRACKS.size) { androidx.compose.runtime.mutableStateListOf(*Array(STEPS) { false }) } }
    var playing by remember { mutableStateOf(false) }
    var bpm by remember { mutableFloatStateOf(120f) }
    var cursor by remember { mutableIntStateOf(0) }

    // Pre-synthesized PCM buffers for the drum hits
    val kickBuffer = remember {
        val sr = 44100
        val len = (0.08 * sr).toInt() // 80 ms
        val buf = ShortArray(len)
        for (i in 0 until len) {
            val t = i.toDouble() / sr
            val freq = 150.0 * exp(-40.0 * t) + 40.0
            val phase = 2.0 * PI * freq * t
            val env = exp(-25.0 * t)
            buf[i] = (sin(phase) * env * 0.7 * Short.MAX_VALUE).toInt().toShort()
        }
        buf
    }

    val hatBuffer = remember {
        val sr = 44100
        val len = (0.03 * sr).toInt() // 30 ms
        val buf = ShortArray(len)
        var rand = 12345L
        for (i in 0 until len) {
            rand = (rand * 1103515245L + 12345L)
            val noise = (rand.toDouble() / Long.MAX_VALUE)
            val t = i.toDouble() / sr
            val env = exp(-120.0 * t)
            buf[i] = (noise * env * 0.15 * Short.MAX_VALUE).toInt().toShort()
        }
        buf
    }

    val snareBuffer = remember {
        val sr = 44100
        val len = (0.12 * sr).toInt() // 120 ms
        val buf = ShortArray(len)
        var rand = 54321L
        for (i in 0 until len) {
            val t = i.toDouble() / sr
            rand = (rand * 1103515245L + 12345L)


            val noise = (rand.toDouble() / Long.MAX_VALUE)
            val noiseEnv = exp(-25.0 * t)
            val body = sin(2.0 * PI * 180.0 * t) * exp(-40.0 * t)
            val mix = noise * noiseEnv * 0.4 + body * 0.3
            buf[i] = (mix * Short.MAX_VALUE).toInt().toShort()
        }
        buf
    }

    val clapBuffer = remember {
        val sr = 44100
        val len = (0.16 * sr).toInt() // 160 ms
        val buf = ShortArray(len)
        var rand = 98765L
        for (i in 0 until len) {
            val t = i.toDouble() / sr
            rand = (rand * 1103515245L + 12345L)
            val noise = (rand.toDouble() / Long.MAX_VALUE)
            val env = when {
                t < 0.01 -> exp(-200.0 * t)
                t < 0.02 -> exp(-200.0 * (t - 0.01))
                t < 0.03 -> exp(-200.0 * (t - 0.02))
                else -> exp(-35.0 * (t - 0.03))
            }
            buf[i] = (noise * env * 0.35 * Short.MAX_VALUE).toInt().toShort()
        }
        buf
    }

    val player = remember { AudioPlayer(44100) }
    DisposableEffect(Unit) { onDispose { player.release() } }

    LaunchedEffect(playing, bpm) {
        if (!playing) return@LaunchedEffect
        player.start()
        try {
            while (playing) {
                val stepMs = (60_000f / bpm / 4f).toLong()
                val stepLength = (44100 * (stepMs / 1000f)).toInt()
                val stepBuf = ShortArray(stepLength)

                // Mix active channels
                for (t in TRACKS.indices) {
                    if (grid[t][cursor]) {
                        val soundBuf = when (t) {
                            0 -> kickBuffer
                            1 -> snareBuffer
                            2 -> hatBuffer
                            else -> clapBuffer
                        }
                        for (i in 0 until minOf(stepLength, soundBuf.size)) {
                            val mixed = stepBuf[i] + soundBuf[i]
                            stepBuf[i] = mixed.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
                        }
                    }
                }

                player.write(stepBuf, 0, stepLength)
                delay(stepMs)
                cursor = (cursor + 1) % STEPS
            }
        } finally {
            player.stop()
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Drum Machine") }, navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }) },
        modifier = Modifier.fillMaxSize()
    ) { inner ->
        Column(Modifier.padding(inner).fillMaxSize().padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Button(onClick = { playing = !playing; if (!playing) cursor = 0 }) { Text(if (playing) "Stop" else "Play") }
                Spacer(Modifier.size(12.dp))
                Text("${bpm.toInt()} BPM")
            }
            Slider(value = bpm, onValueChange = { bpm = it }, valueRange = 60f..200f)
            Spacer(Modifier.height(8.dp))
            TRACKS.forEachIndexed { t, name ->
                Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(name, Modifier.width(46.dp), style = MaterialTheme.typography.labelSmall)
                    Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        for (s in 0 until STEPS) {
                            val on = grid[t][s]
                            Box(Modifier.weight(1f).height(34.dp).clip(RoundedCornerShape(4.dp))
                                .background(when {
                                    s == cursor && playing -> Color(0xFFFFFFFF)
                                    on -> MaterialTheme.colorScheme.primary
                                    s % 4 == 0 -> MaterialTheme.colorScheme.surfaceVariant
                                    else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                })
                                .clickable { grid[t][s] = !on })
                        }
                    }
                }
            }
        }
    }
}
