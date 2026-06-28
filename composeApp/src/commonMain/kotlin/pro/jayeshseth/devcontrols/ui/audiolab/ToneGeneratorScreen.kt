package pro.jayeshseth.devcontrols.ui.audiolab

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import pro.jayeshseth.devcontrols.platform.AudioPlayer
import kotlin.math.PI
import kotlin.math.sin
import kotlin.concurrent.Volatile

private class ToneState { @Volatile var freq = 440.0; @Volatile var wave = 0; @Volatile var on = false }

/** Tone generator — sine/square/saw/triangle at a chosen frequency (20 Hz–8 kHz). */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ToneGeneratorScreen(onBack: () -> Unit) {
    val state = remember { ToneState() }
    var freq by remember { mutableFloatStateOf(440f) }
    var wave by remember { mutableIntStateOf(0) }
    var on by remember { mutableStateOf(false) }
    state.freq = freq.toDouble(); state.wave = wave; state.on = on

    DisposableEffect(Unit) {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        scope.launch {
            val sr = 44100
            val player = AudioPlayer(sr)
            player.start()
            val n = 512; val buf = ShortArray(n); var phase = 0.0
            try {
                while (isActive) {
                    for (i in 0 until n) {
                        val s = if (!state.on) 0.0 else when (state.wave) {
                            1 -> if (sin(phase) >= 0) 1.0 else -1.0
                            2 -> phase / (2 * PI) * 2 - 1
                            3 -> { val t = phase / (2 * PI); (if (t < 0.5) 4 * t - 1 else 3 - 4 * t) }
                            else -> sin(phase)
                        }
                        buf[i] = (s * 0.3 * Short.MAX_VALUE).toInt().toShort()
                        phase += 2 * PI * state.freq / sr; if (phase > 2 * PI) phase -= 2 * PI
                    }
                    player.write(buf, 0, n)
                }
            } finally {
                runCatching { player.stop() }
                runCatching { player.release() }
            }
        }
        onDispose { scope.cancel() }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Tone Generator") }, navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }) },
        modifier = Modifier.fillMaxSize()
    ) { inner ->
        Column(Modifier.padding(inner).fillMaxSize().padding(16.dp)) {
            Text("${freq.toInt()} Hz", fontFamily = FontFamily.Monospace, fontSize = 40.sp)
            Slider(value = freq, onValueChange = { freq = it }, valueRange = 20f..8000f)
            Spacer(Modifier.height(8.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Sine", "Square", "Saw", "Triangle").forEachIndexed { i, w -> FilterChip(wave == i, { wave = i }, { Text(w) }) }
            }
            Spacer(Modifier.height(16.dp))
            Button(onClick = { on = !on }, modifier = Modifier.fillMaxWidth()) { Text(if (on) "Stop" else "Play") }
            Spacer(Modifier.height(8.dp))
            Text("⚠ High frequencies + volume can damage hearing/speakers.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
