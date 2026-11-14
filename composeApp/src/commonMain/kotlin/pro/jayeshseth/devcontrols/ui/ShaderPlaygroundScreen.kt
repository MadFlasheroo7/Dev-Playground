package pro.jayeshseth.devcontrols.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

// ---------------------------------------------------------------------------
// THEME & SCENE DEFINITIONS
// ---------------------------------------------------------------------------

enum class ShaderCategory(val title: String, val icon: String) {
    COSMOS("Cosmos", "✦"),
    JAPAN("Japan", "⛩")
}

enum class ShaderScene(
    val category: ShaderCategory,
    val title: String,
    val subtitle: String,
    val icon: String,
    val primaryColor: Color,
    val secondaryColor: Color,
    val accentColor: Color
) {
    // Cosmos Collection
    DEEP_NEBULA(
        category = ShaderCategory.COSMOS,
        title = "Deep Nebula",
        subtitle = "Gas clouds • stellar dust • gravity vortex",
        icon = "✦",
        primaryColor = Color(0xFF090615),
        secondaryColor = Color(0xFF28114B),
        accentColor = Color(0xFFC084FC)
    ),
    BLACK_HOLE(
        category = ShaderCategory.COSMOS,
        title = "Event Horizon",
        subtitle = "Accretion disk • photon sphere • spacetime warp",
        icon = "◎",
        primaryColor = Color(0xFF040407),
        secondaryColor = Color(0xFF1E0D05),
        accentColor = Color(0xFFFF9E0B)
    ),
    PULSAR_STAR(
        category = ShaderCategory.COSMOS,
        title = "Pulsar Beam",
        subtitle = "Relativistic light cones • shockwave blasts",
        icon = "✶",
        primaryColor = Color(0xFF040D1A),
        secondaryColor = Color(0xFF0D2847),
        accentColor = Color(0xFF38BDF8)
    ),
    QUANTUM_WARP(
        category = ShaderCategory.COSMOS,
        title = "Quantum Warp",
        subtitle = "3D depth parallax • warp speed trails",
        icon = "☄",
        primaryColor = Color(0xFF030A14),
        secondaryColor = Color(0xFF082218),
        accentColor = Color(0xFF4ADE80)
    ),

    // Japan Collection
    SAKURA_DUSK(
        category = ShaderCategory.JAPAN,
        title = "Sakura Dusk",
        subtitle = "Mount Fuji • Torii gate • swirling petals",
        icon = "🌸",
        primaryColor = Color(0xFF25142E),
        secondaryColor = Color(0xFF5B1E48),
        accentColor = Color(0xFFF472B6)
    ),
    CYBERPUNK_TOKYO(
        category = ShaderCategory.JAPAN,
        title = "Neo Tokyo",
        subtitle = "Rain on glass • neon kanji • chromatic glow",
        icon = "⚡",
        primaryColor = Color(0xFF080B16),
        secondaryColor = Color(0xFF141936),
        accentColor = Color(0xFF22D3EE)
    ),
    THE_GREAT_WAVE(
        category = ShaderCategory.JAPAN,
        title = "Great Wave",
        subtitle = "Ukiyo-e woodblock curves • seafoam currents",
        icon = "🌊",
        primaryColor = Color(0xFF071426),
        secondaryColor = Color(0xFF0F2E4D),
        accentColor = Color(0xFF60A5FA)
    ),
    KOMOREBI_BAMBOO(
        category = ShaderCategory.JAPAN,
        title = "Komorebi Grove",
        subtitle = "Arashiyama bamboo • light beams • fireflies",
        icon = "🎋",
        primaryColor = Color(0xFF06140D),
        secondaryColor = Color(0xFF0E2E1D),
        accentColor = Color(0xFFA3E635)
    )
}

enum class ColorPalette(val title: String) {
    DEFAULT("Theme Native"),
    VIBRANT_CYBER("Cyber Neon"),
    SUNSET_EMBER("Sunset Ember"),
    CELESTIAL_ICE("Celestial Ice"),
    MONOCHROME_GOLD("Monochrome Gold")
}

data class ShockwaveRipple(
    val x: Float,
    val y: Float,
    val startTime: Float,
    val maxRadius: Float = 600f
)

