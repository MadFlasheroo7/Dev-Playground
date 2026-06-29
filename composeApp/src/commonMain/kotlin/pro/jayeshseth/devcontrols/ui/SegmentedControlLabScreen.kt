package pro.jayeshseth.devcontrols.ui

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

/** Segmented control with a sliding pill indicator that springs to the selected segment, supporting multiple drag modes and haptic feedback. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SegmentedControlLabScreen(onBack: () -> Unit) {
    val items = listOf("Day", "Week", "Month", "Year")
    var sel by remember { mutableIntStateOf(0) }
    var instantDragMode by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Segmented Control") },
                navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }
            )
        },
        modifier = Modifier.fillMaxSize()
    ) { inner ->
        Column(Modifier.padding(inner).fillMaxSize().padding(24.dp)) {
            BoxWithConstraints(
                Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
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
                            animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
                        )
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
                                                pillScale.animateTo(1.12f, spring(stiffness = Spring.StiffnessMedium))
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
                                            coroutineScope.launch {
                                                pillScale.animateTo(1f, spring(stiffness = Spring.StiffnessMedium))
                                            }
                                        }
                                    },
                                    onDragCancel = {
                                        if (isDragging) {
                                            isDragging = false
                                            coroutineScope.launch {
                                                pillScale.animateTo(1f, spring(stiffness = Spring.StiffnessMedium))
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
                                                pillScale.animateTo(1.12f, spring(stiffness = Spring.StiffnessMedium))
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
                                            coroutineScope.launch {
                                                pillScale.animateTo(1f, spring(stiffness = Spring.StiffnessMedium))
                                            }
                                        }
                                    },
                                    onDragCancel = {
                                        if (isDragging) {
                                            isDragging = false
                                            coroutineScope.launch {
                                                pillScale.animateTo(1f, spring(stiffness = Spring.StiffnessMedium))
                                            }
                                        }
                                    }
                                )
                            }
                        }
                ) {
                    // Sliding Pill Background
                    val pillOffsetDp = with(density) { dragOffset.value.toDp() }
                    Box(
                        Modifier
                            .offset(x = pillOffsetDp)
                            .width(segW)
                            .fillMaxSize()
                            .padding(4.dp)
                            .scale(pillScale.value)
                            .shadow(
                                elevation = if (isDragging) 8.dp else 0.dp,
                                shape = RoundedCornerShape(24.dp)
                            )
                            .clip(RoundedCornerShape(24.dp))
                            .background(
                                Brush.horizontalGradient(
                                    listOf(Color(0xFF7F00FF), Color(0xFF00C9FF))
                                )
                            )
                    )

                    // Foreground Labels (Tap Selection)
                    Row(Modifier.fillMaxSize()) {
                        items.forEachIndexed { i, label ->
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
                                    color = if (i == sel) Color.White else MaterialTheme.colorScheme.onSurface,
                                    fontWeight = if (i == sel) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 15.sp
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
            Text("Selected: ${items[sel]}", style = MaterialTheme.typography.titleMedium)

            Spacer(Modifier.height(32.dp))

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Instant Slide Mode", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                        Text(
                            text = if (instantDragMode) "Directly drag the active tab without long pressing." else "Press and hold the active tab briefly before sliding.",
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
