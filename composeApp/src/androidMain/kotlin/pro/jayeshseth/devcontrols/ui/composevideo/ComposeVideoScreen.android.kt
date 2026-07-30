package pro.jayeshseth.devcontrols.ui.composevideo

import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pro.jayeshseth.devcontrols.composevideo.ComposeVideoController
import pro.jayeshseth.devcontrols.composevideo.exportComposeVideoAndroid

actual suspend fun platformExportComposeVideo(
    controller: ComposeVideoController,
    onProgress: (Float) -> Unit,
    onComplete: (String) -> Unit,
    onError: (String) -> Unit,
) {
    // Context is passed via a global holder set below
    val ctx = ComposeVideoContextHolder.context
        ?: run { onError("Context not available"); return }
    exportComposeVideoAndroid(ctx, controller, onProgress, onComplete, onError)
}

actual fun supportsAudioPicker() = true
actual fun supportsAudioTrack() = true

// ─────────────────────────────────────────────────────────────────────────────
// Context holder — set via side-effect in the screen
// ─────────────────────────────────────────────────────────────────────────────

internal object ComposeVideoContextHolder {
    var context: Context? = null
}

// ─────────────────────────────────────────────────────────────────────────────
// AudioTab — Android actual with file picker
// ─────────────────────────────────────────────────────────────────────────────

@Composable
actual fun AudioTab(controller: ComposeVideoController) {
    val context = LocalContext.current

    // Register context so the export function can access it
    DisposableEffect(context) {
        ComposeVideoContextHolder.context = context
        onDispose { }
    }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let {
            runCatching {
                context.contentResolver.takePersistableUriPermission(it, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            controller.audioUriString = it.toString()
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Text("Audio Track", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

        // Audio file picker card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = controller.audioUriString == null) { launcher.launch(arrayOf("audio/*")) },
            colors = CardDefaults.cardColors(
                containerColor = if (controller.audioUriString != null)
                    MaterialTheme.colorScheme.primaryContainer.copy(0.3f)
                else MaterialTheme.colorScheme.surfaceVariant.copy(0.4f)
            ),
            border = if (controller.audioUriString == null) CardDefaults.outlinedCardBorder() else null
        ) {
            Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(48.dp).clip(CircleShape)
                        .background(if (controller.audioUriString != null) Color(0xFF6C7BFF) else MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Text(if (controller.audioUriString != null) "♪" else "+", fontSize = 22.sp,
                        color = if (controller.audioUriString != null) Color.White else MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Column(Modifier.weight(1f)) {
                    Text(
                        if (controller.audioUriString != null) "Audio file selected" else "Pick audio from device",
                        fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        if (controller.audioUriString != null)
                            controller.audioUriString!!.substringAfterLast("/").take(40)
                        else "MP3, AAC, OGG, M4A, WAV supported",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (controller.audioUriString != null) {
                    TextButton(onClick = { controller.audioUriString = null }) { Text("✕", color = MaterialTheme.colorScheme.error) }
                }
            }
        }

        if (controller.audioUriString != null) {
            OutlinedButton(onClick = { launcher.launch(arrayOf("audio/*")) }, modifier = Modifier.fillMaxWidth()) {
                Text("Change Audio File")
            }
        }

        AnimatedVisibility(controller.audioUriString != null) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                HorizontalDivider()
                Text("Audio Settings", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                EffectSlider("Volume", controller.audioVolume, 0f..2f, "${(controller.audioVolume * 100).toInt()}%") { controller.audioVolume = it }
                EffectSlider("Audio Start Offset", controller.audioOffsetMs.toFloat(), 0f..10_000f, "${controller.audioOffsetMs}ms") { controller.audioOffsetMs = it.toLong() }

                // Waveform
                Text("Waveform Preview", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                AudioWaveformView(
                    progress = controller.currentProgress,
                    modifier = Modifier.fillMaxWidth().height(72.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF0A0A14))
                )
            }
        }

        AnimatedVisibility(controller.audioUriString == null) {
            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF0A1A2A))) {
                Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("ℹ", color = Color(0xFF6C7BFF), fontSize = 16.sp)
                    Text("Pick an audio file to mux into the exported MP4. The audio is added directly without re-encoding.",
                        style = MaterialTheme.typography.bodySmall, color = Color(0xFF86AEFF))
                }
            }
        }
    }
}
