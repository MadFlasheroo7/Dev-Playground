package pro.jayeshseth.devcontrols.ui

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Sensor-driven parallax lab. The accelerometer's gravity vector gives device tilt;
 * we map it to translation offsets across layered cards with increasing depth factors,
 * so tilting the phone shifts the layers at different rates — a faux-3D parallax. No
 * permission needed (motion sensors are unrestricted).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SensorParallaxLabScreen(onBack: () -> Unit) {
    val context = LocalContext()
    var tiltX by remember { mutableFloatStateOf(0f) } // -1..1 (left/right)
    var tiltY by remember { mutableFloatStateOf(0f) } // -1..1 (up/down)

    DisposableEffect(Unit) {
        val sm = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        val sensor = sm?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val listener = object : SensorEventListener {
            override fun onSensorChanged(e: SensorEvent) {
                // smooth toward the new normalized tilt
                val nx = (-e.values[0] / 9.81f).coerceIn(-1f, 1f)
                val ny = (e.values[1] / 9.81f).coerceIn(-1f, 1f)
                tiltX = tiltX * 0.8f + nx * 0.2f
                tiltY = tiltY * 0.8f + ny * 0.2f
            }
            override fun onAccuracyChanged(s: Sensor?, a: Int) {}
        }
        if (sensor != null) sm.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_GAME)
        onDispose { sm?.unregisterListener(listener) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Sensor Parallax") },
                navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }
            )
        },
        modifier = Modifier.fillMaxSize()
    ) { inner ->
        Column(Modifier.padding(inner).fillMaxSize().padding(16.dp)) {
            Text(
                "Tilt the phone — each layer shifts by its depth factor (back moves least, front most).",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(440.dp)
                    .padding(top = 12.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Brush.linearGradient(listOf(Color(0xFF0F2027), Color(0xFF203A43)))),
                contentAlignment = Alignment.Center
            ) {
                ParallaxLayer(tiltX, tiltY, depth = 12f, size = 300, color = Color(0x33FFFFFF))
                ParallaxLayer(tiltX, tiltY, depth = 26f, size = 210, color = Color(0x55FF5F6D))
                ParallaxLayer(tiltX, tiltY, depth = 44f, size = 130, color = Color(0xAA64B5F6))
                Box(Modifier.graphicsLayer {
                    translationX = tiltX * 64f
                    translationY = tiltY * 64f
                }) {
                    Text("PARALLAX", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 22.sp)
                }
            }
        }
    }
}

@Composable
private fun ParallaxLayer(tiltX: Float, tiltY: Float, depth: Float, size: Int, color: Color) {
    Box(
        Modifier
            .size(size.dp)
            .graphicsLayer {
                translationX = tiltX * depth
                translationY = tiltY * depth
            }
            .clip(RoundedCornerShape(24.dp))
            .background(color)
    )
}

@Composable
private fun LocalContext(): Context = androidx.compose.ui.platform.LocalContext.current
