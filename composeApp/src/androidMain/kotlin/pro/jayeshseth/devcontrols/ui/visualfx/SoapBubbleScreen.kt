package pro.jayeshseth.devcontrols.ui.visualfx

import androidx.compose.runtime.Composable
import pro.jayeshseth.devcontrols.ui.shaders.GenerativeShaderScreen

private const val BUBBLE_AGSL = """
uniform float2 iResolution;
uniform float iTime;
half4 main(float2 fragCoord) {
    float2 uv = (fragCoord - 0.5 * iResolution) / iResolution.y;
    float r = length(uv);
    float t = iTime * 0.4;
    float thick = 6.0 + 4.0 * sin(uv.x * 10.0 + t) + 4.0 * sin(uv.y * 8.0 - t) + r * 8.0;
    half3 col = half3(0.5) + half3(0.5) * half3(cos(thick + float3(0.0, 2.09, 4.18)));
    float bubble = smoothstep(0.82, 0.80, r);
    float rim = smoothstep(0.82, 0.81, r) - smoothstep(0.81, 0.79, r);
    col = col * half(bubble) + half(rim) * half3(1.0);
    return half4(col, 1.0);
}
"""

/** Soap bubble — thin-film interference iridescence on a floating bubble (AGSL). */
@Composable
fun SoapBubbleScreen(onBack: () -> Unit) = GenerativeShaderScreen("Soap Bubble", BUBBLE_AGSL, onBack)
