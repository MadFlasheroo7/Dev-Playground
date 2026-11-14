package io.github.shaderengine.presets

import org.intellij.lang.annotations.Language

/**
 * Kinetic Shockwave Ripple Post-Processing Preset.
 *
 * Emits expanding concentric sinusoidal shockwaves from the pointer position,
 * warping the underlying UI with radial chromatic aberration.
 */
object ShockwaveRipplePreset {

    @Language("AGSL")
    const val SHADER = """
        uniform shader content;
        uniform float2 resolution;
        uniform float time;
        uniform float2 pointer;
        uniform float pointerStrength;

        uniform float amplitude;  // Shockwave displacement amplitude (0.0 .. 1.0)
        uniform float frequency;  // Wave ring frequency (0.0 .. 1.0)
        uniform float dispersion; // Chromatic aberration intensity (0.0 .. 1.0)
        uniform float uSpecularBoost; // Specular overdrive (1.0 = standard, 2.5 = boost)

        const float PI = 3.14159265359;

        half4 main(float2 fragCoord) {
            float2 origin = pointer.x > 0.0 ? pointer : resolution * 0.5;
            float2 toFrag = fragCoord - origin;
            float dist = length(toFrag);

            // Expanding wave speed
            float wavePhase = dist * (frequency * 0.08 + 0.02) - time * 6.0;
            float wave = sin(wavePhase);

            // Attenuation: ripple decays with distance from cursor
            float decay = exp(-dist / (resolution.x * 0.45));
            float strength = wave * decay * (amplitude * 35.0) * (0.5 + 0.5 * pointerStrength);

            // Spatial early-out
            if (abs(strength) < 0.1) {
                return content.eval(fragCoord);
            }

            float2 dir = normalize(toFrag + float2(0.0001));
            float2 displacement = dir * strength;

            // Chromatic aberration along radial direction
            float dispOffset = strength * (dispersion * 0.5);
            float2 coordR = clamp(fragCoord - displacement - dir * dispOffset, float2(0.0), resolution);
            float2 coordG = clamp(fragCoord - displacement, float2(0.0), resolution);
            float2 coordB = clamp(fragCoord - displacement + dir * dispOffset, float2(0.0), resolution);

            half r = content.eval(coordR).r;
            half4 sampleG = content.eval(coordG);
            half b = content.eval(coordB).b;
            half a = sampleG.a;

            half3 finalRGB = half3(r, sampleG.g, b);

            // Highlight on ripple crests
            half specBoost = half(max(uSpecularBoost, 1.0));
            half crest = max(half(wave * decay * 0.3), half(0.0)) * specBoost;
            finalRGB += half3(crest, crest * half(0.9), crest * half(1.1));

            finalRGB = min(clamp(finalRGB, half(0.0), half(1.0)), half3(a));
            return half4(finalRGB, a);
        }
    """
}
