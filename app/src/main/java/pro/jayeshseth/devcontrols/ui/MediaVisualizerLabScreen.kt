package pro.jayeshseth.devcontrols.ui

import android.media.MediaPlayer
import android.media.audiofx.Visualizer
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlin.math.hypot

/** Media Visualizer — pick an audio file, play it, and render FFT bars via the Visualizer API. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaVisualizerLabScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = { TopAppBar(title = { Text("Media Visualizer") }, navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }) },
        modifier = Modifier.fillMaxSize()
    ) { inner ->
        Column(Modifier.padding(inner).fillMaxSize()) {
            PermGate(android.Manifest.permission.RECORD_AUDIO, "The Visualizer API requires the microphone permission (Android policy), even for file playback.") {
                VizContent()
            }
        }
    }
}

@Composable
private fun VizContent() {
    val context = LocalContext.current
    var player by remember { mutableStateOf<MediaPlayer?>(null) }
    var visualizer by remember { mutableStateOf<Visualizer?>(null) }
    var bars by remember { mutableStateOf(FloatArray(64)) }
    var picked by remember { mutableStateOf<Uri?>(null) }

    val pick = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri -> picked = uri }

    DisposableEffect(picked) {
        if (picked == null) return@DisposableEffect onDispose { }
        val mp = MediaPlayer().apply {
            runCatching { setDataSource(context, picked!!); prepare(); start() }
        }
        player = mp
        val viz = runCatching {
            Visualizer(mp.audioSessionId).apply {
                captureSize = Visualizer.getCaptureSizeRange()[1]
                setDataCaptureListener(object : Visualizer.OnDataCaptureListener {
                    override fun onWaveFormDataCapture(v: Visualizer?, wf: ByteArray?, sr: Int) {}
                    override fun onFftDataCapture(v: Visualizer?, fft: ByteArray?, sr: Int) {
                        if (fft == null) return
                        val n = bars.size
                        val out = FloatArray(n)
                        val per = (fft.size / 2) / n
                        for (b in 0 until n) {
                            var mx = 0f
                            for (k in 0 until per) {
                                val idx = (b * per + k) * 2
                                if (idx + 1 < fft.size) {
                                    val mag = hypot(fft[idx].toFloat(), fft[idx + 1].toFloat())
                                    if (mag > mx) mx = mag
                                }
                            }
                            out[b] = (mx / 80f).coerceIn(0f, 1f)
                        }
                        bars = out
                    }
                }, Visualizer.getMaxCaptureRate() / 2, false, true)
                enabled = true
            }
        }.getOrNull()
        visualizer = viz
        onDispose {
            runCatching { viz?.enabled = false; viz?.release() }
            runCatching { mp.stop() }; runCatching { mp.release() }
        }
    }

    Column(Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxWidth().weight(1f).background(Color(0xFF06060C)).drawBehind {
            val bw = size.width / bars.size
            for (i in bars.indices) {
                val h = bars[i] * size.height
                drawRect(Color.hsv(i * 320f / bars.size, 0.85f, 1f), Offset(i * bw, size.height - h), Size(bw - 1f, h))
            }
        })
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Button(onClick = { pick.launch("audio/*") }) { Text(if (picked == null) "Pick an audio file" else "Pick another") }
            Spacer(Modifier.height(4.dp))
            Text(if (picked != null) "Playing. Bars are the live FFT." else "Choose a track to begin.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
