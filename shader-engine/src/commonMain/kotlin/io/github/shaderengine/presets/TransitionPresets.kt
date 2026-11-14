package io.github.shaderengine.presets

import org.intellij.lang.annotations.Language

/**
 * High-level categories for organizing dual-input transitions.
 */
enum class TransitionCategory(val title: String) {
    FLUIDS("Fluids"),
    ORIGAMI_3D("3D & Origami"),
    WARPS_PHYSICS("Warps"),
    RETRO_GLITCH("Retro Glitch"),
    LIGHT_PYRO("Light & Pyro"),
    CANVAS("Canvas [API 21+]")
}

/**
 * Geometric algorithms for Pure Canvas transitions.
 */
enum class CanvasTransitionType {
    CIRCULAR_IRIS,
    CLOCK_RADAR_SWEEP,
    DIAGONAL_SLATS,
    DIAMOND_GRID,
    CURTAIN_SPLIT,
    MESH_LIQUID_RIPPLE,
    MESH_SWIRL_VORTEX,
    CHROMATIC_RGB_SPLIT,
    PIXEL_BAYER_DITHER,
    HALFTONE_DISSOLVE,
    SPIRAL_NAUTILUS,
    DOOR_SWING_3D,
    CONCENTRIC_RINGS,
    POLYGON_SHATTER
}

/**
 * Production-grade Dual-Input Transition Presets (31 presets across 6 categories).
 *
 * Each AGSL shader declares:
 *   uniform shader imageA;
 *   uniform shader imageB;
 *   uniform float2 resolution;
 *   uniform float progress;
 *   uniform float time;
 *
 * Canvas presets run 100% shader-free down to Android API 21+.
 */
enum class TransitionPreset(
    val title: String,
    val category: TransitionCategory,
    val shaderCode: String,
    val isCanvas: Boolean = false,
    val canvasType: CanvasTransitionType? = null
) {
    // FLUIDS
    LIQUID_MELT("Liquid Melt", TransitionCategory.FLUIDS, LiquidMeltTransition.SHADER),
    DOOM_MELT_DRIP("Doom Melt Drip", TransitionCategory.FLUIDS, DoomMeltDripTransition.SHADER),
    WATER_DROPLET_RIPPLE("Water Droplets", TransitionCategory.FLUIDS, WaterDropletRippleTransition.SHADER),
    SLIME_VISCOUS_TEAR("Slime Viscous Tear", TransitionCategory.FLUIDS, SlimeViscousTearTransition.SHADER),
    THERMAL_HEAT_HAZE("Thermal Heat Haze", TransitionCategory.FLUIDS, ThermalHeatHazeTransition.SHADER),

    // 3D & ORIGAMI
    PAGE_CURL_3D("Page Curl 3D", TransitionCategory.ORIGAMI_3D, PageCurl3DTransition.SHADER),
    HEXAGONAL_PRISM_WIPE("Hexagonal Prism", TransitionCategory.ORIGAMI_3D, HexagonalPrismWipeTransition.SHADER),
    ORIGAMI_TRIANGLE_FOLD("Origami Fold", TransitionCategory.ORIGAMI_3D, OrigamiTriangleFoldTransition.SHADER),
    VENETIAN_BLINDS_3D("Venetian Blinds", TransitionCategory.ORIGAMI_3D, VenetianBlinds3DTransition.SHADER),
    CIRCLE_IRIS_ZOOM("Circle Iris Zoom", TransitionCategory.ORIGAMI_3D, CircleIrisZoomTransition.SHADER),
    VORONOI_FRACTURE("Voronoi Fracture", TransitionCategory.ORIGAMI_3D, VoronoiFractureTransition.SHADER),

    // WARPS & PHYSICS
    DISPLACEMENT_WAVE("Displacement Wave", TransitionCategory.WARPS_PHYSICS, DisplacementWaveTransition.SHADER),
    RADIAL_SWIRL_VORTEX("Radial Swirl Vortex", TransitionCategory.WARPS_PHYSICS, RadialSwirlVortexTransition.SHADER),
    CROSS_ZOOM_WARP("Cross Zoom Warp", TransitionCategory.WARPS_PHYSICS, CrossZoomWarpTransition.SHADER),
    BLACK_HOLE_LENS("Black Hole Lens", TransitionCategory.WARPS_PHYSICS, BlackHoleLensTransition.SHADER),
    KALEIDOSCOPE_COLLAPSE("Kaleidoscope Collapse", TransitionCategory.WARPS_PHYSICS, KaleidoscopeCollapseTransition.SHADER),
    QUANTUM_WORMHOLE("Quantum Wormhole", TransitionCategory.WARPS_PHYSICS, QuantumWormholeTransition.SHADER),

    // RETRO & GLITCH
    CYBER_GLITCH_SLICE("Cyber Glitch Slice", TransitionCategory.RETRO_GLITCH, CyberGlitchSliceTransition.SHADER),
    PIXELATE_MOSAIC("Pixelate Mosaic", TransitionCategory.RETRO_GLITCH, PixelateMosaicTransition.SHADER),
    CRT_TV_OFF_COLLAPSE("CRT TV-Off", TransitionCategory.RETRO_GLITCH, CrtTvOffCollapseTransition.SHADER),
    MATRIX_DIGITAL_RAIN("Matrix Digital Rain", TransitionCategory.RETRO_GLITCH, MatrixDigitalRainTransition.SHADER),
    RGB_HOLOGRAM_DISSOLVE("RGB Hologram Dissolve", TransitionCategory.RETRO_GLITCH, RgbHologramDissolveTransition.SHADER),
    VHS_ANALOG_DRIFT("VHS Analog Drift", TransitionCategory.RETRO_GLITCH, VhsAnalogDriftTransition.SHADER),

    // LIGHT & PYRO
    FIRE_EMBERS_BURN("Fire Embers Burn", TransitionCategory.LIGHT_PYRO, FireEmbersBurnTransition.SHADER),
    LUMA_STARRY_WIPE("Luma Starry Wipe", TransitionCategory.LIGHT_PYRO, LumaStarryWipeTransition.SHADER),
    PRISM_RAINBOW_SPLIT("Prism Rainbow Split", TransitionCategory.LIGHT_PYRO, PrismRainbowSplitTransition.SHADER),
    SUPERNOVA_SHOCKWAVE("Supernova Shockwave", TransitionCategory.LIGHT_PYRO, SupernovaShockwaveTransition.SHADER),

    // CANVAS [API 21+]
    CANVAS_CIRCULAR_IRIS("Canvas Circular Iris", TransitionCategory.CANVAS, "", isCanvas = true, canvasType = CanvasTransitionType.CIRCULAR_IRIS),
    CANVAS_CLOCK_RADAR("Canvas Clock Sweep", TransitionCategory.CANVAS, "", isCanvas = true, canvasType = CanvasTransitionType.CLOCK_RADAR_SWEEP),
    CANVAS_DIAGONAL_SLATS("Canvas Diagonal Slats", TransitionCategory.CANVAS, "", isCanvas = true, canvasType = CanvasTransitionType.DIAGONAL_SLATS),
    CANVAS_DIAMOND_GRID("Canvas Diamond Grid", TransitionCategory.CANVAS, "", isCanvas = true, canvasType = CanvasTransitionType.DIAMOND_GRID),
    CANVAS_CURTAIN_SPLIT("Canvas Curtain Split", TransitionCategory.CANVAS, "", isCanvas = true, canvasType = CanvasTransitionType.CURTAIN_SPLIT),
    CANVAS_MESH_RIPPLE("Canvas Mesh Ripple", TransitionCategory.CANVAS, "", isCanvas = true, canvasType = CanvasTransitionType.MESH_LIQUID_RIPPLE),
    CANVAS_MESH_SWIRL("Canvas Mesh Swirl", TransitionCategory.CANVAS, "", isCanvas = true, canvasType = CanvasTransitionType.MESH_SWIRL_VORTEX),
    CANVAS_RGB_SPLIT("Canvas RGB Split", TransitionCategory.CANVAS, "", isCanvas = true, canvasType = CanvasTransitionType.CHROMATIC_RGB_SPLIT),
    CANVAS_PIXEL_DITHER("Canvas Bayer Dither", TransitionCategory.CANVAS, "", isCanvas = true, canvasType = CanvasTransitionType.PIXEL_BAYER_DITHER),
    CANVAS_HALFTONE_DISSOLVE("Canvas Halftone Dot", TransitionCategory.CANVAS, "", isCanvas = true, canvasType = CanvasTransitionType.HALFTONE_DISSOLVE),
    CANVAS_SPIRAL_NAUTILUS("Canvas Spiral Sweep", TransitionCategory.CANVAS, "", isCanvas = true, canvasType = CanvasTransitionType.SPIRAL_NAUTILUS),
    CANVAS_DOOR_SWING("Canvas 3D Door Swing", TransitionCategory.CANVAS, "", isCanvas = true, canvasType = CanvasTransitionType.DOOR_SWING_3D),
    CANVAS_CONCENTRIC_RINGS("Canvas Ripple Rings", TransitionCategory.CANVAS, "", isCanvas = true, canvasType = CanvasTransitionType.CONCENTRIC_RINGS),
    CANVAS_POLYGON_SHATTER("Canvas Polygon Shatter", TransitionCategory.CANVAS, "", isCanvas = true, canvasType = CanvasTransitionType.POLYGON_SHATTER)
}

