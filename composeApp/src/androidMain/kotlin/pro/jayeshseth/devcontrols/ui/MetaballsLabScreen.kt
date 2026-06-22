package pro.jayeshseth.devcontrols.ui

import android.content.Context
import android.graphics.RuntimeShader
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.random.Random

// 2D Liquid Drop physics particle
private class LiquidDrop(
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    var r: Float
)

// Liquid themes mapped to shader uniforms
private enum class LiquidTheme(val label: String, val uniformVal: Float, val accentColor: Color) {
    MERCURY("Mercury", 0.0f, Color(0xFF94A3B8)),
    GOLD("Liquid Gold", 1.0f, Color(0xFFFFD700)),
    ACID("Neon Acid", 2.0f, Color(0xFF39FF14)),
    MAGMA("Magma Red", 3.0f, Color(0xFFFF4500))
}

// Metaball Normal-mapping Specular AGSL Shader
private const val METABALLS_AGSL = """
uniform shader content;
uniform float2 iResolution;
uniform float threshold;
uniform float metalSpec;
uniform float liquidTheme; // 0.0 = Mercury, 1.0 = Gold, 2.0 = Acid, 3.0 = Magma

half4 main(float2 fragCoord) {
    half4 color = content.eval(fragCoord);
    float density = color.a;
    
    // Draw background color if density is below metaball threshold
    if (density < threshold) {
        return half4(0.04, 0.04, 0.08, 1.0);
    }
    
    // Compute surface normal via central difference of density gradient
    float step = 1.8;
    float dRight = content.eval(fragCoord + float2(step, 0.0)).a;
    float dLeft  = content.eval(fragCoord - float2(step, 0.0)).a;
    float dUp    = content.eval(fragCoord + float2(0.0, step)).a;
    float dDown  = content.eval(fragCoord - float2(0.0, step)).a;
    
    float3 normal;
    normal.x = dLeft - dRight;
    normal.y = dDown - dUp;
    normal.z = 0.45;
    normal = normalize(normal);
    
    // Specular lighting variables (Blinn-Phong)
    float3 lightDir = normalize(float3(0.5, 0.6, 1.0));
    float3 viewDir = float3(0.0, 0.0, 1.0);
    float3 halfVec = normalize(lightDir + viewDir);
    
    float diff = max(dot(normal, lightDir), 0.0);
    float spec = pow(max(dot(normal, halfVec), 0.0), 28.0) * metalSpec;
    
    // Determine base color matching selected theme
    half3 baseCol;
    if (liquidTheme < 0.5) { // Mercury (Chrome)
        baseCol = half3(0.72, 0.74, 0.79) * half(diff + 0.3);
    } else if (liquidTheme > 0.5 && liquidTheme < 1.5) { // Gold
        baseCol = half3(1.0, 0.81, 0.15) * half(diff + 0.25);
    } else if (liquidTheme > 1.5 && liquidTheme < 2.5) { // Neon Acid Green
        baseCol = half3(0.18, 0.92, 0.12) * half(diff + 0.32);
    } else { // Magma Flow
        half3 magmaRed = half3(0.9, 0.16, 0.02);
        half3 magmaYellow = half3(1.0, 0.85, 0.12);
        baseCol = mix(magmaRed, magmaYellow, half(smoothstep(threshold, 0.95, density))) * half(diff + 0.3);
    }
    
    // Blend specular highlight and draw a glossy rim contour
    half3 finalColor = baseCol + half3(1.0) * half(spec * 0.92);
    float rim = smoothstep(threshold, threshold + 0.035, density);
    finalColor *= half(rim);
    
    return half4(finalColor, 1.0);
}
"""

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MetaballsLabScreen(onBack: () -> Unit) {
    val context = LocalContext.current

    // Check Android SDK API level
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Metaballs Liquid Metal", color = Color.White) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Text("< Back", color = Color(0xFF00E5FF), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF06060E))
                )
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
                    .background(Color(0xFF06060E))
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "This visual laboratory requires Android 13 (API level 33) or higher for AGSL metaball post-processing shaders.",
                    color = Color.White,
                    fontSize = 16.sp
                )
            }
        }
        return
    }

    // Interactive configurations
    var selectedTheme by remember { mutableStateOf(LiquidTheme.MERCURY) }
    var dropCount by remember { mutableIntStateOf(16) }
    var baseRadius by remember { mutableFloatStateOf(28f) }
    var surfaceTension by remember { mutableFloatStateOf(0.48f) } // threshold uniform
    var viscosity by remember { mutableFloatStateOf(0.04f) } // damping
    var specularIntensity by remember { mutableFloatStateOf(1.0f) }
    var useGyroscope by remember { mutableStateOf(false) }
    var useTouchWell by remember { mutableStateOf(false) }

    // Screen boundaries size
    var viewportSize by remember { mutableStateOf(IntSize.Zero) }

    // Touch positions
    var touchPos by remember { mutableStateOf<Offset?>(null) }

    // Accelerometer Gravity Vectors
    var gravityX by remember { mutableFloatStateOf(0f) }
    var gravityY by remember { mutableFloatStateOf(0.45f) } // default falls down

    // Accelerometer Sensor Listener
    DisposableEffect(useGyroscope) {
        if (!useGyroscope) {
            gravityX = 0f
            gravityY = 0.45f
            return@DisposableEffect onDispose {}
        }
        
        val sm = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val sensor = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                // Map tilt coordinates to gravity vector (invert X for landscape matches screen)
                gravityX = -event.values[0] * 0.08f
                gravityY = event.values[1] * 0.08f
            }
            override fun onAccuracyChanged(s: Sensor?, a: Int) {}
        }
        sm.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_GAME)
        onDispose { sm.unregisterListener(listener) }
    }

    // Initialize list of Liquid Drops
    val drops = remember { mutableListOf<LiquidDrop>() }
    LaunchedEffect(viewportSize) {
        if (viewportSize == IntSize.Zero) return@LaunchedEffect
        drops.clear()
        val w = viewportSize.width.toFloat()
        val h = viewportSize.height.toFloat()
        repeat(dropCount) {
            drops.add(
                LiquidDrop(
                    x = Random.nextFloat() * (w - 100f) + 50f,
                    y = Random.nextFloat() * (h - 100f) + 50f,
                    vx = (Random.nextFloat() - 0.5f) * 6f,
                    vy = (Random.nextFloat() - 0.5f) * 6f,
                    r = baseRadius * (0.8f + Random.nextFloat() * 0.4f)
                )
            )
        }
    }

    // Synchronize drop list count when slider changes
    LaunchedEffect(dropCount, baseRadius) {
        if (viewportSize == IntSize.Zero) return@LaunchedEffect
        val w = viewportSize.width.toFloat()
        val h = viewportSize.height.toFloat()
        
        while (drops.size < dropCount) {
            drops.add(
                LiquidDrop(
                    x = Random.nextFloat() * (w - 100f) + 50f,
                    y = Random.nextFloat() * (h - 100f) + 50f,
                    vx = (Random.nextFloat() - 0.5f) * 5f,
                    vy = (Random.nextFloat() - 0.5f) * 5f,
                    r = baseRadius * (0.8f + Random.nextFloat() * 0.4f)
                )
            )
        }
        while (drops.size > dropCount) {
            drops.removeAt(drops.size - 1)
        }
        // Sync radii
        for (d in drops) {
            d.r = baseRadius * (0.8f + Random.nextFloat() * 0.4f)
        }
    }

    // Animation Tick & Physics updates
    var tick by remember { mutableIntStateOf(0) }
    LaunchedEffect(viewportSize) {
        while (true) {
            withFrameNanos {
                if (viewportSize != IntSize.Zero && drops.isNotEmpty()) {
                    val w = viewportSize.width.toFloat()
                    val h = viewportSize.height.toFloat()
                    val gx = gravityX
                    val gy = gravityY
                    val vis = viscosity
                    val tPos = touchPos
                    val pullWell = useTouchWell

                    // 1. Position Integration, Boundary Bounce & Gravity
                    for (d in drops) {
                        // Apply gravity
                        d.vx += gx
                        d.vy += gy
                        
                        // Touch gravity pull well
                        if (pullWell && tPos != null) {
                            val dx = tPos.x - d.x
                            val dy = tPos.y - d.y
                            val distSq = dx * dx + dy * dy
                            if (distSq > 0.1f) {
                                val dist = kotlin.math.sqrt(distSq)
                                val pull = 0.58f * (1f - (dist / 1000f).coerceIn(0f, 1f))
                                d.vx += (dx / dist) * pull
                                d.vy += (dy / dist) * pull
                            }
                        }

                        // Apply damping viscosity
                        d.vx *= (1f - vis)
                        d.vy *= (1f - vis)
                        
                        // Integrate position
                        d.x += d.vx
                        d.y += d.vy

                        // Boundary bounds collision bounces
                        if (d.x < d.r) {
                            d.x = d.r
                            d.vx = -d.vx * 0.45f
                        } else if (d.x > w - d.r) {
                            d.x = w - d.r
                            d.vx = -d.vx * 0.45f
                        }
                        if (d.y < d.r) {
                            d.y = d.r
                            d.vy = -d.vy * 0.45f
                        } else if (d.y > h - d.r) {
                            d.y = h - d.r
                            d.vy = -d.vy * 0.45f
                        }
                    }

                    // 2. Drop-to-Drop boundary collision push separation
                    for (i in 0 until drops.size) {
                        val d1 = drops[i]
                        for (j in i + 1 until drops.size) {
                            val d2 = drops[j]
                            val dx = d2.x - d1.x
                            val dy = d2.y - d1.y
                            val distSq = dx * dx + dy * dy
                            val minDist = d1.r + d2.r
                            if (distSq < minDist * minDist && distSq > 0.05f) {
                                val dist = kotlin.math.sqrt(distSq)
                                val overlap = minDist - dist
                                val nx = dx / dist
                                val ny = dy / dist
                                // Soft spring repulsion separation
                                val force = overlap * 0.09f
                                d1.vx -= nx * force
                                d1.vy -= ny * force
                                d2.vx += nx * force
                                d2.vy += ny * force
                            }
                        }
                    }
                }
                tick++
            }
        }
    }

    // Trigger explosive radial scatter impulse
    fun scatter() {
        for (d in drops) {
            val angle = Random.nextDouble(0.0, 6.283185).toFloat()
            val speed = Random.nextFloat() * 12f + 4f
            d.vx = kotlin.math.cos(angle) * speed
            d.vy = kotlin.math.sin(angle) * speed
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mercury Liquid Sandbox", style = MaterialTheme.typography.titleMedium, color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Text("< Back", color = Color(0xFF00E5FF), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF06060E))
            )
        },
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .background(Color(0xFF06060E))
        ) {
            // Viewport Canvas drawing blurred density fields (Top 50% height)
            val shader = remember { RuntimeShader(METABALLS_AGSL) }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.5f)
                    .onSizeChanged { viewportSize = it }
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = { offset -> touchPos = offset },
                            onDragEnd = { touchPos = null },
                            onDragCancel = { touchPos = null },
                            onDrag = { change, _ -> touchPos = change.position }
                        )
                    }
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onPress = { offset ->
                                touchPos = offset
                                tryAwaitRelease()
                                touchPos = null
                            }
                        )
                    }
                    .graphicsLayer {
                        // Set AGSL uniforms
                        shader.setFloatUniform("iResolution", size.width, size.height)
                        shader.setFloatUniform("threshold", surfaceTension)
                        shader.setFloatUniform("metalSpec", specularIntensity)
                        shader.setFloatUniform("liquidTheme", selectedTheme.uniformVal)
                        
                        renderEffect = android.graphics.RenderEffect
                            .createRuntimeShaderEffect(shader, "content")
                            .asComposeRenderEffect()
                    }
                    .background(Color.Transparent)
                    .drawBehind {
                        // Read tick
                        tick
                        
                        // Paint blurred density field circles for metaballs
                        for (d in drops) {
                            drawCircle(
                                brush = Brush.radialGradient(
                                    colors = listOf(Color.White, Color.Transparent),
                                    center = Offset(d.x, d.y),
                                    radius = d.r * 2.1f
                                ),
                                radius = d.r * 2.1f,
                                center = Offset(d.x, d.y)
                            )
                        }
                    }
            )

            // Settings Control Dashboard (Bottom 50% height)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.5f)
                    .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                    .border(1.dp, Color(0xFF1E1E2E), RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0D0D18)),
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp, vertical = 16.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Liquid Physics Config", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Button(
                            onClick = { scatter() },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF007F)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Scatter Drops 💥", color = Color.White, fontSize = 12.sp)
                        }
                    }

                    Spacer(Modifier.height(14.dp))

                    // Liquid Themes selector
                    Text("Select Liquid Theme", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        LiquidTheme.values().forEach { theme ->
                            val isSel = selectedTheme == theme
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSel) Color(0xFF1E1E30) else Color(0xFF0D0D18))
                                    .border(
                                        width = 1.dp,
                                        color = if (isSel) theme.accentColor else Color(0xFF1E1E2E),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .clickable { selectedTheme = theme }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = theme.label.split(" ").last(),
                                    fontSize = 11.sp,
                                    color = if (isSel) theme.accentColor else Color(0xFF64748B),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    // Sensor / Touch interaction toggles
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Gyroscope
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Gyro Gravity", color = Color.White, fontSize = 12.sp)
                            Switch(
                                checked = useGyroscope,
                                onCheckedChange = { useGyroscope = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color(0xFF00E5FF),
                                    checkedTrackColor = Color(0xFF00E5FF).copy(alpha = 0.3f),
                                    uncheckedThumbColor = Color(0xFF64748B),
                                    uncheckedTrackColor = Color(0xFF1E1E2E)
                                ),
                                modifier = Modifier.graphicsLayer(scaleX = 0.75f, scaleY = 0.75f)
                            )
                        }

                        // Touch pull well
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Touch Gravity", color = Color.White, fontSize = 12.sp)
                            Switch(
                                checked = useTouchWell,
                                onCheckedChange = { useTouchWell = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color(0xFF00E5FF),
                                    checkedTrackColor = Color(0xFF00E5FF).copy(alpha = 0.3f),
                                    uncheckedThumbColor = Color(0xFF64748B),
                                    uncheckedTrackColor = Color(0xFF1E1E2E)
                                ),
                                modifier = Modifier.graphicsLayer(scaleX = 0.75f, scaleY = 0.75f)
                            )
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    ControlSlider(
                        label = "Drop Count (Liquid Density)",
                        value = dropCount.toFloat(),
                        valueRange = 5f..32f,
                        onValueChange = { dropCount = it.toInt() },
                        displayValue = "$dropCount drops"
                    )

                    ControlSlider(
                        label = "Base Drop Radius",
                        value = baseRadius,
                        valueRange = 15f..50f,
                        onValueChange = { baseRadius = it },
                        displayValue = String.format("%.0f px", baseRadius)
                    )

                    ControlSlider(
                        label = "Surface Tension (Coalescence limit)",
                        value = surfaceTension,
                        valueRange = 0.32f..0.85f,
                        onValueChange = { surfaceTension = it },
                        displayValue = String.format("%.2f", surfaceTension)
                    )

                    ControlSlider(
                        label = "Viscosity (Damping friction)",
                        value = viscosity,
                        valueRange = 0.0f..0.12f,
                        onValueChange = { viscosity = it },
                        displayValue = String.format("%.2f", viscosity)
                    )

                    ControlSlider(
                        label = "Chrome Specular Highlights",
                        value = specularIntensity,
                        valueRange = 0.0f..1.5f,
                        onValueChange = { specularIntensity = it },
                        displayValue = String.format("%.1f", specularIntensity)
                    )

                    Spacer(Modifier.height(20.dp))
                }
            }
        }
    }
}

@Composable
private fun ControlSlider(
    label: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
    displayValue: String
) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, color = Color(0xFFCBD5E1), fontSize = 13.sp)
            Text(displayValue, color = Color(0xFF00E5FF), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            colors = SliderDefaults.colors(
                thumbColor = Color(0xFF00E5FF),
                activeTrackColor = Color(0xFF00E5FF),
                inactiveTrackColor = Color(0xFF1E1E2E)
            )
        )
    }
}
