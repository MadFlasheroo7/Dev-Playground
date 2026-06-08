package pro.jayeshseth.devcontrols.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import pro.jayeshseth.devcontrols.ui.adbtools.AdbToolScaffold
import pro.jayeshseth.devcontrols.ui.adbtools.AppPickerDialog
import pro.jayeshseth.devcontrols.util.AppEntry

/**
 * Package & APK Explorer — pick any installed package and inspect APK path, version,
 * SDK levels, signature, declared/granted permissions, and components. Launch any
 * exported activity, enable/disable the app or a component. UID-2000 shell.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PackageExplorerLabScreen(onBack: () -> Unit) {
    AdbToolScaffold("Package Explorer", onBack, scrollable = false) { runner ->
        val scope = rememberCoroutineScope()
        var picker by remember { mutableStateOf(false) }
        var app by remember { mutableStateOf<AppEntry?>(null) }
        var summary by remember { mutableStateOf("") }
        var activities by remember { mutableStateOf<List<String>>(emptyList()) }
        var permsOut by remember { mutableStateOf("") }
        var rawDump by remember { mutableStateOf("") }
        var showRaw by remember { mutableStateOf(false) }
        var loading by remember { mutableStateOf(false) }

        fun load(pkg: String) {
            loading = true
            scope.launch {
                val path = runner.exec("pm path $pkg").output.trim()
                val dump = runner.exec("dumpsys package $pkg").output
                fun field(re: String): String? = Regex(re).find(dump)?.groupValues?.getOrNull(1)
                val vName = field("versionName=(\\S+)")
                val vCode = field("versionCode=(\\d+)")
                val minSdk = field("minSdk=(\\d+)")
                val tSdk = field("targetSdk=(\\d+)")
                val installer = field("installerPackageName=(\\S+)") ?: field("installInitiator=(\\S+)")
                val enabled = if (dump.contains("enabled=false") || dump.contains("DISABLED")) "maybe disabled" else "enabled"
                summary = buildString {
                    appendLine("package: $pkg")
                    appendLine("version: ${vName ?: "?"} ($vCode)")
                    appendLine("sdk: min ${minSdk ?: "?"} / target ${tSdk ?: "?"}")
                    appendLine("installer: ${installer ?: "—"}")
                    appendLine("state: $enabled")
                    appendLine(path)
                }
                // components: pkg/Component tokens (dedup, activities-ish)
                activities = Regex(Regex.escape(pkg) + "/[\\w.\$]+")
                    .findAll(dump).map { it.value }.distinct().take(40).toList()
                permsOut = runner.exec("dumpsys package $pkg | grep -E 'permission|granted=' ").output.take(3000)
                rawDump = dump.take(8000)
                loading = false
            }
        }

        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { picker = true }) { Text(app?.let { "Change app" } ?: "Pick app") }
            app?.let { Text(it.label, Modifier.padding(top = 12.dp), style = MaterialTheme.typography.bodyMedium) }
        }

        if (loading) Text("Loading…", Modifier.padding(8.dp))

        if (summary.isNotEmpty()) {
            Card(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                Text(summary.trim(), Modifier.padding(12.dp), fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodySmall)
            }
            app?.let { a ->
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AssistChip(onClick = { runner.run("monkey -p ${a.packageName} -c android.intent.category.LAUNCHER 1") }, label = { Text("Launch") }, enabled = !runner.busy)
                    AssistChip(onClick = { runner.run("am force-stop ${a.packageName}") }, label = { Text("Force-stop") }, enabled = !runner.busy)
                    AssistChip(onClick = { runner.run("pm enable ${a.packageName}") { load(a.packageName) } }, label = { Text("Enable") }, enabled = !runner.busy)
                    AssistChip(onClick = { runner.run("pm disable-user --user 0 ${a.packageName}") { load(a.packageName) } }, label = { Text("Disable") }, enabled = !runner.busy)
                }
            }
        }

        if (activities.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            Text("Components — tap to launch as activity (am start -n)", style = MaterialTheme.typography.labelMedium)
            LazyColumn(Modifier.fillMaxWidth().heightIn(max = 220.dp)) {
                items(activities) { comp ->
                    Text(comp, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.fillMaxWidth().clickable { runner.run("am start -n $comp") }.padding(vertical = 6.dp))
                    HorizontalDivider()
                }
            }
        }

        if (permsOut.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = { showRaw = !showRaw }) { Text(if (showRaw) "Show permissions" else "Show raw dumpsys") }
            Text(if (showRaw) rawDump else permsOut, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.fillMaxWidth().heightIn(max = 260.dp).padding(top = 6.dp))
        }
        Spacer(Modifier.height(24.dp))

        if (picker) AppPickerDialog(includeSystem = true, onPick = { app = it; picker = false; load(it.packageName) }, onDismiss = { picker = false })
    }
}
