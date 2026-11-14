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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PointMode
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

enum class FluidViewMode(val displayName: String, val description: String) {
    DYE_LUMINESCENCE("RGB Dye", "Multi-channel continuous dye mixing with volumetric glow"),
    STREAK_TRACERS("Streamline Tracers", "3,500 Lagrangian particles advected by the velocity field"),
    VORTICITY_HEATMAP("Vorticity (Curl)", "Highlights rotating vortices & turbulent shear layers"),
    PRESSURE_ISOBARS("Pressure Field", "Poisson pressure contours & stagnation zones"),
    VELOCITY_VECTORS("Velocity Glyphs", "Eulerian direction arrows indicating instantaneous velocity")
}

enum class FluidPalette(
    val displayName: String,
    val primaryColor: Color,
    val secondaryColor: Color,
    val accentColor: Color
) {
    NEON_CYBERPUNK("Neon Cyberpunk", Color(0xFF00F0FF), Color(0xFFFF007F), Color(0xFFFFE600)),
    BIOLUMINESCENT("Bioluminescent", Color(0xFF06B6D4), Color(0xFF3B82F6), Color(0xFF10B981)),
    SOLAR_FLARE("Solar Plasma", Color(0xFFF97316), Color(0xFFEF4444), Color(0xFFFDE047)),
    ACID_EMERALD("Toxic Emerald", Color(0xFF10B981), Color(0xFF84CC16), Color(0xFFA7F3D0)),
    MONOCHROME_SMOKE("Silver Smoke", Color(0xFFE2E8F0), Color(0xFF94A3B8), Color(0xFFF8FAFC))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EulerianFluidScreen(onBack: () -> Unit) {
    val solver = remember { EulerianFluidSolver(width = 72, height = 108) }

    var viewMode by remember { mutableStateOf(FluidViewMode.STREAK_TRACERS) }
    var palette by remember { mutableStateOf(FluidPalette.NEON_CYBERPUNK) }
    var isRunning by remember { mutableStateOf(true) }
    var continuousJet by remember { mutableStateOf(false) }
    var obstacleMode by remember { mutableStateOf(false) }

    // Physical Parameters
    var viscosity by remember { mutableFloatStateOf(0.0001f) }
    var vorticityConfinement by remember { mutableFloatStateOf(2.2f) }
    var buoyancy by remember { mutableFloatStateOf(0.35f) }
    var jacobiIterations by remember { mutableIntStateOf(18) }
    var dyeDissipation by remember { mutableFloatStateOf(0.994f) }

    // Telemetry State
    var maxVelocity by remember { mutableFloatStateOf(0f) }
    var reynoldsEstimate by remember { mutableIntStateOf(0) }
    var frameTick by remember { mutableIntStateOf(0) }
    var showSettingsDialog by remember { mutableStateOf(false) }

    // Hue rotation for injected dye
    var colorPhase by remember { mutableFloatStateOf(0f) }

    // Continuous 60fps Physics & Solver Loop
    LaunchedEffect(isRunning, continuousJet) {
        var lastTime = 0L
        while (true) {
            withFrameNanos { now ->
                if (lastTime != 0L) {
                    val dt = ((now - lastTime) / 1_000_000_000f).coerceIn(0.005f, 0.033f)

                    if (isRunning) {
                        colorPhase = (colorPhase + dt * 0.8f) % (2f * PI.toFloat())

                        // Continuous Jet Emitter (Wind Tunnel / Smoke Nozzle)
                        if (continuousJet) {
                            val r = 0.5f + 0.5f * sin(colorPhase)
                            val g = 0.5f + 0.5f * sin(colorPhase + 2.09f)
                            val b = 0.5f + 0.5f * sin(colorPhase + 4.18f)
                            solver.injectContinuousJet(
                                gx = solver.width / 2f,
                                gy = solver.height - 4f,
                                fvx = (RandomFloat() - 0.5f) * 0.2f,
                                fvy = -3.2f,
                                r = r, g = g, b = b
                            )
                        }

                        // Execute 5-Pass Eulerian Navier-Stokes Step
                        solver.step(
                            dt = dt,
                            viscosity = viscosity,
                            vorticityEps = vorticityConfinement,
                            buoyancy = buoyancy,
                            jacobiIters = jacobiIterations,
                            dyeDissipation = dyeDissipation
                        )

                        // Compute Telemetry Stats
                        var maxV = 0f
                        for (i in 0 until solver.size) {
                            val vSq = solver.vx[i] * solver.vx[i] + solver.vy[i] * solver.vy[i]
                            if (vSq > maxV) maxV = vSq
                        }
                        val peakSpeed = sqrt(maxV)
                        maxVelocity = peakSpeed
                        val charLength = solver.width.toFloat()
                        val viscSafe = max(viscosity, 1e-5f)
                        reynoldsEstimate = ((peakSpeed * charLength) / (viscSafe * 1000f)).toInt().coerceIn(0, 999999)

                        frameTick++
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
                            text = "Eulerian Navier-Stokes Fluid",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "5-Pass Ping-Pong Solver • ${viewMode.displayName}",
                            style = MaterialTheme.typography.labelSmall,
                            color = palette.primaryColor
                        )
                    }
                },
                navigationIcon = {
                    TextButton(onClick = onBack) {
                        Text("Back", color = palette.primaryColor, fontWeight = FontWeight.Bold)
                    }
                },
                actions = {
                    TextButton(onClick = { isRunning = !isRunning }) {
                        Text(
                            text = if (isRunning) "Pause" else "Play",
                            color = if (isRunning) palette.primaryColor else Color.Gray,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    TextButton(onClick = { solver.reset() }) {
                        Text("Clear", color = Color.White.copy(alpha = 0.8f))
                    }
                    TextButton(onClick = { showSettingsDialog = true }) {
                        Text("Settings", color = palette.secondaryColor, fontWeight = FontWeight.Bold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF030307)
                )
            )
        },
        containerColor = Color(0xFF030307),
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Main Fluid Canvas
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(obstacleMode) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            val gx = (change.position.x / size.width * solver.width).coerceIn(1f, solver.width - 2f)
                            val gy = (change.position.y / size.height * solver.height).coerceIn(1f, solver.height - 2f)

                            if (obstacleMode) {
                                solver.setCircularObstacle(gx, gy, radius = 5.5f, active = true)
                            } else {
                                val fvx = (dragAmount.x / size.width * solver.width) * 1.8f
                                val fvy = (dragAmount.y / size.height * solver.height) * 1.8f

                                val r = 0.5f + 0.5f * sin(colorPhase)
                                val g = 0.5f + 0.5f * sin(colorPhase + 2.09f)
                                val b = 0.5f + 0.5f * sin(colorPhase + 4.18f)

                                solver.addImpulse(
                                    gx = gx,
                                    gy = gy,
                                    fvx = fvx,
                                    fvy = fvy,
                                    r = r,
                                    g = g,
                                    b = b,
                                    radius = 4.2f
                                )
                            }
                        }
                    }
                    .pointerInput(obstacleMode) {
                        detectTapGestures { tapOffset ->
                            val gx = (tapOffset.x / size.width * solver.width).coerceIn(1f, solver.width - 2f)
                            val gy = (tapOffset.y / size.height * solver.height).coerceIn(1f, solver.height - 2f)

                            if (obstacleMode) {
                                solver.setCircularObstacle(gx, gy, radius = 6.0f, active = true)
                            } else {
                                // Fluid splash burst
                                for (angleIdx in 0..8) {
                                    val a = (angleIdx / 8f) * 2f * PI.toFloat()
                                    solver.addImpulse(
                                        gx = gx,
                                        gy = gy,
                                        fvx = cos(a) * 3.5f,
                                        fvy = sin(a) * 3.5f,
                                        r = 1f, g = 0.4f, b = 0.9f,
                                        radius = 5f
                                    )
                                }
                            }
                        }
                    }
            ) {
                // Trigger recomposition on solver frame tick
                frameTick

                drawFluidVisualization(
                    solver = solver,
                    viewMode = viewMode,
                    palette = palette
                )
            }

            // Top-Left Telemetry Card
            Card(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(14.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.Black.copy(alpha = 0.65f)
                ),
                border = androidx.compose.foundation.BorderStroke(1.dp, palette.primaryColor.copy(alpha = 0.4f))
            ) {
                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "NAVIER-STOKES EULERIAN",
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = palette.primaryColor
                        )
                        Spacer(Modifier.width(6.dp))
                        Surface(
                            shape = CircleShape,
                            color = palette.secondaryColor.copy(alpha = 0.2f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, palette.secondaryColor)
                        ) {
                            Text(
                                text = "60 FPS",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black,
                                color = palette.secondaryColor,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                            )
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Grid: ${solver.width}×${solver.height} (${solver.size} cells)",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                    Text(
                        text = "Tracers: ${solver.tracerCount}  •  Re ≈ $reynoldsEstimate",
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                    Text(
                        text = "Peak Velocity: ${((maxVelocity * 100).roundToInt() / 100f)} u/s",
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        color = palette.accentColor
                    )
                }
            }

            // Top-Right Quick Action Chips
            Column(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(14.dp),
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Continuous Jet Toggle Chip
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = if (continuousJet) palette.accentColor else Color.Black.copy(alpha = 0.6f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, palette.accentColor),
                    modifier = Modifier.clickable { continuousJet = !continuousJet }
                ) {
                    Text(
                        text = if (continuousJet) "✦ Jet Active" else "▷ Jet Off",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (continuousJet) Color.Black else Color.White,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }

                // Obstacle Placement Mode Chip
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = if (obstacleMode) palette.secondaryColor else Color.Black.copy(alpha = 0.6f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, palette.secondaryColor),
                    modifier = Modifier.clickable { obstacleMode = !obstacleMode }
                ) {
                    Text(
                        text = if (obstacleMode) "● Place Obstacle" else "✎ Drag Fluid",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (obstacleMode) Color.Black else Color.White,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }

            // Bottom View Mode Switcher
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FluidViewMode.values().forEach { mode ->
                        val isSelected = viewMode == mode
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewMode = mode },
                            label = { Text(mode.displayName, fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = palette.primaryColor,
                                selectedLabelColor = Color.Black,
                                containerColor = Color.Black.copy(alpha = 0.6f),
                                labelColor = Color.White
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = isSelected,
                                selectedBorderColor = palette.primaryColor,
                                borderColor = Color.White.copy(alpha = 0.25f)
                            )
                        )
                    }
                }
            }
        }
    }

    if (showSettingsDialog) {
        FluidSettingsDialog(
            viscosity = viscosity,
            onViscosityChange = { viscosity = it },
            vorticityConfinement = vorticityConfinement,
            onVorticityChange = { vorticityConfinement = it },
            buoyancy = buoyancy,
            onBuoyancyChange = { buoyancy = it },
            jacobiIterations = jacobiIterations,
            onJacobiChange = { jacobiIterations = it },
            palette = palette,
            onPaletteChange = { palette = it },
            onClearObstacles = { solver.clearObstacles() },
            onDismiss = { showSettingsDialog = false }
        )
    }
}

