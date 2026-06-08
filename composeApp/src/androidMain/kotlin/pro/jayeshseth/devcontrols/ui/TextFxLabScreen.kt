package pro.jayeshseth.devcontrols.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.sin

/**
 * Text FX lab: animated gradient-brush text, a per-character wave, a `basicMarquee`
 * ticker, and text rendered along a curved path (native `drawTextOnPath`).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TextFxLabScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Text FX") },
                navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }
            )
        },
        modifier = Modifier.fillMaxSize()
    ) { inner ->
        val tr = rememberInfiniteTransition(label = "t")
        val shift by tr.animateFloat(0f, 600f, infiniteRepeatable(tween(2500, easing = LinearEasing), RepeatMode.Restart), label = "g")
        val wave by tr.animateFloat(0f, (2 * Math.PI).toFloat(), infiniteRepeatable(tween(1400, easing = LinearEasing), RepeatMode.Restart), label = "w")

        Column(Modifier.padding(inner).fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
            Label("Gradient brush text")
            Text(
                "GRADIENT",
                style = TextStyle(
                    brush = Brush.linearGradient(
                        listOf(Color(0xFF00C9FF), Color(0xFF92FE9D), Color(0xFFE100FF), Color(0xFF00C9FF)),
                        start = Offset(shift, 0f), end = Offset(shift + 300f, 120f),
                        tileMode = TileMode.Repeated
                    ),
                    fontSize = 52.sp, fontWeight = FontWeight.Black
                )
            )

            Spacer(Modifier.height(24.dp))
            Label("Per-character wave")
            Row {
                "BOUNCE".forEachIndexed { i, ch ->
                    Text(
                        ch.toString(),
                        fontSize = 40.sp, fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.graphicsLayer { translationY = sin(wave + i * 0.6f) * 18f }
                    )
                }
            }

            Spacer(Modifier.height(24.dp))
            Label("Marquee")
            Text(
                "This text scrolls forever via Modifier.basicMarquee() — handy for long single-line labels.   •   ",
                modifier = Modifier.fillMaxWidth().basicMarquee(),
                maxLines = 1, fontSize = 18.sp
            )

            Spacer(Modifier.height(24.dp))
            Label("Text on a path")
            Box(
                Modifier.fillMaxWidth().height(160.dp).drawBehind {
                    drawIntoCanvas { canvas ->
                        val paint = android.graphics.Paint().apply {
                            color = android.graphics.Color.WHITE
                            textSize = 44f
                            isAntiAlias = true
                            isFakeBoldText = true
                        }
                        val path = android.graphics.Path().apply {
                            moveTo(20f, size.height * 0.8f)
                            quadTo(size.width / 2f, -size.height * 0.4f, size.width - 20f, size.height * 0.8f)
                        }
                        canvas.nativeCanvas.drawTextOnPath("text following a curve", path, 0f, 0f, paint)
                    }
                }
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun Label(s: String) =
    Text(s, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
