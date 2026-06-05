package pro.jayeshseth.devcontrols.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/** Maze: recursive-backtracker generation, then BFS solve — both animated. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MazeLabScreen(onBack: () -> Unit) {
    val cc = 16; val cr = 26
    val gw = 2 * cc + 1; val gh = 2 * cr + 1
    val maze = remember { BooleanArray(gw * gh) { true } }   // true = wall
    val seen = remember { BooleanArray(gw * gh) }
    val path = remember { BooleanArray(gw * gh) }
    var version by remember { mutableIntStateOf(0) }
    var busy by remember { mutableStateOf(false) }
    var go by remember { mutableIntStateOf(0) }

    fun gi(x: Int, y: Int) = y * gw + x
    fun cidx(cx: Int, cy: Int) = (2 * cy + 1) * gw + (2 * cx + 1)

    LaunchedEffect(go) {
        if (go == 0) return@LaunchedEffect
        busy = true
        maze.fill(true); seen.fill(false); path.fill(false); version++
        // generate
        val vis = BooleanArray(cc * cr)
        val stack = ArrayDeque<Int>()
        maze[cidx(0, 0)] = false; vis[0] = true; stack.addLast(0); version++
        val dirs = listOf(1 to 0, -1 to 0, 0 to 1, 0 to -1)
        while (stack.isNotEmpty()) {
            val cell = stack.last(); val cx = cell % cc; val cy = cell / cc
            val nbrs = dirs.filter { (dx, dy) ->
                val nx = cx + dx; val ny = cy + dy
                nx in 0 until cc && ny in 0 until cr && !vis[ny * cc + nx]
            }
            if (nbrs.isEmpty()) { stack.removeLast() } else {
                val (dx, dy) = nbrs[(version * 31 + cell) % nbrs.size]
                maze[gi(2 * cx + 1 + dx, 2 * cy + 1 + dy)] = false
                maze[cidx(cx + dx, cy + dy)] = false
                vis[(cy + dy) * cc + (cx + dx)] = true
                stack.addLast((cy + dy) * cc + (cx + dx)); version++; delay(5)
            }
        }
        // solve BFS
        val q = ArrayDeque<Int>(); val came = IntArray(gw * gh) { -1 }
        val s = cidx(0, 0); val g = cidx(cc - 1, cr - 1)
        q.addLast(s); seen[s] = true
        var found = false
        val gdirs = listOf(1 to 0, -1 to 0, 0 to 1, 0 to -1)
        while (q.isNotEmpty()) {
            val c = q.removeFirst()
            if (c == g) { found = true; break }
            version++; if (version % 2 == 0) delay(3)
            val x = c % gw; val y = c / gw
            for ((dx, dy) in gdirs) {
                val nx = x + dx; val ny = y + dy
                if (nx in 0 until gw && ny in 0 until gh) {
                    val ni = gi(nx, ny)
                    if (!maze[ni] && !seen[ni]) { seen[ni] = true; came[ni] = c; q.addLast(ni) }
                }
            }
        }
        if (found) { var c = g; while (c != -1) { path[c] = true; version++; delay(6); c = came[c] } }
        busy = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Maze") },
                navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }
            )
        },
        modifier = Modifier.fillMaxSize()
    ) { inner ->
        Column(Modifier.padding(inner).fillMaxSize().padding(12.dp)) {
            OutlinedButton(onClick = { go++ }, enabled = !busy) { Text("Generate & solve") }
            Box(
                Modifier.fillMaxSize().padding(top = 8.dp).background(Color(0xFF0A0A12)).drawBehind {
                    version
                    val cw = size.width / gw; val chh = size.height / gh
                    for (i in 0 until gw * gh) {
                        val col = when {
                            path[i] -> Color(0xFFFFD740)
                            maze[i] -> Color(0xFF20202C)
                            seen[i] -> Color(0xFF1B5E78)
                            else -> Color(0xFFE8E8F0)
                        }
                        drawRect(col, topLeft = Offset((i % gw) * cw, (i / gw) * chh), size = Size(cw, chh))
                    }
                }
            )
        }
    }
}
