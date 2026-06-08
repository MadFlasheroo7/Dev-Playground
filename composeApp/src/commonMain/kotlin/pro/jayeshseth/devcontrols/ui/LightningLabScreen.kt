package pro.jayeshseth.devcontrols.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import kotlin.random.Random

private class Bolt(val pts: List<Offset>, var life: Float)

/** Lightning — tap to strike a midpoint-displacement bolt from the top that flickers and fades. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LightningLabScreen(onBack: () -> Unit) {
    val bolts = remember { mutableStateListOf<Bolt>() }
    var tick by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        while (true) { withFrameNanos { bolts.forEach { it.life -= 0.04f }; bolts.removeAll { it.life <= 0f }; tick++ } }
    }

    fun makeBolt(from: Offset, to: Offset): List<Offset> {
        var seg = mutableListOf(from, to)
        repeat(6) {
            val next = mutableListOf<Offset>()
            for (i in 0 until seg.size - 1) {
                val a = seg[i]; val b = seg[i + 1]
                val mid = Offset((a.x + b.x) / 2, (a.y + b.y) / 2)
                val len = kotlin.math.hypot(b.x - a.x, b.y - a.y)
                val off = (Random.nextFloat() - 0.5f) * len * 0.4f
                // perpendicular
                val nx = -(b.y - a.y) / (len + 1e-3f); val ny = (b.x - a.x) / (len + 1e-3f)
                next.add(a); next.add(Offset(mid.x + nx * off, mid.y + ny * off))
            }
            next.add(seg.last()); seg = next
        }
        return seg
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Lightning") }, navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }) },
        modifier = Modifier.fillMaxSize()
    ) { inner ->
        Column(Modifier.padding(inner).fillMaxSize()) {
            Text("Tap to strike.", Modifier.padding(8.dp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Box(Modifier.fillMaxSize().background(Color(0xFF05060E))
                .pointerInput(Unit) { detectTapGestures { p -> bolts.add(Bolt(makeBolt(Offset(p.x, 0f), p), 1f)) } }
                .drawBehind {
                    tick
                    for (b in bolts) {
                        val a = b.life.coerceIn(0f, 1f)
                        for (i in 0 until b.pts.size - 1) {
                            drawLine(Color(0xFFB3E5FC).copy(alpha = a), b.pts[i], b.pts[i + 1], 5f, cap = StrokeCap.Round)
                            drawLine(Color.White.copy(alpha = a), b.pts[i], b.pts[i + 1], 2f, cap = StrokeCap.Round)
                        }
                    }
                })
        }
    }
}
