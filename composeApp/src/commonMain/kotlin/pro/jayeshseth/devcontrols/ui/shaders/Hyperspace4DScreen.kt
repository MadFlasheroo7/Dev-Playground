package pro.jayeshseth.devcontrols.ui.shaders

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
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
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
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
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

enum class PolytopeType(val displayName: String, val badge: String) {
    TESSERACT("Tesseract", "8-Cell"),
    CELL_16("16-Cell", "Dual"),
    CELL_24("24-Cell", "4D Exclusive"),
    CELL_5("5-Cell", "Simplex"),
    CLIFFORD_TORUS("Clifford Torus", "S³ Hopf"),
    HYPERSPHERE("Hypersphere", "Geodesic")
}

enum class DragInteraction {
    ROTATION_3D,       // XZ and YZ planes (standard 3D viewing angle)
    HYPER_FOLD_4D      // XW and YW planes (folds 4th dimension inside-out)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Hyperspace4DScreen(onBack: () -> Unit) {
    var selectedPolytopeType by remember { mutableStateOf(PolytopeType.TESSERACT) }
    var projectionMode by remember { mutableStateOf(ProjectionMode.PERSPECTIVE) }
    var palette by remember { mutableStateOf(HyperspacePalette.COSMIC_WARP) }
    var dragInteraction by remember { mutableStateOf(DragInteraction.HYPER_FOLD_4D) }

    // 6D Rotation Angles (in radians)
    var angleXY by remember { mutableFloatStateOf(0f) }
    var angleYZ by remember { mutableFloatStateOf(0.4f) }
    var angleZX by remember { mutableFloatStateOf(0.6f) }
    var angleXW by remember { mutableFloatStateOf(0f) }
    var angleYW by remember { mutableFloatStateOf(0f) }
    var angleZW by remember { mutableFloatStateOf(0f) }

    // Simulation & Visual Tweaks
    var isAutoRotating by remember { mutableStateOf(true) }
    var speed3D by remember { mutableFloatStateOf(0.8f) }
    var speed4D by remember { mutableFloatStateOf(1.0f) }
    var d4Perspective by remember { mutableFloatStateOf(2.35f) }
    var sliceW0 by remember { mutableFloatStateOf(0.0f) }
    var lineWidth by remember { mutableFloatStateOf(2.2f) }
    var showVertexOrbs by remember { mutableStateOf(true) }
    var showBasisGizmo by remember { mutableStateOf(true) }
    var showSettingsDialog by remember { mutableStateOf(false) }

    // Touch Ripple Shockwave State
    var rippleProgress by remember { mutableFloatStateOf(1.0f) } // 0..1, 1 = inactive

    // Load active polytope mesh
    val polytope = remember(selectedPolytopeType) {
        when (selectedPolytopeType) {
            PolytopeType.TESSERACT -> Hyperspace4DMath.createTesseract()
            PolytopeType.CELL_16 -> Hyperspace4DMath.create16Cell()
            PolytopeType.CELL_24 -> Hyperspace4DMath.create24Cell()
            PolytopeType.CELL_5 -> Hyperspace4DMath.create5Cell()
            PolytopeType.CLIFFORD_TORUS -> Hyperspace4DMath.createCliffordTorus()
            PolytopeType.HYPERSPHERE -> Hyperspace4DMath.createHypersphereLattice()
        }
    }

