package pro.jayeshseth.devcontrols.media

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.coroutines.flow.MutableStateFlow
import pro.jayeshseth.media.MediaController

class IosAudioSyncManager : AudioSyncManager {
    override val isHost = MutableStateFlow(false)
    override val isClient = MutableStateFlow(false)
    override val isConnected = MutableStateFlow(false)
    override val hostIp = MutableStateFlow<String?>(null)
    override val clientIp = "127.0.0.1"
    override val connectedClients = MutableStateFlow<List<ClientInfo>>(emptyList())
    override val syncedTitle = MutableStateFlow("")
    override val syncedUploader = MutableStateFlow("")
    override val playbackSyncState = MutableStateFlow(PlaybackSyncState.IDLE)
    override var clientLatencyNudgeMs: Long = 0L
    override val logConsole = MutableStateFlow<List<String>>(listOf("[iOS] Audio sync network sockets are stubbed on iOS."))
    override val discoveredHosts = MutableStateFlow<List<Pair<String, String>>>(emptyList())
    override val appStandbyBucket = MutableStateFlow<String?>("Not Applicable (iOS)")
    override fun requestIgnoreBatteryOptimizations() {}

    override fun startHost(port: Int) {}
    override fun stopHost() {}
    override fun broadcastPlay(youtubeUrl: String, title: String, uploader: String, positionMs: Long) {}
    override fun broadcastPause() {}
    override fun broadcastResume(positionMs: Long) {}
    override fun broadcastSeek(positionMs: Long) {}
    override fun setClientVolume(clientId: String, volume: Float) {}
    override fun setMasterVolume(volume: Float) {}
    override fun broadcastPlayLocal(audioUri: String, title: String) {}
    override fun startClient(hostIp: String, port: Int) {}
    override fun stopClient() {}
    override fun startAutoDiscovery() {}
    override fun stopAutoDiscovery() {}
    override fun release() {}
}

@Composable
actual fun rememberAudioSyncManager(mediaController: MediaController): AudioSyncManager =
    remember(mediaController) { IosAudioSyncManager() }

@Composable
actual fun rememberAudioFilePicker(onFilePicked: (String, String) -> Unit): () -> Unit = remember { {} }
