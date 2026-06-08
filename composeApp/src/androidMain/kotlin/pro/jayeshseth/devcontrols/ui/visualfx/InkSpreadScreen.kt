package pro.jayeshseth.devcontrols.ui.visualfx

import android.graphics.Bitmap
import androidx.compose.foundation.gestures.detectTapGestures
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
import kotlin.math.min

private const val IW = 110
private const val IH = 200

/** Ink spread — tap to drop dye that blooms and diffuses across the surface. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InkSpreadScreen(onBack: () -> Unit) {
    val r = remember { FloatArray(IW * IH) }
    val g = remember { FloatArray(IW * IH) }
    val b = remember { FloatArray(IW * IH) }
    val r2 = remember { FloatArray(IW * IH) }
    val g2 = remember { FloatArray(IW * IH) }
    val b2 = remember { FloatArray(IW * IH) }
    val bitmap = remember { Bitmap.createBitmap(IW, IH, Bitmap.Config.ARGB_8888) }
    val pixels = remember { IntArray(IW * IH) }
    var tick by remember { mutableIntStateOf(0) }
    var colorIdx by remember { mutableIntStateOf(0) }
    val palette = remember { listOf(Triple(0.1f, 0.4f, 1f), Triple(1f, 0.2f, 0.4f), Triple(0.2f, 0.9f, 0.5f), Triple(1f, 0.8f, 0.1f), Triple(0.7f, 0.2f, 1f)) }

    fun inject(cx: Int, cy: Int) {
        val (cr, cg, cb) = palette[colorIdx]
        for (dy in -5..5) for (dx in -5..5) {
            val x = cx + dx; val y = cy + dy
            if (x in 0 until IW && y in 0 until IH && dx * dx + dy * dy <= 25) {
                val i = y * IW + x; r[i] = cr; g[i] = cg; b[i] = cb
            }
        }
        colorIdx = (colorIdx + 1) % palette.size
    }

    LaunchedEffect(Unit) {
        while (true) {
            withFrameNanos {
                for (y in 1 until IH - 1) for (x in 1 until IW - 1) {
                    val i = y * IW + x
                    fun diff(a: FloatArray) = (a[i] * 0.6f + (a[i - 1] + a[i + 1] + a[i - IW] + a[i + IW]) * 0.1f) * 0.997f
                    r2[i] = diff(r); g2[i] = diff(g); b2[i] = diff(b)
                }
                System.arraycopy(r2, 0, r, 0, r.size); System.arraycopy(g2, 0, g, 0, g.size); System.arraycopy(b2, 0, b, 0, b.size)
                for (i in pixels.indices) {
                    val rr = (r[i] * 255).toInt().coerceIn(0, 255); val gg = (g[i] * 255).toInt().coerceIn(0, 255); val bb = (b[i] * 255).toInt().coerceIn(0, 255)
                    pixels[i] = (0xFF shl 24) or (rr shl 16) or (gg shl 8) or bb
                }
                bitmap.setPixels(pixels, 0, IW, 0, 0, IW, IH)
                tick++
            }
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Ink Spread") }, navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }) },
        modifier = Modifier.fillMaxSize()
    ) { inner ->
        Column(Modifier.padding(inner).fillMaxSize()) {
            Text("Tap to drop ink.", Modifier.padding(8.dp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Box(Modifier.fillMaxSize()
                .pointerInput(Unit) { detectTapGestures { o -> inject((o.x / size.width * IW).toInt().coerceIn(0, IW - 1), (o.y / size.height * IH).toInt().coerceIn(0, IH - 1)) } }
                .drawBehind {
                    tick
                    drawIntoCanvas { c -> c.nativeCanvas.drawBitmap(bitmap, null, android.graphics.Rect(0, 0, size.width.toInt(), size.height.toInt()), null) }
                })
        }
    }
}
