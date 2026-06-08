package pro.jayeshseth.devcontrols.ui.shaders

import androidx.compose.runtime.Composable

/** Radial ripple distortion applied to live UI via a content-sampling AGSL shader. */
@Composable
fun RippleShaderLabScreen(onBack: () -> Unit) =
    ContentShaderScreen("Ripple Shader", RIPPLE_AGSL, animated = true, onBack = onBack)

private const val RIPPLE_AGSL = """
uniform shader content;
uniform float2 iResolution;
uniform float iTime;
half4 main(float2 fc) {
    float2 c = fc - 0.5 * iResolution;
    float d = length(c);
    float falloff = smoothstep(iResolution.y * 0.55, 0.0, d);
    float w = sin(d * 0.06 - iTime * 3.0) * 7.0 * falloff;
    float2 dir = d > 0.0 ? c / d : float2(0.0, 0.0);
    return content.eval(fc + dir * w);
}
"""
