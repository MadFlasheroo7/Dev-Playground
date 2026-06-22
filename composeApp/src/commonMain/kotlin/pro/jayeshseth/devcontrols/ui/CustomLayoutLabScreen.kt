package pro.jayeshseth.devcontrols.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

private const val d = "$"

enum class LayoutLabTab { PLAYGROUND, TRACE, CODE }
enum class LayoutType { RADIAL, FLOW, STAGGERED, ADAPTIVE_FIT, VARIABLE_TAB }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomLayoutLabScreen(onBack: () -> Unit) {
    var activeTab by remember { mutableStateOf(LayoutLabTab.PLAYGROUND) }
    var selectedLayout by remember { mutableStateOf(LayoutType.RADIAL) }

    // Colors list for visual blocks
    val colors = remember {
        listOf(
            Color(0xFFFF5F6D), Color(0xFF2193B0), Color(0xFF92FE9D), Color(0xFFFFC371),
            Color(0xFF7F00FF), Color(0xFFE100FF), Color(0xFF26D0CE), Color(0xFFFC466B),
            Color(0xFF3F5EFB), Color(0xFFFF8008), Color(0xFF00C9FF), Color(0xFFB24592)
        )
    }

    // Radial Layout sandbox state
    var radialCount by remember { mutableFloatStateOf(8f) }
    var radialRadiusRatio by remember { mutableFloatStateOf(0.7f) }
    var radialRotationSpeed by remember { mutableFloatStateOf(30f) }

    // Flow Row sandbox state
    var flowHSpacing by remember { mutableFloatStateOf(8f) }
    var flowVSpacing by remember { mutableFloatStateOf(8f) }
    var flowWidthFraction by remember { mutableFloatStateOf(1f) }

    // Staggered Layout sandbox state
    var staggeredColumns by remember { mutableFloatStateOf(3f) }
    var staggeredCount by remember { mutableFloatStateOf(9f) }

    // Adaptive Fit Row state
    var adaptiveWidthFraction by remember { mutableFloatStateOf(0.85f) }
    var adaptiveSpacing by remember { mutableFloatStateOf(8f) }

    // Variable Tab Row state
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabTitles = remember { listOf("Home", "Labs Playground", "My Profile Details", "FAQ") }

