package pro.jayeshseth.devcontrols.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import pro.jayeshseth.devcontrols.adb.AdbClient
import pro.jayeshseth.devcontrols.ui.adbtools.AppPickerDialog
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.regex.Pattern

enum class LogcatLevel(val code: String, val label: String, val color: Color, val priority: Int) {
    ALL("A", "All", Color.Gray, 0),
    VERBOSE("V", "Verbose", Color(0xFF9E9E9E), 1),
    DEBUG("D", "Debug", Color(0xFF29B6F6), 2),
    INFO("I", "Info", Color(0xFF66BB6A), 3),
    WARN("W", "Warn", Color(0xFFFFA726), 4),
    ERROR("E", "Error", Color(0xFFEF5350), 5),
    FATAL("F", "Fatal", Color(0xFFAB47BC), 6)
}

data class ParsedLogLine(
    val id: Long,
    val raw: String,
    val timestamp: String,
    val level: LogcatLevel,
    val pid: String,
    val tid: String,
    val tag: String,
    val message: String
)

enum class LogSource(val label: String) {
    ADB_SHELL("Wireless ADB (System-Wide)"),
    LOCAL_PROCESS("Self Process (No ADB needed)")
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun LogcatStreamerScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val adbClient = remember { AdbClient.get(context) }
    val adbState by adbClient.state.collectAsState()

    var logSource by remember { mutableStateOf(LogSource.ADB_SHELL) }
    var isStreaming by remember { mutableStateOf(true) }
    var autoScroll by remember { mutableStateOf(true) }
    var maxBufferSize by remember { mutableStateOf(1000) }

    // Filters
    var selectedLevel by remember { mutableStateOf(LogcatLevel.ALL) }
    var tagFilter by remember { mutableStateOf("") }
    var queryFilter by remember { mutableStateOf("") }
    var packageOrPidFilter by remember { mutableStateOf("") }
    var resolvedPids by remember { mutableStateOf<Set<String>>(emptySet()) }

    var showAppPicker by remember { mutableStateOf(false) }
    var statusText by remember { mutableStateOf("Ready") }

    // Circular logs buffer
    val logsList = remember { mutableStateListOf<ParsedLogLine>() }
    var logCounter by remember { mutableStateOf(0L) }
    var bufferVersion by remember { mutableStateOf(0L) }

    val listState = rememberLazyListState()

