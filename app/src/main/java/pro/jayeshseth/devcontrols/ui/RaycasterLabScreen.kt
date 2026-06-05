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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin

private val MAP = arrayOf(
    "1111111111","1000000001","1011110101","1000010001","1010010111",
    "1010000001","1011110101","1000010001","1000000001","1111111111"
)

/** Raycaster — a Wolfenstein-style DDA renderer. Drag left/right to turn, up/down to move. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RaycasterLabScreen(onBack: () -> Unit) {
    var pxp by remember { mutableFloatStateOf(4.5f) }
    var pyp by remember { mutableFloatStateOf(4.5f) }
    var dir by remember { mutableFloatStateOf(0.5f) }
    fun wall(x: Int, y: Int) = if (y in MAP.indices && x in MAP[0].indices) MAP[y][x] == '1' else true

    Scaffold(
        topBar = { TopAppBar(title = { Text("Raycaster") }, navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }) },
        modifier = Modifier.fillMaxSize()
    ) { inner ->
        Column(Modifier.padding(inner).fillMaxSize()) {
            Text("Drag: ↔ turn, ↕ move", Modifier.padding(8.dp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Box(Modifier.fillMaxSize()
                .pointerInput(Unit) {
                    detectDragGestures { _, d ->
                        dir += d.x * 0.005f
                        val nx = pxp + cos(dir) * (-d.y) * 0.01f
                        val ny = pyp + sin(dir) * (-d.y) * 0.01f
                        if (!wall(nx.toInt(), pyp.toInt())) pxp = nx
                        if (!wall(pxp.toInt(), ny.toInt())) pyp = ny
                    }
                }
                .drawBehind {
                    val w = size.width; val h = size.height
                    drawRect(Color(0xFF1A2030), size = Size(w, h / 2)) // ceiling
                    drawRect(Color(0xFF2A2018), topLeft = Offset(0f, h / 2), size = Size(w, h / 2)) // floor
                    val fov = 1.0f
                    val cols = 160
                    val colW = w / cols
                    for (c in 0 until cols) {
                        val ang = dir - fov / 2 + fov * c / cols
                        val rdx = cos(ang); val rdy = sin(ang)
                        var dist = 0f; var hit = false
                        while (!hit && dist < 16f) {
                            dist += 0.02f
                            if (wall((pxp + rdx * dist).toInt(), (pyp + rdy * dist).toInt())) hit = true
                        }
                        val corrected = dist * cos(ang - dir)
                        val lineH = (h / corrected).coerceAtMost(h)
                        val shade = (1f - (corrected / 16f)).coerceIn(0.1f, 1f)
                        drawRect(Color(red = 0.2f * shade, green = 0.6f * shade, blue = 0.9f * shade, alpha = 1f),
                            topLeft = Offset(c * colW, (h - lineH) / 2), size = Size(colW + 1f, lineH))
                    }
                })
        }
    }
}
