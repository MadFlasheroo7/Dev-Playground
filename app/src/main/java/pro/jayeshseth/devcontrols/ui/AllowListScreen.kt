package pro.jayeshseth.devcontrols.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import pro.jayeshseth.devcontrols.data.FocusCallsPrefs
import pro.jayeshseth.devcontrols.util.AppEntry
import pro.jayeshseth.devcontrols.util.InstalledApps

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AllowListScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { FocusCallsPrefs.get(context) }
    val scope = rememberCoroutineScope()
    val allowed by prefs.allowedPackages.collectAsState(initial = emptySet())

    var apps by remember { mutableStateOf<List<AppEntry>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        apps = withContext(Dispatchers.IO) { InstalledApps.userApps(context.packageManager) }
        loading = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Allow-listed apps") },
                navigationIcon = {
                    TextButton(onClick = onBack) { Text("Back") }
                }
            )
        },
        modifier = Modifier.fillMaxSize()
    ) { inner ->
        if (loading) {
            Text(
                "Loading installed apps…",
                modifier = Modifier.padding(inner).padding(16.dp)
            )
        } else {
            LazyColumn(modifier = Modifier.padding(inner)) {
                items(apps, key = { it.packageName }) { app ->
                    AllowRow(
                        label = app.label,
                        pkg = app.packageName,
                        checked = app.packageName in allowed,
                        onToggle = { isChecked ->
                            scope.launch { prefs.setAllowed(app.packageName, isChecked) }
                        }
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
private fun AllowRow(
    label: String,
    pkg: String,
    checked: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle(!checked) }
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        androidx.compose.foundation.layout.Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyLarge)
            Text(
                pkg,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Checkbox(checked = checked, onCheckedChange = onToggle)
    }
}

