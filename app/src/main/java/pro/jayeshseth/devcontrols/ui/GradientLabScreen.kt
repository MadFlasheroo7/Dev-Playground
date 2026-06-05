package pro.jayeshseth.devcontrols.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.RuntimeShader
import android.os.Build
import android.widget.Toast
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.sin

/**
 * Gradient Lab v2 — an editable, animated gradient playground.
 *
 * Everything is driven by a list of [ControlPoint]s (a normalized position + colour).
 * In **Edit** mode you drag the handles to move points, double-tap to remove, or
 * **Add point** for richer meshes (up to [MAX_POINTS]). The same points feed every
 * engine: Linear/Radial/Sweep use the point colours; Mesh (Compose) layers a soft
 * radial blob per point; Mesh (AGSL) does the same on the GPU with a noise warp.
 *
 * Motion is intentionally gentle (small-amplitude organic drift / slow swirl /
 * breathing stops) — the old full-spin/large-orbit modes looked frantic.
 * **Export** copies a ready-to-paste Compose snippet.
 */

private const val MAX_POINTS = 8

private enum class GType(val label: String) {
    Linear("Linear"), Radial("Radial"), Sweep("Sweep"),
    MeshCompose("Mesh (Compose)"), MeshShader("Mesh (AGSL)")
}

private enum class GMotion { Off, Drift, Swirl, Breathe }

private data class ControlPoint(val pos: Offset, val color: Color)

private data class Palette(val name: String, val colors: List<Color>)

private val palettes = listOf(
    Palette("Sunset", listOf(Color(0xFFFF5F6D), Color(0xFFFFC371), Color(0xFFFF9A8B), Color(0xFFB24592))),
    Palette("Ocean", listOf(Color(0xFF2193B0), Color(0xFF6DD5ED), Color(0xFF1A2980), Color(0xFF26D0CE))),
    Palette("Aurora", listOf(Color(0xFF00C9FF), Color(0xFF92FE9D), Color(0xFF7F00FF), Color(0xFFE100FF))),
    Palette("Candy", listOf(Color(0xFFFC466B), Color(0xFF3F5EFB), Color(0xFFFFB6B9), Color(0xFFFAD0C4))),
    Palette("Ember", listOf(Color(0xFF0F2027), Color(0xFF203A43), Color(0xFF2C5364), Color(0xFFFF8008))),
)

