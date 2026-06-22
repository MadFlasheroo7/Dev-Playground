package pro.jayeshseth.media

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.add
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.double
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.putJsonArray
import java.io.File
import java.net.StandardProtocolFamily
import java.net.UnixDomainSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.SocketChannel
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import kotlin.concurrent.thread

/**
 * Desktop [MediaController] backed by **mpv** (the `mpv` binary, driven over its JSON IPC socket).
 *
 * Chosen over VLCJ because VLCJ needs native libVLC installed, whereas mpv is a single ubiquitous
 * binary that plays YouTube's `audio/webm` (opus) natively and runs headless (`--no-video`). Since
 * googlevideo can't be streamed directly (open-ended ranges → 403, range bursts get rate-limited), a
 * googlevideo URL is first **downloaded to a temp file** via [ChunkedDownloader], then mpv plays that
 * local file — playback then can't stall mid-track.
 *
 * Control flows over a Unix-domain socket: we launch `mpv --idle --input-ipc-server=<sock>`, then send
 * newline-delimited JSON commands and `observe_property` for `time-pos`/`duration`/`pause`, which mpv
 * pushes back as `property-change` events to drive the state flows.
 */
internal class MpvMediaController : MediaController {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val json = Json { ignoreUnknownKeys = true }
    @Volatile private var tempFile: File? = null
    /** Commands issued before the IPC socket connects; flushed on connect so an early tap isn't lost. */
    private val pending = mutableListOf<String>()

    override val canPlaySourceUrls = true

    private val _playing = MutableStateFlow(false)
    private val _positionMs = MutableStateFlow(0L)
    private val _durationMs = MutableStateFlow(0L)
    private val _buffering = MutableStateFlow(false)
    private val _error = MutableStateFlow<String?>(null)
    override val playing: StateFlow<Boolean> = _playing
    override val positionMs: StateFlow<Long> = _positionMs
    override val durationMs: StateFlow<Long> = _durationMs
    override val buffering: StateFlow<Boolean> = _buffering
    override val error: StateFlow<String?> = _error

    private val socketPath: Path = Files.createTempFile("dc-mpv-", ".sock").also { Files.deleteIfExists(it) }
    private var process: Process? = null
    @Volatile private var channel: SocketChannel? = null

    init {
        val proc = runCatching {
            ProcessBuilder(
                "mpv", "--idle=yes", "--no-video", "--no-terminal", "--really-quiet",
                "--no-config", "--input-ipc-server=$socketPath",
                // Bound read-ahead so mpv pulls data at roughly playback pace instead of slurping the whole
                // song in one burst — a burst of rapid range requests trips YouTube's rate limiter and a
                // chunk 403s mid-stream, cutting playback off after ~a minute.
                "--cache=yes", "--demuxer-readahead-secs=30", "--demuxer-max-bytes=8MiB", "--demuxer-max-back-bytes=4MiB",
            ).redirectErrorStream(true).start()
        }.getOrElse {
            _error.value = "mpv could not start: ${it.message}"
            null
        }
        process = proc

        // Connect once mpv has created the socket (it does so asynchronously after launch).
        if (proc != null) thread(isDaemon = true, name = "mpv-ipc") {
            val ch = connectWithRetry()
            if (ch == null) { _error.value = "Could not connect to mpv IPC socket"; return@thread }
            // Publish the channel and flush anything queued before connect (e.g. a quick tap).
            synchronized(pending) { channel = ch; pending.forEach { writeLine(ch, it) }; pending.clear() }
            writeLine(ch, observe(1, "time-pos"))
            writeLine(ch, observe(2, "duration"))
            writeLine(ch, observe(3, "pause"))
            writeLine(ch, observe(4, "paused-for-cache")) // true while buffering / waiting for the stream
            writeLine(ch, observe(5, "core-idle"))         // true when not actively decoding (loading/stalled)
            readLoop(ch)
        }
    }

    private fun connectWithRetry(): SocketChannel? {
        repeat(60) { // ~6s
            runCatching {
                val ch = SocketChannel.open(StandardProtocolFamily.UNIX)
                ch.connect(UnixDomainSocketAddress.of(socketPath))
                return ch
            }
            Thread.sleep(100)
        }
        return null
    }

    /** Reads newline-delimited JSON events from mpv and maps the observed properties to state flows. */
    private fun readLoop(ch: SocketChannel) {
        val buf = ByteBuffer.allocate(8192)
        val acc = StringBuilder()
        try {
            while (ch.isOpen) {
                buf.clear()
                if (ch.read(buf) < 0) break
                buf.flip()
                acc.append(StandardCharsets.UTF_8.decode(buf))
                var nl = acc.indexOf("\n")
                while (nl >= 0) {
                    handleLine(acc.substring(0, nl))
                    acc.delete(0, nl + 1)
                    nl = acc.indexOf("\n")
                }
            }
        } catch (_: Throwable) { /* socket closed on release */ }
    }

