package pro.jayeshseth.devcontrols.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import pro.jayeshseth.devcontrols.platform.AudioPlayer
import pro.jayeshseth.devcontrols.platform.rememberHapticPulse
import kotlin.math.PI
import kotlin.math.sin

/** Metronome — BPM-timed click (AudioPlayer) + haptic pulse + visual flash. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MetronomeLabScreen(onBack: () -> Unit) {
    var bpm by remember { mutableFloatStateOf(100f) }
    var playing by remember { mutableStateOf(false) }
    var beat by remember { mutableIntStateOf(0) }
    val hapticPulse = rememberHapticPulse()

    val clickBuf = remember {
        val sr = 44100
        val durationSamples = (0.04 * sr).toInt() // 40 ms
        val buf = ShortArray(durationSamples)
        for (i in 0 until durationSamples) {
            val t = i.toDouble() / sr
            val sine = sin(2.0 * PI * 1000.0 * t)
            val env = (durationSamples - i).toDouble() / durationSamples
            buf[i] = (sine * env * 0.3 * Short.MAX_VALUE).toInt().toShort()
        }
        buf
    }

    val player = remember { AudioPlayer(44100) }
    DisposableEffect(Unit) {
        onDispose {
            player.release()
        }
    }

    LaunchedEffect(playing, bpm) {
        if (!playing) return@LaunchedEffect
        player.start()
        try {
            while (true) {
                beat++
                player.write(clickBuf, 0, clickBuf.size)
                hapticPulse()
                delay((60_000f / bpm).toLong())
            }
        } finally {
            player.stop()
        }
    }

    val pulse by animateFloatAsState(if (beat % 2 == 0) 1f else 0.7f, tween(80), label = "pulse")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Metronome") },
                navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }
            )
        },
        modifier = Modifier.fillMaxSize()
    ) { inner ->
        Column(
            Modifier.padding(inner).fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                Modifier.size(180.dp).scale(if (playing) pulse else 1f).clip(CircleShape)
                    .background(if (playing) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Text("${bpm.toInt()}", color = Color.White, fontSize = 48.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(8.dp))
            Text("BPM", style = MaterialTheme.typography.labelMedium)
            Spacer(Modifier.height(24.dp))
            Slider(value = bpm, onValueChange = { bpm = it }, valueRange = 40f..220f, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(16.dp))
            OutlinedButton(onClick = { playing = !playing }) { Text(if (playing) "Stop" else "Start") }
        }
    }
}