    // Continuous 60fps Animation Loop
    LaunchedEffect(isAutoRotating) {
        var lastTime = 0L
        while (true) {
            withFrameNanos { now ->
                if (lastTime != 0L) {
                    val dt = ((now - lastTime) / 1_000_000_000f).coerceIn(0.001f, 0.05f)

                    if (isAutoRotating) {
                        // 3D rotations
                        angleZX += dt * 0.45f * speed3D
                        angleYZ += dt * 0.25f * speed3D

                        // 4D Hyperspace rotations (tumble & fold)
                        angleXW += dt * 0.55f * speed4D
                        angleZW += dt * 0.35f * speed4D
                    }

                    // Progress touch wave ripple
                    if (rippleProgress < 1.0f) {
                        rippleProgress = (rippleProgress + dt * 1.5f).coerceAtMost(1.0f)
                    }
                }
                lastTime = now
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "4D Hyperspace Lab",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "${polytope.name} • ${projectionMode.displayName}",
                            style = MaterialTheme.typography.labelSmall,
                            color = palette.wNearColor
                        )
                    }
                },
                navigationIcon = {
                    TextButton(onClick = onBack) {
                        Text("Back", color = palette.wNearColor, fontWeight = FontWeight.Bold)
                    }
                },
                actions = {
                    // Auto-rotation toggle
                    TextButton(onClick = { isAutoRotating = !isAutoRotating }) {
                        Text(
                            text = if (isAutoRotating) "Pause" else "Play",
                            color = if (isAutoRotating) palette.wNearColor else Color.Gray,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    // Reset orientation
                    TextButton(onClick = {
                        angleXY = 0f
                        angleYZ = 0.4f
                        angleZX = 0.6f
                        angleXW = 0f
                        angleYW = 0f
                        angleZW = 0f
                    }) {
                        Text("Reset", color = Color.White.copy(alpha = 0.8f))
                    }
                    // Settings Dialog button
                    TextButton(onClick = { showSettingsDialog = true }) {
                        Text("Settings", color = palette.wMidColor, fontWeight = FontWeight.Bold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = palette.backgroundColor
                )
            )
        },
        containerColor = palette.backgroundColor,
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Main 4D Viewport Canvas
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(dragInteraction) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            val sensitivity = 0.007f
                            when (dragInteraction) {
                                DragInteraction.ROTATION_3D -> {
                                    angleZX += dragAmount.x * sensitivity
                                    angleYZ += dragAmount.y * sensitivity
                                }
                                DragInteraction.HYPER_FOLD_4D -> {
                                    angleXW += dragAmount.x * sensitivity
                                    angleYW += dragAmount.y * sensitivity
                                }
                            }
                        }
                    }
                    .pointerInput(Unit) {
                        detectTapGestures {
                            // Trigger 4D wave excitation ripple
                            rippleProgress = 0f
                        }
                    }
            ) {
                drawHyperspaceScene(
                    polytope = polytope,
                    projectionMode = projectionMode,
                    palette = palette,
                    angleXY = angleXY,
                    angleYZ = angleYZ,
                    angleZX = angleZX,
                    angleXW = angleXW,
                    angleYW = angleYW,
                    angleZW = angleZW,
                    d4 = d4Perspective,
                    sliceW0 = sliceW0,
                    lineWidth = lineWidth,
                    showVertexOrbs = showVertexOrbs,
                    showBasisGizmo = showBasisGizmo,
                    rippleProgress = rippleProgress
                )
            }

            // Top-Left 4D Polytope Telemetry Card
            Card(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(14.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.Black.copy(alpha = 0.65f)
                ),
                border = Stroke(width = 1f).let {
                    androidx.compose.foundation.BorderStroke(1.dp, palette.wMidColor.copy(alpha = 0.4f))
                }
            ) {
                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = polytope.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = Color.White
                        )
                        Spacer(Modifier.width(8.dp))
                        Surface(
                            shape = CircleShape,
                            color = palette.wNearColor.copy(alpha = 0.2f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, palette.wNearColor)
                        ) {
                            Text(
                                text = "4D",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                                color = palette.wNearColor,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                            )
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "V: ${polytope.vertexCountLabel} • E: ${polytope.edgeCountLabel} • Cells: ${polytope.cellCountLabel}",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = Color.White.copy(alpha = 0.85f)
                    )
                    Text(
                        text = "Euler Char: χ = V - E + F - C = 0",
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        color = palette.wFarColor.copy(alpha = 0.9f)
                    )
                }
            }

            // Top-Right $SO(4)$ Rotation Angles Telemetry Card
            Card(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(14.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.Black.copy(alpha = 0.65f)
                ),
                border = androidx.compose.foundation.BorderStroke(1.dp, palette.wNearColor.copy(alpha = 0.35f))
            ) {
                Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
                    Text(
                        text = "SO(4) HYPER-TELEMETRY",
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        color = palette.wNearColor
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(
                        text = "3D Yaw (ZX): ${((angleZX * 180f / PI) % 360f).roundToInt()}°  Pitch (YZ): ${((angleYZ * 180f / PI) % 360f).roundToInt()}°",
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                    Text(
                        text = "4D Fold (XW): ${((angleXW * 180f / PI) % 360f).roundToInt()}°  Fold (ZW): ${((angleZW * 180f / PI) % 360f).roundToInt()}°",
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        color = palette.wMidColor
                    )
                }
            }

            // Bottom Floating Controls Toolbar
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Interactive Mode Switcher (3D Rotate vs 4D Hyperspace Fold)
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color.Black.copy(alpha = 0.75f))
                        .border(1.dp, palette.wNearColor.copy(alpha = 0.45f), RoundedCornerShape(24.dp))
                        .padding(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = if (dragInteraction == DragInteraction.HYPER_FOLD_4D) palette.wNearColor else Color.Transparent,
                        modifier = Modifier.clickable { dragInteraction = DragInteraction.HYPER_FOLD_4D }
                    ) {
                        Text(
                            text = "✦ 4D Hyperspace Fold",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (dragInteraction == DragInteraction.HYPER_FOLD_4D) Color.Black else Color.White,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                        )
                    }

                    Spacer(Modifier.width(4.dp))

                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = if (dragInteraction == DragInteraction.ROTATION_3D) palette.wMidColor else Color.Transparent,
                        modifier = Modifier.clickable { dragInteraction = DragInteraction.ROTATION_3D }
                    ) {
                        Text(
                            text = "↻ 3D View Angle",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (dragInteraction == DragInteraction.ROTATION_3D) Color.Black else Color.White,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                        )
                    }
                }

                Spacer(Modifier.height(10.dp))

                // Polytope Selector Chips Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PolytopeType.values().forEach { type ->
                        val isSelected = selectedPolytopeType == type
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedPolytopeType = type },
                            label = { Text(type.displayName, fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = palette.wNearColor,
                                selectedLabelColor = Color.Black,
                                containerColor = Color.Black.copy(alpha = 0.6f),
                                labelColor = Color.White
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = isSelected,
                                selectedBorderColor = palette.wNearColor,
                                borderColor = Color.White.copy(alpha = 0.25f)
                            )
                        )
                    }
                }
            }
        }
    }

    // Settings & Parameter Adjustment Dialog
    if (showSettingsDialog) {
        HyperspaceSettingsDialog(
            palette = palette,
            onPaletteChange = { palette = it },
            projectionMode = projectionMode,
            onProjectionModeChange = { projectionMode = it },
            d4 = d4Perspective,
            onD4Change = { d4Perspective = it },
            sliceW0 = sliceW0,
            onSliceW0Change = { sliceW0 = it },
            speed3D = speed3D,
            onSpeed3DChange = { speed3D = it },
            speed4D = speed4D,
            onSpeed4DChange = { speed4D = it },
            lineWidth = lineWidth,
            onLineWidthChange = { lineWidth = it },
            showVertexOrbs = showVertexOrbs,
            onShowVertexOrbsChange = { showVertexOrbs = it },
            showBasisGizmo = showBasisGizmo,
            onShowBasisGizmoChange = { showBasisGizmo = it },
            onDismiss = { showSettingsDialog = false }
        )
    }
}

