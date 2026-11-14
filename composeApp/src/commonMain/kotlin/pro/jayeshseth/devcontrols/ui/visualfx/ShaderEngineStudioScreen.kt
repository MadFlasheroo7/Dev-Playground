package pro.jayeshseth.devcontrols.ui.visualfx

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.shaderengine.audio.rememberAudioSpectrumManager
import io.github.shaderengine.camera.rememberCameraStreamSampler
import io.github.shaderengine.core.rememberShaderController
import io.github.shaderengine.modifiers.generativeShaderCanvas
import io.github.shaderengine.modifiers.shaderPointerInput
import io.github.shaderengine.modifiers.shaderRenderEffect
import io.github.shaderengine.pipeline.buildShaderPipeline
import io.github.shaderengine.pipeline.shaderPipeline
import io.github.shaderengine.presets.AudioCymaticsPreset
import io.github.shaderengine.presets.ChromaticFerrofluidPreset
import io.github.shaderengine.presets.CosmicNebulaPreset
import io.github.shaderengine.presets.CyberpunkHologramPreset
import io.github.shaderengine.presets.FrostedAcrylicPreset
import io.github.shaderengine.presets.FrostedPrismGlassPreset
import io.github.shaderengine.presets.LiquidMercuryPreset
import io.github.shaderengine.presets.MagnifyingDomePreset
import io.github.shaderengine.presets.NightVisionPreset
import io.github.shaderengine.presets.RainyWindowPreset
import io.github.shaderengine.presets.RaymarchedCorePreset
import io.github.shaderengine.presets.ShockwaveRipplePreset
import io.github.shaderengine.presets.ThermalFlirPreset
import io.github.shaderengine.presets.TransitionCategory
import io.github.shaderengine.presets.TransitionPreset
import io.github.shaderengine.transitions.ShaderTransitionLayout

enum class StudioMode {
    LENS,
    CANVAS,
    TRANSITIONS,
    EDITOR,
    PIPELINE
}

enum class StudioPreset(
    val title: String,
    val isFilterMode: Boolean,
    val shaderCode: String
) {
    FERROFLUID("Ferrofluid [LENS]", true, ChromaticFerrofluidPreset.SHADER),
    PRISM_GLASS("Prism Glass [LENS]", true, FrostedPrismGlassPreset.SHADER),
    SHOCKWAVE("Kinetic Ripple [LENS]", true, ShockwaveRipplePreset.SHADER),
    FROSTED_ACRYLIC("Frosted Acrylic [LENS]", true, FrostedAcrylicPreset.SHADER),
    CYBERPUNK_HOLOGRAM("Cyberpunk CRT [LENS]", true, CyberpunkHologramPreset.SHADER),
    MAGNIFYING_DOME("Magnifying Loupe [LENS]", true, MagnifyingDomePreset.SHADER),
    THERMAL_FLIR("Thermal FLIR [LENS]", true, ThermalFlirPreset.SHADER),
    RAINY_WINDOW("Rainy Glass [LENS]", true, RainyWindowPreset.SHADER),
    NIGHT_VISION("Night Vision [LENS]", true, NightVisionPreset.SHADER),
    QUANTUM_CORE("Quantum Core 3D [CANVAS]", false, RaymarchedCorePreset.SHADER),
    COSMIC_NEBULA("Cosmic Nebula [CANVAS]", false, CosmicNebulaPreset.SHADER),
    LIQUID_MERCURY("Liquid Mercury [CANVAS]", false, LiquidMercuryPreset.SHADER),
    CYMATICS("Audio Cymatics [CANVAS]", false, AudioCymaticsPreset.SHADER)
}

/**
 * Interactive Studio & Playground showcasing the full capabilities of :shader-engine:
 * - UI Post-Processing Filters (filtering live, clickable Compose trees).
 * - Generative Canvases (fullscreen procedural raymarching & fluid math).
 * - Zero-recomposition state pipes running at 60/120 FPS.
 * - Dynamic uniform tuning inspector and AGSL code viewer.
 */
