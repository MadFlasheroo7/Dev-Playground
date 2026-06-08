package pro.jayeshseth.devcontrols.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlin.math.sqrt

private class Charge(var x: Float, var y: Float, var vx: Float, var vy: Float, val sign: Int)

/** Charged particles — Coulomb attraction/repulsion. Tap to drop + or − charges. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChargedParticlesLabScreen(onBack: () -> Unit) {
    val charges = remember { mutableListOf<Charge>() }
    var size by remember { mutableStateOf(IntSize.Zero) }
    var tick by remember { mutableIntStateOf(0) }
    var sign by remember { mutableIntStateOf(1) }

    LaunchedEffect(Unit) {
        while (true) {
            withFrameNanos {
                val n = charges.size
                for (i in 0 until n) {
                    var fx = 0f; var fy = 0f
                    for (j in 0 until n) if (i != j) {
                        val dx = charges[i].x - charges[j].x; val dy = charges[i].y - charges[j].y
                        val r2 = (dx * dx + dy * dy).coerceAtLeast(100f); val r = sqrt(r2)
                        val f = 1400f * charges[i].sign * charges[j].sign / r2
                        fx += f * dx / r; fy += f * dy / r
                    }
                    charges[i].vx = (charges[i].vx + fx) * 0.96f
                    charges[i].vy = (charges[i].vy + fy) * 0.96f
                }
                for (c in charges) {
                    c.x += c.vx; c.y += c.vy
                    if (c.x < 12) { c.x = 12f; c.vx = -c.vx * 0.7f }; if (c.x > size.width - 12) { c.x = size.width - 12f; c.vx = -c.vx * 0.7f }
                    if (c.y < 12) { c.y = 12f; c.vy = -c.vy * 0.7f }; if (c.y > size.height - 12) { c.y = size.height - 12f; c.vy = -c.vy * 0.7f }
                }
                tick++
            }
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Charged Particles") }, navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }) },
        modifier = Modifier.fillMaxSize()
    ) { inner ->
        Column(Modifier.padding(inner).fillMaxSize()) {
            Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(sign == 1, { sign = 1 }, { Text("+ positive") })
                FilterChip(sign == -1, { sign = -1 }, { Text("− negative") })
                OutlinedButton(onClick = { charges.clear() }) { Text("Clear") }
            }
            Box(Modifier.fillMaxSize().background(Color(0xFF07070F)).onSizeChanged { size = it }
                .pointerInput(Unit) { detectTapGestures { charges.add(Charge(it.x, it.y, 0f, 0f, sign)) } }
                .drawBehind {
                    tick
                    for (c in charges) {
                        drawCircle(if (c.sign > 0) Color(0xFFEF5350) else Color(0xFF42A5F5), 12f, Offset(c.x, c.y))
                    }
                })
        }
    }
}
