package pro.jayeshseth.devcontrols.ui

import androidx.compose.runtime.Composable
import pro.jayeshseth.devcontrols.ui.shaders.ContentShaderScreen

private const val GLITCH_AGSL = """
uniform shader content;
uniform float2 iResolution;
uniform float iTime;

half4 main(float2 fragCoord) {
    float band = floor(fragCoord.y / 16.0);
    float n = fract(sin(band * 91.7 + floor(iTime * 14.0) * 3.3) * 4391.3);
    float shift = (n - 0.5) * 60.0 * step(0.72, n);
    float2 off = float2(shift, 0.0);
    half r = content.eval(fragCoord + off + float2(5.0, 0.0)).r;
    half g = content.eval(fragCoord + off).g;
    half b = content.eval(fragCoord + off - float2(5.0, 0.0)).b;
    half3 col = half3(r, g, b);
    col *= half(0.82 + 0.18 * sin(fragCoord.y * 3.14159 * 0.7));
    return half4(col, 1.0);
}
"""

/** Glitch / VHS — RGB-split + scanline jitter over the live UI. */
@Composable
fun GlitchLabScreen(onBack: () -> Unit) = ContentShaderScreen("Glitch / VHS", GLITCH_AGSL, animated = true, onBack = onBack)
