package pro.jayeshseth.devcontrols.ui.visualfx

import android.content.Context
import android.graphics.RuntimeShader
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.cos
import kotlin.math.sin

// Holographic Specular Iridescence & Glitter AGSL Shader
private const val HOLO_AGSL = """
uniform shader content;
uniform float2 iResolution;
uniform float2 tilt; // Current tilt vectors in [-1.0, 1.0]
uniform float sheenIntensity;
uniform float sparkleFreq;
uniform float iTime;

float hash(float2 p) {
    p = fract(p * float2(127.1, 311.7));
    return fract(sin(dot(p, float2(12.9898, 78.233))) * 43758.5453);
}

half4 main(float2 fragCoord) {
    float2 uv = fragCoord / iResolution;
    
    // Diagonal iridescent sheen sweeping with tilt offsets
    float d = (uv.x + uv.y) * 0.5 - tilt.x * 0.4 + tilt.y * 0.3;
    float band = d * 3.5 + tilt.x * 1.2;
    
    // Iridescent Rainbow color vector
    half3 rainbow = half3(0.5) + half3(0.5) * half3(cos(6.28318 * (band + float3(0.0, 0.33, 0.67))));
    
    // Dynamic specular sheen sweeping line
    float sheen = smoothstep(0.35, 0.0, abs(fract(d * 1.6 - tilt.y * 0.7) - 0.5));
    
    // Fine glittering sparkle
    float spkVal = hash(floor(uv * 200.0));
    float sparkle = step(1.0 - 0.012 * sparkleFreq, spkVal) * (0.5 + 0.5 * sin(iTime * 14.0 + spkVal * 6.28));
    
    half4 baseColor = content.eval(fragCoord);
    
    // Apply metallic foil sheen & sparkle overlays
    half3 finalColor = baseColor.rgb + 
                       rainbow * half(sheenIntensity * 0.38) + 
                       half(sheen * sheenIntensity * 0.32) * half3(1.0) + 
                       half(sparkle * sheenIntensity);
                       
    return half4(finalColor, baseColor.a);
}
"""

