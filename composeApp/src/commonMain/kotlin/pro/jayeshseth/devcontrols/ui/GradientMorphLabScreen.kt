package pro.jayeshseth.devcontrols.ui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * A set of components that sit as a flat solid color at rest and subtly morph to an animated
 * gradient on hover (desktop/web) or press/click (everywhere). The gradient fades + drifts in
 * proportion to interaction, then eases back. Pure Compose, so it runs on every platform.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GradientMorphLabScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = { TopAppBar(title = { Text("Gradient Morph") }, navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }) },
        modifier = Modifier.fillMaxSize()
    ) { inner ->
        Column(
            Modifier.padding(inner).fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Text(
                "Solid at rest — hover or press to morph to a gradient.",
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            MorphButton("Get started", solid = Color(0xFF3B49B0), gradient = listOf(Color(0xFF4C6FFF), Color(0xFF22D3A6)))
            MorphButton("Delete", solid = Color(0xFF8E2F4C), gradient = listOf(Color(0xFFFF5C8A), Color(0xFFFFC857)))

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MorphChip("Design", Color(0xFF2E7D6B), listOf(Color(0xFF22FFAA), Color(0xFF4C6FFF)))
                MorphChip("Code", Color(0xFF5B3E8E), listOf(Color(0xFFAB47BC), Color(0xFF4C6FFF)))
                MorphChip("Ship", Color(0xFF8A6D1F), listOf(Color(0xFFFFC857), Color(0xFFFF5C8A)))
            }

            MorphCard(
                title = "Interactive Card",
                subtitle = "Hover me on desktop, or press on touch — the fill blooms into a gradient.",
                solid = Color(0xFF1C2138),
                gradient = listOf(Color(0xFF24306B), Color(0xFF143A33))
            )

            Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                MorphTile("A", Color(0xFF3B49B0), listOf(Color(0xFF4C6FFF), Color(0xFF22D3A6)))
                MorphTile("B", Color(0xFF2E7D6B), listOf(Color(0xFF22FFAA), Color(0xFF4C6FFF)))
                MorphTile("C", Color(0xFF8E2F4C), listOf(Color(0xFFFF5C8A), Color(0xFFFFC857)))
                MorphFab()
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

/** Interaction → 0..1 morph amount (animated), plus a continuous drift phase used while active. */
@Composable
private fun morphState(interaction: MutableInteractionSource): Pair<Float, Float> {
    val hovered by interaction.collectIsHoveredAsState()
    val pressed by interaction.collectIsPressedAsState()
    val target = if (pressed) 1f else if (hovered) 0.7f else 0f
    val amount by animateFloatAsState(target, tween(340), label = "morph")
    val phase by rememberInfiniteTransition(label = "drift").animateFloat(
        0f, 1f, infiniteRepeatable(tween(4000), RepeatMode.Reverse), label = "phase"
    )
    return amount to phase
}

/** Draws a solid fill, then a gradient on top whose alpha + angle track [amount]. */
private fun Modifier.morphFill(shape: Shape, solid: Color, gradient: List<Color>, amount: Float, phase: Float): Modifier =
    this.clip(shape).drawBehind {
        drawRect(solid)
        if (amount > 0.001f) {
            val shift = (phase - 0.5f) * size.minDimension * 0.6f * amount
            val brush = Brush.linearGradient(
                gradient,
                start = Offset(-shift, 0f),
                end = Offset(size.width + shift, size.height)
            )
            drawRect(brush, alpha = amount)
        }
    }

@Composable
private fun MorphButton(text: String, solid: Color, gradient: List<Color>) {
    val interaction = remember { MutableInteractionSource() }
    val (amount, phase) = morphState(interaction)
    val scale = 1f + 0.02f * amount
    Box(
        Modifier.fillMaxWidth().height(52.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .morphFill(RoundedCornerShape(14.dp), solid, gradient, amount, phase)
            .hoverable(interaction)
            .clickable(interaction, indication = null) {},
        contentAlignment = Alignment.Center
    ) { Text(text, color = Color.White, fontWeight = FontWeight.SemiBold) }
}

@Composable
private fun MorphChip(label: String, solid: Color, gradient: List<Color>) {
    val interaction = remember { MutableInteractionSource() }
    val (amount, phase) = morphState(interaction)
    Box(
        Modifier.height(36.dp)
            .morphFill(CircleShape, solid, gradient, amount, phase)
            .hoverable(interaction)
            .clickable(interaction, indication = null) {}
            .padding(horizontal = 18.dp),
        contentAlignment = Alignment.Center
    ) { Text(label, color = Color.White, fontSize = 14.sp) }
}

@Composable
private fun MorphCard(title: String, subtitle: String, solid: Color, gradient: List<Color>) {
    val interaction = remember { MutableInteractionSource() }
    val (amount, phase) = morphState(interaction)
    Column(
        Modifier.fillMaxWidth()
            .morphFill(RoundedCornerShape(18.dp), solid, gradient, amount, phase)
            .hoverable(interaction)
            .clickable(interaction, indication = null) {}
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Text(subtitle, color = Color.White.copy(alpha = 0.82f), fontSize = 13.sp)
    }
}

@Composable
private fun MorphTile(letter: String, solid: Color, gradient: List<Color>) {
    val interaction = remember { MutableInteractionSource() }
    val (amount, phase) = morphState(interaction)
    Box(
        Modifier.size(64.dp)
            .morphFill(RoundedCornerShape(16.dp), solid, gradient, amount, phase)
            .hoverable(interaction)
            .clickable(interaction, indication = null) {},
        contentAlignment = Alignment.Center
    ) { Text(letter, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 22.sp) }
}

@Composable
private fun MorphFab() {
    val interaction = remember { MutableInteractionSource() }
    val (amount, phase) = morphState(interaction)
    val scale = 1f + 0.06f * amount
    Box(
        Modifier.size(64.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .morphFill(CircleShape, Color(0xFF333A52), listOf(Color(0xFF4C6FFF), Color(0xFFFF5C8A)), amount, phase)
            .hoverable(interaction)
            .clickable(interaction, indication = null) {},
        contentAlignment = Alignment.Center
    ) { Text("+", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 28.sp) }
}
