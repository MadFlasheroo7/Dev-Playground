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
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp

/** Breakout — drag to move the paddle, clear the bricks. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BreakoutGameScreen(onBack: () -> Unit) {
    val cols = 8; val rows = 5
    val bricks = remember { BooleanArray(cols * rows) { true } }
    var size by remember { mutableStateOf(IntSize.Zero) }
    var paddleX by remember { mutableStateOf(0f) }
    var bx by remember { mutableStateOf(0f) }; var by_ by remember { mutableStateOf(0f) }
    var vx by remember { mutableStateOf(7f) }; var vy by remember { mutableStateOf(-9f) }
    var tick by remember { mutableIntStateOf(0) }
    var msg by remember { mutableStateOf("") }
    var resetN by remember { mutableIntStateOf(0) }
    val palette = listOf(Color(0xFFFF5F6D), Color(0xFFFFC371), Color(0xFF92FE9D), Color(0xFF00C9FF), Color(0xFFE040FB))

    LaunchedEffect(size, resetN) {
        if (size == IntSize.Zero) return@LaunchedEffect
        bricks.fill(true); msg = ""
        paddleX = size.width / 2f; bx = size.width / 2f; by_ = size.height * 0.7f; vx = 7f; vy = -9f
        val pw = size.width * 0.22f; val ph = 26f; val br = 14f
        while (true) {
            withFrameNanos {
                if (msg.isEmpty()) {
                    bx += vx; by_ += vy
                    if (bx < br) { bx = br; vx = -vx }
                    if (bx > size.width - br) { bx = size.width - br; vx = -vx }
                    if (by_ < br) { by_ = br; vy = -vy }
                    // paddle
                    val py = size.height - 60f
                    if (by_ + br >= py && by_ < py + ph && bx in (paddleX - pw / 2)..(paddleX + pw / 2) && vy > 0) {
                        vy = -vy; vx += ((bx - paddleX) / (pw / 2)) * 3f
                    }
                    if (by_ > size.height) msg = "Game over"
                    // bricks
                    val bw = size.width / cols; val bh = size.height * 0.4f / rows
                    val cxi = (bx / bw).toInt(); val cyi = (by_ / bh).toInt()
                    if (cyi in 0 until rows && cxi in 0 until cols) {
                        val bi = cyi * cols + cxi
                        if (bricks[bi]) { bricks[bi] = false; vy = -vy; if (bricks.none { it }) msg = "You win!" }
                    }
                    tick++
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Breakout") },
                navigationIcon = { TextButton(onClick = onBack) { Text("Back") } },
                actions = { TextButton(onClick = { resetN++ }) { Text("Restart") } }
            )
        },
        modifier = Modifier.fillMaxSize()
    ) { inner ->
        Column(Modifier.padding(inner).fillMaxSize()) {
            if (msg.isNotEmpty()) Text(msg, Modifier.padding(12.dp), style = MaterialTheme.typography.titleMedium)
            Box(
                Modifier.fillMaxSize().background(Color(0xFF05050C)).onSizeChanged { size = it }
                    .pointerInput(Unit) { detectDragGestures { ch, _ -> paddleX = ch.position.x; ch.consume() } }
                    .drawBehind {
                        tick
                        val bw = size.width.toFloat() / cols; val bh = size.height * 0.4f / rows
                        for (i in bricks.indices) if (bricks[i]) {
                            drawRect(palette[(i / cols) % palette.size],
                                topLeft = Offset((i % cols) * bw + 2, (i / cols) * bh + 2), size = Size(bw - 4, bh - 4))
                        }
                        val pw = size.width * 0.22f
                        drawRect(Color.White, topLeft = Offset(paddleX - pw / 2, size.height - 60f), size = Size(pw, 18f))
                        drawCircle(Color(0xFFFFD740), 14f, Offset(bx, by_))
                    }
            )
        }
    }
}
