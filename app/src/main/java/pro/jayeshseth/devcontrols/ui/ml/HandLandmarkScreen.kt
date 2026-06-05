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
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarker
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarkerResult
import pro.jayeshseth.devcontrols.ui.PermGate
import java.util.concurrent.Executors

// Standard 21-point hand skeleton connections.
private val HAND_CONNECTIONS = listOf(
    0 to 1, 1 to 2, 2 to 3, 3 to 4,            // thumb
    0 to 5, 5 to 6, 6 to 7, 7 to 8,            // index
    5 to 9, 9 to 10, 10 to 11, 11 to 12,       // middle
    9 to 13, 13 to 14, 14 to 15, 15 to 16,     // ring
    13 to 17, 17 to 18, 18 to 19, 19 to 20,    // pinky
    0 to 17                                    // palm base
)

/** Hand landmarks — CameraX frames → MediaPipe HandLandmarker → 21-point skeleton overlay. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HandLandmarkScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = { TopAppBar(title = { Text("Hand Landmarks") }, navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }) },
        modifier = Modifier.fillMaxSize()
    ) { inner ->
        Column(Modifier.padding(inner).fillMaxSize()) {
            PermGate(android.Manifest.permission.CAMERA, "Camera access is needed for hand tracking.") {
                HandContent()
            }
        }
    }
}

@androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
@Composable
private fun HandContent() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var result by remember { mutableStateOf<HandLandmarkerResult?>(null) }
    val executor = remember { Executors.newSingleThreadExecutor() }

    val landmarker = remember {
        runCatching {
            HandLandmarker.createFromOptions(
                context,
                HandLandmarker.HandLandmarkerOptions.builder()
                    .setBaseOptions(BaseOptions.builder().setModelAssetPath("hand_landmarker.task").build())
                    .setRunningMode(RunningMode.LIVE_STREAM)
                    .setNumHands(2)
                    .setMinHandDetectionConfidence(0.5f)
                    .setResultListener { r, _ -> result = r }
                    .setErrorListener { /* swallow per-frame errors */ }
                    .build()
            )
        }.getOrNull()
    }

    DisposableEffect(Unit) {
        onDispose { runCatching { landmarker?.close() }; executor.shutdown() }
    }

    Box(Modifier.fillMaxSize()) {
        if (landmarker == null) {
            Text("Failed to load the hand model.", Modifier.padding(16.dp))
            return@Box
        }
        AndroidView(factory = { ctx ->
            val previewView = PreviewView(ctx).apply { implementationMode = PreviewView.ImplementationMode.COMPATIBLE }
            val future = ProcessCameraProvider.getInstance(ctx)
            future.addListener({
                val provider = future.get()
                val preview = Preview.Builder().build().also { it.surfaceProvider = previewView.surfaceProvider }
                val analysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).build()
                analysis.setAnalyzer(executor) { proxy ->
                    runCatching {
                        val bmp = proxy.toBitmap()
                        val m = Matrix().apply { postRotate(proxy.imageInfo.rotationDegrees.toFloat()) }
                        val upright = Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, m, true)
                        landmarker.detectAsync(BitmapImageBuilder(upright).build(), SystemClock.uptimeMillis())
                    }
                    proxy.close()
                }
                provider.unbindAll()
                provider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis)
            }, ContextCompat.getMainExecutor(ctx))
            previewView
        }, modifier = Modifier.fillMaxSize())

        Canvas(Modifier.fillMaxSize()) {
            val res = result ?: return@Canvas
            for (hand in res.landmarks()) {
                for ((a, b) in HAND_CONNECTIONS) {
                    if (a < hand.size && b < hand.size) {
                        drawLine(Color(0xFF00E5FF),
                            Offset(hand[a].x() * size.width, hand[a].y() * size.height),
                            Offset(hand[b].x() * size.width, hand[b].y() * size.height),
                            strokeWidth = 5f)
                    }
                }
                for (lm in hand) drawCircle(Color(0xFFFF4081), 9f, Offset(lm.x() * size.width, lm.y() * size.height))
            }
        }
        Text("Show a hand to the back camera.", Modifier.padding(12.dp), color = Color.White, style = MaterialTheme.typography.bodySmall)
    }
}
