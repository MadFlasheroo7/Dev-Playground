package pro.jayeshseth.devcontrols.ui

import android.graphics.RuntimeShader
import android.os.Build
import androidx.compose.foundation.gestures.detectTransformGestures
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp

/**
 * Mandelbrot explorer (AGSL, API 33+). The set is computed in a fragment shader; pinch
 * to zoom and drag to pan — `detectTransformGestures` feeds `uCenter`/`uScale` uniforms.
 */
private const val MANDELBROT_AGSL = """
uniform float2 iResolution;
uniform float2 uCenter;
uniform float uScale;
half4 main(float2 fc) {
    float2 uv = (fc - 0.5 * iResolution) / iResolution.y * uScale + uCenter;
    float2 z = float2(0.0, 0.0);
    float i = 0.0;
    for (int n = 0; n < 240; n++) {
        z = float2(z.x * z.x - z.y * z.y, 2.0 * z.x * z.y) + uv;
        if (dot(z, z) > 4.0) { break; }
        i += 1.0;
    }
    float t = i / 240.0;
    if (i >= 240.0) return half4(0.0, 0.0, 0.0, 1.0);
    half3 col = 0.5 + 0.5 * cos(6.2831 * (t + half3(0.0, 0.33, 0.67)));
    return half4(col, 1.0);
}
"""

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MandelbrotLabScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mandelbrot") },
                navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }
            )
        },
        modifier = Modifier.fillMaxSize()
    ) { inner ->
        Column(Modifier.padding(inner).fillMaxSize()) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                Text("Needs Android 13 (API 33)+ for AGSL.", Modifier.padding(16.dp))
                return@Column
            }
            val shader = remember { RuntimeShader(MANDELBROT_AGSL) }
            val brush = remember { ShaderBrush(shader) }
            var center by remember { mutableStateOf(Offset(-0.5f, 0f)) }
            var scale by remember { mutableFloatStateOf(3f) }

            Text("Pinch to zoom, drag to pan.", style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(12.dp))
            Box(
                Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            val h = size.height.toFloat()
                            // pan in screen px → complex-plane shift (scale is plane-height shown)
                            center = Offset(
                                center.x - pan.x / h * scale,
                                center.y - pan.y / h * scale
                            )
                            scale /= zoom
                        }
                    }
                    .drawWithCache {
                        shader.setFloatUniform("iResolution", size.width, size.height)
                        onDrawBehind {
                            shader.setFloatUniform("uCenter", center.x, center.y)
                            shader.setFloatUniform("uScale", scale)
                            drawRect(brush)
                        }
                    }
            )
        }
    }
}