// =============================================================================
// CATEGORY 1: FLUIDS
// =============================================================================

/**
 * Procedural fractal noise burn dissolve with glowing ember edge borders and chromatic dispersion.
 */
object LiquidMeltTransition {
    @Language("AGSL")
    const val SHADER = """
        uniform shader imageA;
        uniform shader imageB;
        uniform float2 resolution;
        uniform float progress;
        uniform float time;

        float hash21(float2 p) {
            float3 p3 = fract(float3(p.xyx) * 0.1031);
            p3 += float3(dot(p3, p3.yzx + 33.33));
            return fract((p3.x + p3.y) * p3.z);
        }

        float noise(float2 p) {
            float2 i = floor(p);
            float2 f = fract(p);
            float2 u = f * f * (3.0 - 2.0 * f);
            return mix(mix(hash21(i + float2(0.0, 0.0)), hash21(i + float2(1.0, 0.0)), u.x),
                       mix(hash21(i + float2(0.0, 1.0)), hash21(i + float2(1.0, 1.0)), u.x), u.y);
        }

        float fbm(float2 p) {
            float v = 0.0;
            v += 0.5000 * noise(p); p = p * 2.02;
            v += 0.2500 * noise(p); p = p * 2.03;
            v += 0.1250 * noise(p);
            return v;
        }

        half4 main(float2 fragCoord) {
            if (progress <= 0.0) return imageA.eval(fragCoord);
            if (progress >= 1.0) return imageB.eval(fragCoord);

            float2 uv = fragCoord / resolution;

            float n = fbm(uv * 7.0 + float2(0.0, time * 0.15));
            float melt = uv.y * 0.4 + n * 0.6;
            float threshold = progress * 1.4 - 0.2;

            float diff = melt - threshold;
            float edgeWidth = 0.07;
            float edgeFactor = 1.0 - clamp(abs(diff) / edgeWidth, 0.0, 1.0);

            float2 disp = float2(sin(diff * 40.0), cos(diff * 40.0)) * (edgeFactor * 16.0);
            float2 coordA = clamp(fragCoord + disp, float2(0.0, 0.0), resolution);
            float2 coordB = clamp(fragCoord - disp, float2(0.0, 0.0), resolution);

            half4 colA = imageA.eval(coordA);
            half4 colB = imageB.eval(coordB);

            half4 colB_R = imageB.eval(clamp(coordB + float2(edgeFactor * 6.0, 0.0), float2(0.0, 0.0), resolution));
            half4 colB_B = imageB.eval(clamp(coordB - float2(edgeFactor * 6.0, 0.0), float2(0.0, 0.0), resolution));
            half4 dispersedB = half4(colB_R.r, colB.g, colB_B.b, colB.a);

            float blend = smoothstep(0.0, 0.04, diff);
            half4 base = mix(dispersedB, colA, half(blend));

            half3 emberColor = half3(1.0, 0.55, 0.12) * half(edgeFactor * edgeFactor * 2.5);
            half3 finalRGB = base.rgb + emberColor * half(base.a);
            finalRGB = min(clamp(finalRGB, half3(0.0, 0.0, 0.0), half3(1.0, 1.0, 1.0)), half3(base.a));

            return half4(finalRGB, base.a);
        }
    """
}

/**
 * Classic Doom column melt with staggered drip timing and liquid bottom rims.
 */
object DoomMeltDripTransition {
    @Language("AGSL")
    const val SHADER = """
        uniform shader imageA;
        uniform shader imageB;
        uniform float2 resolution;
        uniform float progress;
        uniform float time;

        float hash11(float p) {
            p = fract(p * 0.1031);
            p *= p + 33.33;
            p *= p + p;
            return fract(p);
        }

        half4 main(float2 fragCoord) {
            if (progress <= 0.0) return imageA.eval(fragCoord);
            if (progress >= 1.0) return imageB.eval(fragCoord);

            float2 uv = fragCoord / resolution;

            float numColumns = 60.0;
            float col = floor(uv.x * numColumns);
            float delay = hash11(col + 1.37) * 0.45;
            float dropProgress = clamp((progress - delay) / 0.55, 0.0, 1.0);
            float dropDist = dropProgress * dropProgress * resolution.y * 1.35;

            float2 coordA = fragCoord - float2(0.0, dropDist);
            half4 colB = imageB.eval(fragCoord);

            if (coordA.y < 0.0) {
                return colB;
            }

            half4 colA = imageA.eval(coordA);

            float distFromBottom = coordA.y;
            float rim = 1.0 - clamp(distFromBottom / 14.0, 0.0, 1.0);
            half3 dripRim = half3(0.08, 0.04, 0.02) * half(rim * 0.7);

            half4 blended = mix(colB, colA, half(step(0.0, coordA.y)));
            half3 finalRGB = blended.rgb - dripRim * half(blended.a);
            finalRGB = min(clamp(finalRGB, half3(0.0, 0.0, 0.0), half3(1.0, 1.0, 1.0)), half3(blended.a));

            return half4(finalRGB, blended.a);
        }
    """
}

/**
 * Expanding concentric water droplet ripples with surface displacement and caustics.
 */
object WaterDropletRippleTransition {
    @Language("AGSL")
    const val SHADER = """
        uniform shader imageA;
        uniform shader imageB;
        uniform float2 resolution;
        uniform float progress;
        uniform float time;

        const float PI = 3.14159265359;

        half4 main(float2 fragCoord) {
            if (progress <= 0.0) return imageA.eval(fragCoord);
            if (progress >= 1.0) return imageB.eval(fragCoord);

            float2 uv = fragCoord / resolution;

            float2 c1 = float2(0.35, 0.38);
            float2 c2 = float2(0.68, 0.62);
            float2 c3 = float2(0.48, 0.80);

            float d1 = length(uv - c1);
            float d2 = length(uv - c2);
            float d3 = length(uv - c3);

            float2 dir1 = d1 > 0.0001 ? (uv - c1) / d1 : float2(0.0, 0.0);
            float2 dir2 = d2 > 0.0001 ? (uv - c2) / d2 : float2(0.0, 0.0);
            float2 dir3 = d3 > 0.0001 ? (uv - c3) / d3 : float2(0.0, 0.0);

            float p1 = clamp(progress * 1.3, 0.0, 1.0);
            float p2 = clamp(progress * 1.25 - 0.1, 0.0, 1.0);
            float p3 = clamp(progress * 1.2 - 0.2, 0.0, 1.0);

            float w1 = sin(clamp(d1 - p1 * 0.9, -0.2, 0.2) * 45.0 - time * 2.0) * exp(-d1 * 3.5) * (1.0 - p1);
            float w2 = sin(clamp(d2 - p2 * 0.9, -0.2, 0.2) * 50.0 - time * 2.0) * exp(-d2 * 3.5) * (1.0 - p2);
            float w3 = sin(clamp(d3 - p3 * 0.9, -0.2, 0.2) * 40.0 - time * 2.0) * exp(-d3 * 3.5) * (1.0 - p3);

            float totalWave = (w1 + w2 + w3) * sin(progress * PI);
            float2 totalDisp = (dir1 * w1 + dir2 * w2 + dir3 * w3) * (sin(progress * PI) * 55.0);

            float2 coordA = clamp(fragCoord + totalDisp, float2(0.0, 0.0), resolution);
            float2 coordB = clamp(fragCoord - totalDisp * 0.6, float2(0.0, 0.0), resolution);

            half4 colA = imageA.eval(coordA);
            half4 colB = imageB.eval(coordB);

            float waveThreshold = progress * 1.4 - 0.2;
            float distFromCenters = min(d1, min(d2, d3));
            float mask = smoothstep(waveThreshold - 0.08, waveThreshold + 0.08, distFromCenters);

            half4 blended = mix(colB, colA, half(mask));

            float caustic = pow(clamp(totalWave * 2.5, 0.0, 1.0), 2.0);
            half3 shimmer = half3(0.5, 0.85, 1.0) * half(caustic * 0.45);
            half3 finalRGB = min(clamp(blended.rgb + shimmer * half(blended.a), half3(0.0, 0.0, 0.0), half3(1.0, 1.0, 1.0)), half3(blended.a));

            return half4(finalRGB, blended.a);
        }
    """
}

