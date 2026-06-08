package pro.jayeshseth.devcontrols.ui

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import pro.jayeshseth.devcontrols.util.fft
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.sqrt

private const val SPEC_FFT = 1024
private const val SPEC_BANDS = 64

/** FFT Spectrum + scrolling spectrogram from the mic. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpectrumLabScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = { TopAppBar(title = { Text("Spectrum") }, navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }) },
        modifier = Modifier.fillMaxSize()
    ) { inner ->
        Column(Modifier.padding(inner).fillMaxSize()) {
            PermGate(android.Manifest.permission.RECORD_AUDIO, "Microphone access is needed to show the live spectrum.") {
                SpectrumContent()
            }
        }
    }
}

@Composable
private fun SpectrumContent() {
    var bars by remember { mutableStateOf(FloatArray(SPEC_BANDS)) }
    // spectrogram ring buffer: list of columns (each FloatArray of bands)
    val history = remember { ArrayDeque<FloatArray>() }
    var tick by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.Default) {
            val sr = 44100
            val minBuf = AudioRecord.getMinBufferSize(sr, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
            val record = runCatching {
                AudioRecord(MediaRecorder.AudioSource.MIC, sr, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, maxOf(minBuf, SPEC_FFT * 2))
            }.getOrNull() ?: return@withContext
            if (record.state != AudioRecord.STATE_INITIALIZED) { record.release(); return@withContext }
            val window = FloatArray(SPEC_FFT) { 0.5f - 0.5f * cos(2.0 * Math.PI * it / (SPEC_FFT - 1)).toFloat() }
            val buf = ShortArray(SPEC_FFT); val re = FloatArray(SPEC_FFT); val im = FloatArray(SPEC_FFT)
            record.startRecording()
            try {
                while (isActive) {
                    var off = 0
                    while (off < SPEC_FFT) { val n = record.read(buf, off, SPEC_FFT - off); if (n <= 0) break; off += n }
                    for (i in 0 until SPEC_FFT) { re[i] = (buf[i] / 32768f) * window[i]; im[i] = 0f }
                    fft(re, im)
                    val half = SPEC_FFT / 2
                    val out = FloatArray(SPEC_BANDS)
                    for (b in 0 until SPEC_BANDS) {
                        // log-spaced bins
                        val lo = (Math.pow(half.toDouble(), b.toDouble() / SPEC_BANDS)).toInt().coerceIn(1, half - 1)
                        val hi = (Math.pow(half.toDouble(), (b + 1.0) / SPEC_BANDS)).toInt().coerceIn(lo + 1, half)
                        var mx = 0f
                        for (k in lo until hi) { val mag = sqrt(re[k] * re[k] + im[k] * im[k]); if (mag > mx) mx = mag }
                        out[b] = (ln(1f + mx * 8f) / 4f).coerceIn(0f, 1f)
                    }
                    bars = out
                    history.addLast(out)
                    if (history.size > 160) history.removeFirst()
                    tick++
                }
            } finally { runCatching { record.stop() }; runCatching { record.release() } }
        }
    }

    Column(Modifier.fillMaxSize()) {
        // bars
        Box(Modifier.fillMaxSize().weight(1f).background(Color(0xFF06060C)).drawBehind {
            tick
            val bw = size.width / SPEC_BANDS
            for (i in 0 until SPEC_BANDS) {
                val h = bars[i] * size.height
                drawRect(Color.hsv((i * 300f / SPEC_BANDS), 0.9f, 1f), Offset(i * bw, size.height - h), Size(bw - 1.5f, h))
            }
        })
        // spectrogram
        Box(Modifier.fillMaxSize().weight(1f).background(Color.Black).drawBehind {
            tick
            val cols = history.toList()
            if (cols.isEmpty()) return@drawBehind
            val cw = size.width / cols.size
            val ch = size.height / SPEC_BANDS
            cols.forEachIndexed { x, col ->
                for (b in 0 until SPEC_BANDS) {
                    val v = col[b]
                    drawRect(Color.hsv(240f - v * 240f, 1f, v), Offset(x * cw, size.height - (b + 1) * ch), Size(cw + 1f, ch + 1f))
                }
            }
        })
    }
}
