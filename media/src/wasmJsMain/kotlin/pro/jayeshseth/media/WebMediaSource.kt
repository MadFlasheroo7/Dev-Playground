package pro.jayeshseth.media

/**
 * Web [MediaSource] stub. Browser can't run NewPipeExtractor (JVM) or yt-dlp (binary), and calling
 * YouTube directly is blocked by CORS + PoToken, so web extraction needs a different backend (a CORS
 * search API like Piped/Invidious, with playback via the YouTube IFrame Player). Wired up next; this
 * stub exists so the wasm target compiles.
 */
private object WebMediaSource : MediaSource {
    override suspend fun search(query: String): List<MediaItem> = emptyList()
    override suspend fun resolve(id: String): StreamInfo =
        StreamInfo(id = id, title = "", uploader = "", durationSec = 0, audio = emptyList(), video = emptyList())
}

actual fun createMediaSource(): MediaSource = WebMediaSource
