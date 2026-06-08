package pro.jayeshseth.devcontrols.ui.shaders

import androidx.compose.runtime.Composable

/** Animated Voronoi / cellular-noise field. */
@Composable
fun VoronoiLabScreen(onBack: () -> Unit) =
    GenerativeShaderScreen("Voronoi", VORONOI_AGSL, onBack)

private const val VORONOI_AGSL = """
uniform float2 iResolution;
uniform float iTime;

float2 hash2(float2 p) {
    p = float2(dot(p, float2(127.1, 311.7)), dot(p, float2(269.5, 183.3)));
    return fract(sin(p) * 43758.5453);
}
half4 main(float2 fc) {
    float2 uv = fc / iResolution.y * 6.0;
    float2 g = floor(uv);
    float2 f = fract(uv);
    float md = 8.0;
    for (int j = -1; j <= 1; j++) {
        for (int i = -1; i <= 1; i++) {
            float2 o = float2(float(i), float(j));
            float2 pt = o + 0.5 + 0.5 * sin(iTime + 6.2831 * hash2(g + o)) - f;
            float d = dot(pt, pt);
            md = min(md, d);
        }
    }
    half3 col = 0.5 + 0.5 * cos(6.2831 * (sqrt(md) + half3(0.0, 0.33, 0.67)));
    return half4(col, 1.0);
}
"""
