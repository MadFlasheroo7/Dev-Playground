package pro.jayeshseth.devcontrols.ui.audiolab

import androidx.compose.animation.core.withInfiniteAnimationFrameMillis
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
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

private class BinauralState {
    @Volatile var carrier = 400f
    @Volatile var beat = 10f
    @Volatile var volume = 0.3f
    @Volatile var on = false
}

private data class BrainwavePreset(
    val name: String,
    val beat: Float,
    val description: String
)

private val PRESETS = listOf(
    BrainwavePreset("Delta", 2f, "Deep sleep, healing, rejuvenation"),
    BrainwavePreset("Theta", 6f, "Deep meditation, dreaming, creativity"),
    BrainwavePreset("Alpha", 10f, "Relaxed focus, visual learning, stress relief"),
    BrainwavePreset("Beta", 20f, "Active thinking, cognitive concentration, alertness"),
    BrainwavePreset("Gamma", 40f, "Peak mental processing, memory organization, high focus")
)

/** Binaural Beats — stereo generator playing mismatched sine waves in left & right ear. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BinauralBeatsScreen(onBack: () -> Unit) {
    val state = remember { BinauralState() }
    var carrier by remember { mutableFloatStateOf(400f) }
    var beat by remember { mutableFloatStateOf(10f) }
    var volume by remember { mutableFloatStateOf(0.3f) }
    var on by remember { mutableStateOf(false) }

    state.carrier = carrier
    state.beat = beat
    state.volume = volume
    state.on = on

    DisposableEffect(Unit) {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        scope.launch {
            val sr = 44100
            // Create a Stereo (2 channels) AudioPlayer
            val player = AudioPlayer(sr, channels = 2)
            player.start()
            val n = 256
            val buf = ShortArray(2 * n)
            var leftPhase = 0.0
            var rightPhase = 0.0
            try {
                while (isActive) {
                    val lf = state.carrier.toDouble()
                    val rf = (state.carrier + state.beat).toDouble()
                    val vol = state.volume.toDouble()
                    val play = state.on

                    for (i in 0 until n) {
                        val leftS = if (!play) 0.0 else sin(leftPhase) * vol
                        buf[2 * i] = (leftS * Short.MAX_VALUE).toInt().toShort()

                        val rightS = if (!play) 0.0 else sin(rightPhase) * vol
                        buf[2 * i + 1] = (rightS * Short.MAX_VALUE).toInt().toShort()

                        if (play) {
                            leftPhase += 2.0 * PI * lf / sr
                            if (leftPhase > 2.0 * PI) leftPhase -= 2.0 * PI
                            rightPhase += 2.0 * PI * rf / sr
                            if (rightPhase > 2.0 * PI) rightPhase -= 2.0 * PI
                        }
                    }
                    player.write(buf, 0, 2 * n)
                }
            } finally {
                runCatching { player.stop() }
                runCatching { player.release() }
            }
        }
        onDispose { scope.cancel() }
    }

    val time by produceState(0f) {
        while (true) {
            withInfiniteAnimationFrameMillis { value = (it % 100000) / 1000f }
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Binaural Beats") }, navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }) },
        modifier = Modifier.fillMaxSize()
    ) { inner ->
        Column(Modifier.padding(inner).fillMaxSize().padding(16.dp)) {
            Text("Carrier: ${carrier.toInt()} Hz | Beat: ${beat.toInt()} Hz (${getPresetLabel(beat)})", fontSize = 18.sp, style = MaterialTheme.typography.titleMedium)
            
            // Custom Visualizer Canvas
            Box(Modifier.fillMaxWidth().height(160.dp).background(Color(0xFF0F0F1A))) {
                Canvas(Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height
                    val midY = h / 2f
                    val amp = h * 0.25f

                    val pathLeft = Path()
                    val pathRight = Path()
                    
                    pathLeft.moveTo(0f, midY)
                    pathRight.moveTo(0f, midY)

                    // Draw overlapping left (pink) and right (cyan) waves
                    for (x in 0 until w.toInt() step 2) {
                        val t = x / w
                        val leftVal = sin(t * 2 * PI * 3 + time * 3) * amp
                        val rightVal = sin(t * 2 * PI * (3 + beat / 100f) + time * 3) * amp
                        pathLeft.lineTo(x.toFloat(), midY + leftVal.toFloat())
                        pathRight.lineTo(x.toFloat(), midY + rightVal.toFloat())
                    }

                    drawPath(pathLeft, Color(0xFFFF2E93), style = Stroke(width = 3f))
                    drawPath(pathRight, Color(0xFF00FEFE), style = Stroke(width = 3f))
                    
                    // Draw a subtle yellow beat envelope guide line
                    val envelopePath = Path()
                    envelopePath.moveTo(0f, midY)
                    for (x in 0 until w.toInt() step 4) {
                        val t = x / w
                        // The beating envelope is shaped by the offset/beat frequency
                        val env = sin(t * PI * (beat / 10f) + time) * amp * 1.2f
                        if (x == 0) envelopePath.moveTo(0f, midY + env.toFloat())
                        else envelopePath.lineTo(x.toFloat(), midY + env.toFloat())
                    }
                    drawPath(envelopePath, Color(0xFFFFE600), style = Stroke(width = 2f, pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)))
                }
            }

            Spacer(Modifier.height(16.dp))

            Text("Carrier Frequency (Hz)", style = MaterialTheme.typography.labelMedium)
            Slider(value = carrier, onValueChange = { carrier = it }, valueRange = 100f..1000f)
            
            Text("Beat Frequency (Hz)", style = MaterialTheme.typography.labelMedium)
            Slider(value = beat, onValueChange = { beat = it }, valueRange = 1f..50f)
            
            Text("Volume", style = MaterialTheme.typography.labelMedium)
            Slider(value = volume, onValueChange = { volume = it }, valueRange = 0.0f..1.0f)

            Spacer(Modifier.height(8.dp))

            Text("Brainwave Presets", style = MaterialTheme.typography.labelMedium)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                PRESETS.forEach { preset ->
                    FilterChip(
                        selected = beat == preset.beat,
                        onClick = { beat = preset.beat },
                        label = { Text(preset.name, fontSize = 11.sp) }
                    )
                }
            }

            Spacer(Modifier.height(8.dp))
            // Description of current preset
            val activeDesc = PRESETS.firstOrNull { it.beat == beat }?.description ?: "Custom brainwave frequency shift"
            Text(activeDesc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

            Spacer(Modifier.weight(1f))

            Button(onClick = { on = !on }, modifier = Modifier.fillMaxWidth()) {
                Text(if (on) "Stop Generator" else "Start Generator (Use Headphones)")
            }
        }
    }
}

private fun getPresetLabel(beat: Float): String {
    return when (beat) {
        2f -> "Delta"
        6f -> "Theta"
        10f -> "Alpha"
        20f -> "Beta"
        40f -> "Gamma"
        else -> "Custom"
    }
}
