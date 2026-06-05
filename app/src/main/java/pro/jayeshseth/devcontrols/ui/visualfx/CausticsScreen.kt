package pro.jayeshseth.devcontrols.ui.visualfx

import androidx.compose.runtime.Composable
import pro.jayeshseth.devcontrols.ui.shaders.GenerativeShaderScreen

private const val CAUSTICS_AGSL = """
uniform float2 iResolution;
uniform float iTime;
half4 main(float2 fragCoord) {
    float2 uv = fragCoord / iResolution * 4.0;
    float t = iTime * 0.5;
    float2 p = uv;
    float c = 0.0;
    for (int i = 0; i < 4; i++) {
        float fi = float(i);
        p += float2(sin(p.y * 1.5 + t + fi), cos(p.x * 1.5 - t)) * 0.3;
        c += 1.0 / length(fract(p) - 0.5);
    }
    c = pow(c * 0.08, 2.0);
    half3 col = mix(half3(0.0, 0.2, 0.45), half3(0.45, 0.95, 1.0), half(clamp(c, 0.0, 1.0)));
    return half4(col, 1.0);
}
"""

/** Caustics — rippling underwater light patterns (AGSL). */
@Composable
fun CausticsScreen(onBack: () -> Unit) = GenerativeShaderScreen("Caustics", CAUSTICS_AGSL, onBack)
