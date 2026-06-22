package pro.jayeshseth.media

import okhttp3.ConnectionPool
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * Downloads a YouTube `googlevideo` stream to a local file using **sequential, bounded `Range` chunks,
 * each on a fresh TCP connection** — the recipe from AbleMusicPlayer's `ChunkedDownloader`.
 *
 * Why download instead of stream: YouTube 403s open-ended ranges and rate-limits *bursts* of range
 * requests. A media player buffering aggressively fires such a burst and stalls mid-song. Downloading
 * decouples fetching from playback: we pull chunks at our own pace, **retry rate-limit (403) responses
 * with backoff** until they ride through, and only then hand the player a plain, fully-local file that
 * can't stall. (Per-request size is capped ~512 KiB by YouTube, and reusing a connection gets the
 * follow-up ranges throttled — hence 256 KiB + a zero-size connection pool + `Connection: close`.)
 */
internal object ChunkedDownloader {

    private const val CHUNK = 256L * 1024
    private const val MAX_RETRIES = 7
    private const val PACE_MS = 120L // small gap between chunks so we don't burst into YouTube's rate limiter
    private const val USER_AGENT =
        "Mozilla/5.0 (Linux; Android 10; Quest 3) AppleWebKit/537.36 (KHTML, like Gecko) OculusBrowser/32.0.0.0.0 Mobile Safari/537.36"

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(2, TimeUnit.MINUTES)
        .connectionPool(ConnectionPool(0, 1, TimeUnit.MILLISECONDS)) // fresh TCP connection per request
        .build()

    fun interface Progress {
        fun onProgress(downloadedBytes: Long, totalBytes: Long)
    }

    /** Downloads [url] into [outputFile]. Blocking; call off the main thread. Throws if it can't finish. */
    fun download(url: String, outputFile: File, progress: Progress? = null) {
        val total = contentLength(url)
        outputFile.outputStream().buffered().use { out ->
            if (total <= 0) { // unknown length → single plain request
                fetchWithRetry(url, 0, null).use { it.body!!.byteStream().copyTo(out) }
                return
            }
            var downloaded = 0L
            while (downloaded < total) {
                val end = minOf(downloaded + CHUNK - 1, total - 1)
                fetchWithRetry(url, downloaded, end).use { resp ->
                    downloaded += resp.body!!.byteStream().copyTo(out)
                }
                progress?.onProgress(downloaded, total)
                if (downloaded < total) runCatching { Thread.sleep(PACE_MS) }
            }
        }
    }

    private fun contentLength(url: String): Long {
        val head = runCatching {
            client.newCall(
                Request.Builder().url(url).head()
                    .header("User-Agent", USER_AGENT).header("Connection", "close").build()
            ).execute().use { it.header("Content-Length")?.toLongOrNull() ?: -1L }
        }.getOrDefault(-1L)
        if (head > 0) return head
        // Fallback: YouTube embeds the content length in the URL's `clen` parameter.
        return Regex("""[?&]clen=(\d+)""").find(url)?.groupValues?.get(1)?.toLongOrNull() ?: -1L
    }

    /** Fetches one range, retrying transient failures / rate-limit (403) responses with linear backoff. */
    private fun fetchWithRetry(url: String, start: Long, end: Long?): Response {
        var attempt = 0
        while (true) {
            val result = runCatching {
                val b = Request.Builder().url(url)
                    .header("User-Agent", USER_AGENT)
                    .header("Connection", "close")
                if (end != null) b.header("Range", "bytes=$start-$end")
                client.newCall(b.get().build()).execute()
            }
            val resp = result.getOrNull()
            if (resp != null && resp.isSuccessful) return resp
            resp?.close()
            if (++attempt >= MAX_RETRIES) {
                throw result.exceptionOrNull()
                    ?: java.io.IOException("range $start-${end ?: ""} failed: HTTP ${resp?.code}")
            }
            // Exponential backoff (1s,2s,4s,…capped 20s) to ride out YouTube's rate-limit window.
            runCatching { Thread.sleep(minOf(1000L shl (attempt - 1), 20_000L)) }
        }
    }
}
