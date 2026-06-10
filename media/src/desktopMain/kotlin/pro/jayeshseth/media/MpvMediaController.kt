package pro.jayeshseth.media

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.add
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.double
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.putJsonArray
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
 * binary that plays YouTube's `audio/webm` (opus) natively and runs headless (`--no-video`). Like the
 * Android player it can't talk to googlevideo directly (open-ended ranges → 403), so googlevideo URLs
 * are routed through [YoutubeStreamProxy] first.
 *
 * Control flows over a Unix-domain socket: we launch `mpv --idle --input-ipc-server=<sock>`, then send
 * newline-delimited JSON commands and `observe_property` for `time-pos`/`duration`/`pause`, which mpv
 * pushes back as `property-change` events to drive the state flows.
 */
internal class MpvMediaController : MediaController {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val proxy = YoutubeStreamProxy()
    private val json = Json { ignoreUnknownKeys = true }

    private val _playing = MutableStateFlow(false)
    private val _positionMs = MutableStateFlow(0L)
    private val _durationMs = MutableStateFlow(0L)
    override val playing: StateFlow<Boolean> = _playing
    override val positionMs: StateFlow<Long> = _positionMs
    override val durationMs: StateFlow<Long> = _durationMs

    private val socketPath: Path = Files.createTempFile("dc-mpv-", ".sock").also { Files.deleteIfExists(it) }
    private var process: Process? = null
    @Volatile private var channel: SocketChannel? = null

    init {
        process = ProcessBuilder(
            "mpv", "--idle=yes", "--no-video", "--no-terminal", "--really-quiet",
            "--no-config", "--input-ipc-server=$socketPath",
        ).redirectErrorStream(true).start()

        // Connect once mpv has created the socket (it does so asynchronously after launch).
        thread(isDaemon = true, name = "mpv-ipc") {
            val ch = connectWithRetry() ?: return@thread
            channel = ch
            send(observe(1, "time-pos"))
            send(observe(2, "duration"))
            send(observe(3, "pause"))
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
            "pause" -> _playing.value = (data as? JsonPrimitive)?.runCatching { boolean }?.getOrNull() == false
        }
    }

    override fun play(url: String) {
        val playUrl = if (url.contains("googlevideo.com")) runCatching { proxy.start(url) }.getOrDefault(url) else url
        send(buildJsonObject { putJsonArray("command") { add("loadfile"); add(playUrl); add("replace") } }.toString())
        send(setPause(false))
    }

    override fun pause() = send(setPause(true))
    override fun resume() = send(setPause(false))
    override fun stop() = send(buildJsonObject { putJsonArray("command") { add("stop") } }.toString())
    override fun seekTo(ms: Long) =
        send(buildJsonObject { putJsonArray("command") { add("seek"); add(ms / 1000.0); add("absolute") } }.toString())

    override fun release() {
        scope.cancel()
        runCatching { channel?.close() }
        runCatching { process?.destroy() }
        proxy.stop()
        runCatching { Files.deleteIfExists(socketPath) }
    }

    // ---- IPC helpers ----
    private fun send(command: String) {
        val ch = channel ?: return
        runCatching {
            synchronized(ch) { ch.write(ByteBuffer.wrap((command + "\n").toByteArray(StandardCharsets.UTF_8))) }
        }
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
