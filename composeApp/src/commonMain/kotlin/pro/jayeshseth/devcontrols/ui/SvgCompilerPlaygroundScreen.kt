package pro.jayeshseth.devcontrols.ui

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
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
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// AST SVG Path Segment definitions
sealed class PathSegment {
    data class MoveTo(val x: Float, val y: Float, val isRelative: Boolean = false) : PathSegment()
    data class LineTo(val x: Float, val y: Float, val isRelative: Boolean = false) : PathSegment()
    data class HorizontalLineTo(val x: Float, val isRelative: Boolean = false) : PathSegment()
    data class VerticalLineTo(val y: Float, val isRelative: Boolean = false) : PathSegment()
    data class CubicTo(
        val x1: Float, val y1: Float,
        val x2: Float, val y2: Float,
        val x: Float, val y: Float,
        val isRelative: Boolean = false
    ) : PathSegment()
    data class QuadraticTo(
        val x1: Float, val y1: Float,
        val x: Float, val y: Float,
        val isRelative: Boolean = false
    ) : PathSegment()
    object Close : PathSegment()
}

// Draggable handle structure
data class DragHandle(
    val segmentIndex: Int,
    val handleType: HandleType,
    val position: Offset
)

enum class HandleType {
    MOVE_END,
    LINE_END,
    HORIZ_END,
    VERT_END,
    CUBIC_C1,
    CUBIC_C2,
    CUBIC_END,
    QUAD_C1,
    QUAD_END
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SvgCompilerPlaygroundScreen(onBack: () -> Unit) {
    val presets = remember {
        listOf(
            Pair("Symmetrical Heart", "M 150 240 C 60 180, 40 80, 150 80 C 260 80, 240 180, 150 240 Z"),
            Pair("Bezier Wave", "M 20 150 C 80 50, 120 50, 150 150 C 180 250, 220 250, 280 150"),
            Pair("Drop Loop", "M 150 50 C 220 150, 190 220, 150 220 C 110 220, 80 150, 150 50 Z")
        )
    }

    var selectedPresetIndex by remember { mutableStateOf(0) }
    var pathInput by remember { mutableStateOf(presets[selectedPresetIndex].second) }

    // Compiler outputs
    var parsedSegments by remember { mutableStateOf<List<PathSegment>>(emptyList()) }
    val compilerLogs = remember { mutableStateListOf<String>() }
    var compileTimeMs by remember { mutableStateOf(0L) }
    
    // Snapping configuration
    var isSnappingEnabled by remember { mutableStateOf(true) }

    // Tokenize and compile path on input change
    LaunchedEffect(pathInput) {
        // Try parsing only if not being dragged currently (to prevent feedback loops)
        compilerLogs.clear()
        val startTime = currentTimeMillis()

        compilerLogs.add("Compiling SVG path string...")
        val segments = parseSvgPath(pathInput, compilerLogs)
        parsedSegments = segments

        val endTime = currentTimeMillis()
        compileTimeMs = endTime - startTime
        compilerLogs.add("Compiled: ${segments.size} instruction segments parsed in ${compileTimeMs}ms.")
    }

    var selectedTab by remember { mutableStateOf(0) } // 0: Canvas Editor, 1: AST Tree, 2: Compiler Logs

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("SVG Path Compiler Lab", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    TextButton(onClick = onBack) {
                        Text("< Back", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                },
                actions = {
                    TextButton(onClick = {
                        pathInput = presets[selectedPresetIndex].second
                    }) {
                        Text("Reset Path", color = Color(0xFFEF4444))
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
                    // Left Column: SVG Path string editor & logs
                    Column(
                        modifier = Modifier
                            .weight(0.45f)
                            .fillMaxHeight()
                            .padding(12.dp)
                    ) {
                        // Presets Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            presets.forEachIndexed { index, pair ->
                                Button(
                                    onClick = {
                                        selectedPresetIndex = index
                                        pathInput = pair.second
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (selectedPresetIndex == index) Color(0xFF3B82F6) else Color(0xFF26262F)
                                    ),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(pair.first, fontSize = 11.sp)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Snapping controls
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Snap to 10px Grid", color = Color.Gray, fontSize = 13.sp)
                            Switch(
                                checked = isSnappingEnabled,
                                onCheckedChange = { isSnappingEnabled = it }
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Text Editor Card
                        Card(
                            modifier = Modifier.weight(1f).fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F0F11)),
                            border = BorderStroke(1.dp, Color(0xFF26262D))
                        ) {
                            Column(modifier = Modifier.fillMaxSize()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color(0xFF1B1B1F))
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text("svg-path.d", color = Color.Gray, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                                }

                                BasicTextField(
                                    value = pathInput,
                                    onValueChange = { pathInput = it },
                                    textStyle = TextStyle(
                                        color = Color(0xFF00FF66),
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 13.sp
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxWidth()
                                        .verticalScroll(rememberScrollState())
                                        .padding(12.dp)
                                )
                            }
                        }
                    }

                    Box(modifier = Modifier.fillMaxHeight().width(1.dp).background(Color(0xFF2D2D35)))

                    // Right Column: Canvas Preview, AST, Logs
                    Column(
                        modifier = Modifier
                            .weight(0.55f)
                            .fillMaxHeight()
                            .padding(12.dp)
                    ) {
                        TabRow(
                            selectedTabIndex = selectedTab,
                            containerColor = Color.Transparent,
                            contentColor = Color.White,
                            modifier = Modifier.fillMaxWidth().height(42.dp)
                        ) {
                            Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Canvas Editor", fontSize = 12.sp) })
                            Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("AST Segments", fontSize = 12.sp) })
                            Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }, text = { Text("Compiler Logs", fontSize = 12.sp) })
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                        ) {
                            when (selectedTab) {
                                0 -> CanvasEditorTab(
                                    segments = parsedSegments,
                                    isSnapping = isSnappingEnabled,
                                    onUpdateSegments = { updated ->
                                        parsedSegments = updated
                                        pathInput = serializeSvgPath(updated)
                                    }
                                )
                                1 -> SvgAstTab(parsedSegments = parsedSegments)
                                2 -> SvgLogsTab(logs = compilerLogs)
                            }
                        }
                    }
                }
            } else {
                // Portrait stack
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp)
                ) {
                    TabRow(
                        selectedTabIndex = selectedTab,
                        containerColor = Color.Transparent,
                        contentColor = Color.White,
                        modifier = Modifier.fillMaxWidth().height(40.dp)
                    ) {
                        Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Canvas", fontSize = 11.sp) })
                        Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("AST", fontSize = 11.sp) })
                        Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }, text = { Text("Logs", fontSize = 11.sp) })
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(290.dp)
                            .padding(vertical = 8.dp)
                    ) {
                        when (selectedTab) {
                            0 -> CanvasEditorTab(
                                segments = parsedSegments,
                                isSnapping = isSnappingEnabled,
                                onUpdateSegments = { updated ->
                                    parsedSegments = updated
                                    pathInput = serializeSvgPath(updated)
                                }
                            )
                            1 -> SvgAstTab(parsedSegments = parsedSegments)
                            2 -> SvgLogsTab(logs = compilerLogs)
                        }
                    }

                    HorizontalDivider(color = Color(0xFF2D2D35))
                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Snap to 10px Grid", color = Color.Gray, fontSize = 12.sp)
                        Switch(
                            checked = isSnappingEnabled,
                            onCheckedChange = { isSnappingEnabled = it }
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Bottom part: Editor
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F0F11)),
                        border = BorderStroke(1.dp, Color(0xFF26262D))
                    ) {
                        BasicTextField(
                            value = pathInput,
                            onValueChange = { pathInput = it },
                            textStyle = TextStyle(
                                color = Color(0xFF00FF66),
                                fontFamily = FontFamily.Monospace,
                                fontSize = 13.sp
                            ),
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .padding(12.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CanvasEditorTab(
    segments: List<PathSegment>,
    isSnapping: Boolean,
    onUpdateSegments: (List<PathSegment>) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxSize(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF070709)),
        border = BorderStroke(1.dp, Color(0xFF26262D))
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            // Target coordinate system: 300x300
            val virtualSize = 300f
            val canvasWidthPx = constraints.maxWidth.toFloat()
            val canvasHeightPx = constraints.maxHeight.toFloat()
            val scale = minOf(canvasWidthPx, canvasHeightPx) / virtualSize

            val dragHandles = remember(segments) { calculateDragHandles(segments) }
            val density = LocalDensity.current.density
            
            val currentSegments by rememberUpdatedState(segments)
            val currentDragHandles by rememberUpdatedState(dragHandles)
            
            var draggedSegmentIndex by remember { mutableStateOf(-1) }
            var draggedHandleType by remember { mutableStateOf<HandleType?>(null) }
            var draggedPosition by remember { mutableStateOf(Offset.Zero) }

            Canvas(
                modifier = Modifier
                    .size(300.dp)
                    .background(Color(0xFF0D0D11))
                    .border(1.dp, Color(0xFF1B1B22))
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = { startOffset ->
                                val virtualStart = startOffset / scale
                                val handle = currentDragHandles.minByOrNull { h ->
                                    (h.position - virtualStart).getDistanceSquared()
                                }?.takeIf {
                                    (it.position - virtualStart).getDistance() < 18f
                                }
                                if (handle != null) {
                                    draggedSegmentIndex = handle.segmentIndex
                                    draggedHandleType = handle.handleType
                                    draggedPosition = handle.position
                                }
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                if (draggedSegmentIndex != -1 && draggedHandleType != null) {
                                    val delta = dragAmount / scale
                                    var nextPos = draggedPosition + delta
                                    nextPos = Offset(
                                        nextPos.x.coerceIn(0f, virtualSize),
                                        nextPos.y.coerceIn(0f, virtualSize)
                                    )
                                    draggedPosition = nextPos

                                    val finalPos = if (isSnapping) {
                                        Offset(
                                            (nextPos.x / 10f).roundToInt() * 10f,
                                            (nextPos.y / 10f).roundToInt() * 10f
                                        )
                                    } else nextPos

                                    val updated = updateSegmentCoordinate(
                                        segments = currentSegments,
                                        segmentIndex = draggedSegmentIndex,
                                        handleType = draggedHandleType!!,
                                        newAbsolutePos = finalPos
                                    )
                                    onUpdateSegments(updated)
                                }
                            },
                            onDragEnd = {
                                draggedSegmentIndex = -1
                                draggedHandleType = null
                            },
                            onDragCancel = {
                                draggedSegmentIndex = -1
                                draggedHandleType = null
                            }
                        )
                    }
            ) {
                // 1. Draw grid background
                val gridStrokeWidth = 0.5.dp.toPx()
                val gridColor = Color(0xFF1E1E26)
                for (valIndex in 50..250 step 50) {
                    val pos = valIndex * scale
                    // Vertical grid line
                    drawLine(gridColor, Offset(pos, 0f), Offset(pos, size.height), gridStrokeWidth)
                    // Horizontal grid line
                    drawLine(gridColor, Offset(0f, pos), Offset(size.width, pos), gridStrokeWidth)
                }

                // 2. Draw compiled SVG path outline
                val composePath = buildComposePath(segments)
                withTransform({
                    scale(scale, scale, pivot = Offset.Zero)
                }) {
                    drawPath(
                        path = composePath,
                        color = Color(0xFF00FF66),
                        style = Stroke(width = 3.dp.toPx() / scale)
                    )
                }

                // 3. Draw dashed control lines connecting handles
                var cur = Offset.Zero
                segments.forEach { seg ->
                    when (seg) {
                        is PathSegment.MoveTo -> {
                            val tx = if (seg.isRelative) cur.x + seg.x else seg.x
                            val ty = if (seg.isRelative) cur.y + seg.y else seg.y
                            cur = Offset(tx, ty)
                        }
                        is PathSegment.LineTo -> {
                            val tx = if (seg.isRelative) cur.x + seg.x else seg.x
                            val ty = if (seg.isRelative) cur.y + seg.y else seg.y
                            cur = Offset(tx, ty)
                        }
                        is PathSegment.HorizontalLineTo -> {
                            val tx = if (seg.isRelative) cur.x + seg.x else seg.x
                            cur = Offset(tx, cur.y)
                        }
                        is PathSegment.VerticalLineTo -> {
                            val ty = if (seg.isRelative) cur.y + seg.y else seg.y
                            cur = Offset(cur.x, ty)
                        }
                        is PathSegment.CubicTo -> {
                            val cx1 = if (seg.isRelative) cur.x + seg.x1 else seg.x1
                            val cy1 = if (seg.isRelative) cur.y + seg.y1 else seg.y1
                            val cx2 = if (seg.isRelative) cur.x + seg.x2 else seg.x2
                            val cy2 = if (seg.isRelative) cur.y + seg.y2 else seg.y2
                            val tx = if (seg.isRelative) cur.x + seg.x else seg.x
                            val ty = if (seg.isRelative) cur.y + seg.y else seg.y

                            // Control line 1: Start point to C1
                            drawLine(
                                color = Color.Gray.copy(alpha = 0.5f),
                                start = cur * scale,
                                end = Offset(cx1, cy1) * scale,
                                strokeWidth = 1.dp.toPx(),
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(5f, 5f), 0f)
                            )
                            // Control line 2: C2 to End point
                            drawLine(
                                color = Color.Gray.copy(alpha = 0.5f),
                                start = Offset(cx2, cy2) * scale,
                                end = Offset(tx, ty) * scale,
                                strokeWidth = 1.dp.toPx(),
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(5f, 5f), 0f)
                            )
                            cur = Offset(tx, ty)
                        }
                        is PathSegment.QuadraticTo -> {
                            val cx1 = if (seg.isRelative) cur.x + seg.x1 else seg.x1
                            val cy1 = if (seg.isRelative) cur.y + seg.y1 else seg.y1
                            val tx = if (seg.isRelative) cur.x + seg.x else seg.x
                            val ty = if (seg.isRelative) cur.y + seg.y else seg.y

                            // Control lines: Start to C1, C1 to End
                            drawLine(
                                color = Color.Gray.copy(alpha = 0.5f),
                                start = cur * scale,
                                end = Offset(cx1, cy1) * scale,
                                strokeWidth = 1.dp.toPx(),
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(5f, 5f), 0f)
                            )
                            drawLine(
                                color = Color.Gray.copy(alpha = 0.5f),
                                start = Offset(cx1, cy1) * scale,
                                end = Offset(tx, ty) * scale,
                                strokeWidth = 1.dp.toPx(),
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(5f, 5f), 0f)
                            )
                            cur = Offset(tx, ty)
                        }
                        is PathSegment.Close -> {}
                    }
                }

                // 4. Draw drag handle points
                dragHandles.forEach { handle ->
                    val isSelected = draggedSegmentIndex == handle.segmentIndex && draggedHandleType == handle.handleType
                    val color = when (handle.handleType) {
                        HandleType.MOVE_END, HandleType.LINE_END, HandleType.HORIZ_END, HandleType.VERT_END -> Color(0xFF3B82F6) // Anchor points (Blue)
                        HandleType.CUBIC_C1, HandleType.CUBIC_C2, HandleType.QUAD_C1 -> Color(0xFFF59E0B) // Control handles (Amber)
                        HandleType.CUBIC_END, HandleType.QUAD_END -> Color(0xFF8B5CF6) // Curve endpoints (Purple)
                    }

                    drawCircle(
                        color = if (isSelected) Color.White else color,
                        radius = (if (isSelected) 7.dp else 5.dp).toPx(),
                        center = handle.position * scale
                    )
                    
                    // Draw outline loop around control handles
                    drawCircle(
                        color = Color.Black,
                        radius = (if (isSelected) 7.dp else 5.dp).toPx(),
                        center = handle.position * scale,
                        style = Stroke(width = 1.dp.toPx())
                    )
                }
            }
        }
    }
}

