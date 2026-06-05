package pro.jayeshseth.devcontrols.ui

import androidx.compose.runtime.Composable
import pro.jayeshseth.devcontrols.ui.shaders.ContentShaderScreen

private const val BLOOM_AGSL = """
uniform shader content;
uniform float2 iResolution;

half3 brightPass(float2 c) {
    half3 col = content.eval(c).rgb;
    half l = max(col.r, max(col.g, col.b));
    return col * smoothstep(half(0.55), half(0.9), l);
}
half4 main(float2 fragCoord) {
    half3 base = content.eval(fragCoord).rgb;
    half3 glow = half3(0.0);
    for (int i = 0; i < 12; i++) {
        float a = float(i) / 12.0 * 6.2831853;
        float2 d = float2(cos(a), sin(a));
        glow += brightPass(fragCoord + d * 7.0);
        glow += brightPass(fragCoord + d * 14.0) * half(0.55);
    }
    glow = glow / 12.0;
    half3 col = base + glow * half(1.4);
    return half4(col, 1.0);
}
"""

/** Bloom / light-bleed — bright-pass + radial blur added over the live UI. */
@Composable
fun BloomLabScreen(onBack: () -> Unit) = ContentShaderScreen("Bloom", BLOOM_AGSL, animated = false, onBack = onBack)
