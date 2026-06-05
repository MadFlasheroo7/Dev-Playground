package pro.jayeshseth.devcontrols.ui.visualfx

import android.graphics.BlurMaskFilter
import android.graphics.Paint
import android.graphics.Typeface
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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import kotlin.random.Random

/** Neon sign — glowing tube lettering with bloom and an occasional flicker. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NeonSignScreen(onBack: () -> Unit) {
    var flicker by remember { mutableFloatStateOf(1f) }
    LaunchedEffect(Unit) {
        while (true) {
            withFrameNanos {
                flicker = when {
                    Random.nextFloat() < 0.04f -> Random.nextFloat() * 0.5f
                    else -> 0.85f + Random.nextFloat() * 0.15f
                }
            }
        }
    }
    Scaffold(
        topBar = { TopAppBar(title = { Text("Neon Sign") }, navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }) },
        modifier = Modifier.fillMaxSize()
    ) { inner ->
        Column(Modifier.padding(inner).fillMaxSize()) {
            Box(Modifier.fillMaxSize().background(Color(0xFF0A0710)).drawBehind {
                val canvas = drawContext.canvas.nativeCanvas
                val cx = size.width / 2f; val cy = size.height / 2f
                val neon = 0xFFFF2D95.toInt()
                fun paint(stroke: Float, blur: Float, color: Int, alpha: Int) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    style = Paint.Style.FILL_AND_STROKE; strokeWidth = stroke
                    this.color = color; this.alpha = alpha
                    textAlign = Paint.Align.CENTER
                    typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
                    textSize = size.width * 0.26f
                    if (blur > 0f) maskFilter = BlurMaskFilter(blur, BlurMaskFilter.Blur.NORMAL)
                }
                val a = (flicker * 255).toInt().coerceIn(0, 255)
                // outer bloom layers
                canvas.drawText("NEON", cx, cy, paint(20f, 55f, neon, (a * 0.5f).toInt()))
                canvas.drawText("NEON", cx, cy, paint(14f, 28f, neon, (a * 0.7f).toInt()))
                canvas.drawText("NEON", cx, cy, paint(8f, 10f, neon, a))
                // bright white core
                canvas.drawText("NEON", cx, cy, paint(3f, 0f, 0xFFFFFFFF.toInt(), a))
            })
        }
    }
}
