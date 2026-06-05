package pro.jayeshseth.devcontrols.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import kotlin.math.sqrt

/** Cloth — Verlet grid with distance constraints; pinned top row, drag to tear it around. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClothLabScreen(onBack: () -> Unit) {
    val cols = 20; val rows = 14
    val n = cols * rows
    val px = remember { FloatArray(n) }; val py = remember { FloatArray(n) }
    val ox = remember { FloatArray(n) }; val oy = remember { FloatArray(n) }
    val pinned = remember { BooleanArray(n) { it < cols } }
    var size by remember { mutableStateOf(IntSize.Zero) }
    var seeded by remember { mutableIntStateOf(0) }
    var tick by remember { mutableIntStateOf(0) }
    var rest by remember { mutableIntStateOf(28) }

    fun idx(c: Int, r: Int) = r * cols + c

    LaunchedEffect(size) {
        if (size == IntSize.Zero) return@LaunchedEffect
        val sp = (size.width * 0.8f / cols)
        rest = sp.toInt()
        val ox0 = size.width * 0.1f; val oy0 = size.height * 0.08f
        for (r in 0 until rows) for (c in 0 until cols) {
            val i = idx(c, r); px[i] = ox0 + c * sp; py[i] = oy0 + r * sp; ox[i] = px[i]; oy[i] = py[i]
        }
        seeded = 1
        while (true) {
            withFrameNanos {
                // verlet integrate
                for (i in 0 until n) if (!pinned[i]) {
                    val vx = (px[i] - ox[i]) * 0.99f; val vy = (py[i] - oy[i]) * 0.99f
                    ox[i] = px[i]; oy[i] = py[i]
                    px[i] += vx; py[i] += vy + 0.5f
                }
                // satisfy constraints
                repeat(3) {
                    for (r in 0 until rows) for (c in 0 until cols) {
                        val i = idx(c, r)
                        if (c < cols - 1) solve(i, idx(c + 1, r), rest.toFloat(), px, py, pinned)
                        if (r < rows - 1) solve(i, idx(c, r + 1), rest.toFloat(), px, py, pinned)
                    }
                }
                tick++
            }
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Cloth") }, navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }) },
        modifier = Modifier.fillMaxSize()
    ) { inner ->
        Column(Modifier.padding(inner).fillMaxSize()) {
            Box(Modifier.fillMaxSize().background(Color(0xFF0A0A12)).onSizeChanged { size = it }
                .pointerInput(seeded) {
                    detectDragGestures { change, _ ->
                        val p = change.position
                        var best = -1; var bd = 60f * 60f
                        for (i in 0 until n) { val dx = px[i] - p.x; val dy = py[i] - p.y; val d = dx * dx + dy * dy; if (d < bd) { bd = d; best = i } }
                        if (best >= 0 && !pinned[best]) { px[best] = p.x; py[best] = p.y }
                    }
                }
                .drawBehind {
                    tick
                    if (seeded == 0) return@drawBehind
                    for (r in 0 until rows) for (c in 0 until cols) {
                        val i = idx(c, r)
                        if (c < cols - 1) { val j = idx(c + 1, r); drawLine(Color(0xFF66D9FF), Offset(px[i], py[i]), Offset(px[j], py[j]), 1.5f) }
                        if (r < rows - 1) { val j = idx(c, r + 1); drawLine(Color(0xFF66D9FF), Offset(px[i], py[i]), Offset(px[j], py[j]), 1.5f) }
                    }
                })
        }
    }
}

private fun solve(a: Int, b: Int, rest: Float, px: FloatArray, py: FloatArray, pinned: BooleanArray) {
    val dx = px[b] - px[a]; val dy = py[b] - py[a]
    val d = sqrt(dx * dx + dy * dy); if (d == 0f) return
    val diff = (rest - d) / d * 0.5f
    val ox = dx * diff; val oy = dy * diff
    if (!pinned[a]) { px[a] -= ox; py[a] -= oy }
    if (!pinned[b]) { px[b] += ox; py[b] += oy }
}
