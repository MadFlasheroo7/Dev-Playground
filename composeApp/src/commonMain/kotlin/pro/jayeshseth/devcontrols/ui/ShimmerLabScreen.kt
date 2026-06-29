package pro.jayeshseth.devcontrols.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Reusable Shimmer modifier.
 * Automatically adapts its colors to the current theme (light or dark mode) using MaterialTheme colors.
 * Sweeps a diagonal sheen across the component.
 */
fun Modifier.shimmer(
    durationMillis: Int = 1200
): Modifier = composed {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "progress"
    )

    // Automatically adapt colors to the theme
    val baseColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
    val sheenColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)

    drawWithCache {
        val w = size.width
        val sheenWidth = w * 0.6f
        val x = -sheenWidth + progress * (w + 2 * sheenWidth)
        
        val brush = Brush.linearGradient(
            colors = listOf(baseColor, sheenColor, baseColor),
            start = Offset(x, 0f),
            end = Offset(x + sheenWidth, size.height)
        )
        onDrawBehind {
            drawRect(brush)
        }
    }
}

/** Shimmer screen showcasing skeleton loaders transitioning into real content. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShimmerLabScreen(onBack: () -> Unit) {
    var isLoaded by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Shimmer Skeleton") },
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
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Skeleton Loader", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("Toggle loading state to see real content transition in.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(
                    checked = isLoaded,
                    onCheckedChange = { isLoaded = it }
                )
            }

            Spacer(Modifier.height(32.dp))

            // Skeleton list
            repeat(4) { index ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isLoaded) {
                            // Real Content Avatar
                            Box(
                                modifier = Modifier
                                    .size(52.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.linearGradient(
                                            listOf(Color(0xFF7F00FF), Color(0xFF00C9FF))
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = when (index) {
                                        0 -> "🧑‍💻"
                                        1 -> "🎨"
                                        2 -> "🚀"
                                        else -> "💼"
                                    },
                                    fontSize = 22.sp
                                )
                            }

                            // Real Content Text
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = when (index) {
                                        0 -> "Developer Profile"
                                        1 -> "Designer Portfolio"
                                        2 -> "Product Launch"
                                        else -> "Business Inquiry"
                                    },
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                                Text(
                                    text = "All data loaded successfully.",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 13.sp
                                )
                            }
                        } else {
                            // Skeleton Avatar
                            Box(
                                modifier = Modifier
                                    .size(52.dp)
                                    .clip(CircleShape)
                                    .shimmer()
                            )

                            // Skeleton Text
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(0.6f)
                                        .height(16.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .shimmer()
                                )
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(0.9f)
                                        .height(12.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .shimmer()
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
