package pro.jayeshseth.media

/**
 * Platform-agnostic media model shared by every [MediaSource] backend (NewPipeExtractor on JVM,
 * Piped REST on iOS/Web). Deliberately plain data classes — no `@Serializable`, since the
 * serialization compiler plugin crashes the Kotlin/Wasm frontend; the Piped backend parses JSON
 * with the kotlinx-serialization *runtime* DSL and maps into these.
 */

/** A search result / playlist entry. [id] is an opaque token understood by its own [MediaSource]
 *  (a YouTube watch URL for the NewPipe backend, a video id for the Piped backend). */
data class MediaItem(
    val id: String,
    val title: String,
    val uploader: String,
    /** Length in seconds; 0 for live/unknown. */
    val durationSec: Long,
    val thumbnailUrl: String?,
)

/** A single playable stream. For audio, [rank] is the average bitrate (kbps); for video it's the
 *  pixel height (e.g. 720). [url] is a direct, time-limited progressive URL. */
data class Track(
    val url: String,
    val label: String,
    val mimeType: String,
    val rank: Int,
)

/**
 * Fully resolved playback info for one item.
 *
 * - [hls] / [dash] are adaptive *manifest* URLs (m3u8 / mpd). On YouTube these come from the iOS &
 *   Android InnerTube clients and — crucially — are **not** gated behind a PoToken, so they remain
 *   playable where the individual progressive [audio]/[video] itag URLs increasingly are not. Prefer a
 *   manifest when present (see [bestPlayable]); a capable player (ExoPlayer/VLC) picks the right
 *   renditions from it.
 * - [audio] / [video] are the separate progressive (single-file) tracks, used as a fallback.
 */
data class StreamInfo(
    val id: String,
    val title: String,
    val uploader: String,
    val durationSec: Long,
    val audio: List<Track>,
    val video: List<Track>,
    val hls: String? = null,
    val dash: String? = null,
)

/** Highest-bitrate audio track (best for music playback), or null if none. */
fun StreamInfo.bestAudio(): Track? = audio.maxByOrNull { it.rank }

/**
 * The best URL to hand a player, **music-first**: the highest-bitrate progressive **audio** track (a
 * direct `audio/webm`-style stream — ideal for music and the lightest to play), else the adaptive
 * **HLS** then **DASH** manifest, else any progressive video. Null if nothing is playable (blocked).
 *
 * The progressive audio URLs returned by the iOS InnerTube client are token-free and serve real bytes
 * (HTTP 206 `audio/webm`), which is why they're preferred over the manifests here.
 */
fun StreamInfo.bestPlayable(): String? =
    bestAudio()?.url
        ?: hls?.takeIf { it.isNotBlank() }
        ?: dash?.takeIf { it.isNotBlank() }
        ?: bestVideo()?.url

/** Best progressive video at or below [maxHeight], else the smallest available. */
fun StreamInfo.bestVideo(maxHeight: Int = 720): Track? =
    video.filter { it.rank in 1..maxHeight }.maxByOrNull { it.rank } ?: video.minByOrNull { it.rank }
