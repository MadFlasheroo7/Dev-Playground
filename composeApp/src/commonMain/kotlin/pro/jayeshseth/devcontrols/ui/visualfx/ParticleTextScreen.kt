package pro.jayeshseth.devcontrols.ui.visualfx

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.random.Random

// Represents an interactive text particle
private class Particle(
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    val tx: Float,
    val ty: Float,
    val color: Color
)

// Dynamic Color Palettes
private enum class Palette(val label: String, val colors: List<Color>) {
    NEON_CYAN("Neon Cyan", listOf(Color(0xFF00E5FF), Color(0xFF00B0FF), Color(0xFF2979FF), Color(0xFF00E5FF))),
    CYBERPUNK("Cyberpunk Pink", listOf(Color(0xFFFF007F), Color(0xFFD500F9), Color(0xFF7C4DFF), Color(0xFF00E5FF))),
    MATRIX("Matrix Green", listOf(Color(0xFF00FF00), Color(0xFF00E676), Color(0xFF1DE9B6), Color(0xFF00FF00))),
    FIRE_GLOW("Fire Glow", listOf(Color(0xFFFF3D00), Color(0xFFFF9100), Color(0xFFFFEA00), Color(0xFFFF3D00))),
    RAINBOW("Rainbow", listOf(Color(0xFFFF1744), Color(0xFFFF9100), Color(0xFFFFEA00), Color(0xFF00E676), Color(0xFF2979FF), Color(0xFF651FFF)))
}

// Return Physics Animation Mode
private enum class ReturnMode(val label: String) {
    SPRING("Spring (Bounce)"),
    TWEEN("Tween (Ease-out)")
}

// Particle Entrance Animation Style
private enum class EntrancePattern(val label: String) {
    SCATTER("Scatter"),
    BURST("Center Burst"),
    VORTEX("Vortex Spiral"),
    GRAVITY("Gravity Drop"),
    FLY_IN("Side Fly-In")
}

