package pro.jayeshseth.devcontrols.ui.devtools

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pro.jayeshseth.devcontrols.camera.CameraPreset
import pro.jayeshseth.devcontrols.camera.rememberVirtualCameraEngine

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VirtualCameraStudioScreen() {
    val engine = rememberVirtualCameraEngine()
    val settings = engine.settings.value
    val status = engine.status.value
    val processedFrame = engine.processedFrame.value
    val rawFrame = engine.rawFrame.value

    var isCopied by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "Virtual Camera Studio & Quality Pipeline",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Real-time local webcam processing & v4l2loopback output for Google Meet / Zoom",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ── 1. Live Camera Preview Panel ──────────────────────────────────────────
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "LIVE PREVIEW STREAM",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(if (status.isCameraActive) Color(0xFF00E676) else Color.Red)
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = if (status.isCameraActive) "${status.fps} FPS" else "OFFLINE",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Black
                                )
                            }
                        }

                        // Compare Side-by-Side Toggle
                        FilterChip(
                            selected = settings.isCompareMode,
                            onClick = { engine.updateSettings(settings.copy(isCompareMode = !settings.isCompareMode)) },
                            label = { Text("Compare (Raw vs Enhanced)", fontSize = 11.sp) }
                        )
                    }

                    Spacer(Modifier.height(8.dp))

                    val videoShape = RoundedCornerShape(12.dp)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(16f / 9f)
                            .clip(videoShape)
                            .background(Color.Black)
                            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), videoShape),
                        contentAlignment = Alignment.Center
                    ) {
                        if (settings.isCompareMode) {
                            Row(Modifier.fillMaxSize()) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxSize()
                                        .border(1.dp, Color.Gray.copy(alpha = 0.3f))
                                ) {
                                    rawFrame?.let {
                                        Image(
                                            bitmap = it,
                                            contentDescription = "Raw Frame",
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )
                                    }
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopStart)
                                            .padding(8.dp)
                                            .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(4.dp))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text("RAW (/dev/video0)", color = Color.White, fontSize = 10.sp)
                                    }
                                }
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxSize()
                                        .border(1.dp, Color(0xFF00E676).copy(alpha = 0.5f))
                                ) {
                                    processedFrame?.let {
                                        Image(
                                            bitmap = it,
                                            contentDescription = "Enhanced Frame",
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )
                                    }
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopStart)
                                            .padding(8.dp)
                                            .background(Color(0xFF00E676).copy(alpha = 0.9f), RoundedCornerShape(4.dp))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text("ENHANCED", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                                    }
                                }
                            }
                        } else {
                            processedFrame?.let {
                                Image(
                                    bitmap = it,
                                    contentDescription = "Processed Camera Stream",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            }
                        }
                    }
                }
            }

            // ── 2. Virtual Camera Output Card (Linux v4l2loopback) ────────────────────
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (status.isVirtualCamActive) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                "Google Meet / Zoom Virtual Camera Stream",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "Outputs enhanced video feed to /dev/video20 ('DevControls Enhanced Cam')",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = settings.isVirtualCamEnabled,
                            onCheckedChange = { engine.toggleVirtualCam(it) }
                        )
                    }

                    status.errorMessage?.let { err ->
                        Spacer(Modifier.height(8.dp))
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Column(Modifier.padding(12.dp)) {
                                Text(err, color = MaterialTheme.colorScheme.onErrorContainer, fontSize = 12.sp)
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    "Run in Linux terminal to create virtual webcam loopback device:",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(Modifier.height(4.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color.Black.copy(alpha = 0.8f), RoundedCornerShape(6.dp))
                                        .padding(8.dp)
                                ) {
                                    Text(
                                        "sudo modprobe v4l2loopback devices=1 card_label=\"DevControls Enhanced Cam\" exclusive_caps=1",
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 11.sp,
                                        color = Color(0xFF00FFCC)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ── 3. Camera Quality Presets ──────────────────────────────────────────────
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        "Quality Enhancement Presets",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = false,
                            onClick = { engine.applyPreset(CameraPreset.ProStudio) },
                            label = { Text("💼 Pro Studio", fontSize = 12.sp) }
                        )
                        FilterChip(
                            selected = false,
                            onClick = { engine.applyPreset(CameraPreset.BacklightFix) },
                            label = { Text("☀️ Backlight Fix", fontSize = 12.sp) }
                        )
                        FilterChip(
                            selected = false,
                            onClick = { engine.applyPreset(CameraPreset.LowLightDenoise) },
                            label = { Text("🌙 Low-Light Denoise", fontSize = 12.sp) }
                        )
                        FilterChip(
                            selected = false,
                            onClick = { engine.applyPreset(CameraPreset.CinematicBokeh) },
                            label = { Text("🎬 Cinematic Bokeh", fontSize = 12.sp) }
                        )
                    }
                }
            }

            // ── 4. Image Enhancement Quality Sliders ───────────────────────────────────
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "Live Image Processing Controls",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )

                    // Sharpness Slider
                    Column {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("🔍 Detail & Unsharp Mask", style = MaterialTheme.typography.labelMedium)
                            Text("${(settings.sharpness * 100).toInt()}%", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Slider(
                            value = settings.sharpness,
                            onValueChange = { engine.updateSettings(settings.copy(sharpness = it)) },
                            valueRange = 0f..2f
                        )
                    }

                    // CLAHE Backlight Slider
                    Column {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("☀️ CLAHE Backlight Correction (Shadow Lift)", style = MaterialTheme.typography.labelMedium)
                            Text("${(settings.claheAmount * 100).toInt()}%", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Slider(
                            value = settings.claheAmount,
                            onValueChange = { engine.updateSettings(settings.copy(claheAmount = it)) },
                            valueRange = 0f..1f
                        )
                    }

                    // Denoise Slider
                    Column {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("🧹 Denoise & Sensor Grain Reduction", style = MaterialTheme.typography.labelMedium)
                            Text("${(settings.denoise * 100).toInt()}%", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Slider(
                            value = settings.denoise,
                            onValueChange = { engine.updateSettings(settings.copy(denoise = it)) },
                            valueRange = 0f..1f
                        )
                    }

                    // Color Temperature Slider
                    Column {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("🎨 Color Temperature & Warmth", style = MaterialTheme.typography.labelMedium)
                            Text(
                                if (settings.colorTemperature > 0) "+${(settings.colorTemperature * 100).toInt()}% Warm"
                                else "${(settings.colorTemperature * 100).toInt()}% Cool",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Slider(
                            value = settings.colorTemperature,
                            onValueChange = { engine.updateSettings(settings.copy(colorTemperature = it)) },
                            valueRange = -1f..1f
                        )
                    }

                    // Background Blur Slider
                    Column {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("📷 Background Blur (DSLR Bokeh)", style = MaterialTheme.typography.labelMedium)
                            Text("${(settings.backgroundBlur * 100).toInt()}%", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Slider(
                            value = settings.backgroundBlur,
                            onValueChange = { engine.updateSettings(settings.copy(backgroundBlur = it)) },
                            valueRange = 0f..1f
                        )
                    }
                }
            }
        }
    }
}
