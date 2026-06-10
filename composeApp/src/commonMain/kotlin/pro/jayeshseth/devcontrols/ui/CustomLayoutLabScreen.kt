package pro.jayeshseth.devcontrols.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * Custom-`Layout` lab. Two hand-written [androidx.compose.ui.layout.MeasurePolicy]s:
 * a **Radial** layout that measures each child then places it around a circle (with a
 * live rotation), and a **Staggered** (masonry) layout that drops each child into the
 * currently-shortest column. Demonstrates the measure → place contract directly.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomLayoutLabScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Custom Layout") },
                navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }
            )
        },
        modifier = Modifier.fillMaxSize()
    ) { inner ->
        var radial by remember { mutableStateOf(true) }
        var count by remember { mutableFloatStateOf(8f) }
        val n = count.toInt()
        val rot by rememberInfiniteTransition(label = "rot").animateFloat(
            0f, 360f, infiniteRepeatable(tween(9000, easing = LinearEasing), RepeatMode.Restart), label = "deg"
        )
        val colors = remember {
            listOf(0xFFFF5F6D, 0xFF2193B0, 0xFF92FE9D, 0xFFFFC371, 0xFF7F00FF, 0xFFE100FF, 0xFF26D0CE, 0xFFFC466B,
                0xFF3F5EFB, 0xFFFF8008, 0xFF00C9FF, 0xFFB24592).map { Color(it) }
        }

        Column(Modifier.padding(inner).fillMaxSize().padding(12.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(radial, { radial = true }, { Text("Radial") })
                FilterChip(!radial, { radial = false }, { Text("Staggered") })
            }
            Text("Items = $n", style = MaterialTheme.typography.labelMedium)
            Slider(value = count, onValueChange = { count = it }, valueRange = 3f..12f, steps = 8)

            Spacer(Modifier.height(8.dp))
            if (radial) {
                RadialLayout(
                    rotationDeg = rot,
                    modifier = Modifier.fillMaxWidth().height(380.dp)
                ) {
                    repeat(n) { i ->
                        Box(
                            Modifier.size(54.dp).clip(RoundedCornerShape(14.dp)).background(colors[i % colors.size])
                        )
                    }
                }
            } else {
                Box(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                    StaggeredLayout(columns = 3, modifier = Modifier.fillMaxWidth()) {
                        repeat(n) { i ->
                            val h = 60 + (i * 37) % 90
                            Box(
                                Modifier.padding(4.dp).fillMaxWidth().height(h.dp)
                                    .clip(RoundedCornerShape(12.dp)).background(colors[i % colors.size])
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RadialLayout(rotationDeg: Float, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Layout(content = content, modifier = modifier) { measurables, constraints ->
        val placeables = measurables.map { it.measure(constraints.copy(minWidth = 0, minHeight = 0)) }
        val w = if (constraints.hasBoundedWidth) constraints.maxWidth else 1000
        val h = if (constraints.hasBoundedHeight) constraints.maxHeight else 1000
        val radius = min(w, h) / 2f * 0.72f
        layout(w, h) {
            val cx = w / 2f
            val cy = h / 2f
            val count = placeables.size
            placeables.forEachIndexed { i, p ->
                val ang = (((rotationDeg + i * 360f / count).toDouble()) * kotlin.math.PI / 180.0)
                val x = cx + (radius * cos(ang)).toFloat() - p.width / 2f
                val y = cy + (radius * sin(ang)).toFloat() - p.height / 2f
                p.place(x.toInt(), y.toInt())
            }
        }
    }
}

@Composable
private fun StaggeredLayout(columns: Int, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Layout(content = content, modifier = modifier) { measurables, constraints ->
        val colWidth = constraints.maxWidth / columns
        val itemConstraints = constraints.copy(minWidth = 0, maxWidth = colWidth, minHeight = 0)
        val colHeights = IntArray(columns)
        val placeables = measurables.map { it.measure(itemConstraints) }
        val positions = placeables.map { p ->
            val col = colHeights.indices.minByOrNull { colHeights[it] } ?: 0
            val pos = IntOffset(col * colWidth, colHeights[col])
            colHeights[col] += p.height
            pos
        }
        val totalHeight = (colHeights.maxOrNull() ?: 0)
        layout(constraints.maxWidth, totalHeight) {
            placeables.forEachIndexed { i, p -> p.place(positions[i]) }
        }
    }
}
