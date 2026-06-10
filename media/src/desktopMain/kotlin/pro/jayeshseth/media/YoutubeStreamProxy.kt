package pro.jayeshseth.media

import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.InetSocketAddress
import java.util.concurrent.Executors

/**
 * # Why this exists (desktop)
 *
 * Exactly like Android (see [YoutubeRangeDataSource]), YouTube's `googlevideo.com/videoplayback` URLs
 * **403 on open-ended HTTP ranges**, which is what VLC/libVLC sends — so VLCJ can't play them directly.
 * Unlike ExoPlayer, libVLC gives us no pluggable data source, so instead we run a tiny **localhost HTTP
 * proxy**: VLC plays `http://127.0.0.1:PORT/stream`, and for each (range) request the proxy re-fetches
 * the real stream in bounded `&range=START-END` chunks (which YouTube serves with HTTP 200/206) and
 * streams the bytes back as a normal, seekable resource. Built on the JDK's `com.sun.net.httpserver`,
 * so no extra dependencies.
 */
internal class YoutubeStreamProxy(private val client: OkHttpClient = OkHttpClient()) {

    private var server: HttpServer? = null
    private var targetUrl: String = ""
    private var totalBytes: Long = -1
    private var contentType: String = "application/octet-stream"
    @Volatile private var requestNumber: Long = 0

    /** Starts (or restarts) the proxy for [youtubeUrl] and returns the localhost URL VLC should play. */
    fun start(youtubeUrl: String): String {
        stop()
        targetUrl = stripRangeParam(youtubeUrl)

        // Probe once with a *bounded* range header (which YouTube accepts) to learn the total size and
        // content type up front, so we can answer VLC's range requests with correct Content-Range/Length.
        client.newCall(
            Request.Builder().url(targetUrl).header("Range", "bytes=0-1").header("User-Agent", USER_AGENT).get().build()
        ).execute().use { probe ->
            totalBytes = parseTotal(probe.header("Content-Range")) ?: clenOf(youtubeUrl) ?: -1
            probe.header("Content-Type")?.let { contentType = it }
        }

        val srv = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        srv.executor = Executors.newCachedThreadPool()
        srv.createContext("/") { exchange -> runCatching { handle(exchange) }.also { exchange.close() } }
        srv.start()
        server = srv
        return "http://127.0.0.1:${srv.address.port}/stream"
    }

    fun stop() {
        server?.stop(0)
        server = null
    }

    private fun handle(exchange: HttpExchange) {
        if (totalBytes <= 0) { exchange.sendResponseHeaders(502, -1); return }
        val rangeHeader = exchange.requestHeaders.getFirst("Range")
        val (start, end) = parseRequestedRange(rangeHeader, totalBytes)
        val length = end - start + 1

        exchange.responseHeaders.apply {
            set("Content-Type", contentType)
            set("Accept-Ranges", "bytes")
            if (rangeHeader != null) set("Content-Range", "bytes $start-$end/$totalBytes")
        }
        exchange.sendResponseHeaders(if (rangeHeader != null) 206 else 200, length)

        exchange.responseBody.use { out ->
            var pos = start
            while (pos <= end) {
                val chunkEnd = minOf(pos + CHUNK - 1, end)
                val ok = client.newCall(
                    Request.Builder().url(rangedUrl(pos, chunkEnd)).header("User-Agent", USER_AGENT).get().build()
                ).execute()
                ok.use { resp ->
                    if (!resp.isSuccessful) return            // client/VLC will retry; abort this response
                    resp.body?.byteStream()?.copyTo(out)
                }
                pos = chunkEnd + 1
            }
        }
    }

    private fun rangedUrl(start: Long, end: Long): String =
        targetUrl + (if (targetUrl.contains('?')) '&' else '?') + "range=$start-$end&rn=${requestNumber++}"

    companion object {
        private const val CHUNK = 512L shl 10 // 512 KiB — YouTube 403s larger per-request ranges
        private const val USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
                "(KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36"

        private val CLEN = Regex("""[?&]clen=(\d+)""")
        private val CONTENT_RANGE_TOTAL = Regex("""/(\d+)\s*$""")

        private fun clenOf(url: String): Long? = CLEN.find(url)?.groupValues?.get(1)?.toLongOrNull()
        private fun parseTotal(contentRange: String?): Long? =
            contentRange?.let { CONTENT_RANGE_TOTAL.find(it)?.groupValues?.get(1)?.toLongOrNull() }
        private fun stripRangeParam(url: String): String = url.replace(Regex("""[?&]range=\d+-\d+"""), "")

        /** Parses an HTTP `Range: bytes=start-[end]` into absolute [start, end] bounds within [total]. */
        private fun parseRequestedRange(header: String?, total: Long): Pair<Long, Long> {
            if (header == null) return 0L to (total - 1)
            val spec = header.substringAfter("bytes=", "").substringBefore(',')
            val start = spec.substringBefore('-').trim().toLongOrNull() ?: 0L
            val end = spec.substringAfter('-', "").trim().toLongOrNull() ?: (total - 1)
            return start.coerceIn(0, total - 1) to end.coerceIn(start, total - 1)
        }
    }
}
