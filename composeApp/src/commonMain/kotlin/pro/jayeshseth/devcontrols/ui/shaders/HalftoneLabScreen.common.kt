package pro.jayeshseth.devcontrols.ui.shaders

import androidx.compose.runtime.Composable

@Composable
fun HalftoneLabScreenMultiplatform(onBack: () -> Unit) =
    ContentShaderScreenMultiplatform("Halftone", HALFTONE_AGSL, animated = true, onBack = onBack)

private const val HALFTONE_AGSL = """
uniform float2 iResolution;
uniform float iTime;
half4 main(float2 fc) {
    float cell = 9.0;
    float2 q = floor(fc / cell) * cell + cell * 0.5;
    float2 uv = q / iResolution;
    float lum = 0.5 + 0.5 * sin(uv.x * 6.28 + iTime) * cos(uv.y * 6.28 + iTime);
    float2 p = (fc - q) / cell;
    float rad = (1.0 - lum) * 0.78;
    float m = step(length(p), rad);
    half3 col = half3(uv.x, uv.y, 0.5 + 0.5 * sin(iTime));
    half3 outc = col * m + (1.0 - m) * half3(0.03, 0.03, 0.05);
    return half4(outc, 1.0);
}
"""
