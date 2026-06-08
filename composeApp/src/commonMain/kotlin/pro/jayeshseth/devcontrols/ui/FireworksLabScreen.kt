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
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random
import androidx.compose.runtime.withFrameNanos

private class Rocket(var x: Float, var y: Float, var vy: Float, val targetY: Float, val color: Color)
private class Spark(var x: Float, var y: Float, var vx: Float, var vy: Float, var life: Float, val color: Color)

/** Fireworks — tap to launch a rocket that bursts into sparks under gravity. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FireworksLabScreen(onBack: () -> Unit) {
    val rockets = remember { mutableListOf<Rocket>() }
    val sparks = remember { mutableListOf<Spark>() }
    var tick by remember { mutableIntStateOf(0) }
    val palette = listOf(Color(0xFFFF5F6D), Color(0xFF18FFFF), Color(0xFF69F0AE), Color(0xFFFFD740), Color(0xFFE040FB))

    LaunchedEffect(Unit) {
        while (true) {
            withFrameNanos {
                val ri = rockets.iterator()
                while (ri.hasNext()) {
                    val r = ri.next(); r.y += r.vy; r.vy += 0.06f
                    if (r.vy >= 0 || r.y <= r.targetY) {
                        repeat(60) {
                            val a = Random.nextDouble(0.0, 6.2831).toFloat(); val sp = Random.nextFloat() * 9f + 2f
                            sparks.add(Spark(r.x, r.y, cos(a) * sp, sin(a) * sp, 1f, r.color))
                        }
                        ri.remove()
                    }
                }
                val si = sparks.iterator()
                while (si.hasNext()) {
                    val s = si.next(); s.vy += 0.12f; s.x += s.vx; s.y += s.vy; s.vx *= 0.99f; s.life -= 0.012f
                    if (s.life <= 0f) si.remove()
                }
                tick++
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Fireworks") },
                navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }
            )
        },
        modifier = Modifier.fillMaxSize()
    ) { inner ->
        Column(Modifier.padding(inner).fillMaxSize()) {
            Text("Tap to launch.", Modifier.padding(12.dp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Box(Modifier.fillMaxSize().background(Color(0xFF03030A))
                .pointerInput(Unit) { detectTapGestures { tap ->
                    rockets.add(Rocket(tap.x, java.lang.Float.MAX_VALUE.let { 0f } + 3000f, -16f - Random.nextFloat() * 4f, tap.y, palette[Random.nextInt(palette.size)]))
                } }
                .drawBehind {
                    tick
                    rockets.forEach { drawCircle(it.color, 5f, Offset(it.x, it.y.coerceAtMost(size.height))) }
                    sparks.forEach { drawCircle(it.color.copy(alpha = it.life.coerceIn(0f, 1f)), 4f, Offset(it.x, it.y)) }
                }
            )
        }
    }
}
