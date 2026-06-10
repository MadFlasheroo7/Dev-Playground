package pro.jayeshseth.devcontrols.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import kotlin.random.Random

/**
 * Conway's Game of Life. Tap/drag to paint live cells; Play steps the toroidal grid on
 * a frame loop. Randomize seeds it; Clear empties it.
 */
private const val COLS = 36

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameOfLifeLabScreen(onBack: () -> Unit) {
    val rows = 56
    val grid = remember { BooleanArray(COLS * rows) }
    var gen by remember { mutableIntStateOf(0) }
    var running by remember { mutableStateOf(false) }

    fun idx(c: Int, r: Int) = ((r + rows) % rows) * COLS + ((c + COLS) % COLS)
    fun step() {
        val next = BooleanArray(COLS * rows)
        for (r in 0 until rows) for (c in 0 until COLS) {
            var n = 0
            for (dr in -1..1) for (dc in -1..1) if (dr != 0 || dc != 0) if (grid[idx(c + dc, r + dr)]) n++
            val alive = grid[idx(c, r)]
            next[idx(c, r)] = (alive && (n == 2 || n == 3)) || (!alive && n == 3)
        }
        next.copyInto(grid)
        gen++
    }

    LaunchedEffect(running) {
        while (running) { step(); delay(110) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Game of Life") },
                navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }
            )
        },
        modifier = Modifier.fillMaxSize()
    ) { inner ->
        Column(Modifier.padding(inner).fillMaxSize().padding(12.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { running = !running }) { Text(if (running) "Pause" else "Play") }
                OutlinedButton(onClick = { step() }, enabled = !running) { Text("Step") }
                OutlinedButton(onClick = {
                    for (i in grid.indices) grid[i] = Random.nextFloat() < 0.28f
                    gen++
                }) { Text("Random") }
                OutlinedButton(onClick = { grid.fill(false); gen++ }) { Text("Clear") }
            }
            Text("gen $gen", style = androidx.compose.material3.MaterialTheme.typography.labelSmall)
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(top = 8.dp)
                    .background(Color(0xFF0A0A0A))
                    .pointerInput(Unit) {
                        detectTapGestures { paint(it, size.width, size.height, rows, grid); gen++ }
                    }
                    .pointerInput(Unit) {
                        detectDragGestures { ch, _ -> paint(ch.position, size.width, size.height, rows, grid); gen++; ch.consume() }
                    }
                    .drawBehind {
                        gen
                        val cw = size.width / COLS
                        val ch = size.height / rows
                        for (r in 0 until rows) for (c in 0 until COLS) {
                            if (grid[r * COLS + c]) {
                                drawRect(
                                    Color(0xFF69F0AE),
                                    topLeft = Offset(c * cw + 1, r * ch + 1),
                                    size = Size(cw - 2, ch - 2)
                                )
                            }
                        }
                    }
            )
        }
    }
}

private fun paint(o: Offset, w: Int, h: Int, rows: Int, grid: BooleanArray) {
    if (w == 0 || h == 0) return
    val c = (o.x / (w.toFloat() / COLS)).toInt().coerceIn(0, COLS - 1)
    val r = (o.y / (h.toFloat() / rows)).toInt().coerceIn(0, rows - 1)
    grid[r * COLS + c] = true
}
