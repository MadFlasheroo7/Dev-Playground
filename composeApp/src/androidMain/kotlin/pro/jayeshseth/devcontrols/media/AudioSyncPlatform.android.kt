package pro.jayeshseth.devcontrols.media

import android.content.Context
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import pro.jayeshseth.devcontrols.util.WifiUtils
import pro.jayeshseth.media.MediaController
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.IOException
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketTimeoutException
import java.net.NetworkInterface

class AndroidAudioSyncManager(
    private val context: Context,
    private val mediaController: MediaController
) : AudioSyncManager {
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private val _isHost = MutableStateFlow(false)
    private val _isClient = MutableStateFlow(false)
    private val _isConnected = MutableStateFlow(false)
    private val _hostIp = MutableStateFlow<String?>(null)
    private val _connectedClients = MutableStateFlow<List<ClientInfo>>(emptyList())
    private val _syncedTitle = MutableStateFlow("")
    private val _syncedUploader = MutableStateFlow("")
    private val _playbackSyncState = MutableStateFlow(PlaybackSyncState.IDLE)
    private val _logConsole = MutableStateFlow<List<String>>(emptyList())
    private val _discoveredHosts = MutableStateFlow<List<Pair<String, String>>>(emptyList())

    override val isHost = _isHost.asStateFlow()
    override val isClient = _isClient.asStateFlow()
    override val isConnected = _isConnected.asStateFlow()
    override val hostIp = _hostIp.asStateFlow()
    override val clientIp: String
        get() = getLocalIpv4()
    override val connectedClients = _connectedClients.asStateFlow()
    override val syncedTitle = _syncedTitle.asStateFlow()
    override val syncedUploader = _syncedUploader.asStateFlow()
    override val playbackSyncState = _playbackSyncState.asStateFlow()
    override val logConsole = _logConsole.asStateFlow()
    override val discoveredHosts = _discoveredHosts.asStateFlow()

    override var clientLatencyNudgeMs: Long = 0L

    // Sockets & Jobs
    private var serverSocket: ServerSocket? = null
    private var clientSocket: Socket? = null
    private var serverJob: Job? = null
    private var clientJob: Job? = null
    private var udpBeaconJob: Job? = null
    private var udpReceiverJob: Job? = null
    private var clockSyncJob: Job? = null
    private var volumeReportingJob: Job? = null
    private var hostSyncJob: Job? = null
    private var audioServerJob: Job? = null

    // Local HTTP server socket for audio file sync
    private var audioServerSocket: ServerSocket? = null
    private var localAudioUri: String? = null

    // Synchronization fields
    private var clockOffsetMs: Long = 0L
    private var activeClients = mutableListOf<ClientSession>()
    private var currentUrl: String? = null
    private var currentYoutubeId: String? = null
    private var currentPlayingUrl: String? = null

    // Local Device Name
    private val deviceName = Build.MODEL ?: "Android Device"

    private fun log(msg: String) {
        val time = System.currentTimeMillis()
        val formattedTime = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.US).format(java.util.Date(time))
        _logConsole.value = (_logConsole.value + "[$formattedTime] $msg").takeLast(80)
    }

    private fun getLocalIpv4(): String {
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val ni = interfaces.nextElement()
                if (ni.isLoopback || !ni.isUp) continue
                val addrs = ni.inetAddresses
                while (addrs.hasMoreElements()) {
                    val addr = addrs.nextElement()
                    if (addr is java.net.Inet4Address && !addr.isLoopbackAddress && !addr.isLinkLocalAddress) {
                        return addr.hostAddress ?: "127.0.0.1"
                    }
                }
            }
        } catch (e: Exception) {}
        return "127.0.0.1"
    }

    private fun getSubnetBroadcastAddress(): InetAddress {
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val networkInterface = interfaces.nextElement()
                if (networkInterface.isLoopback || !networkInterface.isUp) continue
                for (interfaceAddress in networkInterface.interfaceAddresses) {
                    val broadcast = interfaceAddress.broadcast
                    if (broadcast != null) return broadcast
                }
            }
        } catch (e: Exception) {}
        return InetAddress.getByName("255.255.255.255")
    }

    // ==========================================
    // HOST MODE METHODS
    // ==========================================
    override fun startHost(port: Int) {
        if (_isHost.value) return
        stopAll()
        _isHost.value = true
        log("Starting host on $clientIp:$port")

        serverJob = scope.launch(Dispatchers.IO) {
            try {
                val ss = ServerSocket(port)
                serverSocket = ss
                log("[TCP] Server listening on port $port")
                while (isActive) {
                    val socket = ss.accept()
                    launchClientSession(socket)
                }
            } catch (e: Exception) {
                if (isActive) log("[TCP] Server error: ${e.message}")
            }
        }

        startUdpBroadcasting(port)
        startHostSyncLoop()
    }

    override fun stopHost() {
        if (!_isHost.value) return
        stopAll()
        log("Host stopped")
    }

    private fun startUdpBroadcasting(tcpPort: Int) {
        udpBeaconJob = scope.launch(Dispatchers.IO) {
            var udpSocket: DatagramSocket? = null
            try {
                udpSocket = DatagramSocket()
                udpSocket.broadcast = true
                val beaconMessage = "DEVAUDIOSYNC_BEACON|$clientIp|$deviceName|$tcpPort"
                val buffer = beaconMessage.toByteArray()
                val broadcastAddress = getSubnetBroadcastAddress()
                val packet = DatagramPacket(buffer, buffer.size, broadcastAddress, 8098)

                log("[UDP] Broadcasting beacons...")
                while (isActive) {
                    udpSocket.send(packet)
                    delay(2000)
                }
            } catch (e: Exception) {
                log("[UDP] Beacon broadcast error: ${e.message}")
            } finally {
                udpSocket?.close()
            }
        }
    }

    private fun startHostSyncLoop() {
        hostSyncJob = scope.launch(Dispatchers.IO) {
            while (isActive) {
                if (_isHost.value) {
                    val url = currentUrl ?: ""
                    val ytid = currentYoutubeId ?: ""
                    val title = _syncedTitle.value
                    val uploader = _syncedUploader.value
                    val pos = mediaController.positionMs.value
                    val hostTime = System.currentTimeMillis()
                    val playing = mediaController.playing.value
                    broadcastToClients("SYNC_STATE|$url|$title|$uploader|$pos|$hostTime|$playing|$ytid")
                }
                delay(1000)
            }
        }
    }

    private fun startAudioServer() {
        audioServerSocket?.close()
        audioServerJob?.cancel()
        audioServerJob = scope.launch(Dispatchers.IO) {
            try {
                val ss = ServerSocket(8090)
                audioServerSocket = ss
                log("[HTTP] Local audio file server started on port 8090")
                while (isActive) {
                    val socket = ss.accept()
                    launchAudioServerSession(socket)
                }
            } catch (e: Exception) {
                if (isActive) log("[HTTP] Local server stopped")
            }
        }
    }

    private fun launchAudioServerSession(socket: Socket) {
        scope.launch(Dispatchers.IO) {
            var outputStream: java.io.OutputStream? = null
            var rawInput: java.io.InputStream? = null
            try {
                rawInput = socket.getInputStream()
                
                // 1. Consume all request headers byte-by-byte to prevent BufferedReader blocking
                val headerBytes = java.io.ByteArrayOutputStream()
                var consecutiveNewlines = 0
                while (true) {
                    val b = rawInput.read()
                    if (b == -1) break
                    headerBytes.write(b)
                    val c = b.toChar()
                    if (c == '\n') {
                        consecutiveNewlines++
                        if (consecutiveNewlines >= 2) break
                    } else if (c != '\r') {
                        consecutiveNewlines = 0
                    }
                }

                val requestStr = headerBytes.toString("UTF-8")
                if (requestStr.isEmpty()) return@launch

                val lines = requestStr.split(Regex("\r?\n"))
                val requestLine = lines.getOrNull(0) ?: return@launch
                
                val headersMap = mutableMapOf<String, String>()
                for (i in 1 until lines.size) {
                    val line = lines[i]
                    if (line.trim().isEmpty()) continue
                    val colonIdx = line.indexOf(':')
                    if (colonIdx != -1) {
                        val key = line.substring(0, colonIdx).trim().lowercase()
                        val value = line.substring(colonIdx + 1).trim()
                        headersMap[key] = value
                    }
                }

                outputStream = socket.getOutputStream()
                val uriStr = localAudioUri
                if (uriStr == null) {
                    outputStream.write("HTTP/1.1 404 Not Found\r\nContent-Length: 0\r\nConnection: close\r\n\r\n".toByteArray())
                    return@launch
                }

                val (inputStream, size) = openAudioStream(uriStr)
                if (inputStream == null) {
                    outputStream.write("HTTP/1.1 404 Not Found\r\nContent-Length: 0\r\nConnection: close\r\n\r\n".toByteArray())
                    return@launch
                }

                // 2. Parse request line for Method and Path
                val requestParts = requestLine.split(" ")
                val method = requestParts.getOrNull(0) ?: "GET"
                
                // 3. Parse range header if present
                val rangeHeader = headersMap["range"]
                var startRange = 0L
                var endRange = size - 1
                var isRangeRequest = false

                if (rangeHeader != null && rangeHeader.startsWith("bytes=")) {
                    val rangeValue = rangeHeader.substring(6)
                    val dashIdx = rangeValue.indexOf('-')
                    if (dashIdx != -1) {
                        isRangeRequest = true
                        val startStr = rangeValue.substring(0, dashIdx).trim()
                        val endStr = rangeValue.substring(dashIdx + 1).trim()
                        if (startStr.isNotEmpty()) {
                            startRange = startStr.toLongOrNull() ?: 0L
                        }
                        if (endStr.isNotEmpty()) {
                            endRange = endStr.toLongOrNull() ?: (size - 1)
                        }
                    }
                }

                if (isRangeRequest) {
                    if (startRange < 0) startRange = 0
                    if (endRange >= size) endRange = size - 1
                    if (startRange > endRange) {
                        isRangeRequest = false
                    }
                }

                if (isRangeRequest) {
                    val contentLength = endRange - startRange + 1
                    val headers = "HTTP/1.1 206 Partial Content\r\n" +
                            "Content-Type: audio/mpeg\r\n" +
                            "Content-Length: $contentLength\r\n" +
                            "Content-Range: bytes $startRange-$endRange/$size\r\n" +
                            "Accept-Ranges: bytes\r\n" +
                            "Connection: close\r\n\r\n"
                    
                    outputStream.write(headers.toByteArray())
                    
                    if (method.equals("HEAD", ignoreCase = true)) {
                        inputStream.close()
                        return@launch
                    }
                    
                    inputStream.use { input ->
                        if (startRange > 0) {
                            var remainingToSkip = startRange
                            while (remainingToSkip > 0 && isActive) {
                                val skipped = input.skip(remainingToSkip)
                                if (skipped <= 0) {
                                    if (input.read() == -1) break
                                    remainingToSkip--
                                } else {
                                    remainingToSkip -= skipped
                                }
                            }
                        }
                        
                        val buffer = ByteArray(8192)
                        var bytesRemaining = contentLength
                        while (bytesRemaining > 0 && isActive) {
                            val toRead = minOf(buffer.size.toLong(), bytesRemaining).toInt()
                            val read = input.read(buffer, 0, toRead)
                            if (read == -1) break
                            outputStream.write(buffer, 0, read)
                            bytesRemaining -= read
                        }
                    }
                } else {
                    val headers = "HTTP/1.1 200 OK\r\n" +
                            "Content-Type: audio/mpeg\r\n" +
                            "Content-Length: $size\r\n" +
                            "Accept-Ranges: bytes\r\n" +
                            "Connection: close\r\n\r\n"
                    
                    outputStream.write(headers.toByteArray())
                    
                    if (method.equals("HEAD", ignoreCase = true)) {
                        inputStream.close()
                        return@launch
                    }
                    
                    inputStream.use { input ->
                        val buffer = ByteArray(8192)
                        var read: Int
                        while (input.read(buffer).also { read = it } != -1 && isActive) {
                            outputStream.write(buffer, 0, read)
                        }
                    }
                }
                outputStream.flush()
            } catch (e: Exception) {
                // Done
            } finally {
                runCatching { rawInput?.close() }
                runCatching { outputStream?.close() }
                runCatching { socket.close() }
            }
        }
    }

    private fun openAudioStream(uriStr: String): Pair<java.io.InputStream?, Long> {
        return try {
            if (uriStr.startsWith("file://") || uriStr.startsWith("/")) {
                val path = uriStr.substringAfter("file://")
                val file = java.io.File(path)
                if (file.exists() && file.isFile) {
                    file.inputStream() to file.length()
                } else {
                    null to 0L
                }
            } else {
                val uri = android.net.Uri.parse(uriStr)
                val resolver = context.contentResolver
                var size = 0L
                resolver.query(uri, null, null, null, null)?.use { cursor ->
                    val sizeIndex = cursor.getColumnIndex(android.provider.OpenableColumns.SIZE)
                    if (sizeIndex != -1 && cursor.moveToFirst()) {
                        size = cursor.getLong(sizeIndex)
                    }
                }
                resolver.openInputStream(uri) to size
            }
        } catch (e: Exception) {
            log("[HTTP] Failed to open local audio stream: ${e.message}")
            null to 0L
        }
    }

    override fun broadcastPlayLocal(audioUri: String, title: String) {
        if (!_isHost.value) return
        
        var localPath = audioUri
        var playUrl = audioUri
        if (audioUri.startsWith("content://")) {
            try {
                val uri = android.net.Uri.parse(audioUri)
                val tempFile = java.io.File(context.cacheDir, "temp_sync_audio.mp3")
                if (tempFile.exists()) tempFile.delete()
                
                context.contentResolver.openInputStream(uri)?.use { input ->
                    tempFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                localPath = tempFile.absolutePath
                playUrl = android.net.Uri.fromFile(tempFile).toString()
                log("[SYNC] Copied content URI to temp file: $localPath")
            } catch (e: Exception) {
                log("[SYNC] Failed to copy temp audio: ${e.message}")
            }
        }
        
        localAudioUri = localPath
        startAudioServer()

        val streamingUrl = "http://$clientIp:8090/audio.mp3"
        currentUrl = streamingUrl
        currentYoutubeId = "" // clear YouTube id for local audio
        _syncedTitle.value = title
        _syncedUploader.value = "Local Hosted Audio"
        _playbackSyncState.value = PlaybackSyncState.PLAYING

        log("[SYNC] Broadcast Local Audio PLAY: '$title'")
        broadcastToClients("SYNC_STATE|$streamingUrl|$title|Local Hosted Audio|0|${System.currentTimeMillis()}|true|")

        // Play locally using the local cache URL
        scope.launch {
            mediaController.play(playUrl)
        }
    }

    private fun launchClientSession(socket: Socket) {
        scope.launch(Dispatchers.IO) {
            var reader: BufferedReader? = null
            var writer: BufferedWriter? = null
            var session: ClientSession? = null
            val clientAddress = socket.inetAddress.hostAddress ?: "Unknown IP"
            try {
                reader = socket.getInputStream().bufferedReader()
                writer = socket.getOutputStream().bufferedWriter()
                
                log("[TCP] Accepted connection from $clientAddress")

                // Wait for REGISTER packet
                val regLine = reader.readLine() ?: return@launch
                if (regLine.startsWith("REGISTER|")) {
                    val parts = regLine.split("|")
                    val name = parts.getOrNull(1) ?: "Client"
                    val id = parts.getOrNull(2) ?: clientAddress

                    session = ClientSession(id, socket, clientAddress, name, writer)
                    synchronized(activeClients) {
                        activeClients.removeAll { it.id == id }
                        activeClients.add(session)
                    }
                    updateConnectedClientsState()
                    log("[TCP] Client registered: '$name' ($clientAddress)")

                    val url = currentUrl ?: ""
                    val ytid = currentYoutubeId ?: ""
                    val title = _syncedTitle.value
                    val uploader = _syncedUploader.value
                    val pos = mediaController.positionMs.value
                    val hostTime = System.currentTimeMillis()
                    val playing = mediaController.playing.value
                    session.send("SYNC_STATE|$url|$title|$uploader|$pos|$hostTime|$playing|$ytid")
                } else {
                    socket.close()
                    return@launch
                }

                while (isActive) {
                    val line = reader.readLine() ?: break
                    handleClientMessage(session, line)
                }
            } catch (e: Exception) {
                // Closed
            } finally {
                socket.close()
                session?.let {
                    synchronized(activeClients) { activeClients.remove(it) }
                    updateConnectedClientsState()
                    log("[TCP] Client disconnected: '${it.name}' (${it.ip})")
                }
            }
        }
    }

    private fun handleClientMessage(session: ClientSession, line: String) {
        val parts = line.split("|")
        val cmd = parts.getOrNull(0) ?: return
        when (cmd) {
            "PING" -> {
                val clientTime = parts.getOrNull(1) ?: "0"
                val hostTime = System.currentTimeMillis().toString()
                session.send("PONG|$clientTime|$hostTime")
            }
            "STATUS" -> {
                val vol = parts.getOrNull(1)?.toFloatOrNull() ?: 1.0f
                val isBuffering = parts.getOrNull(2)?.toBoolean() ?: false
                val pos = parts.getOrNull(3)?.toLongOrNull() ?: 0L
                val rtt = parts.getOrNull(4)?.toLongOrNull() ?: 0L

                session.volume = vol
                session.isBuffering = isBuffering
                session.positionMs = pos
                session.latencyMs = rtt
                updateConnectedClientsState()
            }
        }
    }

    private fun updateConnectedClientsState() {
        val list = synchronized(activeClients) {
            activeClients.map {
                ClientInfo(it.id, it.ip, it.name, it.volume, it.latencyMs, it.isBuffering, it.positionMs)
            }
        }
        _connectedClients.value = list
    }

    private fun broadcastToClients(msg: String) {
        val targets = synchronized(activeClients) { activeClients.toList() }
        targets.forEach { it.send(msg) }
    }

    override fun broadcastPlay(youtubeUrl: String, title: String, uploader: String, positionMs: Long) {
        if (!_isHost.value) return
        val parts = youtubeUrl.split("|")
        val youtubeId = parts.getOrNull(0) ?: youtubeUrl
        val resolvedUrl = parts.getOrNull(1) ?: youtubeUrl

        currentUrl = resolvedUrl
        currentYoutubeId = youtubeId
        _syncedTitle.value = title
        _syncedUploader.value = uploader
        _playbackSyncState.value = PlaybackSyncState.PLAYING

        log("[SYNC] Broadcast PLAY: '$title'")
        broadcastToClients("SYNC_STATE|$resolvedUrl|$title|$uploader|$positionMs|${System.currentTimeMillis()}|true|$youtubeId")

        // Play locally
        scope.launch {
            val isYoutube = youtubeId.contains("youtube.com") || youtubeId.contains("youtu.be") || (!youtubeId.startsWith("http") && youtubeId.length == 11)
            if (mediaController.canPlaySourceUrls && isYoutube) {
                mediaController.playSource(youtubeId)
            } else {
                mediaController.play(resolvedUrl)
            }
            mediaController.seekTo(positionMs)
        }
    }

    override fun broadcastPause() {
        if (!_isHost.value) return
        _playbackSyncState.value = PlaybackSyncState.PAUSED
        log("[SYNC] Broadcast PAUSE")
        mediaController.pause()
        val url = currentUrl ?: ""
        val ytid = currentYoutubeId ?: ""
        broadcastToClients("SYNC_STATE|$url|${_syncedTitle.value}|${_syncedUploader.value}|${mediaController.positionMs.value}|${System.currentTimeMillis()}|false|$ytid")
    }

    override fun broadcastResume(positionMs: Long) {
        if (!_isHost.value) return
        _playbackSyncState.value = PlaybackSyncState.PLAYING
        log("[SYNC] Broadcast RESUME")
        mediaController.resume()
        mediaController.seekTo(positionMs)
        val url = currentUrl ?: ""
        val ytid = currentYoutubeId ?: ""
        broadcastToClients("SYNC_STATE|$url|${_syncedTitle.value}|${_syncedUploader.value}|$positionMs|${System.currentTimeMillis()}|true|$ytid")
    }

    override fun broadcastSeek(positionMs: Long) {
        if (!_isHost.value) return
        mediaController.seekTo(positionMs)
        val url = currentUrl ?: ""
        val ytid = currentYoutubeId ?: ""
        val playing = mediaController.playing.value
        broadcastToClients("SYNC_STATE|$url|${_syncedTitle.value}|${_syncedUploader.value}|$positionMs|${System.currentTimeMillis()}|$playing|$ytid")
    }

    override fun setClientVolume(clientId: String, volume: Float) {
        val client = synchronized(activeClients) { activeClients.firstOrNull { it.id == clientId } }
        client?.let {
            log("Setting client '${it.name}' volume to ${(volume * 100).toInt()}%")
            it.send("SET_VOLUME|$volume")
            it.volume = volume
            updateConnectedClientsState()
        }
    }

    override fun setMasterVolume(volume: Float) {
        log("Setting master volume to ${(volume * 100).toInt()}%")
        broadcastToClients("SET_VOLUME|$volume")
        mediaController.setVolume(volume)
        synchronized(activeClients) {
            activeClients.forEach { it.volume = volume }
        }
        updateConnectedClientsState()
    }

    // ==========================================
    // CLIENT MODE METHODS
    // ==========================================
    override fun startClient(hostIp: String, port: Int) {
        _isClient.value = true
        if (hostIp.isEmpty()) {
            _isConnected.value = false
            return
        }

        stopClientConnectionOnly()
        log("Connecting to Host at $hostIp:$port")

        clientJob = scope.launch(Dispatchers.IO) {
            var socket: Socket? = null
            try {
                socket = Socket(hostIp, port)
                clientSocket = socket
                _isConnected.value = true
                _hostIp.value = hostIp
                log("[TCP] Connected to host at $hostIp. Sending registration...")

                val writer = socket.getOutputStream().bufferedWriter()
                val reader = socket.getInputStream().bufferedReader()

                synchronized(writer) {
                    writer.write("REGISTER|$deviceName|$clientIp\n")
                    writer.flush()
                }

                startClockSync(writer)
                startStatusReporting(writer)

                while (isActive) {
                    val line = reader.readLine() ?: break
                    handleHostMessage(line)
                }
            } catch (e: Exception) {
                if (isActive) log("[TCP] Connection error: ${e.message}")
            } finally {
                socket?.close()
                _isConnected.value = false
                log("[TCP] Disconnected from host")
            }
        }
    }

    override fun stopClient() {
        if (!_isClient.value) return
        stopAll()
        log("Client stopped")
    }

    private fun stopClientConnectionOnly() {
        _isConnected.value = false
        runCatching { clientSocket?.close() }
        clientSocket = null
        clientJob?.cancel(); clientJob = null
        clockSyncJob?.cancel(); clockSyncJob = null
        volumeReportingJob?.cancel(); volumeReportingJob = null
        currentPlayingUrl = null
    }

    override fun startAutoDiscovery() {
        _discoveredHosts.value = emptyList()
        udpReceiverJob = scope.launch(Dispatchers.IO) {
            var socket: DatagramSocket? = null
            try {
                socket = DatagramSocket(8098)
                socket.soTimeout = 3000
                val buffer = ByteArray(1024)
                val packet = DatagramPacket(buffer, buffer.size)

                log("[UDP] Scanning for hosts...")
                while (isActive) {
                    try {
                        socket.receive(packet)
                        val msg = String(packet.data, 0, packet.length).trim()
                        if (msg.startsWith("DEVAUDIOSYNC_BEACON|")) {
                            val parts = msg.split("|")
                            val ip = parts.getOrNull(1)
                            val name = parts.getOrNull(2)
                            if (ip != null && name != null && ip != clientIp) {
                                val current = _discoveredHosts.value
                                if (current.none { it.first == ip }) {
                                    _discoveredHosts.value = current + (ip to name)
                                    log("[UDP] Discovered host: '$name' at $ip")
                                }
                            }
                        }
                    } catch (e: SocketTimeoutException) {
                        // ignore
                    }
                }
            } catch (e: Exception) {
                if (isActive) log("[UDP] Scanner stopped: ${e.message}")
            } finally {
                socket?.close()
            }
        }
    }

    override fun stopAutoDiscovery() {
        udpReceiverJob?.cancel()
        udpReceiverJob = null
        log("[UDP] Scanner stopped")
    }

    private fun startClockSync(writer: BufferedWriter) {
        clockSyncJob = scope.launch(Dispatchers.IO) {
            while (isActive) {
                try {
                    val t0 = System.currentTimeMillis()
                    synchronized(writer) {
                        writer.write("PING|$t0\n")
                        writer.flush()
                    }
                    delay(5000)
                } catch (e: Exception) {
                    break
                }
            }
        }
    }

    private fun startStatusReporting(writer: BufferedWriter) {
        volumeReportingJob = scope.launch(Dispatchers.IO) {
            while (isActive) {
                try {
                    val volumeVal = 1.0f 
                    val isBuffering = mediaController.buffering.value
                    val curPos = mediaController.positionMs.value
                    synchronized(writer) {
                        writer.write("STATUS|$volumeVal|$isBuffering|$curPos|${clockOffsetMs}\n")
                        writer.flush()
                    }
                    delay(1000)
                } catch (e: Exception) {
                    break
                }
            }
        }
    }

    private fun handleHostMessage(line: String) {
        scope.launch {
            val parts = line.split("|")
            val cmd = parts.getOrNull(0) ?: return@launch
            when (cmd) {
                "PONG" -> {
                    val t0 = parts.getOrNull(1)?.toLongOrNull() ?: 0L
                    val hostTime = parts.getOrNull(2)?.toLongOrNull() ?: 0L
                    val t3 = System.currentTimeMillis()
                    val rtt = t3 - t0
                    val estimatedHostTime = hostTime + (rtt / 2)
                    clockOffsetMs = estimatedHostTime - t3
                }
                "SYNC_STATE" -> {
                    val url = parts.getOrNull(1) ?: return@launch
                    val title = parts.getOrNull(2) ?: ""
                    val uploader = parts.getOrNull(3) ?: ""
                    val posMs = parts.getOrNull(4)?.toLongOrNull() ?: 0L
                    val hostTime = parts.getOrNull(5)?.toLongOrNull() ?: 0L
                    val isPlaying = parts.getOrNull(6)?.toBoolean() ?: false
                    val youtubeId = parts.getOrNull(7) ?: ""

                    _syncedTitle.value = title
                    _syncedUploader.value = uploader
                    _playbackSyncState.value = if (isPlaying) PlaybackSyncState.PLAYING else PlaybackSyncState.PAUSED

                    var finalUrl = url
                    if (url.startsWith("http://") && url.contains(":8090")) {
                        val actualHost = _hostIp.value
                        if (actualHost != null && actualHost != "127.0.0.1" && actualHost != "localhost") {
                            val portAndPath = url.substringAfter("://").substringAfter("/", "")
                            val port = url.substringAfter("://").substringBefore("/").substringAfter(":", "8090")
                            finalUrl = "http://$actualHost:$port/$portAndPath"
                        }
                    }

                    if (finalUrl.isNotEmpty() && finalUrl != currentPlayingUrl) {
                        currentPlayingUrl = finalUrl
                        val isYoutube = youtubeId.contains("youtube.com") || youtubeId.contains("youtu.be") || (!youtubeId.startsWith("http") && youtubeId.length == 11)
                        if (mediaController.canPlaySourceUrls && isYoutube) {
                            mediaController.playSource(youtubeId)
                        } else {
                            mediaController.play(finalUrl)
                        }
                        log("[SYNC] Loading track: '$title'")
                    } else if (finalUrl.isEmpty()) {
                        currentPlayingUrl = null
                        mediaController.stop()
                    }

                    if (url.isNotEmpty() && !mediaController.buffering.value) {
                        if (isPlaying && !mediaController.playing.value) {
                            mediaController.resume()
                        } else if (!isPlaying && mediaController.playing.value) {
                            mediaController.pause()
                        }

                        val localTime = System.currentTimeMillis()
                        val hostTimeElapsed = localTime + clockOffsetMs - hostTime
                        val expectedPos = if (isPlaying) posMs + hostTimeElapsed else posMs
                        val currentPos = mediaController.positionMs.value

                        val drift = kotlin.math.abs(currentPos - expectedPos)
                        if (drift > 250) { // 250ms threshold
                            val targetSeek = expectedPos + clientLatencyNudgeMs
                            mediaController.seekTo(targetSeek.coerceAtLeast(0))
                            log("[SYNC] Corrected drift: ${drift}ms (Synced seek to: ${targetSeek}ms)")
                        }
                    }
                }
                "SET_VOLUME" -> {
                    val vol = parts.getOrNull(1)?.toFloatOrNull() ?: 1.0f
                    log("[SYNC] Synced volume: ${(vol * 100).toInt()}%")
                    mediaController.setVolume(vol)
                }
            }
        }
    }

    private fun stopAll() {
        _isHost.value = false
        _isClient.value = false
        _isConnected.value = false
        _hostIp.value = null
        _connectedClients.value = emptyList()
        currentUrl = null
        currentYoutubeId = null
        currentPlayingUrl = null
        localAudioUri = null

        runCatching { serverSocket?.close() }
        runCatching { clientSocket?.close() }
        runCatching { audioServerSocket?.close() }
        serverSocket = null
        clientSocket = null
        audioServerSocket = null

        serverJob?.cancel(); serverJob = null
        clientJob?.cancel(); clientJob = null
        udpBeaconJob?.cancel(); udpBeaconJob = null
        udpReceiverJob?.cancel(); udpReceiverJob = null
        clockSyncJob?.cancel(); clockSyncJob = null
        volumeReportingJob?.cancel(); volumeReportingJob = null
        hostSyncJob?.cancel(); hostSyncJob = null
        audioServerJob?.cancel(); audioServerJob = null

        synchronized(activeClients) {
            activeClients.forEach { runCatching { it.socket.close() } }
            activeClients.clear()
        }
        updateConnectedClientsState()
    }

    override fun release() {
        stopAll()
        scope.cancel()
    }

    private class ClientSession(
        val id: String,
        val socket: Socket,
        val ip: String,
        val name: String,
        private val writer: BufferedWriter
    ) {
        var volume: Float = 1.0f
        var latencyMs: Long = 0L
        var isBuffering: Boolean = false
        var positionMs: Long = 0L

        fun send(msg: String) {
            synchronized(writer) {
                try {
                    writer.write(msg + "\n")
                    writer.flush()
                } catch (e: Exception) {}
            }
        }
    }
}

@Composable
actual fun rememberAudioSyncManager(mediaController: MediaController): AudioSyncManager {
    val context = LocalContext.current.applicationContext
    return remember(mediaController) { AndroidAudioSyncManager(context, mediaController) }
}

@Composable
actual fun rememberAudioFilePicker(onFilePicked: (String, String) -> Unit): () -> Unit {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            val name = getFileName(context, uri) ?: "Local Audio"
            onFilePicked(uri.toString(), name)
        }
    }
    return { launcher.launch("audio/*") }
}

private fun getFileName(context: Context, uri: android.net.Uri): String? {
    if (uri.scheme == "content") {
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (nameIndex != -1) return cursor.getString(nameIndex)
            }
        }
    }
    return uri.path?.substringAfterLast('/')
}
