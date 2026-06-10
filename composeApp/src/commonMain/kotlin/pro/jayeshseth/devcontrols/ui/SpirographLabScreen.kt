package pro.jayeshseth.devcontrols.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/** Spirograph (hypotrochoid) — sliders for R, r, d; the curve traces in progressively. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpirographLabScreen(onBack: () -> Unit) {
    var bigR by remember { mutableFloatStateOf(120f) }
    var smallR by remember { mutableFloatStateOf(50f) }
    var dist by remember { mutableFloatStateOf(70f) }
    val progress by rememberInfiniteTransition(label = "sp").animateFloat(
        0f, 1f, infiniteRepeatable(tween(6000, easing = LinearEasing), RepeatMode.Restart), label = "p"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Spirograph") },
                navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }
            )
        },
        modifier = Modifier.fillMaxSize()
    ) { inner ->
        Column(Modifier.padding(inner).fillMaxSize().padding(12.dp)) {
            Slider3("R ${bigR.toInt()}", bigR, 60f..200f) { bigR = it }
            Slider3("r ${smallR.toInt()}", smallR, 10f..120f) { smallR = it }
            Slider3("d ${dist.toInt()}", dist, 10f..140f) { dist = it }
            Box(
                Modifier.fillMaxSize().background(Color(0xFF0A0A12)).drawBehind {
                    val cx = size.width / 2f; val cy = size.height / 2f
                    val scale = (min(size.width, size.height) / 2f) / (bigR + dist + 10f)
                    val diff = bigR - smallR
                    val ratio = if (smallR != 0f) diff / smallR else 1f
                    val maxTheta = (2.0 * kotlin.math.PI * 14).toFloat() * progress
                    val path = Path()
                    var th = 0f
                    val stepN = 1400
                    val dt = maxTheta / stepN
                    var first = true
                    var k = 0
                    while (k <= stepN) {
                        val x = cx + (diff * cos(th) + dist * cos(ratio * th)) * scale
                        val y = cy + (diff * sin(th) - dist * sin(ratio * th)) * scale
                        if (first) { path.moveTo(x, y); first = false } else path.lineTo(x, y)
                        th += dt; k++
                    }
                    drawPath(path, Color(0xFF18FFFF), style = Stroke(width = 2.5f, cap = StrokeCap.Round))
                }
            )
        }
    }
}

@Composable
private fun Slider3(label: String, value: Float, range: ClosedFloatingPointRange<Float>, onValue: (Float) -> Unit) {
    Text(label, style = MaterialTheme.typography.labelMedium)
    Slider(value = value, onValueChange = onValue, valueRange = range)
}
