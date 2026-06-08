package pro.jayeshseth.devcontrols.ui

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.min
import kotlin.math.roundToInt

/** Compass — rotation-vector → azimuth driving a rotating dial. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompassLabScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var azimuth by remember { mutableFloatStateOf(0f) }

    DisposableEffect(Unit) {
        val sm = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        val sensor = sm?.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        val r = FloatArray(9); val orient = FloatArray(3)
        val listener = object : SensorEventListener {
            override fun onSensorChanged(e: SensorEvent) {
                SensorManager.getRotationMatrixFromVector(r, e.values)
                SensorManager.getOrientation(r, orient)
                val deg = ((Math.toDegrees(orient[0].toDouble()).toFloat()) + 360f) % 360f
                azimuth = azimuth * 0.85f + deg * 0.15f
            }
            override fun onAccuracyChanged(s: Sensor?, a: Int) {}
        }
        if (sensor != null) sm.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_UI)
        onDispose { sm?.unregisterListener(listener) }
    }

    val cardinal = listOf("N", "NE", "E", "SE", "S", "SW", "W", "NW")[((azimuth + 22.5f) / 45f).toInt() % 8]

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Compass") },
                navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }
            )
        },
        modifier = Modifier.fillMaxSize()
    ) { inner ->
        Column(Modifier.padding(inner).fillMaxSize().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.fillMaxSize().background(Color(0xFF06060E)).drawBehind {
                val c = Offset(size.width / 2f, size.height / 2f)
                val rad = min(size.width, size.height) * 0.38f
                drawCircle(Color(0xFF1B1B28), radius = rad, center = c, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 6f))
                rotate(-azimuth, pivot = c) {
                    val paint = android.graphics.Paint().apply { color = android.graphics.Color.WHITE; textSize = 46f; textAlign = android.graphics.Paint.Align.CENTER; isAntiAlias = true }
                    val red = android.graphics.Paint().apply { color = android.graphics.Color.rgb(0xFF, 0x5F, 0x6D); textSize = 52f; textAlign = android.graphics.Paint.Align.CENTER; isFakeBoldText = true; isAntiAlias = true }
                    drawIntoCanvasText("N", c.x, c.y - rad + 20f, red)
                    drawIntoCanvasText("E", c.x + rad - 20f, c.y + 16f, paint)
                    drawIntoCanvasText("S", c.x, c.y + rad - 6f, paint)
                    drawIntoCanvasText("W", c.x - rad + 20f, c.y + 16f, paint)
                    // needle
                    drawLine(Color(0xFFFF5F6D), c, Offset(c.x, c.y - rad * 0.7f), strokeWidth = 10f)
                    drawLine(Color(0xFFE6EDF3), c, Offset(c.x, c.y + rad * 0.7f), strokeWidth = 10f)
                }
                drawCircle(Color.White, 10f, c)
            }, contentAlignment = Alignment.BottomCenter) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(bottom = 24.dp)) {
                    Text("${azimuth.roundToInt()}°", color = Color.White, fontSize = 40.sp, fontWeight = FontWeight.Bold)
                    Text(cardinal, color = MaterialTheme.colorScheme.primary, fontSize = 20.sp)
                }
            }
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawIntoCanvasText(text: String, x: Float, y: Float, paint: android.graphics.Paint) {
    drawContext.canvas.nativeCanvas.drawText(text, x, y, paint)
}
