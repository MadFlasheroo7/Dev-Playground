package io.github.shaderengine.presets

import org.intellij.lang.annotations.Language

/**
 * Spherical glass magnifying dome (loupe) lens with chromatic edge dispersion
 * and specular highlight reflection arc.
 */
object MagnifyingDomePreset {
    @Language("AGSL")
    const val SHADER = """
        uniform shader content;
        uniform float2 resolution;
        uniform float time;
        uniform float2 pointer;
        uniform float p1; // Magnification power (1.2 to 2.5)
        uniform float p2; // Dome radius ratio (0.15 to 0.45)
        uniform float p3; // Chromatic aberration strength (0.0 to 0.05)

        half4 main(float2 fragCoord) {
            float minDim = min(resolution.x, resolution.y);
            float radius = max(p2, 0.1) * minDim;
            float mag = max(p1, 1.0);
            float chroma = p3 * 0.5;

            float2 center = (pointer.x > 0.0 || pointer.y > 0.0) ? pointer : (resolution * 0.5);
            float2 delta = fragCoord - center;
            float dist = length(delta);

            if (dist < radius) {
                // Spherical dome normal & refraction
                float normDist = dist / radius;
                float z = sqrt(max(0.0, 1.0 - normDist * normDist));
                
                // Refractive coordinate pull toward center
                float distortion = (1.0 - z) / mag;
                float2 sampleOffset = delta * (1.0 / mag + distortion * 0.3);
                float2 sampleCoord = center + sampleOffset;

                // Chromatic aberration along radial direction
                float2 dir = normalize(delta + float2(0.0001));
                half r = content.eval(sampleCoord + dir * (chroma * radius)).r;
                half g = content.eval(sampleCoord).g;
                half b = content.eval(sampleCoord - dir * (chroma * radius)).b;
                half4 col = half4(r, g, b, 1.0);

                // Specular light reflection crescent on dome edge
                float3 lightDir = normalize(float3(-0.5, -0.7, 1.0));
                float3 normal = float3(delta / radius, z);
                float spec = pow(max(0.0, dot(normal, lightDir)), 16.0) * 0.45;

                // Rim glass darkening
                float rim = smoothstep(0.85, 1.0, normDist) * 0.25;

                return half4(clamp(col.rgb * (1.0 - rim) + spec, 0.0, 1.0), col.a);
            }

            return content.eval(fragCoord);
        }
    """
}
