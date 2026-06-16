package pro.jayeshseth.devcontrols.ui

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class StateReadMode {
    STANDARD, // Parameter: Composition -> Layout -> Draw
    LAYOUT_LAMBDA, // Lambda offset: Layout -> Draw
    DRAW_LAMBDA // Graphics layer lambda: Draw only
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComposePhasesScreen(onBack: () -> Unit) {
    var readMode by remember { mutableStateOf(StateReadMode.STANDARD) }
    var compositionCount by remember { mutableStateOf(0) }
    var layoutCount by remember { mutableStateOf(0) }
    var drawCount by remember { mutableStateOf(0) }
    
    // Position/animation state
    var offsetValue by remember { mutableStateOf(0f) }
    var isRunning by remember { mutableStateOf(false) }
    
    // Pulse animation helpers for visual feedback
    var pulseComposition by remember { mutableStateOf(false) }
    var pulseLayout by remember { mutableStateOf(false) }
    var pulseDraw by remember { mutableStateOf(false) }
    
    val scope = rememberCoroutineScope()
    
    // Side effect to track composition hits
    SideEffect {
        if (readMode == StateReadMode.STANDARD) {
            compositionCount++
            pulseComposition = true
        }
    }
    
    // Simulate layout & draw triggers when position moves
    LaunchedEffect(offsetValue) {
        if (offsetValue > 0f) {
            when (readMode) {
                StateReadMode.STANDARD -> {
                    layoutCount++
                    drawCount++
                    pulseLayout = true
                    pulseDraw = true
                }
                StateReadMode.LAYOUT_LAMBDA -> {
                    layoutCount++
                    drawCount++
                    pulseLayout = true
                    pulseDraw = true
                }
                StateReadMode.DRAW_LAMBDA -> {
                    drawCount++
                    pulseDraw = true
                }
            }
        }
    }
    
    // Clear pulses
    LaunchedEffect(pulseComposition) {
        if (pulseComposition) {
            delay(150)
            pulseComposition = false
        }
    }
    LaunchedEffect(pulseLayout) {
        if (pulseLayout) {
            delay(150)
            pulseLayout = false
        }
    }
    LaunchedEffect(pulseDraw) {
        if (pulseDraw) {
            delay(150)
            pulseDraw = false
        }
    }
    
    // Reset function
    fun resetStats() {
        compositionCount = 0
        layoutCount = 0
        drawCount = 0
        offsetValue = 0f
        isRunning = false
    }

    // Animation Loop
    LaunchedEffect(isRunning) {
        if (isRunning) {
            while (isRunning) {
                offsetValue = (offsetValue + 10f) % 200f
                delay(100)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Compose Phase Pipeline Lab", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    TextButton(onClick = onBack) {
                        Text("< Back", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                },
                actions = {
                    TextButton(onClick = { resetStats() }) {
                        Text("Reset counts", color = Color(0xFFEF4444))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF1E1E24),
                    titleContentColor = Color.White
                )
            )
        }
    ) { paddingValues ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color(0xFF121214))
        ) {
            val isWide = maxWidth >= 850.dp
            
            if (isWide) {
                Row(modifier = Modifier.fillMaxSize()) {
                    // Control and Code Panel
                    Column(
                        modifier = Modifier
                            .weight(0.45f)
                            .fillMaxHeight()
                            .padding(12.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        ControlPanel(
                            readMode = readMode,
                            onReadModeChange = {
                                readMode = it
                                resetStats()
                            },
                            isRunning = isRunning,
                            onToggleRunning = { isRunning = !isRunning },
                            onManualTrigger = {
                                offsetValue = (offsetValue + 15f) % 200f
                            }
                        )
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        CodePanel(readMode = readMode)
                    }
                    
                    Box(modifier = Modifier.fillMaxHeight().width(1.dp).background(Color(0xFF2D2D35)))
                    
                    // Visualizer Panel
                    Column(
                        modifier = Modifier
                            .weight(0.55f)
                            .fillMaxHeight()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "Phase pipeline trace",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        PipelineCanvas(
                            readMode = readMode,
                            compositionCount = compositionCount,
                            layoutCount = layoutCount,
                            drawCount = drawCount,
                            pulseComp = pulseComposition,
                            pulseLayout = pulseLayout,
                            pulseDraw = pulseDraw,
                            offset = offsetValue,
                            modifier = Modifier
                                .weight(0.6f)
                                .fillMaxWidth()
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        EducationalInfo(readMode = readMode, modifier = Modifier.weight(0.4f))
                    }
                }
            } else {
                // Portrait stack
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    PipelineCanvas(
                        readMode = readMode,
                        compositionCount = compositionCount,
                        layoutCount = layoutCount,
                        drawCount = drawCount,
                        pulseComp = pulseComposition,
                        pulseLayout = pulseLayout,
                        pulseDraw = pulseDraw,
                        offset = offsetValue,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(280.dp)
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    ControlPanel(
                        readMode = readMode,
                        onReadModeChange = {
                            readMode = it
                            resetStats()
                        },
                        isRunning = isRunning,
                        onToggleRunning = { isRunning = !isRunning },
                        onManualTrigger = {
                            offsetValue = (offsetValue + 15f) % 200f
                        }
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    CodePanel(readMode = readMode)
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    EducationalInfo(readMode = readMode)
                }
            }
        }
    }
}

@Composable
fun ControlPanel(
    readMode: StateReadMode,
    onReadModeChange: (StateReadMode) -> Unit,
    isRunning: Boolean,
    onToggleRunning: () -> Unit,
    onManualTrigger: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E24)),
        border = BorderStroke(1.dp, Color(0xFF2D2D35))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "State Read Strategy",
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))
            
            // Selector buttons
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                StateReadRadioOption(
                    title = "Standard Parameter Read (Heavy)",
                    description = "State read during composition. Invalidates and re-runs all 3 phases.",
                    selected = readMode == StateReadMode.STANDARD,
                    onClick = { onReadModeChange(StateReadMode.STANDARD) }
                )
                
                StateReadRadioOption(
                    title = "Lambda Layout Read (Optimized)",
                    description = "State read deferred to Layout phase using offset lambda. Bypasses Composition.",
                    selected = readMode == StateReadMode.LAYOUT_LAMBDA,
                    onClick = { onReadModeChange(StateReadMode.LAYOUT_LAMBDA) }
                )
                
                StateReadRadioOption(
                    title = "Lambda Draw Read (Maximum Performance)",
                    description = "State read deferred to Draw phase inside graphicsLayer lambda. Bypasses Composition and Layout.",
                    selected = readMode == StateReadMode.DRAW_LAMBDA,
                    onClick = { onReadModeChange(StateReadMode.DRAW_LAMBDA) }
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = onManualTrigger,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6)),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Trigger State Update")
                }
                
                Button(
                    onClick = onToggleRunning,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isRunning) Color(0xFFEF4444) else Color(0xFF10B981)
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(if (isRunning) "Stop Simulator" else "Run Simulator")
                }
            }
        }
    }
}

