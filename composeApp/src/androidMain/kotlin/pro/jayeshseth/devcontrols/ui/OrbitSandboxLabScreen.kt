package pro.jayeshseth.devcontrols.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.unit.dp
import kotlin.math.sqrt

private class Body(var x: Float, var y: Float, var vx: Float, var vy: Float, val color: Color, val trail: ArrayDeque<Offset> = ArrayDeque())

/** Orbit sandbox — a central star; tap to launch planets that orbit under gravity, leaving trails. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrbitSandboxLabScreen(onBack: () -> Unit) {
    val bodies = remember { mutableListOf<Body>() }
    var size by remember { mutableStateOf(IntSize.Zero) }
    var tick by remember { mutableIntStateOf(0) }
    val palette = listOf(Color(0xFF4FC3F7), Color(0xFF81C784), Color(0xFFFFB74D), Color(0xFFE57373), Color(0xFFBA68C8))

    LaunchedEffect(size) {
        if (size == IntSize.Zero) return@LaunchedEffect
        val cx = size.width / 2f; val cy = size.height / 2f
        val gm = 90000f
        while (true) {
            withFrameNanos {
                for (b in bodies) {
                    val dx = cx - b.x; val dy = cy - b.y; val r2 = (dx * dx + dy * dy).coerceAtLeast(400f); val r = sqrt(r2)
                    val a = gm / r2
                    b.vx += a * dx / r; b.vy += a * dy / r
                    b.x += b.vx; b.y += b.vy
                    b.trail.addLast(Offset(b.x, b.y)); if (b.trail.size > 60) b.trail.removeFirst()
                }
                tick++
            }
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Orbit Sandbox") }, navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }) },
        modifier = Modifier.fillMaxSize()
    ) { inner ->
        Column(Modifier.padding(inner).fillMaxSize()) {
            Row(Modifier.fillMaxWidth().padding(12.dp)) {
                Text("Tap to launch a planet (${bodies.size})", Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                OutlinedButton(onClick = { bodies.clear() }) { Text("Clear") }
            }
            Box(Modifier.fillMaxSize().background(Color(0xFF05050E)).onSizeChanged { size = it }
                .pointerInput(Unit) {
                    detectTapGestures { p ->
                        val cx = size.width / 2f; val cy = size.height / 2f
                        val dx = p.x - cx; val dy = p.y - cy; val r = sqrt(dx * dx + dy * dy).coerceAtLeast(1f)
                        // perpendicular for a roughly circular orbit
                        val speed = sqrt(90000f / r)
                        bodies.add(Body(p.x, p.y, -dy / r * speed, dx / r * speed, palette[bodies.size % palette.size]))
                    }
                }
                .drawBehind {
                    tick
                    val cx = size.width / 2f; val cy = size.height / 2f
                    drawCircle(Color(0xFFFFE082), 18f, Offset(cx, cy))
                    for (b in bodies) {
                        for (k in 1 until b.trail.size) drawLine(b.color.copy(alpha = k.toFloat() / b.trail.size * 0.6f), b.trail[k - 1], b.trail[k], 2f)
                        drawCircle(b.color, 7f, Offset(b.x, b.y))
                    }
                })
        }
    }
}
