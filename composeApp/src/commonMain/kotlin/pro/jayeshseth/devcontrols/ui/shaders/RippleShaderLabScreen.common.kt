package pro.jayeshseth.devcontrols.ui.shaders

import androidx.compose.runtime.Composable

@Composable
fun RippleShaderLabScreenMultiplatform(onBack: () -> Unit) =
    ContentShaderScreenMultiplatform("Ripple Shader", RIPPLE_AGSL, animated = true, onBack = onBack)

private const val RIPPLE_AGSL = """
uniform float2 iResolution;
uniform float iTime;
half4 main(float2 fc) {
    float2 c = fc - 0.5 * iResolution;
    float d = length(c);
    float falloff = smoothstep(iResolution.y * 0.55, 0.0, d);
    float w = sin(d * 0.06 - iTime * 3.0) * 7.0 * falloff;
    float2 dir = d > 0.0 ? c / d : float2(0.0, 0.0);
    float2 uv = (fc + dir * w) / iResolution;
    half3 col = 0.5 + 0.5 * cos(6.2831 * (uv.xyx + half3(0.0, 2.0, 4.0)));
    return half4(col, 1.0);
}
"""
