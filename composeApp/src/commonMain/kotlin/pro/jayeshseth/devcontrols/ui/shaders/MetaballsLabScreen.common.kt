package pro.jayeshseth.devcontrols.ui.shaders

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pro.jayeshseth.devcontrols.platform.CustomShaderBox
import kotlin.random.Random

private class CommonLiquidDrop(
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    var r: Float
)

private enum class CommonLiquidTheme(val label: String, val uniformVal: Float, val accentColor: Color) {
    MERCURY("Mercury", 0.0f, Color(0xFF94A3B8)),
    GOLD("Liquid Gold", 1.0f, Color(0xFFFFD700)),
    ACID("Neon Acid", 2.0f, Color(0xFF39FF14)),
    MAGMA("Magma Red", 3.0f, Color(0xFFFF4500))
}

private const val ANALYTICAL_METABALLS_AGSL = """
uniform float2 iResolution;
uniform float3 uBalls[32]; // x, y, radius
uniform float uBallCount;
uniform float uThreshold;
uniform float uMetalSpec;
uniform float uTheme;

float evaluateDensity(float2 p, int count) {
    float density = 0.0;
    for (int i = 0; i < 32; i++) {
        if (i >= count) break;
        float2 d = p - uBalls[i].xy;
        float distSq = dot(d, d);
        if (distSq > 0.0) {
            density += (uBalls[i].z * uBalls[i].z) / distSq;
        }
    }
    return density;
}

half4 main(float2 fragCoord) {
    int count = int(uBallCount);
    float density = evaluateDensity(fragCoord, count);
    
    if (density < uThreshold) {
        return half4(0.04, 0.04, 0.08, 1.0);
    }
    
    // Compute normal via central difference of the analytical density field
    float step = 2.0;
    float dRight = evaluateDensity(fragCoord + float2(step, 0.0), count);
    float dLeft  = evaluateDensity(fragCoord - float2(step, 0.0), count);
    float dUp    = evaluateDensity(fragCoord + float2(0.0, step), count);
    float dDown  = evaluateDensity(fragCoord - float2(0.0, step), count);
    
    float3 normal;
    normal.x = dLeft - dRight;
    normal.y = dDown - dUp;
    normal.z = 0.45;
    normal = normalize(normal);
    
    float3 lightDir = normalize(float3(0.5, 0.6, 1.0));
    float3 viewDir = float3(0.0, 0.0, 1.0);
    float3 halfVec = normalize(lightDir + viewDir);
    
    float diff = max(dot(normal, lightDir), 0.0);
    float spec = pow(max(dot(normal, halfVec), 0.0), 28.0) * uMetalSpec;
    
    half3 baseCol;
    if (uTheme < 0.5) {
        baseCol = half3(0.72, 0.74, 0.79) * half(diff + 0.3);
    } else if (uTheme < 1.5) {
        baseCol = half3(1.0, 0.81, 0.15) * half(diff + 0.25);
    } else if (uTheme < 2.5) {
        baseCol = half3(0.18, 0.92, 0.12) * half(diff + 0.32);
    } else {
        half3 magmaRed = half3(0.9, 0.16, 0.02);
        half3 magmaYellow = half3(1.0, 0.85, 0.12);
        baseCol = mix(magmaRed, magmaYellow, half(smoothstep(uThreshold, uThreshold + 0.5, density))) * half(diff + 0.3);
    }
    
    half3 finalColor = baseCol + half3(1.0) * half(spec * 0.92);
    float rim = smoothstep(uThreshold, uThreshold + 0.035, density);
    finalColor *= half(rim);
    
    return half4(finalColor, 1.0);
}
"""

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MetaballsLabScreenMultiplatform(onBack: () -> Unit) {
    var density by remember { mutableStateOf<Float?>(null) }
    var viewSize by remember { mutableStateOf(IntSize.Zero) }
    
    // Physics Config
    var ballCount by remember { mutableIntStateOf(12) }
    var threshold by remember { mutableFloatStateOf(1.0f) }
    var metalSpec by remember { mutableFloatStateOf(0.8f) }
    var currentTheme by remember { mutableStateOf(CommonLiquidTheme.MERCURY) }
    
    val drops = remember { mutableListOf<CommonLiquidDrop>() }
    
    // Initialize drops when size is known
    LaunchedEffect(viewSize, ballCount) {
        if (viewSize.width > 0 && viewSize.height > 0) {
            if (drops.size < ballCount) {
                while (drops.size < ballCount) {
                    drops.add(
                        CommonLiquidDrop(
                            x = Random.nextFloat() * viewSize.width,
                            y = Random.nextFloat() * viewSize.height,
                            vx = (Random.nextFloat() - 0.5f) * 400f,
                            vy = (Random.nextFloat() - 0.5f) * 400f,
                            r = 35f + Random.nextFloat() * 25f
                        )
                    )
                }
            } else if (drops.size > ballCount) {
                while (drops.size > ballCount) {
                    drops.removeAt(drops.size - 1)
                }
            }
        }
    }
    
    // Physics Loop
    var lastNanos by remember { mutableStateOf(0L) }
    LaunchedEffect(viewSize) {
        while (true) {
            withFrameNanos { frameTimeNanos ->
                if (lastNanos == 0L) {
                    lastNanos = frameTimeNanos
                    return@withFrameNanos
                }
                val dt = ((frameTimeNanos - lastNanos) / 1e9f).coerceAtMost(0.03f)
                lastNanos = frameTimeNanos
                
                if (viewSize.width > 0 && viewSize.height > 0) {
                    drops.forEach { drop ->
                        drop.x += drop.vx * dt
                        drop.y += drop.vy * dt
                        
                        // Bounce off boundaries
                        val pad = drop.r
                        if (drop.x < pad) {
                            drop.x = pad
                            drop.vx = -drop.vx
                        } else if (drop.x > viewSize.width - pad) {
                            drop.x = viewSize.width - pad
                            drop.vx = -drop.vx
                        }
                        
                        if (drop.y < pad) {
                            drop.y = pad
                            drop.vy = -drop.vy
                        } else if (drop.y > viewSize.height - pad) {
                            drop.y = viewSize.height - pad
                            drop.vy = -drop.vy
                        }
                    }
                }
            }
        }
    }
    
    // Convert drops to a flat FloatArray for the uniform
    val ballsArray = remember(drops, lastNanos) {
        val arr = FloatArray(32 * 3)
        drops.forEachIndexed { i, drop ->
            if (i < 32) {
                arr[i * 3] = drop.x
                arr[i * 3 + 1] = drop.y
                arr[i * 3 + 2] = drop.r
            }
        }
        arr
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Metaballs Liquid Lab") },
                navigationIcon = {
                    androidx.compose.material3.TextButton(onClick = onBack) { Text("Back") }
                }
            )
        },
        modifier = Modifier.fillMaxSize()
    ) { inner ->
        Column(
            modifier = Modifier
                .padding(inner)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // Interactive Shader Canvas
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(380.dp)
                    .background(Color(0xFF0A0A14))
                    .onSizeChanged { viewSize = it }
            ) {
                if (viewSize.width > 0 && viewSize.height > 0) {
                    CustomShaderBox(
                        shaderSource = ANALYTICAL_METABALLS_AGSL,
                        modifier = Modifier.fillMaxSize(),
                        uniformNames = listOf("iResolution", "uBalls", "uBallCount", "uThreshold", "uMetalSpec", "uTheme"),
                        uniforms = mapOf(
                            "iResolution" to Offset(viewSize.width.toFloat(), viewSize.height.toFloat()),
                            "uBalls" to ballsArray,
                            "uBallCount" to ballCount.toFloat(),
                            "uThreshold" to threshold,
                            "uMetalSpec" to metalSpec,
                            "uTheme" to currentTheme.uniformVal
                        ),
                        fallback = {
                            Box(Modifier.fillMaxSize().background(Color.DarkGray))
                        }
                    )
                }
            }
            
            // Controls Playground
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("Liquid Theme", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CommonLiquidTheme.values().forEach { theme ->
                        val selected = currentTheme == theme
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (selected) theme.accentColor.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { currentTheme = theme }
                                .border(
                                    width = 1.dp,
                                    color = if (selected) theme.accentColor else Color.Transparent,
                                    shape = RoundedCornerShape(8.dp)
                                )
                        ) {
                            Box(modifier = Modifier.padding(12.dp), contentAlignment = Alignment.Center) {
                                Text(theme.label, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                            }
                        }
                    }
                }
                
                // Ball Count Slider
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Drop Count", fontSize = 14.sp)
                        Text("$ballCount", fontWeight = FontWeight.Bold)
                    }
                    Slider(
                        value = ballCount.toFloat(),
                        onValueChange = { ballCount = it.toInt() },
                        valueRange = 2f..32f,
                        steps = 29
                    )
                }
                
                // Threshold Slider
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Liquid Cohesion (Viscosity)", fontSize = 14.sp)
                        Text("${((threshold * 100).toInt() / 100f)}", fontWeight = FontWeight.Bold)
                    }
                    Slider(
                        value = threshold,
                        onValueChange = { threshold = it },
                        valueRange = 0.5f..2.5f
                    )
                }
                
                // Specular Slider
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Gloss Intensity", fontSize = 14.sp)
                        Text("${((metalSpec * 100).toInt() / 100f)}", fontWeight = FontWeight.Bold)
                    }
                    Slider(
                        value = metalSpec,
                        onValueChange = { metalSpec = it },
                        valueRange = 0.0f..1.5f
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}
