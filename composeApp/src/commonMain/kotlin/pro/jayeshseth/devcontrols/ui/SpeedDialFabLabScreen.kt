package pro.jayeshseth.devcontrols.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

enum class FabLayout {
    VERTICAL, HORIZONTAL, RADIAL
}

enum class FabAnimation {
    SCALE, SLIDE, STAGGERED_BOUNCE, ROTATE
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpeedDialFabLabScreen(onBack: () -> Unit) {
    var open by remember { mutableStateOf(false) }
    var selectedLayout by remember { mutableStateOf(FabLayout.VERTICAL) }
    var selectedAnim by remember { mutableStateOf(FabAnimation.STAGGERED_BOUNCE) }

    val rot by animateFloatAsState(if (open) 45f else 0f, label = "rot")
    val items = listOf(
        "Share" to Color(0xFF00C9FF),
        "Edit" to Color(0xFF92FE9D),
        "Delete" to Color(0xFFFF5F6D)
    )

    // Separate Animatable values for each item to support staggered delays
    val itemProgress = remember(items.size) { List(items.size) { Animatable(0f) } }

    LaunchedEffect(open, selectedAnim) {
        itemProgress.forEachIndexed { i, anim ->
            launch {
                if (open) {
                    when (selectedAnim) {
                        FabAnimation.STAGGERED_BOUNCE -> {
                            delay(i * 80L)
                            anim.animateTo(
                                1f,
                                spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness = Spring.StiffnessLow
                                )
                            )
                        }
                        FabAnimation.SCALE -> {
                            anim.animateTo(1f, spring(stiffness = Spring.StiffnessMedium))
                        }
                        FabAnimation.SLIDE -> {
                            anim.animateTo(1f, spring(stiffness = Spring.StiffnessMediumLow))
                        }
                        FabAnimation.ROTATE -> {
                            anim.animateTo(1f, tween(durationMillis = 350))
                        }
                    }
                } else {
                    when (selectedAnim) {
                        FabAnimation.STAGGERED_BOUNCE -> {
                            delay((items.size - 1 - i) * 60L)
                            anim.animateTo(0f, spring(stiffness = Spring.StiffnessMedium))
                        }
                        else -> {
                            anim.animateTo(0f, spring(stiffness = Spring.StiffnessMedium))
                        }
                    }
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Speed-Dial FAB") },
                navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }
            )
        },
        modifier = Modifier.fillMaxSize()
    ) { inner ->
        Box(
            Modifier
                .padding(inner)
                .fillMaxSize()
        ) {
            // Settings Panel at the top
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .align(Alignment.TopCenter),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Layout Selector Card
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Layout Style", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            FabLayout.entries.forEach { layout ->
                                FilterChip(
                                    selected = selectedLayout == layout,
                                    onClick = {
                                        selectedLayout = layout
                                        open = false
                                    },
                                    label = { Text(layout.name.lowercase().replaceFirstChar { it.uppercase() }) }
                                )
                            }
                        }
                    }
                }

                // Animation Selector Card
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Animation Style", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    FilterChip(
                                        selected = selectedAnim == FabAnimation.STAGGERED_BOUNCE,
                                        onClick = {
                                            selectedAnim = FabAnimation.STAGGERED_BOUNCE
                                            open = false
                                        },
                                        label = { Text("Staggered Bounce") }
                                    )
                                    FilterChip(
                                        selected = selectedAnim == FabAnimation.SCALE,
                                        onClick = {
                                            selectedAnim = FabAnimation.SCALE
                                            open = false
                                        },
                                        label = { Text("Scale & Fade") }
                                    )
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    FilterChip(
                                        selected = selectedAnim == FabAnimation.SLIDE,
                                        onClick = {
                                            selectedAnim = FabAnimation.SLIDE
                                            open = false
                                        },
                                        label = { Text("Slide In") }
                                    )
                                    FilterChip(
                                        selected = selectedAnim == FabAnimation.ROTATE,
                                        onClick = {
                                            selectedAnim = FabAnimation.ROTATE
                                            open = false
                                        },
                                        label = { Text("Rotate & Spin") }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Interactive FAB Area
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .align(Alignment.BottomEnd)
            ) {
                val density = LocalDensity.current
                val spacingPx = with(density) { 72.dp.toPx() }
                val radialRadiusPx = with(density) { 110.dp.toPx() }

                items.forEachIndexed { i, (label, color) ->
                    val progress = itemProgress[i].value

                    // Calculate target translation coordinates based on selected layout
                    val (targetX, targetY) = when (selectedLayout) {
                        FabLayout.VERTICAL -> {
                            0f to -(i + 1) * spacingPx
                        }
                        FabLayout.HORIZONTAL -> {
                            -(i + 1) * spacingPx to 0f
                        }
                        FabLayout.RADIAL -> {
                            // Arc from 180 (left) to 270 (up) degrees
                            val angleRad = (180.0 + i * (90.0 / (items.size - 1))) * PI / 180.0
                            val tx = radialRadiusPx * cos(angleRad).toFloat()
                            val ty = radialRadiusPx * sin(angleRad).toFloat()
                            tx to ty
                        }
                    }

                    // Apply animation type transitions
                    val currentX = when (selectedAnim) {
                        FabAnimation.SCALE -> targetX
                        else -> targetX * progress
                    }
                    val currentY = when (selectedAnim) {
                        FabAnimation.SCALE -> targetY
                        else -> targetY * progress
                    }

                    val scaleVal = when (selectedAnim) {
                        FabAnimation.SCALE, FabAnimation.STAGGERED_BOUNCE -> progress
                        else -> 1f
                    }

                    val rotateVal = when (selectedAnim) {
                        FabAnimation.ROTATE -> (1f - progress) * -360f
                        else -> 0f
                    }

                    if (progress > 0.01f) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .offset { IntOffset(currentX.toInt(), currentY.toInt()) }
                                .padding(24.dp)
                                .scale(scaleVal)
                                .rotate(rotateVal)
                                .alpha(progress),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Show labels only in vertical mode or when space permits
                                if (selectedLayout == FabLayout.VERTICAL) {
                                    Text(
                                        text = label,
                                        color = Color.White,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier
                                            .background(Color(0xCC222230), RoundedCornerShape(8.dp))
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                                Box(
                                    Modifier
                                        .size(48.dp)
                                        .shadow(4.dp, CircleShape)
                                        .background(color, CircleShape)
                                        .clickable { open = false },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(label.first().toString(), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                }
                            }
                        }
                    }
                }

                // Main Trigger FAB
                Box(
                    Modifier
                        .align(Alignment.BottomEnd)
                        .padding(24.dp)
                        .size(60.dp)
                        .shadow(6.dp, CircleShape)
                        .background(Color(0xFF7F00FF), CircleShape)
                        .clickable { open = !open },
                    contentAlignment = Alignment.Center
                ) {
                    Text("+", color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Bold, modifier = Modifier.rotate(rot))
                }
            }
        }
    }
}
