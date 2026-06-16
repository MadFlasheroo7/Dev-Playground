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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.drawText

enum class ItemState {
    IDLE,        // Grey: not composed
    PREFETCH,    // Yellow: pre-composed/pre-measured ahead of scroll
    ACTIVE,      // Green: composed, measured, visible in viewport
    RETAINED,    // Cyan: off-screen cached item
    DISPOSED     // Red/Fade: recycled and swept from memory
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LazyLifecycleScreen(onBack: () -> Unit) {
    var scrollPos by remember { mutableStateOf(0f) } // scroll index (0..30)
    var prefetchingEnabled by remember { mutableStateOf(true) }
    var cacheSize by remember { mutableStateOf(2) } // off-screen cache limit
    
    val totalItems = 36
    val viewportHeightItems = 6
    
    // Resolve states for all totalItems
    val itemStates = remember(scrollPos, prefetchingEnabled, cacheSize) {
        val states = Array(totalItems) { ItemState.IDLE }
        
        val visibleStart = scrollPos.toInt()
        val visibleEnd = (visibleStart + viewportHeightItems).coerceAtMost(totalItems - 1)
        
        // 1. Mark visible items as Active
        for (i in visibleStart..visibleEnd) {
            if (i in 0 until totalItems) {
                states[i] = ItemState.ACTIVE
            }
        }
        
        // 2. Mark off-screen cached/retained items
        // Items just above the viewport that were recently visible
        for (i in 1..cacheSize) {
            val idx = visibleStart - i
            if (idx in 0 until totalItems) {
                states[idx] = ItemState.RETAINED
            }
        }
        
        // 3. Mark prefetch items ahead of scroll direction
        // Assuming user is scrolling down, we prefetch items just below visibleEnd
        if (prefetchingEnabled) {
            val prefetchLimit = 2
            for (i in 1..prefetchLimit) {
                val idx = visibleEnd + i
                if (idx in 0 until totalItems && states[idx] == ItemState.IDLE) {
                    states[idx] = ItemState.PREFETCH
                }
            }
        }
        
        // Any item that is not idle, prefetch, active, or retained, and was previously passed, is marked disposed/idle
        for (i in 0 until totalItems) {
            if (i < visibleStart - cacheSize) {
                states[i] = ItemState.DISPOSED
            }
        }
        
        states.toList()
    }
    
    // Count stats
    val activeCount = itemStates.count { it == ItemState.ACTIVE }
    val prefetchCount = itemStates.count { it == ItemState.PREFETCH }
    val retainedCount = itemStates.count { it == ItemState.RETAINED }
    val disposedCount = itemStates.count { it == ItemState.DISPOSED }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Lazy Layout Lifecycle Lab", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    TextButton(onClick = onBack) {
                        Text("< Back", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                },
                actions = {
                    TextButton(onClick = {
                        scrollPos = 0f
                        prefetchingEnabled = true
                        cacheSize = 2
                    }) {
                        Text("Reset", color = Color(0xFFEF4444))
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
                    // Left Pane: Controls and Stats
                    Column(
                        modifier = Modifier
                            .weight(0.42f)
                            .fillMaxHeight()
                            .padding(12.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        LazyControls(
                            scrollPos = scrollPos,
                            onScrollPosChange = { scrollPos = it },
                            prefetchingEnabled = prefetchingEnabled,
                            onPrefetchingToggle = { prefetchingEnabled = it },
                            cacheSize = cacheSize,
                            onCacheSizeChange = { cacheSize = it.toInt() }
                        )
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        StatsDashboard(
                            active = activeCount,
                            prefetch = prefetchCount,
                            retained = retainedCount,
                            recycled = disposedCount
                        )
                    }
                    
                    Box(modifier = Modifier.fillMaxHeight().width(1.dp).background(Color(0xFF2D2D35)))
                    
                    // Right Pane: Virtual Viewport and Grid
                    Row(
                        modifier = Modifier
                            .weight(0.58f)
                            .fillMaxHeight()
                            .padding(16.dp)
                    ) {
                        // Virtual phone screen viewport
                        Box(
                            modifier = Modifier
                                .weight(0.5f)
                                .fillMaxHeight()
                        ) {
                            VirtualViewportList(
                                scrollPos = scrollPos,
                                itemsStates = itemStates,
                                viewportHeight = viewportHeightItems
                            )
                        }
                        
                        Spacer(modifier = Modifier.width(16.dp))
                        
                        // Heap Map (state grid)
                        Box(
                            modifier = Modifier
                                .weight(0.5f)
                                .fillMaxHeight()
                        ) {
                            StateGridMap(
                                itemsStates = itemStates
                            )
                        }
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
                    // Virtual viewport on top
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp)
                    ) {
                        Row(modifier = Modifier.fillMaxSize()) {
                            Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                                VirtualViewportList(
                                    scrollPos = scrollPos,
                                    itemsStates = itemStates,
                                    viewportHeight = viewportHeightItems
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                                StateGridMap(
                                    itemsStates = itemStates
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    LazyControls(
                        scrollPos = scrollPos,
                        onScrollPosChange = { scrollPos = it },
                        prefetchingEnabled = prefetchingEnabled,
                        onPrefetchingToggle = { prefetchingEnabled = it },
                        cacheSize = cacheSize,
                        onCacheSizeChange = { cacheSize = it.toInt() }
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    StatsDashboard(
                        active = activeCount,
                        prefetch = prefetchCount,
                        retained = retainedCount,
                        recycled = disposedCount
                    )
                }
            }
        }
    }
}

@Composable
fun LazyControls(
    scrollPos: Float,
    onScrollPosChange: (Float) -> Unit,
    prefetchingEnabled: Boolean,
    onPrefetchingToggle: (Boolean) -> Unit,
    cacheSize: Int,
    onCacheSizeChange: (Float) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E24)),
        border = BorderStroke(1.dp, Color(0xFF2D2D35))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Lazy Layout Scroll Simulation",
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))
            
