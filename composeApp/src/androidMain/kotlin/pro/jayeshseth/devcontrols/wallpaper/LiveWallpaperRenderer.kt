package pro.jayeshseth.devcontrols.wallpaper

import android.graphics.RuntimeShader
import android.os.Build
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import pro.jayeshseth.devcontrols.data.WallpaperSceneType
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

data class TouchRipple(
    val x: Float,
    val y: Float,
    var age: Float = 0f,
    val maxAge: Float = 1.2f,
    val maxRadius: Float = 280f,
    val color: Color = Color.White
)

data class WallpaperRenderState(
    val scene: WallpaperSceneType = WallpaperSceneType.DEEP_NEBULA,
    val timeSeconds: Float = 0f,
    val tiltX: Float = 0f, // Roll (-1..1)
    val tiltY: Float = 0f, // Pitch (-1..1)
    val launcherOffset: Float = 0.5f, // 0..1 horizontal page scroll
    val particleDensity: Float = 1.0f,
    val animationSpeed: Float = 1.0f,
    val touchIntensity: Float = 1.0f,
    val activeRipples: List<TouchRipple> = emptyList()
)

// =============================================================================
// AGSL SHADER PROGRAMS (Native GPU Fragment Shaders for Android 13+)
// Every shader declares AND actively uses: iResolution, iTime, iTilt, iTouch
// to guarantee the SkSL optimizer never dead-code-strips any uniform.
// =============================================================================

private const val NEBULA_AGSL = """
uniform float2 iResolution;
uniform float iTime;
uniform float2 iTilt;
uniform float2 iTouch;

float hash(float2 p) {
    p = fract(p * float2(123.34, 456.21));
    p += dot(p, p + 45.32);
    return fract(p.x * p.y);
}

float noise(float2 p) {
    float2 i = floor(p);
    float2 f = fract(p);
    f = f * f * (3.0 - 2.0 * f);
    float a = hash(i);
    float b = hash(i + float2(1.0, 0.0));
    float c = hash(i + float2(0.0, 1.0));
    float d = hash(i + float2(1.0, 1.0));
    return mix(mix(a, b, f.x), mix(c, d, f.x), f.y);
}

float fbm(float2 p) {
    float v = 0.0;
    float a = 0.5;
    for (int i = 0; i < 5; ++i) {
        v += a * noise(p);
        p = float2(p.x * 0.866 - p.y * 0.5, p.x * 0.5 + p.y * 0.866) * 2.0 + float2(21.4, 43.1);
        a *= 0.5;
    }
    return v;
}

half4 main(float2 fragCoord) {
    float2 uv = (fragCoord - 0.5 * iResolution) / min(iResolution.x, iResolution.y);
    uv += iTilt * 0.12;
    
    // Graviton vortex centered at touch
    float2 touchUv = (iTouch - 0.5 * iResolution) / min(iResolution.x, iResolution.y);
    float2 toTouch = uv - touchUv;
    float dTouch = length(toTouch);
    float swirl = 0.65 / (dTouch + 0.22);
    float ca = cos(swirl); float sa = sin(swirl);
    float2 warpedUv = touchUv + float2(toTouch.x * ca - toTouch.y * sa, toTouch.x * sa + toTouch.y * ca);

    float t = iTime * 0.18;
    float2 q = float2(fbm(warpedUv * 2.2 + t * 0.2), fbm(warpedUv * 2.2 + float2(5.2, 1.3)));
    float2 r = float2(fbm(warpedUv * 2.5 + 3.8 * q + float2(1.7, 9.2) + t * 0.25),
                      fbm(warpedUv * 2.5 + 3.8 * q + float2(8.3, 2.8) - t * 0.18));
    float f = fbm(warpedUv * 2.4 + 3.5 * r);

    // Deep cosmic palette mapping
    half3 col = mix(half3(0.02, 0.01, 0.07), half3(0.24, 0.07, 0.48), half(clamp(f * f * 3.2, 0.0, 1.0)));
    col = mix(col, half3(0.06, 0.44, 0.74), half(clamp(length(q) * 1.1, 0.0, 1.0)));
    col = mix(col, half3(0.88, 0.32, 0.95), half(clamp(r.x * r.x * 1.6, 0.0, 1.0)));
    col += half3(0.96, 0.88, 0.52) * half(pow(f, 3.8) * 2.2);

    // Starfield twinkle
    float starHash = hash(floor(fragCoord * 0.14));
    if (starHash > 0.982) {
        float twinkle = sin(iTime * 4.5 + starHash * 100.0) * 0.5 + 0.5;
        col += half3(0.92, 0.96, 1.0) * half(twinkle * (starHash - 0.982) * 55.0);
    }
    
    float vig = 1.0 - length(fragCoord / iResolution - 0.5) * 0.65;
    return half4(col * half(vig), 1.0);
}
"""