@Composable
fun ShaderEngineStudioScreen(onBack: () -> Unit) {
    var studioMode by remember { mutableStateOf(StudioMode.LENS) }
    var activePreset by remember { mutableStateOf(StudioPreset.FERROFLUID) }
    var activeTransitionPreset by remember { mutableStateOf(TransitionPreset.LIQUID_MELT) }
    var selectedTransitionCategory by remember { mutableStateOf<TransitionCategory?>(null) }
    var transitionProgress by remember { mutableFloatStateOf(0.0f) }
    var alphaCounter by remember { mutableIntStateOf(0) }
    var alphaToggle by remember { mutableStateOf(true) }
    var betaCounter by remember { mutableIntStateOf(0) }
    var betaToggle by remember { mutableStateOf(false) }
    var showCodeDrawer by remember { mutableStateOf(false) }

    // Tunable Parameters
    var p1 by remember { mutableFloatStateOf(0.55f) }
    var p2 by remember { mutableFloatStateOf(0.15f) }
    var p3 by remember { mutableFloatStateOf(0.10f) }

    // Active controller for the selected shader
    val controller = rememberShaderController(activePreset.shaderCode)
    val audioManager = rememberAudioSpectrumManager()
    val cameraSampler = rememberCameraStreamSampler()
    var arViewfinderEnabled by remember { mutableStateOf(false) }

    // Multi-Pass Pipeline State
    var pass1BlurEnabled by remember { mutableStateOf(true) }
    var pass2LoupeEnabled by remember { mutableStateOf(true) }
    var pass3ScanlinesEnabled by remember { mutableStateOf(true) }

    val multiPassPipeline = remember(pass1BlurEnabled, pass2LoupeEnabled, pass3ScanlinesEnabled, p1, p2) {
        buildShaderPipeline {
            if (pass1BlurEnabled) pass(FrostedAcrylicPreset.SHADER)
            if (pass2LoupeEnabled) pass(MagnifyingDomePreset.SHADER) {
                setFloatUniform("p1", p1)
                setFloatUniform("p2", p2)
            }
            if (pass3ScanlinesEnabled) pass(CyberpunkHologramPreset.SHADER)
        }
    }

    // Interactive under-layer state for filter mode
    var clickCounter by remember { mutableIntStateOf(0) }
    var toggleState by remember { mutableStateOf(true) }

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xF507090E))
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Row 1: Back, Title, Badges
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TextButton(
                            onClick = onBack,
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Text(
                                "← Back",
                                color = Color(0xFF38BDF8),
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 14.sp
                            )
                        }
                        Text(
                            "SHADER STUDIO",
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 15.sp
                        )
                    }

                    // Code toggle button
                    if (studioMode != StudioMode.EDITOR) {
                        TextButton(
                            onClick = { showCodeDrawer = !showCodeDrawer },
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                            modifier = Modifier.height(28.dp)
                        ) {
                            Text(
                                if (showCodeDrawer) "Hide" else "</> AGSL",
                                color = Color(0xFFF59E0B),
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        }
                    }
                }

                // Row 1b: Mode Selector Tabs (Horizontally Scrollable)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    StudioMode.entries.forEach { mode ->
                        val isSelected = studioMode == mode
                        Box(
                            modifier = Modifier
                                .clickable {
                                    studioMode = mode
                                    when (mode) {
                                        StudioMode.LENS -> if (!activePreset.isFilterMode) activePreset = StudioPreset.FERROFLUID
                                        StudioMode.CANVAS -> if (activePreset.isFilterMode) activePreset = StudioPreset.QUANTUM_CORE
                                        StudioMode.TRANSITIONS -> {}
                                        StudioMode.EDITOR -> {}
                                        StudioMode.PIPELINE -> {}
                                    }
                                }
                                .background(
                                    if (isSelected) {
                                        when (mode) {
                                            StudioMode.LENS -> Color(0xFF1E1B4B)
                                            StudioMode.CANVAS -> Color(0xFF064E3B)
                                            StudioMode.TRANSITIONS -> Color(0xFF3B1D54)
                                            StudioMode.EDITOR -> Color(0xFF1E3A8A)
                                            StudioMode.PIPELINE -> Color(0xFF831843)
                                        }
                                    } else Color(0xFF0F172A),
                                    RoundedCornerShape(8.dp)
                                )
                                .border(
                                    1.dp,
                                    if (isSelected) {
                                        when (mode) {
                                            StudioMode.LENS -> Color(0xFF6366F1)
                                            StudioMode.CANVAS -> Color(0xFF10B981)
                                            StudioMode.TRANSITIONS -> Color(0xFFA855F7)
                                            StudioMode.EDITOR -> Color(0xFF38BDF8)
                                            StudioMode.PIPELINE -> Color(0xFFF43F5E)
                                        }
                                    } else Color(0xFF334155),
                                    RoundedCornerShape(8.dp)
                                )
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Text(
                                text = mode.name,
                                color = if (isSelected) Color.White else Color(0xFF94A3B8),
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        }
                    }
                }

                // Row 2: Live Metrics HUD (Scrollable row)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Zero Recompositions badge
                    Box(
                        modifier = Modifier
                            .background(Color(0xFF0F172A), RoundedCornerShape(8.dp))
                            .border(1.dp, Color(0xFF334155), RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            "0 RECOMPS",
                            color = Color(0xFF38BDF8),
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp
                        )
                    }

                    if (studioMode == StudioMode.TRANSITIONS) {
                        Box(
                            modifier = Modifier
                                .background(Color(0xFF0F172A), RoundedCornerShape(8.dp))
                                .border(1.dp, Color(0xFFF59E0B), RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                "CAT: ${activeTransitionPreset.category.title.uppercase()}",
                                color = Color(0xFFFBBF24),
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp
                            )
                        }

                        Box(
                            modifier = Modifier
                                .background(Color(0xFF0F172A), RoundedCornerShape(8.dp))
                                .border(1.dp, Color(0xFFA855F7), RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                "PROGRESS: ${(transitionProgress * 100).toInt()}%",
                                color = Color(0xFFC084FC),
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp
                            )
                        }

                        if (activeTransitionPreset.isCanvas) {
                            Box(
                                modifier = Modifier
                                    .background(Color(0xFF0F172A), RoundedCornerShape(8.dp))
                                    .border(1.dp, Color(0xFF10B981), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    "RENDER: PURE CANVAS",
                                    color = Color(0xFF34D399),
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .background(Color(0xFF0F172A), RoundedCornerShape(8.dp))
                                    .border(1.dp, Color(0xFF06B6D4), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    "ZERO AGSL (API 21+)",
                                    color = Color(0xFF22D3EE),
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp
                                )
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .background(Color(0xFF0F172A), RoundedCornerShape(8.dp))
                                    .border(1.dp, Color(0xFF059669), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    "DUAL GPU INPUTS",
                                    color = Color(0xFF34D399),
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .background(Color(0xFF0F172A), RoundedCornerShape(8.dp))
                                    .border(1.dp, Color(0xFF0284C7), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    "120 FPS NATIVE AGSL",
                                    color = Color(0xFF38BDF8),
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp
                                )
                            }
                        }
                    } else {
                        // Motion / tilt HUD badge
                        Box(
                            modifier = Modifier
                                .background(Color(0xFF0F172A), RoundedCornerShape(8.dp))
                                .border(1.dp, Color(0xFF059669), RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            val rDeg = (controller.tilt.x * 57.29578f).toInt()
                            val pDeg = (controller.tilt.y * 57.29578f).toInt()
                            Text(
                                "TILT R:${rDeg}° P:${pDeg}°",
                                color = Color(0xFF34D399),
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp
                            )
                        }

                        // Specular Boost Status badge
                        Box(
                            modifier = Modifier
                                .background(
                                    if (toggleState) Color(0xFF1E293B) else Color(0xFF0F172A),
                                    RoundedCornerShape(8.dp)
                                )
                                .border(
                                    1.dp,
                                    if (toggleState) Color(0xFF0284C7) else Color(0xFF334155),
                                    RoundedCornerShape(8.dp)
                                )
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                if (toggleState) "BOOST: 2.5x" else "BOOST: 1.0x",
                                color = if (toggleState) Color(0xFF38BDF8) else Color(0xFF94A3B8),
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp
                            )
                        }
                    }
                }

                // Row 3: Category Filter Chips (Transitions mode only)
                if (studioMode == StudioMode.TRANSITIONS) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val categories = listOf<TransitionCategory?>(null) + TransitionCategory.entries
                        categories.forEach { cat ->
                            val isSelected = selectedTransitionCategory == cat
                            val label = cat?.title ?: "All (${TransitionPreset.entries.size})"
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedTransitionCategory = cat },
                                label = {
                                    Text(
                                        label,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 10.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFF4C1D95),
                                    selectedLabelColor = Color.White,
                                    containerColor = Color(0xFF0F172A),
                                    labelColor = Color(0xFF94A3B8)
                                )
                            )
                        }
                    }
                }

                // Row 4: Preset Selector Chips
                if (studioMode != StudioMode.EDITOR && studioMode != StudioMode.PIPELINE) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (studioMode == StudioMode.TRANSITIONS) {
                            val filteredPresets = if (selectedTransitionCategory == null) {
                                TransitionPreset.entries
                            } else {
                                TransitionPreset.entries.filter { it.category == selectedTransitionCategory }
                            }
                            filteredPresets.forEach { preset ->
                                val isSelected = activeTransitionPreset == preset
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { activeTransitionPreset = preset },
                                    label = { Text(preset.title, fontFamily = FontFamily.Monospace, fontSize = 11.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = Color(0xFF7E22CE),
                                        selectedLabelColor = Color.White,
                                        containerColor = Color(0xFF0F172A),
                                        labelColor = Color(0xFF94A3B8)
                                    )
                                )
                            }
                        } else {
                            val availablePresets = StudioPreset.entries.filter {
                                if (studioMode == StudioMode.LENS) it.isFilterMode else !it.isFilterMode
                            }
                            availablePresets.forEach { preset ->
                                val isSelected = activePreset == preset
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        activePreset = preset
                                        when (preset) {
                                            StudioPreset.FERROFLUID -> { p1 = 0.55f; p2 = 0.15f; p3 = 0.10f }
                                            StudioPreset.PRISM_GLASS -> { p1 = 0.45f; p2 = 0.50f; p3 = 0.20f }
                                            StudioPreset.SHOCKWAVE -> { p1 = 0.60f; p2 = 0.40f; p3 = 0.50f }
                                            StudioPreset.FROSTED_ACRYLIC -> { p1 = 0.50f; p2 = 0.35f; p3 = 0.60f }
                                            StudioPreset.CYBERPUNK_HOLOGRAM -> { p1 = 0.45f; p2 = 0.65f; p3 = 0.40f }
                                            StudioPreset.MAGNIFYING_DOME -> { p1 = 1.75f; p2 = 0.28f; p3 = 0.03f }
                                            StudioPreset.THERMAL_FLIR -> { p1 = 1.20f; p2 = 0.25f; p3 = 0.08f }
                                            StudioPreset.RAINY_WINDOW -> { p1 = 1.00f; p2 = 0.08f; p3 = 0.40f }
                                            StudioPreset.NIGHT_VISION -> { p1 = 1.80f; p2 = 0.15f; p3 = 0.60f }
                                            StudioPreset.QUANTUM_CORE -> { p1 = 0.50f; p2 = 0.40f; p3 = 0.60f }
                                            StudioPreset.COSMIC_NEBULA -> { p1 = 0.65f; p2 = 0.50f; p3 = 0.70f }
                                            StudioPreset.LIQUID_MERCURY -> { p1 = 0.55f; p2 = 0.85f; p3 = 0.50f }
                                            StudioPreset.CYMATICS -> { p1 = 1.0f; p2 = 4.0f; p3 = 0.15f }
                                        }
                                    },
                                    label = { Text(preset.title, fontFamily = FontFamily.Monospace, fontSize = 11.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = Color(0xFF0284C7),
                                        selectedLabelColor = Color.White,
                                        containerColor = Color(0xFF0F172A),
                                        labelColor = Color(0xFF94A3B8)
                                    )
                                )
                            }
                        }
                    }
                }
            }
        },
        containerColor = Color(0xFF07090E)
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = innerPadding.calculateTopPadding())
        ) {
            // -------------------------------------------------------------
            // MAIN STAGE
            // -------------------------------------------------------------
            if (studioMode == StudioMode.EDITOR) {
                // MODE D: Interactive On-Device AGSL / SkSL Live Editor
                ShaderLiveEditorScreen(modifier = Modifier.fillMaxSize())
            } else if (studioMode == StudioMode.PIPELINE) {
                // MODE E: Multi-Pass RenderEffect Pipeline
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .shaderPointerInput(controller)
                        .shaderPipeline(multiPassPipeline, controller)
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val step = 36.dp.toPx()
                        val gridColor = Color(0xFF3B1D54)
                        var x = 0f
                        while (x < size.width) {
                            drawLine(gridColor, Offset(x, 0f), Offset(x, size.height), strokeWidth = 1.5f)
                            x += step
                        }
                        var y = 0f
                        while (y < size.height) {
                            drawLine(gridColor, Offset(0f, y), Offset(size.width, y), strokeWidth = 1.5f)
                            y += step
                        }
                    }
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 20.dp, vertical = 12.dp)
                            .padding(bottom = 240.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "STAGE // MULTI-PASS PIPELINE CHAIN",
                            color = Color(0xFFF43F5E),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "Sequential GPU chaining: Pass 1 (Kawase Blur) → Pass 2 (Loupe) → Pass 3 (CRT Scanlines)",
                            color = Color(0xFFCBD5E1),
                            fontSize = 13.sp
                        )
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth().border(1.dp, Color(0xFFF43F5E), RoundedCornerShape(16.dp))
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("Pipeline Target Node", color = Color.White, fontWeight = FontWeight.Bold)
                                    Text("Taps registered: $clickCounter", color = Color(0xFFF43F5E), fontFamily = FontFamily.Monospace, fontSize = 13.sp)
                                }
                                Button(
                                    onClick = { clickCounter++ },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE11D48))
                                ) {
                                    Text("Tap Me", color = Color.White, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                        val frame = cameraSampler.currentFrame
                        if (frame != null) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(180.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .border(1.dp, Color(0xFFF43F5E), RoundedCornerShape(12.dp))
                            ) {
                                Canvas(modifier = Modifier.fillMaxSize()) {
                                    drawImage(
                                        image = frame,
                                        dstSize = androidx.compose.ui.unit.IntSize(size.width.toInt(), size.height.toInt())
                                    )
                                }
                                Text(
                                    "CAMERA STREAM SAMPLER (PIPELINE INPUT)",
                                    color = Color(0xFFF43F5E),
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp,
                                    modifier = Modifier.align(Alignment.TopStart).padding(8.dp)
                                )
                            }
                        }
                    }
                }
            } else if (studioMode == StudioMode.TRANSITIONS) {
                // MODE C: Dual-Input Transitions
                Box(
                    modifier = Modifier.fillMaxSize()
                ) {
                    ShaderTransitionLayout(
                        progress = transitionProgress,
                        preset = activeTransitionPreset,
                        onProgressChange = { transitionProgress = it },
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 20.dp, vertical = 12.dp)
                            .padding(bottom = 220.dp),
                        fromContent = {
                            NodeAlphaCard(
                                counter = alphaCounter,
                                onIncrement = { alphaCounter++ },
                                overdrive = alphaToggle,
                                onToggleOverdrive = { alphaToggle = it }
                            )
                        },
                        toContent = {
                            NodeBetaCard(
                                pingCount = betaCounter,
                                onPing = { betaCounter++ },
                                streaming = betaToggle,
                                onToggleStreaming = { betaToggle = it }
                            )
                        }
                    )
                }
            } else if (activePreset.isFilterMode) {
                // MODE A: Live Compose UI Filter
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .shaderPointerInput(controller)
                        .shaderRenderEffect(controller) {
                            val specularBoostValue = if (toggleState) 2.5f else 1.0f
                            float("uSpecularBoost", specularBoostValue)
                            when (activePreset) {
                                StudioPreset.FERROFLUID -> {
                                    float("dispersion", p1)
                                    float("warpIntensity", p2)
                                    float("surfaceTension", p3)
                                }
                                StudioPreset.PRISM_GLASS -> {
                                    float("refraction", p1)
                                    float("dispersion", p2)
                                    float("roughness", p3)
                                }
                                StudioPreset.SHOCKWAVE -> {
                                    float("amplitude", p1)
                                    float("frequency", p2)
                                    float("dispersion", p3)
                                }
                                StudioPreset.FROSTED_ACRYLIC -> {
                                    float("blurRadius", p1)
                                    float("noiseDither", p2)
                                    float("tintOpacity", p3)
                                }
                                StudioPreset.CYBERPUNK_HOLOGRAM -> {
                                    float("barrelWarp", p1)
                                    float("scanlineDark", p2)
                                    float("glitchRate", p3)
                                }
                                StudioPreset.MAGNIFYING_DOME,
                                StudioPreset.THERMAL_FLIR,
                                StudioPreset.RAINY_WINDOW,
                                StudioPreset.NIGHT_VISION -> {
                                    float("p1", p1)
                                    float("p2", p2)
                                    float("p3", p3)
                                }
                                else -> {}
                            }
                        }
                ) {
                    // Background grid pattern
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val step = 36.dp.toPx()
                        val gridColor = Color(0xFF1E293B)
                        var x = 0f
                        while (x < size.width) {
                            drawLine(gridColor, Offset(x, 0f), Offset(x, size.height), strokeWidth = 1.5f)
                            x += step
                        }
                        var y = 0f
                        while (y < size.height) {
                            drawLine(gridColor, Offset(0f, y), Offset(size.width, y), strokeWidth = 1.5f)
                            y += step
                        }
                    }

                    // Interactive Composable Tree placed UNDER the optical shader lens
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 20.dp, vertical = 12.dp)
                            .padding(bottom = 240.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                    Text(
                        text = "STAGE // LIVE COMPOSE NODES",
                        color = Color(0xFF64748B),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )

                    Text(
                        text = "You can click, toggle, and interact with the UI below directly THROUGH the shader lens!",
                        color = Color(0xFFCBD5E1),
                        fontSize = 14.sp
                    )

                    // Interactive Card 1: Clickable Counter
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth().border(1.dp, Color(0xFF334155), RoundedCornerShape(16.dp))
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp).fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Interactive Node", color = Color.White, fontWeight = FontWeight.Bold)
                                Text("Taps registered: $clickCounter", color = Color(0xFF38BDF8), fontFamily = FontFamily.Monospace, fontSize = 13.sp)
                            }
                            Button(
                                onClick = { clickCounter++ },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7))
                            ) {
                                Text("Tap Me", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // Interactive Card 2: Switch & Toggles
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth().border(1.dp, Color(0xFF334155), RoundedCornerShape(16.dp))
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp).fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Dynamic Specular Boost", color = Color.White, fontWeight = FontWeight.Bold)
                                Text(if (toggleState) "Overdrive ACTIVE" else "Standard pass", color = Color(0xFF94A3B8), fontSize = 12.sp)
                            }
                            Switch(
                                checked = toggleState,
                                onCheckedChange = { toggleState = it },
                                colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF38BDF8))
                            )
                        }
                    }

                    // Interactive Card 3: Camera / Video AR Viewfinder Feed
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth().border(1.dp, Color(0xFF334155), RoundedCornerShape(16.dp))
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("AR Viewfinder Stream", color = Color.White, fontWeight = FontWeight.Bold)
                                    Text(
                                        if (arViewfinderEnabled) "SMPTE radar test pattern ACTIVE" else "Simulated Camera OFF",
                                        color = if (arViewfinderEnabled) Color(0xFF10B981) else Color(0xFF94A3B8),
                                        fontSize = 12.sp
                                    )
                                }
                                Switch(
                                    checked = arViewfinderEnabled,
                                    onCheckedChange = { arViewfinderEnabled = it },
                                    colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF10B981))
                                )
                            }
                            if (arViewfinderEnabled) {
                                val frame = cameraSampler.currentFrame
                                if (frame != null) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(180.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .border(1.dp, Color(0xFF10B981), RoundedCornerShape(12.dp))
                                    ) {
                                        Canvas(modifier = Modifier.fillMaxSize()) {
                                            drawImage(
                                                image = frame,
                                                dstSize = androidx.compose.ui.unit.IntSize(size.width.toInt(), size.height.toInt())
                                            )
                                        }
                                        Text(
                                            "● LIVE AR VIEW (120 FPS)",
                                            color = Color.Green,
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            modifier = Modifier.align(Alignment.TopStart).padding(8.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Feature highlights
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        SpecBadge("SNELL IOR", "Refraction split", Modifier.weight(1f))
                        SpecBadge("SCHLICK FRESNEL", "Fast FP16 multiply", Modifier.weight(1f))
                        SpecBadge("ALPHA INVARIANT", "min(rgb, half3(a))", Modifier.weight(1f))
                    }
                }
            }
        } else {
            // MODE B: Fullscreen Generative Canvas
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .shaderPointerInput(controller)
                    .generativeShaderCanvas(controller) {
                        val specularBoostValue = if (toggleState) 2.5f else 1.0f
                        float("uSpecularBoost", specularBoostValue)
                        when (activePreset) {
                            StudioPreset.QUANTUM_CORE -> {
                                float("complexity", p1)
                                float("glow", p2)
                                float("speed", p3)
                            }
                            StudioPreset.COSMIC_NEBULA -> {
                                float("turbulence", p1)
                                float("vortex", p2)
                                float("speed", p3)
                            }
                            StudioPreset.LIQUID_MERCURY -> {
                                float("viscosity", p1)
                                float("chromeShininess", p2)
                                float("rippleSpeed", p3)
                            }
                            StudioPreset.CYMATICS -> {
                                float("uBass", audioManager.bass)
                                float("uMid", audioManager.mid)
                                float("uTreble", audioManager.treble)
                                float("uAudioEnergy", audioManager.energy)
                                float("p1", p1)
                                float("p2", p2)
                                float("p3", p3)
                            }
                            else -> {}
                        }
                    }
            )
        }

        // -------------------------------------------------------------
        // BOTTOM INSPECTOR: Dynamic Uniform Controls
        // -------------------------------------------------------------
        if (studioMode != StudioMode.EDITOR) {
            Column(
                modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(16.dp)
                .background(Color(0xEB0F172A), RoundedCornerShape(20.dp))
                .border(1.dp, Color(0xFF334155), RoundedCornerShape(20.dp))
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (studioMode == StudioMode.PIPELINE) {
                Text(
                    "MULTI-PASS SHADER PIPELINE",
                    color = Color(0xFFF43F5E),
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Pass 1: Dual Kawase Blur", color = Color.White, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                    Switch(
                        checked = pass1BlurEnabled,
                        onCheckedChange = { pass1BlurEnabled = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFFF43F5E))
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Pass 2: Refractive Spherical Loupe", color = Color.White, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                    Switch(
                        checked = pass2LoupeEnabled,
                        onCheckedChange = { pass2LoupeEnabled = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFFF43F5E))
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Pass 3: CRT Phosphor Scanlines", color = Color.White, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                    Switch(
                        checked = pass3ScanlinesEnabled,
                        onCheckedChange = { pass3ScanlinesEnabled = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFFF43F5E))
                    )
                }
                if (pass2LoupeEnabled) {
                    StudioSlider("Loupe Magnification", p1, 1.0f..3.0f) { p1 = it }
                    StudioSlider("Loupe Radius", p2, 0.1f..0.6f) { p2 = it }
                }
            } else if (studioMode == StudioMode.TRANSITIONS) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = { transitionProgress = 0f },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.height(28.dp)
                    ) {
                        Text("◀ Node Alpha (0.0)", color = Color(0xFF38BDF8), fontFamily = FontFamily.Monospace, fontSize = 10.sp)
                    }
                    Text(
                        text = "SCRUB: ${(transitionProgress * 100).toInt()}%",
                        color = Color(0xFFA855F7),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Button(
                        onClick = { transitionProgress = 1f },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.height(28.dp)
                    ) {
                        Text("Node Beta (1.0) ▶", color = Color(0xFF34D399), fontFamily = FontFamily.Monospace, fontSize = 10.sp)
                    }
                }

                val scrubLabel = if (activeTransitionPreset.isCanvas) "Pure Canvas Scrub" else "Dual-Input AGSL Scrub"
                StudioSlider(scrubLabel, transitionProgress, 0f..1f) {
                    transitionProgress = it
                }

                Text(
                    text = if (activeTransitionPreset.isCanvas) {
                        "Rendered purely with Compose DrawScope & clipPath. 100% backward compatible (API 21+)."
                    } else {
                        "Swipe horizontally directly on the card to scrub with spring physics."
                    },
                    color = Color(0xFF64748B),
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
            } else {
                when (activePreset) {
                    StudioPreset.FERROFLUID -> {
                        StudioSlider("Chromatic Dispersion", p1, 0f..1.5f) { p1 = it }
                        StudioSlider("fBm Warp Turbulence", p2, 0f..0.4f) { p2 = it }
                        StudioSlider("Surface Tension (smin)", p3, 0.02f..0.25f) { p3 = it }
                    }
                    StudioPreset.PRISM_GLASS -> {
                        StudioSlider("Refraction Index", p1, 0f..1.0f) { p1 = it }
                        StudioSlider("Prism Dispersion", p2, 0f..1.0f) { p2 = it }
                        StudioSlider("Surface Roughness", p3, 0f..1.0f) { p3 = it }
                    }
                    StudioPreset.SHOCKWAVE -> {
                        StudioSlider("Wave Amplitude", p1, 0f..1.0f) { p1 = it }
                        StudioSlider("Ring Frequency", p2, 0f..1.0f) { p2 = it }
                        StudioSlider("Chromatic Aberration", p3, 0f..1.0f) { p3 = it }
                    }
                    StudioPreset.FROSTED_ACRYLIC -> {
                        StudioSlider("Dual Kawase Blur", p1, 0f..1.0f) { p1 = it }
                        StudioSlider("Blue-Noise Dither", p2, 0f..1.0f) { p2 = it }
                        StudioSlider("Surface Tint Opacity", p3, 0f..1.0f) { p3 = it }
                    }
                    StudioPreset.CYBERPUNK_HOLOGRAM -> {
                        StudioSlider("CRT Barrel Curvature", p1, 0f..1.0f) { p1 = it }
                        StudioSlider("Scanline Intensity", p2, 0f..1.0f) { p2 = it }
                        StudioSlider("Sync Glitch Rate", p3, 0f..1.0f) { p3 = it }
                    }
                    StudioPreset.MAGNIFYING_DOME -> {
                        StudioSlider("Magnification Power", p1, 1.0f..2.5f) { p1 = it }
                        StudioSlider("Dome Lens Radius", p2, 0.15f..0.45f) { p2 = it }
                        StudioSlider("Chromatic Edge Dispersion", p3, 0.0f..0.05f) { p3 = it }
                    }
                    StudioPreset.THERMAL_FLIR -> {
                        StudioSlider("Thermal Contrast", p1, 0.5f..2.0f) { p1 = it }
                        StudioSlider("Scanline Darkening", p2, 0.0f..0.4f) { p2 = it }
                        StudioSlider("Sensor Noise Grain", p3, 0.0f..0.2f) { p3 = it }
                    }
                    StudioPreset.RAINY_WINDOW -> {
                        StudioSlider("Droplet Density", p1, 0.5f..2.0f) { p1 = it }
                        StudioSlider("Refraction Strength", p2, 0.02f..0.15f) { p2 = it }
                        StudioSlider("Condensation Mist", p3, 0.0f..1.0f) { p3 = it }
                    }
                    StudioPreset.NIGHT_VISION -> {
                        StudioSlider("Photon Gain", p1, 1.0f..3.0f) { p1 = it }
                        StudioSlider("Scintillation Noise", p2, 0.05f..0.35f) { p2 = it }
                        StudioSlider("Barrel Vignette", p3, 0.2f..0.9f) { p3 = it }
                    }
                    StudioPreset.QUANTUM_CORE -> {
                        StudioSlider("Gyroid Complexity", p1, 0f..1.0f) { p1 = it }
                        StudioSlider("Volumetric Glow", p2, 0f..1.0f) { p2 = it }
                        StudioSlider("Rotation Speed", p3, 0f..1.0f) { p3 = it }
                    }
                    StudioPreset.COSMIC_NEBULA -> {
                        StudioSlider("Domain Turbulence", p1, 0f..1.0f) { p1 = it }
                        StudioSlider("Vortex Attraction", p2, 0f..1.0f) { p2 = it }
                        StudioSlider("Fluid Speed", p3, 0f..1.0f) { p3 = it }
                    }
                    StudioPreset.LIQUID_MERCURY -> {
                        StudioSlider("Liquid Viscosity", p1, 0f..1.0f) { p1 = it }
                        StudioSlider("Chrome Polish", p2, 0f..1.0f) { p2 = it }
                        StudioSlider("Capillary Ripple", p3, 0f..1.0f) { p3 = it }
                    }
                    StudioPreset.CYMATICS -> {
                        StudioSlider("Harmonic Mode Gain", p1, 0.5f..2.0f) { p1 = it }
                        StudioSlider("Nodal Line Sharpness", p2, 1.0f..12.0f) { p2 = it }
                        StudioSlider("Resonance Dispersion", p3, 0.0f..0.4f) { p3 = it }
                    }
                }
            }
        }
    }

        // -------------------------------------------------------------
        // AGSL SOURCE CODE DRAWER / MODAL
        // -------------------------------------------------------------
        AnimatedVisibility(
            visible = showCodeDrawer,
            modifier = Modifier
                .align(Alignment.Center)
                .navigationBarsPadding()
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xF5050811)),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
                    .border(1.5.dp, Color(0xFFF59E0B), RoundedCornerShape(20.dp))
            ) {
                Column(modifier = Modifier.padding(20.dp).fillMaxSize()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            val codeTitle = if (studioMode == StudioMode.TRANSITIONS) {
                                "${activeTransitionPreset.title.uppercase()} TRANSITION"
                            } else {
                                activePreset.title.uppercase()
                            }
                            Text("AGSL SOURCE CODE // $codeTitle", color = Color(0xFFF59E0B), fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, fontSize = 14.sp)
                            Text("Preprocessed automatically for Skia / SkSL on Desktop, iOS, Web", color = Color(0xFF94A3B8), fontSize = 12.sp)
                        }
                        Button(
                            onClick = { showCodeDrawer = false },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155))
                        ) {
                            Text("Close", color = Color.White)
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFF030712), RoundedCornerShape(12.dp))
                            .border(1.dp, Color(0xFF1F2937), RoundedCornerShape(12.dp))
                            .padding(16.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        val code = if (studioMode == StudioMode.TRANSITIONS) {
                            activeTransitionPreset.shaderCode
                        } else {
                            activePreset.shaderCode
                        }
                        Text(
                            text = code.trimIndent(),
                            color = Color(0xFF38BDF8),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            lineHeight = 16.sp
                        )
                    }
                }
            }
        }
    }
}
}