private fun defaultPoints(palette: Palette) = listOf(
    ControlPoint(Offset(0.28f, 0.28f), palette.colors[0]),
    ControlPoint(Offset(0.74f, 0.30f), palette.colors[1]),
    ControlPoint(Offset(0.30f, 0.74f), palette.colors[2]),
    ControlPoint(Offset(0.72f, 0.72f), palette.colors[3]),
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun GradientLabScreen(onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current

    var type by remember { mutableStateOf(GType.MeshCompose) }
    var motion by remember { mutableStateOf(GMotion.Drift) }
    var shimmer by remember { mutableStateOf(false) }
    var playing by remember { mutableStateOf(true) }
    var editMode by remember { mutableStateOf(false) }
    var speed by remember { mutableFloatStateOf(0.3f) }
    var selectedPalette by remember { mutableIntStateOf(0) }

    val points = remember { mutableStateListOf<ControlPoint>().apply { addAll(defaultPoints(palettes[0])) } }
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }

    // continuous time accumulator (already speed-scaled); phase wraps 0..1
    var tSec by remember { mutableFloatStateOf(0f) }
    val running = playing && !editMode
    LaunchedEffect(running, speed) {
        if (!running) return@LaunchedEffect
        var last = 0L
        while (true) {
            withFrameNanos { now ->
                if (last != 0L) tSec += (now - last) / 1_000_000_000f * speed
                last = now
            }
        }
    }
    val phase = tSec - floor(tSec)

    val meshAvailable = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
    val meshShader = remember { if (meshAvailable) RuntimeShader(MESH_AGSL) else null }
    val meshBrush = remember(meshShader) { meshShader?.let { ShaderBrush(it) } }

    fun morphTo(p: Palette) {
        scope.launch {
            val start = points.map { it.color }
            val target = points.indices.map { p.colors[it % p.colors.size] }
            animate(0f, 1f, animationSpec = tween(700)) { f, _ ->
                points.indices.forEach { points[it] = points[it].copy(color = lerp(start[it], target[it], f)) }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Gradient Lab") },
                navigationIcon = { TextButton(onClick = onBack) { Text("Back") } },
                actions = {
                    TextButton(onClick = { exportSnippet(context, type, points) }) { Text("Export") }
                }
            )
        },
        modifier = Modifier.fillMaxSize()
    ) { inner ->
        Column(Modifier.padding(inner).fillMaxSize().padding(horizontal = 12.dp)) {

            val positions = effectivePositions(points, motion, tSec, editMode)
            val radiusFactor = if (motion == GMotion.Breathe && !editMode)
                0.62f * (1f + 0.14f * sin(tSec * 2f)) else 0.62f

            var canvasMod = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
                .height(320.dp)
                .clip(RoundedCornerShape(20.dp))
                .onSizeChanged { canvasSize = it }

            if (editMode) {
                canvasMod = canvasMod
                    .pointerInput(points.size) {
                        detectDragGestures(
                            onDragStart = { },
                            onDrag = { change, _ ->
                                val sz = size
                                val idx = nearestPoint(change.position, points, sz)
                                if (idx != null) {
                                    val nx = (change.position.x / sz.width).coerceIn(0f, 1f)
                                    val ny = (change.position.y / sz.height).coerceIn(0f, 1f)
                                    points[idx] = points[idx].copy(pos = Offset(nx, ny))
                                }
                                change.consume()
                            }
                        )
                    }
                    .pointerInput(points.size) {
                        detectTapGestures(onDoubleTap = { off ->
                            val idx = nearestPoint(off, points, size)
                            if (idx != null && points.size > 2) points.removeAt(idx)
                        })
                    }
            }

            if (type == GType.MeshShader && meshBrush != null && meshShader != null) {
                Box(
                    canvasMod.drawWithCache {
                        onDrawBehind {
                            setMeshUniforms(meshShader, positions, points.map { it.color }, tSec, size.width, size.height)
                            drawRect(meshBrush)
                            if (shimmer) drawShimmer(phase)
                            if (editMode) drawHandles(positions, points.map { it.color })
                        }
                    }
                )
            } else {
                Box(
                    canvasMod.drawBehind {
                        drawGradient(type, motion, points.map { it.color }, positions, phase, radiusFactor)
                        if (shimmer) drawShimmer(phase)
                        if (editMode) drawHandles(positions, points.map { it.color })
                    }
                )
            }

            Spacer(Modifier.height(10.dp))
            Text("Engine", style = MaterialTheme.typography.labelLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GType.entries.forEach { t ->
                    if (t == GType.MeshShader && !meshAvailable) return@forEach
                    FilterChip(type == t, { type = t }, { Text(t.label) })
                }
            }

            Spacer(Modifier.height(6.dp))
            Text("Motion", style = MaterialTheme.typography.labelLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GMotion.entries.forEach { m -> FilterChip(motion == m, { motion = m }, { Text(m.name) }) }
            }

            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Edit points (drag • double-tap removes)", Modifier.weight(1f))
                Switch(editMode, { editMode = it })
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    enabled = points.size < MAX_POINTS,
                    onClick = {
                        val c = palettes[selectedPalette].colors[points.size % palettes[selectedPalette].colors.size]
                        points.add(ControlPoint(Offset(0.5f, 0.5f), c))
                    }
                ) { Text("Add point (${points.size}/$MAX_POINTS)") }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Shimmer", Modifier.padding(end = 4.dp))
                    Switch(shimmer, { shimmer = it })
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = { playing = !playing }) { Text(if (playing) "Pause" else "Play") }
                Text("Speed", Modifier.padding(end = 8.dp))
                Slider(value = speed, onValueChange = { speed = it }, valueRange = 0.05f..1.2f, modifier = Modifier.weight(1f))
            }

            Text("Palette (tap to morph)", style = MaterialTheme.typography.labelLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                palettes.forEachIndexed { i, p ->
                    PaletteChip(p, selected = selectedPalette == i) {
                        selectedPalette = i
                        morphTo(p)
                    }
                }
            }
        }
    }
}

@Composable
private fun PaletteChip(p: Palette, selected: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .padding(vertical = 4.dp)
            .size(width = 60.dp, height = 30.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Brush.horizontalGradient(p.colors))
            .border(
                if (selected) BorderStroke(2.dp, Color.White) else BorderStroke(0.dp, Color.Transparent),
                RoundedCornerShape(8.dp)
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) { Text(p.name, color = Color.White, style = MaterialTheme.typography.labelSmall) }
}

