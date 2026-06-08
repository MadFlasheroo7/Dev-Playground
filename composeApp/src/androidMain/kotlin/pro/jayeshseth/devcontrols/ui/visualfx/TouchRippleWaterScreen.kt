package pro.jayeshseth.devcontrols.ui.visualfx

import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.ui.unit.dp

private const val RIPPLE_AGSL = """
uniform shader content;
uniform float2 iResolution;
uniform float iTime;
uniform float rippleCount;
uniform float ripples[24]; // 8 × (x, y, startTime)
half4 main(float2 fragCoord) {
    float2 off = float2(0.0);
    for (int i = 0; i < 8; i++) {
        if (float(i) >= rippleCount) break;
        float2 c = float2(ripples[i * 3], ripples[i * 3 + 1]);
        float age = iTime - ripples[i * 3 + 2];
        if (age < 0.0) continue;
        float2 d = fragCoord - c;
        float dist = length(d) + 0.001;
        float w = sin(dist * 0.09 - age * 7.0) * exp(-age * 2.2) * exp(-dist * 0.004) * 22.0;
        off += (d / dist) * w;
    }
    return content.eval(fragCoord + off);
}
"""

/** Touch-ripple water — tap/drag to send ripples that distort the live UI (content shader). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TouchRippleWaterScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = { TopAppBar(title = { Text("Touch Ripple Water") }, navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }) },
        modifier = Modifier.fillMaxSize()
    ) { inner ->
        Column(Modifier.padding(inner).fillMaxSize()) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) { Text("Needs Android 13 (API 33)+.", Modifier.padding(16.dp)); return@Column }
            RippleContent()
        }
    }
}

@Composable
private fun RippleContent() {
    val shader = remember { RuntimeShader(RIPPLE_AGSL) }
    val ripples = remember { FloatArray(24) }
    var head by remember { mutableIntStateOf(0) }
    var count by remember { mutableIntStateOf(0) }
    var time by remember { mutableFloatStateOf(0f) }
    var tick by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        var start = 0L
        while (true) { withFrameNanos { if (start == 0L) start = it; time = (it - start) / 1e9f; tick++ } }
    }

    fun addRipple(x: Float, y: Float) {
        ripples[head * 3] = x; ripples[head * 3 + 1] = y; ripples[head * 3 + 2] = time
        head = (head + 1) % 8; if (count < 8) count++
    }

    Box(
        Modifier.fillMaxSize()
            .graphicsLayer {
                tick // re-read each frame
                shader.setFloatUniform("iResolution", size.width, size.height)
                shader.setFloatUniform("iTime", time)
                shader.setFloatUniform("rippleCount", count.toFloat())
                shader.setFloatUniform("ripples", ripples)
                renderEffect = RenderEffect.createRuntimeShaderEffect(shader, "content").asComposeRenderEffect()
            }
            .pointerInput(Unit) { detectTapGestures { addRipple(it.x, it.y) } }
            .pointerInput(Unit) { detectDragGestures { c, _ -> addRipple(c.position.x, c.position.y) } }
    ) {
        LazyVerticalGrid(columns = GridCells.Fixed(3), modifier = Modifier.fillMaxSize().background(Color(0xFF0B0B16)).padding(8.dp)) {
            items((1..30).toList()) { i ->
                androidx.compose.foundation.layout.Box(
                    Modifier.padding(6.dp).aspectRatio(1f)
                        .background(Color.hsv((i * 25f) % 360f, 0.6f, 0.95f), androidx.compose.foundation.shape.RoundedCornerShape(14.dp)),
                    androidx.compose.ui.Alignment.Center
                ) { Text("$i", color = Color.White) }
            }
        }
    }
}
