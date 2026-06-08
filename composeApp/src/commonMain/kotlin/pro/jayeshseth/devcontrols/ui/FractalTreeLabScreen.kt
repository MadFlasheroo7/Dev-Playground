package pro.jayeshseth.devcontrols.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** Recursive fractal tree with a wind sway; sliders for branch angle and depth. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FractalTreeLabScreen(onBack: () -> Unit) {
    var angleDeg by remember { mutableFloatStateOf(28f) }
    var depthF by remember { mutableFloatStateOf(10f) }
    val sway by rememberInfiniteTransition(label = "w").animateFloat(
        0f, (2 * PI).toFloat(), infiniteRepeatable(tween(4000, easing = LinearEasing), RepeatMode.Reverse), label = "s"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Fractal Tree") },
                navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }
            )
        },
        modifier = Modifier.fillMaxSize()
    ) { inner ->
        Column(Modifier.padding(inner).fillMaxSize().padding(12.dp)) {
            Text("Branch angle ${angleDeg.toInt()}°", style = MaterialTheme.typography.labelMedium)
            Slider(value = angleDeg, onValueChange = { angleDeg = it }, valueRange = 8f..60f)
            Text("Depth ${depthF.toInt()}", style = MaterialTheme.typography.labelMedium)
            Slider(value = depthF, onValueChange = { depthF = it }, valueRange = 4f..12f)
            androidx.compose.foundation.layout.Box(
                Modifier.fillMaxSize().background(Color(0xFF07120A)).drawBehind {
                    val a = (angleDeg * PI / 180.0).toFloat()
                    branch(size.width / 2f, size.height, size.height * 0.2f, (-PI / 2).toFloat(), depthF.toInt(), a, sway)
                }
            )
        }
    }
}

private fun DrawScope.branch(x: Float, y: Float, len: Float, dir: Float, depth: Int, angle: Float, sway: Float) {
    if (depth <= 0 || len < 2f) return
    val s = sin(sway + depth) * 0.05f
    val nx = x + cos(dir) * len
    val ny = y + sin(dir) * len
    val t = depth / 12f
    drawLine(
        Color(0xFF8D6E63).copy(alpha = 0.4f + 0.6f * t).let { if (depth <= 2) Color(0xFF69F0AE) else it },
        Offset(x, y), Offset(nx, ny), strokeWidth = (depth * 1.4f), cap = StrokeCap.Round
    )
    branch(nx, ny, len * 0.76f, dir - angle + s, depth - 1, angle, sway)
    branch(nx, ny, len * 0.76f, dir + angle + s, depth - 1, angle, sway)
}
