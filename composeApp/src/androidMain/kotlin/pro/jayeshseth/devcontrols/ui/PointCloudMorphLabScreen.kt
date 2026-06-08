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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/** Point-cloud morph — particles ease between a sphere and a cube shell, rotating. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PointCloudMorphLabScreen(onBack: () -> Unit) {
    val n = 700
    val sphere = remember {
        Array(n) {
            val u = Random.nextFloat() * 2 * PI.toFloat(); val v = Random.nextFloat() * 2 - 1
            val s = sqrt(1 - v * v)
            floatArrayOf(s * cos(u), s * sin(u), v)
        }
    }
    val cubeP = remember {
        Array(n) {
            val f = Random.nextInt(6); val a = Random.nextFloat() * 2 - 1; val b = Random.nextFloat() * 2 - 1
            when (f) { 0 -> floatArrayOf(1f,a,b);1 -> floatArrayOf(-1f,a,b);2 -> floatArrayOf(a,1f,b);3 -> floatArrayOf(a,-1f,b);4 -> floatArrayOf(a,b,1f);else -> floatArrayOf(a,b,-1f) }
        }
    }
    var ay by remember { mutableFloatStateOf(0f) }
    var t by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(Unit) { while (true) { withFrameNanos { ay += 0.01f; t += 0.012f } } }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Point-Cloud Morph") }, navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }) },
        modifier = Modifier.fillMaxSize()
    ) { inner ->
        Column(Modifier.padding(inner).fillMaxSize()) {
            Box(Modifier.fillMaxSize().background(Color(0xFF04040C)).drawBehind {
                val cx = size.width / 2f; val cy = size.height / 2f
                val scale = minOf(cx, cy) * 0.7f
                val mix = (sin(t) * 0.5f + 0.5f)
                for (i in 0 until n) {
                    val sx0 = sphere[i][0] * (1 - mix) + cubeP[i][0] * mix
                    val sy0 = sphere[i][1] * (1 - mix) + cubeP[i][1] * mix
                    val sz0 = sphere[i][2] * (1 - mix) + cubeP[i][2] * mix
                    val (x, y, z) = rotate(sx0, sy0, sz0, 0.5f, ay)
                    val p = 3f / (3f + z)
                    drawCircle(Color.hsv((i * 360f / n), 0.7f, 1f).copy(alpha = (p - 0.4f).coerceIn(0.2f, 1f)), 3f * p, Offset(cx + x * scale * p, cy + y * scale * p))
                }
            })
        }
    }
}

private fun sqrt(x: Float) = kotlin.math.sqrt(x)
