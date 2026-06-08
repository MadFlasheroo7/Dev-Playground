package pro.jayeshseth.devcontrols.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlin.random.Random

private val TET = listOf(
    listOf(0 to 0, 0 to 1, 0 to 2, 0 to 3) to Color(0xFF00C9FF), // I
    listOf(0 to 0, 0 to 1, 1 to 0, 1 to 1) to Color(0xFFFFD740), // O
    listOf(0 to 1, 1 to 0, 1 to 1, 1 to 2) to Color(0xFFE040FB), // T
    listOf(0 to 0, 1 to 0, 1 to 1, 1 to 2) to Color(0xFF2196F3), // J
    listOf(0 to 2, 1 to 0, 1 to 1, 1 to 2) to Color(0xFFFF8008), // L
    listOf(0 to 1, 0 to 2, 1 to 0, 1 to 1) to Color(0xFF69F0AE), // S
    listOf(0 to 0, 0 to 1, 1 to 1, 1 to 2) to Color(0xFFFF5F6D)  // Z
)

/** Tetris-lite — buttons to move/rotate/drop; full rows clear. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TetrisLiteScreen(onBack: () -> Unit) {
    val cols = 10; val rows = 18
    val board = remember { IntArray(cols * rows) }   // 0 empty else ARGB color
    var piece by remember { mutableStateOf(listOf<Pair<Int, Int>>()) }
    var pieceColor by remember { mutableStateOf(Color.White) }
    var over by remember { mutableStateOf(false) }
    var score by remember { mutableIntStateOf(0) }
    var tick by remember { mutableIntStateOf(0) }
    var resetN by remember { mutableIntStateOf(0) }

    fun valid(cells: List<Pair<Int, Int>>) = cells.all { (r, c) ->
        c in 0 until cols && r < rows && (r < 0 || board[r * cols + c] == 0)
    }
    fun spawn() {
        val (shape, color) = TET[Random.nextInt(TET.size)]
        val cells = shape.map { (r, c) -> (r - 2) to (c + 3) }
        if (!valid(cells)) { over = true } else { piece = cells; pieceColor = color }
    }
    fun lockAndNext() {
        piece.forEach { (r, c) -> if (r in 0 until rows) board[r * cols + c] = pieceColor.toArgb() }
        // clear full rows
        var cleared = 0
        for (r in rows - 1 downTo 0) {
            if ((0 until cols).all { board[r * cols + it] != 0 }) {
                cleared++
                for (rr in r downTo 1) for (c in 0 until cols) board[rr * cols + c] = board[(rr - 1) * cols + c]
                for (c in 0 until cols) board[c] = 0
            }
        }
        if (cleared > 0) score += cleared * 100
        spawn()
    }
    fun step(dr: Int, dc: Int): Boolean {
        val moved = piece.map { (r, c) -> (r + dr) to (c + dc) }
        return if (valid(moved)) { piece = moved; tick++; true } else false
    }
    fun rotate() {
        if (piece.isEmpty()) return
        val p = piece[1]
        val rot = piece.map { (r, c) -> (p.first + (c - p.second)) to (p.second - (r - p.first)) }
        if (valid(rot)) { piece = rot; tick++ }
    }

    LaunchedEffect(resetN) {
        for (i in board.indices) board[i] = 0
        over = false; score = 0; spawn(); tick++
        while (true) {
            delay(550)
            if (over) continue
            if (!step(1, 0)) lockAndNext()
            tick++
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tetris-lite") },
                navigationIcon = { TextButton(onClick = onBack) { Text("Back") } },
                actions = { TextButton(onClick = { resetN++ }) { Text("New") } }
            )
        },
        modifier = Modifier.fillMaxSize()
    ) { inner ->
        Column(Modifier.padding(inner).fillMaxSize().padding(12.dp)) {
            Text("Score: $score" + if (over) "  —  Game over" else "", style = MaterialTheme.typography.titleMedium)
            Box(
                Modifier.fillMaxWidth().weight(1f).aspectRatio(cols.toFloat() / rows).background(Color(0xFF07070E)).drawBehind {
                    tick
                    val cw = size.width / cols; val ch = size.height / rows
                    for (i in board.indices) if (board[i] != 0) {
                        drawRect(Color(board[i]), topLeft = Offset((i % cols) * cw + 1, (i / cols) * ch + 1), size = Size(cw - 2, ch - 2))
                    }
                    piece.forEach { (r, c) -> if (r >= 0) drawRect(pieceColor, topLeft = Offset(c * cw + 1, r * ch + 1), size = Size(cw - 2, ch - 2)) }
                }
            )
            Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { step(0, -1) }, modifier = Modifier.weight(1f)) { Text("◀") }
                OutlinedButton(onClick = { rotate() }, modifier = Modifier.weight(1f)) { Text("⟳") }
                OutlinedButton(onClick = { step(0, 1) }, modifier = Modifier.weight(1f)) { Text("▶") }
                OutlinedButton(onClick = { while (step(1, 0)) {}; lockAndNext(); tick++ }, modifier = Modifier.weight(1f)) { Text("▼") }
            }
        }
    }
}