            // Scroll Slider
            Text(
                text = "Scroll Position (Viewport Offset: index ${scrollPos.toInt()})",
                color = Color.Gray,
                fontSize = 11.sp
            )
            Slider(
                value = scrollPos,
                onValueChange = onScrollPosChange,
                valueRange = 0f..28f,
                colors = SliderDefaults.colors(
                    thumbColor = Color(0xFF00E5FF),
                    activeTrackColor = Color(0xFF00E5FF)
                )
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = Color(0xFF2D2D35))
            Spacer(modifier = Modifier.height(12.dp))
            
            // Prefetch Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Enable Lazy Pre-fetching",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Compose pre-composes and measures upcoming items before they enter viewport to prevent scroll jank.",
                        color = Color.Gray,
                        fontSize = 10.sp,
                        lineHeight = 13.sp
                    )
                }
                Switch(
                    checked = prefetchingEnabled,
                    onCheckedChange = onPrefetchingToggle,
                    colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF10B981))
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Cache Size Slider
            Text(
                text = "Off-Screen Cache Size (Cached count: $cacheSize)",
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "Specifies how many scrolled-out items are kept active in memory before being recycled (disposed).",
                color = Color.Gray,
                fontSize = 10.sp
            )
            Slider(
                value = cacheSize.toFloat(),
                onValueChange = onCacheSizeChange,
                valueRange = 0f..5f,
                steps = 4,
                colors = SliderDefaults.colors(
                    thumbColor = Color(0xFF8B5CF6),
                    activeTrackColor = Color(0xFF8B5CF6)
                )
            )
        }
    }
}

