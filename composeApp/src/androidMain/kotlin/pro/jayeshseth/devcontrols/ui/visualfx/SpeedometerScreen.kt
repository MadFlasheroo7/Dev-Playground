package pro.jayeshseth.devcontrols.ui.visualfx

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** Speedometer — an animated gauge with a sweeping needle, colored arc, ticks + digital readout. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpeedometerScreen(onBack: () -> Unit) {
    val maxSpeed = 240f
    var target by remember { mutableFloatStateOf(0f) }
    val speed by animateFloatAsState(target, spring(Spring.DampingRatioLowBouncy, Spring.StiffnessLow), label = "spd")

    val startDeg = 150f   // arc start (bottom-left)
    val sweepDeg = 240f   // total sweep

    Scaffold(
        topBar = { TopAppBar(title = { Text("Speedometer") }, navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }) },
        modifier = Modifier.fillMaxSize()
    ) { inner ->
        Column(Modifier.padding(inner).fillMaxSize().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.fillMaxWidth().aspectRatio(1f).drawBehind {
                val cx = size.width / 2f; val cy = size.height / 2f
                val radius = size.minDimension / 2f * 0.82f
                val stroke = radius * 0.13f

                // track
                drawArc(Color(0xFF1E2230), startDeg, sweepDeg, false, topLeft = Offset(cx - radius, cy - radius), size = Size(radius * 2, radius * 2), style = Stroke(stroke, cap = StrokeCap.Round))
                // colored progress arc
                val frac = (speed / maxSpeed).coerceIn(0f, 1f)
                drawArc(
                    Brush.sweepGradient(listOf(Color(0xFF00E676), Color(0xFFFFEB3B), Color(0xFFFF3D00), Color(0xFFFF3D00))),
                    startDeg, sweepDeg * frac, false,
                    topLeft = Offset(cx - radius, cy - radius), size = Size(radius * 2, radius * 2), style = Stroke(stroke, cap = StrokeCap.Round)
                )
                // ticks + labels
                val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                    color = android.graphics.Color.LTGRAY; textAlign = android.graphics.Paint.Align.CENTER; textSize = radius * 0.1f
                }
                val majors = 12
                for (k in 0..majors) {
                    val a = Math.toRadians((startDeg + sweepDeg * k / majors).toDouble())
                    val r0 = radius - stroke; val r1 = radius - stroke - (if (k % 2 == 0) radius * 0.1f else radius * 0.05f)
                    drawLine(Color(0xFFB0BEC5), Offset(cx + cos(a).toFloat() * r0, cy + sin(a).toFloat() * r0), Offset(cx + cos(a).toFloat() * r1, cy + sin(a).toFloat() * r1), 3f)
                    if (k % 2 == 0) {
                        val lr = radius - stroke - radius * 0.24f
                        drawContext.canvas.nativeCanvas.drawText("${(maxSpeed * k / majors).toInt()}", cx + cos(a).toFloat() * lr, cy + sin(a).toFloat() * lr + paint.textSize / 3, paint)
                    }
                }
                // needle
                val na = Math.toRadians((startDeg + sweepDeg * frac).toDouble())
                val tip = Offset(cx + cos(na).toFloat() * radius * 0.78f, cy + sin(na).toFloat() * radius * 0.78f)
                val tail = Offset(cx - cos(na).toFloat() * radius * 0.16f, cy - sin(na).toFloat() * radius * 0.16f)
                drawLine(Color(0xFFFF5252), tail, tip, strokeWidth = stroke * 0.35f, cap = StrokeCap.Round)
                drawCircle(Color(0xFFFF5252), stroke * 0.4f, Offset(cx, cy))
                drawCircle(Color(0xFF101316), stroke * 0.18f, Offset(cx, cy))
            }, contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(top = 40.dp)) {
                    Text("${speed.toInt()}", color = Color.White, fontSize = 56.sp, fontWeight = FontWeight.Bold)
                    Text("km/h", color = Color(0xFF90A4AE))
                }
            }
            Spacer(Modifier.height(20.dp))
            Text("Throttle", style = MaterialTheme.typography.labelMedium)
            Slider(value = target, onValueChange = { target = it }, valueRange = 0f..maxSpeed, modifier = Modifier.fillMaxWidth())
        }
    }
}
