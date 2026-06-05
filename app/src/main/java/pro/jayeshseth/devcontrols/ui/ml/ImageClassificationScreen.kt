package pro.jayeshseth.devcontrols.ui.ml

import android.graphics.Bitmap
import android.graphics.Matrix
import android.os.SystemClock
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
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
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.imageclassifier.ImageClassifier
import pro.jayeshseth.devcontrols.ui.PermGate
import java.util.concurrent.Executors

private data class Label(val name: String, val score: Float)

/** Image classification — EfficientNet-Lite0 over the camera, live top-3 labels + latency. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageClassificationScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = { TopAppBar(title = { Text("Image Classification") }, navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }) },
        modifier = Modifier.fillMaxSize()
    ) { inner ->
        Column(Modifier.padding(inner).fillMaxSize()) {
            PermGate(android.Manifest.permission.CAMERA, "Camera access is needed for classification.") {
                ClassifyContent()
            }
        }
    }
}

@androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
@Composable
private fun ClassifyContent() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var labels by remember { mutableStateOf<List<Label>>(emptyList()) }
    var latency by remember { mutableStateOf(0L) }
    val executor = remember { Executors.newSingleThreadExecutor() }

    val classifier = remember {
        runCatching {
            ImageClassifier.createFromOptions(
                context,
                ImageClassifier.ImageClassifierOptions.builder()
                    .setBaseOptions(BaseOptions.builder().setModelAssetPath("efficientnet_lite0.tflite").build())
                    .setRunningMode(RunningMode.LIVE_STREAM)
                    .setMaxResults(3)
                    .setResultListener { r, _ ->
                        labels = r.classificationResult().classifications().firstOrNull()?.categories()
                            ?.map { Label(it.categoryName(), it.score()) } ?: emptyList()
                    }
                    .setErrorListener { }
                    .build()
            )
        }.getOrNull()
    }
    DisposableEffect(Unit) { onDispose { runCatching { classifier?.close() }; executor.shutdown() } }

    Column(Modifier.fillMaxSize()) {
        if (classifier == null) { Text("Failed to load the classifier model.", Modifier.padding(16.dp)); return@Column }
        Box(Modifier.fillMaxWidth().weight(1f)) {
            AndroidView(factory = { ctx ->
                val pv = PreviewView(ctx).apply { implementationMode = PreviewView.ImplementationMode.COMPATIBLE }
                val future = ProcessCameraProvider.getInstance(ctx)
                future.addListener({
                    val provider = future.get()
                    val preview = Preview.Builder().build().also { it.surfaceProvider = pv.surfaceProvider }
                    val analysis = ImageAnalysis.Builder().setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).build()
                    analysis.setAnalyzer(executor) { proxy ->
                        runCatching {
                            val t0 = SystemClock.uptimeMillis()
                            val bmp = proxy.toBitmap()
                            val m = Matrix().apply { postRotate(proxy.imageInfo.rotationDegrees.toFloat()) }
                            val up = Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, m, true)
                            classifier.classifyAsync(BitmapImageBuilder(up).build(), SystemClock.uptimeMillis())
                            latency = SystemClock.uptimeMillis() - t0
                        }
                        proxy.close()
                    }
                    provider.unbindAll()
                    provider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis)
                }, ContextCompat.getMainExecutor(ctx))
                pv
            }, modifier = Modifier.fillMaxSize())
        }
        Column(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface).padding(16.dp)) {
            Text("Top predictions  ·  ${latency}ms/frame", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(8.dp))
            if (labels.isEmpty()) Text("Point the camera at an object…", style = MaterialTheme.typography.bodyMedium)
            labels.forEach { l ->
                Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(l.name, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                    Box(Modifier.height(10.dp).weight(1f).clip(RoundedCornerShape(5.dp)).background(MaterialTheme.colorScheme.surfaceVariant)) {
                        Box(Modifier.fillMaxWidth(l.score.coerceIn(0f, 1f)).height(10.dp).background(MaterialTheme.colorScheme.primary))
                    }
                    Text("  ${(l.score * 100).toInt()}%", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}
