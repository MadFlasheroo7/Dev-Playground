package pro.jayeshseth.devcontrols.ui.visualfx

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.LocalContext
import kotlin.math.sin

/** 2.5D parallax — a layered scene whose depth layers shift as you tilt the phone (accelerometer). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DepthParallaxScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var tx by remember { mutableFloatStateOf(0f) }
    var ty by remember { mutableFloatStateOf(0f) }
    DisposableEffect(Unit) {
        val sm = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val acc = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val l = object : SensorEventListener {
            override fun onSensorChanged(e: SensorEvent) { tx = tx * 0.85f + (e.values[0] / 9.81f) * 0.15f; ty = ty * 0.85f + (e.values[1] / 9.81f) * 0.15f }
            override fun onAccuracyChanged(s: Sensor?, a: Int) {}
        }
        sm.registerListener(l, acc, SensorManager.SENSOR_DELAY_GAME)
        onDispose { sm.unregisterListener(l) }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("2.5D Parallax") }, navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }) },
        modifier = Modifier.fillMaxSize()
    ) { inner ->
        Column(Modifier.padding(inner).fillMaxSize()) {
            Box(Modifier.fillMaxSize().background(Color(0xFF0B1026)).drawBehind {
                val w = size.width; val h = size.height
                fun ox(depth: Float) = -tx * depth * 60f
                fun oy(depth: Float) = -ty * depth * 30f
                // sky
                drawRect(Brush.verticalGradient(listOf(Color(0xFF12183A), Color(0xFF3A2A5A))), size = size)
                // moon
                drawCircle(Color(0xFFFFF3C4), w * 0.08f, Offset(w * 0.72f + ox(0.3f), h * 0.25f + oy(0.3f)))
                // far hills
                fun hill(baseY: Float, amp: Float, color: Color, depth: Float, k: Float) {
                    val p = Path().apply {
                        moveTo(0f, h)
                        val o = ox(depth); val oo = oy(depth)
                        var x = 0f
                        moveTo(-100f + o, h)
                        var first = true
                        while (x <= w + 100) {
                            val y = baseY + oo + sin(x * k) * amp
                            if (first) { lineTo(x + o, y); first = false } else lineTo(x + o, y)
                            x += 20f
                        }
                        lineTo(w + 100 + o, h); close()
                    }
                    drawPath(p, color)
                }
                hill(h * 0.55f, 40f, Color(0xFF2A3566), 0.6f, 0.004f)
                hill(h * 0.7f, 60f, Color(0xFF1E2750), 1.0f, 0.006f)
                hill(h * 0.85f, 50f, Color(0xFF11162E), 1.6f, 0.008f)
                drawRect(Color(0xFF0A0D1C), Offset(0f, h * 0.92f + oy(2f)), Size(w, h))
            })
        }
    }
}
