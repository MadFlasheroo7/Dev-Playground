package pro.jayeshseth.devcontrols.ui.composevideo

import androidx.compose.animation.AnimatedContent
import kotlinx.coroutines.launch
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pro.jayeshseth.devcontrols.composevideo.*
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

// ─────────────────────────────────────────────────────────────────────────────
// Platform-side export hook (expect/actual)
// ─────────────────────────────────────────────────────────────────────────────

expect suspend fun platformExportComposeVideo(
    controller: ComposeVideoController,
    onProgress: (Float) -> Unit,
    onComplete: (String) -> Unit,
    onError: (String) -> Unit,
)

expect fun supportsAudioPicker(): Boolean
expect fun supportsAudioTrack(): Boolean

// ─────────────────────────────────────────────────────────────────────────────
// Animation Template Enum
// ─────────────────────────────────────────────────────────────────────────────

enum class AnimationTemplate(val title: String, val icon: String) {
    COSMIC_AURORA("Cosmic Aurora", "🌌"),
    NEON_GRID("Cyberpunk Grid", "⚡"),
    PARTICLE_NEXUS("Particle Nexus", "🧬"),
    LIQUID_GLASS("Liquid Morph", "💎"),
    CUBE_3D("3D Wireframe", "🧊"),
    SPECTRUM_WAVE("Spectrum Wave", "🌊"),
}

// Global active template holder so renderer can access it
var selectedAnimationTemplate by mutableStateOf(AnimationTemplate.COSMIC_AURORA)

