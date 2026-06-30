package pro.jayeshseth.devcontrols.ui.visualfx

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Gooey Nav Bar & Magnetic Button screen.
 * - Gooey Nav Bar: Uses custom Bezier path drawing to simulate an organic liquid connector
 *   that stretches and snaps between tabs.
 * - Magnetic Button: Attracts itself to the user's finger/mouse cursor when they get close.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GooeyNavBarScreen(onBack: () -> Unit) {
    val tabs = listOf("🏠", "🔍", "❤️", "🔔", "👤")
    var selected by remember { mutableIntStateOf(0) }
    var previousSelected by remember { mutableIntStateOf(0) }
    val haptic = LocalHapticFeedback.current

    // Animated target position (0f to 4f)
    val animatedTarget = remember { Animatable(0f) }

    LaunchedEffect(selected) {
        animatedTarget.animateTo(
            targetValue = selected.toFloat(),
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        )
        previousSelected = selected
    }

    // Tab-specific gradient colors
    val targetStartColor = when (selected) {
        0 -> Color(0xFFF27121) // Sunset Orange
        1 -> Color(0xFF00F2FE) // Neon Cyan
        2 -> Color(0xFFFF0844) // Love Red
        3 -> Color(0xFFF12711) // Alert Red
        4 -> Color(0xFF7F00FF) // Cyber Purple
        else -> Color(0xFFF27121)
    }
    val targetEndColor = when (selected) {
        0 -> Color(0xFFE94057)
        1 -> Color(0xFF4FACFE)
        2 -> Color(0xFFFFB199)
        3 -> Color(0xFFF5AF19)
        4 -> Color(0xFFE100FF)
        else -> Color(0xFFE94057)
    }

    val startColor by animateColorAsState(targetStartColor, spring(stiffness = Spring.StiffnessLow))
    val endColor by animateColorAsState(targetEndColor, spring(stiffness = Spring.StiffnessLow))

    // Jiggly scale animation
    val scale = remember { Animatable(1f) }
    LaunchedEffect(selected) {
        launch {
            scale.animateTo(0.7f, spring(stiffness = Spring.StiffnessHigh))
            scale.animateTo(1.35f, spring(dampingRatio = Spring.DampingRatioHighBouncy, stiffness = Spring.StiffnessMediumLow))
            scale.animateTo(1.15f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
        }
    }

    // Y-offset animation: sink when moving, pop up beyond bounds when settled
    val isMoving = abs(animatedTarget.value - selected.toFloat()) > 0.05f
    val targetYOffset = if (isMoving) 0f else -26f // Pop up by 26dp
    val yOffset by animateFloatAsState(
        targetValue = targetYOffset,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioHighBouncy,
            stiffness = Spring.StiffnessMediumLow
        )
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Gooey Nav & Magnetic Button") },
                navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }
            )
        },
        modifier = Modifier.fillMaxSize()
    ) { inner ->
        Column(
            modifier = Modifier
                .padding(inner)
                .fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top/Middle Area: Magnetic Button Playground
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        "Magnetic Button",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "Drag or hover near the button to attract it.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(32.dp))
                    MagneticButton()
                }
            }

            // Bottom Area: Gooey Navigation Bar
            val barBgColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    .background(barBgColor)
                    .drawBehind {
                        val slotWidth = size.width / tabs.size
                        val targetX = (animatedTarget.value + 0.5f) * slotWidth
                        val sourceX = (previousSelected + 0.5f) * slotWidth
                        val centerY = size.height / 2f
                        val activeCenterY = centerY + yOffset.dp.toPx()

                        val baseRadius = 24.dp.toPx()
                        val animatedRadius = baseRadius * scale.value
                        val distance = abs(targetX - sourceX)
                        val maxGooeyDistance = slotWidth * 1.5f

                        val gradientBrush = Brush.linearGradient(
                            colors = listOf(startColor, endColor),
                            start = Offset(targetX - animatedRadius, activeCenterY),
                            end = Offset(targetX + animatedRadius, activeCenterY)
                        )

                        if (distance > 1f && distance < maxGooeyDistance) {
                            // Draw Gooey Liquid Connector Path (drawn at centerY since it is moving/sunk)
                            val path = Path()

                            // Calculate dynamic gooey thickness based on distance
                            val progress = distance / maxGooeyDistance
                            val midThickness = baseRadius * (1f - progress * 0.8f)

                            // Control points for the bridge curves
                            val ctrlX = (sourceX + targetX) / 2f
                            val ctrlYTop = centerY - midThickness
                            val ctrlYBottom = centerY + midThickness

                            path.moveTo(sourceX, centerY - baseRadius)
                            // Top curve from source to target
                            path.quadraticTo(ctrlX, ctrlYTop, targetX, centerY - baseRadius)
                            // Right cap
                            path.lineTo(targetX, centerY + baseRadius)
                            // Bottom curve from target to source
                            path.quadraticTo(ctrlX, ctrlYBottom, sourceX, centerY + baseRadius)
                            // Left cap
                            path.lineTo(sourceX, centerY - baseRadius)
                            path.close()

                            drawPath(path, gradientBrush)
                        }

                        // Draw the active pill circle (can go beyond bounds based on activeCenterY)
                        drawCircle(
                            brush = gradientBrush,
                            radius = animatedRadius,
                            center = Offset(targetX, activeCenterY)
                        )
                    }
            ) {
                Row(Modifier.fillMaxSize()) {
                    tabs.forEachIndexed { i, icon ->
                        val isSelected = selected == i
                        val iconYOffset by animateFloatAsState(
                            targetValue = if (isSelected) -26f else 0f,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioHighBouncy,
                                stiffness = Spring.StiffnessMediumLow
                            )
                        )
                        val iconScale by animateFloatAsState(
                            targetValue = if (isSelected) 1.25f else 1f,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioHighBouncy,
                                stiffness = Spring.StiffnessMediumLow
                            )
                        )

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxSize()
                                .clickable {
                                    if (selected != i) {
                                        previousSelected = selected
                                        selected = i
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = icon,
                                fontSize = 22.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                modifier = Modifier.graphicsLayer {
                                    scaleX = iconScale
                                    scaleY = iconScale
                                    translationY = iconYOffset.dp.toPx()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MagneticButton() {
    val coroutineScope = rememberCoroutineScope()
    val offsetX = remember { Animatable(0f) }
    val offsetY = remember { Animatable(0f) }

    Box(
        modifier = Modifier
            .size(200.dp)
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        val density = LocalDensity.current
        val radiusPx = with(density) { 100.dp.toPx() }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { },
                        onDragEnd = {
                            coroutineScope.launch {
                                offsetX.animateTo(0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium))
                            }
                            coroutineScope.launch {
                                offsetY.animateTo(0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium))
                            }
                        },
                        onDragCancel = {
                            coroutineScope.launch {
                                offsetX.animateTo(0f, spring(stiffness = Spring.StiffnessMedium))
                            }
                            coroutineScope.launch {
                                offsetY.animateTo(0f, spring(stiffness = Spring.StiffnessMedium))
                            }
                        }
                    ) { change, dragAmount ->
                        change.consume()
                        coroutineScope.launch {
                            // Attract the button towards the drag coordinates
                            val targetX = (offsetX.value + dragAmount.x).coerceIn(-radiusPx * 0.4f, radiusPx * 0.4f)
                            val targetY = (offsetY.value + dragAmount.y).coerceIn(-radiusPx * 0.4f, radiusPx * 0.4f)
                            offsetX.snapTo(targetX)
                            offsetY.snapTo(targetY)
                        }
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .offset { IntOffset(offsetX.value.toInt(), offsetY.value.toInt()) }
                    .size(80.dp)
                    .shadow(8.dp, CircleShape)
                    .background(
                        Brush.linearGradient(
                            listOf(Color(0xFF7F00FF), Color(0xFF00C9FF))
                        ),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text("🧲", fontSize = 28.sp)
            }
        }
    }
}
