package pro.jayeshseth.devcontrols.ui.visualfx

import android.content.Intent
import android.graphics.RuntimeShader
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import pro.jayeshseth.devcontrols.data.RainyWindowPrefs
import pro.jayeshseth.devcontrols.service.RainyWindowService

private const val RAIN_PREVIEW_AGSL = """
    uniform shader content;
    uniform float2 iResolution;
    uniform float iTime;
    uniform float rainIntensity;
    uniform float glassMisty;
    uniform float dropSpeed;
    uniform float refraction;

    float N21(float2 p) {
        p = fract(p * float2(123.34, 345.45));
        p += dot(p, p + 34.345);
        return fract(p.x * p.y);
    }

    half4 main(float2 fragCoord) {
        float2 uv = fragCoord / iResolution;
        float t = iTime * dropSpeed * 0.8;
        
        float2 dropDistortion = float2(0.0);
        float dropMask = 0.0;
        
        // Layer 1 (Large drops)
        float2 uv1 = uv * float2(5.0, 2.5);
        float2 id1 = floor(uv1);
        float2 grid1 = fract(uv1) - 0.5;
        float n1 = N21(id1);
        float t1 = t + n1 * 6.28;
        float x1 = (n1 - 0.5) * 0.7;
        float y1 = fract(t1);
        float2 p1 = float2(grid1.x - x1, grid1.y - (0.5 - y1));
        float d1 = length(p1 * float2(1.0, 2.0));
        float drop1 = smoothstep(0.12 * rainIntensity, 0.05 * rainIntensity, d1);
        
        // Layer 2 (Medium drops)
        float2 uv2 = uv * float2(8.0, 4.0) + float2(1.5, 0.7);
        float2 id2 = floor(uv2);
        float2 grid2 = fract(uv2) - 0.5;
        float n2 = N21(id2);
        float t2 = t * 1.3 + n2 * 6.28;
        float x2 = (n2 - 0.5) * 0.75;
        float y2 = fract(t2);
        float2 p2 = float2(grid2.x - x2, grid2.y - (0.5 - y2));
        float d2 = length(p2 * float2(1.0, 2.0));
        float drop2 = smoothstep(0.08 * rainIntensity, 0.03 * rainIntensity, d2);
        
        // Accumulate distortion
        if (drop1 > 0.0) {
            dropDistortion = p1 * drop1 * refraction * 0.25;
            dropMask = drop1;
        } else if (drop2 > 0.0) {
            dropDistortion = p2 * drop2 * refraction * 0.18;
            dropMask = drop2;
        }
        
        float2 sampleUv = uv + dropDistortion;
        half4 col = content.eval(clamp(sampleUv, 0.0, 1.0) * iResolution);
        
        // Apply misty screen effect (vignette)
        float2 cc = uv - 0.5;
        float r2 = dot(cc, cc);
        half4 mistColor = half4(0.15, 0.18, 0.23, 1.0);
        float mistAmount = glassMisty * 0.35 * (0.4 + r2 * 0.8);
        mistAmount *= (1.0 - dropMask);
        
        col = mix(col, mistColor, mistAmount);
        
        if (dropMask > 0.0) {
            col.rgb += half3(0.08 * dropMask);
        }
        
        return col;
    }
"""

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RainyWindowScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs = remember { RainyWindowPrefs.get(context) }

    val overlayEnabledPref by prefs.overlayEnabled.collectAsState(initial = false)

    var rainIntensity by remember { mutableStateOf(0.5f) }
    var glassMisty by remember { mutableStateOf(0.3f) }
    var dropSpeed by remember { mutableStateOf(1.0f) }
    var refraction by remember { mutableStateOf(0.2f) }

    LaunchedEffect(Unit) {
        prefs.rainIntensity.collectLatest { rainIntensity = it }
    }
    LaunchedEffect(Unit) {
        prefs.glassMisty.collectLatest { glassMisty = it }
    }
    LaunchedEffect(Unit) {
        prefs.dropSpeed.collectLatest { dropSpeed = it }
    }
    LaunchedEffect(Unit) {
        prefs.refraction.collectLatest { refraction = it }
    }

    var time by remember { mutableStateOf(0f) }
    LaunchedEffect(Unit) {
        var start = 0L
        while (true) {
            withFrameNanos {
                if (start == 0L) start = it
                time = (it - start) / 1e9f
            }
        }
    }

    var isOverlayGranted by remember { mutableStateOf(Settings.canDrawOverlays(context)) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                isOverlayGranted = Settings.canDrawOverlays(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(overlayEnabledPref, isOverlayGranted) {
        if (overlayEnabledPref && isOverlayGranted) {
            val intent = Intent(context, RainyWindowService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        } else {
            val intent = Intent(context, RainyWindowService::class.java)
            context.stopService(intent)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Rainy Window Lab", style = MaterialTheme.typography.titleMedium, color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Text("< Back", color = Color(0xFF00E5FF), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0B0F19))
            )
        },
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .background(Color(0xFF0B0F19))
        ) {
            // 1. Interactive Preview Composable
            val shader = remember { RuntimeShader(RAIN_PREVIEW_AGSL) }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.48f)
                    .padding(8.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .border(2.dp, Color(0xFF2C2C3E), RoundedCornerShape(12.dp))
                    .graphicsLayer {
                        shader.setFloatUniform("iResolution", size.width, size.height)
                        shader.setFloatUniform("iTime", time)
                        shader.setFloatUniform("rainIntensity", rainIntensity)
                        shader.setFloatUniform("glassMisty", glassMisty)
                        shader.setFloatUniform("dropSpeed", dropSpeed)
                        shader.setFloatUniform("refraction", refraction)

                        renderEffect = android.graphics.RenderEffect
                            .createRuntimeShaderEffect(shader, "content")
                            .asComposeRenderEffect()
                    }
            ) {
                // Procedural Cozy Rainy Day Scene backdrop
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color(0xFF0F172A), Color(0xFF1E1B4B), Color(0xFF311042))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text("Cozy Evening Cabin", color = Color(0xFFFFB74D), fontSize = 22.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        Text("Water drops will warp and refract this text", color = Color(0xFF94A3B8), fontSize = 11.sp)
                        Text("Simulating raindrops sliding down glass", color = Color(0xFF94A3B8), fontSize = 11.sp)
                        Spacer(Modifier.height(20.dp))
                        
                        // Cozy glowing lamp silhouette
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(RoundedCornerShape(25.dp))
                                .background(
                                    Brush.radialGradient(
                                        colors = listOf(Color(0xFFFFEE58), Color(0xFFFFEE58).copy(alpha = 0f))
                                    )
                                )
                        )
                    }
                }
            }

            // 2. Control Panel Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.52f)
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
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF161622)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2C2C3E))
                    ) {
                        Column(Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text("OS-Wide Rain Overlay", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text(
                                        if (isOverlayGranted) "Overlay permission granted" else "Overlay permission required",
                                        color = if (isOverlayGranted) Color(0xFF00FF00) else Color(0xFFFF5252),
                                        fontSize = 11.sp
                                    )
                                }
                                if (!isOverlayGranted) {
                                    Button(
                                        onClick = {
                                            val intent = Intent(
                                                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                                Uri.parse("package:${context.packageName}")
                                            )
                                            context.startActivity(intent)
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF))
                                    ) {
                                        Text("Grant", color = Color.Black, fontSize = 11.sp)
                                    }
                                } else {
                                    Switch(
                                        checked = overlayEnabledPref,
                                        onCheckedChange = { enabled ->
                                            scope.launch { prefs.setOverlayEnabled(enabled) }
                                        },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = Color(0xFF00FF00),
                                            checkedTrackColor = Color(0xFF00FF00).copy(alpha = 0.3f)
                                        )
                                    )
                                }
                            }
                        }
                    }

                    Text("Aesthetic Parameters", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(16.dp))

                    ControlSlider(
                        label = "Rain Intensity (Droplets Count)",
                        value = rainIntensity,
                        valueRange = 0.0f..1.5f,
                        onValueChange = {
                            rainIntensity = it
                            scope.launch { prefs.setRainIntensity(it) }
                        },
                        displayValue = String.format("%.1f", rainIntensity)
                    )

                    ControlSlider(
                        label = "Glass Mist Opacity",
                        value = glassMisty,
                        valueRange = 0.0f..1.0f,
                        onValueChange = {
                            glassMisty = it
                            scope.launch { prefs.setGlassMisty(it) }
                        },
                        displayValue = String.format("%.1f", glassMisty)
                    )

                    ControlSlider(
                        label = "Drop Slide Speed",
                        value = dropSpeed,
                        valueRange = 0.1f..3.0f,
                        onValueChange = {
                            dropSpeed = it
                            scope.launch { prefs.setDropSpeed(it) }
                        },
                        displayValue = String.format("%.1f", dropSpeed)
                    )

                    ControlSlider(
                        label = "Refraction Distortion (Preview only)",
                        value = refraction,
                        valueRange = 0.0f..1.0f,
                        onValueChange = {
                            refraction = it
                            scope.launch { prefs.setRefraction(it) }
                        },
                        displayValue = String.format("%.2f", refraction)
                    )
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
    Column(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, color = Color(0xFF94A3B8), fontSize = 11.sp)
            Text(displayValue, color = Color(0xFF00E5FF), fontSize = 11.sp, fontWeight = FontWeight.Bold)
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
