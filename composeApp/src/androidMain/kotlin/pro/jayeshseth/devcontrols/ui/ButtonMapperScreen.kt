package pro.jayeshseth.devcontrols.ui

import android.content.Intent
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import pro.jayeshseth.devcontrols.data.ButtonAction
import pro.jayeshseth.devcontrols.data.ButtonMapperPrefs
import pro.jayeshseth.devcontrols.data.TriggerCondition
import pro.jayeshseth.devcontrols.data.TriggerConfig
import pro.jayeshseth.devcontrols.kiosk.KioskAccessibilityService

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ButtonMapperScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs = remember { ButtonMapperPrefs.get(context) }

    // State flows
    val powerDouble by prefs.powerDoubleFlow.collectAsState(initial = TriggerConfig())
    val powerTriple by prefs.powerTripleFlow.collectAsState(initial = TriggerConfig())

    val volUpDouble by prefs.volUpDoubleFlow.collectAsState(initial = TriggerConfig())
    val volUpLong by prefs.volUpLongFlow.collectAsState(initial = TriggerConfig())
    val volDownDouble by prefs.volDownDoubleFlow.collectAsState(initial = TriggerConfig())
    val volDownLong by prefs.volDownLongFlow.collectAsState(initial = TriggerConfig())
    val volChord by prefs.volChordFlow.collectAsState(initial = TriggerConfig())

    val headsetSingle by prefs.headsetSingleFlow.collectAsState(initial = TriggerConfig())
    val headsetDouble by prefs.headsetDoubleFlow.collectAsState(initial = TriggerConfig())
    val headsetLong by prefs.headsetLongFlow.collectAsState(initial = TriggerConfig())

    val assistKey by prefs.assistKeyFlow.collectAsState(initial = TriggerConfig())
    val cameraKey by prefs.cameraKeyFlow.collectAsState(initial = TriggerConfig())

    val hapticEnabled by prefs.hapticFeedbackFlow.collectAsState(initial = true)
    val a11yGranted = KioskAccessibilityService.isRunning()

    // App Picker state
    var showAppPicker by remember { mutableStateOf(false) }
    var activeAppPickerTargetKey by remember { mutableStateOf<String?>(null) }

    // Expandable card states
    var powerExpanded by remember { mutableStateOf(true) }
    var volumeExpanded by remember { mutableStateOf(true) }
    var headsetExpanded by remember { mutableStateOf(false) }
    var specialExpanded by remember { mutableStateOf(false) }

    if (showAppPicker) {
        AppPickerSheet(
            onDismissRequest = {
                showAppPicker = false
                activeAppPickerTargetKey = null
            },
            onAppSelected = { pkg, label ->
                val targetKey = activeAppPickerTargetKey
                if (targetKey != null) {
                    val current = when (targetKey) {
                        ButtonMapperPrefs.Keys.POWER_DOUBLE -> powerDouble
                        ButtonMapperPrefs.Keys.POWER_TRIPLE -> powerTriple
                        ButtonMapperPrefs.Keys.VOL_UP_DOUBLE -> volUpDouble
                        ButtonMapperPrefs.Keys.VOL_UP_LONG -> volUpLong
                        ButtonMapperPrefs.Keys.VOL_DOWN_DOUBLE -> volDownDouble
                        ButtonMapperPrefs.Keys.VOL_DOWN_LONG -> volDownLong
                        ButtonMapperPrefs.Keys.VOL_CHORD -> volChord
                        ButtonMapperPrefs.Keys.HEADSET_SINGLE -> headsetSingle
                        ButtonMapperPrefs.Keys.HEADSET_DOUBLE -> headsetDouble
                        ButtonMapperPrefs.Keys.HEADSET_LONG -> headsetLong
                        ButtonMapperPrefs.Keys.ASSIST_KEY -> assistKey
                        ButtonMapperPrefs.Keys.CAMERA_KEY -> cameraKey
                        else -> TriggerConfig()
                    }
                    scope.launch {
                        prefs.updateTrigger(
                            targetKey,
                            current.copy(
                                action = ButtonAction.LAUNCH_APP,
                                extraPackage = pkg,
                                extraLabel = label
                            )
                        )
                    }
                }
                showAppPicker = false
                activeAppPickerTargetKey = null
            }
        )
    }

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

            // Info & Service Status Card
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Hardware Key Actions",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Map hardware power, volume rocker, headset, and special keys to accessibility actions, system toggles, or applications. Configure condition filters to trigger only when locked, unlocked, or always.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Haptic Vibration Feedback",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                "Vibrate device briefly on successful gesture trigger",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = hapticEnabled,
                            onCheckedChange = { scope.launch { prefs.setHapticFeedback(it) } }
                        )
                    }

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
                                    "Accessibility Service Required",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    "To intercept button events in the background, you must enable the Dev Controls Kiosk accessibility service.",
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
                                    Text("Open Accessibility Settings")
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // 1. Power Button Card
            ExpandableCategoryCard(
                icon = "⏻",
                title = "Power Button",
                subtitle = "Hybrid detection (rapid clicks & screen transitions)",
                expanded = powerExpanded,
                onToggleExpand = { powerExpanded = !powerExpanded }
            ) {
                TriggerItemRow(
                    gestureName = "Double-Press",
                    gestureHint = "Click power button 2 times rapidly (~500ms)",
                    config = powerDouble,
                    onUpdateConfig = { scope.launch { prefs.updateTrigger(ButtonMapperPrefs.Keys.POWER_DOUBLE, it) } },
                    onRequestAppPicker = {
                        activeAppPickerTargetKey = ButtonMapperPrefs.Keys.POWER_DOUBLE
                        showAppPicker = true
                    }
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                TriggerItemRow(
                    gestureName = "Triple-Press",
                    gestureHint = "Click power button 3 times rapidly",
                    config = powerTriple,
                    onUpdateConfig = { scope.launch { prefs.updateTrigger(ButtonMapperPrefs.Keys.POWER_TRIPLE, it) } },
                    onRequestAppPicker = {
                        activeAppPickerTargetKey = ButtonMapperPrefs.Keys.POWER_TRIPLE
                        showAppPicker = true
                    }
                )
            }

            Spacer(Modifier.height(16.dp))

            // 2. Volume Buttons & Chord Card
            ExpandableCategoryCard(
                icon = "🔊",
                title = "Volume Buttons & Chords",
                subtitle = "Hardware rocker double-taps, long-presses, and chords",
                expanded = volumeExpanded,
                onToggleExpand = { volumeExpanded = !volumeExpanded }
            ) {
                TriggerItemRow(
                    gestureName = "Volume Up ➕ (Long-Press)",
                    gestureHint = "Hold Volume Up for 500ms",
                    config = volUpLong,
                    onUpdateConfig = { scope.launch { prefs.updateTrigger(ButtonMapperPrefs.Keys.VOL_UP_LONG, it) } },
                    onRequestAppPicker = {
                        activeAppPickerTargetKey = ButtonMapperPrefs.Keys.VOL_UP_LONG
                        showAppPicker = true
                    }
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                TriggerItemRow(
                    gestureName = "Volume Up ➕ (Double-Press)",
                    gestureHint = "Double-tap Volume Up key",
                    config = volUpDouble,
                    onUpdateConfig = { scope.launch { prefs.updateTrigger(ButtonMapperPrefs.Keys.VOL_UP_DOUBLE, it) } },
                    onRequestAppPicker = {
                        activeAppPickerTargetKey = ButtonMapperPrefs.Keys.VOL_UP_DOUBLE
                        showAppPicker = true
                    }
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                TriggerItemRow(
                    gestureName = "Volume Down ➖ (Long-Press)",
                    gestureHint = "Hold Volume Down for 500ms",
                    config = volDownLong,
                    onUpdateConfig = { scope.launch { prefs.updateTrigger(ButtonMapperPrefs.Keys.VOL_DOWN_LONG, it) } },
                    onRequestAppPicker = {
                        activeAppPickerTargetKey = ButtonMapperPrefs.Keys.VOL_DOWN_LONG
                        showAppPicker = true
                    }
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                TriggerItemRow(
                    gestureName = "Volume Down ➖ (Double-Press)",
                    gestureHint = "Double-tap Volume Down key",
                    config = volDownDouble,
                    onUpdateConfig = { scope.launch { prefs.updateTrigger(ButtonMapperPrefs.Keys.VOL_DOWN_DOUBLE, it) } },
                    onRequestAppPicker = {
                        activeAppPickerTargetKey = ButtonMapperPrefs.Keys.VOL_DOWN_DOUBLE
                        showAppPicker = true
                    }
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                TriggerItemRow(
                    gestureName = "Volume Chord (Vol Up + Vol Down)",
                    gestureHint = "Press Volume Up and Volume Down at the same time",
                    config = volChord,
                    onUpdateConfig = { scope.launch { prefs.updateTrigger(ButtonMapperPrefs.Keys.VOL_CHORD, it) } },
                    onRequestAppPicker = {
                        activeAppPickerTargetKey = ButtonMapperPrefs.Keys.VOL_CHORD
                        showAppPicker = true
                    }
                )
            }

            Spacer(Modifier.height(16.dp))

            // 3. Headset / Media Buttons Card
            ExpandableCategoryCard(
                icon = "🎧",
                title = "Headset & Media Buttons",
                subtitle = "Wired & Bluetooth media buttons (Hook / Play-Pause)",
                expanded = headsetExpanded,
                onToggleExpand = { headsetExpanded = !headsetExpanded }
            ) {
                TriggerItemRow(
                    gestureName = "Single-Press",
                    gestureHint = "Click headset / media button once",
                    config = headsetSingle,
                    onUpdateConfig = { scope.launch { prefs.updateTrigger(ButtonMapperPrefs.Keys.HEADSET_SINGLE, it) } },
                    onRequestAppPicker = {
                        activeAppPickerTargetKey = ButtonMapperPrefs.Keys.HEADSET_SINGLE
                        showAppPicker = true
                    }
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                TriggerItemRow(
                    gestureName = "Double-Press",
                    gestureHint = "Double-click headset / media button",
                    config = headsetDouble,
                    onUpdateConfig = { scope.launch { prefs.updateTrigger(ButtonMapperPrefs.Keys.HEADSET_DOUBLE, it) } },
                    onRequestAppPicker = {
                        activeAppPickerTargetKey = ButtonMapperPrefs.Keys.HEADSET_DOUBLE
                        showAppPicker = true
                    }
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                TriggerItemRow(
                    gestureName = "Long-Press",
                    gestureHint = "Hold headset / media button for 500ms",
                    config = headsetLong,
                    onUpdateConfig = { scope.launch { prefs.updateTrigger(ButtonMapperPrefs.Keys.HEADSET_LONG, it) } },
                    onRequestAppPicker = {
                        activeAppPickerTargetKey = ButtonMapperPrefs.Keys.HEADSET_LONG
                        showAppPicker = true
                    }
                )
            }

            Spacer(Modifier.height(16.dp))

            // 4. Special Hardware Keys Card
            ExpandableCategoryCard(
                icon = "⭐",
                title = "Special Hardware Keys",
                subtitle = "Dedicated Assistant & Camera shutter buttons",
                expanded = specialExpanded,
                onToggleExpand = { specialExpanded = !specialExpanded }
            ) {
                TriggerItemRow(
                    gestureName = "Assistant Key (Assist / Voice)",
                    gestureHint = "Dedicated hardware Google/AI Assistant key",
                    config = assistKey,
                    onUpdateConfig = { scope.launch { prefs.updateTrigger(ButtonMapperPrefs.Keys.ASSIST_KEY, it) } },
                    onRequestAppPicker = {
                        activeAppPickerTargetKey = ButtonMapperPrefs.Keys.ASSIST_KEY
                        showAppPicker = true
                    }
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                TriggerItemRow(
                    gestureName = "Camera Shutter Key",
                    gestureHint = "Dedicated hardware camera button",
                    config = cameraKey,
                    onUpdateConfig = { scope.launch { prefs.updateTrigger(ButtonMapperPrefs.Keys.CAMERA_KEY, it) } },
                    onRequestAppPicker = {
                        activeAppPickerTargetKey = ButtonMapperPrefs.Keys.CAMERA_KEY
                        showAppPicker = true
                    }
                )
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun ExpandableCategoryCard(
    icon: String,
    title: String,
    subtitle: String,
    expanded: Boolean,
    onToggleExpand: () -> Unit,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggleExpand() },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(icon, fontSize = 22.sp, modifier = Modifier.padding(end = 12.dp))
                    Column {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Text(
                    text = if (expanded) "▲" else "▼",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }

            AnimatedVisibility(
                visible = expanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(modifier = Modifier.padding(top = 16.dp)) {
                    content()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TriggerItemRow(
    gestureName: String,
    gestureHint: String,
    config: TriggerConfig,
    onUpdateConfig: (TriggerConfig) -> Unit,
    onRequestAppPicker: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = gestureName,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = gestureHint,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(8.dp))

        // Condition Filter Chips
        Text(
            text = "Trigger Condition:",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TriggerCondition.entries.forEach { cond ->
                FilterChip(
                    selected = config.condition == cond,
                    onClick = { onUpdateConfig(config.copy(condition = cond)) },
                    label = { Text(cond.label, fontSize = 12.sp) }
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        // Action Dropdown Selector
        ActionSelector(
            config = config,
            onSelectAction = { action ->
                if (action == ButtonAction.LAUNCH_APP) {
                    onRequestAppPicker()
                } else {
                    onUpdateConfig(
                        config.copy(
                            action = action,
                            extraPackage = null,
                            extraLabel = null
                        )
                    )
                }
            },
            onChangeCustomApp = onRequestAppPicker
        )
    }
}

@Composable
private fun ActionSelector(
    config: TriggerConfig,
    onSelectAction: (ButtonAction) -> Unit,
    onChangeCustomApp: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    val displayLabel = if (config.action == ButtonAction.LAUNCH_APP && !config.extraLabel.isNullOrBlank()) {
        "${config.action.icon} Launch: ${config.extraLabel}"
    } else {
        "${config.action.icon} ${config.action.label}"
    }

    Column(modifier = Modifier.fillMaxWidth()) {
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
                    Text(
                        text = displayLabel,
                        fontSize = 14.sp,
                        maxLines = 1,
                        modifier = Modifier.weight(1f)
                    )
                    Text("▼", fontSize = 10.sp, modifier = Modifier.padding(start = 8.dp))
                }
            }

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.fillMaxWidth(0.9f)
            ) {
                ButtonAction.entries.forEach { action ->
                    DropdownMenuItem(
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(action.icon, modifier = Modifier.padding(end = 8.dp))
                                Text(action.label)
                            }
                        },
                        onClick = {
                            onSelectAction(action)
                            expanded = false
                        }
                    )
                }
            }
        }

        if (config.action == ButtonAction.LAUNCH_APP) {
            Spacer(Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onChangeCustomApp) {
                    Text("Change App…", fontSize = 12.sp)
                }
            }
        }
    }
}