@Composable
fun SvgAstTab(parsedSegments: List<PathSegment>) {
    Card(
        modifier = Modifier.fillMaxSize(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF18181C)),
        border = BorderStroke(1.dp, Color(0xFF26262D))
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(parsedSegments.mapIndexed { index, seg -> Pair(index, seg) }) { pair ->
                val index = pair.first
                val seg = pair.second
                val name = seg::class.simpleName ?: "Close"
                val color = when (seg) {
                    is PathSegment.MoveTo -> Color(0xFF3B82F6)
                    is PathSegment.LineTo, is PathSegment.HorizontalLineTo, is PathSegment.VerticalLineTo -> Color(0xFF10B981)
                    is PathSegment.CubicTo, is PathSegment.QuadraticTo -> Color(0xFF8B5CF6)
                    is PathSegment.Close -> Color(0xFFEF4444)
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF232329), shape = RoundedCornerShape(4.dp))
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "[$index] $name",
                        color = color,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(0.4f)
                    )
                    Text(
                        text = when (seg) {
                            is PathSegment.MoveTo -> "x:${seg.x.toInt()}, y:${seg.y.toInt()}"
                            is PathSegment.LineTo -> "x:${seg.x.toInt()}, y:${seg.y.toInt()}"
                            is PathSegment.HorizontalLineTo -> "x:${seg.x.toInt()}"
                            is PathSegment.VerticalLineTo -> "y:${seg.y.toInt()}"
                            is PathSegment.CubicTo -> "c1:(${seg.x1.toInt()},${seg.y1.toInt()}) c2:(${seg.x2.toInt()},${seg.y2.toInt()}) end:(${seg.x.toInt()},${seg.y.toInt()})"
                            is PathSegment.QuadraticTo -> "c1:(${seg.x1.toInt()},${seg.y1.toInt()}) end:(${seg.x.toInt()},${seg.y.toInt()})"
                            is PathSegment.Close -> "z"
                        },
                        color = Color.LightGray,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.weight(0.6f)
                    )
                }
            }
        }
    }
}

