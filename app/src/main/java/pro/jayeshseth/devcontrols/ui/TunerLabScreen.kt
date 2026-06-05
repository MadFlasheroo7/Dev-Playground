package pro.jayeshseth.devcontrols.ui

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import kotlin.math.ln
import kotlin.math.log2
import kotlin.math.roundToInt

private const val TUNE_N = 4096

/** Guitar/instrument tuner — YIN-style autocorrelation pitch detection → note + cents. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TunerLabScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = { TopAppBar(title = { Text("Tuner") }, navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }) },
        modifier = Modifier.fillMaxSize()
    ) { inner ->
        Column(Modifier.padding(inner).fillMaxSize()) {
            PermGate(android.Manifest.permission.RECORD_AUDIO, "Microphone access is needed to detect pitch.") {
                TunerContent()
            }
        }
    }
}

private val NOTE_NAMES = arrayOf("C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B")

@Composable
private fun TunerContent() {
    var freq by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.Default) {
            val sr = 44100
            val minBuf = AudioRecord.getMinBufferSize(sr, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
            val record = runCatching {
                AudioRecord(MediaRecorder.AudioSource.MIC, sr, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, maxOf(minBuf, TUNE_N * 2))
            }.getOrNull() ?: return@withContext
            if (record.state != AudioRecord.STATE_INITIALIZED) { record.release(); return@withContext }
            val buf = ShortArray(TUNE_N); val x = FloatArray(TUNE_N)
            record.startRecording()
            try {
                while (isActive) {
                    var off = 0
                    while (off < TUNE_N) { val n = record.read(buf, off, TUNE_N - off); if (n <= 0) break; off += n }
                    var rms = 0f
                    for (i in 0 until TUNE_N) { x[i] = buf[i] / 32768f; rms += x[i] * x[i] }
                    rms = kotlin.math.sqrt(rms / TUNE_N)
                    if (rms < 0.01f) { freq = 0f; continue }
                    freq = detectPitch(x, sr)
                }
            } finally { runCatching { record.stop() }; runCatching { record.release() } }
        }
    }

    val note = if (freq > 0f) {
        val midi = (69 + 12 * log2(freq / 440f)).roundToInt()
        val name = NOTE_NAMES[(midi % 12 + 12) % 12]
        val octave = midi / 12 - 1
        val refFreq = 440f * Math.pow(2.0, (midi - 69) / 12.0).toFloat()
        val cents = (1200f * log2(freq / refFreq))
        Triple("$name$octave", cents, freq)
    } else null

    Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.height(40.dp))
        Text(note?.first ?: "—", fontSize = 96.sp, fontWeight = FontWeight.Bold,
            color = if (note != null && kotlin.math.abs(note.second) < 5f) Color(0xFF4CAF50) else MaterialTheme.colorScheme.onSurface)
        Text(if (note != null) "${"%.1f".format(note.third)} Hz" else "play a note", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(24.dp))
        // cents meter
        val cents = note?.second ?: 0f
        val outline = MaterialTheme.colorScheme.outline
        val good = kotlin.math.abs(cents) < 5f
        androidx.compose.foundation.layout.Box(Modifier.fillMaxWidth().height(80.dp).drawBehind {
            val cx = size.width / 2f
            drawLine(outline, Offset(cx, 0f), Offset(cx, size.height), strokeWidth = 3f)
            for (c in -50..50 step 10) {
                val px = cx + (c / 50f) * (size.width / 2f) * 0.9f
                drawLine(outline.copy(alpha = 0.4f), Offset(px, size.height * 0.3f), Offset(px, size.height * 0.7f))
            }
            if (note != null) {
                val px = cx + (cents.coerceIn(-50f, 50f) / 50f) * (size.width / 2f) * 0.9f
                drawCircle(if (good) Color(0xFF4CAF50) else Color(0xFFE57373), 16f, Offset(px, size.height / 2f))
            }
        })
        Text(if (note != null) "${note.second.roundToInt()} cents" else "", style = MaterialTheme.typography.bodyMedium)
    }
}

/** Simple autocorrelation pitch detector over 80–1000 Hz. */
private fun detectPitch(x: FloatArray, sr: Int): Float {
    val minLag = sr / 1000
    val maxLag = sr / 80
    var bestLag = -1; var best = 0f
    var prev = 0f; var rising = false
    for (lag in minLag..maxLag) {
        var sum = 0f
        var i = 0
        while (i < x.size - lag) { sum += x[i] * x[i + lag]; i++ }
        if (sum > prev) rising = true
        if (rising && sum < prev && prev > best) { best = prev; bestLag = lag - 1 }
        prev = sum
    }
    return if (bestLag > 0) sr.toFloat() / bestLag else 0f
}
