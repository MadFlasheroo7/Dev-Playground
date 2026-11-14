package pro.jayeshseth.devcontrols.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Process
import android.os.SystemClock
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import pro.jayeshseth.devcontrols.adb.AdbClient
import pro.jayeshseth.devcontrols.data.AdbConsolePrefs

enum class ProviderOperation(val label: String) {
    QUERY("Query"),
    INSERT("Insert"),
    UPDATE("Update"),
    DELETE("Delete")
}

data class ProviderPreset(
    val title: String,
    val uri: String,
    val projection: String = "",
    val sortOrder: String = ""
)

private val COMMON_PRESETS = listOf(
    ProviderPreset("MediaStore Images", "content://media/external/images/media", "_id, _display_name, mime_type, _size, date_added", "date_added DESC"),
    ProviderPreset("MediaStore Audio", "content://media/external/audio/media", "_id, title, artist, duration, _size", "_id DESC"),
    ProviderPreset("MediaStore Video", "content://media/external/video/media", "_id, _display_name, duration, _size", "_id DESC"),
    ProviderPreset("Settings System", "content://settings/system", "name, value", "name ASC"),
    ProviderPreset("Settings Secure", "content://settings/secure", "name, value", "name ASC"),
    ProviderPreset("Settings Global", "content://settings/global", "name, value", "name ASC"),
    ProviderPreset("User Dictionary", "content://user_dictionary/words", "_id, word, frequency, locale", "word ASC"),
    ProviderPreset("Contacts", "content://com.android.contacts/contacts", "_id, display_name, has_phone_number", "display_name ASC"),
    ProviderPreset("Telephony SMS", "content://sms/inbox", "_id, address, body, date", "date DESC")
)

data class ColumnHeader(val name: String, val type: String)

