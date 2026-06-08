package pro.jayeshseth.devcontrols.ui

import android.graphics.Bitmap
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
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
import kotlin.math.min

private const val GW = 150
private const val GH = 150

/** Reaction-diffusion (Gray-Scott) on a CPU grid, rendered to a bitmap. Tap to seed. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReactionDiffusionLabScreen(onBack: () -> Unit) {
    val a = remember { FloatArray(GW * GH) { 1f } }
    val b = remember { FloatArray(GW * GH) { 0f } }
    val a2 = remember { FloatArray(GW * GH) }
    val b2 = remember { FloatArray(GW * GH) }
    val bitmap = remember { Bitmap.createBitmap(GW, GH, Bitmap.Config.ARGB_8888) }
    val pixels = remember { IntArray(GW * GH) }
    var tick by remember { mutableIntStateOf(0) }

    fun seed(cx: Int, cy: Int) {
        for (dy in -6..6) for (dx in -6..6) {
            val x = cx + dx; val y = cy + dy
            if (x in 0 until GW && y in 0 until GH) b[y * GW + x] = 1f
        }
    }
    remember { seed(GW / 2, GH / 2); true }

    LaunchedEffect(Unit) {
        val da = 1.0f; val db = 0.5f; val f = 0.055f; val k = 0.062f
        while (true) {
            withFrameNanos {
                repeat(8) {
                    for (y in 1 until GH - 1) for (x in 1 until GW - 1) {
                        val i = y * GW + x
                        val lapA = a[i - 1] + a[i + 1] + a[i - GW] + a[i + GW] +
                            0.05f * (a[i - GW - 1] + a[i - GW + 1] + a[i + GW - 1] + a[i + GW + 1]) * 4f - 3.2f * a[i]
                        val lapB = b[i - 1] + b[i + 1] + b[i - GW] + b[i + GW] +
                            0.05f * (b[i - GW - 1] + b[i - GW + 1] + b[i + GW - 1] + b[i + GW + 1]) * 4f - 3.2f * b[i]
                        val abb = a[i] * b[i] * b[i]
                        a2[i] = (a[i] + da * lapA * 0.18f - abb + f * (1f - a[i])).coerceIn(0f, 1f)
                        b2[i] = (b[i] + db * lapB * 0.18f + abb - (k + f) * b[i]).coerceIn(0f, 1f)
                    }
                    System.arraycopy(a2, 0, a, 0, a.size)
                    System.arraycopy(b2, 0, b, 0, b.size)
                }
                for (i in pixels.indices) {
                    val v = (b[i] * 255f).toInt().coerceIn(0, 255)
                    pixels[i] = (0xFF shl 24) or (v shl 16) or ((v / 2) shl 8) or (255 - v)
                }
                bitmap.setPixels(pixels, 0, GW, 0, 0, GW, GH)
                tick++
            }
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Reaction-Diffusion") }, navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }) },
        modifier = Modifier.fillMaxSize()
    ) { inner ->
        Column(Modifier.padding(inner).fillMaxSize()) {
            androidx.compose.foundation.layout.Box(
                Modifier.fillMaxSize()
                    .pointerInput(Unit) {
                        detectTapGestures { o ->
                            val s = min(size.width, size.height).toFloat()
                            val gx = (o.x / s * GW).toInt(); val gy = (o.y / s * GH).toInt()
                            seed(gx.coerceIn(0, GW - 1), gy.coerceIn(0, GH - 1))
                        }
                    }
                    .drawBehind {
                        tick
                        val s = min(size.width, size.height)
                        drawIntoCanvas { c ->
                            val dst = android.graphics.Rect(0, 0, s.toInt(), s.toInt())
                            c.nativeCanvas.drawBitmap(bitmap, null, dst, null)
                        }
                    }
            ) {}
        }
    }
}
