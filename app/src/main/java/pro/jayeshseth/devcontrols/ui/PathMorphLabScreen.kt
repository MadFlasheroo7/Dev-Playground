package pro.jayeshseth.devcontrols.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * Path drawing lab. A star outline is rendered two ways: **Draw-on** uses
 * [PathMeasure.getSegment] to reveal the stroke from 0→length (the "signature writing
 * itself" effect), and **March** animates a dashed [PathEffect] phase (marching ants).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PathMorphLabScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Path Drawing") },
                navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }
            )
        },
        modifier = Modifier.fillMaxSize()
    ) { inner ->
        var drawOn by remember { mutableStateOf(true) }
        var manual by remember { mutableFloatStateOf(0.5f) }
        var auto by remember { mutableStateOf(true) }

        val t by rememberInfiniteTransition(label = "t").animateFloat(
            0f, 1f, infiniteRepeatable(tween(2200, easing = LinearEasing), RepeatMode.Restart), label = "p"
        )
        val phase by rememberInfiniteTransition(label = "ph").animateFloat(
            0f, 60f, infiniteRepeatable(tween(700, easing = LinearEasing), RepeatMode.Restart), label = "dash"
        )

        Column(Modifier.padding(inner).fillMaxSize().padding(16.dp)) {
            Box(
                Modifier.fillMaxWidth().height(360.dp).drawBehind {
                    val r = min(size.width, size.height) / 2f * 0.8f
                    val star = starPath(size.width / 2f, size.height / 2f, r, r * 0.45f, 5)
                    if (drawOn) {
                        val progress = if (auto) t else manual
                        val pm = PathMeasure()
                        pm.setPath(star, false)
                        val dst = Path()
                        pm.getSegment(0f, pm.length * progress, dst, true)
                        drawPath(dst, Color(0xFF00E5FF), style = Stroke(width = 10f, cap = StrokeCap.Round))
                    } else {
                        drawPath(
                            star, Color(0xFFE040FB),
                            style = Stroke(
                                width = 8f, cap = StrokeCap.Round,
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(40f, 22f), phase)
                            )
                        )
                    }
                }
            )
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(drawOn, { drawOn = true }, { Text("Draw-on") })
                FilterChip(!drawOn, { drawOn = false }, { Text("March") })
            }
            if (drawOn) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Auto", Modifier.weight(1f))
                    androidx.compose.material3.Switch(auto, { auto = it })
                }
                Slider(value = if (auto) t else manual, onValueChange = { manual = it; auto = false }, enabled = !auto)
            }
        }
    }
}

private fun starPath(cx: Float, cy: Float, outer: Float, inner: Float, points: Int): Path {
    val path = Path()
    val step = PI / points
    for (i in 0 until points * 2) {
        val r = if (i % 2 == 0) outer else inner
        val a = step * i - PI / 2
        val x = cx + (r * cos(a)).toFloat()
        val y = cy + (r * sin(a)).toFloat()
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()
    return path
}
