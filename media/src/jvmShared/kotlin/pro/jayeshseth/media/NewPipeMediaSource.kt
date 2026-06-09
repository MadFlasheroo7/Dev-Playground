package pro.jayeshseth.media

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Request as OkRequest
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.downloader.Request
import org.schabi.newpipe.extractor.downloader.Response
import org.schabi.newpipe.extractor.search.SearchInfo
import org.schabi.newpipe.extractor.stream.StreamInfoItem
import org.schabi.newpipe.extractor.stream.StreamInfo as NpStreamInfo

/**
 * JVM (Android + Desktop) [MediaSource] backed by **NewPipeExtractor**.
 *
 * NewPipeExtractor is a pure-Java library: it scrapes YouTube's pages/Innertube API, and crucially
 * runs YouTube's obfuscated *player JS* (the signature / `n`-parameter cipher) through Mozilla Rhino
 * to turn the encrypted stream descriptors into working URLs. All network goes through a single
 * [Downloader] abstraction we implement here on top of Ktor's OkHttp engine — so there's no Android
 * dependency, which is exactly why the same code serves both JVM targets.
 *
 * Everything runs on [Dispatchers.IO]; NewPipe's API is blocking and can throw on any YouTube change.
 */
actual fun createMediaSource(): MediaSource = NewPipeMediaSource

private object NewPipeMediaSource : MediaSource {
    init {
        // One-time global init: give NewPipe our OkHttp-backed downloader. Localization defaults to en/US.
        NewPipe.init(OkHttpDownloader(OkHttpClient()))
        // Register the platform PoToken provider (Android WebView/BotGuard) so YouTube hands back real
        // stream URLs. No-op on Desktop, where extraction degrades to an inline error. See PoTokenSetup.kt.
        runCatching { installYoutubePoTokenProvider() }
    }

    override suspend fun search(query: String): List<MediaItem> = withContext(Dispatchers.IO) {
        val service = ServiceList.YouTube
        val handler = service.searchQHFactory.fromQuery(query)
        val info = SearchInfo.getInfo(service, handler)
        info.relatedItems.filterIsInstance<StreamInfoItem>().map { item ->
            MediaItem(
                id = item.url,                       // YouTube watch URL — fed straight back to resolve()
                title = item.name.orEmpty(),
                uploader = item.uploaderName.orEmpty(),
                durationSec = item.duration.coerceAtLeast(0),
                thumbnailUrl = item.thumbnails.firstOrNull()?.url,
            )
        }
    }

    override suspend fun resolve(id: String): StreamInfo = withContext(Dispatchers.IO) {
        val info = NpStreamInfo.getInfo(ServiceList.YouTube, id)
        val audio = info.audioStreams.mapNotNull { a ->
            val url = a.content?.takeIf { a.isUrl } ?: return@mapNotNull null
            Track(url, "audio ${a.averageBitrate}k", a.format?.mimeType ?: "audio/*", a.averageBitrate)
        }
        val video = info.videoStreams.mapNotNull { v ->   // progressive (video+audio) tracks
            val url = v.content?.takeIf { v.isUrl } ?: return@mapNotNull null
            val height = v.resolution?.substringBefore('p')?.toIntOrNull() ?: 0
            Track(url, v.resolution ?: "video", v.format?.mimeType ?: "video/*", height)
        }
        StreamInfo(id, info.name.orEmpty(), info.uploaderName.orEmpty(), info.duration.coerceAtLeast(0), audio, video)
    }
}

/** Bridges NewPipe's synchronous [Downloader] contract onto OkHttp (the reference approach). */
private class OkHttpDownloader(private val client: OkHttpClient) : Downloader() {
    override fun execute(req: Request): Response {
        val builder = OkRequest.Builder().url(req.url())
        req.headers().forEach { (key, values) -> values.forEach { builder.addHeader(key, it) } }
        val body = req.dataToSend()?.toRequestBody()
        builder.method(req.httpMethod(), body)
        client.newCall(builder.build()).execute().use { r ->
            return Response(r.code, r.message, r.headers.toMultimap(), r.body?.string(), r.request.url.toString())
        }
    }
}
