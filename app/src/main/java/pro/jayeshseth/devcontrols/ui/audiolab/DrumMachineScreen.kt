package pro.jayeshseth.devcontrols.ui.audiolab

import android.media.AudioManager
import android.media.ToneGenerator
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

private val TRACKS = listOf("Kick" to ToneGenerator.TONE_DTMF_1, "Snare" to ToneGenerator.TONE_DTMF_5, "Hat" to ToneGenerator.TONE_DTMF_9, "Clap" to ToneGenerator.TONE_PROP_BEEP)
private const val STEPS = 16

/** Drum machine — a 4×16 step sequencer using ToneGenerator clicks. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DrumMachineScreen(onBack: () -> Unit) {
    val grid = remember { Array(TRACKS.size) { androidx.compose.runtime.mutableStateListOf(*Array(STEPS) { false }) } }
    var playing by remember { mutableStateOf(false) }
    var bpm by remember { mutableFloatStateOf(120f) }
    var cursor by remember { mutableIntStateOf(0) }
    val tone = remember { ToneGenerator(AudioManager.STREAM_MUSIC, 100) }
    DisposableEffect(Unit) { onDispose { tone.release() } }

    LaunchedEffect(playing, bpm) {
        if (!playing) return@LaunchedEffect
        val stepMs = (60_000f / bpm / 4f).toLong()
        while (playing) {
            for (t in TRACKS.indices) if (grid[t][cursor]) tone.startTone(TRACKS[t].second, 60)
            delay(stepMs)
            cursor = (cursor + 1) % STEPS
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
            TRACKS.forEachIndexed { t, (name, _) ->
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
