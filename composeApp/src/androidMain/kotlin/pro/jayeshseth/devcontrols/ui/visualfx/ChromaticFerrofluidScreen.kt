package pro.jayeshseth.devcontrols.ui.visualfx

import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.withInfiniteAnimationFrameMillis
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import org.intellij.lang.annotations.Language

@Language("AGSL")
const val CHROMATIC_FERROFLUID_SHADER = """
    uniform shader content;
    uniform float2 resolution;
    uniform float time;
    
    // Interactive physics uniforms driven by Compose Animatables
    uniform float2 pointer;          // Current spring-smoothed pointer (px)
    uniform float2 velocity;         // Instantaneous pointer velocity (px/s)
    uniform float pointerStrength;   // 0.0 (idle) -> 1.0 (pressed/active)
    
    // Tunable optical & fluid parameters
    uniform float dispersion;        // Chromatic aberration intensity
    uniform float warpIntensity;     // Domain-warping fBm turbulence
    uniform float surfaceTension;    // Smooth-min (smin) blending radius

    const float PI = 3.14159265359;

    // 2D Hash & Value Noise for fBm
    float hash21(float2 p) {
        p = fract(p * float2(234.34, 435.345));
        p += dot(p, p + 34.23);
        return fract(p.x * p.y);
    }

    float noise(float2 p) {
        float2 i = floor(p);
        float2 f = fract(p);
        // Quintic interpolation curve for C2 continuity (smooth normals!)
        float2 u = f * f * f * (f * (f * 6.0 - 15.0) + 10.0);
        
        float a = hash21(i);
        float b = hash21(i + float2(1.0, 0.0));
        float c = hash21(i + float2(0.0, 1.0));
        float d = hash21(i + float2(1.0, 1.0));
        
        return mix(mix(a, b, u.x), mix(c, d, u.x), u.y);
    }

    // 4-Octave Fractal Brownian Motion with rotation matrix to reduce grid artifacts
    float fbm(float2 p) {
        float value = 0.0;
        float amp = 0.5;
        float2x2 rot = float2x2(0.8, -0.6, 0.6, 0.8);
        for (int i = 0; i < 4; i++) {
            value += amp * noise(p);
            p = rot * p * 2.02 + float2(time * 0.15, -time * 0.1);
            amp *= 0.5;
        }
        return value;
    }

    // Inigo Quilez Polynomial Smooth Minimum for organic ferrofluid fusion
    float smin(float a, float b, float k) {
        float h = clamp(0.5 + 0.5 * (b - a) / max(k, 0.0001), 0.0, 1.0);
        return mix(b, a, h) - k * h * (1.0 - h);
    }

    // Velocity-stretched capsule/ellipse SDF for the main cursor droplet
    float sdVelocityDroplet(float2 p, float2 center, float2 vel, float radius) {
        float2 rel = p - center;
        float speed = length(vel);
        if (speed > 1.0) {
            float2 dir = vel / speed;
            float2 perp = float2(-dir.y, dir.x);
            // Stretch along velocity vector, compress perpendicular (conservation of volume)
            float stretch = clamp(1.0 + speed * 0.0008, 1.0, 2.4);
            float2 local = float2(
                dot(rel, dir) / stretch,
                dot(rel, perp) * sqrt(stretch)
            );
            return length(local) - radius;
        }
        return length(rel) - radius;
    }

    // Evaluates the combined scalar heightfield of the ferrofluid lens at UV-scaled coords
    float evaluateFluidField(float2 uv, float aspect) {
        float2 p = float2(uv.x * aspect, uv.y);
        float2 ptr = float2((pointer.x / resolution.x) * aspect, pointer.y / resolution.y);
        float2 vel = velocity * float2(aspect / resolution.x, 1.0 / resolution.y);

        // Base radius swells when touched
        float mainRadius = mix(0.11, 0.17, pointerStrength);
        float dMain = sdVelocityDroplet(p, ptr, vel, mainRadius);

        // Orbiting satellite ferro-droplets that get pulled into the main well
        float dSatellites = 10.0;
        for (int i = 0; i < 4; i++) {
            float fi = float(i);
            float angle = time * (0.9 + fi * 0.35) + fi * 1.57079;
            // Orbit tightens when pointer is pressed
            float orbitRadius = mix(0.22, 0.12, pointerStrength) + 0.04 * sin(time * 2.0 + fi);
            float2 offset = float2(cos(angle), sin(angle * 1.3)) * orbitRadius;
            float satRadius = 0.038 + 0.012 * sin(time * 3.0 + fi * 2.0);
            float dSat = length(p - (ptr + offset)) - satRadius;
            dSatellites = smin(dSatellites, dSat, surfaceTension * 0.7);
        }

        // Fuse primary velocity droplet with satellites
        float dCombined = smin(dMain, dSatellites, surfaceTension);

        // Iterative Domain Warping: f(p + f(p))
        float2 q = float2(
            fbm(p * 3.5 + float2(0.0, time * 0.4)),
            fbm(p * 3.5 + float2(5.2, -time * 0.3))
        );
        float2 r = float2(
            fbm(p * 4.0 + 2.5 * q + float2(1.7, 9.2) + 0.25 * time),
            fbm(p * 4.0 + 2.5 * q + float2(8.3, 2.8) - 0.2 * time)
        );
        float warp = fbm(p * 3.0 + 2.0 * r);

        // Displace SDF boundary with domain-warped ripples
        dCombined -= (warp - 0.5) * warpIntensity * (0.4 + 0.6 * pointerStrength);

        // Convert signed distance inside the boundary into a smooth 3D dome height [0..1]
        float inside = smoothstep(0.02, -0.14, dCombined);
        // Add micro-ripples on the dome interior
        float ripples = sin(dCombined * 65.0 - time * 6.0) * 0.03 * inside * pointerStrength;
        return inside * sqrt(max(0.0, 1.0 - pow(clamp(dCombined / -0.16, 0.0, 1.0) - 1.0, 2.0))) + ripples;
    }

    // Thin-film cosine palette for iridescent Fresnel interference
    float3 thinFilmPalette(float t) {
        float3 a = float3(0.5, 0.5, 0.5);
        float3 b = float3(0.5, 0.5, 0.5);
        float3 c = float3(1.0, 1.0, 1.0);
        float3 d = float3(0.00, 0.33, 0.67);
        return a + b * cos(2.0 * PI * (c * t + d));
    }

    half4 main(float2 fragCoord) {
        float2 uv = fragCoord / resolution;
        float aspect = resolution.x / max(resolution.y, 1.0);

        // 1. Reconstruct 3D Surface Normal via Central Finite Differences
        float eps = 2.0 / max(resolution.x, resolution.y);
        float hCenter = evaluateFluidField(uv, aspect);
        
        // Early-out optimization for fragments well outside the fluid field
        if (hCenter <= 0.0005) {
            // Subtle gravitational pincushion pull near the boundary even outside the droplet
            float2 ptrUV = pointer / resolution;
            float2 toPtr = fragCoord - pointer;
            float dist = length(toPtr / resolution.x);
            float pull = exp(-dist * 12.0) * 14.0 * pointerStrength;
            return content.eval(fragCoord - normalize(toPtr + 0.0001) * pull);
        }

        float hRight = evaluateFluidField(uv + float2(eps, 0.0), aspect);
        float hUp    = evaluateFluidField(uv + float2(0.0, eps), aspect);

        float dHdX = (hRight - hCenter) / eps;
        float dHdY = (hUp - hCenter) / eps;

        // 3D unit surface normal (Z points out of the screen toward the viewer)
        float3 normal = normalize(float3(-dHdX * 0.18, -dHdY * 0.18, 1.0));

        // 2. Snell's Law Refraction & 3-Channel Chromatic Dispersion
        float3 viewDir = float3(0.0, 0.0, 1.0);
        float2 refractBase = normal.xy * hCenter * resolution.x * 0.12;

        // Chromatic aberration spreads R, G, B channels along the normal + radial vector
        float dispScale = dispersion * (1.0 + 0.8 * pointerStrength);
        float2 offsetR = refractBase * (1.0 - dispScale * 0.35);
        float2 offsetG = refractBase * 1.0;
        float2 offsetB = refractBase * (1.0 + dispScale * 0.35);

        half r = content.eval(clamp(fragCoord - offsetR, float2(0.0), resolution)).r;
        half g = content.eval(clamp(fragCoord - offsetG, float2(0.0), resolution)).g;
        half b = content.eval(clamp(fragCoord - offsetB, float2(0.0), resolution)).b;
        half a = content.eval(fragCoord - offsetG).a;
        float3 refractedColor = float3(r, g, b);

        // 3. 3D Lighting: Blinn-Phong Specular + Schlick Fresnel Iridescence
        float3 lightDir1 = normalize(float3(-0.45, -0.6, 0.65));
        float3 lightDir2 = normalize(float3(0.6, 0.4, 0.5));

        float3 halfVec1 = normalize(lightDir1 + viewDir);
        float3 halfVec2 = normalize(lightDir2 + viewDir);

        float spec1 = pow(max(dot(normal, halfVec1), 0.0), 96.0);
        float spec2 = pow(max(dot(normal, halfVec2), 0.0), 48.0) * 0.4;

        // Schlick's Fresnel approximation for grazing-angle rim reflections
        float cosTheta = clamp(dot(normal, viewDir), 0.0, 1.0);
        float fresnel = pow(1.0 - cosTheta, 3.5);

        // Thin-film interference shifts hue based on viewing angle and time
        float3 iridescence = thinFilmPalette(fresnel * 2.2 + time * 0.25 + hCenter * 0.5);

        // Beer-Lambert subtle liquid absorption tint inside thick regions of the droplet
        float3 absorption = exp(-float3(0.18, 0.08, 0.02) * hCenter * 1.8);
        float3 finalRGB = refractedColor * absorption;

        // Composite iridescent Fresnel rim and razor-sharp wet specular highlights
        finalRGB = mix(finalRGB, iridescence, fresnel * 0.65);
        finalRGB += float3(1.0, 0.98, 0.95) * (spec1 * 1.15 + spec2);

        // Crisp anti-aliased meniscus shadow at the outer contact edge
        float edgeShadow = smoothstep(0.0, 0.08, hCenter) * (1.0 - smoothstep(0.08, 0.22, hCenter));
        finalRGB *= (1.0 - edgeShadow * 0.25);

        return half4(half3(clamp(finalRGB, 0.0, 1.0)), a);
    }
"""

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
fun Modifier.chromaticFerrofluidLens(
    dispersion: Float = 0.45f,
    warpIntensity: Float = 0.14f,
    surfaceTension: Float = 0.09f
): Modifier = composed {
    val shader = remember { RuntimeShader(CHROMATIC_FERROFLUID_SHADER) }
    val scope = rememberCoroutineScope()

    var timeSeconds by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(Unit) {
        val startNanos = System.nanoTime()
        while (true) {
            withInfiniteAnimationFrameMillis {
                timeSeconds = (System.nanoTime() - startNanos) / 1_000_000_000f
            }
        }
    }

    val pointerAnim = remember {
        Animatable(Offset(500f, 800f), Offset.VectorConverter)
    }
    val strengthAnim = remember { Animatable(0f) }

    this
        .clipToBounds()
        .onSizeChanged { size ->
            if (pointerAnim.value == Offset(500f, 800f) && size.width > 0) {
                scope.launch {
                    pointerAnim.snapTo(Offset(size.width / 2f, size.height / 2.2f))
                }
            }
        }
        .pointerInput(Unit) {
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false)
                scope.launch {
                    strengthAnim.animateTo(
                        targetValue = 1f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow
                        )
                    )
                }
                scope.launch {
                    pointerAnim.animateTo(
                        targetValue = down.position,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioLowBouncy,
                            stiffness = Spring.StiffnessMediumLow
                        )
                    )
                }

                do {
                    val event = awaitPointerEvent()
                    val change = event.changes.firstOrNull()
                    if (change != null && change.pressed) {
                        change.consume()
                        scope.launch {
                            pointerAnim.animateTo(
                                targetValue = change.position,
                                animationSpec = spring(
                                    dampingRatio = 0.65f,
                                    stiffness = 380f
                                )
                            )
                        }
                    }
                } while (event.changes.any { it.pressed })

                scope.launch {
                    strengthAnim.animateTo(
                        targetValue = 0f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioHighBouncy,
                            stiffness = Spring.StiffnessLow
                        )
                    )
                }
            }
        }
        .graphicsLayer {
            val currentPos = pointerAnim.value
            val currentVel = pointerAnim.velocity

            shader.setFloatUniform("resolution", size.width, size.height)
            shader.setFloatUniform("time", timeSeconds)
            shader.setFloatUniform("pointer", currentPos.x, currentPos.y)
            shader.setFloatUniform("velocity", currentVel.x, currentVel.y)
            shader.setFloatUniform("pointerStrength", strengthAnim.value)
            shader.setFloatUniform("dispersion", dispersion)
            shader.setFloatUniform("warpIntensity", warpIntensity)
            shader.setFloatUniform("surfaceTension", surfaceTension)

            renderEffect = RenderEffect
                .createRuntimeShaderEffect(shader, "content")
                .asComposeRenderEffect()
        }
}

