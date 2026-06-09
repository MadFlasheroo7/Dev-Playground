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

/** Fully resolved playback info for one item: separate audio-only and (progressive) video tracks. */
data class StreamInfo(
    val id: String,
    val title: String,
    val uploader: String,
    val durationSec: Long,
    val audio: List<Track>,
    val video: List<Track>,
)

/** Highest-bitrate audio track (best for music playback), or null if none. */
fun StreamInfo.bestAudio(): Track? = audio.maxByOrNull { it.rank }

/** Best progressive video at or below [maxHeight], else the smallest available. */
fun StreamInfo.bestVideo(maxHeight: Int = 720): Track? =
    video.filter { it.rank in 1..maxHeight }.maxByOrNull { it.rank } ?: video.minByOrNull { it.rank }
