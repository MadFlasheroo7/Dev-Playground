package pro.jayeshseth.devcontrols.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import kotlin.math.abs

/**
 * Swipeable card stack. The top card follows the finger (translation + rotation keyed
 * to horizontal offset); past a threshold it flings off-screen and the next card
 * surfaces (the one behind is pre-scaled). Under the threshold it springs back.
 */
private val cardColors = listOf(
    Color(0xFFFF5F6D), Color(0xFF2193B0), Color(0xFF92FE9D),
    Color(0xFFFFC371), Color(0xFF7F00FF)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeCardLabScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Swipe Cards") },
                navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }
            )
        },
        modifier = Modifier.fillMaxSize()
    ) { inner ->
        val scope = rememberCoroutineScope()
        var top by remember { mutableIntStateOf(0) }
        val offset = remember { Animatable(Offset.Zero, Offset.VectorConverter) }

        Box(Modifier.padding(inner).fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
            // card behind
            val behind = (top + 1) % cardColors.size
            CardFace(
                cardColors[behind], "Card ${behind + 1}",
                Modifier.fillMaxSize().graphicsLayer { scaleX = 0.92f; scaleY = 0.92f; translationY = 28f }
            )
            // top card
            val threshold = 320f
            CardFace(
                cardColors[top], "Card ${top + 1}",
                Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        translationX = offset.value.x
                        translationY = offset.value.y
                        rotationZ = (offset.value.x / 30f).coerceIn(-25f, 25f)
                    }
                    .pointerInput(top) {
                        detectDragGestures(
                            onDrag = { change, drag ->
                                change.consume()
                                scope.launch { offset.snapTo(offset.value + Offset(drag.x, drag.y)) }
                            },
                            onDragEnd = {
                                if (abs(offset.value.x) > threshold) {
                                    val dir = if (offset.value.x > 0) 1 else -1
                                    scope.launch {
                                        offset.animateTo(Offset(dir * 1600f, offset.value.y), tween(280))
                                        top = (top + 1) % cardColors.size
                                        offset.snapTo(Offset.Zero)
                                    }
                                } else {
                                    scope.launch { offset.animateTo(Offset.Zero, spring()) }
                                }
                            }
                        )
                    }
            )
        }
    }
}

@Composable
private fun CardFace(color: Color, label: String, modifier: Modifier) {
    Box(
        modifier.clip(RoundedCornerShape(28.dp)).background(Brush.verticalGradient(listOf(color, Color.Black))),
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Bold)
    }
}
