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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.drawText

enum class ModifierType {
    FILL_MAX_SIZE,
    WRAP_CONTENT_SIZE,
    PADDING_20,
    SIZE_100,
    REQUIRED_SIZE_200
}

data class ConstraintSet(
    val minW: Int,
    val maxW: Int,
    val minH: Int,
    val maxH: Int
) {
    override fun toString(): String {
        return "w=[$minW..$maxW], h=[$minH..$maxH]"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConstraintFlowScreen(onBack: () -> Unit) {
    var modifierChain = remember { 
        mutableStateListOf(
            ModifierType.FILL_MAX_SIZE,
            ModifierType.WRAP_CONTENT_SIZE,
            ModifierType.SIZE_100
        )
    }
    
    // Simulate constraints flow down and sizes up
    val parentConstraints = ConstraintSet(minW = 200, maxW = 350, minH = 200, maxH = 350)
    
    // Calculate intermediate constraint steps
    val steps = remember(modifierChain) {
        val list = mutableListOf<Pair<ModifierType, Pair<ConstraintSet, ConstraintSet>>>()
        var current = parentConstraints
        
        modifierChain.forEach { mod ->
            val outgoing = when (mod) {
                ModifierType.FILL_MAX_SIZE -> {
                    // forces min to match max
                    ConstraintSet(minW = current.maxW, maxW = current.maxW, minH = current.maxH, maxH = current.maxH)
                }
                ModifierType.WRAP_CONTENT_SIZE -> {
                    // loosens min to 0
                    ConstraintSet(minW = 0, maxW = current.maxW, minH = 0, maxH = current.maxH)
                }
                ModifierType.PADDING_20 -> {
                    // subtracts padding
                    val outMaxW = (current.maxW - 40).coerceAtLeast(0)
                    val outMinW = (current.minW - 40).coerceAtLeast(0)
                    val outMaxH = (current.maxH - 40).coerceAtLeast(0)
                    val outMinH = (current.minH - 40).coerceAtLeast(0)
                    ConstraintSet(minW = outMinW, maxW = outMaxW, minH = outMinH, maxH = outMaxH)
                }
                ModifierType.SIZE_100 -> {
                    // forces exact size, constrained by incoming max
                    val size = 100
                    val resolvedW = size.coerceIn(current.minW, current.maxW)
                    val resolvedH = size.coerceIn(current.minH, current.maxH)
                    ConstraintSet(minW = resolvedW, maxW = resolvedW, minH = resolvedH, maxH = resolvedH)
                }
                ModifierType.REQUIRED_SIZE_200 -> {
                    // ignores incoming constraints and forces 200
                    ConstraintSet(minW = 200, maxW = 200, minH = 200, maxH = 200)
                }
            }
            list.add(mod to (current to outgoing))
            current = outgoing
        }
        list
    }
    
    // Final child dimension measurement
    val childMeasuredSize = remember(steps) {
        val finalConstraints = steps.lastOrNull()?.second?.second ?: parentConstraints
        // Child wants to be 150x150, constrained by final constraints
        val finalW = 150.coerceIn(finalConstraints.minW, finalConstraints.maxW)
        val finalH = 150.coerceIn(finalConstraints.minH, finalConstraints.maxH)
        Offset(finalW.toFloat(), finalH.toFloat())
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Modifier Constraints Laboratory", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    TextButton(onClick = onBack) {
                        Text("< Back", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                },
                actions = {
                    TextButton(onClick = {
                        modifierChain.clear()
                        modifierChain.addAll(listOf(ModifierType.FILL_MAX_SIZE, ModifierType.WRAP_CONTENT_SIZE, ModifierType.SIZE_100))
                    }) {
                        Text("Reset Chain", color = Color(0xFFEF4444))
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
                    // Left Column: Modifier Chain Builder
                    Column(
                        modifier = Modifier
                            .weight(0.42f)
                            .fillMaxHeight()
                            .padding(12.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        ChainBuilder(
                            chain = modifierChain,
                            onAdd = { modifierChain.add(it) },
                            onRemove = { modifierChain.removeAt(it) },
                            onMoveUp = { index ->
                                if (index > 0) {
                                    val temp = modifierChain[index]
                                    modifierChain[index] = modifierChain[index - 1]
                                    modifierChain[index - 1] = temp
                                }
                            },
                            onMoveDown = { index ->
                                if (index < modifierChain.size - 1) {
                                    val temp = modifierChain[index]
                                    modifierChain[index] = modifierChain[index + 1]
                                    modifierChain[index + 1] = temp
                                }
                            }
                        )
                    }
                    
                    Box(modifier = Modifier.fillMaxHeight().width(1.dp).background(Color(0xFF2D2D35)))
                    
                    // Right Column: Pipeline flow trace
                    Column(
                        modifier = Modifier
                            .weight(0.58f)
                            .fillMaxHeight()
                            .padding(12.dp)
                    ) {
                        Text(
                            text = "Constraints Down, Sizes Up Flow",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        FlowCanvas(
                            parentConstraints = parentConstraints,
                            steps = steps,
                            measuredSize = childMeasuredSize,
                            modifier = Modifier
                                .weight(0.6f)
                                .fillMaxWidth()
                        )
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        LivePreviewBox(
                            width = childMeasuredSize.x,
                            height = childMeasuredSize.y,
                            modifier = Modifier.weight(0.4f)
                        )
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
                    FlowCanvas(
                        parentConstraints = parentConstraints,
                        steps = steps,
                        measuredSize = childMeasuredSize,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(350.dp)
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    LivePreviewBox(
                        width = childMeasuredSize.x,
                        height = childMeasuredSize.y,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    ChainBuilder(
                        chain = modifierChain,
                        onAdd = { modifierChain.add(it) },
                        onRemove = { modifierChain.removeAt(it) },
                        onMoveUp = { index ->
                            if (index > 0) {
                                val temp = modifierChain[index]
                                modifierChain[index] = modifierChain[index - 1]
                                modifierChain[index - 1] = temp
                            }
                        },
                        onMoveDown = { index ->
                            if (index < modifierChain.size - 1) {
                                val temp = modifierChain[index]
                                modifierChain[index] = modifierChain[index + 1]
                                modifierChain[index + 1] = temp
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun ChainBuilder(
    chain: List<ModifierType>,
    onAdd: (ModifierType) -> Unit,
    onRemove: (Int) -> Unit,
    onMoveUp: (Int) -> Unit,
    onMoveDown: (Int) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E24)),
        border = BorderStroke(1.dp, Color(0xFF2D2D35))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Modifier Chain Builder",
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Chain modifiers to see constraints flow down and size output resolve up.",
                color = Color.Gray,
                fontSize = 11.sp
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Current Chain List
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (chain.isEmpty()) {
                    Text("No modifiers in chain. Child measures freely.", color = Color.Gray, fontSize = 12.sp, modifier = Modifier.padding(vertical = 12.dp))
                }
                
                chain.forEachIndexed { index, mod ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFF2B2B33))
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${index + 1}. ${getModifierLabel(mod)}",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f)
                        )
                        
                        // Move buttons
                        TextButton(
                            onClick = { onMoveUp(index) },
                            enabled = index > 0,
                            contentPadding = PaddingValues(horizontal = 4.dp)
                        ) {
                            Text("▲", fontSize = 11.sp, color = if (index > 0) Color.Cyan else Color.DarkGray)
                        }
                        
                        TextButton(
                            onClick = { onMoveDown(index) },
                            enabled = index < chain.size - 1,
                            contentPadding = PaddingValues(horizontal = 4.dp)
                        ) {
                            Text("▼", fontSize = 11.sp, color = if (index < chain.size - 1) Color.Cyan else Color.DarkGray)
                        }
                        
                        TextButton(
                            onClick = { onRemove(index) },
                            contentPadding = PaddingValues(horizontal = 4.dp)
                        ) {
                            Text("Remove", fontSize = 11.sp, color = Color(0xFFEF4444))
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = Color(0xFF2D2D35))
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = "Add Modifier to Chain",
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            
            // Add buttons grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    AddModifierButton(label = "fillMaxSize()", onClick = { onAdd(ModifierType.FILL_MAX_SIZE) })
                    AddModifierButton(label = "wrapContentSize()", onClick = { onAdd(ModifierType.WRAP_CONTENT_SIZE) })
                    AddModifierButton(label = "padding(20.dp)", onClick = { onAdd(ModifierType.PADDING_20) })
                }
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    AddModifierButton(label = "size(100.dp)", onClick = { onAdd(ModifierType.SIZE_100) })
                    AddModifierButton(label = "requiredSize(200.dp)", onClick = { onAdd(ModifierType.REQUIRED_SIZE_200) })
                }
            }
        }
    }
}

@Composable
fun AddModifierButton(label: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6)),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
        modifier = Modifier.fillMaxWidth().height(32.dp)
    ) {
        Text(label, fontSize = 11.sp, color = Color.White)
    }
}

fun getModifierLabel(type: ModifierType): String {
    return when (type) {
        ModifierType.FILL_MAX_SIZE -> "Modifier.fillMaxSize()"
        ModifierType.WRAP_CONTENT_SIZE -> "Modifier.wrapContentSize()"
        ModifierType.PADDING_20 -> "Modifier.padding(20.dp)"
        ModifierType.SIZE_100 -> "Modifier.size(100.dp)"
        ModifierType.REQUIRED_SIZE_200 -> "Modifier.requiredSize(200.dp)"
    }
}

@Composable
fun FlowCanvas(
    parentConstraints: ConstraintSet,
    steps: List<Pair<ModifierType, Pair<ConstraintSet, ConstraintSet>>>,
    measuredSize: Offset,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF151518)),
        border = BorderStroke(1.dp, Color(0xFF26262D))
    ) {
        Canvas(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            val canvasWidth = size.width
            val canvasHeight = size.height
            
            // Draw nodes along vertical flow line
            val stepCount = steps.size
            val nodeHeight = 44f
            val nodeWidth = 240f
            
            val totalSteps = stepCount + 2 // Parent node + steps + Child Node
            val spacing = (canvasHeight - totalSteps * nodeHeight) / (totalSteps - 1)
            
            // Draw background flow pipe
            drawLine(
                color = Color(0xFF2D2D35),
                start = Offset(canvasWidth / 2f, 20f),
                end = Offset(canvasWidth / 2f, canvasHeight - 20f),
                strokeWidth = 6f
            )
            
            // Draw arrows Down (Left side) and Up (Right side)
            // Left arrows: constraints traveling down (blue)
            // Right arrows: sizes traveling up (green)
            for (i in 0 until totalSteps - 1) {
                val startY = 20f + i * (nodeHeight + spacing) + nodeHeight
                val endY = startY + spacing
                
                // Constraints Down (Blue Arrow)
                drawLine(
                    color = Color(0xFF3B82F6),
                    start = Offset(canvasWidth / 2f - 140f, startY + 5f),
                    end = Offset(canvasWidth / 2f - 140f, endY - 10f),
                    strokeWidth = 3f
                )
                drawCircle(
                    color = Color(0xFF3B82F6),
                    radius = 4f,
                    center = Offset(canvasWidth / 2f - 140f, endY - 10f)
                )
                
                // Sizes Up (Green Arrow)
                drawLine(
                    color = Color(0xFF10B981),
                    start = Offset(canvasWidth / 2f + 140f, endY - 5f),
                    end = Offset(canvasWidth / 2f + 140f, startY + 10f),
                    strokeWidth = 3f
                )
                drawCircle(
                    color = Color(0xFF10B981),
                    radius = 4f,
                    center = Offset(canvasWidth / 2f + 140f, startY + 10f)
                )
            }
            
            // 1. Draw Parent node
            val parentY = 20f
            val nodeX = (canvasWidth - nodeWidth) / 2f
            drawRoundRect(
                color = Color(0xFF374151),
                topLeft = Offset(nodeX, parentY),
                size = androidx.compose.ui.geometry.Size(nodeWidth, nodeHeight),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f, 4f)
            )
            
            // 2. Draw Modifier Nodes
            steps.forEachIndexed { index, pair ->
                val nodeY = parentY + (index + 1) * (nodeHeight + spacing)
                val isLast = index == stepCount - 1
                
                drawRoundRect(
                    color = Color(0xFF1E1E24),
                    topLeft = Offset(nodeX, nodeY),
                    size = androidx.compose.ui.geometry.Size(nodeWidth, nodeHeight),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f, 4f),
                    style = Stroke(width = 2f)
                )
                drawRoundRect(
                    color = Color(0xFF2563EB).copy(alpha = 0.1f),
                    topLeft = Offset(nodeX, nodeY),
                    size = androidx.compose.ui.geometry.Size(nodeWidth, nodeHeight),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f, 4f)
                )
            }
            
            // 3. Draw Child Node
            val childY = parentY + (stepCount + 1) * (nodeHeight + spacing)
            drawRoundRect(
                color = Color(0xFF1F2937),
                topLeft = Offset(nodeX, childY),
                size = androidx.compose.ui.geometry.Size(nodeWidth, nodeHeight),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f, 4f)
            )
        }
        
        // Render labels on top of Canvas using a simple overlay
        Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            val stepCount = steps.size
            
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Parent Node info
                NodeText(
                    title = "Parent Component",
                    subtitle = "Sends: $parentConstraints",
                    color = Color.LightGray
                )
                
                // Modifier steps
                steps.forEach { step ->
                    NodeText(
                        title = getModifierLabel(step.first),
                        subtitle = "Received: ${step.second.first}\nPasses: ${step.second.second}",
                        color = Color(0xFF60A5FA)
                    )
                }
                
                // Child Node info
                val finalConstraints = steps.lastOrNull()?.second?.second ?: parentConstraints
                NodeText(
                    title = "Child Component (Wants 150x150)",
                    subtitle = "Fits to: $finalConstraints\nReturns size: ${measuredSize.x.toInt()} x ${measuredSize.y.toInt()}",
                    color = Color(0xFF34D399)
                )
            }
        }
    }
}

