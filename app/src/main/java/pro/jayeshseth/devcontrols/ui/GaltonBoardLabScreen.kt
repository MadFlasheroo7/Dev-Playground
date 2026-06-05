package pro.jayeshseth.devcontrols.ui

import androidx.compose.foundation.background
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import kotlin.random.Random

private class Ball(var x: Float, var y: Float, var vy: Float = 0f, var settled: Boolean = false)

/** Galton board — balls bounce through a peg lattice into bins, forming a normal distribution. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GaltonBoardLabScreen(onBack: () -> Unit) {
    val rowsOfPegs = 10
    val bins = remember { IntArray(rowsOfPegs + 1) }
    val balls = remember { mutableListOf<Ball>() }
    var size by remember { mutableStateOf(IntSize.Zero) }
    var tick by remember { mutableIntStateOf(0) }

    LaunchedEffect(size) {
        if (size == IntSize.Zero) return@LaunchedEffect
        var frame = 0
        while (true) {
            withFrameNanos {
                frame++
                if (frame % 6 == 0 && balls.count { !it.settled } < 60) balls.add(Ball(size.width / 2f + (Random.nextFloat() - 0.5f) * 8f, 20f))
                val pegTop = size.height * 0.12f
                val pegGapY = (size.height * 0.6f) / rowsOfPegs
                val binTop = pegTop + rowsOfPegs * pegGapY + 20f
                for (b in balls) {
                    if (b.settled) continue
                    b.vy += 0.35f; b.y += b.vy
                    val row = ((b.y - pegTop) / pegGapY).toInt()
                    if (row in 0 until rowsOfPegs && (b.y - pegTop) % pegGapY < 6f) {
                        b.x += if (Random.nextBoolean()) pegGapY * 0.5f else -pegGapY * 0.5f
                        b.vy *= 0.6f
                    }
                    if (b.y >= binTop) {
                        val bin = (((b.x - size.width / 2f) / (pegGapY * 0.5f)).toInt() + rowsOfPegs / 2 + (rowsOfPegs + 1) / 2 - rowsOfPegs / 2).coerceIn(0, rowsOfPegs)
                        bins[bin]++; b.settled = true
                    }
                }
                balls.removeAll { it.settled }
                tick++
            }
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Galton Board") }, navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }) },
        modifier = Modifier.fillMaxSize()
    ) { inner ->
        Column(Modifier.padding(inner).fillMaxSize()) {
            Box(Modifier.fillMaxSize().background(Color(0xFF0A0A14)).onSizeChanged { size = it }.drawBehind {
                tick
                val pegTop = size.height * 0.12f
                val pegGapY = (size.height * 0.6f) / rowsOfPegs
                for (r in 0 until rowsOfPegs) for (c in 0..r) {
                    val x = size.width / 2f + (c - r / 2f) * pegGapY
                    drawCircle(Color(0xFF55607A), 4f, Offset(x, pegTop + r * pegGapY))
                }
                balls.forEach { if (!it.settled) drawCircle(Color(0xFFFFD54F), 6f, Offset(it.x, it.y)) }
                // histogram
                val maxC = (bins.maxOrNull() ?: 1).coerceAtLeast(1)
                val bw = size.width.toFloat() / bins.size
                val baseY = size.height.toFloat()
                bins.forEachIndexed { i, c ->
                    val h = (c.toFloat() / maxC) * (size.height * 0.25f)
                    drawRect(Color(0xFF42A5F5), Offset(i * bw + 2, baseY - h), Size(bw - 4, h))
                }
            })
        }
    }
}
