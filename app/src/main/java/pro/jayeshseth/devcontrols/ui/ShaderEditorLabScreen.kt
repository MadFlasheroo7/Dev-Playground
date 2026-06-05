package pro.jayeshseth.devcontrols.ui

import android.graphics.RuntimeShader
import android.os.Build
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Live AGSL shader editor (API 33+). Edit the source, Recompile, and the canvas
 * renders it via a [ShaderBrush]. Uniforms bound every frame: `iResolution`, `iTime`,
 * and `iMouse` (drag/tap on the canvas). Compile errors surface below the editor.
 */
private const val DEFAULT_SHADER = """uniform float2 iResolution;
uniform float iTime;
uniform float2 iMouse;
half4 main(float2 fc) {
    float2 uv = fc / iResolution;
    float2 m = iMouse / iResolution;
    float d = distance(uv, m);
    float r = 0.5 + 0.5 * sin(iTime + uv.x * 6.0);
    float g = 0.5 + 0.5 * sin(iTime * 1.3 + uv.y * 6.0 + d * 12.0);
    float b = 0.5 + 0.5 * cos(iTime + d * 8.0);
    return half4(r, g, b, 1.0);
}"""

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShaderEditorLabScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Shader Editor") },
                navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }
            )
        },
        modifier = Modifier.fillMaxSize()
    ) { inner ->
        Column(Modifier.padding(inner).fillMaxSize().padding(horizontal = 12.dp)) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                Text("AGSL RuntimeShader needs Android 13 (API 33)+.", Modifier.padding(16.dp))
                return@Column
            }

            var src by remember { mutableStateOf(DEFAULT_SHADER) }
            var shader by remember { mutableStateOf<RuntimeShader?>(null) }
            var brush by remember { mutableStateOf<ShaderBrush?>(null) }
            var error by remember { mutableStateOf("") }
            var time by remember { mutableFloatStateOf(0f) }
            var mouse by remember { mutableStateOf(Offset(0.5f, 0.5f)) }

            fun recompile() {
                runCatching { RuntimeShader(src) }
                    .onSuccess { shader = it; brush = ShaderBrush(it); error = "" }
                    .onFailure { error = it.message ?: it.javaClass.simpleName }
            }
            LaunchedEffect(Unit) { recompile() }
            LaunchedEffect(Unit) {
                var start = 0L
                while (true) {
                    withFrameNanos { now -> if (start == 0L) start = now; time = (now - start) / 1e9f }
                }
            }

            Box(
                Modifier
                    .fillMaxWidth()
                    .height(260.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .pointerInput(Unit) {
                        detectTapGestures { mouse = it }
                    }
                    .pointerInput(Unit) {
                        detectDragGestures { change, _ -> mouse = change.position; change.consume() }
                    }
                    .drawWithCache {
                        val s = shader
                        val b = brush
                        onDrawBehind {
                            if (s != null && b != null) {
                                s.setFloatUniform("iResolution", size.width, size.height)
                                s.setFloatUniform("iTime", time)
                                s.setFloatUniform("iMouse", mouse.x, mouse.y)
                                drawRect(b)
                            } else {
                                drawRect(Color(0xFF222222))
                            }
                        }
                    }
            )

            Spacer(Modifier.height(8.dp))
            Row {
                Button(onClick = { recompile() }) { Text("Recompile") }
                Spacer(Modifier.fillMaxWidth(0.04f))
                TextButton(onClick = { src = DEFAULT_SHADER; recompile() }) { Text("Reset") }
            }
            if (error.isNotEmpty()) {
                Text(
                    error,
                    color = MaterialTheme.colorScheme.error,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }

            OutlinedTextField(
                value = src,
                onValueChange = { src = it },
                label = { Text("AGSL source") },
                textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                modifier = Modifier.fillMaxWidth().weight(1f)
            )
        }
    }
}
