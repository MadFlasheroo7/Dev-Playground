package pro.jayeshseth.devcontrols.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

/** Point-cloud globe — a Fibonacci sphere of points, perspective-projected; drag to spin. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PointCloudGlobeLabScreen(onBack: () -> Unit) {
    val n = 800
    val pts = remember {
        Array(n) { i ->
            val y = 1f - (i / (n - 1f)) * 2f
            val r = sqrt(1f - y * y)
            val th = i * 2.399963f
            floatArrayOf(cos(th) * r, y, sin(th) * r)
        }
    }
    var angX by remember { mutableFloatStateOf(0.2f) }
    var angY by remember { mutableFloatStateOf(0f) }
    var auto by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(Unit) { while (true) { withFrameNanos { auto += 0.006f } } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Point-Cloud Globe") },
                navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }
            )
        },
        modifier = Modifier.fillMaxSize()
    ) { inner ->
        Column(Modifier.padding(inner).fillMaxSize()) {
            Box(
                Modifier.fillMaxSize().background(Color(0xFF03030A))
                    .pointerInput(Unit) {
                        detectDragGestures { ch, drag -> angY += drag.x * 0.01f; angX -= drag.y * 0.01f; ch.consume() }
                    }
                    .drawBehind {
                        val ax = angX; val ay = angY + auto
                        val scale = min(size.width, size.height) * 0.4f
                        val cx = size.width / 2f; val cy = size.height / 2f
                        for (v in pts) {
                            val (x, y, z) = rotate(v[0], v[1], v[2], ax, ay)
                            val depth = (z + 1f) / 2f  // 0 far .. 1 near
                            val sx = cx + x * scale
                            val sy = cy + y * scale
                            val c = Color(0xFF18FFFF).copy(alpha = 0.25f + depth * 0.75f)
                            drawCircle(c, radius = 1.5f + depth * 3.5f, center = Offset(sx, sy))
                        }
                    }
            )
        }
    }
}
