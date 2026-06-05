package pro.jayeshseth.devcontrols.ui

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.compose.foundation.layout.Box
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import kotlin.math.log10
import kotlin.math.sqrt

/** Decibel meter — RMS → relative dB SPL gauge with peak hold. (Uncalibrated.) */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DecibelMeterLabScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = { TopAppBar(title = { Text("Decibel Meter") }, navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }) },
        modifier = Modifier.fillMaxSize()
    ) { inner ->
        Column(Modifier.padding(inner).fillMaxSize()) {
            PermGate(android.Manifest.permission.RECORD_AUDIO, "Microphone access is needed to measure sound level.") {
                MeterContent()
            }
        }
    }
}

@Composable
private fun MeterContent() {
    var db by remember { mutableFloatStateOf(0f) }
    var peak by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.Default) {
            val sr = 44100
            val n = 2048
            val minBuf = AudioRecord.getMinBufferSize(sr, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
            val record = runCatching {
                AudioRecord(MediaRecorder.AudioSource.MIC, sr, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, maxOf(minBuf, n * 2))
            }.getOrNull() ?: return@withContext
            if (record.state != AudioRecord.STATE_INITIALIZED) { record.release(); return@withContext }
            val buf = ShortArray(n)
            record.startRecording()
            try {
                while (isActive) {
                    var off = 0
                    while (off < n) { val r = record.read(buf, off, n - off); if (r <= 0) break; off += r }
                    var sum = 0.0
                    for (s in buf) { val v = s / 32768.0; sum += v * v }
                    val rms = sqrt(sum / n)
                    // ~94 dB SPL reference offset; relative not calibrated
                    val value = (20.0 * log10(rms + 1e-9) + 94.0).toFloat().coerceIn(0f, 130f)
                    db = db * 0.6f + value * 0.4f
                    if (db > peak) peak = db else peak *= 0.999f
                }
            } finally { runCatching { record.stop() }; runCatching { record.release() } }
        }
    }

    Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.height(40.dp))
        Text("${db.toInt()}", fontSize = 110.sp, fontWeight = FontWeight.Bold, color = gaugeColor(db))
        Text("dB SPL (approx, uncalibrated)", style = MaterialTheme.typography.bodyMedium)
        Text("peak ${peak.toInt()}", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(24.dp))
        Box(Modifier.fillMaxWidth().height(28.dp).drawBehind {
            val frac = (db / 130f).coerceIn(0f, 1f)
            drawRect(Color(0x33888888), size = size)
            drawRect(gaugeColor(db), size = Size(size.width * frac, size.height))
        })
    }
}

private fun gaugeColor(db: Float): Color = when {
    db < 60 -> Color(0xFF4CAF50)
    db < 85 -> Color(0xFFFFB300)
    else -> Color(0xFFE53935)
}
