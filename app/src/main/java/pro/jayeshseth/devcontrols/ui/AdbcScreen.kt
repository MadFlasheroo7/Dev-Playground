package pro.jayeshseth.devcontrols.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import pro.jayeshseth.devcontrols.adb.AdbClient
import pro.jayeshseth.devcontrols.adb.AdbRecipe
import pro.jayeshseth.devcontrols.adb.AdbRecipes
import pro.jayeshseth.devcontrols.adb.PairingService
import pro.jayeshseth.devcontrols.data.AdbConsolePrefs

private data class AdbcLine(val text: String, val type: AdbcLineType)
private enum class AdbcLineType { COMMAND, STDOUT, STDERR, SYSTEM }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdbcScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val client = remember { AdbClient.get(context) }
    val prefs = remember { AdbConsolePrefs.get(context) }

    val adbState by client.state.collectAsState()
    val paired by prefs.paired.collectAsState(initial = false)

    var commandInput by remember { mutableStateOf("") }
    var isRunning by remember { mutableStateOf(false) }
    var pendingRecipe by remember { mutableStateOf<AdbRecipe?>(null) }

    val consoleBuffer = remember {
        mutableStateListOf(
            AdbcLine("Welcome to adb_c — wireless ADB shell over loopback.", AdbcLineType.SYSTEM),
            AdbcLine(
                "Pair once via the Pair button (6-digit code from Settings → Developer options → Wireless debugging → Pair device with pairing code).",
                AdbcLineType.SYSTEM
            ),
            AdbcLine("Once paired, every shell command runs as UID 2000 (pm grant, settings put global, am force-stop, …).", AdbcLineType.SYSTEM)
        )
    }

    val lazyListState = rememberLazyListState()
    val shortcutScrollState = rememberScrollState()

    LaunchedEffect(consoleBuffer.size) {
        if (consoleBuffer.isNotEmpty()) {
            lazyListState.animateScrollToItem(consoleBuffer.size - 1)
        }
    }

    // Auto-connect on screen entry only if the prefs flag says paired AND the
    // backing key/cert files still exist. After a reinstall the key files are
    // wiped but the DataStore value can survive — that mismatch otherwise spams
    // AdbPairingRequiredException in logs.
    LaunchedEffect(paired) {
        if (paired && adbState == AdbClient.State.Disconnected && AdbClient.get(context).hasStoredKeys()) {
            client.connect()
        }
    }

    fun runCommand(cmd: String) {
        val trimmed = cmd.trim()
        if (trimmed.isEmpty() || isRunning) return
        isRunning = true
        consoleBuffer.add(AdbcLine("$ $trimmed", AdbcLineType.COMMAND))
        scope.launch {
            prefs.pushHistory(trimmed)
            val r = client.exec(trimmed)
            if (r.ok) {
                appendOutput(consoleBuffer, r.output, AdbcLineType.STDOUT)
                consoleBuffer.add(AdbcLine("done in ${r.durationMs}ms", AdbcLineType.SYSTEM))
            } else {
                consoleBuffer.add(AdbcLine(r.error ?: "Command failed", AdbcLineType.STDERR))
                if (adbState == AdbClient.State.NeedsPair) {
                    consoleBuffer.add(AdbcLine("Pairing required — tap Pair above.", AdbcLineType.SYSTEM))
                }
            }
            isRunning = false
            commandInput = ""
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("adb_c · wireless ADB") },
                navigationIcon = { TextButton(onClick = onBack) { Text("Back") } },
                actions = {
                    TextButton(onClick = {
                        val combinedText = consoleBuffer.joinToString("\n") { it.text }
                        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        cm.setPrimaryClip(ClipData.newPlainText("adb_c output", combinedText))
                        Toast.makeText(context, "Output copied", Toast.LENGTH_SHORT).show()
                    }) { Text("Copy") }
                    TextButton(onClick = {
                        consoleBuffer.clear()
                        consoleBuffer.add(AdbcLine("Cleared.", AdbcLineType.SYSTEM))
                    }) { Text("Clear") }
                }
            )
        },
        modifier = Modifier.fillMaxSize()
    ) { inner ->
        Column(
            modifier = Modifier
                .padding(inner)
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            StatusCard(
                adbState = adbState,
                paired = paired,
                onPair = {
                    PairingService.start(context)
                    consoleBuffer.add(
                        AdbcLine(
                            "Pairing notification posted. Pull down the notification shade — it auto-fetches the port and accepts the 6-digit code inline.",
                            AdbcLineType.SYSTEM
                        )
                    )
                },
                onReconnect = { scope.launch { client.connect() } }
            )

            Spacer(Modifier.height(8.dp))

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(Color(0xFF1E1E1E), RoundedCornerShape(8.dp))
                    .padding(12.dp)
            ) {
                LazyColumn(state = lazyListState, modifier = Modifier.fillMaxSize()) {
                    items(consoleBuffer) { line ->
                        val textColor = when (line.type) {
                            AdbcLineType.COMMAND -> Color(0xFF81D4FA)
                            AdbcLineType.STDOUT -> Color(0xFFC8E6C9)
                            AdbcLineType.STDERR -> Color(0xFFFFCDD2)
                            AdbcLineType.SYSTEM -> Color(0xFFFFCC80)
                        }
                        val prefix = when (line.type) {
                            AdbcLineType.COMMAND -> ""
                            AdbcLineType.STDOUT -> "  "
                            AdbcLineType.STDERR -> "[ERR] "
                            AdbcLineType.SYSTEM -> "[SYS] "
                        }
                        Text(
                            text = "$prefix${line.text}",
                            color = textColor,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 13.sp,
                            lineHeight = 16.sp,
                            modifier = Modifier.padding(vertical = 2.dp)
                        )
                    }
                }
                if (isRunning) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .size(36.dp)
                            .align(Alignment.TopEnd)
                            .padding(8.dp),
                        color = MaterialTheme.colorScheme.primary,
                        strokeWidth = 2.dp
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            Text(
                text = "Recipes",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(shortcutScrollState),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AdbRecipes.all.forEach { recipe ->
                    SuggestionChip(
                        onClick = {
                            if (recipe.confirm != null) {
                                pendingRecipe = recipe
                            } else {
                                runCommand(recipe.build())
                            }
                        },
                        label = { Text(recipe.title) }
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = commandInput,
                    onValueChange = { commandInput = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("e.g. pm grant <pkg> <perm>") },
                    textStyle = LocalTextStyle.current.copy(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 14.sp
                    ),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        autoCorrectEnabled = false,
                        capitalization = KeyboardCapitalization.None,
                        imeAction = ImeAction.Send
                    ),
                    keyboardActions = KeyboardActions(onSend = { runCommand(commandInput) }),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    )
                )
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = { runCommand(commandInput) },
                    enabled = commandInput.trim().isNotEmpty() && !isRunning,
                    modifier = Modifier.height(56.dp)
                ) { Text("Run") }
            }
        }
    }

    pendingRecipe?.let { recipe ->
        AlertDialog(
            onDismissRequest = { pendingRecipe = null },
            title = { Text(recipe.title) },
            text = { Text(recipe.confirm ?: "") },
            confirmButton = {
                TextButton(onClick = {
                    runCommand(recipe.build())
                    pendingRecipe = null
                }) { Text("Run") }
            },
            dismissButton = {
                TextButton(onClick = { pendingRecipe = null }) { Text("Cancel") }
            }
        )
    }

}

