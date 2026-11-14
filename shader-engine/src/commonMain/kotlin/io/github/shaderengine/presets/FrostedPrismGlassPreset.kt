package io.github.shaderengine.presets

import org.intellij.lang.annotations.Language

/**
 * Frosted Prism Glass Post-Processing Preset.
 *
 * Simulates a floating optical prism slab with Snell refraction,
 * micro-surface roughness scattering, chromatic dispersion, and Schlick Fresnel bevels.
 */
object FrostedPrismGlassPreset {

    @Language("AGSL")
    const val SHADER = """
        uniform shader content;
        uniform float2 resolution;
        uniform float time;
        uniform float2 pointer;
        uniform float pointerStrength;
        
        uniform float refraction;   // Refraction index scale (0.0 .. 1.0)
        uniform float dispersion;   // Chromatic aberration split (0.0 .. 1.0)
        uniform float roughness;    // Surface scattering noise (0.0 .. 1.0)
        uniform float uSpecularBoost; // Specular overdrive (1.0 = standard, 2.5 = boost)

        const float PI = 3.14159265359;

        // Hash noise without sine (Dave Hoskins)
        float hash21(float2 p) {
            float3 p3 = fract(float3(p.xyx) * 0.1031);
            p3 += float3(dot(p3, p3.yzx + 33.33));
            return fract((p3.x + p3.y) * p3.z);
        }

        // Rounded box SDF
        float sdRoundBox(float2 p, float2 b, float r) {
            float2 q = abs(p) - b + r;
            return min(max(q.x, q.y), 0.0) + length(max(q, 0.0)) - r;
        }

        half4 main(float2 fragCoord) {
            // Glass slab tracks pointer with dynamic dimensions
            float2 slabCenter = pointer.x > 0.0 ? pointer : resolution * 0.5;
            float2 p = fragCoord - slabCenter;
            float2 slabSize = float2(min(resolution.x * 0.38, 420.0), min(resolution.y * 0.28, 280.0));
            float slabRadius = 32.0;

            float d = sdRoundBox(p, slabSize, slabRadius);

            // Spatial early-out: outside the prism glass boundaries
            if (d > 8.0) {
                return content.eval(fragCoord);
            }

            // True SDF gradient along beveled glass edges
            float eps = 1.0;
            float2 grad = normalize(float2(
                sdRoundBox(p + float2(eps, 0.0), slabSize, slabRadius) - sdRoundBox(p - float2(eps, 0.0), slabSize, slabRadius),
                sdRoundBox(p + float2(0.0, eps), slabSize, slabRadius) - sdRoundBox(p - float2(0.0, eps), slabSize, slabRadius)
            ));

            // Smooth convex chamfer profile: slope = 1 at outer rim, 0 on interior
            float bevelWidth = 26.0;
            float bevelDepth = clamp(-d / bevelWidth, 0.0, 1.0);
            float slope = cos(bevelDepth * 1.5707963); // 1.0 at d=0, 0.0 at d=-bevelWidth
            
            // Micro-roughness scatter jitter
            float n1 = hash21(fragCoord + float2(17.1, 71.3));
            float n2 = hash21(fragCoord + float2(93.7, 31.9));
            float2 jitter = (float2(n1, n2) - 0.5) * (roughness * 14.0);

            // Surface normal: steep at bevel, flat (0,0,1) at interior
            float2 normal2D = grad * (slope * 1.2);
            float3 normal = normalize(float3(normal2D.x, normal2D.y, 1.0 / (refraction * 1.5 + 0.6)));

            // Optical Snell refraction & chromatic dispersion
            float2 refractOffset = normal2D * (refraction * 36.0);
            float2 dispOffset = grad * (dispersion * 16.0 * slope);

            float2 coordR = clamp(fragCoord - refractOffset - dispOffset + jitter, float2(0.0), resolution);
            float2 coordG = clamp(fragCoord - refractOffset + jitter * 0.5, float2(0.0), resolution);
            float2 coordB = clamp(fragCoord - refractOffset + dispOffset - jitter, float2(0.0), resolution);

            half r = content.eval(coordR).r;
            half4 sampleG = content.eval(coordG);
            half b = content.eval(coordB).b;
            half alpha = sampleG.a;

            half3 refracted = half3(r, sampleG.g, b);

            // Focused Blinn-Phong specular glint on the curved rim
            half3 lightDir = normalize(half3(0.5, -0.8, 0.6));
            half3 viewDir = half3(0.0, 0.0, 1.0);
            half3 halfVec = normalize(lightDir + viewDir);

            half ndoth = max(dot(half3(normal), halfVec), half(0.0));
            half specBoost = half(max(uSpecularBoost, 1.0));
            half spec = pow(ndoth, half(32.0)) * half(0.7) * half(slope) * specBoost;

            // Grazing Fresnel
            half cosTheta = clamp(half(normal.z), half(0.0), half(1.0));
            half fresnel = pow(half(1.0) - cosTheta, half(3.0)) * half(0.35) * specBoost;

            // Glass tint & inner luminescence
            half3 glassTint = half3(0.96, 0.98, 1.0);
            half3 body = refracted * glassTint + half3(0.03, 0.06, 0.10) * half(1.0 - slope);

            // Edge highlight line
            half rim = half(smoothstep(2.0, 0.0, abs(d))) * half(0.35);

            half3 finalRGB = body + half3(spec + fresnel + rim);
            finalRGB = min(finalRGB, half3(alpha));

            // Smooth outer anti-aliasing transition
            half edgeAA = half(smoothstep(1.5, -0.5, d));
            half4 original = content.eval(fragCoord);
            return mix(original, half4(finalRGB, alpha), edgeAA);
        }
    """
}
