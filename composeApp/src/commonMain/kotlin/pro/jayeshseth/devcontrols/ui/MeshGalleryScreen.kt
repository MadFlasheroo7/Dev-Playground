package pro.jayeshseth.devcontrols.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.max

/**
 * Mesh-gradient component gallery. A reusable [meshBackground] modifier paints a soft
 * 4-blob mesh gradient (the same look as the Gradient Lab's Compose mesh) behind a set
 * of fully custom components — buttons, card, text field, switch, slider, chips,
 * checkbox, FAB, and progress — so the whole UI kit shares one cohesive mesh aesthetic.
 */
private val MeshColors = listOf(Color(0xFF7F00FF), Color(0xFF00C9FF), Color(0xFFFF5F6D), Color(0xFFFFC371))
private val MeshPoints = listOf(Offset(0.2f, 0.25f), Offset(0.85f, 0.2f), Offset(0.25f, 0.85f), Offset(0.82f, 0.8f))

private fun Modifier.meshBackground(shape: Shape = RectangleShape, colors: List<Color> = MeshColors): Modifier =
    this.clip(shape).drawBehind {
        drawRect(colors[0])
        val r = max(size.width, size.height) * 0.75f
        colors.forEachIndexed { i, c ->
            val p = MeshPoints[i % MeshPoints.size]
            drawRect(
                Brush.radialGradient(
                    listOf(c.copy(alpha = 0.92f), c.copy(alpha = 0f)),
                    center = Offset(p.x * size.width, p.y * size.height), radius = r
                )
            )
        }
    }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MeshGalleryScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mesh Components") },
                navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }
            )
        },
        modifier = Modifier.fillMaxSize()
    ) { inner ->
        Column(
            Modifier.padding(inner).fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)
        ) {
            // headline
            Box(Modifier.fillMaxWidth().height(120.dp).meshBackground(RoundedCornerShape(24.dp)), contentAlignment = Alignment.Center) {
                Text("Mesh UI Kit", color = Color.White, fontSize = 34.sp, fontWeight = FontWeight.Black)
            }
            Spacer(Modifier.height(20.dp))

            Section("Buttons")
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MeshButton("Filled", Modifier.weight(1f)) {}
                MeshOutlinedButton("Outlined", Modifier.weight(1f)) {}
            }

            Section("Card")
            MeshCard()

            Section("Text field")
            MeshTextField()

            Section("Switch & Checkbox")
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                MeshSwitch()
                MeshCheckbox()
            }

            Section("Slider")
            MeshSlider()

            Section("Chips")
            MeshChips()

            Section("FAB & Progress")
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                MeshFab()
                MeshCircularProgress()
                Column(Modifier.weight(1f)) { MeshLinearProgress() }
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun Section(title: String) {
    Spacer(Modifier.height(20.dp))
    Text(title, color = Color(0xFFB0B0B0), fontWeight = FontWeight.Bold, fontSize = 13.sp)
    Spacer(Modifier.height(8.dp))
}

@Composable
private fun MeshButton(label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    Box(
        modifier
            .height(52.dp)
            .graphicsLayer { val s = if (pressed) 0.96f else 1f; scaleX = s; scaleY = s }
            .meshBackground(RoundedCornerShape(26.dp))
            .clickable(interactionSource = interaction, indication = null) { onClick() },
        contentAlignment = Alignment.Center
    ) { Text(label, color = Color.White, fontWeight = FontWeight.Bold) }
}