/* ----------------------------- motion ----------------------------- */

private fun effectivePositions(
    points: List<ControlPoint>, motion: GMotion, t: Float, editing: Boolean
): List<Offset> {
    if (editing || motion == GMotion.Off || motion == GMotion.Breathe) return points.map { it.pos }
    return points.mapIndexed { i, p ->
        when (motion) {
            GMotion.Drift -> {
                // gentle, small-amplitude organic wobble, per-point phase offset
                val amp = 0.05f
                Offset(
                    (p.pos.x + amp * sin(t * 0.8f + i * 1.7f)).coerceIn(0.05f, 0.95f),
                    (p.pos.y + amp * cos(t * 0.7f + i * 2.3f)).coerceIn(0.05f, 0.95f)
                )
            }
            GMotion.Swirl -> {
                // slow rotation of all points around the centre
                val ang = t * 0.5f
                val dx = p.pos.x - 0.5f
                val dy = p.pos.y - 0.5f
                Offset(
                    (0.5f + dx * cos(ang) - dy * sin(ang)).coerceIn(0.02f, 0.98f),
                    (0.5f + dx * sin(ang) + dy * cos(ang)).coerceIn(0.02f, 0.98f)
                )
            }
            else -> p.pos
        }
    }
}

/* ----------------------------- drawing ----------------------------- */

private fun DrawScope.drawGradient(
    type: GType, motion: GMotion, colors: List<Color>, positions: List<Offset>, phase: Float, radiusFactor: Float
) {
    val w = size.width
    val h = size.height
    val cx = w / 2f
    val cy = h / 2f
    when (type) {
        GType.Linear -> {
            val angle = if (motion == GMotion.Swirl) phase * 360f else 45f
            val rad = Math.toRadians(angle.toDouble())
            val dx = cos(rad).toFloat()
            val dy = sin(rad).toFloat()
            val len = max(w, h)
            if (motion == GMotion.Breathe) {
                // animate the stop positions (breathing spread)
                val n = colors.size
                val stops = colors.mapIndexed { i, c ->
                    val base = i / (n - 1f)
                    val s = (base + 0.06f * sin(phase * 2f * Math.PI.toFloat() + i)).coerceIn(0f, 1f)
                    s to c
                }.sortedBy { it.first }
                drawRect(
                    Brush.linearGradient(
                        colorStops = stops.toTypedArray(),
                        start = Offset(cx - dx * len, cy - dy * len),
                        end = Offset(cx + dx * len, cy + dy * len)
                    )
                )
            } else {
                drawRect(
                    Brush.linearGradient(
                        colors = colors,
                        start = Offset(cx - dx * len, cy - dy * len),
                        end = Offset(cx + dx * len, cy + dy * len)
                    )
                )
            }
        }
        GType.Radial -> {
            // centre follows the first control point so Edit/Drift affect it
            val c0 = positions.firstOrNull() ?: Offset(0.5f, 0.5f)
            val r = max(w, h) * (if (motion == GMotion.Breathe) radiusFactor + 0.15f else 0.85f)
            drawRect(Brush.radialGradient(colors = colors, center = Offset(c0.x * w, c0.y * h), radius = r))
        }
        GType.Sweep -> {
            val spin = if (motion == GMotion.Swirl) phase * 360f else 0f
            rotate(spin, pivot = Offset(cx, cy)) {
                drawRect(Brush.sweepGradient(colors = colors + colors.first(), center = Offset(cx, cy)))
            }
        }
        else -> drawMeshCompose(colors, positions, radiusFactor)
    }
}

private fun DrawScope.drawMeshCompose(colors: List<Color>, positions: List<Offset>, radiusFactor: Float) {
    val w = size.width
    val h = size.height
    drawRect(colors.first())
    val radius = max(w, h) * radiusFactor
    positions.forEachIndexed { i, p ->
        val c = colors[i % colors.size]
        drawRect(
            Brush.radialGradient(
                colors = listOf(c.copy(alpha = 0.85f), c.copy(alpha = 0f)),
                center = Offset(p.x * w, p.y * h),
                radius = radius
            )
        )
    }
}

