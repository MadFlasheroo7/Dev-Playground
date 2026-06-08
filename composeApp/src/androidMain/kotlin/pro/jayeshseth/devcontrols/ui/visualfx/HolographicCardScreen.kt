package pro.jayeshseth.devcontrols.ui.visualfx

import android.content.Context
import android.graphics.RuntimeShader
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

private const val HOLO_AGSL = """
uniform float2 iResolution;
uniform float2 tilt;
half4 main(float2 fragCoord) {
    float2 uv = fragCoord / iResolution;
    float d = (uv.x + uv.y) * 0.5 + tilt.x * 0.6 + tilt.y * 0.4;
    float band = d * 3.5 + tilt.x * 2.0;
    half3 rainbow = half3(0.5) + half3(0.5) * half3(cos(6.2831 * (band + float3(0.0, 0.33, 0.67))));
    // diagonal sheen sweeping with tilt
    float sheen = smoothstep(0.35, 0.0, abs(fract(d * 1.5 - tilt.y) - 0.5));
    // fine sparkle
    float spk = fract(sin(dot(floor(uv * 120.0), float2(12.99, 78.23))) * 43758.5);
    spk = step(0.985, spk) * (0.5 + 0.5 * sin(tilt.x * 10.0 + uv.x * 50.0));
    half3 base = half3(0.06, 0.07, 0.12);
    half3 col = base + rainbow * half(0.55) + half(sheen) * half3(1.0) * half(0.5) + half(spk);
    return half4(col, 1.0);
}
"""

/** Holographic foil card — iridescent sheen that shifts as you tilt the phone (gyro/accel + AGSL). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HolographicCardScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var tx by remember { mutableFloatStateOf(0f) }
    var ty by remember { mutableFloatStateOf(0f) }

    DisposableEffect(Unit) {
        val sm = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val acc = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val l = object : SensorEventListener {
            override fun onSensorChanged(e: SensorEvent) {
                tx = (tx * 0.85f + (e.values[0] / 9.81f) * 0.15f)
                ty = (ty * 0.85f + (e.values[1] / 9.81f) * 0.15f)
            }
            override fun onAccuracyChanged(s: Sensor?, a: Int) {}
        }
        sm.registerListener(l, acc, SensorManager.SENSOR_DELAY_GAME)
        onDispose { sm.unregisterListener(l) }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Holographic Card") }, navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }) },
        modifier = Modifier.fillMaxSize()
    ) { inner ->
        Column(Modifier.padding(inner).fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                Text("Needs Android 13 (API 33)+ for AGSL."); return@Column
            }
            Spacer(Modifier.height(40.dp))
            val shader = remember { RuntimeShader(HOLO_AGSL) }
            val brush = remember { ShaderBrush(shader) }
            Box(
                Modifier.fillMaxWidth().aspectRatio(0.66f)
                    .graphicsLayer { rotationY = (tx * 12f).coerceIn(-12f, 12f); rotationX = (-ty * 12f).coerceIn(-12f, 12f); cameraDistance = 16f * density }
                    .clip(androidx.compose.foundation.shape.RoundedCornerShape(20.dp))
                    .drawWithCache {
                        shader.setFloatUniform("iResolution", size.width, size.height)
                        onDrawBehind { shader.setFloatUniform("tilt", tx, ty); drawRect(brush) }
                    }
            )
            Spacer(Modifier.height(24.dp))
            Text("Tilt the phone — the foil shifts and sparkles.")
        }
    }
}
