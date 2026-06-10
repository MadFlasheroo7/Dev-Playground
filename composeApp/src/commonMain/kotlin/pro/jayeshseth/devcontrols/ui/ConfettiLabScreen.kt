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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotateRad
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * Confetti emitter. Tapping bursts ~50 physics pieces from the touch point: each has a
 * random velocity, spin, and colour; gravity pulls them down and they fade out, getting
 * culled when their life expires. A single frame loop integrates the live pieces.
 */
private class Confetto(
    var x: Float, var y: Float, var vx: Float, var vy: Float,
    var rot: Float, val spin: Float, val color: Color, var life: Float
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfettiLabScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Confetti") },
                navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }
            )
        },
        modifier = Modifier.fillMaxSize()
    ) { inner ->
        val pieces = remember { mutableListOf<Confetto>() }
        var frame by remember { mutableIntStateOf(0) }
        val palette = remember {
            listOf(Color(0xFFFF5F6D), Color(0xFF18FFFF), Color(0xFF69F0AE), Color(0xFFFFD740), Color(0xFFE040FB))
        }

        LaunchedEffect(Unit) {
            while (true) {
                withFrameNanosCompat {
                    val it = pieces.iterator()
                    while (it.hasNext()) {
                        val p = it.next()
                        p.vy += 0.5f
                        p.vx *= 0.99f
                        p.x += p.vx; p.y += p.vy
                        p.rot += p.spin
                        p.life -= 0.012f
                        if (p.life <= 0f) it.remove()
                    }
                    frame++
                }
            }
        }

        fun burst(at: Offset) {
            repeat(50) {
                val ang = Random.nextDouble(0.0, 2 * kotlin.math.PI)
                val speed = Random.nextDouble(6.0, 22.0).toFloat()
                pieces.add(
                    Confetto(
                        x = at.x, y = at.y,
                        vx = (cos(ang) * speed).toFloat(),
                        vy = (sin(ang) * speed).toFloat() - 6f,
                        rot = Random.nextFloat() * 6.28f,
                        spin = (Random.nextFloat() - 0.5f) * 0.6f,
                        color = palette[Random.nextInt(palette.size)],
                        life = 1f
                    )
                )
            }
        }

        Column(Modifier.padding(inner).fillMaxSize()) {
            Text("Tap anywhere to burst confetti.", style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(12.dp))
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color(0xFF101014))
                    .pointerInput(Unit) { detectTapGestures { burst(it) } }
                    .drawBehind {
                        frame
                        pieces.forEach { p ->
                            rotateRad(p.rot, pivot = Offset(p.x, p.y)) {
                                drawRect(
                                    color = p.color.copy(alpha = p.life.coerceIn(0f, 1f)),
                                    topLeft = Offset(p.x - 9f, p.y - 5f),
                                    size = Size(18f, 10f)
                                )
                            }
                        }
                    }
            )
        }
    }
}

private suspend inline fun withFrameNanosCompat(crossinline block: () -> Unit) =
    androidx.compose.runtime.withFrameNanos { block() }
