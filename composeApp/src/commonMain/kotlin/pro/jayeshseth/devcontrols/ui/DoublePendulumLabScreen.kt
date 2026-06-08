package pro.jayeshseth.devcontrols.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin

/** Double pendulum — chaotic motion via the Lagrangian equations, with a fading tip trail. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DoublePendulumLabScreen(onBack: () -> Unit) {
    // a1, a2, a1v, a2v
    val s = remember { floatArrayOf(Math.PI.toFloat() / 2f * 1.7f, Math.PI.toFloat() / 2f * 1.2f, 0f, 0f) }
    val trail = remember { ArrayDeque<Offset>() }
    var tick by remember { mutableIntStateOf(0) }
    var resetN by remember { mutableIntStateOf(0) }

    LaunchedEffect(resetN) {
        s[0] = 2.4f; s[1] = 1.8f; s[2] = 0f; s[3] = 0f; trail.clear()
        val m1 = 1f; val m2 = 1f; val l1 = 1f; val l2 = 1f; val g = 1.2f; val dt = 0.04f
        while (true) {
            withFrameNanos {
                repeat(4) {
                    val a1 = s[0]; val a2 = s[1]; val a1v = s[2]; val a2v = s[3]
                    val d = a1 - a2
                    val den = 2 * m1 + m2 - m2 * cos(2 * a1 - 2 * a2)
                    val a1a = (-g * (2 * m1 + m2) * sin(a1) - m2 * g * sin(a1 - 2 * a2) -
                        2 * sin(d) * m2 * (a2v * a2v * l2 + a1v * a1v * l1 * cos(d))) / (l1 * den)
                    val a2a = (2 * sin(d) * (a1v * a1v * l1 * (m1 + m2) + g * (m1 + m2) * cos(a1) +
                        a2v * a2v * l2 * m2 * cos(d))) / (l2 * den)
                    s[2] = a1v + a1a * dt; s[3] = a2v + a2a * dt
                    s[0] = a1 + s[2] * dt; s[1] = a2 + s[3] * dt
                }
                tick++
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Double Pendulum") },
                navigationIcon = { TextButton(onClick = onBack) { Text("Back") } },
                actions = { TextButton(onClick = { resetN++ }) { Text("Reset") } }
            )
        },
        modifier = Modifier.fillMaxSize()
    ) { inner ->
        Column(Modifier.padding(inner).fillMaxSize()) {
            Box(
                Modifier.fillMaxSize().background(Color(0xFF07070E)).drawBehind {
                    tick
                    val pivot = Offset(size.width / 2f, size.height * 0.32f)
                    val len = size.minDimension * 0.22f
                    val p1 = pivot + Offset(sin(s[0]) * len, cos(s[0]) * len)
                    val p2 = p1 + Offset(sin(s[1]) * len, cos(s[1]) * len)
                    trail.addLast(p2); if (trail.size > 160) trail.removeFirst()
                    if (trail.size > 1) {
                        val path = Path().apply { moveTo(trail.first().x, trail.first().y); trail.forEach { lineTo(it.x, it.y) } }
                        drawPath(path, Color(0xFFE040FB).copy(alpha = 0.5f), style = Stroke(width = 2f))
                    }
                    drawLine(Color.White, pivot, p1, strokeWidth = 4f, cap = StrokeCap.Round)
                    drawLine(Color.White, p1, p2, strokeWidth = 4f, cap = StrokeCap.Round)
                    drawCircle(Color(0xFF18FFFF), 14f, p1)
                    drawCircle(Color(0xFFFF5F6D), 14f, p2)
                }
            )
        }
    }
}
