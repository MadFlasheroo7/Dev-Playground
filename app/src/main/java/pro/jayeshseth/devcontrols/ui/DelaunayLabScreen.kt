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
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp

private class Tri(val a: Offset, val b: Offset, val c: Offset) {
    // circumcircle
    val cx: Float; val cy: Float; val r2: Float
    init {
        val ad = a.x * a.x + a.y * a.y; val bd = b.x * b.x + b.y * b.y; val cd = c.x * c.x + c.y * c.y
        val d = 2 * (a.x * (b.y - c.y) + b.x * (c.y - a.y) + c.x * (a.y - b.y))
        if (d == 0f) { cx = 0f; cy = 0f; r2 = Float.MAX_VALUE } else {
            cx = (ad * (b.y - c.y) + bd * (c.y - a.y) + cd * (a.y - b.y)) / d
            cy = (ad * (c.x - b.x) + bd * (a.x - c.x) + cd * (b.x - a.x)) / d
            r2 = (a.x - cx) * (a.x - cx) + (a.y - cy) * (a.y - cy)
        }
    }
    fun inCircum(p: Offset) = (p.x - cx) * (p.x - cx) + (p.y - cy) * (p.y - cy) <= r2
    fun shares(v: Offset) = a == v || b == v || c == v
}

/** Delaunay triangulation (Bowyer-Watson) — tap to add sites, watch the mesh retriangulate. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DelaunayLabScreen(onBack: () -> Unit) {
    val pts = remember { mutableStateListOf<Offset>() }
    val tris by remember { derivedStateOf { triangulate(pts) } }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Delaunay") }, navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }) },
        modifier = Modifier.fillMaxSize()
    ) { inner ->
        Column(Modifier.padding(inner).fillMaxSize()) {
            Row(Modifier.fillMaxWidth().padding(12.dp)) {
                Text("Tap to add sites (${pts.size})", Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                OutlinedButton(onClick = { pts.clear() }) { Text("Clear") }
            }
            Box(Modifier.fillMaxSize().background(Color(0xFF0C0C14))
                .pointerInput(Unit) { detectTapGestures { pts.add(it) } }
                .drawBehind {
                    tris.forEach { t ->
                        drawLine(Color(0xFF18FFC2), t.a, t.b, strokeWidth = 2f)
                        drawLine(Color(0xFF18FFC2), t.b, t.c, strokeWidth = 2f)
                        drawLine(Color(0xFF18FFC2), t.c, t.a, strokeWidth = 2f)
                    }
                    pts.forEach { drawCircle(Color(0xFFFF6CAB), 7f, it) }
                })
        }
    }
}

private fun triangulate(points: List<Offset>): List<Tri> {
    if (points.size < 3) return emptyList()
    val st1 = Offset(-1e5f, -1e5f); val st2 = Offset(1e5f, -1e5f); val st3 = Offset(0f, 1e5f)
    var tris = mutableListOf(Tri(st1, st2, st3))
    for (p in points) {
        val bad = tris.filter { it.inCircum(p) }
        // collect boundary edges (edges not shared by two bad triangles)
        val edges = ArrayList<Pair<Offset, Offset>>()
        for (t in bad) { edges.add(t.a to t.b); edges.add(t.b to t.c); edges.add(t.c to t.a) }
        val boundary = edges.filter { e ->
            edges.count { (it.first == e.first && it.second == e.second) || (it.first == e.second && it.second == e.first) } == 1
        }
        tris.removeAll(bad.toSet())
        boundary.forEach { (u, v) -> tris.add(Tri(u, v, p)) }
    }
    return tris.filter { !it.shares(st1) && !it.shares(st2) && !it.shares(st3) }
}
