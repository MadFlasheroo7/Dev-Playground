package pro.jayeshseth.devcontrols.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
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
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import pro.jayeshseth.devcontrols.adb.AdbClient
import pro.jayeshseth.devcontrols.adb.DumpSection
import pro.jayeshseth.devcontrols.adb.InfoDumpRecipes
import pro.jayeshseth.devcontrols.util.AppEntry
import pro.jayeshseth.devcontrols.util.InstalledApps

private enum class InfoMode { Picking, Inspecting }

private data class SectionResult(
    val ok: Boolean,
    val output: String,
    val durationMs: Long,
)

/** App info dump — build/version/permission/signature details for a chosen package. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InfoDumpScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val client = remember { AdbClient.get(context) }
    val adbState by client.state.collectAsState()

    var mode by remember { mutableStateOf(InfoMode.Picking) }
    var selectedApp by remember { mutableStateOf<AppEntry?>(null) }
    val results = remember { mutableStateMapOf<String, SectionResult>() }
    var running by remember { mutableStateOf(false) }
    var doneCount by remember { mutableStateOf(0) }
    var totalCount by remember { mutableStateOf(0) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        when (mode) {
                            InfoMode.Picking -> "Info Dump — pick app"
                            InfoMode.Inspecting -> selectedApp?.label ?: "Info Dump"
                        }
                    )
                },
                navigationIcon = {
                    TextButton(onClick = {
                        if (mode == InfoMode.Inspecting) {
                            mode = InfoMode.Picking
                            results.clear()
                            selectedApp = null
                        } else onBack()
                    }) { Text(if (mode == InfoMode.Inspecting) "Apps" else "Back") }
                },
                actions = {
                    if (mode == InfoMode.Inspecting && results.isNotEmpty()) {
                        TextButton(onClick = {
                            copyAll(context, selectedApp!!, results)
                        }) { Text("Copy all") }
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
                .padding(horizontal = 12.dp)
        ) {
            AdbStrip(adbState) { scope.launch { client.connect() } }
            Spacer(Modifier.height(8.dp))

            when (mode) {
                InfoMode.Picking -> AppPicker(onPick = {
                    selectedApp = it
                    mode = InfoMode.Inspecting
                    results.clear()
                })
                InfoMode.Inspecting -> selectedApp?.let { app ->
                    InspectorView(
                        app = app,
                        results = results,
                        running = running,
                        doneCount = doneCount,
                        totalCount = totalCount,
                        onRunAll = {
                            scope.launch {
                                running = true
                                results.clear()
                                doneCount = 0
                                totalCount = InfoDumpRecipes.sections.size
                                for (section in InfoDumpRecipes.sections) {
                                    val cmd = section.command(app.packageName)
                                    val r = client.exec(cmd)
                                    results[section.id] = SectionResult(
                                        ok = r.ok,
                                        output = if (r.ok) r.output
                                            else "ERROR: ${r.error ?: "unknown"}",
                                        durationMs = r.durationMs
                                    )
                                    doneCount += 1
                                }
                                running = false
                            }
                        },
                        onRunSection = { section ->
                            scope.launch {
                                running = true
                                val r = client.exec(section.command(app.packageName))
                                results[section.id] = SectionResult(
                                    ok = r.ok,
                                    output = if (r.ok) r.output else "ERROR: ${r.error ?: "unknown"}",
                                    durationMs = r.durationMs
                                )
                                running = false
                            }
                        },
                        onCopySection = { sid ->
                            val r = results[sid] ?: return@InspectorView
                            copy(context, r.output)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun AdbStrip(state: AdbClient.State, onConnect: () -> Unit) {
    val (label, color, showConnect) = when (state) {
        AdbClient.State.Connected -> Triple("ADB connected — UID 2000 shell", Color(0xFF4CAF50), false)
        AdbClient.State.Connecting -> Triple("Connecting…", Color(0xFFFFB300), false)
        AdbClient.State.Disconnected -> Triple("Not connected — connect first (adb_c)", Color(0xFF9AA0A6), true)
        AdbClient.State.NeedsPair -> Triple("Pairing required — go to adb_c", Color(0xFFE57373), false)
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
            Box(modifier = Modifier.size(10.dp).background(color, RoundedCornerShape(50)))
            Text(label, modifier = Modifier.weight(1f))
            if (showConnect) OutlinedButton(onClick = onConnect) { Text("Connect") }
        }
    }
}

@Composable
private fun AppPicker(onPick: (AppEntry) -> Unit) {
    val context = LocalContext.current
    var apps by remember { mutableStateOf<List<AppEntry>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var query by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        apps = withContext(Dispatchers.IO) { InstalledApps.userApps(context.packageManager) }
        loading = false
    }

    val filtered by remember(query, apps) {
        derivedStateOf {
            if (query.isBlank()) apps
            else apps.filter {
                it.label.contains(query, ignoreCase = true) ||
                    it.packageName.contains(query, ignoreCase = true)
            }
        }
    }

    OutlinedTextField(
        value = query,
        onValueChange = { query = it },
        label = { Text("Filter apps (e.g. \"moneyview\")") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
        keyboardOptions = KeyboardOptions(
            autoCorrectEnabled = false,
            capitalization = KeyboardCapitalization.None
        )
    )
    Spacer(Modifier.height(8.dp))

    if (loading) {
        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
    } else {
        LazyColumn {
            items(filtered, key = { it.packageName }) { app ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onPick(app) }
                        .padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(app.label, style = MaterialTheme.typography.bodyLarge)
                        Text(
                            app.packageName,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
                HorizontalDivider()
            }
        }
    }
}

@Composable
private fun InspectorView(
    app: AppEntry,
    results: Map<String, SectionResult>,
    running: Boolean,
    doneCount: Int,
    totalCount: Int,
    onRunAll: () -> Unit,
    onRunSection: (DumpSection) -> Unit,
    onCopySection: (String) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // Header card
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(app.label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    app.packageName,
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Pulls everything an attacker with shell-level ADB access (UID 2000) can see about this app — metadata, exported components, sigs, external storage, /proc, etc. Designed to validate that nothing sensitive leaks to attacker-reachable surfaces.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = onRunAll,
                    enabled = !running,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        if (running) "Running ${doneCount}/${totalCount}…"
                        else "Run full dump (${InfoDumpRecipes.sections.size} sections)"
                    )
                }
                if (running) {
                    Spacer(Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { if (totalCount == 0) 0f else doneCount.toFloat() / totalCount },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        InfoDumpRecipes.sections.forEach { section ->
            SectionCard(
                section = section,
                result = results[section.id],
                running = running,
                onRun = { onRunSection(section) },
                onCopy = { onCopySection(section.id) }
            )
            Spacer(Modifier.height(10.dp))
        }
        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun SectionCard(
    section: DumpSection,
    result: SectionResult?,
    running: Boolean,
    onRun: () -> Unit,
    onCopy: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(section.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium)
                    Text(
                        section.subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (result != null) {
                    Text(
                        "${result.durationMs}ms",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(Modifier.width(8.dp))
                    TextButton(onClick = onCopy) { Text("Copy") }
                }
                Spacer(Modifier.width(4.dp))
                OutlinedButton(onClick = onRun, enabled = !running) { Text("Run") }
            }
            section.riskHint?.let { hint ->
                Spacer(Modifier.height(4.dp))
                Text(
                    "⚠ $hint",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.tertiary
                )
            }
            if (result != null) {
                Spacer(Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF1A1A1A), RoundedCornerShape(6.dp))
                        .padding(8.dp)
                ) {
                    Text(
                        result.output.ifBlank { "(empty)" },
                        color = if (result.ok) Color(0xFFCFE9CF) else Color(0xFFFFB4B4),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}

private fun copy(context: Context, text: String) {
    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    cm.setPrimaryClip(ClipData.newPlainText("info dump", text))
    Toast.makeText(context, "Copied", Toast.LENGTH_SHORT).show()
}

private fun copyAll(
    context: Context,
    app: AppEntry,
    results: Map<String, SectionResult>,
) {
    val report = buildString {
        appendLine("# Info Dump — ${app.label} (${app.packageName})")
        appendLine("# Generated by Dev Controls on ${java.time.LocalDateTime.now()}")
        appendLine()
        InfoDumpRecipes.sections.forEach { s ->
            val r = results[s.id] ?: return@forEach
            appendLine("## ${s.title}")
            appendLine("# ${s.subtitle}")
            appendLine("# ran in ${r.durationMs}ms · ${if (r.ok) "ok" else "ERROR"}")
            appendLine()
            appendLine(r.output)
            appendLine()
            appendLine("---")
            appendLine()
        }
    }
    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    cm.setPrimaryClip(ClipData.newPlainText("info dump report", report))
    Toast.makeText(context, "Full report copied (${report.length} chars)", Toast.LENGTH_SHORT).show()
}
