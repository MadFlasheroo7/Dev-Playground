package io.github.shaderengine.presets

import org.intellij.lang.annotations.Language

/**
 * Dynamic rainy window pane lens with procedural condensation and dripping droplets.
 */
object RainyWindowPreset {
    @Language("AGSL")
    const val SHADER = """
        uniform shader content;
        uniform float2 resolution;
        uniform float time;
        uniform float2 pointer;
        uniform float p1; // Rain intensity / droplet scale (0.5 to 2.0)
        uniform float p2; // Refraction distortion strength (0.02 to 0.15)
        uniform float p3; // Condensation blur/mist factor (0.0 to 1.0)

        float hash12(float2 p) {
            float3 p3 = fract(float3(p.xyx) * 0.1031);
            p3 += dot(p3, p3.yzx + 33.33);
            return fract((p3.x + p3.y) * p3.z);
        }

        float2 hash22(float2 p) {
            float3 p3 = fract(float3(p.xyx) * float3(0.1031, 0.1030, 0.0973));
            p3 += dot(p3, p3.yzx + 33.33);
            return fract((p3.xx + p3.yz) * p3.zy);
        }

        half4 main(float2 fragCoord) {
            float2 uv = fragCoord / resolution;
            float t = time * 0.8;
            float scale = 14.0 * max(p1, 0.5);

            // Grid for static raindrops
            float2 st = uv * float2(scale * (resolution.x / resolution.y), scale);
            float2 id = floor(st);
            float2 gv = fract(st) - 0.5;

            // Droplet pseudo-random offset
            float2 p = (hash22(id) - 0.5) * 0.6;
            float d = length(gv - p);
            float dropSize = 0.12 * hash12(id + 11.0);

            // Falling streak droplets
            float2 streakUV = uv * float2(scale * 0.5, 1.0);
            float streakId = floor(streakUV.x);
            float streakSpeed = 0.5 + hash12(float2(streakId, 17.0)) * 1.5;
            float streakY = fract(uv.y + t * streakSpeed * 0.2 + hash12(float2(streakId, 43.0)));
            float streakDist = abs(fract(streakUV.x) - 0.5);
            float streakMask = smoothstep(0.04, 0.0, streakDist) * smoothstep(0.3, 0.0, streakY) * 0.25;

            float2 normal = float2(0.0);
            float dropMask = smoothstep(dropSize, dropSize * 0.2, d);
            if (d < dropSize) {
                normal = normalize(gv - p) * dropMask;
            }
            normal += float2(0.0, streakMask * 2.0);

            // Refracted lookup
            float refrStrength = p2 * 120.0;
            float2 sampleCoord = clamp(fragCoord + normal * refrStrength, float2(0.0), resolution);
            half4 col = content.eval(sampleCoord);

            // Specular droplet glint
            float glint = smoothstep(0.05, 0.0, length(gv - p - float2(0.0, dropSize * 0.4))) * dropMask * 0.4;
            col.rgb += half3(glint);

            return col;
        }
    """
}
