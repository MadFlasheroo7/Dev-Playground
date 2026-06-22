package pro.jayeshseth.devcontrols.ui.visualfx

import android.content.Intent
import android.graphics.RuntimeShader
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import android.content.Context
import android.media.projection.MediaProjectionManager
import pro.jayeshseth.devcontrols.data.RetroShaderPrefs
import pro.jayeshseth.devcontrols.service.RetroShaderService
import kotlin.random.Random

// CRT Shader with barrel distortion, scanlines, chromatic aberration, tracking noise, and phosphor tint / GameBoy dot matrix simulation
private const val CRT_AGSL = """
uniform shader content;
uniform float2 iResolution;
uniform float iTime;
uniform float curvature;
uniform float scanlineIntensity;
uniform float scanlineFreq;
uniform float rgbOffset;
uniform float glitchIntensity;
uniform float phosphorMode; // 0.0 = Color, 1.0 = Green, 2.0 = Amber, 3.0 = GameBoy

float rand(float x) {
    return fract(sin(x) * 43758.5453);
}

half4 main(float2 fragCoord) {
    float2 uv = fragCoord / iResolution;
    float2 cc = uv - 0.5;
    float r2 = dot(cc, cc);
    
    // 1. Curvature / Barrel Distortion
    uv = 0.5 + cc * (1.0 + curvature * r2 * 1.5);
    
    // Bounds Cropping
    if (uv.x < 0.0 || uv.x > 1.0 || uv.y < 0.0 || uv.y > 1.0) {
        return half4(0.0, 0.0, 0.0, 1.0);
    }
    
    // 2. Glitch horizontal tracking offset
    float jitterTime = floor(iTime * 15.0);
    float jitterChance = rand(jitterTime);
    float jitter = 0.0;
    if (jitterChance < 0.18 * glitchIntensity) {
        float lineY = rand(jitterTime + 1.0);
        if (abs(uv.y - lineY) < 0.07) {
            jitter = (rand(jitterTime + 2.0) - 0.5) * 0.04 * glitchIntensity;
        }
    }
    
    // 3. Chromatic Aberration sampling
    float2 uvR = uv + float2((rgbOffset + jitter * iResolution.x) / iResolution.x, 0.0);
    float2 uvG = uv + float2(jitter, 0.0);
    float2 uvB = uv - float2((rgbOffset - jitter * iResolution.x) / iResolution.x, 0.0);
    
    // Sample channels
    half r = content.eval(clamp(uvR, 0.0, 1.0) * iResolution).r;
    half g = content.eval(clamp(uvG, 0.0, 1.0) * iResolution).g;
    half b = content.eval(clamp(uvB, 0.0, 1.0) * iResolution).b;
    
    half3 col = half3(r, g, b);
    
    // 4. Scanlines
    float scanline = sin(uv.y * iResolution.y * scanlineFreq * 0.35 + iTime * 6.0);
    float sFactor = 1.0 - (scanlineIntensity * 0.4 * (scanline * 0.5 + 0.5));
    col *= half(sFactor);
    
    // 5. Retro Vignette
    col *= half(smoothstep(0.85, 0.35, r2));
    
    // 6. Phosphor tinting / GameBoy Grid
    if (phosphorMode > 0.5 && phosphorMode < 1.5) { // Green Screen
        float luma = dot(col, half3(0.299, 0.587, 0.114));
        col = half3(0.0, luma * 1.25, 0.0);
    } else if (phosphorMode > 1.5 && phosphorMode < 2.5) { // Amber Screen
        float luma = dot(col, half3(0.299, 0.587, 0.114));
        col = half3(luma * 1.3, luma * 0.75, 0.0);
    } else if (phosphorMode > 2.5) { // GameBoy Green
        float luma = dot(col, half3(0.299, 0.587, 0.114));
        half3 gbTint = half3(0.55, 0.68, 0.21);
        col = mix(gbTint * 0.2, gbTint * 1.3, luma);
        
        float2 grid = fract(fragCoord / 4.0);
        if (grid.x < 0.15 || grid.y < 0.15) {
            col = half3(0.18, 0.22, 0.08); // Dark olive grid separator
        }
    }
    
    // 7. Video Static Noise
    float noise = rand(uv.x * uv.y * iTime);
    col += half(noise * 0.06 * glitchIntensity);
    
    return half4(col, 1.0);
}
"""

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CrtScreen(onBack: () -> Unit) {
    // Check API availability
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("CRT TV & VHS Glitch", color = Color.White) },
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
                    "This visual laboratory requires Android 13 (API level 33) or higher for AGSL Post-Processing shaders.",
                    color = Color.White,
                    fontSize = 16.sp
                )
            }
        }
        return
    }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val overlayPrefs = remember { RetroShaderPrefs.get(context) }
    val overlayEnabledPref by overlayPrefs.overlayEnabled.collectAsState(initial = false)
    val warpEnabledPref by overlayPrefs.warpEnabled.collectAsState(initial = false)

    val projectionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        android.util.Log.d("CrtScreen", "projectionLauncher returned: resultCode=${result.resultCode}, data=${result.data != null}")
        if (result.resultCode == android.app.Activity.RESULT_OK && result.data != null) {
            scope.launch { overlayPrefs.setWarpEnabled(true) }
            val intent = Intent(context, RetroShaderService::class.java).apply {
                putExtra("enable_warp", true)
                putExtra("result_code", result.resultCode)
                putExtra("projection_data", result.data)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        } else {
            scope.launch { overlayPrefs.setWarpEnabled(false) }
        }
    }

    // Shader Uniform State variables
    var curvature by remember { mutableFloatStateOf(0.12f) }
    var scanlineIntensity by remember { mutableFloatStateOf(0.6f) }
    var scanlineFreq by remember { mutableFloatStateOf(2.2f) }
    var rgbOffset by remember { mutableFloatStateOf(2.5f) }
    var glitchIntensity by remember { mutableFloatStateOf(0.3f) }
    var phosphorMode by remember { mutableFloatStateOf(0f) } // 0=color, 1=green, 2=amber, 3=gameboy

    LaunchedEffect(Unit) {
        overlayPrefs.setWarpEnabled(false)
        overlayPrefs.setOverlayEnabled(false) // Start fresh
        overlayPrefs.curvature.collectLatest { curvature = it }
    }
    LaunchedEffect(Unit) {
        overlayPrefs.scanlineIntensity.collectLatest { scanlineIntensity = it }
    }
    LaunchedEffect(Unit) {
        overlayPrefs.noiseIntensity.collectLatest { glitchIntensity = it }
    }
    LaunchedEffect(Unit) {
        overlayPrefs.phosphorMode.collectLatest { phosphorMode = it }
    }

    // Frame clock for shader animation
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

    // Simulate temporary glitch trigger
    var isGlitching by remember { mutableStateOf(false) }
    LaunchedEffect(isGlitching) {
        if (isGlitching) {
            val originalGlitch = glitchIntensity
            glitchIntensity = 1.0f
            delay(500)
            glitchIntensity = originalGlitch
            isGlitching = false
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
        android.util.Log.d("CrtScreen", "LaunchedEffect triggered: overlayEnabledPref=$overlayEnabledPref, isOverlayGranted=$isOverlayGranted")
        if (overlayEnabledPref && isOverlayGranted) {
            val intent = Intent(context, RetroShaderService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        } else {
            val intent = Intent(context, RetroShaderService::class.java)
            context.stopService(intent)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Retro CRT & VHS Lab", style = MaterialTheme.typography.titleMedium, color = Color.White) },
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
            // 1. Post-processed UI Frame Viewport (Top 48% height)
            val shader = remember { RuntimeShader(CRT_AGSL) }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.48f)
                    .padding(8.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .border(2.dp, Color(0xFF2C2C3E), RoundedCornerShape(12.dp))
                    .graphicsLayer {
                        // Bind AGSL uniforms dynamically
                        shader.setFloatUniform("iResolution", size.width, size.height)
                        shader.setFloatUniform("iTime", time)
                        shader.setFloatUniform("curvature", curvature)
                        shader.setFloatUniform("scanlineIntensity", scanlineIntensity)
                        shader.setFloatUniform("scanlineFreq", scanlineFreq)
                        shader.setFloatUniform("rgbOffset", rgbOffset)
                        shader.setFloatUniform("glitchIntensity", glitchIntensity)
                        shader.setFloatUniform("phosphorMode", phosphorMode)
                        
                        renderEffect = android.graphics.RenderEffect
                            .createRuntimeShaderEffect(shader, "content")
                            .asComposeRenderEffect()
                    }
            ) {
                // Interactive Retro Terminal App inside
                RetroTerminalApp()
            }

            // 2. Clear Settings Dashboard (Bottom 52% height)
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
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Post-Processing Controls", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Button(
                            onClick = { isGlitching = true },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF007F)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Simulate Glitch ⚡", color = Color.White, fontSize = 12.sp)
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    // OS-Wide overlay control card
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
                                    Text("OS-Wide Retro Overlay", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
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
                                            scope.launch { overlayPrefs.setOverlayEnabled(enabled) }
                                        },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = Color(0xFF00FF00),
                                            checkedTrackColor = Color(0xFF00FF00).copy(alpha = 0.3f)
                                        )
                                    )
                                }
                            }
                            if (isOverlayGranted && overlayEnabledPref) {
                                Spacer(Modifier.height(12.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(Modifier.weight(1f)) {
                                        Text("Warp OS Screen (Experimental)", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        Text(
                                            "Distorts the entire screen. Requires screen recording permission (auto-granted if rooted).",
                                            color = Color(0xFF94A3B8),
                                            fontSize = 11.sp
                                        )
                                    }
                                    Switch(
                                        checked = warpEnabledPref,
                                        onCheckedChange = { enabled ->
                                            android.util.Log.d("CrtScreen", "Warp switch changed to $enabled")
                                            if (enabled) {
                                                scope.launch(Dispatchers.IO) {
                                                    try {
                                                        Runtime.getRuntime().exec("su -c cmd appops set ${context.packageName} PROJECT_MEDIA allow")
                                                    } catch (e: Exception) {
                                                        // Fallback to standard flow
                                                    }
                                                }
                                                val projectionManager = context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
                                                projectionLauncher.launch(projectionManager.createScreenCaptureIntent())
                                            } else {
                                                scope.launch { overlayPrefs.setWarpEnabled(false) }
                                                val intent = Intent(context, RetroShaderService::class.java).apply {
                                                    putExtra("enable_warp", false)
                                                }
                                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                                    context.startForegroundService(intent)
                                                } else {
                                                    context.startService(intent)
                                                }
                                            }
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

                    // Phosphor Selectors
                    Text("Phosphor Display Tint", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            Triple("Full Color", 0f, Color(0xFF00E5FF)),
                            Triple("Green", 1f, Color(0xFF00FF00)),
                            Triple("Amber", 2f, Color(0xFFFF9100)),
                            Triple("GameBoy", 3f, Color(0xFF8BAC0F))
                        ).forEach { (label, value, tintColor) ->
                            val isSel = phosphorMode == value
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSel) Color(0xFF1E1E30) else Color(0xFF0D0D18))
                                    .border(
                                        width = 1.dp,
                                        color = if (isSel) tintColor else Color(0xFF1E1E2E),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .clickable {
                                        phosphorMode = value
                                        scope.launch { overlayPrefs.setPhosphorMode(value) }
                                    }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 11.sp,
                                    color = if (isSel) tintColor else Color(0xFF64748B),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    ControlSlider(
                        label = "Curvature (Glass Tube Distortion)",
                        value = curvature,
                        valueRange = 0.0f..0.35f,
                        onValueChange = {
                            curvature = it
                            scope.launch { overlayPrefs.setCurvature(it) }
                        },
                        displayValue = String.format("%.2f", curvature)
                    )

                    ControlSlider(
                        label = "Scanline Darkness",
                        value = scanlineIntensity,
                        valueRange = 0.0f..1.0f,
                        onValueChange = {
                            scanlineIntensity = it
                            scope.launch { overlayPrefs.setScanlineIntensity(it) }
                        },
                        displayValue = String.format("%.1f", scanlineIntensity)
                    )

                    ControlSlider(
                        label = "Scanline Grid Density",
                        value = scanlineFreq,
                        valueRange = 1.0f..4.0f,
                        onValueChange = { scanlineFreq = it },
                        displayValue = String.format("%.1f", scanlineFreq)
                    )

                    ControlSlider(
                        label = "RGB Chromatic Aberration Offset",
                        value = rgbOffset,
                        valueRange = 0.0f..7.0f,
                        onValueChange = { rgbOffset = it },
                        displayValue = String.format("%.1f px", rgbOffset)
                    )

                    ControlSlider(
                        label = "Glitch static / VHS Jitter",
                        value = glitchIntensity,
                        valueRange = 0.0f..1.0f,
                        onValueChange = {
                            glitchIntensity = it
                            scope.launch { overlayPrefs.setNoiseIntensity(it) }
                        },
                        displayValue = String.format("%.1f", glitchIntensity)
                    )

                    Spacer(Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
private fun RetroTerminalApp() {
    var logs = remember { mutableStateListOf("RETRO TERMINAL INIT...", "READY FOR SECURE SHELL INTERFACE.") }
    var isHacking by remember { mutableStateOf(false) }
    var switchVal by remember { mutableStateOf(true) }

    LaunchedEffect(isHacking) {
        if (isHacking) {
            logs.add("> Initializing systems bypass...")
            delay(400)
            logs.add("> Penetrating firewalls...")
            delay(500)
            logs.add("> Downloading core database...")
            delay(600)
            logs.add("> COMPLETED. Access code: 0x992B")
            delay(400)
            isHacking = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF08080C))
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "SYSTEM TERMINAL v8.4",
                color = Color(0xFF00FF00),
                fontFamily = FontFamily.Monospace,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("SECURE", color = Color(0xFF64748B), fontFamily = FontFamily.Monospace, fontSize = 10.sp)
                Spacer(Modifier.width(6.dp))
                Switch(
                    checked = switchVal,
                    onCheckedChange = { switchVal = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color(0xFF00FF00),
                        checkedTrackColor = Color(0xFF00FF00).copy(alpha = 0.3f),
                        uncheckedThumbColor = Color(0xFF334155),
                        uncheckedTrackColor = Color(0xFF1E1E2A)
                    ),
                    modifier = Modifier.scale(0.7f)
                )
            }
        }

        Spacer(Modifier.height(10.dp))

        // Diagnostic Console Output Box
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .border(1.dp, Color(0xFF1E1E2E), RoundedCornerShape(8.dp)),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF040406))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                logs.forEach { log ->
                    Text(
                        text = log,
                        color = if (log.contains("COMPLETED")) Color(0xFF00FF00) else Color(0xFF38BDF8),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                }
                
                if (isHacking) {
                    Spacer(Modifier.height(8.dp))
                    CircularProgressIndicator(
                        color = Color(0xFF00FF00),
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // Action Trigger Button
        Button(
            onClick = {
                if (!isHacking) {
                    isHacking = true
                }
            },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FF00)),
            shape = RoundedCornerShape(6.dp),
            modifier = Modifier.fillMaxWidth(),
            enabled = !isHacking
        ) {
            Text(
                text = if (isHacking) "BYPASSING..." else "EXECUTE SECURE BYPASS",
                color = Color.Black,
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

// Utility extension to scale Compose elements
@Composable
private fun Modifier.scale(scale: Float): Modifier = this.then(
    Modifier.graphicsLayer(scaleX = scale, scaleY = scale)
)

@Composable
private fun ControlSlider(
    label: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
    displayValue: String
) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
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
