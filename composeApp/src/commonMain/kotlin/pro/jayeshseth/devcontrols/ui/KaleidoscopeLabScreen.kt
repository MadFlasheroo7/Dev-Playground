package pro.jayeshseth.devcontrols.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import kotlin.math.cos
import kotlin.math.sin

/** Kaleidoscope — a small motif mirrored and rotated into radial symmetry, animated. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KaleidoscopeLabScreen(onBack: () -> Unit) {
    var t by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(Unit) { var s = 0L; while (true) { withFrameNanos { if (s == 0L) s = it; t = (it - s) / 1e9f } } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Kaleidoscope") },
                navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }
            )
        },
        modifier = Modifier.fillMaxSize()
    ) { inner ->
        Column(Modifier.padding(inner).fillMaxSize()) {
            Box(Modifier.fillMaxSize().background(Color.Black).drawBehind {
                val c = Offset(size.width / 2f, size.height / 2f)
                val segs = 12
                val cols = listOf(Color(0xFF18FFFF), Color(0xFFE040FB), Color(0xFF69F0AE), Color(0xFFFFD740))
                for (k in 0 until segs) {
                    rotate(k * 360f / segs + t * 12f, pivot = c) {
                        for (m in 0 until 2) {
                            scale(if (m == 0) 1f else -1f, 1f, pivot = c) {
                                for (i in 0 until 5) {
                                    val r = (i + 1) * size.minDimension * 0.06f * (1.1f + 0.3f * sin(t + i))
                                    val px = c.x + cos(t * 0.7f + i) * r
                                    val py = c.y - r * 1.3f
                                    drawCircle(
                                        Brush.radialGradient(listOf(cols[i % cols.size].copy(alpha = 0.8f), Color.Transparent),
                                            center = Offset(px, py), radius = 60f),
                                        radius = 60f, center = Offset(px, py), blendMode = BlendMode.Plus
                                    )
                                }
                            }
                        }
                    }
                }
            })
        }
    }
}
