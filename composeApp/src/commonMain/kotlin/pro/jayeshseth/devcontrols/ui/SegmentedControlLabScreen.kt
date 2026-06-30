package pro.jayeshseth.devcontrols.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

/** Segmented control with an interactive playground to customize spring physics, jiggle, and dimensions. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SegmentedControlLabScreen(onBack: () -> Unit) {
    val items = listOf("Day", "Week", "Month", "Year")
    var sel by remember { mutableIntStateOf(0) }
    var instantDragMode by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current

    // Playground Customization States
    var dampingRatio by remember { mutableFloatStateOf(0.62f) }
    var stiffness by remember { mutableFloatStateOf(400f) }
    var maxScaleMultiplier by remember { mutableFloatStateOf(1.16f) }
    var wobbleAmplitude by remember { mutableFloatStateOf(0.035f) }
    var cornerRadiusDp by remember { mutableFloatStateOf(28f) }
    var barHeightDp by remember { mutableFloatStateOf(56f) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Segmented Control Lab") },
                navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }
            )
        },
        modifier = Modifier.fillMaxSize()
    ) { inner ->
        Column(
            Modifier
                .padding(inner)
                .fillMaxSize()
                .padding(24.dp)
                .verticalScroll(rememberScrollState())
        ) {
            
            // Tab-specific gradient colors
            val targetStartColor = when (sel) {
                0 -> Color(0xFFF27121) // Day: Sunset Orange
                1 -> Color(0xFF00F2FE) // Week: Neon Cyan
                2 -> Color(0xFFFF0844) // Month: Love Red
                3 -> Color(0xFFF5AF19) // Year: Golden Yellow
                else -> Color(0xFFF27121)
            }
            val targetEndColor = when (sel) {
                0 -> Color(0xFFE94057)
                1 -> Color(0xFF4FACFE)
                2 -> Color(0xFFFFB199)
                3 -> Color(0xFFE100FF)
                else -> Color(0xFFE94057)
            }

            val startColor by animateColorAsState(targetStartColor, spring(stiffness = Spring.StiffnessLow))
            val endColor by animateColorAsState(targetEndColor, spring(stiffness = Spring.StiffnessLow))

            // The Segmented Control Container
            BoxWithConstraints(
                Modifier
                    .fillMaxWidth()
                    .height(barHeightDp.dp)
                    .background(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(cornerRadiusDp.dp)
                    )
            ) {
                val density = LocalDensity.current
                val widthPx = with(density) { maxWidth.toPx() }
                val segWPx = widthPx / items.size
                val segW = maxWidth / items.size

                val dragOffset = remember { Animatable(sel * segWPx) }
                val pillScale = remember { Animatable(1f) }
                val coroutineScope = rememberCoroutineScope()
                var isDragging by remember { mutableStateOf(false) }
                var lastCrossedIndex by remember { mutableIntStateOf(sel) }

                // Keep the offset in sync when not dragging
                LaunchedEffect(sel, isDragging) {
                    if (!isDragging) {
                        dragOffset.animateTo(
                            targetValue = sel * segWPx,
                            animationSpec = spring(stiffness = stiffness)
                        )
                    }
                }

                // Jiggly scale animation when tab changes
                LaunchedEffect(sel) {
                    launch {
                        // 1. Squish down during the slide
                        pillScale.animateTo(0.88f, spring(stiffness = stiffness))
                        // 2. Overshoot on arrival with the user-defined damping and stiffness
                        pillScale.animateTo(maxScaleMultiplier, spring(dampingRatio = dampingRatio, stiffness = stiffness))
                        // 3. Settle at resting scale (1.06x for a slight pop)
                        pillScale.animateTo(1.06f, spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = stiffness))
                    }
                }

                Box(
                    Modifier
                        .fillMaxSize()
                        .pointerInput(items.size, instantDragMode) {
                            if (instantDragMode) {
                                detectDragGestures(
                                    onDragStart = { offset ->
                                        val touchedIndex = (offset.x / segWPx).toInt().coerceIn(0, items.size - 1)
                                        if (touchedIndex == sel) {
                                            isDragging = true
                                            lastCrossedIndex = sel
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            coroutineScope.launch {
                                                pillScale.animateTo(1.1f, spring(stiffness = stiffness))
                                            }
                                        }
                                    },
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        if (isDragging) {
                                            coroutineScope.launch {
                                                val newOffset = (dragOffset.value + dragAmount.x)
                                                    .coerceIn(0f, widthPx - segWPx)
                                                dragOffset.snapTo(newOffset)

                                                val currentIndex = ((newOffset + segWPx / 2) / segWPx)
                                                    .toInt()
                                                    .coerceIn(0, items.size - 1)
                                                if (currentIndex != lastCrossedIndex) {
                                                    lastCrossedIndex = currentIndex
                                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                }
                                            }
                                        }
                                    },
                                    onDragEnd = {
                                        if (isDragging) {
                                            isDragging = false
                                            val targetIndex = ((dragOffset.value + segWPx / 2) / segWPx)
                                                .toInt()
                                                .coerceIn(0, items.size - 1)
                                            
                                            sel = targetIndex
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        }
                                    },
                                    onDragCancel = {
                                        if (isDragging) {
                                            isDragging = false
                                            coroutineScope.launch {
                                                pillScale.animateTo(1.06f, spring(stiffness = stiffness))
                                            }
                                        }
                                    }
                                )
                            } else {
                                detectDragGesturesAfterLongPress(
                                    onDragStart = { offset ->
                                        val touchedIndex = (offset.x / segWPx).toInt().coerceIn(0, items.size - 1)
                                        if (touchedIndex == sel) {
                                            isDragging = true
                                            lastCrossedIndex = sel
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            coroutineScope.launch {
                                                pillScale.animateTo(1.1f, spring(stiffness = stiffness))
                                            }
                                        }
                                    },
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        if (isDragging) {
                                            coroutineScope.launch {
                                                val newOffset = (dragOffset.value + dragAmount.x)
                                                    .coerceIn(0f, widthPx - segWPx)
                                                dragOffset.snapTo(newOffset)

                                                val currentIndex = ((newOffset + segWPx / 2) / segWPx)
                                                    .toInt()
                                                    .coerceIn(0, items.size - 1)
                                                if (currentIndex != lastCrossedIndex) {
                                                    lastCrossedIndex = currentIndex
                                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                }
                                            }
                                        }
                                    },
                                    onDragEnd = {
                                        if (isDragging) {
                                            isDragging = false
                                            val targetIndex = ((dragOffset.value + segWPx / 2) / segWPx)
                                                .toInt()
                                                .coerceIn(0, items.size - 1)
                                            
                                            sel = targetIndex
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        }
                                    },
                                    onDragCancel = {
                                        if (isDragging) {
                                            isDragging = false
                                            coroutineScope.launch {
                                                pillScale.animateTo(1.06f, spring(stiffness = stiffness))
                                            }
                                        }
                                    }
                                )
                            }
                        }
                ) {
                    // Sliding Pill Background
                    val pillOffsetDp = with(density) { dragOffset.value.toDp() }
                    
                    // Calculate position-based wobble for an organic sliding jiggle using the user-defined amplitude
                    val currentProgress = if (segWPx > 0) dragOffset.value / segWPx else 0f
                    val wobble = if (isDragging || dragOffset.isRunning) {
                        kotlin.math.sin(currentProgress * kotlin.math.PI * 2.0).toFloat() * wobbleAmplitude
                    } else {
                        0f
                    }
                    val finalScale = (pillScale.value + wobble).coerceIn(0.5f, 1.5f)

                    Box(
                        Modifier
                            .offset(x = pillOffsetDp)
                            .width(segW)
                            .fillMaxSize()
                            .padding(4.dp)
                            .scale(finalScale)
                            .shadow(
                                elevation = if (isDragging) 12.dp else 4.dp,
                                shape = RoundedCornerShape((cornerRadiusDp - 4).coerceAtLeast(0f).dp)
                            )
                            .background(
                                brush = Brush.horizontalGradient(
                                    listOf(startColor, endColor)
                                ),
                                shape = RoundedCornerShape((cornerRadiusDp - 4).coerceAtLeast(0f).dp)
                            )
                    )

                    // Foreground Labels (Tap Selection)
                    Row(Modifier.fillMaxSize()) {
                        items.forEachIndexed { i, label ->
                            val isSelected = i == sel
                            val labelScale = if (isSelected) 1.1f else 1.0f

                            Box(
                                Modifier
                                    .weight(1f)
                                    .fillMaxSize()
                                    .clickable(enabled = !isDragging) {
                                        sel = i
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 15.sp,
                                    modifier = Modifier.graphicsLayer {
                                        scaleX = labelScale
                                        scaleY = labelScale
                                    }
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            Text("Selected: ${items[sel]}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

            Spacer(Modifier.height(28.dp))

            // Playground Controls Card
            Text("Playground Controls", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    
                    // 1. Damping Ratio Slider
                    Column {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Damping Ratio (Bounciness)", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                            Text(
                                text = when {
                                    dampingRatio < 0.4f -> "Highly Jiggly (${((dampingRatio * 100).toInt() / 100f)})"
                                    dampingRatio < 0.7f -> "Medium Bouncy (${((dampingRatio * 100).toInt() / 100f)})"
                                    else -> "Subtle (${((dampingRatio * 100).toInt() / 100f)})"
                                },
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Slider(
                            value = dampingRatio,
                            onValueChange = { dampingRatio = it },
                            valueRange = 0.15f..1.0f
                        )
                    }

                    // 2. Stiffness Slider
                    Column {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Stiffness (Speed)", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                            Text(
                                text = "${stiffness.toInt()} ms",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Slider(
                            value = stiffness,
                            onValueChange = { stiffness = it },
                            valueRange = 80f..1200f
                        )
                    }

                    // 3. Bulge Scale Multiplier Slider
                    Column {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Pill Bulge Scale", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                            Text(
                                text = "${((maxScaleMultiplier * 100).toInt() / 100f)}x",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Slider(
                            value = maxScaleMultiplier,
                            onValueChange = { maxScaleMultiplier = it },
                            valueRange = 1.0f..1.4f
                        )
                    }

                    // 4. Wobble Amplitude Slider
                    Column {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Wobble Amplitude (Sliding Jiggle)", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                            Text(
                                text = "${((wobbleAmplitude * 1000).toInt() / 10f)}%",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Slider(
                            value = wobbleAmplitude,
                            onValueChange = { wobbleAmplitude = it },
                            valueRange = 0.0f..0.08f
                        )
                    }

                    // 5. Corner Radius Slider
                    Column {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Corner Radius", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                            Text(
                                text = "${cornerRadiusDp.toInt()} dp",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Slider(
                            value = cornerRadiusDp,
                            onValueChange = { cornerRadiusDp = it },
                            valueRange = 0f..28f
                        )
                    }

                    // 6. Bar Height Slider
                    Column {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Control Bar Height", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                            Text(
                                text = "${barHeightDp.toInt()} dp",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Slider(
                            value = barHeightDp,
                            onValueChange = { barHeightDp = it },
                            valueRange = 40f..80f
                        )
                    }

                    // 7. Instant Slide Mode Switch
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Instant Slide Mode", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                            Text(
                                text = "Directly drag without needing to long-press first.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(Modifier.width(16.dp))
                        Switch(
                            checked = instantDragMode,
                            onCheckedChange = { instantDragMode = it }
                        )
                    }
                }
            }
        }
    }
}
