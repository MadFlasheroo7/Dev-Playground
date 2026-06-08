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
import kotlin.random.Random

private class Ball2(var x: Float, var y: Float, var vx: Float, var vy: Float, val r: Float, val color: Color)

/** Doodle physics — tap to drop balls that fall, bounce, and collide with each other. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DoodlePhysicsLabScreen(onBack: () -> Unit) {
    val balls = remember { mutableListOf<Ball2>() }
    var size by remember { mutableStateOf(IntSize.Zero) }
    var tick by remember { mutableIntStateOf(0) }
    val palette = listOf(Color(0xFFEF5350), Color(0xFF42A5F5), Color(0xFF66BB6A), Color(0xFFFFCA28), Color(0xFFAB47BC))

    LaunchedEffect(size) {
        if (size == IntSize.Zero) return@LaunchedEffect
        while (true) {
            withFrameNanos {
                for (b in balls) { b.vy += 0.4f; b.x += b.vx; b.y += b.vy; b.vx *= 0.999f }
                val nb = balls.size
                for (i in 0 until nb) for (j in i + 1 until nb) {
                    val a = balls[i]; val c = balls[j]
                    val dx = c.x - a.x; val dy = c.y - a.y; val d = sqrt(dx * dx + dy * dy); val min = a.r + c.r
                    if (d in 0.01f..min) {
                        val nx = dx / d; val ny = dy / d; val ov = min - d
                        a.x -= nx * ov / 2; a.y -= ny * ov / 2; c.x += nx * ov / 2; c.y += ny * ov / 2
                        val rel = (c.vx - a.vx) * nx + (c.vy - a.vy) * ny
                        if (rel < 0) { val imp = rel * 0.9f; a.vx += imp * nx; a.vy += imp * ny; c.vx -= imp * nx; c.vy -= imp * ny }
                    }
                }
                for (b in balls) {
                    if (b.x < b.r) { b.x = b.r; b.vx = -b.vx * 0.7f }
                    if (b.x > size.width - b.r) { b.x = size.width - b.r; b.vx = -b.vx * 0.7f }
                    if (b.y > size.height - b.r) { b.y = size.height - b.r; b.vy = -b.vy * 0.6f; b.vx *= 0.9f }
                }
                tick++
            }
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Doodle Physics") }, navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }) },
        modifier = Modifier.fillMaxSize()
    ) { inner ->
        Column(Modifier.padding(inner).fillMaxSize()) {
            Row(Modifier.fillMaxWidth().padding(12.dp)) {
                Text("Tap to drop balls (${balls.size})", Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                OutlinedButton(onClick = { balls.clear() }) { Text("Clear") }
            }
            Box(Modifier.fillMaxSize().background(Color(0xFF0A0A14)).onSizeChanged { size = it }
                .pointerInput(Unit) {
                    detectTapGestures { p -> balls.add(Ball2(p.x, p.y, (Random.nextFloat() - 0.5f) * 6f, 0f, 18f + Random.nextFloat() * 18f, palette[Random.nextInt(palette.size)])) }
                }
                .drawBehind {
                    tick
                    for (b in balls) drawCircle(b.color, b.r, Offset(b.x, b.y))
                })
        }
    }
}
