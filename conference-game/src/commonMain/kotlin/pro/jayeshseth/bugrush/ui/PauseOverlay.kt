package pro.jayeshseth.bugrush.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Authentic classic arcade CRT pause screen.
 * Flashes "PAUSED" text with arcade keyboard prompts.
 */
@Composable
fun PauseOverlay(
    onResume: () -> Unit,
    onQuitToTitle: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pause_pulse")
    val blinkAlpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.2f,
        animationSpec = infiniteRepeatable(tween(450, easing = LinearEasing), RepeatMode.Reverse),
        label = "blink_alpha"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xCC0E0E2A)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(24.dp)
        ) {
            // Big chunky arcade PAUSED title
            Text(
                text = "PAUSED",
                color = Color(0xFFFF8C00).copy(alpha = blinkAlpha),
                fontSize = 42.sp,
                fontFamily = ArcadeTheme.pixelHeader,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                letterSpacing = 4.sp
            )

            Spacer(modifier = Modifier.height(36.dp))

            // Action 1: Resume
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onResume
                    )
                    .padding(vertical = 10.dp)
            ) {
                Text(
                    text = "> RESUME",
                    color = Color(0xFF00E5FF),
                    fontSize = 16.sp,
                    fontFamily = ArcadeTheme.pixelHeader,
                    fontWeight = FontWeight.Normal,
                    letterSpacing = 2.sp
                )
                Text(
                    text = "  [ESC / ENTER / SPACE]",
                    color = Color(0xFF888899),
                    fontSize = 10.sp,
                    fontFamily = ArcadeTheme.pixelHeader,
                    fontWeight = FontWeight.Normal
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Action 2: Quit
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onQuitToTitle
                    )
                    .padding(vertical = 10.dp)
            ) {
                Text(
                    text = "  QUIT TO TITLE",
                    color = Color(0xFFFF4444),
                    fontSize = 16.sp,
                    fontFamily = ArcadeTheme.pixelHeader,
                    fontWeight = FontWeight.Normal,
                    letterSpacing = 2.sp
                )
                Text(
                    text = "  [Q]",
                    color = Color(0xFF888899),
                    fontSize = 10.sp,
                    fontFamily = ArcadeTheme.pixelHeader,
                    fontWeight = FontWeight.Normal
                )
            }
        }

        // CRT Scanline Overlay
        Canvas(modifier = Modifier.fillMaxSize()) {
            val lineSpacing = 3f
            var y = 0f
            while (y < size.height) {
                drawLine(
                    color = Color.Black.copy(alpha = 0.2f),
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = 1f
                )
                y += lineSpacing
            }
        }
    }
}
