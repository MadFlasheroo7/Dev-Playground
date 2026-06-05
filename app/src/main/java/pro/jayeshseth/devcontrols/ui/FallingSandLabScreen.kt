package pro.jayeshseth.devcontrols.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import kotlin.random.Random

/** Falling-sand (powder) cellular automaton — drag to pour sand; it piles and slides. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FallingSandLabScreen(onBack: () -> Unit) {
    val cols = 72; val rows = 128
    val grid = remember { IntArray(cols * rows) }   // 0 empty, 1..5 sand color
    var tick by remember { mutableIntStateOf(0) }
    val sandPalette = remember {
        listOf(Color(0xFFFFD740), Color(0xFFFFB300), Color(0xFFFF8008), Color(0xFFFF6E40), Color(0xFFFFC371))
    }

    LaunchedEffect(Unit) {
        while (true) {
            withFrameNanos {
                for (y in rows - 2 downTo 0) {
                    val rowOff = y * cols
                    // alternate scan direction to reduce bias
                    val range = if (tick % 2 == 0) 0 until cols else cols - 1 downTo 0
                    for (x in range) {
                        val c = grid[rowOff + x]
                        if (c == 0) continue
                        val below = rowOff + cols + x
                        if (grid[below] == 0) { grid[below] = c; grid[rowOff + x] = 0; continue }
                        val left = if (x > 0) below - 1 else -1
                        val right = if (x < cols - 1) below + 1 else -1
                        val tryLeftFirst = Random.nextBoolean()
                        val a = if (tryLeftFirst) left else right
                        val b = if (tryLeftFirst) right else left
                        if (a >= 0 && grid[a] == 0) { grid[a] = c; grid[rowOff + x] = 0 }
                        else if (b >= 0 && grid[b] == 0) { grid[b] = c; grid[rowOff + x] = 0 }
                    }
                }
                tick++
            }
        }
    }

    fun pour(o: Offset, w: Int, h: Int) {
        if (w == 0) return
        val cx = (o.x / (w.toFloat() / cols)).toInt()
        val cy = (o.y / (h.toFloat() / rows)).toInt()
        for (dy in -2..2) for (dx in -2..2) {
            val x = cx + dx; val y = cy + dy
            if (x in 0 until cols && y in 0 until rows && Random.nextFloat() < 0.6f) grid[y * cols + x] = Random.nextInt(1, 6)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Falling Sand") },
                navigationIcon = { TextButton(onClick = onBack) { Text("Back") } },
                actions = { TextButton(onClick = { grid.fill(0) }) { Text("Clear") } }
            )
        },
        modifier = Modifier.fillMaxSize()
    ) { inner ->
        Column(Modifier.padding(inner).fillMaxSize()) {
            Box(
                Modifier.fillMaxSize().background(Color(0xFF0A0A10))
                    .pointerInput(Unit) { detectTapGestures { pour(it, size.width, size.height) } }
                    .pointerInput(Unit) { detectDragGestures { ch, _ -> pour(ch.position, size.width, size.height); ch.consume() } }
                    .drawBehind {
                        tick
                        val cw = size.width / cols; val chh = size.height / rows
                        for (i in 0 until cols * rows) {
                            val c = grid[i]
                            if (c != 0) drawRect(sandPalette[(c - 1) % sandPalette.size],
                                topLeft = Offset((i % cols) * cw, (i / cols) * chh), size = Size(cw + 0.5f, chh + 0.5f))
                        }
                    }
            )
        }
    }
}
