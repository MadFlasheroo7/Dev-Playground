package pro.jayeshseth.media

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.encodeURLParameter
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull

/**
 * Web [MediaSource]. The browser can't run NewPipeExtractor (JVM) or yt-dlp (binary), so search goes
 * through a **Piped** instance's REST API (CORS-enabled, metadata only — not PoToken-gated like stream
 * URLs). Playback is by **YouTube video id** via the YouTube IFrame Player (see the web MediaController),
 * so [MediaItem.id] here is the bare video id and [resolve] is a no-op pass-through.
 */
private object WebMediaSource : MediaSource {
    // CORS-enabled Piped instance. If it goes down, swap for another from the public Piped instance list.
    private const val PIPED = "https://api.piped.private.coffee"
    private val client = HttpClient()
    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun search(query: String): List<MediaItem> {
        val url = "$PIPED/search?q=${query.encodeURLParameter()}&filter=videos"
        val body = client.get(url).bodyAsText()
        val items = json.parseToJsonElement(body).jsonObject["items"]?.jsonArray ?: return emptyList()
        return items.mapNotNull { el ->
            val o = el.jsonObject
            if (o["type"]?.jsonPrimitive?.contentOrNull != "stream") return@mapNotNull null
            val watch = o["url"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null // "/watch?v=ID"
            val id = watch.substringAfter("v=", "").substringBefore('&').ifEmpty { return@mapNotNull null }
            MediaItem(
                id = id,
                title = o["title"]?.jsonPrimitive?.contentOrNull.orEmpty(),
                uploader = o["uploaderName"]?.jsonPrimitive?.contentOrNull.orEmpty(),
                durationSec = (o["duration"]?.jsonPrimitive?.longOrNull ?: 0).coerceAtLeast(0),
                thumbnailUrl = o["thumbnail"]?.jsonPrimitive?.contentOrNull,
            )
        }
    }

    /** No extraction on web — the IFrame player plays by [id] directly. */
    override suspend fun resolve(id: String): StreamInfo =
        StreamInfo(id = id, title = "", uploader = "", durationSec = 0, audio = emptyList(), video = emptyList())
}

actual fun createMediaSource(): MediaSource = WebMediaSource