@Composable
fun StateReadRadioOption(
    title: String,
    description: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) Color(0xFF26262F) else Color.Transparent)
            .clickable { onClick() }
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            onClick = onClick,
            colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF00E5FF))
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(
                text = title,
                color = if (selected) Color.White else Color(0xFFECEFF1),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = description,
                color = Color.Gray,
                fontSize = 11.sp,
                lineHeight = 14.sp
            )
        }
    }
}

@Composable
fun CodePanel(readMode: StateReadMode) {
    val code = when (readMode) {
        StateReadMode.STANDARD -> """
// STATE READ IN COMPOSITION
val offsetState = remember { mutableStateOf(0f) }

Box(
    modifier = Modifier
        // Reads state value during composition:
        // triggers re-composition, measurement, and draw
        .offset(x = offsetState.value.dp, y = 0.dp) 
        .size(60.dp)
        .background(Color.Cyan)
)
        """.trimIndent()
        StateReadMode.LAYOUT_LAMBDA -> """
// DEFERRED READ IN LAYOUT PHASE
val offsetState = remember { mutableStateOf(0f) }

Box(
    modifier = Modifier
        // Lambda defers read to layout measurement:
        // skips composition entirely!
        .offset { IntOffset(offsetState.value.roundToInt(), 0) } 
        .size(60.dp)
        .background(Color.Cyan)
)
        """.trimIndent()
        StateReadMode.DRAW_LAMBDA -> """
// DEFERRED READ IN DRAW PHASE
val offsetState = remember { mutableStateOf(0f) }

Box(
    modifier = Modifier
        // Lambda defers read to graphics pipeline drawing:
        // skips both composition and layout!
        .graphicsLayer { 
            translationX = offsetState.value 
        } 
        .size(60.dp)
        .background(Color.Cyan)
)
        """.trimIndent()
    }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F0F11)),
        border = BorderStroke(1.dp, Color(0xFF26262D))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = "Implementation Code",
                color = Color.Gray,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = code,
                color = Color(0xFF00FF66),
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
                lineHeight = 16.sp
            )
        }
    }
}

