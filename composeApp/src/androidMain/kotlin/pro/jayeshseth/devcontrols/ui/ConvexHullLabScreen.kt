package pro.jayeshseth.devcontrols.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.runtime.derivedStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp

/** Convex hull — tap to drop points; the hull (Andrew's monotone chain) wraps them. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConvexHullLabScreen(onBack: () -> Unit) {
    val pts = remember { mutableStateListOf<Offset>() }
    val hull by remember { derivedStateOf { convexHull(pts) } }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Convex Hull") }, navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }) },
        modifier = Modifier.fillMaxSize()
    ) { inner ->
        Column(Modifier.padding(inner).fillMaxSize()) {
            Row(Modifier.fillMaxWidth().padding(12.dp)) {
                Text("Tap to add points (${pts.size})", Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                OutlinedButton(onClick = { pts.clear() }) { Text("Clear") }
            }
            Box(Modifier.fillMaxSize().background(Color(0xFF0C0C14))
                .pointerInput(Unit) { detectTapGestures { pts.add(it) } }
                .drawBehind {
                    if (hull.size >= 2) {
                        val path = Path().apply {
                            moveTo(hull[0].x, hull[0].y)
                            hull.drop(1).forEach { lineTo(it.x, it.y) }
                            close()
                        }
                        drawPath(path, Color(0x3322FFAA))
                        drawPath(path, Color(0xFF22FFAA), style = Stroke(4f))
                    }
                    pts.forEach { drawCircle(Color(0xFFFF6CAB), 9f, it) }
                })
        }
    }
}

/** Andrew's monotone chain convex hull. */
private fun convexHull(points: List<Offset>): List<Offset> {
    if (points.size < 3) return points.toList()
    val p = points.sortedWith(compareBy({ it.x }, { it.y }))
    fun cross(o: Offset, a: Offset, b: Offset) = (a.x - o.x) * (b.y - o.y) - (a.y - o.y) * (b.x - o.x)
    val lower = ArrayList<Offset>()
    for (pt in p) {
        while (lower.size >= 2 && cross(lower[lower.size - 2], lower[lower.size - 1], pt) <= 0) lower.removeAt(lower.size - 1)
        lower.add(pt)
    }
    val upper = ArrayList<Offset>()
    for (pt in p.asReversed()) {
        while (upper.size >= 2 && cross(upper[upper.size - 2], upper[upper.size - 1], pt) <= 0) upper.removeAt(upper.size - 1)
        upper.add(pt)
    }
    lower.removeAt(lower.size - 1); upper.removeAt(upper.size - 1)
    return lower + upper
}
