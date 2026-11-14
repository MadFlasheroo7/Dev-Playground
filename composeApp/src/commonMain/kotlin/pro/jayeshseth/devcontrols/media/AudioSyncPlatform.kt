package pro.jayeshseth.devcontrols.media

import androidx.compose.runtime.Composable
import kotlinx.coroutines.flow.StateFlow
import pro.jayeshseth.media.MediaController

data class StandbyBucketRecord(
    val id: String,
    val timestampMs: Long,
    val formattedTime: String,
    val bucketName: String,
    val bucketId: Int,
    val triggerReason: String,
    val durationInPreviousBucketMs: Long = 0L
)

interface AudioSyncManager {
    val isHost: StateFlow<Boolean>
    val isClient: StateFlow<Boolean>
    val isConnected: StateFlow<Boolean>
    val hostIp: StateFlow<String?>
    val clientIp: String
    val connectedClients: StateFlow<List<ClientInfo>>
    val syncedTitle: StateFlow<String>
    val syncedUploader: StateFlow<String>
    val playbackSyncState: StateFlow<PlaybackSyncState>
    var clientLatencyNudgeMs: Long
    val logConsole: StateFlow<List<String>>
    val discoveredHosts: StateFlow<List<Pair<String, String>>> // IP to Name
    val appStandbyBucket: StateFlow<String?>
    val standbyHistoryLog: StateFlow<List<StandbyBucketRecord>>

    fun refreshStandbyBucket()
    fun clearStandbyHistory()
    fun requestIgnoreBatteryOptimizations()
    fun startHost(port: Int = 8099)
    fun stopHost()
    fun broadcastPlay(youtubeUrl: String, title: String, uploader: String, positionMs: Long = 0L)
    fun broadcastPause()
    fun broadcastResume(positionMs: Long = 0L)
    fun broadcastSeek(positionMs: Long)
    fun setClientVolume(clientId: String, volume: Float)
    fun setMasterVolume(volume: Float)

    fun broadcastPlayLocal(audioUri: String, title: String)

    fun startClient(hostIp: String, port: Int = 8099)
    fun stopClient()
    fun startAutoDiscovery()
    fun stopAutoDiscovery()
    fun release()
}

@Composable
expect fun rememberAudioSyncManager(mediaController: MediaController): AudioSyncManager

@Composable
expect fun rememberAudioFilePicker(onFilePicked: (String, String) -> Unit): () -> Unit
