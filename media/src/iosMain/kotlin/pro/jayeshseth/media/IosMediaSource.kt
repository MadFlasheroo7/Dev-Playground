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
 * iOS [MediaSource]. Like web, search goes through a **Piped** REST instance (NewPipeExtractor is JVM-only;
 * yt-dlp is a binary). Playback on iOS is by **YouTube video id** via a WKWebView IFrame player (the app's
 * iOS MediaController — to be fleshed out on a Mac), so [MediaItem.id] is the bare video id.
 *
 * NOTE: authored on Linux for completeness; compile/link is macOS-gated (Xcode) and unverified here.
 */
private object IosMediaSource : MediaSource {
    private const val PIPED = "https://api.piped.private.coffee"
    private val client = HttpClient() // Darwin engine (ktor-client-darwin)
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

    override suspend fun resolve(id: String): StreamInfo =
        StreamInfo(id = id, title = "", uploader = "", durationSec = 0, audio = emptyList(), video = emptyList())
}

actual fun createMediaSource(): MediaSource = IosMediaSource