private const val BLACK_HOLE_AGSL = """
uniform float2 iResolution;
uniform float iTime;
uniform float2 iTilt;
uniform float2 iTouch;

float hash(float2 p) {
    return fract(sin(dot(p, float2(12.9898, 78.233))) * 43758.5453);
}

half4 main(float2 fragCoord) {
    float2 uv = (fragCoord - 0.5 * iResolution) / min(iResolution.x, iResolution.y);
    float2 bhCenter = (iTouch - 0.5 * iResolution) / min(iResolution.x, iResolution.y) + iTilt * 0.08;
    float2 d = uv - bhCenter;
    float r = length(d);
    float rs = 0.17; // Schwarzschild radius
    
    // Gravitational Lensing (Einstein ray deflection)
    float defl = (rs * rs * 0.88) / max(r, 0.001);
    float2 lensedUv = uv + normalize(d) * defl;
    
    // Accretion disk coordinate transform (tilted plane)
    float2 diskP = lensedUv - bhCenter;
    diskP.y /= 0.36; // Perspective inclination
    float diskR = length(diskP);
    float diskPhi = atan(diskP.y, diskP.x) - iTime * 2.2;
    
    half3 col = half3(0.012, 0.012, 0.022); // Void background
    
    // Background lensed stars
    float s = hash(floor(lensedUv * 32.0));
    if (s > 0.976 && r > rs * 0.96) {
        col += half3(0.85, 0.9, 1.0) * half((s - 0.976) * 42.0);
    }
    
    // Accretion Disk Physics
    if (diskR > rs * 1.15 && diskR < rs * 3.8) {
        float innerFade = smoothstep(rs * 1.15, rs * 1.55, diskR);
        float outerFade = smoothstep(rs * 3.8, rs * 2.3, diskR);
        float bands = sin(diskR * 50.0 - iTime * 3.5) * 0.18 + 0.82;
        float swirls = sin(diskPhi * 4.0 + diskR * 14.0) * 0.25 + 0.75;
        
        // Relativistic Doppler beaming (left side approaches, blue-shifted & brighter)
        float doppler = 1.0 - 0.62 * (diskP.x / diskR);
        
        float intensity = innerFade * outerFade * bands * swirls * doppler;
        half3 diskCol = mix(half3(1.0, 0.22, 0.04), half3(1.0, 0.88, 0.42), half(clamp(intensity * 0.95, 0.0, 1.0)));
        diskCol = mix(diskCol, half3(0.35, 0.75, 1.0), half(clamp((doppler - 1.0) * 0.65, 0.0, 1.0)));
        col += diskCol * half(intensity * 2.1);
    }
    
    // Photon Sphere (Sub-milliradian light ring)
    float ringDist = abs(r - rs * 1.16);
    float photonRing = exp(-ringDist * ringDist * 1800.0);
    col += half3(1.0, 0.96, 0.85) * half(photonRing * 2.6);
    
    // Event horizon absolute shadow
    float shadow = smoothstep(rs * 0.98, rs * 1.05, r);
    col *= half(shadow);
    
    return half4(col, 1.0);
}
"""

