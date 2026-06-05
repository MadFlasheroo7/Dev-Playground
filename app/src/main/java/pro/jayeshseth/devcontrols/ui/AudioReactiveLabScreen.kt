package pro.jayeshseth.devcontrols.ui

import android.Manifest
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import kotlin.coroutines.coroutineContext
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.ln
import kotlin.math.min
import kotlin.math.sin

/**
 * Audio-reactive lab. Captures the **microphone** with [AudioRecord] and runs a small
 * radix-2 FFT to a [BANDS]-band spectrum (auto-gained + EMA-smoothed), drawn as a live
 * equalizer. (We use the mic rather than `Visualizer(0)` because capturing the global
 * output mix is privileged on modern Android — that's the "error -3" you hit.) Play
 * music out loud and the bars react.
 */
private const val BANDS = 40
private const val FFT_SIZE = 1024

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioReactiveLabScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var granted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted = it }
    var bands by remember { mutableStateOf(FloatArray(BANDS)) }
    var error by remember { mutableStateOf("") }

    LaunchedEffect(granted) {
        if (!granted) return@LaunchedEffect
        withContext(Dispatchers.Default) {
            val sampleRate = 44100
            val minBuf = AudioRecord.getMinBufferSize(
                sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT
            )
            val record = runCatching {
                AudioRecord(
                    MediaRecorder.AudioSource.MIC, sampleRate,
                    AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT,
                    maxOf(minBuf, FFT_SIZE * 2)
                )
            }.getOrElse { error = it.message ?: "AudioRecord init failed"; return@withContext }

            if (record.state != AudioRecord.STATE_INITIALIZED) {
                error = "AudioRecord not initialized"; record.release(); return@withContext
            }

            val window = FloatArray(FFT_SIZE) { 0.5f - 0.5f * cos(2.0 * Math.PI * it / (FFT_SIZE - 1)).toFloat() }
            val buf = ShortArray(FFT_SIZE)
            val re = FloatArray(FFT_SIZE)
            val im = FloatArray(FFT_SIZE)
            var smoothed = FloatArray(BANDS)

            runCatching {
                record.startRecording()
                while (coroutineContext.isActive) {
                    var off = 0
                    while (off < FFT_SIZE) {
                        val n = record.read(buf, off, FFT_SIZE - off)
                        if (n <= 0) break
                        off += n
                    }
                    for (i in 0 until FFT_SIZE) { re[i] = (buf[i] / 32768f) * window[i]; im[i] = 0f }
                    fft(re, im)

                    val half = FFT_SIZE / 2
                    val raw = FloatArray(BANDS)
                    for (b in 0 until BANDS) {
                        val start = 1 + b * (half - 1) / BANDS
                        val end = 1 + (b + 1) * (half - 1) / BANDS
                        var sum = 0f; var c = 0; var k = start
                        while (k < end) { sum += hypot(re[k], im[k]); c++; k++ }
                        raw[b] = if (c > 0) ln(1f + sum / c) else 0f
                    }
                    val mx = raw.maxOrNull() ?: 0f
                    val norm = if (mx > 0.05f) FloatArray(BANDS) { raw[it] / mx } else FloatArray(BANDS)
                    smoothed = FloatArray(BANDS) { smoothed[it] * 0.7f + norm[it] * 0.3f }
                    bands = smoothed
                }
            }.also {
                runCatching { record.stop() }
                runCatching { record.release() }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Audio Reactive") },
                navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }
            )
        },
        modifier = Modifier.fillMaxSize()
    ) { inner ->
        Column(Modifier.padding(inner).fillMaxSize().padding(16.dp)) {
            if (!granted) {
                Text("This visualizer listens through the microphone and runs its own FFT.")
                Button(onClick = { launcher.launch(Manifest.permission.RECORD_AUDIO) }) { Text("Grant microphone") }
                return@Column
            }
            if (error.isNotEmpty()) Text("Error: $error", color = MaterialTheme.colorScheme.error)
            Text(
                "Play music out loud — the bars are the live FFT of what the mic hears.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Box(
                Modifier.fillMaxWidth().height(360.dp).padding(top = 16.dp).drawBehind {
                    val current = bands
                    val n = current.size
                    val gap = 3f
                    val barW = (size.width - gap * (n - 1)) / n
                    for (i in 0 until n) {
                        val h = current[i] * size.height
                        val x = i * (barW + gap)
                        drawRect(
                            brush = Brush.verticalGradient(
                                listOf(Color(0xFF18FFFF), Color(0xFF7F00FF)),
                                startY = size.height - h, endY = size.height
                            ),
                            topLeft = Offset(x, size.height - h),
                            size = Size(barW, h)
                        )
                    }
                }
            )
        }
    }
}

/** In-place iterative radix-2 Cooley-Tukey FFT (FFT_SIZE must be a power of two). */
private fun fft(re: FloatArray, im: FloatArray) {
    val n = re.size
    var j = 0
    for (i in 1 until n) {
        var bit = n shr 1
        while (j and bit != 0) { j = j xor bit; bit = bit shr 1 }
        j = j or bit
        if (i < j) {
            val tr = re[i]; re[i] = re[j]; re[j] = tr
            val ti = im[i]; im[i] = im[j]; im[j] = ti
        }
    }
    var len = 2
    while (len <= n) {
        val ang = -2.0 * Math.PI / len
        val wr = cos(ang).toFloat(); val wi = sin(ang).toFloat()
        var i = 0
        while (i < n) {
            var curR = 1f; var curI = 0f
            for (k in 0 until len / 2) {
                val aR = re[i + k]; val aI = im[i + k]
                val bR = re[i + k + len / 2]; val bI = im[i + k + len / 2]
                val tR = bR * curR - bI * curI; val tI = bR * curI + bI * curR
                re[i + k] = aR + tR; im[i + k] = aI + tI
                re[i + k + len / 2] = aR - tR; im[i + k + len / 2] = aI - tI
                val nR = curR * wr - curI * wi; curI = curR * wi + curI * wr; curR = nR
            }
            i += len
        }
        len = len shl 1
    }
}
