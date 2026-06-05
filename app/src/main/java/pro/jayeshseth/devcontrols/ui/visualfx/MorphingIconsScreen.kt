package pro.jayeshseth.devcontrols.ui.visualfx

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin

/** Morphing icons — tap each: hamburger↔X, plus↔X (rotate), play↔pause (crossfade). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MorphingIconsScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = { TopAppBar(title = { Text("Morphing Icons") }, navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }) },
        modifier = Modifier.fillMaxSize()
    ) { inner ->
        Column(Modifier.padding(inner).fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Tap an icon to morph it.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(Modifier.fillMaxWidth().padding(top = 40.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                IconTile { p, c -> drawHamburger(p, c) }
                IconTile { p, c -> drawPlus(p, c) }
                IconTile { p, c -> drawPlayPause(p, c) }
            }
        }
    }
}

@Composable
private fun IconTile(draw: androidx.compose.ui.graphics.drawscope.DrawScope.(Float, Color) -> Unit) {
    var on by remember { mutableStateOf(false) }
    val p by animateFloatAsState(if (on) 1f else 0f, label = "icon")
    val color = MaterialTheme.colorScheme.primary
    Canvas(Modifier.size(72.dp).clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surfaceVariant).clickable { on = !on }.padding(18.dp)) {
        draw(p, color)
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawHamburger(p: Float, c: Color) {
    val w = size.width; val midY = size.height / 2; val sw = 6f
    // middle line fades
    drawLine(c.copy(alpha = 1f - p), Offset(0f, midY), Offset(w, midY), sw, StrokeCap.Round)
    val gap = size.height * 0.32f
    rotate(45f * p, pivot = Offset(w / 2, midY)) { drawLine(c, Offset(0f, midY - gap * (1 - p)), Offset(w, midY - gap * (1 - p)), sw, StrokeCap.Round) }
    rotate(-45f * p, pivot = Offset(w / 2, midY)) { drawLine(c, Offset(0f, midY + gap * (1 - p)), Offset(w, midY + gap * (1 - p)), sw, StrokeCap.Round) }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawPlus(p: Float, c: Color) {
    val w = size.width; val h = size.height; val sw = 6f
    rotate(45f * p, pivot = Offset(w / 2, h / 2)) {
        drawLine(c, Offset(w / 2, 0f), Offset(w / 2, h), sw, StrokeCap.Round)
        drawLine(c, Offset(0f, h / 2), Offset(w, h / 2), sw, StrokeCap.Round)
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawPlayPause(p: Float, c: Color) {
    val w = size.width; val h = size.height
    // pause (two bars) at p=0, play (triangle) at p=1 — crossfade
    val barW = w * 0.28f
    drawRect(c.copy(alpha = 1f - p), Offset(0f, 0f), Size(barW, h))
    drawRect(c.copy(alpha = 1f - p), Offset(w - barW, 0f), Size(barW, h))
    val tri = androidx.compose.ui.graphics.Path().apply { moveTo(w * 0.1f, 0f); lineTo(w, h / 2); lineTo(w * 0.1f, h); close() }
    drawPath(tri, c.copy(alpha = p))
}
