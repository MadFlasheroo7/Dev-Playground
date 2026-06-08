package pro.jayeshseth.devcontrols.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.dp

/**
 * Animated borders. A rotating sweep-gradient glow border (the "conic gradient" card
 * outline) and a pulsing focus ring — both drawn with `drawWithCache` + a looped
 * transition, no extra views.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnimatedBordersLabScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Animated Borders") },
                navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }
            )
        },
        modifier = Modifier.fillMaxSize()
    ) { inner ->
        val tr = rememberInfiniteTransition(label = "b")
        val angle by tr.animateFloat(0f, 360f, infiniteRepeatable(tween(2800, easing = LinearEasing), RepeatMode.Restart), label = "a")
        val pulse by tr.animateFloat(0f, 1f, infiniteRepeatable(tween(1200, easing = LinearEasing), RepeatMode.Reverse), label = "p")

        Column(Modifier.padding(inner).fillMaxSize().padding(24.dp)) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(150.dp)
                    .drawWithCache {
                        val brush = Brush.sweepGradient(
                            listOf(Color(0xFF00C9FF), Color(0xFF92FE9D), Color(0xFFE100FF), Color(0xFF00C9FF)),
                            center = Offset(size.width / 2f, size.height / 2f)
                        )
                        onDrawBehind {
                            rotate(angle, pivot = Offset(size.width / 2f, size.height / 2f)) {
                                drawRoundRect(
                                    brush = brush,
                                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(48f, 48f),
                                    style = Stroke(width = 10f)
                                )
                            }
                        }
                    },
                contentAlignment = Alignment.Center
            ) { Text("Rotating sweep border", color = MaterialTheme.colorScheme.onSurface) }

            Spacer(Modifier.height(32.dp))

            Box(
                Modifier
                    .fillMaxWidth()
                    .height(150.dp)
                    .drawWithCache {
                        onDrawBehind {
                            val w = 4f + pulse * 10f
                            val alpha = 0.4f + pulse * 0.6f
                            drawRoundRect(
                                color = Color(0xFF18FFFF).copy(alpha = alpha),
                                cornerRadius = androidx.compose.ui.geometry.CornerRadius(36f, 36f),
                                style = Stroke(width = w)
                            )
                        }
                    },
                contentAlignment = Alignment.Center
            ) { Text("Pulsing focus ring", color = MaterialTheme.colorScheme.onSurface) }
        }
    }
}
