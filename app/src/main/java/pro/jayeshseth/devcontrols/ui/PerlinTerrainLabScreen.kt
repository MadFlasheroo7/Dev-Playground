package pro.jayeshseth.devcontrols.ui

import android.graphics.Bitmap
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.dp
import kotlin.math.floor

private const val PW = 200
private const val PH = 200

/** Perlin/value-noise terrain — octave-summed heightmap with a terrain colour ramp. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PerlinTerrainLabScreen(onBack: () -> Unit) {
    var seaLevel by remember { mutableFloatStateOf(0.4f) }
    var scaleK by remember { mutableFloatStateOf(4f) }
    val bitmap = remember { Bitmap.createBitmap(PW, PH, Bitmap.Config.ARGB_8888) }
    val pixels = remember { IntArray(PW * PH) }
    val perm = remember { IntArray(512).also { p -> val base = (0 until 256).shuffled(java.util.Random(7)); for (i in 0 until 512) p[i] = base[i and 255] } }

    fun vnoise(x: Float, y: Float): Float {
        val xi = floor(x).toInt(); val yi = floor(y).toInt()
        val xf = x - xi; val yf = y - yi
        fun h(a: Int, b: Int) = perm[(perm[a and 255] + b) and 255] / 255f
        fun sm(t: Float) = t * t * (3 - 2 * t)
        val u = sm(xf); val v = sm(yf)
        val a = h(xi, yi); val b = h(xi + 1, yi); val c = h(xi, yi + 1); val d = h(xi + 1, yi + 1)
        return (a + (b - a) * u) * (1 - v) + (c + (d - c) * u) * v
    }

    LaunchedEffect(seaLevel, scaleK) {
        for (y in 0 until PH) for (x in 0 until PW) {
            var amp = 1f; var freq = scaleK / PW; var sum = 0f; var norm = 0f
            for (o in 0 until 5) { sum += amp * vnoise(x * freq, y * freq); norm += amp; amp *= 0.5f; freq *= 2f }
            val hgt = sum / norm
            pixels[y * PW + x] = terrainColor(hgt, seaLevel)
        }
        bitmap.setPixels(pixels, 0, PW, 0, 0, PW, PH)
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Perlin Terrain") }, navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }) },
        modifier = Modifier.fillMaxSize()
    ) { inner ->
        Column(Modifier.padding(inner).fillMaxSize().padding(16.dp)) {
            Text("Sea level: ${(seaLevel * 100).toInt()}", style = MaterialTheme.typography.bodySmall)
            Slider(value = seaLevel, onValueChange = { seaLevel = it }, valueRange = 0.2f..0.7f)
            Text("Zoom", style = MaterialTheme.typography.bodySmall)
            Slider(value = scaleK, onValueChange = { scaleK = it }, valueRange = 2f..10f)
            Spacer(Modifier.height(12.dp))
            androidx.compose.foundation.layout.Box(Modifier.fillMaxWidth().height(360.dp).drawBehind {
                drawIntoCanvas { c ->
                    val s = minOf(size.width, size.height).toInt()
                    c.nativeCanvas.drawBitmap(bitmap, null, android.graphics.Rect(0, 0, size.width.toInt(), s), null)
                }
            })
        }
    }
}

private fun terrainColor(h: Float, sea: Float): Int = when {
    h < sea * 0.7f -> 0xFF1A3A6B.toInt()
    h < sea -> 0xFF2E6CA6.toInt()
    h < sea + 0.05f -> 0xFFE3D9A0.toInt()
    h < sea + 0.22f -> 0xFF3F8F3A.toInt()
    h < sea + 0.38f -> 0xFF6B5A3A.toInt()
    h < sea + 0.5f -> 0xFF8A8A8A.toInt()
    else -> 0xFFFFFFFF.toInt()
}
