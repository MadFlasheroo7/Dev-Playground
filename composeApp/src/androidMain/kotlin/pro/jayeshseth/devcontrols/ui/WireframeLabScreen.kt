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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/** Software 3D wireframe cube — perspective-projected, auto-spins, drag to orbit. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WireframeLabScreen(onBack: () -> Unit) {
    var angX by remember { mutableFloatStateOf(0.4f) }
    var angY by remember { mutableFloatStateOf(0.4f) }
    var auto by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(Unit) { while (true) { withFrameNanos { auto += 0.01f } } }

    val verts = remember {
        arrayOf(
            floatArrayOf(-1f, -1f, -1f), floatArrayOf(1f, -1f, -1f), floatArrayOf(1f, 1f, -1f), floatArrayOf(-1f, 1f, -1f),
            floatArrayOf(-1f, -1f, 1f), floatArrayOf(1f, -1f, 1f), floatArrayOf(1f, 1f, 1f), floatArrayOf(-1f, 1f, 1f)
        )
    }
    val edges = remember {
        arrayOf(0 to 1, 1 to 2, 2 to 3, 3 to 0, 4 to 5, 5 to 6, 6 to 7, 7 to 4, 0 to 4, 1 to 5, 2 to 6, 3 to 7)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Wireframe 3D") },
                navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }
            )
        },
        modifier = Modifier.fillMaxSize()
    ) { inner ->
        Column(Modifier.padding(inner).fillMaxSize()) {
            Box(
                Modifier.fillMaxSize().background(Color(0xFF05050C))
                    .pointerInput(Unit) {
                        detectDragGestures { ch, drag -> angY += drag.x * 0.01f; angX -= drag.y * 0.01f; ch.consume() }
                    }
                    .drawBehind {
                        val ax = angX; val ay = angY + auto
                        val scale = min(size.width, size.height) * 0.28f
                        val cx = size.width / 2f; val cy = size.height / 2f
                        val proj = verts.map { v ->
                            val (x, y, z) = rotate(v[0], v[1], v[2], ax, ay)
                            val zc = z + 4f
                            Offset(cx + x / zc * scale * 2.2f, cy + y / zc * scale * 2.2f)
                        }
                        for ((a, b) in edges) {
                            drawLine(Color(0xFF18FFFF), proj[a], proj[b], strokeWidth = 4f, cap = StrokeCap.Round)
                        }
                        proj.forEach { drawCircle(Color(0xFFFF5F6D), 6f, it) }
                    }
            )
        }
    }
}

internal fun rotate(x0: Float, y0: Float, z0: Float, ax: Float, ay: Float): Triple<Float, Float, Float> {
    val x1 = x0 * cos(ay) + z0 * sin(ay)
    val z1 = -x0 * sin(ay) + z0 * cos(ay)
    val y2 = y0 * cos(ax) - z1 * sin(ax)
    val z2 = y0 * sin(ax) + z1 * cos(ax)
    return Triple(x1, y2, z2)
}
