package pro.jayeshseth.devcontrols.ui.camera

import android.content.ContentValues
import android.provider.MediaStore
import android.widget.Toast
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.extensions.ExtensionMode
import androidx.camera.extensions.ExtensionsManager
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat

private val EXT_MODES = listOf(
    "None" to ExtensionMode.NONE, "Auto" to ExtensionMode.AUTO, "HDR" to ExtensionMode.HDR,
    "Night" to ExtensionMode.NIGHT, "Bokeh" to ExtensionMode.BOKEH, "Face" to ExtensionMode.FACE_RETOUCH
)

/** CameraX Extensions — toggle HDR/Night/Bokeh/Face-Retouch where the device supports them. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ExtensionsScreen(onBack: () -> Unit) {
    CameraScreenScaffold("Camera Extensions", onBack) { ExtContent() }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ExtContent() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var mode by remember { mutableIntStateOf(0) }
    val available = remember { mutableStateOf(setOf<Int>()) }
    var rebindKey by remember { mutableIntStateOf(0) }
    val imageCapture = remember { ImageCapture.Builder().build() }

    fun capture() {
        val cv = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, "EXT_${System.currentTimeMillis()}.jpg")
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            put(MediaStore.MediaColumns.RELATIVE_PATH, android.os.Environment.DIRECTORY_PICTURES + "/DevControls")
        }
        val opts = ImageCapture.OutputFileOptions.Builder(context.contentResolver, MediaStore.Images.Media.EXTERNAL_CONTENT_URI, cv).build()
        imageCapture.takePicture(opts, ContextCompat.getMainExecutor(context), object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(r: ImageCapture.OutputFileResults) { Toast.makeText(context, "Saved", Toast.LENGTH_SHORT).show() }
            override fun onError(e: ImageCaptureException) { Toast.makeText(context, "Failed: ${e.message}", Toast.LENGTH_SHORT).show() }
        })
    }

    Column(Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxWidth().weight(1f)) {
            AndroidView(factory = { ctx ->
                val pv = PreviewView(ctx).apply { implementationMode = PreviewView.ImplementationMode.COMPATIBLE }
                pv.tag = ctx
                pv
            }, update = { pv ->
                rebindKey
                val future = ProcessCameraProvider.getInstance(context)
                future.addListener({
                    val provider = future.get()
                    val extFuture = ExtensionsManager.getInstanceAsync(context, provider)
                    extFuture.addListener({
                        val em = extFuture.get()
                        val base = CameraSelector.DEFAULT_BACK_CAMERA
                        // record availability
                        available.value = EXT_MODES.indices.filter { i ->
                            EXT_MODES[i].second == ExtensionMode.NONE || runCatching { em.isExtensionAvailable(base, EXT_MODES[i].second) }.getOrDefault(false)
                        }.toSet()
                        val selected = EXT_MODES[mode].second
                        val selector = if (selected == ExtensionMode.NONE) base
                        else runCatching { em.getExtensionEnabledCameraSelector(base, selected) }.getOrDefault(base)
                        val preview = Preview.Builder().build().also { it.surfaceProvider = pv.surfaceProvider }
                        provider.unbindAll()
                        runCatching { provider.bindToLifecycle(lifecycleOwner, selector, preview, imageCapture) }
                    }, ContextCompat.getMainExecutor(context))
                }, ContextCompat.getMainExecutor(context))
            }, modifier = Modifier.fillMaxSize())
        }
        Column(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface).padding(12.dp)) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                EXT_MODES.forEachIndexed { i, (label, _) ->
                    val avail = available.value.contains(i)
                    FilterChip(mode == i, { if (avail) { mode = i; rebindKey++ } }, { Text(if (avail) label else "$label ✕") }, enabled = avail)
                }
            }
            Button(onClick = { capture() }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) { Text("Capture") }
            Text("✕ = not supported on this device.", Modifier.padding(top = 4.dp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
