package pro.jayeshseth.devcontrols.ui.shaders

import android.graphics.RuntimeShader
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp

/**
 * Shared scaffold for generative AGSL shader labs: top bar + back, API-33 gate, a time
 * uniform driven by [withFrameNanos], and a full-bleed canvas that binds `iResolution`
 * + `iTime` and paints the [RuntimeShader] via a [ShaderBrush].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GenerativeShaderScreen(title: String, agsl: String, onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
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
            val shader = remember { RuntimeShader(agsl) }
            val brush = remember { ShaderBrush(shader) }
            var time by remember { mutableFloatStateOf(0f) }
            LaunchedEffect(Unit) {
                var start = 0L
                while (true) {
                    withFrameNanos { if (start == 0L) start = it; time = (it - start) / 1e9f }
                }
            }
            Box(
                Modifier.fillMaxSize().drawWithCache {
                    shader.setFloatUniform("iResolution", size.width, size.height)
                    onDrawBehind {
                        shader.setFloatUniform("iTime", time)
                        drawRect(brush)
                    }
                }
            )
        }
    }
}

/**
 * Scaffold for shaders that *sample live UI*: the AGSL must declare
 * `uniform shader content;` and `content.eval(...)`. We bind it via
 * `RenderEffect.createRuntimeShaderEffect(shader, "content")` on a `graphicsLayer`
 * wrapping the demo content.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContentShaderScreen(title: String, agsl: String, animated: Boolean, onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }
            )
        },
        modifier = Modifier.fillMaxSize()
    ) { inner ->
        Column(Modifier.padding(inner).fillMaxSize()) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                Text("Needs Android 13 (API 33)+ for AGSL.", Modifier.padding(16.dp)); return@Column
            }
            val shader = remember { RuntimeShader(agsl) }
            var time by remember { mutableFloatStateOf(0f) }
            if (animated) LaunchedEffect(Unit) {
                var start = 0L
                while (true) { withFrameNanos { if (start == 0L) start = it; time = (it - start) / 1e9f } }
            }
            Box(
                Modifier.fillMaxSize().graphicsLayer {
                    shader.setFloatUniform("iResolution", size.width, size.height)
                    if (animated) shader.setFloatUniform("iTime", time)
                    renderEffect = android.graphics.RenderEffect
                        .createRuntimeShaderEffect(shader, "content")
                        .asComposeRenderEffect()
                }
            ) { ShaderDemoContent() }
        }
    }
}

@Composable
private fun ShaderDemoContent() {
    Column(Modifier.fillMaxSize().background(Color(0xFF101018)).padding(20.dp)) {
        repeat(7) { i ->
            val c = listOf(Color(0xFFFF5F6D), Color(0xFF00C9FF), Color(0xFF92FE9D), Color(0xFFFFC371), Color(0xFFE100FF))[i % 5]
            Text(
                "Live UI sampled by the shader $i",
                color = c,
                modifier = Modifier
                    .padding(vertical = 10.dp)
                    .fillMaxWidth()
                    .background(c.copy(alpha = 0.12f), androidx.compose.foundation.shape.RoundedCornerShape(10.dp))
                    .padding(14.dp)
            )
        }
    }
}