// -----------------------------------------------------------------------------
// HYPERSPACE SCENE RENDERING PIPELINE
// -----------------------------------------------------------------------------

private fun DrawScope.drawHyperspaceScene(
    polytope: Polytope4D,
    projectionMode: ProjectionMode,
    palette: HyperspacePalette,
    angleXY: Float,
    angleYZ: Float,
    angleZX: Float,
    angleXW: Float,
    angleYW: Float,
    angleZW: Float,
    d4: Float,
    sliceW0: Float,
    lineWidth: Float,
    showVertexOrbs: Boolean,
    showBasisGizmo: Boolean,
    rippleProgress: Float
) {
    val cx = size.width / 2f
    val cy = size.height / 2f

    // Background gradient glow
    drawRect(
        brush = Brush.radialGradient(
            colors = listOf(
                palette.wMidColor.copy(alpha = 0.12f),
                Color.Transparent
            ),
            center = Offset(cx, cy),
            radius = min(cx, cy) * 1.4f
        )
    )

    // 1. Transform each 4D vertex via SO(4) rotation
    val rotated4D = polytope.vertices.map { v ->
        var rv = Hyperspace4DMath.rotateSO4(v, angleXY, angleYZ, angleZX, angleXW, angleYW, angleZW)

        // Apply touch shockwave if active
        if (rippleProgress < 1.0f) {
            val dist = rv.length()
            val wavePhase = (dist * 5f - rippleProgress * 15f)
            val damp = exp(-rippleProgress * 3.5f)
            val waveW = rv.w + 0.35f * sin(wavePhase) * damp
            rv = Vec4(rv.x, rv.y, rv.z, waveW)
        }
        rv
    }

    // 2. Handle Cross-Section (Hyperplane Slice at w = sliceW0) Mode
    if (projectionMode == ProjectionMode.CROSS_SECTION) {
        // Draw ghosted wireframe of the full 4D object behind
        for (edge in polytope.edges) {
            val v1_3D = Hyperspace4DMath.project4DTo3D(rotated4D[edge.startIndex], ProjectionMode.PERSPECTIVE, d4)
            val v2_3D = Hyperspace4DMath.project4DTo3D(rotated4D[edge.endIndex], ProjectionMode.PERSPECTIVE, d4)
            val p1 = Hyperspace4DMath.project3DToScreen(v1_3D, size)
            val p2 = Hyperspace4DMath.project3DToScreen(v2_3D, size)

            drawLine(
                color = palette.wFarColor.copy(alpha = 0.12f),
                start = p1,
                end = p2,
                strokeWidth = 1f
            )
        }

        // Calculate and render hyperplane 3D intersection points
        val intersections = ArrayList<Vec3>()
        for (edge in polytope.edges) {
            val v1 = rotated4D[edge.startIndex]
            val v2 = rotated4D[edge.endIndex]
            val slicePt = Hyperspace4DMath.sliceEdgeAtW(v1, v2, sliceW0)
            if (slicePt != null) {
                intersections.add(slicePt)
            }
        }

        // Render intersection points as glowing hyper-slices
        for (pt in intersections) {
            val screenPos = Hyperspace4DMath.project3DToScreen(pt, size)
            // Outer pulse
            drawCircle(
                color = palette.wNearColor.copy(alpha = 0.35f),
                radius = 7f,
                center = screenPos
            )
            // Inner core
            drawCircle(
                color = palette.vertexCoreColor,
                radius = 3.5f,
                center = screenPos
            )
        }

        // Draw slice connections (inter-slice wireframe)
        for (i in 0 until intersections.size) {
            for (j in (i + 1) until intersections.size) {
                val p1 = intersections[i]
                val p2 = intersections[j]
                val dist = (p1 - p2).length()
                if (dist < 1.0f) {
                    val sp1 = Hyperspace4DMath.project3DToScreen(p1, size)
                    val sp2 = Hyperspace4DMath.project3DToScreen(p2, size)
                    drawLine(
                        color = palette.wNearColor,
                        start = sp1,
                        end = sp2,
                        strokeWidth = lineWidth * 1.3f
                    )
                }
            }
        }
        return
    }

    // 3. Project 4D to 3D and then to Screen 2D
    val projected3D = rotated4D.map { v ->
        Hyperspace4DMath.project4DTo3D(v, projectionMode, d4)
    }

    val screen2D = projected3D.map { v3 ->
        Hyperspace4DMath.project3DToScreen(v3, size)
    }

    // Sort edges by average 3D depth for painter's depth cueing
    val edgeOrders = polytope.edges.mapIndexed { idx, edge ->
        val z1 = projected3D[edge.startIndex].z
        val z2 = projected3D[edge.endIndex].z
        val w1 = rotated4D[edge.startIndex].w
        val w2 = rotated4D[edge.endIndex].w
        Triple(idx, (z1 + z2) / 2f, (w1 + w2) / 2f)
    }.sortedBy { it.second } // Far z drawn first

    // 4. Render Edges (Double-pass: Broad soft neon bloom + Crisp bright core)
    for ((edgeIndex, avgZ, avgW) in edgeOrders) {
        val edge = polytope.edges[edgeIndex]
        val p1 = screen2D[edge.startIndex]
        val p2 = screen2D[edge.endIndex]

        val w1 = rotated4D[edge.startIndex].w
        val w2 = rotated4D[edge.endIndex].w

        val edgeColor = Hyperspace4DMath.colorForW(avgW, palette)

        // Depth cueing alpha based on 3D z and 4D w
        val depthAlpha = (1f - (avgZ * 0.15f)).coerceIn(0.25f, 1f)

        // Pass 1: Volumetric Glow
        drawLine(
            color = edgeColor.copy(alpha = 0.28f * depthAlpha),
            start = p1,
            end = p2,
            strokeWidth = lineWidth * 3.4f,
            cap = StrokeCap.Round
        )

        // Pass 2: Core Beam
        drawLine(
            color = edgeColor.copy(alpha = 0.95f * depthAlpha),
            start = p1,
            end = p2,
            strokeWidth = lineWidth,
            cap = StrokeCap.Round
        )
    }

    // 5. Render Vertex Energy Nodes (if enabled)
    if (showVertexOrbs) {
        for (i in rotated4D.indices) {
            val v4 = rotated4D[i]
            val v3 = projected3D[i]
            val screenPos = screen2D[i]

            val nodeColor = Hyperspace4DMath.colorForW(v4.w, palette)
            val perspectiveScale = (1.5f / (3.5f - v3.z)).coerceIn(0.4f, 2.2f)
            val baseRadius = 4.5f * perspectiveScale

            // Outer energy aura
            drawCircle(
                color = nodeColor.copy(alpha = 0.35f),
                radius = baseRadius * 2.2f,
                center = screenPos
            )
            // Intermediate flare
            drawCircle(
                color = nodeColor.copy(alpha = 0.85f),
                radius = baseRadius,
                center = screenPos
            )
            // Core singularity
            drawCircle(
                color = palette.vertexCoreColor,
                radius = baseRadius * 0.45f,
                center = screenPos
            )
        }
    }

    // 6. Draw 4D Basis Vectors Gizmo in Bottom-Right Corner
    if (showBasisGizmo) {
        drawBasisGizmo4D(
            size = size,
            angleXY = angleXY,
            angleYZ = angleYZ,
            angleZX = angleZX,
            angleXW = angleXW,
            angleYW = angleYW,
            angleZW = angleZW,
            palette = palette
        )
    }
}

