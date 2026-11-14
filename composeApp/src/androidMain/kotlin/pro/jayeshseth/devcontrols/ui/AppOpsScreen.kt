package pro.jayeshseth.devcontrols.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import pro.jayeshseth.devcontrols.adb.AdbClient
import pro.jayeshseth.devcontrols.ui.adbtools.AppPickerDialog
import java.util.Locale

data class AppOpDefinition(
    val opName: String,
    val label: String,
    val description: String,
    val category: String
)

enum class AppOpMode(val commandValue: String, val label: String, val color: Color) {
    ALLOW("allow", "allow", Color(0xFF2E7D32)),
    IGNORE("ignore", "ignore", Color(0xFFF57C00)),
    DENY("deny", "deny", Color(0xFFC62828)),
    DEFAULT("default", "default", Color(0xFF757575))
}

private val KNOWN_APP_OPS = listOf(
    AppOpDefinition("SYSTEM_ALERT_WINDOW", "Draw Over Other Apps", "Allows displaying windows above other applications (floating overlays).", "Special Access"),
    AppOpDefinition("WRITE_SETTINGS", "Modify System Settings", "Allows modifying system settings such as ringtones, screen timeout, etc.", "Special Access"),
    AppOpDefinition("RUN_IN_BACKGROUND", "Background Execution", "Allows running code while app is in the background without battery restrictions.", "Background"),
    AppOpDefinition("RUN_ANY_IN_BACKGROUND", "Run Any in Background", "Allows background tasks, alarms, and job execution regardless of power saving.", "Background"),
    AppOpDefinition("READ_CLIPBOARD", "Read Clipboard", "Allows reading the clipboard even when the app is not in the foreground.", "Privacy"),
    AppOpDefinition("TOAST_WINDOW", "Toast Windows", "Controls ability to pop up toast banners on screen.", "UI"),
    AppOpDefinition("PROJECT_MEDIA", "Project Media / Screen Capture", "Allows recording the screen or projecting audio/video without user prompt.", "Privacy"),
    AppOpDefinition("GET_USAGE_STATS", "Usage Access", "Inspects other running apps and user app-usage statistics.", "Special Access"),
    AppOpDefinition("MANAGE_EXTERNAL_STORAGE", "All Files Access", "Broad access to entire external shared storage.", "Storage"),
    AppOpDefinition("ACCESS_RESTRICTED_SETTINGS", "Restricted Settings", "Overrides Android 13+ restricted settings for accessibility and notification listeners.", "Special Access"),
    AppOpDefinition("FINE_LOCATION", "Precise Location", "Access to GNSS/GPS precise coordinates.", "Location"),
    AppOpDefinition("COARSE_LOCATION", "Approximate Location", "Access to cell tower/Wi-Fi coarse location.", "Location"),
    AppOpDefinition("POST_NOTIFICATION", "Post Notifications", "Permission to show system notifications.", "Notifications"),
    AppOpDefinition("PICTURE_IN_PICTURE", "Picture in Picture", "Allows entering PiP mode when transitioning to background.", "UI"),
    AppOpDefinition("START_FOREGROUND", "Start Foreground Service", "Allows promoting background service to foreground with ongoing notification.", "Background"),
    AppOpDefinition("WAKE_LOCK", "Wake Lock", "Allows acquiring CPU wake locks to prevent device sleep.", "Power"),
    AppOpDefinition("RECORD_AUDIO", "Microphone Access", "Live audio recording from hardware microphones.", "Hardware"),
    AppOpDefinition("CAMERA", "Camera Access", "Hardware camera sensor access.", "Hardware"),
    AppOpDefinition("SCHEDULE_EXACT_ALARM", "Exact Alarms", "Allows scheduling exact alarm clocks and time-sensitive reminders.", "Power")
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AppOpsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val adbClient = remember { AdbClient.get(context) }
    val adbState by adbClient.state.collectAsState()

    var targetPackage by remember { mutableStateOf(context.packageName) }
    var showAppPicker by remember { mutableStateOf(false) }

    var isLoading by remember { mutableStateOf(false) }
    var rawOutput by remember { mutableStateOf("") }
    var statusMessage by remember { mutableStateOf("") }
    var isErrorStatus by remember { mutableStateOf(false) }
    var showResetDialog by remember { mutableStateOf(false) }
    var showRawOutput by remember { mutableStateOf(false) }

    // Map of OpName -> current mode string (e.g. "allow", "ignore", "deny", "default")
    val opModesMap = remember { mutableStateMapOf<String, String>() }

    // Custom Op state
    var customOpName by remember { mutableStateOf("") }
    var customOpMode by remember { mutableStateOf(AppOpMode.ALLOW) }

    // Op filter
    var selectedCategory by remember { mutableStateOf("All") }
    var opSearchQuery by remember { mutableStateOf("") }

    val categories = remember {
        listOf("All") + KNOWN_APP_OPS.map { it.category }.distinct()
    }

    fun parseAppOpsOutput(output: String) {
        opModesMap.clear()
        // Format of `cmd appops get <pkg>`:
        // OpName: mode; time=+1d...
        // e.g.:
        // SYSTEM_ALERT_WINDOW: allow; time=+1d3h...
        // WAKE_LOCK: allow; time=+2d...
        // COARSE_LOCATION: ignore; time=+1d...
        val lines = output.lines()
        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.contains(":")) {
                val parts = trimmed.split(":", limit = 2)
                val op = parts[0].trim().uppercase(Locale.US)
                val rest = parts[1].trim()
                val mode = rest.split(";").firstOrNull()?.trim()?.lowercase(Locale.US) ?: "default"
                opModesMap[op] = mode
            }
        }
    }

    fun loadAppOps() {
        if (targetPackage.isBlank()) return
        scope.launch {
            isLoading = true
            statusMessage = "Querying appops for '$targetPackage'..."
            isErrorStatus = false
            withContext(Dispatchers.IO) {
                val cmd = "cmd appops get $targetPackage"
                val result = adbClient.exec(cmd)
                rawOutput = result.output
                if (result.ok) {
                    if (result.output.contains("No operations", ignoreCase = true) || result.output.isBlank()) {
                        statusMessage = "No custom AppOps set for '$targetPackage' (all default)."
                    } else {
                        parseAppOpsOutput(result.output)
                        statusMessage = "Loaded ${opModesMap.size} configured AppOps."
                    }
                    isErrorStatus = false
                } else {
                    statusMessage = "Query failed: ${result.error ?: "Ensure ADB is connected"}"
                    isErrorStatus = true
                }
                isLoading = false
            }
        }
    }

    fun setAppOp(op: String, mode: AppOpMode) {
        scope.launch {
            statusMessage = "Setting $op to ${mode.commandValue}..."
            isErrorStatus = false
            withContext(Dispatchers.IO) {
                val cmd = "cmd appops set $targetPackage $op ${mode.commandValue}"
                val result = adbClient.exec(cmd)
                if (result.ok) {
                    opModesMap[op.uppercase(Locale.US)] = mode.commandValue
                    statusMessage = "Successfully set $op -> ${mode.commandValue}"
                    isErrorStatus = false
                } else {
                    statusMessage = "Failed to set $op: ${result.error ?: result.output}"
                    isErrorStatus = true
                }
            }
        }
    }

    fun resetAllAppOps() {
        scope.launch {
            statusMessage = "Resetting all AppOps for '$targetPackage'..."
            isErrorStatus = false
            withContext(Dispatchers.IO) {
                val cmd = "cmd appops reset $targetPackage"
                val result = adbClient.exec(cmd)
                if (result.ok) {
                    opModesMap.clear()
                    statusMessage = "All AppOps reset to default for '$targetPackage'."
                    isErrorStatus = false
                    loadAppOps()
                } else {
                    statusMessage = "Reset failed: ${result.error ?: result.output}"
                    isErrorStatus = true
                }
            }
        }
    }

    fun copyToClipboard(label: String, content: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText(label, content))
        Toast.makeText(context, "$label copied", Toast.LENGTH_SHORT).show()
    }

    // Auto-load once target package is set
    LaunchedEffect(targetPackage) {
        loadAppOps()
    }

    if (showAppPicker) {
        AppPickerDialog(
            includeSystem = true,
            onPick = { app ->
                targetPackage = app.packageName
                showAppPicker = false
            },
            onDismiss = { showAppPicker = false }
        )
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Reset All AppOps?") },
            text = { Text("This will execute 'cmd appops reset $targetPackage' to revert all operation overrides back to system defaults.") },
            confirmButton = {
                Button(
                    onClick = {
                        showResetDialog = false
                        resetAllAppOps()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Reset")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    val filteredOps = remember(selectedCategory, opSearchQuery, opModesMap.size) {
        KNOWN_APP_OPS.filter { item ->
            val matchesCategory = selectedCategory == "All" || item.category == selectedCategory
            val matchesSearch = opSearchQuery.isBlank() ||
                    item.opName.contains(opSearchQuery, ignoreCase = true) ||
                    item.label.contains(opSearchQuery, ignoreCase = true) ||
                    item.description.contains(opSearchQuery, ignoreCase = true)
            matchesCategory && matchesSearch
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("AppOps Console", fontWeight = FontWeight.Bold)
                        Text(
                            "Hidden System & Runtime AppOps Inspector",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    TextButton(onClick = onBack) {
                        Text("Back")
                    }
                },
                actions = {
                    IconButton(onClick = { loadAppOps() }, enabled = !isLoading) {
                        Text("🔄", fontSize = 16.sp)
                    }
                    IconButton(onClick = { showResetDialog = true }) {
                        Text("⚠️", fontSize = 16.sp)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Target Package Header Card
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(0.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Target Package:", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)

                        // ADB status pill
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (adbState == AdbClient.State.Connected) Color(0xFF2E7D32).copy(alpha = 0.2f) else Color(0xFFC62828).copy(alpha = 0.2f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                if (adbState == AdbClient.State.Connected) "ADB Active" else "ADB: $adbState",
                                fontSize = 11.sp,
                                color = if (adbState == AdbClient.State.Connected) Color(0xFF2E7D32) else Color(0xFFC62828),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = targetPackage,
                            onValueChange = { targetPackage = it },
                            placeholder = { Text("package.name", fontSize = 12.sp) },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { showAppPicker = true }) {
                            Text("📱", fontSize = 20.sp)
                        }
                    }

                    // Presets chips
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        SuggestionChip(
                            onClick = { targetPackage = context.packageName },
                            label = { Text("Self App", fontSize = 11.sp) }
                        )
                        SuggestionChip(
                            onClick = { targetPackage = "com.google.android.gms" },
                            label = { Text("GMS Play Services", fontSize = 11.sp) }
                        )
                        SuggestionChip(
                            onClick = { targetPackage = "com.android.systemui" },
                            label = { Text("SystemUI", fontSize = 11.sp) }
                        )
                    }

                    // Status banner
                    if (statusMessage.isNotBlank()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isErrorStatus) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer)
                                .padding(8.dp)
                        ) {
                            Text(
                                statusMessage,
                                color = if (isErrorStatus) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimaryContainer,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }

            // Categories & Search Filter
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = opSearchQuery,
                    onValueChange = { opSearchQuery = it },
                    placeholder = { Text("Search AppOp...", fontSize = 12.sp) },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )

                OutlinedButton(
                    onClick = { showRawOutput = !showRawOutput },
                    modifier = Modifier.height(50.dp)
                ) {
                    Text(if (showRawOutput) "Hide Raw" else "Raw Dump", fontSize = 11.sp)
                }
            }

            // Category Chips Row
            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                categories.forEach { cat ->
                    FilterChip(
                        selected = selectedCategory == cat,
                        onClick = { selectedCategory = cat },
                        label = { Text(cat, fontSize = 11.sp) },
                        modifier = Modifier.height(28.dp)
                    )
                }
            }

            // Raw Output expansion
            AnimatedVisibility(visible = showRawOutput) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("cmd appops get $targetPackage", color = Color.LightGray, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                            IconButton(onClick = { copyToClipboard("AppOps Output", rawOutput) }) {
                                Text("📋", fontSize = 14.sp)
                            }
                        }
                        Text(
                            rawOutput.ifBlank { "(empty output)" },
                            color = Color(0xFFE0E0E0),
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            maxLines = 8,
                            lineHeight = 14.sp
                        )
                    }
                }
            }

            // Ops List
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Custom Op Injector Card
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = CardDefaults.outlinedCardBorder(),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Execute Custom AppOp", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = customOpName,
                                    onValueChange = { customOpName = it },
                                    placeholder = { Text("e.g. VIBRATE or 26", fontSize = 12.sp) },
                                    singleLine = true,
                                    modifier = Modifier.weight(1.2f)
                                )

                                FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    AppOpMode.entries.forEach { m ->
                                        FilterChip(
                                            selected = customOpMode == m,
                                            onClick = { customOpMode = m },
                                            label = { Text(m.label, fontSize = 10.sp) },
                                            modifier = Modifier.height(28.dp)
                                        )
                                    }
                                }

                                Button(
                                    onClick = {
                                        if (customOpName.isNotBlank()) {
                                            setAppOp(customOpName.trim(), customOpMode)
                                        }
                                    },
                                    enabled = customOpName.isNotBlank()
                                ) {
                                    Text("Set", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }

                // Known Ops
                items(filteredOps, key = { it.opName }) { opDef ->
                    val currentMode = opModesMap[opDef.opName] ?: "default"
                    AppOpItemCard(
                        def = opDef,
                        currentMode = currentMode,
                        onModeSelected = { mode -> setAppOp(opDef.opName, mode) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AppOpItemCard(
    def: AppOpDefinition,
    currentMode: String,
    onModeSelected: (AppOpMode) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = CardDefaults.outlinedCardBorder(),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(def.label, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text(
                        def.opName,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                // Current mode pill
                val pillColor = when (currentMode.lowercase()) {
                    "allow" -> Color(0xFF2E7D32)
                    "ignore" -> Color(0xFFF57C00)
                    "deny" -> Color(0xFFC62828)
                    else -> Color(0xFF757575)
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(pillColor.copy(alpha = 0.15f))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        currentMode.uppercase(),
                        color = pillColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                }
            }

            Text(
                def.description,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Mode Toggle Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AppOpMode.entries.forEach { mode ->
                    val isSelected = currentMode.equals(mode.commandValue, ignoreCase = true)
                    FilterChip(
                        selected = isSelected,
                        onClick = { onModeSelected(mode) },
                        label = {
                            Text(
                                mode.label,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) mode.color else MaterialTheme.colorScheme.onSurface
                            )
                        },
                        modifier = Modifier.height(30.dp)
                    )
                }
            }
        }
    }
}