private const val NEO_TOKYO_AGSL = """
uniform float2 iResolution;
uniform float iTime;
uniform float2 iTilt;
uniform float2 iTouch;

float hash12(float2 p) {
    return fract(sin(dot(p, float2(127.1, 311.7))) * 43758.5453123);
}

float2 dropNormal(float2 uv, float t) {
    float2 grid = float2(16.0, 30.0);
    float2 id = floor(uv * grid);
    float2 st = fract(uv * grid) - 0.5;
    float n = hash12(id);
    
    float dropY = fract(t * (0.28 + n * 0.35) + n) * 1.4 - 0.2;
    float2 dropPos = float2((n - 0.5) * 0.4, dropY);
    float2 diff = st - dropPos;
    float d = length(diff);
    float r = 0.17 * (0.6 + 0.4 * n);
    
    if (d < r) {
        float z = sqrt(max(0.0, r * r - d * d));
        return normalize(float2(diff.x, diff.y * 1.6)) * (z / r);
    }
    return float2(0.0);
}

half4 main(float2 fragCoord) {
    float2 uv = fragCoord / iResolution;
    float aspect = iResolution.x / iResolution.y;
    float2 scaledUv = float2(uv.x * aspect, uv.y);
    
    // Procedural normal map from rain droplets
    float2 norm = dropNormal(scaledUv, iTime);
    norm += dropNormal(scaledUv * 1.8 + float2(0.35, 0.65), iTime * 1.25) * 0.5;
    
    // Touch water perturbation on glass
    float2 touchUv = iTouch / iResolution;
    float touchDist = length((uv - touchUv) * float2(aspect, 1.0));
    float touchWave = sin(touchDist * 36.0 - iTime * 7.0) * exp(-touchDist * 5.0) * 0.035;
    
    float2 distortedUv = uv + norm * 0.048 + iTilt * 0.035 + normalize(uv - touchUv + 0.001) * touchWave;
    
    // Moody Cyberpunk Night Gradient
    half3 bg = mix(half3(0.03, 0.04, 0.09), half3(0.06, 0.08, 0.18), half(1.0 - uv.y));
    
    // Distant Neon Bokeh Orbs
    for (int i = 0; i < 11; ++i) {
        float fi = float(i);
        float2 bPos = float2(
            fract(sin(fi * 71.3) * 452.1 + iTime * 0.015),
            0.32 + 0.55 * fract(cos(fi * 89.7) * 314.2)
        );
        float bDist = length((distortedUv - bPos) * float2(aspect, 1.0));
        float bRad = 0.07 + 0.06 * fract(fi * 17.5);
        float bGlow = smoothstep(bRad, 0.0, bDist);
        
        half3 neonCol = (i % 3 == 0) ? half3(0.0, 0.94, 1.0) : // Cyan
                        (i % 3 == 1) ? half3(1.0, 0.09, 0.62) : // Hot Magenta
                                       half3(0.55, 0.22, 1.0);  // Violet
        bg += neonCol * half(bGlow * 0.75);
    }
    
    // Rain specularity
    float spec = pow(max(0.0, dot(norm, normalize(float2(-0.45, 0.85)))), 18.0);
    bg += half3(0.92, 0.96, 1.0) * half(spec * 1.35);
    
    float vig = 1.0 - length(uv - 0.5) * 0.55;
    return half4(bg * half(vig), 1.0);
}
"""

private const val MATRIX_AGSL = """
uniform float2 iResolution;
uniform float iTime;
uniform float2 iTilt;
uniform float2 iTouch;

float hash(float2 p) {
    return fract(sin(dot(p, float2(127.1, 311.7))) * 43758.5453);
}

half4 main(float2 fragCoord) {
    float2 uv = fragCoord / iResolution;
    uv.x += iTilt.x * 0.05;
    
    // Touch shockwave perturbation
    float2 touchUv = iTouch / iResolution;
    float touchDist = length((uv - touchUv) * float2(iResolution.x / iResolution.y, 1.0));
    float shock = sin(touchDist * 38.0 - iTime * 6.5) * exp(-touchDist * 4.2) * 0.026;
    uv += normalize(uv - touchUv + 0.001) * shock;
    
    float numCols = 44.0;
    float col = floor(uv.x * numCols);
    float colFrac = fract(uv.x * numCols);
    
    float speed = (0.42 + hash(float2(col, 13.0)) * 0.62) * (1.0 + iTilt.y * 0.2);
    float t = iTime * speed;
    
    float numRows = 72.0;
    float row = floor((uv.y - t) * numRows);
    float rowFrac = fract((uv.y - t) * numRows);
    
    float glyph = hash(float2(col, row));
    float headY = fract(t * 0.34 + hash(float2(col, 47.0)));
    float distFromHead = fract(uv.y - headY);
    float tail = exp(-distFromHead * 4.8);
    
    half3 colOut = half3(0.012, 0.032, 0.015);
    
    if (colFrac > 0.16 && colFrac < 0.84 && rowFrac > 0.16 && rowFrac < 0.84) {
        if (glyph > 0.36) {
            half3 greenPhosphor = half3(0.14, 0.88, 0.32);
            if (distFromHead < 0.038) {
                colOut = half3(0.88, 1.0, 0.94);
            } else {
                colOut = greenPhosphor * half(tail * 1.55 + 0.08);
            }
        }
    }
    
    // CRT scanlines
    float scanline = sin(fragCoord.y * 1.55) * 0.1 + 0.9;
    colOut *= half(scanline);
    
    return half4(colOut, 1.0);
}
"""