private fun DrawScope.drawShimmer(phase: Float) {
    val w = size.width
    val band = w * 0.22f
    val x = phase * (w + band) - band
    drawRect(
        Brush.linearGradient(
            colors = listOf(Color.Transparent, Color.White.copy(alpha = 0.35f), Color.Transparent),
            start = Offset(x, 0f), end = Offset(x + band, 0f)
        )
    )
}

private fun DrawScope.drawHandles(positions: List<Offset>, colors: List<Color>) {
    val w = size.width
    val h = size.height
    positions.forEachIndexed { i, p ->
        val center = Offset(p.x * w, p.y * h)
        drawCircle(Color.White, radius = 13.dp.toPx(), center = center, style = Stroke(width = 3.dp.toPx()))
        drawCircle(colors[i % colors.size], radius = 10.dp.toPx(), center = center)
    }
}

private fun nearestPoint(at: Offset, points: List<ControlPoint>, size: IntSize): Int? {
    if (size.width == 0) return null
    var best = -1
    var bestD = Float.MAX_VALUE
    points.forEachIndexed { i, p ->
        val d = hypot(p.pos.x * size.width - at.x, p.pos.y * size.height - at.y)
        if (d < bestD) { bestD = d; best = i }
    }
    return if (best >= 0 && bestD < 90f) best else null
}

/* ----------------------------- AGSL mesh ----------------------------- */

private fun setMeshUniforms(
    shader: RuntimeShader, positions: List<Offset>, colors: List<Color>, t: Float, w: Float, h: Float
) {
    val count = positions.size.coerceIn(1, MAX_POINTS)
    val pts = FloatArray(MAX_POINTS * 2)
    val cols = FloatArray(MAX_POINTS * 4)
    for (i in 0 until MAX_POINTS) {
        val j = i.coerceAtMost(count - 1)
        pts[i * 2] = positions[j].x
        pts[i * 2 + 1] = positions[j].y
        val c = colors[j % colors.size]
        cols[i * 4] = c.red; cols[i * 4 + 1] = c.green; cols[i * 4 + 2] = c.blue; cols[i * 4 + 3] = c.alpha
    }
    shader.setFloatUniform("iResolution", w, h)
    shader.setFloatUniform("iTime", t)
    shader.setIntUniform("uCount", count)
    shader.setFloatUniform("uPts", pts)
    shader.setFloatUniform("uCols", cols)
}

private const val MESH_AGSL = """
uniform float2 iResolution;
uniform float iTime;
uniform int uCount;
uniform float2 uPts[8];
uniform float4 uCols[8];
half4 main(float2 fragCoord) {
    float2 uv = fragCoord / iResolution;
    // gentle domain-warp for organic blob edges
    uv += 0.025 * float2(sin(uv.y * 7.0 + iTime), cos(uv.x * 7.0 + iTime * 0.9));
    float4 acc = float4(0.0);
    float wsum = 0.0;
    for (int i = 0; i < 8; i++) {
        if (i >= uCount) { break; }
        float d = distance(uv, uPts[i]);
        float w = 1.0 / (d * d + 0.03);
        acc += uCols[i] * w;
        wsum += w;
    }
    return half4(acc / wsum);
}
"""

/* ----------------------------- export ----------------------------- */

private fun hex(c: Color): String = "0x%08X".format(c.toArgb())

private fun exportSnippet(context: Context, type: GType, points: List<ControlPoint>) {
    val colorList = points.joinToString(", ") { "Color(${hex(it.color)})" }
    val snippet = when (type) {
        GType.Linear -> "Brush.linearGradient(listOf($colorList))"
        GType.Radial -> "Brush.radialGradient(listOf($colorList))"
        GType.Sweep -> "Brush.sweepGradient(listOf($colorList))"
        else -> buildString {
            appendLine("// Mesh gradient — layer one radial blob per control point over a base fill:")
            appendLine("val points = listOf(")
            points.forEach { appendLine("    ControlPoint(Offset(${"%.2f".format(it.pos.x)}f, ${"%.2f".format(it.pos.y)}f), Color(${hex(it.color)})),") }
            appendLine(")")
            append("// drawRect(points.first().color); points.forEach { drawRect(Brush.radialGradient(listOf(it.color.copy(alpha=0.85f), it.color.copy(alpha=0f)), center = it.pos * size, radius = maxDimension * 0.62f)) }")
        }
    }
    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    cm.setPrimaryClip(ClipData.newPlainText("gradient", snippet))
    Toast.makeText(context, "Copied ${type.label} snippet", Toast.LENGTH_SHORT).show()
}
