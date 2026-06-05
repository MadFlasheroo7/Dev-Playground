package pro.jayeshseth.devcontrols.ui

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import pro.jayeshseth.devcontrols.cloak.A11yCloakService
import pro.jayeshseth.devcontrols.data.A11yCloakPrefs
import pro.jayeshseth.devcontrols.util.AppEntry
import pro.jayeshseth.devcontrols.util.ForegroundAppDetector
import pro.jayeshseth.devcontrols.util.InstalledApps

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun A11yCloakScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs = remember { A11yCloakPrefs.get(context) }

    val enabled by prefs.enabled.collectAsState(initial = false)
    val targets by prefs.targetPackages.collectAsState(initial = emptySet())
    val alsoHideDev by prefs.alsoHideDev.collectAsState(initial = false)
    val serviceRunning by prefs.serviceRunning.collectAsState(initial = false)
    val cloakActive by prefs.cloakActive.collectAsState(initial = false)

    var apps by remember { mutableStateOf<List<AppEntry>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var query by remember { mutableStateOf("") }
    val hasUsage = ForegroundAppDetector.hasUsageStatsPermission(context)

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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Accessibility Cloak") },
                navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }
            )
        },
        modifier = Modifier.fillMaxSize()
    ) { inner ->
        Column(
            modifier = Modifier
                .padding(inner)
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            Spacer(Modifier.height(8.dp))
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Hide this app's a11y service", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "While a checked app is foreground, a UsageStats-driven service strips Dev Controls " +
                            "from enabled_accessibility_services so the target's detection logic sees a clean " +
                            "list, then restores it on leave. The poller is independent of accessibility — that's " +
                            "what lets it rebind us after we've been unbound.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(10.dp))

                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            "Caveat: while cloaked our service is unbound, so Kiosk / Hide Dev / Force Dark pause " +
                                "for that window. There is also a ~0.4–1 s poll lag — an app that checks only in its " +
                                "first frame can still catch the service before it's stripped. Use ‘Also hide dev options’ " +
                                "below to cover the dev-options flag during the same window.",
                            modifier = Modifier.padding(12.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }

                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Enabled", style = MaterialTheme.typography.bodyLarge)
                        Switch(
                            checked = enabled,
                            onCheckedChange = { scope.launch { prefs.setEnabled(it) } }
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Also hide dev options", style = MaterialTheme.typography.bodyLarge)
                            Text(
                                "Zero developer-options + USB debugging during the same window " +
                                    "(a11y-based Hide Dev can't, since we're unbound).",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = alsoHideDev,
                            onCheckedChange = { scope.launch { prefs.setAlsoHideDev(it) } }
                        )
                    }

                    if (!hasUsage) {
                        Spacer(Modifier.height(8.dp))
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(Modifier.padding(12.dp)) {
                                Text(
                                    "Usage access not granted — the poller can't see the foreground app. " +
                                        "Grant it (Settings → Usage access → Dev Controls) or via ADB:\n" +
                                        "appops set ${context.packageName} GET_USAGE_STATS allow",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                                Spacer(Modifier.height(6.dp))
                                OutlinedButton(onClick = {
                                    context.startActivity(
                                        Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
                                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    )
                                }) { Text("Open Usage access") }
                            }
                        }
                    }

                    Spacer(Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                if (serviceRunning) "Poller: running" else "Poller: stopped",
                                style = MaterialTheme.typography.bodyLarge
                            )
                            if (cloakActive) {
                                Text(
                                    "● cloaked right now (target foreground)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.tertiary
                                )
                            }
                        }
                        if (serviceRunning) {
                            OutlinedButton(onClick = { A11yCloakService.stop(context) }) { Text("Stop") }
                        } else {
                            Button(
                                enabled = enabled && targets.isNotEmpty() && hasUsage,
                                onClick = { A11yCloakService.start(context) }
                            ) { Text("Start poller") }
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            Text("Cloak from these apps (${targets.size})", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(4.dp))

            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                label = { Text("Filter apps") },
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
                LazyColumn(modifier = Modifier.fillMaxWidth()) {
                    items(filtered, key = { it.packageName }) { app ->
                        val checked = app.packageName in targets
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    scope.launch { prefs.setTargetIncluded(app.packageName, !checked) }
                                }
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(app.label, style = MaterialTheme.typography.bodyLarge)
                                Text(
                                    app.packageName,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Checkbox(
                                checked = checked,
                                onCheckedChange = { wanted ->
                                    scope.launch { prefs.setTargetIncluded(app.packageName, wanted) }
                                }
                            )
                        }
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}
