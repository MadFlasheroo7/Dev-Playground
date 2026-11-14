package io.github.shaderengine.presets

import org.intellij.lang.annotations.Language

/**
 * Raymarched Quantum Core Generative Canvas Preset.
 *
 * Implements 3D signed distance field (SDF) sphere-tracing in fragment shaders
 * with interactive gyroscopic rotation, Blinn-Phong specular, and volumetric glow.
 */
object RaymarchedCorePreset {

    @Language("AGSL")
    const val SHADER = """
        uniform float2 resolution;
        uniform float time;
        uniform float2 pointer;
        uniform float pointerStrength;

        uniform float complexity; // Gyroid scale (0.0 .. 1.0)
        uniform float glow;       // Subsurface emission intensity (0.0 .. 1.0)
        uniform float speed;      // Rotation speed multiplier (0.0 .. 1.0)
        uniform float uSpecularBoost; // Specular overdrive (1.0 = standard, 2.5 = boost)

        const float PI = 3.14159265359;

        // 3D rotation helpers
        float3 rotateY(float3 p, float a) {
            float c = cos(a);
            float s = sin(a);
            return float3(c * p.x + s * p.z, p.y, -s * p.x + c * p.z);
        }

        float3 rotateX(float3 p, float a) {
            float c = cos(a);
            float s = sin(a);
            return float3(p.x, c * p.y - s * p.z, s * p.y + c * p.z);
        }

        // Gyroid lattice signed distance function
        float sdGyroid(float3 p, float scale, float thickness, float bias) {
            float3 sp = p * scale;
            return abs(dot(sin(sp), cos(sp.zxy)) - bias) / scale - thickness;
        }

        // Scene distance evaluation
        float map(float3 p) {
            float gScale = mix(2.4, 5.2, complexity);
            float gyroid = sdGyroid(p, gScale, 0.045, 0.28);
            float boundingSphere = length(p) - 1.35;
            return max(gyroid, boundingSphere);
        }

        // 6-tap central differences surface normal (eliminates all skew & conical pinching)
        float3 calcNormal(float3 p) {
            float e = 0.002;
            return normalize(float3(
                map(p + float3(e, 0.0, 0.0)) - map(p - float3(e, 0.0, 0.0)),
                map(p + float3(0.0, e, 0.0)) - map(p - float3(0.0, e, 0.0)),
                map(p + float3(0.0, 0.0, e)) - map(p - float3(0.0, 0.0, e))
            ));
        }

        // 4-step Ambient Occlusion inside gyroid cavities
        float calcAO(float3 p, float3 n) {
            float occ = 0.0;
            float sca = 1.0;
            for (int i = 1; i <= 4; i++) {
                float d = float(i) * 0.045;
                occ += (d - map(p + n * d)) * sca;
                sca *= 0.7;
            }
            return clamp(1.0 - occ * 2.2, 0.0, 1.0);
        }

        half4 main(float2 fragCoord) {
            float2 uv = (fragCoord - resolution * 0.5) / min(resolution.x, resolution.y);

            // Upright default orbit; follows pointer tilt when interacting
            float rotY = time * 0.55 * (speed * 1.8 + 0.3);
            float rotX = 0.25 + 0.08 * sin(time * 0.4);
            if (pointer.x > 0.0) {
                rotY += (pointer.x / resolution.x - 0.5) * 4.0;
                rotX += (pointer.y / resolution.y - 0.5) * 2.5;
            }

            float3 ro = float3(0.0, 0.0, -3.4); // Ray origin
            float3 rd = normalize(float3(uv, 1.4)); // Ray direction

            ro = rotateX(ro, rotX);
            ro = rotateY(ro, rotY);
            rd = rotateX(rd, rotX);
            rd = rotateY(rd, rotY);

            // Sphere-tracing march loop
            float t = 0.0;
            float accumGlow = 0.0;
            int hit = 0;

            for (int i = 0; i < 56; i++) {
                float3 p = ro + rd * t;
                float d = map(p);

                accumGlow += exp(-max(d, 0.0) * 8.0) * (0.012 + glow * 0.025);

                if (d < 0.001) {
                    hit = 1;
                    break;
                }
                if (t > 5.5) break;
                t += d * 0.65; // Conservative step to prevent wall penetration
            }

            half3 col = half3(0.02, 0.03, 0.06); // Deep space background

            if (hit == 1) {
                float3 p = ro + rd * t;
                float3 n = calcNormal(p);

                // Multi-light shading
                float3 keyLight = normalize(float3(0.8, 1.2, -1.0));
                float3 fillLight = normalize(float3(-1.0, -0.5, 0.5));
                float3 viewDir = -rd;

                float diffKey = max(dot(n, keyLight), 0.0);
                float diffFill = max(dot(n, fillLight), 0.0) * 0.35;

                float3 halfVec = normalize(keyLight + viewDir);
                float spec = pow(max(dot(n, halfVec), 0.0), 32.0);

                float ao = calcAO(p, n);
                float fresnel = pow(1.0 - max(dot(n, viewDir), 0.0), 3.0);

                // Iridescent quantum palette based on surface coordinates
                half3 coreColor = half3(
                    half(0.5 + 0.5 * sin(time * 0.8 + p.x * 2.2)),
                    half(0.5 + 0.5 * sin(time * 0.8 + p.y * 2.2 + 2.09)),
                    half(0.5 + 0.5 * sin(time * 0.8 + p.z * 2.2 + 4.18))
                );

                half lightTerm = half((diffKey + diffFill) * ao + 0.15);
                half specBoost = half(max(uSpecularBoost, 1.0));
                col = coreColor * lightTerm + half3(half(spec * 0.7 * ao * specBoost)) + half3(0.3, 0.6, 1.0) * half(fresnel * 0.5 * specBoost);
            }

            // Volumetric quantum glow halo
            half3 glowColor = half3(0.2, 0.55, 1.0);
            col += glowColor * half(accumGlow * (1.0 + pointerStrength * 1.5));

            // Vignette
            float2 q = fragCoord / resolution;
            col *= half(clamp(16.0 * q.x * q.y * (1.0 - q.x) * (1.0 - q.y), 0.0, 1.0));

            return half4(col, 1.0);
        }
    """
}