// ---------------------------------------------------------------------------
// MAIN SCREEN COMPOSABLE
// ---------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShaderPlaygroundScreen(onBack: () -> Unit) {
    var activeCategory by remember { mutableStateOf(ShaderCategory.COSMOS) }
    var activeScene by remember { mutableStateOf(ShaderScene.DEEP_NEBULA) }
    var settingsOpen by remember { mutableStateOf(false) }

    // Adjustable Parameters
    var speed by remember { mutableFloatStateOf(0.75f) }
    var glow by remember { mutableFloatStateOf(1.0f) }
    var particleDensity by remember { mutableFloatStateOf(1.0f) }
    var touchInfluence by remember { mutableFloatStateOf(1.0f) }
    var turbulence by remember { mutableFloatStateOf(1.0f) }
    var palette by remember { mutableStateOf(ColorPalette.DEFAULT) }
    var showHud by remember { mutableStateOf(true) }
    var showRipples by remember { mutableStateOf(true) }

    // Dynamic State
    var paused by remember { mutableStateOf(false) }
    var time by remember { mutableFloatStateOf(0f) }
    var tick by remember { mutableIntStateOf(0) }
    var touch by remember { mutableStateOf(Offset(0.5f, 0.45f)) }
    var targetTouch by remember { mutableStateOf(Offset(0.5f, 0.45f)) }
    var isTouching by remember { mutableStateOf(false) }
    val touchAlpha by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (isTouching) 1f else 0f,
        animationSpec = tween(durationMillis = 350)
    )

    // Shockwave ripples
    val ripples = remember { mutableStateListOf<ShockwaveRipple>() }

    // Pre-allocated random seed particles for 60+ FPS efficiency
    val randomPoints = remember {
        List(280) {
            Triple(Random.nextFloat(), Random.nextFloat(), Random.nextFloat())
        }
    }

    // Animation Clock Loop
    LaunchedEffect(paused, speed) {
        var previousTimeNanos = 0L
        while (true) {
            withFrameNanos { frameTimeNanos ->
                if (previousTimeNanos != 0L && !paused) {
                    val deltaSeconds = (frameTimeNanos - previousTimeNanos) / 1_000_000_000f
                    time += deltaSeconds * speed
                    // Smooth touch lerp for organic responsiveness
                    touch = Offset(
                        touch.x + (targetTouch.x - touch.x) * 0.18f,
                        touch.y + (targetTouch.y - touch.y) * 0.18f
                    )
                }
                previousTimeNanos = frameTimeNanos
                if (!paused) tick++
            }
        }
    }

    // Clean up expired ripples
    LaunchedEffect(tick) {
        if (ripples.isNotEmpty()) {
            val threshold = time - 2.5f
            ripples.removeAll { it.startTime < threshold }
        }
    }

    Scaffold(
        containerColor = Color(0xFF090A10),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                "Shader Playground — Gemini",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(if (paused) Color(0xFFFFB74D) else Color(0xFF00E676))
                            )
                        }
                        Text(
                            "CROSS-PLATFORM GENERATIVE ART ENGINE",
                            style = MaterialTheme.typography.labelSmall,
                            letterSpacing = 1.2.sp,
                            color = Color(0xFF8E92A8)
                        )
                    }
                },
                navigationIcon = {
                    TextButton(onClick = onBack) {
                        Text("← Back", color = Color(0xFFB0B5CE))
                    }
                },
                actions = {
                    TextButton(onClick = { paused = !paused }) {
                        Text(if (paused) "▶ Play" else "⏸ Pause", color = activeScene.accentColor)
                    }
                    TextButton(onClick = { time = 0f; targetTouch = Offset(0.5f, 0.45f); ripples.clear() }) {
                        Text("↻ Reset", color = Color(0xFFB0B5CE))
                    }
                    Button(
                        onClick = { settingsOpen = true },
                        colors = ButtonDefaults.buttonColors(containerColor = activeScene.accentColor.copy(alpha = 0.25f)),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text("⚙ Tune", color = Color.White, style = MaterialTheme.typography.labelMedium)
                    }
                    Spacer(Modifier.width(8.dp))
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF10121C))
            )
        }
    ) { insets ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(insets)
        ) {
            // Category & Scene Selection Bar
            ShaderToolbar(
                activeCategory = activeCategory,
                activeScene = activeScene,
                onSelectCategory = { cat ->
                    activeCategory = cat
                    activeScene = ShaderScene.entries.first { it.category == cat }
                },
                onSelectScene = { scene ->
                    activeScene = scene
                    activeCategory = scene.category
                }
            )

            // Main Interactive Canvas Area
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 8.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .border(1.dp, Color(0xFF1E2235), RoundedCornerShape(20.dp))
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(activeScene) {
                            detectDragGestures(
                                onDragStart = { offset ->
                                    isTouching = true
                                    targetTouch = Offset(
                                        (offset.x / size.width).coerceIn(0f, 1f),
                                        (offset.y / size.height).coerceIn(0f, 1f)
                                    )
                                },
                                onDragEnd = { isTouching = false },
                                onDragCancel = { isTouching = false },
                                onDrag = { change, _ ->
                                    isTouching = true
                                    targetTouch = Offset(
                                        (change.position.x / size.width).coerceIn(0f, 1f),
                                        (change.position.y / size.height).coerceIn(0f, 1f)
                                    )
                                    change.consume()
                                }
                            )
                        }
                        .pointerInput(activeScene) {
                            detectTapGestures(
                                onPress = { offset ->
                                    isTouching = true
                                    targetTouch = Offset(
                                        (offset.x / size.width).coerceIn(0f, 1f),
                                        (offset.y / size.height).coerceIn(0f, 1f)
                                    )
                                    tryAwaitRelease()
                                    isTouching = false
                                },
                                onTap = { offset ->
                                    if (showRipples) {
                                        ripples.add(ShockwaveRipple(offset.x, offset.y, time))
                                    }
                                }
                            )
                        }
                ) {
                    tick // register recomposition dependency

                    val w = size.width
                    val h = size.height

                    // 1. Resolve Color Palette
                    val (bgTop, bgBottom, accentGlow) = resolveColors(activeScene, palette)

                    // 2. Draw Background Atmospheric Gradient
                    drawRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(bgTop, bgBottom),
                            startY = 0f,
                            endY = h
                        )
                    )

                    // 3. Render Theme-Specific Procedural Shaders
                    when (activeScene) {
                        ShaderScene.DEEP_NEBULA -> drawNebulaScene(
                            w, h, time, touch, glow, particleDensity, turbulence, accentGlow, randomPoints
                        )
                        ShaderScene.BLACK_HOLE -> drawBlackHoleScene(
                            w, h, time, touch, glow, particleDensity, turbulence, accentGlow, randomPoints
                        )
                        ShaderScene.PULSAR_STAR -> drawPulsarScene(
                            w, h, time, touch, glow, particleDensity, turbulence, accentGlow, randomPoints
                        )
                        ShaderScene.QUANTUM_WARP -> drawQuantumWarpScene(
                            w, h, time, touch, glow, particleDensity, turbulence, accentGlow, randomPoints
                        )
                        ShaderScene.SAKURA_DUSK -> drawSakuraScene(
                            w, h, time, touch, glow, particleDensity, turbulence, accentGlow, randomPoints
                        )
                        ShaderScene.CYBERPUNK_TOKYO -> drawCyberpunkTokyoScene(
                            w, h, time, touch, glow, particleDensity, turbulence, accentGlow, randomPoints
                        )
                        ShaderScene.THE_GREAT_WAVE -> drawGreatWaveScene(
                            w, h, time, touch, glow, particleDensity, turbulence, accentGlow, randomPoints
                        )
                        ShaderScene.KOMOREBI_BAMBOO -> drawKomorebiBambooScene(
                            w, h, time, touch, glow, particleDensity, turbulence, accentGlow, randomPoints
                        )
                    }

                    // 4. Render Dynamic Touch Ripples
                    if (showRipples) {
                        ripples.forEach { ripple ->
                            val age = time - ripple.startTime
                            if (age > 0f) {
                                val radius = age * 260f * speed
                                val alpha = (1f - (age / 2.2f)).coerceIn(0f, 1f) * 0.45f * glow
                                if (alpha > 0.01f) {
                                    drawCircle(
                                        color = accentGlow.copy(alpha = alpha),
                                        radius = radius,
                                        center = Offset(ripple.x, ripple.y),
                                        style = Stroke(width = 2.5f * (1f - age / 2.2f))
                                    )
                                }
                            }
                        }
                    }

                    // 5. Interactive Touch Field (Shown only on active tap/press/drag)
                    if (touchAlpha > 0.01f) {
                        val touchPx = Offset(touch.x * w, touch.y * h)
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    accentGlow.copy(alpha = 0.35f * glow * touchAlpha),
                                    accentGlow.copy(alpha = 0.08f * glow * touchAlpha),
                                    Color.Transparent
                                ),
                                center = touchPx,
                                radius = w * 0.25f * touchInfluence
                            ),
                            radius = w * 0.25f * touchInfluence,
                            center = touchPx
                        )
                    }
                }

                // Telemetry HUD Overlay
                if (showHud) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(14.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF0B0D18).copy(alpha = 0.75f))
                            .border(1.dp, Color(0xFF22263D), RoundedCornerShape(10.dp))
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = activeScene.title.uppercase(),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = activeScene.accentColor,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text("•", color = Color.Gray, fontSize = 10.sp)
                                Text(
                                    text = "TOUCH ${(touch.x * 100).toInt()}% / ${(touch.y * 100).toInt()}%",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFFC0C5DF),
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                            Text(
                                text = "TIME ${"%.2f".format(time)}s • SPEED ${"%.1f".format(speed)}x • GLOW ${"%.1f".format(glow)}",
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 10.sp,
                                color = Color(0xFF7E84A3),
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }

                // Interactive Drag Hint Pill
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(14.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF121422).copy(alpha = 0.65f))
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = "Touch & Drag to warp",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFA5ABC4),
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            // Bottom Scene Descriptor Strip
            Surface(
                color = Color(0xFF10121C),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(activeScene.accentColor)
                        )
                        Column {
                            Text(
                                activeScene.title,
                                color = Color.White,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                activeScene.subtitle,
                                color = Color(0xFF868B9E),
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = activeScene.accentColor.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = activeScene.category.title.uppercase(),
                                color = activeScene.accentColor,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    // Settings / Aspects Dialog
    if (settingsOpen) {
        ShaderSettingsDialog(
            scene = activeScene,
            speed = speed,
            glow = glow,
            particleDensity = particleDensity,
            touchInfluence = touchInfluence,
            turbulence = turbulence,
            palette = palette,
            showHud = showHud,
            showRipples = showRipples,
            onSpeedChange = { speed = it },
            onGlowChange = { glow = it },
            onParticleDensityChange = { particleDensity = it },
            onTouchInfluenceChange = { touchInfluence = it },
            onTurbulenceChange = { turbulence = it },
            onPaletteChange = { palette = it },
            onShowHudChange = { showHud = it },
            onShowRipplesChange = { showRipples = it },
            onReset = {
                speed = 0.75f
                glow = 1.0f
                particleDensity = 1.0f
                touchInfluence = 1.0f
                turbulence = 1.0f
                palette = ColorPalette.DEFAULT
                showHud = true
                showRipples = true
            },
            onDismiss = { settingsOpen = false }
        )
    }
}

// ---------------------------------------------------------------------------
// TOOLBAR & SELECTION BAR
// ---------------------------------------------------------------------------

@Composable
fun ShaderToolbar(
    activeCategory: ShaderCategory,
    activeScene: ShaderScene,
    onSelectCategory: (ShaderCategory) -> Unit,
    onSelectScene: (ShaderScene) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Category Switcher Pills
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ShaderCategory.entries.forEach { cat ->
                val isSelected = activeCategory == cat
                Surface(
                    onClick = { onSelectCategory(cat) },
                    color = if (isSelected) Color(0xFF22273D) else Color(0xFF131522),
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (isSelected) Color(0xFF6366F1) else Color.Transparent
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 8.dp, horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(cat.icon, fontSize = 14.sp)
                        Spacer(Modifier.width(6.dp))
                        Text(
                            cat.title,
                            color = if (isSelected) Color.White else Color(0xFF9095AC),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        }

        // Scene Selector Chips
        val categoryScenes = ShaderScene.entries.filter { it.category == activeCategory }
        val scrollState = rememberScrollState()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(scrollState),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            categoryScenes.forEach { scene ->
                val isSelected = activeScene == scene
                Surface(
                    onClick = { onSelectScene(scene) },
                    color = if (isSelected) scene.accentColor.copy(alpha = 0.22f) else Color(0xFF141726),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (isSelected) scene.accentColor else Color(0xFF202438)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(scene.icon, fontSize = 13.sp)
                        Text(
                            scene.title,
                            color = if (isSelected) Color.White else Color(0xFF9EA3BB),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                        )
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// SETTINGS DIALOG (TUNE ASPECTS)
// ---------------------------------------------------------------------------

@Composable
fun ShaderSettingsDialog(
    scene: ShaderScene,
    speed: Float,
    glow: Float,
    particleDensity: Float,
    touchInfluence: Float,
    turbulence: Float,
    palette: ColorPalette,
    showHud: Boolean,
    showRipples: Boolean,
    onSpeedChange: (Float) -> Unit,
    onGlowChange: (Float) -> Unit,
    onParticleDensityChange: (Float) -> Unit,
    onTouchInfluenceChange: (Float) -> Unit,
    onTurbulenceChange: (Float) -> Unit,
    onPaletteChange: (ColorPalette) -> Unit,
    onShowHudChange: (Boolean) -> Unit,
    onShowRipplesChange: (Boolean) -> Unit,
    onReset: () -> Unit,
    onDismiss: () -> Unit
) {
    val scrollState = rememberScrollState()

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF131522),
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Tune Shader Aspects", color = Color.White, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                TextButton(onClick = onReset) {
                    Text("Reset Defaults", color = scene.accentColor, style = MaterialTheme.typography.labelSmall)
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Color Palette Preset Selector
                Text("Color Palette", color = Color.White, style = MaterialTheme.typography.labelMedium)
                val palScroll = rememberScrollState()
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(palScroll),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    ColorPalette.entries.forEach { p ->
                        val selected = palette == p
                        Surface(
                            onClick = { onPaletteChange(p) },
                            color = if (selected) scene.accentColor.copy(alpha = 0.25f) else Color(0xFF1B1E30),
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (selected) scene.accentColor else Color(0xFF282C44)
                            )
                        ) {
                            Text(
                                text = p.title,
                                color = if (selected) Color.White else Color(0xFF9094AB),
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                // Aspect Sliders
                AspectSlider(
                    title = "Animation Speed",
                    value = speed,
                    range = 0.1f..2.5f,
                    displayValue = "${"%.2f".format(speed)}x",
                    accentColor = scene.accentColor,
                    onValueChange = onSpeedChange
                )

                AspectSlider(
                    title = "Glow & Bloom Intensity",
                    value = glow,
                    range = 0.2f..2.0f,
                    displayValue = "${"%.1f".format(glow)}",
                    accentColor = scene.accentColor,
                    onValueChange = onGlowChange
                )

                AspectSlider(
                    title = "Particle & Star Density",
                    value = particleDensity,
                    range = 0.2f..2.0f,
                    displayValue = "${"%.1f".format(particleDensity)}",
                    accentColor = scene.accentColor,
                    onValueChange = onParticleDensityChange
                )

                AspectSlider(
                    title = "Touch Gravity Influence",
                    value = touchInfluence,
                    range = 0.2f..2.0f,
                    displayValue = "${"%.1f".format(touchInfluence)}",
                    accentColor = scene.accentColor,
                    onValueChange = onTouchInfluenceChange
                )

                AspectSlider(
                    title = "Wave & Wind Turbulence",
                    value = turbulence,
                    range = 0.2f..2.0f,
                    displayValue = "${"%.1f".format(turbulence)}",
                    accentColor = scene.accentColor,
                    onValueChange = onTurbulenceChange
                )

                // Switches
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Telemetry HUD Overlay", color = Color(0xFFD4D8EC), style = MaterialTheme.typography.bodySmall)
                    Switch(
                        checked = showHud,
                        onCheckedChange = onShowHudChange,
                        colors = SwitchDefaults.colors(checkedThumbColor = scene.accentColor)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Tap Shockwave Ripples", color = Color(0xFFD4D8EC), style = MaterialTheme.typography.bodySmall)
                    Switch(
                        checked = showRipples,
                        onCheckedChange = onShowRipplesChange,
                        colors = SwitchDefaults.colors(checkedThumbColor = scene.accentColor)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = scene.accentColor)
            ) {
                Text("Apply & Close", color = Color.White)
            }
        }
    )
}

@Composable
fun AspectSlider(
    title: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    displayValue: String,
    accentColor: Color,
    onValueChange: (Float) -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(title, color = Color(0xFFD4D8EC), style = MaterialTheme.typography.bodySmall)
            Text(displayValue, color = accentColor, style = MaterialTheme.typography.labelSmall, fontFamily = FontFamily.Monospace)
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = range,
            modifier = Modifier.height(30.dp)
        )
    }
}

// ---------------------------------------------------------------------------
// COLOR PALETTE RESOLVER
// ---------------------------------------------------------------------------

fun resolveColors(scene: ShaderScene, palette: ColorPalette): Triple<Color, Color, Color> {
    return when (palette) {
        ColorPalette.DEFAULT -> Triple(scene.primaryColor, scene.secondaryColor, scene.accentColor)
        ColorPalette.VIBRANT_CYBER -> Triple(Color(0xFF070B18), Color(0xFF130E2E), Color(0xFF00E5FF))
        ColorPalette.SUNSET_EMBER -> Triple(Color(0xFF1C0A10), Color(0xFF45131C), Color(0xFFFF6D00))
        ColorPalette.CELESTIAL_ICE -> Triple(Color(0xFF06111C), Color(0xFF0B243B), Color(0xFF80D8FF))
        ColorPalette.MONOCHROME_GOLD -> Triple(Color(0xFF12110D), Color(0xFF2B2618), Color(0xFFFFD700))
    }
}

// ---------------------------------------------------------------------------
// PROCEDURAL SHADER RENDERERS (COMPOSE DRAW SCOPE)
// ---------------------------------------------------------------------------

/**
 * COSMOS 1: Deep Nebula (Swirling gas clouds, gravitational vortex, stellar dust)
 */
fun DrawScope.drawNebulaScene(
    w: Float, h: Float, time: Float, touch: Offset,
    glow: Float, particleDensity: Float, turbulence: Float,
    accent: Color, randomPoints: List<Triple<Float, Float, Float>>
) {
    val center = Offset(touch.x * w, touch.y * h)

    // Swirling Volumetric Nebula Gas Blobs
    for (i in 0..4) {
        val angle = time * 0.25f * turbulence + (i * PI.toFloat() * 2f / 5f)
        val dist = w * (0.12f + i * 0.04f)
        val blobCenter = Offset(center.x + cos(angle) * dist, center.y + sin(angle) * dist)
        val radius = w * (0.24f + (i % 2) * 0.08f)
        val blobColor = if (i % 2 == 0) accent else Color(0xFF7C3AED)

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    blobColor.copy(alpha = 0.22f * glow),
                    blobColor.copy(alpha = 0.06f * glow),
                    Color.Transparent
                ),
                center = blobCenter,
                radius = radius
            ),
            radius = radius,
            center = blobCenter
        )
    }

    // 180+ Stardust Particles Attracted to Touch Gravity Well
    val count = (randomPoints.size * 0.75f * particleDensity).toInt()
    randomPoints.take(count).forEachIndexed { i, pt ->
        val origX = pt.first * w
        val origY = pt.second * h
        val dx = center.x - origX
        val dy = center.y - origY
        val dist = sqrt(dx * dx + dy * dy).coerceAtLeast(10f)

        // Orbital pull
        val pullFactor = (1f - (dist / w).coerceIn(0f, 1f)) * 50f * turbulence
        val orbitAngle = time * (0.8f + (i % 5) * 0.2f) + pt.third * 10f
        val px = (origX + (dx / dist) * pullFactor + cos(orbitAngle) * 8f).mod(w)
        val py = (origY + (dy / dist) * pullFactor + sin(orbitAngle) * 8f).mod(h)

        val pulse = 0.35f + 0.65f * sin(time * 2.5f + i).let { it * it }
        val size = if (i % 8 == 0) 2.5f else 1.2f
        drawCircle(
            color = Color.White.copy(alpha = pulse * 0.85f),
            radius = size,
            center = Offset(px, py)
        )
    }
}

