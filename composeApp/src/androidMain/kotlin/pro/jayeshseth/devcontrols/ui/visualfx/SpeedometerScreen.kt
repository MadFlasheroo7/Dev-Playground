package pro.jayeshseth.devcontrols.ui.visualfx

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Supercar-style Speedometer.
 * Renders an arced dial gauge with a sweeping neon needle, glowing active progress track,
 * major ticks, and a digital HUD readout.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpeedometerScreen(onBack: () -> Unit) {
    val maxSpeed = 240f
    var targetSpeed by remember { mutableFloatStateOf(0f) }
    
    // Smooth needle physics
    val speed by animateFloatAsState(
        targetValue = targetSpeed,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "speed"
    )

    val startAngle = 150f   // Bottom-left start
    val sweepAngle = 240f   // 240-degree sweep arc

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Speedometer Gauge") },
                navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }
            )
        },
        modifier = Modifier.fillMaxSize()
    ) { inner ->
        Column(
            Modifier
                .padding(inner)
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Supercar Digital Cockpit",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                "Scrub the throttle slider below to accelerate and watch the needle sweep.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(32.dp))

            // Speedometer Gauge Container
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .drawBehind {
                        val cx = size.width / 2f
                        val cy = size.height / 2f
                        val radius = size.minDimension / 2f * 0.85f
                        val strokeWidth = radius * 0.12f

                        // 1. Draw Inactive Background Track
                        drawArc(
                            color = Color(0xFF1E2030),
                            startAngle = startAngle,
                            sweepAngle = sweepAngle,
                            useCenter = false,
                            topLeft = Offset(cx - radius, cy - radius),
                            size = Size(radius * 2, radius * 2),
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                        )

                        // 2. Draw Glowing Active Speed Arc
                        val fraction = (speed / maxSpeed).coerceIn(0f, 1f)
                        if (fraction > 0f) {
                            drawArc(
                                brush = Brush.sweepGradient(
                                    colors = listOf(
                                        Color(0xFF00FF87), // Green
                                        Color(0xFF60EFFF), // Cyan
                                        Color(0xFF00C9FF), // Blue
                                        Color(0xFFFF007F)  // Pink/Red at high speeds
                                    ),
                                    center = Offset(cx, cy)
                                ),
                                startAngle = startAngle,
                                sweepAngle = sweepAngle * fraction,
                                useCenter = false,
                                topLeft = Offset(cx - radius, cy - radius),
                                size = Size(radius * 2, radius * 2),
                                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                            )
                        }

                        // 3. Draw Dial Ticks
                        val totalTicks = 24
                        for (i in 0..totalTicks) {
                            val angleRad = Math.toRadians((startAngle + sweepAngle * i / totalTicks).toDouble())
                            val isMajor = i % 4 == 0
                            val tickLength = if (isMajor) radius * 0.12f else radius * 0.06f
                            val tickWidth = if (isMajor) 3.dp.toPx() else 1.5.dp.toPx()

                            val rStart = radius - strokeWidth / 2f - 4.dp.toPx()
                            val rEnd = rStart - tickLength

                            val startX = cx + cos(angleRad).toFloat() * rStart
                            val startY = cy + sin(angleRad).toFloat() * rStart
                            val endX = cx + cos(angleRad).toFloat() * rEnd
                            val endY = cy + sin(angleRad).toFloat() * rEnd

                            drawHighlightLine(
                                start = Offset(startX, startY),
                                end = Offset(endX, endY),
                                color = if (isMajor) Color.White.copy(alpha = 0.8f) else Color.White.copy(alpha = 0.4f),
                                strokeWidth = tickWidth
                            )
                        }

                        // 4. Draw Sweeping Needle
                        val needleAngleRad = Math.toRadians((startAngle + sweepAngle * fraction).toDouble())
                        val needleLength = radius * 0.85f
                        val needleTip = Offset(
                            x = cx + cos(needleAngleRad).toFloat() * needleLength,
                            y = cy + sin(needleAngleRad).toFloat() * needleLength
                        )
                        val needleTail = Offset(
                            x = cx - cos(needleAngleRad).toFloat() * (radius * 0.15f),
                            y = cy - sin(needleAngleRad).toFloat() * (radius * 0.15f)
                        )

                        // Draw needle line with neon red accent
                        drawLine(
                            color = Color(0xFFFF2A55),
                            start = needleTail,
                            end = needleTip,
                            strokeWidth = 4.dp.toPx(),
                            cap = StrokeCap.Round
                        )

                        // Center cap
                        drawCircle(
                            color = Color(0xFFFF2A55),
                            radius = 12.dp.toPx(),
                            center = Offset(cx, cy)
                        )
                        drawCircle(
                            color = Color(0xFF0A0B10),
                            radius = 6.dp.toPx(),
                            center = Offset(cx, cy)
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                // Digital HUD readout in the center
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(top = 40.dp)
                ) {
                    Text(
                        text = "${speed.toInt()}",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 64.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "KM/H",
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp,
                        fontSize = 14.sp
                    )
                }
            }

            Spacer(Modifier.height(40.dp))

            // Throttle Control Slider
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Throttle Control",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.height(8.dp))
                    Slider(
                        value = targetSpeed,
                        onValueChange = { targetSpeed = it },
                        valueRange = 0f..maxSpeed,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

// Helper extension to draw anti-aliased lines cleanly
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawHighlightLine(
    start: Offset,
    end: Offset,
    color: Color,
    strokeWidth: Float
) {
    drawLine(
        color = color,
        start = start,
        end = end,
        strokeWidth = strokeWidth,
        cap = StrokeCap.Round
    )
}