@Composable
fun ChromaticFerrofluidScreen(onBack: () -> Unit) {
    BackHandler(onBack = onBack)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        FerrofluidShaderShowcase(onBack = onBack)
    } else {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF07090E))
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    text = "Requires Android 13+ (API 33)",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                Text(
                    text = "AGSL RenderEffect shaders require Android Tiramisu or newer.",
                    color = Color(0xFF94A3B8),
                    fontSize = 14.sp
                )
                TextButton(onClick = onBack) {
                    Text("Go Back", color = Color(0xFF38BDF8))
                }
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
fun FerrofluidShaderShowcase(onBack: () -> Unit) {
    var dispersion by remember { mutableFloatStateOf(0.55f) }
    var warpIntensity by remember { mutableFloatStateOf(0.15f) }
    var surfaceTension by remember { mutableFloatStateOf(0.10f) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF07090E))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .chromaticFerrofluidLens(
                    dispersion = dispersion,
                    warpIntensity = warpIntensity,
                    surfaceTension = surfaceTension
                )
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val step = 36.dp.toPx()
                val gridColor = Color(0xFF1E293B)
                var x = 0f
                while (x < size.width) {
                    drawLine(gridColor, Offset(x, 0f), Offset(x, size.height), strokeWidth = 1.5f)
                    x += step
                }
                var y = 0f
                while (y < size.height) {
                    drawLine(gridColor, Offset(0f, y), Offset(size.width, y), strokeWidth = 1.5f)
                    y += step
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp, vertical = 52.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = onBack,
                        colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFF38BDF8))
                    ) {
                        Text("← Back", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }

                Text(
                    text = "AGSL // FERROFLUID\nOPTICAL LENS",
                    color = Color.White,
                    fontSize = 32.sp,
                    lineHeight = 36.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace
                )

                Text(
                    text = "Drag across the UI to stretch the anisotropic SDF field, fuse orbiting droplets, and refract live Compose nodes.",
                    color = Color(0xFF94A3B8),
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    OpticalSpecCard(
                        title = "SNELL IOR",
                        metric = "1.49x",
                        subtitle = "3-Tap RGB Split",
                        accent = Brush.linearGradient(
                            listOf(Color(0xFF6366F1), Color(0xFFEC4899))
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    OpticalSpecCard(
                        title = "DOMAIN WARP",
                        metric = "4-OCT",
                        subtitle = "f(p + f(p)) fBm",
                        accent = Brush.linearGradient(
                            listOf(Color(0xFF06B6D4), Color(0xFF10B981))
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }

                OpticalSpecCard(
                    title = "SURFACE RECONSTRUCTION",
                    metric = "∇H Central Diff → Blinn-Phong + Schlick Fresnel",
                    subtitle = "Zero-recomposition graphicsLayer uniform pipeline",
                    accent = Brush.horizontalGradient(
                        listOf(Color(0xFFF59E0B), Color(0xFFEF4444), Color(0xFF8B5CF6))
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(16.dp)
                .background(Color(0xCC0F172A), RoundedCornerShape(20.dp))
                .border(1.dp, Color(0xFF334155), RoundedCornerShape(20.dp))
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            ShaderControlSlider("Chromatic Dispersion", dispersion, 0f..1.5f) { dispersion = it }
            ShaderControlSlider("fBm Warp Turbulence", warpIntensity, 0f..0.4f) { warpIntensity = it }
            ShaderControlSlider("Surface Tension (smin)", surfaceTension, 0.02f..0.25f) { surfaceTension = it }
        }
    }
}

@Composable
private fun OpticalSpecCard(
    title: String,
    metric: String,
    subtitle: String,
    accent: Brush,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(Color(0xFF0F172A), RoundedCornerShape(18.dp))
            .border(1.5.dp, accent, RoundedCornerShape(18.dp))
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = title,
            color = Color(0xFF64748B),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
        Text(
            text = metric,
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.ExtraBold,
            fontFamily = FontFamily.Monospace
        )
        Text(
            text = subtitle,
            color = Color(0xFFCBD5E1),
            fontSize = 12.sp
        )
    }
}

@Composable
private fun ShaderControlSlider(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, color = Color(0xFFE2E8F0), fontSize = 12.sp, fontFamily = FontFamily.Monospace)
            Text("%.2f".format(value), color = Color(0xFF38BDF8), fontSize = 12.sp, fontFamily = FontFamily.Monospace)
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = range,
            colors = SliderDefaults.colors(
                thumbColor = Color(0xFF38BDF8),
                activeTrackColor = Color(0xFF0284C7)
            ),
            modifier = Modifier.height(28.dp)
        )
    }
}