@Composable
private fun MeshOutlinedButton(label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier
            .height(52.dp)
            .clip(RoundedCornerShape(26.dp))
            .border(2.dp, Brush.linearGradient(MeshColors), RoundedCornerShape(26.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) { Text(label, color = Color.White, fontWeight = FontWeight.Bold) }
}

@Composable
private fun MeshCard() {
    Box(Modifier.fillMaxWidth().height(140.dp).meshBackground(RoundedCornerShape(20.dp))) {
        Column(Modifier.padding(18.dp)) {
            Text("Mesh card", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text("Soft multi-point gradient behind the content.", color = Color.White.copy(alpha = 0.9f))
        }
    }
}

@Composable
private fun MeshTextField() {
    var value by remember { mutableStateOf(TextFieldValue("")) }
    var focused by remember { mutableStateOf(false) }
    Column {
        BasicTextField(
            value = value,
            onValueChange = { value = it },
            textStyle = TextStyle(color = Color.White, fontSize = 16.sp),
            cursorBrush = Brush.linearGradient(MeshColors),
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color.White.copy(alpha = 0.06f))
                .onFocusBorder { focused = it }
                .padding(16.dp),
            decorationBox = { inner ->
                if (value.text.isEmpty()) Text("Type something…", color = Color.White.copy(alpha = 0.4f))
                inner()
            }
        )
        Box(
            Modifier
                .padding(top = 4.dp)
                .fillMaxWidth()
                .height(if (focused) 3.dp else 1.dp)
                .meshBackground()
        )
    }
}

private fun Modifier.onFocusBorder(onFocus: (Boolean) -> Unit): Modifier =
    this.onFocusChanged { onFocus(it.isFocused) }

@Composable
private fun MeshSwitch() {
    var on by remember { mutableStateOf(true) }
    val thumbOffset by animateDpAsState(if (on) 26.dp else 2.dp, label = "thumb")
    Box(
        Modifier
            .size(width = 58.dp, height = 32.dp)
            .clip(RoundedCornerShape(16.dp))
            .then(if (on) Modifier.meshBackground(RoundedCornerShape(16.dp)) else Modifier.background(Color(0xFF3A3A3A)))
            .clickable { on = !on },
        contentAlignment = Alignment.CenterStart
    ) {
        Box(Modifier.padding(start = thumbOffset).size(28.dp).clip(CircleShape).background(Color.White))
    }
}

@Composable
private fun MeshCheckbox() {
    var checked by remember { mutableStateOf(true) }
    Box(
        Modifier
            .size(32.dp)
            .clip(RoundedCornerShape(8.dp))
            .then(if (checked) Modifier.meshBackground(RoundedCornerShape(8.dp)) else Modifier.border(2.dp, Color(0xFF888888), RoundedCornerShape(8.dp)))
            .clickable { checked = !checked },
        contentAlignment = Alignment.Center
    ) { if (checked) Text("✓", color = Color.White, fontWeight = FontWeight.Black) }
}

@Composable
private fun MeshSlider() {
    var value by remember { mutableFloatStateOf(0.4f) }
    var trackW by remember { mutableFloatStateOf(1f) }
    Box(
        Modifier
            .fillMaxWidth()
            .height(36.dp)
            .pointerInput(Unit) {
                detectHorizontalDragGestures { change, _ ->
                    value = (change.position.x / size.width).coerceIn(0f, 1f); change.consume()
                }
            },
        contentAlignment = Alignment.CenterStart
    ) {
        // track
        Box(Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)).background(Color(0xFF333333)))
        // fill
        Box(Modifier.fillMaxWidth(value).height(8.dp).clip(RoundedCornerShape(4.dp)).meshBackground(RoundedCornerShape(4.dp)))
        // thumb
        Box(Modifier.fillMaxWidth(value), contentAlignment = Alignment.CenterEnd) {
            Box(Modifier.size(24.dp).clip(CircleShape).meshBackground(CircleShape).border(2.dp, Color.White, CircleShape))
        }
    }
}

@Composable
private fun MeshChips() {
    val labels = listOf("Design", "Motion", "Shaders", "Mesh")
    var sel by remember { mutableStateOf(0) }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        labels.forEachIndexed { i, l ->
            val selected = i == sel
            Box(
                Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .then(if (selected) Modifier.meshBackground(RoundedCornerShape(20.dp)) else Modifier.border(1.dp, Color(0xFF666666), RoundedCornerShape(20.dp)))
                    .clickable { sel = i }
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) { Text(l, color = if (selected) Color.White else Color(0xFFBBBBBB)) }
        }
    }
}

@Composable
private fun MeshFab() {
    Box(Modifier.size(56.dp).meshBackground(CircleShape).clickable { }, contentAlignment = Alignment.Center) {
        Text("+", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun MeshLinearProgress() {
    val p by rememberInfiniteTransition(label = "lp").animateFloat(
        0.1f, 1f, infiniteRepeatable(tween(1600, easing = LinearEasing), RepeatMode.Reverse), label = "v"
    )
    Box(Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(5.dp)).background(Color(0xFF333333))) {
        Box(Modifier.fillMaxWidth(p).height(10.dp).clip(RoundedCornerShape(5.dp)).meshBackground(RoundedCornerShape(5.dp)))
    }
}

@Composable
private fun MeshCircularProgress() {
    val angle by rememberInfiniteTransition(label = "cp").animateFloat(
        0f, 360f, infiniteRepeatable(tween(1100, easing = LinearEasing), RepeatMode.Restart), label = "a"
    )
    Box(
        Modifier.size(48.dp).drawBehind {
            drawArc(
                brush = Brush.sweepGradient(MeshColors + MeshColors.first()),
                startAngle = angle, sweepAngle = 280f, useCenter = false,
                style = Stroke(width = 8f, cap = StrokeCap.Round)
            )
        }
    )
}