@Composable
fun SvgLogsTab(logs: List<String>) {
    Card(
        modifier = Modifier.fillMaxSize(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F0F11)),
        border = BorderStroke(1.dp, Color(0xFF26262D))
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(logs) { log ->
                val color = if (log.contains("Warning:")) Color(0xFFFFCC00) else Color(0xFFECEFF1)
                Text(
                    text = log,
                    color = color,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

// SVG Path lexer/parser implementation
fun parseSvgPath(d: String, logs: MutableList<String>): List<PathSegment> {
    val segments = mutableListOf<PathSegment>()

    // Tokenize commands and coordinate numbers
    val tokens = mutableListOf<String>()
    var i = 0
    val len = d.length
    while (i < len) {
        val c = d[i]
        if (c.isLetter()) {
            tokens.add(c.toString())
            i++
        } else if (c.isDigit() || c == '.' || c == '-') {
            val start = i
            i++
            while (i < len && (d[i].isDigit() || d[i] == '.' || d[i] == 'e' || d[i] == 'E' || d[i] == '-' || d[i] == '+')) {
                i++
            }
            tokens.add(d.substring(start, i))
        } else {
            i++ // skip whitespace, commas, etc.
        }
    }

    var idx = 0
    var lastCommand = ' '
    while (idx < tokens.size) {
        val token = tokens[idx]
        val isCmd = token.length == 1 && token[0].isLetter()
        val cmd = if (isCmd) {
            idx++
            token[0]
        } else {
            if (lastCommand == ' ') {
                logs.add("Warning: Ignored floating coordinate token '$token' without active command context.")
                break
            }
            lastCommand
        }

        try {
            when (cmd) {
                'M', 'm' -> {
                    val x = tokens[idx++].toFloat()
                    val y = tokens[idx++].toFloat()
                    segments.add(PathSegment.MoveTo(x, y, cmd == 'm'))
                    lastCommand = if (cmd == 'M') 'L' else 'l'
                }
                'L', 'l' -> {
                    val x = tokens[idx++].toFloat()
                    val y = tokens[idx++].toFloat()
                    segments.add(PathSegment.LineTo(x, y, cmd == 'l'))
                    lastCommand = cmd
                }
                'H', 'h' -> {
                    val x = tokens[idx++].toFloat()
                    segments.add(PathSegment.HorizontalLineTo(x, cmd == 'h'))
                    lastCommand = cmd
                }
                'V', 'v' -> {
                    val y = tokens[idx++].toFloat()
                    segments.add(PathSegment.VerticalLineTo(y, cmd == 'v'))
                    lastCommand = cmd
                }
                'C', 'c' -> {
                    val x1 = tokens[idx++].toFloat()
                    val y1 = tokens[idx++].toFloat()
                    val x2 = tokens[idx++].toFloat()
                    val y2 = tokens[idx++].toFloat()
                    val x = tokens[idx++].toFloat()
                    val y = tokens[idx++].toFloat()
                    segments.add(PathSegment.CubicTo(x1, y1, x2, y2, x, y, cmd == 'c'))
                    lastCommand = cmd
                }
                'Q', 'q' -> {
                    val x1 = tokens[idx++].toFloat()
                    val y1 = tokens[idx++].toFloat()
                    val x = tokens[idx++].toFloat()
                    val y = tokens[idx++].toFloat()
                    segments.add(PathSegment.QuadraticTo(x1, y1, x, y, cmd == 'q'))
                    lastCommand = cmd
                }
                'Z', 'z' -> {
                    segments.add(PathSegment.Close)
                    lastCommand = ' '
                }
                else -> {
                    logs.add("Warning: Unsupported SVG path command '$cmd'.")
                    break
                }
            }
        } catch (e: Exception) {
            logs.add("Error parsing arguments for command '$cmd' at token index $idx: ${e.message}")
            break
        }
    }

    return segments
}

// AST to SVG Path string serializer
fun serializeSvgPath(segments: List<PathSegment>): String {
    val sb = StringBuilder()
    segments.forEachIndexed { idx, seg ->
        if (idx > 0) sb.append(" ")
        when (seg) {
            is PathSegment.MoveTo -> sb.append(if (seg.isRelative) "m" else "M").append(" ").append(seg.x.toInt()).append(" ").append(seg.y.toInt())
            is PathSegment.LineTo -> sb.append(if (seg.isRelative) "l" else "L").append(" ").append(seg.x.toInt()).append(" ").append(seg.y.toInt())
            is PathSegment.HorizontalLineTo -> sb.append(if (seg.isRelative) "h" else "H").append(" ").append(seg.x.toInt())
            is PathSegment.VerticalLineTo -> sb.append(if (seg.isRelative) "v" else "V").append(" ").append(seg.y.toInt())
            is PathSegment.CubicTo -> sb.append(if (seg.isRelative) "c" else "C").append(" ")
                .append(seg.x1.toInt()).append(" ").append(seg.y1.toInt()).append(", ")
                .append(seg.x2.toInt()).append(" ").append(seg.y2.toInt()).append(", ")
                .append(seg.x.toInt()).append(" ").append(seg.y.toInt())
            is PathSegment.QuadraticTo -> sb.append(if (seg.isRelative) "q" else "Q").append(" ")
                .append(seg.x1.toInt()).append(" ").append(seg.y1.toInt()).append(", ")
                .append(seg.x.toInt()).append(" ").append(seg.y.toInt())
            is PathSegment.Close -> sb.append("Z")
        }
    }
    return sb.toString()
}

// Helper calculations to find preceding coordinate state
fun getPreviousCursorPosition(segments: List<PathSegment>, segmentIndex: Int): Offset {
    var cur = Offset.Zero
    var startPoint = Offset.Zero
    for (i in 0 until segmentIndex) {
        when (val seg = segments[i]) {
            is PathSegment.MoveTo -> {
                val tx = if (seg.isRelative) cur.x + seg.x else seg.x
                val ty = if (seg.isRelative) cur.y + seg.y else seg.y
                cur = Offset(tx, ty)
                startPoint = cur
            }
            is PathSegment.LineTo -> {
                val tx = if (seg.isRelative) cur.x + seg.x else seg.x
                val ty = if (seg.isRelative) cur.y + seg.y else seg.y
                cur = Offset(tx, ty)
            }
            is PathSegment.HorizontalLineTo -> {
                val tx = if (seg.isRelative) cur.x + seg.x else seg.x
                cur = Offset(tx, cur.y)
            }
            is PathSegment.VerticalLineTo -> {
                val ty = if (seg.isRelative) cur.y + seg.y else seg.y
                cur = Offset(cur.x, ty)
            }
            is PathSegment.CubicTo -> {
                val tx = if (seg.isRelative) cur.x + seg.x else seg.x
                val ty = if (seg.isRelative) cur.y + seg.y else seg.y
                cur = Offset(tx, ty)
            }
            is PathSegment.QuadraticTo -> {
                val tx = if (seg.isRelative) cur.x + seg.x else seg.x
                val ty = if (seg.isRelative) cur.y + seg.y else seg.y
                cur = Offset(tx, ty)
            }
            is PathSegment.Close -> {
                cur = startPoint
            }
        }
    }
    return cur
}

// Update coordinate mappings on handles drags
fun updateSegmentCoordinate(
    segments: List<PathSegment>,
    segmentIndex: Int,
    handleType: HandleType,
    newAbsolutePos: Offset
): List<PathSegment> {
    val prev = getPreviousCursorPosition(segments, segmentIndex)
    return segments.mapIndexed { idx, seg ->
        if (idx == segmentIndex) {
            when (seg) {
                is PathSegment.MoveTo -> {
                    val nx = if (seg.isRelative) newAbsolutePos.x - prev.x else newAbsolutePos.x
                    val ny = if (seg.isRelative) newAbsolutePos.y - prev.y else newAbsolutePos.y
                    seg.copy(x = nx, y = ny)
                }
                is PathSegment.LineTo -> {
                    val nx = if (seg.isRelative) newAbsolutePos.x - prev.x else newAbsolutePos.x
                    val ny = if (seg.isRelative) newAbsolutePos.y - prev.y else newAbsolutePos.y
                    seg.copy(x = nx, y = ny)
                }
                is PathSegment.HorizontalLineTo -> {
                    val nx = if (seg.isRelative) newAbsolutePos.x - prev.x else newAbsolutePos.x
                    seg.copy(x = nx)
                }
                is PathSegment.VerticalLineTo -> {
                    val ny = if (seg.isRelative) newAbsolutePos.y - prev.y else newAbsolutePos.y
                    seg.copy(y = ny)
                }
                is PathSegment.CubicTo -> {
                    when (handleType) {
                        HandleType.CUBIC_C1 -> {
                            val nx1 = if (seg.isRelative) newAbsolutePos.x - prev.x else newAbsolutePos.x
                            val ny1 = if (seg.isRelative) newAbsolutePos.y - prev.y else newAbsolutePos.y
                            seg.copy(x1 = nx1, y1 = ny1)
                        }
                        HandleType.CUBIC_C2 -> {
                            val nx2 = if (seg.isRelative) newAbsolutePos.x - prev.x else newAbsolutePos.x
                            val ny2 = if (seg.isRelative) newAbsolutePos.y - prev.y else newAbsolutePos.y
                            seg.copy(x2 = nx2, y2 = ny2)
                        }
                        HandleType.CUBIC_END -> {
                            val nx = if (seg.isRelative) newAbsolutePos.x - prev.x else newAbsolutePos.x
                            val ny = if (seg.isRelative) newAbsolutePos.y - prev.y else newAbsolutePos.y
                            seg.copy(x = nx, y = ny)
                        }
                        else -> seg
                    }
                }
                is PathSegment.QuadraticTo -> {
                    when (handleType) {
                        HandleType.QUAD_C1 -> {
                            val nx1 = if (seg.isRelative) newAbsolutePos.x - prev.x else newAbsolutePos.x
                            val ny1 = if (seg.isRelative) newAbsolutePos.y - prev.y else newAbsolutePos.y
                            seg.copy(x1 = nx1, y1 = ny1)
                        }
                        HandleType.QUAD_END -> {
                            val nx = if (seg.isRelative) newAbsolutePos.x - prev.x else newAbsolutePos.x
                            val ny = if (seg.isRelative) newAbsolutePos.y - prev.y else newAbsolutePos.y
                            seg.copy(x = nx, y = ny)
                        }
                        else -> seg
                    }
                }
                is PathSegment.Close -> seg
            }
        } else {
            seg
        }
    }
}

fun calculateDragHandles(segments: List<PathSegment>): List<DragHandle> {
    val handles = mutableListOf<DragHandle>()
    var cur = Offset.Zero
    var startPoint = Offset.Zero

    segments.forEachIndexed { index, seg ->
        when (seg) {
            is PathSegment.MoveTo -> {
                val tx = if (seg.isRelative) cur.x + seg.x else seg.x
                val ty = if (seg.isRelative) cur.y + seg.y else seg.y
                val pos = Offset(tx, ty)
                handles.add(DragHandle(index, HandleType.MOVE_END, pos))
                cur = pos
                startPoint = cur
            }
            is PathSegment.LineTo -> {
                val tx = if (seg.isRelative) cur.x + seg.x else seg.x
                val ty = if (seg.isRelative) cur.y + seg.y else seg.y
                val pos = Offset(tx, ty)
                handles.add(DragHandle(index, HandleType.LINE_END, pos))
                cur = pos
            }
            is PathSegment.HorizontalLineTo -> {
                val tx = if (seg.isRelative) cur.x + seg.x else seg.x
                val pos = Offset(tx, cur.y)
                handles.add(DragHandle(index, HandleType.HORIZ_END, pos))
                cur = pos
            }
            is PathSegment.VerticalLineTo -> {
                val ty = if (seg.isRelative) cur.y + seg.y else seg.y
                val pos = Offset(cur.x, ty)
                handles.add(DragHandle(index, HandleType.VERT_END, pos))
                cur = pos
            }
            is PathSegment.CubicTo -> {
                val cx1 = if (seg.isRelative) cur.x + seg.x1 else seg.x1
                val cy1 = if (seg.isRelative) cur.y + seg.y1 else seg.y1
                val cx2 = if (seg.isRelative) cur.x + seg.x2 else seg.x2
                val cy2 = if (seg.isRelative) cur.y + seg.y2 else seg.y2
                val tx = if (seg.isRelative) cur.x + seg.x else seg.x
                val ty = if (seg.isRelative) cur.y + seg.y else seg.y

                handles.add(DragHandle(index, HandleType.CUBIC_C1, Offset(cx1, cy1)))
                handles.add(DragHandle(index, HandleType.CUBIC_C2, Offset(cx2, cy2)))
                handles.add(DragHandle(index, HandleType.CUBIC_END, Offset(tx, ty)))
                cur = Offset(tx, ty)
            }
            is PathSegment.QuadraticTo -> {
                val cx1 = if (seg.isRelative) cur.x + seg.x1 else seg.x1
                val cy1 = if (seg.isRelative) cur.y + seg.y1 else seg.y1
                val tx = if (seg.isRelative) cur.x + seg.x else seg.x
                val ty = if (seg.isRelative) cur.y + seg.y else seg.y

                handles.add(DragHandle(index, HandleType.QUAD_C1, Offset(cx1, cy1)))
                handles.add(DragHandle(index, HandleType.QUAD_END, Offset(tx, ty)))
                cur = Offset(tx, ty)
            }
            is PathSegment.Close -> {
                cur = startPoint
            }
        }
    }
    return handles
}

fun buildComposePath(segments: List<PathSegment>): Path {
    val path = Path()
    var cur = Offset.Zero
    var startPoint = Offset.Zero
    segments.forEach { seg ->
        when (seg) {
            is PathSegment.MoveTo -> {
                val tx = if (seg.isRelative) cur.x + seg.x else seg.x
                val ty = if (seg.isRelative) cur.y + seg.y else seg.y
                path.moveTo(tx, ty)
                cur = Offset(tx, ty)
                startPoint = cur
            }
            is PathSegment.LineTo -> {
                val tx = if (seg.isRelative) cur.x + seg.x else seg.x
                val ty = if (seg.isRelative) cur.y + seg.y else seg.y
                path.lineTo(tx, ty)
                cur = Offset(tx, ty)
            }
            is PathSegment.HorizontalLineTo -> {
                val tx = if (seg.isRelative) cur.x + seg.x else seg.x
                path.lineTo(tx, cur.y)
                cur = Offset(tx, cur.y)
            }
            is PathSegment.VerticalLineTo -> {
                val ty = if (seg.isRelative) cur.y + seg.y else seg.y
                path.lineTo(cur.x, ty)
                cur = Offset(cur.x, ty)
            }
            is PathSegment.CubicTo -> {
                val cx1 = if (seg.isRelative) cur.x + seg.x1 else seg.x1
                val cy1 = if (seg.isRelative) cur.y + seg.y1 else seg.y1
                val cx2 = if (seg.isRelative) cur.x + seg.x2 else seg.x2
                val cy2 = if (seg.isRelative) cur.y + seg.y2 else seg.y2
                val tx = if (seg.isRelative) cur.x + seg.x else seg.x
                val ty = if (seg.isRelative) cur.y + seg.y else seg.y
                path.cubicTo(cx1, cy1, cx2, cy2, tx, ty)
                cur = Offset(tx, ty)
            }
            is PathSegment.QuadraticTo -> {
                val cx1 = if (seg.isRelative) cur.x + seg.x1 else seg.x1
                val cy1 = if (seg.isRelative) cur.y + seg.y1 else seg.y1
                val tx = if (seg.isRelative) cur.x + seg.x else seg.x
                val ty = if (seg.isRelative) cur.y + seg.y else seg.y
                path.quadraticTo(cx1, cy1, tx, ty)
                cur = Offset(tx, ty)
            }
            is PathSegment.Close -> {
                path.close()
                cur = startPoint
            }
        }
    }
    return path
}
private fun Float.roundToInt(): Int = (this + 0.5f).toInt()
