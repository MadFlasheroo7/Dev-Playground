package pro.jayeshseth.devcontrols.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

/**
 * HSV colour-wheel picker. The wheel is a hue sweep-gradient with a white→transparent
 * radial overlay for saturation; touch position maps to (hue = angle, saturation =
 * radius). A Value slider dims it. Shows the selected colour + hex.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ColorPickerLabScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Color Picker") },
                navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }
            )
        },
        modifier = Modifier.fillMaxSize()
    ) { inner ->
        var hue by remember { mutableFloatStateOf(200f) }
        var sat by remember { mutableFloatStateOf(0.8f) }
        var value by remember { mutableFloatStateOf(1f) }
        var marker by remember { mutableStateOf(Offset.Unspecified) }

        val selected = Color.hsv(hue, sat, value)
        val hueColors = remember {
            (0..360 step 30).map { Color.hsv(it.toFloat(), 1f, 1f) }
        }

        fun pick(o: Offset, sizePx: Float) {
            val cx = sizePx / 2f; val cy = sizePx / 2f
            val dx = o.x - cx; val dy = o.y - cy
            val r = hypot(dx, dy)
            val radius = sizePx / 2f
            hue = ((Math.toDegrees(atan2(dy, dx).toDouble()) + 360) % 360).toFloat()
            sat = (r / radius).coerceIn(0f, 1f)
            marker = o
        }

        Column(Modifier.padding(inner).fillMaxSize().padding(16.dp)) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .pointerInput(Unit) {
                        detectTapGestures { pick(it, size.width.toFloat()) }
                    }
                    .pointerInput(Unit) {
                        detectDragGestures { change, _ -> pick(change.position, size.width.toFloat()); change.consume() }
                    }
                    .drawBehind {
                        val d = size.minDimension
                        val c = Offset(size.width / 2f, size.height / 2f)
                        drawCircle(Brush.sweepGradient(hueColors, center = c), radius = d / 2f, center = c)
                        drawCircle(
                            Brush.radialGradient(
                                listOf(Color.White, Color.Transparent),
                                center = c, radius = d / 2f
                            ), radius = d / 2f, center = c
                        )
                        if (marker != Offset.Unspecified) {
                            drawCircle(Color.White, radius = 14f, center = marker, style = Stroke(width = 4f))
                        }
                    }
            )
            Spacer(Modifier.height(16.dp))
            Text("Value", style = MaterialTheme.typography.labelMedium)
            Slider(value = value, onValueChange = { value = it }, valueRange = 0f..1f)

            Spacer(Modifier.height(8.dp))
            Row {
                Box(Modifier.size(72.dp).clip(RoundedCornerShape(12.dp)).background(selected))
                Spacer(Modifier.size(16.dp))
                Column {
                    Text("#%06X".format(0xFFFFFF and selected.toArgb()), fontFamily = FontFamily.Monospace)
                    Text("H ${hue.toInt()}  S ${"%.2f".format(sat)}  V ${"%.2f".format(value)}",
                        fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}
