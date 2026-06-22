package pro.jayeshseth.devcontrols.ui.audiolab

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import pro.jayeshseth.devcontrols.media.PlaybackSyncState
import pro.jayeshseth.devcontrols.media.rememberAudioSyncManager
import pro.jayeshseth.devcontrols.media.rememberMediaController
import pro.jayeshseth.devcontrols.media.rememberAudioFilePicker
import pro.jayeshseth.media.MediaItem
import pro.jayeshseth.media.bestPlayable
import pro.jayeshseth.media.createMediaSource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioSyncScreen(onBack: () -> Unit) {
    val mediaController = rememberMediaController()
    val syncManager = rememberAudioSyncManager(mediaController)
    val filePicker = rememberAudioFilePicker { uri, name ->
        syncManager.broadcastPlayLocal(uri, name)
    }
    val scope = rememberCoroutineScope()

    val isHost by syncManager.isHost.collectAsState()
    val isClient by syncManager.isClient.collectAsState()
    val isConnected by syncManager.isConnected.collectAsState()
    val hostIp by syncManager.hostIp.collectAsState()
    val connectedClients by syncManager.connectedClients.collectAsState()
    val syncedTitle by syncManager.syncedTitle.collectAsState()
    val syncedUploader by syncManager.syncedUploader.collectAsState()
    val syncState by syncManager.playbackSyncState.collectAsState()
    val logConsole by syncManager.logConsole.collectAsState()
    val discoveredHosts by syncManager.discoveredHosts.collectAsState()

    val mediaPlaying by mediaController.playing.collectAsState()
    val mediaPositionMs by mediaController.positionMs.collectAsState()
    val mediaDurationMs by mediaController.durationMs.collectAsState()
    val mediaBuffering by mediaController.buffering.collectAsState()

    var selectedTab by remember { mutableStateOf(0) } // 0: Select Role, 1: Active Panel

    DisposableEffect(Unit) {
        onDispose {
            syncManager.release()
            mediaController.release()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Connected Audio Sync") },
                navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }
            )
        },
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (!isHost && !isClient) {
                // ROLE SELECTION PANEL
                Text(
                    text = "Synchronize audio playback across multiple Android devices on the same Wi-Fi network in real-time.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
                )

                Spacer(Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Host Card
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .height(180.dp)
                            .clickable {
                                syncManager.startHost()
                            },
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.SpaceBetween,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                "Host Session",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                "Broadcasts audio streams, controls play/pause state, and overrides volume of all connected devices.",
                                style = MaterialTheme.typography.bodySmall,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                                maxLines = 4,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                "Tap to Host →",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    // Client Card
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .height(180.dp)
                            .clickable {
                                syncManager.startAutoDiscovery()
                                // Transition to client view
                                scope.launch {
                                    syncManager.startClient("127.0.0.1") // initial placeholder connection trigger or wait
                                }
                                syncManager.stopClient() // reset to scan mode
                                // To let the client run discovery first
                                syncManager.startAutoDiscovery()
                            },
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.SpaceBetween,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                "Join Session",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Text(
                                "Scans the Wi-Fi network to auto-discover hosts. Synchronizes its local clock and mirrors Host commands.",
                                style = MaterialTheme.typography.bodySmall,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f),
                                maxLines = 4,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                "Tap to Join →",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                }
            } else {
                // ACTIVE DASHBOARD SCREEN
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = if (isHost) "Hosting Audio Session" else "Client Audio Session",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isHost) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                        )
                        Text(
                            text = "IP: ${syncManager.clientIp}",
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Button(
                        onClick = {
                            syncManager.stopHost()
                            syncManager.stopClient()
                            syncManager.stopAutoDiscovery()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer
                        )
                    ) {
                        Text("Disconnect")
                    }
                }

                HorizontalDivider()

                if (isHost) {
                    HostDashboard(syncManager, mediaController, onPickFile = filePicker)
                } else {
                    ClientDashboard(syncManager, mediaController)
                }

                Spacer(Modifier.height(8.dp))
                Text("Real-time Trace Console", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                
                // Console Box
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .padding(8.dp)
                ) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(logConsole) { line ->
                            Text(
                                text = line,
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = FontFamily.Monospace,
                                color = if (line.contains("[SYNC]")) {
                                    MaterialTheme.colorScheme.primary
                                } else if (line.contains("[TCP]")) {
                                    MaterialTheme.colorScheme.tertiary
                                } else if (line.contains("[UDP]")) {
                                    MaterialTheme.colorScheme.secondary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HostDashboard(
    syncManager: pro.jayeshseth.devcontrols.media.AudioSyncManager,
    mediaController: pro.jayeshseth.media.MediaController,
    onPickFile: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val mediaSource = remember { createMediaSource() }

    var searchQuery by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<MediaItem>>(emptyList()) }
    var searchLoading by remember { mutableStateOf(false) }

    val connectedClients by syncManager.connectedClients.collectAsState()
    val syncedTitle by syncManager.syncedTitle.collectAsState()
    val syncedUploader by syncManager.syncedUploader.collectAsState()

    val positionMs by mediaController.positionMs.collectAsState()
    val durationMs by mediaController.durationMs.collectAsState()
    val playing by mediaController.playing.collectAsState()
    val buffering by mediaController.buffering.collectAsState()

    var masterVolume by remember { mutableStateOf(1f) }

    fun runSearch() {
        if (searchQuery.isBlank()) return
        searchLoading = true
        scope.launch {
            runCatching { mediaSource.search(searchQuery.trim()) }
                .onSuccess { results = it; searchLoading = false }
                .onFailure { searchLoading = false }
        }
    }

    fun playTrack(item: MediaItem) {
        scope.launch {
            searchLoading = true
            val resolvedUrl = runCatching {
                mediaSource.resolve(item.id).bestPlayable()
            }.getOrNull() ?: ""
            searchLoading = false

            val compositeUrl = "${item.id}|$resolvedUrl"
            syncManager.broadcastPlay(compositeUrl, item.title, item.uploader)
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth().height(420.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Song Details / Playing state
        if (syncedTitle.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
            ) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(syncedTitle, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(syncedUploader, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    
                    val playerError by mediaController.error.collectAsState()
                    val stateText = when {
                        playerError != null -> "❌ Error: $playerError"
                        buffering -> "⏳ Buffering..."
                        playing -> "▶ Playing"
                        else -> "⏸ Paused"
                    }
                    val stateColor = when {
                        playerError != null -> MaterialTheme.colorScheme.error
                        buffering -> MaterialTheme.colorScheme.tertiary
                        playing -> MaterialTheme.colorScheme.primary
                        else -> MaterialTheme.colorScheme.outline
                    }
                    Text(
                        text = "State: $stateText",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = stateColor
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = {
                                if (playing) syncManager.broadcastPause() else syncManager.broadcastResume(positionMs)
                            }
                        ) {
                            Text(if (playing) "❚❚" else "▶", style = MaterialTheme.typography.titleLarge)
                        }

                        Slider(
                            value = if (durationMs > 0) (positionMs.toFloat() / durationMs) else 0f,
                            onValueChange = { percent ->
                                if (durationMs > 0) {
                                    syncManager.broadcastSeek((percent * durationMs).toLong())
                                }
                            },
                            modifier = Modifier.weight(1f)
                        )

                        Text(
                            text = "${formatTime(positionMs)} / ${formatTime(durationMs)}",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }
        }

        // Master Volume
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("🔊 Master Volume", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
            Slider(
                value = masterVolume,
                onValueChange = {
                    masterVolume = it
                    syncManager.setMasterVolume(it)
                },
                modifier = Modifier.weight(1f)
            )
        }

        var hostTabSelected by remember { mutableStateOf(0) } // 0: Stream Search, 1: Local File

        TabRow(selectedTabIndex = hostTabSelected) {
            Tab(selected = hostTabSelected == 0, onClick = { hostTabSelected = 0 }, text = { Text("Stream Search") })
            Tab(selected = hostTabSelected == 1, onClick = { hostTabSelected = 1 }, text = { Text("Local File") })
        }

        if (hostTabSelected == 0) {
            // Search panel
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text("Search YouTube Track") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                Button(onClick = { runSearch() }) {
                    Text("Search")
                }
            }

            if (searchLoading) LinearProgressIndicator(Modifier.fillMaxWidth())

            LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth()) {
                items(results) { item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { playTrack(item) }
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(item.title, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.Medium)
                            Text(item.uploader, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text("▶", modifier = Modifier.padding(start = 8.dp))
                    }
                    HorizontalDivider()
                }
            }
        } else {
            // Local file selection panel
            Column(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Button(
                    onClick = onPickFile,
                    modifier = Modifier.padding(16.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("📁 Choose Local Audio File", fontSize = 16.sp)
                }

                Text(
                    text = "This will start a local HTTP streaming server on your device. Clients will connect and play the file directly in sync.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
            }
        }

        // Connected clients list
        if (connectedClients.isNotEmpty()) {
            Text("Connected Clients (${connectedClients.size})", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            LazyColumn(modifier = Modifier.height(100.dp).fillMaxWidth()) {
                items(connectedClients) { client ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(0.4f)) {
                            Text(client.name, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.SemiBold)
                            Text("RTT: ${client.latencyMs}ms", style = MaterialTheme.typography.labelSmall)
                        }
                        
                        Slider(
                            value = client.volume,
                            onValueChange = { syncManager.setClientVolume(client.id, it) },
                            modifier = Modifier.weight(0.6f)
                        )
                    }
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
fun ClientDashboard(syncManager: pro.jayeshseth.devcontrols.media.AudioSyncManager, mediaController: pro.jayeshseth.media.MediaController) {
    val isConnected by syncManager.isConnected.collectAsState()
    val hostIp by syncManager.hostIp.collectAsState()
    val discoveredHosts by syncManager.discoveredHosts.collectAsState()
    val syncedTitle by syncManager.syncedTitle.collectAsState()
    val syncedUploader by syncManager.syncedUploader.collectAsState()
    val syncState by syncManager.playbackSyncState.collectAsState()

    var manualIp by remember { mutableStateOf("") }
    var latencyNudge by remember { mutableStateOf(0f) }

    val positionMs by mediaController.positionMs.collectAsState()
    val durationMs by mediaController.durationMs.collectAsState()

    Column(
        modifier = Modifier.fillMaxWidth().height(420.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (!isConnected) {
            // Discovery state
            Text("Scanning local Wi-Fi subnet for Hosts...", style = MaterialTheme.typography.bodySmall)
            
            LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth()) {
                items(discoveredHosts) { host ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable { syncManager.startClient(host.first) },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f))
                    ) {
                        Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column {
                                Text(host.second, fontWeight = FontWeight.Bold)
                                Text(host.first, style = MaterialTheme.typography.bodySmall)
                            }
                            Text("Connect →", color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = manualIp,
                    onValueChange = { manualIp = it },
                    label = { Text("Manual Host IP Address") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                Button(onClick = { syncManager.startClient(manualIp.trim()) }) {
                    Text("Connect")
                }
            }
        } else {
            // Connected client dashboard
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.2f))
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Connected to Host at: $hostIp", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                    
                    if (syncedTitle.isNotEmpty()) {
                        val playerError by mediaController.error.collectAsState()
                        val playing by mediaController.playing.collectAsState()
                        val buffering by mediaController.buffering.collectAsState()

                        Text("Track: $syncedTitle", maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.SemiBold)
                        Text("Uploader: $syncedUploader", style = MaterialTheme.typography.bodySmall)

                        val stateText = when {
                            playerError != null -> "❌ Error: $playerError"
                            buffering -> "⏳ Buffering..."
                            playing -> "▶ Playing"
                            else -> "⏸ Paused"
                        }
                        val stateColor = when {
                            playerError != null -> MaterialTheme.colorScheme.error
                            buffering -> MaterialTheme.colorScheme.tertiary
                            playing -> MaterialTheme.colorScheme.primary
                            else -> MaterialTheme.colorScheme.outline
                        }
                        Text(
                            text = "State: $stateText",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = stateColor
                        )

                        Text("Progress: ${formatTime(positionMs)} / ${formatTime(durationMs)}", style = MaterialTheme.typography.bodySmall)
                        
                        LinearProgressIndicator(
                            progress = { if (durationMs > 0) (positionMs.toFloat() / durationMs) else 0f },
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        Text("Waiting for Host to play track...", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            // Latency nudging controls
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("🔈 Latency Sync Adjuster", fontWeight = FontWeight.SemiBold)
                    Text("${latencyNudge.toInt()} ms", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
                
                Text(
                    "If your device speaker plays slightly ahead or behind the host physically, slide this to nudging timing.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Slider(
                    value = latencyNudge,
                    onValueChange = {
                        latencyNudge = it
                        syncManager.clientLatencyNudgeMs = it.toLong()
                    },
                    valueRange = -300f..300f
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TextButton(onClick = {
                        latencyNudge = 0f
                        syncManager.clientLatencyNudgeMs = 0L
                    }) {
                        Text("Reset Sync")
                    }
                }
            }
        }
    }
}

private fun formatTime(ms: Long): String {
    val total = ms / 1000
    val m = total / 60
    val s = total % 60
    return "$m:${s.toString().padStart(2, '0')}"
}
