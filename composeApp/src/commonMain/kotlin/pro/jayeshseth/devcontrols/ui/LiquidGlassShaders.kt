package pro.jayeshseth.devcontrols.ui

/**
 * AGSL (Android 13+) / SkSL (Desktop Compose via Skia) shader for the animated
 * liquid glass background.  Domain-warped FBM produces organic fluid shapes in a
 * deep navy → teal → indigo palette.
 *
 * Declare order matters for the Desktop uniform-byte-packing in ShaderBackground:
 *   float2 iResolution  (8 bytes)
 *   float  iTime        (4 bytes)
 * Total = 12 bytes = 3 × float LE
 */
const val LIQUID_FLOW_SHADER = """
uniform float2 iResolution;
uniform float  iTime;

float hash(float2 p) {
    return fract(sin(dot(p, float2(127.1, 311.7))) * 43758.5453);
}

float noise(float2 p) {
    float2 i = floor(p);
    float2 f = fract(p);
    float2 u = f * f * (3.0 - 2.0 * f);
    return mix(
        mix(hash(i),                hash(i + float2(1.0, 0.0)), u.x),
        mix(hash(i + float2(0.0, 1.0)), hash(i + float2(1.0, 1.0)), u.x),
        u.y
    );
}

float fbm(float2 seed) {
    float2 q = seed;
    float  v = 0.0;
    float  a = 0.5;
    for (int i = 0; i < 5; i++) {
        v += a * noise(q);
        q  = q * 2.02 + float2(3.91, 1.71);
        a *= 0.5;
    }
    return v;
}

half4 main(float2 fragCoord) {
    float2 uv = fragCoord / iResolution;
    float  ar = iResolution.x / iResolution.y;
    float  t  = iTime * 0.10;

    float2 p = (uv - float2(0.5, 0.5)) * float2(ar, 1.0) * 3.0;

    // Two passes of domain warping give organic fluid motion
    float2 q = float2(
        fbm(p + float2(0.0, 0.0) + t),
        fbm(p + float2(5.2, 1.3)  + t * 0.9)
    );
    float2 r = float2(
        fbm(p + 3.8 * q + float2(1.7, 9.2) + 0.15 * t),
        fbm(p + 3.8 * q + float2(8.3, 2.8) + 0.12 * t)
    );
    float f = fbm(p + 4.0 * r);

    // Deep-navy → ocean → teal → dark-indigo palette
    float3 c0 = float3(0.010, 0.028, 0.085);
    float3 c1 = float3(0.000, 0.088, 0.225);
    float3 c2 = float3(0.000, 0.158, 0.335);
    float3 c3 = float3(0.048, 0.000, 0.180);

    float3 col = mix(c0, c1, clamp(f * 2.5,       0.0, 1.0));
    col        = mix(col, c2, clamp(f * 2.5 - 0.8, 0.0, 1.0));
    col        = mix(col, c3, clamp(f * 2.5 - 1.5, 0.0, 1.0));

    // Accent glows — cyan highlight
    col += smoothstep(0.62, 0.88, f)       * float3(0.00, 0.55, 0.85) * 0.42;
    // Mint accent
    col += smoothstep(0.70, 0.85, f)       * float3(0.00, 0.80, 0.65) * 0.20;
    // Purple counter-glow
    col += smoothstep(0.50, 0.70, 1.0 - f) * float3(0.22, 0.00, 0.52) * 0.20;

    // Edge vignette
    float2 vig = uv * (1.0 - uv);
    col *= pow(max(vig.x * vig.y * 16.0, 0.0), 0.20);

    return half4(half3(col), 1.0);
}
"""
