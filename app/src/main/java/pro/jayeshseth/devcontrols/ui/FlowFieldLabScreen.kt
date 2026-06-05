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
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/** Flow field — particles ride a Perlin-ish vector field, painting generative streams. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlowFieldLabScreen(onBack: () -> Unit) {
    val n = 900
    val px = remember { FloatArray(n) }; val py = remember { FloatArray(n) }
    var size by remember { mutableStateOf(IntSize.Zero) }
    var tick by remember { mutableIntStateOf(0) }
    var seeded by remember { mutableIntStateOf(0) }
    var t by remember { androidx.compose.runtime.mutableFloatStateOf(0f) }

    LaunchedEffect(size) {
        if (size == IntSize.Zero) return@LaunchedEffect
        for (i in 0 until n) { px[i] = Random.nextFloat() * size.width; py[i] = Random.nextFloat() * size.height }
        seeded = 1
        while (true) { withFrameNanos { t += 0.003f; tick++ } }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Flow Field") }, navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }) },
        modifier = Modifier.fillMaxSize()
    ) { inner ->
        Column(Modifier.padding(inner).fillMaxSize()) {
            Box(Modifier.fillMaxSize().background(Color(0xFF05050A)).onSizeChanged { size = it }.drawBehind {
                tick
                if (seeded == 0) return@drawBehind
                for (i in 0 until n) {
                    val a = (sin(px[i] * 0.006f + t) + cos(py[i] * 0.006f - t) + sin((px[i] + py[i]) * 0.004f)) * 2f
                    val nx = px[i] + cos(a) * 2f; val ny = py[i] + sin(a) * 2f
                    drawLine(Color.hsv((a * 40f).mod(360f), 0.6f, 1f, 0.5f), Offset(px[i], py[i]), Offset(nx, ny), 1.4f)
                    px[i] = nx; py[i] = ny
                    if (px[i] < 0 || px[i] > size.width || py[i] < 0 || py[i] > size.height) {
                        px[i] = Random.nextFloat() * size.width; py[i] = Random.nextFloat() * size.height
                    }
                }
            })
        }
    }
}
