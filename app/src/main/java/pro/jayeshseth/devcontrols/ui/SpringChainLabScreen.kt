package pro.jayeshseth.devcontrols.ui

import androidx.compose.foundation.background
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlin.math.hypot

/**
 * Spring-chain / rope sim with Verlet integration. A chain of nodes hangs under
 * gravity; segment-length constraints (relaxed over several iterations per frame) keep
 * it rope-like. Drag to grab the end node and swing it around.
 */
private const val NODES = 26

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpringChainLabScreen(onBack: () -> Unit) {
    val px = remember { FloatArray(NODES) }
    val py = remember { FloatArray(NODES) }
    val ox = remember { FloatArray(NODES) }
    val oy = remember { FloatArray(NODES) }
    val seeded = remember { booleanArrayOf(false) }
    var size by remember { mutableStateOf(IntSize.Zero) }
    var grab by remember { mutableStateOf<Offset?>(null) }
    var frame by remember { mutableIntStateOf(0) }
    var segLen by remember { mutableStateOf(0f) }

    LaunchedEffect(size) {
        if (size == IntSize.Zero) return@LaunchedEffect
        val anchorX = size.width / 2f
        segLen = (size.height * 0.8f) / NODES
        if (!seeded[0]) {
            for (i in 0 until NODES) {
                px[i] = anchorX; py[i] = 40f + i * segLen
                ox[i] = px[i]; oy[i] = py[i]
            }
            seeded[0] = true
        }
        while (true) {
            withFrameNanos {
                // verlet integrate (node 0 is the pinned anchor)
                for (i in 1 until NODES) {
                    val vx = (px[i] - ox[i]) * 0.99f
                    val vy = (py[i] - oy[i]) * 0.99f
                    ox[i] = px[i]; oy[i] = py[i]
                    px[i] += vx; py[i] += vy + 0.6f // gravity
                }
                // grabbed end follows finger
                grab?.let { px[NODES - 1] = it.x; py[NODES - 1] = it.y }
                // constraint relaxation
                repeat(6) {
                    px[0] = size.width / 2f; py[0] = 40f
                    for (i in 0 until NODES - 1) {
                        val dx = px[i + 1] - px[i]; val dy = py[i + 1] - py[i]
                        val d = hypot(dx, dy).coerceAtLeast(0.001f)
                        val diff = (d - segLen) / d
                        val mCur = if (i == 0) 0f else 0.5f
                        val mNext = if (grab != null && i + 1 == NODES - 1) 0f else 0.5f
                        px[i] += dx * diff * mCur; py[i] += dy * diff * mCur
                        px[i + 1] -= dx * diff * mNext; py[i + 1] -= dy * diff * mNext
                    }
                }
                frame++
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Spring Chain") },
                navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }
            )
        },
        modifier = Modifier.fillMaxSize()
    ) { inner ->
        Column(Modifier.padding(inner).fillMaxSize()) {
            Text("Drag the chain's end to swing it.", style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(12.dp))
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color(0xFF0B0B12))
                    .onSizeChanged { size = it }
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = { grab = it },
                            onDrag = { ch, _ -> grab = ch.position; ch.consume() },
                            onDragEnd = { grab = null },
                            onDragCancel = { grab = null }
                        )
                    }
                    .drawBehind {
                        frame
                        for (i in 0 until NODES - 1) {
                            drawLine(Color(0xFF58A6FF), Offset(px[i], py[i]), Offset(px[i + 1], py[i + 1]),
                                strokeWidth = 8f, cap = StrokeCap.Round)
                        }
                        for (i in 0 until NODES) {
                            drawCircle(if (i == NODES - 1) Color(0xFFFF5F6D) else Color(0xFF92FE9D),
                                radius = if (i == NODES - 1) 18f else 7f, center = Offset(px[i], py[i]))
                        }
                    }
            )
        }
    }
}