/**
 * Renders a 4-dimensional basis axis gizmo (Ex, Ey, Ez, Ew) in the corner.
 * Illustrates how the 4th dimension (W) is pointing relative to 3D space.
 */
private fun DrawScope.drawBasisGizmo4D(
    size: Size,
    angleXY: Float,
    angleYZ: Float,
    angleZX: Float,
    angleXW: Float,
    angleYW: Float,
    angleZW: Float,
    palette: HyperspacePalette
) {
    val gizmoCenter = Offset(size.width - 60f, size.height - 110f)
    val axisLength = 34f

    // 4 Unit Basis Vectors in R^4
    val axes = listOf(
        Pair(Vec4(1f, 0f, 0f, 0f), Color(0xFFEF4444)), // X: Red
        Pair(Vec4(0f, 1f, 0f, 0f), Color(0xFF10B981)), // Y: Green
        Pair(Vec4(0f, 0f, 1f, 0f), Color(0xFF3B82F6)), // Z: Blue
        Pair(Vec4(0f, 0f, 0f, 1f), palette.wNearColor) // W: 4D Hyperspace Violet/Cyan
    )

    // Backdrop disc
    drawCircle(
        color = Color.Black.copy(alpha = 0.6f),
        radius = 42f,
        center = gizmoCenter
    )

    for ((axisVec, axisColor) in axes) {
        val rVec = Hyperspace4DMath.rotateSO4(axisVec, angleXY, angleYZ, angleZX, angleXW, angleYW, angleZW)
        // Project to 2D
        val endPos = Offset(
            gizmoCenter.x + rVec.x * axisLength,
            gizmoCenter.y - rVec.y * axisLength
        )

        drawLine(
            color = axisColor,
            start = gizmoCenter,
            end = endPos,
            strokeWidth = 2.4f,
            cap = StrokeCap.Round
        )

        drawCircle(
            color = axisColor,
            radius = 3.5f,
            center = endPos
        )
    }
}

