package pro.jayeshseth.devcontrols.ui

import androidx.compose.runtime.Composable
import pro.jayeshseth.devcontrols.ui.shaders.GenerativeShaderScreen

private const val METABALLS_AGSL = """
uniform float2 iResolution;
uniform float iTime;

half4 main(float2 fragCoord) {
    float2 uv = (fragCoord - 0.5 * iResolution) / iResolution.y;
    float t = iTime;
    float v = 0.0;
    for (int i = 0; i < 6; i++) {
        float fi = float(i);
        float2 c = 0.6 * float2(sin(t * (0.3 + fi * 0.13) + fi), cos(t * (0.4 + fi * 0.11) + fi * 1.7));
        float r = 0.05 + 0.03 * sin(t + fi);
        v += r / dot(uv - c, uv - c);
    }
    float iso = smoothstep(2.0, 3.5, v);
    half3 inside = mix(half3(0.0, 0.8, 1.0), half3(0.8, 0.0, 0.9), half(sin(v * 0.3) * 0.5 + 0.5));
    half3 col = mix(half3(0.02, 0.02, 0.06), inside, half(iso));
    // rim
    col += half(smoothstep(2.6, 2.9, v) - smoothstep(2.9, 3.2, v)) * half3(1.0);
    return half4(col, 1.0);
}
"""

/** Metaballs — gooey iso-surface of moving charges (AGSL). */
@Composable
fun MetaballsLabScreen(onBack: () -> Unit) = GenerativeShaderScreen("Metaballs", METABALLS_AGSL, onBack)
