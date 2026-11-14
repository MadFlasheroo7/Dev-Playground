package io.github.shaderengine.presets

import org.intellij.lang.annotations.Language

/**
 * Military Gen-3 P-43 phosphor night vision monocular lens.
 * Features high photon amplification, dynamic scintillation, and optical barrel vignetting.
 */
object NightVisionPreset {
    @Language("AGSL")
    const val SHADER = """
        uniform shader content;
        uniform float2 resolution;
        uniform float time;
        uniform float2 pointer;
        uniform float p1; // Light amplification gain (1.0 to 3.0)
        uniform float p2; // Scintillation noise strength (0.05 to 0.35)
        uniform float p3; // Optical vignette intensity (0.2 to 0.9)

        float hash21(float2 p) {
            p = fract(p * float2(123.34, 456.21));
            p += dot(p, p + 45.32);
            return fract(p.x * p.y);
        }

        half4 main(float2 fragCoord) {
            half4 src = content.eval(fragCoord);
            float luma = dot(src.rgb, half3(0.299, 0.587, 0.114));

            // High-gain luminance amplification
            float gain = max(p1, 0.5);
            float amplified = pow(luma * gain, 0.7);

            // High-frequency photon scintillation noise
            float noise = (hash21(fragCoord + float2(time * 120.0, time * 80.0)) - 0.5) * p2;
            amplified = clamp(amplified + noise, 0.0, 1.0);

            // P-43 Phosphor spectral response: vibrant green
            half3 phosphor = half3(0.12, 1.0, 0.24) * amplified;

            // Subtle scanlines
            float scan = 1.0 - sin(fragCoord.y * 2.0) * 0.04;
            phosphor *= scan;

            // Optical barrel vignetting
            float2 uv = fragCoord / resolution;
            float2 centerOffset = uv - float2(0.5);
            float dist = length(centerOffset);
            float vignette = smoothstep(0.75, 0.25, dist * p3 * 1.5);
            phosphor *= vignette;

            return half4(phosphor, src.a);
        }
    """
}
