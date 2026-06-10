package pro.jayeshseth.devcontrols.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import pro.jayeshseth.devcontrols.util.IntMinHeap
import kotlin.math.abs

/** A* pathfinding visualizer — paint walls, run, watch the frontier expand and the path form. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PathfindingLabScreen(onBack: () -> Unit) {
    val cols = 24
    val rows = 36
    val wall = remember { BooleanArray(cols * rows) }
    val visited = remember { BooleanArray(cols * rows) }
    val path = remember { BooleanArray(cols * rows) }
    var version by remember { mutableIntStateOf(0) }
    var running by remember { mutableStateOf(false) }
    val start = 0
    val goal = cols * rows - 1

    fun clearSearch() { visited.fill(false); path.fill(false); version++ }

    LaunchedEffect(running) {
        if (!running) return@LaunchedEffect
        clearSearch()
        val g = IntArray(cols * rows) { Int.MAX_VALUE }
        val came = IntArray(cols * rows) { -1 }
        fun hx(i: Int): Int { val x = i % cols; val y = i / cols; return abs(x - goal % cols) + abs(y - goal / cols) }
        val open = IntMinHeap()
        g[start] = 0; open.add(start, hx(start))
        var found = false
        while (open.isNotEmpty()) {
            val cur = open.poll()
            if (cur == goal) { found = true; break }
            if (visited[cur]) continue
            visited[cur] = true; version++; delay(4)
            val x = cur % cols; val y = cur / cols
            for ((dx, dy) in listOf(1 to 0, -1 to 0, 0 to 1, 0 to -1)) {
                val nx = x + dx; val ny = y + dy
                if (nx in 0 until cols && ny in 0 until rows) {
                    val ni = ny * cols + nx
                    if (wall[ni] || visited[ni]) continue
                    val ng = g[cur] + 1
                    if (ng < g[ni]) { g[ni] = ng; came[ni] = cur; open.add(ni, ng + hx(ni)) }
                }
            }
        }
        if (found) {
            var c = goal
            while (c != -1) { path[c] = true; version++; delay(8); c = came[c] }
        }
        running = false
    }

    fun paintWall(o: Offset, w: Int, h: Int) {
        if (w == 0) return
        val c = (o.x / (w.toFloat() / cols)).toInt().coerceIn(0, cols - 1)
        val r = (o.y / (h.toFloat() / rows)).toInt().coerceIn(0, rows - 1)
        val i = r * cols + c
        if (i != start && i != goal) { wall[i] = true; version++ }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Pathfinding (A*)") },
                navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }
            )
        },
        modifier = Modifier.fillMaxSize()
    ) { inner ->
        Column(Modifier.padding(inner).fillMaxSize().padding(12.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { running = true }, enabled = !running) { Text("Run") }
                OutlinedButton(onClick = { clearSearch() }, enabled = !running) { Text("Clear search") }
                OutlinedButton(onClick = { wall.fill(false); clearSearch() }, enabled = !running) { Text("Clear walls") }
            }
            Text("Drag to paint walls. Start = top-left, goal = bottom-right.",
                style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant)
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(top = 8.dp)
                    .background(Color(0xFF15151E))
                    .pointerInput(Unit) { detectTapGestures { paintWall(it, size.width, size.height) } }
                    .pointerInput(Unit) { detectDragGestures { ch, _ -> paintWall(ch.position, size.width, size.height); ch.consume() } }
                    .drawBehind {
                        version
                        val cw = size.width / cols
                        val ch = size.height / rows
                        for (i in 0 until cols * rows) {
                            val col = when {
                                i == start -> Color(0xFF69F0AE)
                                i == goal -> Color(0xFFFF5F6D)
                                wall[i] -> Color(0xFF2A2A33)
                                path[i] -> Color(0xFFFFD740)
                                visited[i] -> Color(0xFF1B5E78)
                                else -> Color(0xFF0E0E16)
                            }
                            drawRect(col, topLeft = Offset((i % cols) * cw, (i / cols) * ch), size = Size(cw - 1f, ch - 1f))
                        }
                    }
            )
        }
    }
}
