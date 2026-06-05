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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import kotlin.math.sqrt

/** Billiards — elastic circle-circle collisions; drag from the white ball to shoot. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BilliardsLabScreen(onBack: () -> Unit) {
    val count = 10
    val px = remember { FloatArray(count) }; val py = remember { FloatArray(count) }
    val vx = remember { FloatArray(count) }; val vy = remember { FloatArray(count) }
    val cols = remember { listOf(Color.White, Color(0xFFFFD54F), Color(0xFF4FC3F7), Color(0xFFE57373), Color(0xFFBA68C8),
        Color(0xFF81C784), Color(0xFFFF8A65), Color(0xFF90A4AE), Color(0xFFF06292), Color(0xFFA1887F)) }
    val rad = 26f
    var size by remember { mutableStateOf(IntSize.Zero) }
    var seeded by remember { mutableIntStateOf(0) }
    var tick by remember { mutableIntStateOf(0) }
    var aim by remember { mutableStateOf<Offset?>(null) }

    LaunchedEffect(size) {
        if (size == IntSize.Zero) return@LaunchedEffect
        px[0] = size.width * 0.3f; py[0] = size.height * 0.5f
        var k = 1
        for (row in 0 until 4) for (c in 0..row) {
            if (k >= count) break
            px[k] = size.width * 0.65f + row * (rad * 1.8f)
            py[k] = size.height * 0.5f + (c - row / 2f) * (rad * 2.1f); k++
        }
        seeded = 1
        while (true) {
            withFrameNanos {
                for (i in 0 until count) {
                    px[i] += vx[i]; py[i] += vy[i]; vx[i] *= 0.992f; vy[i] *= 0.992f
                    if (px[i] < rad) { px[i] = rad; vx[i] = -vx[i] }
                    if (px[i] > size.width - rad) { px[i] = size.width - rad; vx[i] = -vx[i] }
                    if (py[i] < rad) { py[i] = rad; vy[i] = -vy[i] }
                    if (py[i] > size.height - rad) { py[i] = size.height - rad; vy[i] = -vy[i] }
                }
                for (i in 0 until count) for (j in i + 1 until count) {
                    val dx = px[j] - px[i]; val dy = py[j] - py[i]; val d = sqrt(dx * dx + dy * dy)
                    if (d in 0.01f..(2 * rad)) {
                        val nx = dx / d; val ny = dy / d; val overlap = 2 * rad - d
                        px[i] -= nx * overlap / 2; py[i] -= ny * overlap / 2; px[j] += nx * overlap / 2; py[j] += ny * overlap / 2
                        val rvx = vx[j] - vx[i]; val rvy = vy[j] - vy[i]; val sep = rvx * nx + rvy * ny
                        if (sep < 0) { val imp = sep; vx[i] += imp * nx; vy[i] += imp * ny; vx[j] -= imp * nx; vy[j] -= imp * ny }
                    }
                }
                tick++
            }
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Billiards") }, navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }) },
        modifier = Modifier.fillMaxSize()
    ) { inner ->
        Column(Modifier.padding(inner).fillMaxSize()) {
            Box(Modifier.fillMaxSize().background(Color(0xFF0B3D1F)).onSizeChanged { size = it }
                .pointerInput(seeded) {
                    detectDragGestures(
                        onDrag = { change, _ -> aim = change.position },
                        onDragEnd = {
                            aim?.let { a -> vx[0] = (px[0] - a.x) * 0.12f; vy[0] = (py[0] - a.y) * 0.12f }
                            aim = null
                        }
                    )
                }
                .drawBehind {
                    tick
                    if (seeded == 0) return@drawBehind
                    aim?.let { drawLine(Color(0x88FFFFFF), Offset(px[0], py[0]), it, 3f) }
                    for (i in 0 until count) { drawCircle(cols[i], rad, Offset(px[i], py[i])); drawCircle(Color(0x33000000), rad, Offset(px[i], py[i]), style = androidx.compose.ui.graphics.drawscope.Stroke(2f)) }
                })
        }
    }
}
