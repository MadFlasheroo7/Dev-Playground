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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp

private const val HOLE_AGSL = """
uniform shader content;
uniform float2 iResolution;
uniform float3 hole; // x, y, radius
half4 main(float2 fragCoord) {
    float2 c = hole.xy; float r = hole.z;
    float2 d = fragCoord - c; float dist = length(d) + 0.001;
    if (dist < r * 0.25) return half4(0.0, 0.0, 0.0, 1.0);
    float pull = (r * r) / (dist * dist) * 16.0;
    float2 dir = d / dist;
    half3 col = content.eval(fragCoord - dir * pull).rgb;
    float ring = smoothstep(r * 0.34, r * 0.25, dist);
    col += half3(0.4, 0.55, 1.0) * half(ring);
    return half4(col, 1.0);
}
"""

/** Black hole — drag a gravitational lens that bends the live UI around an event horizon. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BlackHoleScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = { TopAppBar(title = { Text("Black Hole") }, navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }) },
        modifier = Modifier.fillMaxSize()
    ) { inner ->
        Column(Modifier.padding(inner).fillMaxSize()) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) { Text("Needs Android 13 (API 33)+.", Modifier.padding(16.dp)); return@Column }
            HoleContent()
        }
    }
}

@Composable
private fun HoleContent() {
    val shader = remember { RuntimeShader(HOLE_AGSL) }
    var hx by remember { mutableFloatStateOf(-1f) }
    var hy by remember { mutableFloatStateOf(-1f) }
    val r = with(LocalDensity.current) { 130.dp.toPx() }
    Box(
        Modifier.fillMaxSize()
            .graphicsLayer {
                if (hx < 0f) { hx = size.width / 2f; hy = size.height / 2f }
                shader.setFloatUniform("iResolution", size.width, size.height)
                shader.setFloatUniform("hole", hx, hy, r)
                renderEffect = RenderEffect.createRuntimeShaderEffect(shader, "content").asComposeRenderEffect()
            }
            .pointerInput(Unit) { detectDragGestures { ch, _ -> ch.consume(); hx = ch.position.x; hy = ch.position.y } }
    ) {
        LazyVerticalGrid(columns = GridCells.Fixed(4), modifier = Modifier.fillMaxSize().background(Color(0xFF0A0A16)).padding(6.dp)) {
            items((1..60).toList()) { i ->
                Box(Modifier.padding(4.dp).aspectRatio(1f).background(Color.hsv((i * 18f) % 360f, 0.7f, 0.95f), androidx.compose.foundation.shape.RoundedCornerShape(10.dp)))
            }
        }
    }
}
