package pro.jayeshseth.devcontrols.ui.threed

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Path
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/** 3D tactile components rendered with Canvas, with haptic + audio depth feedback. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Components3DScreen(onBack: () -> Unit) {
    val tactile = rememberTactile()
    Scaffold(
        topBar = { TopAppBar(title = { Text("3D Components · Canvas") }, navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }) },
        modifier = Modifier.fillMaxSize()
    ) { inner ->
        Column(Modifier.padding(inner).fillMaxSize().background(Color(0xFF202530)).padding(16.dp).verticalScroll(rememberScrollState())) {
            Text("Press, turn, flip — feel the depth (best with haptics on).", color = Color(0xFF9AA0B5), style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(20.dp))
            Label("Push button"); PushButton3D(tactile); Spacer(Modifier.height(28.dp))
            Label("Rotary knob"); Knob3D(tactile); Spacer(Modifier.height(28.dp))
            Label("Physical toggle"); Toggle3D(tactile); Spacer(Modifier.height(28.dp))
            Label("Depth slider"); DepthSlider3D(tactile); Spacer(Modifier.height(28.dp))
            Label("Liquid fill (hold)"); LiquidFillButton(tactile); Spacer(Modifier.height(20.dp))
        }
    }
}

@Composable private fun Label(t: String) = Text(t, color = Color(0xFFC8CEE0), style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(bottom = 8.dp))

@Composable
private fun PushButton3D(tactile: Tactile) {
    val press = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    Box(
        Modifier.fillMaxWidth().height(96.dp)
            .pointerInput(Unit) {
                detectTapGestures(onPress = {
                    tactile.pressDown(1f); scope.launch { press.animateTo(1f, tween(60)) }
                    tryAwaitRelease()
                    tactile.release(); scope.launch { press.animateTo(0f, spring(Spring.DampingRatioMediumBouncy)) }
                })
            }
            .drawBehind {
                val p = press.value
                val lift = (1f - p) * 16f
                // recessed well
                drawRoundRect(Color(0xFF12141C), topLeft = Offset(20f, 14f), size = Size(size.width - 40, size.height - 20), cornerRadius = cr(22f))
                // cap (raised, lowers on press)
                val capTop = 6f + p * 14f
                val face = Brush.verticalGradient(listOf(Color(0xFF4C6FFF), Color(0xFF2D3FAE)), startY = capTop, endY = size.height - lift)
                drawRoundRect(face, topLeft = Offset(24f, capTop), size = Size(size.width - 48, size.height - 24 - lift + (capTop - 6f)), cornerRadius = cr(20f))
                // top highlight
                drawRoundRect(Color(0x66FFFFFF), topLeft = Offset(30f, capTop + 4), size = Size(size.width - 60, 10f), cornerRadius = cr(6f))
            },
        contentAlignment = Alignment.Center
    ) { Text("PRESS", color = Color.White, style = MaterialTheme.typography.titleMedium) }
}

@Composable
private fun Knob3D(tactile: Tactile) {
    var angle by remember { mutableFloatStateOf(0f) }
    var lastDetent by remember { mutableFloatStateOf(0f) }
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Box(
            Modifier.size(150.dp)
                .pointerInput(Unit) {
                    detectDragGestures { change, _ ->
                        val c = Offset(size.width / 2f, size.height / 2f)
                        val a = Math.toDegrees(atan2((change.position.y - c.y).toDouble(), (change.position.x - c.x).toDouble())).toFloat()
                        angle = a
                        if (kotlin.math.abs(a - lastDetent) > 18f || kotlin.math.abs(a - lastDetent) > 342f) { lastDetent = a; tactile.detent() }
                    }
                }
                .drawBehind {
                    val c = Offset(size.width / 2, size.height / 2); val r = size.minDimension / 2 * 0.9f
                    // base ring
                    drawCircle(Color(0xFF0E1018), r + 6f, c)
                    // knob body with radial shading (light top-left)
                    drawCircle(Brush.radialGradient(listOf(Color(0xFF6A7286), Color(0xFF2C3240)), center = Offset(c.x - r * 0.35f, c.y - r * 0.35f), radius = r * 1.4f), r, c)
                    // rim highlight
                    drawCircle(Color(0x55FFFFFF), r, c, style = Stroke(2.5f))
                    // indicator notch
                    val ar = Math.toRadians(angle.toDouble())
                    drawLine(Color(0xFF4CE0B3), c, Offset(c.x + cos(ar).toFloat() * r * 0.78f, c.y + sin(ar).toFloat() * r * 0.78f), strokeWidth = 8f)
                    drawCircle(Color(0xFF1A1E28), r * 0.18f, c)
                }
        )
    }
}

@Composable
private fun Toggle3D(tactile: Tactile) {
    var on by remember { mutableStateOf(false) }
    val pos = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    Box(
        Modifier.fillMaxWidth().height(70.dp)
            .pointerInput(Unit) { detectTapGestures { on = !on; tactile.thud(); scope.launch { pos.animateTo(if (on) 1f else 0f, spring(Spring.DampingRatioMediumBouncy)) } } }
            .drawBehind {
                val h = size.height; val w = h * 2.2f; val ox = size.width / 2 - w / 2
                // recessed track
                drawRoundRect(if (on) Color(0xFF1B5E20) else Color(0xFF161922), topLeft = Offset(ox, 8f), size = Size(w, h - 16), cornerRadius = cr(h / 2))
                drawRoundRect(Color(0x33000000), topLeft = Offset(ox, 8f), size = Size(w, h - 16), cornerRadius = cr(h / 2), style = Stroke(3f))
                // 3D knob
                val kr = (h - 24f) / 2; val kx = ox + 12f + kr + pos.value * (w - 24f - kr * 2)
                drawCircle(Color(0x66000000), kr, Offset(kx, h / 2 + 3))
                drawCircle(Brush.radialGradient(listOf(Color(0xFFF2F4FA), Color(0xFFB9C0D0)), center = Offset(kx - kr * 0.3f, h / 2 - kr * 0.3f), radius = kr * 1.6f), kr, Offset(kx, h / 2))
            }
    )
}

@Composable
private fun DepthSlider3D(tactile: Tactile) {
    var v by remember { mutableFloatStateOf(0.4f) }
    var lastStep by remember { androidx.compose.runtime.mutableIntStateOf(4) }
    Box(
        Modifier.fillMaxWidth().height(64.dp)
            .pointerInput(Unit) {
                detectDragGestures { change, _ ->
                    v = (change.position.x / size.width).coerceIn(0f, 1f)
                    val step = (v * 10).roundToInt()
                    if (step != lastStep) { lastStep = step; tactile.detent() }
                }
            }
            .drawBehind {
                val cy = size.height / 2; val pad = 24f
                // recessed groove
                drawRoundRect(Color(0xFF12141C), topLeft = Offset(pad, cy - 9), size = Size(size.width - 2 * pad, 18f), cornerRadius = cr(9f))
                // fill
                val fx = pad + v * (size.width - 2 * pad)
                drawRoundRect(Brush.horizontalGradient(listOf(Color(0xFF4C6FFF), Color(0xFF8A6BFF))), topLeft = Offset(pad, cy - 9), size = Size((fx - pad).coerceAtLeast(0f), 18f), cornerRadius = cr(9f))
                // raised 3D thumb
                drawCircle(Color(0x66000000), 22f, Offset(fx, cy + 4))
                drawCircle(Brush.radialGradient(listOf(Color(0xFFF2F4FA), Color(0xFFAEB6C8)), center = Offset(fx - 7, cy - 7), radius = 34f), 20f, Offset(fx, cy))
            }
    )
}

@Composable
private fun LiquidFillButton(tactile: Tactile) {
    val fill = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    var phase by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(Unit) { while (true) { withFrameNanos { phase += 0.09f } } }
    Box(
        Modifier.fillMaxWidth().height(110.dp).clip(RoundedCornerShape(22.dp)).background(Color(0xFF12141C))
            .pointerInput(Unit) {
                detectTapGestures(onPress = {
                    tactile.pressDown(1f); scope.launch { fill.animateTo(1f, tween(480)) }
                    tryAwaitRelease()
                    tactile.release(); scope.launch { fill.animateTo(0f, tween(620)) }
                })
            }
            .drawBehind {
                val f = fill.value
                if (f <= 0.001f) return@drawBehind
                val level = size.height * (1f - f)
                fun wave(amp: Float, k: Float, ph: Float, base: Float): Path {
                    val path = Path(); path.moveTo(0f, size.height); path.lineTo(0f, base)
                    var x = 0f
                    while (x <= size.width) { path.lineTo(x, base + sin(x * k + ph) * amp); x += 8f }
                    path.lineTo(size.width, size.height); path.close(); return path
                }
                // back liquid layer
                drawPath(wave(9f, 0.022f, phase * 0.7f, level + 7f),
                    Brush.verticalGradient(listOf(Color(0xFF2BE0FF), Color(0xFF1230B0)), startY = level, endY = size.height), alpha = 0.55f)
                // front liquid layer
                drawPath(wave(14f, 0.03f, phase, level),
                    Brush.verticalGradient(listOf(Color(0xFF7CF8FF), Color(0xFF143ED0)), startY = level, endY = size.height))
                // surface shimmer
                var x = 0f
                while (x <= size.width) { val y = level + sin(x * 0.03f + phase) * 14f; drawCircle(Color(0x66FFFFFF), 1.6f, Offset(x, y)); x += 16f }
            },
        contentAlignment = Alignment.Center
    ) { Text("HOLD", color = Color.White, style = MaterialTheme.typography.titleMedium) }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.cr(r: Float) = androidx.compose.ui.geometry.CornerRadius(r, r)
