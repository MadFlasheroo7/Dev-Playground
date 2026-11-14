package pro.jayeshseth.devcontrols.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.ComponentInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.net.Uri
import android.os.Build
import android.os.Process
import android.os.SystemClock
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.runtime.mutableStateListOf
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
import pro.jayeshseth.devcontrols.adb.PairingService
import pro.jayeshseth.devcontrols.data.AdbConsolePrefs
import pro.jayeshseth.devcontrols.ui.adbtools.AppPickerDialog
import pro.jayeshseth.devcontrols.util.AppEntry
import java.util.UUID

enum class IntentTargetType(val label: String, val amCommand: String) {
    ACTIVITY("Activity", "am start"),
    BROADCAST("Broadcast", "am broadcast"),
    SERVICE("Service", "am startservice")
}

enum class ExtraDataType(val label: String, val cliFlag: String) {
    STRING("String", "--es"),
    INT("Int", "--ei"),
    BOOLEAN("Boolean", "--ez"),
    URI("Uri", "--eu")
}

data class ExtraItem(
    val id: String = UUID.randomUUID().toString(),
    val key: String = "",
    val value: String = "",
    val type: ExtraDataType = ExtraDataType.STRING
)

data class FlagItem(
    val name: String,
    val value: Int
)

data class ComponentDetail(
    val className: String,
    val simpleName: String,
    val exported: Boolean,
    val permission: String?
)

data class DispatchResult(
    val mode: String,
    val success: Boolean,
    val durationMs: Long,
    val commandOrDesc: String,
    val output: String,
    val isSecurityException: Boolean = false
)

private val COMMON_FLAGS = listOf(
    FlagItem("NEW_TASK", Intent.FLAG_ACTIVITY_NEW_TASK),
    FlagItem("CLEAR_TOP", Intent.FLAG_ACTIVITY_CLEAR_TOP),
    FlagItem("SINGLE_TOP", Intent.FLAG_ACTIVITY_SINGLE_TOP),
    FlagItem("GRANT_READ_URI", Intent.FLAG_GRANT_READ_URI_PERMISSION),
    FlagItem("GRANT_WRITE_URI", Intent.FLAG_GRANT_WRITE_URI_PERMISSION),
    FlagItem("INCLUDE_STOPPED", Intent.FLAG_INCLUDE_STOPPED_PACKAGES),
    FlagItem("EXCLUDE_STOPPED", Intent.FLAG_EXCLUDE_STOPPED_PACKAGES)
)

private val ACTION_PRESETS_ACTIVITY = listOf(
    Intent.ACTION_VIEW,
    Intent.ACTION_MAIN,
    Intent.ACTION_SEND,
    Intent.ACTION_DIAL,
    "android.settings.SETTINGS",
    "android.settings.WIRELESS_SETTINGS",
    "android.settings.APPLICATION_DEVELOPMENT_SETTINGS"
)

private val ACTION_PRESETS_BROADCAST = listOf(
    Intent.ACTION_BOOT_COMPLETED,
    Intent.ACTION_AIRPLANE_MODE_CHANGED,
    Intent.ACTION_BATTERY_LOW,
    "android.provider.Telephony.SECRET_CODE",
    Intent.ACTION_POWER_CONNECTED,
    Intent.ACTION_POWER_DISCONNECTED
)

