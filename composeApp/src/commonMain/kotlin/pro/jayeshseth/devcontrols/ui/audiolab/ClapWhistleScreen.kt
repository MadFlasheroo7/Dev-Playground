package pro.jayeshseth.devcontrols.ui.audiolab

import androidx.compose.animation.core.Animatable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import pro.jayeshseth.devcontrols.platform.AudioRecorder
import pro.jayeshseth.devcontrols.platform.MicrophonePermissionGate
import pro.jayeshseth.devcontrols.util.fft
import kotlin.math.cos
import kotlin.math.sqrt

private const val N = 1024

/** Clap & whistle detector — counts sharp broadband transients (claps) and sustained tones (whistles). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClapWhistleScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = { TopAppBar(title = { Text("Clap & Whistle") }, navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }) },
        modifier = Modifier.fillMaxSize()
    ) { inner ->
        Column(Modifier.padding(inner).fillMaxSize()) {
            MicrophonePermissionGate("Microphone access is needed to hear claps and whistles.") {
                Detector()
            }
        }
    }
}

@Composable
private fun Detector() {
    var claps by remember { mutableIntStateOf(0) }
    var whistles by remember { mutableIntStateOf(0) }
    var level by remember { mutableFloatStateOf(0f) }
    var freq by remember { mutableFloatStateOf(0f) }
    var whistling by remember { mutableStateOf(false) }
    var sens by remember { mutableFloatStateOf(0.5f) }

    val clapPulse = remember { Animatable(0f) }
    LaunchedEffect(claps) { if (claps > 0) { clapPulse.snapTo(1f); clapPulse.animateTo(0f, androidx.compose.animation.core.tween(450)) } }
    val whistlePulse = remember { Animatable(0f) }
    LaunchedEffect(whistles) { if (whistles > 0) { whistlePulse.snapTo(1f); whistlePulse.animateTo(0f, androidx.compose.animation.core.tween(450)) } }

    LaunchedEffect(sens) { /* sens read live below */ }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.Default) {
            val sr = 44100
            val record = AudioRecorder(sr, N * 2)
            if (!record.isInitialized) { record.release(); return@withContext }
            val window = FloatArray(N) { (0.5f - 0.5f * cos(2.0 * Math.PI * it / (N - 1))).toFloat() }
            val buf = ShortArray(N); val re = FloatArray(N); val im = FloatArray(N)
            var ambient = 0.02f
            var frame = 0; var lastClapFrame = -100
            var whistleRun = 0; var whistleCounted = false; var silenceRun = 0
            record.start()
            try {
                while (isActive) {
                    var off = 0
                    while (off < N) { val n = record.read(buf, off, N - off); if (n <= 0) break; off += n }
                    var sumSq = 0.0; var peak = 0f
                    for (i in 0 until N) { val s = buf[i] / 32768f; sumSq += s * s; val a = if (s < 0) -s else s; if (a > peak) peak = a; re[i] = s * window[i]; im[i] = 0f }
                    val rms = sqrt(sumSq / N).toFloat()
                    fft(re, im)
                    val half = N / 2
                    val binHz = sr.toFloat() / N
                    var peakMag = 0f; var peakBin = 0; var totalMag = 1e-6f
                    for (k in 1 until half) {
                        val mag = re[k] * re[k] + im[k] * im[k]
                        totalMag += mag
                        val f = k * binHz
                        if (f in 700f..4500f && mag > peakMag) { peakMag = mag; peakBin = k }
                    }
                    val peakFreq = peakBin * binHz
                    val tonality = peakMag / totalMag
                    ambient = ambient * 0.97f + peak * 0.03f

                    level = peak; freq = if (tonality > 0.2f) peakFreq else 0f
                    frame++

                    val clapThresh = 0.12f + (1f - sens) * 0.4f
                    val isClap = peak > clapThresh && peak > ambient * 2.5f && tonality < 0.6f && (frame - lastClapFrame) > 5
                    if (isClap) { claps++; lastClapFrame = frame }

                    val tonalThresh = 0.32f - sens * 0.12f
                    val isWhistle = tonality > tonalThresh && peakFreq in 700f..4500f && rms > 0.015f
                    if (isWhistle) {
                        whistleRun++; silenceRun = 0
                        if (whistleRun >= 2 && !whistleCounted) { whistles++; whistleCounted = true }
                        whistling = true
                    } else {
                        silenceRun++; whistleRun = 0
                        if (silenceRun >= 3) { whistleCounted = false; whistling = false }
                    }
                }
            } finally { runCatching { record.stop() }; runCatching { record.release() } }
        }
    }

    Column(Modifier.fillMaxSize().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Counter("👏 CLAPS", claps, Color(0xFFFFB300), clapPulse.value, Modifier.weight(1f))
            Counter("😗 WHISTLES", whistles, Color(0xFF26C6DA), whistlePulse.value, Modifier.weight(1f))
        }
        Spacer(Modifier.height(24.dp))
        Text(if (whistling) "Whistling… ~${freq.toInt()} Hz" else "Listening…", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(8.dp))
        Box(Modifier.fillMaxWidth().height(16.dp).clip(RoundedCornerShape(8.dp)).background(Color(0xFF2A2A38))) {
            Box(Modifier.fillMaxWidth((level * 1.3f).coerceIn(0f, 1f)).height(16.dp).clip(RoundedCornerShape(8.dp)).background(Color(0xFF7CF8FF)))
        }
        Spacer(Modifier.height(24.dp))
        Text("Sensitivity", style = MaterialTheme.typography.labelMedium)
        Slider(value = sens, onValueChange = { sens = it })
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = { claps = 0; whistles = 0 }) { Text("Reset counts") }
        Spacer(Modifier.height(12.dp))
        Text("Clap = sharp loud burst (broadband). Whistle = steady tone (700–4500 Hz). Raise sensitivity if it misses; lower it if it over-counts.",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun Counter(label: String, value: Int, color: Color, pulse: Float, modifier: Modifier) {
    Column(
        modifier.clip(RoundedCornerShape(16.dp)).background(color.copy(alpha = 0.12f + pulse * 0.35f)).padding(vertical = 20.dp)
            .graphicsLayer { val s = 1f + pulse * 0.08f; scaleX = s; scaleY = s },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(label, color = color, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
        Text("$value", color = MaterialTheme.colorScheme.onSurface, fontSize = 52.sp, fontWeight = FontWeight.Bold)
    }
}