/**
 * COSMOS 2: Black Hole (Accretion disk, photon sphere, gravitational spacetime lensing)
 */
fun DrawScope.drawBlackHoleScene(
    w: Float, h: Float, time: Float, touch: Offset,
    glow: Float, particleDensity: Float, turbulence: Float,
    accent: Color, randomPoints: List<Triple<Float, Float, Float>>
) {
    val center = Offset(touch.x * w, touch.y * h)
    val horizonR = w * 0.075f

    // Relativistic Spacetime Grid Lensing Distortion
    for (gridX in 0..12) {
        val gx = w * (gridX / 12f)
        val path = Path()
        path.moveTo(gx, 0f)
        for (step in 0..20) {
            val gy = h * (step / 20f)
            val d = Offset(gx - center.x, gy - center.y)
            val dist = sqrt(d.x * d.x + d.y * d.y)
            val warp = if (dist > horizonR) (horizonR * horizonR / dist.coerceAtLeast(1f)) * 0.4f else 0f
            val wx = gx - (d.x / dist.coerceAtLeast(1f)) * warp
            val wy = gy - (d.y / dist.coerceAtLeast(1f)) * warp
            path.lineTo(wx, wy)
        }
        drawPath(path, Color(0xFF1E2238).copy(alpha = 0.4f), style = Stroke(width = 1f))
    }

    // Glowing Doppler-Shifted Accretion Disk (Tilted Oval)
    val diskW = w * 0.52f
    val diskH = w * 0.16f
    rotate(degrees = -18f, pivot = center) {
        drawOval(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFFFFF7ED),
                    accent.copy(alpha = 0.9f * glow),
                    Color(0xFFDC2626).copy(alpha = 0.6f * glow),
                    Color.Transparent
                ),
                center = center,
                radius = diskW * 0.5f
            ),
            topLeft = Offset(center.x - diskW * 0.5f, center.y - diskH * 0.5f),
            size = Size(diskW, diskH)
        )
    }

    // Infalling Matter Spiral Ribbons
    for (i in 0..3) {
        val armPath = Path()
        val baseAngle = time * 1.8f * turbulence + (i * PI.toFloat() * 0.5f)
        for (step in 0..18) {
            val r = horizonR * 1.1f + step * (w * 0.018f)
            val a = baseAngle + step * 0.28f
            val x = center.x + cos(a) * r
            val y = center.y + sin(a) * r * 0.42f
            if (step == 0) armPath.moveTo(x, y) else armPath.lineTo(x, y)
        }
        drawPath(
            armPath,
            brush = Brush.linearGradient(listOf(Color.White, accent, Color.Transparent)),
            style = Stroke(width = 2.5f, cap = StrokeCap.Round)
        )
    }

    // Pitch Black Singularity / Event Horizon
    drawCircle(color = Color(0xFF030305), radius = horizonR, center = center)

    // Blinding Photon Sphere Ring
    drawCircle(
        color = Color(0xFFFFFBEB).copy(alpha = 0.95f),
        radius = horizonR * 1.05f,
        center = center,
        style = Stroke(width = 2.2f)
    )
}

