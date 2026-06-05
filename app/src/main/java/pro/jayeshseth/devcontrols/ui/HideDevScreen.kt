package pro.jayeshseth.devcontrols.ui

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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import pro.jayeshseth.devcontrols.data.HideDevPrefs
import pro.jayeshseth.devcontrols.kiosk.KioskAccessibilityService
import pro.jayeshseth.devcontrols.util.AppEntry
import pro.jayeshseth.devcontrols.util.InstalledApps

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HideDevScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs = remember { HideDevPrefs.get(context) }

    val enabled by prefs.enabled.collectAsState(initial = false)
    val targets by prefs.targetPackages.collectAsState(initial = emptySet())
    val includeWifiAdb by prefs.includeWifiAdb.collectAsState(initial = false)
    val hidingActive by prefs.currentlyHiding.collectAsState(initial = false)

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

    val a11yGranted = KioskAccessibilityService.isRunning()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Hide Dev") },
                navigationIcon = {
                    TextButton(onClick = onBack) { Text("Back") }
                }
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
                    Text("Per-app dev-options hiding", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Same trick Geto pulls without Shizuku: while a checked app is in the foreground, " +
                            "this app temporarily sets developer-options-enabled and adb-enabled to 0, and " +
                            "restores them when you switch away. Apps that gate themselves on those flags " +
                            "won't see them on. Requires the Kiosk Accessibility service granted (which it " +
                            "already is on this device).",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
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
                            Text("Also hide wireless ADB", style = MaterialTheme.typography.bodyLarge)
                            Text(
                                "Apps that check `ADB_WIFI_ENABLED` will see 0 too. " +
                                    "Caveat: this also kills the embedded ADB connection (adb_c) for the duration of the hide window — " +
                                    "any in-flight commands will fail until you switch away from the target app.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = includeWifiAdb,
                            onCheckedChange = { scope.launch { prefs.setIncludeWifiAdb(it) } }
                        )
                    }
                    if (!a11yGranted) {
                        Spacer(Modifier.height(8.dp))
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                "Accessibility service not running — grant it in adb_c → Kiosk Mode, " +
                                    "or via Settings → Accessibility → Dev Controls Kiosk.",
                                modifier = Modifier.padding(12.dp),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                    if (hidingActive) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "● currently hiding (target app foregrounded)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            Text(
                "Allow-list (${targets.size} apps selected)",
                style = MaterialTheme.typography.titleSmall
            )
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
                                    scope.launch {
                                        prefs.setTargetIncluded(app.packageName, !checked)
                                    }
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
                                    scope.launch {
                                        prefs.setTargetIncluded(app.packageName, wanted)
                                    }
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
