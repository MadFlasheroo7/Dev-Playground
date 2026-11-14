package pro.jayeshseth.devcontrols.ui.hyprdeck

import java.io.BufferedReader
import java.io.ByteArrayOutputStream
import java.io.File
import java.net.UnixDomainSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.Channels
import java.nio.channels.SocketChannel
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

object HyprIpc {

    val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    private var cachedSignature: String? = null

    /**
     * Resolves the active Hyprland instance signature, checking environment variables
     * first and falling back to directory scanning in $XDG_RUNTIME_DIR/hypr/.
     */
    fun getInstanceSignature(): String? {
        cachedSignature?.let { sig ->
            if (getSocketPathForSignature(sig, isEventSocket = false)?.let { Files.exists(it) } == true) {
                return sig
            }
        }

        // 1. Try env variable
        val envSig = System.getenv("HYPRLAND_INSTANCE_SIGNATURE")
        if (!envSig.isNullOrBlank()) {
            val sock = getSocketPathForSignature(envSig, isEventSocket = false)
            if (sock != null && Files.exists(sock)) {
                cachedSignature = envSig
                return envSig
            }
        }

        // 2. Discover from runtime directory
        val runtimeDir = System.getenv("XDG_RUNTIME_DIR") ?: "/run/user/${getUid()}"
        val hyprDir = File(runtimeDir, "hypr")
        if (hyprDir.exists() && hyprDir.isDirectory) {
            val validDirs = hyprDir.listFiles { dir ->
                dir.isDirectory && File(dir, ".socket.sock").exists()
            }
            val newest = validDirs?.maxByOrNull { it.lastModified() }
            if (newest != null) {
                cachedSignature = newest.name
                return newest.name
            }
        }

        return null
    }

    private fun getUid(): String {
        return try {
            val userName = System.getProperty("user.name")
            val p = ProcessBuilder("id", "-u", userName).start()
            p.inputStream.bufferedReader().readText().trim().ifEmpty { "1000" }
        } catch (_: Exception) {
            "1000"
        }
    }

    fun isAvailable(): Boolean = getInstanceSignature() != null

    fun getSocketPath(isEventSocket: Boolean): Path? {
        val sig = getInstanceSignature() ?: return null
        return getSocketPathForSignature(sig, isEventSocket)
    }

    private fun getSocketPathForSignature(signature: String, isEventSocket: Boolean): Path? {
        val runtimeDir = System.getenv("XDG_RUNTIME_DIR") ?: "/run/user/${getUid()}"
        val sockName = if (isEventSocket) ".socket2.sock" else ".socket.sock"
        val path = Paths.get(runtimeDir, "hypr", signature, sockName)
        return path
    }

    /**
     * Sends a raw command to Hyprland's request socket (.socket.sock) and returns the text response.
     */
    suspend fun sendRaw(command: String): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val sockPath = getSocketPath(isEventSocket = false)
                ?: error("Hyprland socket not found. Is Hyprland running?")

            val address = UnixDomainSocketAddress.of(sockPath)
            SocketChannel.open(address).use { channel ->
                val bytes = command.toByteArray(Charsets.UTF_8)
                val writeBuf = ByteBuffer.wrap(bytes)
                while (writeBuf.hasRemaining()) {
                    channel.write(writeBuf)
                }
                channel.shutdownOutput()

                val baos = ByteArrayOutputStream()
                val readBuf = ByteBuffer.allocate(8192)
                while (true) {
                    val read = channel.read(readBuf)
                    if (read == -1) break
                    readBuf.flip()
                    baos.write(readBuf.array(), readBuf.arrayOffset() + readBuf.position(), readBuf.remaining())
                    readBuf.clear()
                }
                baos.toString(Charsets.UTF_8)
            }
        }
    }

    suspend fun getWorkspaces(): Result<List<HyprWorkspace>> = runCatching {
        val raw = sendRaw("j/workspaces").getOrThrow()
        json.decodeFromString<List<HyprWorkspace>>(raw)
    }

    suspend fun getClients(): Result<List<HyprClient>> = runCatching {
        val raw = sendRaw("j/clients").getOrThrow()
        json.decodeFromString<List<HyprClient>>(raw)
    }

    suspend fun getMonitors(): Result<List<HyprMonitor>> = runCatching {
        val raw = sendRaw("j/monitors").getOrThrow()
        json.decodeFromString<List<HyprMonitor>>(raw)
    }

    suspend fun getActiveWindow(): Result<HyprActiveWindow?> = runCatching {
        val raw = sendRaw("j/activewindow").getOrThrow()
        if (raw.trim() == "{}" || raw.isBlank()) null
        else json.decodeFromString<HyprActiveWindow>(raw)
    }

    suspend fun getVersion(): Result<String> = runCatching {
        sendRaw("version").getOrThrow()
    }

    suspend fun dispatch(command: String, args: String = ""): Result<String> {
        val full = if (args.isBlank()) "dispatch $command" else "dispatch $command $args"
        return sendRaw(full)
    }

    suspend fun focusWindow(address: String): Result<String> =
        dispatch("focuswindow", "address:$address")

    suspend fun moveWindowToWorkspace(address: String, workspaceId: Int, silent: Boolean = true): Result<String> {
        val cmd = if (silent) "movetoworkspacesilent" else "movetoworkspace"
        return dispatch(cmd, "$workspaceId,address:$address")
    }

    suspend fun toggleFloating(address: String): Result<String> =
        dispatch("togglefloating", "address:$address")

    suspend fun toggleFullscreen(address: String, mode: Int = 1): Result<String> {
        // Ensure the target window is focused first, then toggle fullscreen
        focusWindow(address)
        return dispatch("fullscreen", mode.toString())
    }

    suspend fun pinWindow(address: String): Result<String> =
        dispatch("pin", "address:$address")

    suspend fun closeWindow(address: String): Result<String> =
        dispatch("closewindow", "address:$address")

    suspend fun switchWorkspace(workspaceId: Int): Result<String> =
        dispatch("workspace", workspaceId.toString())

    suspend fun focusMonitor(monitorName: String): Result<String> =
        dispatch("focusmonitor", monitorName)

    /**
     * Streams real-time Hyprland Wayland events from .socket2.sock as a reactive Flow.
     * Reconnects automatically if disconnected while the coroutine remains active.
     */
    fun eventFlow(): Flow<HyprEvent> = flow {
        while (currentCoroutineContext().isActive) {
            val sockPath = getSocketPath(isEventSocket = true)
            if (sockPath == null || !Files.exists(sockPath)) {
                delay(1500)
                continue
            }

            try {
                val address = UnixDomainSocketAddress.of(sockPath)
                SocketChannel.open(address).use { channel ->
                    val reader = BufferedReader(Channels.newReader(channel, Charsets.UTF_8))
                    while (currentCoroutineContext().isActive) {
                        val line = reader.readLine() ?: break
                        if (line.isNotBlank()) {
                            val delimiterIdx = line.indexOf(">>")
                            val (name, data) = if (delimiterIdx != -1) {
                                line.substring(0, delimiterIdx) to line.substring(delimiterIdx + 2)
                            } else {
                                "raw" to line
                            }
                            emit(HyprEvent(name = name, data = data, timestamp = System.currentTimeMillis()))
                        }
                    }
                }
            } catch (_: Exception) {
                // Connection closed or socket reset, retry after backoff
                delay(1000)
            }
        }
    }.flowOn(Dispatchers.IO)
}
