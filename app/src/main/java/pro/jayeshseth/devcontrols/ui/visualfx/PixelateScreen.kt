package pro.jayeshseth.devcontrols.ui.visualfx

import androidx.compose.runtime.Composable
import pro.jayeshseth.devcontrols.ui.shaders.ContentShaderScreen

private const val PIXELATE_AGSL = """
uniform shader content;
uniform float2 iResolution;
uniform float iTime;
half4 main(float2 fragCoord) {
    float blocks = mix(6.0, 90.0, 0.5 + 0.5 * sin(iTime));
    float2 cell = iResolution / blocks;
    float2 p = floor(fragCoord / cell) * cell + cell * 0.5;
    return content.eval(p);
}
"""

/** Pixelate — the live UI dissolves into big mosaic blocks and reforms, pulsing. */
@Composable
fun PixelateScreen(onBack: () -> Unit) = ContentShaderScreen("Pixelate", PIXELATE_AGSL, animated = true, onBack = onBack)
