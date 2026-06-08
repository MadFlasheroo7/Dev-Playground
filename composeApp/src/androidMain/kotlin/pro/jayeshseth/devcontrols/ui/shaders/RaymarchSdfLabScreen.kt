package pro.jayeshseth.devcontrols.ui.shaders

import androidx.compose.runtime.Composable

/** Raymarched rotating rounded-box SDF with diffuse + specular lighting, in AGSL. */
@Composable
fun RaymarchSdfLabScreen(onBack: () -> Unit) =
    GenerativeShaderScreen("Raymarch SDF", RAYMARCH_AGSL, onBack)

private const val RAYMARCH_AGSL = """
uniform float2 iResolution;
uniform float iTime;

float sdBox(float3 p, float3 b) {
    float3 q = abs(p) - b;
    return length(max(q, 0.0)) + min(max(q.x, max(q.y, q.z)), 0.0);
}
float3 rotY(float3 p, float a) {
    float c = cos(a); float s = sin(a);
    return float3(c * p.x - s * p.z, p.y, s * p.x + c * p.z);
}
float3 rotX(float3 p, float a) {
    float c = cos(a); float s = sin(a);
    return float3(p.x, c * p.y - s * p.z, s * p.y + c * p.z);
}
float map(float3 p) {
    float3 q = rotY(rotX(p, iTime * 0.5), iTime * 0.7);
    return sdBox(q, float3(0.7, 0.7, 0.7)) - 0.08;
}
float3 calcNormal(float3 p) {
    float e = 0.001;
    float2 h = float2(e, 0.0);
    return normalize(float3(
        map(p + h.xyy) - map(p - h.xyy),
        map(p + h.yxy) - map(p - h.yxy),
        map(p + h.yyx) - map(p - h.yyx)));
}
half4 main(float2 fc) {
    float2 uv = (fc - 0.5 * iResolution) / iResolution.y;
    float3 ro = float3(0.0, 0.0, 3.0);
    float3 rd = normalize(float3(uv, -1.5));
    float t = 0.0;
    float hit = 0.0;
    float3 p = ro;
    for (int i = 0; i < 80; i++) {
        p = ro + rd * t;
        float d = map(p);
        if (d < 0.001) { hit = 1.0; break; }
        t += d;
        if (t > 8.0) break;
    }
    half3 col = half3(0.04, 0.04, 0.07);
    if (hit > 0.5) {
        float3 n = calcNormal(p);
        float3 lig = normalize(float3(0.8, 0.8, 0.6));
        float dif = max(dot(n, lig), 0.0);
        float3 base = 0.5 + 0.5 * cos(float3(0.0, 2.0, 4.0) + iTime + p.y);
        col = half3(base * (0.2 + 0.8 * dif));
        float3 hv = normalize(lig - rd);
        float spec = pow(max(dot(n, hv), 0.0), 32.0);
        col = col + half3(spec);
    }
    return half4(col, 1.0);
}
"""
