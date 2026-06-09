package pro.jayeshseth.devcontrols.ui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

private val GA = listOf(Color(0xFF4C6FFF), Color(0xFF22D3A6))
private val GB = listOf(Color(0xFF22FFAA), Color(0xFF4C6FFF))
private val GC = listOf(Color(0xFFFF5C8A), Color(0xFFFFC857))
private val GD = listOf(Color(0xFFAB47BC), Color(0xFF4C6FFF))
private val SolidA = Color(0xFF3B49B0)
private val SolidB = Color(0xFF2E7D6B)
private val SolidC = Color(0xFF8E2F4C)
private val SolidTrack = Color(0xFF2A3050)

/**
 * A component set mirroring the Components category, but each control is a flat solid color at rest
 * and subtly morphs to a drifting gradient on hover (desktop/web) or press/click/drag (everywhere).
 * Pure Compose, cross-platform. Reusable bits: [morphState] + [Modifier.morphFill].
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
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text("Solid at rest — hover or press/drag to morph to a gradient.",
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

            Section("Buttons & chips")
            MorphButton("Get started", SolidA, GA)
            MorphButton("Delete", SolidC, GC)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MorphChip("Design", SolidB, GB); MorphChip("Code", Color(0xFF5B3E8E), GD); MorphChip("Ship", Color(0xFF8A6D1F), GC)
            }

            Section("Selection")
            MorphSegmented(listOf("Day", "Week", "Month"))
            Row(horizontalArrangement = Arrangement.spacedBy(20.dp), verticalAlignment = Alignment.CenterVertically) {
                MorphSwitch(GA); MorphSwitch(GC); Text("toggle me", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
            }

            Section("Inputs")
            MorphSlider(GA)
            MorphSlider(GC)

            Section("Surfaces")
            MorphCard("Interactive Card", "Hover on desktop, press on touch — the fill blooms into a gradient.", Color(0xFF1C2138), listOf(Color(0xFF24306B), Color(0xFF143A33)))
            MorphListRow("Appearance"); MorphListRow("Notifications"); MorphListRow("Storage")
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                MorphTile("A", SolidA, GA); MorphTile("B", SolidB, GB); MorphTile("C", SolidC, GC); MorphFab()
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun Section(title: String) =
    Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 6.dp))

// ---- reusable morph primitives ----

/** Interaction → animated 0..1 morph amount, plus a continuous drift phase used while active. */
@Composable
fun morphState(interaction: MutableInteractionSource, extra: Float = 0f): Pair<Float, Float> {
    val hovered by interaction.collectIsHoveredAsState()
    val pressed by interaction.collectIsPressedAsState()
    val target = maxOf(if (pressed) 1f else if (hovered) 0.7f else 0f, extra)
    val amount by animateFloatAsState(target, tween(340), label = "morph")
    val phase by rememberInfiniteTransition(label = "drift").animateFloat(
        0f, 1f, infiniteRepeatable(tween(4000), RepeatMode.Reverse), label = "phase"
    )
    return amount to phase
}

/** Solid fill, then a gradient on top whose alpha + angle track [amount]. */
fun Modifier.morphFill(shape: Shape, solid: Color, gradient: List<Color>, amount: Float, phase: Float): Modifier =
    this.clip(shape).drawBehind {
        drawRect(solid)
        if (amount > 0.001f) {
            val shift = (phase - 0.5f) * size.minDimension * 0.6f * amount
            drawRect(Brush.linearGradient(gradient, start = Offset(-shift, 0f), end = Offset(size.width + shift, size.height)), alpha = amount)
        }
    }

// ---- components ----

@Composable
private fun MorphButton(text: String, solid: Color, gradient: List<Color>) {
    val it = remember { MutableInteractionSource() }
    val (a, p) = morphState(it)
    Box(Modifier.fillMaxWidth().height(52.dp).graphicsLayer { scaleX = 1f + 0.02f * a; scaleY = 1f + 0.02f * a }
        .morphFill(RoundedCornerShape(14.dp), solid, gradient, a, p).hoverable(it).clickable(it, null) {},
        contentAlignment = Alignment.Center) { Text(text, color = Color.White, fontWeight = FontWeight.SemiBold) }
}

@Composable
private fun MorphChip(label: String, solid: Color, gradient: List<Color>) {
    val it = remember { MutableInteractionSource() }
    val (a, p) = morphState(it)
    Box(Modifier.height(36.dp).morphFill(CircleShape, solid, gradient, a, p).hoverable(it).clickable(it, null) {}
        .padding(horizontal = 18.dp), contentAlignment = Alignment.Center) { Text(label, color = Color.White, fontSize = 14.sp) }
}

