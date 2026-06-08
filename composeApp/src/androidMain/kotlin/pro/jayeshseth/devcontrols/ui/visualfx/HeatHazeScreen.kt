package pro.jayeshseth.devcontrols.ui.visualfx

import androidx.compose.runtime.Composable
import pro.jayeshseth.devcontrols.ui.shaders.ContentShaderScreen

private const val HAZE_AGSL = """
uniform shader content;
uniform float2 iResolution;
uniform float iTime;
half4 main(float2 fragCoord) {
    float2 uv = fragCoord / iResolution;
    float fade = 1.0 - uv.y;
    float off = sin(uv.y * 40.0 + iTime * 5.0) * fade * 6.0
              + sin(uv.y * 25.0 - iTime * 3.0) * fade * 4.0;
    return content.eval(fragCoord + float2(off, 0.0));
}
"""

/** Heat haze — rising mirage shimmer that distorts the live UI from the bottom up. */
@Composable
fun HeatHazeScreen(onBack: () -> Unit) = ContentShaderScreen("Heat Haze", HAZE_AGSL, animated = true, onBack = onBack)
