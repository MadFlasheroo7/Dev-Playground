package pro.jayeshseth.devcontrols.ui

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/** Metal Detector — magnetic-field magnitude vs a calibrated baseline (TYPE_MAGNETIC_FIELD). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MetalDetectorLabScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val sm = remember { context.getSystemService(Context.SENSOR_SERVICE) as SensorManager }
    var magnitude by remember { mutableFloatStateOf(0f) }
    var baseline by remember { mutableFloatStateOf(0f) }

    DisposableEffect(Unit) {
        val mag = sm.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
        val l = object : SensorEventListener {
            override fun onSensorChanged(e: SensorEvent) {
                val m = sqrt(e.values[0] * e.values[0] + e.values[1] * e.values[1] + e.values[2] * e.values[2])
                magnitude = magnitude * 0.7f + m * 0.3f
                if (baseline == 0f) baseline = m
            }
            override fun onAccuracyChanged(s: Sensor?, a: Int) {}
        }
        sm.registerListener(l, mag, SensorManager.SENSOR_DELAY_GAME)
        onDispose { sm.unregisterListener(l) }
    }

    val delta = magnitude - baseline
    val intensity = (kotlin.math.abs(delta) / 80f).coerceIn(0f, 1f)

    Scaffold(
        topBar = { TopAppBar(title = { Text("Metal Detector") }, navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }) },
        modifier = Modifier.fillMaxSize()
    ) { inner ->
        Column(Modifier.padding(inner).fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            val ring = MaterialTheme.colorScheme.outline
            Box(Modifier.fillMaxWidth().weight(1f).drawBehind {
                val cx = size.width / 2f; val cy = size.height / 2f
                val rMax = minOf(cx, cy) * 0.92f
                for (i in 1..5) drawCircle(ring.copy(alpha = 0.3f), rMax * i / 5f, Offset(cx, cy), style = androidx.compose.ui.graphics.drawscope.Stroke(2f))
                val needle = -PI.toFloat() / 2f + intensity * PI.toFloat()
                drawLine(Color(red = intensity, green = 1f - intensity, blue = 0.2f),
                    Offset(cx, cy), Offset(cx + cos(needle) * rMax, cy + sin(needle) * rMax), strokeWidth = 8f)
                drawCircle(ring, 10f, Offset(cx, cy))
            })
            Text("${"%.1f".format(magnitude)} µT", fontSize = 44.sp, fontWeight = FontWeight.Bold,
                color = Color(red = intensity, green = 1f - intensity, blue = 0.2f))
            Text("Δ ${"%+.1f".format(delta)} µT from baseline", style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(12.dp))
            Button(onClick = { baseline = magnitude }) { Text("Recalibrate baseline") }
        }
    }
}
