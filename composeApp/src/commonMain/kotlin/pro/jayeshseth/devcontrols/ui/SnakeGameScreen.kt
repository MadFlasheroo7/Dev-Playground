package pro.jayeshseth.devcontrols.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.random.Random

/** Snake — swipe to steer; eat food to grow; don't hit yourself. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SnakeGameScreen(onBack: () -> Unit) {
    val cols = 17; val rows = 24
    val snake = remember { ArrayDeque<Int>() }
    var dir by remember { mutableIntStateOf(1) }   // 0 up,1 right,2 down,3 left
    var pendingDir by remember { mutableIntStateOf(1) }
    var food by remember { mutableIntStateOf(0) }
    var alive by remember { mutableStateOf(true) }
    var score by remember { mutableIntStateOf(0) }
    var tick by remember { mutableIntStateOf(0) }
    var resetN by remember { mutableIntStateOf(0) }

    LaunchedEffect(resetN) {
        snake.clear(); snake.addLast(rows / 2 * cols + cols / 2); dir = 1; pendingDir = 1; alive = true; score = 0
        food = Random.nextInt(cols * rows); tick++
        while (true) {
            delay(140)
            if (!alive) continue
            dir = pendingDir
            val head = snake.last()
            var x = head % cols; var y = head / cols
            when (dir) { 0 -> y--; 1 -> x++; 2 -> y++; else -> x-- }
            if (x < 0 || x >= cols || y < 0 || y >= rows) { alive = false; tick++; continue }
            val nh = y * cols + x
            if (snake.contains(nh)) { alive = false; tick++; continue }
            snake.addLast(nh)
            if (nh == food) { score++; do { food = Random.nextInt(cols * rows) } while (snake.contains(food)) }
            else snake.removeFirst()
            tick++
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Snake") },
                navigationIcon = { TextButton(onClick = onBack) { Text("Back") } },
                actions = { TextButton(onClick = { resetN++ }) { Text("Restart") } }
            )
        },
        modifier = Modifier.fillMaxSize()
    ) { inner ->
        Column(Modifier.padding(inner).fillMaxSize().padding(12.dp)) {
            Text("Score: $score" + if (!alive) "  —  Game over" else "", style = MaterialTheme.typography.titleMedium)
            Box(
                Modifier.fillMaxSize().padding(top = 8.dp).background(Color(0xFF0A140A))
                    .pointerInput(Unit) {
                        var dx = 0f; var dy = 0f
                        detectDragGestures(
                            onDragStart = { dx = 0f; dy = 0f },
                            onDrag = { ch, d -> dx += d.x; dy += d.y; ch.consume() },
                            onDragEnd = {
                                val nd = if (abs(dx) > abs(dy)) (if (dx < 0) 3 else 1) else (if (dy < 0) 0 else 2)
                                if ((nd + 2) % 4 != dir) pendingDir = nd
                            }
                        )
                    }
                    .drawBehind {
                        tick
                        val cw = size.width / cols; val ch = size.height / rows
                        drawRect(Color(0xFFFF5F6D), topLeft = Offset((food % cols) * cw, (food / cols) * ch), size = Size(cw, ch))
                        snake.forEachIndexed { i, cell ->
                            val c = if (i == snake.size - 1) Color(0xFF69F0AE) else Color(0xFF2E7D32)
                            drawRect(c, topLeft = Offset((cell % cols) * cw + 1, (cell / cols) * ch + 1), size = Size(cw - 2, ch - 2))
                        }
                    }
            )
        }
    }
}
