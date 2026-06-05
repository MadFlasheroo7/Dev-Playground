package pro.jayeshseth.devcontrols.ui

import androidx.compose.runtime.Composable
import pro.jayeshseth.devcontrols.ui.shaders.ContentShaderScreen

private const val SOBEL_AGSL = """
uniform shader content;
uniform float2 iResolution;

float lum(float2 c) {
    half4 p = content.eval(c);
    return 0.299 * float(p.r) + 0.587 * float(p.g) + 0.114 * float(p.b);
}
half4 main(float2 fragCoord) {
    float gx = lum(fragCoord + float2(1.0, -1.0)) + 2.0 * lum(fragCoord + float2(1.0, 0.0)) + lum(fragCoord + float2(1.0, 1.0))
             - lum(fragCoord + float2(-1.0, -1.0)) - 2.0 * lum(fragCoord + float2(-1.0, 0.0)) - lum(fragCoord + float2(-1.0, 1.0));
    float gy = lum(fragCoord + float2(-1.0, 1.0)) + 2.0 * lum(fragCoord + float2(0.0, 1.0)) + lum(fragCoord + float2(1.0, 1.0))
             - lum(fragCoord + float2(-1.0, -1.0)) - 2.0 * lum(fragCoord + float2(0.0, -1.0)) - lum(fragCoord + float2(1.0, -1.0));
    float e = clamp(sqrt(gx * gx + gy * gy) * 1.5, 0.0, 1.0);
    return half4(half3(half(e)), 1.0);
}
"""

/** Sobel edge detection over the live UI. */
@Composable
fun EdgeDetectLabScreen(onBack: () -> Unit) = ContentShaderScreen("Edge Detect", SOBEL_AGSL, animated = false, onBack = onBack)
