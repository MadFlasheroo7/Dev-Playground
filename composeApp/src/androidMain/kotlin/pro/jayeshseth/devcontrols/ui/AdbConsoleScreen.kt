package pro.jayeshseth.devcontrols.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.InputStreamReader

data class ConsoleLine(
    val text: String,
    val type: LineType
)

enum class LineType {
    COMMAND, STDOUT, STDERR, SYSTEM
}

/** adb_g — local on-device shell console (sh / su) for running commands. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdbConsoleScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var commandInput by remember { mutableStateOf("") }
    var useRoot by remember { mutableStateOf(false) }
    var isRunning by remember { mutableStateOf(false) }

    val consoleBuffer = remember {
        mutableStateListOf(
            ConsoleLine("Welcome to Dev Controls ADB & Shell Console.", LineType.SYSTEM),
            ConsoleLine("Select shell execution mode (sh vs su for root) and type a command below.", LineType.SYSTEM)
        )
    }

    val lazyListState = rememberLazyListState()
    val shortcutScrollState = rememberScrollState()

    // Auto-scroll to bottom on new log line
    LaunchedEffect(consoleBuffer.size) {
        if (consoleBuffer.isNotEmpty()) {
            lazyListState.animateScrollToItem(consoleBuffer.size - 1)
        }
    }

    val executeCommand: (String) -> Unit = { cmd ->
        val trimmed = cmd.trim()
        if (trimmed.isNotEmpty() && !isRunning) {
            isRunning = true
            consoleBuffer.add(ConsoleLine("$ ${if (useRoot) "[root] " else ""}$trimmed", LineType.COMMAND))

            scope.launch {
                val result = runProcessAsync(trimmed, useRoot)
                withContext(Dispatchers.Main) {
                    if (result.stdout.isNotEmpty()) {
                        result.stdout.forEach { line ->
                            consoleBuffer.add(ConsoleLine(line, LineType.STDOUT))
                        }
                    }
                    if (result.stderr.isNotEmpty()) {
                        result.stderr.forEach { line ->
                            consoleBuffer.add(ConsoleLine(line, LineType.STDERR))
                        }
                    }
                    consoleBuffer.add(
                        ConsoleLine(
                            "Process completed with exit code: ${result.exitCode}",
                            if (result.exitCode == 0) LineType.SYSTEM else LineType.STDERR
                        )
                    )
                    isRunning = false
                }
            }
            commandInput = ""
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("ADB & Shell Console") },
                navigationIcon = {
                    TextButton(onClick = onBack) {
                        Text("Back")
                    }
                },
                actions = {
                    TextButton(
                        onClick = {
                            val combinedText = consoleBuffer.joinToString("\n") { it.text }
                            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            cm.setPrimaryClip(ClipData.newPlainText("ADB Console Output", combinedText))
                            Toast.makeText(context, "Console output copied", Toast.LENGTH_SHORT).show()
                        }
                    ) {
                        Text("Copy Output")
                    }
                    TextButton(
                        onClick = {
                            consoleBuffer.clear()
                            consoleBuffer.add(ConsoleLine("Console cleared.", LineType.SYSTEM))
                        }
                    ) {
                        Text("Clear")
                    }
                }
            )
        },
        modifier = Modifier.fillMaxSize()
    ) { inner ->
        Column(
            modifier = Modifier
                .padding(inner)
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Shell Mode Selection Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "Execution Mode",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (useRoot) "Elevated Root Shell (su)" else "Standard Sandboxed Shell (sh)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Root (su)",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Switch(
                            checked = useRoot,
                            onCheckedChange = { useRoot = it }
                        )
                    }
                }
            }

            // Terminal screen area
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(Color(0xFF1E1E1E), shape = RoundedCornerShape(8.dp))
                    .padding(12.dp)
            ) {
                LazyColumn(
                    state = lazyListState,
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(consoleBuffer) { line ->
                        val textColor = when (line.type) {
                            LineType.COMMAND -> Color(0xFF81D4FA) // Light Blue
                            LineType.STDOUT -> Color(0xFFC8E6C9)  // Soft Green
                            LineType.STDERR -> Color(0xFFFFCDD2)  // Soft Red
                            LineType.SYSTEM -> Color(0xFFFFCC80)  // Soft Orange / Amber
                        }

                        val prefix = when (line.type) {
                            LineType.COMMAND -> ""
                            LineType.STDOUT -> "  "
                            LineType.STDERR -> "[ERR] "
                            LineType.SYSTEM -> "[SYS] "
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

            Spacer(modifier = Modifier.height(12.dp))

            // Shortcuts row
            Text(
                text = "Preset Quick Shortcuts:",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(bottom = 6.dp)
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(shortcutScrollState)
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val shortcuts = listOf(
                    "getprop ro.product.model" to "Device Model",
                    "dumpsys battery" to "Battery Info",
                    "pm list packages -3" to "List 3rd Party Apps",
                    "ip addr show" to "Network IPs",
                    "settings get global adb_enabled" to "Check USB Debug",
                    "settings get global adb_wifi_enabled" to "Check Wireless Debug"
                )

                shortcuts.forEach { (cmd, label) ->
                    SuggestionChip(
                        onClick = { commandInput = cmd },
                        label = { Text(label) }
                    )
                }
            }

            // Command input panel
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = commandInput,
                    onValueChange = { commandInput = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Enter command (e.g. getprop)") },
                    textStyle = LocalTextStyle.current.copy(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 14.sp
                    ),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    )
                )

                Spacer(modifier = Modifier.width(8.dp))

                Button(
                    onClick = { executeCommand(commandInput) },
                    enabled = commandInput.trim().isNotEmpty() && !isRunning,
                    modifier = Modifier.height(56.dp)
                ) {
                    Text("Run")
                }
            }
        }
    }
}

data class ProcessResult(
    val exitCode: Int,
    val stdout: List<String>,
    val stderr: List<String>
)

private suspend fun runProcessAsync(command: String, asRoot: Boolean): ProcessResult = withContext(Dispatchers.IO) {
    val stdoutLines = mutableListOf<String>()
    val stderrLines = mutableListOf<String>()
    var exitCode = -1

    try {
        val process = if (asRoot) {
            val p = Runtime.getRuntime().exec("su")
            val os = DataOutputStream(p.outputStream)
            os.writeBytes("$command\n")
            os.writeBytes("exit\n")
            os.flush()
            p
        } else {
            Runtime.getRuntime().exec(arrayOf("sh", "-c", command))
        }

        val outReader = BufferedReader(InputStreamReader(process.inputStream))
        val errReader = BufferedReader(InputStreamReader(process.errorStream))

        // Capture stdout
        var line: String? = outReader.readLine()
        while (line != null) {
            stdoutLines.add(line)
            line = outReader.readLine()
        }

        // Capture stderr
        var errLine: String? = errReader.readLine()
        while (errLine != null) {
            stderrLines.add(errLine)
            errLine = errReader.readLine()
        }

        exitCode = process.waitFor()
    } catch (e: Exception) {
        val msg = e.message ?: "Unknown process execution error"
        stderrLines.add(msg)
        if (asRoot && (msg.contains("error=2") || msg.contains("No such file") || msg.contains("ENOENT"))) {
            stderrLines.add("")
            stderrLines.add("[DIAGNOSTIC] The 'su' binary was not found in your system's PATH.")
            stderrLines.add("[DIAGNOSTIC] This indicates your device is not rooted (or root access is not configured).")
            stderrLines.add("[DIAGNOSTIC] Root execution is only possible on devices rooted via Magisk, KernelSU, or APatch.")
        }
    }

    ProcessResult(exitCode, stdoutLines, stderrLines)
}
