package pro.jayeshseth.devcontrols.ui

import pro.jayeshseth.devcontrols.util.fmt
import pro.jayeshseth.devcontrols.util.hex2
import pro.jayeshseth.devcontrols.util.pad
import pro.jayeshseth.devcontrols.util.nowMillis

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp

private enum class Chart { Bar, Line, Donut }

/**
 * Charts lab — Bar, Line, and Donut drawn on Canvas with an animated reveal on each
 * switch. Tap the Line chart to scrub to the nearest data point and read its value.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChartsLabScreen(onBack: () -> Unit) {
    val data = remember { listOf(0.4f, 0.8f, 0.55f, 0.95f, 0.3f, 0.7f, 0.6f, 0.85f) }
    val colors = remember {
        listOf(Color(0xFFFF5F6D), Color(0xFF2193B0), Color(0xFF92FE9D), Color(0xFFFFC371),
            Color(0xFF7F00FF), Color(0xFF26D0CE), Color(0xFFFC466B), Color(0xFFFF8008))
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Charts") },
                navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }
            )
        },
        modifier = Modifier.fillMaxSize()
    ) { inner ->
        var chart by remember { mutableStateOf(Chart.Bar) }
        var scrub by remember { mutableIntStateOf(-1) }
        val anim = remember { Animatable(0f) }
        LaunchedEffect(chart) { anim.snapTo(0f); anim.animateTo(1f, tween(800)) }

        Column(Modifier.padding(inner).fillMaxSize().padding(16.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Chart.entries.forEach { c -> FilterChip(chart == c, { chart = c; scrub = -1 }, { Text(c.name) }) }
            }
            Spacer(Modifier.height(16.dp))
            Box(
                Modifier.fillMaxWidth().height(380.dp)
                    .pointerInput(chart) {
                        if (chart == Chart.Line) detectTapGestures { tap ->
                            scrub = (tap.x / size.width * (data.size - 1)).toInt().coerceIn(0, data.size - 1)
                        }
                    }
                    .drawBehind {
                        val p = anim.value
                        when (chart) {
                            Chart.Bar -> drawBars(data, colors, p)
                            Chart.Line -> drawLine(data, p, scrub)
                            Chart.Donut -> drawDonut(data, colors, p)
                        }
                    }
            )
            if (chart == Chart.Line && scrub >= 0) {
                Text("point ${scrub + 1} = ${fmt(data[scrub], 2)}", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawBars(data: List<Float>, colors: List<Color>, p: Float) {
    val n = data.size
    val gap = 12f
    val barW = (size.width - gap * (n - 1)) / n
    data.forEachIndexed { i, v ->
        val h = v * size.height * p
        drawRect(colors[i % colors.size], topLeft = Offset(i * (barW + gap), size.height - h), size = Size(barW, h))
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawLine(data: List<Float>, p: Float, scrub: Int) {
    val n = data.size
    val dx = size.width / (n - 1)
    val path = Path()
    data.forEachIndexed { i, v ->
        val x = i * dx; val y = size.height - v * size.height
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    val pm = PathMeasure(); pm.setPath(path, false)
    val dst = Path(); pm.getSegment(0f, pm.length * p, dst, true)
    drawPath(dst, Color(0xFF18FFFF), style = Stroke(width = 6f, cap = StrokeCap.Round))
    if (scrub in data.indices) {
        val x = scrub * dx; val y = size.height - data[scrub] * size.height
        drawCircle(Color.White, radius = 12f, center = Offset(x, y))
        drawLine(Color.White.copy(alpha = 0.4f), Offset(x, 0f), Offset(x, size.height), strokeWidth = 2f)
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawDonut(data: List<Float>, colors: List<Color>, p: Float) {
    val total = data.sum()
    var start = -90f
    val stroke = 70f
    val d = kotlin.math.min(size.width, size.height) - stroke
    val topLeft = Offset((size.width - d) / 2f, (size.height - d) / 2f)
    data.forEachIndexed { i, v ->
        val sweep = v / total * 360f * p
        drawArc(
            color = colors[i % colors.size],
            startAngle = start, sweepAngle = sweep, useCenter = false,
            topLeft = topLeft, size = Size(d, d),
            style = Stroke(width = stroke)
        )
        start += sweep
    }
}
