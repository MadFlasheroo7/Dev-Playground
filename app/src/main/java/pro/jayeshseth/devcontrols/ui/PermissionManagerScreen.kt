package pro.jayeshseth.devcontrols.ui

import android.app.AppOpsManager
import android.content.Context
import android.content.pm.PackageManager
import android.content.pm.PermissionInfo
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import pro.jayeshseth.devcontrols.adb.AdbClient
import pro.jayeshseth.devcontrols.adb.PermissionMapping
import pro.jayeshseth.devcontrols.util.AppEntry
import pro.jayeshseth.devcontrols.util.InstalledApps

private enum class PmMode { Picking, Managing }

private data class PermissionRow(
    val permission: String,
    val granted: Boolean,
    val isAppop: Boolean,
    val protectionLevel: Int,
)

private data class AppopRow(
    val op: String,
    val mode: Int,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PermissionManagerScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val client = remember { AdbClient.get(context) }
    val adbState by client.state.collectAsState()

    var mode by remember { mutableStateOf(PmMode.Picking) }
    var selectedApp by remember { mutableStateOf<AppEntry?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        when (mode) {
                            PmMode.Picking -> "adb_pm — pick app"
                            PmMode.Managing -> selectedApp?.label ?: "adb_pm"
                        }
                    )
                },
                navigationIcon = {
                    TextButton(onClick = {
                        if (mode == PmMode.Managing) mode = PmMode.Picking
                        else onBack()
                    }) {
                        Text(if (mode == PmMode.Managing) "Apps" else "Back")
                    }
                }
            )
        },
        modifier = Modifier.fillMaxSize()
    ) { inner ->
        Column(
            modifier = Modifier
                .padding(inner)
                .fillMaxSize()
                .padding(horizontal = 12.dp)
        ) {
            AdbStatusStrip(adbState, onConnect = { scope.launch { client.connect() } })

            Spacer(Modifier.height(8.dp))

            when (mode) {
                PmMode.Picking -> AppPickerList(
                    onPick = {
                        selectedApp = it
                        mode = PmMode.Managing
                    }
                )
                PmMode.Managing -> selectedApp?.let { app ->
                    ManageApp(
                        app = app,
                        client = client,
                        scope = scope
                    )
                }
            }
        }
    }
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
            Text(label, modifier = Modifier.weight(1f))
            if (showConnect) OutlinedButton(onClick = onConnect) { Text("Connect") }
        }
    }
}

@Composable
private fun AppPickerList(onPick: (AppEntry) -> Unit) {
    val context = LocalContext.current
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
        LazyColumn {
            items(filtered, key = { it.packageName }) { app ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onPick(app) }
                        .padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(app.label, style = MaterialTheme.typography.bodyLarge)
                        Text(
                            app.packageName,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                HorizontalDivider()
            }
        }
    }
}

