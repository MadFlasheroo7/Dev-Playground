package pro.jayeshseth.devcontrols.ui.visualfx

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import pro.jayeshseth.devcontrols.ui.PathSegment
import pro.jayeshseth.devcontrols.ui.parseSvgPath
import pro.jayeshseth.devcontrols.ui.buildComposePath
import kotlin.random.Random

// Represents an interactive vector particle
private class VectorParticle(
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    val tx: Float,
    val ty: Float,
    val color: Color
)

// Dynamic Color Palettes
private enum class VectorPalette(val label: String, val colors: List<Color>) {
    NEON_CYAN("Neon Cyan", listOf(Color(0xFF00E5FF), Color(0xFF00B0FF), Color(0xFF2979FF), Color(0xFF00E5FF))),
    CYBERPUNK("Cyberpunk Pink", listOf(Color(0xFFFF007F), Color(0xFFD500F9), Color(0xFF7C4DFF), Color(0xFF00E5FF))),
    MATRIX("Matrix Green", listOf(Color(0xFF00FF00), Color(0xFF00E676), Color(0xFF1DE9B6), Color(0xFF00FF00))),
    FIRE_GLOW("Fire Glow", listOf(Color(0xFFFF3D00), Color(0xFFFF9100), Color(0xFFFFEA00), Color(0xFFFF3D00))),
    RAINBOW("Rainbow", listOf(Color(0xFFFF1744), Color(0xFFFF9100), Color(0xFFFFEA00), Color(0xFF00E676), Color(0xFF2979FF), Color(0xFF651FFF)))
}

// Return Physics Animation Mode
private enum class VectorReturnMode(val label: String) {
    SPRING("Spring (Bounce)"),
    TWEEN("Tween (Ease-out)")
}

// Particle Entrance Animation Style
private enum class VectorEntrancePattern(val label: String) {
    SCATTER("Scatter"),
    BURST("Center Burst"),
    VORTEX("Vortex Spiral"),
    GRAVITY("Gravity Drop"),
    FLY_IN("Side Fly-In")
}

private data class VectorPreset(val label: String, val pathData: String)

private val PRESETS = listOf(
    VectorPreset("Heart ❤️", "M 150 240 C 60 180, 40 80, 150 80 C 260 80, 240 180, 150 240 Z"),
    VectorPreset("Star ⭐", "M 150 20 L 190 110 L 280 110 L 210 170 L 240 260 L 150 200 L 60 260 L 90 170 L 20 110 L 110 110 Z"),
    VectorPreset("Infinity ♾️", "M 150 150 C 180 110, 230 110, 250 150 C 270 190, 220 190, 150 150 C 80 110, 30 110, 50 150 C 70 190, 120 190, 150 150 Z"),
    VectorPreset("Gear ⚙️", "M 120 80 L 140 80 L 145 60 L 155 60 L 160 80 L 180 80 L 195 65 L 205 75 L 190 95 L 200 115 L 220 110 L 225 125 L 205 135 L 205 155 L 225 165 L 220 180 L 200 175 L 190 195 L 205 215 L 195 225 L 175 210 L 155 220 L 160 240 L 140 240 L 135 220 L 115 210 L 95 225 L 85 215 L 100 195 L 90 175 L 70 180 L 65 165 L 85 155 L 85 135 L 65 125 L 70 110 L 90 115 L 100 95 L 85 75 L 95 65 L 115 80 Z"),
    VectorPreset("Rocket 🚀", "M 150 50 C 120 100, 120 160, 120 200 L 100 220 L 110 230 L 130 220 L 130 240 L 150 250 L 170 240 L 170 220 L 190 230 L 200 220 L 180 200 C 180 160, 180 100, 150 50 Z"),
    VectorPreset("Android 🤖", "M 110 100 C 110 50, 190 50, 190 100 L 200 100 L 200 170 C 200 185, 175 185, 175 170 V 190 C 175 205, 160 205, 160 190 V 170 H 140 V 190 C 140 205, 125 205, 125 190 V 170 C 125 185, 100 185, 100 170 L 100 100 Z")
)