/**
 * High-viscosity organic fluid tear with cellular surface tension and neon sheen.
 */
object SlimeViscousTearTransition {
    @Language("AGSL")
    const val SHADER = """
        uniform shader imageA;
        uniform shader imageB;
        uniform float2 resolution;
        uniform float progress;
        uniform float time;

        const float PI = 3.14159265359;

        float hash21(float2 p) {
            float3 p3 = fract(float3(p.xyx) * 0.1031);
            p3 += float3(dot(p3, p3.yzx + 33.33));
            return fract((p3.x + p3.y) * p3.z);
        }

        float cellular(float2 p) {
            float2 i = floor(p);
            float2 f = fract(p);
            float minDist = 1.0;
            for (int y = -1; y <= 1; y++) {
                for (int x = -1; x <= 1; x++) {
                    float2 neighbor = float2(float(x), float(y));
                    float2 pt = float2(hash21(i + neighbor), hash21(i + neighbor + float2(5.2, 5.2)));
                    pt = float2(0.5, 0.5) + float2(0.5, 0.5) * sin(float2(time * 0.8, time * 0.8) + float2(6.2831, 6.2831) * pt);
                    float2 diff = neighbor + pt - f;
                    minDist = min(minDist, length(diff));
                }
            }
            return minDist;
        }

        half4 main(float2 fragCoord) {
            if (progress <= 0.0) return imageA.eval(fragCoord);
            if (progress >= 1.0) return imageB.eval(fragCoord);

            float2 uv = fragCoord / resolution;

            float cell = cellular(uv * 5.0);
            float tear = cell * 0.7 + uv.y * 0.3;
            float threshold = progress * 1.3 - 0.15;
            float diff = tear - threshold;

            float edgeWidth = 0.06;
            float edge = 1.0 - clamp(abs(diff) / edgeWidth, 0.0, 1.0);

            float2 stretch = float2(sin(diff * 25.0), cos(diff * 25.0)) * (edge * 20.0);
            float2 coordA = clamp(fragCoord + stretch, float2(0.0, 0.0), resolution);
            float2 coordB = clamp(fragCoord - stretch * 0.5, float2(0.0, 0.0), resolution);

            half4 colA = imageA.eval(coordA);
            half4 colB = imageB.eval(coordB);

            float blend = smoothstep(-0.02, 0.02, diff);
            half4 mixedCol = mix(colB, colA, half(blend));

            half3 slimeColor = half3(0.2, 0.95, 0.4) * half(edge * edge * 1.8);
            half3 finalRGB = min(clamp(mixedCol.rgb + slimeColor * half(mixedCol.a), half3(0.0, 0.0, 0.0), half3(1.0, 1.0, 1.0)), half3(mixedCol.a));

            return half4(finalRGB, mixedCol.a);
        }
    """
}

// =============================================================================
// CATEGORY 2: 3D & ORIGAMI
// =============================================================================

/**
 * 3D cylindrical diagonal page curl with rolling surface coordinates and shadow cast.
 */
object PageCurl3DTransition {
    @Language("AGSL")
    const val SHADER = """
        uniform shader imageA;
        uniform shader imageB;
        uniform float2 resolution;
        uniform float progress;
        uniform float time;

        const float PI = 3.14159265359;

        half4 main(float2 fragCoord) {
            if (progress <= 0.0) return imageA.eval(fragCoord);
            if (progress >= 1.0) return imageB.eval(fragCoord);

            float2 uv = fragCoord / resolution;

            float2 curlDir = float2(0.707106, 0.707106);
            float origin = (1.0 - progress) * 1.8 - 0.4;
            float d = dot(uv, curlDir) - origin;

            float radius = 0.15;
            float cylinderLen = PI * radius;

            half4 colB = imageB.eval(fragCoord);

            if (d < 0.0) {
                return imageA.eval(fragCoord);
            } else if (d < cylinderLen) {
                float theta = d / radius;
                float proj = dot(uv, curlDir);
                float2 perp = uv - curlDir * proj;

                float curledProj = origin + sin(theta) * radius;
                float2 curledUV = perp + curlDir * curledProj;
                float2 coordA = clamp(curledUV * resolution, float2(0.0, 0.0), resolution);

                half4 colA = imageA.eval(coordA);

                float ridge = pow(clamp(sin(theta), 0.0, 1.0), 3.0);
                half3 specular = half3(1.0, 1.0, 1.0) * half(ridge * 0.45);

                half3 finalRGB = min(clamp(colA.rgb + specular * half(colA.a), half3(0.0, 0.0, 0.0), half3(1.0, 1.0, 1.0)), half3(colA.a));
                return half4(finalRGB, colA.a);
            } else {
                float shadowDist = d - cylinderLen;
                float shadow = smoothstep(0.0, 0.18, shadowDist);
                half3 shadedB = colB.rgb * half(0.65 + 0.35 * shadow);
                shadedB = min(clamp(shadedB, half3(0.0, 0.0, 0.0), half3(1.0, 1.0, 1.0)), half3(colB.a));
                return half4(shadedB, colB.a);
            }
        }
    """
}

/**
 * Hexagonal prism wipe rotating individual cells around their 3D axes with specular glint.
 */
object HexagonalPrismWipeTransition {
    @Language("AGSL")
    const val SHADER = """
        uniform shader imageA;
        uniform shader imageB;
        uniform float2 resolution;
        uniform float progress;
        uniform float time;

        const float PI = 3.14159265359;

        float2 hexCoord(float2 p) {
            float2 r = float2(1.0, 1.73205);
            float2 h = r * 0.5;
            float2 a = mod(p, r) - h;
            float2 b = mod(p - h, r) - h;
            return dot(a, a) < dot(b, b) ? a : b;
        }

        half4 main(float2 fragCoord) {
            if (progress <= 0.0) return imageA.eval(fragCoord);
            if (progress >= 1.0) return imageB.eval(fragCoord);

            float2 uv = fragCoord / resolution;

            float hexScale = 14.0;
            float2 hexP = uv * hexScale;
            float2 localH = hexCoord(hexP);
            float2 cellCenter = hexP - localH;

            float distFromCorner = length(cellCenter / hexScale);
            float delay = distFromCorner * 0.35;
            float localProgress = clamp((progress - delay) / 0.65, 0.0, 1.0);

            float angle = localProgress * PI;
            float cosA = cos(angle);

            float absCos = max(abs(cosA), 0.05);
            float2 rotLocal = float2(localH.x / absCos, localH.y);
            float2 sampledUV = (cellCenter + rotLocal) / hexScale;
            float2 sampledCoord = clamp(sampledUV * resolution, float2(0.0, 0.0), resolution);

            half4 col;
            if (cosA >= 0.0) {
                col = imageA.eval(sampledCoord);
            } else {
                col = imageB.eval(sampledCoord);
            }

            float shade = absCos * 0.7 + 0.3;
            float glint = pow(1.0 - abs(cosA), 4.0);
            half3 finalRGB = col.rgb * half(shade) + half3(0.6, 0.8, 1.0) * half(glint * 0.5);
            finalRGB = min(clamp(finalRGB, half3(0.0, 0.0, 0.0), half3(1.0, 1.0, 1.0)), half3(col.a));

            return half4(finalRGB, col.a);
        }
    """
}

/**
 * Diagonal origami paper fold with crease lighting and cast shadow underneath.
 */
object OrigamiTriangleFoldTransition {
    @Language("AGSL")
    const val SHADER = """
        uniform shader imageA;
        uniform shader imageB;
        uniform float2 resolution;
        uniform float progress;
        uniform float time;

        const float PI = 3.14159265359;

        half4 main(float2 fragCoord) {
            if (progress <= 0.0) return imageA.eval(fragCoord);
            if (progress >= 1.0) return imageB.eval(fragCoord);

            float2 uv = fragCoord / resolution;

            float2 foldNormal = float2(0.707106, 0.707106);
            float foldPos = progress * 1.8 - 0.4;
            float d = dot(uv, foldNormal) - foldPos;

            half4 colB = imageB.eval(fragCoord);

            if (d < 0.0) {
                return imageA.eval(fragCoord);
            }

            float2 reflectedUV = uv - foldNormal * (2.0 * d);
            float2 reflectedCoord = reflectedUV * resolution;

            bool inBounds = reflectedUV.x >= 0.0 && reflectedUV.x <= 1.0 &&
                            reflectedUV.y >= 0.0 && reflectedUV.y <= 1.0;

            if (inBounds && d < 0.35) {
                half4 colFlap = imageA.eval(clamp(reflectedCoord, float2(0.0, 0.0), resolution));
                float foldAngle = progress * PI;
                float shade = 0.55 + 0.45 * sin(foldAngle);
                half3 litFlap = colFlap.rgb * half(shade);
                litFlap = min(clamp(litFlap, half3(0.0, 0.0, 0.0), half3(1.0, 1.0, 1.0)), half3(colFlap.a));
                return half4(litFlap, colFlap.a);
            }

            float shadow = smoothstep(0.0, 0.12, d);
            half3 shadedB = colB.rgb * half(0.7 + 0.3 * shadow);
            shadedB = min(clamp(shadedB, half3(0.0, 0.0, 0.0), half3(1.0, 1.0, 1.0)), half3(colB.a));

            return half4(shadedB, colB.a);
        }
    """
}