private enum class CardTheme(val title: String, val rarity: String, val serial: String) {
    COSMIC_NEBULA("NEBULA WARDEN", "ULTRA RARE", "#042/100"),
    CYBER_DRAGON("CYBER DRAGON", "SECRET RARE", "#009/100"),
    RETRO_GAMER("ARCADE MASTER", "PROMO EDITION", "#088/100")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HolographicCardScreen(onBack: () -> Unit) {
    val context = LocalContext.current

    // Aesthetic Config dials
    var maxRotation by remember { mutableFloatStateOf(16f) }
    var sheenIntensity by remember { mutableFloatStateOf(0.7f) }
    var sparkleFreq by remember { mutableFloatStateOf(0.6f) }
    var useGyroscope by remember { mutableStateOf(false) }
    var activeTheme by remember { mutableStateOf(CardTheme.COSMIC_NEBULA) }

    // Tilt state values (normalized range -1.0f to 1.0f)
    var tiltX by remember { mutableFloatStateOf(0f) }
    var tiltY by remember { mutableFloatStateOf(0f) }

    // Handle Drag gestures for manual tilt
    var dragOffset by remember { mutableStateOf(Offset.Zero) }
    var cardSize by remember { mutableStateOf(IntSize.Zero) }

    // Gyroscope/Accelerometer Sensor Integration
    DisposableEffect(useGyroscope) {
        if (!useGyroscope) return@DisposableEffect onDispose {}
        
        val sm = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val sensor = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                // Map sensor coordinate ranges to range [-1.0, 1.0]
                val rawX = -event.values[0] / 9.81f
                val rawY = event.values[1] / 9.81f
                
                // Dampen sensor readings using a low pass filter
                tiltX = tiltX * 0.82f + rawX.coerceIn(-1f, 1f) * 0.18f
                tiltY = tiltY * 0.82f + rawY.coerceIn(-1f, 1f) * 0.18f
            }
            override fun onAccuracyChanged(s: Sensor?, a: Int) {}
        }
        sm.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_GAME)
        onDispose { sm.unregisterListener(listener) }
    }

    // Handle back-to-zero tilt animation when drag ends
    LaunchedEffect(dragOffset, useGyroscope) {
        if (useGyroscope) return@LaunchedEffect
        if (dragOffset == Offset.Zero) {
            // Smoothly animate back to center
            var elapsed = 0
            val originalX = tiltX
            val originalY = tiltY
            while (elapsed < 20) {
                withFrameNanos {
                    tiltX = originalX * (1f - elapsed / 20f)
                    tiltY = originalY * (1f - elapsed / 20f)
                }
                elapsed++
            }
            tiltX = 0f
            tiltY = 0f
        }
    }

    // Time clock uniform for shader
    var time by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(Unit) {
        var start = 0L
        while (true) {
            withFrameNanos {
                if (start == 0L) start = it
                time = (it - start) / 1e9f
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("3D Holographic Card", style = MaterialTheme.typography.titleMedium, color = Color.White) },
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
            // Viewport area containing card (54% height)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.54f)
                    .onSizeChanged { cardSize = it }
                    .pointerInput(useGyroscope) {
                        if (useGyroscope) return@pointerInput
                        detectDragGestures(
                            onDragStart = { dragOffset = Offset.Zero },
                            onDragEnd = { dragOffset = Offset.Zero },
                            onDragCancel = { dragOffset = Offset.Zero },
                            onDrag = { change, dragAmount ->
                                dragOffset += dragAmount
                                val cx = size.width / 2f
                                val cy = size.height / 2f
                                // Normalize drag offset coordinates
                                tiltX = (dragOffset.x / cx).coerceIn(-1f, 1f)
                                tiltY = (dragOffset.y / cy).coerceIn(-1f, 1f)
                            }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                    Text("Needs Android 13 (API 33)+ for AGSL foil shaders.", color = Color.White)
                    return@Box
                }

                val shader = remember { RuntimeShader(HOLO_AGSL) }
                
                // Holographic card stack rotated in 3D
                Box(
                    modifier = Modifier
                        .fillMaxHeight(0.85f)
                        .aspectRatio(0.66f)
                        .graphicsLayer {
                            // Apply tilt rotation and depth projection
                            rotationY = tiltX * maxRotation
                            rotationX = -tiltY * maxRotation
                            cameraDistance = 18f * density
                            clip = true
                            shape = RoundedCornerShape(18.dp)
                            
                            // Apply AGSL metallic sheen Shader overlay
                            shader.setFloatUniform("iResolution", size.width, size.height)
                            shader.setFloatUniform("tilt", tiltX, tiltY)
                            shader.setFloatUniform("sheenIntensity", sheenIntensity)
                            shader.setFloatUniform("sparkleFreq", sparkleFreq)
                            shader.setFloatUniform("iTime", time)
                            
                            renderEffect = android.graphics.RenderEffect
                                .createRuntimeShaderEffect(shader, "content")
                                .asComposeRenderEffect()
                        }
                        .background(Color(0xFF08080C))
                ) {
                    // Card Visual Layers (True Parallax Depth Shift)
                    
                    // 1. Background layer (Shifts opposite to tilt)
                    val bgOffsetX = (-tiltX * 22f).toInt()
                    val bgOffsetY = (-tiltY * 22f).toInt()
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .offset { IntOffset(bgOffsetX, bgOffsetY) }
                    ) {
                        CardBackground(theme = activeTheme)
                    }

                    // 2. Character / Subject layer (Shifts towards tilt)
                    val subOffsetX = (tiltX * 10f).toInt()
                    val subOffsetY = (tiltY * 10f).toInt()
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .offset { IntOffset(subOffsetX, subOffsetY) }
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CardSubject(theme = activeTheme)
                    }

                    // 3. Frame & Text labels layer (Fixed / shifts minimally)
                    val frameOffsetX = (tiltX * 2.5f).toInt()
                    val frameOffsetY = (tiltY * 2.5f).toInt()
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .offset { IntOffset(frameOffsetX, frameOffsetY) }
                            .padding(14.dp)
                    ) {
                        CardFrameOverlay(theme = activeTheme)
                    }
                }
            }

            // Controls Dashboard (46% height)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.46f)
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
                    Text("Collectible Customization", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(Modifier.height(14.dp))

                    // Tilt control source switcher
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Gyroscope Tilt Sensor", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                            Text("Tilt physical device vs drag finger", color = Color(0xFF64748B), fontSize = 11.sp)
                        }
                        Switch(
                            checked = useGyroscope,
                            onCheckedChange = { useGyroscope = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color(0xFF00E5FF),
                                checkedTrackColor = Color(0xFF00E5FF).copy(alpha = 0.3f),
                                uncheckedThumbColor = Color(0xFF64748B),
                                uncheckedTrackColor = Color(0xFF1E1E2E)
                            )
                        )
                    }

                    Spacer(Modifier.height(16.dp))

                    // Card Themes
                    Text("Select Card Theme", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CardTheme.values().forEach { theme ->
                            val isSel = activeTheme == theme
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSel) Color(0xFF1E1E30) else Color(0xFF0D0D18))
                                    .border(
                                        width = 1.dp,
                                        color = if (isSel) Color(0xFF00E5FF) else Color(0xFF1E1E2E),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .clickable { activeTheme = theme }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = theme.title.split(" ").last(),
                                    fontSize = 11.sp,
                                    color = if (isSel) Color(0xFF00E5FF) else Color(0xFF64748B),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    ControlSlider(
                        label = "Foil Sheen Iridescence",
                        value = sheenIntensity,
                        valueRange = 0.0f..1.0f,
                        onValueChange = { sheenIntensity = it },
                        displayValue = String.format("%.1f", sheenIntensity)
                    )

                    ControlSlider(
                        label = "Foil Sparkle Frequency",
                        value = sparkleFreq,
                        valueRange = 0.0f..1.0f,
                        onValueChange = { sparkleFreq = it },
                        displayValue = String.format("%.1f", sparkleFreq)
                    )

                    ControlSlider(
                        label = "Max 3D Perspective Tilt",
                        value = maxRotation,
                        valueRange = 5f..30f,
                        onValueChange = { maxRotation = it },
                        displayValue = String.format("%.0f deg", maxRotation)
                    )

                    Spacer(Modifier.height(20.dp))
                }
            }
        }
    }
}

