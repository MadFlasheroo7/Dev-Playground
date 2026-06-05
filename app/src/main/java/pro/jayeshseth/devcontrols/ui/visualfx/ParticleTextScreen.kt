package pro.jayeshseth.devcontrols.ui.visualfx

import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import kotlin.random.Random

private class P(var x: Float, var y: Float, var vx: Float, var vy: Float, val tx: Float, val ty: Float)

/** Particle text — text shatters into particles and reforms on tap. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParticleTextScreen(onBack: () -> Unit) {
    var size by remember { mutableStateOf(IntSize.Zero) }
    var particles by remember { mutableStateOf<List<P>>(emptyList()) }
    var formed by remember { mutableStateOf(true) }
    var tick by remember { mutableIntStateOf(0) }

    LaunchedEffect(size) {
        if (size == IntSize.Zero) return@LaunchedEffect
        val w = size.width; val h = size.height
        val bmp = Bitmap.createBitmap(w, (h * 0.4f).toInt().coerceAtLeast(1), Bitmap.Config.ARGB_8888)
        val c = AndroidCanvas(bmp)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = AndroidColor.WHITE; textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD); textSize = w * 0.22f
        }
        c.drawText("HELLO", w / 2f, bmp.height * 0.7f, paint)
        val list = ArrayList<P>()
        val step = 5
        val oy = h * 0.3f
        var y = 0
        while (y < bmp.height) {
            var x = 0
            while (x < w) {
                if ((bmp.getPixel(x, y) ushr 24) > 128) {
                    list.add(P(Random.nextFloat() * w, Random.nextFloat() * h, 0f, 0f, x.toFloat(), y + oy))
                }
                x += step
            }
            y += step
        }
        bmp.recycle()
        particles = list
        while (true) {
            withFrameNanos {
                for (p in particles) {
                    if (formed) {
                        p.x += (p.tx - p.x) * 0.12f; p.y += (p.ty - p.y) * 0.12f
                    } else {
                        p.x += p.vx; p.y += p.vy; p.vy += 0.15f; p.vx *= 0.99f
                    }
                }
                tick++
            }
        }
    }

    fun explode() {
        for (p in particles) { val a = Random.nextDouble(0.0, 6.283).toFloat(); val s = Random.nextFloat() * 18f + 4f; p.vx = kotlin.math.cos(a) * s; p.vy = kotlin.math.sin(a) * s - 6f }
        formed = false
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Particle Text") }, navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }) },
        modifier = Modifier.fillMaxSize()
    ) { inner ->
        Column(Modifier.padding(inner).fillMaxSize()) {
            Text("Tap: shatter ↔ reform (${particles.size} particles)", Modifier.padding(8.dp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Box(Modifier.fillMaxSize().background(Color(0xFF06060E)).onSizeChanged { size = it }
                .pointerInput(particles) { detectTapGestures { if (formed) explode() else formed = true } }
                .drawBehind {
                    tick
                    for (p in particles) drawCircle(Color(0xFF4FC3F7), 2.4f, Offset(p.x, p.y))
                })
        }
    }
}