@Composable
private fun NodeAlphaCard(
    counter: Int,
    onIncrement: () -> Unit,
    overdrive: Boolean,
    onToggleOverdrive: (Boolean) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0B132B)),
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.5.dp, Color(0xFF1E3A8A), RoundedCornerShape(24.dp))
            .padding(1.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(20.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(Color(0xFF38BDF8), CircleShape)
                    )
                    Text(
                        "NODE ALPHA // PROFILE",
                        color = Color(0xFF38BDF8),
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
                Box(
                    modifier = Modifier
                        .background(Color(0xFF1E293B), RoundedCornerShape(6.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text("SYNC: 100%", color = Color(0xFF6EE7B7), fontFamily = FontFamily.Monospace, fontSize = 10.sp)
                }
            }

            // Identity info
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .background(Color(0xFF1E293B), CircleShape)
                        .border(2.dp, Color(0xFF0284C7), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Ω", color = Color(0xFF38BDF8), fontSize = 26.sp, fontWeight = FontWeight.Black)
                }
                Column {
                    Text("JAYESH SETH", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 17.sp)
                    Text("Lead Systems Architect", color = Color(0xFF94A3B8), fontSize = 13.sp)
                    Text("AGSL & SkSL Graphics Core", color = Color(0xFF38BDF8), fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                }
            }

            // Telemetry Badges
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SpecBadge("UPTIME", "99.99%", Modifier.weight(1f))
                SpecBadge("LATENCY", "1.2 ms", Modifier.weight(1f))
                SpecBadge("SEC-LVL", "OMEGA", Modifier.weight(1f))
            }

            // Interactive Counter
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .padding(12.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Node Alpha Tap Engine", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text("Interactive touches: $counter", color = Color(0xFF38BDF8), fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                    }
                    Button(
                        onClick = onIncrement,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("+ Tap", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }

            // Interactive Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Quantum Overdrive Link", color = Color.White, fontWeight = FontWeight.Medium, fontSize = 13.sp)
                    Text(if (overdrive) "Active (2.5x pipeline)" else "Standard pass", color = Color(0xFF94A3B8), fontSize = 11.sp)
                }
                Switch(
                    checked = overdrive,
                    onCheckedChange = onToggleOverdrive,
                    colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF38BDF8))
                )
            }
        }
    }
}

