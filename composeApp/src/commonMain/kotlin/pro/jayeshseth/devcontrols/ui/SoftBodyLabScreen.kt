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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

/** Soft-body jelly blob — Verlet mass points with perimeter + radial springs; drag to poke it. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SoftBodyLabScreen(onBack: () -> Unit) {
    val np = 24
    val total = np + 1            // last index = centre
    val px = remember { FloatArray(total) }; val py = remember { FloatArray(total) }
    val ox = remember { FloatArray(total) }; val oy = remember { FloatArray(total) }
    val seeded = remember { booleanArrayOf(false) }
    var size by remember { mutableStateOf(IntSize.Zero) }
    var grab by remember { mutableStateOf(-1) }
    var grabPos by remember { mutableStateOf(Offset.Zero) }
    var tick by remember { mutableIntStateOf(0) }
    var r0 by remember { mutableStateOf(0f) }

    LaunchedEffect(size) {
        if (size == IntSize.Zero) return@LaunchedEffect
        val cx = size.width / 2f; val cy = size.height * 0.35f
        r0 = kotlin.math.min(size.width, size.height) * 0.22f
        if (!seeded[0]) {
            for (i in 0 until np) {
                val a = i * 6.2831f / np
                px[i] = cx + cos(a) * r0; py[i] = cy + sin(a) * r0
            }
            px[np] = cx; py[np] = cy
            for (i in 0 until total) { ox[i] = px[i]; oy[i] = py[i] }
            seeded[0] = true
        }
        val restPerim = 2 * kotlin.math.PI.toFloat() * r0 / np
        while (true) {
            withFrameNanos {
                val w = size.width.toFloat(); val h = size.height.toFloat()
                for (i in 0 until total) {
                    if (i == grab) { px[i] = grabPos.x; py[i] = grabPos.y; ox[i] = px[i]; oy[i] = py[i]; continue }
                    val vx = (px[i] - ox[i]) * 0.99f; val vy = (py[i] - oy[i]) * 0.99f
                    ox[i] = px[i]; oy[i] = py[i]
                    px[i] += vx; py[i] += vy + 0.5f
                }
                repeat(5) {
                    for (i in 0 until np) {
                        // perimeter spring to next
                        constrain(px, py, i, (i + 1) % np, restPerim, grab)
                        // radial spring to centre
                        constrain(px, py, i, np, r0, grab)
                    }
                    for (i in 0 until total) {
                        if (px[i] < 8f) px[i] = 8f; if (px[i] > w - 8f) px[i] = w - 8f
                        if (py[i] > h - 8f) py[i] = h - 8f; if (py[i] < 8f) py[i] = 8f
                    }
                }
                tick++
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Soft Body") },
                navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }
            )
        },
        modifier = Modifier.fillMaxSize()
    ) { inner ->
        Column(Modifier.padding(inner).fillMaxSize()) {
            Box(
                Modifier.fillMaxSize().background(Color(0xFF0B0B14)).onSizeChanged { size = it }
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = { o ->
                                var best = -1; var bd = 80f
                                for (i in 0 until np) { val d = hypot(px[i] - o.x, py[i] - o.y); if (d < bd) { bd = d; best = i } }
                                grab = best; grabPos = o
                            },
                            onDrag = { ch, _ -> grabPos = ch.position; ch.consume() },
                            onDragEnd = { grab = -1 }, onDragCancel = { grab = -1 }
                        )
                    }
                    .drawBehind {
                        tick
                        val path = Path().apply {
                            moveTo(px[0], py[0]); for (i in 1 until np) lineTo(px[i], py[i]); close()
                        }
                        drawPath(path, Color(0xFFE040FB).copy(alpha = 0.55f))
                        for (i in 0 until np) drawCircle(Color(0xFF18FFFF), 6f, Offset(px[i], py[i]))
                    }
            )
        }
    }
}

private fun constrain(px: FloatArray, py: FloatArray, a: Int, b: Int, rest: Float, grab: Int) {
    val dx = px[b] - px[a]; val dy = py[b] - py[a]
    val d = hypot(dx, dy); if (d < 0.001f) return
    val diff = (d - rest) / d
    val ma = if (a == grab) 0f else 0.5f
    val mb = if (b == grab) 0f else 0.5f
    px[a] += dx * diff * ma; py[a] += dy * diff * ma
    px[b] -= dx * diff * mb; py[b] -= dy * diff * mb
}
