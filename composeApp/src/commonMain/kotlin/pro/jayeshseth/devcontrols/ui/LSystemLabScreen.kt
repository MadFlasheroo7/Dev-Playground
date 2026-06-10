package pro.jayeshseth.devcontrols.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin

private class LRule(val name: String, val axiom: String, val rules: Map<Char, String>, val angle: Float, val iters: Int)

private val L_PRESETS = listOf(
    LRule("Koch", "F", mapOf('F' to "F+F-F-F+F"), 90f, 4),
    LRule("Dragon", "FX", mapOf('X' to "X+YF+", 'Y' to "-FX-Y"), 90f, 11),
    LRule("Plant", "X", mapOf('X' to "F+[[X]-X]-F[-FX]+X", 'F' to "FF"), 25f, 5),
    LRule("Sierpinski", "F-G-G", mapOf('F' to "F-G+F+G-F", 'G' to "GG"), 120f, 5),
)

/** L-systems — rewrite an axiom, then turtle-draw it. Pick a preset, scale the depth. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun LSystemLabScreen(onBack: () -> Unit) {
    var preset by remember { mutableIntStateOf(2) }
    var depth by remember { mutableFloatStateOf(5f) }
    val rule = L_PRESETS[preset]
    val maxIters = depth.toInt().coerceAtMost(rule.iters + 3)
    val sentence = remember(preset, maxIters) { expand(rule, maxIters) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("L-System") }, navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }) },
        modifier = Modifier.fillMaxSize()
    ) { inner ->
        Column(Modifier.padding(inner).fillMaxSize().padding(12.dp)) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                L_PRESETS.forEachIndexed { i, r -> FilterChip(preset == i, { preset = i; depth = r.iters.toFloat() }, { Text(r.name) }) }
            }
            Text("Depth: $maxIters", style = MaterialTheme.typography.bodySmall)
            Slider(value = depth, onValueChange = { depth = it }, valueRange = 1f..8f)
            Box(Modifier.fillMaxSize().background(Color(0xFF07120A)).drawBehind {
                drawLSystem(sentence, rule.angle)
            })
        }
    }
}

private fun expand(rule: LRule, iters: Int): String {
    var s = rule.axiom
    repeat(iters) { s = s.map { rule.rules[it] ?: it.toString() }.joinToString("") }
    return s
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawLSystem(sentence: String, angleDeg: Float) {
    // bounds pass then draw — simple: start bottom center, fixed step, scale to fit via two passes
    val step = 6f
    data class State(var x: Float, var y: Float, var a: Float)
    var minX = Float.MAX_VALUE; var maxX = -Float.MAX_VALUE; var minY = Float.MAX_VALUE; var maxY = -Float.MAX_VALUE
    run {
        val st = State(0f, 0f, -90f); val stack = ArrayDeque<State>()
        for (c in sentence) when (c) {
            'F', 'G' -> { val r = ((st.a.toDouble()) * kotlin.math.PI / 180.0); st.x += (cos(r) * step).toFloat(); st.y += (sin(r) * step).toFloat()
                minX = minOf(minX, st.x); maxX = maxOf(maxX, st.x); minY = minOf(minY, st.y); maxY = maxOf(maxY, st.y) }
            '+' -> st.a += angleDeg
            '-' -> st.a -= angleDeg
            '[' -> stack.addLast(st.copy())
            ']' -> { val s = stack.removeLastOrNull(); if (s != null) { st.x = s.x; st.y = s.y; st.a = s.a } }
        }
    }
    val w = (maxX - minX).coerceAtLeast(1f); val h = (maxY - minY).coerceAtLeast(1f)
    val scale = minOf(size.width * 0.9f / w, size.height * 0.9f / h)
    val ox = (size.width - w * scale) / 2f - minX * scale
    val oy = (size.height - h * scale) / 2f - minY * scale
    val st = State(0f, 0f, -90f); val stack = ArrayDeque<State>()
    for (c in sentence) when (c) {
        'F', 'G' -> {
            val r = ((st.a.toDouble()) * kotlin.math.PI / 180.0)
            val nx = st.x + (cos(r) * step).toFloat(); val ny = st.y + (sin(r) * step).toFloat()
            drawLine(Color(0xFF7CFF9E), Offset(st.x * scale + ox, st.y * scale + oy), Offset(nx * scale + ox, ny * scale + oy), strokeWidth = 2f)
            st.x = nx; st.y = ny
        }
        '+' -> st.a += angleDeg
        '-' -> st.a -= angleDeg
        '[' -> stack.addLast(st.copy())
        ']' -> { val s = stack.removeLastOrNull(); if (s != null) { st.x = s.x; st.y = s.y; st.a = s.a } }
    }
}
