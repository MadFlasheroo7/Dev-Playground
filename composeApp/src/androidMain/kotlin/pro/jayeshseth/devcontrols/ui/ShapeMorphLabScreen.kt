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
import androidx.compose.material3.Switch
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.unit.dp
import androidx.graphics.shapes.CornerRounding
import androidx.graphics.shapes.Morph
import androidx.graphics.shapes.RoundedPolygon
import androidx.graphics.shapes.star

/** graphics-shapes core is Path-agnostic, so build a Compose Path from the morph cubics. */
private fun Morph.toComposePath(progress: Float): Path {
    val path = Path()
    var first = true
    forEachCubic(progress) { c ->
        if (first) { path.moveTo(c.anchor0X, c.anchor0Y); first = false }
        path.cubicTo(c.control0X, c.control0Y, c.control1X, c.control1Y, c.anchor1X, c.anchor1Y)
    }
    path.close()
    return path
}

/**
 * Shape-morph lab (androidx.graphics.shapes). A [Morph] interpolates between two
 * [RoundedPolygon]s; we scrub the morph progress (or auto-loop it) and render the
 * resulting path. These are the Material-3-expressive morphing shapes.
 */
private data class MorphPair(val name: String, val start: RoundedPolygon, val end: RoundedPolygon)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShapeMorphLabScreen(onBack: () -> Unit) {
    val pairs = remember {
        listOf(
            MorphPair(
                "Hexagon ⇄ Star",
                RoundedPolygon(numVertices = 6, rounding = CornerRounding(0.25f)),
                RoundedPolygon.star(numVerticesPerRadius = 8, innerRadius = 0.5f, rounding = CornerRounding(0.1f))
            ),
            MorphPair(
                "Triangle ⇄ Scallop",
                RoundedPolygon(numVertices = 3, rounding = CornerRounding(0.2f)),
                RoundedPolygon.star(numVerticesPerRadius = 12, innerRadius = 0.85f, rounding = CornerRounding(0.4f))
            ),
            MorphPair(
                "Square ⇄ Blob",
                RoundedPolygon(numVertices = 4, rounding = CornerRounding(0.15f)),
                RoundedPolygon.star(numVerticesPerRadius = 5, innerRadius = 0.75f, rounding = CornerRounding(0.5f))
            ),
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Shape Morph") },
                navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }
            )
        },
        modifier = Modifier.fillMaxSize()
    ) { inner ->
        var which by remember { mutableStateOf(0) }
        var auto by remember { mutableStateOf(true) }
        var manual by remember { mutableFloatStateOf(0f) }

        val morph = remember(which) { Morph(pairs[which].start, pairs[which].end) }
        val looped by rememberInfiniteTransition(label = "m").animateFloat(
            0f, 1f, infiniteRepeatable(tween(1600, easing = LinearEasing), RepeatMode.Reverse), label = "p"
        )
        val progress = if (auto) looped else manual

        Column(Modifier.padding(inner).fillMaxSize().padding(16.dp)) {
            Box(
                Modifier.fillMaxWidth().height(340.dp).drawBehind {
                    val path = morph.toComposePath(progress)
                    val scale = (size.minDimension / 2f) * 0.82f
                    withTransform({
                        translate(size.width / 2f, size.height / 2f)
                        scale(scale, scale, pivot = Offset.Zero)
                    }) {
                        drawPath(
                            path,
                            brush = Brush.linearGradient(
                                listOf(Color(0xFF00C9FF), Color(0xFF7F00FF)),
                                start = Offset(-1f, -1f), end = Offset(1f, 1f)
                            )
                        )
                    }
                }
            )

            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                pairs.forEachIndexed { i, p -> FilterChip(which == i, { which = i }, { Text(p.name) }) }
            }
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Text("Auto-loop", Modifier.weight(1f))
                Switch(auto, { auto = it })
            }
            Slider(value = progress, onValueChange = { manual = it; auto = false }, valueRange = 0f..1f, enabled = !auto)
        }
    }
}
