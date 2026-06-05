package pro.jayeshseth.devcontrols.ui.visualfx

import androidx.compose.runtime.Composable
import pro.jayeshseth.devcontrols.ui.shaders.ContentShaderScreen

private const val CRT_AGSL = """
uniform shader content;
uniform float2 iResolution;
uniform float iTime;
half4 main(float2 fragCoord) {
    float2 uv = fragCoord / iResolution;
    float2 cc = uv - 0.5;
    float r2 = dot(cc, cc);
    uv = 0.5 + cc * (1.0 + 0.18 * r2);
    if (uv.x < 0.0 || uv.x > 1.0 || uv.y < 0.0 || uv.y > 1.0) return half4(0.0, 0.0, 0.0, 1.0);
    float2 sc = uv * iResolution;
    half r = content.eval(sc + float2(1.6, 0.0)).r;
    half g = content.eval(sc).g;
    half b = content.eval(sc - float2(1.6, 0.0)).b;
    half3 col = half3(r, g, b);
    col = col * half(0.82 + 0.18 * sin(uv.y * iResolution.y + iTime * 6.0));
    col = col * half(smoothstep(0.8, 0.15, r2));
    return half4(col, 1.0);
}
"""

/** CRT — barrel distortion, scanlines, chromatic fringe + vignette over the live UI. */
@Composable
fun CrtScreen(onBack: () -> Unit) = ContentShaderScreen("CRT TV", CRT_AGSL, animated = true, onBack = onBack)
