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
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin

/** Marching squares — iso-contours of a field of moving metaball sources. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarchingSquaresLabScreen(onBack: () -> Unit) {
    var t by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(Unit) { var s = 0L; while (true) { withFrameNanos { if (s == 0L) s = it; t = (it - s) / 1e9f } } }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Marching Squares") }, navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }) },
        modifier = Modifier.fillMaxSize()
    ) { inner ->
        Column(Modifier.padding(inner).fillMaxSize()) {
            Box(Modifier.fillMaxSize().background(Color(0xFF06060C)).drawBehind {
                val cell = 26f
                val cols = (size.width / cell).toInt() + 1
                val rows = (size.height / cell).toInt() + 1
                val srcs = Array(4) { i ->
                    Offset(
                        size.width * (0.5f + 0.35f * cos(t * (0.5f + i * 0.2f) + i)),
                        size.height * (0.5f + 0.35f * sin(t * (0.4f + i * 0.17f) + i * 1.3f))
                    )
                }
                fun field(x: Float, y: Float): Float {
                    var v = 0f
                    for (s in srcs) { val dx = x - s.x; val dy = y - s.y; v += 9000f / (dx * dx + dy * dy + 1f) }
                    return v
                }
                val iso = 1.2f
                fun interp(a: Float, b: Float) = ((iso - a) / (b - a)).coerceIn(0f, 1f)
                for (gy in 0 until rows) for (gx in 0 until cols) {
                    val x0 = gx * cell; val y0 = gy * cell; val x1 = x0 + cell; val y1 = y0 + cell
                    val v0 = field(x0, y0); val v1 = field(x1, y0); val v2 = field(x1, y1); val v3 = field(x0, y1)
                    var idx = 0
                    if (v0 > iso) idx = idx or 1; if (v1 > iso) idx = idx or 2
                    if (v2 > iso) idx = idx or 4; if (v3 > iso) idx = idx or 8
                    val top = Offset(x0 + cell * interp(v0, v1), y0)
                    val right = Offset(x1, y0 + cell * interp(v1, v2))
                    val bottom = Offset(x0 + cell * interp(v3, v2), y1)
                    val left = Offset(x0, y0 + cell * interp(v0, v3))
                    val col = Color(0xFF18FFC2)
                    fun seg(a: Offset, b: Offset) = drawLine(col, a, b, strokeWidth = 2.5f)
                    when (idx) {
                        1, 14 -> seg(left, top); 2, 13 -> seg(top, right); 3, 12 -> seg(left, right)
                        4, 11 -> seg(right, bottom); 5 -> { seg(left, top); seg(right, bottom) }
                        6, 9 -> seg(top, bottom); 7, 8 -> seg(left, bottom)
                        10 -> { seg(left, bottom); seg(top, right) }
                    }
                }
            })
        }
    }
}