@Composable
fun PipelineCanvas(
    readMode: StateReadMode,
    compositionCount: Int,
    layoutCount: Int,
    drawCount: Int,
    pulseComp: Boolean,
    pulseLayout: Boolean,
    pulseDraw: Boolean,
    offset: Float,
    modifier: Modifier = Modifier
) {
    val textMeasurer = rememberTextMeasurer()
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF151518)),
        border = BorderStroke(1.dp, Color(0xFF26262D))
    ) {
        Canvas(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            val canvasWidth = size.width
            val canvasHeight = size.height
            
            val blockWidth = 130f
            val blockHeight = 90f
            
            // Draw connecting flow pipe lines
            val path = Path().apply {
                // Pipe from state trigger
                moveTo(60f, canvasHeight / 2)
                lineTo(canvasWidth - 100f, canvasHeight / 2)
            }
            
            drawPath(
                path = path,
                color = Color(0xFF2D2D35),
                style = Stroke(width = 8f)
            )
            
            // Draw state read trigger origin
            val stateOriginX = 60f
            val stateOriginY = canvasHeight / 2
            drawCircle(
                color = Color(0xFF00FF66),
                radius = 18f,
                center = Offset(stateOriginX, stateOriginY)
            )
            drawCircle(
                color = Color.White,
                radius = 7f,
                center = Offset(stateOriginX, stateOriginY)
            )
            
            // Calculate center points of the three phase blocks
            // Placing blocks linearly
            val blockSpacing = (canvasWidth - 120f - 3 * blockWidth) / 2
            val compX = stateOriginX + 60f
            val layoutX = compX + blockWidth + blockSpacing
            val drawX = layoutX + blockWidth + blockSpacing
            
            val yPos = (canvasHeight - blockHeight) / 2
            
            // Draw Phase 1: Composition
            val compColor = if (readMode == StateReadMode.STANDARD) {
                if (pulseComp) Color(0xFF10B981) else Color(0xFF059669)
            } else {
                Color(0xFF374151) // Disabled/Bypassed
            }
            drawRoundRect(
                color = compColor,
                topLeft = Offset(compX, yPos),
                size = androidx.compose.ui.geometry.Size(blockWidth, blockHeight),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(8f, 8f),
                style = if (readMode == StateReadMode.STANDARD) Stroke(width = if (pulseComp) 6f else 2f) else Stroke(width = 1f)
            )
            if (readMode == StateReadMode.STANDARD) {
                drawRoundRect(
                    color = compColor.copy(alpha = 0.15f),
                    topLeft = Offset(compX, yPos),
                    size = androidx.compose.ui.geometry.Size(blockWidth, blockHeight),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(8f, 8f)
                )
            }
            
            // Draw Phase 2: Layout
            val layoutEnabled = readMode == StateReadMode.STANDARD || readMode == StateReadMode.LAYOUT_LAMBDA
            val layoutColor = if (layoutEnabled) {
                if (pulseLayout) Color(0xFF3B82F6) else Color(0xFF2563EB)
            } else {
                Color(0xFF374151)
            }
            drawRoundRect(
                color = layoutColor,
                topLeft = Offset(layoutX, yPos),
                size = androidx.compose.ui.geometry.Size(blockWidth, blockHeight),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(8f, 8f),
                style = if (layoutEnabled) Stroke(width = if (pulseLayout) 6f else 2f) else Stroke(width = 1f)
            )
            if (layoutEnabled) {
                drawRoundRect(
                    color = layoutColor.copy(alpha = 0.15f),
                    topLeft = Offset(layoutX, yPos),
                    size = androidx.compose.ui.geometry.Size(blockWidth, blockHeight),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(8f, 8f)
                )
            }
            
            // Draw Phase 3: Draw
            val drawColor = if (pulseDraw) Color(0xFF8B5CF6) else Color(0xFF7C3AED)
            drawRoundRect(
                color = drawColor,
                topLeft = Offset(drawX, yPos),
                size = androidx.compose.ui.geometry.Size(blockWidth, blockHeight),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(8f, 8f),
                style = Stroke(width = if (pulseDraw) 6f else 2f)
            )
            drawRoundRect(
                color = drawColor.copy(alpha = 0.15f),
                topLeft = Offset(drawX, yPos),
                size = androidx.compose.ui.geometry.Size(blockWidth, blockHeight),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(8f, 8f)
            )
            
            // Draw simulated moving Box target at the end
            val endBoxX = drawX + blockWidth + 40f
            val endBoxY = stateOriginY
            
            // Animate box translation visually based on offsetValue
            val animatedBoxY = endBoxY - 30f + (offset % 50f - 25f) / 2f
            drawRoundRect(
                color = Color(0xFF00E5FF),
                topLeft = Offset(endBoxX, animatedBoxY),
                size = androidx.compose.ui.geometry.Size(50f, 50f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f, 6f)
            )
            
            // Draw pulse paths from State Origin according to read mode
            val pulseY = stateOriginY
            when (readMode) {
                StateReadMode.STANDARD -> {
                    // Particle travels origin -> composition -> layout -> draw -> box
                    drawCircle(
                        color = Color(0xFF00FF66),
                        radius = 8f,
                        center = Offset(stateOriginX + 30f, pulseY)
                    )
                }
                StateReadMode.LAYOUT_LAMBDA -> {
                    // Curved bridge bypass composition
                    val bypassPath = Path().apply {
                        moveTo(stateOriginX, pulseY)
                        cubicTo(
                            stateOriginX + 50f, pulseY - 80f,
                            layoutX - 50f, pulseY - 80f,
                            layoutX, pulseY
                        )
                    }
                    drawPath(
                        path = bypassPath,
                        color = Color(0xFF3B82F6).copy(alpha = 0.5f),
                        style = Stroke(width = 4f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f))
                    )
                }
                StateReadMode.DRAW_LAMBDA -> {
                    // Large curved bridge bypass composition and layout
                    val bypassPath = Path().apply {
                        moveTo(stateOriginX, pulseY)
                        cubicTo(
                            stateOriginX + 100f, pulseY - 120f,
                            drawX - 100f, pulseY - 120f,
                            drawX, pulseY
                        )
                    }
                    drawPath(
                        path = bypassPath,
                        color = Color(0xFF8B5CF6).copy(alpha = 0.5f),
                        style = Stroke(width = 4f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f))
                    )
                }
            }
            
            // Centered text helper
            val drawCenteredTextInBlock = { title: String, subtitle: String, x: Float, y: Float, width: Float, height: Float, active: Boolean ->
                val textColor = if (active) Color.White else Color.Gray
                val subColor = if (active) Color.LightGray.copy(alpha = 0.8f) else Color.DarkGray
                
                val titleLayout = textMeasurer.measure(
                    text = title,
                    style = TextStyle(
                        color = textColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
                val titleX = x + (width - titleLayout.size.width) / 2f
                val titleY = y + (height - 35f) / 2f - 4f
                drawText(titleLayout, topLeft = Offset(titleX, titleY))
                
                val subLayout = textMeasurer.measure(
                    text = subtitle,
                    style = TextStyle(
                        color = subColor,
                        fontSize = 9.sp,
                        fontFamily = FontFamily.SansSerif
                    )
                )
                val subX = x + (width - subLayout.size.width) / 2f
                val subY = titleY + titleLayout.size.height + 2f
                drawText(subLayout, topLeft = Offset(subX, subY))
            }
            
            drawCenteredTextInBlock(
                "Composition", 
                "What to show", 
                compX, yPos, blockWidth, blockHeight, 
                readMode == StateReadMode.STANDARD
            )
            drawCenteredTextInBlock(
                "Layout", 
                "Where to show", 
                layoutX, yPos, blockWidth, blockHeight, 
                readMode == StateReadMode.STANDARD || readMode == StateReadMode.LAYOUT_LAMBDA
            )
            drawCenteredTextInBlock(
                "Drawing", 
                "How to show", 
                drawX, yPos, blockWidth, blockHeight, 
                true
            )
        }
        
        // Add overlay counts at the bottom
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF111115))
                .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            CountBadge(label = "COMPOSITION", count = compositionCount, active = readMode == StateReadMode.STANDARD)
            CountBadge(label = "LAYOUT", count = layoutCount, active = readMode != StateReadMode.DRAW_LAMBDA)
            CountBadge(label = "DRAW", count = drawCount, active = true)
        }
    }
}

