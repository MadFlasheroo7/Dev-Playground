package pro.jayeshseth.devcontrols.ui.present

import androidx.compose.animation.core.withInfiniteAnimationFrameMillis
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import kotlin.math.cos
import kotlin.math.sin

/**
 * Built-in animated backgrounds drawn with the Compose [Canvas]/DrawScope — pure Compose, so they
 * run on every platform (no RuntimeShader/skiko needed). Selected per slide via [Slide.bgCanvas];
 * drawn over the slide's gradient so its [Slide.bgColors] still tint the look.
 */
val canvasBgKeys: List<String> = listOf("Dots", "Waves", "Grid", "Orbs", "Confetti")

@Composable
fun CanvasBackground(key: String, modifier: Modifier = Modifier) {
    when (key) {
        "Dots" -> DotsBg(modifier)
        "Waves" -> WavesBg(modifier)
        "Grid" -> GridBg(modifier)
        "Orbs" -> OrbsBg(modifier)
        "Confetti" -> ConfettiBg(modifier)
        else -> {} // unknown key -> nothing (the gradient base still shows)
    }
}

@Composable
private fun animTime(): Float {
    val t by produceState(0f) { while (true) { withInfiniteAnimationFrameMillis { value = it / 1000f } } }
    return t
}

@Composable
private fun DotsBg(modifier: Modifier) {
    val t = animTime()
    Canvas(modifier.fillMaxSize()) {
        val sp = 46f
        var y = sp / 2
        while (y < size.height) {
            var x = sp / 2
            while (x < size.width) {
                val r = 2.5f + 2.5f * (0.5f + 0.5f * sin(t * 2f + (x + y) * 0.012f))
                drawCircle(Color.White.copy(alpha = 0.16f), r, Offset(x, y))
                x += sp
            }
            y += sp
        }
    }
}

@Composable
private fun WavesBg(modifier: Modifier) {
    val t = animTime()
    Canvas(modifier.fillMaxSize()) {
        val rows = 6
        for (i in 0 until rows) {
            val baseY = size.height * (i + 0.5f) / rows
            val amp = size.height / (rows * 2f)
            var x = 0f
            var prev = Offset(0f, baseY)
            while (x <= size.width) {
                val y = baseY + amp * sin(t * 1.3f + x * 0.012f + i)
                if (x > 0f) drawLine(Color.White.copy(alpha = 0.12f + 0.03f * i), prev, Offset(x, y), strokeWidth = 2f)
                prev = Offset(x, y); x += 12f
            }
        }
    }
}

@Composable
private fun GridBg(modifier: Modifier) {
    val t = animTime()
    Canvas(modifier.fillMaxSize()) {
        val sp = 54f
        val off = (t * 18f) % sp
        var x = -sp + off
        while (x < size.width) { drawLine(Color.White.copy(alpha = 0.10f), Offset(x, 0f), Offset(x, size.height), 1.5f); x += sp }
        var y = -sp + off
        while (y < size.height) { drawLine(Color.White.copy(alpha = 0.10f), Offset(0f, y), Offset(size.width, y), 1.5f); y += sp }
    }
}

@Composable
private fun OrbsBg(modifier: Modifier) {
    val t = animTime()
    Canvas(modifier.fillMaxSize()) {
        val cols = listOf(Color(0x334C6FFF), Color(0x3322FFAA), Color(0x33FF5C8A), Color(0x33FFC857))
        for (i in cols.indices) {
            val cx = size.width * (0.5f + 0.4f * sin(t * 0.5f + i * 1.7f))
            val cy = size.height * (0.5f + 0.4f * cos(t * 0.4f + i * 2.1f))
            drawCircle(cols[i], radius = size.minDimension * 0.35f, center = Offset(cx, cy))
        }
    }
}

@Composable
private fun ConfettiBg(modifier: Modifier) {
    val t = animTime()
    Canvas(modifier.fillMaxSize()) {
        val cols = listOf(Color(0x554C6FFF), Color(0x5522FFAA), Color(0x55FF5C8A), Color(0x55FFC857), Color(0x55FFFFFF))
        val n = 60
        for (i in 0 until n) {
            val seed = i * 12.9898f
            val x = (size.width * fract(sin(seed) * 43758.5f))
            val fall = (t * (40f + 30f * fract(sin(seed * 1.7f) * 1000f)) + i * 37f) % (size.height + 40f)
            val y = fall - 20f
            drawCircle(cols[i % cols.size], radius = 3.5f, center = Offset(x, y))
        }
    }
}

private fun fract(v: Float): Float = v - kotlin.math.floor(v)