/**
 * 3D venetian blinds with rotating slats and depth shading.
 */
object VenetianBlinds3DTransition {
    @Language("AGSL")
    const val SHADER = """
        uniform shader imageA;
        uniform shader imageB;
        uniform float2 resolution;
        uniform float progress;
        uniform float time;

        const float PI = 3.14159265359;

        half4 main(float2 fragCoord) {
            if (progress <= 0.0) return imageA.eval(fragCoord);
            if (progress >= 1.0) return imageB.eval(fragCoord);

            float2 uv = fragCoord / resolution;

            float numSlats = 18.0;
            float slatIndex = floor(uv.y * numSlats);
            float localY = fract(uv.y * numSlats) - 0.5;

            float delay = (slatIndex / numSlats) * 0.25;
            float localProgress = clamp((progress - delay) / 0.75, 0.0, 1.0);

            float angle = localProgress * PI;
            float cosA = cos(angle);
            float absCos = max(abs(cosA), 0.06);

            float slatCenterY = (slatIndex + 0.5) / numSlats;
            float sampledY = slatCenterY + (localY / absCos) / numSlats;
            float2 sampledCoord = clamp(float2(uv.x, sampledY) * resolution, float2(0.0, 0.0), resolution);

            half4 col;
            if (cosA >= 0.0) {
                col = imageA.eval(sampledCoord);
            } else {
                col = imageB.eval(sampledCoord);
            }

            float bevel = 1.0 - abs(localY * 2.0);
            float shade = (absCos * 0.75 + 0.25) * (0.8 + 0.2 * bevel);
            half3 finalRGB = col.rgb * half(shade);
            finalRGB = min(clamp(finalRGB, half3(0.0, 0.0, 0.0), half3(1.0, 1.0, 1.0)), half3(col.a));

            return half4(finalRGB, col.a);
        }
    """
}

/**
 * Camera iris aperture zoom with beveled glass ring and refractive dispersion.
 */
object CircleIrisZoomTransition {
    @Language("AGSL")
    const val SHADER = """
        uniform shader imageA;
        uniform shader imageB;
        uniform float2 resolution;
        uniform float progress;
        uniform float time;

        const float PI = 3.14159265359;

        half4 main(float2 fragCoord) {
            if (progress <= 0.0) return imageA.eval(fragCoord);
            if (progress >= 1.0) return imageB.eval(fragCoord);

            float2 uv = fragCoord / resolution;
            float2 center = float2(0.5, 0.5);
            float2 delta = uv - center;
            float r = length(delta);
            float angle = atan(delta.y, delta.x);

            float blades = 8.0;
            float bladeMod = cos(angle * blades) * 0.025 * (1.0 - progress);
            float maxR = 0.85;
            float irisR = progress * maxR;

            float dist = r - (irisR + bladeMod);

            float ringWidth = 0.045;
            float ringFactor = 1.0 - clamp(abs(dist) / ringWidth, 0.0, 1.0);

            float2 dir = r > 0.0001 ? delta / r : float2(0.0, 0.0);
            float2 disp = dir * (ringFactor * 22.0);

            float2 coordA = clamp(fragCoord + disp, float2(0.0, 0.0), resolution);
            float2 coordB = clamp(fragCoord - disp, float2(0.0, 0.0), resolution);

            half4 colA = imageA.eval(coordA);
            half4 colB = imageB.eval(coordB);

            float mask = smoothstep(-0.01, 0.01, dist);
            half4 blended = mix(colB, colA, half(mask));

            half3 rimGlint = half3(0.85, 0.95, 1.0) * half(pow(ringFactor, 3.0) * 0.65);
            half3 finalRGB = min(clamp(blended.rgb + rimGlint * half(blended.a), half3(0.0, 0.0, 0.0), half3(1.0, 1.0, 1.0)), half3(blended.a));

            return half4(finalRGB, blended.a);
        }
    """
}

// =============================================================================
// CATEGORY 3: WARPS & PHYSICS
// =============================================================================

/**
 * Directional sinusoidal ripple pushing outgoing content to reveal incoming content.
 */
object DisplacementWaveTransition {
    @Language("AGSL")
    const val SHADER = """
        uniform shader imageA;
        uniform shader imageB;
        uniform float2 resolution;
        uniform float progress;
        uniform float time;

        const float PI = 3.14159265359;

        half4 main(float2 fragCoord) {
            if (progress <= 0.0) return imageA.eval(fragCoord);
            if (progress >= 1.0) return imageB.eval(fragCoord);

            float2 uv = fragCoord / resolution;

            float diag = (uv.x + uv.y) * 0.5;
            float waveFront = progress * 1.5 - 0.25;
            float dist = diag - waveFront;

            float wave = sin(dist * 28.0 - time * 3.0) * exp(-abs(dist) * 9.0);
            float envelope = sin(progress * PI);
            float rippleIntensity = wave * envelope * 45.0;

            float2 dir = normalize(float2(1.0, 0.75));
            float2 disp = dir * rippleIntensity;

            float2 coordA = clamp(fragCoord + disp, float2(0.0, 0.0), resolution);
            float2 coordB = clamp(fragCoord - disp * 0.7, float2(0.0, 0.0), resolution);

            float caOffset = abs(rippleIntensity) * 0.35;
            half4 colA_R = imageA.eval(clamp(coordA + float2(caOffset, 0.0), float2(0.0, 0.0), resolution));
            half4 colA_G = imageA.eval(coordA);
            half4 colA_B = imageA.eval(clamp(coordA - float2(caOffset, 0.0), float2(0.0, 0.0), resolution));
            half4 colA = half4(colA_R.r, colA_G.g, colA_B.b, colA_G.a);

            half4 colB = imageB.eval(coordB);

            float mask = smoothstep(-0.06, 0.06, dist);
            half4 blended = mix(colB, colA, half(mask));

            float crest = pow(max(0.0, wave), 3.0) * envelope;
            half3 shimmer = half3(0.5, 0.85, 1.0) * half(crest * 0.6);
            half3 finalRGB = min(clamp(blended.rgb + shimmer * half(blended.a), half3(0.0, 0.0, 0.0), half3(1.0, 1.0, 1.0)), half3(blended.a));

            return half4(finalRGB, blended.a);
        }
    """
}

/**
 * Polar coordinate swirl vortex with counter-twisting dual scenes and core flash.
 */
object RadialSwirlVortexTransition {
    @Language("AGSL")
    const val SHADER = """
        uniform shader imageA;
        uniform shader imageB;
        uniform float2 resolution;
        uniform float progress;
        uniform float time;

        const float PI = 3.14159265359;

        half4 main(float2 fragCoord) {
            if (progress <= 0.0) return imageA.eval(fragCoord);
            if (progress >= 1.0) return imageB.eval(fragCoord);

            float2 uv = fragCoord / resolution;
            float2 center = float2(0.5, 0.5);
            float2 delta = uv - center;
            float r = length(delta);
            float angle = atan(delta.y, delta.x);

            float maxRadius = 0.75;
            float swirlStrength = sin(progress * PI) * 5.5;
            float falloff = clamp((maxRadius - r) / maxRadius, 0.0, 1.0);
            float twist = falloff * falloff * swirlStrength;

            float angleA = angle + twist * (1.0 - progress);
            float angleB = angle - twist * progress;

            float2 uvA = center + float2(cos(angleA), sin(angleA)) * r;
            float2 uvB = center + float2(cos(angleB), sin(angleB)) * r;

            float2 coordA = clamp(uvA * resolution, float2(0.0, 0.0), resolution);
            float2 coordB = clamp(uvB * resolution, float2(0.0, 0.0), resolution);

            half4 colA = imageA.eval(coordA);
            half4 colB = imageB.eval(coordB);

            float vortexCore = clamp(1.0 - r / 0.25, 0.0, 1.0) * sin(progress * PI);
            half4 blended = mix(colA, colB, half(progress));

            half3 coreGlow = half3(0.4, 0.8, 1.0) * half(vortexCore * 0.45);
            half3 finalRGB = min(clamp(blended.rgb + coreGlow * half(blended.a), half3(0.0, 0.0, 0.0), half3(1.0, 1.0, 1.0)), half3(blended.a));

            return half4(finalRGB, blended.a);
        }
    """
}

/**
 * Exponential focal zoom warp with radial multi-tap motion blur and central exposure flash.
 */
