package io.github.shaderengine.presets

import org.intellij.lang.annotations.Language

/**
 * Military FLIR / Ironbow false-color thermal infrared lens.
 * Maps UI component luminance into an infrared spectrum with CRT sensor scanlines.
 */
object ThermalFlirPreset {
    @Language("AGSL")
    const val SHADER = """
        uniform shader content;
        uniform float2 resolution;
        uniform float time;
        uniform float2 pointer;
        uniform float p1; // Thermal sensitivity / contrast (0.5 to 2.0)
        uniform float p2; // Scanline intensity (0.0 to 0.4)
        uniform float p3; // Sensor noise strength (0.0 to 0.2)

        float hash21(float2 p) {
            p = fract(p * float2(234.34, 435.345));
            p += dot(p, p + 34.23);
            return fract(p.x * p.y);
        }

        // Ironbow thermal gradient: Indigo -> Violet -> Magenta -> Orange -> Yellow -> White
        half3 ironbow(float t) {
            t = clamp(t, 0.0, 1.0);
            half3 c0 = half3(0.05, 0.02, 0.25); // Deep indigo
            half3 c1 = half3(0.35, 0.05, 0.55); // Violet
            half3 c2 = half3(0.85, 0.15, 0.35); // Hot magenta
            half3 c3 = half3(1.00, 0.55, 0.05); // Blazing orange
            half3 c4 = half3(1.00, 0.95, 0.30); // Bright yellow
            half3 c5 = half3(1.00, 1.00, 1.00); // White core

            if (t < 0.2) return mix(c0, c1, t / 0.2);
            if (t < 0.4) return mix(c1, c2, (t - 0.2) / 0.2);
            if (t < 0.7) return mix(c2, c3, (t - 0.4) / 0.3);
            if (t < 0.9) return mix(c3, c4, (t - 0.7) / 0.2);
            return mix(c4, c5, (t - 0.9) / 0.1);
        }

        half4 main(float2 fragCoord) {
            half4 src = content.eval(fragCoord);
            float luma = dot(src.rgb, half3(0.299, 0.587, 0.114));

            // Thermal contrast adjustment
            float thermal = pow(luma, 1.0 / max(p1, 0.2));

            // Analog sensor noise
            float noise = (hash21(fragCoord + float2(time * 60.0, 0.0)) - 0.5) * p3;
            thermal = clamp(thermal + noise, 0.0, 1.0);

            // Ironbow palette
            half3 thermalCol = ironbow(thermal);

            // CRT horizontal thermal sensor scanlines
            float scan = 1.0 - sin(fragCoord.y * 1.5) * p2 * 0.5;
            thermalCol *= scan;

            return half4(thermalCol, src.a);
        }
    """
}