private val CATEGORY_PRESETS = listOf(
    Intent.CATEGORY_DEFAULT,
    Intent.CATEGORY_BROWSABLE,
    Intent.CATEGORY_LAUNCHER,
    Intent.CATEGORY_HOME,
    Intent.CATEGORY_PREFERENCE
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun IntentForgeScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val client = remember { AdbClient.get(context) }
    val prefs = remember { AdbConsolePrefs.get(context) }

    val adbState by client.state.collectAsState()
    val paired by prefs.paired.collectAsState(initial = false)

    // Configuration states
    var targetType by remember { mutableStateOf(IntentTargetType.ACTIVITY) }
    var action by remember { mutableStateOf(Intent.ACTION_VIEW) }
    var dataUri by remember { mutableStateOf("") }
    var mimeType by remember { mutableStateOf("") }
    var selectedCategories = remember { mutableStateListOf<String>() }
    var selectedFlags by remember { mutableStateOf(Intent.FLAG_ACTIVITY_NEW_TASK) }
    val extras = remember { mutableStateListOf<ExtraItem>() }

    var targetPackage by remember { mutableStateOf("") }
    var targetClass by remember { mutableStateOf("") }
    var pickedAppLabel by remember { mutableStateOf<String?>(null) }
    var showAppPicker by remember { mutableStateOf(false) }

    // Inspect components of selected package
    var packageComponents by remember { mutableStateOf<List<ComponentDetail>>(emptyList()) }
    var showComponentMenu by remember { mutableStateOf(false) }

    var lastResult by remember { mutableStateOf<DispatchResult?>(null) }
    var isExecuting by remember { mutableStateOf(false) }

    // Auto-connect ADB if paired and keys present
    LaunchedEffect(paired) {
        if (paired && adbState == AdbClient.State.Disconnected && client.hasStoredKeys()) {
            client.connect()
        }
    }

    // Load components when target package or target type changes
    LaunchedEffect(targetPackage, targetType) {
        if (targetPackage.isBlank()) {
            packageComponents = emptyList()
            return@LaunchedEffect
        }
        packageComponents = withContext(Dispatchers.IO) {
            queryComponents(context.packageManager, targetPackage, targetType)
        }
    }

    // Determine security audit badge
    val selectedComponentDetail = remember(packageComponents, targetClass) {
        packageComponents.firstOrNull { it.className == targetClass }
    }

    fun normalizeClassName(pkg: String, cls: String): String {
        val trimmed = cls.trim()
        return when {
            trimmed.isBlank() -> ""
            trimmed.startsWith(".") -> "$pkg$trimmed"
            !trimmed.contains(".") && pkg.isNotBlank() -> "$pkg.$trimmed"
            else -> trimmed
        }
    }

    fun buildAdbCommand(): String {
        return buildString {
            append(targetType.amCommand)
            if (action.isNotBlank()) append(" -a $action")
            if (dataUri.isNotBlank()) append(" -d \"$dataUri\"")
            if (mimeType.isNotBlank()) append(" -t \"$mimeType\"")
            selectedCategories.forEach { append(" -c $it") }
            if (selectedFlags != 0) append(" -f 0x${Integer.toHexString(selectedFlags)}")
            if (targetPackage.isNotBlank()) {
                val normClass = normalizeClassName(targetPackage, targetClass)
                if (normClass.isNotBlank()) {
                    append(" -n $targetPackage/$normClass")
                } else {
                    append(" -p $targetPackage")
                }
            }
            extras.filter { it.key.isNotBlank() }.forEach { extra ->
                append(" ${extra.type.cliFlag} \"${extra.key}\" \"${extra.value.replace("\"", "\\\"")}\"")
            }
        }
    }

    fun buildFrameworkIntent(): Intent {
        return Intent(action.ifBlank { null }).apply {
            if (dataUri.isNotBlank()) {
                val parsed = Uri.parse(dataUri)
                if (mimeType.isNotBlank()) {
                    setDataAndType(parsed, mimeType)
                } else {
                    data = parsed
                }
            } else if (mimeType.isNotBlank()) {
                type = mimeType
            }
            selectedCategories.forEach { addCategory(it) }
            if (selectedFlags != 0) {
                flags = selectedFlags
            }
            if (targetPackage.isNotBlank()) {
                val normClass = normalizeClassName(targetPackage, targetClass)
                if (normClass.isNotBlank()) {
                    component = ComponentName(targetPackage, normClass)
                } else {
                    setPackage(targetPackage)
                }
            }
            extras.filter { it.key.isNotBlank() }.forEach { extra ->
                when (extra.type) {
                    ExtraDataType.STRING -> putExtra(extra.key, extra.value)
                    ExtraDataType.INT -> putExtra(extra.key, extra.value.toIntOrNull() ?: 0)
                    ExtraDataType.BOOLEAN -> putExtra(extra.key, extra.value.toBoolean())
                    ExtraDataType.URI -> putExtra(extra.key, Uri.parse(extra.value))
                }
            }
        }
    }

    fun dispatchAppUid() {
        if (isExecuting) return
        isExecuting = true
        scope.launch {
            val start = SystemClock.elapsedRealtime()
            val intentDesc = buildFrameworkIntent().toString()
            try {
                val intent = buildFrameworkIntent()
                when (targetType) {
                    IntentTargetType.ACTIVITY -> {
                        // Ensure FLAG_ACTIVITY_NEW_TASK for context start
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        context.startActivity(intent)
                        val elapsed = SystemClock.elapsedRealtime() - start
                        lastResult = DispatchResult(
                            mode = "App UID (${Process.myUid()})",
                            success = true,
                            durationMs = elapsed,
                            commandOrDesc = intentDesc,
                            output = "startActivity() dispatched successfully."
                        )
                    }
                    IntentTargetType.BROADCAST -> {
                        context.sendBroadcast(intent)
                        val elapsed = SystemClock.elapsedRealtime() - start
                        lastResult = DispatchResult(
                            mode = "App UID (${Process.myUid()})",
                            success = true,
                            durationMs = elapsed,
                            commandOrDesc = intentDesc,
                            output = "sendBroadcast() dispatched successfully."
                        )
                    }
                    IntentTargetType.SERVICE -> {
                        val comp = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            context.startForegroundService(intent)
                        } else {
                            context.startService(intent)
                        }
                        val elapsed = SystemClock.elapsedRealtime() - start
                        lastResult = DispatchResult(
                            mode = "App UID (${Process.myUid()})",
                            success = true,
                            durationMs = elapsed,
                            commandOrDesc = intentDesc,
                            output = "startService() returned: ${comp?.flattenToString() ?: "null"}"
                        )
                    }
                }
            } catch (se: SecurityException) {
                val elapsed = SystemClock.elapsedRealtime() - start
                lastResult = DispatchResult(
                    mode = "App UID (${Process.myUid()})",
                    success = false,
                    durationMs = elapsed,
                    commandOrDesc = intentDesc,
                    output = "SecurityException: ${se.message}",
                    isSecurityException = true
                )
            } catch (t: Throwable) {
                val elapsed = SystemClock.elapsedRealtime() - start
                lastResult = DispatchResult(
                    mode = "App UID (${Process.myUid()})",
                    success = false,
                    durationMs = elapsed,
                    commandOrDesc = intentDesc,
                    output = "${t.javaClass.simpleName}: ${t.message}"
                )
            } finally {
                isExecuting = false
            }
        }
    }

    fun dispatchShellUid() {
        if (isExecuting) return
        isExecuting = true
        scope.launch {
            val cmd = buildAdbCommand()
            val start = SystemClock.elapsedRealtime()
            val res = client.exec(cmd)
            val elapsed = SystemClock.elapsedRealtime() - start
            val isSecEx = res.output.contains("SecurityException", ignoreCase = true) ||
                    res.output.contains("Permission Denial", ignoreCase = true)
            val hasError = !res.ok ||
                    res.output.contains("Error", ignoreCase = true) ||
                    res.output.contains("Exception", ignoreCase = true) ||
                    isSecEx
            lastResult = DispatchResult(
                mode = "Shell UID 2000 (ADB)",
                success = !hasError,
                durationMs = elapsed,
                commandOrDesc = cmd,
                output = if (res.output.isNotBlank()) res.output.trim() else (res.error ?: "Done"),
                isSecurityException = isSecEx
            )
            isExecuting = false
        }
    }

    if (showAppPicker) {
        AppPickerDialog(
            includeSystem = true,
            onPick = { app ->
                targetPackage = app.packageName
                pickedAppLabel = app.label
                targetClass = ""
                showAppPicker = false
            },
            onDismiss = { showAppPicker = false }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Intent Forge & Dispatcher") },
                navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header / Intro
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(14.dp)) {
                    Text(
                        "Dual-Mode Intent Testing Engine",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Forge explicit or implicit intents. Test framework security boundaries in App UID mode, or dispatch through UID 2000 privileged ADB shell to bypass component exposure limits.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            AdbStatusStrip(adbState = adbState, onConnect = { scope.launch { client.connect() } })

            // Target Type Selector
            Column {
                Text("Target Dispatch Type", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                Row(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IntentTargetType.values().forEach { type ->
                        FilterChip(
                            selected = targetType == type,
                            onClick = {
                                targetType = type
                                if (type == IntentTargetType.ACTIVITY) {
                                    action = Intent.ACTION_VIEW
                                    selectedFlags = selectedFlags or Intent.FLAG_ACTIVITY_NEW_TASK
                                } else if (type == IntentTargetType.BROADCAST) {
                                    action = Intent.ACTION_BOOT_COMPLETED
                                } else {
                                    action = ""
                                }
                            },
                            label = { Text(type.label) }
                        )
                    }
                }
            }

            // Target Component & App Picker
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Target Component", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                        Button(onClick = { showAppPicker = true }) {
                            Text(if (pickedAppLabel != null) "Change App" else "Pick Installed App")
                        }
                    }

                    if (pickedAppLabel != null) {
                        Text(
                            "Selected: $pickedAppLabel ($targetPackage)",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    OutlinedTextField(
                        value = targetPackage,
                        onValueChange = {
                            targetPackage = it
                            pickedAppLabel = null
                        },
                        label = { Text("Package Name (Optional for implicit intents)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = targetClass,
                            onValueChange = { targetClass = it },
                            label = { Text("Component Class Name") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        if (packageComponents.isNotEmpty()) {
                            Box {
                                OutlinedButton(
                                    onClick = { showComponentMenu = true },
                                    modifier = Modifier.align(Alignment.CenterEnd).padding(top = 8.dp)
                                ) {
                                    Text("Classes (${packageComponents.size})")
                                }
                                DropdownMenu(
                                    expanded = showComponentMenu,
                                    onDismissRequest = { showComponentMenu = false }
                                ) {
                                    packageComponents.forEach { comp ->
                                        DropdownMenuItem(
                                            text = {
                                                Column {
                                                    Text(comp.simpleName, fontWeight = FontWeight.Bold)
                                                    Text(
                                                        if (comp.exported) "Exported ${comp.permission ?: "(No Perm)"}" else "Unexported",
                                                        fontSize = 11.sp,
                                                        color = if (comp.exported) Color(0xFF2E7D32) else Color(0xFFC62828)
                                                    )
                                                }
                                            },
                                            onClick = {
                                                targetClass = comp.className
                                                showComponentMenu = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Security Audit Indicator
                    if (targetClass.isNotBlank() && selectedComponentDetail != null) {
                        val comp = selectedComponentDetail
                        val isExported = comp.exported
                        val hasPerm = comp.permission != null

                        val bgColor = when {
                            !isExported -> Color(0xFFFBE9E7)
                            !hasPerm -> Color(0xFFFFF8E1)
                            else -> Color(0xFFE8F5E9)
                        }
                        val borderColor = when {
                            !isExported -> Color(0xFFD84315)
                            !hasPerm -> Color(0xFFF9A825)
                            else -> Color(0xFF2E7D32)
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(bgColor)
                                .border(1.dp, borderColor, RoundedCornerShape(8.dp))
                                .padding(10.dp)
                        ) {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        when {
                                            !isExported -> "🔒 Security Warning: Component is NOT Exported"
                                            !hasPerm -> "⚠️ Security Advisory: Exported without Permission"
                                            else -> "🛡️ Protected Component: Requires Permission"
                                        },
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = borderColor
                                    )
                                }
                                Text(
                                    when {
                                        !isExported -> "android:exported=\"false\". In App UID mode, dispatching will throw a SecurityException. Privileged Shell UID 2000 mode bypasses this check."
                                        !hasPerm -> "android:exported=\"true\" with no permission gate. Any installed app can invoke this component. Vulnerable to intent redirection if input is unchecked."
                                        else -> "Permission requirement: ${comp.permission}. App UID requires this permission or dispatch will fail."
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.Black
                                )
                            }
                        }
                    } else if (targetPackage.isBlank() && targetClass.isBlank()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .padding(8.dp)
                        ) {
                            Text(
                                "ℹ️ Implicit Intent — Resolved at runtime across all apps handling this action/data filter.",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }

            // Action & Presets
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Action", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                OutlinedTextField(
                    value = action,
                    onValueChange = { action = it },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Intent Action") }
                )

                val presets = if (targetType == IntentTargetType.ACTIVITY) ACTION_PRESETS_ACTIVITY else ACTION_PRESETS_BROADCAST
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    presets.forEach { preset ->
                        SuggestionChip(
                            onClick = { action = preset },
                            label = { Text(preset.substringAfterLast('.')) }
                        )
                    }
                }
            }

            // Data URI & MIME Type
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = dataUri,
                    onValueChange = { dataUri = it },
                    label = { Text("Data URI (e.g. https://...)") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = mimeType,
                    onValueChange = { mimeType = it },
                    label = { Text("MIME Type") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }

            // Categories
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Categories", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    CATEGORY_PRESETS.forEach { cat ->
                        val selected = selectedCategories.contains(cat)
                        FilterChip(
                            selected = selected,
                            onClick = {
                                if (selected) selectedCategories.remove(cat) else selectedCategories.add(cat)
                            },
                            label = { Text(cat.substringAfterLast('.')) }
                        )
                    }
                }
            }

            // Flags
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Flags", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    COMMON_FLAGS.forEach { flag ->
                        val selected = (selectedFlags and flag.value) != 0
                        FilterChip(
                            selected = selected,
                            onClick = {
                                selectedFlags = if (selected) {
                                    selectedFlags and flag.value.inv()
                                } else {
                                    selectedFlags or flag.value
                                }
                            },
                            label = { Text(flag.name) }
                        )
                    }
                }
            }

            // Extras Builder
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Extras (Key-Value)", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                    TextButton(onClick = { extras.add(ExtraItem()) }) {
                        Text("+ Add Extra")
                    }
                }

                if (extras.isEmpty()) {
                    Text("No extras added.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                extras.forEachIndexed { index, extra ->
                    var typeExpanded by remember { mutableStateOf(false) }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = extra.key,
                            onValueChange = { extras[index] = extra.copy(key = it) },
                            label = { Text("Key") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = extra.value,
                            onValueChange = { extras[index] = extra.copy(value = it) },
                            label = { Text("Value") },
                            singleLine = true,
                            modifier = Modifier.weight(1.2f)
                        )
                        Box {
                            OutlinedButton(onClick = { typeExpanded = true }, modifier = Modifier.padding(top = 6.dp)) {
                                Text(extra.type.label)
                            }
                            DropdownMenu(expanded = typeExpanded, onDismissRequest = { typeExpanded = false }) {
                                ExtraDataType.values().forEach { dt ->
                                    DropdownMenuItem(
                                        text = { Text(dt.label) },
                                        onClick = {
                                            extras[index] = extra.copy(type = dt)
                                            typeExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                        IconButton(onClick = { extras.removeAt(index) }) {
                            Text("✕", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Command Preview
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Generated Shell Command", color = Color.LightGray, style = MaterialTheme.typography.labelSmall)
                        TextButton(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("ADB Command", buildAdbCommand()))
                                Toast.makeText(context, "Command copied to clipboard", Toast.LENGTH_SHORT).show()
                            }
                        ) {
                            Text("Copy", color = Color(0xFF8C9CFF))
                        }
                    }
                    Text(
                        buildAdbCommand(),
                        fontFamily = FontFamily.Monospace,
                        color = Color(0xFF81C784),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            // Execution Controls
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = { dispatchAppUid() },
                    modifier = Modifier.weight(1f),
                    enabled = !isExecuting
                ) {
                    Text("Dispatch (App UID)")
                }

                Button(
                    onClick = { dispatchShellUid() },
                    modifier = Modifier.weight(1f),
                    enabled = !isExecuting,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
                ) {
                    Text(
                        if (adbState == AdbClient.State.Connected) "Dispatch (Shell UID 2000)"
                        else "Dispatch (ADB Connect)"
                    )
                }
            }

            if (isExecuting) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(Modifier.size(24.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Executing dispatch…", style = MaterialTheme.typography.bodyMedium)
                }
            }

            // Dispatch Result Inspection
            lastResult?.let { res ->
                val cardColor = if (res.success) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
                val textColor = if (res.success) Color(0xFF1B5E20) else Color(0xFFB71C1C)

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = cardColor)
                ) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                if (res.success) "✓ Dispatch Success" else "✗ Dispatch Failed",
                                fontWeight = FontWeight.Bold,
                                color = textColor
                            )
                            Text("${res.durationMs}ms · ${res.mode}", style = MaterialTheme.typography.bodySmall)
                        }

                        if (res.isSecurityException) {
                            Text(
                                "🔒 SecurityException caught: Permission Denial / Component not exported. App UID was blocked by Android OS framework.",
                                fontWeight = FontWeight.SemiBold,
                                color = textColor,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }

                        HorizontalDivider(color = textColor.copy(alpha = 0.2f))

                        Text(
                            res.output,
                            fontFamily = FontFamily.Monospace,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Black
                        )
                    }
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

private fun queryComponents(pm: PackageManager, packageName: String, type: IntentTargetType): List<ComponentDetail> {
    return runCatching {
        val info = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.getPackageInfo(
                packageName,
                PackageManager.PackageInfoFlags.of(
                    (PackageManager.GET_ACTIVITIES or
                     PackageManager.GET_SERVICES or
                     PackageManager.GET_RECEIVERS or
                     PackageManager.GET_PERMISSIONS).toLong()
                )
            )
        } else {
            @Suppress("DEPRECATION")
            pm.getPackageInfo(
                packageName,
                PackageManager.GET_ACTIVITIES or
                PackageManager.GET_SERVICES or
                PackageManager.GET_RECEIVERS or
                PackageManager.GET_PERMISSIONS
            )
        }

        when (type) {
            IntentTargetType.ACTIVITY -> info.activities?.map {
                ComponentDetail(it.name, it.name.substringAfterLast('.'), it.exported, it.permission)
            } ?: emptyList()
            IntentTargetType.BROADCAST -> info.receivers?.map {
                ComponentDetail(it.name, it.name.substringAfterLast('.'), it.exported, it.permission)
            } ?: emptyList()
            IntentTargetType.SERVICE -> info.services?.map {
                ComponentDetail(it.name, it.name.substringAfterLast('.'), it.exported, it.permission)
            } ?: emptyList()
        }
    }.getOrElse { emptyList() }
}

@Composable
private fun AdbStatusStrip(adbState: AdbClient.State, onConnect: () -> Unit) {
    val (label, color, showConnect) = when (adbState) {
        AdbClient.State.Connected -> Triple("ADB connected", Color(0xFF4CAF50), false)
        AdbClient.State.Connecting -> Triple("Connecting…", Color(0xFFFFB300), false)
        AdbClient.State.Disconnected -> Triple("Not connected — connect first", Color(0xFF9AA0A6), true)
        AdbClient.State.NeedsPair -> Triple("Pair first (use adb_c)", Color(0xFFE57373), false)
        AdbClient.State.Failed -> Triple("ADB connect failed — retry", Color(0xFFE57373), true)
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .background(color, RoundedCornerShape(50))
            )
            Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
            if (showConnect) OutlinedButton(onClick = onConnect) { Text("Connect") }
        }
    }
}