private const val AURORA_AGSL = """
uniform float2 iResolution;
uniform float iTime;
uniform float2 iTilt;
uniform float2 iTouch;

float hash(float2 p) {
    return fract(sin(dot(p, float2(41.1, 289.7))) * 45758.5453);
}

float noise(float2 p) {
    float2 i = floor(p);
    float2 f = fract(p);
    f = f * f * (3.0 - 2.0 * f);
    return mix(mix(hash(i), hash(i + float2(1.0, 0.0)), f.x),
               mix(hash(i + float2(0.0, 1.0)), hash(i + float2(1.0, 1.0)), f.x), f.y);
}

half4 main(float2 fragCoord) {
    float2 uv = fragCoord / iResolution;
    float aspect = iResolution.x / iResolution.y;
    uv.x *= aspect;
    uv += iTilt * 0.07;
    
    // Touch bend
    float2 touchUv = float2(iTouch.x / iResolution.x * aspect, iTouch.y / iResolution.y);
    float dTouch = length(uv - touchUv);
    uv.y += exp(-dTouch * 4.5) * 0.06;
    
    half3 sky = mix(half3(0.012, 0.035, 0.08), half3(0.03, 0.085, 0.16), half(1.0 - uv.y));
    
    // Background Starlight
    float s = hash(floor(fragCoord * 0.15));
    if (s > 0.985 && uv.y > 0.22) {
        sky += half3(0.9, 0.95, 1.0) * half((s - 0.985) * 60.0);
    }
    
    // Volumetric Ionized Curtains
    half3 auroraCol = half3(0.0);
    for (int i = 0; i < 3; ++i) {
        float fi = float(i);
        float t = iTime * 0.26 + fi * 1.75;
        
        float wave = sin(uv.x * 1.75 + t) * 0.11 + sin(uv.x * 3.4 - t * 0.72) * 0.055;
        wave += noise(float2(uv.x * 2.1, t * 0.48)) * 0.09;
        
        float ribbonY = 0.42 + fi * 0.13 + wave;
        float dist = uv.y - ribbonY;
        float rays = noise(float2(uv.x * 15.0 + fi * 5.0, t * 1.25)) * 0.65 + 0.35;
        
        if (dist > 0.0 && dist < 0.4) {
            float fade = smoothstep(0.0, 0.055, dist) * smoothstep(0.4, 0.08, dist);
            float intensity = fade * rays;
            
            half3 c = (dist < 0.14) ? half3(0.12, 0.96, 0.48) :
                      (dist < 0.25) ? half3(0.16, 0.86, 0.96) :
                                      half3(0.78, 0.22, 0.88);
            auroraCol += c * half(intensity * (1.15 - fi * 0.2));
        }
    }
    
    return half4(sky + auroraCol, 1.0);
}
"""

private const val FLUID_AGSL = """
uniform float2 iResolution;
uniform float iTime;
uniform float2 iTilt;
uniform float2 iTouch;

float hash(float2 p) {
    return fract(sin(dot(p, float2(127.1, 311.7))) * 43758.5453);
}

float noise(float2 p) {
    float2 i = floor(p);
    float2 f = fract(p);
    f = f * f * (3.0 - 2.0 * f);
    return mix(mix(hash(i), hash(i + float2(1.0, 0.0)), f.x),
               mix(hash(i + float2(0.0, 1.0)), hash(i + float2(1.0, 1.0)), f.x), f.y);
}

half4 main(float2 fragCoord) {
    float2 uv = fragCoord / iResolution;
    float aspect = iResolution.x / iResolution.y;
    float2 p = float2(uv.x * aspect, uv.y) * 3.5;
    
    float t = iTime * 0.45;
    
    // Navier-Stokes operator splitting flow approximation
    for (int i = 1; i < 4; ++i) {
        float fi = float(i);
        p.x += 0.35 / fi * sin(fi * 2.8 * p.y + t + fi * 0.8) + iTilt.x * 0.2;
        p.y += 0.35 / fi * cos(fi * 2.8 * p.x - t + fi * 0.8) + iTilt.y * 0.2;
    }
    
    // Touch vortex disruption
    float2 touchP = float2(iTouch.x / iResolution.x * aspect, iTouch.y / iResolution.y) * 3.5;
    float dTouch = length(p - touchP);
    p += normalize(p - touchP + 0.001) * exp(-dTouch * 1.5) * 0.4;
    
    float dye = sin(p.x + p.y + t) * 0.5 + 0.5;
    float vorticity = noise(p * 2.0 + t);
    
    half3 col = mix(half3(0.02, 0.04, 0.1), half3(0.05, 0.65, 0.55), half(dye));
    col = mix(col, half3(0.65, 0.15, 0.85), half(clamp(vorticity * 1.2, 0.0, 1.0)));
    col += half3(1.0, 0.75, 0.2) * half(pow(dye * vorticity, 3.0) * 1.5);
    
    return half4(col, 1.0);
}
"""