/**
 * COSMOS 3: Supernova Pulsar (Rotating relativistic light beams, expanding shockwaves)
 */
fun DrawScope.drawPulsarScene(
    w: Float, h: Float, time: Float, touch: Offset,
    glow: Float, particleDensity: Float, turbulence: Float,
    accent: Color, randomPoints: List<Triple<Float, Float, Float>>
) {
    val center = Offset(touch.x * w, touch.y * h)
    val beamAngle = time * 1.2f * turbulence

    // Dual Relativistic Pulsar Light Beams
    val beamLen = max(w, h) * 1.4f
    for (dir in listOf(1f, -1f)) {
        val angle = beamAngle + (if (dir == 1f) 0f else PI.toFloat())
        val beamPath = Path().apply {
            moveTo(center.x, center.y)
            lineTo(
                center.x + cos(angle - 0.16f) * beamLen,
                center.y + sin(angle - 0.16f) * beamLen
            )
            lineTo(
                center.x + cos(angle + 0.16f) * beamLen,
                center.y + sin(angle + 0.16f) * beamLen
            )
            close()
        }
        drawPath(
            beamPath,
            brush = Brush.radialGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.7f * glow),
                    accent.copy(alpha = 0.35f * glow),
                    Color.Transparent
                ),
                center = center,
                radius = beamLen * 0.75f
            )
        )
    }

    // Expanding Magnetosphere Pulse Rings
    for (i in 1..5) {
        val r = ((time * 140f * turbulence + i * 70f) % (w * 0.5f))
        val alpha = (1f - (r / (w * 0.5f))).coerceIn(0f, 1f) * 0.45f * glow
        drawCircle(
            color = accent.copy(alpha = alpha),
            radius = r,
            center = center,
            style = Stroke(width = 2f)
        )
    }

    // High Energy Core Flare
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color.White, accent, Color.Transparent),
            center = center,
            radius = w * 0.08f * glow
        ),
        radius = w * 0.08f * glow,
        center = center
    )
}