private fun formatDecimal(value: Float, decimals: Int): String {
    var multiplier = 1f
    repeat(decimals) { multiplier *= 10f }
    val rounded = kotlin.math.round(value * multiplier) / multiplier
    return rounded.toString()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParticleVectorScreen(onBack: () -> Unit) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val density = LocalDensity.current

    // Preset & SVG State
    var selectedPresetIdx by remember { mutableStateOf(0) }
    var pathInput by remember { mutableStateOf(PRESETS[selectedPresetIdx].pathData) }
    var debouncedPathInput by remember { mutableStateOf(PRESETS[selectedPresetIdx].pathData) }

    // Visual Controls
    var isFillMode by remember { mutableStateOf(false) }
    var strokeWidth by remember { mutableFloatStateOf(4f) }
    var densityStep by remember { mutableIntStateOf(5) } // pixel scan step
    var particleSize by remember { mutableFloatStateOf(2.5f) }
    var returnSpeed by remember { mutableFloatStateOf(0.08f) }
    var friction by remember { mutableFloatStateOf(0.06f) }
    var interactionStrength by remember { mutableFloatStateOf(4.5f) }
    var interactionRadius by remember { mutableFloatStateOf(120f) }
    var isRepulsionMode by remember { mutableStateOf(true) }
    var enableMesh by remember { mutableStateOf(false) }
    var selectedPalette by remember { mutableStateOf(VectorPalette.NEON_CYAN) }

    var returnMode by remember { mutableStateOf(VectorReturnMode.SPRING) }
    var entrancePattern by remember { mutableStateOf(VectorEntrancePattern.SCATTER) }
    var returnSpeedScale by remember { mutableFloatStateOf(1.0f) }
    var interactionSpeedScale by remember { mutableFloatStateOf(1.0f) }

    // Simulation Engine State
    var size by remember { mutableStateOf(IntSize.Zero) }
    var particles by remember { mutableStateOf<List<VectorParticle>>(emptyList()) }
    var tick by remember { mutableIntStateOf(0) }

    // Pointer position
    var pointerPos by remember { mutableStateOf<Offset?>(null) }
    var forceRegenTrigger by remember { mutableIntStateOf(0) }

    // Debounce custom path text changes to prevent stuttering while typing
    LaunchedEffect(pathInput) {
        delay(350)
        debouncedPathInput = pathInput
    }

    // Rasterization and Particle Spawning
    LaunchedEffect(size, debouncedPathInput, isFillMode, strokeWidth, densityStep, selectedPalette, entrancePattern, returnSpeedScale, forceRegenTrigger) {
        if (size == IntSize.Zero || debouncedPathInput.isEmpty()) {
            particles = emptyList()
            return@LaunchedEffect
        }

        val w = size.width
        val h = size.height

        val bmpW = w
        val bmpH = (h * 0.45f).toInt().coerceAtLeast(1)
        val imageBitmap = ImageBitmap(bmpW, bmpH)
        val canvas = Canvas(imageBitmap)

        // Parse path segments and build the Compose Path
        val tempLogs = mutableListOf<String>()
        val segments = parseSvgPath(debouncedPathInput, tempLogs)
        val composePath = buildComposePath(segments)
        val bounds = composePath.getBounds()

        val canvasDrawScope = CanvasDrawScope()
        canvasDrawScope.draw(
            density = density,
            layoutDirection = LayoutDirection.Ltr,
            canvas = canvas,
            size = Size(bmpW.toFloat(), bmpH.toFloat())
        ) {
            if (bounds.width > 0f && bounds.height > 0f) {
                // Calculate scale and translation to perfectly fit path inside canvas viewport (80% size)
                val targetW = bmpW * 0.8f
                val targetH = bmpH * 0.8f
                val scaleX = targetW / bounds.width
                val scaleY = targetH / bounds.height
                val scale = minOf(scaleX, scaleY).coerceIn(0.1f, 100f)

                val dx = (bmpW - bounds.width * scale) / 2f - bounds.left * scale
                val dy = (bmpH - bounds.height * scale) / 2f - bounds.top * scale

                withTransform({
                    translate(dx, dy)
                    scale(scale, scale, Offset.Zero)
                }) {
                    if (isFillMode) {
                        drawPath(
                            path = composePath,
                            color = Color.White
                        )
                    } else {
                        drawPath(
                            path = composePath,
                            color = Color.White,
                            style = Stroke(
                                width = strokeWidth.dp.toPx(),
                                cap = StrokeCap.Round,
                                join = StrokeJoin.Round
                            )
                        )
                    }
                }
            }
        }

        val pixelMap = imageBitmap.toPixelMap()
        val list = ArrayList<VectorParticle>()
        val oy = h * 0.22f // offset path vertically to center in interactive viewport
        val paletteColors = selectedPalette.colors

        var y = 0
        while (y < bmpH) {
            var x = 0
            while (x < bmpW) {
                val alpha = pixelMap[x, y].alpha
                if (alpha > 0.35f) {
                    val colorIdx = (x / (bmpW.toFloat() / paletteColors.size).coerceAtLeast(1f)).toInt().coerceIn(0, paletteColors.size - 1)
                    val color = paletteColors[colorIdx]

                    val txPos = x.toFloat()
                    val tyPos = y.toFloat() + oy

                    var startX = txPos
                    var startY = tyPos
                    var initVx = 0f
                    var initVy = 0f

                    when (entrancePattern) {
                        VectorEntrancePattern.SCATTER -> {
                            startX = Random.nextFloat() * w
                            startY = Random.nextFloat() * h
                        }
                        VectorEntrancePattern.BURST -> {
                            startX = w / 2f
                            startY = h / 2f
                            val angle = Random.nextFloat() * 6.28318f
                            val spd = (Random.nextFloat() * 8f + 2f) * returnSpeedScale
                            initVx = kotlin.math.cos(angle) * spd
                            initVy = kotlin.math.sin(angle) * spd
                        }
                        VectorEntrancePattern.VORTEX -> {
                            val angle = Random.nextFloat() * 6.28318f
                            val radius = maxOf(w, h) * 0.5f
                            startX = w / 2f + radius * kotlin.math.cos(angle)
                            startY = h / 2f + radius * kotlin.math.sin(angle)
                            initVx = -kotlin.math.sin(angle) * 8f * returnSpeedScale
                            initVy = kotlin.math.cos(angle) * 8f * returnSpeedScale
                        }
                        VectorEntrancePattern.GRAVITY -> {
                            startX = txPos
                            startY = -Random.nextFloat() * 200f
                            initVx = 0f
                            initVy = (Random.nextFloat() * 12f + 6f) * returnSpeedScale
                        }
                        VectorEntrancePattern.FLY_IN -> {
                            startX = -100f
                            startY = tyPos + (Random.nextFloat() - 0.5f) * 150f
                            initVx = (Random.nextFloat() * 16f + 10f) * returnSpeedScale
                            initVy = 0f
                        }
                    }

                    list.add(
                        VectorParticle(
                            x = startX,
                            y = startY,
                            vx = initVx,
                            vy = initVy,
                            tx = txPos,
                            ty = tyPos,
                            color = color
                        )
                    )
                }
                x += densityStep
            }
            y += densityStep
        }

        particles = list
    }

    // High performance updates frame loop
    LaunchedEffect(Unit) {
        while (true) {
            withFrameNanos {
                val currentParticles = particles
                if (currentParticles.isNotEmpty()) {
                    val px = pointerPos?.x
                    val py = pointerPos?.y
                    val radius = interactionRadius
                    val radiusSq = radius * radius
                    val strength = interactionStrength
                    val speed = returnSpeed * returnSpeedScale
                    val f = friction
                    val repMode = isRepulsionMode
                    val mode = returnMode
                    val interactSpeed = interactionSpeedScale

                    for (p in currentParticles) {
                        if (mode == VectorReturnMode.SPRING) {
                            // Target attraction
                            val dx = p.tx - p.x
                            val dy = p.ty - p.y
                            p.vx += dx * speed
                            p.vy += dy * speed

                            // Force field physics
                            if (px != null && py != null) {
                                val pdx = p.x - px
                                val pdy = p.y - py
                                val distSq = pdx * pdx + pdy * pdy
                                if (distSq < radiusSq && distSq > 0.1f) {
                                    val dist = kotlin.math.sqrt(distSq)
                                    val nx = pdx / dist
                                    val ny = pdy / dist
                                    val factor = (radius - dist) / radius
                                    val force = factor * strength * 4.5f * interactSpeed
                                    if (repMode) {
                                        p.vx += nx * force
                                        p.vy += ny * force
                                    } else {
                                        p.vx -= nx * force
                                        p.vy -= ny * force
                                    }
                                }
                            }

                            // Damping & Euler integration
                            p.vx *= (1f - f)
                            p.vy *= (1f - f)
                            p.x += p.vx
                            p.y += p.vy
                        } else {
                            // TWEEN MODE: Direct interpolation
                            val dx = p.tx - p.x
                            val dy = p.ty - p.y
                            p.x += dx * speed * 2.5f
                            p.y += dy * speed * 2.5f
                            p.vx = 0f
                            p.vy = 0f

                            // Force field interactions
                            if (px != null && py != null) {
                                val pdx = p.x - px
                                val pdy = p.y - py
                                val distSq = pdx * pdx + pdy * pdy
                                if (distSq < radiusSq && distSq > 0.1f) {
                                    val dist = kotlin.math.sqrt(distSq)
                                    val nx = pdx / dist
                                    val ny = pdy / dist
                                    val factor = (radius - dist) / radius
                                    val force = factor * strength * 2.0f * interactSpeed
                                    if (repMode) {
                                        p.x += nx * force
                                        p.y += ny * force
                                    } else {
                                        p.x -= nx * force
                                        p.y -= ny * force
                                    }
                                }
                            }
                        }
                    }
                }
                tick++
            }
        }
    }

    // Trigger instant explosion dispersion
    fun explode() {
        val interactSpeed = interactionSpeedScale
        for (p in particles) {
            val angle = Random.nextDouble(0.0, 6.28318).toFloat()
            val force = (Random.nextFloat() * 25f + 8f) * interactSpeed
            p.vx = kotlin.math.cos(angle) * force
            p.vy = kotlin.math.sin(angle) * force
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Particle Vector Sandbox", style = MaterialTheme.typography.titleMedium, color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Text("< Back", color = Color(0xFF00E5FF), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF06060E))
            )
        },
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .background(Color(0xFF06060E))
        ) {
            // Interactive Simulation Canvas Viewport (50% height)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.5f)
                    .onSizeChanged { size = it }
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = { offset -> pointerPos = offset },
                            onDragEnd = { pointerPos = null },
                            onDragCancel = { pointerPos = null },
                            onDrag = { change, _ ->
                                pointerPos = change.position
                            }
                        )
                    }
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onPress = { offset ->
                                pointerPos = offset
                                tryAwaitRelease()
                                pointerPos = null
                            }
                        )
                    }
                    .drawBehind {
                        tick // recompose dependency

                        val currentParticles = particles

                        // Connect close particles with spatial web lines
                        if (enableMesh && currentParticles.isNotEmpty()) {
                            val maxLines = 450
                            var linesDrawn = 0
                            val maxDistSq = 32f * 32f
                            for (i in currentParticles.indices) {
                                if (linesDrawn >= maxLines) break
                                val p1 = currentParticles[i]
                                val limit = minOf(currentParticles.size, i + 16)
                                for (j in i + 1 until limit) {
                                    val p2 = currentParticles[j]
                                    val dx = p1.x - p2.x
                                    val dy = p1.y - p2.y
                                    val dSq = dx * dx + dy * dy
                                    if (dSq < maxDistSq) {
                                        val dist = kotlin.math.sqrt(dSq)
                                        val alpha = (1f - (dist / 32f)).coerceIn(0f, 1f) * 0.4f
                                        drawLine(
                                            color = p1.color.copy(alpha = alpha),
                                            start = Offset(p1.x, p1.y),
                                            end = Offset(p2.x, p2.y),
                                            strokeWidth = 1.2f
                                        )
                                        linesDrawn++
                                    }
                                }
                            }
                        }

                        // Render particles
                        val pSize = particleSize
                        for (p in currentParticles) {
                            drawCircle(
                                color = p.color,
                                radius = pSize,
                                center = Offset(p.x, p.y)
                            )
                        }

                        // Force Field Radius Guide
                        pointerPos?.let { pos ->
                            drawCircle(
                                color = if (isRepulsionMode) Color(0xFFFF007F).copy(alpha = 0.08f) else Color(0xFF00E5FF).copy(alpha = 0.08f),
                                radius = interactionRadius,
                                center = pos
                            )
                            drawCircle(
                                color = if (isRepulsionMode) Color(0xFFFF007F).copy(alpha = 0.35f) else Color(0xFF00E5FF).copy(alpha = 0.35f),
                                radius = 6f,
                                center = pos
                            )
                        }
                    }
            )

            // Scrollable Settings Dashboard Panel (50% height)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.5f)
                    .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                    .border(1.dp, Color(0xFF1E1E2E), RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0D0D18)),
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp, vertical = 16.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    // Title for Presets
                    Text("Presets", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(8.dp))

                    // Horizontally scrollable row of Preset buttons
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier.height(38.dp)
                        ) {
                            val presetScrollState = rememberScrollState()
                            Row(
                                modifier = Modifier.horizontalScroll(presetScrollState),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                PRESETS.forEachIndexed { index, preset ->
                                    val isSelected = selectedPresetIdx == index
                                    Button(
                                        onClick = {
                                            selectedPresetIdx = index
                                            pathInput = preset.pathData
                                            forceRegenTrigger++
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (isSelected) Color(0xFF00B0FF) else Color(0xFF1E1E2E)
                                        ),
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 2.dp)
                                    ) {
                                        Text(preset.label, color = if (isSelected) Color.White else Color.Gray, fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    // SVG Editor Card
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        OutlinedTextField(
                            value = pathInput,
                            onValueChange = {
                                pathInput = it
                                selectedPresetIdx = -1 // customized
                            },
                            label = { Text("SVG Path Data", color = Color(0xFF94A3B8)) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = Color(0xFF00E5FF),
                                unfocusedBorderColor = Color(0xFF334155),
                                cursorColor = Color(0xFF00E5FF)
                            ),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = {
                                keyboardController?.hide()
                                debouncedPathInput = pathInput
                                forceRegenTrigger++
                            }),
                            modifier = Modifier.weight(0.7f),
                            maxLines = 2
                        )
                        Spacer(Modifier.width(12.dp))
                        Button(
                            onClick = {
                                keyboardController?.hide()
                                debouncedPathInput = pathInput
                                forceRegenTrigger++
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00B0FF)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(56.dp)
                        ) {
                            Text("Apply", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    // Interaction Actions
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { explode() },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF007F)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Explode 💥", color = Color.White, fontWeight = FontWeight.SemiBold)
                        }

                        Button(
                            onClick = { isRepulsionMode = !isRepulsionMode },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isRepulsionMode) Color(0xFF1E1E2E) else Color(0xFF00E5FF)
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1.2f)
                        ) {
                            Text(
                                text = if (isRepulsionMode) "Mode: Repulse" else "Mode: Attract",
                                color = if (isRepulsionMode) Color(0xFFFF007F) else Color.Black,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Spacer(Modifier.height(20.dp))

                    // Fill Mode Toggle & Outline Settings
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Fill Mode", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                            Text("Fill shape with particles instead of outlining it", color = Color(0xFF64748B), fontSize = 11.sp)
                        }
                        Switch(
                            checked = isFillMode,
                            onCheckedChange = { isFillMode = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color(0xFF00E5FF),
                                checkedTrackColor = Color(0xFF00E5FF).copy(alpha = 0.3f),
                                uncheckedThumbColor = Color(0xFF64748B),
                                uncheckedTrackColor = Color(0xFF1E1E2E)
                            )
                        )
                    }

                    if (!isFillMode) {
                        Spacer(Modifier.height(8.dp))
                        ControlSlider(
                            label = "Outline Stroke Width",
                            value = strokeWidth,
                            valueRange = 1f..12f,
                            onValueChange = { strokeWidth = it },
                            displayValue = formatDecimal(strokeWidth, 0) + " dp"
                        )
                    }

                    Spacer(Modifier.height(16.dp))

                    // Return Physics Switcher
                    Text("Return Physics Mode", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        VectorReturnMode.values().forEach { mode ->
                            val isSel = returnMode == mode
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSel) Color(0xFF1E1E30) else Color(0xFF0D0D18))
                                    .border(
                                        width = 1.dp,
                                        color = if (isSel) Color(0xFF00E5FF) else Color(0xFF1E1E2E),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .clickable { returnMode = mode }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                  Text(
                                      text = mode.label,
                                      fontSize = 11.sp,
                                      color = if (isSel) Color(0xFF00E5FF) else Color(0xFF64748B),
                                      fontWeight = FontWeight.Bold
                                  )
                            }
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    // Entrance Animation Pattern
                    Text("Entrance Animation Pattern", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        VectorEntrancePattern.values().forEach { pattern ->
                            val isSel = entrancePattern == pattern
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSel) Color(0xFF1E1E30) else Color(0xFF0D0D18))
                                    .border(
                                        width = 1.dp,
                                        color = if (isSel) Color(0xFF00E5FF) else Color(0xFF1E1E2E),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .clickable {
                                        entrancePattern = pattern
                                        forceRegenTrigger++
                                    }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = pattern.label.split(" ").last(),
                                    fontSize = 9.sp,
                                    color = if (isSel) Color(0xFF00E5FF) else Color(0xFF64748B),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(20.dp))

                    // Color Palettes
                    Text("Color Palette", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        VectorPalette.values().forEach { palette ->
                            val isSelected = selectedPalette == palette
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) Color(0xFF1E1E30) else Color(0xFF0D0D18))
                                    .border(
                                        width = 1.dp,
                                        color = if (isSelected) Color(0xFF00E5FF) else Color(0xFF1E1E2E),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .clickable {
                                        selectedPalette = palette
                                        forceRegenTrigger++
                                    }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = palette.label.split(" ").last(),
                                    fontSize = 11.sp,
                                    color = if (isSelected) Color(0xFF00E5FF) else Color(0xFF64748B),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(20.dp))

                    // Mesh Web Connection Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Constellation Mesh", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                            Text("Draw connecting webs between close particles", color = Color(0xFF64748B), fontSize = 11.sp)
                        }
                        Switch(
                            checked = enableMesh,
                            onCheckedChange = { enableMesh = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color(0xFF00E5FF),
                                checkedTrackColor = Color(0xFF00E5FF).copy(alpha = 0.3f),
                                uncheckedThumbColor = Color(0xFF64748B),
                                uncheckedTrackColor = Color(0xFF1E1E2E)
                            )
                        )
                    }

                    Spacer(Modifier.height(16.dp))

                    // Slider Settings
                    ControlSlider(
                        label = "Entrance/Return Speed Scale",
                        value = returnSpeedScale,
                        valueRange = 0.2f..2.5f,
                        onValueChange = { returnSpeedScale = it },
                        displayValue = formatDecimal(returnSpeedScale, 1) + "x"
                    )

                    ControlSlider(
                        label = "Interaction Speed Scale",
                        value = interactionSpeedScale,
                        valueRange = 0.2f..2.5f,
                        onValueChange = { interactionSpeedScale = it },
                        displayValue = formatDecimal(interactionSpeedScale, 1) + "x"
                    )

                    ControlSlider(
                        label = "Particle Density (Fewer = faster)",
                        value = (9 - densityStep).toFloat(),
                        valueRange = 1f..6f,
                        onValueChange = { densityStep = 9 - it.toInt() },
                        displayValue = "${particles.size} particles"
                    )

                    ControlSlider(
                        label = "Particle Size",
                        value = particleSize,
                        valueRange = 1.0f..6.0f,
                        onValueChange = { particleSize = it },
                        displayValue = formatDecimal(particleSize, 1) + " px"
                    )

                    ControlSlider(
                        label = "Return Speed",
                        value = returnSpeed,
                        valueRange = 0.02f..0.30f,
                        onValueChange = { returnSpeed = it },
                        displayValue = formatDecimal(returnSpeed, 2)
                    )

                    ControlSlider(
                        label = "Viscous Friction (Spring damping)",
                        value = friction,
                        valueRange = 0.01f..0.15f,
                        onValueChange = { friction = it },
                        displayValue = formatDecimal(friction, 2)
                    )

                    ControlSlider(
                        label = "Force Field Strength",
                        value = interactionStrength,
                        valueRange = 1.0f..8.0f,
                        onValueChange = { interactionStrength = it },
                        displayValue = formatDecimal(interactionStrength, 1)
                    )

                    ControlSlider(
                        label = "Force Field Radius",
                        value = interactionRadius,
                        valueRange = 60f..250f,
                        onValueChange = { interactionRadius = it },
                        displayValue = formatDecimal(interactionRadius, 0) + " dp"
                    )

                    Spacer(Modifier.height(20.dp))
                }
            }
        }
    }
}

@Composable
private fun ControlSlider(
    label: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
    displayValue: String
) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, color = Color(0xFFCBD5E1), fontSize = 13.sp)
            Text(displayValue, color = Color(0xFF00E5FF), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            colors = SliderDefaults.colors(
                thumbColor = Color(0xFF00E5FF),
                activeTrackColor = Color(0xFF00E5FF),
                inactiveTrackColor = Color(0xFF1E1E2E)
            )
        )
    }
}