@Composable
private fun MorphSegmented(items: List<String>) {
    var sel by remember { mutableIntStateOf(0) }
    Row(Modifier.fillMaxWidth().height(44.dp).clip(RoundedCornerShape(12.dp)).background(SolidTrack)) {
        items.forEachIndexed { i, label ->
            val it = remember { MutableInteractionSource() }
            val (a, p) = morphState(it, extra = if (i == sel) 1f else 0f)
            Box(Modifier.weight(1f).fillMaxSize().padding(4.dp).morphFill(RoundedCornerShape(9.dp), if (i == sel) SolidA else Color.Transparent, GA, a, p)
                .hoverable(it).clickable(it, null) { sel = i }, contentAlignment = Alignment.Center) {
                Text(label, color = Color.White.copy(alpha = if (i == sel) 1f else 0.7f), fontSize = 14.sp)
            }
        }
    }
}

@Composable
private fun MorphSwitch(gradient: List<Color>) {
    var on by remember { mutableStateOf(false) }
    val it = remember { MutableInteractionSource() }
    val (a, p) = morphState(it, extra = if (on) 1f else 0f)
    val thumb by animateFloatAsState(if (on) 1f else 0f, tween(260), label = "thumb")
    Box(Modifier.size(58.dp, 32.dp).morphFill(CircleShape, SolidTrack, gradient, a, p).hoverable(it).clickable(it, null) { on = !on }) {
        Box(Modifier.offset { IntOffset((4.dp.toPx() + thumb * 26.dp.toPx()).roundToInt(), 4.dp.toPx().roundToInt()) }
            .size(24.dp).clip(CircleShape).background(Color.White))
    }
}

@Composable
private fun MorphSlider(gradient: List<Color>) {
    var v by remember { mutableFloatStateOf(0.45f) }
    var dragging by remember { mutableStateOf(false) }
    val it = remember { MutableInteractionSource() }
    val (a, p) = morphState(it, extra = if (dragging) 1f else 0f)
    BoxWithConstraints(Modifier.fillMaxWidth().height(36.dp).hoverable(it)) {
        val wPx = constraints.maxWidth.toFloat()
        Box(Modifier.fillMaxWidth().height(10.dp).align(Alignment.CenterStart).clip(CircleShape).background(SolidTrack))
        Box(Modifier.fillMaxWidth(v).height(10.dp).align(Alignment.CenterStart).morphFill(CircleShape, SolidA, gradient, maxOf(a, 0.35f), p))
        Box(Modifier.offset { IntOffset((v * wPx - 10.dp.toPx()).roundToInt(), 0) }
            .size(20.dp).align(Alignment.CenterStart).clip(CircleShape).background(Color.White)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { dragging = true }, onDragEnd = { dragging = false }, onDragCancel = { dragging = false }
                ) { change, drag -> change.consume(); v = (v + drag.x / wPx).coerceIn(0f, 1f) }
            })
    }
}

@Composable
private fun MorphCard(title: String, subtitle: String, solid: Color, gradient: List<Color>) {
    val it = remember { MutableInteractionSource() }
    val (a, p) = morphState(it)
    Column(Modifier.fillMaxWidth().morphFill(RoundedCornerShape(18.dp), solid, gradient, a, p).hoverable(it).clickable(it, null) {}.padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Text(subtitle, color = Color.White.copy(alpha = 0.82f), fontSize = 13.sp)
    }
}

@Composable
private fun MorphListRow(label: String) {
    val it = remember { MutableInteractionSource() }
    val (a, p) = morphState(it)
    Row(Modifier.fillMaxWidth().height(52.dp).morphFill(RoundedCornerShape(12.dp), Color(0xFF1A1F36), GA, a, p).hoverable(it).clickable(it, null) {}
        .padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = Color.White, fontSize = 15.sp, modifier = Modifier.weight(1f))
        Text("›", color = Color.White.copy(alpha = 0.7f), fontSize = 20.sp)
    }
}

@Composable
private fun MorphTile(letter: String, solid: Color, gradient: List<Color>) {
    val it = remember { MutableInteractionSource() }
    val (a, p) = morphState(it)
    Box(Modifier.size(64.dp).morphFill(RoundedCornerShape(16.dp), solid, gradient, a, p).hoverable(it).clickable(it, null) {},
        contentAlignment = Alignment.Center) { Text(letter, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 22.sp) }
}

@Composable
private fun MorphFab() {
    val it = remember { MutableInteractionSource() }
    val (a, p) = morphState(it)
    Box(Modifier.size(64.dp).graphicsLayer { scaleX = 1f + 0.06f * a; scaleY = 1f + 0.06f * a }
        .morphFill(CircleShape, Color(0xFF333A52), listOf(Color(0xFF4C6FFF), Color(0xFFFF5C8A)), a, p).hoverable(it).clickable(it, null) {},
        contentAlignment = Alignment.Center) { Text("+", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 28.sp) }
}
