package pro.jayeshseth.devcontrols.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp

/** Path keyframe player — tap to drop waypoints; a dot rides the smoothed path on a loop. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PathKeyframePlayerScreen(onBack: () -> Unit) {
    val points = remember { mutableStateListOf<Offset>() }
    val inf = rememberInfiniteTransition(label = "t")
    val t by inf.animateFloat(0f, 1f, infiniteRepeatable(tween(4000, easing = LinearEasing), RepeatMode.Restart), label = "p")
    val measure = remember { PathMeasure() }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Path Player") }, navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }) },
        modifier = Modifier.fillMaxSize()
    ) { inner ->
        Column(Modifier.padding(inner).fillMaxSize()) {
            Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Tap to add waypoints", Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                OutlinedButton(onClick = { points.clear() }) { Text("Clear") }
            }
            Box(Modifier.fillMaxSize().background(Color(0xFF0C0C14))
                .pointerInput(Unit) { detectTapGestures { points.add(it) } }) {
                Canvas(Modifier.fillMaxSize()) {
                    if (points.size < 2) return@Canvas
                    val path = Path().apply {
                        moveTo(points[0].x, points[0].y)
                        // Catmull-Rom-ish smoothing via quadratic midpoints
                        for (i in 1 until points.size) {
                            val mid = Offset((points[i - 1].x + points[i].x) / 2f, (points[i - 1].y + points[i].y) / 2f)
                            quadraticBezierTo(points[i - 1].x, points[i - 1].y, mid.x, mid.y)
                        }
                        lineTo(points.last().x, points.last().y)
                    }
                    drawPath(path, Color(0x556C7BFF), style = Stroke(width = 4f))
                    points.forEach { drawCircle(Color(0xFF6C7BFF), 8f, it) }
                    measure.setPath(path, false)
                    val len = measure.length
                    if (len > 0f) drawCircle(Color(0xFF22FFAA), 14f, measure.getPosition(t * len))
                }
            }
        }
    }
}