// ─────────────────────────────────────────────────────────────────────────────
// ComposeVideoScreen — fully cross-platform
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComposeVideoScreen(onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    val controller = rememberComposeVideoController(fps = 30, durationMs = 5_000L)

    var activeTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Preview", "Effects", "Audio", "Export")

    var showSuccessDialog by remember { mutableStateOf(false) }
    var showErrorDialog by remember { mutableStateOf(false) }
    var successPath by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "ComposeVideo { }",
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 16.sp
                        )
                        Text(
                            "Compose → MP4 Exporter",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    TextButton(onClick = onBack) { Text("Back") }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { inner ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(inner)
                .background(MaterialTheme.colorScheme.background)
        ) {
            PrimaryTabRow(selectedTabIndex = activeTab) {
                tabs.forEachIndexed { i, title ->
                    Tab(selected = activeTab == i, onClick = { activeTab = i },
                        text = { Text(title, fontSize = 13.sp) })
                }
            }

            AnimatedContent(
                targetState = activeTab,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "cvTab"
            ) { tab ->
                when (tab) {
                    0 -> PreviewTab(controller)
                    1 -> EffectsTab(controller)
                    2 -> AudioTab(controller)
                    3 -> ExportTab(
                        controller = controller,
                        onExport = {
                            scope.launch {
                                controller.isExporting = true
                                controller.exportProgress = 0f
                                platformExportComposeVideo(
                                    controller = controller,
                                    onProgress = { controller.exportProgress = it },
                                    onComplete = { path ->
                                        controller.isExporting = false
                                        successPath = path
                                        showSuccessDialog = true
                                    },
                                    onError = { err ->
                                        controller.isExporting = false
                                        errorMsg = err
                                        showErrorDialog = true
                                    }
                                )
                            }
                        }
                    )
                }
            }
        }
    }

    if (showSuccessDialog) {
        AlertDialog(
            onDismissRequest = { showSuccessDialog = false },
            title = { Text("✅ Export Complete!", fontWeight = FontWeight.Bold) },
            text = { Text("Saved to:\n\n$successPath", style = MaterialTheme.typography.bodyMedium) },
            confirmButton = { Button(onClick = { showSuccessDialog = false }) { Text("Done") } }
        )
    }
    if (showErrorDialog) {
        AlertDialog(
            onDismissRequest = { showErrorDialog = false },
            title = { Text("⚠ Export Failed", fontWeight = FontWeight.Bold) },
            text = { Text(errorMsg, style = MaterialTheme.typography.bodyMedium) },
            confirmButton = { Button(onClick = { showErrorDialog = false }) { Text("OK") } }
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Tab 0: Preview
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun PreviewTab(controller: ComposeVideoController) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Animation Template Selector
        Text("Animation Preset", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(AnimationTemplate.entries.toList()) { tmpl ->
                val isSelected = selectedAnimationTemplate == tmpl
                FilterChip(
                    selected = isSelected,
                    onClick = { selectedAnimationTemplate = tmpl },
                    label = { Text("${tmpl.icon} ${tmpl.title}", fontSize = 12.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFF6C7BFF),
                        selectedLabelColor = Color.White
                    )
                )
            }
        }

        // Preview canvas with live ComposeVideo wrapper
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF080812))
                .border(1.dp, Color(0xFF6C7BFF).copy(alpha = 0.4f), RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center
        ) {
            ComposeVideo(
                controller = controller,
                modifier = Modifier.fillMaxSize(),
            ) {
                SampleAnimationContent(progress = progress, timeMs = timeMs)
            }

            // Time overlay
            Box(
                Modifier
                    .align(Alignment.TopStart).padding(10.dp)
                    .background(Color(0x88000000), RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    "${(controller.currentProgress * 100).toInt()}% · ${controller.currentTimeMs}ms",
                    color = Color.White, fontSize = 11.sp, fontFamily = FontFamily.Monospace
                )
            }
            // Resolution badge
            Box(
                Modifier
                    .align(Alignment.TopEnd).padding(10.dp)
                    .background(Color(0x886C7BFF), RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(controller.resolution.label, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }

        // Transport controls
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(onClick = { controller.isPlaying = false; controller.currentTimeMs = 0L },
                shape = CircleShape, contentPadding = PaddingValues(12.dp)) {
                Text("⏮", fontSize = 18.sp)
            }
            Spacer(Modifier.width(12.dp))
            Button(
                onClick = { controller.isPlaying = !controller.isPlaying },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6C7BFF)),
                shape = CircleShape,
                contentPadding = PaddingValues(16.dp),
                modifier = Modifier.size(56.dp)
            ) {
                Text(if (controller.isPlaying) "⏸" else "▶", fontSize = 20.sp, color = Color.White)
            }
            Spacer(Modifier.width(12.dp))
            OutlinedButton(
                onClick = { controller.isLooping = !controller.isLooping },
                shape = CircleShape,
                contentPadding = PaddingValues(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = if (controller.isLooping) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                )
            ) {
                Text("🔁", fontSize = 18.sp)
            }
        }

        // Timeline scrubber
        TimelineScrubber(controller)

        // Duration presets & FPS
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Duration Preset", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                Text("${controller.durationMs / 1000f}s (${controller.durationMs}ms)", style = MaterialTheme.typography.labelMedium, color = Color(0xFF6C7BFF))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(3000L to "3s", 5000L to "5s", 10000L to "10s", 15000L to "15s", 30000L to "30s").forEach { (d, label) ->
                    FilterChip(
                        selected = controller.durationMs == d,
                        onClick = { controller.durationMs = d },
                        label = { Text(label, fontSize = 12.sp) }
                    )
                }
            }
            Slider(value = controller.durationMs.toFloat(), onValueChange = { controller.durationMs = it.toLong() }, valueRange = 1000f..30_000f)
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(Modifier.weight(1f)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("FPS", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                    Text("${controller.fps}", style = MaterialTheme.typography.labelMedium, color = Color(0xFF6C7BFF))
                }
                Slider(value = controller.fps.toFloat(), onValueChange = { controller.fps = it.toInt() }, valueRange = 12f..60f, steps = 3)
            }
        }

        // Info strip
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))) {
            Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                InfoChip("Frames", "${controller.totalFrames}")
                InfoChip("FPS", "${controller.fps}")
                InfoChip("Duration", "${"%.1f".format(controller.durationMs / 1000f)}s")
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Tab 1: Effects
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun EffectsTab(controller: ComposeVideoController) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Text("Post-Processing Effects", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

        EffectCard("🎨 Colour Grading") {
            EffectSlider(
                "Brightness",
                controller.brightness,
                0.2f..3f,
                "${"%.2f".format(controller.brightness)}"
            ) { controller.brightness = it }

            EffectSlider(
                "Contrast",
                controller.contrast,
                0.2f..3f,
                "${"%.2f".format(controller.contrast)}"
            ) { controller.contrast = it }

            EffectSlider(
                "Saturation",
                controller.saturation,
                0f..3f,
                "${"%.2f".format(controller.saturation)}"
            ) { controller.saturation = it }

            Text("Quick Presets", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val presets = listOf(
                    "Vivid" to Triple(1.1f, 1.2f, 1.4f),
                    "Warm" to Triple(1.05f, 1.0f, 0.85f),
                    "Mono" to Triple(1.0f, 1.0f, 0.0f),
                    "Fade" to Triple(0.85f, 0.8f, 0.7f),
                    "Punchy" to Triple(1.2f, 1.5f, 1.3f),
                    "Reset" to Triple(1.0f, 1.0f, 1.0f),
                )
                items(presets) { (label, v) ->
                    FilterChip(
                        selected = false,
                        onClick = {
                            controller.brightness = v.first
                            controller.contrast = v.second
                            controller.saturation = v.third
                        },
                        label = { Text(label, fontSize = 12.sp) }
                    )
                }
            }
        }

        EffectCard("🌀 Blur") {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("Box Blur", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                    Text("Applied per-frame during export", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(checked = controller.blurEnabled, onCheckedChange = { controller.blurEnabled = it })
            }
            AnimatedVisibility(controller.blurEnabled) {
                EffectSlider(
                    "Blur Radius",
                    controller.blurRadius.toFloat(),
                    1f..25f,
                    "${controller.blurRadius} px"
                ) { controller.blurRadius = it.toInt() }
            }
        }

        EffectCard("✂️ Trim") {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("Trim In/Out Points", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                    Text("Export only a segment", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(checked = controller.trimEnabled, onCheckedChange = { controller.trimEnabled = it })
            }
            AnimatedVisibility(controller.trimEnabled) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    EffectSlider(
                        "In Point",
                        controller.trimStart,
                        0f..(controller.trimEnd - 0.05f).coerceAtLeast(0f),
                        "${"%.1f".format(controller.trimStart * controller.durationMs / 1000f)}s"
                    ) { controller.trimStart = it }

                    EffectSlider(
                        "Out Point",
                        controller.trimEnd,
                        (controller.trimStart + 0.05f).coerceAtMost(1f)..1f,
                        "${"%.1f".format(controller.trimEnd * controller.durationMs / 1000f)}s"
                    ) { controller.trimEnd = it }

                    Text(
                        "Segment: ${"%.1f".format((controller.trimEnd - controller.trimStart) * controller.durationMs / 1000f)}s",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF6C7BFF)
                    )
                }
            }
        }

        AnimatedVisibility(controller.effects.isNotEmpty()) {
            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF0E0E1E))) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Active Effects", color = Color(0xFF6C7BFF), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
                    controller.effects.forEach { fx ->
                        Text("• ${effectSummary(fx)}", color = Color.White.copy(alpha = 0.8f), style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace)
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Tab 2: Audio
// ─────────────────────────────────────────────────────────────────────────────

@Composable
expect fun AudioTab(controller: ComposeVideoController)

// ─────────────────────────────────────────────────────────────────────────────
// Tab 3: Export
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ExportTab(controller: ComposeVideoController, onExport: () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Text("Export Settings", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

        OutlinedTextField(
            value = controller.outputFileName,
            onValueChange = { controller.outputFileName = it },
            label = { Text("Output filename") },
            suffix = { Text(".mp4") },
            modifier = Modifier.fillMaxWidth(),
            enabled = !controller.isExporting,
            singleLine = true,
        )

        Text("Resolution", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(VideoResolution.entries.toList()) { res ->
                val isSelected = controller.resolution == res
                val bg by animateColorAsState(if (isSelected) Color(0xFF6C7BFF) else MaterialTheme.colorScheme.surfaceVariant, label = "resBg")
                Card(modifier = Modifier.clickable(enabled = !controller.isExporting) { controller.resolution = res },
                    colors = CardDefaults.cardColors(containerColor = bg)) {
                    Column(Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                        Text(res.label, fontWeight = FontWeight.Bold, color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface, fontSize = 13.sp)
                        Text("${res.width}×${res.height}", color = if (isSelected) Color.White.copy(0.8f) else MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                    }
                }
            }
        }

        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Export Summary", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                SummaryRow("Preset", "${selectedAnimationTemplate.icon} ${selectedAnimationTemplate.title}")
                SummaryRow("Resolution", "${controller.resolution.width} × ${controller.resolution.height}")
                SummaryRow("Frame Rate", "${controller.fps} fps")
                SummaryRow("Duration", "${controller.durationMs}ms (${"%.1f".format(controller.durationMs / 1000f)}s)")
                SummaryRow("Total Frames", "${controller.totalFrames}")
                SummaryRow("Audio", if (controller.audioUriString != null) "✓ Included" else "✗ None")
                SummaryRow("Effects", "${controller.effects.size}")
                SummaryRow("Format", "MP4 (H.264 Video)")
            }
        }

        AnimatedVisibility(controller.isExporting) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                val p by animateFloatAsState(controller.exportProgress, spring(), label = "ep")
                LinearProgressIndicator(progress = { p }, modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)), color = Color(0xFF6C7BFF))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(controller.exportPhase, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${(p * 100).toInt()}%", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = Color(0xFF6C7BFF))
                }
            }
        }

        Button(
            onClick = onExport,
            enabled = !controller.isExporting && controller.outputFileName.isNotBlank(),
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6C7BFF)),
            shape = RoundedCornerShape(14.dp),
        ) {
            if (controller.isExporting) {
                CircularProgressIndicator(Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                Spacer(Modifier.width(12.dp))
                Text("Exporting…", color = Color.White, fontWeight = FontWeight.Bold)
            } else {
                Text("⬇  Export MP4 Video", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Rich Sample Animation Renderer (supports 6 distinct templates)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun SampleAnimationContent(progress: Float, timeMs: Long) {
    Canvas(Modifier.fillMaxSize()) {
        renderTemplateAnimation(selectedAnimationTemplate, progress, timeMs)
    }
}

/** Draws the selected animation template on any Canvas DrawScope. */
fun DrawScope.renderTemplateAnimation(
    template: AnimationTemplate,
    progress: Float,
    timeMs: Long
) {
    val w = size.width
    val h = size.height

    when (template) {
        AnimationTemplate.COSMIC_AURORA -> {
            // Dark cosmic background
            drawRect(Color(0xFF070614))
            val angle = progress * 360f

            // Pulsating gradient aurora sweeps
            rotate(degrees = angle, pivot = Offset(w * 0.5f, h * 0.5f)) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFF8B5CF6).copy(0.7f), Color(0xFF3B82F6).copy(0.3f), Color.Transparent),
                        center = Offset(w * 0.3f, h * 0.4f),
                        radius = w * 0.7f
                    ),
                    center = Offset(w * 0.3f, h * 0.4f),
                    radius = w * 0.7f
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFFEC4899).copy(0.6f), Color(0xFF06B6D4).copy(0.2f), Color.Transparent),
                        center = Offset(w * 0.7f, h * 0.6f),
                        radius = w * 0.6f
                    ),
                    center = Offset(w * 0.7f, h * 0.6f),
                    radius = w * 0.6f
                )
            }

            // Star particles
            for (i in 0..60) {
                val px = ((i * 137.5f) % w)
                val py = ((i * 219.3f) % h)
                val starAlpha = (sin((progress * 2 * PI + i).toFloat()) * 0.5f + 0.5f)
                drawCircle(Color.White.copy(alpha = starAlpha), radius = 2f + (i % 3), center = Offset(px, py))
            }
        }

        AnimationTemplate.NEON_GRID -> {
            // Cyberpunk background
            drawRect(Color(0xFF090014))

            // Horizon glow
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFFD946EF).copy(0.4f), Color.Transparent),
                    startY = h * 0.3f, endY = h * 0.7f
                )
            )

            // Perspective Grid
            val gridYStart = h * 0.45f
            val vp = Offset(w * 0.5f, gridYStart)

            // Perspective lines
            for (i in -10..10) {
                val startX = vp.x + i * (w / 12f)
                drawLine(
                    color = Color(0xFF00F0FF).copy(0.5f),
                    start = vp,
                    end = Offset(startX, h),
                    strokeWidth = 1.5f
                )
            }

            // Moving horizontal grid bars
            val speed = (progress * 10f) % 1f
            for (j in 1..12) {
                val yFrac = ((j + speed) / 12f)
                val lineY = gridYStart + (h - gridYStart) * (yFrac * yFrac)
                val alpha = (yFrac * 0.8f).coerceIn(0f, 1f)
                drawLine(
                    color = Color(0xFFFF007F).copy(alpha),
                    start = Offset(0f, lineY),
                    end = Offset(w, lineY),
                    strokeWidth = 2f
                )
            }

            // Glowing sun
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFFFFEE00), Color(0xFFFF0055), Color.Transparent),
                    center = vp, radius = h * 0.25f
                ),
                center = vp, radius = h * 0.25f
            )
        }

        AnimationTemplate.PARTICLE_NEXUS -> {
            drawRect(Color(0xFF030812))
            val count = 24
            val points = mutableListOf<Offset>()

            for (i in 0 until count) {
                val a = progress * 2 * PI.toFloat() + (i * 2 * PI.toFloat() / count)
                val r = (h * 0.3f) + sin(a * 3f) * (h * 0.08f)
                val x = w * 0.5f + cos(a) * r
                val y = h * 0.5f + sin(a) * r
                points.add(Offset(x, y))
            }

            // Connect nearest nodes
            for (i in points.indices) {
                for (j in i + 1 until points.size) {
                    val p1 = points[i]
                    val p2 = points[j]
                    val dist = (p1 - p2).getDistance()
                    if (dist < w * 0.25f) {
                        val alpha = (1f - dist / (w * 0.25f)) * 0.6f
                        drawLine(
                            color = Color(0xFF38BDF8).copy(alpha = alpha),
                            start = p1, end = p2, strokeWidth = 1f
                        )
                    }
                }
            }

            // Draw nodes
            points.forEachIndexed { idx, pt ->
                val hue = (idx * 15f + progress * 360f) % 360f
                drawCircle(Color.hsv(hue, 0.9f, 1f), radius = 6f, center = pt)
                drawCircle(Color.White, radius = 3f, center = pt)
            }
        }

        AnimationTemplate.LIQUID_GLASS -> {
            drawRect(Color(0xFF0C0A1D))
            val hue1 = (progress * 360f) % 360f
            val hue2 = (hue1 + 120f) % 360f

            // Liquid blobs
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color.hsv(hue1, 0.8f, 0.9f).copy(0.7f), Color.Transparent),
                    center = Offset(w * 0.35f + sin(progress * 2 * PI.toFloat()) * w * 0.1f, h * 0.4f),
                    radius = w * 0.45f
                ),
                center = Offset(w * 0.35f, h * 0.4f), radius = w * 0.45f
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color.hsv(hue2, 0.8f, 0.9f).copy(0.7f), Color.Transparent),
                    center = Offset(w * 0.65f - cos(progress * 2 * PI.toFloat()) * w * 0.1f, h * 0.6f),
                    radius = w * 0.4f
                ),
                center = Offset(w * 0.65f, h * 0.6f), radius = w * 0.4f
            )

            // Concentric glowing glass rings
            val ringCount = 5
            for (r in 1..ringCount) {
                val radius = (r / ringCount.toFloat()) * (h * 0.38f)
                val strokeW = 3f + (r % 2) * 2f
                drawCircle(
                    color = Color.White.copy(alpha = 0.2f + sin(progress * 2 * PI.toFloat() + r) * 0.15f),
                    radius = radius,
                    center = Offset(w * 0.5f, h * 0.5f),
                    style = Stroke(width = strokeW)
                )
            }
        }

        AnimationTemplate.CUBE_3D -> {
            drawRect(Color(0xFF050B14))
            val rotX = (progress * 2 * PI.toFloat())
            val rotY = (progress * 4 * PI.toFloat())

            val size3d = h * 0.25f
            val vertices = listOf(
                floatArrayOf(-1f, -1f, -1f), floatArrayOf(1f, -1f, -1f),
                floatArrayOf(1f, 1f, -1f), floatArrayOf(-1f, 1f, -1f),
                floatArrayOf(-1f, -1f, 1f), floatArrayOf(1f, -1f, 1f),
                floatArrayOf(1f, 1f, 1f), floatArrayOf(-1f, 1f, 1f)
            )

            val projected = vertices.map { v ->
                val x0 = v[0]; val y0 = v[1]; val z0 = v[2]
                // Y rotation
                val x1 = x0 * cos(rotY) + z0 * sin(rotY)
                val z1 = -x0 * sin(rotY) + z0 * cos(rotY)
                // X rotation
                val y2 = y0 * cos(rotX) - z1 * sin(rotX)
                val z2 = y0 * sin(rotX) + z1 * cos(rotX)
                // Perspective projection
                val distance = 3.5f
                val fz = 1f / (distance + z2)
                Offset(w * 0.5f + x1 * fz * size3d * 2f, h * 0.5f + y2 * fz * size3d * 2f)
            }

            val edges = listOf(
                0 to 1, 1 to 2, 2 to 3, 3 to 0,
                4 to 5, 5 to 6, 6 to 7, 7 to 4,
                0 to 4, 1 to 5, 2 to 6, 3 to 7
            )

            edges.forEach { (i, j) ->
                drawLine(
                    color = Color(0xFF00FFCC),
                    start = projected[i],
                    end = projected[j],
                    strokeWidth = 3f,
                    cap = StrokeCap.Round
                )
            }

            projected.forEach { pt ->
                drawCircle(Color.White, radius = 5f, center = pt)
            }
        }

        AnimationTemplate.SPECTRUM_WAVE -> {
            drawRect(Color(0xFF0A0518))
            val bars = 36
            val barWidth = w / bars

            for (i in 0 until bars) {
                val phase = (i / bars.toFloat()) * 4 * PI.toFloat() + (progress * 2 * PI.toFloat())
                val heightFrac = (sin(phase) * 0.4f + 0.5f).coerceIn(0.1f, 0.95f)
                val barH = heightFrac * (h * 0.6f)
                val x = i * barWidth
                val y = h * 0.5f - barH / 2f
                val hue = (i * 10f + progress * 360f) % 360f

                drawRect(
                    brush = Brush.verticalGradient(listOf(Color.hsv(hue, 0.9f, 1f), Color.hsv(hue, 0.9f, 0.3f))),
                    topLeft = Offset(x + barWidth * 0.15f, y),
                    size = Size(barWidth * 0.7f, barH)
                )
            }

            // Dynamic wave line
            val wavePath = Path()
            for (x in 0..w.toInt() step 5) {
                val p = (x / w) * 4 * PI.toFloat() + (progress * 2 * PI.toFloat())
                val y = h * 0.5f + sin(p) * (h * 0.25f)
                if (x == 0) wavePath.moveTo(x.toFloat(), y) else wavePath.lineTo(x.toFloat(), y)
            }
            drawPath(wavePath, color = Color.White, style = Stroke(width = 3f))
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Shared UI helpers
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun TimelineScrubber(controller: ComposeVideoController) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("0s", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("${"%.1f".format(controller.durationMs / 1000f)}s", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Box(
            Modifier
                .fillMaxWidth()
                .height(36.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                .pointerInput(controller.durationMs) {
                    detectTapGestures { offset ->
                        controller.isPlaying = false
                        controller.currentTimeMs = ((offset.x / size.width).coerceIn(0f, 1f) * controller.durationMs).toLong()
                    }
                }
                .pointerInput(controller.durationMs) {
                    detectHorizontalDragGestures { change, _ ->
                        controller.isPlaying = false
                        controller.currentTimeMs = ((change.position.x / size.width).coerceIn(0f, 1f) * controller.durationMs).toLong()
                    }
                }
        ) {
            Box(
                Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(controller.currentProgress)
                    .background(Brush.horizontalGradient(listOf(Color(0xFF6C7BFF), Color(0xFF22FFAA))))
            )
            Box(
                Modifier
                    .align(Alignment.CenterStart)
                    .offset(x = (controller.currentProgress * 1000).dp.coerceAtLeast(0.dp))
                    .width(2.dp).fillMaxHeight()
                    .background(Color.White)
            )
        }
    }
}

@Composable
fun AudioWaveformView(progress: Float, modifier: Modifier = Modifier) {
    val heights = remember { List(60) { (0.2f + Math.random().toFloat() * 0.8f) } }
    Canvas(modifier) {
        val barW = size.width / heights.size
        heights.forEachIndexed { i, h ->
            val played = (i.toFloat() / heights.size) < progress
            drawLine(
                color = if (played) Color(0xFF6C7BFF) else Color(0xFF444466),
                start = Offset(i * barW + barW / 2, size.height / 2 - h * size.height / 2),
                end = Offset(i * barW + barW / 2, size.height / 2 + h * size.height / 2),
                strokeWidth = barW * 0.6f,
                cap = StrokeCap.Round
            )
        }
    }
}

@Composable
fun EffectCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            content()
        }
    }
}

@Composable
fun EffectSlider(label: String, value: Float, range: ClosedFloatingPointRange<Float>, valueLabel: String = "%.2f".format(value), onValueChange: (Float) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            Text(valueLabel, style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace, color = MaterialTheme.colorScheme.primary)
        }
        Slider(value = value, onValueChange = onValueChange, valueRange = range)
    }
}

@Composable
private fun InfoChip(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun SummaryRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
    }
}

private fun effectSummary(effect: VideoEffect): String = when (effect) {
    is VideoEffect.Brightness -> "Brightness(${"%.2f".format(effect.factor)})"
    is VideoEffect.Contrast -> "Contrast(${"%.2f".format(effect.factor)})"
    is VideoEffect.Saturation -> "Saturation(${"%.2f".format(effect.factor)})"
    is VideoEffect.Blur -> "Blur(radius=${effect.radius}px)"
    is VideoEffect.Trim -> "Trim(${effect.startMs}ms..${effect.endMs}ms)"
    is VideoEffect.SpeedRamp -> "SpeedRamp(x${effect.factor})"
}
