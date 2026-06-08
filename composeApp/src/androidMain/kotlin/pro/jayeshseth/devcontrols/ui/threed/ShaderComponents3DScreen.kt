package pro.jayeshseth.devcontrols.ui.threed

import android.graphics.RuntimeShader
import android.os.Build
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

private const val ORB_AGSL = """
uniform float2 iResolution;
uniform float press;
half4 main(float2 fragCoord) {
    float2 uv = (fragCoord - 0.5 * iResolution) / (0.5 * min(iResolution.x, iResolution.y));
    float r = length(uv);
    if (r > 1.0) return half4(0.08, 0.09, 0.12, 1.0);
    float z = sqrt(max(0.0, 1.0 - r * r));
    float3 N = float3(uv, z);
    float3 L = normalize(float3(-0.45, -0.5, 0.75));
    float3 V = float3(0.0, 0.0, 1.0);
    float3 H = normalize(L + V);
    float diff = max(dot(N, L), 0.0);
    float spec = pow(max(dot(N, H), 0.0), 60.0);
    float3 base = float3(0.30, 0.45, 1.0) * (1.0 - press * 0.45);
    float3 col = base * (0.18 + 0.82 * diff) + spec * (1.0 - press * 0.5);
    col = col * mix(1.0, 0.55, smoothstep(0.82, 1.0, r));
    return half4(half3(col), 1.0);
}
"""

private const val EMBOSS_AGSL = """
uniform float2 iResolution;
uniform float2 light;
float rrect(float2 p, float2 b, float r) { float2 d = abs(p) - b + r; return length(max(d, 0.0)) - r; }
half4 main(float2 fragCoord) {
    float2 uv = fragCoord - 0.5 * iResolution;
    float2 b = 0.5 * iResolution - 28.0;
    float d = rrect(uv, b, 46.0);
    if (d > 0.0) return half4(0.10, 0.11, 0.14, 1.0);
    float e = 2.0;
    float dx = rrect(uv + float2(e, 0.0), b, 46.0) - rrect(uv - float2(e, 0.0), b, 46.0);
    float dy = rrect(uv + float2(0.0, e), b, 46.0) - rrect(uv - float2(0.0, e), b, 46.0);
    float edge = smoothstep(-42.0, 0.0, d);
    float3 N = normalize(float3(dx * edge * 8.0, dy * edge * 8.0, 4.0));
    float3 L = normalize(float3((light - 0.5 * iResolution) / iResolution.x, 0.6));
    float diff = max(dot(N, L), 0.0);
    float spec = pow(diff, 24.0);
    float3 col = float3(0.42, 0.45, 0.54) * (0.28 + 0.85 * diff) + spec * 0.6;
    return half4(half3(col), 1.0);
}
"""

private const val LIQUID_AGSL = """
uniform float2 iResolution;
uniform float iTime;
uniform float press;
half4 main(float2 fragCoord) {
    float2 uv = fragCoord / iResolution;
    float fillTop = 1.0 - press;
    float wave = sin(uv.x * 5.5 + iTime * 0.8) * 0.055 + sin(uv.x * 9.0 - iTime * 0.5) * 0.03;
    float surf = fillTop + wave;
    if (uv.y < surf) return half4(0.07, 0.08, 0.11, 1.0);
    float depth = clamp((uv.y - surf) / max(press, 0.001), 0.0, 1.0);
    float flow = sin(uv.x * 4.0 + iTime * 0.7 + uv.y * 3.5) * 0.5 + 0.5;
    half3 top = half3(0.45, 0.97, 1.0);
    half3 bot = half3(0.07, 0.18, 0.7);
    half3 col = mix(top, bot, half(depth)) + half(flow * 0.07);
    col += half(smoothstep(0.02, 0.0, uv.y - surf) * 0.7);
    return half4(col, 1.0);
}
"""

private const val DOME_LED_AGSL = """
uniform float2 iResolution;
uniform float glow;
half4 main(float2 fragCoord) {
    float2 uv = (fragCoord - 0.5 * iResolution) / (0.5 * min(iResolution.x, iResolution.y));
    float r = length(uv);
    if (r > 1.0) return half4(0.08, 0.09, 0.12, 1.0);
    float z = sqrt(max(0.0, 1.0 - r * r));
    float3 N = float3(uv, z);
    float3 L = normalize(float3(-0.4, -0.5, 0.75));
    float3 H = normalize(L + float3(0.0, 0.0, 1.0));
    float diff = max(dot(N, L), 0.0);
    float spec = pow(max(dot(N, H), 0.0), 50.0);
    half3 ledOff = half3(0.12, 0.16, 0.14);
    half3 ledOn = half3(0.2, 1.0, 0.45);
    half3 base = mix(ledOff, ledOn, half(glow));
    half3 col = base * half(0.25 + 0.75 * diff) + half(spec * (0.4 + 0.6 * glow));
    col += ledOn * half(glow * (1.0 - r) * 1.2);
    col = col * half(mix(1.0, 0.6, smoothstep(0.82, 1.0, r)));
    return half4(col, 1.0);
}
"""

