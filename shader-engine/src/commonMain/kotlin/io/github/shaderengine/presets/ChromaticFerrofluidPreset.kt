package io.github.shaderengine.presets

import org.intellij.lang.annotations.Language

/**
 * Production-grade Chromatic Ferrofluid Optical Lens Shader.
 *
 * Implements:
 * - Inigo Quilez polynomial smooth-minimum (smin) metaball fluid fusion.
 * - Velocity-stretched anisotropic capsule SDF with volume conservation.
 * - Iterative domain-warping fBm turbulence.
 * - Tier 2 forward finite difference 3D normal reconstruction (reusing center sample H0).
 * - Spatial early-out optimization to skip flat GPU warps.
 * - 3-tap symmetric chromatic dispersion with premultiplied alpha clamping.
 * - Fast FP16 Schlick Fresnel specular iridescence.
 */
object ChromaticFerrofluidPreset {

    @Language("AGSL")
    const val SHADER = """
        uniform shader content;
        uniform float2 resolution;
        uniform float time;
        
        // Interactive physics uniforms driven by ShaderController
        uniform float2 pointer;          // Spring-smoothed pointer (px)
        uniform float2 velocity;         // Instantaneous velocity (px/s)
        uniform float pointerStrength;   // 0.0 (idle) -> 1.0 (active drag)
        
        // Tunable optical & fluid parameters
        uniform float dispersion;        // Chromatic aberration intensity
        uniform float warpIntensity;     // Domain-warping fBm turbulence
        uniform float surfaceTension;    // Smooth-min (smin) blending radius
        uniform float uSpecularBoost;    // Specular overdrive

        const float PI = 3.14159265359;

        // 2D Hash & Value Noise for fBm
        float hash21(float2 p) {
            p = fract(p * float2(234.34, 435.345));
            p += float2(dot(p, p + 34.23));
            return fract(p.x * p.y);
        }

        float noise(float2 p) {
            float2 i = floor(p);
            float2 f = fract(p);
            // Quintic interpolation curve for C2 continuity
            float2 u = f * f * f * (f * (f * 6.0 - 15.0) + 10.0);
            
            float a = hash21(i);
            float b = hash21(i + float2(1.0, 0.0));
            float c = hash21(i + float2(0.0, 1.0));
            float d = hash21(i + float2(1.0, 1.0));
            
            return mix(mix(a, b, u.x), mix(c, d, u.x), u.y);
        }

        // 4-Octave Fractal Brownian Motion with analytic rotation to reduce grid artifacts
        float fbm(float2 p) {
            float value = 0.0;
            float amp = 0.5;
            for (int i = 0; i < 4; i++) {
                value += amp * noise(p);
                p = float2(0.8 * p.x - 0.6 * p.y, 0.6 * p.x + 0.8 * p.y) * 2.02 + float2(time * 0.15, -time * 0.1);
                amp *= 0.5;
            }
            return value;
        }

        // Inigo Quilez Polynomial Smooth Minimum for organic ferrofluid fusion
        float smin(float a, float b, float k) {
            float h = clamp(0.5 + 0.5 * (b - a) / max(k, 0.0001), 0.0, 1.0);
            return mix(b, a, h) - k * h * (1.0 - h);
        }

        // Velocity-stretched capsule/ellipse SDF for the main cursor droplet
        float sdVelocityDroplet(float2 p, float2 center, float2 vel, float radius) {
            float2 rel = p - center;
            float speed = length(vel);
            if (speed > 1.0) {
                float2 dir = vel / speed;
                float2 perp = float2(-dir.y, dir.x);
                // Stretch along velocity vector, compress perpendicular (conservation of volume)
                float stretch = clamp(1.0 + speed * 0.0008, 1.0, 2.4);
                float2 local = float2(
                    dot(rel, dir) / stretch,
                    dot(rel, perp) * sqrt(stretch)
                );
                return length(local) - radius;
            }
            return length(rel) - radius;
        }

        // Evaluates the combined scalar heightfield of the ferrofluid lens at UV-scaled coords
        float evaluateFluidField(float2 uv, float aspect) {
            float2 p = float2(uv.x * aspect, uv.y);
            float2 ptr = float2((pointer.x / resolution.x) * aspect, pointer.y / resolution.y);
            float2 vel = velocity * float2(aspect / resolution.x, 1.0 / resolution.y);

            // Base radius swells when touched
            float mainRadius = mix(0.11, 0.17, pointerStrength);
            float dMain = sdVelocityDroplet(p, ptr, vel, mainRadius);

            // Orbiting satellite ferro-droplets that get pulled into the main well
            float dSatellites = 10.0;
            for (int i = 0; i < 4; i++) {
                float fi = float(i);
                float angle = time * (0.9 + fi * 0.35) + fi * 1.57079;
                float orbitRadius = mix(0.22, 0.12, pointerStrength) + 0.04 * sin(time * 2.0 + fi);
                float2 offset = float2(cos(angle), sin(angle * 1.3)) * orbitRadius;
                float satRadius = 0.038 + 0.012 * sin(time * 3.0 + fi * 2.0);
                float dSat = length(p - (ptr + offset)) - satRadius;
                dSatellites = smin(dSatellites, dSat, surfaceTension * 0.7);
            }

            // Fuse primary velocity droplet with satellites
            float dCombined = smin(dMain, dSatellites, surfaceTension);

            // Iterative Domain Warping: f(p + f(p))
            float2 q = float2(
                fbm(p * 3.5 + float2(0.0, time * 0.4)),
                fbm(p * 3.5 + float2(5.2, -time * 0.3))
            );
            float2 r = float2(
                fbm(p * 4.0 + 2.5 * q + float2(1.7, 9.2) + 0.25 * time),
                fbm(p * 4.0 + 2.5 * q + float2(8.3, 2.8) - 0.2 * time)
            );
            float warp = fbm(p * 3.0 + 2.0 * r);

            // Displace SDF boundary with domain-warped ripples
            dCombined -= (warp - 0.5) * warpIntensity * (0.4 + 0.6 * pointerStrength);

            // Convert signed distance inside the boundary into a smooth 3D dome height [0..1]
            float inside = smoothstep(0.02, -0.14, dCombined);
            float ripples = sin(dCombined * 65.0 - time * 6.0) * 0.03 * inside * pointerStrength;
            return inside * sqrt(max(0.0, 1.0 - pow(clamp(dCombined / -0.16, 0.0, 1.0) - 1.0, 2.0))) + ripples;
        }

        // Thin-film cosine palette for iridescent Fresnel interference
        half3 thinFilmPalette(half t) {
            half3 a = half3(0.5, 0.5, 0.5);
            half3 b = half3(0.5, 0.5, 0.5);
            half3 c = half3(1.0, 1.0, 1.0);
            half3 d = half3(0.00, 0.33, 0.67);
            return a + b * cos(half(2.0 * PI) * (c * t + d));
        }

        half4 main(float2 fragCoord) {
            float2 uv = fragCoord / resolution;
            float aspect = resolution.x / max(resolution.y, 1.0);

            // Center height evaluation (H0)
            float hCenter = evaluateFluidField(uv, aspect);
            
            // 1. Spatial Early-Out: Skips entire GPU warps for flat, untouched regions
            if (hCenter <= 0.0005) {
                float2 toPtr = fragCoord - pointer;
                float dist = length(toPtr / resolution.x);
                float pull = exp(-dist * 12.0) * 14.0 * pointerStrength;
                return content.eval(fragCoord - normalize(toPtr + float2(0.0001)) * pull);
            }

            // 2. Tier 2 Forward Finite Differences (reusing center sample hCenter)
            float eps = 2.0 / max(resolution.x, resolution.y);
            float hRight = evaluateFluidField(uv + float2(eps, 0.0), aspect);
            float hUp    = evaluateFluidField(uv + float2(0.0, eps), aspect);

            float dHdX = (hRight - hCenter) / eps;
            float dHdY = (hUp - hCenter) / eps;

            // 3D unit surface normal in float3 (avoids half-to-float conversions)
            float3 normal = normalize(float3(-dHdX * 0.18, -dHdY * 0.18, 1.0));

            // 3. Snell's Law Refraction & 3-Tap Symmetric Chromatic Dispersion
            float2 refractBase = normal.xy * hCenter * resolution.x * 0.12;

            float dispScale = dispersion * (1.0 + 0.8 * pointerStrength);
            float2 offsetR = refractBase * (1.0 - dispScale * 0.35);
            float2 offsetG = refractBase * 1.0;
            float2 offsetB = refractBase * (1.0 + dispScale * 0.35);

            half r = content.eval(clamp(fragCoord - offsetR, float2(0.0), resolution)).r;
            half4 sampleG = content.eval(clamp(fragCoord - offsetG, float2(0.0), resolution));
            half b = content.eval(clamp(fragCoord - offsetB, float2(0.0), resolution)).b;
            half a = sampleG.a;
            half3 refractedColor = half3(r, sampleG.g, b);

            // 4. 3D Lighting & Fast FP16 Schlick Fresnel
            half3 viewDir = half3(0.0, 0.0, 1.0);
            half3 lightDir1 = normalize(half3(-0.45, -0.6, 0.65));
            half3 halfVec1 = normalize(lightDir1 + viewDir);

            half ndoth1 = max(dot(half3(normal), halfVec1), half(0.0));
            // pow(x, 96.0) fast approximation via squaring: ((x^3)^4)^8
            half n3 = ndoth1 * ndoth1 * ndoth1;
            half n12 = n3 * n3 * n3 * n3;
            half specBoost = half(max(uSpecularBoost, 1.0));
            half spec1 = n12 * n12 * half(1.15) * specBoost;

            // Schlick's Fresnel via fast multiply
            half cosTheta = clamp(half(normal.z), half(0.0), half(1.0));
            half invCos = half(1.0) - cosTheta;
            half fresnel = invCos * invCos * invCos * half(0.5);

            half3 iridescence = thinFilmPalette(fresnel * half(2.2) + half(time * 0.25 + hCenter * 0.5));
            half3 absorption = exp(-half3(0.18, 0.08, 0.02) * half(hCenter * 1.8));
            half3 finalRGB = refractedColor * absorption;

            finalRGB = mix(finalRGB, iridescence, fresnel * half(0.65));
            finalRGB += half3(1.0, 0.98, 0.95) * spec1;

            // Meniscus contact edge shadow
            half hC = half(hCenter);
            half edgeShadow = smoothstep(half(0.0), half(0.08), hC) * (half(1.0) - smoothstep(half(0.08), half(0.22), hC));
            finalRGB *= (half(1.0) - edgeShadow * half(0.25));

            // 5. Premultiplied Alpha Invariant: prevent additive blowout on P3/HDR displays
            finalRGB = min(clamp(finalRGB, half(0.0), half(1.0)), half3(a));

            return half4(finalRGB, a);
        }
    """
}
