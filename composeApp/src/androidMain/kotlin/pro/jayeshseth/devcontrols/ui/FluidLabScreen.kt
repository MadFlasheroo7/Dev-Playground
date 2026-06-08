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
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntSize
import kotlin.math.sqrt

/** Particle fluid — gravity + short-range separation via a spatial grid; tilt the phone to slosh. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FluidLabScreen(onBack: () -> Unit) {
    val n = 150
    val px = remember { FloatArray(n) }; val py = remember { FloatArray(n) }
    val vx = remember { FloatArray(n) }; val vy = remember { FloatArray(n) }
    var size by remember { mutableStateOf(IntSize.Zero) }
    var seeded by remember { mutableIntStateOf(0) }
    var tick by remember { mutableIntStateOf(0) }
    var gx by remember { mutableFloatStateOf(0f) }
    var gy by remember { mutableFloatStateOf(0.35f) }
    val context = LocalContext.current

    DisposableEffect(Unit) {
        val sm = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val acc = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val l = object : SensorEventListener {
            override fun onSensorChanged(e: SensorEvent) { gx = -e.values[0] * 0.05f; gy = e.values[1] * 0.05f + 0.15f }
            override fun onAccuracyChanged(s: Sensor?, a: Int) {}
        }
        sm.registerListener(l, acc, SensorManager.SENSOR_DELAY_GAME)
        onDispose { sm.unregisterListener(l) }
    }

    val rad = 13f
    LaunchedEffect(size) {
        if (size == IntSize.Zero) return@LaunchedEffect
        for (i in 0 until n) { px[i] = size.width * 0.3f + (i % 12) * rad; py[i] = size.height * 0.2f + (i / 12) * rad }
        seeded = 1
        val cell = rad * 2f
        while (true) {
            withFrameNanos {
                val cols = (size.width / cell).toInt() + 1
                val grid = HashMap<Int, MutableList<Int>>()
                for (i in 0 until n) {
                    vx[i] += gx; vy[i] += gy
                    val key = (px[i] / cell).toInt() + (py[i] / cell).toInt() * cols
                    grid.getOrPut(key) { ArrayList() }.add(i)
                }
                // separation against neighbours in 3x3 cells
                for (i in 0 until n) {
                    val cxi = (px[i] / cell).toInt(); val cyi = (py[i] / cell).toInt()
                    for (oy in -1..1) for (ox in -1..1) {
                        val list = grid[(cxi + ox) + (cyi + oy) * cols] ?: continue
                        for (j in list) if (j > i) {
                            val dx = px[j] - px[i]; val dy = py[j] - py[i]; val d = sqrt(dx * dx + dy * dy)
                            if (d in 0.01f..(2 * rad)) {
                                val nx = dx / d; val ny = dy / d; val push = (2 * rad - d) * 0.5f
                                px[i] -= nx * push; py[i] -= ny * push; px[j] += nx * push; py[j] += ny * push
                                val rel = (vx[j] - vx[i]) * nx + (vy[j] - vy[i]) * ny
                                if (rel < 0) { vx[i] += rel * nx * 0.5f; vy[i] += rel * ny * 0.5f; vx[j] -= rel * nx * 0.5f; vy[j] -= rel * ny * 0.5f }
                            }
                        }
                    }
                }
                for (i in 0 until n) {
                    vx[i] *= 0.99f; vy[i] *= 0.99f; px[i] += vx[i]; py[i] += vy[i]
                    if (px[i] < rad) { px[i] = rad; vx[i] *= -0.4f }; if (px[i] > size.width - rad) { px[i] = size.width - rad; vx[i] *= -0.4f }
                    if (py[i] < rad) { py[i] = rad; vy[i] *= -0.4f }; if (py[i] > size.height - rad) { py[i] = size.height - rad; vy[i] *= -0.4f }
                }
                tick++
            }
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Fluid") }, navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }) },
        modifier = Modifier.fillMaxSize()
    ) { inner ->
        Column(Modifier.padding(inner).fillMaxSize()) {
            Box(Modifier.fillMaxSize().background(Color(0xFF04060E)).onSizeChanged { size = it }.drawBehind {
                tick
                if (seeded == 0) return@drawBehind
                for (i in 0 until n) drawCircle(Color(0xFF29B6F6), rad, Offset(px[i], py[i]))
            })
        }
    }
}
