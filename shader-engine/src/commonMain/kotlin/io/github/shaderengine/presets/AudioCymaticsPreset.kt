package io.github.shaderengine.presets

import org.intellij.lang.annotations.Language

/**
 * Audio-reactive Chladni cymatics resonance shader.
 * Simulates physical acoustic standing wave plate nodal geometries that pulse, ripple,
 * and morph in response to audio frequency uniforms:
 * - [uBass]: Drives macro plate harmonic modes ($m, n$) and geometric expansion
 * - [uMid]: Drives fine harmonic nodal lines and concentric ripple frequency
 * - [uTreble]: Drives high-frequency particle shimmer and acoustic dispersion
 * - [uAudioEnergy]: Drives core incandescent glow and luminance gain
 */
object AudioCymaticsPreset {
    @Language("AGSL")
    const val SHADER = """
        uniform float2 resolution;
        uniform float time;
        uniform float2 pointer;
        uniform float uBass;
        uniform float uMid;
        uniform float uTreble;
        uniform float uAudioEnergy;
        uniform float p1; // Geometric complexity gain (0.5 to 2.0)
        uniform float p2; // Cymatic nodal line sharpness (1.0 to 12.0)
        uniform float p3; // Chromatic resonance dispersion (0.0 to 0.4)

        const float PI = 3.14159265359;

        // Chladni 2D standing wave equation: a * sin(n*PI*x)*sin(m*PI*y) - b * sin(m*PI*x)*sin(n*PI*y)
        float chladni(float2 p, float n, float m) {
            float t1 = sin(n * PI * p.x) * sin(m * PI * p.y);
            float t2 = sin(m * PI * p.x) * sin(n * PI * p.y);
            return t1 - t2;
        }

        half4 main(float2 fragCoord) {
            float2 uv = (fragCoord - resolution * 0.5) / min(resolution.x, resolution.y);
            float r = length(uv);

            // Dynamic harmonic modes driven by audio frequency bands
            float bassGain = max(uBass, 0.05);
            float midGain = max(uMid, 0.05);
            float trebleGain = max(uTreble, 0.05);
            float energy = max(uAudioEnergy, 0.05);

            float n = 2.0 + floor(bassGain * 5.0) * max(p1, 0.5);
            float m = 3.0 + floor(midGain * 6.0) * max(p1, 0.5);

            // Polar acoustic rotation with time
            float angle = time * (0.3 + bassGain * 0.6);
            float s = sin(angle);
            float c = cos(angle);
            float2 rotUV = float2(uv.x * c - uv.y * s, uv.x * s + uv.y * c);

            // Evaluate acoustic nodal wave field
            float wave = chladni(rotUV * 1.8, n, m);
            float wave2 = chladni(rotUV * 2.6, m + 1.0, n + 2.0);
            float combinedWave = mix(wave, wave2, midGain);

            // Sharp nodal line boundary
            float sharpness = max(p2, 1.0) * (3.0 + bassGain * 8.0);
            float nodalLine = 1.0 - clamp(abs(combinedWave) * sharpness, 0.0, 1.0);
            nodalLine = pow(nodalLine, 2.5);

            // Chromatic resonance dispersion (p3 slider: 0.0 to 0.4)
            float disp = p3 * 0.05;
            float waveR = chladni((rotUV + float2(disp, 0.0)) * 1.8, n, m);
            float waveB = chladni((rotUV - float2(disp, 0.0)) * 1.8, n, m);
            float nodalR = pow(1.0 - clamp(abs(waveR) * sharpness, 0.0, 1.0), 2.5);
            float nodalB = pow(1.0 - clamp(abs(waveB) * sharpness, 0.0, 1.0), 2.5);

            // Concentric circular ripples from audio energy
            float ripple = sin(r * 32.0 - time * 8.0 * bassGain) * 0.5 + 0.5;
            ripple *= smoothstep(0.7, 0.1, r) * trebleGain;

            // Chromatic electric neon palette: Deep Indigo -> Electric Cyan -> Hot Gold
            half3 cBass = half3(0.08, 0.25, 0.95);
            half3 cMid = half3(0.05, 0.95, 0.85);
            half3 cTreble = half3(1.00, 0.82, 0.20);

            half3 acousticColor = mix(cBass, cMid, half(midGain)) * half(nodalLine * 2.2);
            acousticColor.r += half(nodalR * 0.6);
            acousticColor.b += half(nodalB * 0.6);
            acousticColor += cTreble * half(ripple * 1.5 + trebleGain * 0.4);

            // Core energy bloom
            float coreGlow = smoothstep(0.45, 0.0, r) * energy * 1.8;
            acousticColor += half3(0.9, 0.7, 1.0) * half(coreGlow);

            // Deep resonant acoustic plate background
            half3 plateBg = half3(0.03, 0.04, 0.08);
            half3 finalColor = plateBg + acousticColor;

            // Subtle perimeter vignette
            float2 qVig = fragCoord / resolution;
            finalColor *= half(clamp(16.0 * qVig.x * qVig.y * (1.0 - qVig.x) * (1.0 - qVig.y), 0.0, 1.0));

            return half4(clamp(finalColor, half3(0.0), half3(1.0)), 1.0);
        }
    """
}
