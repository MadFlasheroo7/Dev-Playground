package pro.jayeshseth.devcontrols.ui

import androidx.compose.runtime.Composable
import pro.jayeshseth.devcontrols.ui.shaders.GenerativeShaderScreen

private const val PLASMA_AGSL = """
uniform float2 iResolution;
uniform float iTime;

half4 main(float2 fragCoord) {
    float2 uv = fragCoord / iResolution;
    float2 p = uv * 8.0;
    float t = iTime;
    float v = sin(p.x + t);
    v += sin((p.y + t) * 0.5);
    v += sin((p.x + p.y + t) * 0.5);
    float2 c = p + float2(sin(t * 0.33), cos(t * 0.25)) * 3.0;
    v += sin(sqrt(c.x * c.x + c.y * c.y) + t);
    v *= 0.5;
    float r = sin(v * 3.14159) * 0.5 + 0.5;
    float g = sin(v * 3.14159 + 2.094) * 0.5 + 0.5;
    float b = sin(v * 3.14159 + 4.188) * 0.5 + 0.5;
    return half4(half3(r, g, b), 1.0);
}
"""

/** Classic demoscene plasma, AGSL. */
@Composable
fun PlasmaLabScreen(onBack: () -> Unit) = GenerativeShaderScreen("Plasma", PLASMA_AGSL, onBack)