@Composable
fun StatsDashboard(
    active: Int,
    prefetch: Int,
    retained: Int,
    recycled: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF151518)),
        border = BorderStroke(1.dp, Color(0xFF26262D))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = "Composition Heap Stats",
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(10.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatCard(title = "ACTIVE", count = active, color = Color(0xFF10B981), modifier = Modifier.weight(1f))
                StatCard(title = "PREFETCH", count = prefetch, color = Color(0xFFF59E0B), modifier = Modifier.weight(1f))
                StatCard(title = "CACHED", count = retained, color = Color(0xFF06B6D4), modifier = Modifier.weight(1f))
                StatCard(title = "RECYCLED", count = recycled, color = Color(0xFFEF4444), modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
fun StatCard(title: String, count: Int, color: Color, modifier: Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(color.copy(alpha = 0.12f))
            .border(BorderStroke(1.dp, color.copy(alpha = 0.4f)))
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(title, color = color, fontSize = 8.sp, fontWeight = FontWeight.Bold)
            Text("$count", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun VirtualViewportList(
    scrollPos: Float,
    itemsStates: List<ItemState>,
    viewportHeight: Int
) {
    Card(
        modifier = Modifier.fillMaxSize(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F0F11)),
        border = BorderStroke(2.dp, Color(0xFF2D2D35))
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Viewport header bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF1E1E24))
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("Virtual Scrolling Viewport", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            
            // Viewport body
            // We show 6 cells vertically corresponding to viewport items
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val offsetFraction = scrollPos % 1f
                val startIdx = scrollPos.toInt()
                
                // Show 7 items to allow partial scrolling translation offset
                for (i in 0..viewportHeight) {
                    val currentItemIndex = startIdx + i
                    if (currentItemIndex in itemsStates.indices) {
                        val state = itemsStates[currentItemIndex]
                        
                        val stateBg = when (state) {
                            ItemState.ACTIVE -> Color(0xFF10B981).copy(alpha = 0.2f)
                            ItemState.RETAINED -> Color(0xFF06B6D4).copy(alpha = 0.2f)
                            ItemState.PREFETCH -> Color(0xFFF59E0B).copy(alpha = 0.2f)
                            else -> Color(0xFF374151).copy(alpha = 0.1f)
                        }
                        
                        val stateBorder = when (state) {
                            ItemState.ACTIVE -> Color(0xFF10B981)
                            ItemState.RETAINED -> Color(0xFF06B6D4)
                            ItemState.PREFETCH -> Color(0xFFF59E0B)
                            else -> Color.DarkGray
                        }
                        
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(38.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(stateBg)
                                .border(BorderStroke(1.dp, stateBorder))
                                .padding(horizontal = 10.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Item #$currentItemIndex",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = state.name,
                                    color = stateBorder,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StateGridMap(
    itemsStates: List<ItemState>
) {
    Card(
        modifier = Modifier.fillMaxSize(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF151518)),
        border = BorderStroke(1.dp, Color(0xFF26262D))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = "Full List Memory States",
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(10.dp))
            
            // Grid Canvas representing all 36 items
            Canvas(modifier = Modifier.weight(1f).fillMaxWidth()) {
                val boxWidth = 32f
                val boxHeight = 32f
                val spacing = 8f
                
                val cols = 4
                
                itemsStates.forEachIndexed { index, state ->
                    val col = index % cols
                    val row = index / cols
                    
                    val x = col * (boxWidth + spacing) + 12f
                    val y = row * (boxHeight + spacing) + 8f
                    
                    val color = when (state) {
                        ItemState.IDLE -> Color(0xFF374151)      // Grey
                        ItemState.PREFETCH -> Color(0xFFF59E0B)  // Yellow
                        ItemState.ACTIVE -> Color(0xFF10B981)    // Green
                        ItemState.RETAINED -> Color(0xFF06B6D4)  // Cyan
                        ItemState.DISPOSED -> Color(0xFFEF4444).copy(alpha = 0.4f)  // Recycled
                    }
                    
                    // Draw state box
                    drawRoundRect(
                        color = color,
                        topLeft = Offset(x, y),
                        size = Size(boxWidth, boxHeight),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(3f, 3f)
                    )
                    
                    // Box index text
                    // We can draw a tiny text or simple overlay inside
                }
            }
            
            // Color legend
            Column(
                modifier = Modifier.fillMaxWidth().background(Color(0xFF0F0F11)).padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                LegendRow(label = "Active (in Viewport)", color = Color(0xFF10B981))
                LegendRow(label = "Pre-fetched (Yellow)", color = Color(0xFFF59E0B))
                LegendRow(label = "Cached (Cyan)", color = Color(0xFF06B6D4))
                LegendRow(label = "Recycled (Red/Grey)", color = Color(0xFFEF4444).copy(alpha = 0.5f))
            }
        }
    }
}

@Composable
fun LegendRow(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(10.dp).clip(RoundedCornerShape(2.dp)).background(color))
        Spacer(modifier = Modifier.width(6.dp))
        Text(label, color = Color.Gray, fontSize = 9.sp)
    }
}