@Composable
private fun ManageApp(
    app: AppEntry,
    client: AdbClient,
    scope: kotlinx.coroutines.CoroutineScope,
) {
    val context = LocalContext.current
    var refreshKey by remember { mutableStateOf(0) }
    var busy by remember { mutableStateOf(false) }
    var lastResult by remember { mutableStateOf<String?>(null) }
    var customPerm by remember { mutableStateOf("") }

    val rows by remember(app.packageName, refreshKey) {
        mutableStateOf(loadRows(context, app.packageName))
    }
    val appops by remember(app.packageName, refreshKey) {
        mutableStateOf(loadAppops(context, app.packageName))
    }

    fun runAndRefresh(cmd: String, label: String) {
        if (busy) return
        busy = true
        scope.launch {
            val r = client.exec(cmd)
            lastResult = if (r.ok) "✓ $label" else "✗ $label — ${r.error}"
            busy = false
            refreshKey++
        }
    }

    LazyColumn {
        item {
            Text(
                app.packageName,
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            lastResult?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (it.startsWith("✓")) Color(0xFF1B6F3C) else MaterialTheme.colorScheme.error
                )
                Spacer(Modifier.height(8.dp))
            }
        }

        item {
            Text("Declared permissions", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(4.dp))
            if (rows.isEmpty()) {
                Text(
                    "No declared permissions (or package not found).",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        items(rows, key = { it.permission }) { row ->
            PermissionRowItem(
                row = row,
                busy = busy,
                onToggle = { grantNow ->
                    val cmd = if (row.isAppop) {
                        val op = PermissionMapping.PERMISSION_TO_APPOP[row.permission] ?: return@PermissionRowItem
                        if (grantNow) "appops set ${app.packageName} $op allow"
                        else "appops set ${app.packageName} $op default"
                    } else {
                        if (grantNow) "pm grant ${app.packageName} ${row.permission}"
                        else "pm revoke ${app.packageName} ${row.permission}"
                    }
                    runAndRefresh(cmd, "${if (grantNow) "Grant" else "Revoke"} ${row.permission.substringAfterLast('.')}")
                }
            )
            HorizontalDivider()
        }

        item {
            Spacer(Modifier.height(16.dp))
            Text("Common appops (regardless of declaration)", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(4.dp))
        }

        items(appops, key = { it.op }) { ao ->
            AppopRowItem(
                row = ao,
                busy = busy,
                onSet = { allow ->
                    val cmd = if (allow) "appops set ${app.packageName} ${ao.op} allow"
                    else "appops set ${app.packageName} ${ao.op} default"
                    runAndRefresh(cmd, "${if (allow) "Allow" else "Reset"} appop ${ao.op}")
                }
            )
            HorizontalDivider()
        }

        item {
            Spacer(Modifier.height(16.dp))
            Text("Custom grant", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(4.dp))
            OutlinedTextField(
                value = customPerm,
                onValueChange = { customPerm = it.trim() },
                label = { Text("permission or appop (no spaces)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(
                    autoCorrectEnabled = false,
                    capitalization = KeyboardCapitalization.None,
                    imeAction = ImeAction.Done
                )
            )
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    enabled = !busy && customPerm.isNotBlank(),
                    onClick = {
                        val perm = customPerm
                        if (perm.startsWith("android.permission.") || perm.startsWith("pro.jayeshseth.")) {
                            runAndRefresh("pm grant ${app.packageName} $perm", "Grant $perm")
                        } else {
                            runAndRefresh("appops set ${app.packageName} $perm allow", "Allow appop $perm")
                        }
                    }
                ) { Text("Grant / Allow") }
                OutlinedButton(
                    enabled = !busy && customPerm.isNotBlank(),
                    onClick = {
                        val perm = customPerm
                        if (perm.startsWith("android.permission.") || perm.startsWith("pro.jayeshseth.")) {
                            runAndRefresh("pm revoke ${app.packageName} $perm", "Revoke $perm")
                        } else {
                            runAndRefresh("appops set ${app.packageName} $perm default", "Reset appop $perm")
                        }
                    }
                ) { Text("Revoke / Reset") }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "Heuristic: names starting with android.permission. or pro.jayeshseth. are treated as runtime permissions (pm grant/revoke). Anything else is treated as an appop name (appops set allow/default).",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun PermissionRowItem(
    row: PermissionRow,
    busy: Boolean,
    onToggle: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                row.permission.removePrefix("android.permission."),
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                buildString {
                    append(if (row.isAppop) "appop" else "perm")
                    append(" · ")
                    append(if (row.granted) "granted" else "not granted")
                    append(" · ")
                    append(protectionLevelName(row.protectionLevel))
                },
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = row.granted,
            enabled = !busy,
            onCheckedChange = onToggle
        )
    }
}

@Composable
private fun AppopRowItem(
    row: AppopRow,
    busy: Boolean,
    onSet: (Boolean) -> Unit,
) {
    val allowed = row.mode == AppOpsManager.MODE_ALLOWED
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(row.op, style = MaterialTheme.typography.bodyMedium)
            Text(
                "appop · ${modeName(row.mode)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = allowed,
            enabled = !busy,
            onCheckedChange = onSet
        )
    }
}

private fun loadRows(context: Context, pkg: String): List<PermissionRow> {
    val pm = context.packageManager
    val pi = try {
        if (android.os.Build.VERSION.SDK_INT >= 33) {
            pm.getPackageInfo(pkg, PackageManager.PackageInfoFlags.of(PackageManager.GET_PERMISSIONS.toLong()))
        } else {
            @Suppress("DEPRECATION")
            pm.getPackageInfo(pkg, PackageManager.GET_PERMISSIONS)
        }
    } catch (_: PackageManager.NameNotFoundException) {
        return emptyList()
    }
    val requested = pi.requestedPermissions ?: return emptyList()
    return requested.map { perm ->
        val isAppop = perm in PermissionMapping.PERMISSION_TO_APPOP
        val granted = if (isAppop) {
            val op = PermissionMapping.PERMISSION_TO_APPOP[perm]
                ?: return@map permissionRow(context, perm, false)
            isAppopAllowed(context, pkg, op)
        } else {
            pm.checkPermission(perm, pkg) == PackageManager.PERMISSION_GRANTED
        }
        val pl = runCatching {
            pm.getPermissionInfo(perm, 0).protection
        }.getOrDefault(PermissionInfo.PROTECTION_NORMAL)
        PermissionRow(perm, granted, isAppop, pl)
    }.distinctBy { it.permission }
}

private fun permissionRow(context: Context, perm: String, granted: Boolean): PermissionRow =
    PermissionRow(perm, granted, isAppop = false, protectionLevel = PermissionInfo.PROTECTION_NORMAL)

private fun loadAppops(context: Context, pkg: String): List<AppopRow> {
    return PermissionMapping.COMMON_APPOPS.map { op ->
        AppopRow(op, appopMode(context, pkg, op))
    }
}

private fun isAppopAllowed(context: Context, pkg: String, opName: String): Boolean =
    appopMode(context, pkg, opName) == AppOpsManager.MODE_ALLOWED

private fun appopMode(context: Context, pkg: String, opName: String): Int {
    val mgr = context.getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager ?: return AppOpsManager.MODE_DEFAULT
    val uid = try {
        val ai = if (android.os.Build.VERSION.SDK_INT >= 33) {
            context.packageManager.getApplicationInfo(pkg, PackageManager.ApplicationInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION")
            context.packageManager.getApplicationInfo(pkg, 0)
        }
        ai.uid
    } catch (_: PackageManager.NameNotFoundException) {
        return AppOpsManager.MODE_DEFAULT
    }
    val opStr = "android:" + opName.lowercase()
    return runCatching {
        mgr.unsafeCheckOpNoThrow(opStr, uid, pkg)
    }.getOrDefault(AppOpsManager.MODE_DEFAULT)
}

private fun modeName(mode: Int): String = when (mode) {
    AppOpsManager.MODE_ALLOWED -> "allowed"
    AppOpsManager.MODE_IGNORED -> "ignored"
    AppOpsManager.MODE_ERRORED -> "errored"
    AppOpsManager.MODE_DEFAULT -> "default"
    else -> "mode=$mode"
}

private fun protectionLevelName(pl: Int): String {
    val base = when (pl and PermissionInfo.PROTECTION_MASK_BASE) {
        PermissionInfo.PROTECTION_NORMAL -> "normal"
        PermissionInfo.PROTECTION_DANGEROUS -> "dangerous"
        PermissionInfo.PROTECTION_SIGNATURE -> "signature"
        PermissionInfo.PROTECTION_SIGNATURE_OR_SYSTEM,
        PermissionInfo.PROTECTION_SIGNATURE or PermissionInfo.PROTECTION_FLAG_PRIVILEGED -> "signature|priv"
        else -> "pl=$pl"
    }
    return base
}

