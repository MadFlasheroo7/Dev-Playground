package pro.jayeshseth.devcontrols.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.max
import kotlin.random.Random

private class Cell2048(val id: Int, value: Int, row: Int, col: Int) {
    var value by mutableIntStateOf(value)
    var row by mutableIntStateOf(row)
    var col by mutableIntStateOf(col)
    var pop by mutableIntStateOf(0)
    fun cell() = row * 4 + col
    fun setCell(c: Int) { row = c / 4; col = c % 4 }
}

/** 2048 — swipe to slide & merge. Tiles slide to their new cells, merges pop, spawns scale in. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Game2048Screen(onBack: () -> Unit) {
    val tiles = remember { mutableStateListOf<Cell2048>() }
    var nextId by remember { mutableIntStateOf(0) }
    var score by remember { mutableIntStateOf(0) }
    var best by remember { mutableIntStateOf(0) }
    var won by remember { mutableStateOf(false) }
    var over by remember { mutableStateOf(false) }
    var settling by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    fun spawn() {
        val occupied = tiles.map { it.cell() }.toSet()
        val empty = (0..15).filterNot { occupied.contains(it) }
        if (empty.isEmpty()) return
        val c = empty[Random.nextInt(empty.size)]
        tiles.add(Cell2048(nextId++, if (Random.nextFloat() < 0.9f) 2 else 4, c / 4, c % 4))
    }
    fun reset() { tiles.clear(); score = 0; won = false; over = false; settling = false; spawn(); spawn() }
    LaunchedEffect(Unit) { if (tiles.isEmpty()) reset() }

    fun lineCells(dir: Int, i: Int): List<Int> = when (dir) {
        0 -> (0..3).map { i * 4 + it }        // left
        1 -> (3 downTo 0).map { i * 4 + it }  // right
        2 -> (0..3).map { it * 4 + i }        // up
        else -> (3 downTo 0).map { it * 4 + i } // down
    }
    fun tileAt(c: Int) = tiles.firstOrNull { it.cell() == c }

    fun canMove(): Boolean {
        if (tiles.size < 16) return true
        for (r in 0..3) for (c in 0..3) {
            val v = tileAt(r * 4 + c)?.value ?: 0
            if (c < 3 && (tileAt(r * 4 + c + 1)?.value ?: 0) == v) return true
            if (r < 3 && (tileAt((r + 1) * 4 + c)?.value ?: 0) == v) return true
        }
        return false
    }

    fun move(dir: Int) {
        if (settling || over) return
        val merges = ArrayList<Pair<Cell2048, Cell2048>>() // survivor, removed
        var moved = false
        for (i in 0..3) {
            val cells = lineCells(dir, i)
            val lineTiles = cells.mapNotNull { tileAt(it) }
            var slot = 0; var prev: Cell2048? = null; var prevMerged = false
            for (t in lineTiles) {
                val p = prev
                if (p != null && p.value == t.value && !prevMerged) {
                    merges.add(p to t); t.setCell(p.cell()); prevMerged = true; moved = true
                } else {
                    val dest = cells[slot]; slot++
                    if (t.cell() != dest) moved = true
                    t.setCell(dest); prev = t; prevMerged = false
                }
            }
        }
        if (!moved) return
        settling = true
        scope.launch {
            delay(110)
            var gained = 0
            for ((survivor, removed) in merges) {
                survivor.value *= 2; survivor.pop++; gained += survivor.value
                if (survivor.value >= 2048) won = true
                tiles.remove(removed)
            }
            score += gained; best = max(best, score)
            spawn()
            if (!canMove()) over = true
            settling = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("2048") },
                navigationIcon = { TextButton(onClick = onBack) { Text("Back") } },
                actions = { TextButton(onClick = { reset() }) { Text("New") } }
            )
        },
        modifier = Modifier.fillMaxSize()
    ) { inner ->
        Column(Modifier.padding(inner).fillMaxSize().padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ScoreCard("SCORE", score, Modifier.weight(1f))
                ScoreCard("BEST", best, Modifier.weight(1f))
            }
            Spacer(Modifier.height(16.dp))
            BoxWithConstraints(
                Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(12.dp)).background(Color(0xFF1B1B26))
                    .pointerInput(Unit) {
                        var dx = 0f; var dy = 0f
                        detectDragGestures(
                            onDragStart = { dx = 0f; dy = 0f },
                            onDrag = { ch, d -> dx += d.x; dy += d.y; ch.consume() },
                            onDragEnd = {
                                if (max(abs(dx), abs(dy)) > 40) {
                                    if (abs(dx) > abs(dy)) move(if (dx < 0) 0 else 1) else move(if (dy < 0) 2 else 3)
                                }
                            }
                        )
                    }
            ) {
                val cell: Dp = maxWidth / 4
                // empty slots
                Column(Modifier.fillMaxSize()) {
                    repeat(4) {
                        Row(Modifier.fillMaxWidth().weight(1f)) {
                            repeat(4) {
                                Box(Modifier.weight(1f).fillMaxHeight().padding(4.dp).clip(RoundedCornerShape(8.dp)).background(Color(0xFF2A2A38)))
                            }
                        }
                    }
                }
                // tiles
                tiles.forEach { tile -> key(tile.id) { TileView(tile, cell) } }
            }
            Spacer(Modifier.height(10.dp))
            Text(
                when { over -> "Game over — tap New."; won -> "🎉 2048! Keep going."; else -> "Swipe to move." },
                style = MaterialTheme.typography.bodyMedium,
                color = if (over) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun TileView(tile: Cell2048, cell: Dp) {
    val x by animateDpAsState(cell * tile.col, tween(110), label = "x")
    val y by animateDpAsState(cell * tile.row, tween(110), label = "y")
    val scale = remember { Animatable(0f) }
    LaunchedEffect(Unit) { scale.animateTo(1f, tween(130)) }
    LaunchedEffect(tile.pop) {
        if (tile.pop > 0) { scale.snapTo(1f); scale.animateTo(1.16f, tween(80)); scale.animateTo(1f, tween(80)) }
    }
    Box(
        Modifier.offset(x = x, y = y).size(cell).padding(4.dp)
            .graphicsLayer { scaleX = scale.value; scaleY = scale.value }
            .clip(RoundedCornerShape(8.dp)).background(tileColor(tile.value)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            "${tile.value}",
            color = if (tile.value <= 4) Color(0xFF333333) else Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = when { tile.value < 100 -> 26.sp; tile.value < 1000 -> 22.sp; else -> 17.sp }
        )
    }
}

@Composable
private fun ScoreCard(label: String, value: Int, modifier: Modifier) {
    Column(modifier.clip(RoundedCornerShape(10.dp)).background(Color(0xFF2A2A38)).padding(vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = Color(0xFF9AA0B5), style = MaterialTheme.typography.labelSmall)
        Text("$value", color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
    }
}

private fun tileColor(v: Int): Color = when (v) {
    0 -> Color(0xFF2A2A38); 2 -> Color(0xFFEEE4DA); 4 -> Color(0xFFEDE0C8); 8 -> Color(0xFFF2B179)
    16 -> Color(0xFFF59563); 32 -> Color(0xFFF67C5F); 64 -> Color(0xFFF65E3B); 128 -> Color(0xFFEDCF72)
    256 -> Color(0xFFEDCC61); 512 -> Color(0xFFEDC850); 1024 -> Color(0xFFEDC53F); else -> Color(0xFFEDC22E)
}
