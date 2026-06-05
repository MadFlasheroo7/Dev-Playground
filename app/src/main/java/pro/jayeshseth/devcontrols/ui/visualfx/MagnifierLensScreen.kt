package pro.jayeshseth.devcontrols.ui.visualfx

import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp

private const val LENS_AGSL = """
uniform shader content;
uniform float2 iResolution;
uniform float3 lens; // x, y, radius
half4 main(float2 fragCoord) {
    float2 c = lens.xy; float r = lens.z;
    float2 d = fragCoord - c; float dist = length(d);
    if (dist < r) {
        float t = dist / r;
        float mag = mix(0.45, 1.0, t * t);          // magnify toward centre
        float2 src = c + d * mag;
        // glass rim darkening
        float rim = smoothstep(1.0, 0.85, t);
        return content.eval(src) * half(0.6 + 0.4 * rim);
    }
    return content.eval(fragCoord);
}
"""

/** Magnifier lens — drag a glass lens that refracts + magnifies the live content beneath it. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MagnifierLensScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = { TopAppBar(title = { Text("Magnifier Lens") }, navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }) },
        modifier = Modifier.fillMaxSize()
    ) { inner ->
        Column(Modifier.padding(inner).fillMaxSize()) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) { Text("Needs Android 13 (API 33)+.", Modifier.padding(16.dp)); return@Column }
            LensContent()
        }
    }
}

@Composable
private fun LensContent() {
    val shader = remember { RuntimeShader(LENS_AGSL) }
    var lx by remember { mutableFloatStateOf(-1f) }
    var ly by remember { mutableFloatStateOf(-1f) }
    val radiusPx = with(androidx.compose.ui.platform.LocalDensity.current) { 90.dp.toPx() }

    Box(
        Modifier.fillMaxSize()
            .graphicsLayer {
                if (lx < 0f) { lx = size.width / 2f; ly = size.height / 2f }
                shader.setFloatUniform("iResolution", size.width, size.height)
                shader.setFloatUniform("lens", lx, ly, radiusPx)
                renderEffect = android.graphics.RenderEffect.createRuntimeShaderEffect(shader, "content").asComposeRenderEffect()
            }
            .pointerInput(Unit) {
                detectDragGestures { change, _ -> change.consume(); lx = change.position.x; ly = change.position.y }
            }
    ) {
        LazyColumn(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface).padding(16.dp)) {
            items((1..40).toList()) { i ->
                Text("Row $i — drag the lens over this text to magnify and refract it.", Modifier.padding(vertical = 10.dp), style = MaterialTheme.typography.bodyLarge)
                HorizontalDivider()
            }
        }
    }
}