// -----------------------------------------------------------------------------
// FLUID CANVAS RENDERING
// -----------------------------------------------------------------------------

private fun DrawScope.drawFluidVisualization(
    solver: EulerianFluidSolver,
    viewMode: FluidViewMode,
    palette: FluidPalette
) {
    val scaleX = size.width / solver.width.toFloat()
    val scaleY = size.height / solver.height.toFloat()

    when (viewMode) {
        FluidViewMode.STREAK_TRACERS -> {
            // Draw faint ambient background
            drawRect(Color(0xFF04030A), size = size)

            // Render 3,500 glowing Lagrangian Streak Particles
            for (t in solver.tracers) {
                val startPos = Offset(t.prevX * scaleX, t.prevY * scaleY)
                val endPos = Offset(t.x * scaleX, t.y * scaleY)

                val alphaNorm = (1f - t.life / t.maxLife).coerceIn(0.1f, 1f)
                val tracerColor = Color(
                    red = (t.r * palette.primaryColor.red + (1f - t.r) * palette.secondaryColor.red).coerceIn(0f, 1f),
                    green = (t.g * palette.primaryColor.green + (1f - t.g) * palette.secondaryColor.green).coerceIn(0f, 1f),
                    blue = (t.b * palette.primaryColor.blue + (1f - t.b) * palette.secondaryColor.blue).coerceIn(0f, 1f),
                    alpha = alphaNorm
                )

                // Particle streak line
                drawLine(
                    color = tracerColor,
                    start = startPos,
                    end = endPos,
                    strokeWidth = 2.2f,
                    cap = StrokeCap.Round
                )
            }
        }

        FluidViewMode.DYE_LUMINESCENCE -> {
            drawRect(Color(0xFF020205), size = size)

            // Multi-channel RGB Dye Field rendering with cell blending
            val step = 2 // Render 2x2 blocks for high performance
            for (y in 1 until solver.height - 1 step step) {
                for (x in 1 until solver.width - 1 step step) {
                    val i = solver.idx(x, y)
                    if (solver.solid[i]) continue

                    val r = solver.dyeR[i].coerceIn(0f, 1f)
                    val g = solver.dyeG[i].coerceIn(0f, 1f)
                    val b = solver.dyeB[i].coerceIn(0f, 1f)
                    val density = max(r, max(g, b))

                    if (density > 0.02f) {
                        val cellColor = Color(
                            red = (r * palette.primaryColor.red + (1f - r) * palette.secondaryColor.red * 0.5f).coerceIn(0f, 1f),
                            green = (g * palette.primaryColor.green + (1f - g) * palette.secondaryColor.green * 0.5f).coerceIn(0f, 1f),
                            blue = (b * palette.primaryColor.blue + (1f - b) * palette.secondaryColor.blue * 0.5f).coerceIn(0f, 1f),
                            alpha = density.coerceIn(0.1f, 0.95f)
                        )
                        drawRect(
                            color = cellColor,
                            topLeft = Offset(x * scaleX, y * scaleY),
                            size = Size(scaleX * step * 1.15f, scaleY * step * 1.15f)
                        )
                    }
                }
            }
        }

        FluidViewMode.VORTICITY_HEATMAP -> {
            drawRect(Color(0xFF03020A), size = size)

            // Vorticity / Curl visualization
            val step = 2
            for (y in 1 until solver.height - 1 step step) {
                for (x in 1 until solver.width - 1 step step) {
                    val i = solver.idx(x, y)
                    if (solver.solid[i]) continue

                    val omega = solver.vort[i]
                    val mag = abs(omega) * 4.5f
                    if (mag > 0.05f) {
                        // Clockwise (positive) in cyan, counter-clockwise (negative) in magenta
                        val vortColor = if (omega > 0f) {
                            Color(0xFF00F0FF).copy(alpha = mag.coerceIn(0.1f, 0.9f))
                        } else {
                            Color(0xFFFF007F).copy(alpha = mag.coerceIn(0.1f, 0.9f))
                        }
                        drawRect(
                            color = vortColor,
                            topLeft = Offset(x * scaleX, y * scaleY),
                            size = Size(scaleX * step * 1.1f, scaleY * step * 1.1f)
                        )
                    }
                }
            }
        }

        FluidViewMode.PRESSURE_ISOBARS -> {
            drawRect(Color(0xFF020409), size = size)

            // Pressure Poisson solution contours
            val step = 2
            for (y in 1 until solver.height - 1 step step) {
                for (x in 1 until solver.width - 1 step step) {
                    val i = solver.idx(x, y)
                    if (solver.solid[i]) continue

                    val pVal = solver.p[i]
                    val pMag = abs(pVal) * 8.0f
                    if (pMag > 0.02f) {
                        // High stagnation pressure in orange, low suction pressure in blue
                        val pColor = if (pVal > 0f) {
                            Color(0xFFF97316).copy(alpha = pMag.coerceIn(0.1f, 0.9f))
                        } else {
                            Color(0xFF3B82F6).copy(alpha = pMag.coerceIn(0.1f, 0.9f))
                        }
                        drawRect(
                            color = pColor,
                            topLeft = Offset(x * scaleX, y * scaleY),
                            size = Size(scaleX * step * 1.1f, scaleY * step * 1.1f)
                        )
                    }
                }
            }
        }

        FluidViewMode.VELOCITY_VECTORS -> {
            drawRect(Color(0xFF030307), size = size)

            // Vector arrow glyphs sampled across grid
            val glyphStep = 4
            for (y in 2 until solver.height - 2 step glyphStep) {
                for (x in 2 until solver.width - 2 step glyphStep) {
                    val i = solver.idx(x, y)
                    if (solver.solid[i]) continue

                    val vx = solver.vx[i]
                    val vy = solver.vy[i]
                    val speed = sqrt(vx * vx + vy * vy)
                    if (speed > 0.05f) {
                        val start = Offset(x * scaleX, y * scaleY)
                        val end = Offset((x + vx * 2.2f) * scaleX, (y + vy * 2.2f) * scaleY)

                        val speedColor = if (speed > 2.0f) palette.accentColor else palette.primaryColor
                        drawLine(
                            color = speedColor.copy(alpha = 0.75f),
                            start = start,
                            end = end,
                            strokeWidth = 1.8f,
                            cap = StrokeCap.Round
                        )
                        drawCircle(
                            color = speedColor,
                            radius = 2.0f,
                            center = end
                        )
                    }
                }
            }
        }
    }

    // Render Solid Obstacles (von Kármán barriers)
    for (y in 1 until solver.height - 1) {
        for (x in 1 until solver.width - 1) {
            val i = solver.idx(x, y)
            if (solver.solid[i]) {
                drawRect(
                    color = Color(0xFF64748B),
                    topLeft = Offset(x * scaleX, y * scaleY),
                    size = Size(scaleX * 1.05f, scaleY * 1.05f)
                )
            }
        }
    }
}

