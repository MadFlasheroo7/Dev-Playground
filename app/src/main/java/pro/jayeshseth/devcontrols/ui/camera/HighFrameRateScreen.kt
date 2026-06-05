package pro.jayeshseth.devcontrols.ui.camera

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CameraMetadata
import android.hardware.camera2.CaptureRequest
import android.provider.MediaStore
import android.util.Range
import androidx.camera.camera2.interop.Camera2Interop
import androidx.camera.camera2.interop.ExperimentalCamera2Interop
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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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

/** High frame rate — records video with the highest available AE FPS range (slow-mo source). */
@Composable
fun HighFrameRateScreen(onBack: () -> Unit) {
    CameraScreenScaffold("High Frame Rate", onBack) { HfrContent() }
}

private fun maxFps(context: Context): Int {
    val cm = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    return runCatching {
        cm.cameraIdList.filter { cm.getCameraCharacteristics(it).get(CameraCharacteristics.LENS_FACING) == CameraMetadata.LENS_FACING_BACK }
            .flatMap { cm.getCameraCharacteristics(it).get(CameraCharacteristics.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES)?.toList() ?: emptyList() }
            .maxOfOrNull { it.upper } ?: 30
    }.getOrDefault(30)
}

@OptIn(ExperimentalCamera2Interop::class)
@SuppressLint("MissingPermission")
@Composable
private fun HfrContent() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val fps = remember { maxFps(context) }
    var videoCapture by remember { mutableStateOf<VideoCapture<Recorder>?>(null) }
    var recording by remember { mutableStateOf<Recording?>(null) }
    var active by remember { mutableStateOf(false) }
    var elapsed by remember { mutableStateOf(0L) }

    fun toggle() {
        val vc = videoCapture ?: return
        if (active) { recording?.stop(); recording = null; active = false; return }
        val cv = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, "HFR_${System.currentTimeMillis()}.mp4")
            put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
            put(MediaStore.MediaColumns.RELATIVE_PATH, android.os.Environment.DIRECTORY_MOVIES + "/DevControls")
        }
        val opts = MediaStoreOutputOptions.Builder(context.contentResolver, MediaStore.Video.Media.EXTERNAL_CONTENT_URI).setContentValues(cv).build()
        recording = vc.output.prepareRecording(context, opts).start(ContextCompat.getMainExecutor(context)) { e ->
            when (e) {
                is VideoRecordEvent.Start -> active = true
                is VideoRecordEvent.Status -> elapsed = e.recordingStats.recordedDurationNanos / 1_000_000_000
                is VideoRecordEvent.Finalize -> { active = false; elapsed = 0 }
            }
        }
    }

    Column(Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxWidth().weight(1f)) {
            AndroidView(factory = { ctx ->
                val pv = PreviewView(ctx).apply { implementationMode = PreviewView.ImplementationMode.COMPATIBLE }
                val future = ProcessCameraProvider.getInstance(ctx)
                future.addListener({
                    val provider = future.get()
                    val previewBuilder = Preview.Builder()
                    val target = if (fps >= 120) 120 else if (fps >= 60) 60 else fps
                    Camera2Interop.Extender(previewBuilder).setCaptureRequestOption(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, Range(target, target))
                    val preview = previewBuilder.build().also { it.surfaceProvider = pv.surfaceProvider }
                    val recorder = Recorder.Builder().setQualitySelector(QualitySelector.from(Quality.HD)).build()
                    val vc = VideoCapture.withOutput(recorder)
                    videoCapture = vc
                    provider.unbindAll()
                    provider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, vc)
                }, ContextCompat.getMainExecutor(ctx))
                pv
            }, modifier = Modifier.fillMaxSize())
            if (active) Text("● ${elapsed}s", Modifier.padding(12.dp), color = Color(0xFFFF5252), style = MaterialTheme.typography.titleMedium)
        }
        Column(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface).padding(12.dp)) {
            Text("Max sensor FPS reported: $fps", style = MaterialTheme.typography.bodyMedium)
            Text("Recording at up to ${if (fps >= 120) 120 else if (fps >= 60) 60 else fps} fps. Play back slowed for slow-mo.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Button(onClick = { toggle() }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) { Text(if (active) "Stop" else "Record") }
        }
    }
}
