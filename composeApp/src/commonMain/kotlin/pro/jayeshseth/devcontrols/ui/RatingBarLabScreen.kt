package pro.jayeshseth.devcontrols.ui

import pro.jayeshseth.devcontrols.util.fmt
import pro.jayeshseth.devcontrols.util.hex2
import pro.jayeshseth.devcontrols.util.pad
import pro.jayeshseth.devcontrols.util.nowMillis

import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** Star rating — tap or drag across the stars for a partial (per-tenth) rating. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RatingBarLabScreen(onBack: () -> Unit) {
    var rating by remember { mutableFloatStateOf(3.5f) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Rating") },
                navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }
            )
        },
        modifier = Modifier.fillMaxSize()
    ) { inner ->
        Column(Modifier.padding(inner).fillMaxSize().padding(24.dp)) {
            Box(
                Modifier.fillMaxWidth().height(72.dp)
                    .pointerInput(Unit) { detectTapGestures { rating = (it.x / size.width * 5f).coerceIn(0f, 5f) } }
                    .pointerInput(Unit) { detectHorizontalDragGestures { ch, _ -> rating = (ch.position.x / size.width * 5f).coerceIn(0f, 5f); ch.consume() } }
                    .drawBehind {
                        val starW = size.width / 5f
                        for (i in 0 until 5) {
                            val cx = i * starW + starW / 2f
                            val cy = size.height / 2f
                            val rad = size.height * 0.42f
                            val empty = starPath(cx, cy, rad)
                            drawPath(empty, Color(0xFF3A3A44))
                            val fill = (rating - i).coerceIn(0f, 1f)
                            if (fill > 0f) {
                                clipRect(left = i * starW, top = 0f, right = i * starW + starW * fill, bottom = size.height) {
                                    drawPath(empty, Color(0xFFFFD740))
                                }
                            }
                        }
                    }
            )
            Spacer(Modifier.height(16.dp))
            Text("${fmt(rating, 1)} / 5", fontSize = 26.sp, color = MaterialTheme.colorScheme.primary)
        }
    }
}

private fun starPath(cx: Float, cy: Float, r: Float): Path {
    val p = Path()
    for (i in 0 until 10) {
        val rr = if (i % 2 == 0) r else r * 0.45f
        val a = PI / 5 * i - PI / 2
        val x = cx + (rr * cos(a)).toFloat(); val y = cy + (rr * sin(a)).toFloat()
        if (i == 0) p.moveTo(x, y) else p.lineTo(x, y)
    }
    p.close(); return p
}
