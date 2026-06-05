package pro.jayeshseth.devcontrols.ui

import androidx.compose.animation.core.animateOffsetAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Spotlight / coachmark. A dim overlay covers the UI except a circular hole punched
 * with `BlendMode.Clear` (offscreen layer) around the current target. Tap to advance
 * through targets; the hole animates to each. Demonstrates onboarding-style highlights.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpotlightLabScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Spotlight") },
                navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }
            )
        },
        modifier = Modifier.fillMaxSize()
    ) { inner ->
        // targets as fractions of the content area: (x, y, radius-fraction)
        val targets = remember { listOf(Triple(0.25f, 0.2f, 0.18f), Triple(0.8f, 0.45f, 0.16f), Triple(0.5f, 0.82f, 0.22f)) }
        var step by remember { mutableIntStateOf(0) }
        val active = step < targets.size

        Box(Modifier.padding(inner).fillMaxSize()) {
            // the "app UI" underneath
            Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.SpaceBetween) {
                Box(Modifier.size(120.dp).background(Color(0xFF2193B0), RoundedCornerShape(16.dp)))
                Box(Modifier.align(Alignment.End).size(120.dp).background(Color(0xFFFF5F6D), RoundedCornerShape(16.dp)))
                Box(Modifier.align(Alignment.CenterHorizontally).fillMaxWidth().size(120.dp).background(Color(0xFF92FE9D), RoundedCornerShape(16.dp)))
            }

            if (active) {
                BoxWithSize { w, h ->
                    val (fx, fy, fr) = targets[step]
                    val center by animateOffsetAsState(Offset(fx * w, fy * h), tween(450), label = "c")
                    val radius by animateFloatAsState(fr * w, tween(450), label = "r")
                    Box(
                        Modifier
                            .fillMaxSize()
                            .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                            .clickable { step++ }
                            .drawWithContent {
                                drawRect(Color.Black.copy(alpha = 0.78f))
                                drawCircle(Color.Transparent, radius = radius, center = center, blendMode = BlendMode.Clear)
                            }
                    )
                    Text(
                        "Step ${step + 1} of ${targets.size} — tap anywhere to continue",
                        color = Color.White, fontWeight = FontWeight.Bold,
                        modifier = Modifier.align(Alignment.BottomCenter).padding(32.dp)
                    )
                }
            } else {
                Button(onClick = { step = 0 }, modifier = Modifier.align(Alignment.Center)) { Text("Replay tour") }
            }
        }
    }
}

@Composable
private fun BoxWithSize(content: @Composable (w: Float, h: Float) -> Unit) {
    androidx.compose.foundation.layout.BoxWithConstraints(Modifier.fillMaxSize()) {
        val density = androidx.compose.ui.platform.LocalDensity.current
        val w = with(density) { maxWidth.toPx() }
        val h = with(density) { maxHeight.toPx() }
        content(w, h)
    }
}