private fun formatDecimal(value: Float, decimals: Int): String {
    var multiplier = 1f
    repeat(decimals) { multiplier *= 10f }
    val rounded = kotlin.math.round(value * multiplier) / multiplier
    return rounded.toString()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParticleTextScreen(onBack: () -> Unit) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val density = LocalDensity.current
    val textMeasurer = rememberTextMeasurer()

    // Sandbox Customisation Parameters
    var textInput by remember { mutableStateOf("COMPOSE") }
    var debouncedTextInput by remember { mutableStateOf("COMPOSE") }
    var densityStep by remember { mutableIntStateOf(5) } // pixel scan step
    var particleSize by remember { mutableFloatStateOf(2.5f) }
    var returnSpeed by remember { mutableFloatStateOf(0.08f) }
    var friction by remember { mutableFloatStateOf(0.06f) }
    var interactionStrength by remember { mutableFloatStateOf(4.5f) }
    var interactionRadius by remember { mutableFloatStateOf(120f) }
    var isRepulsionMode by remember { mutableStateOf(true) }
    var enableMesh by remember { mutableStateOf(false) }
    var selectedPalette by remember { mutableStateOf(Palette.CYBERPUNK) }
    
    // New Feature states
    var returnMode by remember { mutableStateOf(ReturnMode.SPRING) }
    var entrancePattern by remember { mutableStateOf(EntrancePattern.SCATTER) }
    var textSizeScale by remember { mutableFloatStateOf(0.85f) } // text size slider
    var returnSpeedScale by remember { mutableFloatStateOf(1.0f) }
    var interactionSpeedScale by remember { mutableFloatStateOf(1.0f) }

    // Simulation Engine State
    var size by remember { mutableStateOf(IntSize.Zero) }
    var particles by remember { mutableStateOf<List<Particle>>(emptyList()) }
    var tick by remember { mutableIntStateOf(0) }
    
    // Pointer Drag Position
    var pointerPos by remember { mutableStateOf<Offset?>(null) }

    // Trigger helper
    var forceRegenTrigger by remember { mutableIntStateOf(0) }

    // Debounce typing to prevent stuttering on every keystroke
    LaunchedEffect(textInput) {
        if (textInput.isEmpty()) {
            debouncedTextInput = ""
            return@LaunchedEffect
        }
        delay(260)
        debouncedTextInput = textInput
    }

    // Text-to-Pixel Rasterization & Particle Spawning in commonMain (instant reaction to settings sliders/dropdowns)
    LaunchedEffect(size, debouncedTextInput, densityStep, selectedPalette, textSizeScale, entrancePattern, returnSpeedScale, forceRegenTrigger) {
        if (size == IntSize.Zero || debouncedTextInput.isEmpty()) {
            particles = emptyList()
            return@LaunchedEffect
        }
        
        val w = size.width
        val h = size.height

        // Rasterize text on a memory ImageBitmap
        val bmpW = w
        val bmpH = (h * 0.45f).toInt().coerceAtLeast(1)
        val imageBitmap = ImageBitmap(bmpW, bmpH)
        val canvas = Canvas(imageBitmap)
        
        // Calculate font size in pixels scaled correctly by density to prevent text being too large/overflowing
        val densityVal = density.density
        val targetFontSizePx = ((bmpW * 0.8f * textSizeScale) / (debouncedTextInput.length * 0.55f))
            .coerceIn(12f * densityVal, bmpH * 0.75f)
        val fontSizeSp = targetFontSizePx / densityVal
        
        val textStyle = TextStyle(
            color = Color.White,
            fontSize = fontSizeSp.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        
        val textLayoutResult = textMeasurer.measure(
            text = AnnotatedString(debouncedTextInput),
            style = textStyle,
            softWrap = false
        )
        
        val canvasDrawScope = CanvasDrawScope()
        canvasDrawScope.draw(
            density = density,
            layoutDirection = LayoutDirection.Ltr,
            canvas = canvas,
            size = Size(bmpW.toFloat(), bmpH.toFloat())
        ) {
            val textWidth = textLayoutResult.size.width.toFloat()
            val textHeight = textLayoutResult.size.height.toFloat()
            val tx = (bmpW - textWidth) / 2f
            val ty = (bmpH - textHeight) / 2f
            
            drawText(
                textLayoutResult = textLayoutResult,
                topLeft = Offset(tx, ty)
            )
        }

        val pixelMap = imageBitmap.toPixelMap()
        val list = ArrayList<Particle>()
        val oy = h * 0.22f // offset text vertically to center in canvas
        val paletteColors = selectedPalette.colors

        var y = 0
        while (y < bmpH) {
            var x = 0
            while (x < bmpW) {
                // Read alpha channel (range 0.0f to 1.0f in Compose Color)
                val alpha = pixelMap[x, y].alpha
                if (alpha > 0.4f) {
                    val colorIdx = (x / (bmpW.toFloat() / paletteColors.size).coerceAtLeast(1f)).toInt().coerceIn(0, paletteColors.size - 1)
                    val color = paletteColors[colorIdx]
                    
                    val txPos = x.toFloat()
                    val tyPos = y.toFloat() + oy
                    
                    // Initialize starting position based on selected entrance pattern
                    var startX = txPos
                    var startY = tyPos
                    var initVx = 0f
                    var initVy = 0f
                    
                    when (entrancePattern) {
                        EntrancePattern.SCATTER -> {
                            startX = Random.nextFloat() * w
                            startY = Random.nextFloat() * h
                        }
                        EntrancePattern.BURST -> {
                            startX = w / 2f
                            startY = h / 2f
                            val angle = Random.nextFloat() * 6.28318f
                            val spd = (Random.nextFloat() * 8f + 2f) * returnSpeedScale
                            initVx = kotlin.math.cos(angle) * spd
                            initVy = kotlin.math.sin(angle) * spd
                        }
                        EntrancePattern.VORTEX -> {
                            val angle = Random.nextFloat() * 6.28318f
                            val radius = maxOf(w, h) * 0.5f
                            startX = w / 2f + radius * kotlin.math.cos(angle)
                            startY = h / 2f + radius * kotlin.math.sin(angle)
                            initVx = -kotlin.math.sin(angle) * 8f * returnSpeedScale
                            initVy = kotlin.math.cos(angle) * 8f * returnSpeedScale
                        }
                        EntrancePattern.GRAVITY -> {
                            startX = txPos
                            startY = -Random.nextFloat() * 200f
                            initVx = 0f
                            initVy = (Random.nextFloat() * 12f + 6f) * returnSpeedScale
                        }
                        EntrancePattern.FLY_IN -> {
                            startX = -100f
                            startY = tyPos + (Random.nextFloat() - 0.5f) * 150f
                            initVx = (Random.nextFloat() * 16f + 10f) * returnSpeedScale
                            initVy = 0f
                        }
                    }
                    
                    list.add(
                        Particle(
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

    // High performance simulation frame loop
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
                        if (mode == ReturnMode.SPRING) {
                            // 1. Target Attraction Force (Hooke's Law spring)
                            val dx = p.tx - p.x
                            val dy = p.ty - p.y
                            p.vx += dx * speed
                            p.vy += dy * speed

                            // 2. Cursor Force Field (Repulsion / Attraction)
                            if (px != null && py != null) {
                                val pdx = p.x - px
                                val pdy = p.y - py
                                val distSq = pdx * pdx + pdy * pdy
                                if (distSq < radiusSq && distSq > 0.1f) {
                                    val dist = kotlin.math.sqrt(distSq)
                                    val nx = pdx / dist
                                    val ny = pdy / dist
                                    // Linear interpolation force based on closeness
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

                            // 3. Friction Damping & Integration
                            p.vx *= (1f - f)
                            p.vy *= (1f - f)
                            p.x += p.vx
                            p.y += p.vy
                        } else {
                            // TWEEN MODE: Smooth direct Ease-out interpolation without oscillation
                            val dx = p.tx - p.x
                            val dy = p.ty - p.y
                            p.x += dx * speed * 2.5f
                            p.y += dy * speed * 2.5f
                            p.vx = 0f
                            p.vy = 0f

                            // Force field interactions still affect position temporarily in tween mode
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

    // Trigger full explosion impulse
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
                title = { Text("Particle Text Sandbox", style = MaterialTheme.typography.titleMedium, color = Color.White) },
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
            // Viewport Canvas (takes 50% height)
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
                            onDrag = { change, dragAmount ->
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
                        // Register dependency on tick
                        tick
                        
                        // Draw mesh constellation links first if enabled
                        val currentParticles = particles
                        if (enableMesh && currentParticles.isNotEmpty()) {
                            val maxLines = 450
                            var linesDrawn = 0
                            val maxDistSq = 32f * 32f
                            for (i in currentParticles.indices) {
                                if (linesDrawn >= maxLines) break
                                val p1 = currentParticles[i]
                                
                                // Spatial heuristic: search next 15 neighbors in grid-rasterized array
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

                        // Draw individual particles
                        val pSize = particleSize
                        for (p in currentParticles) {
                            drawCircle(
                                color = p.color,
                                radius = pSize,
                                center = Offset(p.x, p.y)
                            )
                        }

                        // Draw visual guide for touch radius
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

            // Glassmorphic Scrollable Controls Dashboard (takes 50% height)
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
                    // Text generator field
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        OutlinedTextField(
                            value = textInput,
                            onValueChange = { textInput = it },
                            label = { Text("Live Text Input", color = Color(0xFF94A3B8)) },
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
                                debouncedTextInput = textInput
                                forceRegenTrigger++
                            }),
                            modifier = Modifier.weight(0.7f),
                            maxLines = 1
                        )
                        Spacer(Modifier.width(12.dp))
                        Button(
                            onClick = {
                                keyboardController?.hide()
                                debouncedTextInput = textInput
                                forceRegenTrigger++
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00B0FF)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(56.dp)
                        ) {
                            Text("Spawn", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    // Buttons for Explode & Mode
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

                    // Animation Physics return mode switcher
                    Text("Return Physics Mode", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ReturnMode.values().forEach { mode ->
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

                    // Particle Entrance Pattern selector
                    Text("Entrance Animation Pattern", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        EntrancePattern.values().forEach { pattern ->
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

                    // Color Palettes Selection
                    Text("Color Palette", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Palette.values().forEach { palette ->
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

                    // Toggle: Mesh Web
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

                    // Parameter Slider Controls
                    ControlSlider(
                        label = "Text Font Size Scale",
                        value = textSizeScale,
                        valueRange = 0.4f..1.3f,
                        onValueChange = { textSizeScale = it },
                        displayValue = formatDecimal(textSizeScale * 100, 0) + "%"
                    )

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
