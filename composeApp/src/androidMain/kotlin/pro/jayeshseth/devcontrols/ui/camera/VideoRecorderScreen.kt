package pro.jayeshseth.devcontrols.ui.camera

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.provider.MediaStore
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.MediaStoreOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat

/** Video Recorder — CameraX Recorder → MP4 in MediaStore, with quality, pause/resume, timer. */
@Composable
fun VideoRecorderScreen(onBack: () -> Unit) {
    CameraScreenScaffold("Video Recorder", onBack) { VideoContent() }
}

@SuppressLint("MissingPermission")
@Composable
private fun VideoContent() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var qualityIdx by remember { mutableIntStateOf(1) }
    val qualities = listOf("SD" to Quality.SD, "HD" to Quality.HD, "FHD" to Quality.FHD, "UHD" to Quality.UHD)
    var videoCapture by remember { mutableStateOf<VideoCapture<Recorder>?>(null) }
    var recording by remember { mutableStateOf<Recording?>(null) }
    var recordingActive by remember { mutableStateOf(false) }
    var paused by remember { mutableStateOf(false) }
    var elapsed by remember { mutableStateOf(0L) }

    fun bind(provider: ProcessCameraProvider, pv: PreviewView) {
        val preview = Preview.Builder().build().also { it.surfaceProvider = pv.surfaceProvider }
        val recorder = Recorder.Builder().setQualitySelector(QualitySelector.from(qualities[qualityIdx].second)).build()
        val vc = VideoCapture.withOutput(recorder)
        videoCapture = vc
        provider.unbindAll()
        provider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, vc)
    }

    fun toggleRecord() {
        val vc = videoCapture ?: return
        if (recordingActive) { recording?.stop(); recording = null; recordingActive = false; paused = false; return }
        val name = "VID_${System.currentTimeMillis()}.mp4"
        val cv = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
            put(MediaStore.MediaColumns.RELATIVE_PATH, android.os.Environment.DIRECTORY_MOVIES + "/DevControls")
        }
        val opts = MediaStoreOutputOptions.Builder(context.contentResolver, MediaStore.Video.Media.EXTERNAL_CONTENT_URI).setContentValues(cv).build()
        val hasAudio = ContextCompat.checkSelfPermission(context, android.Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        var pending = vc.output.prepareRecording(context, opts)
        if (hasAudio) pending = pending.withAudioEnabled()
        recording = pending.start(ContextCompat.getMainExecutor(context)) { event ->
            when (event) {
                is VideoRecordEvent.Start -> recordingActive = true
                is VideoRecordEvent.Status -> elapsed = event.recordingStats.recordedDurationNanos / 1_000_000_000
                is VideoRecordEvent.Finalize -> { recordingActive = false; paused = false; elapsed = 0 }
            }
        }
    }

    Column(Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxWidth().weight(1f)) {
            AndroidView(factory = { ctx ->
                val pv = PreviewView(ctx).apply { implementationMode = PreviewView.ImplementationMode.COMPATIBLE }
                val future = ProcessCameraProvider.getInstance(ctx)
                future.addListener({ bind(future.get(), pv) }, ContextCompat.getMainExecutor(ctx))
                pv
            }, modifier = Modifier.fillMaxSize())
            if (recordingActive) Text("● ${elapsed}s", Modifier.padding(12.dp), color = Color(0xFFFF5252), style = MaterialTheme.typography.titleMedium)
        }
        Column(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface).padding(12.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                qualities.forEachIndexed { i, (label, _) ->
                    FilterChip(qualityIdx == i, { if (!recordingActive) { qualityIdx = i } }, { Text(label) }, enabled = !recordingActive)
                }
            }
            Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { toggleRecord() }, modifier = Modifier.weight(1f)) { Text(if (recordingActive) "Stop" else "Record") }
                if (recordingActive) OutlinedButton(onClick = {
                    if (paused) { recording?.resume(); paused = false } else { recording?.pause(); paused = true }
                }) { Text(if (paused) "Resume" else "Pause") }
            }
        }
    }
}