object CrossZoomWarpTransition {
    @Language("AGSL")
    const val SHADER = """
        uniform shader imageA;
        uniform shader imageB;
        uniform float2 resolution;
        uniform float progress;
        uniform float time;

        const float PI = 3.14159265359;

        half4 main(float2 fragCoord) {
            if (progress <= 0.0) return imageA.eval(fragCoord);
            if (progress >= 1.0) return imageB.eval(fragCoord);

            float2 uv = fragCoord / resolution;
            float2 center = float2(0.5, 0.5);
            float2 delta = uv - center;

            float zoomA = 1.0 + progress * 4.0;
            float zoomB = 0.2 + progress * 0.8;

            float blurAmount = sin(progress * PI) * 0.12;

            half4 accA = half4(0.0, 0.0, 0.0, 0.0);
            half4 accB = half4(0.0, 0.0, 0.0, 0.0);

            for (int i = 0; i < 5; i++) {
                float stepOffset = (float(i) / 4.0 - 0.5) * blurAmount;

                float2 sampleUvA = center + delta * (zoomA + stepOffset);
                float2 sampleUvB = center + delta * (zoomB + stepOffset);

                float2 coordA = clamp(sampleUvA * resolution, float2(0.0, 0.0), resolution);
                float2 coordB = clamp(sampleUvB * resolution, float2(0.0, 0.0), resolution);

                accA += imageA.eval(coordA);
                accB += imageB.eval(coordB);
            }

            half4 colA = accA * half(0.2);
            half4 colB = accB * half(0.2);

            half4 blended = mix(colA, colB, half(smoothstep(0.4, 0.6, progress)));

            float flash = sin(progress * PI) * 0.3;
            half3 finalRGB = min(clamp(blended.rgb + half3(flash, flash, flash) * half(blended.a), half3(0.0, 0.0, 0.0), half3(1.0, 1.0, 1.0)), half3(blended.a));

            return half4(finalRGB, blended.a);
        }
    """
}

/**
 * Einstein gravitational lensing around a central singularity with glowing accretion disk.
 */
object BlackHoleLensTransition {
    @Language("AGSL")
    const val SHADER = """
        uniform shader imageA;
        uniform shader imageB;
        uniform float2 resolution;
        uniform float progress;
        uniform float time;

        const float PI = 3.14159265359;

        half4 main(float2 fragCoord) {
            if (progress <= 0.0) return imageA.eval(fragCoord);
            if (progress >= 1.0) return imageB.eval(fragCoord);

            float2 uv = fragCoord / resolution;
            float2 center = float2(0.5, 0.5);
            float2 delta = uv - center;
            float r = length(delta);

            float rs = progress * 0.45;
            float re = rs * 1.5;

            float2 dir = r > 0.0001 ? delta / r : float2(0.0, 0.0);

            float deflection = (rs * 0.06) / (r * r + 0.004);
            float2 warpedUV = uv + dir * deflection;

            float2 coordA = clamp(warpedUV * resolution, float2(0.0, 0.0), resolution);
            float2 coordB = clamp(fragCoord, float2(0.0, 0.0), resolution);

            half4 colA = imageA.eval(coordA);
            half4 colB = imageB.eval(coordB);

            float horizonMask = smoothstep(rs - 0.04, rs + 0.04, r);
            half4 blended = mix(colB, colA, half(horizonMask));

            float ringDist = abs(r - re);
            float accretion = pow(clamp(1.0 - ringDist / 0.08, 0.0, 1.0), 3.0) * sin(progress * PI);
            half3 ringGlow = half3(1.0, 0.65, 0.25) * half(accretion * 1.2);

            half3 finalRGB = min(clamp(blended.rgb + ringGlow * half(blended.a), half3(0.0, 0.0, 0.0), half3(1.0, 1.0, 1.0)), half3(blended.a));
            return half4(finalRGB, blended.a);
        }
    """
}

/**
 * 8-fold polar symmetry kaleidoscope collapse with crystal facet edge highlights.
 */
object KaleidoscopeCollapseTransition {
    @Language("AGSL")
    const val SHADER = """
        uniform shader imageA;
        uniform shader imageB;
        uniform float2 resolution;
        uniform float progress;
        uniform float time;

        const float PI = 3.14159265359;

        half4 main(float2 fragCoord) {
            if (progress <= 0.0) return imageA.eval(fragCoord);
            if (progress >= 1.0) return imageB.eval(fragCoord);

            float2 uv = fragCoord / resolution;
            float2 center = float2(0.5, 0.5);
            float2 delta = uv - center;
            float r = length(delta);
            float angle = atan(delta.y, delta.x);

            float segments = 8.0;
            float segAngle = PI * 2.0 / segments;
            float env = sin(progress * PI);

            float modAngle = mod(angle + time * 0.2, segAngle);
            float foldAngle = abs(modAngle - segAngle * 0.5);

            float finalAngle = mix(angle, foldAngle, env);
            float2 kaleidoUV = center + float2(cos(finalAngle), sin(finalAngle)) * r;

            float2 coordA = clamp(kaleidoUV * resolution, float2(0.0, 0.0), resolution);
            float2 coordB = clamp(fragCoord, float2(0.0, 0.0), resolution);

            half4 colA = imageA.eval(coordA);
            half4 colB = imageB.eval(coordB);

            float facetEdge = sin(finalAngle * segments * 2.0);
            float facetHighlight = pow(clamp(facetEdge, 0.0, 1.0), 4.0) * env;

            half4 blended = mix(colA, colB, half(progress));
            half3 prismGlint = half3(0.7, 0.9, 1.0) * half(facetHighlight * 0.4);
            half3 finalRGB = min(clamp(blended.rgb + prismGlint * half(blended.a), half3(0.0, 0.0, 0.0), half3(1.0, 1.0, 1.0)), half3(blended.a));

            return half4(finalRGB, blended.a);
        }
    """
}

// =============================================================================
// CATEGORY 4: RETRO & GLITCH
// =============================================================================

/**
 * Horizontal digital scanline slice tearing with RGB channel split.
 */
object CyberGlitchSliceTransition {
    @Language("AGSL")
    const val SHADER = """
        uniform shader imageA;
        uniform shader imageB;
        uniform float2 resolution;
        uniform float progress;
        uniform float time;

        const float PI = 3.14159265359;

        float hash21(float2 p) {
            float3 p3 = fract(float3(p.xyx) * 0.1031);
            p3 += float3(dot(p3, p3.yzx + 33.33));
            return fract((p3.x + p3.y) * p3.z);
        }

        half4 main(float2 fragCoord) {
            if (progress <= 0.0) return imageA.eval(fragCoord);
            if (progress >= 1.0) return imageB.eval(fragCoord);

            float2 uv = fragCoord / resolution;

            float bands = 32.0;
            float bandId = floor(uv.y * bands);
            float sliceTime = floor(time * 12.0) + floor(progress * 16.0);
            float seed = hash21(float2(bandId, sliceTime));

            float env = sin(progress * PI);
            float isGlitched = step(0.4, seed);
            float sliceOffset = (seed - 0.5) * 85.0 * env * isGlitched;

            float jitter = (fract(sin(uv.y * 300.0 + time * 20.0) * 43758.5453) - 0.5) * 6.0 * env;
            float totalShift = sliceOffset + jitter;

            float rgbSplit = totalShift * 0.65 + env * 12.0 * (seed - 0.5);

            float2 coordR = clamp(fragCoord + float2(totalShift + rgbSplit, 0.0), float2(0.0, 0.0), resolution);
            float2 coordG = clamp(fragCoord + float2(totalShift, 0.0), float2(0.0, 0.0), resolution);
            float2 coordB = clamp(fragCoord + float2(totalShift - rgbSplit, 0.0), float2(0.0, 0.0), resolution);

            float wipeX = uv.x + (seed - 0.5) * 0.35 * env;
            float wipeThreshold = progress;

            half rA = imageA.eval(coordR).r;
            half gA = imageA.eval(coordG).g;
            half bA = imageA.eval(coordB).b;
            half aA = imageA.eval(coordG).a;
            half4 glitchA = half4(rA, gA, bA, aA);

            half rB = imageB.eval(coordR).r;
            half gB = imageB.eval(coordG).g;
            half bB = imageB.eval(coordB).b;
            half aB = imageB.eval(coordB).a;
            half4 glitchB = half4(rB, gB, bB, aB);

            float blend = smoothstep(wipeThreshold - 0.05, wipeThreshold + 0.05, wipeX);
            half4 result = mix(glitchB, glitchA, half(blend));

            float scanline = sin(uv.y * resolution.y * 1.5) * 0.5 + 0.5;
            result.rgb *= half(1.0 - scanline * 0.12 * env);

            if (isGlitched > 0.5 && env > 0.2) {
                half3 cyberGlow = mix(half3(0.0, 0.9, 1.0), half3(1.0, 0.0, 0.6), half(seed));
                result.rgb += cyberGlow * half(env * 0.18);
            }

            result.rgb = min(clamp(result.rgb, half3(0.0, 0.0, 0.0), half3(1.0, 1.0, 1.0)), half3(result.a));
            return result;
        }
    """
}

