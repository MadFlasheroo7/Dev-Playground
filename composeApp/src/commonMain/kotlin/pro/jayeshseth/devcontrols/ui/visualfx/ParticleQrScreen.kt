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
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import pro.jayeshseth.devcontrols.ui.visualfx.qr.PureQrEncoder
import pro.jayeshseth.devcontrols.ui.visualfx.qr.QrEcLevel
import pro.jayeshseth.devcontrols.ui.visualfx.qr.QrMatrix
import kotlin.random.Random

// Represents an interactive QR particle
private class QrParticle(
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    var tx: Float,
    var ty: Float,
    var color: Color,
    var isFinder: Boolean = false
)

private data class QrTargetPoint(
    val tx: Float,
    val ty: Float,
    val isFinder: Boolean,
    val color: Color
)

// Dynamic Color Palettes
private enum class QrPalette(val label: String, val colors: List<Color>, val scannableDarkColor: Color = Color(0xFF000000)) {
    MONOCHROME("High Contrast (B&W)", listOf(Color(0xFF000000), Color(0xFF111111)), Color(0xFF000000)),
    NEON_CYAN("Neon Cyan", listOf(Color(0xFF00E5FF), Color(0xFF00B0FF), Color(0xFF2979FF), Color(0xFF00E5FF)), Color(0xFF003344)),
    CYBERPUNK("Cyberpunk Pink", listOf(Color(0xFFFF007F), Color(0xFFD500F9), Color(0xFF7C4DFF), Color(0xFF00E5FF)), Color(0xFF3B0020)),
    MATRIX("Matrix Green", listOf(Color(0xFF00FF00), Color(0xFF00E676), Color(0xFF1DE9B6), Color(0xFF00FF00)), Color(0xFF00330A)),
    FIRE_GLOW("Fire Glow", listOf(Color(0xFFFF3D00), Color(0xFFFF9100), Color(0xFFFFEA00), Color(0xFFFF3D00)), Color(0xFF3A0D00)),
    RAINBOW("Rainbow", listOf(Color(0xFFFF1744), Color(0xFFFF9100), Color(0xFFFFEA00), Color(0xFF00E676), Color(0xFF2979FF), Color(0xFF651FFF)), Color(0xFF101020))
}

// Return Physics Animation Mode
private enum class QrReturnMode(val label: String) {
    SPRING("Spring (Bounce)"),
    TWEEN("Tween (Ease-out)")
}

// Particle Entrance Animation Style
private enum class QrEntrancePattern(val label: String) {
    INSTANT("Instant (Pre-formed)"),
    SCATTER("Scatter"),
    BURST("Center Burst"),
    VORTEX("Vortex Spiral"),
    GRAVITY("Gravity Drop"),
    FLY_IN("Side Fly-In")
}

// Runtime change transition style
private enum class QrTransitionMode(val label: String) {
    SMOOTH_MORPH("Smooth Morph"),
    EXPLODE_REASSEMBLE("Explode & Reassemble")
}

// Particle rendering representation mode
private enum class QrRenderMode(val label: String) {
    MODULE_GRID("Module Grid (Scannable)"),
    FLUID_CLOUD("Fluid Particle Cloud")
}

// Interaction mode: tap & drag vs hover
private enum class QrInteractionMode(val label: String) {
    TAP_DRAG("Tap & Drag"),
    HOVER("Hover")
}

private data class QrPreset(val label: String, val value: String)

private val QR_PRESETS = listOf(
    QrPreset("Google", "google.com"),
    QrPreset("GitHub", "https://github.com"),
    QrPreset("Android", "https://android.com"),
    QrPreset("Compose", "https://jetbrains.com/compose-multiplatform"),
    QrPreset("WiFi Config", "WIFI:S:DevControls_WiFi;T:WPA;P:SecretCode2026;;")
)

