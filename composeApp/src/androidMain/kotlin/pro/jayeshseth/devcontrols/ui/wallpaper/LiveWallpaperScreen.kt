package pro.jayeshseth.devcontrols.ui.wallpaper

import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.widget.Toast
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import pro.jayeshseth.devcontrols.data.LiveWallpaperPrefs
import pro.jayeshseth.devcontrols.data.WallpaperSceneType
import pro.jayeshseth.devcontrols.wallpaper.LiveWallpaperRenderer
import pro.jayeshseth.devcontrols.wallpaper.LiveWallpaperService
import pro.jayeshseth.devcontrols.wallpaper.TouchRipple
import pro.jayeshseth.devcontrols.wallpaper.WallpaperRenderState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiveWallpaperScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs = remember { LiveWallpaperPrefs.get(context) }

    // Collect prefs
    val selectedScene by prefs.selectedScene.collectAsState(initial = WallpaperSceneType.DEEP_NEBULA)
    val gyroEnabled by prefs.gyroParallaxEnabled.collectAsState(initial = true)
    val gyroSensitivity by prefs.gyroSensitivity.collectAsState(initial = 1.0f)
    val touchEnabled by prefs.touchReactiveEnabled.collectAsState(initial = true)
    val touchIntensity by prefs.touchIntensity.collectAsState(initial = 1.0f)
    val launcherParallaxEnabled by prefs.launcherParallaxEnabled.collectAsState(initial = true)
    val targetFps by prefs.targetFps.collectAsState(initial = 60)
    val batteryThrottle by prefs.batterySaverThrottle.collectAsState(initial = true)
    val monetEnabled by prefs.monetThemingEnabled.collectAsState(initial = true)
    val particleDensity by prefs.particleDensity.collectAsState(initial = 1.0f)
    val animationSpeed by prefs.animationSpeed.collectAsState(initial = 1.0f)

    // Live preview state
    val renderer = remember { LiveWallpaperRenderer() }
    var elapsedSeconds by remember { mutableFloatStateOf(0f) }
    var fpsTelemetry by remember { mutableIntStateOf(60) }
    var frameCount by remember { mutableIntStateOf(0) }
    var lastFpsCalcTime by remember { mutableFloatStateOf(0f) }

    // Interactive preview tilt
    var previewTiltX by remember { mutableFloatStateOf(0f) }
    var previewTiltY by remember { mutableFloatStateOf(0f) }
    var simulatedLauncherOffset by remember { mutableFloatStateOf(0.5f) }
    val previewRipples = remember { mutableStateListOf<TouchRipple>() }

    // Sensor listener for preview
    DisposableEffect(gyroEnabled) {
        val sm = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val acc = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                if (gyroEnabled && event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
                    val rawX = (-event.values[0] / 9.8f).coerceIn(-1.5f, 1.5f)
                    val rawY = (event.values[1] / 9.8f).coerceIn(-1.5f, 1.5f)
                    previewTiltX = rawX * gyroSensitivity
                    previewTiltY = rawY * gyroSensitivity
                }
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }
        if (gyroEnabled && acc != null) {
            sm.registerListener(listener, acc, SensorManager.SENSOR_DELAY_GAME)
        }
        onDispose {
            sm.unregisterListener(listener)
        }
    }

    // Preview render ticker loop
    LaunchedEffect(Unit) {
        var lastNanos = 0L
        while (true) {
            withFrameNanos { now ->
                val dt = if (lastNanos > 0L) ((now - lastNanos) / 1_000_000_000f).coerceIn(0.001f, 0.1f) else 0.016f
                lastNanos = now
                elapsedSeconds += dt

                // Update preview ripples
                val it = previewRipples.iterator()
                while (it.hasNext()) {
                    val r = it.next()
                    r.age += dt
                    if (r.age >= r.maxAge) it.remove()
                }

                // FPS Telemetry
                frameCount++
                if (elapsedSeconds - lastFpsCalcTime >= 1.0f) {
                    fpsTelemetry = frameCount
                    frameCount = 0
                    lastFpsCalcTime = elapsedSeconds
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Live Wallpaper Engine",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "WallpaperService • Hardware Canvas • Monet",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Text(
                            text = "<",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // -----------------------------------------------------------------
            // 1. LIVE INTERACTIVE PREVIEW
            // -----------------------------------------------------------------
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(320.dp),
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(touchEnabled) {
                                if (touchEnabled) {
                                    detectTapGestures { offset ->
                                        if (previewRipples.size >= 10) previewRipples.removeAt(0)
                                        previewRipples.add(
                                            TouchRipple(
                                                x = offset.x,
                                                y = offset.y,
                                                color = Color(selectedScene.accentColorHex)
                                            )
                                        )
                                    }
                                }
                            }
                            .pointerInput(touchEnabled) {
                                if (touchEnabled) {
                                    detectDragGestures { change, _ ->
                                        change.consume()
                                        if (previewRipples.size < 8) {
                                            previewRipples.add(
                                                TouchRipple(
                                                    x = change.position.x,
                                                    y = change.position.y,
                                                    color = Color(selectedScene.accentColorHex)
                                                )
                                            )
                                        }
                                    }
                                }
                            }
                    ) {
                        val state = WallpaperRenderState(
                            scene = selectedScene,
                            timeSeconds = elapsedSeconds,
                            tiltX = previewTiltX,
                            tiltY = previewTiltY,
                            launcherOffset = simulatedLauncherOffset,
                            particleDensity = particleDensity,
                            animationSpeed = animationSpeed,
                            touchIntensity = touchIntensity,
                            activeRipples = previewRipples.toList()
                        )
                        renderer.render(this, state, 0.016f)
                    }

                    // Floating Live Preview Badge
                    Surface(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(12.dp),
                        color = Color.Black.copy(alpha = 0.65f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(Color(0xFF22C55E), CircleShape)
                            )
                            Text(
                                text = "Interactive Preview • Tap & Drag",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White
                            )
                        }
                    }

                    // Telemetry HUD in Bottom
                    Surface(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(12.dp),
                        color = Color.Black.copy(alpha = 0.75f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${fpsTelemetry} FPS",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF38BDF8),
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = if (gyroEnabled) "Gyro: Active" else "Gyro: Off",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (gyroEnabled) Color(0xFF4ADE80) else Color(0xFF94A3B8)
                            )
                        }
                    }
                }
            }

            // -----------------------------------------------------------------
            // 2. PRIMARY ACTION: SET AS SYSTEM WALLPAPER
            // -----------------------------------------------------------------
            Button(
                onClick = {
                    try {
                        val intent = Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER).apply {
                            putExtra(
                                WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
                                ComponentName(context, LiveWallpaperService::class.java)
                            )
                        }
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        // Fallback to general wallpaper chooser
                        try {
                            val chooserIntent = Intent(WallpaperManager.ACTION_LIVE_WALLPAPER_CHOOSER)
                            context.startActivity(chooserIntent)
                        } catch (err: Exception) {
                            Toast.makeText(context, "Could not launch wallpaper picker: ${err.message}", Toast.LENGTH_LONG).show()
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text("▶", fontSize = 16.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Apply as System Wallpaper",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }

            // -----------------------------------------------------------------
            // 3. SCENE SELECTOR
            // -----------------------------------------------------------------
            Text(
                text = "Interactive Scenes",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                WallpaperSceneType.entries.forEach { scene ->
                    val isSelected = selectedScene == scene
                    Card(
                        modifier = Modifier
                            .width(180.dp)
                            .clickable {
                                scope.launch { prefs.setSelectedScene(scene) }
                            },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected)
                                MaterialTheme.colorScheme.primaryContainer
                            else
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                        ),
                        border = if (isSelected)
                            androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                        else null
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = scene.icon,
                                    fontSize = 24.sp
                                )
                                Box(
                                    modifier = Modifier
                                        .size(14.dp)
                                        .background(Color(scene.accentColorHex), CircleShape)
                                )
                            }
                            Text(
                                text = scene.title,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = scene.subtitle,
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 2,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // -----------------------------------------------------------------
            // 4. MOTION & SENSOR TWEAKS
            // -----------------------------------------------------------------
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "Motion & Sensor Core",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )

                    // Gyro Parallax Switch
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Hardware Gyro Parallax", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                            Text("Tilts layers with device accelerometer", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = gyroEnabled,
                            onCheckedChange = { scope.launch { prefs.setGyroParallaxEnabled(it) } }
                        )
                    }

                    if (gyroEnabled) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Gyro Sensitivity", style = MaterialTheme.typography.bodySmall)
                                Text("${(gyroSensitivity * 100).toInt()}%", style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace)
                            }
                            Slider(
                                value = gyroSensitivity,
                                onValueChange = { scope.launch { prefs.setGyroSensitivity(it) } },
                                valueRange = 0.2f..2.5f
                            )
                        }
                    }

                    // Launcher Scroll Parallax
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Launcher Scroll Parallax", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                            Text("Shifts scene horizontally when swiping pages", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = launcherParallaxEnabled,
                            onCheckedChange = { scope.launch { prefs.setLauncherParallaxEnabled(it) } }
                        )
                    }

                    // Launcher offset preview test slider
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Preview Launcher Page Scroll", style = MaterialTheme.typography.bodySmall)
                            Text("${(simulatedLauncherOffset * 100).toInt()}%", style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace)
                        }
                        Slider(
                            value = simulatedLauncherOffset,
                            onValueChange = { simulatedLauncherOffset = it },
                            valueRange = 0.0f..1.0f
                        )
                    }

                    // Touch Reactivity Switch
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Home Screen Touch Ripples", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                            Text("Intercepts taps & drags behind home icons", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = touchEnabled,
                            onCheckedChange = { scope.launch { prefs.setTouchReactiveEnabled(it) } }
                        )
                    }

                    if (touchEnabled) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Touch Intensity", style = MaterialTheme.typography.bodySmall)
                                Text("${(touchIntensity * 100).toInt()}%", style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace)
                            }
                            Slider(
                                value = touchIntensity,
                                onValueChange = { scope.launch { prefs.setTouchIntensity(it) } },
                                valueRange = 0.2f..2.0f
                            )
                        }
                    }
                }
            }

            // -----------------------------------------------------------------
            // 5. SIMULATION & SPEED
            // -----------------------------------------------------------------
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "Simulation & Particle Tuning",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )

                    // Animation Speed
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Animation Speed", style = MaterialTheme.typography.bodySmall)
                            Text("${(animationSpeed * 100).toInt()}%", style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace)
                        }
                        Slider(
                            value = animationSpeed,
                            onValueChange = { scope.launch { prefs.setAnimationSpeed(it) } },
                            valueRange = 0.3f..2.5f
                        )
                    }

                    // Particle Density
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Particle / Star Density", style = MaterialTheme.typography.bodySmall)
                            Text("${(particleDensity * 100).toInt()}%", style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace)
                        }
                        Slider(
                            value = particleDensity,
                            onValueChange = { scope.launch { prefs.setParticleDensity(it) } },
                            valueRange = 0.4f..2.0f
                        )
                    }
                }
            }

            // -----------------------------------------------------------------
            // 6. SYSTEM CORE & POWER
            // -----------------------------------------------------------------
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "Power & Material You System Core",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )

                    // Material You Monet Theming
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Material You Monet Theming", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                            Text("Notifies WallpaperColors to tint entire Android OS", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = monetEnabled,
                            onCheckedChange = { scope.launch { prefs.setMonetThemingEnabled(it) } }
                        )
                    }

                    // Monet Palette preview badges
                    if (monetEnabled) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Palette:", style = MaterialTheme.typography.labelSmall)
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .background(Color(selectedScene.primaryColorHex), CircleShape)
                                    .border(1.dp, Color.White.copy(alpha = 0.4f), CircleShape)
                            )
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .background(Color(selectedScene.secondaryColorHex), CircleShape)
                                    .border(1.dp, Color.White.copy(alpha = 0.4f), CircleShape)
                            )
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .background(Color(selectedScene.accentColorHex), CircleShape)
                                    .border(1.dp, Color.White.copy(alpha = 0.4f), CircleShape)
                            )
                        }
                    }

                    // Target FPS selector
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Target Frame Rate", style = MaterialTheme.typography.bodySmall)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(30, 60, 90, 120).forEach { fps ->
                                FilterChip(
                                    selected = targetFps == fps,
                                    onClick = { scope.launch { prefs.setTargetFps(fps) } },
                                    label = { Text("${fps} FPS") },
                                    colors = FilterChipDefaults.filterChipColors()
                                )
                            }
                        }
                    }

                    // Battery Saver Throttling
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Battery Saver Auto-Throttle", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                            Text("Cuts FPS to 30 automatically when Power Saver is on", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = batteryThrottle,
                            onCheckedChange = { scope.launch { prefs.setBatterySaverThrottle(it) } }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