/**
 * COSMOS 4: Quantum Warp (3D depth-parallax warp stars, speed streaks)
 */
fun DrawScope.drawQuantumWarpScene(
    w: Float, h: Float, time: Float, touch: Offset,
    glow: Float, particleDensity: Float, turbulence: Float,
    accent: Color, randomPoints: List<Triple<Float, Float, Float>>
) {
    val origin = Offset(touch.x * w, touch.y * h)
    val count = (randomPoints.size * particleDensity).toInt()

    randomPoints.take(count).forEachIndexed { i, pt ->
        val z = ((pt.third + time * 0.35f * turbulence) % 1f).coerceIn(0.01f, 1f)
        val k = 1f / z
        val sx = origin.x + (pt.first - 0.5f) * w * k * 0.4f
        val sy = origin.y + (pt.second - 0.5f) * h * k * 0.4f

        if (sx in 0f..w && sy in 0f..h) {
            val tailLen = (1f - z) * 22f * turbulence
            val alpha = (1f - z).coerceIn(0f, 1f) * 0.9f * glow
            val starColor = if (i % 3 == 0) accent else Color.White

            // Warp streak line
            val dx = sx - origin.x
            val dy = sy - origin.y
            val d = sqrt(dx * dx + dy * dy).coerceAtLeast(1f)
            drawLine(
                color = starColor.copy(alpha = alpha),
                start = Offset(sx, sy),
                end = Offset(sx + (dx / d) * tailLen, sy + (dy / d) * tailLen),
                strokeWidth = (1f - z) * 3f + 1f,
                cap = StrokeCap.Round
            )
        }
    }
}

