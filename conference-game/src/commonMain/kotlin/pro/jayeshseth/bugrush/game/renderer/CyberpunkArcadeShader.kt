package pro.jayeshseth.bugrush.game.renderer

import org.intellij.lang.annotations.Language

/**
 * Cyberpunk Arcade & Holographic Post-Processing Shader.
 *
 * Implements:
 * - Subtle CRT barrel curvature (0.04) and fine horizontal scanlines (0.12)
 * - Kinetic shockwave ripple rings with radial chromatic dispersion on dash, elite kills, and supercharges
 * - Dynamic edge chromatic aberration scaling during combat
 * - Horizontal sync-roll glitch jitter and crimson alert flare during Titan Kernel Panic or player damage
 * - Specular boost (1.4x) for vivid neon lasers, bullets, and circuit traces
 */
object CyberpunkArcadeShader {

    const val DEFAULT_BARREL_WARP = 0.04f
    const val DEFAULT_SCANLINE_DARK = 0.12f
    const val DEFAULT_SPECULAR_BOOST = 1.4f

    @Language("AGSL")
    const val SHADER = """
        uniform shader content;
        uniform float2 resolution;
        uniform float time;
        uniform float2 pointer;
        uniform float pointerStrength;

        // Post-processing uniform drivers
        uniform float barrelWarp;        // Subtle CRT barrel curvature (0.04)
        uniform float scanlineDark;      // Fine horizontal scanlines (0.12)
        uniform float glitchIntensity;   // Horizontal sync-roll glitch jitter (0.0 .. 1.0)
        uniform float crimsonAlert;      // Crimson alert flare when Titan Kernel Panic or player damage (0.0 .. 1.0)
        uniform float combatAberration;  // Dynamic edge chromatic aberration scaling during combat (0.0 .. 1.0)
        uniform float uSpecularBoost;    // Specular overdrive (1.4x boost for vivid neon lasers/bullets)

        // Kinetic Shockwave uniform drivers
        uniform float2 shockwaveOrigin;  // In pixel coordinates
        uniform float shockwaveProgress;// 0.0 (triggered) to 1.0 (completed)
        uniform float shockwaveIntensity;// Shockwave amplitude (0.0 .. 1.0)

        const float PI = 3.14159265359;

        // CRT Barrel Distortion
        float2 crtDistort(float2 uv, float bend) {
            float2 centered = uv - 0.5;
            float r2 = dot(centered, centered);
            centered *= 1.0 + r2 * bend * 1.8;
            return centered + 0.5;
        }

        half4 main(float2 fragCoord) {
            float2 uv = fragCoord / resolution;

            // 1. Subtle CRT Barrel Distortion
            float2 warpedUV = crtDistort(uv, barrelWarp);
            if (warpedUV.x < 0.0 || warpedUV.x > 1.0 || warpedUV.y < 0.0 || warpedUV.y > 1.0) {
                return half4(0.015, 0.02, 0.035, 1.0);
            }
            float2 warpedCoord = warpedUV * resolution;

            // 2. Horizontal Sync-Roll Glitch Jitter
            if (glitchIntensity > 0.005) {
                float glitchY = floor(warpedCoord.y * 0.06);
                float glitchTime = floor(time * 24.0);
                float glitchNoise = fract(sin(dot(float2(glitchY, glitchTime), float2(12.9898, 78.233))) * 43758.5453);
                float isGlitchLine = step(0.90 - glitchIntensity * 0.45, glitchNoise);
                float glitchOffset = (glitchNoise - 0.5) * isGlitchLine * (glitchIntensity * 42.0);
                warpedCoord.x += glitchOffset;
            }

            // 3. Kinetic Shockwave Ripple Rings with Radial Chromatic Dispersion
            float2 shockOffset = float2(0.0);
            float shockRadialDisp = 0.0;
            if (shockwaveProgress >= 0.0 && shockwaveProgress < 1.0 && shockwaveIntensity > 0.01) {
                float2 shockCenter = (shockwaveOrigin.x > 0.0 || shockwaveOrigin.y > 0.0) ? shockwaveOrigin : (resolution * 0.5);
                float2 toCenter = warpedCoord - shockCenter;
                float dist = length(toCenter);
                float maxR = max(resolution.x, resolution.y) * 0.85;
                float currentRadius = shockwaveProgress * maxR;
                float ringDist = abs(dist - currentRadius);
                float ringThickness = 32.0 + shockwaveProgress * 28.0;

                float ringFactor = exp(-(ringDist * ringDist) / (2.0 * ringThickness * ringThickness));
                float decay = (1.0 - shockwaveProgress) * shockwaveIntensity;
                float waveStrength = ringFactor * decay * 32.0;

                if (waveStrength > 0.05) {
                    float2 waveDir = dist > 0.001 ? toCenter / dist : float2(0.0, 1.0);
                    shockOffset = waveDir * waveStrength;
                    shockRadialDisp = waveStrength * 0.65;
                }
            }

            // 4. Dynamic Edge Chromatic Aberration
            float2 centeredNorm = warpedUV - 0.5;
            float edgeDist = length(centeredNorm) * 1.414;
            float caSpread = 1.2 + edgeDist * (combatAberration * 9.0 + 3.0) + shockRadialDisp;

            // Displaced samples for RGB chromatic separation
            float2 coordR = clamp(warpedCoord + float2(caSpread, 0.0) + shockOffset, float2(0.0), resolution);
            float2 coordG = clamp(warpedCoord + shockOffset * 0.6, float2(0.0), resolution);
            float2 coordB = clamp(warpedCoord - float2(caSpread, 0.0) + shockOffset * 0.2, float2(0.0), resolution);

            half r = content.eval(coordR).r;
            half4 sampleG = content.eval(coordG);
            half b = content.eval(coordB).b;
            half a = sampleG.a;

            half3 col = half3(r, sampleG.g, b);

            // 5. Fine Horizontal Scanlines
            if (scanlineDark > 0.005) {
                float scanline = sin(warpedCoord.y * 1.5 + time * 7.0);
                float scanlineFactor = 1.0 - (scanline * 0.5 + 0.5) * scanlineDark;
                col *= half(scanlineFactor);
            }

            // 6. Specular Boost for Vivid Neon Lasers, Bullets, and Circuit Traces
            half maxVal = max(col.r, max(col.g, col.b));
            half neonMask = smoothstep(half(0.50), half(0.95), maxVal);
            col += col * neonMask * half(max(uSpecularBoost - 1.0, 0.0));

            // 7. Crimson Alert Flare (Titan Kernel Panic or Player Damage)
            if (crimsonAlert > 0.005) {
                float vignette = edgeDist;
                half alertFlare = half(smoothstep(0.35, 1.1, vignette) * crimsonAlert * 0.5);
                col = mix(col, half3(1.0, 0.06, 0.15), alertFlare);
                col.r += half(crimsonAlert * 0.07);
            }

            col = min(clamp(col, half(0.0), half(1.0)), half3(a));
            return half4(col, a);
        }
    """
}
