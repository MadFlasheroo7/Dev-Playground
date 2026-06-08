package pro.jayeshseth.devcontrols.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp
import kotlin.random.Random

/** Warp starfield — stars stream toward the viewer with motion-streaks; adjustable speed. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StarfieldLabScreen(onBack: () -> Unit) {
    val n = 450
    val sx = remember { FloatArray(n) { Random.nextFloat() * 2f - 1f } }
    val sy = remember { FloatArray(n) { Random.nextFloat() * 2f - 1f } }
    val sz = remember { FloatArray(n) { Random.nextFloat() } }
    val pz = remember { FloatArray(n) { 1f } }
    var speed by remember { mutableFloatStateOf(0.012f) }
    var tick by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        while (true) {
            withFrameNanos {
                for (i in 0 until n) {
                    pz[i] = sz[i]
                    sz[i] -= speed
                    if (sz[i] <= 0.02f) { sx[i] = Random.nextFloat() * 2f - 1f; sy[i] = Random.nextFloat() * 2f - 1f; sz[i] = 1f; pz[i] = 1f }
                }
                tick++
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Starfield") },
                navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }
            )
        },
        modifier = Modifier.fillMaxSize()
    ) { inner ->
        Column(Modifier.padding(inner).fillMaxSize()) {
            Slider(value = speed, onValueChange = { speed = it }, valueRange = 0.003f..0.04f,
                modifier = Modifier.padding(horizontal = 16.dp))
            Box(
                Modifier.fillMaxSize().background(Color.Black).drawBehind {
                    tick
                    val cx = size.width / 2f; val cy = size.height / 2f
                    val f = size.width * 0.9f
                    for (i in 0 until n) {
                        val x = cx + sx[i] / sz[i] * f
                        val y = cy + sy[i] / sz[i] * f
                        val px = cx + sx[i] / pz[i] * f
                        val py = cy + sy[i] / pz[i] * f
                        val b = (1f - sz[i]).coerceIn(0f, 1f)
                        drawLine(Color.White.copy(alpha = b), Offset(px, py), Offset(x, y),
                            strokeWidth = 1f + b * 2.5f, cap = StrokeCap.Round)
                    }
                }
            )
        }
    }
}