    // Animation calculation for radial layout
    val infiniteTransition = rememberInfiniteTransition(label = "radial_rotation")
    val baseRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )
    val activeRotation = baseRotation * (radialRotationSpeed / 30f)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Custom Layouts Learn Lab") },
                navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }
            )
        },
        modifier = Modifier.fillMaxSize()
    ) { inner ->
        Column(
            modifier = Modifier
                .padding(inner)
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // Main Top Tab Selector
            TabRow(
                selectedTabIndex = activeTab.ordinal,
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
            ) {
                LayoutLabTab.values().forEach { tab ->
                    Tab(
                        selected = activeTab == tab,
                        onClick = { activeTab = tab },
                        text = {
                            val label = when (tab) {
                                LayoutLabTab.PLAYGROUND -> "Playground"
                                LayoutLabTab.TRACE -> "Execution Trace"
                                LayoutLabTab.CODE -> "Kotlin Code"
                            }
                            Text(label, fontWeight = FontWeight.Bold)
                        }
                    )
                }
            }

            // Layout Category Horizontal Selector
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                LayoutType.values().forEach { type ->
                    val label = when (type) {
                        LayoutType.RADIAL -> "Radial (Layout)"
                        LayoutType.FLOW -> "Flow Row (Layout)"
                        LayoutType.STAGGERED -> "Staggered (Layout)"
                        LayoutType.ADAPTIVE_FIT -> "Adaptive Fit (Subcompose)"
                        LayoutType.VARIABLE_TAB -> "Variable Tabs (Subcompose)"
                    }
                    FilterChip(
                        selected = selectedLayout == type,
                        onClick = { selectedLayout = type },
                        label = { Text(label) }
                    )
                }
            }

            when (activeTab) {
                LayoutLabTab.PLAYGROUND -> {
                    // 1. The Interactive Playground View
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(bottom = 12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            when (selectedLayout) {
                                LayoutType.RADIAL -> {
                                    RadialLayout(
                                        rotationDeg = activeRotation,
                                        radiusRatio = radialRadiusRatio,
                                        modifier = Modifier.fillMaxSize()
                                    ) {
                                        repeat(radialCount.toInt()) { i ->
                                            Box(
                                                modifier = Modifier
                                                    .size(48.dp)
                                                    .clip(RoundedCornerShape(12.dp))
                                                    .background(colors[i % colors.size]),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = "${i + 1}",
                                                    color = Color.White,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                }
                                LayoutType.FLOW -> {
                                    // Simulated border representing constraints limit
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth(flowWidthFraction)
                                            .border(1.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                            .padding(8.dp)
                                    ) {
                                        CustomFlowRow(
                                            horizontalSpacing = flowHSpacing.dp,
                                            verticalSpacing = flowVSpacing.dp,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            val chips = listOf("Kotlin", "Compose", "Android", "Multiplatform", "AGSL", "Layout", "Subcompose", "Measure", "Place", "Constraints", "Canvas", "State")
                                            chips.forEachIndexed { i, text ->
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(16.dp))
                                                        .background(colors[i % colors.size].copy(alpha = 0.85f))
                                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                                ) {
                                                    Text(text, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                                }
                                            }
                                        }
                                    }
                                }
                                LayoutType.STAGGERED -> {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .verticalScroll(rememberScrollState())
                                    ) {
                                        StaggeredLayout(
                                            columns = staggeredColumns.toInt().coerceAtLeast(1),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            repeat(staggeredCount.toInt()) { i ->
                                                val heightMultiplier = 60 + (i * 37) % 80
                                                Box(
                                                    modifier = Modifier
                                                        .padding(4.dp)
                                                        .fillMaxWidth()
                                                        .height(heightMultiplier.dp)
                                                        .clip(RoundedCornerShape(12.dp))
                                                        .background(colors[i % colors.size]),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        text = "Item ${i + 1}\n(${heightMultiplier}dp)",
                                                        color = Color.White,
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                                LayoutType.ADAPTIVE_FIT -> {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center,
                                        modifier = Modifier.fillMaxSize()
                                    ) {
                                        Text(
                                            text = "Squeeze/expand container width slider below to test fit",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(bottom = 8.dp)
                                        )
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                                                .padding(12.dp),
                                            contentAlignment = Alignment.CenterStart
                                        ) {
                                            val items = remember {
                                                listOf("Search", "Bookmarks", "Downloads", "History", "Settings", "Profile & Info", "Security Logs", "Cloud sync")
                                            }
                                            AdaptiveFitRow(
                                                maxWidthFraction = adaptiveWidthFraction,
                                                spacing = adaptiveSpacing.dp,
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                items.forEachIndexed { i, text ->
                                                    Box(
                                                        modifier = Modifier
                                                            .clip(RoundedCornerShape(8.dp))
                                                            .background(colors[i % colors.size])
                                                            .padding(horizontal = 12.dp, vertical = 8.dp)
                                                    ) {
                                                        Text(text, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                                LayoutType.VARIABLE_TAB -> {
                                    Column(
                                        modifier = Modifier.fillMaxSize(),
                                        verticalArrangement = Arrangement.Center,
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        VariableWidthTabRow(
                                            selectedIndex = selectedTabIndex,
                                            tabs = tabTitles,
                                            onTabSelected = { selectedTabIndex = it },
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                        Spacer(Modifier.height(32.dp))
                                        Text(
                                            text = "Active Tab: \"${tabTitles[selectedTabIndex]}\"",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "Indicator subcomposes itself to match tab width exactly.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Sandbox Controls Panel
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("Sandbox Controls", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 6.dp))

                            when (selectedLayout) {
                                LayoutType.RADIAL -> {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("Count (${radialCount.toInt()}): ", modifier = Modifier.width(90.dp), style = MaterialTheme.typography.bodySmall)
                                        Slider(value = radialCount, onValueChange = { radialCount = it }, valueRange = 3f..12f, modifier = Modifier.weight(1f))
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("Radius: ", modifier = Modifier.width(90.dp), style = MaterialTheme.typography.bodySmall)
                                        Slider(value = radialRadiusRatio, onValueChange = { radialRadiusRatio = it }, valueRange = 0.3f..0.9f, modifier = Modifier.weight(1f))
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("Rotation Speed: ", modifier = Modifier.width(90.dp), style = MaterialTheme.typography.bodySmall)
                                        Slider(value = radialRotationSpeed, onValueChange = { radialRotationSpeed = it }, valueRange = 0f..100f, modifier = Modifier.weight(1f))
                                    }
                                }
                                LayoutType.FLOW -> {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("H Spacing: ", modifier = Modifier.width(90.dp), style = MaterialTheme.typography.bodySmall)
                                        Slider(value = flowHSpacing, onValueChange = { flowHSpacing = it }, valueRange = 0f..24f, modifier = Modifier.weight(1f))
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("V Spacing: ", modifier = Modifier.width(90.dp), style = MaterialTheme.typography.bodySmall)
                                        Slider(value = flowVSpacing, onValueChange = { flowVSpacing = it }, valueRange = 0f..24f, modifier = Modifier.weight(1f))
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("Width Limit: ", modifier = Modifier.width(90.dp), style = MaterialTheme.typography.bodySmall)
                                        Slider(value = flowWidthFraction, onValueChange = { flowWidthFraction = it }, valueRange = 0.3f..1f, modifier = Modifier.weight(1f))
                                    }
                                }
                                LayoutType.STAGGERED -> {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("Columns: ", modifier = Modifier.width(90.dp), style = MaterialTheme.typography.bodySmall)
                                        Slider(value = staggeredColumns, onValueChange = { staggeredColumns = it }, valueRange = 1f..5f, steps = 3, modifier = Modifier.weight(1f))
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("Count: ", modifier = Modifier.width(90.dp), style = MaterialTheme.typography.bodySmall)
                                        Slider(value = staggeredCount, onValueChange = { staggeredCount = it }, valueRange = 3f..20f, modifier = Modifier.weight(1f))
                                    }
                                }
                                LayoutType.ADAPTIVE_FIT -> {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("Max Width: ", modifier = Modifier.width(90.dp), style = MaterialTheme.typography.bodySmall)
                                        Slider(value = adaptiveWidthFraction, onValueChange = { adaptiveWidthFraction = it }, valueRange = 0.2f..1f, modifier = Modifier.weight(1f))
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("Spacing: ", modifier = Modifier.width(90.dp), style = MaterialTheme.typography.bodySmall)
                                        Slider(value = adaptiveSpacing, onValueChange = { adaptiveSpacing = it }, valueRange = 0f..20f, modifier = Modifier.weight(1f))
                                    }
                                }
                                LayoutType.VARIABLE_TAB -> {
                                    Text(
                                        text = "Select different tabs in the playground view to watch the indicator adapt its placement constraints dynamically during the measurement pass.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
                LayoutLabTab.TRACE -> {
                    // 2. The Execution Trace Debugger Tab
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp)
                                .verticalScroll(rememberScrollState())
                        ) {
                            Text(
                                text = "Live Layout Execution Console",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "A simulated breakdown of the measurement & placement calculations taking place based on your playground parameters.",
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )
                            HorizontalDivider(modifier = Modifier.padding(bottom = 12.dp))

                            TraceConsoleContent(
                                type = selectedLayout,
                                radialCount = radialCount.toInt(),
                                radialRadius = radialRadiusRatio,
                                flowH = flowHSpacing.toInt(),
                                flowV = flowVSpacing.toInt(),
                                flowFraction = flowWidthFraction,
                                staggeredCols = staggeredColumns.toInt(),
                                staggeredN = staggeredCount.toInt(),
                                adaptiveFraction = adaptiveWidthFraction,
                                adaptiveS = adaptiveSpacing.toInt(),
                                tabIndex = selectedTabIndex,
                                tabTitles = tabTitles
                            )
                        }
                    }
                }
                LayoutLabTab.CODE -> {
                    // 3. Kotlin Code & Spec Snippets Tab
                    val codeContent = when (selectedLayout) {
                        LayoutType.RADIAL -> radialLayoutCode
                        LayoutType.FLOW -> flowLayoutCode
                        LayoutType.STAGGERED -> staggeredLayoutCode
                        LayoutType.ADAPTIVE_FIT -> adaptiveFitCode
                        LayoutType.VARIABLE_TAB -> variableTabCode
                    }
                    val explanation = when (selectedLayout) {
                        LayoutType.RADIAL -> "Radial layouts measure all items with loose/unbounded constraints and then place them on the perimeter of a circle using basic trigonometry offsets (cos, sin)."
                        LayoutType.FLOW -> "Flow Row layouts wrap items to the next row when the cumulative width of placed elements plus horizontal spacing exceeds constraints.maxWidth."
                        LayoutType.STAGGERED -> "Masonry Staggered grids measure children using a fixed column width. Each child is placed dynamically in the column that currently has the shortest height."
                        LayoutType.ADAPTIVE_FIT -> "SubcomposeLayout allows composing items on demand during the measurement pass. If items overflow constraints.maxWidth, we stop composing them and replace them with a '+N more' badge."
                        LayoutType.VARIABLE_TAB -> "Variable Tab rows measure individual tabs first, and then subcompose the indicator using the selected tab's exact measured width to render it precisely."
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f))
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = explanation,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }

                        HighlightedCodeBlock(code = codeContent)
                    }
                }
            }
        }
    }
}

@Composable
private fun TraceConsoleContent(
    type: LayoutType,
    radialCount: Int,
    radialRadius: Float,
    flowH: Int,
    flowV: Int,
    flowFraction: Float,
    staggeredCols: Int,
    staggeredN: Int,
    adaptiveFraction: Float,
    adaptiveS: Int,
    tabIndex: Int,
    tabTitles: List<String>
) {
    val monospace = FontFamily.Monospace
    val bold = FontWeight.Bold

    when (type) {
        LayoutType.RADIAL -> {
            Text("[1] incoming constraints: maxWidth=1080px, maxHeight=1080px", fontFamily = monospace, fontSize = 12.sp)
            Text("[2] measuring $radialCount children with childConstraints = Constraints(min=0, max=135px)", fontFamily = monospace, fontSize = 12.sp)
            Text("[3] calculated radius: (min(1080, 1080) / 2) * $radialRadius = ${(540 * radialRadius).toInt()}px", fontFamily = monospace, fontSize = 12.sp)
            Text("[4] placing children around center (cx=540px, cy=540px):", fontFamily = monospace, fontSize = 12.sp, fontWeight = bold, modifier = Modifier.padding(top = 6.dp))
            repeat(radialCount) { i ->
                val ang = i * 360f / radialCount
                val rad = (ang * kotlin.math.PI / 180.0)
                val x = 540 + (540 * radialRadius * cos(rad)).toInt() - 24
                val y = 540 + (540 * radialRadius * sin(rad)).toInt() - 24
                Text("    -> Child $i: angle=${ang.toInt()}° | x=$x, y=$y", fontFamily = monospace, fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
            }
            Text("[5] declaring layout size: 1080px x 1080px", fontFamily = monospace, fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp))
        }
        LayoutType.FLOW -> {
            val maxW = (1080 * flowFraction).toInt()
            Text("[1] incoming constraints: maxWidth=${maxW}px, maxHeight=Infinity", fontFamily = monospace, fontSize = 12.sp)
            Text("[2] item sizes (widths): 140px, 170px, 150px, 200px, 120px, 130px, 190px, 160px", fontFamily = monospace, fontSize = 12.sp)
            Text("[3] layout mapping and wrapping pass:", fontFamily = monospace, fontSize = 12.sp, fontWeight = bold, modifier = Modifier.padding(top = 6.dp))
            
            val widths = listOf(140, 170, 150, 200, 120, 130, 190, 160)
            var currentW = 0
            var rowIdx = 1
            var rowItems = mutableListOf<String>()
            widths.forEachIndexed { i, w ->
                val needed = if (rowItems.isEmpty()) w else w + flowH
                if (currentW + needed <= maxW) {
                    rowItems.add("Item$i(${w}px)")
                    currentW += needed
                } else {
                    Text("    Row $rowIdx: [${rowItems.joinToString(", ")}] width=${currentW}px", fontFamily = monospace, fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                    rowIdx++
                    rowItems = mutableListOf("Item$i(${w}px)")
                    currentW = w
                }
            }
            if (rowItems.isNotEmpty()) {
                Text("    Row $rowIdx: [${rowItems.joinToString(", ")}] width=${currentW}px", fontFamily = monospace, fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
            }
            Text("[4] vertical height: ${rowIdx * 80 + (rowIdx - 1) * flowV}px", fontFamily = monospace, fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp))
        }
        LayoutType.STAGGERED -> {
            Text("[1] incoming constraints: maxWidth=1080px", fontFamily = monospace, fontSize = 12.sp)
            Text("[2] column width calculation: 1080px / $staggeredCols = ${1080 / staggeredCols}px", fontFamily = monospace, fontSize = 12.sp)
            Text("[3] layout items placement queue:", fontFamily = monospace, fontSize = 12.sp, fontWeight = bold, modifier = Modifier.padding(top = 6.dp))
            
            val colHeights = IntArray(staggeredCols)
            repeat(staggeredN) { i ->
                val col = colHeights.indices.minByOrNull { colHeights[it] } ?: 0
                val h = 120 + (i * 74) % 180
                val x = col * (1080 / staggeredCols)
                val y = colHeights[col]
                colHeights[col] += h
                Text("    -> Item $i: placed in Col $col at ($x, $y) | itemHeight=${h}px", fontFamily = monospace, fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
            }
            Text("[4] column final heights: [${colHeights.joinToString(", ")}]", fontFamily = monospace, fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp))
            Text("[5] parent layout final height: ${colHeights.maxOrNull() ?: 0}px", fontFamily = monospace, fontSize = 12.sp)
        }
        LayoutType.ADAPTIVE_FIT -> {
            val maxW = (1080 * adaptiveFraction).toInt()
            Text("[1] incoming constraints: maxWidth=1080px (simulated active constraint: ${maxW}px)", fontFamily = monospace, fontSize = 12.sp)
            Text("[2] subcomposition starting during measurement...", fontFamily = monospace, fontSize = 12.sp, fontWeight = bold, color = MaterialTheme.colorScheme.secondary)
            
            val items = listOf("Search", "Bookmarks", "Downloads", "History", "Settings", "Profile & Info", "Security Logs", "Cloud sync")
            val itemWidths = listOf(140, 180, 170, 150, 160, 210, 220, 170)
            var currentW = 0
            var fitCount = 0
            for (i in itemWidths.indices) {
                val req = itemWidths[i] + (if (i > 0) adaptiveS else 0)
                if (currentW + req <= maxW) {
                    currentW += req
                    fitCount++
                    Text("    -> Subcomposed item '$i': \"${items[i]}\" (width=${itemWidths[i]}px) -> Fits!", fontFamily = monospace, fontSize = 11.sp)
                } else {
                    val remaining = items.size - fitCount
                    Text("    -> Subcomposed item '$i': \"${items[i]}\" -> Exceeds bounds. Stopping composition.", fontFamily = monospace, fontSize = 11.sp, color = Color.Red)
                    Text("    -> Subcomposing indicator badge: '+${remaining} More'...", fontFamily = monospace, fontSize = 11.sp, fontWeight = bold)
                    break
                }
            }
            Text("[3] placing final selected placeables with width bounds.", fontFamily = monospace, fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp))
        }
        LayoutType.VARIABLE_TAB -> {
            Text("[1] variable titles: [${tabTitles.joinToString(", ")}]", fontFamily = monospace, fontSize = 12.sp)
            Text("[2] measuring tabs independently...", fontFamily = monospace, fontSize = 12.sp)
            val widths = listOf(100, 220, 250, 80)
            widths.forEachIndexed { i, w ->
                Text("    -> Tab $i (\"${tabTitles[i]}\") measured: width=${w}px", fontFamily = monospace, fontSize = 11.sp)
            }
            val activeTabW = widths[tabIndex]
            Text("[3] subcomposing slide indicator with constraints matching active tab $tabIndex...", fontFamily = monospace, fontSize = 12.sp, fontWeight = bold, color = MaterialTheme.colorScheme.secondary, modifier = Modifier.padding(top = 6.dp))
            Text("    -> Indicator Constraints set to: minWidth=${activeTabW}px, maxWidth=${activeTabW}px", fontFamily = monospace, fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
            Text("[4] placing components: indicator shifted to index $tabIndex offset.", fontFamily = monospace, fontSize = 12.sp)
        }
    }
}

// -------------------------------------------------------------------------------------
// Custom Layout Implementations
// -------------------------------------------------------------------------------------

@Composable
private fun RadialLayout(
    rotationDeg: Float,
    radiusRatio: Float,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Layout(content = content, modifier = modifier) { measurables, constraints ->
        val placeables = measurables.map { it.measure(constraints.copy(minWidth = 0, minHeight = 0)) }
        val w = if (constraints.hasBoundedWidth) constraints.maxWidth else 1000
        val h = if (constraints.hasBoundedHeight) constraints.maxHeight else 1000
        val radius = min(w, h) / 2f * radiusRatio
        layout(w, h) {
            val cx = w / 2f
            val cy = h / 2f
            val count = placeables.size
            placeables.forEachIndexed { i, p ->
                val ang = (((rotationDeg + i * 360f / count).toDouble()) * kotlin.math.PI / 180.0)
                val x = cx + (radius * cos(ang)).toFloat() - p.width / 2f
                val y = cy + (radius * sin(ang)).toFloat() - p.height / 2f
                p.place(x.toInt(), y.toInt())
            }
        }
    }
}

@Composable
private fun CustomFlowRow(
    horizontalSpacing: Dp,
    verticalSpacing: Dp,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Layout(content = content, modifier = modifier) { measurables, constraints ->
        val hSpacingPx = horizontalSpacing.roundToPx()
        val vSpacingPx = verticalSpacing.roundToPx()

        val placeables = measurables.map { it.measure(constraints.copy(minWidth = 0, minHeight = 0)) }

        val rows = mutableListOf<List<Placeable>>()
        var currentRow = mutableListOf<Placeable>()
        var currentRowWidth = 0

        placeables.forEach { placeable ->
            val requiredWidth = if (currentRow.isEmpty()) placeable.width else placeable.width + hSpacingPx
            if (currentRowWidth + requiredWidth <= constraints.maxWidth) {
                currentRow.add(placeable)
                currentRowWidth += requiredWidth
            } else {
                rows.add(currentRow)
                currentRow = mutableListOf(placeable)
                currentRowWidth = placeable.width
            }
        }
        if (currentRow.isNotEmpty()) {
            rows.add(currentRow)
        }

        val layoutWidth = constraints.maxWidth
        val layoutHeight = if (rows.isEmpty()) 0 else {
            rows.sumOf { row -> row.maxOf { it.height } } + (rows.size - 1) * vSpacingPx
        }

        layout(layoutWidth, layoutHeight) {
            var currentY = 0
            rows.forEach { row ->
                val rowHeight = row.maxOf { it.height }
                var currentX = 0
                row.forEach { placeable ->
                    val yOffset = currentY + (rowHeight - placeable.height) / 2
                    placeable.place(currentX, yOffset)
                    currentX += placeable.width + hSpacingPx
                }
                currentY += rowHeight + vSpacingPx
            }
        }
    }
}

@Composable
private fun StaggeredLayout(
    columns: Int,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Layout(content = content, modifier = modifier) { measurables, constraints ->
        val colWidth = constraints.maxWidth / columns
        val itemConstraints = constraints.copy(minWidth = 0, maxWidth = colWidth, minHeight = 0)
        val colHeights = IntArray(columns)
        val placeables = measurables.map { it.measure(itemConstraints) }
        val positions = placeables.map { p ->
            val col = colHeights.indices.minByOrNull { colHeights[it] } ?: 0
            val pos = IntOffset(col * colWidth, colHeights[col])
            colHeights[col] += p.height
            pos
        }
        val totalHeight = (colHeights.maxOrNull() ?: 0)
        layout(constraints.maxWidth, totalHeight) {
            placeables.forEachIndexed { i, p -> p.place(positions[i]) }
        }
    }
}

@Composable
private fun AdaptiveFitRow(
    maxWidthFraction: Float,
    spacing: Dp,
    modifier: Modifier = Modifier,
    items: @Composable () -> Unit
) {
    SubcomposeLayout(modifier = modifier) { constraints ->
        val maxAvailableWidth = (constraints.maxWidth * maxWidthFraction).toInt()
        val spacingPx = spacing.roundToPx()
        val mainPlaceables = mutableListOf<Placeable>()
        var remainingWidth = maxAvailableWidth

        // Force compose children to retrieve list of measurables
        val measurables = subcompose("items_content", items)
        var itemsComposed = 0

        for (i in measurables.indices) {
            val itemPlaceable = measurables[i].measure(constraints.copy(minWidth = 0, minHeight = 0))
            val requiredWidth = itemPlaceable.width + (if (itemsComposed > 0) spacingPx else 0)

            if (remainingWidth >= requiredWidth) {
                mainPlaceables.add(itemPlaceable)
                remainingWidth -= requiredWidth
                itemsComposed++
            } else {
                val remainingCount = measurables.size - itemsComposed
                if (remainingCount > 0) {
                    val badgeMeasurables = subcompose("badge_$i") {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.primary)
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "+$remainingCount More",
                                color = MaterialTheme.colorScheme.onPrimary,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    val badgePlaceable = badgeMeasurables.first().measure(constraints.copy(minWidth = 0, minHeight = 0))

                    var badgeFits = remainingWidth >= badgePlaceable.width + (if (itemsComposed > 0) spacingPx else 0)
                    if (!badgeFits && mainPlaceables.isNotEmpty()) {
                        val popped = mainPlaceables.removeLast()
                        remainingWidth += popped.width + (if (mainPlaceables.isEmpty()) 0 else spacingPx)
                        badgeFits = remainingWidth >= badgePlaceable.width + (if (mainPlaceables.isEmpty()) 0 else spacingPx)
                    }
                    if (badgeFits) {
                        mainPlaceables.add(badgePlaceable)
                    }
                }
                break
            }
        }

        val totalHeight = mainPlaceables.maxOfOrNull { it.height } ?: 0
        layout(maxAvailableWidth, totalHeight) {
            var currentX = 0
            mainPlaceables.forEach { placeable ->
                placeable.place(currentX, (totalHeight - placeable.height) / 2)
                currentX += placeable.width + spacingPx
            }
        }
    }
}

@Composable
private fun VariableWidthTabRow(
    selectedIndex: Int,
    tabs: List<String>,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    SubcomposeLayout(modifier = modifier) { constraints ->
        val spacing = 16.dp.roundToPx()
        val tabPlaceables = tabs.indices.map { index ->
            subcompose("tab_$index") {
                Box(
                    modifier = Modifier
                        .clickable { onTabSelected(index) }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = tabs[index],
                        color = if (selectedIndex == index) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = if (selectedIndex == index) FontWeight.Bold else FontWeight.Normal,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }.first().measure(constraints.copy(minWidth = 0, minHeight = 0))
        }

        val rowHeight = tabPlaceables.maxOfOrNull { it.height } ?: 0
        val tabOffsets = IntArray(tabs.size)
        var accumulatedX = 0
        tabPlaceables.forEachIndexed { index, placeable ->
            tabOffsets[index] = accumulatedX
            accumulatedX += placeable.width + spacing
        }
        val totalTabsWidth = accumulatedX - if (tabs.isNotEmpty()) spacing else 0

        // Subcompose indicator under selected tab width
        val targetWidth = tabPlaceables.getOrNull(selectedIndex)?.width ?: 0
        val indicatorMeasurables = subcompose("indicator") {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .clip(RoundedCornerShape(1.5.dp))
                    .background(MaterialTheme.colorScheme.primary)
            )
        }
        val indicatorPlaceable = indicatorMeasurables.first().measure(
            constraints.copy(minWidth = targetWidth, maxWidth = targetWidth)
        )

        val totalHeight = rowHeight + indicatorPlaceable.height

        layout(constraints.maxWidth, totalHeight) {
            val startPadding = ((constraints.maxWidth - totalTabsWidth) / 2).coerceAtLeast(0)

            tabPlaceables.forEachIndexed { index, placeable ->
                placeable.place(startPadding + tabOffsets[index], 0)
            }
            val activeX = tabOffsets.getOrElse(selectedIndex) { 0 }
            indicatorPlaceable.place(startPadding + activeX, rowHeight)
        }
    }
}

// -------------------------------------------------------------------------------------
// Syntax Highlighting View & Code Snippets
// -------------------------------------------------------------------------------------

@Composable
fun HighlightedCodeBlock(code: String, modifier: Modifier = Modifier) {
    val annotatedString = remember(code) {
        buildAnnotatedString {
            var currentIndex = 0
            val keywords = listOf(
                "fun", "val", "var", "import", "package", "private", "class", "interface",
                "return", "if", "else", "for", "in", "when", "repeat", "forEach", "forEachIndexed",
                "map", "first", "layout", "measure", "subcompose", "Layout", "SubcomposeLayout"
            )
            val types = listOf(
                "Modifier", "Dp", "Constraints", "Placeable", "Measurable", "MeasureScope",
                "MeasureResult", "IntOffset", "Color", "Box", "Text", "Composable", "IntArray"
            )

            val regex = Regex("\\b(${keywords.joinToString("|")})\\b|\\b(${types.joinToString("|")})\\b|//.*|\\\".*?\\\"|\\d+")
            val matches = regex.findAll(code)

            for (match in matches) {
                if (match.range.first > currentIndex) {
                    append(code.substring(currentIndex, match.range.first))
                }

                val text = match.value
                when {
                    text.startsWith("//") -> {
                        withStyle(SpanStyle(color = Color(0xFF6A9955))) {
                            append(text)
                        }
                    }
                    text.startsWith("\"") -> {
                        withStyle(SpanStyle(color = Color(0xFFCE9178))) {
                            append(text)
                        }
                    }
                    text.matches(Regex("\\d+")) -> {
                        withStyle(SpanStyle(color = Color(0xFFB5CEA8))) {
                            append(text)
                        }
                    }
                    keywords.contains(text) -> {
                        withStyle(SpanStyle(color = Color(0xFF569CD6), fontWeight = FontWeight.Bold)) {
                            append(text)
                        }
                    }
                    types.contains(text) -> {
                        withStyle(SpanStyle(color = Color(0xFF4EC9B0))) {
                            append(text)
                        }
                    }
                }
                currentIndex = match.range.last + 1
            }
            if (currentIndex < code.length) {
                append(code.substring(currentIndex))
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF1E1E1E))
            .border(1.dp, Color(0xFF333333), RoundedCornerShape(12.dp))
            .padding(16.dp)
            .horizontalScroll(rememberScrollState())
    ) {
        Text(
            text = annotatedString,
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
            lineHeight = 15.sp
        )
    }
}

val radialLayoutCode = """
// 1. RADIAL LAYOUT
// Places children evenly on a circle perimeter.
@Composable
fun RadialLayout(
    rotationDeg: Float,
    radiusRatio: Float,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Layout(content = content, modifier = modifier) { measurables, constraints ->
        // Measure children without enforcing min bounds
        val placeables = measurables.map { 
            it.measure(constraints.copy(minWidth = 0, minHeight = 0)) 
        }
        val w = if (constraints.hasBoundedWidth) constraints.maxWidth else 1000
        val h = if (constraints.hasBoundedHeight) constraints.maxHeight else 1000
        val radius = min(w, h) / 2f * radiusRatio
        
        layout(w, h) {
            val cx = w / 2f
            val cy = h / 2f
            val count = placeables.size
            placeables.forEachIndexed { i, p ->
                val ang = ((rotationDeg + i * 360f / count) * kotlin.math.PI / 180.0)
                val x = cx + (radius * cos(ang)).toFloat() - p.width / 2f
                val y = cy + (radius * sin(ang)).toFloat() - p.height / 2f
                p.place(x.toInt(), y.toInt())
            }
        }
    }
}
""".trimIndent()

val flowLayoutCode = """
// 2. FLOW ROW LAYOUT
// Places items horizontally and wraps to the next line on overflow.
@Composable
fun CustomFlowRow(
    horizontalSpacing: Dp,
    verticalSpacing: Dp,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Layout(content = content, modifier = modifier) { measurables, constraints ->
        val hSpacingPx = horizontalSpacing.roundToPx()
        val vSpacingPx = verticalSpacing.roundToPx()
        val placeables = measurables.map { it.measure(constraints.copy(minWidth = 0, minHeight = 0)) }
        val rows = mutableListOf<List<Placeable>>()
        var currentRow = mutableListOf<Placeable>()
        var currentRowWidth = 0

        placeables.forEach { p ->
            val reqWidth = if (currentRow.isEmpty()) p.width else p.width + hSpacingPx
            if (currentRowWidth + reqWidth <= constraints.maxWidth) {
                currentRow.add(p)
                currentRowWidth += reqWidth
            } else {
                rows.add(currentRow)
                currentRow = mutableListOf(p)
                currentRowWidth = p.width
            }
        }
        if (currentRow.isNotEmpty()) rows.add(currentRow)

        val layoutWidth = constraints.maxWidth
        val layoutHeight = if (rows.isEmpty()) 0 else {
            rows.sumOf { r -> r.maxOf { it.height } } + (rows.size - 1) * vSpacingPx
        }

        layout(layoutWidth, layoutHeight) {
            var currentY = 0
            rows.forEach { row ->
                val rowHeight = row.maxOf { it.height }
                var currentX = 0
                row.forEach { p ->
                    p.place(currentX, currentY + (rowHeight - p.height) / 2)
                    currentX += p.width + hSpacingPx
                }
                currentY += rowHeight + vSpacingPx
            }
        }
    }
}
""".trimIndent()

val staggeredLayoutCode = """
// 3. STAGGERED MASONRY GRID LAYOUT
// Columns are divided equally; drops children into the shortest column.
@Composable
fun StaggeredLayout(
    columns: Int,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Layout(content = content, modifier = modifier) { measurables, constraints ->
        val colWidth = constraints.maxWidth / columns
        val itemConstraints = constraints.copy(minWidth = 0, maxWidth = colWidth, minHeight = 0)
        val colHeights = IntArray(columns)
        val placeables = measurables.map { it.measure(itemConstraints) }
        val positions = placeables.map { p ->
            val col = colHeights.indices.minByOrNull { colHeights[it] } ?: 0
            val pos = IntOffset(col * colWidth, colHeights[col])
            colHeights[col] += p.height
            pos
        }
        val totalHeight = (colHeights.maxOrNull() ?: 0)
        layout(constraints.maxWidth, totalHeight) {
            placeables.forEachIndexed { i, p -> p.place(positions[i]) }
        }
    }
}
""".trimIndent()

val adaptiveFitCode = """
// 4. ADAPTIVE ROW (SubcomposeLayout)
// Decides how many children to compose during layout phase.
@Composable
fun AdaptiveFitRow(
    maxWidthFraction: Float,
    spacing: Dp,
    modifier: Modifier = Modifier,
    items: @Composable () -> Unit
) {
    SubcomposeLayout(modifier = modifier) { constraints ->
        val maxAvailableWidth = (constraints.maxWidth * maxWidthFraction).toInt()
        val spacingPx = spacing.roundToPx()
        val mainPlaceables = mutableListOf<Placeable>()
        var remainingWidth = maxAvailableWidth

        // Subcompose children of the content slot to measure them
        val measurables = subcompose("items_content", items)
        var itemsComposed = 0

        for (i in measurables.indices) {
            val p = measurables[i].measure(constraints.copy(minWidth = 0, minHeight = 0))
            val reqW = p.width + (if (itemsComposed > 0) spacingPx else 0)

            if (remainingWidth >= reqW) {
                mainPlaceables.add(p)
                remainingWidth -= reqW
                itemsComposed++
            } else {
                val remaining = measurables.size - itemsComposed
                if (remaining > 0) {
                    val badgeMeasurables = subcompose("badge") {
                        Text("+${d}{remaining} More")
                    }
                    val badgeP = badgeMeasurables.first().measure(constraints.copy(minWidth = 0, minHeight = 0))
                    var fits = remainingWidth >= badgeP.width + spacingPx
                    if (!fits && mainPlaceables.isNotEmpty()) {
                        val popped = mainPlaceables.removeLast()
                        remainingWidth += popped.width + spacingPx
                        fits = remainingWidth >= badgeP.width + spacingPx
                    }
                    if (fits) mainPlaceables.add(badgeP)
                }
                break
            }
        }
        val totalHeight = mainPlaceables.maxOfOrNull { it.height } ?: 0
        layout(maxAvailableWidth, totalHeight) {
            var currentX = 0
            mainPlaceables.forEach { p ->
                p.place(currentX, (totalHeight - p.height) / 2)
                currentX += p.width + spacingPx
            }
        }
    }
}
""".trimIndent()

val variableTabCode = """
// 5. VARIABLE TAB ROW (SubcomposeLayout)
// Subcomposes select slider indicator matching active tab width.
@Composable
fun VariableWidthTabRow(
    selectedIndex: Int,
    tabs: List<String>,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    SubcomposeLayout(modifier = modifier) { constraints ->
        val spacing = 16.dp.roundToPx()
        val tabPlaceables = tabs.indices.map { idx ->
            subcompose("tab_${d}idx") {
                Text(tabs[idx], modifier = Modifier.clickable { onTabSelected(idx) })
            }.first().measure(constraints.copy(minWidth = 0, minHeight = 0))
        }

        val rowHeight = tabPlaceables.maxOfOrNull { it.height } ?: 0
        val tabOffsets = IntArray(tabs.size)
        var accumulatedX = 0
        tabPlaceables.forEachIndexed { i, p ->
            tabOffsets[i] = accumulatedX
            accumulatedX += p.width + spacing
        }
        val totalTabsWidth = accumulatedX - if (tabs.isNotEmpty()) spacing else 0

        // Subcompose the slider indicator to match active tab width
        val targetWidth = tabPlaceables.getOrNull(selectedIndex)?.width ?: 0
        val indicatorMeasurables = subcompose("indicator") {
            Box(Modifier.fillMaxWidth().height(3.dp).background(Color.Blue))
        }
        val indicatorPlaceable = indicatorMeasurables.first().measure(
            constraints.copy(minWidth = targetWidth, maxWidth = targetWidth)
        )

        layout(constraints.maxWidth, rowHeight + indicatorPlaceable.height) {
            val startPadding = ((constraints.maxWidth - totalTabsWidth) / 2).coerceAtLeast(0)
            tabPlaceables.forEachIndexed { idx, p ->
                p.place(startPadding + tabOffsets[idx], 0)
            }
            val activeX = tabOffsets.getOrElse(selectedIndex) { 0 }
            indicatorPlaceable.place(startPadding + activeX, rowHeight)
        }
    }
}
""".trimIndent()
