package pro.jayeshseth.devcontrols.ui

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlin.math.hypot
import kotlin.math.roundToInt

/** Bubble Level — a 2-axis spirit level + pitch/roll inclinometer from the accelerometer. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BubbleLevelLabScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val sm = remember { context.getSystemService(Context.SENSOR_SERVICE) as SensorManager }
    var gx by remember { mutableFloatStateOf(0f) }
    var gy by remember { mutableFloatStateOf(0f) }

    DisposableEffect(Unit) {
        val accel = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val l = object : SensorEventListener {
            override fun onSensorChanged(e: SensorEvent) {
                gx = gx * 0.8f + e.values[0] * 0.2f
                gy = gy * 0.8f + e.values[1] * 0.2f
            }
            override fun onAccuracyChanged(s: Sensor?, a: Int) {}
        }
        sm.registerListener(l, accel, SensorManager.SENSOR_DELAY_GAME)
        onDispose { sm.unregisterListener(l) }
    }

    val roll = (gx / 9.81f).coerceIn(-1f, 1f)
    val pitch = (gy / 9.81f).coerceIn(-1f, 1f)

    Scaffold(
        topBar = { TopAppBar(title = { Text("Bubble Level") }, navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }) },
        modifier = Modifier.fillMaxSize()
    ) { inner ->
        Column(Modifier.padding(inner).fillMaxSize().padding(16.dp)) {
            val outline = MaterialTheme.colorScheme.outline
            val primary = MaterialTheme.colorScheme.primary
            val level = hypot(roll, pitch) < 0.03f
            Box(Modifier.fillMaxSize().drawBehind {
                val cx = size.width / 2f; val cy = size.height / 2f
                val r = minOf(cx, cy) * 0.9f
                drawCircle(outline, r, Offset(cx, cy), style = androidx.compose.ui.graphics.drawscope.Stroke(3f))
                drawCircle(outline.copy(alpha = 0.4f), r / 3f, Offset(cx, cy), style = androidx.compose.ui.graphics.drawscope.Stroke(2f))
                drawLine(outline, Offset(cx - r, cy), Offset(cx + r, cy))
                drawLine(outline, Offset(cx, cy - r), Offset(cx, cy + r))
                val bx = cx + roll * r
                val by = cy - pitch * r
                drawCircle(if (level) Color(0xFF4CAF50) else primary, r / 6f, Offset(bx, by))
            })
            Text("roll ${(roll * 90).roundToInt()}°   pitch ${(pitch * 90).roundToInt()}°",
                style = MaterialTheme.typography.titleMedium, color = if (level) Color(0xFF4CAF50) else MaterialTheme.colorScheme.onSurface)
        }
    }
}
