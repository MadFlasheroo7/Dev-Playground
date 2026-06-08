package pro.jayeshseth.devcontrols.ui.shaders

import androidx.compose.runtime.Composable

/** Halftone-dot filter applied to live UI (dot size tracks luminance of the content). */
@Composable
fun HalftoneLabScreen(onBack: () -> Unit) =
    ContentShaderScreen("Halftone", HALFTONE_AGSL, animated = false, onBack = onBack)

private const val HALFTONE_AGSL = """
uniform shader content;
uniform float2 iResolution;
half4 main(float2 fc) {
    float cell = 9.0;
    float2 q = floor(fc / cell) * cell + cell * 0.5;
    half4 col = content.eval(q);
    float lum = dot(col.rgb, half3(0.299, 0.587, 0.114));
    float2 p = (fc - q) / cell;
    float rad = (1.0 - lum) * 0.78;
    float m = step(length(p), rad);
    half3 outc = col.rgb * m + (1.0 - m) * half3(0.03, 0.03, 0.05);
    return half4(outc, 1.0);
}
"""
