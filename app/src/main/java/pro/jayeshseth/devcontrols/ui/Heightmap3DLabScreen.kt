package pro.jayeshseth.devcontrols.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import kotlin.math.sin

/** Heightmap 3D — an animated noise surface rendered as a rotating wireframe mesh. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Heightmap3DLabScreen(onBack: () -> Unit) {
    val grid = 24
    var ay by remember { mutableFloatStateOf(0.6f) }
    var t by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(Unit) { while (true) { withFrameNanos { ay += 0.006f; t += 0.02f } } }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Heightmap 3D") }, navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }) },
        modifier = Modifier.fillMaxSize()
    ) { inner ->
        Column(Modifier.padding(inner).fillMaxSize()) {
            Box(Modifier.fillMaxSize().background(Color(0xFF05070E)).drawBehind {
                val cx = size.width / 2f; val cy = size.height / 2f
                val scale = minOf(cx, cy) * 0.85f
                fun h(i: Int, j: Int): Float {
                    val u = i / (grid - 1f) - 0.5f; val v = j / (grid - 1f) - 0.5f
                    return (sin(u * 6 + t) * 0.5f + sin(v * 5 - t * 0.7f) * 0.5f + sin((u + v) * 8 + t) * 0.25f) * 0.18f
                }
                fun proj(i: Int, j: Int): Offset {
                    val x0 = i / (grid - 1f) - 0.5f; val z0 = j / (grid - 1f) - 0.5f; val y0 = -h(i, j)
                    val (x, y, z) = rotate(x0, y0, z0, 0.9f, ay)
                    val p = 2.2f / (2.2f + z)
                    return Offset(cx + x * scale * p, cy + y * scale * p)
                }
                for (i in 0 until grid) for (j in 0 until grid) {
                    val c = Color.hsv(200f - (h(i, j) + 0.4f) * 120f, 0.7f, 0.9f)
                    if (i < grid - 1) drawLine(c, proj(i, j), proj(i + 1, j), 1.4f)
                    if (j < grid - 1) drawLine(c, proj(i, j), proj(i, j + 1), 1.4f)
                }
            })
        }
    }
}
