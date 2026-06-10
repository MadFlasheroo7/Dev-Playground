package pro.jayeshseth.devcontrols.ui

import pro.jayeshseth.devcontrols.util.fmt
import pro.jayeshseth.devcontrols.util.hex2
import pro.jayeshseth.devcontrols.util.pad
import pro.jayeshseth.devcontrols.util.nowMillis

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

/**
 * Physics & gesture motion lab. A ball driven by an `Animatable<Offset>`:
 *  - **Tap** anywhere → the ball springs to that point using the tuned spring.
 *  - **Drag** the ball → it follows your finger; on release it springs back to the
 *    last tap target with the chosen damping/stiffness.
 * The two sliders are a live spring tuner — drop the damping ratio to see overshoot,
 * raise stiffness to see it snap.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhysicsMotionLabScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Physics Motion") },
                navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }
            )
        },
        modifier = Modifier.fillMaxSize()
    ) { inner ->
        val scope = rememberCoroutineScope()
        val density = LocalDensity.current
        var damping by remember { mutableFloatStateOf(0.5f) }
        var stiffness by remember { mutableFloatStateOf(Spring.StiffnessMedium) }
        val ballPx = with(density) { 56.dp.toPx() }

        Column(Modifier.padding(inner).fillMaxSize().padding(16.dp)) {
            BoxWithConstraints(
                Modifier
                    .fillMaxWidth()
                    .height(380.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                val maxX = with(density) { maxWidth.toPx() } - ballPx
                val maxY = with(density) { maxHeight.toPx() } - ballPx
                val center = Offset(maxX / 2f, maxY / 2f)
                val pos = remember { Animatable(center, Offset.VectorConverter) }
                val springSpec = spring<Offset>(dampingRatio = damping, stiffness = stiffness)

                Box(
                    Modifier
                        .fillMaxSize()
                        .pointerInput(damping, stiffness) {
                            detectTapGestures { tap ->
                                val t = Offset(
                                    (tap.x - ballPx / 2).coerceIn(0f, maxX),
                                    (tap.y - ballPx / 2).coerceIn(0f, maxY)
                                )
                                scope.launch { pos.animateTo(t, springSpec) }
                            }
                        }
                ) {
                    Box(
                        Modifier
                            .size(56.dp)
                            .offsetPx(pos.value)
                            .background(MaterialTheme.colorScheme.primary, CircleShape)
                            .pointerInput(damping, stiffness) {
                                detectDragGestures(
                                    onDragStart = { scope.launch { pos.stop() } },
                                    onDrag = { change, drag ->
                                        change.consume()
                                        val next = Offset(
                                            (pos.value.x + drag.x).coerceIn(0f, maxX),
                                            (pos.value.y + drag.y).coerceIn(0f, maxY)
                                        )
                                        scope.launch { pos.snapTo(next) }
                                    },
                                    onDragEnd = {
                                        scope.launch { pos.animateTo(center, springSpec) }
                                    }
                                )
                            }
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
            Text("dampingRatio = ${fmt(damping, 2)}  (low = bouncy)", style = MaterialTheme.typography.labelMedium)
            Slider(value = damping, onValueChange = { damping = it }, valueRange = 0.1f..1f)
            Text("stiffness = ${stiffness.toInt()}  (high = snappy)", style = MaterialTheme.typography.labelMedium)
            Slider(value = stiffness, onValueChange = { stiffness = it }, valueRange = 20f..2000f)
            Text(
                "Tap to spring the ball there; drag it and let go to spring back to center.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun Modifier.offsetPx(o: Offset): Modifier =
    this.offset { androidx.compose.ui.unit.IntOffset(o.x.toInt(), o.y.toInt()) }
