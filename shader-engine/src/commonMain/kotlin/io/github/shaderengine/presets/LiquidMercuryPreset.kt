package io.github.shaderengine.presets

import org.intellij.lang.annotations.Language

/**
 * Liquid Mercury Generative Canvas Shader.
 *
 * Implements:
 * - Viscous liquid metal SDF raymarching with polynomial smooth minimum (smin).
 * - Full 3D camera rotation and liquid pooling responding to hardware tilt [uTilt].
 * - Metallic Schlick Fresnel reflection (F0 = 0.91) with studio HDRI matcap simulation.
 * - Dynamic surface capillary tension ripples driven by touch velocity and pointer strength.
 * - Overdrive specular boost [uSpecularBoost].
 */
object LiquidMercuryPreset {

    @Language("AGSL")
    const val SHADER = """
        uniform float2 resolution;
        uniform float time;
        uniform float2 pointer;
        uniform float2 velocity;
        uniform float pointerStrength;
        uniform float2 uTilt;

        uniform float viscosity;     // Liquid damping / cohesion (0.0 .. 1.0)
        uniform float chromeShininess;// Metal polish reflectivity (0.0 .. 1.0)
        uniform float rippleSpeed;    // Surface capillary oscillation (0.0 .. 1.0)
        uniform float uSpecularBoost; // Specular overdrive (1.0 = standard, 2.5 = boost)

        const float PI = 3.14159265359;

        // Inigo Quilez smooth-minimum
        float smin(float a, float b, float k) {
            float h = clamp(0.5 + 0.5 * (b - a) / max(k, 0.0001), 0.0, 1.0);
            return mix(b, a, h) - k * h * (1.0 - h);
        }

        // 3D rotation helpers
        float3 rotY(float3 p, float a) {
            float c = cos(a), s = sin(a);
            return float3(c * p.x + s * p.z, p.y, -s * p.x + c * p.z);
        }

        float3 rotX(float3 p, float a) {
            float c = cos(a), s = sin(a);
            return float3(p.x, c * p.y - s * p.z, s * p.y + c * p.z);
        }

        // Viscous Liquid Mercury SDF Scene
        float mapMercury(float3 p) {
            // Gravity tilt displacement: liquid pools and sloshes towards tilt direction
            float2 tiltOffset = uTilt * 0.65;
            float3 center = float3(tiltOffset.x, -tiltOffset.y, 0.0);

            // Pointer cursor displacement
            if (pointer.x > 0.0) {
                float2 ptrNorm = (pointer - resolution * 0.5) / min(resolution.x, resolution.y) * 2.2;
                center += float3(ptrNorm.x, -ptrNorm.y, 0.0) * 0.45;
            }

            // Central primary liquid mercury blob
            float baseRadius = 0.95 + 0.08 * sin(time * 2.5 * (rippleSpeed + 0.5));
            float dMain = length(p - center) - baseRadius;

            // Flowing satellite liquid mercury drops orbiting and merging
            float dDrops = 10.0;
            for (int i = 0; i < 4; i++) {
                float fi = float(i);
                float a = time * (1.2 + fi * 0.3) + fi * 1.57;
                float r = 1.25 + 0.2 * sin(time * 3.0 + fi * 2.0);
                float3 dropPos = center + float3(cos(a) * r, sin(a * 1.2) * r * 0.7, sin(a * 0.8) * 0.5);
                // Tilt gravity pulls drops downward
                dropPos += float3(uTilt.x * 0.5, -uTilt.y * 0.5, 0.0);
                float dropRadius = 0.28 + 0.06 * sin(time * 4.0 + fi);
                float dDrop = length(p - dropPos) - dropRadius;
                dDrops = smin(dDrops, dDrop, 0.35);
            }

            // Viscous metaball blending
            float blendFactor = mix(0.25, 0.65, viscosity);
            float d = smin(dMain, dDrops, blendFactor);

            // High-frequency surface capillary tension ripples
            float ripple = sin(p.x * 9.0 + time * 4.0) * sin(p.y * 9.0 - time * 3.0) * sin(p.z * 9.0) * 0.035 * (1.0 + pointerStrength);
            return d + ripple;
        }

        // 6-tap central differences surface normal
        float3 calcNormal(float3 p) {
            float e = 0.003;
            return normalize(float3(
                mapMercury(p + float3(e, 0.0, 0.0)) - mapMercury(p - float3(e, 0.0, 0.0)),
                mapMercury(p + float3(0.0, e, 0.0)) - mapMercury(p - float3(0.0, e, 0.0)),
                mapMercury(p + float3(0.0, 0.0, e)) - mapMercury(p - float3(0.0, 0.0, e))
            ));
        }

        // Studio HDRI reflection approximation (metallic environment map)
        half3 studioEnv(float3 r) {
            // Studio softbox 1 (warm key)
            float light1 = pow(max(dot(r, normalize(float3(0.8, 1.2, 0.6))), 0.0), 18.0);
            // Studio softbox 2 (cool rim)
            float light2 = pow(max(dot(r, normalize(float3(-1.0, -0.6, 0.8))), 0.0), 12.0);
            // Studio ceiling strip light
            float light3 = pow(max(dot(r, normalize(float3(0.0, 1.5, -0.2))), 0.0), 32.0);

            half3 sky = mix(half3(0.08, 0.10, 0.14), half3(0.35, 0.40, 0.48), half(r.y * 0.5 + 0.5));
            sky += half3(1.0, 0.95, 0.88) * half(light1 * 1.8);
            sky += half3(0.6, 0.85, 1.0) * half(light2 * 1.4);
            sky += half3(1.0, 1.0, 1.0) * half(light3 * 2.2);
            return sky;
        }

        half4 main(float2 fragCoord) {
            float2 uv = (fragCoord - resolution * 0.5) / min(resolution.x, resolution.y);

            float3 ro = float3(0.0, 0.0, -3.2);
            float3 rd = normalize(float3(uv, 1.4));

            // Gyro tilt camera rotation
            ro = rotX(ro, uTilt.y * 0.4);
            ro = rotY(ro, uTilt.x * 0.4);
            rd = rotX(rd, uTilt.y * 0.4);
            rd = rotY(rd, uTilt.x * 0.4);

            float t = 0.0;
            int hit = 0;
            for (int i = 0; i < 48; i++) {
                float3 p = ro + rd * t;
                float d = mapMercury(p);
                if (d < 0.001) {
                    hit = 1;
                    break;
                }
                if (t > 5.0) break;
                t += d * 0.72;
            }

            half3 col = half3(0.03, 0.04, 0.07); // Dark studio background

            if (hit == 1) {
                float3 p = ro + rd * t;
                float3 n = calcNormal(p);
                float3 viewDir = -rd;
                float3 refDir = reflect(rd, n);

                // Metallic Mercury Fresnel (Schlick with F0 = 0.91)
                float cosTheta = max(dot(n, viewDir), 0.0);
                float fresnel = 0.91 + (1.0 - 0.91) * pow(1.0 - cosTheta, 5.0);

                // Reflection from studio environment
                half3 envReflect = studioEnv(refDir);

                // Chrome base color with specular boost
                half3 chromeBase = half3(0.92, 0.94, 0.97);
                half specBoost = half(max(uSpecularBoost, 1.0));
                col = chromeBase * envReflect * half(fresnel * chromeShininess) * specBoost;

                // Anisotropic glint along flow vector
                float2 vel = velocity / max(length(velocity), 1.0);
                float glint = pow(max(dot(n.xy, vel), 0.0), 16.0) * pointerStrength;
                col += half3(1.0, 1.0, 1.0) * half(glint * 1.5) * specBoost;
            }

            // Subtle edge vignette
            float2 q = fragCoord / resolution;
            col *= half(clamp(16.0 * q.x * q.y * (1.0 - q.x) * (1.0 - q.y), 0.0, 1.0));

            return half4(col, 1.0);
        }
    """
}