/**
 * JAPAN 1: Sakura Dusk & Torii Gate (Mount Fuji, sacred Torii, swirling petals)
 */
fun DrawScope.drawSakuraScene(
    w: Float, h: Float, time: Float, touch: Offset,
    glow: Float, particleDensity: Float, turbulence: Float,
    accent: Color, randomPoints: List<Triple<Float, Float, Float>>
) {
    // Twilight Sun / Moon behind Fuji
    val sunCenter = Offset(w * 0.65f, h * 0.38f)
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color(0xFFFFD1B2).copy(alpha = 0.95f),
                Color(0xFFFF7597).copy(alpha = 0.5f * glow),
                Color.Transparent
            ),
            center = sunCenter,
            radius = w * 0.15f
        ),
        radius = w * 0.15f,
        center = sunCenter
    )

    // Mount Fuji Silhouette
    val fujiPath = Path().apply {
        moveTo(0f, h * 0.82f)
        lineTo(w * 0.22f, h * 0.82f)
        lineTo(w * 0.44f, h * 0.44f) // peak left
        lineTo(w * 0.56f, h * 0.44f) // peak right
        lineTo(w * 0.78f, h * 0.82f)
        lineTo(w, h * 0.82f)
        lineTo(w, h)
        lineTo(0f, h)
        close()
    }
    drawPath(fujiPath, Color(0xFF231535))

    // Fuji Snow Cap
    val snowPath = Path().apply {
        moveTo(w * 0.44f, h * 0.44f)
        lineTo(w * 0.56f, h * 0.44f)
        lineTo(w * 0.61f, h * 0.53f)
        lineTo(w * 0.57f, h * 0.51f)
        lineTo(w * 0.53f, h * 0.55f)
        lineTo(w * 0.48f, h * 0.52f)
        lineTo(w * 0.44f, h * 0.56f)
        lineTo(w * 0.39f, h * 0.53f)
        close()
    }
    drawPath(snowPath, Color(0xFFFDE8EF).copy(alpha = 0.92f))

    // Water Surface Reflection
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(Color(0xFF1F102D), Color(0xFF12081B)),
            startY = h * 0.82f,
            endY = h
        ),
        topLeft = Offset(0f, h * 0.82f),
        size = Size(w, h * 0.18f)
    )

    // Torii Gate Silhouette
    val tx = w * 0.28f
    val ty = h * 0.72f
    val gateColor = Color(0xFFBD284E)
    // Top roof
    drawLine(gateColor, Offset(tx - w * 0.11f, ty), Offset(tx + w * 0.11f, ty), 6f, StrokeCap.Round)
    drawLine(gateColor, Offset(tx - w * 0.085f, ty - 6f), Offset(tx + w * 0.085f, ty - 6f), 3.5f, StrokeCap.Round)
    // Columns
    drawLine(gateColor, Offset(tx - w * 0.065f, ty), Offset(tx - w * 0.065f, ty + h * 0.14f), 5f)
    drawLine(gateColor, Offset(tx + w * 0.065f, ty), Offset(tx + w * 0.065f, ty + h * 0.14f), 5f)
    // Crossbar
    drawLine(gateColor, Offset(tx - w * 0.08f, ty + h * 0.035f), Offset(tx + w * 0.08f, ty + h * 0.035f), 4f)

    // 140+ Interactive Cherry Blossom Petals Swirling toward Touch
    val touchPx = Offset(touch.x * w, touch.y * h)
    val petalCount = (randomPoints.size * 0.6f * particleDensity).toInt()

    randomPoints.take(petalCount).forEachIndexed { i, pt ->
        val origX = (pt.first * w + sin(time * 0.8f + i) * 35f).mod(w)
        val origY = (pt.second * h + time * 45f * turbulence + i * 6f).mod(h)

        // Swirl vortex around touch
        val dx = origX - touchPx.x
        val dy = origY - touchPx.y
        val d = sqrt(dx * dx + dy * dy).coerceAtLeast(10f)
        val swirlAngle = atan2(dy, dx) + (1.2f / (d * 0.015f + 1f))
        val vortexR = d * (1f - (0.4f / (d * 0.005f + 1f)))

        val px = if (d < w * 0.35f) touchPx.x + cos(swirlAngle) * vortexR else origX
        val py = if (d < w * 0.35f) touchPx.y + sin(swirlAngle) * vortexR else origY

        // Petal shape (small rotated ellipse)
        val petalRot = time * 2f + i * 25f
        rotate(degrees = petalRot, pivot = Offset(px, py)) {
            drawOval(
                color = Color(0xFFFFB7CE).copy(alpha = 0.85f * glow),
                topLeft = Offset(px - 4f, py - 2.5f),
                size = Size(8f, 5f)
            )
        }
    }
}