@Composable
fun CountBadge(label: String, count: Int, active: Boolean) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            color = if (active) Color.Gray else Color(0xFF374151)
        )
        Text(
            text = "$count runs",
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = if (active) Color.White else Color(0xFF4B5563)
        )
    }
}

@Composable
fun EducationalInfo(readMode: StateReadMode, modifier: Modifier = Modifier) {
    val text = when (readMode) {
        StateReadMode.STANDARD -> "State value is read directly during the composition scope. Any value change forces Compose to re-run the composable function (Re-composition). The layout must then measure the new tree, and draw the pixels again. This is heavy and can cause UI stutters during scrolling or drag interactions."
        StateReadMode.LAYOUT_LAMBDA -> "State value read is wrapped in a lambda modifier offset { ... }. This tells Compose to defer reading the state until the Layout/Measurement phase. Since the value is not read in composition, Compose bypasses re-composition entirely. Only measure & place runs."
        StateReadMode.DRAW_LAMBDA -> "State value is deferred until the Drawing phase by using a graphicsLayer { ... } modifier. Since the offset is applied as a hardware layer translation during draw, Compose completely bypasses both Composition and Layout phases. Only draw runs, resulting in maximum rendering performance."
    }
    
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF18181C)),
        border = BorderStroke(1.dp, Color(0xFF2D2D35))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = "Performance Insight",
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = text,
                color = Color(0xFFD1D5DB),
                fontSize = 12.sp,
                lineHeight = 16.sp
            )
        }
    }
}