// Background layer rendering depending on card theme
@Composable
private fun CardBackground(theme: CardTheme) {
    val r = remember { java.util.Random(42L) }
    when (theme) {
        CardTheme.COSMIC_NEBULA -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(Color(0xFF6A1B9A), Color(0xFF1A0033), Color(0xFF020208)),
                            radius = 600f
                        )
                    )
            ) {
                // Nebula Stars
                Canvas(Modifier.fillMaxSize()) {
                    repeat(120) {
                        drawCircle(
                            color = Color.White.copy(alpha = r.nextFloat() * 0.7f),
                            radius = r.nextFloat() * 2.5f + 0.8f,
                            center = Offset(r.nextFloat() * size.width, r.nextFloat() * size.height)
                        )
                    }
                }
            }
        }
        CardTheme.CYBER_DRAGON -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF0A0B10))
            ) {
                // Futuristic Hex Grid
                Canvas(Modifier.fillMaxSize()) {
                    val spacing = 28f
                    val gridColor = Color(0xFF00E5FF).copy(alpha = 0.08f)
                    var y = 0f
                    while (y < size.height) {
                        drawLine(gridColor, Offset(0f, y), Offset(size.width, y), 1f)
                        y += spacing
                    }
                    var x = 0f
                    while (x < size.width) {
                        drawLine(gridColor, Offset(x, 0f), Offset(x, size.height), 1f)
                        x += spacing
                    }
                }
            }
        }
        CardTheme.RETRO_GAMER -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color(0xFF111827), Color(0xFF030712))
                        )
                    )
            ) {
                // Arcade Retro grid pattern
                Canvas(Modifier.fillMaxSize()) {
                    val step = 32f
                    var y = 0f
                    while (y < size.height) {
                        val colorVal = if (y % (step * 2) == 0f) Color(0xFFFFCC00).copy(alpha = 0.05f) else Color(0xFFFF3D00).copy(alpha = 0.05f)
                        drawLine(colorVal, Offset(0f, y), Offset(size.width, y), 2f)
                        y += step
                    }
                }
            }
        }
    }
}

