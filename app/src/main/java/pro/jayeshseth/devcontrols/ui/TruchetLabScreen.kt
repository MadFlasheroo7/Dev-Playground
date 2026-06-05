package pro.jayeshseth.devcontrols.ui

import androidx.compose.runtime.Composable
import pro.jayeshseth.devcontrols.ui.shaders.GenerativeShaderScreen

private const val TRUCHET_AGSL = """
uniform float2 iResolution;
uniform float iTime;

float hash(float2 p) { return fract(sin(dot(p, float2(127.1, 311.7))) * 43758.5453); }

half4 main(float2 fragCoord) {
    float2 uv = fragCoord / iResolution.y * 8.0;
    uv += iTime * 0.3;
    float2 id = floor(uv);
    float2 f = fract(uv) - 0.5;
    float flip = step(0.5, hash(id));
    if (flip > 0.5) f.x = -f.x;
    // two quarter-arcs
    float d1 = abs(length(f - float2(0.5, 0.5)) - 0.5);
    float d2 = abs(length(f - float2(-0.5, -0.5)) - 0.5);
    float d = min(d1, d2);
    float line = smoothstep(0.08, 0.05, d);
    half3 bg = half3(0.05, 0.07, 0.12);
    half3 fg = mix(half3(0.1, 0.9, 0.8), half3(0.9, 0.3, 0.6), half(hash(id + 3.0)));
    return half4(mix(bg, fg, half(line)), 1.0);
}
"""

/** Truchet tiles — randomly-flipped arc tiles forming maze-like curves (AGSL). */
@Composable
fun TruchetLabScreen(onBack: () -> Unit) = GenerativeShaderScreen("Truchet Tiles", TRUCHET_AGSL, onBack)
