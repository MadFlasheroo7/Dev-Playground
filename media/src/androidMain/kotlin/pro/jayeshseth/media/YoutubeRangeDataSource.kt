package pro.jayeshseth.media

import android.net.Uri
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.BaseDataSource
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.DefaultHttpDataSource
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.io.InputStream

/**
 * # Why this exists
 *
 * YouTube's `…googlevideo.com/videoplayback` URLs **reject open-ended HTTP range requests**: a
 * `Range: bytes=0-` (what ExoPlayer and most players send to stream progressively) returns **HTTP 403**.
 * Only *bounded* ranges are accepted — either as a `Range: bytes=0-100000` header, or, equivalently and
 * more reliably, via YouTube's own **`&range=START-END` query parameter** on a plain GET (which returns
 * `HTTP 200` with exactly that byte range as the body). That query-param form is what this data source
 * uses, fetching the stream in fixed-size [CHUNK]s and stitching them into one continuous stream for
 * ExoPlayer. The total size is read from the URL's `clen` (content-length) parameter that YouTube
 * always includes, so [open] can report an exact length and seeking works.
 *
 * Any non-googlevideo URL (e.g. an HLS/DASH manifest fallback) is transparently delegated to a stock
 * [DefaultHttpDataSource], so this can safely be the single data source for the whole player.
 *
 * Verified against live URLs: `&range=0-100000` + plain GET → `HTTP 200 audio/webm`; open-ended → 403.
 */
@UnstableApi
internal class YoutubeRangeDataSource(
    private val client: OkHttpClient,
) : BaseDataSource(/* isNetwork = */ true) {

    private var spec: DataSpec? = null
    private var baseUrl: String = ""
    /** absolute byte offset of the next byte to deliver */
    private var position: Long = 0
    /** bytes still to deliver for this open(), or [C.LENGTH_UNSET] if unknown */
    private var bytesRemaining: Long = 0
    private var chunkStream: InputStream? = null
    /** bytes still readable from the currently-open chunk */
    private var chunkRemaining: Long = 0
    private var currentBody: okhttp3.ResponseBody? = null
    /** true once [transferStarted] has fired, so [close] only balances it then (avoids a bandwidth-meter NPE). */
    private var opened = false

    /** Non-null when the URL isn't a googlevideo stream and we just proxy a normal HTTP data source. */
    private var delegate: DefaultHttpDataSource? = null

    override fun open(dataSpec: DataSpec): Long {
        spec = dataSpec
        val full = dataSpec.uri.toString()

        if (dataSpec.uri.host?.contains("googlevideo.com") != true) {
            // Not a YouTube progressive URL — behave like an ordinary HTTP source.
            return DefaultHttpDataSource.Factory().createDataSource().also { delegate = it }.open(dataSpec)
        }

        transferInitializing(dataSpec)
        baseUrl = stripRangeParam(full)
        position = dataSpec.position
        val clen = clenOf(full)
        bytesRemaining = when {
            dataSpec.length != C.LENGTH_UNSET.toLong() -> dataSpec.length
            clen != null -> (clen - position).coerceAtLeast(0)
            else -> C.LENGTH_UNSET.toLong()
        }
        openChunk()
        transferStarted(dataSpec)
        opened = true
        return bytesRemaining
    }

    override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
        delegate?.let { return it.read(buffer, offset, length) }
        if (length == 0) return 0
        if (bytesRemaining == 0L) return C.RESULT_END_OF_INPUT
        if (chunkRemaining == 0L && !openChunk()) return C.RESULT_END_OF_INPUT
        if (chunkRemaining == 0L) return C.RESULT_END_OF_INPUT

        val cap = if (bytesRemaining == C.LENGTH_UNSET.toLong()) Long.MAX_VALUE else bytesRemaining
        val toRead = minOf(length.toLong(), chunkRemaining, cap).toInt()
        val read = chunkStream!!.read(buffer, offset, toRead)
        if (read == -1) { chunkRemaining = 0; return read(buffer, offset, length) }

        position += read
        chunkRemaining -= read
        if (bytesRemaining != C.LENGTH_UNSET.toLong()) bytesRemaining -= read
        bytesTransferred(read)
        return read
    }

    /** Opens the next [CHUNK]-sized range starting at [position]. Returns false at end of stream. */
    private fun openChunk(): Boolean {
        closeChunk()
        if (bytesRemaining == 0L) return false

        val rawEnd = position + CHUNK - 1
        val end = if (bytesRemaining != C.LENGTH_UNSET.toLong()) minOf(rawEnd, position + bytesRemaining - 1) else rawEnd
        val url = baseUrl + (if (baseUrl.contains('?')) '&' else '?') + "range=$position-$end"

        val response = client.newCall(
            Request.Builder().url(url).header("User-Agent", USER_AGENT).get().build()
        ).execute()

        if (response.code == 416) { response.close(); return false } // requested past end → done
        if (!response.isSuccessful) {
            val code = response.code
            response.close()
            throw IOException("YouTube range request failed: HTTP $code for $position-$end")
        }
        val body = response.body ?: run { response.close(); throw IOException("empty body") }
        currentBody = body
        chunkStream = body.byteStream()
        chunkRemaining = body.contentLength().let { if (it >= 0) it else (end - position + 1) }
        return true
    }

    private fun closeChunk() {
        runCatching { chunkStream?.close() }
        runCatching { currentBody?.close() }
        chunkStream = null
        currentBody = null
        chunkRemaining = 0
    }

    override fun close() {
        delegate?.let { it.close(); delegate = null; return }
        closeChunk()
        if (opened) { transferEnded(); opened = false }
        spec = null
    }

    override fun getUri(): Uri? = delegate?.uri ?: spec?.uri

    companion object {
        private const val CHUNK = 1L shl 20 // 1 MiB per range request (well under YouTube's limit)
        private const val USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
                "(KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36"

        private val CLEN = Regex("""[?&]clen=(\d+)""")
        private fun clenOf(url: String): Long? = CLEN.find(url)?.groupValues?.get(1)?.toLongOrNull()
        private fun stripRangeParam(url: String): String =
            url.replace(Regex("""[?&]range=\d+-\d+"""), "")
    }

    /** Builds [YoutubeRangeDataSource]s sharing one OkHttp client. */
    @UnstableApi
    class Factory(private val client: OkHttpClient) : DataSource.Factory {
        override fun createDataSource(): DataSource = YoutubeRangeDataSource(client)
    }
}