private fun formatDecimal(value: Float, decimals: Int): String {
    var multiplier = 1f
    repeat(decimals) { multiplier *= 10f }
    val rounded = kotlin.math.round(value * multiplier) / multiplier
    return rounded.toString()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParticleQrScreen(onBack: () -> Unit) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val density = LocalDensity.current

    // Sandbox Customisation Parameters
    var qrInput by remember { mutableStateOf("google.com") }
    var debouncedQrInput by remember { mutableStateOf("google.com") }
    var densityStep by remember { mutableIntStateOf(5) } // pixel scan step in fluid mode
    var particleSize by remember { mutableFloatStateOf(3.2f) }
    var returnSpeed by remember { mutableFloatStateOf(0.08f) }
    var friction by remember { mutableFloatStateOf(0.06f) }
    var interactionStrength by remember { mutableFloatStateOf(4.5f) }
    var interactionRadius by remember { mutableFloatStateOf(120f) }
    var isRepulsionMode by remember { mutableStateOf(true) }
    var enableMesh by remember { mutableStateOf(false) }
    var selectedPalette by remember { mutableStateOf(QrPalette.MONOCHROME) }

    // Feature states
    var returnMode by remember { mutableStateOf(QrReturnMode.SPRING) }
    var entrancePattern by remember { mutableStateOf(QrEntrancePattern.INSTANT) }
    var transitionMode by remember { mutableStateOf(QrTransitionMode.SMOOTH_MORPH) }
    var renderMode by remember { mutableStateOf(QrRenderMode.MODULE_GRID) }
    var lockFinderPatterns by remember { mutableStateOf(true) }
    var showScannablePlate by remember { mutableStateOf(true) }
    var interactionMode by remember { mutableStateOf(QrInteractionMode.TAP_DRAG) }
    var qrSizeScale by remember { mutableFloatStateOf(0.85f) }
    var returnSpeedScale by remember { mutableFloatStateOf(1.0f) }
    var interactionSpeedScale by remember { mutableFloatStateOf(1.0f) }

    // Simulation Engine State
    var size by remember { mutableStateOf(IntSize.Zero) }
    var particles by remember { mutableStateOf<List<QrParticle>>(emptyList()) }
    var tick by remember { mutableIntStateOf(0) }

    // Pointer Drag Position
    var pointerPos by remember { mutableStateOf<Offset?>(null) }
    var forceRegenTrigger by remember { mutableIntStateOf(0) }

    // Debounce typing to prevent stuttering on every keystroke
    LaunchedEffect(qrInput) {
        if (qrInput.isEmpty()) {
            debouncedQrInput = ""
            return@LaunchedEffect
        }
        delay(260)
        debouncedQrInput = qrInput
    }

    // QR Particle Calculation & Transition Animation
    LaunchedEffect(size, debouncedQrInput, renderMode, densityStep, selectedPalette, qrSizeScale, forceRegenTrigger) {
        if (size == IntSize.Zero || debouncedQrInput.isEmpty()) {
            particles = emptyList()
            return@LaunchedEffect
        }

        val w = size.width.toFloat()
        val h = size.height.toFloat()

        // 1. Generate QR Code Matrix
        val qrMatrix: QrMatrix = try {
            PureQrEncoder.encode(debouncedQrInput, QrEcLevel.M)
        } catch (e: Exception) {
            PureQrEncoder.encode("google.com", QrEcLevel.M)
        }

        val qrModulesCount = qrMatrix.size
        val availableDim = minOf(w, h) * qrSizeScale
        val moduleSize = availableDim / qrModulesCount
        val startLeft = (w - availableDim) / 2f
        val startTop = (h - availableDim) / 2f
        val paletteColors = selectedPalette.colors

        val newTargets = ArrayList<QrTargetPoint>()

        if (renderMode == QrRenderMode.MODULE_GRID) {
            // Crisp module grid: 1 particle per dark module
            for (r in 0 until qrModulesCount) {
                for (c in 0 until qrModulesCount) {
                    if (qrMatrix.isDark(r, c)) {
                        val tx = startLeft + (c + 0.5f) * moduleSize
                        val ty = startTop + (r + 0.5f) * moduleSize
                        val isFinder = qrMatrix.isFinderPattern(r, c)
                        val colorIdx = ((c.toFloat() / qrModulesCount) * paletteColors.size).toInt().coerceIn(0, paletteColors.size - 1)
                        val color = paletteColors[colorIdx]
                        newTargets.add(QrTargetPoint(tx, ty, isFinder, color))
                    }
                }
            }
        } else {
            // Fluid Particle Cloud: Rasterize QR to offscreen ImageBitmap and sample pixels via densityStep
            val bmpW = availableDim.toInt().coerceAtLeast(1)
            val bmpH = availableDim.toInt().coerceAtLeast(1)
            val imageBitmap = ImageBitmap(bmpW, bmpH)
            val canvas = Canvas(imageBitmap)
            val canvasDrawScope = CanvasDrawScope()

            canvasDrawScope.draw(
                density = density,
                layoutDirection = LayoutDirection.Ltr,
                canvas = canvas,
                size = Size(bmpW.toFloat(), bmpH.toFloat())
            ) {
                val modPx = bmpW.toFloat() / qrModulesCount
                for (r in 0 until qrModulesCount) {
                    for (c in 0 until qrModulesCount) {
                        if (qrMatrix.isDark(r, c)) {
                            drawRect(
                                color = Color.White,
                                topLeft = Offset(c * modPx, r * modPx),
                                size = Size(modPx, modPx)
                            )
                        }
                    }
                }
            }

            val pixelMap = imageBitmap.toPixelMap()
            var y = 0
            while (y < bmpH) {
                var x = 0
                while (x < bmpW) {
                    if (pixelMap[x, y].alpha > 0.4f) {
                        val tx = startLeft + x.toFloat()
                        val ty = startTop + y.toFloat()
                        val gridCol = ((x.toFloat() / bmpW) * qrModulesCount).toInt().coerceIn(0, qrModulesCount - 1)
                        val gridRow = ((y.toFloat() / bmpH) * qrModulesCount).toInt().coerceIn(0, qrModulesCount - 1)
                        val isFinder = qrMatrix.isFinderPattern(gridRow, gridCol)
                        val colorIdx = ((x.toFloat() / bmpW) * paletteColors.size).toInt().coerceIn(0, paletteColors.size - 1)
                        val color = paletteColors[colorIdx]
                        newTargets.add(QrTargetPoint(tx, ty, isFinder, color))
                    }
                    x += densityStep
                }
                y += densityStep
            }
        }

        // 2. Animate Transition: Morph vs Explode vs Initial Spawn
        val currentList = particles
        if (currentList.isEmpty() || entrancePattern != QrEntrancePattern.SCATTER && forceRegenTrigger > 0 && currentList.isEmpty()) {
            // Initial generation: spawn with selected entrance pattern
            val list = ArrayList<QrParticle>(newTargets.size)
            for (target in newTargets) {
                var startX = target.tx
                var startY = target.ty
                var initVx = 0f
                var initVy = 0f

                when (entrancePattern) {
                    QrEntrancePattern.INSTANT -> {
                        startX = target.tx
                        startY = target.ty
                        initVx = 0f
                        initVy = 0f
                    }
                    QrEntrancePattern.SCATTER -> {
                        startX = Random.nextFloat() * w
                        startY = Random.nextFloat() * h
                    }
                    QrEntrancePattern.BURST -> {
                        startX = w / 2f
                        startY = h / 2f
                        val angle = Random.nextFloat() * 6.28318f
                        val spd = (Random.nextFloat() * 8f + 2f) * returnSpeedScale
                        initVx = kotlin.math.cos(angle) * spd
                        initVy = kotlin.math.sin(angle) * spd
                    }
                    QrEntrancePattern.VORTEX -> {
                        val angle = Random.nextFloat() * 6.28318f
                        val radius = maxOf(w, h) * 0.5f
                        startX = w / 2f + radius * kotlin.math.cos(angle)
                        startY = h / 2f + radius * kotlin.math.sin(angle)
                        initVx = -kotlin.math.sin(angle) * 8f * returnSpeedScale
                        initVy = kotlin.math.cos(angle) * 8f * returnSpeedScale
                    }
                    QrEntrancePattern.GRAVITY -> {
                        startX = target.tx
                        startY = -Random.nextFloat() * 200f
                        initVx = 0f
                        initVy = (Random.nextFloat() * 12f + 6f) * returnSpeedScale
                    }
                    QrEntrancePattern.FLY_IN -> {
                        startX = -100f
                        startY = target.ty + (Random.nextFloat() - 0.5f) * 150f
                        initVx = (Random.nextFloat() * 16f + 10f) * returnSpeedScale
                        initVy = 0f
                    }
                }

                list.add(
                    QrParticle(
                        x = startX,
                        y = startY,
                        vx = initVx,
                        vy = initVy,
                        tx = target.tx,
                        ty = target.ty,
                        color = target.color,
                        isFinder = target.isFinder
                    )
                )
            }
            particles = list
        } else if (transitionMode == QrTransitionMode.SMOOTH_MORPH) {
            // Smooth Morph: Retarget existing particles to new coordinates without resetting positions
            val updatedList = ArrayList<QrParticle>(newTargets.size)
            val commonCount = minOf(currentList.size, newTargets.size)

            for (i in 0 until commonCount) {
                val p = currentList[i]
                val t = newTargets[i]
                p.tx = t.tx
                p.ty = t.ty
                p.color = t.color
                p.isFinder = t.isFinder
                updatedList.add(p)
            }

            // If new QR requires more particles, spawn excess particles flying in from center
            if (newTargets.size > currentList.size) {
                for (i in currentList.size until newTargets.size) {
                    val t = newTargets[i]
                    val angle = Random.nextFloat() * 6.28318f
                    val spd = (Random.nextFloat() * 10f + 2f) * returnSpeedScale
                    updatedList.add(
                        QrParticle(
                            x = w / 2f,
                            y = h / 2f,
                            vx = kotlin.math.cos(angle) * spd,
                            vy = kotlin.math.sin(angle) * spd,
                            tx = t.tx,
                            ty = t.ty,
                            color = t.color,
                            isFinder = t.isFinder
                        )
                    )
                }
            }
            particles = updatedList
        } else {
            // Explode & Reassemble: Impart radial explosion velocities, then reassemble into new targets
            val updatedList = ArrayList<QrParticle>(newTargets.size)
            val commonCount = minOf(currentList.size, newTargets.size)

            for (i in 0 until commonCount) {
                val p = currentList[i]
                val t = newTargets[i]
                val angle = Random.nextFloat() * 6.28318f
                val force = (Random.nextFloat() * 22f + 8f) * returnSpeedScale
                p.vx = kotlin.math.cos(angle) * force
                p.vy = kotlin.math.sin(angle) * force
                p.tx = t.tx
                p.ty = t.ty
                p.color = t.color
                p.isFinder = t.isFinder
                updatedList.add(p)
            }

            if (newTargets.size > currentList.size) {
                for (i in currentList.size until newTargets.size) {
                    val t = newTargets[i]
                    val angle = Random.nextFloat() * 6.28318f
                    val force = (Random.nextFloat() * 25f + 10f) * returnSpeedScale
                    updatedList.add(
                        QrParticle(
                            x = w / 2f,
                            y = h / 2f,
                            vx = kotlin.math.cos(angle) * force,
                            vy = kotlin.math.sin(angle) * force,
                            tx = t.tx,
                            ty = t.ty,
                            color = t.color,
                            isFinder = t.isFinder
                        )
                    )
                }
            }
            particles = updatedList
        }
    }

    // High performance physics simulation frame loop
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
                    val lockFinder = lockFinderPatterns

                    for (p in currentParticles) {
                        // If finder patterns are locked, pin them permanently to target coordinates
                        if (lockFinder && p.isFinder) {
                            p.x = p.tx
                            p.y = p.ty
                            p.vx = 0f
                            p.vy = 0f
                            continue
                        }

                        if (mode == QrReturnMode.SPRING) {
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
        val lockFinder = lockFinderPatterns
        for (p in particles) {
            if (lockFinder && p.isFinder) continue
            val angle = Random.nextDouble(0.0, 6.28318).toFloat()
            val force = (Random.nextFloat() * 25f + 8f) * interactSpeed
            p.vx = kotlin.math.cos(angle) * force
            p.vy = kotlin.math.sin(angle) * force
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Particle QR Sandbox", style = MaterialTheme.typography.titleMedium, color = Color.White) },
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
                    .pointerInput(interactionMode) {
                        if (interactionMode == QrInteractionMode.HOVER) {
                            awaitPointerEventScope {
                                while (true) {
                                    val event = awaitPointerEvent(androidx.compose.ui.input.pointer.PointerEventPass.Main)
                                    when (event.type) {
                                        androidx.compose.ui.input.pointer.PointerEventType.Move,
                                        androidx.compose.ui.input.pointer.PointerEventType.Enter -> {
                                            pointerPos = event.changes.firstOrNull()?.position
                                        }
                                        androidx.compose.ui.input.pointer.PointerEventType.Exit -> {
                                            pointerPos = null
                                        }
                                        else -> {}
                                    }
                                }
                            }
                        } else {
                            detectDragGestures(
                                onDragStart = { offset -> pointerPos = offset },
                                onDragEnd = { pointerPos = null },
                                onDragCancel = { pointerPos = null },
                                onDrag = { change, _ ->
                                    pointerPos = change.position
                                }
                            )
                        }
                    }
                    .pointerInput(interactionMode) {
                        if (interactionMode == QrInteractionMode.TAP_DRAG) {
                            detectTapGestures(
                                onPress = { offset ->
                                    pointerPos = offset
                                    tryAwaitRelease()
                                    pointerPos = null
                                }
                            )
                        }
                    }
                    .drawBehind {
                        tick // register dependency on simulation frame loop

                        // Constellation mesh links if enabled
                        val currentParticles = particles
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

                        // Draw Scannable White Plate (Quiet Zone Card)
                        if (showScannablePlate) {
                            val availableDim = minOf(size.width, size.height) * qrSizeScale
                            val startLeft = (size.width - availableDim) / 2f
                            val startTop = (size.height - availableDim) / 2f
                            val quietZone = (availableDim / 21f).coerceIn(16f, 36f)
                            drawRoundRect(
                                color = Color.White,
                                topLeft = Offset(startLeft - quietZone, startTop - quietZone),
                                size = Size(availableDim + quietZone * 2f, availableDim + quietZone * 2f),
                                cornerRadius = androidx.compose.ui.geometry.CornerRadius(14f, 14f)
                            )
                        }

                        // Draw individual particles / modules
                        val pSize = particleSize
                        val isPlate = showScannablePlate
                        val isGrid = renderMode == QrRenderMode.MODULE_GRID

                        for (p in currentParticles) {
                            val drawColor = if (isPlate) {
                                if (selectedPalette == QrPalette.MONOCHROME) Color.Black else selectedPalette.scannableDarkColor
                            } else {
                                p.color
                            }

                            if (isGrid && isPlate) {
                                val modSide = pSize * 2.2f
                                drawRoundRect(
                                    color = drawColor,
                                    topLeft = Offset(p.x - modSide / 2f, p.y - modSide / 2f),
                                    size = Size(modSide, modSide),
                                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(modSide * 0.15f, modSide * 0.15f)
                                )
                            } else {
                                drawCircle(
                                    color = drawColor,
                                    radius = if (p.isFinder && lockFinderPatterns) pSize * 1.15f else pSize,
                                    center = Offset(p.x, p.y)
                                )
                            }
                        }

                        // Visual guide for touch radius
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
                    // QR Code live input field
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        OutlinedTextField(
                            value = qrInput,
                            onValueChange = { qrInput = it },
                            label = { Text("QR Code URL / Text", color = Color(0xFF94A3B8)) },
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
                                debouncedQrInput = qrInput
                                forceRegenTrigger++
                            }),
                            modifier = Modifier.weight(0.7f),
                            maxLines = 1
                        )
                        Spacer(Modifier.width(12.dp))
                        Button(
                            onClick = {
                                keyboardController?.hide()
                                debouncedQrInput = qrInput
                                forceRegenTrigger++
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00B0FF)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(56.dp)
                        ) {
                            Text("Generate", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    // Preset Quick Chips
                    Text("Quick Presets", color = Color(0xFF94A3B8), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(6.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        for (preset in QR_PRESETS) {
                            val isSelected = qrInput == preset.value
                            Surface(
                                modifier = Modifier.clickable {
                                    qrInput = preset.value
                                    debouncedQrInput = preset.value
                                    forceRegenTrigger++
                                },
                                shape = RoundedCornerShape(16.dp),
                                color = if (isSelected) Color(0xFF00E5FF).copy(alpha = 0.2f) else Color(0xFF1E1E2E),
                                border = BorderStroke(1.dp, if (isSelected) Color(0xFF00E5FF) else Color(0xFF334155))
                            ) {
                                Text(
                                    text = preset.label,
                                    color = if (isSelected) Color(0xFF00E5FF) else Color.White,
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    // Buttons for Explode & Force Mode
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

                    Spacer(Modifier.height(16.dp))

                    // Interaction Input Mode Toggle (Tap & Drag vs Hover)
                    Text("Interaction Input Mode", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        for (mode in QrInteractionMode.entries) {
                            val selected = interactionMode == mode
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { interactionMode = mode },
                                shape = RoundedCornerShape(8.dp),
                                color = if (selected) Color(0xFF00E5FF).copy(alpha = 0.2f) else Color(0xFF1E1E2E),
                                border = BorderStroke(1.dp, if (selected) Color(0xFF00E5FF) else Color(0xFF334155))
                            ) {
                                Text(
                                    text = if (mode == QrInteractionMode.TAP_DRAG) "Tap & Move" else "Hover",
                                    color = if (selected) Color(0xFF00E5FF) else Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                    modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    // Transition Mode Toggle (Morph vs Explode & Reassemble)
                    Text("Value Change Transition", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        for (mode in QrTransitionMode.entries) {
                            val selected = transitionMode == mode
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { transitionMode = mode },
                                shape = RoundedCornerShape(8.dp),
                                color = if (selected) Color(0xFF00E5FF).copy(alpha = 0.2f) else Color(0xFF1E1E2E),
                                border = BorderStroke(1.dp, if (selected) Color(0xFF00E5FF) else Color(0xFF334155))
                            ) {
                                Text(
                                    text = mode.label,
                                    color = if (selected) Color(0xFF00E5FF) else Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                    modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    // Render Mode Toggle (Module Grid vs Fluid Cloud)
                    Text("Render Representation", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        for (mode in QrRenderMode.entries) {
                            val selected = renderMode == mode
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { renderMode = mode },
                                shape = RoundedCornerShape(8.dp),
                                color = if (selected) Color(0xFF00E5FF).copy(alpha = 0.2f) else Color(0xFF1E1E2E),
                                border = BorderStroke(1.dp, if (selected) Color(0xFF00E5FF) else Color(0xFF334155))
                            ) {
                                Text(
                                    text = mode.label,
                                    color = if (selected) Color(0xFF00E5FF) else Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                    modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    // Lock Finder Patterns Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Lock Finder Patterns", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                            Text(
                                "Anchors 3 corner eyes so QR can be scanned while data dances",
                                color = Color(0xFF94A3B8),
                                fontSize = 11.sp
                            )
                        }
                        Switch(
                            checked = lockFinderPatterns,
                            onCheckedChange = { lockFinderPatterns = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF00E5FF))
                        )
                    }

                    Spacer(Modifier.height(16.dp))

                    // Scannable White Card Plate Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Scannable White Plate", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                            Text(
                                "Quiet-zone background card for immediate camera scanning",
                                color = Color(0xFF94A3B8),
                                fontSize = 11.sp
                            )
                        }
                        Switch(
                            checked = showScannablePlate,
                            onCheckedChange = { showScannablePlate = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF00E5FF))
                        )
                    }

                    Spacer(Modifier.height(16.dp))

                    // Return Physics Mode (Spring vs Tween)
                    Text("Return Physics Mode", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        for (mode in QrReturnMode.entries) {
                            val selected = returnMode == mode
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { returnMode = mode },
                                shape = RoundedCornerShape(8.dp),
                                color = if (selected) Color(0xFF00E5FF).copy(alpha = 0.2f) else Color(0xFF1E1E2E),
                                border = BorderStroke(1.dp, if (selected) Color(0xFF00E5FF) else Color(0xFF334155))
                            ) {
                                Text(
                                    text = mode.label,
                                    color = if (selected) Color(0xFF00E5FF) else Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                    modifier = Modifier.padding(vertical = 10.dp),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    // Entrance Animation Pattern
                    Text("Entrance Animation Pattern", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(6.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        for (pattern in QrEntrancePattern.entries) {
                            val selected = entrancePattern == pattern
                            Surface(
                                modifier = Modifier.clickable {
                                    entrancePattern = pattern
                                    forceRegenTrigger++
                                },
                                shape = RoundedCornerShape(8.dp),
                                color = if (selected) Color(0xFF00E5FF).copy(alpha = 0.2f) else Color(0xFF1E1E2E),
                                border = BorderStroke(1.dp, if (selected) Color(0xFF00E5FF) else Color(0xFF334155))
                            ) {
                                Text(
                                    text = pattern.label,
                                    color = if (selected) Color(0xFF00E5FF) else Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    // Dynamic Color Palettes
                    Text("Color Palette", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(6.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        for (palette in QrPalette.entries) {
                            val selected = selectedPalette == palette
                            Surface(
                                modifier = Modifier.clickable { selectedPalette = palette },
                                shape = RoundedCornerShape(8.dp),
                                color = if (selected) Color(0xFF00E5FF).copy(alpha = 0.2f) else Color(0xFF1E1E2E),
                                border = BorderStroke(1.dp, if (selected) Color(0xFF00E5FF) else Color(0xFF334155))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .clip(RoundedCornerShape(2.dp))
                                            .background(palette.colors.first())
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        text = palette.label,
                                        color = if (selected) Color(0xFF00E5FF) else Color.White,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    // Sliders
                    // QR Size Scale
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("QR Code Scale", color = Color.White, fontSize = 13.sp)
                        Text("${formatDecimal(qrSizeScale, 2)}x", color = Color(0xFF00E5FF), fontSize = 13.sp)
                    }
                    Slider(
                        value = qrSizeScale,
                        onValueChange = { qrSizeScale = it },
                        valueRange = 0.5f..1.0f,
                        colors = SliderDefaults.colors(thumbColor = Color(0xFF00E5FF), activeTrackColor = Color(0xFF00E5FF))
                    )

                    // Particle Size
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Particle Radius", color = Color.White, fontSize = 13.sp)
                        Text("${formatDecimal(particleSize, 1)} px", color = Color(0xFF00E5FF), fontSize = 13.sp)
                    }
                    Slider(
                        value = particleSize,
                        onValueChange = { particleSize = it },
                        valueRange = 1.0f..8.0f,
                        colors = SliderDefaults.colors(thumbColor = Color(0xFF00E5FF), activeTrackColor = Color(0xFF00E5FF))
                    )

                    if (renderMode == QrRenderMode.FLUID_CLOUD) {
                        // Density Step
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Pixel Scan Step (Density)", color = Color.White, fontSize = 13.sp)
                            Text("$densityStep px", color = Color(0xFF00E5FF), fontSize = 13.sp)
                        }
                        Slider(
                            value = densityStep.toFloat(),
                            onValueChange = { densityStep = it.toInt() },
                            valueRange = 2f..8f,
                            steps = 5,
                            colors = SliderDefaults.colors(thumbColor = Color(0xFF00E5FF), activeTrackColor = Color(0xFF00E5FF))
                        )
                    }

                    // Return Speed
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Return Speed", color = Color.White, fontSize = 13.sp)
                        Text(formatDecimal(returnSpeed, 2), color = Color(0xFF00E5FF), fontSize = 13.sp)
                    }
                    Slider(
                        value = returnSpeed,
                        onValueChange = { returnSpeed = it },
                        valueRange = 0.02f..0.20f,
                        colors = SliderDefaults.colors(thumbColor = Color(0xFF00E5FF), activeTrackColor = Color(0xFF00E5FF))
                    )

                    // Friction Damping
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Friction Damping", color = Color.White, fontSize = 13.sp)
                        Text(formatDecimal(friction, 2), color = Color(0xFF00E5FF), fontSize = 13.sp)
                    }
                    Slider(
                        value = friction,
                        onValueChange = { friction = it },
                        valueRange = 0.01f..0.15f,
                        colors = SliderDefaults.colors(thumbColor = Color(0xFF00E5FF), activeTrackColor = Color(0xFF00E5FF))
                    )

                    // Interaction Strength
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Interaction Strength", color = Color.White, fontSize = 13.sp)
                        Text(formatDecimal(interactionStrength, 1), color = Color(0xFF00E5FF), fontSize = 13.sp)
                    }
                    Slider(
                        value = interactionStrength,
                        onValueChange = { interactionStrength = it },
                        valueRange = 1.0f..10.0f,
                        colors = SliderDefaults.colors(thumbColor = Color(0xFF00E5FF), activeTrackColor = Color(0xFF00E5FF))
                    )

                    // Interaction Radius
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Interaction Radius", color = Color.White, fontSize = 13.sp)
                        Text("${formatDecimal(interactionRadius, 0)} px", color = Color(0xFF00E5FF), fontSize = 13.sp)
                    }
                    Slider(
                        value = interactionRadius,
                        onValueChange = { interactionRadius = it },
                        valueRange = 50f..250f,
                        colors = SliderDefaults.colors(thumbColor = Color(0xFF00E5FF), activeTrackColor = Color(0xFF00E5FF))
                    )

                    // Constellation Mesh Links
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Constellation Mesh Links", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                            Text("Draws connective links between nearby particles", color = Color(0xFF94A3B8), fontSize = 11.sp)
                        }
                        Switch(
                            checked = enableMesh,
                            onCheckedChange = { enableMesh = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF00E5FF))
                        )
                    }

                    Spacer(Modifier.height(24.dp))
                }
            }
        }
    }
}
