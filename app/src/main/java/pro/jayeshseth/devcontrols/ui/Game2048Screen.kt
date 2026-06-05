package pro.jayeshseth.devcontrols.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs
import kotlin.random.Random

/** 2048 — swipe to slide and merge tiles. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Game2048Screen(onBack: () -> Unit) {
    val grid = remember { IntArray(16) }
    var score by remember { mutableIntStateOf(0) }
    var version by remember { mutableIntStateOf(0) }

    fun spawn() {
        val empty = (0..15).filter { grid[it] == 0 }
        if (empty.isEmpty()) return
        grid[empty[Random.nextInt(empty.size)]] = if (Random.nextFloat() < 0.9f) 2 else 4
    }
    fun reset() { grid.fill(0); score = 0; spawn(); spawn(); version++ }
    if (version == 0) reset()

    fun slide(line: IntArray): Pair<IntArray, Int> {
        val nums = line.filter { it != 0 }
        val out = ArrayList<Int>(); var sc = 0; var i = 0
        while (i < nums.size) {
            if (i + 1 < nums.size && nums[i] == nums[i + 1]) { val v = nums[i] * 2; out.add(v); sc += v; i += 2 }
            else { out.add(nums[i]); i++ }
        }
        while (out.size < 4) out.add(0)
        return out.toIntArray() to sc
    }
    fun move(dir: Int): Boolean {
        val before = grid.copyOf()
        for (r in 0..3) {
            val idx = when (dir) {
                0 -> (0..3).map { r * 4 + it }       // left
                1 -> (3 downTo 0).map { r * 4 + it } // right
                2 -> (0..3).map { it * 4 + r }       // up
                else -> (3 downTo 0).map { it * 4 + r } // down
            }
            val line = IntArray(4) { grid[idx[it]] }
            val (ns, sc) = slide(line); score += sc
            for (k in 0..3) grid[idx[k]] = ns[k]
        }
        val changed = !before.contentEquals(grid)
        if (changed) { spawn(); version++ }
        return changed
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
            Text("Score: $score", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(12.dp))
            Column(
                Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(12.dp)).background(Color(0xFF1A1A24))
                    .pointerInput(Unit) {
                        var dx = 0f; var dy = 0f
                        detectDragGestures(
                            onDragStart = { dx = 0f; dy = 0f },
                            onDrag = { ch, d -> dx += d.x; dy += d.y; ch.consume() },
                            onDragEnd = {
                                if (abs(dx) > abs(dy)) { if (abs(dx) > 40) move(if (dx < 0) 0 else 1) }
                                else { if (abs(dy) > 40) move(if (dy < 0) 2 else 3) }
                            }
                        )
                    }.padding(6.dp)
            ) {
                version
                for (r in 0..3) {
                    Row(Modifier.fillMaxWidth().weight(1f)) {
                        for (c in 0..3) {
                            val v = grid[r * 4 + c]
                            Box(Modifier.weight(1f).aspectRatio(1f).padding(4.dp).clip(RoundedCornerShape(8.dp)).background(tileColor(v)),
                                contentAlignment = Alignment.Center) {
                                if (v != 0) Text("$v", color = if (v <= 4) Color(0xFF333333) else Color.White,
                                    fontWeight = FontWeight.Bold, fontSize = if (v < 1000) 22.sp else 16.sp)
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Text("Swipe to move.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private fun tileColor(v: Int): Color = when (v) {
    0 -> Color(0xFF2A2A36); 2 -> Color(0xFFEEE4DA); 4 -> Color(0xFFEDE0C8); 8 -> Color(0xFFF2B179)
    16 -> Color(0xFFF59563); 32 -> Color(0xFFF67C5F); 64 -> Color(0xFFF65E3B); 128 -> Color(0xFFEDCF72)
    256 -> Color(0xFFEDCC61); 512 -> Color(0xFFEDC850); 1024 -> Color(0xFFEDC53F); else -> Color(0xFFEDC22E)
}
