package pro.jayeshseth.devcontrols.ui.devtools

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin

enum class LayerType { RECTANGLE, CIRCLE, STAR }

data class Keyframe<T>(val frame: Int, val value: T)

class AnimProperty<T>(
    val name: String,
    val defaultValue: T,
    val interpolate: (T, T, Float) -> T
) {
    val keyframes = mutableStateListOf<Keyframe<T>>()

    fun getValueAt(frame: Int): T {
        if (keyframes.isEmpty()) return defaultValue
        val sorted = keyframes.sortedBy { it.frame }
        val exact = sorted.firstOrNull { it.frame == frame }
        if (exact != null) return exact.value

        val prev = sorted.lastOrNull { it.frame < frame }
        val next = sorted.firstOrNull { it.frame > frame }

        if (prev == null && next == null) return defaultValue
        if (prev == null) return next!!.value
        if (next == null) return prev.value

        val range = next.frame - prev.frame
        val progress = if (range == 0) 0f else (frame - prev.frame).toFloat() / range.toFloat()
        return interpolate(prev.value, next.value, progress)
    }

    fun addKeyframe(frame: Int, value: T) {
        keyframes.removeAll { it.frame == frame }
        keyframes.add(Keyframe(frame, value))
        keyframes.sortBy { it.frame }
    }

    fun removeKeyframe(frame: Int) {
        keyframes.removeAll { it.frame == frame }
    }
}

class Layer(
    val id: String,
    var name: String,
    val type: LayerType,
    val posX: AnimProperty<Float> = AnimProperty("PosX", 200f) { s, e, p -> s + p * (e - s) },
    val posY: AnimProperty<Float> = AnimProperty("PosY", 200f) { s, e, p -> s + p * (e - s) },
    val scale: AnimProperty<Float> = AnimProperty("Scale", 1.0f) { s, e, p -> s + p * (e - s) },
    val rotation: AnimProperty<Float> = AnimProperty("Rotation", 0f) { s, e, p -> s + p * (e - s) },
    val opacity: AnimProperty<Float> = AnimProperty("Opacity", 1f) { s, e, p -> s + p * (e - s) },
    val fillColor: AnimProperty<Color> = AnimProperty("FillColor", Color(0xFF6C7BFF)) { s, e, p -> lerpColor(s, e, p) },
    val strokeColor: AnimProperty<Color> = AnimProperty("StrokeColor", Color.Transparent) { s, e, p -> lerpColor(s, e, p) },
    val strokeWidth: AnimProperty<Float> = AnimProperty("StrokeWidth", 0f) { s, e, p -> s + p * (e - s) }
)