    private fun handleLine(line: String) {
        if (line.isBlank()) return
        val obj = runCatching { json.parseToJsonElement(line) as? JsonObject }.getOrNull() ?: return
        if ((obj["event"] as? JsonPrimitive)?.content != "property-change") return
        val name = (obj["name"] as? JsonPrimitive)?.content ?: return
        val data = obj["data"]
        when (name) {
            "time-pos" -> _positionMs.value = ((data as? JsonPrimitive)?.doubleOrZero() ?: 0.0).times(1000).toLong()
            "duration" -> _durationMs.value = ((data as? JsonPrimitive)?.doubleOrZero() ?: 0.0).times(1000).toLong()
            "pause" -> _playing.value = (data as? JsonPrimitive)?.boolOrNull() == false
            "paused-for-cache" -> if ((data as? JsonPrimitive)?.boolOrNull() == true) _buffering.value = true
                                  else if (_positionMs.value > 0) _buffering.value = false
            "core-idle" -> if ((data as? JsonPrimitive)?.boolOrNull() == false) _buffering.value = false
        }
    }

    /** Hand mpv a YouTube watch URL directly — its yt-dlp hook resolves an uncapped stream and plays it. */
    override fun playSource(youtubeUrl: String) {
        _error.value = null; _buffering.value = true
        _positionMs.value = 0; _durationMs.value = 0
        send(buildJsonObject { putJsonArray("command") { add("loadfile"); add(youtubeUrl); add("replace") } }.toString())
        send(setPause(false))
    }

    override fun play(url: String) {
        _error.value = null
        _buffering.value = true // downloading/loading until the first bytes decode
        _positionMs.value = 0; _durationMs.value = 0
        scope.launch {
            val playPath = try {
                if (url.contains("googlevideo.com")) {
                    runCatching {
                        val f = File.createTempFile("dc-mpv-", ".media")
                        tempFile?.delete(); tempFile = f
                        ChunkedDownloader.download(url, f)
                        f.absolutePath
                    }.getOrElse { url }
                } else url
            } catch (e: Exception) {
                _error.value = "Playback failed: ${e.message}"; _buffering.value = false
                return@launch
            }
            send(buildJsonObject { putJsonArray("command") { add("loadfile"); add(playPath); add("replace") } }.toString())
            send(setPause(false))
        }
    }

    override fun pause() = send(setPause(true))
    override fun resume() = send(setPause(false))
    override fun stop() = send(buildJsonObject { putJsonArray("command") { add("stop") } }.toString())
    override fun seekTo(ms: Long) =
        send(buildJsonObject { putJsonArray("command") { add("seek"); add(ms / 1000.0); add("absolute") } }.toString())
    override fun setVolume(volume: Float) =
        send(buildJsonObject { putJsonArray("command") { add("set_property"); add("volume"); add((volume * 100).toDouble()) } }.toString())

    override fun release() {
        scope.cancel()
        runCatching { channel?.close() }
        runCatching { process?.destroy() }
        runCatching { tempFile?.delete() }
        runCatching { Files.deleteIfExists(socketPath) }
    }

    // ---- IPC helpers ----
    private fun send(command: String) {
        synchronized(pending) {
            val ch = channel
            if (ch == null) { pending.add(command); return } // not connected yet — queue, flushed on connect
            writeLine(ch, command)
        }
    }

    private fun writeLine(ch: SocketChannel, command: String) {
        runCatching { ch.write(ByteBuffer.wrap((command + "\n").toByteArray(StandardCharsets.UTF_8))) }
    }

    private fun observe(id: Int, property: String): String =
        buildJsonObject { putJsonArray("command") { add("observe_property"); add(id); add(property) } }.toString()

    private fun setPause(paused: Boolean): String =
        buildJsonObject { putJsonArray("command") { add("set_property"); add("pause"); add(paused) } }.toString()

    companion object {
        /** True if the `mpv` binary is on PATH and runnable. */
        fun isAvailable(): Boolean = runCatching {
            ProcessBuilder("mpv", "--version").redirectErrorStream(true).start().waitFor() == 0
        }.getOrDefault(false)
    }
}

private fun JsonPrimitive.doubleOrZero(): Double = runCatching { double }.getOrDefault(0.0)
private fun JsonPrimitive.boolOrNull(): Boolean? = runCatching { boolean }.getOrNull()
