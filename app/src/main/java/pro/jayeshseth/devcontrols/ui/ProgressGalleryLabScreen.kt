package pro.jayeshseth.devcontrols.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import kotlin.math.sin

/** Progress gallery — determinate bar, segmented, wavy (M3-style), and a ring countdown. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProgressGalleryLabScreen(onBack: () -> Unit) {
    var p by remember { mutableFloatStateOf(0.4f) }
    val inf = rememberInfiniteTransition(label = "p")
    val phase by inf.animateFloat(0f, (2 * Math.PI).toFloat(),
        infiniteRepeatable(tween(1400, easing = LinearEasing), RepeatMode.Restart), label = "ph")

    Scaffold(
        topBar = { TopAppBar(title = { Text("Progress Gallery") }, navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }) },
        modifier = Modifier.fillMaxSize()
    ) { inner ->
        Column(Modifier.padding(inner).fillMaxSize().padding(16.dp)) {
            Text("Progress: ${(p * 100).toInt()}%")
            Slider(value = p, onValueChange = { p = it })
            Spacer(Modifier.height(16.dp))

            val primary = MaterialTheme.colorScheme.primary
            val track = MaterialTheme.colorScheme.surfaceVariant

            Text("Determinate", style = MaterialTheme.typography.labelMedium)
            Canvas(Modifier.fillMaxWidth().height(12.dp)) {
                drawRoundRect(track, size = size, cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.height / 2))
                drawRoundRect(primary, size = Size(size.width * p, size.height), cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.height / 2))
            }
            Spacer(Modifier.height(20.dp))

            Text("Segmented", style = MaterialTheme.typography.labelMedium)
            Canvas(Modifier.fillMaxWidth().height(12.dp)) {
                val segs = 10; val gap = 6f; val w = (size.width - gap * (segs - 1)) / segs
                for (i in 0 until segs) {
                    val on = (i + 1f) / segs <= p + 1e-3f
                    drawRoundRect(if (on) primary else track, topLeft = Offset(i * (w + gap), 0f),
                        size = Size(w, size.height), cornerRadius = androidx.compose.ui.geometry.CornerRadius(3f))
                }
            }
            Spacer(Modifier.height(20.dp))

            Text("Wavy", style = MaterialTheme.typography.labelMedium)
            Canvas(Modifier.fillMaxWidth().height(28.dp)) {
                val mid = size.height / 2; val amp = 5f; val k = 0.06f
                val filled = size.width * p
                val path = Path().apply {
                    moveTo(0f, mid)
                    var x = 0f
                    while (x <= filled) { lineTo(x, mid + amp * sin(x * k + phase)); x += 4f }
                }
                drawLine(track, Offset(filled, mid), Offset(size.width, mid), strokeWidth = 4f, cap = StrokeCap.Round)
                drawPath(path, primary, style = Stroke(width = 4f, cap = StrokeCap.Round))
            }
            Spacer(Modifier.height(24.dp))

            Text("Ring", style = MaterialTheme.typography.labelMedium)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                Canvas(Modifier.size(120.dp)) {
                    val stroke = 14f
                    drawArc(track, -90f, 360f, false, style = Stroke(stroke, cap = StrokeCap.Round),
                        size = Size(size.width - stroke, size.height - stroke), topLeft = Offset(stroke / 2, stroke / 2))
                    drawArc(primary, -90f, 360f * p, false, style = Stroke(stroke, cap = StrokeCap.Round),
                        size = Size(size.width - stroke, size.height - stroke), topLeft = Offset(stroke / 2, stroke / 2))
                }
            }
        }
    }
}
