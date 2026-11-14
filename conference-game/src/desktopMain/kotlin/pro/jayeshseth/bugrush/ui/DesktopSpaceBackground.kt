package pro.jayeshseth.bugrush.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.skiaCanvas
import pro.jayeshseth.bugrush.game.renderer.SpaceBackground
import org.jetbrains.skia.Paint
import org.jetbrains.skia.Rect
import org.jetbrains.skia.RuntimeEffect
import org.jetbrains.skia.RuntimeShaderBuilder

class DesktopSpaceBackground : SpaceBackground {

    private val sksl = """
        uniform float2 resolution;
        uniform float time;
        uniform float2 cameraOffset;
        uniform float kernelPanic;

        // Hash function without sine for uniform pseudo-randomness
        float hash(float2 p) {
            float3 p3 = fract(float3(p.xyx) * 0.1031);
            p3 += dot(p3, p3.yzx + 33.33);
            return fract((p3.x + p3.y) * p3.z);
        }

        // 2D Value Noise with cubic Hermite interpolation
        float noise(float2 p) {
            float2 i = floor(p);
            float2 f = fract(p);
            float2 u = f * f * (3.0 - 2.0 * f);
            return mix(
                mix(hash(i + float2(0.0, 0.0)), hash(i + float2(1.0, 0.0)), u.x),
                mix(hash(i + float2(0.0, 1.0)), hash(i + float2(1.0, 1.0)), u.x),
                u.y
            );
        }

        // 4-Octave Fractional Brownian Motion for cosmic nebula clouds
        float fbm(float2 p) {
            float v = 0.0;
            float a = 0.5;
            for (int i = 0; i < 4; i++) {
                v += a * noise(p);
                p = float2(0.8 * p.x - 0.6 * p.y, 0.6 * p.x + 0.8 * p.y) * 2.02;
                a *= 0.5;
            }
            return v;
        }

        // Starfield generator with individual twinkle frequencies
        float stars(float2 uv, float speed, float scale, float density) {
            float2 grid = uv * scale;
            float2 id = floor(grid);
            float2 f = fract(grid) - 0.5;
            
            float h = hash(id);
            if (h > density) return 0.0;
            
            float twinkle = 0.6 + 0.4 * sin(time * speed + h * 6.28);
            float d = length(f);
            float starRadius = 0.045 + h * 0.035;
            return smoothstep(starRadius, 0.0, d) * twinkle;
        }

        half4 main(float2 fragCoord) {
            float2 uv = (fragCoord - resolution * 0.5) / min(resolution.x, resolution.y);
            
            // Parallax drift responsive to player position
            float2 camDrift = cameraOffset * 0.00015;
            float2 p = uv + camDrift;
            
            // 1. Deep Space Void Gradient
            half3 spaceVoid = mix(half3(0.012, 0.015, 0.030), half3(0.035, 0.020, 0.060), uv.y + 0.5);
            
            // 2. Swirling Interstellar Nebula (domain warped)
            float t = time * 0.035;
            float2 q = float2(fbm(p * 1.5 + float2(0.0, t)), fbm(p * 1.5 + float2(3.1, -t * 0.8)));
            float f = fbm(p * 1.2 + 2.0 * q);
            
            // Palettes: Interstellar Normal vs Imperial Red Alert
            half3 cViolet  = half3(0.32, 0.08, 0.48);
            half3 cCyan    = half3(0.03, 0.42, 0.55);
            half3 cMagenta = half3(0.58, 0.10, 0.38);
            
            half3 cSithRed = half3(0.72, 0.05, 0.08);
            half3 cCoreHot = half3(0.95, 0.22, 0.10);
            
            half3 nebulaNormal = mix(spaceVoid, cViolet, smoothstep(0.25, 0.75, f));
            nebulaNormal = mix(nebulaNormal, cCyan, smoothstep(0.30, 0.80, q.x) * 0.65);
            nebulaNormal = mix(nebulaNormal, cMagenta, smoothstep(0.35, 0.85, q.y) * 0.5);
            
            half3 nebulaPanic = mix(half3(0.06, 0.01, 0.01), cSithRed, smoothstep(0.20, 0.75, f));
            nebulaPanic = mix(nebulaPanic, cCoreHot, smoothstep(0.30, 0.85, q.x) * 0.7);
            
            half3 col = mix(nebulaNormal, nebulaPanic, kernelPanic);
            
            // 3. Multi-layer Star Wars Starfield
            float s1 = stars(p, 2.5, 95.0, 0.09);
            float s2 = stars(p + float2(12.3, 45.6), 3.8, 48.0, 0.04);
            float s3 = stars(p + float2(89.1, 23.4), 1.6, 22.0, 0.015);
            
            half3 starColor = half3(0.95, 0.98, 1.0);
            col += (s1 * 0.65 + s2 * 0.9 + s3 * 1.5) * starColor;
            
            return half4(col, 1.0);
        }
    """.trimIndent()

    private var effect: RuntimeEffect? = null
    private var builder: RuntimeShaderBuilder? = null
    private var lastPaint: Paint? = null
    private val startTimeMs = System.currentTimeMillis()

    init {
        try {
            effect = RuntimeEffect.makeForShader(sksl)
            builder = effect?.let { RuntimeShaderBuilder(it) }
            lastPaint = Paint()
        } catch (e: Throwable) {
            println("Space shader compilation error: ${e.message}")
        }
    }

    override fun draw(
        scope: DrawScope,
        cameraX: Float,
        cameraY: Float,
        isKernelPanic: Boolean,
        viewportWidth: Float,
        viewportHeight: Float
    ) {
        val b = builder
        val paint = lastPaint
        if (b != null && paint != null && viewportWidth > 0f && viewportHeight > 0f) {
            val time = (System.currentTimeMillis() - startTimeMs) * 0.001f
            b.uniform("resolution", viewportWidth, viewportHeight)
            b.uniform("time", time)
            b.uniform("cameraOffset", cameraX, cameraY)
            b.uniform("kernelPanic", if (isKernelPanic) 1.0f else 0.0f)
            paint.shader = b.makeShader()
            scope.drawIntoCanvas { canvas ->
                canvas.skiaCanvas.drawRect(
                    Rect.makeWH(viewportWidth, viewportHeight),
                    paint
                )
            }
        } else {
            scope.drawRect(Color(0xFF090D16))
        }
    }
}
