package pro.jayeshseth.devcontrols.ui.ml

import android.graphics.Bitmap
import android.graphics.Matrix
import android.os.SystemClock
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarker
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult
import pro.jayeshseth.devcontrols.ui.PermGate
import java.util.concurrent.Executors

private val POSE_CONNECTIONS = listOf(
    11 to 12, 11 to 13, 13 to 15, 12 to 14, 14 to 16,       // arms + shoulders
    11 to 23, 12 to 24, 23 to 24,                           // torso
    23 to 25, 25 to 27, 27 to 29, 29 to 31, 27 to 31,       // left leg
    24 to 26, 26 to 28, 28 to 30, 30 to 32, 28 to 32        // right leg
)

/** Pose landmarks — MediaPipe PoseLandmarker body skeleton over the camera. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PoseLandmarkScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = { TopAppBar(title = { Text("Pose Landmarks") }, navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }) },
        modifier = Modifier.fillMaxSize()
    ) { inner ->
        Column(Modifier.padding(inner).fillMaxSize()) {
            PermGate(android.Manifest.permission.CAMERA, "Camera access is needed for pose tracking.") {
                PoseContent()
            }
        }
    }
}

@androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
@Composable
private fun PoseContent() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var result by remember { mutableStateOf<PoseLandmarkerResult?>(null) }
    val executor = remember { Executors.newSingleThreadExecutor() }

    val landmarker = remember {
        runCatching {
            PoseLandmarker.createFromOptions(
                context,
                PoseLandmarker.PoseLandmarkerOptions.builder()
                    .setBaseOptions(BaseOptions.builder().setModelAssetPath("pose_landmarker_lite.task").build())
                    .setRunningMode(RunningMode.LIVE_STREAM)
                    .setNumPoses(1)
                    .setResultListener { r, _ -> result = r }
                    .setErrorListener { }
                    .build()
            )
        }.getOrNull()
    }
    DisposableEffect(Unit) { onDispose { runCatching { landmarker?.close() }; executor.shutdown() } }

    Box(Modifier.fillMaxSize()) {
        if (landmarker == null) { Text("Failed to load the pose model.", Modifier.padding(16.dp)); return@Box }
        AndroidView(factory = { ctx ->
            val pv = PreviewView(ctx).apply { implementationMode = PreviewView.ImplementationMode.COMPATIBLE }
            val future = ProcessCameraProvider.getInstance(ctx)
            future.addListener({
                val provider = future.get()
                val preview = Preview.Builder().build().also { it.surfaceProvider = pv.surfaceProvider }
                val analysis = ImageAnalysis.Builder().setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).build()
                analysis.setAnalyzer(executor) { proxy ->
                    runCatching {
                        val bmp = proxy.toBitmap()
                        val m = Matrix().apply { postRotate(proxy.imageInfo.rotationDegrees.toFloat()) }
                        val up = Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, m, true)
                        landmarker.detectAsync(BitmapImageBuilder(up).build(), SystemClock.uptimeMillis())
                    }
                    proxy.close()
                }
                provider.unbindAll()
                provider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis)
            }, ContextCompat.getMainExecutor(ctx))
            pv
        }, modifier = Modifier.fillMaxSize())

        Canvas(Modifier.fillMaxSize()) {
            val res = result ?: return@Canvas
            for (pose in res.landmarks()) {
                for ((a, b) in POSE_CONNECTIONS) if (a < pose.size && b < pose.size)
                    drawLine(Color(0xFF76FF03),
                        Offset(pose[a].x() * size.width, pose[a].y() * size.height),
                        Offset(pose[b].x() * size.width, pose[b].y() * size.height), strokeWidth = 6f)
                for (lm in pose) drawCircle(Color(0xFFFFEB3B), 8f, Offset(lm.x() * size.width, lm.y() * size.height))
            }
        }
        Text("Step back so your body is in frame.", Modifier.padding(12.dp), color = Color.White, style = MaterialTheme.typography.bodySmall)
    }
}
