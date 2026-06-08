package pro.jayeshseth.devcontrols.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Scratch-to-reveal lab. The hidden prize is drawn underneath; a grey overlay sits on
 * top in an **offscreen** graphics layer. Dragging strokes a path with
 * `BlendMode.Clear`, which (because the layer composites offscreen) punches transparent
 * holes in just the overlay — revealing the content beneath. Tap Reset to re-cover.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScratchRevealLabScreen(onBack: () -> Unit) {
    val scratch = remember { Path() }
    var ticks by remember { mutableIntStateOf(0) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Scratch to Reveal") },
                navigationIcon = { TextButton(onClick = onBack) { Text("Back") } },
                actions = { TextButton(onClick = { scratch.reset(); ticks++ }) { Text("Reset") } }
            )
        },
        modifier = Modifier.fillMaxSize()
    ) { inner ->
        Box(Modifier.padding(inner).fillMaxSize()) {
            // hidden content underneath
            Box(
                Modifier.fillMaxSize().background(
                    Brush.linearGradient(listOf(Color(0xFFFFD740), Color(0xFFFF6E40), Color(0xFFE040FB)))
                ),
                contentAlignment = Alignment.Center
            ) {
                Text("🎉 YOU WIN 🎉", color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Bold)
            }
            // scratch overlay
            Box(
                Modifier
                    .fillMaxSize()
                    .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = { scratch.moveTo(it.x, it.y); ticks++ },
                            onDrag = { change, _ -> scratch.lineTo(change.position.x, change.position.y); ticks++; change.consume() }
                        )
                    }
                    .drawWithContent {
                        ticks // invalidate as the path grows
                        drawRect(Color(0xFF9E9E9E))
                        drawPath(
                            scratch, Color.Transparent,
                            style = Stroke(width = 90f, cap = StrokeCap.Round, join = StrokeJoin.Round),
                            blendMode = BlendMode.Clear
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                Text("scratch here", color = Color.White)
            }
        }
    }
}
