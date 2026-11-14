package io.github.shaderengine.presets

import org.intellij.lang.annotations.Language

/**
 * Cyberpunk Hologram & CRT Optical Lens Shader.
 *
 * Implements:
 * - Geometric CRT barrel curvature distortion.
 * - Dynamic RGB sub-pixel phosphor triads (aperture grille simulation).
 * - Animated horizontal scanlines with luminance attenuation.
 * - Periodic horizontal sync-roll glitches.
 * - Edge chromatic aberration and screen glare specular boost.
 * - Alpha invariant clamping.
 */
object CyberpunkHologramPreset {

    @Language("AGSL")
    const val SHADER = """
        uniform shader content;
        uniform float2 resolution;
        uniform float time;
        uniform float2 pointer;
        uniform float pointerStrength;

        uniform float barrelWarp;     // CRT screen curvature (0.0 .. 1.0)
        uniform float scanlineDark;   // Scanline depth (0.0 .. 1.0)
        uniform float glitchRate;     // Periodic horizontal sync glitch (0.0 .. 1.0)
        uniform float uSpecularBoost; // Specular overdrive (1.0 = standard, 2.5 = boost)

        const float PI = 3.14159265359;

        float hash11(float p) {
            p = fract(p * 0.1031);
            p *= p + 33.33;
            p *= p + p;
            return fract(p);
        }

        // CRT Barrel Distortion
        float2 crtDistort(float2 uv, float bend) {
            float2 centered = uv - 0.5;
            float r2 = dot(centered, centered);
            centered *= 1.0 + r2 * bend * 1.8;
            return centered + 0.5;
        }

        half4 main(float2 fragCoord) {
            float2 uv = fragCoord / resolution;

            // Apply barrel warp
            float2 warpedUV = crtDistort(uv, barrelWarp * 0.35);

            // Screen border cutoff / chassis housing
            if (warpedUV.x < 0.0 || warpedUV.x > 1.0 || warpedUV.y < 0.0 || warpedUV.y > 1.0) {
                return half4(0.01, 0.01, 0.02, 1.0);
            }

            float2 warpedCoord = warpedUV * resolution;

            // Periodic horizontal sync roll / glitch displacement
            float glitchPhase = fract(time * 0.65);
            float glitchTrigger = step(0.92 - glitchRate * 0.15, glitchPhase);
            float glitchY = floor(warpedCoord.y * 0.04);
            float glitchOffset = (hash11(glitchY + time) - 0.5) * (glitchTrigger * 38.0 * (glitchRate + 0.2));
            warpedCoord.x += glitchOffset;

            // Chromatic dispersion along horizontal axis
            float caSpread = (barrelWarp * 6.0 + 3.0 + glitchTrigger * 12.0);
            half r = content.eval(clamp(warpedCoord + float2(caSpread, 0.0), float2(0.0), resolution)).r;
            half4 gSample = content.eval(clamp(warpedCoord, float2(0.0), resolution));
            half b = content.eval(clamp(warpedCoord - float2(caSpread, 0.0), float2(0.0), resolution)).b;
            half a = gSample.a;
            half3 col = half3(r, gSample.g, b);

            // Animated CRT Scanlines
            float scanline = sin(warpedCoord.y * 1.5 + time * 8.0);
            float scanlineFactor = 1.0 - (scanline * 0.5 + 0.5) * (scanlineDark * 0.45);
            col *= half(scanlineFactor);

            // RGB Sub-pixel Phosphor Triad Mask (vertical striping)
            int pixelX = int(mod(fragCoord.x, 3.0));
            half3 triad = half3(0.7, 0.7, 0.7);
            if (pixelX == 0) triad = half3(1.15, 0.65, 0.65);
            else if (pixelX == 1) triad = half3(0.65, 1.15, 0.65);
            else triad = half3(0.65, 0.65, 1.15);
            col *= triad;

            // CRT Screen Glass Glare / Specular highlight with boost
            float2 cornerDist = abs(warpedUV - 0.5) * 2.0;
            float borderVignette = clamp(1.0 - dot(cornerDist, cornerDist) * 0.4, 0.0, 1.0);
            half specBoost = half(max(uSpecularBoost, 1.0));
            half glassGlint = half(pow(borderVignette, 3.0) * 0.18) * specBoost;
            col += half3(glassGlint * half(0.8), glassGlint, glassGlint * half(1.2));

            // Hologram phosphor bloom
            col += half3(0.02, 0.08, 0.12) * half(1.0 + pointerStrength);

            // Premultiplied alpha clamping
            col = min(clamp(col, half(0.0), half(1.0)), half3(a));

            return half4(col, a);
        }
    """
}
