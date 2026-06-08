package pro.jayeshseth.devcontrols.ui.camera

import android.content.ContentValues
import android.provider.MediaStore
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.core.ImageAnalysis
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
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
import java.util.concurrent.Executors
import kotlin.math.abs

/** Motion cam — frame-differencing motion meter; when armed, snapshots on motion (with cooldown). */
@Composable
fun MotionCamScreen(onBack: () -> Unit) {
    CameraScreenScaffold("Motion Cam", onBack) { MotionContent() }
}

@Composable
private fun MotionContent() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val executor = remember { Executors.newSingleThreadExecutor() }
    val imageCapture = remember { ImageCapture.Builder().build() }
    val prev = remember { IntArray(32 * 32) }
    var motion by remember { mutableFloatStateOf(0f) }
    var armed by remember { mutableStateOf(false) }
    var sensitivity by remember { mutableFloatStateOf(0.3f) }
    var lastShot by remember { mutableLongStateOf(0L) }
    var shots by remember { androidx.compose.runtime.mutableIntStateOf(0) }

    fun snapshot() {
        val cv = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, "MOTION_${System.currentTimeMillis()}.jpg")
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            put(MediaStore.MediaColumns.RELATIVE_PATH, android.os.Environment.DIRECTORY_PICTURES + "/DevControls")
        }
        val opts = ImageCapture.OutputFileOptions.Builder(context.contentResolver, MediaStore.Images.Media.EXTERNAL_CONTENT_URI, cv).build()
        imageCapture.takePicture(opts, executor, object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(r: ImageCapture.OutputFileResults) { shots++ }
            override fun onError(e: ImageCaptureException) {}
        })
    }

    Column(Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxWidth().weight(1f)) {
            AndroidView(factory = { ctx ->
                val pv = PreviewView(ctx).apply { implementationMode = PreviewView.ImplementationMode.COMPATIBLE }
                val future = ProcessCameraProvider.getInstance(ctx)
                future.addListener({
                    val provider = future.get()
                    val preview = Preview.Builder().build().also { it.surfaceProvider = pv.surfaceProvider }
                    val analysis = ImageAnalysis.Builder().setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).build()
                    analysis.setAnalyzer(executor) { proxy ->
                        val y = proxy.planes[0].buffer; val rs = proxy.planes[0].rowStride
                        val w = proxy.width; val h = proxy.height
                        var diff = 0L
                        for (gy in 0 until 32) for (gx in 0 until 32) {
                            val v = y.get((gy * h / 32) * rs + (gx * w / 32)).toInt() and 0xFF
                            val idx = gy * 32 + gx
                            diff += abs(v - prev[idx]); prev[idx] = v
                        }
                        val norm = (diff / (32f * 32f) / 255f)
                        motion = norm
                        if (armed && norm > (0.02f + sensitivity * 0.2f)) {
                            val now = System.currentTimeMillis()
                            if (now - lastShot > 2000) { lastShot = now; ContextCompat.getMainExecutor(context).execute { snapshot() } }
                        }
                        proxy.close()
                    }
                    provider.unbindAll()
                    provider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, imageCapture, analysis)
                }, ContextCompat.getMainExecutor(ctx))
                pv
            }, modifier = Modifier.fillMaxSize())
        }
        Column(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface).padding(12.dp)) {
            Row(Modifier.fillMaxWidth()) {
                FilterChip(armed, { armed = !armed }, { Text(if (armed) "Armed · $shots shots" else "Arm") })
            }
            Text("Motion", style = MaterialTheme.typography.bodySmall)
            Box(Modifier.fillMaxWidth().height(12.dp).background(Color(0x33888888))) {
                Box(Modifier.fillMaxWidth(motion.coerceIn(0f, 1f) * 4f).height(12.dp).background(if (motion > 0.02f + sensitivity * 0.2f) Color(0xFFFF5252) else Color(0xFF4CAF50)))
            }
            Text("Sensitivity", style = MaterialTheme.typography.bodySmall)
            Slider(sensitivity, { sensitivity = it })
        }
    }
}
