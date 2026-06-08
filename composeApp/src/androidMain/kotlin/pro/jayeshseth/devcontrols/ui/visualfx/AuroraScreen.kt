package pro.jayeshseth.devcontrols.ui.visualfx

import androidx.compose.runtime.Composable
import pro.jayeshseth.devcontrols.ui.shaders.GenerativeShaderScreen

private const val AURORA_AGSL = """
uniform float2 iResolution;
uniform float iTime;
float hash(float2 p) { return fract(sin(dot(p, float2(127.1, 311.7))) * 43758.5453); }
float noise(float2 p) {
    float2 i = floor(p); float2 f = fract(p); float2 u = f * f * (3.0 - 2.0 * f);
    return mix(mix(hash(i), hash(i + float2(1.0, 0.0)), u.x), mix(hash(i + float2(0.0, 1.0)), hash(i + float2(1.0, 1.0)), u.x), u.y);
}
float fbm(float2 p) { float v = 0.0; float a = 0.5; for (int i = 0; i < 5; i++) { v += a * noise(p); p = p * 2.0; a *= 0.5; } return v; }
half4 main(float2 fragCoord) {
    float2 uv = fragCoord / iResolution;
    float t = iTime * 0.12;
    float aurora = 0.0;
    for (int i = 0; i < 3; i++) {
        float fi = float(i);
        float y = 0.32 + 0.16 * fi + 0.08 * sin(uv.x * 3.0 + t + fi);
        float band = fbm(float2(uv.x * 3.0 + t, fi * 2.0));
        float d = abs(uv.y - (y + band * 0.15));
        aurora += smoothstep(0.13, 0.0, d) * 0.5;
    }
    half3 col = half3(0.02, 0.03, 0.08);
    col += half3(0.1, 0.9, 0.5) * half(aurora);
    col += half3(0.35, 0.2, 0.85) * half(aurora * aurora * 0.5);
    col += half(step(0.997, hash(floor(uv * float2(320.0, 560.0)))));
    return half4(col, 1.0);
}
"""

/** Aurora — drifting northern-lights ribbons over a starfield (AGSL). */
@Composable
fun AuroraScreen(onBack: () -> Unit) = GenerativeShaderScreen("Aurora", AURORA_AGSL, onBack)