@Composable
private fun NodeBetaCard(
    pingCount: Int,
    onPing: () -> Unit,
    streaming: Boolean,
    onToggleStreaming: (Boolean) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF06231A)),
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.5.dp, Color(0xFF059669), RoundedCornerShape(24.dp))
            .padding(1.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(20.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(Color(0xFF10B981), CircleShape)
                    )
                    Text(
                        "NODE BETA // TELEMETRY",
                        color = Color(0xFF34D399),
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
                Box(
                    modifier = Modifier
                        .background(Color(0xFF064E3B), RoundedCornerShape(6.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text("ACTIVE PASS", color = Color(0xFF6EE7B7), fontFamily = FontFamily.Monospace, fontSize = 10.sp)
                }
            }

            // System Metrics readout
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF0F172A), RoundedCornerShape(12.dp))
                    .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(12.dp))
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text("GPU TELEMETRY // REALTIME STREAM", color = Color(0xFFF59E0B), fontFamily = FontFamily.Monospace, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Text("• Pipeline Target: 120 FPS [8.33ms frametime]", color = Color(0xFFE2E8F0), fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                Text("• Input 1: uniform shader imageA (Layer A)", color = Color(0xFF38BDF8), fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                Text("• Input 2: uniform shader imageB (Layer B)", color = Color(0xFF34D399), fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                Text("• Recomposition Cost: 0 Recomps / Frame", color = Color(0xFFA78BFA), fontFamily = FontFamily.Monospace, fontSize = 11.sp)
            }

            // Diagnostic Badges
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SpecBadge("VRAM", "1.4 MB", Modifier.weight(1f))
                SpecBadge("CHANNELS", "RGBA_8888", Modifier.weight(1f))
                SpecBadge("SHADING", "1.0x FP16", Modifier.weight(1f))
            }

            // Diagnostic Action
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .padding(12.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Bus Diagnostic Ping", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text("Signals acknowledged: $pingCount", color = Color(0xFF34D399), fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                    }
                    Button(
                        onClick = onPing,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Ping Bus", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }

            // Streaming Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Continuous Telemetry Stream", color = Color.White, fontWeight = FontWeight.Medium, fontSize = 13.sp)
                    Text(if (streaming) "Broadcasting UDP loop" else "Telemetry paused", color = Color(0xFF94A3B8), fontSize = 11.sp)
                }
                Switch(
                    checked = streaming,
                    onCheckedChange = onToggleStreaming,
                    colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF34D399))
                )
            }
        }
    }
}

@Composable
private fun SpecBadge(title: String, subtitle: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .background(Color(0xFF0F172A), RoundedCornerShape(12.dp))
            .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(12.dp))
            .padding(10.dp)
    ) {
        Column {
            Text(title, color = Color(0xFF38BDF8), fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 10.sp)
            Text(subtitle, color = Color(0xFF94A3B8), fontSize = 11.sp)
        }
    }
}

@Composable
private fun StudioSlider(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, color = Color(0xFFE2E8F0), fontSize = 12.sp, fontFamily = FontFamily.Monospace)
            Text(
                text = ((value * 100).toInt() / 100f).toString(),
                color = Color(0xFF38BDF8),
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = range,
            colors = SliderDefaults.colors(
                thumbColor = Color(0xFF38BDF8),
                activeTrackColor = Color(0xFF0284C7)
            ),
            modifier = Modifier.height(28.dp)
        )
    }
}
