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
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import kotlin.math.sqrt
import kotlin.random.Random

/** N-body gravity — point masses attract each other (with softening); additive glow render. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NBodyLabScreen(onBack: () -> Unit) {
    val n = 60
    val px = remember { FloatArray(n) }; val py = remember { FloatArray(n) }
    val vx = remember { FloatArray(n) }; val vy = remember { FloatArray(n) }
    val mass = remember { FloatArray(n) { if (it == 0) 60f else 1f } }
    var size by remember { mutableStateOf(IntSize.Zero) }
    var tick by remember { mutableIntStateOf(0) }
    var resetN by remember { mutableIntStateOf(0) }

    LaunchedEffect(size, resetN) {
        if (size == IntSize.Zero) return@LaunchedEffect
        val cx = size.width / 2f; val cy = size.height / 2f
        for (i in 0 until n) {
            if (i == 0) { px[i] = cx; py[i] = cy; vx[i] = 0f; vy[i] = 0f }
            else {
                val ang = Random.nextFloat() * 6.2831f
                val r = 120f + Random.nextFloat() * (kotlin.math.min(size.width, size.height) / 2.4f)
                px[i] = cx + kotlin.math.cos(ang) * r; py[i] = cy + kotlin.math.sin(ang) * r
                // tangential velocity for orbit
                val sp = sqrt(0.25f * mass[0] / r)
                vx[i] = -kotlin.math.sin(ang) * sp; vy[i] = kotlin.math.cos(ang) * sp
            }
        }
        val gconst = 0.25f; val dt = 1f; val soft = 60f
        while (true) {
            withFrameNanos {
                for (i in 0 until n) {
                    var ax = 0f; var ay = 0f
                    for (j in 0 until n) if (j != i) {
                        val dx = px[j] - px[i]; val dy = py[j] - py[i]
                        val d2 = dx * dx + dy * dy + soft * soft
                        val inv = mass[j] / (d2 * sqrt(d2))
                        ax += dx * inv; ay += dy * inv
                    }
                    if (i != 0) { vx[i] += ax * gconst * dt; vy[i] += ay * gconst * dt }
                }
                for (i in 1 until n) { px[i] += vx[i] * dt; py[i] += vy[i] * dt }
                tick++
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("N-Body Gravity") },
                navigationIcon = { TextButton(onClick = onBack) { Text("Back") } },
                actions = { TextButton(onClick = { resetN++ }) { Text("Reset") } }
            )
        },
        modifier = Modifier.fillMaxSize()
    ) { inner ->
        Column(Modifier.padding(inner).fillMaxSize()) {
            Box(
                Modifier.fillMaxSize().background(Color.Black).onSizeChanged { size = it }.drawBehind {
                    tick
                    for (i in 0 until n) {
                        val c = if (i == 0) Color(0xFFFFD740) else Color(0xFF18FFFF)
                        val r = if (i == 0) 22f else 9f
                        drawCircle(
                            Brush.radialGradient(listOf(c.copy(alpha = 0.9f), c.copy(alpha = 0f)),
                                center = Offset(px[i], py[i]), radius = r * 2.2f),
                            radius = r * 2.2f, center = Offset(px[i], py[i]), blendMode = BlendMode.Plus
                        )
                    }
                }
            )
        }
    }
}