/** 3D tactile components rendered with AGSL lighting — real normals/specular + depth feedback. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShaderComponents3DScreen(onBack: () -> Unit) {
    val tactile = rememberTactile()
    Scaffold(
        topBar = { TopAppBar(title = { Text("3D Components · Shader") }, navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }) },
        modifier = Modifier.fillMaxSize()
    ) { inner ->
        Column(Modifier.padding(inner).fillMaxSize().background(Color(0xFF14161E)).padding(16.dp)) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                Text("Needs Android 13 (API 33)+ for AGSL.", color = Color.White); return@Column
            }
            Text("Press the orb; drag across the panel to move the light and feel the bevel.", color = Color(0xFF9AA0B5), style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(24.dp))
            Text("Glossy orb button", color = Color(0xFFC8CEE0), style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(8.dp))
            OrbButton(tactile)
            Spacer(Modifier.height(32.dp))
            Text("Embossed panel", color = Color(0xFFC8CEE0), style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(8.dp))
            EmbossPanel(tactile)
            Spacer(Modifier.height(28.dp))
            Text("Liquid fill (hold)", color = Color(0xFFC8CEE0), style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(8.dp))
            LiquidFillShader(tactile)
            Spacer(Modifier.height(28.dp))
            Text("Domed LED button", color = Color(0xFFC8CEE0), style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(8.dp))
            DomedLedButton(tactile)
        }
    }
}

@Composable
private fun OrbButton(tactile: Tactile) {
    val shader = remember { RuntimeShader(ORB_AGSL) }
    val brush = remember { ShaderBrush(shader) }
    val press = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Box(
            Modifier.size(190.dp)
                .graphicsLayer { val s = 1f - press.value * 0.06f; scaleX = s; scaleY = s }
                .pointerInput(Unit) {
                    detectTapGestures(onPress = {
                        tactile.pressDown(1f); scope.launch { press.animateTo(1f, tween(70)) }
                        tryAwaitRelease()
                        tactile.release(); scope.launch { press.animateTo(0f, spring(Spring.DampingRatioMediumBouncy)) }
                    })
                }
                .drawWithCache {
                    shader.setFloatUniform("iResolution", size.width, size.height)
                    onDrawBehind { shader.setFloatUniform("press", press.value); drawRect(brush) }
                }
        )
    }
}

@Composable
private fun DomedLedButton(tactile: Tactile) {
    val shader = remember { RuntimeShader(DOME_LED_AGSL) }
    val brush = remember { ShaderBrush(shader) }
    val glow = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Box(
            Modifier.size(170.dp)
                .pointerInput(Unit) {
                    detectTapGestures(onPress = {
                        tactile.pressDown(1f); scope.launch { glow.animateTo(1f, tween(60)) }
                        tryAwaitRelease()
                        tactile.release(); scope.launch { glow.animateTo(0f, tween(550)) }
                    })
                }
                .drawWithCache {
                    shader.setFloatUniform("iResolution", size.width, size.height)
                    onDrawBehind { shader.setFloatUniform("glow", glow.value); drawRect(brush) }
                }
        )
    }
}

@Composable
private fun LiquidFillShader(tactile: Tactile) {
    val shader = remember { RuntimeShader(LIQUID_AGSL) }
    val brush = remember { ShaderBrush(shader) }
    val press = remember { Animatable(0f) }
    var time by remember { mutableFloatStateOf(0f) }
    val scope = rememberCoroutineScope()
    LaunchedEffect(Unit) { var s = 0L; while (true) { withFrameNanos { if (s == 0L) s = it; time = (it - s) / 1e9f } } }
    Box(
        Modifier.fillMaxWidth().height(130.dp).clip(RoundedCornerShape(22.dp))
            .pointerInput(Unit) {
                detectTapGestures(onPress = {
                    tactile.pressDown(1f); scope.launch { press.animateTo(1f, tween(480)) }
                    tryAwaitRelease()
                    tactile.release(); scope.launch { press.animateTo(0f, tween(620)) }
                })
            }
            .drawWithCache {
                shader.setFloatUniform("iResolution", size.width, size.height)
                onDrawBehind { shader.setFloatUniform("iTime", time); shader.setFloatUniform("press", press.value); drawRect(brush) }
            },
        contentAlignment = Alignment.Center
    ) { Text("HOLD", color = Color.White, style = MaterialTheme.typography.titleMedium) }
}

@Composable
private fun EmbossPanel(tactile: Tactile) {
    val shader = remember { RuntimeShader(EMBOSS_AGSL) }
    val brush = remember { ShaderBrush(shader) }
    var light by remember { mutableStateOf(Offset(0.5f, 0.3f)) }
    Box(
        Modifier.fillMaxWidth().height(180.dp)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { light = it; tactile.detent() },
                    onDrag = { ch, _ -> ch.consume(); light = ch.position }
                )
            }
            .drawWithCache {
                shader.setFloatUniform("iResolution", size.width, size.height)
                onDrawBehind {
                    val lx = if (light.x <= 1f) light.x * size.width else light.x
                    val ly = if (light.y <= 1f) light.y * size.height else light.y
                    shader.setFloatUniform("light", lx, ly); drawRect(brush)
                }
            }
    )
}
