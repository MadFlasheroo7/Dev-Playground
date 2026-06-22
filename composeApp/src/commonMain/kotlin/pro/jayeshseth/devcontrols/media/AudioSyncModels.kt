package pro.jayeshseth.devcontrols.media

enum class PlaybackSyncState {
    IDLE, BUFFERING, PLAYING, PAUSED
}

data class ClientInfo(
    val id: String,
    val ip: String,
    val name: String,
    val volume: Float,
    val latencyMs: Long = 0L,
    val isBuffering: Boolean = false,
    val positionMs: Long = 0L
)
