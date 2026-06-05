package pro.jayeshseth.devcontrols.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import pro.jayeshseth.devcontrols.ui.adbtools.AdbToolScaffold

/**
 * Feature Flags — browse and edit Phenotype/`device_config` flags by namespace.
 * Flip a server-gated flag locally to exercise gated features in your own app.
 * Snapshot/restore a namespace so you can undo. UID-2000 shell.
 */
@Composable
fun FeatureFlagsLabScreen(onBack: () -> Unit) {
    AdbToolScaffold("Feature Flags", onBack, scrollable = false) { runner ->
        // namespace -> (key -> value)
        var all by remember { mutableStateOf<Map<String, Map<String, String>>>(emptyMap()) }
        var loading by remember { mutableStateOf(false) }
        var ns by remember { mutableStateOf<String?>(null) }
        var query by remember { mutableStateOf("") }
        var menuOpen by remember { mutableStateOf(false) }
        var editing by remember { mutableStateOf<Pair<String, String>?>(null) }
        var reloadKey by remember { mutableIntStateOf(0) }
        var snapshot by remember { mutableStateOf<Map<String, String>?>(null) }

        LaunchedEffect(reloadKey, runner.connected) {
            if (!runner.connected) return@LaunchedEffect
            loading = true
            val out = runner.exec("device_config list").output
            val map = HashMap<String, HashMap<String, String>>()
            out.lineSequence().forEach { line ->
                val slash = line.indexOf('/'); val eq = line.indexOf('=')
                if (slash > 0 && eq > slash) {
                    val n = line.substring(0, slash)
                    val k = line.substring(slash + 1, eq)
                    val v = line.substring(eq + 1)
                    map.getOrPut(n) { HashMap() }[k] = v
                }
            }
            all = map.mapValues { it.value.toMap() }
            if (ns == null) ns = all.keys.sorted().firstOrNull()
            loading = false
        }

        val curNs = ns
        val entries = remember(curNs, all, query) {
            val m = all[curNs] ?: emptyMap()
            (if (query.isBlank()) m.entries else m.entries.filter { it.key.contains(query, true) || it.value.contains(query, true) })
                .map { it.key to it.value }.sortedBy { it.first }
        }

        Spacer(Modifier.height(8.dp))
        Box {
            OutlinedButton(onClick = { menuOpen = true }) { Text(curNs ?: "Loading namespaces…") }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                Box(Modifier.heightIn(max = 400.dp)) {
                    LazyColumn {
                        items(all.keys.sorted()) { name ->
                            DropdownMenuItem(text = { Text("$name (${all[name]?.size ?: 0})") },
                                onClick = { ns = name; menuOpen = false; query = "" })
                        }
                    }
                }
            }
        }

        OutlinedTextField(query, { query = it }, label = { Text("Filter ${entries.size} flags") },
            singleLine = true, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(bottom = 8.dp)) {
            OutlinedButton(onClick = { editing = "" to "" }, enabled = curNs != null) { Text("+ Add flag") }
            OutlinedButton(onClick = { snapshot = all[curNs] }, enabled = curNs != null) {
                Text(if (snapshot == null) "Snapshot" else "Snapshot ✓")
            }
            OutlinedButton(onClick = {
                val base = snapshot ?: return@OutlinedButton
                base.forEach { (k, v) -> runner.run("device_config put $curNs $k $v", toast = false) }
                reloadKey++
            }, enabled = snapshot != null && curNs != null) { Text("Restore") }
        }

        if (loading) Text("Loading…", Modifier.padding(8.dp))
        LazyColumn(Modifier.fillMaxWidth()) {
            items(entries) { (k, v) ->
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
                title = { Text(if (k.isEmpty()) "Add flag to $curNs" else "Edit flag") },
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
                        runner.run("device_config put $curNs ${key.trim()} ${value.trim()}") { reloadKey++ }
                        editing = null
                    }, enabled = key.isNotBlank()) { Text("Save") }
                },
                dismissButton = {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (k.isNotEmpty()) TextButton(onClick = {
                            runner.run("device_config delete $curNs ${key.trim()}") { reloadKey++ }
                            editing = null
                        }) { Text("Delete") }
                        TextButton(onClick = { editing = null }) { Text("Cancel") }
                    }
                }
            )
        }
    }
}
