package pro.jayeshseth.devcontrols.ui

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import pro.jayeshseth.devcontrols.data.ButtonAction
import pro.jayeshseth.devcontrols.data.ButtonMapperPrefs
import pro.jayeshseth.devcontrols.kiosk.KioskAccessibilityService

/** Screen mapping custom actions to system hardware volume buttons on lock screen vs unlocked state. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ButtonMapperScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs = remember { ButtonMapperPrefs.get(context) }

    val volUpLocked by prefs.volUpLockedAction.collectAsState(initial = ButtonAction.DEFAULT)
    val volUpUnlocked by prefs.volUpUnlockedAction.collectAsState(initial = ButtonAction.DEFAULT)
    val volDownLocked by prefs.volDownLockedAction.collectAsState(initial = ButtonAction.DEFAULT)
    val volDownUnlocked by prefs.volDownUnlockedAction.collectAsState(initial = ButtonAction.DEFAULT)

    val a11yGranted = KioskAccessibilityService.isRunning()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("System Button Mapper") },
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
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(Modifier.height(8.dp))

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Configure hardware key actions", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Long-press volume buttons to trigger custom actions. " +
                                "Tip: configuring a \"Locked\" action automatically applies when Unlocked too — " +
                                "unless you separately pick a different Unlocked action. " +
                                "Screen-completely-off is a system limitation; the service only receives key events when the screen is on.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (!a11yGranted) {
                        Spacer(Modifier.height(12.dp))
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    "Accessibility permission required",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    "To intercept button events in the background, you must enable the " +
                                            "Dev Controls Kiosk accessibility service.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                                Spacer(Modifier.height(8.dp))
                                OutlinedButton(onClick = {
                                    runCatching {
                                        context.startActivity(
                                            Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        )
                                    }
                                }) {
                                    Text("Open Accessibility settings")
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Volume Up Key Options Card
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Volume Up Button 🔊➕ (Long-Press)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(12.dp))

                    Text("When Screen is Locked / Off:", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(4.dp))
                    ActionSelector(selected = volUpLocked) { action ->
                        scope.launch { prefs.setVolUpLockedAction(action) }
                    }

                    Spacer(Modifier.height(16.dp))

                    Text("When Device is Unlocked:", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(4.dp))
                    ActionSelector(selected = volUpUnlocked) { action ->
                        scope.launch { prefs.setVolUpUnlockedAction(action) }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Volume Down Key Options Card
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Volume Down Button 🔊➖ (Long-Press)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(12.dp))

                    Text("When Screen is Locked / Off:", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(4.dp))
                    ActionSelector(selected = volDownLocked) { action ->
                        scope.launch { prefs.setVolDownLockedAction(action) }
                    }

                    Spacer(Modifier.height(16.dp))

                    Text("When Device is Unlocked:", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(4.dp))
                    ActionSelector(selected = volDownUnlocked) { action ->
                        scope.launch { prefs.setVolDownUnlockedAction(action) }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun ActionSelector(
    selected: ButtonAction,
    onSelect: (ButtonAction) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxWidth()) {
        OutlinedButton(
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("${selected.icon} ${selected.label}", fontSize = 15.sp)
                Text("▼", fontSize = 10.sp)
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.fillMaxWidth(0.9f)
        ) {
            ButtonAction.values().forEach { action ->
                DropdownMenuItem(
                    text = { Text("${action.icon} ${action.label}") },
                    onClick = {
                        onSelect(action)
                        expanded = false
                    }
                )
            }
        }
    }
}
