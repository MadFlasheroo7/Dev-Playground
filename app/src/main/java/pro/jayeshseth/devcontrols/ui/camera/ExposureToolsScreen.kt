package pro.jayeshseth.devcontrols.ui.camera

import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
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
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import java.util.concurrent.Executors
import kotlin.math.abs

private const val GW = 48
private const val GH = 64

/** Exposure Tools — live luma grid → histogram, zebra (overexposure), focus peaking, grid overlay. */
@Composable
fun ExposureToolsScreen(onBack: () -> Unit) {
    CameraScreenScaffold("Exposure Tools", onBack) { ExposureContent() }
}

@Composable
private fun ExposureContent() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val executor = remember { Executors.newSingleThreadExecutor() }
    val luma = remember { IntArray(GW * GH) }
    val hist = remember { IntArray(64) }
    var tick by remember { mutableStateOf(0) }
    var zebra by remember { mutableStateOf(true) }
    var peaking by remember { mutableStateOf(true) }
    var grid by remember { mutableStateOf(true) }

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
                        val y = proxy.planes[0].buffer
                        val rs = proxy.planes[0].rowStride
                        val w = proxy.width; val h = proxy.height
                        java.util.Arrays.fill(hist, 0)
                        for (gy in 0 until GH) for (gx in 0 until GW) {
                            val sx = gx * w / GW; val sy = gy * h / GH
                            val v = y.get(sy * rs + sx).toInt() and 0xFF
                            luma[gy * GW + gx] = v
                            hist[v shr 2]++
                        }
                        tick++
                        proxy.close()
                    }
                    provider.unbindAll()
                    provider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis)
                }, ContextCompat.getMainExecutor(ctx))
                pv
            }, modifier = Modifier.fillMaxSize())

            Canvas(Modifier.fillMaxSize()) {
                tick
                val cw = size.width / GW; val ch = size.height / GH
                if (zebra || peaking) {
                    for (gy in 0 until GH) for (gx in 0 until GW) {
                        val v = luma[gy * GW + gx]
                        if (zebra && v > 245) drawRect(Color(0x88FFEB3B), Offset(gx * cw, gy * ch), Size(cw, ch))
                        if (peaking && gx > 0 && gy > 0) {
                            val g = abs(v - luma[gy * GW + gx - 1]) + abs(v - luma[(gy - 1) * GW + gx])
                            if (g > 60) drawRect(Color(0xCCFF1744), Offset(gx * cw, gy * ch), Size(cw * 0.5f, ch * 0.5f))
                        }
                    }
                }
                if (grid) {
                    for (k in 1..2) {
                        drawLine(Color(0x55FFFFFF), Offset(size.width * k / 3, 0f), Offset(size.width * k / 3, size.height))
                        drawLine(Color(0x55FFFFFF), Offset(0f, size.height * k / 3), Offset(size.width, size.height * k / 3))
                    }
                }
                // histogram bottom-left
                val hw = size.width * 0.4f; val hh = size.height * 0.14f; val ox = 16f; val oy = size.height - hh - 16f
                drawRect(Color(0x66000000), Offset(ox, oy), Size(hw, hh))
                val max = (hist.maxOrNull() ?: 1).coerceAtLeast(1); val bw = hw / 64
                for (k in 0 until 64) drawRect(Color(0xCCFFFFFF), Offset(ox + k * bw, oy + hh - hist[k].toFloat() / max * hh), Size(bw - 0.4f, hist[k].toFloat() / max * hh))
            }
        }
        Row(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface).padding(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(zebra, { zebra = !zebra }, { Text("Zebra") })
            FilterChip(peaking, { peaking = !peaking }, { Text("Peaking") })
            FilterChip(grid, { grid = !grid }, { Text("Grid") })
        }
    }
}

@Composable private fun Text(t: String) = androidx.compose.material3.Text(t)
