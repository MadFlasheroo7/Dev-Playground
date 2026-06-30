package pro.jayeshseth.devcontrols.ui.shaders

import androidx.compose.runtime.Composable

@Composable
fun DissolveLabScreenMultiplatform(onBack: () -> Unit) =
    GenerativeShaderScreenMultiplatform("Dissolve", DISSOLVE_AGSL, onBack)

private const val DISSOLVE_AGSL = """
uniform float2 iResolution;
uniform float iTime;

float hash(float2 p) { return fract(sin(dot(p, float2(127.1, 311.7))) * 43758.5453); }
float noise(float2 p) {
    float2 i = floor(p);
    float2 f = fract(p);
    float a = hash(i);
    float b = hash(i + float2(1.0, 0.0));
    float c = hash(i + float2(0.0, 1.0));
    float d = hash(i + float2(1.0, 1.0));
    float2 u = f * f * (3.0 - 2.0 * f);
    return mix(mix(a, b, u.x), mix(c, d, u.x), u.y);
}
half4 main(float2 fc) {
    float2 uv = fc / iResolution;
    float n = noise(uv * 9.0);
    float thr = 0.5 + 0.5 * sin(iTime);
    half3 a = half3(0.0, 0.78, 1.0);
    half3 b = half3(1.0, 0.37, 0.43);
    half3 col = n < thr ? a : b;
    float edge = smoothstep(0.0, 0.05, abs(n - thr));
    col = mix(half3(1.0, 1.0, 1.0), col, edge);
    return half4(col, 1.0);
}
"""