// -----------------------------------------------------------------------------
// SETTINGS DIALOG
// -----------------------------------------------------------------------------

@Composable
private fun HyperspaceSettingsDialog(
    palette: HyperspacePalette,
    onPaletteChange: (HyperspacePalette) -> Unit,
    projectionMode: ProjectionMode,
    onProjectionModeChange: (ProjectionMode) -> Unit,
    d4: Float,
    onD4Change: (Float) -> Unit,
    sliceW0: Float,
    onSliceW0Change: (Float) -> Unit,
    speed3D: Float,
    onSpeed3DChange: (Float) -> Unit,
    speed4D: Float,
    onSpeed4DChange: (Float) -> Unit,
    lineWidth: Float,
    onLineWidthChange: (Float) -> Unit,
    showVertexOrbs: Boolean,
    onShowVertexOrbsChange: (Boolean) -> Unit,
    showBasisGizmo: Boolean,
    onShowBasisGizmoChange: (Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Hyperspace & 4D Controls", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Projection Mode Picker
                Text("Projection Mode", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    ProjectionMode.values().forEach { mode ->
                        FilterChip(
                            selected = projectionMode == mode,
                            onClick = { onProjectionModeChange(mode) },
                            label = {
                                Column {
                                    Text(mode.displayName, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    Text(mode.description, fontSize = 10.sp, color = Color.Gray)
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                // If Cross-Section mode, show Hyperplane Slice slider
                if (projectionMode == ProjectionMode.CROSS_SECTION) {
                    Text("4D Slicing Plane (w₀ = ${((sliceW0 * 100).roundToInt() / 100f)})", fontSize = 12.sp)
                    Slider(
                        value = sliceW0,
                        onValueChange = onSliceW0Change,
                        valueRange = -1.5f..1.5f
                    )
                }

                // 4D Perspective Distance d4
                if (projectionMode == ProjectionMode.PERSPECTIVE) {
                    Text("4D Perspective Camera (d₄ = ${((d4 * 100).roundToInt() / 100f)})", fontSize = 12.sp)
                    Slider(
                        value = d4,
                        onValueChange = onD4Change,
                        valueRange = 1.4f..4.0f
                    )
                }

                // Hyperspace 4D Rotation Speed
                Text("4D Hyperspace Spin Speed (${((speed4D * 100).roundToInt() / 100f)}x)", fontSize = 12.sp)
                Slider(
                    value = speed4D,
                    onValueChange = onSpeed4DChange,
                    valueRange = 0.0f..3.0f
                )

                // 3D View Rotation Speed
                Text("3D Rotation Speed (${((speed3D * 100).roundToInt() / 100f)}x)", fontSize = 12.sp)
                Slider(
                    value = speed3D,
                    onValueChange = onSpeed3DChange,
                    valueRange = 0.0f..3.0f
                )

                // Edge Line Width
                Text("Wireframe Thickness (${((lineWidth * 10).roundToInt() / 10f)}dp)", fontSize = 12.sp)
                Slider(
                    value = lineWidth,
                    onValueChange = onLineWidthChange,
                    valueRange = 1.0f..6.0f
                )

                // Color Themes
                Text("4D Depth Palette", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    HyperspacePalette.values().forEach { pal ->
                        FilterChip(
                            selected = palette == pal,
                            onClick = { onPaletteChange(pal) },
                            label = { Text(pal.displayName, fontSize = 11.sp) }
                        )
                    }
                }

                // Toggles
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Glowing Vertex Nodes", fontSize = 13.sp)
                    Switch(checked = showVertexOrbs, onCheckedChange = onShowVertexOrbsChange)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("4D Basis Vectors Gizmo", fontSize = 13.sp)
                    Switch(checked = showBasisGizmo, onCheckedChange = onShowBasisGizmoChange)
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}
