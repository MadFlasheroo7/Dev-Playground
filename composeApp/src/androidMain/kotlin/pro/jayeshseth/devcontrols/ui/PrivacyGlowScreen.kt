package pro.jayeshseth.devcontrols.ui

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import kotlinx.coroutines.launch
import pro.jayeshseth.devcontrols.data.PrivacyGlowPrefs
import pro.jayeshseth.devcontrols.service.PrivacyGlowService
import pro.jayeshseth.devcontrols.util.ForegroundAppDetector

@Composable
fun PrivacyGlowScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs = remember { PrivacyGlowPrefs.get(context) }
    
    val hudEnabledPref by prefs.hudEnabled.collectAsState(initial = false)
    
    var isOverlayGranted by remember { mutableStateOf(Settings.canDrawOverlays(context)) }
    var isWatchAppOpsGranted by remember { mutableStateOf(pro.jayeshseth.devcontrols.util.PermissionUtils.hasWatchAppOps(context)) }
    var isLocationGranted by remember {
        mutableStateOf(
            androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        )
    }
    var isServiceRunning by remember { mutableStateOf(PrivacyGlowService.isRunning()) }

    val locationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        isLocationGranted = granted
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                isOverlayGranted = Settings.canDrawOverlays(context)
                isWatchAppOpsGranted = pro.jayeshseth.devcontrols.util.PermissionUtils.hasWatchAppOps(context)
                isLocationGranted = androidx.core.content.ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.ACCESS_FINE_LOCATION
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                isServiceRunning = PrivacyGlowService.isRunning()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Auto-sync preference with service if needed, restarting if permission is newly granted
    LaunchedEffect(hudEnabledPref, isOverlayGranted, isWatchAppOpsGranted, isLocationGranted) {
        if (hudEnabledPref && isOverlayGranted) {
            if (isServiceRunning) {
                val stopIntent = Intent(context, PrivacyGlowService::class.java)
                context.stopService(stopIntent)
            }
            val intent = Intent(context, PrivacyGlowService::class.java)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
            isServiceRunning = true
        }
    }

    pro.jayeshseth.devcontrols.ui.adbtools.AdbToolScaffold(
        title = "Privacy Glow",
        onBack = onBack,
        subtitle = "Real-time edge glow when Mic/Camera/Location is in use."
    ) { runner ->
        Column(Modifier.padding(vertical = 16.dp)) {
            Text(
                "This tool draws a colored glow around the screen edges when an app accesses sensitive permissions.",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(Modifier.height(16.dp))

            PermissionCard(
                title = "Draw over other apps",
                granted = isOverlayGranted,
                onRequest = {
                    val intent = Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:${context.packageName}")
                    )
                    context.startActivity(intent)
                }
            )

            Spacer(Modifier.height(16.dp))

            PermissionCard(
                title = "Access location (needed for global location detection)",
                granted = isLocationGranted,
                onRequest = {
                    locationLauncher.launch(android.Manifest.permission.ACCESS_FINE_LOCATION)
                }
            )

            Spacer(Modifier.height(16.dp))

            PermissionCard(
                title = "Watch sensitive permissions of other apps (Optional)",
                granted = isWatchAppOpsGranted,
                onRequest = {
                    if (runner.connected) {
                        runner.run("pm grant ${context.packageName} android.permission.WATCH_APPOPS") {
                            isWatchAppOpsGranted = pro.jayeshseth.devcontrols.util.PermissionUtils.hasWatchAppOps(context)
                        }
                    } else {
                        val cmd = "adb shell pm grant ${context.packageName} android.permission.WATCH_APPOPS"
                        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                        cm.setPrimaryClip(android.content.ClipData.newPlainText("adb_command", cmd))
                        android.widget.Toast.makeText(context, "ADB command copied to clipboard", android.widget.Toast.LENGTH_SHORT).show()
                    }
                }
            )

            if (!isWatchAppOpsGranted) {
                Spacer(Modifier.height(8.dp))
                androidx.compose.material3.Card(
                    colors = androidx.compose.material3.CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(12.dp)) {
                        Text(
                            "Optional: WatchAppOps allows direct event monitoring. Without it, Dev Controls will automatically use real-time hardware status listeners.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "adb shell pm grant ${context.packageName} android.permission.WATCH_APPOPS",
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.height(8.dp))
                        Button(
                            onClick = {
                                val cmd = "adb shell pm grant ${context.packageName} android.permission.WATCH_APPOPS"
                                val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                cm.setPrimaryClip(android.content.ClipData.newPlainText("adb_command", cmd))
                                android.widget.Toast.makeText(context, "Command copied", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        ) {
                            Text("Copy ADB Command")
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Enable Privacy Glow HUD", modifier = Modifier.weight(1f))
                Switch(
                    checked = hudEnabledPref,
                    enabled = isOverlayGranted,
                    onCheckedChange = { running ->
                        scope.launch { prefs.setHudEnabled(running) }
                        
                        val intent = Intent(context, PrivacyGlowService::class.java)
                        if (running) {
                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                                context.startForegroundService(intent)
                            } else {
                                context.startService(intent)
                            }
                            isServiceRunning = true
                        } else {
                            context.stopService(intent)
                            isServiceRunning = false
                        }
                    }
                )
            }

            Spacer(Modifier.height(32.dp))
            Text("Legend:", style = MaterialTheme.typography.titleSmall)
            Text("• Red: Camera", color = androidx.compose.ui.graphics.Color.Red)
            Text("• Orange: Microphone", color = androidx.compose.ui.graphics.Color(0xFFFFA500))
            Text("• Blue: Location", color = androidx.compose.ui.graphics.Color.Cyan)

            Spacer(Modifier.height(32.dp))
            Text("Debug Test Controls:", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = {
                    context.sendBroadcast(Intent("pro.jayeshseth.devcontrols.TEST_GLOW").apply {
                        putExtra("op", AppOpsManager.OPSTR_CAMERA)
                        putExtra("active", true)
                        setPackage(context.packageName)
                    })
                }) { Text("Test Camera") }
                Button(onClick = {
                    context.sendBroadcast(Intent("pro.jayeshseth.devcontrols.TEST_GLOW").apply {
                        putExtra("op", AppOpsManager.OPSTR_CAMERA)
                        putExtra("active", false)
                        setPackage(context.packageName)
                    })
                }) { Text("Clear") }
            }
        }
    }
}

@Composable
private fun PermissionCard(
    title: String,
    granted: Boolean,
    onRequest: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(
                if (granted) "Granted" else "Required",
                style = MaterialTheme.typography.bodySmall,
                color = if (granted) androidx.compose.ui.graphics.Color(0xFF4CAF50) else MaterialTheme.colorScheme.error
            )
        }
        if (!granted) {
            Button(onClick = onRequest) {
                Text("Grant")
            }
        }
    }
}