/**
 * 8-bit dynamic pixel mosaic downsampling with palette quantization.
 */
object PixelateMosaicTransition {
    @Language("AGSL")
    const val SHADER = """
        uniform shader imageA;
        uniform shader imageB;
        uniform float2 resolution;
        uniform float progress;
        uniform float time;

        half4 main(float2 fragCoord) {
            if (progress <= 0.0) return imageA.eval(fragCoord);
            if (progress >= 1.0) return imageB.eval(fragCoord);

            float peak = 1.0 - abs(progress - 0.5) * 2.0;
            float tileSize = 1.0 + peak * 32.0;

            float2 blockCoord = floor(fragCoord / tileSize) * tileSize + float2(tileSize * 0.5, tileSize * 0.5);
            blockCoord = clamp(blockCoord, float2(0.0, 0.0), resolution);

            half4 colA = imageA.eval(blockCoord);
            half4 colB = imageB.eval(blockCoord);

            half4 blended = mix(colA, colB, half(step(0.5, progress)));
            if (peak > 0.4) {
                blended.rgb = floor(blended.rgb * half(8.0)) / half(8.0);
            }

            float2 cellUv = fract(fragCoord / tileSize);
            float border = step(0.06, cellUv.x) * step(0.06, cellUv.y);
            blended.rgb *= half(0.85 + 0.15 * border);

            blended.rgb = min(clamp(blended.rgb, half3(0.0, 0.0, 0.0), half3(1.0, 1.0, 1.0)), half3(blended.a));
            return blended;
        }
    """
}

/**
 * Cathode ray tube power-off beam collapse into a high-intensity horizontal streak and bloom.
 */
object CrtTvOffCollapseTransition {
    @Language("AGSL")
    const val SHADER = """
        uniform shader imageA;
        uniform shader imageB;
        uniform float2 resolution;
        uniform float progress;
        uniform float time;

        const float PI = 3.14159265359;

        half4 main(float2 fragCoord) {
            if (progress <= 0.0) return imageA.eval(fragCoord);
            if (progress >= 1.0) return imageB.eval(fragCoord);

            float2 uv = fragCoord / resolution;

            float p1 = clamp(progress / 0.65, 0.0, 1.0);
            float p2 = clamp((progress - 0.65) / 0.30, 0.0, 1.0);
            float p3 = clamp((progress - 0.90) / 0.10, 0.0, 1.0);

            float scaleY = max(0.003, 1.0 - p1);
            float scaleX = max(0.003, 1.0 - p2);

            float2 center = float2(0.5, 0.5);
            float2 delta = uv - center;

            float2 crtUV = center + float2(delta.x / scaleX, delta.y / scaleY);
            float2 coordA = clamp(crtUV * resolution, float2(0.0, 0.0), resolution);

            half4 colA = imageA.eval(coordA);
            half4 colB = imageB.eval(fragCoord);

            float intensity = 1.0 + (p1 + p2) * 4.0;
            half3 beamRGB = colA.rgb * half(intensity);

            bool insideBeam = abs(delta.x) <= (scaleX * 0.5) && abs(delta.y) <= (scaleY * 0.5);
            half3 resultRGB = insideBeam ? beamRGB : half3(0.0, 0.0, 0.0);

            float glow = exp(-abs(delta.y) * (40.0 * scaleY + 2.0)) * (1.0 - p3);
            resultRGB += half3(0.7, 0.9, 1.0) * half(glow * 0.6);

            half3 finalRGB = mix(resultRGB, colB.rgb, half(p3));
            finalRGB = min(clamp(finalRGB, half3(0.0, 0.0, 0.0), half3(1.0, 1.0, 1.0)), half3(colB.a));

            return half4(finalRGB, colB.a);
        }
    """
}

/**
 * Matrix digital rain cascade with phosphor glyph trails and streaming columns.
 */
object MatrixDigitalRainTransition {
    @Language("AGSL")
    const val SHADER = """
        uniform shader imageA;
        uniform shader imageB;
        uniform float2 resolution;
        uniform float progress;
        uniform float time;

        float hash11(float p) {
            p = fract(p * 0.1031);
            p *= p + 33.33;
            p *= p + p;
            return fract(p);
        }

        half4 main(float2 fragCoord) {
            if (progress <= 0.0) return imageA.eval(fragCoord);
            if (progress >= 1.0) return imageB.eval(fragCoord);

            float2 uv = fragCoord / resolution;

            float numColumns = 45.0;
            float col = floor(uv.x * numColumns);
            float speed = 0.8 + hash11(col * 3.17) * 0.6;
            float dropPos = progress * 1.5 * speed - hash11(col * 7.41) * 0.3;

            float dist = uv.y - dropPos;

            half4 colA = imageA.eval(fragCoord);
            half4 colB = imageB.eval(fragCoord);

            float trailLen = 0.35;
            float trail = clamp(-dist / trailLen, 0.0, 1.0);

            float row = floor(uv.y * 60.0);
            float glyphNoise = hash11(col * 9.1 + row * 13.7 + floor(time * 15.0));
            float glyph = step(0.4, glyphNoise);

            half3 matrixGreen = half3(0.1, 1.0, 0.4) * half(trail * glyph * 1.6);
            half3 headWhite = half3(1.0, 1.0, 1.0) * half(smoothstep(0.04, 0.0, abs(dist)) * 2.0);

            float mask = step(0.0, -dist);
            half4 blended = mix(colA, colB, half(mask));

            half3 finalRGB = blended.rgb + (matrixGreen + headWhite) * half(blended.a);
            finalRGB = min(clamp(finalRGB, half3(0.0, 0.0, 0.0), half3(1.0, 1.0, 1.0)), half3(blended.a));

            return half4(finalRGB, blended.a);
        }
    """
}

/**
 * Holographic transmission interference with decoupled RGB phase distortion and cyan edge glow.
 */
object RgbHologramDissolveTransition {
    @Language("AGSL")
    const val SHADER = """
        uniform shader imageA;
        uniform shader imageB;
        uniform float2 resolution;
        uniform float progress;
        uniform float time;

        const float PI = 3.14159265359;

        float hash21(float2 p) {
            float3 p3 = fract(float3(p.xyx) * 0.1031);
            p3 += float3(dot(p3, p3.yzx + 33.33));
            return fract((p3.x + p3.y) * p3.z);
        }

        half4 main(float2 fragCoord) {
            if (progress <= 0.0) return imageA.eval(fragCoord);
            if (progress >= 1.0) return imageB.eval(fragCoord);

            float2 uv = fragCoord / resolution;
            float env = sin(progress * PI);

            float fringe = sin(uv.y * 180.0 + time * 12.0) * env * 25.0;
            float jitter = (hash21(float2(floor(uv.y * 40.0), floor(time * 8.0))) - 0.5) * env * 30.0;

            float2 dispR = float2(fringe + jitter, 0.0);
            float2 dispG = float2(-fringe * 0.5, 0.0);
            float2 dispB = float2(-fringe - jitter, 0.0);

            float2 coordR = clamp(fragCoord + dispR, float2(0.0, 0.0), resolution);
            float2 coordG = clamp(fragCoord + dispG, float2(0.0, 0.0), resolution);
            float2 coordB = clamp(fragCoord + dispB, float2(0.0, 0.0), resolution);

            half rA = imageA.eval(coordR).r;
            half gA = imageA.eval(coordG).g;
            half bA = imageA.eval(coordB).b;
            half aA = imageA.eval(coordG).a;
            half4 holoA = half4(rA, gA, bA, aA);

            half rB = imageB.eval(coordR).r;
            half gB = imageB.eval(coordG).g;
            half bB = imageB.eval(coordB).b;
            half aB = imageB.eval(coordG).a;
            half4 holoB = half4(rB, gB, bB, aB);

            float scanline = sin(uv.y * resolution.y * 2.0) * 0.5 + 0.5;
            half4 blended = mix(holoA, holoB, half(progress));
            blended.rgb *= half(0.85 + 0.15 * scanline);

            half3 cyanGlow = half3(0.0, 0.85, 1.0) * half(env * abs(fringe) * 0.04);
            half3 finalRGB = min(clamp(blended.rgb + cyanGlow * half(blended.a), half3(0.0, 0.0, 0.0), half3(1.0, 1.0, 1.0)), half3(blended.a));

            return half4(finalRGB, blended.a);
        }
    """
}

// =============================================================================
// CATEGORY 5: LIGHT & PYRO
// =============================================================================

