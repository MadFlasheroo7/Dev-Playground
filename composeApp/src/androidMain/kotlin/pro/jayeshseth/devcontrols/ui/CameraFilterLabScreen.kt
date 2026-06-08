package pro.jayeshseth.devcontrols.ui

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat

/**
 * Camera filter lab. A CameraX preview with a live [RenderEffect] applied to the
 * `PreviewView` itself (it's a normal View, so `setRenderEffect` works API 31+):
 * None / Blur / Grayscale / Invert. Needs the CAMERA permission.
 */
private enum class CamFx { None, Blur, Grayscale, Invert }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CameraFilterLabScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var granted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted = it }
    var fx by remember { mutableStateOf(CamFx.None) }
    val lifecycleOwner = LocalLifecycleOwner.current
    var previewView by remember { mutableStateOf<PreviewView?>(null) }

    // re-apply the render effect whenever fx or the view changes
    val pv = previewView
    if (pv != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        pv.setRenderEffect(effectFor(fx))
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Camera Filter") },
                navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }
            )
        },
        modifier = Modifier.fillMaxSize()
    ) { inner ->
        Column(Modifier.padding(inner).fillMaxSize()) {
            if (!granted) {
                Column(Modifier.padding(16.dp)) {
                    Text("Camera permission needed for the live preview.")
                    Button(onClick = { launcher.launch(Manifest.permission.CAMERA) }) { Text("Grant camera") }
                }
                return@Column
            }
            Box(Modifier.weight(1f).fillMaxWidth()) {
                AndroidView(
                    factory = { ctx ->
                        PreviewView(ctx).also { view ->
                            // RenderEffect only composites onto view-hierarchy content. The default
                            // PreviewView uses a SurfaceView (separate surface, unaffected by
                            // setRenderEffect), so force the TextureView-backed COMPATIBLE mode.
                            view.implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                            previewView = view
                            val future = ProcessCameraProvider.getInstance(ctx)
                            future.addListener({
                                val provider = future.get()
                                val preview = Preview.Builder().build().also { it.surfaceProvider = view.surfaceProvider }
                                runCatching {
                                    provider.unbindAll()
                                    provider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview)
                                }
                            }, ContextCompat.getMainExecutor(ctx))
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
            Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CamFx.entries.forEach { e -> FilterChip(fx == e, { fx = e }, { Text(e.name) }) }
            }
        }
    }
}

private fun effectFor(fx: CamFx): RenderEffect? {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return null
    return when (fx) {
        CamFx.None -> null
        CamFx.Blur -> RenderEffect.createBlurEffect(28f, 28f, Shader.TileMode.CLAMP)
        CamFx.Grayscale -> RenderEffect.createColorFilterEffect(
            android.graphics.ColorMatrixColorFilter(android.graphics.ColorMatrix().apply { setSaturation(0f) })
        )
        CamFx.Invert -> RenderEffect.createColorFilterEffect(
            android.graphics.ColorMatrixColorFilter(
                floatArrayOf(
                    -1f, 0f, 0f, 0f, 255f,
                    0f, -1f, 0f, 0f, 255f,
                    0f, 0f, -1f, 0f, 255f,
                    0f, 0f, 0f, 1f, 0f
                )
            )
        )
    }
}
