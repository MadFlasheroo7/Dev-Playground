package pro.jayeshseth.devcontrols.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import kotlin.random.Random

/** Minesweeper — tap to reveal (flood-fill), long-press to flag. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MinesweeperScreen(onBack: () -> Unit) {
    val n = 9; val mines = 10
    val mine = remember { BooleanArray(n * n) }
    val revealed = remember { BooleanArray(n * n) }
    val flagged = remember { BooleanArray(n * n) }
    val adj = remember { IntArray(n * n) }
    var state by remember { mutableStateOf("play") }   // play / lost / won
    var version by remember { mutableIntStateOf(0) }
    var resetN by remember { mutableIntStateOf(0) }

    fun idx(x: Int, y: Int) = y * n + x
    fun neighbors(i: Int): List<Int> {
        val x = i % n; val y = i / n; val out = ArrayList<Int>()
        for (dy in -1..1) for (dx in -1..1) if (dx != 0 || dy != 0) {
            val nx = x + dx; val ny = y + dy; if (nx in 0 until n && ny in 0 until n) out.add(idx(nx, ny))
        }
        return out
    }
    fun build() {
        mine.fill(false); revealed.fill(false); flagged.fill(false); adj.fill(0); state = "play"
        val cells = (0 until n * n).shuffled().take(mines)
        cells.forEach { mine[it] = true }
        for (i in 0 until n * n) adj[i] = neighbors(i).count { mine[it] }
        version++
    }
    if (version == 0) build()

    fun reveal(i: Int) {
        if (revealed[i] || flagged[i] || state != "play") return
        revealed[i] = true
        if (mine[i]) { state = "lost"; for (j in 0 until n * n) if (mine[j]) revealed[j] = true; version++; return }
        if (adj[i] == 0) neighbors(i).forEach { if (!revealed[it]) reveal(it) }
        if ((0 until n * n).all { revealed[it] || mine[it] }) state = "won"
        version++
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Minesweeper") },
                navigationIcon = { TextButton(onClick = onBack) { Text("Back") } },
                actions = { TextButton(onClick = { resetN++; build() }) { Text("New") } }
            )
        },
        modifier = Modifier.fillMaxSize()
    ) { inner ->
        Column(Modifier.padding(inner).fillMaxSize().padding(16.dp)) {
            Text(when (state) { "lost" -> "💥 Boom!"; "won" -> "🏆 Cleared!"; else -> "Tap to reveal · long-press to flag" },
                style = MaterialTheme.typography.titleMedium)
            Box(
                Modifier.fillMaxWidth().aspectRatio(1f).padding(top = 12.dp).background(Color(0xFF1A1A24))
                    .pointerInput(version) {
                        detectTapGestures(
                            onTap = { val cw = size.width / n; reveal(idx((it.x / cw).toInt().coerceIn(0, n - 1), (it.y / cw).toInt().coerceIn(0, n - 1))) },
                            onLongPress = {
                                val cw = size.width / n; val i = idx((it.x / cw).toInt().coerceIn(0, n - 1), (it.y / cw).toInt().coerceIn(0, n - 1))
                                if (!revealed[i]) { flagged[i] = !flagged[i]; version++ }
                            }
                        )
                    }
                    .drawBehind {
                        version
                        val cw = size.width / n
                        val paint = android.graphics.Paint().apply { textSize = cw * 0.5f; textAlign = android.graphics.Paint.Align.CENTER; isFakeBoldText = true }
                        for (i in 0 until n * n) {
                            val x = (i % n) * cw; val y = (i / n) * cw
                            val cell = if (revealed[i]) Color(0xFF2A2A36) else Color(0xFF3E4250)
                            drawRect(cell, topLeft = Offset(x + 1, y + 1), size = Size(cw - 2, cw - 2))
                            val cx = x + cw / 2; val cy = y + cw * 0.68f
                            when {
                                flagged[i] && !revealed[i] -> { paint.color = android.graphics.Color.rgb(0xFF, 0x5F, 0x6D); drawContext.canvas.nativeCanvas.drawText("⚑", cx, cy, paint) }
                                revealed[i] && mine[i] -> { paint.color = android.graphics.Color.WHITE; drawContext.canvas.nativeCanvas.drawText("✸", cx, cy, paint) }
                                revealed[i] && adj[i] > 0 -> { paint.color = numColor(adj[i]); drawContext.canvas.nativeCanvas.drawText("${adj[i]}", cx, cy, paint) }
                            }
                        }
                    }
            )
        }
    }
}

private fun numColor(n: Int): Int = when (n) {
    1 -> android.graphics.Color.rgb(0x42, 0xA5, 0xF5); 2 -> android.graphics.Color.rgb(0x66, 0xBB, 0x6A)
    3 -> android.graphics.Color.rgb(0xEF, 0x53, 0x50); 4 -> android.graphics.Color.rgb(0xAB, 0x47, 0xBC)
    else -> android.graphics.Color.rgb(0xFF, 0xB3, 0x00)
}
