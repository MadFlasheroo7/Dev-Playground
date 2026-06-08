package pro.jayeshseth.devcontrols.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.sin
import kotlin.math.sqrt

/** 3D function plotter — z = f(x,y) surface mesh, rotating; pick a function. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun FunctionPlotterLabScreen(onBack: () -> Unit) {
    val grid = 30
    var fn by remember { mutableIntStateOf(0) }
    var ay by remember { mutableFloatStateOf(0.5f) }
    LaunchedEffect(Unit) { while (true) { withFrameNanos { ay += 0.007f } } }
    val names = listOf("ripple", "saddle", "gaussian", "egg")
    fun f(x: Float, y: Float): Float {
        val r = sqrt(x * x + y * y)
        return when (fn) {
            0 -> sin(r * 8f) / (1f + r * 4f)
            1 -> (x * x - y * y) * 0.8f
            2 -> exp(-(x * x + y * y) * 6f)
            else -> sin(x * 6f) * cos(y * 6f) * 0.4f
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Function Plotter") }, navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }) },
        modifier = Modifier.fillMaxSize()
    ) { inner ->
        Column(Modifier.padding(inner).fillMaxSize()) {
            FlowRow(Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                names.forEachIndexed { i, n -> FilterChip(fn == i, { fn = i }, { Text(n) }) }
            }
            Box(Modifier.fillMaxSize().background(Color(0xFF06060E)).drawBehind {
                val cx = size.width / 2f; val cy = size.height / 2f
                val scale = minOf(cx, cy) * 0.9f
                fun proj(i: Int, j: Int): Offset {
                    val x0 = i / (grid - 1f) - 0.5f; val z0 = j / (grid - 1f) - 0.5f
                    val y0 = -f(x0 * 2f, z0 * 2f) * 0.5f
                    val (x, y, z) = rotate(x0, y0, z0, 1.0f, ay)
                    val p = 2.4f / (2.4f + z)
                    return Offset(cx + x * scale * p, cy + y * scale * p)
                }
                for (i in 0 until grid) for (j in 0 until grid) {
                    val zz = f((i / (grid - 1f) - 0.5f) * 2f, (j / (grid - 1f) - 0.5f) * 2f)
                    val c = Color.hsv((200f + zz * 200f).mod(360f), 0.7f, 0.95f)
                    if (i < grid - 1) drawLine(c, proj(i, j), proj(i + 1, j), 1.3f)
                    if (j < grid - 1) drawLine(c, proj(i, j), proj(i, j + 1), 1.3f)
                }
            })
        }
    }
}