/**
 * Turbulent flame front eating through scene with heat haze refraction and rising embers.
 */
object FireEmbersBurnTransition {
    @Language("AGSL")
    const val SHADER = """
        uniform shader imageA;
        uniform shader imageB;
        uniform float2 resolution;
        uniform float progress;
        uniform float time;

        float hash21(float2 p) {
            float3 p3 = fract(float3(p.xyx) * 0.1031);
            p3 += float3(dot(p3, p3.yzx + 33.33));
            return fract((p3.x + p3.y) * p3.z);
        }

        float noise(float2 p) {
            float2 i = floor(p);
            float2 f = fract(p);
            float2 u = f * f * (3.0 - 2.0 * f);
            return mix(mix(hash21(i + float2(0.0, 0.0)), hash21(i + float2(1.0, 0.0)), u.x),
                       mix(hash21(i + float2(0.0, 1.0)), hash21(i + float2(1.0, 1.0)), u.x), u.y);
        }

        float fbm(float2 p) {
            float v = 0.0;
            v += 0.5000 * noise(p); p = p * 2.01;
            v += 0.2500 * noise(p); p = p * 2.02;
            v += 0.1250 * noise(p);
            return v;
        }

        half4 main(float2 fragCoord) {
            if (progress <= 0.0) return imageA.eval(fragCoord);
            if (progress >= 1.0) return imageB.eval(fragCoord);

            float2 uv = fragCoord / resolution;

            float n = fbm(uv * 5.0 - float2(0.0, time * 1.5));
            float burnThreshold = progress * 1.4 - 0.2;
            float burnDist = (1.0 - uv.y) * 0.45 + n * 0.55 - burnThreshold;

            float flameProximity = 1.0 - clamp(abs(burnDist) / 0.12, 0.0, 1.0);
            float2 heatHaze = float2(sin(uv.y * 30.0 + time * 5.0), cos(uv.x * 30.0 + time * 5.0)) * (flameProximity * 8.0);

            float2 coordA = clamp(fragCoord + heatHaze, float2(0.0, 0.0), resolution);
            float2 coordB = clamp(fragCoord - heatHaze * 0.5, float2(0.0, 0.0), resolution);

            half4 colA = imageA.eval(coordA);
            half4 colB = imageB.eval(coordB);

            half3 fireCore = half3(1.0, 1.0, 0.8);
            half3 fireMid  = half3(1.0, 0.5, 0.0);
            half3 fireEdge = half3(0.8, 0.05, 0.0);

            float flameGrad = clamp((0.08 - abs(burnDist)) / 0.08, 0.0, 1.0);
            half3 flameColor = mix(fireEdge, fireMid, half(flameGrad));
            flameColor = mix(flameColor, fireCore, half(pow(flameGrad, 3.0)));

            float sparkNoise = hash21(floor(uv * 60.0) + float2(0.0, floor(time * 20.0)));
            float isSpark = step(0.96, sparkNoise) * flameProximity;
            half3 sparkColor = half3(1.0, 0.9, 0.5) * half(isSpark * 2.0);

            float mask = smoothstep(-0.02, 0.02, burnDist);
            half4 blended = mix(colB, colA, half(mask));

            half3 finalRGB = blended.rgb + (flameColor * half(flameGrad * 1.8) + sparkColor) * half(blended.a);
            finalRGB = min(clamp(finalRGB, half3(0.0, 0.0, 0.0), half3(1.0, 1.0, 1.0)), half3(blended.a));

            return half4(finalRGB, blended.a);
        }
    """
}

/**
 * Luminance-driven threshold dissolve with glittering starburst glares.
 */
object LumaStarryWipeTransition {
    @Language("AGSL")
    const val SHADER = """
        uniform shader imageA;
        uniform shader imageB;
        uniform float2 resolution;
        uniform float progress;
        uniform float time;

        const float PI = 3.14159265359;

        float hash21(float2 p) {
            float3 p3 = fract(float3(p.xyx) * 0.1031);
            p3 += float3(dot(p3, p3.yzx + 33.33));
            return fract((p3.x + p3.y) * p3.z);
        }

        half4 main(float2 fragCoord) {
            if (progress <= 0.0) return imageA.eval(fragCoord);
            if (progress >= 1.0) return imageB.eval(fragCoord);

            float2 uv = fragCoord / resolution;

            half4 colA = imageA.eval(fragCoord);
            half4 colB = imageB.eval(fragCoord);

            float lumaA = dot(colA.rgb, half3(0.299, 0.587, 0.114));
            float wipeLuma = uv.x * 0.3 + lumaA * 0.7;
            float threshold = progress * 1.3 - 0.15;
            float diff = wipeLuma - threshold;

            float edge = 1.0 - clamp(abs(diff) / 0.08, 0.0, 1.0);

            float2 cell = floor(fragCoord / 32.0);
            float2 cellLocal = fract(fragCoord / 32.0) - float2(0.5, 0.5);
            float starSeed = hash21(cell);
            float hasStar = step(0.65, starSeed) * edge;

            float spike = max(0.0, 1.0 - abs(cellLocal.x) * 12.0) * max(0.0, 1.0 - abs(cellLocal.y) * 2.0)
                        + max(0.0, 1.0 - abs(cellLocal.y) * 12.0) * max(0.0, 1.0 - abs(cellLocal.x) * 2.0);
            float twinkle = sin(time * 8.0 + starSeed * 6.28) * 0.5 + 0.5;
            half3 starGlow = half3(0.9, 0.95, 1.0) * half(spike * hasStar * twinkle * 2.2);

            float mask = smoothstep(-0.02, 0.02, diff);
            half4 blended = mix(colB, colA, half(mask));

            half3 finalRGB = min(clamp(blended.rgb + starGlow * half(blended.a), half3(0.0, 0.0, 0.0), half3(1.0, 1.0, 1.0)), half3(blended.a));
            return half4(finalRGB, blended.a);
        }
    """
}

/**
 * Optical prism chromatic dispersion separating wavelengths with rainbow sweep crest.
 */
object PrismRainbowSplitTransition {
    @Language("AGSL")
    const val SHADER = """
        uniform shader imageA;
        uniform shader imageB;
        uniform float2 resolution;
        uniform float progress;
        uniform float time;

        const float PI = 3.14159265359;

        half4 main(float2 fragCoord) {
            if (progress <= 0.0) return imageA.eval(fragCoord);
            if (progress >= 1.0) return imageB.eval(fragCoord);

            float2 uv = fragCoord / resolution;

            float env = sin(progress * PI);
            float maxDisp = env * 45.0;
            float2 prismDir = float2(0.866025, 0.5);

            float2 offR = prismDir * maxDisp;
            float2 offG = float2(0.0, 0.0);
            float2 offB = -prismDir * maxDisp;

            float2 coordR = clamp(fragCoord + offR, float2(0.0, 0.0), resolution);
            float2 coordG = clamp(fragCoord + offG, float2(0.0, 0.0), resolution);
            float2 coordB = clamp(fragCoord + offB, float2(0.0, 0.0), resolution);

            half4 colA_R = imageA.eval(coordR);
            half4 colA_G = imageA.eval(coordG);
            half4 colA_B = imageA.eval(coordB);
            half4 prismA = half4(colA_R.r, colA_G.g, colA_B.b, colA_G.a);

            half4 colB_R = imageB.eval(coordR);
            half4 colB_G = imageB.eval(coordG);
            half4 colB_B = imageB.eval(coordB);
            half4 prismB = half4(colB_R.r, colB_G.g, colB_B.b, colB_G.a);

            float sweep = (uv.x * 0.8 + uv.y * 0.6) * 0.5;
            float threshold = progress * 1.4 - 0.2;
            float mask = smoothstep(threshold - 0.1, threshold + 0.1, sweep);

            half4 blended = mix(prismB, prismA, half(mask));

            float crest = 1.0 - clamp(abs(sweep - threshold) / 0.08, 0.0, 1.0);
            half3 rainbow = half3(
                sin(crest * 3.14 + 0.0) * 0.5 + 0.5,
                sin(crest * 3.14 + 2.09) * 0.5 + 0.5,
                sin(crest * 3.14 + 4.18) * 0.5 + 0.5
            ) * half(crest * env * 0.6);

            half3 finalRGB = min(clamp(blended.rgb + rainbow * half(blended.a), half3(0.0, 0.0, 0.0), half3(1.0, 1.0, 1.0)), half3(blended.a));
            return half4(finalRGB, blended.a);
        }
    """
}

