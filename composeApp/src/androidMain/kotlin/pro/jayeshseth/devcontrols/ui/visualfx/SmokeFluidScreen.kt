package pro.jayeshseth.devcontrols.ui.visualfx

import android.graphics.Bitmap
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp

private const val NW = 72
private const val NH = 128

/** Interactive smoke — drag to push a semi-Lagrangian dye/velocity field around. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmokeFluidScreen(onBack: () -> Unit) {
    val vx = remember { FloatArray(NW * NH) }
    val vy = remember { FloatArray(NW * NH) }
    val den = remember { FloatArray(NW * NH) }
    val vx2 = remember { FloatArray(NW * NH) }
    val vy2 = remember { FloatArray(NW * NH) }
    val den2 = remember { FloatArray(NW * NH) }
    val bitmap = remember { Bitmap.createBitmap(NW, NH, Bitmap.Config.ARGB_8888) }
    val pixels = remember { IntArray(NW * NH) }
    var tick by remember { mutableIntStateOf(0) }

    fun idx(x: Int, y: Int) = y.coerceIn(0, NH - 1) * NW + x.coerceIn(0, NW - 1)
    fun sample(a: FloatArray, fx: Float, fy: Float): Float {
        val x = fx.coerceIn(0f, NW - 1.001f); val y = fy.coerceIn(0f, NH - 1.001f)
        val x0 = x.toInt(); val y0 = y.toInt(); val tx = x - x0; val ty = y - y0
        val a00 = a[idx(x0, y0)]; val a10 = a[idx(x0 + 1, y0)]; val a01 = a[idx(x0, y0 + 1)]; val a11 = a[idx(x0 + 1, y0 + 1)]
        return (a00 * (1 - tx) + a10 * tx) * (1 - ty) + (a01 * (1 - tx) + a11 * tx) * ty
    }

    LaunchedEffect(Unit) {
        while (true) {
            withFrameNanos {
                // advect velocity + density (semi-Lagrangian)
                for (y in 0 until NH) for (x in 0 until NW) {
                    val i = y * NW + x
                    val px = x - vx[i]; val py = y - vy[i]
                    vx2[i] = sample(vx, px, py) * 0.995f
                    vy2[i] = (sample(vy, px, py) - 0.02f) * 0.995f // slight buoyancy upward
                    den2[i] = sample(den, px, py) * 0.992f
                }
                System.arraycopy(vx2, 0, vx, 0, vx.size); System.arraycopy(vy2, 0, vy, 0, vy.size); System.arraycopy(den2, 0, den, 0, den.size)
                for (i in pixels.indices) {
                    val d = (den[i] * 255).toInt().coerceIn(0, 255)
                    // warm smoke colormap
                    pixels[i] = (0xFF shl 24) or (d shl 16) or ((d * 180 / 255) shl 8) or (d * 90 / 255)
                }
                bitmap.setPixels(pixels, 0, NW, 0, 0, NW, NH)
                tick++
            }
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Smoke") }, navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }) },
        modifier = Modifier.fillMaxSize()
    ) { inner ->
        Column(Modifier.padding(inner).fillMaxSize()) {
            Text("Drag to push the smoke.", Modifier.padding(8.dp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Box(Modifier.fillMaxSize()
                .pointerInput(Unit) {
                    detectDragGestures { change, drag ->
                        val gx = (change.position.x / size.width * NW).toInt().coerceIn(1, NW - 2)
                        val gy = (change.position.y / size.height * NH).toInt().coerceIn(1, NH - 2)
                        val fvx = (drag.x / size.width * NW) * 0.5f
                        val fvy = (drag.y / size.height * NH) * 0.5f
                        for (dy in -2..2) for (dx in -2..2) {
                            val i = (gy + dy).coerceIn(0, NH - 1) * NW + (gx + dx).coerceIn(0, NW - 1)
                            den[i] = 1f; vx[i] += fvx; vy[i] += fvy
                        }
                    }
                }
                .drawBehind {
                    tick
                    drawIntoCanvas { c -> c.nativeCanvas.drawBitmap(bitmap, null, android.graphics.Rect(0, 0, size.width.toInt(), size.height.toInt()), null) }
                })
        }
    }
}