private fun lerpColor(start: Color, stop: Color, fraction: Float): Color {
    return Color(
        red = start.red + fraction * (stop.red - start.red),
        green = start.green + fraction * (stop.green - start.green),
        blue = start.blue + fraction * (stop.blue - start.blue),
        alpha = start.alpha + fraction * (stop.alpha - start.alpha)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnimationBuilderScreen(onBack: () -> Unit) {
    val layers = remember { mutableStateListOf<Layer>() }
    var selectedLayerId by remember { mutableStateOf<String?>(null) }
    val selectedLayer = layers.firstOrNull { it.id == selectedLayerId }

    var currentFrame by remember { mutableStateOf(0) }
    var isPlaying by remember { mutableStateOf(false) }
    var isLooping by remember { mutableStateOf(true) }

    val coroutineScope = rememberCoroutineScope()
    var showExportDialog by remember { mutableStateOf(false) }
    var showJsonDialog by remember { mutableStateOf(false) }
    var generatedCode by remember { mutableStateOf("") }
    var jsonPayload by remember { mutableStateOf("") }

    var showExportVideoDialog by remember { mutableStateOf(false) }
    var exportFileName by remember { mutableStateOf("animation_export") }
    var exportType by remember { mutableStateOf(ExportType.VIDEO) }
    var isExporting by remember { mutableStateOf(false) }
    var exportProgress by remember { mutableStateOf(0f) }
    var showExportSuccessDialog by remember { mutableStateOf(false) }
    var exportSuccessPath by remember { mutableStateOf("") }
    var showExportErrorDialog by remember { mutableStateOf(false) }
    var exportErrorMessage by remember { mutableStateOf("") }

    val colorPresets = listOf(
        Color(0xFF6C7BFF), Color(0xFF22FFAA), Color(0xFFFF5252), Color(0xFFFFD700),
        Color(0xFFFF7F50), Color(0xFFDA70D6), Color.White, Color.Transparent
    )

    // Playback engine
    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            while (isPlaying) {
                delay(30) // ~30 fps
                currentFrame += 1
                if (currentFrame > 100) {
                    if (isLooping) {
                        currentFrame = 0
                    } else {
                        isPlaying = false
                        currentFrame = 100
                    }
                }
            }
        }
    }

    // Auto-populate with sample layers if empty
    LaunchedEffect(Unit) {
        if (layers.isEmpty()) {
            val sampleCircle = Layer("circle_1", "Floating Ring", LayerType.CIRCLE)
            sampleCircle.posX.addKeyframe(0, 100f)
            sampleCircle.posX.addKeyframe(50, 300f)
            sampleCircle.posX.addKeyframe(100, 100f)
            sampleCircle.posY.addKeyframe(0, 150f)
            sampleCircle.posY.addKeyframe(50, 250f)
            sampleCircle.posY.addKeyframe(100, 150f)
            sampleCircle.scale.addKeyframe(0, 0.8f)
            sampleCircle.scale.addKeyframe(50, 1.4f)
            sampleCircle.scale.addKeyframe(100, 0.8f)
            sampleCircle.fillColor.addKeyframe(0, Color(0xFF6C7BFF))
            sampleCircle.fillColor.addKeyframe(50, Color(0xFF22FFAA))
            sampleCircle.fillColor.addKeyframe(100, Color(0xFF6C7BFF))

            val sampleRect = Layer("rect_1", "Spinning Block", LayerType.RECTANGLE)
            sampleRect.posX.addKeyframe(0, 200f)
            sampleRect.posY.addKeyframe(0, 200f)
            sampleRect.rotation.addKeyframe(0, 0f)
            sampleRect.rotation.addKeyframe(100, 360f)
            sampleRect.fillColor.addKeyframe(0, Color(0xFFFF5252))
            sampleRect.fillColor.addKeyframe(100, Color(0xFFFFD700))

            layers.add(sampleCircle)
            layers.add(sampleRect)
            selectedLayerId = sampleCircle.id
        }
    }

    fun applyPreset(presetName: String) {
        selectedLayer?.let { layer ->
            layer.posX.keyframes.clear()
            layer.posY.keyframes.clear()
            layer.scale.keyframes.clear()
            layer.rotation.keyframes.clear()
            layer.opacity.keyframes.clear()

            when (presetName) {
                "Bounce" -> {
                    layer.posY.addKeyframe(0, 150f)
                    layer.posY.addKeyframe(30, 280f)
                    layer.posY.addKeyframe(50, 180f)
                    layer.posY.addKeyframe(70, 280f)
                    layer.posY.addKeyframe(100, 150f)
                    layer.scale.addKeyframe(0, 1f)
                    layer.scale.addKeyframe(30, 0.8f)
                    layer.scale.addKeyframe(50, 1f)
                    layer.scale.addKeyframe(70, 0.9f)
                    layer.scale.addKeyframe(100, 1f)
                }
                "Spin & Fade" -> {
                    layer.rotation.addKeyframe(0, 0f)
                    layer.rotation.addKeyframe(100, 720f)
                    layer.opacity.addKeyframe(0, 1f)
                    layer.opacity.addKeyframe(50, 0.2f)
                    layer.opacity.addKeyframe(100, 1f)
                }
                "Pulse" -> {
                    layer.scale.addKeyframe(0, 1f)
                    layer.scale.addKeyframe(25, 1.5f)
                    layer.scale.addKeyframe(50, 1f)
                    layer.scale.addKeyframe(75, 1.5f)
                    layer.scale.addKeyframe(100, 1f)
                }
            }
        }
    }

    fun exportCode() {
        val sb = StringBuilder()
        sb.append("// Generated by Compose Animation Builder\n")
        sb.append("import androidx.compose.animation.core.*\n")
        sb.append("import androidx.compose.foundation.Canvas\n")
        sb.append("import androidx.compose.foundation.layout.size\n")
        sb.append("import androidx.compose.runtime.*\n")
        sb.append("import androidx.compose.ui.Modifier\n")
        sb.append("import androidx.compose.ui.geometry.Offset\n")
        sb.append("import androidx.compose.ui.geometry.Size\n")
        sb.append("import androidx.compose.ui.graphics.Color\n")
        sb.append("import androidx.compose.ui.graphics.Path\n")
        sb.append("import androidx.compose.ui.graphics.drawscope.Stroke\n")
        sb.append("import androidx.compose.ui.graphics.drawscope.rotate\n")
        sb.append("import androidx.compose.ui.graphics.drawscope.scale\n")
        sb.append("import androidx.compose.ui.unit.dp\n")
        sb.append("import kotlin.math.cos\n")
        sb.append("import kotlin.math.sin\n\n")

        sb.append("@Composable\n")
        sb.append("fun LiveComposeAnimation(modifier: Modifier = Modifier) {\n")
        sb.append("    val infiniteTransition = rememberInfiniteTransition(label = \"lottie_loop\")\n")
        sb.append("    val frame by infiniteTransition.animateFloat(\n")
        sb.append("        initialValue = 0f,\n")
        sb.append("        targetValue = 100f,\n")
        sb.append("        animationSpec = infiniteRepeatable(\n")
        sb.append("            animation = tween(durationMillis = 3000, easing = LinearEasing),\n")
        sb.append("            repeatMode = RepeatMode.Restart\n")
        sb.append("        ),\n")
        sb.append("        label = \"frame\"\n")
        sb.append("    )\n\n")

        sb.append("    Canvas(modifier = modifier.size(400.dp)) {\n")

        layers.forEachIndexed { idx, layer ->
            sb.append("        // Layer: ${layer.name}\n")
            sb.append("        val l${idx}_x = interpolateFloat(frame, listOf(${layer.posX.keyframes.joinToString { "${it.frame}f to ${it.value}f" }}), ${layer.posX.defaultValue}f)\n")
            sb.append("        val l${idx}_y = interpolateFloat(frame, listOf(${layer.posY.keyframes.joinToString { "${it.frame}f to ${it.value}f" }}), ${layer.posY.defaultValue}f)\n")
            sb.append("        val l${idx}_s = interpolateFloat(frame, listOf(${layer.scale.keyframes.joinToString { "${it.frame}f to ${it.value}f" }}), ${layer.scale.defaultValue}f)\n")
            sb.append("        val l${idx}_r = interpolateFloat(frame, listOf(${layer.rotation.keyframes.joinToString { "${it.frame}f to ${it.value}f" }}), ${layer.rotation.defaultValue}f)\n")
            sb.append("        val l${idx}_o = interpolateFloat(frame, listOf(${layer.opacity.keyframes.joinToString { "${it.frame}f to ${it.value}f" }}), ${layer.opacity.defaultValue}f)\n")
            
            val fillKeyframes = layer.fillColor.keyframes.joinToString { "${it.frame}f to Color(0x${it.value.value.toString(16).uppercase()})" }
            sb.append("        val l${idx}_fc = interpolateColor(frame, listOf($fillKeyframes), Color(0x${layer.fillColor.defaultValue.value.toString(16).uppercase()}))\n")

            val strokeKeyframes = layer.strokeColor.keyframes.joinToString { "${it.frame}f to Color(0x${it.value.value.toString(16).uppercase()})" }
            sb.append("        val l${idx}_sc = interpolateColor(frame, listOf($strokeKeyframes), Color(0x${layer.strokeColor.defaultValue.value.toString(16).uppercase()}))\n")
            sb.append("        val l${idx}_sw = interpolateFloat(frame, listOf(${layer.strokeWidth.keyframes.joinToString { "${it.frame}f to ${it.value}f" }}), ${layer.strokeWidth.defaultValue}f)\n\n")

            sb.append("        rotate(l${idx}_r, Offset(l${idx}_x, l${idx}_y)) {\n")
            sb.append("            scale(l${idx}_s, l${idx}_s, Offset(l${idx}_x, l${idx}_y)) {\n")
            when (layer.type) {
                LayerType.RECTANGLE -> {
                    sb.append("                drawRect(color = l${idx}_fc, topLeft = Offset(l${idx}_x - 50f, l${idx}_y - 50f), size = Size(100f, 100f), alpha = l${idx}_o)\n")
                    sb.append("                if (l${idx}_sw > 0f) {\n")
                    sb.append("                    drawRect(color = l${idx}_sc, topLeft = Offset(l${idx}_x - 50f, l${idx}_y - 50f), size = Size(100f, 100f), style = Stroke(width = l${idx}_sw), alpha = l${idx}_o)\n")
                    sb.append("                }\n")
                }
                LayerType.CIRCLE -> {
                    sb.append("                drawCircle(color = l${idx}_fc, center = Offset(l${idx}_x, l${idx}_y), radius = 50f, alpha = l${idx}_o)\n")
                    sb.append("                if (l${idx}_sw > 0f) {\n")
                    sb.append("                    drawCircle(color = l${idx}_sc, center = Offset(l${idx}_x, l${idx}_y), radius = 50f, style = Stroke(width = l${idx}_sw), alpha = l${idx}_o)\n")
                    sb.append("                }\n")
                }
                LayerType.STAR -> {
                    sb.append("                val starPath = Path().apply {\n")
                    sb.append("                    val cx = l${idx}_x; val cy = l${idx}_y; val outerRadius = 50f; val innerRadius = 20f\n")
                    sb.append("                    var angle = -Math.PI / 2\n")
                    sb.append("                    moveTo((cx + cos(angle) * outerRadius).toFloat(), (cy + sin(angle) * outerRadius).toFloat())\n")
                    sb.append("                    for (k in 0 until 10) {\n")
                    sb.append("                        val r = if (k % 2 == 0) innerRadius else outerRadius\n")
                    sb.append("                        angle += Math.PI / 5\n")
                    sb.append("                        lineTo((cx + cos(angle) * r).toFloat(), (cy + sin(angle) * r).toFloat())\n")
                    sb.append("                    }\n")
                    sb.append("                    close()\n")
                    sb.append("                }\n")
                    sb.append("                drawPath(path = starPath, color = l${idx}_fc, alpha = l${idx}_o)\n")
                    sb.append("                if (l${idx}_sw > 0f) {\n")
                    sb.append("                    drawPath(path = starPath, color = l${idx}_sc, style = Stroke(width = l${idx}_sw), alpha = l${idx}_o)\n")
                    sb.append("                }\n")
                }
            }
            sb.append("            }\n")
            sb.append("        }\n\n")
        }

        sb.append("    }\n")
        sb.append("}\n\n")

        // Append helper interpolation logic
        sb.append("private fun interpolateFloat(frame: Float, keyframes: List<Pair<Float, Float>>, default: Float): Float {\n")
        sb.append("    if (keyframes.isEmpty()) return default\n")
        sb.append("    val sorted = keyframes.sortedBy { it.first }\n")
        sb.append("    val exact = sorted.firstOrNull { it.first == frame }\n")
        sb.append("    if (exact != null) return exact.second\n")
        sb.append("    val prev = sorted.lastOrNull { it.first < frame }\n")
        sb.append("    val next = sorted.firstOrNull { it.first > frame }\n")
        sb.append("    if (prev == null && next == null) return default\n")
        sb.append("    if (prev == null) return next.second\n")
        sb.append("    if (next == null) return prev.second\n")
        sb.append("    val progress = (frame - prev.first) / (next.first - prev.first)\n")
        sb.append("    return prev.second + progress * (next.second - prev.second)\n")
        sb.append("}\n\n")

        sb.append("private fun interpolateColor(frame: Float, keyframes: List<Pair<Float, Color>>, default: Color): Color {\n")
        sb.append("    if (keyframes.isEmpty()) return default\n")
        sb.append("    val sorted = keyframes.sortedBy { it.first }\n")
        sb.append("    val exact = sorted.firstOrNull { it.first == frame }\n")
        sb.append("    if (exact != null) return exact.second\n")
        sb.append("    val prev = sorted.lastOrNull { it.first < frame }\n")
        sb.append("    val next = sorted.firstOrNull { it.first > frame }\n")
        sb.append("    if (prev == null && next == null) return default\n")
        sb.append("    if (prev == null) return next.second\n")
        sb.append("    if (next == null) return prev.second\n")
        sb.append("    val progress = (frame - prev.first) / (next.first - prev.first)\n")
        sb.append("    val s = prev.second; val e = next.second\n")
        sb.append("    return Color(red = s.red + progress * (e.red - s.red), green = s.green + progress * (e.green - s.green), blue = s.blue + progress * (e.blue - s.blue), alpha = s.alpha + progress * (e.alpha - s.alpha))\n")
        sb.append("}\n")

        generatedCode = sb.toString()
        showExportDialog = true
    }

    fun exportJson() {
        val sb = StringBuilder()
        sb.append("[\n")
        layers.forEachIndexed { i, layer ->
            sb.append("  {\n")
            sb.append("    \"name\": \"${layer.name}\",\n")
            sb.append("    \"type\": \"${layer.type.name}\",\n")
            sb.append("    \"posX\": [${layer.posX.keyframes.joinToString { "[${it.frame}, ${it.value}]" }}],\n")
            sb.append("    \"posY\": [${layer.posY.keyframes.joinToString { "[${it.frame}, ${it.value}]" }}],\n")
            sb.append("    \"scale\": [${layer.scale.keyframes.joinToString { "[${it.frame}, ${it.value}]" }}],\n")
            sb.append("    \"rotation\": [${layer.rotation.keyframes.joinToString { "[${it.frame}, ${it.value}]" }}],\n")
            sb.append("    \"opacity\": [${layer.opacity.keyframes.joinToString { "[${it.frame}, ${it.value}]" }}],\n")
            sb.append("    \"fillColor\": [${layer.fillColor.keyframes.joinToString { "[${it.frame}, ${it.value.value}]" }}]\n")
            sb.append(if (i == layers.size - 1) "  }\n" else "  },\n")
        }
        sb.append("]")
        jsonPayload = sb.toString()
        showJsonDialog = true
    }

    fun importJson(payload: String) {
        try {
            layers.clear()
            val blocks = payload.split("},")
            blocks.forEachIndexed { index, block ->
                val typeStr = Regex("\"type\":\\s*\"(.*?)\"").find(block)?.groupValues?.get(1) ?: "CIRCLE"
                val nameStr = Regex("\"name\":\\s*\"(.*?)\"").find(block)?.groupValues?.get(1) ?: "Layer $index"
                val type = LayerType.valueOf(typeStr)

                val layer = Layer("imported_$index", nameStr, type)

                fun parseFloats(propertyText: String, prop: AnimProperty<Float>) {
                    val pattern = Regex("\\[(\\d+),\\s*([\\d.]+)\\]")
                    pattern.findAll(propertyText).forEach { match ->
                        val frame = match.groupValues[1].toInt()
                        val value = match.groupValues[2].toFloat()
                        prop.addKeyframe(frame, value)
                    }
                }

                Regex("\"posX\":\\s*\\[(.*?)\\]").find(block)?.value?.let { parseFloats(it, layer.posX) }
                Regex("\"posY\":\\s*\\[(.*?)\\]").find(block)?.value?.let { parseFloats(it, layer.posY) }
                Regex("\"scale\":\\s*\\[(.*?)\\]").find(block)?.value?.let { parseFloats(it, layer.scale) }
                Regex("\"rotation\":\\s*\\[(.*?)\\]").find(block)?.value?.let { parseFloats(it, layer.rotation) }
                Regex("\"opacity\":\\s*\\[(.*?)\\]").find(block)?.value?.let { parseFloats(it, layer.opacity) }

                Regex("\"fillColor\":\\s*\\[(.*?)\\]").find(block)?.value?.let { colorText ->
                    val pattern = Regex("\\[(\\d+),\\s*(-?\\d+)\\]")
                    pattern.findAll(colorText).forEach { match ->
                        val frame = match.groupValues[1].toInt()
                        val value = match.groupValues[2].toLong()
                        layer.fillColor.addKeyframe(frame, Color(value))
                    }
                }

                layers.add(layer)
            }
            if (layers.isNotEmpty()) selectedLayerId = layers.first().id
            showJsonDialog = false
        } catch (e: Exception) {
            jsonPayload = "Failed to parse JSON: ${e.message}\nEnsure syntax matches the export format."
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Compose Animation Builder") },
                navigationIcon = { TextButton(onClick = onBack) { Text("Back") } },
                actions = {
                    Button(onClick = { exportCode() }, shape = RoundedCornerShape(8.dp)) {
                        Text("Export Code")
                    }
                    Spacer(Modifier.width(6.dp))
                    OutlinedButton(onClick = { exportJson() }, shape = RoundedCornerShape(8.dp)) {
                        Text("JSON")
                    }
                    Spacer(Modifier.width(6.dp))
                    OutlinedButton(onClick = { showExportVideoDialog = true }, shape = RoundedCornerShape(8.dp)) {
                        Text("Export Video")
                    }
                }
            )
        },
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        Row(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            // LEFT PANEL: Layers & Presets
            Column(
                modifier = Modifier
                    .width(260.dp)
                    .fillMaxHeight()
                    .border(1.dp, MaterialTheme.colorScheme.surfaceVariant)
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("Layers List", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Button(
                        onClick = {
                            val newL = Layer("rect_${layers.size + 1}", "Rectangle ${layers.size + 1}", LayerType.RECTANGLE)
                            layers.add(newL)
                            selectedLayerId = newL.id
                        },
                        contentPadding = PaddingValues(horizontal = 8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Rect", fontSize = 11.sp)
                    }
                    Button(
                        onClick = {
                            val newL = Layer("circle_${layers.size + 1}", "Circle ${layers.size + 1}", LayerType.CIRCLE)
                            layers.add(newL)
                            selectedLayerId = newL.id
                        },
                        contentPadding = PaddingValues(horizontal = 8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Circle", fontSize = 11.sp)
                    }
                    Button(
                        onClick = {
                            val newL = Layer("star_${layers.size + 1}", "Star ${layers.size + 1}", LayerType.STAR)
                            layers.add(newL)
                            selectedLayerId = newL.id
                        },
                        contentPadding = PaddingValues(horizontal = 8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Star", fontSize = 11.sp)
                    }
                }

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(layers) { layer ->
                        val isSelected = layer.id == selectedLayerId
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedLayerId = layer.id },
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = layer.name,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f),
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                                TextButton(
                                    onClick = {
                                        layers.remove(layer)
                                        if (selectedLayerId == layer.id) {
                                            selectedLayerId = layers.firstOrNull()?.id
                                        }
                                    },
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Text("Delete", color = MaterialTheme.colorScheme.error, fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }

                HorizontalDivider()

                if (selectedLayer != null) {
                    Text("Selected Presets", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            OutlinedButton(onClick = { applyPreset("Bounce") }, modifier = Modifier.weight(1f)) {
                                Text("Bounce", fontSize = 10.sp)
                            }
                            OutlinedButton(onClick = { applyPreset("Pulse") }, modifier = Modifier.weight(1f)) {
                                Text("Pulse", fontSize = 10.sp)
                            }
                        }
                        OutlinedButton(onClick = { applyPreset("Spin & Fade") }, modifier = Modifier.fillMaxWidth()) {
                            Text("Spin & Fade Loop", fontSize = 10.sp)
                        }
                    }
                }
            }

            // CENTER PANEL: Preview Canvas & Timeline
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF0F0F1A))
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                        .pointerInput(selectedLayer, currentFrame) {
                            detectDragGestures { change, dragAmount ->
                                change.consume()
                                selectedLayer?.let { layer ->
                                    val currentX = layer.posX.getValueAt(currentFrame)
                                    val currentY = layer.posY.getValueAt(currentFrame)
                                    layer.posX.addKeyframe(currentFrame, currentX + dragAmount.x)
                                    layer.posY.addKeyframe(currentFrame, currentY + dragAmount.y)
                                }
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(Modifier.fillMaxSize()) {
                        val stroke = Stroke(width = 1f)
                        val gridColor = Color(0x1F6C7BFF)
                        for (x in 0..size.width.toInt() step 40) {
                            drawLine(gridColor, Offset(x.toFloat(), 0f), Offset(x.toFloat(), size.height), strokeWidth = 1f)
                        }
                        for (y in 0..size.height.toInt() step 40) {
                            drawLine(gridColor, Offset(0f, y.toFloat()), Offset(size.width, y.toFloat()), strokeWidth = 1f)
                        }
                    }

                    Canvas(Modifier.fillMaxSize()) {
                        layers.forEach { layer ->
                            val x = layer.posX.getValueAt(currentFrame)
                            val y = layer.posY.getValueAt(currentFrame)
                            val scaleVal = layer.scale.getValueAt(currentFrame)
                            val rot = layer.rotation.getValueAt(currentFrame)
                            val op = layer.opacity.getValueAt(currentFrame)
                            val fill = layer.fillColor.getValueAt(currentFrame)
                            val strokeW = layer.strokeWidth.getValueAt(currentFrame)
                            val strokeC = layer.strokeColor.getValueAt(currentFrame)

                            rotate(rot, Offset(x, y)) {
                                scale(scaleVal, scaleVal, Offset(x, y)) {
                                    when (layer.type) {
                                        LayerType.RECTANGLE -> {
                                            drawRect(
                                                color = fill,
                                                topLeft = Offset(x - 50f, y - 50f),
                                                size = Size(100f, 100f),
                                                alpha = op
                                            )
                                            if (strokeW > 0f) {
                                                drawRect(
                                                    color = strokeC,
                                                    topLeft = Offset(x - 50f, y - 50f),
                                                    size = Size(100f, 100f),
                                                    style = Stroke(width = strokeW),
                                                    alpha = op
                                                )
                                            }
                                        }
                                        LayerType.CIRCLE -> {
                                            drawCircle(
                                                color = fill,
                                                center = Offset(x, y),
                                                radius = 50f,
                                                alpha = op
                                            )
                                            if (strokeW > 0f) {
                                                drawCircle(
                                                    color = strokeC,
                                                    center = Offset(x, y),
                                                    radius = 50f,
                                                    style = Stroke(width = strokeW),
                                                    alpha = op
                                                )
                                            }
                                        }
                                        LayerType.STAR -> {
                                            val starPath = Path().apply {
                                                val cx = x; val cy = y
                                                val outerRadius = 50f; val innerRadius = 20f
                                                var angle = -kotlin.math.PI / 2
                                                moveTo((cx + cos(angle) * outerRadius).toFloat(), (cy + sin(angle) * outerRadius).toFloat())
                                                for (k in 0 until 10) {
                                                    val r = if (k % 2 == 0) innerRadius else outerRadius
                                                    angle += kotlin.math.PI / 5
                                                    lineTo((cx + cos(angle) * r).toFloat(), (cy + sin(angle) * r).toFloat())
                                                }
                                                close()
                                            }
                                            drawPath(path = starPath, color = fill, alpha = op)
                                            if (strokeW > 0f) {
                                                drawPath(path = starPath, color = strokeC, style = Stroke(width = strokeW), alpha = op)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Text(
                        text = "Auto-Keyframe Mode Active: Drag shapes on screen to record position",
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(8.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.5f)
                    )
                }

                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                Button(onClick = { isPlaying = !isPlaying }) {
                                    Text(if (isPlaying) "⏸ Pause" else "▶ Play")
                                }
                                OutlinedButton(onClick = {
                                    isPlaying = false
                                    currentFrame = 0
                                }) {
                                    Text("Stop")
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Checkbox(checked = isLooping, onCheckedChange = { isLooping = it })
                                    Text("Loop", fontSize = 12.sp)
                                }
                            }

                            Text("Frame: $currentFrame / 100", fontWeight = FontWeight.Bold)
                        }

                        Slider(
                            value = currentFrame.toFloat(),
                            onValueChange = { currentFrame = it.toInt() },
                            valueRange = 0f..100f,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            // RIGHT PANEL: Tabbed Property Inspector & AI Generator
            Column(
                modifier = Modifier
                    .width(300.dp)
                    .fillMaxHeight()
                    .border(1.dp, MaterialTheme.colorScheme.surfaceVariant)
            ) {
                var activeRightTab by remember { mutableStateOf(0) }
                
                TabRow(selectedTabIndex = activeRightTab) {
                    Tab(
                        selected = activeRightTab == 0,
                        onClick = { activeRightTab = 0 },
                        text = { Text("Inspector", fontSize = 12.sp) }
                    )
                    Tab(
                        selected = activeRightTab == 1,
                        onClick = { activeRightTab = 1 },
                        text = { Text("AI Generator", fontSize = 12.sp) }
                    )
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (activeRightTab == 0) {
                        // Traditional Property Inspector
                        Text("Property Inspector", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                        if (selectedLayer == null) {
                            Text(
                                "Select a layer from the list to animate its keyframe properties.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            Text("Layer: ${selectedLayer.name}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            HorizontalDivider()

                            InspectorSlider(
                                name = "Position X",
                                value = selectedLayer.posX.getValueAt(currentFrame),
                                onValueChange = { selectedLayer.posX.addKeyframe(currentFrame, it) },
                                valueRange = 0f..400f,
                                hasKeyframe = selectedLayer.posX.keyframes.any { it.frame == currentFrame },
                                onToggleKeyframe = {
                                    if (selectedLayer.posX.keyframes.any { it.frame == currentFrame }) {
                                        selectedLayer.posX.removeKeyframe(currentFrame)
                                    } else {
                                        selectedLayer.posX.addKeyframe(currentFrame, selectedLayer.posX.getValueAt(currentFrame))
                                    }
                                }
                            )

                            InspectorSlider(
                                name = "Position Y",
                                value = selectedLayer.posY.getValueAt(currentFrame),
                                onValueChange = { selectedLayer.posY.addKeyframe(currentFrame, it) },
                                valueRange = 0f..400f,
                                hasKeyframe = selectedLayer.posY.keyframes.any { it.frame == currentFrame },
                                onToggleKeyframe = {
                                    if (selectedLayer.posY.keyframes.any { it.frame == currentFrame }) {
                                        selectedLayer.posY.removeKeyframe(currentFrame)
                                    } else {
                                        selectedLayer.posY.addKeyframe(currentFrame, selectedLayer.posY.getValueAt(currentFrame))
                                    }
                                }
                            )

                            InspectorSlider(
                                name = "Scale",
                                value = selectedLayer.scale.getValueAt(currentFrame),
                                onValueChange = { selectedLayer.scale.addKeyframe(currentFrame, it) },
                                valueRange = 0.1f..3.0f,
                                hasKeyframe = selectedLayer.scale.keyframes.any { it.frame == currentFrame },
                                onToggleKeyframe = {
                                    if (selectedLayer.scale.keyframes.any { it.frame == currentFrame }) {
                                        selectedLayer.scale.removeKeyframe(currentFrame)
                                    } else {
                                        selectedLayer.scale.addKeyframe(currentFrame, selectedLayer.scale.getValueAt(currentFrame))
                                    }
                                }
                            )

                            InspectorSlider(
                                name = "Rotation",
                                value = selectedLayer.rotation.getValueAt(currentFrame),
                                onValueChange = { selectedLayer.rotation.addKeyframe(currentFrame, it) },
                                valueRange = 0f..360f,
                                hasKeyframe = selectedLayer.rotation.keyframes.any { it.frame == currentFrame },
                                onToggleKeyframe = {
                                    if (selectedLayer.rotation.keyframes.any { it.frame == currentFrame }) {
                                        selectedLayer.rotation.removeKeyframe(currentFrame)
                                    } else {
                                        selectedLayer.rotation.addKeyframe(currentFrame, selectedLayer.rotation.getValueAt(currentFrame))
                                    }
                                }
                            )

                            InspectorSlider(
                                name = "Opacity",
                                value = selectedLayer.opacity.getValueAt(currentFrame),
                                onValueChange = { selectedLayer.opacity.addKeyframe(currentFrame, it) },
                                valueRange = 0f..1f,
                                hasKeyframe = selectedLayer.opacity.keyframes.any { it.frame == currentFrame },
                                onToggleKeyframe = {
                                    if (selectedLayer.opacity.keyframes.any { it.frame == currentFrame }) {
                                        selectedLayer.opacity.removeKeyframe(currentFrame)
                                    } else {
                                        selectedLayer.opacity.addKeyframe(currentFrame, selectedLayer.opacity.getValueAt(currentFrame))
                                    }
                                }
                            )

                            InspectorSlider(
                                name = "Stroke Width",
                                value = selectedLayer.strokeWidth.getValueAt(currentFrame),
                                onValueChange = { selectedLayer.strokeWidth.addKeyframe(currentFrame, it) },
                                valueRange = 0f..20f,
                                hasKeyframe = selectedLayer.strokeWidth.keyframes.any { it.frame == currentFrame },
                                onToggleKeyframe = {
                                    if (selectedLayer.strokeWidth.keyframes.any { it.frame == currentFrame }) {
                                        selectedLayer.strokeWidth.removeKeyframe(currentFrame)
                                    } else {
                                        selectedLayer.strokeWidth.addKeyframe(currentFrame, selectedLayer.strokeWidth.getValueAt(currentFrame))
                                    }
                                }
                            )

                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                    Text("Fill Color", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                    TextButton(onClick = {
                                        if (selectedLayer.fillColor.keyframes.any { it.frame == currentFrame }) {
                                            selectedLayer.fillColor.removeKeyframe(currentFrame)
                                        } else {
                                            selectedLayer.fillColor.addKeyframe(currentFrame, selectedLayer.fillColor.getValueAt(currentFrame))
                                        }
                                    }) {
                                        Text(if (selectedLayer.fillColor.keyframes.any { it.frame == currentFrame }) "◆ Keyframe" else "◇ Add Key")
                                    }
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    colorPresets.forEach { c ->
                                        Box(
                                            modifier = Modifier
                                                .size(24.dp)
                                                .clip(CircleShape)
                                                .background(if (c == Color.Transparent) Color.DarkGray else c)
                                                .border(2.dp, if (selectedLayer.fillColor.getValueAt(currentFrame) == c) MaterialTheme.colorScheme.primary else Color.Transparent, CircleShape)
                                                .clickable { selectedLayer.fillColor.addKeyframe(currentFrame, c) }
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        // AI Generator Panel
                        Text("AI Prompt Generator", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                        var aiPrompt by remember { mutableStateOf("") }
                        var geminiApiKey by remember { mutableStateOf("") }
                        var useGeminiApi by remember { mutableStateOf(false) }
                        var aiLoading by remember { mutableStateOf(false) }
                        var aiError by remember { mutableStateOf<String?>(null) }

                        OutlinedTextField(
                            value = geminiApiKey,
                            onValueChange = { geminiApiKey = it },
                            label = { Text("Gemini API Key") },
                            placeholder = { Text("AIzaSy...") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation()
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth().clickable { useGeminiApi = !useGeminiApi }
                        ) {
                            Checkbox(checked = useGeminiApi, onCheckedChange = { useGeminiApi = it })
                            Spacer(Modifier.width(8.dp))
                            Text("Use Gemini API online", style = MaterialTheme.typography.bodyMedium)
                        }

                        OutlinedTextField(
                            value = aiPrompt,
                            onValueChange = { aiPrompt = it },
                            label = { Text("Describe your animation") },
                            placeholder = { Text("e.g. A central sun with a fast orbiting planet and a slow outer planet") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 3,
                            maxLines = 5
                        )

                        if (aiError != null) {
                            Text(
                                text = aiError!!,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }

                        Button(
                            onClick = {
                                if (useGeminiApi && geminiApiKey.isBlank()) {
                                    aiError = "Please enter a Gemini API Key or use Simulation Mode."
                                    return@Button
                                }
                                if (aiPrompt.isBlank()) {
                                    aiError = "Please describe the animation first."
                                    return@Button
                                }

                                aiLoading = true
                                aiError = null
                                coroutineScope.launch {
                                    try {
                                        if (useGeminiApi) {
                                            val systemInstruction = """
                                                You are an expert animation designer. Generate a JSON array representing shape layers and their keyframe properties to construct a beautiful animation.
                                                The animation has exactly 101 frames (from 0 to 100 inclusive).
                                                Supported Layer Types: "RECTANGLE", "CIRCLE", "STAR".
                                                Your JSON output must be a valid JSON array of layer objects, conforming strictly to this structure:
                                                [
                                                  {
                                                    "name": "Layer Name",
                                                    "type": "RECTANGLE" or "CIRCLE" or "STAR",
                                                    "posX": [[frame, value], ...],
                                                    "posY": [[frame, value], ...],
                                                    "scale": [[frame, value], ...],
                                                    "rotation": [[frame, value], ...],
                                                    "opacity": [[frame, value], ...],
                                                    "fillColor": [[frame, "#AARRGGBB"], ...],
                                                    "strokeColor": [[frame, "#AARRGGBB"], ...],
                                                    "strokeWidth": [[frame, value], ...]
                                                  }
                                                ]
                                                Constraints:
                                                1. Make coordinates fit nicely inside a 400x400 canvas. PosX and PosY should generally be between 0 and 400.
                                                2. FillColor and StrokeColor must be standard Hex strings like "#FF6C7BFF" or "#FF0000".
                                                3. Scale is normally around 1.0. Opacity is between 0.0 and 1.0. Rotation is in degrees (0 to 360).
                                                4. Provide coordinates for keyframes. You do not need to output every single frame from 0 to 100, just the key changes (e.g. at 0, 25, 50, 75, 100) to keep the JSON concise. The system will interpolate linearly between keyframes.
                                                5. Return ONLY the JSON array. Do not include markdown code block syntax (like ```json) or any conversational text.
                                            """.trimIndent()

                                            val rawResponse = callGeminiApiPlatform(
                                                apiKey = geminiApiKey.trim(),
                                                prompt = aiPrompt,
                                                systemInstruction = systemInstruction
                                            )
                                            val cleanJson = extractJsonArrayFromGemini(rawResponse)
                                            importJsonLayers(cleanJson, layers)
                                            if (layers.isNotEmpty()) {
                                                selectedLayerId = layers.first().id
                                            }
                                        } else {
                                            generateSimulatedAnimation(aiPrompt, layers)
                                            if (layers.isNotEmpty()) {
                                                selectedLayerId = layers.first().id
                                            }
                                        }
                                    } catch (e: Exception) {
                                        aiError = "Error: ${e.message}"
                                    } finally {
                                        aiLoading = false
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !aiLoading
                        ) {
                            if (aiLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    strokeWidth = 2.dp
                                )
                                Spacer(Modifier.width(8.dp))
                                Text("Generating...")
                            } else {
                                Text("Generate Animation")
                            }
                        }

                        HorizontalDivider()

                        Text("Instant Simulation Presets", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Text("No API Key required. Click to procedurally generate keyframed layers:", style = MaterialTheme.typography.bodySmall)

                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = {
                                    aiPrompt = "Solar System Orbit"
                                    generateSimulatedAnimation(aiPrompt, layers)
                                    if (layers.isNotEmpty()) selectedLayerId = layers.first().id
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("🌌 Solar Orbit (Sun/Planet/Moon)")
                            }

                            OutlinedButton(
                                onClick = {
                                    aiPrompt = "Pulsing Heartbeat"
                                    generateSimulatedAnimation(aiPrompt, layers)
                                    if (layers.isNotEmpty()) selectedLayerId = layers.first().id
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("❤️ Double Heartbeat Pulse")
                            }

                            OutlinedButton(
                                onClick = {
                                    aiPrompt = "Twinkling Starfield"
                                    generateSimulatedAnimation(aiPrompt, layers)
                                    if (layers.isNotEmpty()) selectedLayerId = layers.first().id
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("✨ Twinkling Starfield")
                            }

                            OutlinedButton(
                                onClick = {
                                    aiPrompt = "Helix Loading Spinner"
                                    generateSimulatedAnimation(aiPrompt, layers)
                                    if (layers.isNotEmpty()) selectedLayerId = layers.first().id
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("🌀 Helix Loading Spinner")
                            }
                        }
                    }
                }
            }
        }
    }

    if (showExportDialog) {
        AlertDialog(
            onDismissRequest = { showExportDialog = false },
            title = { Text("Generated Compose Code") },
            text = {
                OutlinedTextField(
                    value = generatedCode,
                    onValueChange = {},
                    readOnly = true,
                    textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                    modifier = Modifier.fillMaxWidth().height(400.dp)
                )
            },
            confirmButton = {
                Button(onClick = { showExportDialog = false }) { Text("Done") }
            }
        )
    }

    if (showJsonDialog) {
        var importInput by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showJsonDialog = false },
            title = { Text("JSON Payload Configuration") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Copy/Paste the JSON payload to back up or restore the current layers.", style = MaterialTheme.typography.bodySmall)
                    OutlinedTextField(
                        value = if (importInput.isEmpty()) jsonPayload else importInput,
                        onValueChange = { importInput = it },
                        textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                        modifier = Modifier.fillMaxWidth().height(250.dp)
                    )
                }
            },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { if (importInput.isNotEmpty()) importJson(importInput) }) {
                        Text("Import JSON")
                    }
                    Button(onClick = { showJsonDialog = false }) { Text("Done") }
                }
            }
        )
    }

    if (showExportVideoDialog) {
        val platformContext = getPlatformContext()
        AlertDialog(
            onDismissRequest = { if (!isExporting) showExportVideoDialog = false },
            title = { Text("Export Animation") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("Choose filename and format to export your 100-frame animation.", style = MaterialTheme.typography.bodySmall)

                    OutlinedTextField(
                        value = exportFileName,
                        onValueChange = { exportFileName = it },
                        label = { Text("Filename") },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isExporting
                    )

                    Text("Export Format", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = exportType == ExportType.VIDEO,
                                onClick = { exportType = ExportType.VIDEO },
                                enabled = !isExporting
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = if (platformContext != null) "MP4 Video" else "Animated GIF",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = exportType == ExportType.PNG_ZIP,
                                onClick = { exportType = ExportType.PNG_ZIP },
                                enabled = !isExporting
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "PNG Frame Sequence (ZIP)",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }

                    if (isExporting) {
                        Spacer(Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { exportProgress },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text(
                            text = "Encoding: ${(exportProgress * 100).toInt()}%",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        isExporting = true
                        exportProgress = 0f
                        exportAnimationToVideo(
                            context = platformContext,
                            width = 400,
                            height = 400,
                            frameCount = 101, // 0 to 100 inclusive is 101 frames
                            fps = 30,
                            fileName = exportFileName,
                            exportType = exportType,
                            onProgress = { progress ->
                                exportProgress = progress
                            },
                            onComplete = { path ->
                                isExporting = false
                                showExportVideoDialog = false
                                exportSuccessPath = path
                                showExportSuccessDialog = true
                            },
                            onError = { err ->
                                isExporting = false
                                exportErrorMessage = err
                                showExportErrorDialog = true
                            },
                            renderFrame = { frame ->
                                // Draw background
                                drawRect(Color(0xFF0F0F1A), size = size)
                                // Draw layers
                                layers.forEach { layer ->
                                    val x = layer.posX.getValueAt(frame)
                                    val y = layer.posY.getValueAt(frame)
                                    val scaleVal = layer.scale.getValueAt(frame)
                                    val rot = layer.rotation.getValueAt(frame)
                                    val op = layer.opacity.getValueAt(frame)
                                    val fill = layer.fillColor.getValueAt(frame)
                                    val strokeW = layer.strokeWidth.getValueAt(frame)
                                    val strokeC = layer.strokeColor.getValueAt(frame)

                                    rotate(rot, Offset(x, y)) {
                                        scale(scaleVal, scaleVal, Offset(x, y)) {
                                            when (layer.type) {
                                                LayerType.RECTANGLE -> {
                                                    drawRect(
                                                        color = fill,
                                                        topLeft = Offset(x - 50f, y - 50f),
                                                        size = Size(100f, 100f),
                                                        alpha = op
                                                    )
                                                    if (strokeW > 0f) {
                                                        drawRect(
                                                            color = strokeC,
                                                            topLeft = Offset(x - 50f, y - 50f),
                                                            size = Size(100f, 100f),
                                                            style = Stroke(width = strokeW),
                                                            alpha = op
                                                        )
                                                    }
                                                }
                                                LayerType.CIRCLE -> {
                                                    drawCircle(
                                                        color = fill,
                                                        center = Offset(x, y),
                                                        radius = 50f,
                                                        alpha = op
                                                    )
                                                    if (strokeW > 0f) {
                                                        drawCircle(
                                                            color = strokeC,
                                                            center = Offset(x, y),
                                                            radius = 50f,
                                                            style = Stroke(width = strokeW),
                                                            alpha = op
                                                        )
                                                    }
                                                }
                                                LayerType.STAR -> {
                                                    val starPath = Path().apply {
                                                        val cx = x; val cy = y
                                                        val outerRadius = 50f; val innerRadius = 20f
                                                        var angle = -kotlin.math.PI / 2
                                                        moveTo((cx + cos(angle) * outerRadius).toFloat(), (cy + sin(angle) * outerRadius).toFloat())
                                                        for (k in 0 until 10) {
                                                            val r = if (k % 2 == 0) innerRadius else outerRadius
                                                            angle += kotlin.math.PI / 5
                                                            lineTo((cx + cos(angle) * r).toFloat(), (cy + sin(angle) * r).toFloat())
                                                        }
                                                        close()
                                                    }
                                                    drawPath(path = starPath, color = fill, alpha = op)
                                                    if (strokeW > 0f) {
                                                        drawPath(path = starPath, color = strokeC, style = Stroke(width = strokeW), alpha = op)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        )
                    },
                    enabled = !isExporting && exportFileName.isNotBlank()
                ) {
                    Text("Export")
                }
            },
            dismissButton = {
                if (!isExporting) {
                    TextButton(onClick = { showExportVideoDialog = false }) {
                        Text("Cancel")
                    }
                }
            }
        )
    }

    if (showExportSuccessDialog) {
        AlertDialog(
            onDismissRequest = { showExportSuccessDialog = false },
            title = { Text("Export Successful") },
            text = { Text("Your animation has been exported successfully to:\n\n$exportSuccessPath") },
            confirmButton = {
                Button(onClick = { showExportSuccessDialog = false }) { Text("OK") }
            }
        )
    }

    if (showExportErrorDialog) {
        AlertDialog(
            onDismissRequest = { showExportErrorDialog = false },
            title = { Text("Export Failed") },
            text = { Text("An error occurred during export:\n\n$exportErrorMessage") },
            confirmButton = {
                Button(onClick = { showExportErrorDialog = false }) { Text("OK") }
            }
        )
    }
}

@Composable
private fun InspectorSlider(
    name: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    hasKeyframe: Boolean,
    onToggleKeyframe: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
            IconButton(onClick = onToggleKeyframe, modifier = Modifier.size(24.dp)) {
                Text(if (hasKeyframe) "◆" else "◇", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Slider(
                value = value,
                onValueChange = onValueChange,
                valueRange = valueRange,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = ((value * 10).toInt() / 10f).toString(),
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.width(40.dp)
            )
        }
    }
}

private fun parseHexColor(hex: String): Color {
    val clean = hex.trim().replace("#", "").uppercase()
    return try {
        when (clean.length) {
            6 -> {
                val r = clean.substring(0, 2).toInt(16)
                val g = clean.substring(2, 4).toInt(16)
                val b = clean.substring(4, 6).toInt(16)
                Color(red = r / 255f, green = g / 255f, blue = b / 255f, alpha = 1f)
            }
            8 -> {
                val a = clean.substring(0, 2).toInt(16)
                val r = clean.substring(2, 4).toInt(16)
                val g = clean.substring(4, 6).toInt(16)
                val b = clean.substring(6, 8).toInt(16)
                Color(red = r / 255f, green = g / 255f, blue = b / 255f, alpha = a / 255f)
            }
            else -> Color(0xFF6C7BFF)
        }
    } catch (e: Exception) {
        Color(0xFF6C7BFF)
    }
}

private fun extractJsonArrayFromGemini(response: String): String {
    val textIndex = response.indexOf("\"text\"")
    if (textIndex == -1) return response

    val startIndex = response.indexOf("[", textIndex)
    if (startIndex == -1) return response

    var bracketCount = 0
    var endIndex = -1
    for (i in startIndex until response.length) {
        val char = response[i]
        if (char == '[') {
            bracketCount++
        } else if (char == ']') {
            bracketCount--
            if (bracketCount == 0) {
                endIndex = i
                break
            }
        }
    }

    if (endIndex == -1) return response

    val rawContent = response.substring(startIndex, endIndex + 1)
    return rawContent
        .replace("\\\"", "\"")
        .replace("\\n", "\n")
        .replace("\\r", "\r")
        .replace("\\t", "\t")
        .replace("\\\\", "\\")
}

private fun importJsonLayers(payload: String, targetLayers: MutableList<Layer>) {
    try {
        targetLayers.clear()
        val layerRegex = Regex("(?s)\\{(.*?)\\}")
        val matches = layerRegex.findAll(payload).toList()
        matches.forEachIndexed { index, matchResult ->
            val block = matchResult.groupValues[1]
            val typeStr = Regex("\"type\"\\s*:\\s*\"(.*?)\"").find(block)?.groupValues?.get(1) ?: "CIRCLE"
            val nameStr = Regex("\"name\"\\s*:\\s*\"(.*?)\"").find(block)?.groupValues?.get(1) ?: "Layer ${index + 1}"
            val type = try {
                LayerType.valueOf(typeStr.uppercase().trim())
            } catch (e: Exception) {
                LayerType.CIRCLE
            }

            val layer = Layer("ai_layer_${index}_${kotlin.random.Random.nextInt(100000)}", nameStr, type)

            fun parseFloats(propertyText: String, prop: AnimProperty<Float>) {
                val pattern = Regex("\\[\\s*(\\d+)\\s*,\\s*(-?[\\d.]+)[fF]?\\s*\\]")
                pattern.findAll(propertyText).forEach { match ->
                    val frame = match.groupValues[1].toInt()
                    val value = match.groupValues[2].toFloat()
                    prop.addKeyframe(frame, value)
                }
            }

            fun parseColors(propertyText: String, prop: AnimProperty<Color>) {
                val pattern = Regex("\\[\\s*(\\d+)\\s*,\\s*(?:\"([^\"]*)\"|#?([a-fA-F0-9]{6,8})|(-?\\d+))\\s*\\]")
                pattern.findAll(propertyText).forEach { match ->
                    val frame = match.groupValues[1].toInt()
                    val quotedHex = match.groupValues[2]
                    val rawHex = match.groupValues[3]
                    val longValStr = match.groupValues[4]

                    if (quotedHex.isNotEmpty()) {
                        prop.addKeyframe(frame, parseHexColor(quotedHex))
                    } else if (rawHex.isNotEmpty()) {
                        prop.addKeyframe(frame, parseHexColor(rawHex))
                    } else if (longValStr.isNotEmpty()) {
                        try {
                            val value = longValStr.toLong()
                            prop.addKeyframe(frame, Color(value))
                        } catch (e: Exception) {
                            // Ignore
                        }
                    }
                }
            }

            Regex("(?s)\"posX\"\\s*:\\s*\\[(.*?)\\]").find(block)?.value?.let { parseFloats(it, layer.posX) }
            Regex("(?s)\"posY\"\\s*:\\s*\\[(.*?)\\]").find(block)?.value?.let { parseFloats(it, layer.posY) }
            Regex("(?s)\"scale\"\\s*:\\s*\\[(.*?)\\]").find(block)?.value?.let { parseFloats(it, layer.scale) }
            Regex("(?s)\"rotation\"\\s*:\\s*\\[(.*?)\\]").find(block)?.value?.let { parseFloats(it, layer.rotation) }
            Regex("(?s)\"opacity\"\\s*:\\s*\\[(.*?)\\]").find(block)?.value?.let { parseFloats(it, layer.opacity) }
            Regex("(?s)\"fillColor\"\\s*:\\s*\\[(.*?)\\]").find(block)?.value?.let { parseColors(it, layer.fillColor) }
            Regex("(?s)\"strokeColor\"\\s*:\\s*\\[(.*?)\\]").find(block)?.value?.let { parseColors(it, layer.strokeColor) }
            Regex("(?s)\"strokeWidth\"\\s*:\\s*\\[(.*?)\\]").find(block)?.value?.let { parseFloats(it, layer.strokeWidth) }

            targetLayers.add(layer)
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

private fun generateSimulatedAnimation(prompt: String, targetLayers: MutableList<Layer>) {
    targetLayers.clear()
    val cleanPrompt = prompt.lowercase()
    val pi = 3.1415927f
    when {
        cleanPrompt.contains("orbit") || cleanPrompt.contains("solar") || cleanPrompt.contains("space") || cleanPrompt.contains("planet") -> {
            // Sun (Central)
            val sun = Layer("sim_sun", "Sun", LayerType.CIRCLE)
            sun.posX.addKeyframe(0, 200f)
            sun.posY.addKeyframe(0, 200f)
            sun.fillColor.addKeyframe(0, Color(0xFFFFD700))
            for (f in 0..100 step 20) {
                val s = 1.2f + 0.1f * sin(f * pi / 25f)
                sun.scale.addKeyframe(f, s)
            }
            targetLayers.add(sun)

            // Planet (Orbiting Sun)
            val planet = Layer("sim_planet", "Planet", LayerType.CIRCLE)
            planet.fillColor.addKeyframe(0, Color(0xFF22FFAA))
            planet.scale.addKeyframe(0, 0.7f)
            for (f in 0..100) {
                val angle = (f / 100f) * 2 * pi
                val px = 200f + cos(angle) * 110f
                val py = 200f + sin(angle) * 110f
                planet.posX.addKeyframe(f, px)
                planet.posY.addKeyframe(f, py)
            }
            targetLayers.add(planet)

            // Moon (Orbiting Planet)
            val moon = Layer("sim_moon", "Moon", LayerType.CIRCLE)
            moon.fillColor.addKeyframe(0, Color(0xFFDA70D6))
            moon.scale.addKeyframe(0, 0.3f)
            for (f in 0..100) {
                val planetAngle = (f / 100f) * 2 * pi
                val px = 200f + cos(planetAngle) * 110f
                val py = 200f + sin(planetAngle) * 110f

                val moonAngle = (f / 25f) * 2 * pi
                val mx = px + cos(moonAngle) * 30f
                val my = py + sin(moonAngle) * 30f

                moon.posX.addKeyframe(f, mx)
                moon.posY.addKeyframe(f, my)
            }
            targetLayers.add(moon)
        }

        cleanPrompt.contains("heart") || cleanPrompt.contains("beat") || cleanPrompt.contains("love") || cleanPrompt.contains("pulse") -> {
            // Pulsing Heart Lobe Simulation (Red Star/Circle)
            val heart = Layer("sim_heart", "Pulsing Heart", LayerType.STAR)
            heart.posX.addKeyframe(0, 200f)
            heart.posY.addKeyframe(0, 200f)
            heart.fillColor.addKeyframe(0, Color(0xFFFF5252))
            heart.fillColor.addKeyframe(50, Color(0xFFFF7F50))
            heart.fillColor.addKeyframe(100, Color(0xFFFF5252))

            heart.scale.addKeyframe(0, 1.0f)
            heart.scale.addKeyframe(15, 1.4f)
            heart.scale.addKeyframe(25, 1.1f)
            heart.scale.addKeyframe(40, 1.4f)
            heart.scale.addKeyframe(60, 1.0f)
            heart.scale.addKeyframe(100, 1.0f)

            heart.rotation.addKeyframe(0, 0f)
            heart.rotation.addKeyframe(100, 360f)

            targetLayers.add(heart)

            // Expanding pulse ring
            val ring = Layer("sim_heart_ring", "Pulse Wave", LayerType.CIRCLE)
            ring.posX.addKeyframe(0, 200f)
            ring.posY.addKeyframe(0, 200f)
            ring.fillColor.addKeyframe(0, Color.Transparent)
            ring.strokeColor.addKeyframe(0, Color(0xFFFF5252))
            ring.strokeWidth.addKeyframe(0, 3f)

            for (f in 0..100 step 10) {
                ring.scale.addKeyframe(f, 0.5f + (f / 100f) * 2.0f)
                ring.opacity.addKeyframe(f, 1f - (f / 100f))
            }
            targetLayers.add(ring)
        }

        cleanPrompt.contains("star") || cleanPrompt.contains("twinkle") || cleanPrompt.contains("night") || cleanPrompt.contains("sparkle") -> {
            // Twinkling Starfield
            val starCoords = listOf(
                Pair(100f, 100f), Pair(300f, 120f), Pair(150f, 280f),
                Pair(280f, 300f), Pair(200f, 180f), Pair(80f, 220f)
            )
            starCoords.forEachIndexed { i, coord ->
                val star = Layer("sim_star_$i", "Star ${i + 1}", LayerType.STAR)
                star.posX.addKeyframe(0, coord.first)
                star.posY.addKeyframe(0, coord.second)
                star.fillColor.addKeyframe(0, Color(0xFFFFD700))
                star.scale.addKeyframe(0, 0.4f + (i * 0.1f))

                val phaseOffset = i * 15
                for (f in 0..100 step 10) {
                    val opacityVal = 0.3f + 0.7f * sin(((f + phaseOffset) * pi / 25f))
                    val rotationVal = (f * 3.6f) + (i * 60f)
                    star.opacity.addKeyframe(f, opacityVal.coerceIn(0f, 1f))
                    star.rotation.addKeyframe(f, rotationVal)
                }
                targetLayers.add(star)
            }
        }

        else -> {
            // Helix spinner
            for (i in 0 until 5) {
                val spinner = Layer("sim_spinner_$i", "Helix Node ${i + 1}", LayerType.CIRCLE)
                val baseHue = Color(0xFF6C7BFF)
                spinner.fillColor.addKeyframe(0, baseHue)

                val phaseOffset = i * 20
                for (f in 0..100 step 5) {
                    val angle = ((f + phaseOffset) / 100f) * 2 * pi
                    val dist = 80f + 30f * sin(((f + phaseOffset) / 50f) * pi)
                    val px = 200f + cos(angle) * dist
                    val py = 200f + sin(angle) * dist

                    spinner.posX.addKeyframe(f, px)
                    spinner.posY.addKeyframe(f, py)
                    spinner.scale.addKeyframe(f, 0.6f + 0.4f * sin(((f + phaseOffset) / 25f) * pi))
                }
                targetLayers.add(spinner)
            }
        }
    }
}
