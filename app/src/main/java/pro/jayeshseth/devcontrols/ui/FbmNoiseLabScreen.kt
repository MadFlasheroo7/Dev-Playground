package pro.jayeshseth.devcontrols.ui

import androidx.compose.runtime.Composable
import pro.jayeshseth.devcontrols.ui.shaders.GenerativeShaderScreen

private const val FBM_AGSL = """
uniform float2 iResolution;
uniform float iTime;

float hash(float2 p) { return fract(sin(dot(p, float2(127.1, 311.7))) * 43758.5453); }
float vnoise(float2 p) {
    float2 i = floor(p); float2 f = fract(p);
    float2 u = f * f * (3.0 - 2.0 * f);
    float a = hash(i), b = hash(i + float2(1.0, 0.0));
    float c = hash(i + float2(0.0, 1.0)), d = hash(i + float2(1.0, 1.0));
    return mix(mix(a, b, u.x), mix(c, d, u.x), u.y);
}
float fbm(float2 p) {
    float v = 0.0; float amp = 0.5;
    for (int i = 0; i < 6; i++) { v += amp * vnoise(p); p = p * 2.0 + 1.3; amp *= 0.5; }
    return v;
}
half4 main(float2 fragCoord) {
    float2 uv = fragCoord / iResolution.y;
    float t = iTime * 0.15;
    // domain warp
    float2 q = float2(fbm(uv + t), fbm(uv + float2(5.2, 1.3) - t));
    float2 r = float2(fbm(uv + 4.0 * q + float2(1.7, 9.2)), fbm(uv + 4.0 * q + float2(8.3, 2.8)));
    float n = fbm(uv + 4.0 * r);
    half3 col = mix(half3(0.1, 0.15, 0.4), half3(0.95, 0.7, 0.3), half(n));
    col = mix(col, half3(0.9, 0.2, 0.4), half(r.x * 0.6));
    return half4(col, 1.0);
}
"""

/** fBm domain-warp noise field (AGSL). */
@Composable
fun FbmNoiseLabScreen(onBack: () -> Unit) = GenerativeShaderScreen("fBm Noise", FBM_AGSL, onBack)