data class QueryResultData(
    val mode: String,
    val success: Boolean,
    val durationMs: Long,
    val columns: List<ColumnHeader>,
    val rows: List<Map<String, String?>>,
    val rawOutput: String = "",
    val errorMessage: String? = null,
    val isSecurityException: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ContentProviderProbeScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val client = remember { AdbClient.get(context) }
    val prefs = remember { AdbConsolePrefs.get(context) }

    val adbState by client.state.collectAsState()
    val paired by prefs.paired.collectAsState(initial = false)

    var operation by remember { mutableStateOf(ProviderOperation.QUERY) }
    var uriString by remember { mutableStateOf("content://settings/system") }
    var projection by remember { mutableStateOf("name, value") }
    var selection by remember { mutableStateOf("") }
    var selectionArgs by remember { mutableStateOf("") }
    var sortOrder by remember { mutableStateOf("name ASC") }
    var rowLimit by remember { mutableStateOf("100") }

    // Key-value binds for insert/update
    val bindKeyValues = remember { mutableStateListOf<Pair<String, String>>() }
    var newKey by remember { mutableStateOf("") }
    var newVal by remember { mutableStateOf("") }

    var isRunning by remember { mutableStateOf(false) }
    var resultData by remember { mutableStateOf<QueryResultData?>(null) }
    var selectedCellDetail by remember { mutableStateOf<Pair<String, String?>?>(null) }
    var showRawOutput by remember { mutableStateOf(false) }

    // Auto-connect ADB if paired and keys present
    LaunchedEffect(paired) {
        if (paired && adbState == AdbClient.State.Disconnected && client.hasStoredKeys()) {
            client.connect()
        }
    }

    fun buildShellCommand(): String {
        val trimmedUri = uriString.trim()
        return when (operation) {
            ProviderOperation.QUERY -> buildString {
                append("content query --uri $trimmedUri")
                if (projection.isNotBlank()) {
                    val shellProj = projection.split(',', ':')
                        .map { it.trim() }
                        .filter { it.isNotEmpty() }
                        .joinToString(":")
                    if (shellProj.isNotBlank()) append(" --projection $shellProj")
                }
                if (selection.isNotBlank()) append(" --where \"${selection.replace("\"", "\\\"")}\"")
                if (sortOrder.isNotBlank()) append(" --sort \"$sortOrder\"")
            }
            ProviderOperation.INSERT -> buildString {
                append("content insert --uri $trimmedUri")
                bindKeyValues.forEach { (k, v) ->
                    val escapedV = v.replace("\"", "\\\"")
                    append(" --bind \"$k:s:$escapedV\"")
                }
            }
            ProviderOperation.UPDATE -> buildString {
                append("content update --uri $trimmedUri")
                if (selection.isNotBlank()) append(" --where \"${selection.replace("\"", "\\\"")}\"")
                bindKeyValues.forEach { (k, v) ->
                    val escapedV = v.replace("\"", "\\\"")
                    append(" --bind \"$k:s:$escapedV\"")
                }
            }
            ProviderOperation.DELETE -> buildString {
                append("content delete --uri $trimmedUri")
                if (selection.isNotBlank()) append(" --where \"${selection.replace("\"", "\\\"")}\"")
            }
        }
    }

    fun executeAppUid() {
        if (isRunning) return
        isRunning = true
        scope.launch {
            val start = SystemClock.elapsedRealtime()
            try {
                val parsedUri = Uri.parse(uriString.trim())
                when (operation) {
                    ProviderOperation.QUERY -> {
                        val projArray = if (projection.isNotBlank()) {
                            projection.split(',', ':').map { it.trim() }.filter { it.isNotEmpty() }.toTypedArray()
                        } else null
                        val sel = selection.ifBlank { null }
                        val selArgs = if (selectionArgs.isNotBlank()) {
                            selectionArgs.split(",").map { it.trim() }.toTypedArray()
                        } else null
                        val sort = sortOrder.ifBlank { null }

                        val cursor = withContext(Dispatchers.IO) {
                            context.contentResolver.query(parsedUri, projArray, sel, selArgs, sort)
                        }

                        if (cursor == null) {
                            val elapsed = SystemClock.elapsedRealtime() - start
                            resultData = QueryResultData(
                                mode = "App UID (${Process.myUid()})",
                                success = false,
                                durationMs = elapsed,
                                columns = emptyList(),
                                rows = emptyList(),
                                errorMessage = "ContentResolver.query returned null (provider not found, or inaccessible)."
                            )
                        } else {
                            cursor.use { c ->
                                val columnNames = c.columnNames.toList()
                                val columnsWithTypes = mutableListOf<ColumnHeader>()
                                val rows = mutableListOf<Map<String, String?>>()
                                val limit = rowLimit.toIntOrNull() ?: 100

                                var typesExtracted = false
                                while (c.moveToNext()) {
                                    if (!typesExtracted) {
                                        for (i in 0 until c.columnCount) {
                                            val typeName = when (c.getType(i)) {
                                                Cursor.FIELD_TYPE_INTEGER -> "INTEGER"
                                                Cursor.FIELD_TYPE_FLOAT -> "FLOAT"
                                                Cursor.FIELD_TYPE_STRING -> "TEXT"
                                                Cursor.FIELD_TYPE_BLOB -> "BLOB"
                                                Cursor.FIELD_TYPE_NULL -> "NULL"
                                                else -> "UNKNOWN"
                                            }
                                            columnsWithTypes.add(ColumnHeader(columnNames[i], typeName))
                                        }
                                        typesExtracted = true
                                    }

                                    val row = mutableMapOf<String, String?>()
                                    for (i in 0 until c.columnCount) {
                                        val colName = columnNames[i]
                                        val valueStr = when (c.getType(i)) {
                                            Cursor.FIELD_TYPE_NULL -> null
                                            Cursor.FIELD_TYPE_INTEGER -> c.getLong(i).toString()
                                            Cursor.FIELD_TYPE_FLOAT -> c.getDouble(i).toString()
                                            Cursor.FIELD_TYPE_STRING -> c.getString(i)
                                            Cursor.FIELD_TYPE_BLOB -> "[BLOB ${c.getBlob(i).size} bytes]"
                                            else -> c.getString(i)
                                        }
                                        row[colName] = valueStr
                                    }
                                    rows.add(row)
                                    if (rows.size >= limit) break
                                }

                                if (!typesExtracted) {
                                    columnNames.forEach { columnsWithTypes.add(ColumnHeader(it, "UNKNOWN")) }
                                }

                                val elapsed = SystemClock.elapsedRealtime() - start
                                resultData = QueryResultData(
                                    mode = "App UID (${Process.myUid()})",
                                    success = true,
                                    durationMs = elapsed,
                                    columns = columnsWithTypes,
                                    rows = rows,
                                    rawOutput = "Retrieved ${rows.size} rows (${c.count} total in provider)."
                                )
                            }
                        }
                    }
                    ProviderOperation.INSERT -> {
                        val cv = ContentValues().apply {
                            bindKeyValues.forEach { (k, v) -> put(k, v) }
                        }
                        val resultUri = withContext(Dispatchers.IO) {
                            context.contentResolver.insert(parsedUri, cv)
                        }
                        val elapsed = SystemClock.elapsedRealtime() - start
                        resultData = QueryResultData(
                            mode = "App UID (${Process.myUid()})",
                            success = resultUri != null,
                            durationMs = elapsed,
                            columns = listOf(ColumnHeader("Result URI", "URI")),
                            rows = listOf(mapOf("Result URI" to (resultUri?.toString() ?: "null"))),
                            rawOutput = "Insert completed: $resultUri"
                        )
                    }
                    ProviderOperation.UPDATE -> {
                        val cv = ContentValues().apply {
                            bindKeyValues.forEach { (k, v) -> put(k, v) }
                        }
                        val sel = selection.ifBlank { null }
                        val selArgs = if (selectionArgs.isNotBlank()) selectionArgs.split(",").map { it.trim() }.toTypedArray() else null
                        val updatedCount = withContext(Dispatchers.IO) {
                            context.contentResolver.update(parsedUri, cv, sel, selArgs)
                        }
                        val elapsed = SystemClock.elapsedRealtime() - start
                        resultData = QueryResultData(
                            mode = "App UID (${Process.myUid()})",
                            success = true,
                            durationMs = elapsed,
                            columns = listOf(ColumnHeader("Rows Updated", "INTEGER")),
                            rows = listOf(mapOf("Rows Updated" to updatedCount.toString())),
                            rawOutput = "Update affected $updatedCount rows."
                        )
                    }
                    ProviderOperation.DELETE -> {
                        val sel = selection.ifBlank { null }
                        val selArgs = if (selectionArgs.isNotBlank()) selectionArgs.split(",").map { it.trim() }.toTypedArray() else null
                        val deletedCount = withContext(Dispatchers.IO) {
                            context.contentResolver.delete(parsedUri, sel, selArgs)
                        }
                        val elapsed = SystemClock.elapsedRealtime() - start
                        resultData = QueryResultData(
                            mode = "App UID (${Process.myUid()})",
                            success = true,
                            durationMs = elapsed,
                            columns = listOf(ColumnHeader("Rows Deleted", "INTEGER")),
                            rows = listOf(mapOf("Rows Deleted" to deletedCount.toString())),
                            rawOutput = "Delete affected $deletedCount rows."
                        )
                    }
                }
            } catch (se: SecurityException) {
                val elapsed = SystemClock.elapsedRealtime() - start
                resultData = QueryResultData(
                    mode = "App UID (${Process.myUid()})",
                    success = false,
                    durationMs = elapsed,
                    columns = emptyList(),
                    rows = emptyList(),
                    errorMessage = se.message ?: "SecurityException: Permission Denial",
                    isSecurityException = true
                )
            } catch (t: Throwable) {
                val elapsed = SystemClock.elapsedRealtime() - start
                resultData = QueryResultData(
                    mode = "App UID (${Process.myUid()})",
                    success = false,
                    durationMs = elapsed,
                    columns = emptyList(),
                    rows = emptyList(),
                    errorMessage = "${t.javaClass.simpleName}: ${t.message}"
                )
            } finally {
                isRunning = false
            }
        }
    }

    fun executeShellUid() {
        if (isRunning) return
        isRunning = true
        scope.launch {
            val cmd = buildShellCommand()
            val start = SystemClock.elapsedRealtime()
            val res = client.exec(cmd)
            val elapsed = SystemClock.elapsedRealtime() - start

            val isSecEx = res.output.contains("SecurityException", ignoreCase = true) ||
                    res.output.contains("Permission Denial", ignoreCase = true)
            val hasError = !res.ok ||
                    res.output.contains("Error", ignoreCase = true) ||
                    res.output.contains("Exception", ignoreCase = true) ||
                    isSecEx

            if (hasError) {
                resultData = QueryResultData(
                    mode = "Shell UID 2000 (ADB)",
                    success = false,
                    durationMs = elapsed,
                    columns = emptyList(),
                    rows = emptyList(),
                    rawOutput = res.output,
                    errorMessage = if (res.output.isNotBlank()) res.output.trim() else (res.error ?: "Execution failed"),
                    isSecurityException = isSecEx
                )
            } else if (operation != ProviderOperation.QUERY) {
                // Non-query operations (insert, update, delete)
                val outputText = res.output.trim().ifEmpty { res.error ?: "Operation executed successfully." }
                resultData = QueryResultData(
                    mode = "Shell UID 2000 (ADB)",
                    success = true,
                    durationMs = elapsed,
                    columns = listOf(ColumnHeader("Operation Result", "TEXT")),
                    rows = listOf(mapOf("Operation Result" to outputText)),
                    rawOutput = outputText
                )
            } else {
                // Parse shell `content query` format
                // Typical line: "Row: 0 _id=1, name=foo, value=bar"
                val lines = res.output.lines()
                val parsedRows = mutableListOf<Map<String, String?>>()
                val columnSet = linkedSetOf<String>()
                val rowRegex = Regex("""^Row:\s*\d+\s+(.*)$""")

                lines.forEach { line ->
                    val trimmed = line.trim()
                    val match = rowRegex.find(trimmed)
                    if (match != null) {
                        val rowData = match.groupValues[1].trim()
                        val pairs = rowData.split(", ")
                        val rowMap = mutableMapOf<String, String?>()
                        pairs.forEach { p ->
                            val kv = p.split("=", limit = 2)
                            if (kv.isNotEmpty()) {
                                val k = kv[0].trim()
                                val v = kv.getOrNull(1)
                                columnSet.add(k)
                                rowMap[k] = v
                            }
                        }
                        if (rowMap.isNotEmpty()) {
                            parsedRows.add(rowMap)
                        }
                    }
                }

                val columnsWithTypes = columnSet.map { ColumnHeader(it, "TEXT") }

                resultData = QueryResultData(
                    mode = "Shell UID 2000 (ADB)",
                    success = true,
                    durationMs = elapsed,
                    columns = columnsWithTypes,
                    rows = parsedRows,
                    rawOutput = res.output.trim()
                )
            }
            isRunning = false
        }
    }

    // Cell Detail Dialog
    selectedCellDetail?.let { (col, valStr) ->
        AlertDialog(
            onDismissRequest = { selectedCellDetail = null },
            title = { Text(col) },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    Text(
                        valStr ?: "NULL",
                        fontFamily = FontFamily.Monospace,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText(col, valStr ?: ""))
                        Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                        selectedCellDetail = null
                    }
                ) {
                    Text("Copy")
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedCellDetail = null }) {
                    Text("Close")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("ContentProvider Probe") },
                navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Intro Card
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(14.dp)) {
                    Text(
                        "Interactive ContentProvider Inspector",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Probe system and third-party ContentProviders with live projections, filters, and schema detection. Test runtime permissions in App UID mode or query unexported internal databases via UID 2000 shell.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            AdbStatusStrip(adbState = adbState, onConnect = { scope.launch { client.connect() } })

            // Operation selector
            Column {
                Text("Operation", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                Row(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ProviderOperation.values().forEach { op ->
                        FilterChip(
                            selected = operation == op,
                            onClick = { operation = op },
                            label = { Text(op.label) }
                        )
                    }
                }
            }

            // Presets
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Common Presets", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    COMMON_PRESETS.forEach { preset ->
                        SuggestionChip(
                            onClick = {
                                uriString = preset.uri
                                projection = preset.projection
                                sortOrder = preset.sortOrder
                            },
                            label = { Text(preset.title) }
                        )
                    }
                }
            }

            // URI Input
            OutlinedTextField(
                value = uriString,
                onValueChange = { uriString = it },
                label = { Text("Provider URI") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            // Query Specific Parameters
            if (operation == ProviderOperation.QUERY) {
                OutlinedTextField(
                    value = projection,
                    onValueChange = { projection = it },
                    label = { Text("Projection (Columns, comma-separated)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = selection,
                        onValueChange = { selection = it },
                        label = { Text("Selection (WHERE)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = selectionArgs,
                        onValueChange = { selectionArgs = it },
                        label = { Text("Selection Args") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = sortOrder,
                        onValueChange = { sortOrder = it },
                        label = { Text("Sort Order") },
                        singleLine = true,
                        modifier = Modifier.weight(1.2f)
                    )
                    OutlinedTextField(
                        value = rowLimit,
                        onValueChange = { rowLimit = it },
                        label = { Text("Row Limit") },
                        singleLine = true,
                        modifier = Modifier.weight(0.8f)
                    )
                }
            }

            // Insert / Update Binds
            if (operation == ProviderOperation.INSERT || operation == ProviderOperation.UPDATE) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("ContentValues (Key-Value Binds)", fontWeight = FontWeight.Bold)

                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            OutlinedTextField(
                                value = newKey,
                                onValueChange = { newKey = it },
                                label = { Text("Column Key") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = newVal,
                                onValueChange = { newVal = it },
                                label = { Text("Value") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            Button(
                                onClick = {
                                    if (newKey.isNotBlank()) {
                                        bindKeyValues.add(newKey.trim() to newVal)
                                        newKey = ""
                                        newVal = ""
                                    }
                                },
                                modifier = Modifier.padding(top = 8.dp)
                            ) {
                                Text("Add")
                            }
                        }

                        bindKeyValues.forEachIndexed { index, (k, v) ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("$k = $v", fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodyMedium)
                                IconButton(onClick = { bindKeyValues.removeAt(index) }) {
                                    Text("✕", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            // Command Preview
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Generated Shell Command", color = Color.LightGray, style = MaterialTheme.typography.labelSmall)
                        TextButton(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("ADB Command", buildShellCommand()))
                                Toast.makeText(context, "Command copied to clipboard", Toast.LENGTH_SHORT).show()
                            }
                        ) {
                            Text("Copy", color = Color(0xFF8C9CFF))
                        }
                    }
                    Text(
                        buildShellCommand(),
                        fontFamily = FontFamily.Monospace,
                        color = Color(0xFF81C784),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            // Execute Buttons
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = { executeAppUid() },
                    modifier = Modifier.weight(1f),
                    enabled = !isRunning
                ) {
                    Text("Execute (App UID)")
                }

                Button(
                    onClick = { executeShellUid() },
                    modifier = Modifier.weight(1f),
                    enabled = !isRunning,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
                ) {
                    Text(
                        if (adbState == AdbClient.State.Connected) "Execute (Shell UID 2000)"
                        else "Execute (ADB Connect)"
                    )
                }
            }

            if (isRunning) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(Modifier.size(24.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Querying ContentProvider…", style = MaterialTheme.typography.bodyMedium)
                }
            }

            // Result Display
            resultData?.let { res ->
                val cardColor = if (res.success) Color(0xFFF1F8E9) else Color(0xFFFFEBEE)
                val textColor = if (res.success) Color(0xFF2E7D32) else Color(0xFFC62828)

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = cardColor)
                ) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val title = when {
                                !res.success -> "✗ Operation Failed"
                                operation == ProviderOperation.QUERY -> "✓ ${res.rows.size} rows returned"
                                operation == ProviderOperation.INSERT -> "✓ Insert Succeeded"
                                operation == ProviderOperation.UPDATE -> "✓ Update Succeeded"
                                operation == ProviderOperation.DELETE -> "✓ Delete Succeeded"
                                else -> "✓ Operation Complete"
                            }
                            Text(
                                title,
                                fontWeight = FontWeight.Bold,
                                color = textColor
                            )
                            Text("${res.durationMs}ms · ${res.mode}", style = MaterialTheme.typography.bodySmall)
                        }

                        if (res.isSecurityException) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFFFFCDD2))
                                    .padding(8.dp)
                            ) {
                                Column {
                                    Text(
                                        "🔒 SecurityException: Permission Denial",
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFB71C1C),
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                    Text(
                                        "The App UID was rejected by the Android OS framework. Run with Privileged Shell UID 2000 (ADB) to bypass app runtime permissions.",
                                        color = Color.Black,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        }

                        if (!res.errorMessage.isNullOrBlank()) {
                            Text(
                                res.errorMessage,
                                fontFamily = FontFamily.Monospace,
                                color = Color.Red,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }

                        if (res.rows.isEmpty() && res.rawOutput.isNotBlank() && res.errorMessage.isNullOrBlank()) {
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFF212121))
                                    .padding(8.dp)
                            ) {
                                Text(
                                    res.rawOutput,
                                    fontFamily = FontFamily.Monospace,
                                    color = Color(0xFFE0E0E0),
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }

                        if (res.success && res.rows.isNotEmpty()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        val json = buildJson(res.rows)
                                        clipboard.setPrimaryClip(ClipData.newPlainText("JSON Results", json))
                                        Toast.makeText(context, "Copied JSON to clipboard", Toast.LENGTH_SHORT).show()
                                    }
                                ) {
                                    Text("Copy JSON")
                                }
                                OutlinedButton(
                                    onClick = {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        val csv = buildCsv(res.columns, res.rows)
                                        clipboard.setPrimaryClip(ClipData.newPlainText("CSV Results", csv))
                                        Toast.makeText(context, "Copied CSV to clipboard", Toast.LENGTH_SHORT).show()
                                    }
                                ) {
                                    Text("Copy CSV")
                                }
                                OutlinedButton(
                                    onClick = { showRawOutput = !showRawOutput }
                                ) {
                                    Text(if (showRawOutput) "Table View" else "Raw View")
                                }
                            }

                            if (showRawOutput) {
                                Box(
                                    Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color(0xFF212121))
                                        .padding(8.dp)
                                ) {
                                    Text(
                                        res.rawOutput.ifBlank { "No raw output" },
                                        fontFamily = FontFamily.Monospace,
                                        color = Color(0xFFE0E0E0),
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            } else {
                                // Interactive Table
                                ResultsTable(
                                    columns = res.columns,
                                    rows = res.rows,
                                    onCellClick = { col, valStr -> selectedCellDetail = col to valStr }
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun ResultsTable(
    columns: List<ColumnHeader>,
    rows: List<Map<String, String?>>,
    onCellClick: (String, String?) -> Unit
) {
    val horizontalScroll = rememberScrollState()

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(horizontalScroll)
                .padding(8.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .padding(vertical = 8.dp)
            ) {
                Box(Modifier.width(44.dp).padding(horizontal = 4.dp)) {
                    Text("#", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
                }
                columns.forEach { col ->
                    Column(
                        Modifier
                            .width(160.dp)
                            .padding(horizontal = 6.dp)
                    ) {
                        Text(col.name, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(col.type, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            HorizontalDivider()

            // Data Rows
            rows.forEachIndexed { index, row ->
                val rowBg = if (index % 2 == 0) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                Row(
                    modifier = Modifier
                        .background(rowBg)
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(Modifier.width(44.dp).padding(horizontal = 4.dp)) {
                        Text("${index + 1}", fontSize = 11.sp, color = Color.Gray)
                    }
                    columns.forEach { col ->
                        val cellVal = row[col.name]
                        Box(
                            Modifier
                                .width(160.dp)
                                .clickable { onCellClick(col.name, cellVal) }
                                .padding(horizontal = 6.dp)
                        ) {
                            Text(
                                text = cellVal ?: "NULL",
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp,
                                color = if (cellVal == null) Color.Gray else Color.Unspecified
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun buildJson(rows: List<Map<String, String?>>): String {
    val sb = StringBuilder("[\n")
    rows.forEachIndexed { i, row ->
        sb.append("  {\n")
        val entries = row.entries.toList()
        entries.forEachIndexed { j, (k, v) ->
            val escapedV = v?.replace("\"", "\\\"")?.replace("\n", "\\n")
            val valStr = if (escapedV != null) "\"$escapedV\"" else "null"
            sb.append("    \"$k\": $valStr")
            if (j < entries.size - 1) sb.append(",")
            sb.append("\n")
        }
        sb.append("  }")
        if (i < rows.size - 1) sb.append(",")
        sb.append("\n")
    }
    sb.append("]")
    return sb.toString()
}

private fun buildCsv(columns: List<ColumnHeader>, rows: List<Map<String, String?>>): String {
    val sb = StringBuilder()
    sb.append(columns.joinToString(",") { "\"${it.name}\"" })
    sb.append("\n")
    rows.forEach { row ->
        val line = columns.joinToString(",") { col ->
            val v = row[col.name] ?: ""
            "\"${v.replace("\"", "\"\"")}\""
        }
        sb.append(line).append("\n")
    }
    return sb.toString()
}

@Composable
private fun AdbStatusStrip(adbState: AdbClient.State, onConnect: () -> Unit) {
    val (label, color, showConnect) = when (adbState) {
        AdbClient.State.Connected -> Triple("ADB connected", Color(0xFF4CAF50), false)
        AdbClient.State.Connecting -> Triple("Connecting…", Color(0xFFFFB300), false)
        AdbClient.State.Disconnected -> Triple("Not connected — connect first", Color(0xFF9AA0A6), true)
        AdbClient.State.NeedsPair -> Triple("Pair first (use adb_c)", Color(0xFFE57373), false)
        AdbClient.State.Failed -> Triple("ADB connect failed — retry", Color(0xFFE57373), true)
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .background(color, RoundedCornerShape(50))
            )
            Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
            if (showConnect) OutlinedButton(onClick = onConnect) { Text("Connect") }
        }
    }
}