// =============================================================================
// LIVE WALLPAPER RENDERER
// =============================================================================

class LiveWallpaperRenderer {

    private val rng = Random(42)

    // Cached AGSL Shaders (Android 13+ / API 33+)
    private var nebulaShader: RuntimeShader? = null
    private var blackHoleShader: RuntimeShader? = null
    private var neoTokyoShader: RuntimeShader? = null
    private var matrixShader: RuntimeShader? = null
    private var auroraShader: RuntimeShader? = null
    private var fluidShader: RuntimeShader? = null

    // Persistent Petals for Sakura Dusk
    private class Petal(
        var x: Float, var y: Float, var z: Float,
        var vx: Float, var vy: Float,
        var pitch: Float, var yaw: Float, var roll: Float,
        var pitchSpeed: Float, var rollSpeed: Float,
        var size: Float, var alpha: Float
    )
    private val petals = ArrayList<Petal>()
    private var petalsInitialized = false

    // 4D Polytope vertices for Hyperspace scene (Tesseract 16 vertices)
    private val tesseractVertices = Array(16) { i ->
        floatArrayOf(
            if ((i and 1) != 0) 1f else -1f,
            if ((i and 2) != 0) 1f else -1f,
            if ((i and 4) != 0) 1f else -1f,
            if ((i and 8) != 0) 1f else -1f
        )
    }

    private val tesseractEdges = ArrayList<Pair<Int, Int>>().apply {
        for (i in 0 until 16) {
            for (j in i + 1 until 16) {
                val diff = i xor j
                if (diff == 1 || diff == 2 || diff == 4 || diff == 8) {
                    add(Pair(i, j))
                }
            }
        }
    }

