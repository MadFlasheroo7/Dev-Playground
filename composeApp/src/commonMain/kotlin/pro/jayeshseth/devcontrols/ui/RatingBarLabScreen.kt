package pro.jayeshseth.devcontrols.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Reusable RatingBar component.
 * Supports drag/tap gestures, fractional ratings (decimal steps), custom colors, and sizes.
 */
@Composable
fun RatingBar(
    rating: Float,
    onRatingChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    maxStars: Int = 5,
    starSize: Dp = 36.dp,
    starColor: Color = Color(0xFFFFD740),
    emptyColor: Color = Color(0xFF3A3A44),
    isIndicator: Boolean = false
) {
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()

    // Interactive pointer inputs
    val pointerModifier = if (isIndicator) Modifier else {
        Modifier
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val calculated = (offset.x / size.width * maxStars).coerceIn(0f, maxStars.toFloat())
                    // Round to nearest tenth
                    val rounded = (calculated * 10).toInt() / 10f
                    onRatingChange(rounded)
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                }
            }
            .pointerInput(Unit) {
                detectHorizontalDragGestures { change, _ ->
                    val calculated = (change.position.x / size.width * maxStars).coerceIn(0f, maxStars.toFloat())
                    val rounded = (calculated * 10).toInt() / 10f
                    if (rounded != rating) {
                        onRatingChange(rounded)
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    }
                    change.consume()
                }
            }
    }

    Row(
        modifier = modifier.then(pointerModifier),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (i in 0 until maxStars) {
            val fillFraction = (rating - i).coerceIn(0f, 1f)
            
            // Animation for star scale pop on change
            val scaleAnim = remember { Animatable(1f) }
            LaunchedEffect(rating.toInt() == i + 1) {
                if (rating.toInt() == i + 1) {
                    scaleAnim.animateTo(1.25f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium))
                    scaleAnim.animateTo(1f, spring(stiffness = Spring.StiffnessMedium))
                }
            }

            Box(
                modifier = Modifier
                    .size(starSize)
                    .graphicsLayer {
                        scaleX = scaleAnim.value
                        scaleY = scaleAnim.value
                    }
                    .drawBehind {
                        val cx = size.width / 2f
                        val cy = size.height / 2f
                        val radius = size.width * 0.45f
                        val starShape = starPath(cx, cy, radius)

                        // Draw background empty star
                        drawPath(starShape, emptyColor)

                        // Draw filled portion of star
                        if (fillFraction > 0f) {
                            clipRect(left = 0f, top = 0f, right = size.width * fillFraction, bottom = size.height) {
                                drawPath(starShape, starColor)
                            }
                        }
                    }
            )
        }
    }
}

private fun starPath(cx: Float, cy: Float, r: Float): Path {
    val path = Path()
    for (i in 0 until 10) {
        val rr = if (i % 2 == 0) r else r * 0.45f
        val angle = PI / 5 * i - PI / 2
        val x = cx + (rr * cos(angle)).toFloat()
        val y = cy + (rr * sin(angle)).toFloat()
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()
    return path
}

/** Rating bar screen showcasing the reusable RatingBar component. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RatingBarLabScreen(onBack: () -> Unit) {
    var userRating by remember { mutableFloatStateOf(3.7f) }
    var indicatorRating by remember { mutableFloatStateOf(4.5f) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Rating Bar") },
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
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                "Interactive Rating Bar",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                "Tap or slide across the stars to set a fractional rating.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(Modifier.height(24.dp))

            // Reusable Interactive Rating Bar
            RatingBar(
                rating = userRating,
                onRatingChange = { userRating = it },
                starSize = 44.dp
            )

            Spacer(Modifier.height(16.dp))
            
            Text(
                text = "${userRating} / 5.0",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(Modifier.height(48.dp))

            Text(
                "Read-Only Indicator Mode",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(12.dp))

            // Reusable Indicator Rating Bar (Read-Only)
            RatingBar(
                rating = indicatorRating,
                onRatingChange = {},
                isIndicator = true,
                starSize = 28.dp,
                starColor = Color(0xFFFF9500)
            )
            
            Spacer(Modifier.height(16.dp))
            
            Slider(
                value = indicatorRating,
                onValueChange = { indicatorRating = it },
                valueRange = 0f..5f,
                modifier = Modifier.width(240.dp)
            )
            Text(
                text = "Scrub slider to change indicator: ${((indicatorRating * 10).toInt() / 10f)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