/**
 * JAPAN 2: Cyberpunk Tokyo (Rain on glass, glowing neon kanji signs, reflections)
 */
fun DrawScope.drawCyberpunkTokyoScene(
    w: Float, h: Float, time: Float, touch: Offset,
    glow: Float, particleDensity: Float, turbulence: Float,
    accent: Color, randomPoints: List<Triple<Float, Float, Float>>
) {
    // City Skyline Silhouette
    val bldgCount = 14
    for (i in 0 until bldgCount) {
        val bx = w * (i / bldgCount.toFloat())
        val bw = w / bldgCount.toFloat() + 4f
        val bh = h * (0.35f + ((i * 7) % 5) * 0.08f)
        drawRect(Color(0xFF090D1C), Offset(bx, h - bh), Size(bw, bh))
    }

    // Glowing Neon Kanji Signs
    val signY = h * 0.42f
    val signColor = accent
    // Draw sign backing
    drawRect(
        color = Color(0xFF0F172A).copy(alpha = 0.85f),
        topLeft = Offset(w * 0.22f, signY - 20f),
        size = Size(100f, 130f)
    )
    drawRect(
        color = signColor.copy(alpha = 0.8f * glow),
        topLeft = Offset(w * 0.22f, signY - 20f),
        size = Size(100f, 130f),
        style = Stroke(width = 2f)
    )

    // Neon Strokes simulating Kanji glyphs (東 & 京)
    val kx = w * 0.22f + 50f
    drawLine(signColor, Offset(kx - 25f, signY), Offset(kx + 25f, signY), 3.5f * glow)
    drawLine(signColor, Offset(kx, signY - 12f), Offset(kx, signY + 35f), 3.5f * glow)
    drawLine(signColor, Offset(kx - 20f, signY + 15f), Offset(kx + 20f, signY + 15f), 3f * glow)
    drawLine(signColor, Offset(kx - 25f, signY + 35f), Offset(kx + 25f, signY + 35f), 3.5f * glow)

    // Wet Asphalt Reflections
    val roadY = h * 0.78f
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(Color(0xFF0E1325), Color(0xFF05070E)),
            startY = roadY,
            endY = h
        ),
        topLeft = Offset(0f, roadY),
        size = Size(w, h - roadY)
    )

    // Neon Reflection on Wet Road
    drawOval(
        brush = Brush.radialGradient(
            colors = listOf(signColor.copy(alpha = 0.25f * glow), Color.Transparent),
            center = Offset(kx, roadY + 30f),
            radius = 120f
        ),
        topLeft = Offset(kx - 120f, roadY),
        size = Size(240f, 60f)
    )

    // Falling Rain Streaks
    val rainCount = (randomPoints.size * 1.1f * particleDensity).toInt()
    randomPoints.take(rainCount).forEachIndexed { i, pt ->
        val rx = (pt.first * w + sin(time * 0.5f) * 15f).mod(w)
        val ry = (pt.second * h + time * 380f * turbulence + i * 14f).mod(h)
        val len = 16f + (i % 4) * 6f

        drawLine(
            color = Color(0xFF7DD3FC).copy(alpha = 0.45f * glow),
            start = Offset(rx, ry),
            end = Offset(rx - 3f, ry + len),
            strokeWidth = 1.2f
        )
    }
}