object VoronoiFractureTransition {
    @Language("AGSL")
    const val SHADER = """
        uniform shader imageA;
        uniform shader imageB;
        uniform float2 resolution;
        uniform float progress;
        uniform float time;

        float2 hash22(float2 p) {
            float3 p3 = fract(float3(p.xyx) * float3(0.1031, 0.1030, 0.0973));
            p3 += dot(p3, p3.yzx + 33.33);
            return fract((p3.xx + p3.yz) * p3.zy);
        }

        half4 main(float2 fragCoord) {
            if (progress <= 0.0) return imageA.eval(fragCoord);
            if (progress >= 1.0) return imageB.eval(fragCoord);

            float2 uv = fragCoord / resolution;
            float scale = 6.0;
            float2 st = uv * scale;
            float2 i_st = floor(st);
            float2 f_st = fract(st);

            float m_dist = 10.0;
            float2 m_point = float2(0.0);
            float2 cell_id = float2(0.0);

            for (int y = -1; y <= 1; y++) {
                for (int x = -1; x <= 1; x++) {
                    float2 neighbor = float2(float(x), float(y));
                    float2 pt = hash22(i_st + neighbor);
                    float2 diff = neighbor + pt - f_st;
                    float dist = length(diff);
                    if (dist < m_dist) {
                        m_dist = dist;
                        m_point = pt;
                        cell_id = i_st + neighbor;
                    }
                }
            }

            float cellRandom = hash22(cell_id).x;
            float falloff = smoothstep(0.0, 1.0, progress * 1.5 - cellRandom * 0.5);

            // Shard 3D tumble displacement
            float angle = falloff * (cellRandom - 0.5) * 4.0;
            float2 dir = normalize(cell_id - float2(scale * 0.5) + float2(0.001));
            float2 offset = dir * (falloff * falloff * 150.0);

            float s = sin(angle);
            float c = cos(angle);
            float2 localCoord = fragCoord - (cell_id / scale) * resolution;
            float2 rotatedLocal = float2(localCoord.x * c - localCoord.y * s, localCoord.x * s + localCoord.y * c);
            float2 sampleA = (cell_id / scale) * resolution + rotatedLocal + offset;

            half4 colA = imageA.eval(clamp(sampleA, float2(0.0), resolution));
            half4 colB = imageB.eval(fragCoord);

            float alphaA = clamp(1.0 - falloff * 1.2, 0.0, 1.0);
            return mix(colB, colA, half(alphaA));
        }
    """
}

object SupernovaShockwaveTransition {
    @Language("AGSL")
    const val SHADER = """
        uniform shader imageA;
        uniform shader imageB;
        uniform float2 resolution;
        uniform float progress;
        uniform float time;

        half4 main(float2 fragCoord) {
            if (progress <= 0.0) return imageA.eval(fragCoord);
            if (progress >= 1.0) return imageB.eval(fragCoord);

            float2 uv = fragCoord / resolution;
            float2 center = float2(0.5, 0.5);
            float aspect = resolution.x / resolution.y;
            float2 delta = (uv - center) * float2(aspect, 1.0);
            float dist = length(delta);
            float maxDist = length(float2(aspect, 1.0) * 0.5) * 1.25;

            float waveRadius = progress * maxDist;
            float waveWidth = 0.15;
            float waveDiff = dist - waveRadius;

            // Chromatic flare shockwave
            float shock = 1.0 - clamp(abs(waveDiff) / waveWidth, 0.0, 1.0);
            shock = pow(shock, 2.0);

            float2 dir = normalize(delta + float2(0.0001));
            float2 disp = dir * (shock * 30.0);

            half4 colA = imageA.eval(clamp(fragCoord + disp, float2(0.0), resolution));
            half4 colB = imageB.eval(clamp(fragCoord - disp * 0.5, float2(0.0), resolution));

            half3 flareColor = half3(1.0, 0.75, 0.4) * half(shock * 2.2);

            float mask = smoothstep(waveRadius - waveWidth * 0.5, waveRadius + waveWidth * 0.5, dist);
            half4 finalCol = mix(colB, colA, half(mask));
            finalCol.rgb += flareColor * finalCol.a;

            return clamp(finalCol, half4(0.0), half4(1.0));
        }
    """
}

object QuantumWormholeTransition {
    @Language("AGSL")
    const val SHADER = """
        uniform shader imageA;
        uniform shader imageB;
        uniform float2 resolution;
        uniform float progress;
        uniform float time;

        half4 main(float2 fragCoord) {
            if (progress <= 0.0) return imageA.eval(fragCoord);
            if (progress >= 1.0) return imageB.eval(fragCoord);

            float2 uv = fragCoord / resolution;
            float2 center = float2(0.5, 0.5);
            float2 delta = uv - center;
            float r = length(delta);
            float theta = atan(delta.y, delta.x);

            // Relativistic gravitational frame-dragging
            float warpFactor = progress * 6.0;
            float twistedTheta = theta + (warpFactor / max(r * 2.0 + 0.1, 0.2));
            float contractedR = r * (1.0 + progress * 2.5);

            float2 warpedUV = center + float2(cos(twistedTheta), sin(twistedTheta)) * contractedR;
            float2 sampleA = clamp(warpedUV * resolution, float2(0.0), resolution);

            float blueShift = smoothstep(0.4, 0.0, r) * progress;
            half4 colA = imageA.eval(sampleA);
            colA.rgb += half3(0.1, 0.4, 1.0) * half(blueShift);

            float singularityRadius = progress * 0.85;
            float coreMask = smoothstep(singularityRadius - 0.08, singularityRadius + 0.08, r);

            half4 colB = imageB.eval(fragCoord);
            return mix(colB, colA, half(coreMask));
        }
    """
}

object ThermalHeatHazeTransition {
    @Language("AGSL")
    const val SHADER = """
        uniform shader imageA;
        uniform shader imageB;
        uniform float2 resolution;
        uniform float progress;
        uniform float time;

        const float PI = 3.14159265359;

        half4 main(float2 fragCoord) {
            if (progress <= 0.0) return imageA.eval(fragCoord);
            if (progress >= 1.0) return imageB.eval(fragCoord);

            float2 uv = fragCoord / resolution;

            float wave1 = sin(uv.x * 24.0 + time * 6.0 + uv.y * 12.0);
            float wave2 = cos(uv.x * 48.0 - time * 9.0 + uv.y * 20.0);
            float heatDisplacement = (wave1 + wave2 * 0.5) * sin(progress * PI) * 18.0;

            float2 displacedCoordA = clamp(fragCoord + float2(heatDisplacement * 0.5, heatDisplacement), float2(0.0), resolution);
            float2 displacedCoordB = clamp(fragCoord - float2(heatDisplacement * 0.3, heatDisplacement * 0.6), float2(0.0), resolution);

            half4 colA = imageA.eval(displacedCoordA);
            half4 colB = imageB.eval(displacedCoordB);

            float meltLine = (1.0 - uv.y) * 0.7 + (wave1 * 0.15);
            float threshold = progress * 1.3 - 0.15;
            float blendMask = smoothstep(threshold - 0.15, threshold + 0.15, meltLine);

            return mix(colB, colA, half(blendMask));
        }
    """
}

object VhsAnalogDriftTransition {
    @Language("AGSL")
    const val SHADER = """
        uniform shader imageA;
        uniform shader imageB;
        uniform float2 resolution;
        uniform float progress;
        uniform float time;

        float hash11(float p) {
            p = fract(p * 0.1031);
            p *= p + 33.33;
            p *= p + p;
            return fract(p);
        }

        half4 main(float2 fragCoord) {
            if (progress <= 0.0) return imageA.eval(fragCoord);
            if (progress >= 1.0) return imageB.eval(fragCoord);

            float2 uv = fragCoord / resolution;

            float bandY = fract(time * 0.7);
            float inBand = smoothstep(0.12, 0.0, abs(uv.y - bandY));
            float trackingJitter = (hash11(floor(fragCoord.y * 0.25) + time * 100.0) - 0.5) * inBand * progress * 60.0;

            float syncTear = sin(progress * 3.14159) * (hash11(time * 40.0) - 0.5) * 25.0;
            float totalShift = trackingJitter + syncTear;

            float chromaOffset = progress * 14.0;
            float2 coordR = clamp(fragCoord + float2(totalShift + chromaOffset, 0.0), float2(0.0), resolution);
            float2 coordG = clamp(fragCoord + float2(totalShift, 0.0), float2(0.0), resolution);
            float2 coordB = clamp(fragCoord + float2(totalShift - chromaOffset, 0.0), float2(0.0), resolution);

            half4 colA = half4(imageA.eval(coordR).r, imageA.eval(coordG).g, imageA.eval(coordB).b, 1.0);
            half4 colB = half4(imageB.eval(coordR).r, imageB.eval(coordG).g, imageB.eval(coordB).b, 1.0);

            float staticNoise = hash11(fragCoord.x * 0.1 + fragCoord.y * 3.7 + time * 80.0) * inBand * 0.25;

            half4 blended = mix(colA, colB, half(progress));
            blended.rgb += half3(staticNoise);

            return clamp(blended, half4(0.0), half4(1.0));
        }
    """
}

