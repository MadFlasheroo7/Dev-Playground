package pro.jayeshseth.devcontrols.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

private class Drop(var x: Float, var y: Float, var len: Float, var sp: Float)

/** Procedural weather — rain or snow falling over an animated gradient sky. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeatherLabScreen(onBack: () -> Unit) {
    var mode by remember { mutableIntStateOf(0) } // 0 rain, 1 snow
    val drops = remember { MutableList(220) { Drop(Random.nextFloat(), Random.nextFloat(), Random.nextFloat(), 0f) } }
    var tick by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        while (true) {
            withFrameNanos {
                for (d in drops) {
                    if (mode == 0) {
                        d.sp = 0.025f + d.len * 0.03f; d.y += d.sp; d.x += 0.004f
                    } else {
                        d.sp = 0.004f + d.len * 0.006f; d.y += d.sp; d.x += sin((d.y + d.len) * 8f) * 0.0015f
                    }
                    if (d.y > 1.05f) { d.y = -0.05f; d.x = Random.nextFloat(); d.len = Random.nextFloat() }
                }
                tick++
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Weather") },
                navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }
            )
        },
        modifier = Modifier.fillMaxSize()
    ) { inner ->
        Column(Modifier.padding(inner).fillMaxSize()) {
            Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(mode == 0, { mode = 0 }, { Text("Rain") })
                FilterChip(mode == 1, { mode = 1 }, { Text("Snow") })
            }
            Box(Modifier.fillMaxSize()
                .background(Brush.verticalGradient(listOf(Color(0xFF1A2238), Color(0xFF394867), Color(0xFF9BA4B5))))
                .drawBehind {
                    tick
                    val w = size.width; val h = size.height
                    for (d in drops) {
                        val px = d.x * w
                        val py = d.y * h
                        if (mode == 0) {
                            drawLine(Color(0x99CFE8FF), Offset(px, py), Offset(px - 2f, py + 14f + d.len * 14f), strokeWidth = 1.5f)
                        } else {
                            drawCircle(Color(0xCCFFFFFF), 1.5f + d.len * 3f, Offset(px, py))
                        }
                    }
                }
            )
        }
    }
}
