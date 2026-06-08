package pro.jayeshseth.devcontrols.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlin.math.hypot

/**
 * Particle / metaball sandbox. [N] particles integrate each frame: when you touch, they
 * accelerate toward your finger; otherwise gravity pulls them down. They bounce off the
 * walls. Each is drawn as an additive (BlendMode.Plus) radial glow so overlapping ones
 * merge into metaball blobs.
 */
private const val N = 70

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParticleLabScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Particles") },
                navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }
            )
        },
        modifier = Modifier.fillMaxSize()
    ) { inner ->
        val px = remember { FloatArray(N) }
        val py = remember { FloatArray(N) }
        val vx = remember { FloatArray(N) }
        val vy = remember { FloatArray(N) }
        val seeded = remember { booleanArrayOf(false) }
        var size by remember { mutableStateOf(IntSize.Zero) }
        var attractor by remember { mutableStateOf<Offset?>(null) }
        var frame by remember { mutableIntStateOf(0) }
        val colors = remember {
            listOf(Color(0xFF18FFFF), Color(0xFFE040FB), Color(0xFF69F0AE), Color(0xFFFFD740), Color(0xFFFF6E40))
        }

        LaunchedEffect(size) {
            if (size == IntSize.Zero) return@LaunchedEffect
            val w = size.width.toFloat(); val h = size.height.toFloat()
            if (!seeded[0]) {
                for (i in 0 until N) {
                    px[i] = (i * 97 % size.width).toFloat()
                    py[i] = (i * 53 % size.height).toFloat()
                    vx[i] = ((i % 7) - 3) * 1.5f
                    vy[i] = ((i % 5) - 2) * 1.5f
                }
                seeded[0] = true
            }
            while (true) {
                withFrameNanos {
                    val a = attractor
                    for (i in 0 until N) {
                        if (a != null) {
                            val dx = a.x - px[i]; val dy = a.y - py[i]
                            val d = hypot(dx, dy).coerceAtLeast(20f)
                            vx[i] += dx / d * 0.9f
                            vy[i] += dy / d * 0.9f
                        } else {
                            vy[i] += 0.35f // gravity
                        }
                        vx[i] *= 0.97f; vy[i] *= 0.97f
                        px[i] += vx[i]; py[i] += vy[i]
                        if (px[i] < 0) { px[i] = 0f; vx[i] = -vx[i] * 0.7f }
                        if (px[i] > w) { px[i] = w; vx[i] = -vx[i] * 0.7f }
                        if (py[i] < 0) { py[i] = 0f; vy[i] = -vy[i] * 0.7f }
                        if (py[i] > h) { py[i] = h; vy[i] = -vy[i] * 0.7f }
                    }
                    frame++
                }
            }
        }

        Column(Modifier.padding(inner).fillMaxSize()) {
            Text("Touch and drag — particles chase your finger; let go and gravity takes over.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(12.dp))
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.Black)
                    .onSizeChanged { size = it }
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onPress = { attractor = it; tryAwaitRelease(); attractor = null }
                        )
                    }
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = { attractor = it },
                            onDrag = { change, _ -> attractor = change.position; change.consume() },
                            onDragEnd = { attractor = null },
                            onDragCancel = { attractor = null }
                        )
                    }
                    .drawBehind {
                        frame // read to invalidate each physics frame
                        for (i in 0 until N) {
                            val c = colors[i % colors.size]
                            drawCircle(
                                brush = Brush.radialGradient(
                                    listOf(c.copy(alpha = 0.9f), c.copy(alpha = 0f)),
                                    center = Offset(px[i], py[i]), radius = 60f
                                ),
                                radius = 60f,
                                center = Offset(px[i], py[i]),
                                blendMode = BlendMode.Plus
                            )
                        }
                    }
            )
        }
    }
}
