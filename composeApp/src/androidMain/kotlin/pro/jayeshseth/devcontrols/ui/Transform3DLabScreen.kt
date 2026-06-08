package pro.jayeshseth.devcontrols.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs

/**
 * 3D transform playground. A card driven by `Modifier.graphicsLayer` rotationX/Y/Z +
 * `cameraDistance`. Drag the card to rotate it (horizontal → Y axis, vertical → X
 * axis), scrub the sliders, or tap **Flip** to animate a 180° Y rotation with the back
 * face revealed (we swap content past 90°).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Transform3DLabScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("3D Transform") },
                navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }
            )
        },
        modifier = Modifier.fillMaxSize()
    ) { inner ->
        var rotX by remember { mutableFloatStateOf(0f) }
        var rotY by remember { mutableFloatStateOf(0f) }
        var rotZ by remember { mutableFloatStateOf(0f) }
        var cam by remember { mutableFloatStateOf(8f) }
        var flipped by remember { mutableStateOf(false) }
        val flipDeg by animateFloatAsState(if (flipped) 180f else 0f, tween(600), label = "flip")

        Column(
            Modifier.padding(inner).fillMaxSize().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                Modifier.fillMaxWidth().height(320.dp),
                contentAlignment = Alignment.Center
            ) {
                val effectiveY = rotY + flipDeg
                val showingBack = ((abs(effectiveY) % 360f) in 90f..270f)
                Box(
                    Modifier
                        .size(200.dp, 280.dp)
                        .graphicsLayer {
                            rotationX = rotX
                            rotationY = effectiveY
                            rotationZ = rotZ
                            cameraDistance = cam * density
                        }
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            if (showingBack)
                                Brush.linearGradient(listOf(Color(0xFF2C5364), Color(0xFF0F2027)))
                            else
                                Brush.linearGradient(listOf(Color(0xFFFF5F6D), Color(0xFFB24592)))
                        )
                        .pointerInput(Unit) {
                            detectDragGestures { change, drag ->
                                rotY += drag.x * 0.4f
                                rotX -= drag.y * 0.4f
                                change.consume()
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    // counter-flip the back content so it isn't mirror-reversed
                    Box(Modifier.graphicsLayer { if (showingBack) rotationY = 180f }) {
                        Text(
                            if (showingBack) "BACK" else "FRONT",
                            color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = { flipped = !flipped }) { Text("Flip") }
                Button(onClick = { rotX = 0f; rotY = 0f; rotZ = 0f }) { Text("Reset") }
            }

            Slider3D("rotationX", rotX, -180f..180f) { rotX = it }
            Slider3D("rotationY", rotY, -180f..180f) { rotY = it }
            Slider3D("rotationZ", rotZ, -180f..180f) { rotZ = it }
            Slider3D("cameraDistance", cam, 2f..30f) { cam = it }

            Text(
                "Lower cameraDistance = stronger perspective. Drag the card to rotate.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun Slider3D(label: String, value: Float, range: ClosedFloatingPointRange<Float>, onValue: (Float) -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        Text("$label = ${value.toInt()}", style = MaterialTheme.typography.labelMedium)
        Slider(value = value, onValueChange = onValue, valueRange = range)
    }
}
