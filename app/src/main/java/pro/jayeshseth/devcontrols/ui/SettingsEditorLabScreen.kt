package pro.jayeshseth.devcontrols.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import pro.jayeshseth.devcontrols.ui.adbtools.AdbToolScaffold

private val NAMESPACES = listOf("global", "system", "secure")

/**
 * System Settings Editor — browse / search / edit the global·system·secure namespaces
 * via `settings list/get/put/delete`, plus a snapshot+diff to see which keys a system
 * toggle flips. UID-2000 shell.
 */
@Composable
fun SettingsEditorLabScreen(onBack: () -> Unit) {
    AdbToolScaffold("Settings Editor", onBack, scrollable = false) { runner ->
        var tab by remember { mutableIntStateOf(0) }
        var query by remember { mutableStateOf("") }
        var entries by remember { mutableStateOf<List<Pair<String, String>>>(emptyList()) }
        var loading by remember { mutableStateOf(false) }
        var editing by remember { mutableStateOf<Pair<String, String>?>(null) }
        var reloadKey by remember { mutableIntStateOf(0) }
        // snapshot: ns -> (key -> value)
        var snapshot by remember { mutableStateOf<Map<String, Map<String, String>>?>(null) }
        var diffText by remember { mutableStateOf<String?>(null) }

        val ns = NAMESPACES[tab]

        fun parse(out: String): List<Pair<String, String>> =
            out.lineSequence().mapNotNull { line ->
                val i = line.indexOf('=')
                if (i <= 0) null else line.substring(0, i) to line.substring(i + 1)
            }.sortedBy { it.first }.toList()

        LaunchedEffect(tab, reloadKey, runner.connected) {
            if (!runner.connected) return@LaunchedEffect
            loading = true
            entries = parse(runner.exec("settings list $ns").output)
            loading = false
        }

        val filtered = remember(query, entries) {
            if (query.isBlank()) entries
            else entries.filter { it.first.contains(query, true) || it.second.contains(query, true) }
        }

        Spacer(Modifier.height(8.dp))
        TabRow(selectedTabIndex = tab) {
            NAMESPACES.forEachIndexed { i, name ->
                Tab(selected = tab == i, onClick = { tab = i }, text = { Text(name) })
            }
        }
        OutlinedTextField(query, { query = it }, label = { Text("Filter ${entries.size} keys") },
            singleLine = true, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(bottom = 8.dp)) {
            OutlinedButton(onClick = { editing = "" to "" }) { Text("+ Add key") }
            SnapshotButtons(
                onSnapshot = { snap -> snapshot = snap; diffText = "Snapshot captured (${snap.values.sumOf { it.size }} keys). Change something, then Diff." },
                onDiff = { now ->
                    val base = snapshot
                    diffText = if (base == null) "Take a snapshot first." else buildDiff(base, now)
                },
                runner = runner
            )
        }

        diffText?.let {
            Text(it, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.fillMaxWidth().heightIn(max = 160.dp).padding(bottom = 8.dp))
            HorizontalDivider()
        }

        if (loading) Text("Loading…", Modifier.padding(8.dp))
        LazyColumn(Modifier.fillMaxWidth()) {
            items(filtered) { (k, v) ->
                Column(Modifier.fillMaxWidth().clickable { editing = k to v }.padding(vertical = 8.dp)) {
                    Text(k, style = MaterialTheme.typography.bodyMedium, fontFamily = FontFamily.Monospace)
                    Text(v.ifBlank { "(empty)" }, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant, fontFamily = FontFamily.Monospace)
                }
                HorizontalDivider()
            }
        }

        editing?.let { (k, v) ->
            var key by remember(k) { mutableStateOf(k) }
            var value by remember(k, v) { mutableStateOf(v) }
            AlertDialog(
                onDismissRequest = { editing = null },
                title = { Text(if (k.isEmpty()) "Add to $ns" else "Edit $ns") },
                text = {
                    Column {
                        OutlinedTextField(key, { key = it }, label = { Text("key") }, singleLine = true,
                            enabled = k.isEmpty(), modifier = Modifier.fillMaxWidth())
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(value, { value = it }, label = { Text("value") }, modifier = Modifier.fillMaxWidth())
                    }
                },
                confirmButton = {
                    Button(onClick = {
                        runner.run("settings put $ns ${key.trim()} ${value.trim()}") { reloadKey++ }
                        editing = null
                    }, enabled = key.isNotBlank()) { Text("Save") }
                },
                dismissButton = {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (k.isNotEmpty()) TextButton(onClick = {
                            runner.run("settings delete $ns ${key.trim()}") { reloadKey++ }
                            editing = null
                        }) { Text("Delete") }
                        TextButton(onClick = { editing = null }) { Text("Cancel") }
                    }
                }
            )
        }
    }
}

@Composable
private fun SnapshotButtons(
    onSnapshot: (Map<String, Map<String, String>>) -> Unit,
    onDiff: (Map<String, Map<String, String>>) -> Unit,
    runner: pro.jayeshseth.devcontrols.ui.adbtools.AdbRunner,
) {
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    suspend fun captureAll(): Map<String, Map<String, String>> = NAMESPACES.associateWith { ns ->
        runner.exec("settings list $ns").output.lineSequence().mapNotNull { l ->
            val i = l.indexOf('='); if (i <= 0) null else l.substring(0, i) to l.substring(i + 1)
        }.toMap()
    }
    OutlinedButton(onClick = { scope.launch { onSnapshot(captureAll()) } }) { Text("Snapshot") }
    OutlinedButton(onClick = { scope.launch { onDiff(captureAll()) } }) { Text("Diff") }
}

private fun buildDiff(base: Map<String, Map<String, String>>, now: Map<String, Map<String, String>>): String {
    val sb = StringBuilder()
    for (ns in NAMESPACES) {
        val b = base[ns] ?: emptyMap(); val n = now[ns] ?: emptyMap()
        (b.keys + n.keys).sorted().forEach { k ->
            val bv = b[k]; val nv = n[k]
            if (bv != nv) {
                when {
                    bv == null -> sb.append("+ [$ns] $k = $nv\n")
                    nv == null -> sb.append("- [$ns] $k (was $bv)\n")
                    else -> sb.append("~ [$ns] $k: $bv → $nv\n")
                }
            }
        }
    }
    return if (sb.isEmpty()) "No changes since snapshot." else sb.toString()
}
