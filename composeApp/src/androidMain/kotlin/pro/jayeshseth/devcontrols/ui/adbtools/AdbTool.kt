package pro.jayeshseth.devcontrols.ui.adbtools

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import pro.jayeshseth.devcontrols.adb.AdbClient
import pro.jayeshseth.devcontrols.adb.AdbExecResult
import pro.jayeshseth.devcontrols.adb.PairingService
import pro.jayeshseth.devcontrols.data.AdbConsolePrefs

/**
 * Drives shell commands for an ADB tool screen: exposes [connected], a [busy] flag,
 * and a [run] helper that execs a command off the main thread and surfaces a snackbar
 * (output preview on success, error on failure). [lastOutput] holds the full stdout for
 * screens that render it.
 */
class AdbRunner internal constructor(
    val connected: Boolean,
    private val client: AdbClient,
    private val scope: CoroutineScope,
    private val snackbar: SnackbarHostState,
) {
    var busy by mutableStateOf(false)
        private set
    var lastOutput by mutableStateOf("")
        internal set

    /** Fire-and-forget with a snackbar. [onResult] runs on the main thread after exec. */
    fun run(cmd: String, toast: Boolean = true, onResult: (AdbExecResult) -> Unit = {}) {
        if (busy) return
        busy = true
        scope.launch {
            val r = client.exec(cmd)
            lastOutput = r.output
            if (toast) {
                val msg = when {
                    !r.ok -> "✗ ${r.error ?: "failed"}"
                    r.output.isBlank() -> "✓ done (${r.durationMs}ms)"
                    else -> r.output.lineSequence().firstOrNull()?.take(120) ?: "✓ done"
                }
                snackbar.showMessage(msg)
            }
            onResult(r)
            busy = false
        }
    }

    /** Suspending exec for screens that need to await output (e.g. loading a list). */
    suspend fun exec(cmd: String): AdbExecResult = client.exec(cmd)
}

private suspend fun SnackbarHostState.showMessage(msg: String) {
    currentSnackbarData?.dismiss()
    showSnackbar(msg)
}

/**
 * Shared scaffold for every System & ADB power tool: top bar + back, the pair/connect
 * gate banner, a snackbar host, and a vertically-scrolling content column that receives
 * an [AdbRunner]. Actions in [content] should gate on `runner.connected`.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdbToolScaffold(
    title: String,
    onBack: () -> Unit,
    subtitle: String = "Runs as UID 2000 (shell) over wireless ADB.",
    scrollable: Boolean = true,
    content: @Composable ColumnScope.(AdbRunner) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val client = remember { AdbClient.get(context) }
    val prefs = remember { AdbConsolePrefs.get(context) }
    val snackbar = remember { SnackbarHostState() }

    val adbState by client.state.collectAsState()
    val paired by prefs.paired.collectAsState(initial = false)

    // Auto-connect when entering if we were paired and the keys survive.
    androidx.compose.runtime.LaunchedEffect(paired) {
        if (paired && adbState == AdbClient.State.Disconnected && client.hasStoredKeys()) {
            client.connect()
        }
    }

    val connected = adbState == AdbClient.State.Connected
    val runner = remember(connected) { AdbRunner(connected, client, scope, snackbar) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }
            )
        },
        snackbarHost = {
            SnackbarHost(snackbar) { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = Color(0xFF323232),
                    contentColor = Color.White,
                    actionColor = Color(0xFF8C9CFF)
                )
            }
        },
        modifier = Modifier.fillMaxSize()
    ) { inner ->
        val base = Modifier.padding(inner).fillMaxSize().padding(horizontal = 12.dp)
        Column(modifier = if (scrollable) base.verticalScroll(rememberScrollState()) else base) {
            Box(Modifier.padding(top = 8.dp)) {
                GateCard(
                    state = adbState,
                    paired = paired,
                    subtitle = subtitle,
                    onPair = {
                        PairingService.start(context)
                        scope.launch { snackbar.showMessage("Pairing notification posted — pull down the shade.") }
                    },
                    onConnect = { scope.launch { client.connect() } }
                )
            }
            if (connected) {
                content(runner)
            } else {
                Text(
                    "Connect to ADB above to enable these controls.",
                    Modifier.padding(24.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun GateCard(
    state: AdbClient.State,
    paired: Boolean,
    subtitle: String,
    onPair: () -> Unit,
    onConnect: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            val dot = when (state) {
                AdbClient.State.Connected -> Color(0xFF4CAF50)
                AdbClient.State.Connecting -> Color(0xFFFFB300)
                AdbClient.State.NeedsPair, AdbClient.State.Failed -> Color(0xFFE57373)
                AdbClient.State.Disconnected -> Color(0xFF9AA0A6)
            }
            Box(Modifier.size(12.dp).background(dot, RoundedCornerShape(50)))
            Column(Modifier.weight(1f)) {
                Text(
                    when (state) {
                        AdbClient.State.Disconnected -> if (paired) "Paired · disconnected" else "Not paired"
                        AdbClient.State.Connecting -> "Connecting…"
                        AdbClient.State.Connected -> "Connected"
                        AdbClient.State.NeedsPair -> "Pairing required"
                        AdbClient.State.Failed -> "Failed — try again"
                    },
                    style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold
                )
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            when (state) {
                AdbClient.State.Connecting -> CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
                AdbClient.State.Connected -> OutlinedButton(onClick = onConnect) { Text("Reconnect") }
                else -> {
                    if (paired) OutlinedButton(onClick = onConnect) { Text("Connect") }
                    Button(onClick = onPair) { Text(if (paired) "Re-pair" else "Pair") }
                }
            }
        }
    }
}
