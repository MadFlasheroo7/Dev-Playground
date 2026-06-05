package pro.jayeshseth.devcontrols.ui.visualfx

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.dp
import kotlin.random.Random

private class Shard(val cx: Float, val cy: Float, val w: Float, val h: Float, val color: Color) {
    var dx = 0f; var dy = 0f; var vx = 0f; var vy = 0f; var rot = 0f; var rotV = 0f; var alpha = 1f
}

/** Voronoi shatter — tap to crack the surface into shards that fly apart; reassemble to restore. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoronoiShatterScreen(onBack: () -> Unit) {
    val cols = 7; val rows = 12
    var shards by remember { mutableStateOf<List<Shard>>(emptyList()) }
    var exploded by remember { mutableStateOf(false) }
    var tick by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        while (true) {
            withFrameNanos {
                if (exploded) for (s in shards) { s.vy += 0.4f; s.dx += s.vx; s.dy += s.vy; s.rot += s.rotV; s.alpha = (s.alpha - 0.006f).coerceAtLeast(0f) }
                else for (s in shards) { s.dx *= 0.8f; s.dy *= 0.8f; s.rot *= 0.8f; s.alpha = (s.alpha + 0.05f).coerceAtMost(1f) }
                tick++
            }
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Voronoi Shatter") }, navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }) },
        modifier = Modifier.fillMaxSize()
    ) { inner ->
        Column(Modifier.padding(inner).fillMaxSize()) {
            Row(Modifier.fillMaxWidth().padding(8.dp)) {
                Text(if (exploded) "Shattered" else "Tap to shatter", Modifier.weight(1f).padding(top = 8.dp), style = MaterialTheme.typography.bodyMedium)
                OutlinedButton(onClick = { exploded = false; for (s in shards) { s.vx = 0f; s.vy = 0f; s.rotV = 0f } }) { Text("Reassemble") }
            }
            Box(Modifier.fillMaxSize().background(Color(0xFF07070F)).onSizeChanged { sz ->
                if (sz.width > 0 && sz.height > 0 && shards.isEmpty()) {
                    val cw = sz.width.toFloat() / cols; val ch = sz.height.toFloat() / rows
                    shards = buildList {
                        for (ry in 0 until rows) for (cx in 0 until cols) {
                            add(Shard(cx * cw + cw / 2, ry * ch + ch / 2, cw, ch, Color.hsv(((cx + ry) * 18f) % 360f, 0.6f, 0.9f)))
                        }
                    }
                }
            }
                .pointerInput(shards) {
                    detectTapGestures { tap ->
                        if (!exploded) {
                            for (s in shards) {
                                val ddx = s.cx - tap.x; val ddy = s.cy - tap.y
                                val d = kotlin.math.hypot(ddx, ddy).coerceAtLeast(1f)
                                val force = (600f / d).coerceAtMost(22f)
                                s.vx = ddx / d * force + (Random.nextFloat() - 0.5f) * 4f
                                s.vy = ddy / d * force - 6f
                                s.rotV = (Random.nextFloat() - 0.5f) * 12f
                            }
                            exploded = true
                        }
                    }
                }
                .drawBehind {
                    tick
                    for (s in shards) {
                        if (s.alpha <= 0f) continue
                        rotate(s.rot, pivot = Offset(s.cx + s.dx, s.cy + s.dy)) {
                            drawRect(s.color.copy(alpha = s.alpha), topLeft = Offset(s.cx + s.dx - s.w / 2, s.cy + s.dy - s.h / 2), size = Size(s.w - 1.5f, s.h - 1.5f))
                        }
                    }
                })
        }
    }
}
