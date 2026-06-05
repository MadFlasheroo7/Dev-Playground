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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import kotlin.math.sqrt
import kotlin.random.Random

private class Circ(val x: Float, val y: Float, var r: Float, var growing: Boolean, val hue: Float)

/** Circle packing — circles seed at random spots and grow until they kiss a neighbour or edge. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CirclePackingLabScreen(onBack: () -> Unit) {
    val circles = remember { mutableListOf<Circ>() }
    var size by remember { mutableStateOf(IntSize.Zero) }
    var tick by remember { mutableIntStateOf(0) }

    LaunchedEffect(size) {
        if (size == IntSize.Zero) return@LaunchedEffect
        while (true) {
            withFrameNanos {
                // try to add a few new seeds
                repeat(5) {
                    if (circles.size > 1400) return@repeat
                    val x = Random.nextFloat() * size.width; val y = Random.nextFloat() * size.height
                    if (circles.none { sqrt((it.x - x) * (it.x - x) + (it.y - y) * (it.y - y)) < it.r + 2 })
                        circles.add(Circ(x, y, 1f, true, Random.nextFloat() * 360f))
                }
                for (c in circles) if (c.growing) {
                    if (c.x - c.r < 0 || c.x + c.r > size.width || c.y - c.r < 0 || c.y + c.r > size.height) { c.growing = false; continue }
                    var touch = false
                    for (o in circles) if (o !== c) {
                        val d = sqrt((o.x - c.x) * (o.x - c.x) + (o.y - c.y) * (o.y - c.y))
                        if (d < c.r + o.r + 1) { touch = true; break }
                    }
                    if (touch) c.growing = false else c.r += 0.8f
                }
                tick++
            }
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Circle Packing") }, navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }) },
        modifier = Modifier.fillMaxSize()
    ) { inner ->
        Column(Modifier.padding(inner).fillMaxSize()) {
            Box(Modifier.fillMaxSize().background(Color(0xFF0A0A12)).onSizeChanged { size = it }.drawBehind {
                tick
                for (c in circles) drawCircle(Color.hsv(c.hue, 0.6f, 0.95f), c.r, Offset(c.x, c.y), style = Stroke(2f))
            })
        }
    }
}
