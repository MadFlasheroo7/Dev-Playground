package pro.jayeshseth.devcontrols.ui

import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class FlowTab { COLD_FLOW, STATE_FLOW, SHARED_FLOW, OPERATORS }
enum class OperatorPreset { BUFFER, COLLECT_LATEST, DEBOUNCE, ZIP, COMBINE }

data class Particle(
    val id: Int,
    val value: String,
    var progress: Float, // 0.0 to 1.0
    val color: Color,
    val sourceLine: Int = 0, // For zip/combine dual sources
    var isCancelled: Boolean = false
)

data class MarbleEvent(
    val id: Int,
    val value: String,
    val timeOffsetMs: Long,
    val type: EventType,
    val lineIndex: Int // 0 = Emitter, 1 = Emitter2 (for dual), 2 = Collector A, 3 = Collector B
)

enum class EventType { EMIT, COLLECT, CANCEL }

@OptIn(ExperimentalMaterial3Api::class, ExperimentalTextApi::class)
@Composable
fun FlowLearnerScreen(onBack: () -> Unit) {
    var selectedTab by remember { mutableStateOf(FlowTab.COLD_FLOW) }
    var selectedOperator by remember { mutableStateOf(OperatorPreset.BUFFER) }

    // Control parameters
    var emitterIntervalMs by remember { mutableStateOf(1000) }
    var collectorDelayMs by remember { mutableStateOf(1500) }
    var isAutoEmitting by remember { mutableStateOf(true) }
    var isPaused by remember { mutableStateOf(false) }

    // Subscribers
    var isSubAConnected by remember { mutableStateOf(false) }
    var isSubBConnected by remember { mutableStateOf(false) }
    var subAConnectionTime by remember { mutableStateOf(0L) }
    var subBConnectionTime by remember { mutableStateOf(0L) }

    // Simulation states
    var systemTimeMs by remember { mutableStateOf(0L) }
    var emissionCounter by remember { mutableStateOf(1) }
    val particles = remember { mutableStateListOf<Particle>() }
    val marbleEvents = remember { mutableStateListOf<MarbleEvent>() }
    val logs = remember { mutableStateListOf<String>() }

    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    val textMeasurer = rememberTextMeasurer()

    // Reset helper
    fun resetSimulation() {
        particles.clear()
        marbleEvents.clear()
        logs.clear()
        systemTimeMs = 0L
        emissionCounter = 1
        isSubAConnected = false
        isSubBConnected = false
        subAConnectionTime = 0L
        subBConnectionTime = 0L
        logs.add("Simulation reset.")
    }

    // Set defaults when changing tabs
    LaunchedEffect(selectedTab) {
        resetSimulation()
        when (selectedTab) {
            FlowTab.COLD_FLOW -> {
                isAutoEmitting = true
                emitterIntervalMs = 1200
                collectorDelayMs = 400 // Fast collector to show sequence
            }
            FlowTab.STATE_FLOW -> {
                isAutoEmitting = true
                isSubAConnected = true // Hot flows typically have at least one collector
                subAConnectionTime = 0L
                emitterIntervalMs = 1000
                collectorDelayMs = 2500 // Slow collector to demonstrate conflation
            }
            FlowTab.SHARED_FLOW -> {
                isAutoEmitting = true
                isSubAConnected = true
                subAConnectionTime = 0L
                emitterIntervalMs = 1200
                collectorDelayMs = 800
            }
            FlowTab.OPERATORS -> {
                isAutoEmitting = true
                isSubAConnected = true
                subAConnectionTime = 0L
                when (selectedOperator) {
                    OperatorPreset.BUFFER -> {
                        emitterIntervalMs = 500
                        collectorDelayMs = 2000
                    }
                    OperatorPreset.COLLECT_LATEST -> {
                        emitterIntervalMs = 800
                        collectorDelayMs = 2000
                    }
                    OperatorPreset.DEBOUNCE -> {
                        emitterIntervalMs = 300 // Burst typing
                        isAutoEmitting = false
                        collectorDelayMs = 1000 // Debounce window
                    }
                    OperatorPreset.ZIP, OperatorPreset.COMBINE -> {
                        emitterIntervalMs = 1000
                        collectorDelayMs = 300
                    }
                }
            }
        }
    }

    // React to changing specific operators
    LaunchedEffect(selectedOperator) {
        if (selectedTab == FlowTab.OPERATORS) {
            resetSimulation()
            isSubAConnected = true
            subAConnectionTime = 0L
            when (selectedOperator) {
                OperatorPreset.BUFFER -> {
                    emitterIntervalMs = 500
                    collectorDelayMs = 2000
                    isAutoEmitting = true
                }
                OperatorPreset.COLLECT_LATEST -> {
                    emitterIntervalMs = 800
                    collectorDelayMs = 2000
                    isAutoEmitting = true
                }
                OperatorPreset.DEBOUNCE -> {
                    emitterIntervalMs = 300
                    isAutoEmitting = false
                    collectorDelayMs = 1000
                }
                OperatorPreset.ZIP, OperatorPreset.COMBINE -> {
                    emitterIntervalMs = 1000
                    collectorDelayMs = 300
                    isAutoEmitting = true
                }
            }
        }
    }

    // Emitter Loop
    LaunchedEffect(selectedTab, selectedOperator, isAutoEmitting, isPaused, emitterIntervalMs) {
        if (!isAutoEmitting || isPaused) return@LaunchedEffect
        var lastEmitTime = 0L
        var lastEmitTime2 = 0L // Dual source for zip/combine

        while (true) {
            delay(100) // Poll timing
            if (isPaused) continue

            val elapsed = systemTimeMs - lastEmitTime
            if (elapsed >= emitterIntervalMs) {
                // Time to emit
                val value = emissionCounter.toString()
                emissionCounter++
                lastEmitTime = systemTimeMs

                // Trigger emission
                triggerEmission(
                    value = value,
                    sourceIndex = 0,
                    selectedTab = selectedTab,
                    selectedOperator = selectedOperator,
                    isSubAConnected = isSubAConnected,
                    isSubBConnected = isSubBConnected,
                    subAConnectionTime = subAConnectionTime,
                    subBConnectionTime = subBConnectionTime,
                    systemTimeMs = systemTimeMs,
                    collectorDelayMs = collectorDelayMs,
                    particles = particles,
                    marbleEvents = marbleEvents,
                    logs = logs,
                    scope = scope
                )
            }

            // Dual flow logic for Zip / Combine
            if (selectedTab == FlowTab.OPERATORS &&
                (selectedOperator == OperatorPreset.ZIP || selectedOperator == OperatorPreset.COMBINE)
            ) {
                val elapsed2 = systemTimeMs - lastEmitTime2
                // Emit on Flow B slightly offset or at a different rate
                if (elapsed2 >= (emitterIntervalMs * 1.4).toLong()) {
                    val letter = ('A' + (emissionCounter % 26)).toString()
                    lastEmitTime2 = systemTimeMs
                    logs.add("Flow B (Source 2) emitted: '$letter'")

                    marbleEvents.add(
                        MarbleEvent(
                            id = emissionCounter * 100,
                            value = letter,
                            timeOffsetMs = systemTimeMs,
                            type = EventType.EMIT,
                            lineIndex = 1 // Emitter 2
                        )
                    )

                    // Zip/Combine operator logic
                    processDualEmissions(
                        selectedOperator = selectedOperator,
                        letter = letter,
                        systemTimeMs = systemTimeMs,
                        collectorDelayMs = collectorDelayMs,
                        marbleEvents = marbleEvents,
                        logs = logs,
                        scope = scope
                    )
                }
            }
        }
    }

    // Engine Animation Clock Loop (Ticks every 50ms)
    LaunchedEffect(isPaused) {
        if (isPaused) return@LaunchedEffect
        while (true) {
            delay(50)
            if (!isPaused) {
                systemTimeMs += 50

                // Update particle positions on visual pipeline track
                val iterator = particles.iterator()
                while (iterator.hasNext()) {
                    val particle = iterator.next()
                    if (particle.isCancelled) {
                        iterator.remove()
                        continue
                    }
                    particle.progress += 0.05f // Speed of particle movement
                    if (particle.progress >= 1.0f) {
                        // Reached end of pipeline! Perform collection event
                        iterator.remove()
                    }
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Kotlin Flow Visualizer",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                },
                navigationIcon = {
                    TextButton(onClick = onBack) { Text("Back") }
                },
                actions = {
                    TextButton(onClick = { resetSimulation() }) {
                        Text("Reset Simulation", color = MaterialTheme.colorScheme.error)
                    }
                }
            )
        },
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(scrollState)
        ) {
            // Tab Header
            ScrollableTabRow(
                selectedTabIndex = selectedTab.ordinal,
                edgePadding = 16.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                FlowTab.values().forEach { tab ->
                    Tab(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        text = {
                            Text(
                                text = when (tab) {
                                    FlowTab.COLD_FLOW -> "Cold Flow"
                                    FlowTab.STATE_FLOW -> "StateFlow (Hot)"
                                    FlowTab.SHARED_FLOW -> "SharedFlow (Hot)"
                                    FlowTab.OPERATORS -> "Operators Lab"
                                },
                                fontWeight = if (selectedTab == tab) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // Sub-selector for operators
            if (selectedTab == FlowTab.OPERATORS) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OperatorPreset.values().forEach { op ->
                        FilterChip(
                            selected = selectedOperator == op,
                            onClick = { selectedOperator = op },
                            label = { Text(op.name) }
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
            }

            // Real-time Visual Pipeline & Scrolling Timeline Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Real-time Marble Diagram Timeline",
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.height(8.dp))

                    // Marble Diagram Canvas
                    MarbleDiagramTimeline(
                        selectedTab = selectedTab,
                        selectedOperator = selectedOperator,
                        systemTimeMs = systemTimeMs,
                        isSubAConnected = isSubAConnected,
                        isSubBConnected = isSubBConnected,
                        subAConnectionTime = subAConnectionTime,
                        subBConnectionTime = subBConnectionTime,
                        events = marbleEvents,
                        textMeasurer = textMeasurer
                    )

                    Spacer(Modifier.height(16.dp))

                    // Pipeline Track Animation
                    Text(
                        text = "Active Streams Pipe Animation",
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Spacer(Modifier.height(8.dp))

                    PipelineAnimation(
                        particles = particles,
                        isSubAConnected = isSubAConnected,
                        isSubBConnected = isSubBConnected,
                        selectedTab = selectedTab
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // Control panel card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Simulation Controls",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(Modifier.height(12.dp))

                    // Emitter & Collector Sliders
                    if (selectedOperator != OperatorPreset.DEBOUNCE && selectedTab != FlowTab.STATE_FLOW) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                "Emitter speed: ${emitterIntervalMs}ms",
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Slider(
                                value = emitterIntervalMs.toFloat(),
                                onValueChange = { emitterIntervalMs = it.toInt() },
                                valueRange = 200f..3000f,
                                modifier = Modifier.width(180.dp)
                            )
                        }
                    }

                    if (selectedOperator != OperatorPreset.ZIP && selectedOperator != OperatorPreset.COMBINE) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = when {
                                    selectedTab == FlowTab.STATE_FLOW -> "Collector processing delay: ${collectorDelayMs}ms"
                                    selectedOperator == OperatorPreset.DEBOUNCE -> "Debounce window: ${collectorDelayMs}ms"
                                    else -> "Collector processing delay: ${collectorDelayMs}ms"
                                },
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Slider(
                                value = collectorDelayMs.toFloat(),
                                onValueChange = { collectorDelayMs = it.toInt() },
                                valueRange = 200f..3000f,
                                modifier = Modifier.width(180.dp)
                            )
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    // Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                if (selectedTab == FlowTab.OPERATORS && selectedOperator == OperatorPreset.DEBOUNCE) {
                                    // Manual burst emission for debounce typing
                                    val value = ('A' + (emissionCounter % 26)).toString()
                                    emissionCounter++
                                    triggerEmission(
                                        value = value,
                                        sourceIndex = 0,
                                        selectedTab = selectedTab,
                                        selectedOperator = selectedOperator,
                                        isSubAConnected = isSubAConnected,
                                        isSubBConnected = isSubBConnected,
                                        subAConnectionTime = subAConnectionTime,
                                        subBConnectionTime = subBConnectionTime,
                                        systemTimeMs = systemTimeMs,
                                        collectorDelayMs = collectorDelayMs,
                                        particles = particles,
                                        marbleEvents = marbleEvents,
                                        logs = logs,
                                        scope = scope
                                    )
                                } else {
                                    isAutoEmitting = !isAutoEmitting
                                }
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                if (selectedTab == FlowTab.OPERATORS && selectedOperator == OperatorPreset.DEBOUNCE) "Simulate Typing Key"
                                else if (isAutoEmitting) "Stop Auto Emission" else "Start Auto Emission"
                            )
                        }

                        OutlinedButton(
                            onClick = { isPaused = !isPaused },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(if (isPaused) "Resume UI" else "Pause UI")
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    // Connect/Disconnect controls
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                isSubAConnected = !isSubAConnected
                                if (isSubAConnected) {
                                    subAConnectionTime = systemTimeMs
                                    logs.add("Subscriber A connected.")
                                    // Visual event
                                    marbleEvents.add(MarbleEvent(-1, "SubA", systemTimeMs, EventType.COLLECT, 2))
                                } else {
                                    logs.add("Subscriber A disconnected.")
                                }
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = if (isSubAConnected) Color(0xFF1B6F3C) else MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Text(if (isSubAConnected) "Disconnect Collector A" else "Connect Collector A")
                        }

                        if (selectedOperator != OperatorPreset.DEBOUNCE &&
                            selectedOperator != OperatorPreset.ZIP &&
                            selectedOperator != OperatorPreset.COMBINE
                        ) {
                            OutlinedButton(
                                onClick = {
                                    isSubBConnected = !isSubBConnected
                                    if (isSubBConnected) {
                                        subBConnectionTime = systemTimeMs
                                        logs.add("Subscriber B connected.")
                                        marbleEvents.add(MarbleEvent(-2, "SubB", systemTimeMs, EventType.COLLECT, 3))
                                    } else {
                                        logs.add("Subscriber B disconnected.")
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = if (isSubBConnected) Color(0xFF1B6F3C) else MaterialTheme.colorScheme.primary
                                )
                            ) {
                                Text(if (isSubBConnected) "Disconnect Collector B" else "Connect Collector B")
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Explanation & Code Snippets Grid
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Tutorial card
                Card(
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "Visual Lesson",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = getTutorialText(selectedTab, selectedOperator),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Code snippet card
                Card(
                    modifier = Modifier.weight(1.2f),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "Kotlin Coroutines Code",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleSmall,
                            color = Color(0xFF80CBC4)
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = getCodeSnippet(selectedTab, selectedOperator, emitterIntervalMs, collectorDelayMs),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = Color(0xFFECEFF1)
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Simulation Logs
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Execution Logs",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .clip(RoundedCornerShape(6.dp))
                            .padding(8.dp)
                    ) {
                        val logScrollState = rememberScrollState()
                        LaunchedEffect(logs.size) {
                            logScrollState.animateScrollTo(logScrollState.maxValue)
                        }
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(logScrollState)
                        ) {
                            logs.forEach { log ->
                                Text(
                                    text = log,
                                    fontSize = 12.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

// Visual timeline drawer drawing beautiful scrolling marble timelines
@OptIn(ExperimentalTextApi::class)
@Composable
fun MarbleDiagramTimeline(
    selectedTab: FlowTab,
    selectedOperator: OperatorPreset,
    systemTimeMs: Long,
    isSubAConnected: Boolean,
    isSubBConnected: Boolean,
    subAConnectionTime: Long,
    subBConnectionTime: Long,
    events: List<MarbleEvent>,
    textMeasurer: TextMeasurer
) {
    val density = LocalDensity.current
    val strokeWidth = with(density) { 2.dp.toPx() }
    val normalLineColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
    val gridColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f)
    val textStyle = TextStyle(fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold)

    // Layout configuration
    val lineSpacing = 120f
    val height = 400f
    val paddingLeft = 140f
    val timelineWidth = 600f // Scroll window size in pixels

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .background(Color(0xFF121212), shape = RoundedCornerShape(8.dp))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape = RoundedCornerShape(8.dp))
    ) {
        val viewWidth = size.width
        val canvasHeight = size.height

        // 1. Draw Labels on the left
        drawText(
            textMeasurer = textMeasurer,
            text = "Emitter",
            topLeft = Offset(15f, 25f),
            style = TextStyle(color = Color(0xFFFFCC00), fontSize = 11.sp, fontWeight = FontWeight.Bold)
        )

        val secondEmitterActive = selectedTab == FlowTab.OPERATORS &&
                (selectedOperator == OperatorPreset.ZIP || selectedOperator == OperatorPreset.COMBINE)

        if (secondEmitterActive) {
            drawText(
                textMeasurer = textMeasurer,
                text = "Emitter B",
                topLeft = Offset(15f, 75f),
                style = TextStyle(color = Color(0xFF26C6DA), fontSize = 11.sp, fontWeight = FontWeight.Bold)
            )
        }

        drawText(
            textMeasurer = textMeasurer,
            text = "Collector A",
            topLeft = Offset(15f, 115f),
            style = TextStyle(
                color = if (isSubAConnected) Color(0xFF66BB6A) else Color.Gray,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        )

        if (selectedOperator != OperatorPreset.DEBOUNCE && !secondEmitterActive) {
            drawText(
                textMeasurer = textMeasurer,
                text = "Collector B",
                topLeft = Offset(15f, 155f),
                style = TextStyle(
                    color = if (isSubBConnected) Color(0xFF29B6F6) else Color.Gray,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            )
        }

        // Horizontal axes
        drawLine(
            color = Color(0xFFFFCC00).copy(alpha = 0.7f),
            start = Offset(paddingLeft, 35f),
            end = Offset(viewWidth - 20f, 35f),
            strokeWidth = strokeWidth
        )

        if (secondEmitterActive) {
            drawLine(
                color = Color(0xFF26C6DA).copy(alpha = 0.7f),
                start = Offset(paddingLeft, 85f),
                end = Offset(viewWidth - 20f, 85f),
                strokeWidth = strokeWidth
            )
        }

        drawLine(
            color = if (isSubAConnected) Color(0xFF66BB6A) else Color.Gray.copy(alpha = 0.4f),
            start = Offset(paddingLeft, 125f),
            end = Offset(viewWidth - 20f, 125f),
            strokeWidth = strokeWidth
        )

        if (selectedOperator != OperatorPreset.DEBOUNCE && !secondEmitterActive) {
            drawLine(
                color = if (isSubBConnected) Color(0xFF29B6F6) else Color.Gray.copy(alpha = 0.4f),
                start = Offset(paddingLeft, 165f),
                end = Offset(viewWidth - 20f, 165f),
                strokeWidth = strokeWidth
            )
        }

        // Draw rolling grids based on systemTimeMs
        val scale = 0.05f // Pixels per ms
        val timelineStartX = paddingLeft
        val timelineEndX = viewWidth - 30f
        val maxVisibleMs = (timelineEndX - timelineStartX) / scale

        // Vertical timeline bar mapping
        events.forEach { event ->
            val eventX = timelineEndX - (systemTimeMs - event.timeOffsetMs) * scale
            // Only draw if inside bounds
            if (eventX in timelineStartX..timelineEndX) {
                val lineY = when (event.lineIndex) {
                    0 -> 35f
                    1 -> 85f
                    2 -> 125f
                    3 -> 165f
                    else -> 35f
                }

                // If it is a connection line, draw connection dotted path
                if (event.type == EventType.COLLECT) {
                    // Find emitter offset
                    val emitterY = if (secondEmitterActive && event.lineIndex >= 2 && event.value.startsWith("Pair")) 85f else 35f

                    // Draw vertical collection trigger
                    drawLine(
                        color = Color.White.copy(alpha = 0.15f),
                        start = Offset(eventX, emitterY),
                        end = Offset(eventX, lineY),
                        strokeWidth = strokeWidth / 2,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                    )
                }

                // Draw circle node
                val color = when (event.lineIndex) {
                    0 -> Color(0xFFFFCC00) // Yellow Emitter
                    1 -> Color(0xFF26C6DA) // Cyan Emitter
                    2 -> Color(0xFF66BB6A) // Green Collector A
                    3 -> Color(0xFF29B6F6) // Blue Collector B
                    else -> Color.White
                }

                // If it's collect-latest cancel event, draw cross
                if (event.type == EventType.CANCEL) {
                    drawLine(
                        color = Color.Red,
                        start = Offset(eventX - 8f, lineY - 8f),
                        end = Offset(eventX + 8f, lineY + 8f),
                        strokeWidth = strokeWidth * 1.5f
                    )
                    drawLine(
                        color = Color.Red,
                        start = Offset(eventX + 8f, lineY - 8f),
                        end = Offset(eventX - 8f, lineY + 8f),
                        strokeWidth = strokeWidth * 1.5f
                    )
                } else {
                    // Normal bubble
                    drawCircle(
                        color = color,
                        radius = 16f,
                        center = Offset(eventX, lineY)
                    )

                    // Draw value text inside bubble
                    val valText = event.value
                    if (valText.length <= 4) {
                        drawText(
                            textMeasurer = textMeasurer,
                            text = valText,
                            topLeft = Offset(eventX - 8f, lineY - 14f),
                            style = textStyle
                        )
                    }
                }
            }
        }
    }
}

// Pipeline visual animation track showing flow of values down pipes
@Composable
fun PipelineAnimation(
    particles: List<Particle>,
    isSubAConnected: Boolean,
    isSubBConnected: Boolean,
    selectedTab: FlowTab
) {
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp)
            .background(Color(0xFF1E1E1E), shape = RoundedCornerShape(8.dp))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape = RoundedCornerShape(8.dp))
    ) {
        val width = size.width
        val height = size.height

        // Emitter box (Source)
        val srcRectX = 40f
        val srcRectY = height / 2 - 25f
        drawRect(
            color = Color(0xFFFFCC00),
            topLeft = Offset(srcRectX, srcRectY),
            size = androidx.compose.ui.geometry.Size(60f, 50f)
        )

        // Draw pipeline pipe
        val pipeStartY = height / 2
        val pipeEndX = width - 120f
        drawLine(
            color = Color.DarkGray,
            start = Offset(srcRectX + 60f, pipeStartY),
            end = Offset(pipeEndX, pipeStartY),
            strokeWidth = 12f
        )

        // Sub A connection bridge
        if (isSubAConnected) {
            drawLine(
                color = Color(0xFF66BB6A),
                start = Offset(pipeEndX, pipeStartY),
                end = Offset(width - 60f, pipeStartY - 20f),
                strokeWidth = 6f
            )
            // Subscriber Box A
            drawRect(
                color = Color(0xFF66BB6A),
                topLeft = Offset(width - 60f, pipeStartY - 35f),
                size = androidx.compose.ui.geometry.Size(40f, 30f)
            )
        }

        // Sub B connection bridge
        if (isSubBConnected && selectedTab != FlowTab.OPERATORS) {
            drawLine(
                color = Color(0xFF29B6F6),
                start = Offset(pipeEndX, pipeStartY),
                end = Offset(width - 60f, pipeStartY + 20f),
                strokeWidth = 6f
            )
            // Subscriber Box B
            drawRect(
                color = Color(0xFF29B6F6),
                topLeft = Offset(width - 60f, pipeStartY + 5f),
                size = androidx.compose.ui.geometry.Size(40f, 30f)
            )
        }

        // Draw particles traversing the pipe
        particles.forEach { particle ->
            val pX = (srcRectX + 60f) + (pipeEndX - (srcRectX + 60f)) * particle.progress
            drawCircle(
                color = particle.color,
                radius = 12f,
                center = Offset(pX, pipeStartY)
            )
        }
    }
}

// Triggers an emission and launches coroutines representing collectors subscribing
fun triggerEmission(
    value: String,
    sourceIndex: Int,
    selectedTab: FlowTab,
    selectedOperator: OperatorPreset,
    isSubAConnected: Boolean,
    isSubBConnected: Boolean,
    subAConnectionTime: Long,
    subBConnectionTime: Long,
    systemTimeMs: Long,
    collectorDelayMs: Int,
    particles: MutableList<Particle>,
    marbleEvents: MutableList<MarbleEvent>,
    logs: MutableList<String>,
    scope: kotlinx.coroutines.CoroutineScope
) {
    logs.add("Emitter emitted value: '$value'")

    // Add emission event to the top line (0)
    marbleEvents.add(
        MarbleEvent(
            id = value.hashCode() + sourceIndex,
            value = value,
            timeOffsetMs = systemTimeMs,
            type = EventType.EMIT,
            lineIndex = 0
        )
    )

    // Add a visual bubble particle
    val color = when (sourceIndex) {
        0 -> Color(0xFFFFCC00)
        else -> Color(0xFF26C6DA)
    }
    val particle = Particle(
        id = value.hashCode() + sourceIndex,
        value = value,
        progress = 0f,
        color = color,
        sourceLine = sourceIndex
    )
    particles.add(particle)

    // Collector A logic
    if (isSubAConnected) {
        if (selectedTab == FlowTab.COLD_FLOW) {
            // Cold flow: Collector gets the full stream
            scope.launch {
                val delayTime = (particle.progress * 1000).toLong() + collectorDelayMs
                delay(delayTime)
                logs.add("Collector A collected: '$value'")
                marbleEvents.add(
                    MarbleEvent(
                        id = value.hashCode() + 10,
                        value = value,
                        timeOffsetMs = systemTimeMs + delayTime,
                        type = EventType.COLLECT,
                        lineIndex = 2 // Collector A line
                    )
                )
            }
        } else if (selectedTab == FlowTab.STATE_FLOW) {
            // StateFlow: Conflation
            // If Collector A is slow, it drops intervening emissions
            scope.launch {
                val startCollectTime = systemTimeMs
                // Check if Collector is busy. We simulate processing delay
                // In StateFlow, collector only reads latest.
                delay(collectorDelayMs.toLong())
                logs.add("Collector A processed state: '$value'")
                marbleEvents.add(
                    MarbleEvent(
                        id = value.hashCode() + 10,
                        value = value,
                        timeOffsetMs = systemTimeMs + collectorDelayMs,
                        type = EventType.COLLECT,
                        lineIndex = 2
                    )
                )
            }
        } else if (selectedTab == FlowTab.OPERATORS && selectedOperator == OperatorPreset.COLLECT_LATEST) {
            // Cancel any previously active collections for Collector A
            marbleEvents.filter { it.lineIndex == 2 && it.type == EventType.COLLECT }.forEach {
                // Remove or mark cancelled
            }
            // Add cancellation cross visually
            val activeEvents = marbleEvents.filter { it.lineIndex == 2 && it.type == EventType.COLLECT }
            if (activeEvents.isNotEmpty()) {
                val lastEvent = activeEvents.last()
                if (systemTimeMs - lastEvent.timeOffsetMs < collectorDelayMs) {
                    marbleEvents.add(
                        MarbleEvent(
                            id = lastEvent.id + 999,
                            value = "X",
                            timeOffsetMs = systemTimeMs,
                            type = EventType.CANCEL,
                            lineIndex = 2
                        )
                    )
                    logs.add("CollectLatest: Cancelled processing of '${lastEvent.value}' due to new emission '$value'")
                }
            }

            scope.launch {
                delay(collectorDelayMs.toLong())
                logs.add("CollectLatest completed: '$value'")
                marbleEvents.add(
                    MarbleEvent(
                        id = value.hashCode() + 10,
                        value = value,
                        timeOffsetMs = systemTimeMs + collectorDelayMs,
                        type = EventType.COLLECT,
                        lineIndex = 2
                    )
                )
            }
        } else if (selectedTab == FlowTab.OPERATORS && selectedOperator == OperatorPreset.DEBOUNCE) {
            // Debounce window
            scope.launch {
                val myEmitTime = systemTimeMs
                delay(collectorDelayMs.toLong()) // wait debounce window
                // If no other emissions happened since myEmitTime
                val isLatest = marbleEvents.filter { it.lineIndex == 0 }.maxByOrNull { it.timeOffsetMs }?.timeOffsetMs == myEmitTime
                if (isLatest) {
                    logs.add("Debounce passed: Collected '$value'")
                    marbleEvents.add(
                        MarbleEvent(
                            id = value.hashCode() + 10,
                            value = value,
                            timeOffsetMs = systemTimeMs + collectorDelayMs,
                            type = EventType.COLLECT,
                            lineIndex = 2
                        )
                    )
                } else {
                    logs.add("Debounce rejected intermediate character: '$value'")
                }
            }
        } else {
            // SharedFlow / standard emit
            scope.launch {
                delay(400) // Pipeline travel delay
                logs.add("Collector A received: '$value'")
                marbleEvents.add(
                    MarbleEvent(
                        id = value.hashCode() + 10,
                        value = value,
                        timeOffsetMs = systemTimeMs + 400,
                        type = EventType.COLLECT,
                        lineIndex = 2
                    )
                )
            }
        }
    }

    // Collector B logic
    if (isSubBConnected && selectedTab != FlowTab.OPERATORS) {
        if (selectedTab == FlowTab.COLD_FLOW) {
            // Cold flow B gets same sequence starting from its connection offset
            scope.launch {
                val delayTime = (particle.progress * 1000).toLong() + collectorDelayMs
                delay(delayTime)
                logs.add("Collector B collected: '$value'")
                marbleEvents.add(
                    MarbleEvent(
                        id = value.hashCode() + 20,
                        value = value,
                        timeOffsetMs = systemTimeMs + delayTime,
                        type = EventType.COLLECT,
                        lineIndex = 3 // Collector B line
                    )
                )
            }
        } else {
            // Hot Flow B gets future items
            scope.launch {
                delay(400)
                logs.add("Collector B received event: '$value'")
                marbleEvents.add(
                    MarbleEvent(
                        id = value.hashCode() + 20,
                        value = value,
                        timeOffsetMs = systemTimeMs + 400,
                        type = EventType.COLLECT,
                        lineIndex = 3
                    )
                )
            }
        }
    }
}

// Dual emission processor for Zip / Combine operators
fun processDualEmissions(
    selectedOperator: OperatorPreset,
    letter: String,
    systemTimeMs: Long,
    collectorDelayMs: Int,
    marbleEvents: MutableList<MarbleEvent>,
    logs: MutableList<String>,
    scope: kotlinx.coroutines.CoroutineScope
) {
    // Find latest emissions from line 0 (numbers) and line 1 (letters)
    val numberEmissions = marbleEvents.filter { it.lineIndex == 0 && it.type == EventType.EMIT }
    val letterEmissions = marbleEvents.filter { it.lineIndex == 1 && it.type == EventType.EMIT }

    if (numberEmissions.isEmpty() || letterEmissions.isEmpty()) return

    if (selectedOperator == OperatorPreset.ZIP) {
        // Zip matches index-by-index. Find unmatched pairs
        val zippedCount = marbleEvents.filter { it.lineIndex == 2 && it.type == EventType.COLLECT }.size
        if (numberEmissions.size > zippedCount && letterEmissions.size > zippedCount) {
            val num = numberEmissions[zippedCount].value
            val let = letterEmissions[zippedCount].value
            val pairStr = "$num-$let"

            scope.launch {
                delay(collectorDelayMs.toLong())
                logs.add("Zip collected pair: ($num, $let)")
                marbleEvents.add(
                    MarbleEvent(
                        id = pairStr.hashCode(),
                        value = pairStr,
                        timeOffsetMs = systemTimeMs + collectorDelayMs,
                        type = EventType.COLLECT,
                        lineIndex = 2 // Collector line
                    )
                )
            }
        }
    } else if (selectedOperator == OperatorPreset.COMBINE) {
        // Combine matches latest whenever either emits
        val latestNum = numberEmissions.last().value
        val latestLet = letterEmissions.last().value
        val pairStr = "$latestNum-$latestLet"

        scope.launch {
            delay(collectorDelayMs.toLong())
            logs.add("Combine collected combination: ($latestNum, $latestLet)")
            marbleEvents.add(
                MarbleEvent(
                    id = pairStr.hashCode() + systemTimeMs.toInt(),
                    value = pairStr,
                    timeOffsetMs = systemTimeMs + collectorDelayMs,
                    type = EventType.COLLECT,
                    lineIndex = 2
                )
            )
        }
    }
}

// Get rich visual descriptions
fun getTutorialText(tab: FlowTab, operator: OperatorPreset): String {
    return when (tab) {
        FlowTab.COLD_FLOW -> {
            "A Cold Flow is a data stream whose producer starts emitting values ONLY when a collector calls collect().\n\n" +
            "Key Observations:\n" +
            "• If no collectors are connected, nothing is emitted.\n" +
            "• Each collector (Collector A and Collector B) gets its own independent execution. If B connects later, B starts receiving emissions from the first element (1, 2, 3...) regardless of what A has already received."
        }
        FlowTab.STATE_FLOW -> {
            "StateFlow is a hot flow that represents a state-holding value (similar to a LiveData variable). It always contains a single 'current' value.\n\n" +
            "Key Observations:\n" +
            "• Since it's 'hot', it emits even if no collectors are active.\n" +
            "• New collectors immediately receive the current state upon connecting.\n" +
            "• StateFlow conflates values: if the collector is processing slowly (long delay), intermediate emissions are skipped/dropped. Only the latest state is processed when the collector is ready."
        }
        FlowTab.SHARED_FLOW -> {
            "SharedFlow is a hot flow optimized for broadcasting events/one-time triggers. It can broadcast to multiple collectors simultaneously.\n\n" +
            "Key Observations:\n" +
            "• If no collectors are active, emissions are dropped/lost immediately (unless a replay cache > 0 is configured).\n" +
            "• All connected collectors receive the broadcast event at the same time."
        }
        FlowTab.OPERATORS -> {
            when (operator) {
                OperatorPreset.BUFFER -> {
                    "Buffer allows a flow to run the emitter concurrently with the collector.\n\n" +
                    "Normal flow runs sequentially: the emitter waits for the collector to finish before emitting the next item. Buffer creates a queue, allowing a fast emitter to keep emitting without being blocked by a slow collector."
                }
                OperatorPreset.COLLECT_LATEST -> {
                    "CollectLatest cancels the collector's current processing block whenever a new item is emitted.\n\n" +
                    "Watch the diagram: if the collector is slowly processing an item (takes 2 seconds) and a new item arrives, you will see a red 'X' indicating the previous work was cancelled, and the collector restarts using the new value."
                }
                OperatorPreset.DEBOUNCE -> {
                    "Debounce only emits an item to the collector if a specified timing window has passed without any new emissions.\n\n" +
                    "Ideal for search autocomplete text inputs. Click 'Simulate Typing Key' rapidly to type keys 'A', 'B', 'C'. Notice that intermediate emissions are ignored, and only the final state is collected once you stop typing."
                }
                OperatorPreset.ZIP -> {
                    "Zip pairs emissions from two source flows index-by-index (1st of A with 1st of B, 2nd of A with 2nd of B).\n\n" +
                    "It waits for both source lines to emit before delivering the pair. If one stream is faster, items will wait in a queue for their matching partner from the other stream."
                }
                OperatorPreset.COMBINE -> {
                    "Combine pairs the latest emitted values of both source flows whenever either of them emits a new value.\n\n" +
                    "Unlike Zip, it does not wait for index pairs. It instantly pairs the absolute latest item from Flow A with the absolute latest item from Flow B, producing updates much more frequently."
                }
            }
        }
    }
}

// Get coroutine code snippet
fun getCodeSnippet(tab: FlowTab, operator: OperatorPreset, emitMs: Int, collectMs: Int): String {
    return when (tab) {
        FlowTab.COLD_FLOW -> {
            "// Cold Flow Definition\n" +
            "val coldFlow = flow {\n" +
            "    var x = 1\n" +
            "    while(true) {\n" +
            "        emit(x++)\n" +
            "        delay(${emitMs}L)\n" +
            "    }\n" +
            "}\n\n" +
            "// Collector A\n" +
            "scope.launch {\n" +
            "    coldFlow.collect { value ->\n" +
            "        println(\"A: \$value\")\n" +
            "    }\n" +
            "}\n\n" +
            "// Collector B (Starts later)\n" +
            "scope.launch {\n" +
            "    delay(3000)\n" +
            "    coldFlow.collect { value ->\n" +
            "        println(\"B: \$value\")\n" +
            "    }\n" +
            "}"
        }
        FlowTab.STATE_FLOW -> {
            "// StateFlow (Hot Flow)\n" +
            "val _state = MutableStateFlow(1)\n" +
            "val stateFlow = _state.asStateFlow()\n\n" +
            "// Producer emitting states\n" +
            "scope.launch {\n" +
            "    var x = 1\n" +
            "    while(true) {\n" +
            "        _state.value = x++\n" +
            "        delay(${emitMs}L)\n" +
            "    }\n" +
            "}\n\n" +
            "// Collector A (Processes slowly)\n" +
            "scope.launch {\n" +
            "    stateFlow.collect { state ->\n" +
            "        // Conflates: drops items if busy\n" +
            "        delay(${collectMs}L)\n" +
            "        println(\"Processed: \$state\")\n" +
            "    }\n" +
            "}"
        }
        FlowTab.SHARED_FLOW -> {
            "// SharedFlow (Broadcast events)\n" +
            "val eventFlow = MutableSharedFlow<Int>(\n" +
            "    replay = 0 // events are lost if no active sub\n" +
            ")\n\n" +
            "// Emitter\n" +
            "scope.launch {\n" +
            "    var x = 1\n" +
            "    while(true) {\n" +
            "        eventFlow.emit(x++)\n" +
            "        delay(${emitMs}L)\n" +
            "    }\n" +
            "}\n\n" +
            "// Active Collector A\n" +
            "scope.launch {\n" +
            "    eventFlow.collect { event ->\n" +
            "        println(\"A: \$event\")\n" +
            "    }\n" +
            "}"
        }
        FlowTab.OPERATORS -> {
            when (operator) {
                OperatorPreset.BUFFER -> {
                    "// Buffer Operator\n" +
                    "flow {\n" +
                    "    (1..10).forEach { \n" +
                    "        emit(it)\n" +
                    "        delay(${emitMs}L) // Fast emitter\n" +
                    "    }\n" +
                    "}\n" +
                    ".buffer() // Runs emitter & collector concurrently\n" +
                    ".collect { value ->\n" +
                    "    delay(${collectMs}L) // Slow collector\n" +
                    "    println(value)\n" +
                    "}"
                }
                OperatorPreset.COLLECT_LATEST -> {
                    "// CollectLatest Operator\n" +
                    "flow {\n" +
                    "    var x = 1\n" +
                    "    while(true) {\n" +
                    "        emit(x++)\n" +
                    "        delay(${emitMs}L)\n" +
                    "    }\n" +
                    "}\n" +
                    ".collectLatest { value ->\n" +
                    "    // Cancelled if new value arrives before finishing\n" +
                    "    delay(${collectMs}L)\n" +
                    "    println(\"Finished: \$value\")\n" +
                    "}"
                }
                OperatorPreset.DEBOUNCE -> {
                    "// Debounce (API search filter)\n" +
                    "val queryFlow = MutableSharedFlow<String>()\n\n" +
                    "queryFlow\n" +
                    "    .debounce(${collectMs}L) // wait for typing pause\n" +
                    "    .collect { query ->\n" +
                    "        performSearch(query)\n" +
                    "    }"
                }
                OperatorPreset.ZIP -> {
                    "// Zip dual flows\n" +
                    "val flowA = flowOf(1, 2, 3)\n" +
                    "val flowB = flowOf(\"A\", \"B\", \"C\")\n\n" +
                    "flowA.zip(flowB) { a, b ->\n" +
                    "    \"\$a-\$b\"\n" +
                    "}\n" +
                    ".collect { zipped ->\n" +
                    "    // 1-A, 2-B, 3-C\n" +
                    "    println(zipped)\n" +
                    "}"
                }
                OperatorPreset.COMBINE -> {
                    "// Combine dual flows\n" +
                    "val flowA = flowOf(1, 2, 3).onEach { delay(${emitMs}L) }\n" +
                    "val flowB = flowOf(\"A\", \"B\").onEach { delay(${(emitMs*1.4).toInt()}L) }\n\n" +
                    "flowA.combine(flowB) { a, b ->\n" +
                    "    \"\$a-\$b\"\n" +
                    "}\n" +
                    ".collect { combined ->\n" +
                    "    // Combines latest active elements\n" +
                    "    println(combined)\n" +
                    "}"
                }
            }
        }
    }
}