private fun RandomFloat(): Float = kotlin.random.Random.nextFloat()

// -----------------------------------------------------------------------------
// SETTINGS DIALOG
// -----------------------------------------------------------------------------

@Composable
private fun FluidSettingsDialog(
    viscosity: Float,
    onViscosityChange: (Float) -> Unit,
    vorticityConfinement: Float,
    onVorticityChange: (Float) -> Unit,
    buoyancy: Float,
    onBuoyancyChange: (Float) -> Unit,
    jacobiIterations: Int,
    onJacobiChange: (Int) -> Unit,
    palette: FluidPalette,
    onPaletteChange: (FluidPalette) -> Unit,
    onClearObstacles: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Navier-Stokes Physics Parameters", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Vorticity Confinement
                Text(
                    text = "Vorticity Confinement ε = ${((vorticityConfinement * 10).roundToInt() / 10f)}",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp
                )
                Text(
                    text = "Restores curling micro-vortices and turbulent eddies lost to numerical dissipation.",
                    fontSize = 10.sp,
                    color = Color.Gray
                )
                Slider(
                    value = vorticityConfinement,
                    onValueChange = onVorticityChange,
                    valueRange = 0.0f..5.0f
                )

                // Viscosity
                Text(
                    text = "Kinematic Viscosity ν = ${((viscosity * 10000).roundToInt() / 10000f)}",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp
                )
                Slider(
                    value = viscosity,
                    onValueChange = onViscosityChange,
                    valueRange = 0.00001f..0.003f
                )

                // Thermal Buoyancy
                Text(
                    text = "Thermal Buoyancy = ${((buoyancy * 100).roundToInt() / 100f)}",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp
                )
                Slider(
                    value = buoyancy,
                    onValueChange = onBuoyancyChange,
                    valueRange = -1.0f..1.5f
                )

                // Jacobi Relaxation Iterations
                Text(
                    text = "Pressure Poisson Jacobi Iterations = $jacobiIterations",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp
                )
                Slider(
                    value = jacobiIterations.toFloat(),
                    onValueChange = { onJacobiChange(it.toInt()) },
                    valueRange = 6f..30f,
                    steps = 23
                )

                // Color Themes
                Text("Color Palette", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FluidPalette.values().forEach { pal ->
                        FilterChip(
                            selected = palette == pal,
                            onClick = { onPaletteChange(pal) },
                            label = { Text(pal.displayName, fontSize = 11.sp) }
                        )
                    }
                }

                // Clear Obstacles Button
                Button(
                    onClick = onClearObstacles,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Clear All Solid Obstacles")
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
