package pro.jayeshseth.devcontrols.ui.camera

import android.content.ContentValues
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CaptureRequest
import android.os.Build
import android.provider.MediaStore
import android.widget.Toast
import androidx.camera.camera2.interop.Camera2CameraControl
import androidx.camera.camera2.interop.Camera2CameraInfo
import androidx.camera.camera2.interop.CaptureRequestOptions
import androidx.camera.camera2.interop.ExperimentalCamera2Interop
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import java.util.concurrent.Executors

/** Pro Mode — manual ISO/shutter/focus/EV/zoom via Camera2Interop, live luma histogram, JPEG capture. */
@Composable
fun ProModeScreen(onBack: () -> Unit) {
    CameraScreenScaffold("Pro Mode", onBack) { ProModeContent() }
}

@OptIn(ExperimentalCamera2Interop::class)
@Composable
private fun ProModeContent() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val executor = remember { Executors.newSingleThreadExecutor() }
    val imageCapture = remember { ImageCapture.Builder().setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY).build() }
    var camera by remember { mutableStateOf<Camera?>(null) }
    val histogram = remember { IntArray(64) }
    var histTick by remember { mutableStateOf(0) }

    // manual state (normalized 0..1 sliders mapped to device ranges)
    var manual by remember { mutableStateOf(false) }
    var isoN by remember { mutableStateOf(0.3f) }
    var shutterN by remember { mutableStateOf(0.4f) }
    var focusManual by remember { mutableStateOf(false) }
    var focusN by remember { mutableStateOf(0.5f) }
    var evN by remember { mutableStateOf(0.5f) }
    var zoom by remember { mutableStateOf(1f) }

    fun applyControls() {
        val cam = camera ?: return
        val info = Camera2CameraInfo.from(cam.cameraInfo)
        val isoR = info.getCameraCharacteristic(CameraCharacteristics.SENSOR_INFO_SENSITIVITY_RANGE)
        val expR = info.getCameraCharacteristic(CameraCharacteristics.SENSOR_INFO_EXPOSURE_TIME_RANGE)
        val focusMin = info.getCameraCharacteristic(CameraCharacteristics.LENS_INFO_MINIMUM_FOCUS_DISTANCE) ?: 0f
        val b = CaptureRequestOptions.Builder()
        if (manual && isoR != null && expR != null) {
            b.setCaptureRequestOption(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_OFF)
            b.setCaptureRequestOption(CaptureRequest.SENSOR_SENSITIVITY, (isoR.lower + isoN * (isoR.upper - isoR.lower)).toInt())
            b.setCaptureRequestOption(CaptureRequest.SENSOR_EXPOSURE_TIME, (expR.lower + shutterN * (expR.upper - expR.lower)).toLong())
        } else {
            b.setCaptureRequestOption(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON)
        }
        if (focusManual) {
            b.setCaptureRequestOption(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_OFF)
            b.setCaptureRequestOption(CaptureRequest.LENS_FOCUS_DISTANCE, focusN * focusMin)
        }
        Camera2CameraControl.from(cam.cameraControl).setCaptureRequestOptions(b.build())
        cam.cameraControl.setExposureCompensationIndex(((evN - 0.5f) * 2 * (cam.cameraInfo.exposureState.exposureCompensationRange.upper)).toInt())
        cam.cameraControl.setZoomRatio(zoom)
    }

    fun capture() {
        val name = "PRO_${System.currentTimeMillis()}.jpg"
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            put(MediaStore.MediaColumns.RELATIVE_PATH, android.os.Environment.DIRECTORY_PICTURES + "/DevControls")
        }
        val opts = ImageCapture.OutputFileOptions.Builder(context.contentResolver, MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values).build()
        imageCapture.takePicture(opts, executor, object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(r: ImageCapture.OutputFileResults) {
                ContextCompat.getMainExecutor(context).execute { Toast.makeText(context, "Saved $name", Toast.LENGTH_SHORT).show() }
            }
            override fun onError(e: ImageCaptureException) {
                ContextCompat.getMainExecutor(context).execute { Toast.makeText(context, "Capture failed: ${e.message}", Toast.LENGTH_SHORT).show() }
            }
        })
    }

    Column(Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxWidth().weight(1f)) {
            androidx.compose.ui.viewinterop.AndroidView(factory = { ctx ->
                val pv = PreviewView(ctx).apply { implementationMode = PreviewView.ImplementationMode.COMPATIBLE }
                val future = ProcessCameraProvider.getInstance(ctx)
                future.addListener({
                    val provider = future.get()
                    val preview = Preview.Builder().build().also { it.surfaceProvider = pv.surfaceProvider }
                    val analysis = ImageAnalysis.Builder().setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).build()
                    analysis.setAnalyzer(executor) { proxy ->
                        val y = proxy.planes[0].buffer
                        val h = IntArray(64)
                        val step = (y.remaining() / 4096).coerceAtLeast(1)
                        var i = 0
                        while (i < y.remaining()) { val v = y.get(i).toInt() and 0xFF; h[v shr 2]++; i += step }
                        System.arraycopy(h, 0, histogram, 0, 64); histTick++
                        proxy.close()
                    }
                    provider.unbindAll()
                    camera = provider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, imageCapture, analysis)
                    applyControls()
                }, ContextCompat.getMainExecutor(ctx))
                pv
            }, modifier = Modifier.fillMaxSize())

            // histogram overlay
            Canvas(Modifier.fillMaxWidth().height(80.dp).padding(8.dp)) {
                histTick
                val max = (histogram.maxOrNull() ?: 1).coerceAtLeast(1)
                val bw = size.width / 64
                drawRect(Color(0x66000000), size = size)
                for (k in 0 until 64) {
                    val bh = histogram[k].toFloat() / max * size.height
                    drawRect(Color(0xCCFFFFFF), Offset(k * bw, size.height - bh), Size(bw - 0.5f, bh))
                }
            }
        }
        Column(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface).padding(12.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(manual, { manual = !manual; applyControls() }, { Text(if (manual) "Manual" else "Auto exposure") })
                FilterChip(focusManual, { focusManual = !focusManual; applyControls() }, { Text("Manual focus") })
            }
            if (manual) {
                Labeled("ISO") { Slider(isoN, { isoN = it; applyControls() }) }
                Labeled("Shutter") { Slider(shutterN, { shutterN = it; applyControls() }) }
            } else {
                Labeled("EV") { Slider(evN, { evN = it; applyControls() }) }
            }
            if (focusManual) Labeled("Focus") { Slider(focusN, { focusN = it; applyControls() }) }
            Labeled("Zoom ${"%.1f".format(zoom)}x") { Slider(zoom, { zoom = it; applyControls() }, valueRange = 1f..8f) }
            Button(onClick = { capture() }, modifier = Modifier.fillMaxWidth()) { Text("Capture") }
        }
    }
}

@Composable
private fun Labeled(label: String, content: @Composable () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
        Text(label, Modifier.padding(end = 8.dp).height(24.dp), style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace)
        Box(Modifier.weight(1f)) { content() }
    }
}