@Composable
fun NodeText(title: String, subtitle: String, color: Color) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(230.dp)
            .padding(vertical = 4.dp)
    ) {
        Text(
            text = title,
            color = color,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
        Text(
            text = subtitle,
            color = Color.Gray,
            fontSize = 9.sp,
            fontFamily = FontFamily.Monospace,
            lineHeight = 11.sp,
            maxLines = 2
        )
    }
}

@Composable
fun LivePreviewBox(
    width: Float,
    height: Float,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF18181C)),
        border = BorderStroke(1.dp, Color(0xFF2D2D35))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Resolved Layout Preview",
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            
            // Parent box representation
            Box(
                modifier = Modifier
                    .weight(1f)
                    .width(300.dp)
                    .background(Color(0xFF0F0F11))
                    .border(BorderStroke(1.dp, Color.DarkGray)),
                contentAlignment = Alignment.Center
            ) {
                // Scale preview box proportionally
                // Max size of parent is 350
                val scale = 0.45f
                Box(
                    modifier = Modifier
                        .size(
                            width = (width * scale).dp,
                            height = (height * scale).dp
                        )
                        .background(Color(0xFF10B981).copy(alpha = 0.3f))
                        .border(BorderStroke(2.dp, Color(0xFF10B981))),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${width.toInt()}x${height.toInt()}",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}
