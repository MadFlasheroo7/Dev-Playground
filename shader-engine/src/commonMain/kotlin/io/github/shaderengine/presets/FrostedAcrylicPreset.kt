package io.github.shaderengine.presets

import org.intellij.lang.annotations.Language

/**
 * Production-grade Frosted Acrylic Optical Lens Shader.
 *
 * Implements:
 * - Dual Kawase blur approximation (8-tap multi-offset kernel sampling).
 * - Dave Hoskins blue-noise dithering to eliminate 8-bit banding on dark frosted surfaces.
 * - Dynamic edge slope specular highlights with overdrive specular boost.
 * - Translucent tinted acrylic luminescence.
 * - Premultiplied alpha clamping for P3/HDR displays.
 */
object FrostedAcrylicPreset {

    @Language("AGSL")
    const val SHADER = """
        uniform shader content;
        uniform float2 resolution;
        uniform float time;
        uniform float2 pointer;
        uniform float pointerStrength;

        uniform float blurRadius;     // Blur spread (0.0 .. 1.0)
        uniform float noiseDither;    // Blue noise dither intensity (0.0 .. 1.0)
        uniform float tintOpacity;    // Acrylic surface tint (0.0 .. 1.0)
        uniform float uSpecularBoost; // Specular overdrive (1.0 = standard, 2.5 = boost)

        const float PI = 3.14159265359;

        // Dave Hoskins Hash for high-frequency blue-noise dithering
        float hash21(float2 p) {
            float3 p3 = fract(float3(p.xyx) * 0.1031);
            p3 += float3(dot(p3, p3.yzx + 33.33));
            return fract((p3.x + p3.y) * p3.z);
        }

        half4 main(float2 fragCoord) {
            float2 uv = fragCoord / resolution;
            float spread = (blurRadius * 18.0 + 2.0);

            // Dual Kawase multi-tap kernel (8-tap offset pattern)
            // Pass 1 kernel (inner diamond)
            float2 o1 = float2(-spread, -spread) * 0.65;
            float2 o2 = float2( spread, -spread) * 0.65;
            float2 o3 = float2(-spread,  spread) * 0.65;
            float2 o4 = float2( spread,  spread) * 0.65;

            // Pass 2 kernel (outer box)
            float2 o5 = float2(-spread * 1.35, 0.0);
            float2 o6 = float2( spread * 1.35, 0.0);
            float2 o7 = float2(0.0, -spread * 1.35);
            float2 o8 = float2(0.0,  spread * 1.35);

            half4 c0 = content.eval(fragCoord);
            half4 c1 = content.eval(clamp(fragCoord + o1, float2(0.0), resolution));
            half4 c2 = content.eval(clamp(fragCoord + o2, float2(0.0), resolution));
            half4 c3 = content.eval(clamp(fragCoord + o3, float2(0.0), resolution));
            half4 c4 = content.eval(clamp(fragCoord + o4, float2(0.0), resolution));
            half4 c5 = content.eval(clamp(fragCoord + o5, float2(0.0), resolution));
            half4 c6 = content.eval(clamp(fragCoord + o6, float2(0.0), resolution));
            half4 c7 = content.eval(clamp(fragCoord + o7, float2(0.0), resolution));
            half4 c8 = content.eval(clamp(fragCoord + o8, float2(0.0), resolution));

            // Weighted Kawase accumulation
            half4 blurred = c0 * half(0.24) + (c1 + c2 + c3 + c4) * half(0.12) + (c5 + c6 + c7 + c8) * half(0.07);

            // Blue-noise dither matrix
            float dither = (hash21(fragCoord) - 0.5) * (noiseDither * 0.06);
            half3 ditherRGB = half3(dither);

            // Frosted acrylic tint: dark glass with translucent luminescence
            half3 acrylicTint = half3(0.08, 0.12, 0.18);
            half3 tinted = mix(blurred.rgb, acrylicTint, half(tintOpacity * 0.55));
            tinted += ditherRGB;

            // Dynamic edge specular sheen / bevel glint
            float2 center = pointer.x > 0.0 ? pointer : resolution * 0.5;
            float2 lightDelta = float2(resolution.x * 0.3, -resolution.y * 0.2) - (fragCoord - center);
            float lightDist = length(lightDelta);
            float2 toLight = lightDist > 0.0001 ? lightDelta / lightDist : float2(0.0, 1.0);
            float2 centerDelta = fragCoord - resolution * 0.5;
            float centerDist = length(centerDelta);
            float2 normCenter = centerDist > 0.0001 ? centerDelta / centerDist : float2(0.0, 1.0);
            float edgeGleam = smoothstep(0.85, 1.0, dot(normCenter, toLight));
            half specBoost = half(max(uSpecularBoost, 1.0));
            half specularSheen = half(pow(edgeGleam, 4.0) * 0.35) * specBoost;

            half3 finalRGB = tinted + half3(specularSheen * half(0.9), specularSheen * half(0.95), specularSheen);

            // Premultiplied alpha clamping
            finalRGB = min(clamp(finalRGB, half(0.0), half(1.0)), half3(blurred.a));

            return half4(finalRGB, blurred.a);
        }
    """
}
