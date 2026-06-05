package pro.jayeshseth.devcontrols.ui

import androidx.compose.foundation.background
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
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.random.Random

/** Boids flocking — separation, alignment, cohesion across the flock; edge-wrapping. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BoidsLabScreen(onBack: () -> Unit) {
    val n = 80
    val px = remember { FloatArray(n) }; val py = remember { FloatArray(n) }
    val vx = remember { FloatArray(n) }; val vy = remember { FloatArray(n) }
    var size by remember { mutableStateOf(IntSize.Zero) }
    var tick by remember { mutableIntStateOf(0) }

    LaunchedEffect(size) {
        if (size == IntSize.Zero) return@LaunchedEffect
        for (i in 0 until n) {
            px[i] = Random.nextFloat() * size.width; py[i] = Random.nextFloat() * size.height
            val a = Random.nextFloat() * 6.2831f; vx[i] = cos(a) * 2f; vy[i] = sin(a) * 2f
        }
        val r = 70f; val maxSp = 4.5f
        while (true) {
            withFrameNanos {
                val w = size.width.toFloat(); val h = size.height.toFloat()
                for (i in 0 until n) {
                    var sepx = 0f; var sepy = 0f; var avx = 0f; var avy = 0f; var cx = 0f; var cy = 0f; var cnt = 0
                    for (j in 0 until n) if (j != i) {
                        val dx = px[i] - px[j]; val dy = py[i] - py[j]; val d = hypot(dx, dy)
                        if (d < r && d > 0f) {
                            sepx += dx / d; sepy += dy / d
                            avx += vx[j]; avy += vy[j]; cx += px[j]; cy += py[j]; cnt++
                        }
                    }
                    if (cnt > 0) {
                        avx /= cnt; avy /= cnt; cx /= cnt; cy /= cnt
                        vx[i] += sepx * 0.05f + (avx - vx[i]) * 0.04f + (cx - px[i]) * 0.0006f
                        vy[i] += sepy * 0.05f + (avy - vy[i]) * 0.04f + (cy - py[i]) * 0.0006f
                    }
                    val sp = hypot(vx[i], vy[i]); if (sp > maxSp) { vx[i] = vx[i] / sp * maxSp; vy[i] = vy[i] / sp * maxSp }
                    px[i] += vx[i]; py[i] += vy[i]
                    if (px[i] < 0) px[i] += w; if (px[i] > w) px[i] -= w
                    if (py[i] < 0) py[i] += h; if (py[i] > h) py[i] -= h
                }
                tick++
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Boids") },
                navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }
            )
        },
        modifier = Modifier.fillMaxSize()
    ) { inner ->
        Column(Modifier.padding(inner).fillMaxSize()) {
            Box(
                Modifier.fillMaxSize().background(Color(0xFF0A1020)).onSizeChanged { size = it }.drawBehind {
                    tick
                    for (i in 0 until n) {
                        val ang = atan2(vy[i], vx[i])
                        val p = Offset(px[i], py[i])
                        val tip = p + Offset(cos(ang) * 11f, sin(ang) * 11f)
                        val l = p + Offset(cos(ang + 2.5f) * 7f, sin(ang + 2.5f) * 7f)
                        val rr = p + Offset(cos(ang - 2.5f) * 7f, sin(ang - 2.5f) * 7f)
                        val path = Path().apply { moveTo(tip.x, tip.y); lineTo(l.x, l.y); lineTo(rr.x, rr.y); close() }
                        drawPath(path, Color(0xFF69F0AE))
                    }
                }
            )
        }
    }
}
