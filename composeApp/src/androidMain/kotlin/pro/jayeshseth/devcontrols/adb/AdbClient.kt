package pro.jayeshseth.devcontrols.adb

import android.content.Context
import android.os.SystemClock
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.InetSocketAddress
import java.net.Socket

/**
 * High-level facade around [AdbConnectionManagerImpl]. Single-threaded by a
 * mutex to serialise pair/connect/exec — the underlying ADB stream object
 * isn't safe to drive from multiple coroutines.
 */
class AdbClient private constructor(private val app: Context) {

    enum class State { Disconnected, Connecting, Connected, NeedsPair, Failed }

    private val _state = MutableStateFlow(State.Disconnected)
    val state: StateFlow<State> = _state.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val mutex = Mutex()

    suspend fun pair(host: String, port: Int, code: String): Result<Unit> = withContext(Dispatchers.IO) {
        mutex.withLock {
            // Pre-flight: confirm something is listening. The pairing port is only
            // alive while the system "Pair device with pairing code" dialog is on
            // screen on the phone. If we get connection refused here, the dialog
            // is closed and the user needs to re-open it.
            val reachable = runCatching {
                Socket().use { s ->
                    s.connect(InetSocketAddress(host, port), 1500)
                    true
                }
            }.getOrDefault(false)
            if (!reachable) {
                val msg = "No service listening on $host:$port. " +
                    "Reopen the phone's 'Pair device with pairing code' dialog and try again — " +
                    "the port is only valid while that dialog is on screen."
                _error.value = msg
                return@withLock Result.failure(IOException(msg))
            }
            runCatching {
                val mgr = AdbConnectionManagerImpl.get(app)
                val ok = mgr.pair(host, port, code)
                check(ok) { "pair() returned false (wrong code, or dialog timed out)" }
            }.onFailure {
                _error.value = it.message
                Log.w(TAG, "pair failed", it)
            }
        }
    }

    suspend fun connect(timeoutMs: Long = 10_000L): Result<Unit> = withContext(Dispatchers.IO) {
        mutex.withLock {
            if (_state.value == State.Connected) return@withLock Result.success(Unit)
            _state.value = State.Connecting
            runCatching {
                val mgr = AdbConnectionManagerImpl.get(app)
                if (mgr.isConnected) {
                    _state.value = State.Connected
                    return@runCatching
                }
                val ok = mgr.autoConnect(app, timeoutMs)
                check(ok) { "autoConnect returned false" }
                _state.value = State.Connected
            }.onFailure {
                val msg = it.message ?: it.javaClass.simpleName
                if (msg.contains("pair", ignoreCase = true) ||
                    it.javaClass.simpleName.contains("Pairing", ignoreCase = true)
                ) {
                    _state.value = State.NeedsPair
                } else {
                    _state.value = State.Failed
                }
                _error.value = msg
                Log.w(TAG, "connect failed", it)
            }
        }
    }

    suspend fun exec(command: String): AdbExecResult = withContext(Dispatchers.IO) {
        mutex.withLock {
            val start = SystemClock.elapsedRealtime()
            try {
                val mgr = AdbConnectionManagerImpl.get(app)
                if (!mgr.isConnected) {
                    val ok = mgr.autoConnect(app, 8_000L)
                    if (!ok) {
                        _state.value = State.NeedsPair
                        return@withLock AdbExecResult(
                            command = command,
                            output = "",
                            durationMs = SystemClock.elapsedRealtime() - start,
                            ok = false,
                            error = "Not connected — pair the device first."
                        )
                    }
                    _state.value = State.Connected
                }
                val stream = mgr.openStream("shell:$command")
                // Read robustly: copy bytes into a buffer so a mid-read close
                // (e.g. `exit 0` inside the shell, or a long-running command
                // that the device terminates) returns whatever was emitted
                // instead of throwing "Stream closed" and dropping all output.
                val buf = java.io.ByteArrayOutputStream()
                var midReadError: String? = null
                try {
                    val ins = stream.openInputStream()
                    val chunk = ByteArray(8 * 1024)
                    while (true) {
                        val n = try {
                            ins.read(chunk)
                        } catch (closed: Throwable) {
                            midReadError = closed.message ?: closed.javaClass.simpleName
                            -1
                        }
                        if (n <= 0) break
                        buf.write(chunk, 0, n)
                    }
                } finally {
                    runCatching { stream.close() }
                }
                val text = buf.toString(Charsets.UTF_8)
                AdbExecResult(
                    command = command,
                    output = if (midReadError != null && text.isEmpty())
                        "(no output before stream closed: $midReadError)"
                    else text,
                    durationMs = SystemClock.elapsedRealtime() - start,
                    ok = true
                )
            } catch (t: Throwable) {
                Log.w(TAG, "exec failed for '$command'", t)
                if (t.javaClass.simpleName.contains("Pairing", ignoreCase = true)) {
                    _state.value = State.NeedsPair
                }
                AdbExecResult(
                    command = command,
                    output = "",
                    durationMs = SystemClock.elapsedRealtime() - start,
                    ok = false,
                    error = t.message ?: t.javaClass.simpleName
                )
            }
        }
    }

    fun disconnect() {
        runCatching { AdbConnectionManagerImpl.get(app).disconnect() }
        _state.value = State.Disconnected
    }

    fun hasStoredKeys(): Boolean = AdbConnectionManagerImpl.hasStoredKeys(app)

    companion object {
        private const val TAG = "AdbClient"

        @Volatile
        private var instance: AdbClient? = null

        fun get(context: Context): AdbClient =
            instance ?: synchronized(this) {
                instance ?: AdbClient(context.applicationContext).also { instance = it }
            }
    }
}