    fun copyLogsToClipboard(content: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("Logcat Dump", content))
        Toast.makeText(context, "Logs copied to clipboard", Toast.LENGTH_SHORT).show()
    }

    fun exportLogs(content: String) {
        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, content)
            type = "text/plain"
        }
        val shareIntent = Intent.createChooser(sendIntent, "Export Logcat")
        context.startActivity(shareIntent)
    }

    fun parseLine(raw: String): ParsedLogLine {
        // Typical format: 05-06 12:34:56.789  1234  5678 D TagName : message
        // Or: 05-06 12:34:56.789 D/TagName( 1234): message
        var timestamp = ""
        var level = LogcatLevel.VERBOSE
        var pid = ""
        var tid = ""
        var tag = ""
        var msg = raw

        val timePattern = Pattern.compile("^(\\d{2}-\\d{2}\\s+\\d{2}:\\d{2}:\\d{2}\\.\\d{3})\\s+(\\d+)\\s+(\\d+)\\s+([VDIWEF])\\s+([^:]+):\\s*(.*)$")
        val matcher = timePattern.matcher(raw)
        if (matcher.matches()) {
            timestamp = matcher.group(1) ?: ""
            pid = matcher.group(2) ?: ""
            tid = matcher.group(3) ?: ""
            val lvlChar = matcher.group(4) ?: "V"
            level = when (lvlChar) {
                "V" -> LogcatLevel.VERBOSE
                "D" -> LogcatLevel.DEBUG
                "I" -> LogcatLevel.INFO
                "W" -> LogcatLevel.WARN
                "E" -> LogcatLevel.ERROR
                "F" -> LogcatLevel.FATAL
                else -> LogcatLevel.VERBOSE
            }
            tag = matcher.group(5)?.trim() ?: ""
            msg = matcher.group(6) ?: ""
        } else {
            // Alternative brief format: D/TagName( 1234): message
            val briefPattern = Pattern.compile("^([VDIWEF])/([^\\(:\\s]+)(?:\\(\\s*(\\d+)\\))?:\\s*(.*)$")
            val m2 = briefPattern.matcher(raw)
            if (m2.matches()) {
                val lvlChar = m2.group(1) ?: "V"
                level = when (lvlChar) {
                    "V" -> LogcatLevel.VERBOSE
                    "D" -> LogcatLevel.DEBUG
                    "I" -> LogcatLevel.INFO
                    "W" -> LogcatLevel.WARN
                    "E" -> LogcatLevel.ERROR
                    "F" -> LogcatLevel.FATAL
                    else -> LogcatLevel.VERBOSE
                }
                tag = m2.group(2)?.trim() ?: ""
                pid = m2.group(3) ?: ""
                msg = m2.group(4) ?: ""
            }
        }

        logCounter++
        return ParsedLogLine(
            id = logCounter,
            raw = raw,
            timestamp = timestamp,
            level = level,
            pid = pid,
            tid = tid,
            tag = tag,
            message = msg
        )
    }

    // Streaming Coroutine
    DisposableEffect(isStreaming, logSource) {
        if (!isStreaming) {
            statusText = "Stream paused"
            return@DisposableEffect onDispose {}
        }

        var streamJob: Job? = null
        if (logSource == LogSource.ADB_SHELL) {
            statusText = "Connecting ADB logcat stream..."
            streamJob = scope.launch(Dispatchers.IO) {
                val cmd = "logcat -v threadtime"
                val batch = ArrayList<ParsedLogLine>(32)
                var lastFlush = System.currentTimeMillis()

                suspend fun flushBatch() {
                    if (batch.isEmpty()) return
                    val copy = ArrayList(batch)
                    batch.clear()
                    lastFlush = System.currentTimeMillis()
                    withContext(Dispatchers.Main) {
                        if (logsList.size + copy.size > maxBufferSize) {
                            val toRemove = (logsList.size + copy.size) - maxBufferSize
                            val actualRemove = toRemove.coerceAtMost(logsList.size)
                            if (actualRemove > 0) {
                                logsList.removeRange(0, actualRemove)
                            }
                        }
                        logsList.addAll(copy)
                        bufferVersion++
                        statusText = "Streaming via ADB (Buffer: ${logsList.size}/$maxBufferSize)"
                    }
                }

                adbClient.openShellFlow(cmd)
                    .catch { t ->
                        withContext(Dispatchers.Main) {
                            statusText = "ADB stream failed: ${t.message ?: "Unknown"}. Switch to Self Process or pair ADB."
                        }
                    }
                    .collect { line ->
                        if (line.isNotBlank()) {
                            val parsed = parseLine(line)
                            batch.add(parsed)
                            if (batch.size >= 25 || System.currentTimeMillis() - lastFlush > 80) {
                                flushBatch()
                            }
                        }
                    }
                flushBatch()
            }
        } else {
            // Local Process Stream
            statusText = "Streaming local process logcat..."
            streamJob = scope.launch(Dispatchers.IO) {
                var proc: Process? = null
                val batch = ArrayList<ParsedLogLine>(32)
                var lastFlush = System.currentTimeMillis()

                suspend fun flushBatch() {
                    if (batch.isEmpty()) return
                    val copy = ArrayList(batch)
                    batch.clear()
                    lastFlush = System.currentTimeMillis()
                    withContext(Dispatchers.Main) {
                        if (logsList.size + copy.size > maxBufferSize) {
                            val toRemove = (logsList.size + copy.size) - maxBufferSize
                            val actualRemove = toRemove.coerceAtMost(logsList.size)
                            if (actualRemove > 0) {
                                logsList.removeRange(0, actualRemove)
                            }
                        }
                        logsList.addAll(copy)
                        bufferVersion++
                        statusText = "Streaming local process (Buffer: ${logsList.size}/$maxBufferSize)"
                    }
                }

                try {
                    proc = Runtime.getRuntime().exec(arrayOf("logcat", "-v", "threadtime"))
                    val reader = BufferedReader(InputStreamReader(proc.inputStream))
                    while (isActive) {
                        val line = reader.readLine() ?: break
                        if (line.isNotBlank()) {
                            val parsed = parseLine(line)
                            batch.add(parsed)
                            if (batch.size >= 25 || System.currentTimeMillis() - lastFlush > 80) {
                                flushBatch()
                            }
                        }
                    }
                    flushBatch()
                } catch (t: Throwable) {
                    withContext(Dispatchers.Main) {
                        statusText = "Local logcat error: ${t.message}"
                    }
                } finally {
                    proc?.destroy()
                }
            }
        }

        onDispose {
            streamJob?.cancel()
        }
    }

    // Resolve PID dynamically when package name is entered or chosen
    LaunchedEffect(packageOrPidFilter, logSource, adbState) {
        val input = packageOrPidFilter.trim()
        if (input.isBlank()) {
            resolvedPids = emptySet()
            return@LaunchedEffect
        }
        if (input.all { it.isDigit() }) {
            resolvedPids = setOf(input)
            return@LaunchedEffect
        }
        withContext(Dispatchers.IO) {
            val pids = mutableSetOf<String>()
            if (logSource == LogSource.ADB_SHELL && adbState == AdbClient.State.Connected) {
                val res = adbClient.exec("pidof $input")
                if (res.ok && res.output.isNotBlank()) {
                    val tokens = res.output.trim().split("\\s+".toRegex()).filter { it.isNotBlank() }
                    pids.addAll(tokens)
                }
            }
            if (pids.isEmpty()) {
                runCatching {
                    val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? android.app.ActivityManager
                    am?.runningAppProcesses?.forEach { proc ->
                        if (proc.processName == input || proc.pkgList?.contains(input) == true) {
                            pids.add(proc.pid.toString())
                        }
                    }
                }
            }
            withContext(Dispatchers.Main) {
                resolvedPids = pids
            }
        }
    }

    // Filtered logs computation (re-evaluated on bufferVersion or filter changes)
    val filteredLogs = remember(bufferVersion, selectedLevel, tagFilter, queryFilter, packageOrPidFilter, resolvedPids) {
        val trimmedPkgOrPid = packageOrPidFilter.trim()
        val isNumericPid = trimmedPkgOrPid.isNotEmpty() && trimmedPkgOrPid.all { it.isDigit() }
        logsList.filter { line ->
            // Level filter
            if (selectedLevel != LogcatLevel.ALL && line.level.priority < selectedLevel.priority) {
                return@filter false
            }
            // Tag filter
            if (tagFilter.isNotBlank()) {
                val tagMatches = runCatching {
                    val regex = Regex(tagFilter, RegexOption.IGNORE_CASE)
                    regex.containsMatchIn(line.tag)
                }.getOrElse {
                    line.tag.contains(tagFilter, ignoreCase = true)
                }
                if (!tagMatches) return@filter false
            }
            // Query filter
            if (queryFilter.isNotBlank() && !line.message.contains(queryFilter, ignoreCase = true)) {
                return@filter false
            }
            // PID / Package filter
            if (trimmedPkgOrPid.isNotBlank()) {
                val pidMatches = if (isNumericPid) {
                    line.pid == trimmedPkgOrPid || line.raw.contains(trimmedPkgOrPid)
                } else {
                    line.pid in resolvedPids ||
                    line.raw.contains(trimmedPkgOrPid, ignoreCase = true)
                }
                if (!pidMatches) return@filter false
            }
            true
        }
    }

    // Auto-scroll effect
    LaunchedEffect(filteredLogs.size, autoScroll) {
        if (autoScroll && filteredLogs.isNotEmpty()) {
            listState.scrollToItem(filteredLogs.size - 1)
        }
    }

    if (showAppPicker) {
        AppPickerDialog(
            includeSystem = true,
            onPick = { app ->
                packageOrPidFilter = app.packageName
                showAppPicker = false
            },
            onDismiss = { showAppPicker = false }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Live Logcat Streamer", fontWeight = FontWeight.Bold)
                        Text(
                            statusText,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                navigationIcon = {
                    TextButton(onClick = onBack) {
                        Text("Back")
                    }
                },
                actions = {
                    // Pause / Resume Toggle
                    IconButton(onClick = { isStreaming = !isStreaming }) {
                        Text(if (isStreaming) "⏸" else "▶", fontSize = 18.sp)
                    }
                    // Clear buffer
                    IconButton(onClick = {
                        logsList.clear()
                        Toast.makeText(context, "Logcat buffer cleared", Toast.LENGTH_SHORT).show()
                    }) {
                        Text("🗑", fontSize = 16.sp)
                    }
                    // Export logs
                    IconButton(onClick = {
                        val fullLog = filteredLogs.joinToString("\n") { it.raw }
                        exportLogs(fullLog)
                    }) {
                        Text("📤", fontSize = 16.sp)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Source & ADB Banner Card
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
                shape = RoundedCornerShape(0.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Source:", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            FilterChip(
                                selected = logSource == LogSource.ADB_SHELL,
                                onClick = { logSource = LogSource.ADB_SHELL },
                                label = { Text("Wireless ADB") }
                            )
                            FilterChip(
                                selected = logSource == LogSource.LOCAL_PROCESS,
                                onClick = { logSource = LogSource.LOCAL_PROCESS },
                                label = { Text("Self App") }
                            )
                        }

                        if (logSource == LogSource.ADB_SHELL) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (adbState == AdbClient.State.Connected) Color(0xFF2E7D32).copy(alpha = 0.2f) else Color(0xFFC62828).copy(alpha = 0.2f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    if (adbState == AdbClient.State.Connected) "ADB Connected" else "ADB: $adbState",
                                    fontSize = 11.sp,
                                    color = if (adbState == AdbClient.State.Connected) Color(0xFF2E7D32) else Color(0xFFC62828),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // Level Filter Chips
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        LogcatLevel.entries.forEach { lvl ->
                            FilterChip(
                                selected = selectedLevel == lvl,
                                onClick = { selectedLevel = lvl },
                                label = {
                                    Text(
                                        lvl.label,
                                        fontSize = 11.sp,
                                        color = if (selectedLevel == lvl) MaterialTheme.colorScheme.primary else lvl.color
                                    )
                                },
                                modifier = Modifier.height(30.dp)
                            )
                        }
                    }

                    // Search & Filters Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = queryFilter,
                            onValueChange = { queryFilter = it },
                            placeholder = { Text("Filter message...", fontSize = 12.sp) },
                            singleLine = true,
                            modifier = Modifier.weight(1.3f)
                        )
                        OutlinedTextField(
                            value = tagFilter,
                            onValueChange = { tagFilter = it },
                            placeholder = { Text("Tag / Regex", fontSize = 12.sp) },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = packageOrPidFilter,
                            onValueChange = { packageOrPidFilter = it },
                            placeholder = { Text("PID / Pkg", fontSize = 12.sp) },
                            singleLine = true,
                            modifier = Modifier.weight(0.9f)
                        )
                        IconButton(onClick = { showAppPicker = true }) {
                            Text("📱", fontSize = 16.sp)
                        }
                    }

                    if (resolvedPids.isNotEmpty() && !packageOrPidFilter.all { it.isDigit() }) {
                        Text(
                            "Resolved PID(s) for '$packageOrPidFilter': ${resolvedPids.joinToString(", ")}",
                            fontSize = 11.sp,
                            color = Color(0xFF2E7D32),
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    // Bottom info bar: Counts + Auto-scroll toggle + Copy visible
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Showing ${filteredLogs.size} of ${logsList.size} logs",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Auto-scroll", fontSize = 11.sp)
                                Spacer(Modifier.width(4.dp))
                                Switch(
                                    checked = autoScroll,
                                    onCheckedChange = { autoScroll = it }
                                )
                            }

                            TextButton(
                                onClick = {
                                    val visibleDump = filteredLogs.joinToString("\n") { it.raw }
                                    copyLogsToClipboard(visibleDump)
                                }
                            ) {
                                Text("Copy Visible", fontSize = 11.sp)
                            }
                        }
                    }
                }
            }

            // Live Logcat Output Terminal List
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF121212))
            ) {
                if (filteredLogs.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            if (logsList.isEmpty()) "Waiting for logcat output..." else "No logs match current filters",
                            color = Color.Gray,
                            fontSize = 13.sp
                        )
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(filteredLogs, key = { it.id }) { log ->
                            LogcatLineItem(log = log)
                            HorizontalDivider(color = Color(0xFF222222), thickness = 0.5.dp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LogcatLineItem(log: ParsedLogLine) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 3.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.Top
    ) {
        // Log Level indicator pill
        Box(
            modifier = Modifier
                .size(width = 16.dp, height = 16.dp)
                .clip(CircleShape)
                .background(log.level.color.copy(alpha = 0.25f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                log.level.code,
                color = log.level.color,
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            // Header: Timestamp, PID, Tag
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (log.timestamp.isNotBlank()) {
                    Text(
                        log.timestamp,
                        color = Color(0xFF888888),
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
                if (log.pid.isNotBlank()) {
                    Text(
                        "${log.pid}/${log.tid}",
                        color = Color(0xFF666666),
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
                if (log.tag.isNotBlank()) {
                    Text(
                        log.tag,
                        color = log.level.color,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Log Message Body
            Text(
                log.message.ifBlank { log.raw },
                color = Color(0xFFE0E0E0),
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                lineHeight = 14.sp
            )
        }
    }
}