// Subject layer rendering
@Composable
private fun CardSubject(theme: CardTheme) {
    when (theme) {
        CardTheme.COSMIC_NEBULA -> {
            // Draw floating planet orb
            Box(
                modifier = Modifier
                    .size(160.dp)
                    .clip(androidx.compose.foundation.shape.CircleShape)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(Color(0xFFE91E63), Color(0xFF9C27B0), Color(0xFF00E5FF))
                        )
                    )
                    .border(
                        2.dp,
                        Color(0xFF00E5FF).copy(alpha = 0.6f),
                        androidx.compose.foundation.shape.CircleShape
                    )
            ) {
                // Ring drawing on card subject
                Canvas(Modifier.fillMaxSize()) {
                    drawArc(
                        color = Color.White.copy(alpha = 0.35f),
                        startAngle = 180f,
                        sweepAngle = 180f,
                        useCenter = false,
                        style = Stroke(width = 8f)
                    )
                }
            }
        }
        CardTheme.CYBER_DRAGON -> {
            // Futuristic holographic vector shape
            Canvas(Modifier.size(150.dp)) {
                val path = androidx.compose.ui.graphics.Path().apply {
                    moveTo(size.width / 2f, 0f)
                    lineTo(size.width, size.height * 0.35f)
                    lineTo(size.width * 0.75f, size.height)
                    lineTo(size.width * 0.25f, size.height)
                    lineTo(0f, size.height * 0.35f)
                    close()
                }
                drawPath(
                    path = path,
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0xFF00E5FF), Color(0xFFFF007F))
                    )
                )
                drawPath(
                    path = path,
                    color = Color.White,
                    style = Stroke(width = 3f)
                )
            }
        }
        CardTheme.RETRO_GAMER -> {
            // Draw pixelated control pad vector
            Canvas(Modifier.size(140.dp)) {
                val cx = size.width / 2f
                val cy = size.height / 2f
                
                // Outer gameboy style casing
                drawRoundRect(
                    color = Color(0xFF374151),
                    topLeft = Offset(cx - 70f, cy - 40f),
                    size = androidx.compose.ui.geometry.Size(140f, 80f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(10f, 10f)
                )
                
                // Cross D-pad
                drawRect(
                    color = Color(0xFF111827),
                    topLeft = Offset(cx - 50f, cy - 25f),
                    size = androidx.compose.ui.geometry.Size(15f, 45f)
                )
                drawRect(
                    color = Color(0xFF111827),
                    topLeft = Offset(cx - 65f, cy - 10f),
                    size = androidx.compose.ui.geometry.Size(45f, 15f)
                )
                
                // Action Buttons A/B (Red)
                drawCircle(Color(0xFFEF4444), 10f, Offset(cx + 25f, cy + 5f))
                drawCircle(Color(0xFFEF4444), 10f, Offset(cx + 45f, cy - 5f))
            }
        }
    }
}

// Frame overlay layer rendering
@Composable
private fun CardFrameOverlay(theme: CardTheme) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .border(
                width = 2.dp,
                brush = Brush.linearGradient(
                    colors = when (theme) {
                        CardTheme.COSMIC_NEBULA -> listOf(Color(0xFFE91E63), Color(0xFF9C27B0))
                        CardTheme.CYBER_DRAGON -> listOf(Color(0xFF00E5FF), Color(0xFFFF007F))
                        CardTheme.RETRO_GAMER -> listOf(Color(0xFFFF9100), Color(0xFFFFEA00))
                    }
                ),
                shape = RoundedCornerShape(10.dp)
            )
            .padding(10.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Top Card metadata labels
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = theme.title,
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.SansSerif,
                letterSpacing = 1.2.sp
            )
            
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color.White.copy(alpha = 0.12f))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "LVL 99",
                    color = Color.White,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        // Bottom Card details labels
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = theme.rarity,
                    color = when (theme) {
                        CardTheme.COSMIC_NEBULA -> Color(0xFFE040FB)
                        CardTheme.CYBER_DRAGON -> Color(0xFF00E5FF)
                        CardTheme.RETRO_GAMER -> Color(0xFFFFEA00)
                    },
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.SansSerif
                )
                
                Text(
                    text = theme.serial,
                    color = Color(0xFF94A3B8),
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
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