@Composable
private fun StatusCard(
    adbState: AdbClient.State,
    paired: Boolean,
    onPair: () -> Unit,
    onReconnect: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StatusDot(adbState)
            Column(modifier = Modifier.weight(1f)) {
                Text(statusLabel(adbState, paired), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                Text(
                    "Wireless ADB via loopback. UID 2000 shell privileges.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (adbState == AdbClient.State.Connected) {
                OutlinedButton(onClick = onReconnect) { Text("Reconnect") }
            } else {
                Button(onClick = onPair) { Text(if (paired) "Re-pair" else "Pair") }
            }
        }
    }
}

@Composable
private fun StatusDot(state: AdbClient.State) {
    val c = when (state) {
        AdbClient.State.Connected -> Color(0xFF4CAF50)
        AdbClient.State.Connecting -> Color(0xFFFFB300)
        AdbClient.State.NeedsPair, AdbClient.State.Failed -> Color(0xFFE57373)
        AdbClient.State.Disconnected -> Color(0xFF9AA0A6)
    }
    Box(
        modifier = Modifier
            .size(12.dp)
            .background(c, RoundedCornerShape(50))
    )
}

private fun statusLabel(state: AdbClient.State, paired: Boolean): String = when (state) {
    AdbClient.State.Disconnected -> if (paired) "Paired · disconnected" else "Not paired"
    AdbClient.State.Connecting -> "Connecting…"
    AdbClient.State.Connected -> "Connected"
    AdbClient.State.NeedsPair -> "Pairing required"
    AdbClient.State.Failed -> "Failed — try again"
}

private fun appendOutput(buffer: MutableList<AdbcLine>, text: String, type: AdbcLineType) {
    if (text.isEmpty()) return
    text.split("\n").forEach { line ->
        if (line.isNotEmpty()) buffer.add(AdbcLine(line, type))
    }
}

