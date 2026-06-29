package pro.jayeshseth.devcontrols.ui.visualfx

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private enum class IslandState {
    COMPACT,      // Simple camera pill
    NOW_PLAYING,  // Expanded media player with animated waveform
    SPLIT_TIMER,  // Apple-style split pill (Left: icon, Right: timer)
    CHARGING      // Wide battery charging alert (disappears after 3s)
}

/** Dynamic Island — a morphing pill that springs between compact, expanded, split, and alert states. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DynamicIslandScreen(onBack: () -> Unit) {
    var islandState by remember { mutableStateOf(IslandState.COMPACT) }
    var isPlaying by remember { mutableStateOf(true) }
    var timerSeconds by remember { mutableIntStateOf(899) } // 14:59
    val coroutineScope = rememberCoroutineScope()

    // Countdown Timer Logic
    LaunchedEffect(isPlaying, islandState) {
        while (isPlaying && islandState == IslandState.SPLIT_TIMER && timerSeconds > 0) {
            delay(1000)
            timerSeconds--
        }
    }

    // Auto-dismiss charging state after 3 seconds
    LaunchedEffect(islandState) {
        if (islandState == IslandState.CHARGING) {
            delay(3000)
            islandState = IslandState.COMPACT
        }
    }

    // Animating dimensions with spring physics
    val springSpec = spring<androidx.compose.ui.unit.Dp>(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessLow
    )

    val islandWidth by animateDpAsState(
        targetValue = when (islandState) {
            IslandState.COMPACT -> 140.dp
            IslandState.NOW_PLAYING -> 340.dp
            IslandState.SPLIT_TIMER -> 200.dp // Right pill width
            IslandState.CHARGING -> 280.dp
        },
        animationSpec = springSpec,
        label = "width"
    )

    val islandHeight by animateDpAsState(
        targetValue = when (islandState) {
            IslandState.COMPACT -> 38.dp
            IslandState.NOW_PLAYING -> 170.dp
            IslandState.SPLIT_TIMER -> 38.dp
            IslandState.CHARGING -> 56.dp
        },
        animationSpec = springSpec,
        label = "height"
    )

    // Left pill width for Split Timer mode (animates from 0.dp to 38.dp)
    val leftPillWidth by animateDpAsState(
        targetValue = if (islandState == IslandState.SPLIT_TIMER) 38.dp else 0.dp,
        animationSpec = springSpec,
        label = "leftPillWidth"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Dynamic Island") },
                navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }
            )
        },
        modifier = Modifier.fillMaxSize()
    ) { inner ->
        Column(
            modifier = Modifier
                .padding(inner)
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(20.dp))

            // The Dynamic Island Container
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Left Pill (Only visible and expanded during SPLIT_TIMER state)
                if (leftPillWidth > 0.dp) {
                    Box(
                        modifier = Modifier
                            .size(leftPillWidth, 38.dp)
                            .clip(CircleShape)
                            .background(Color.Black),
                        contentAlignment = Alignment.Center
                    ) {
                        if (leftPillWidth >= 30.dp) {
                            Text("⏱️", fontSize = 16.sp)
                        }
                    }
                    Spacer(Modifier.width(8.dp))
                }

                // Main Pill
                Box(
                    modifier = Modifier
                        .size(islandWidth, islandHeight)
                        .clip(RoundedCornerShape(32.dp))
                        .background(Color.Black)
                        .clickable {
                            // Cycle through states on tap
                            islandState = when (islandState) {
                                IslandState.COMPACT -> IslandState.NOW_PLAYING
                                IslandState.NOW_PLAYING -> IslandState.SPLIT_TIMER
                                IslandState.SPLIT_TIMER -> IslandState.CHARGING
                                IslandState.CHARGING -> IslandState.COMPACT
                            }
                        }
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    AnimatedContent(
                        targetState = islandState,
                        transitionSpec = {
                            fadeIn(animationSpec = tween(150, delayMillis = 80)) togetherWith
                                    fadeOut(animationSpec = tween(150))
                        },
                        label = "content"
                    ) { state ->
                        when (state) {
                            IslandState.COMPACT -> {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    // Simulated Camera lens and sensor dots
                                    Box(Modifier.size(11.dp).clip(CircleShape).background(Color(0xFF1E1E24)))
                                    Box(Modifier.size(6.dp).clip(CircleShape).background(Color(0xFF2E2E38)))
                                    Box(Modifier.size(11.dp).clip(CircleShape).background(Color(0xFF1E1E24)))
                                }
                            }
                            IslandState.NOW_PLAYING -> {
                                Column(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        // Album art placeholder
                                        Box(
                                            modifier = Modifier
                                                .size(52.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(
                                                    Brush.linearGradient(
                                                        listOf(Color(0xFF7F00FF), Color(0xFF00C9FF))
                                                    )
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text("🎵", fontSize = 24.sp)
                                        }

                                        Spacer(Modifier.width(12.dp))

                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                "Midnight City",
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 15.sp,
                                                maxLines = 1
                                            )
                                            Text(
                                                "M83",
                                                color = Color.Gray,
                                                fontSize = 13.sp
                                            )
                                        }

                                        // Animated Waveform
                                        WaveformVisualizer(isPlaying = isPlaying)
                                    }

                                    // Media Controls
                                    Row(
                                        horizontalArrangement = Arrangement.SpaceEvenly,
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("⏮", color = Color.White, fontSize = 22.sp, modifier = Modifier.clickable { })
                                        Text(
                                            text = if (isPlaying) "⏸" else "▶",
                                            color = Color.White,
                                            fontSize = 24.sp,
                                            modifier = Modifier.clickable { isPlaying = !isPlaying }
                                        )
                                        Text("⏭", color = Color.White, fontSize = 22.sp, modifier = Modifier.clickable { })
                                    }
                                }
                            }
                            IslandState.SPLIT_TIMER -> {
                                val minutes = timerSeconds / 60
                                val seconds = timerSeconds % 60
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}",
                                        color = Color(0xFFFF9500),
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 14.sp
                                    )
                                    Text("Timer", color = Color.White, fontSize = 13.sp)
                                }
                            }
                            IslandState.CHARGING -> {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Charging", color = Color.Gray, fontSize = 14.sp)
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("⚡", color = Color(0xFF30D158), fontSize = 16.sp)
                                        Spacer(Modifier.width(4.dp))
                                        Text("82%", color = Color(0xFF30D158), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(48.dp))

            // State Simulator Buttons
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Simulate States", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Button(
                            onClick = { islandState = IslandState.COMPACT },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = if (islandState == IslandState.COMPACT) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary)
                        ) {
                            Text("Compact")
                        }
                        Button(
                            onClick = { islandState = IslandState.NOW_PLAYING },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = if (islandState == IslandState.NOW_PLAYING) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary)
                        ) {
                            Text("Music")
                        }
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Button(
                            onClick = { islandState = IslandState.SPLIT_TIMER },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = if (islandState == IslandState.SPLIT_TIMER) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary)
                        ) {
                            Text("Split Timer")
                        }
                        Button(
                            onClick = { islandState = IslandState.CHARGING },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = if (islandState == IslandState.CHARGING) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary)
                        ) {
                            Text("Charge (3s)")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WaveformVisualizer(isPlaying: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "waveform")

    Row(
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val heights = listOf(0.3f, 0.7f, 1.0f, 0.5f, 0.8f)
        heights.forEachIndexed { index, baseHeight ->
            val animatedHeight by if (isPlaying) {
                infiniteTransition.animateFloat(
                    initialValue = 0.2f,
                    targetValue = baseHeight,
                    animationSpec = infiniteRepeatable(
                        animation = tween(durationMillis = 300 + index * 100, easing = LinearEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "bar_$index"
                )
            } else {
                remember { mutableStateOf(0.2f) }
            }

            Box(
                modifier = Modifier
                    .size(4.dp, 24.dp)
                    .scale(scaleX = 1f, scaleY = animatedHeight)
                    .clip(CircleShape)
                    .background(Color(0xFF30D158))
            )
        }
    }
}
