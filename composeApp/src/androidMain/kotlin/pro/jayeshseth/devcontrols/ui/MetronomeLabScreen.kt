package pro.jayeshseth.devcontrols.ui

import android.media.AudioManager
import android.media.ToneGenerator
import android.os.VibrationEffect
import android.os.VibratorManager
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

/** Metronome — BPM-timed click (ToneGenerator) + haptic pulse + visual flash. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MetronomeLabScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var bpm by remember { mutableFloatStateOf(100f) }
    var playing by remember { mutableStateOf(false) }
    var beat by remember { mutableIntStateOf(0) }

    val tone = remember { runCatching { ToneGenerator(AudioManager.STREAM_MUSIC, 90) }.getOrNull() }
    DisposableEffect(Unit) { onDispose { tone?.release() } }
    val vibrator = remember {
        (context.getSystemService(android.content.Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
    }

    LaunchedEffect(playing, bpm) {
        if (!playing) return@LaunchedEffect
        while (true) {
            beat++
            tone?.startTone(ToneGenerator.TONE_PROP_BEEP, 40)
            runCatching { vibrator?.vibrate(VibrationEffect.createOneShot(25, VibrationEffect.DEFAULT_AMPLITUDE)) }
            delay((60_000f / bpm).toLong())
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
