package pro.jayeshseth.devcontrols.ui

import android.Manifest
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext

/** Oscilloscope — live mic waveform (time domain) drawn as a scrolling trace. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OscilloscopeLabScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var granted by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED)
    }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted = it }
    var wave by remember { mutableStateOf(FloatArray(0)) }

    LaunchedEffect(granted) {
        if (!granted) return@LaunchedEffect
        withContext(Dispatchers.Default) {
            val sr = 44100
            val frame = 1024
            val minBuf = AudioRecord.getMinBufferSize(sr, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
            val rec = runCatching {
                AudioRecord(MediaRecorder.AudioSource.MIC, sr, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, maxOf(minBuf, frame * 2))
            }.getOrNull() ?: return@withContext
            if (rec.state != AudioRecord.STATE_INITIALIZED) { rec.release(); return@withContext }
            val buf = ShortArray(frame)
            runCatching {
                rec.startRecording()
                while (isActive) {
                    var off = 0
                    while (off < frame) { val r = rec.read(buf, off, frame - off); if (r <= 0) break; off += r }
                    wave = FloatArray(frame) { buf[it] / 32768f }
                }
            }.also { runCatching { rec.stop() }; runCatching { rec.release() } }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Oscilloscope") },
                navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }
            )
        },
        modifier = Modifier.fillMaxSize()
    ) { inner ->
        Column(Modifier.padding(inner).fillMaxSize().padding(12.dp)) {
            if (!granted) {
                Text("Microphone permission needed to scope the input.")
                Button(onClick = { launcher.launch(Manifest.permission.RECORD_AUDIO) }) { Text("Grant microphone") }
                return@Column
            }
            Box(
                Modifier.fillMaxSize().background(Color(0xFF03100A)).drawBehind {
                    val w = wave
                    // grid baseline
                    drawLine(Color(0xFF1B5E20), Offset(0f, size.height / 2f), Offset(size.width, size.height / 2f), strokeWidth = 1f)
                    if (w.size > 1) {
                        val path = Path()
                        for (i in w.indices) {
                            val x = i / (w.size - 1f) * size.width
                            val y = size.height / 2f - w[i] * size.height * 0.45f
                            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                        }
                        drawPath(path, Color(0xFF69F0AE), style = Stroke(width = 2.5f, cap = StrokeCap.Round))
                    }
                }
            )
        }
    }
}
