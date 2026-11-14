package io.github.shaderengine.presets

import org.intellij.lang.annotations.Language

/**
 * Cosmic Nebula Generative Canvas Preset.
 *
 * Implements 4-octave iterative domain-warping fluid mathematics:
 * f(p + f(p + f(p))) with interactive cursor vortex attraction.
 */
object CosmicNebulaPreset {

    @Language("AGSL")
    const val SHADER = """
        uniform float2 resolution;
        uniform float time;
        uniform float2 pointer;
        uniform float pointerStrength;

        uniform float turbulence; // Domain warping multiplier (0.0 .. 1.0)
        uniform float vortex;     // Pointer vortex pull (0.0 .. 1.0)
        uniform float speed;      // Animation speed (0.0 .. 1.0)
        uniform float uSpecularBoost; // Core emission overdrive (1.0 = standard, 2.5 = boost)

        const float PI = 3.14159265359;

        // Dave Hoskins Hash Without Sine: uniform entropy across all quadrants
        float hash(float2 p) {
            float3 p3 = fract(float3(p.xyx) * 0.1031);
            p3 += float3(dot(p3, p3.yzx + 33.33));
            return fract((p3.x + p3.y) * p3.z);
        }

        // 2D Value Noise with cubic Hermite interpolation
        float noise(float2 p) {
            float2 i = floor(p);
            float2 f = fract(p);
            float2 u = f * f * (3.0 - 2.0 * f);
            return mix(
                mix(hash(i + float2(0.0, 0.0)), hash(i + float2(1.0, 0.0)), u.x),
                mix(hash(i + float2(0.0, 1.0)), hash(i + float2(1.0, 1.0)), u.x),
                u.y
            );
        }

        // 5-Octave Fractional Brownian Motion (fBm) with analytic coordinate rotation
        float fbm(float2 p) {
            float v = 0.0;
            float a = 0.5;
            for (int i = 0; i < 5; i++) {
                v += a * noise(p);
                // 37-degree rotation & 2.02x frequency to break grid lattice
                p = float2(0.8 * p.x - 0.6 * p.y, 0.6 * p.x + 0.8 * p.y) * 2.02;
                a *= 0.5;
            }
            return v;
        }

        half4 main(float2 fragCoord) {
            float2 uv = (fragCoord - resolution * 0.5) / min(resolution.x, resolution.y);

            // Gravitational vortex pull around pointer (only active when touching or hovered)
            if (pointer.x > 0.0) {
                float2 ptrUV = (pointer - resolution * 0.5) / min(resolution.x, resolution.y);
                float2 toPtr = ptrUV - uv;
                float dPtr = length(toPtr);
                float pull = exp(-dPtr * 2.5) * (vortex * 1.5) * (0.4 + 0.6 * pointerStrength);
                float angle = atan(toPtr.y, toPtr.x) + pull * 3.0;
                uv += float2(cos(angle), sin(angle)) * pull * 0.25;
            }

            float t = time * 0.35 * (speed * 1.8 + 0.4);

            // Iterative domain warping: f(p + f(p + f(p)))
            float2 q = float2(
                fbm(uv * 1.8 + float2(0.0, t * 0.8)),
                fbm(uv * 1.8 + float2(5.2, -t * 0.6))
            );

            float warpScale = mix(1.2, 3.8, turbulence);
            float2 r = float2(
                fbm(uv * 1.6 + warpScale * q + float2(1.7, 9.2) + 0.15 * t),
                fbm(uv * 1.6 + warpScale * q + float2(8.3, 2.8) - 0.12 * t)
            );

            float f = fbm(uv * 1.4 + warpScale * r);

            // Normalize and boost dynamic contrast of nebular cloud bands
            float cloud1 = smoothstep(0.15, 0.85, f);
            float cloud2 = smoothstep(0.25, 0.90, length(q));
            float cloud3 = smoothstep(0.20, 0.80, r.x);

            // Rich multi-spectrum interstellar palette
            half3 cSpace   = half3(0.03, 0.02, 0.08); // Obsidian void
            half3 cViolet  = half3(0.48, 0.10, 0.65); // Cosmic violet dust
            half3 cTeal    = half3(0.05, 0.72, 0.68); // Ionized gas teal
            half3 cMagenta = half3(0.95, 0.18, 0.55); // Stellar magenta glow
            half3 cGold    = half3(1.00, 0.92, 0.75); // Core star nursery

            // Multi-pass compositional blend
            half3 col = mix(cSpace, cViolet, half(cloud1));
            col = mix(col, cTeal, half(cloud2 * 0.75));
            col = mix(col, cMagenta, half(cloud3 * 0.65));

            // Hot glowing filaments & core energy emission
            half specBoost = half(max(uSpecularBoost, 1.0));
            float coreIntensity = pow(f * cloud2, 2.5) * 2.2 * float(specBoost);
            col += cGold * half(coreIntensity);

            // Deep background star cluster
            float starHash = hash(floor(fragCoord * 0.5));
            if (starHash > 0.993) {
                float twinkle = sin(time * 3.0 + starHash * 100.0) * 0.5 + 0.5;
                col += half3(0.9, 0.95, 1.0) * half(twinkle * 0.85);
            }

            // Subtle vignette to focus cosmic energy
            float2 qVig = fragCoord / resolution;
            col *= half(clamp(16.0 * qVig.x * qVig.y * (1.0 - qVig.x) * (1.0 - qVig.y), 0.0, 1.0));

            return half4(col, 1.0);
        }
    """
}
