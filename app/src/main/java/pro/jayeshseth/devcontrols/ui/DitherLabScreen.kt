package pro.jayeshseth.devcontrols.ui

import androidx.compose.runtime.Composable
import pro.jayeshseth.devcontrols.ui.shaders.ContentShaderScreen

private const val DITHER_AGSL = """
uniform shader content;
uniform float2 iResolution;

float bayer(float2 fc) {
    int x = int(mod(fc.x, 4.0));
    int y = int(mod(fc.y, 4.0));
    float v = 0.0;
    if (y == 0) { v = (x == 0) ? 0.0 : (x == 1) ? 8.0 : (x == 2) ? 2.0 : 10.0; }
    else if (y == 1) { v = (x == 0) ? 12.0 : (x == 1) ? 4.0 : (x == 2) ? 14.0 : 6.0; }
    else if (y == 2) { v = (x == 0) ? 3.0 : (x == 1) ? 11.0 : (x == 2) ? 1.0 : 9.0; }
    else { v = (x == 0) ? 15.0 : (x == 1) ? 7.0 : (x == 2) ? 13.0 : 5.0; }
    return (v + 0.5) / 16.0;
}
half4 main(float2 fragCoord) {
    // pixelate a touch so the dither pattern reads
    float2 pc = floor(fragCoord / 2.0) * 2.0;
    half4 p = content.eval(pc);
    float l = 0.299 * float(p.r) + 0.587 * float(p.g) + 0.114 * float(p.b);
    float t = bayer(fragCoord);
    float bit = step(t, l);
    return half4(half3(half(bit)), 1.0);
}
"""

/** Ordered (Bayer 4×4) dithering of the live UI to 1-bit. */
@Composable
fun DitherLabScreen(onBack: () -> Unit) = ContentShaderScreen("Bayer Dither", DITHER_AGSL, animated = false, onBack = onBack)