    init {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            nebulaShader = runCatching { RuntimeShader(NEBULA_AGSL) }.getOrNull()
            blackHoleShader = runCatching { RuntimeShader(BLACK_HOLE_AGSL) }.getOrNull()
            neoTokyoShader = runCatching { RuntimeShader(NEO_TOKYO_AGSL) }.getOrNull()
            matrixShader = runCatching { RuntimeShader(MATRIX_AGSL) }.getOrNull()
            auroraShader = runCatching { RuntimeShader(AURORA_AGSL) }.getOrNull()
            fluidShader = runCatching { RuntimeShader(FLUID_AGSL) }.getOrNull()
        }
    }

    private fun initPetals(w: Float, h: Float, density: Float) {
        if (petalsInitialized || w <= 0f || h <= 0f) return
        petalsInitialized = true
        val count = (80 * density).toInt().coerceIn(30, 150)
        petals.clear()
        for (i in 0 until count) {
            petals.add(
                Petal(
                    x = rng.nextFloat() * w,
                    y = rng.nextFloat() * h,
                    z = rng.nextFloat() * 0.7f + 0.3f,
                    vx = (rng.nextFloat() - 0.5f) * 35f,
                    vy = rng.nextFloat() * 65f + 40f,
                    pitch = rng.nextFloat() * 360f,
                    yaw = rng.nextFloat() * 360f,
                    roll = rng.nextFloat() * 360f,
                    pitchSpeed = (rng.nextFloat() - 0.5f) * 140f,
                    rollSpeed = (rng.nextFloat() - 0.5f) * 180f,
                    size = rng.nextFloat() * 10f + 7f,
                    alpha = rng.nextFloat() * 0.4f + 0.6f
                )
            )
        }
    }

    fun render(drawScope: DrawScope, state: WallpaperRenderState, dt: Float) {
        val w = drawScope.size.width
        val h = drawScope.size.height
        if (w <= 0f || h <= 0f) return

        initPetals(w, h, state.particleDensity)

        val speed = state.animationSpeed.coerceIn(0.2f, 3.0f)
        val t = state.timeSeconds * speed

        // Global parallax offset from launcher scroll & gyro tilt
        val px = (state.launcherOffset - 0.5f) * 90f + (state.tiltX * 50f)
        val py = state.tiltY * 45f

        // Touch position from last ripple or screen center
        val lastRipple = state.activeRipples.lastOrNull()
        val touchX = lastRipple?.x ?: (w * 0.5f)
        val touchY = lastRipple?.y ?: (h * 0.48f)

        when (state.scene) {
            WallpaperSceneType.DEEP_NEBULA -> {
                if (!renderAgsl(drawScope, nebulaShader, w, h, t, state.tiltX, state.tiltY, touchX, touchY)) {
                    renderFallbackNebula(drawScope, w, h, t, px, py, state)
                }
            }
            WallpaperSceneType.EVENT_HORIZON -> {
                if (!renderAgsl(drawScope, blackHoleShader, w, h, t, state.tiltX, state.tiltY, touchX, touchY)) {
                    renderFallbackEventHorizon(drawScope, w, h, t, px, py, state)
                }
            }
            WallpaperSceneType.NEO_TOKYO -> {
                if (!renderAgsl(drawScope, neoTokyoShader, w, h, t, state.tiltX, state.tiltY, touchX, touchY)) {
                    renderFallbackNeoTokyo(drawScope, w, h, t, px, py, state)
                }
            }
            WallpaperSceneType.MATRIX_RAIN -> {
                if (!renderAgsl(drawScope, matrixShader, w, h, t, state.tiltX, state.tiltY, touchX, touchY)) {
                    renderFallbackMatrix(drawScope, w, h, t, dt, px, py, state)
                }
            }
            WallpaperSceneType.AURORA_BOREALIS -> {
                if (!renderAgsl(drawScope, auroraShader, w, h, t, state.tiltX, state.tiltY, touchX, touchY)) {
                    renderFallbackAurora(drawScope, w, h, t, px, py, state)
                }
            }
            WallpaperSceneType.EULERIAN_FLUID -> {
                if (!renderAgsl(drawScope, fluidShader, w, h, t, state.tiltX, state.tiltY, touchX, touchY)) {
                    renderFallbackFluid(drawScope, w, h, t, px, py, state)
                }
            }
            WallpaperSceneType.SAKURA_DUSK -> {
                renderCinematicSakura(drawScope, w, h, t, dt, px, py, state)
            }
            WallpaperSceneType.HYPERSPACE_4D -> {
                renderCinematicHyperspace4D(drawScope, w, h, t, px, py, state)
            }
        }

        // Kinetic Touch Ripples overlay
        drawTouchRipples(drawScope, state)
    }

    // -------------------------------------------------------------------------
    // AGSL RENDER HELPER
    // -------------------------------------------------------------------------
    private fun renderAgsl(
        scope: DrawScope, shader: RuntimeShader?,
        w: Float, h: Float, t: Float,
        tiltX: Float, tiltY: Float,
        touchX: Float, touchY: Float
    ): Boolean {
        if (shader == null || Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return false
        return try {
            shader.setFloatUniform("iResolution", w, h)
            shader.setFloatUniform("iTime", t)
            shader.setFloatUniform("iTilt", tiltX, tiltY)
            shader.setFloatUniform("iTouch", touchX, touchY)
            scope.drawRect(brush = ShaderBrush(shader))
            true
        } catch (e: Throwable) {
            false
        }
    }

    // -------------------------------------------------------------------------
    // SAKURA DUSK (CINEMATIC 3D VECTOR OVERHAUL)
    // -------------------------------------------------------------------------
    private fun renderCinematicSakura(
        scope: DrawScope, w: Float, h: Float, t: Float, dt: Float,
        px: Float, py: Float, state: WallpaperRenderState
    ) {
        scope.drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color(0xFF13091F),
                    Color(0xFF2C1032),
                    Color(0xFF5B1B47),
                    Color(0xFFA1385B),
                    Color(0xFFE06D5E),
                    Color(0xFFFFB278)
                ),
                startY = 0f,
                endY = h
            )
        )

        val sunX = w * 0.5f + px * 0.2f
        val sunY = h * 0.54f + py * 0.2f
        val sunRad = w * 0.24f
        scope.drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFFFFF7C2),
                    Color(0xFFFF9E7A),
                    Color(0x44FF5A66),
                    Color.Transparent
                ),
                center = Offset(sunX, sunY),
                radius = sunRad * 1.9f
            ),
            center = Offset(sunX, sunY),
            radius = sunRad * 1.9f
        )
        scope.drawCircle(
            color = Color(0xFFFFFCE8),
            radius = sunRad * 0.72f,
            center = Offset(sunX, sunY)
        )

        val m1Path = Path().apply {
            moveTo(0f, h)
            lineTo(0f, h * 0.65f)
            quadraticTo(w * 0.25f, h * 0.61f, w * 0.5f, h * 0.66f)
            quadraticTo(w * 0.75f, h * 0.71f, w, h * 0.63f)
            lineTo(w, h)
            close()
        }
        scope.drawPath(m1Path, color = Color(0xFF381A3C).copy(alpha = 0.6f))

        val fujiPeakX = w * 0.5f + px * 0.32f
        val fujiPeakY = h * 0.46f + py * 0.32f
        val fujiBaseY = h * 0.76f
        val fujiPath = Path().apply {
            moveTo(0f, h)
            lineTo(0f, fujiBaseY)
            cubicTo(w * 0.25f, fujiBaseY - 15f, fujiPeakX - w * 0.22f, fujiPeakY + 60f, fujiPeakX - w * 0.08f, fujiPeakY + 22f)
            lineTo(fujiPeakX + w * 0.08f, fujiPeakY + 22f)
            cubicTo(fujiPeakX + w * 0.22f, fujiPeakY + 60f, w * 0.75f, fujiBaseY - 15f, w, fujiBaseY)
            lineTo(w, h)
            close()
        }
        scope.drawPath(fujiPath, color = Color(0xFF1A0A21))

        val toriiX = w * 0.74f + px * 0.65f
        val toriiY = h * 0.74f + py * 0.65f
        val toriiW = w * 0.19f
        val toriiH = h * 0.13f
        scope.drawRect(Color(0xFF0F0414), Offset(toriiX - toriiW * 0.54f, toriiY), Size(toriiW * 1.08f, toriiH * 0.13f))
        scope.drawRect(Color(0xFF0F0414), Offset(toriiX - toriiW * 0.44f, toriiY + toriiH * 0.22f), Size(toriiW * 0.88f, toriiH * 0.09f))
        scope.drawRect(Color(0xFF0F0414), Offset(toriiX - toriiW * 0.32f, toriiY), Size(toriiW * 0.11f, toriiH))
        scope.drawRect(Color(0xFF0F0414), Offset(toriiX + toriiW * 0.21f, toriiY), Size(toriiW * 0.11f, toriiH))

        val windX = (state.tiltX * 70f) + 25f * sin(t * 0.9f)
        val gravityY = 45f + (state.tiltY * 35f)

        petals.forEach { p ->
            p.x += (p.vx + windX) * dt
            p.y += (p.vy + gravityY) * dt
            p.pitch += p.pitchSpeed * dt
            p.roll += p.rollSpeed * dt

            if (p.x < -30f) p.x = w + 30f
            if (p.x > w + 30f) p.x = -30f
            if (p.y > h + 30f) {
                p.y = -30f
                p.x = rng.nextFloat() * w
            }

            val cosPitch = cos(p.pitch * (PI.toFloat() / 180f))
            val petalWidth = p.size * (0.35f + 0.65f * abs(cosPitch))
            val petalHeight = p.size

            scope.rotate(p.roll, Offset(p.x, p.y)) {
                val petalPath = Path().apply {
                    moveTo(p.x, p.y - petalHeight)
                    cubicTo(p.x + petalWidth, p.y - petalHeight * 0.3f, p.x + petalWidth * 0.8f, p.y + petalHeight * 0.6f, p.x, p.y + petalHeight)
                    cubicTo(p.x - petalWidth * 0.8f, p.y + petalHeight * 0.6f, p.x - petalWidth, p.y - petalHeight * 0.3f, p.x, p.y - petalHeight)
                    close()
                }
                drawPath(
                    petalPath,
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFFFFDDE6).copy(alpha = p.alpha),
                            Color(0xFFFF7A96).copy(alpha = p.alpha * 0.8f)
                        ),
                        center = Offset(p.x, p.y),
                        radius = petalHeight * 1.4f
                    )
                )
            }
        }
    }

    // -------------------------------------------------------------------------
    // 4D HYPERSPACE (GLOWING TESSERACT PROJECTION OVERHAUL)
    // -------------------------------------------------------------------------
    private fun renderCinematicHyperspace4D(
        scope: DrawScope, w: Float, h: Float, t: Float,
        px: Float, py: Float, state: WallpaperRenderState
    ) {
        scope.drawRect(Color(0xFF030611))

        val cx = w * 0.5f + px * 0.75f
        val cy = h * 0.48f + py * 0.75f

        val stars = 60
        for (i in 0 until stars) {
            val fi = i.toFloat()
            val angle = fi * (2f * PI.toFloat() / stars)
            val dist = ((fi * 23.4f + t * 90f) % (w * 0.65f))
            val sx = cx + cos(angle) * dist
            val sy = cy + sin(angle) * dist
            val alpha = (dist / (w * 0.65f)).coerceIn(0.1f, 0.75f)
            scope.drawCircle(
                color = Color(0xFF38BDF8).copy(alpha = alpha),
                radius = 1.6f,
                center = Offset(sx, sy)
            )
        }

        val thetaXW = t * 0.8f + state.tiltX * 1.8f
        val thetaYZ = t * 0.6f + state.tiltY * 1.8f

        val cosXW = cos(thetaXW); val sinXW = sin(thetaXW)
        val cosYZ = cos(thetaYZ); val sinYZ = sin(thetaYZ)

        val projectedPoints = Array(16) { Offset.Zero }
        val scale = w * 0.24f
        val cameraDist4D = 2.5f

        for (i in 0 until 16) {
            val v = tesseractVertices[i]
            val x1 = v[0] * cosXW - v[3] * sinXW
            val w1 = v[0] * sinXW + v[3] * cosXW
            val y1 = v[1] * cosYZ - v[2] * sinYZ
            val z1 = v[1] * sinYZ + v[2] * cosYZ

            val p4 = 1f / (cameraDist4D - w1)
            val x3 = x1 * p4
            val y3 = y1 * p4
            val z3 = z1 * p4

            val cameraDist3D = 3.2f
            val p3 = 1f / (cameraDist3D - z3)
            val x2 = cx + x3 * p3 * scale * 3.2f
            val y2 = cy + y3 * p3 * scale * 3.2f
            projectedPoints[i] = Offset(x2, y2)
        }

        tesseractEdges.forEach { (a, b) ->
            val ptA = projectedPoints[a]
            val ptB = projectedPoints[b]
            val isInner = (a < 8 && b < 8)
            val isOuter = (a >= 8 && b >= 8)

            val edgeColor = when {
                isInner -> Color(0xFF38BDF8)
                isOuter -> Color(0xFFE879F9)
                else -> Color(0xFF818CF8).copy(alpha = 0.65f)
            }

            scope.drawLine(
                color = edgeColor.copy(alpha = 0.3f),
                start = ptA,
                end = ptB,
                strokeWidth = 6.0f,
                cap = StrokeCap.Round
            )
            scope.drawLine(
                color = edgeColor,
                start = ptA,
                end = ptB,
                strokeWidth = if (isInner || isOuter) 2.5f else 1.6f,
                cap = StrokeCap.Round
            )
        }

        projectedPoints.forEachIndexed { idx, pt ->
            val nodeColor = if (idx < 8) Color(0xFFBAE6FD) else Color(0xFFF5D0FE)
            scope.drawCircle(color = nodeColor.copy(alpha = 0.4f), radius = 8f, center = pt)
            scope.drawCircle(color = nodeColor, radius = 4f, center = pt)
        }
    }

    // -------------------------------------------------------------------------
    // FALLBACK PROCEDURAL SCENES
    // -------------------------------------------------------------------------
    private fun renderFallbackNebula(scope: DrawScope, w: Float, h: Float, t: Float, px: Float, py: Float, state: WallpaperRenderState) {
        scope.drawRect(Color(0xFF090615))
    }
    private fun renderFallbackEventHorizon(scope: DrawScope, w: Float, h: Float, t: Float, px: Float, py: Float, state: WallpaperRenderState) {
        scope.drawRect(Color(0xFF040407))
    }
    private fun renderFallbackNeoTokyo(scope: DrawScope, w: Float, h: Float, t: Float, px: Float, py: Float, state: WallpaperRenderState) {
        scope.drawRect(Color(0xFF080B16))
    }
    private fun renderFallbackMatrix(scope: DrawScope, w: Float, h: Float, t: Float, dt: Float, px: Float, py: Float, state: WallpaperRenderState) {
        scope.drawRect(Color(0xFF020904))
    }
    private fun renderFallbackAurora(scope: DrawScope, w: Float, h: Float, t: Float, px: Float, py: Float, state: WallpaperRenderState) {
        scope.drawRect(Color(0xFF020B14))
    }
    private fun renderFallbackFluid(scope: DrawScope, w: Float, h: Float, t: Float, px: Float, py: Float, state: WallpaperRenderState) {
        scope.drawRect(Color(0xFF030712))
    }

    // -------------------------------------------------------------------------
    // KINETIC TOUCH RIPPLES
    // -------------------------------------------------------------------------
    private fun drawTouchRipples(scope: DrawScope, state: WallpaperRenderState) {
        state.activeRipples.forEach { ripple ->
            val progress = (ripple.age / ripple.maxAge).coerceIn(0f, 1f)
            val currentRad = progress * ripple.maxRadius
            val alpha = (1f - progress) * 0.75f * state.touchIntensity
            if (alpha > 0.01f) {
                scope.drawCircle(
                    color = ripple.color.copy(alpha = alpha),
                    radius = currentRad,
                    center = Offset(ripple.x, ripple.y),
                    style = Stroke(width = 3.5f * (1f - progress * 0.5f))
                )
                scope.drawCircle(
                    color = Color.White.copy(alpha = alpha * 0.5f),
                    radius = currentRad * 0.65f,
                    center = Offset(ripple.x, ripple.y),
                    style = Stroke(width = 1.8f)
                )
            }
        }
    }
}