/**
 * JAPAN 3: The Great Wave (Hokusai-style curled crests, ocean currents)
 */
fun DrawScope.drawGreatWaveScene(
    w: Float, h: Float, time: Float, touch: Offset,
    glow: Float, particleDensity: Float, turbulence: Float,
    accent: Color, randomPoints: List<Triple<Float, Float, Float>>
) {
    val seaLevel = h * 0.62f

    // Layer 1: Deep Background Ocean Swell
    val path1 = Path().apply {
        moveTo(0f, h)
        lineTo(0f, seaLevel)
        for (step in 0..24) {
            val x = w * (step / 24f)
            val y = seaLevel + sin(step * 0.35f + time * 1.2f * turbulence) * 25f
            lineTo(x, y)
        }
        lineTo(w, h)
        close()
    }
    drawPath(path1, Color(0xFF0F2642))

    // Layer 2: Main Rising Wave with Hokusai Claw Crest
    val wavePath = Path().apply {
        moveTo(0f, h)
        lineTo(0f, seaLevel + 30f)
        var px = 0f
        var py = seaLevel
        for (step in 0..30) {
            px = w * (step / 30f)
            val waveHeight = 70f * sin(step * 0.28f + time * 1.5f * turbulence)
            py = seaLevel - waveHeight * (px / w) * 1.8f
            lineTo(px, py)
        }
        lineTo(w, h)
        close()
    }
    drawPath(wavePath, Color(0xFF1E3A8A))

    // Wave Foam Crest (Curling Sea Claws)
    for (i in 0..8) {
        val cx = w * (0.35f + i * 0.07f)
        val cy = seaLevel - 45f * sin(i * 0.4f + time * 1.5f)
        drawCircle(
            color = Color(0xFFF8FAFC).copy(alpha = 0.95f),
            radius = 7f + (i % 3) * 3f,
            center = Offset(cx, cy)
        )
    }

    // Interactive Sea Spray Droplets
    val sprayCount = (randomPoints.size * 0.5f * particleDensity).toInt()
    randomPoints.take(sprayCount).forEachIndexed { i, pt ->
        val sx = (pt.first * w + sin(time * 2f + i) * 20f).mod(w)
        val sy = (seaLevel - 80f + pt.second * 90f + cos(time + i) * 15f)
        drawCircle(
            color = Color(0xFFE2E8F0).copy(alpha = 0.85f * glow),
            radius = 1.8f + (i % 2) * 1.2f,
            center = Offset(sx, sy)
        )
    }
}

/**
 * JAPAN 4: Komorebi Bamboo Sanctuary (Sunlight beams, bamboo stalks, fireflies)
 */
fun DrawScope.drawKomorebiBambooScene(
    w: Float, h: Float, time: Float, touch: Offset,
    glow: Float, particleDensity: Float, turbulence: Float,
    accent: Color, randomPoints: List<Triple<Float, Float, Float>>
) {
    // Soft Volumetric Sunbeams (Komorebi light rays)
    for (i in 0..4) {
        val rayPath = Path().apply {
            val startX = w * (0.15f + i * 0.2f)
            moveTo(startX, 0f)
            lineTo(startX + 40f, 0f)
            lineTo(startX + 140f, h)
            lineTo(startX + 60f, h)
            close()
        }
        drawPath(
            rayPath,
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color(0xFFFEF08A).copy(alpha = 0.18f * glow),
                    Color(0xFF86EFAC).copy(alpha = 0.05f * glow),
                    Color.Transparent
                )
            )
        )
    }

    // Towering Bamboo Stalks
    val stalkCount = 10
    for (i in 0 until stalkCount) {
        val bx = w * (i / stalkCount.toFloat()) + 15f
        val bw = 16f + (i % 3) * 6f
        val stalkColor = if (i % 2 == 0) Color(0xFF14532D) else Color(0xFF166534)

        drawRect(stalkColor, Offset(bx, 0f), Size(bw, h))
        // Bamboo Nodes (horizontal joints)
        for (node in 0..7) {
            val ny = h * (node / 7f)
            drawLine(Color(0xFF052E16), Offset(bx - 2f, ny), Offset(bx + bw + 2f, ny), 3f)
        }
    }

    // Drifting Interactive Fireflies (Swarming toward touch)
    val touchPx = Offset(touch.x * w, touch.y * h)
    val flyCount = (randomPoints.size * 0.4f * particleDensity).toInt()

    randomPoints.take(flyCount).forEachIndexed { i, pt ->
        val origX = (pt.first * w + sin(time * 0.9f + i) * 45f).mod(w)
        val origY = (pt.second * h + cos(time * 0.7f + i) * 45f).mod(h)

        // Attraction to touch
        val dx = touchPx.x - origX
        val dy = touchPx.y - origY
        val d = sqrt(dx * dx + dy * dy).coerceAtLeast(1f)
        val pull = (1f - (d / (w * 0.6f)).coerceIn(0f, 1f)) * 40f

        val fx = origX + (dx / d) * pull
        val fy = origY + (dy / d) * pull

        val pulse = 0.4f + 0.6f * sin(time * 3f + i * 2f).let { it * it }
        val flyColor = Color(0xFFA3E635)

        // Firefly glow halo
        drawCircle(
            color = flyColor.copy(alpha = pulse * 0.35f * glow),
            radius = 12f,
            center = Offset(fx, fy)
        )
        // Firefly core
        drawCircle(
            color = Color.White.copy(alpha = pulse * 0.95f),
            radius = 2.5f,
            center = Offset(fx, fy)
        )
    }
}
