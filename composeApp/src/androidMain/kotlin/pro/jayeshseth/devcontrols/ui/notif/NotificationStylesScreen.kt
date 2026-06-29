package pro.jayeshseth.devcontrols.ui.notif

import android.app.NotificationManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationCompat
import androidx.core.app.Person
import pro.jayeshseth.devcontrols.service.CustomNotificationListener
import pro.jayeshseth.devcontrols.service.CustomStatusBarService

/** Notification styles & Custom Notification Panel Control Center. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationStylesScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val ch = remember { ensureChannel(context, "styles", "Styles", NotificationManager.IMPORTANCE_DEFAULT) }

    // Permission States
    var isOverlayGranted by remember { mutableStateOf(false) }
    var isNotifListenerGranted by remember { mutableStateOf(false) }
    var isWriteSettingsGranted by remember { mutableStateOf(false) }
    var isServiceRunning by remember { mutableStateOf(CustomStatusBarService.isRunning) }

    // Check permissions periodically
    LaunchedEffect(Unit) {
        while (true) {
            isOverlayGranted = Settings.canDrawOverlays(context)
            isNotifListenerGranted = isNotificationServiceEnabled(context)
            isWriteSettingsGranted = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                Settings.System.canWrite(context)
            } else true
            isServiceRunning = CustomStatusBarService.isRunning
            
            kotlinx.coroutines.delay(1000)
        }
    }

    fun base() = NotificationCompat.Builder(context, ch).setSmallIcon(SMALL_ICON).setAutoCancel(true)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Notification Center") },
                navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }
            )
        },
        modifier = Modifier.fillMaxSize()
    ) { inner ->
        Column(
            Modifier
                .padding(inner)
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Section 1: Custom Notification Panel Service Control
            Text(
                "System-Wide Overlay Panel",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                "Replaces the default Android notification drawer with a custom Compose panel.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(12.dp))

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Service Switch
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Enable Custom Drawer", fontWeight = FontWeight.Bold)
                            Text("Draws a gesture trigger bar at the top edge of the screen.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = isServiceRunning,
                            onCheckedChange = { enable ->
                                if (enable) {
                                    if (isOverlayGranted) {
                                        context.startService(Intent(context, CustomStatusBarService::class.java))
                                        isServiceRunning = true
                                    } else {
                                        // Request overlay permission
                                        val intent = Intent(
                                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                            Uri.parse("package:${context.packageName}")
                                        )
                                        context.startActivity(intent)
                                    }
                                } else {
                                    context.stopService(Intent(context, CustomStatusBarService::class.java))
                                    isServiceRunning = false
                                }
                            }
                        )
                    }

                    if (isServiceRunning) {
                        Button(
                            onClick = { CustomStatusBarService.openPanel() },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text("Simulate Swipe (Open Custom Drawer)")
                        }

                        val showTriggerArea by CustomStatusBarService.showTriggerArea.collectAsState()
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Show Swipeable Area Hint", fontWeight = FontWeight.Bold)
                                Text("Draws a translucent bar showing where to swipe.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Switch(
                                checked = showTriggerArea,
                                onCheckedChange = { value ->
                                    CustomStatusBarService.showTriggerArea.value = value
                                }
                            )
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

                    // Permission Toggles
                    PermissionRow(
                        title = "Draw Over Other Apps",
                        description = "Required to display the custom drawer trigger and panel.",
                        isGranted = isOverlayGranted,
                        onGrant = {
                            val intent = Intent(
                                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                Uri.parse("package:${context.packageName}")
                            )
                            context.startActivity(intent)
                        }
                    )

                    PermissionRow(
                        title = "Notification Access",
                        description = "Required to sync and display real system notifications.",
                        isGranted = isNotifListenerGranted,
                        onGrant = {
                            val intent = Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS")
                            context.startActivity(intent)
                        }
                    )

                    PermissionRow(
                        title = "Write System Settings",
                        description = "Required to adjust screen brightness from the panel.",
                        isGranted = isWriteSettingsGranted,
                        onGrant = {
                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                                val intent = Intent(
                                    Settings.ACTION_MANAGE_WRITE_SETTINGS,
                                    Uri.parse("package:${context.packageName}")
                                )
                                context.startActivity(intent)
                            }
                        }
                    )
                }
            }

            Spacer(Modifier.height(28.dp))

            // Section 2: Standard Notification Styles Generator
            Text(
                "Notification Styles Test",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                "Trigger various system notification styles to test the custom panel sync.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(12.dp))

            Btn("BigText") {
                post(context, 1, base().setContentTitle("Big Text").setContentText("Tap to expand")
                    .setStyle(NotificationCompat.BigTextStyle().bigText("This is a long expanded body. ".repeat(8))))
            }
            Btn("Inbox") {
                post(context, 2, base().setContentTitle("Inbox · 5 new").setContentText("Multiple lines")
                    .setStyle(NotificationCompat.InboxStyle().also { s -> (1..5).forEach { s.addLine("Line item $it") } }))
            }
            Btn("Messaging") {
                val me = Person.Builder().setName("You").build()
                val them = Person.Builder().setName("Ada").build()
                post(context, 3, base().setStyle(
                    NotificationCompat.MessagingStyle(me)
                        .addMessage("Hey, you up?", System.currentTimeMillis() - 60000, them)
                        .addMessage("Building Dev Controls 😄", System.currentTimeMillis(), me)
                ))
            }
            Btn("Big Picture") {
                val bmp = android.graphics.Bitmap.createBitmap(600, 300, android.graphics.Bitmap.Config.ARGB_8888).also {
                    android.graphics.Canvas(it).drawColor(0xFF3D5AFE.toInt())
                }
                post(context, 4, base().setContentTitle("Big Picture").setContentText("Expanded image")
                    .setStyle(NotificationCompat.BigPictureStyle().bigPicture(bmp)))
            }
            Btn("High-importance (heads-up)") {
                val hch = ensureChannel(context, "headsup", "Heads-up", NotificationManager.IMPORTANCE_HIGH)
                post(context, 5, NotificationCompat.Builder(context, hch).setSmallIcon(SMALL_ICON).setAutoCancel(true)
                    .setPriority(NotificationCompat.PRIORITY_HIGH).setContentTitle("Heads-up!").setContentText("Pops over the top"))
            }
            Btn("Grouped (+ summary)") {
                (10..12).forEach { i ->
                    post(context, i, base().setGroup("grp").setContentTitle("Item $i").setContentText("In a group"))
                }
                post(context, 19, base().setGroup("grp").setGroupSummary(true).setContentTitle("3 items"))
            }
        }
    }
}

@Composable
private fun PermissionRow(
    title: String,
    description: String,
    isGranted: Boolean,
    onGrant: () -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text(description, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.width(8.dp))
        Button(
            onClick = onGrant,
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isGranted) Color(0xFF30D158) else MaterialTheme.colorScheme.primary
            ),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text(if (isGranted) "Granted" else "Grant", fontSize = 12.sp)
        }
    }
}

@Composable
private fun Btn(label: String, onClick: () -> Unit) {
    Button(onClick = onClick, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) { Text(label) }
}

private fun isNotificationServiceEnabled(context: Context): Boolean {
    val cn = ComponentName(context, CustomNotificationListener::class.java)
    val flat = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
    return flat != null && flat.contains(cn.flattenToString())
}
