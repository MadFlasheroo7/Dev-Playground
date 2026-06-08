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
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlin.random.Random
import kotlin.math.sqrt

private class Marble(var x: Float, var y: Float, var vx: Float = 0f, var vy: Float = 0f)

/** Marble run — tap to place pegs; marbles rain from the top and bounce off them. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarbleRunLabScreen(onBack: () -> Unit) {
    val pegs = remember { mutableStateListOf<Offset>() }
    val marbles = remember { mutableListOf<Marble>() }
    var size by remember { mutableStateOf(IntSize.Zero) }
    var tick by remember { mutableIntStateOf(0) }
    val pegR = 14f; val mR = 8f

    LaunchedEffect(size) {
        if (size == IntSize.Zero) return@LaunchedEffect
        var frame = 0
        while (true) {
            withFrameNanos {
                frame++
                if (frame % 10 == 0 && marbles.size < 120) marbles.add(Marble(size.width * (0.2f + Random.nextFloat() * 0.6f), -10f))
                for (m in marbles) {
                    m.vy += 0.3f; m.x += m.vx; m.y += m.vy; m.vx *= 0.999f
                    for (p in pegs) {
                        val dx = m.x - p.x; val dy = m.y - p.y; val d = sqrt(dx * dx + dy * dy); val min = pegR + mR
                        if (d in 0.01f..min) {
                            val nx = dx / d; val ny = dy / d
                            m.x = p.x + nx * min; m.y = p.y + ny * min
                            val dot = m.vx * nx + m.vy * ny
                            m.vx = (m.vx - 2 * dot * nx) * 0.6f; m.vy = (m.vy - 2 * dot * ny) * 0.6f
                        }
                    }
                    if (m.x < mR) { m.x = mR; m.vx = -m.vx * 0.6f }
                    if (m.x > size.width - mR) { m.x = size.width - mR; m.vx = -m.vx * 0.6f }
                }
                marbles.removeAll { it.y > size.height + 20 }
                tick++
            }
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Marble Run") }, navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }) },
        modifier = Modifier.fillMaxSize()
    ) { inner ->
        Column(Modifier.padding(inner).fillMaxSize()) {
            Row(Modifier.fillMaxWidth().padding(12.dp)) {
                Text("Tap to drop pegs (${pegs.size})", Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                OutlinedButton(onClick = { pegs.clear() }) { Text("Clear pegs") }
            }
            Box(Modifier.fillMaxSize().background(Color(0xFF0A0A14)).onSizeChanged { size = it }
                .pointerInput(Unit) { detectTapGestures { pegs.add(it) } }
                .drawBehind {
                    tick
                    pegs.forEach { drawCircle(Color(0xFF7E57C2), pegR, it) }
                    marbles.forEach { drawCircle(Color(0xFF4FC3F7), mR, Offset(it.x, it.y)) }
                })
        }
    }
}
