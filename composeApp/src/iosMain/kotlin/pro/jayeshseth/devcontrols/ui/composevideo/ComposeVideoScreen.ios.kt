package pro.jayeshseth.devcontrols.ui.composevideo

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import pro.jayeshseth.devcontrols.composevideo.ComposeVideoController

actual suspend fun platformExportComposeVideo(
    controller: ComposeVideoController,
    onProgress: (Float) -> Unit,
    onComplete: (String) -> Unit,
    onError: (String) -> Unit,
) {
    onError("Video export is not supported on iOS at this time.")
}

actual fun supportsAudioPicker() = false
actual fun supportsAudioTrack() = false

@Composable
actual fun AudioTab(controller: ComposeVideoController) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Text("Audio Track", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF0E1628))) {
            Column(
                Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Audio selection is supported on Android.", style = MaterialTheme.typography.bodyMedium, color = Color(0xFF8899AA))
            }
        }
    }
}
