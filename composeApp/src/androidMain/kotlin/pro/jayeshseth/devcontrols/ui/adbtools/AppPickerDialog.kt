package pro.jayeshseth.devcontrols.ui.adbtools

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import pro.jayeshseth.devcontrols.util.AppEntry
import pro.jayeshseth.devcontrols.util.InstalledApps

/**
 * Searchable package picker dialog. Loads [InstalledApps.allApps] (system included)
 * when [includeSystem] is true, else only launchable user apps. Calls [onPick] with the
 * chosen package and dismisses.
 */
@Composable
fun AppPickerDialog(
    includeSystem: Boolean,
    onPick: (AppEntry) -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    var all by remember { mutableStateOf<List<AppEntry>>(emptyList()) }
    var query by remember { mutableStateOf("") }

    LaunchedEffect(includeSystem) {
        all = withContext(Dispatchers.IO) {
            if (includeSystem) InstalledApps.allApps(context.packageManager)
            else InstalledApps.userApps(context.packageManager)
        }
    }
    val filtered = remember(query, all) {
        if (query.isBlank()) all
        else all.filter { it.label.contains(query, true) || it.packageName.contains(query, true) }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        title = { Text("Pick an app") },
        text = {
            Column {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = { Text("Search ${all.size} apps") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                LazyColumn(Modifier.fillMaxWidth().heightIn(max = 420.dp).padding(top = 8.dp)) {
                    items(filtered) { app ->
                        Column(
                            Modifier.fillMaxWidth().clickable { onPick(app) }.padding(vertical = 10.dp)
                        ) {
                            Text(app.label, style = MaterialTheme.typography.bodyLarge)
                            Text(app.packageName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    )
}
