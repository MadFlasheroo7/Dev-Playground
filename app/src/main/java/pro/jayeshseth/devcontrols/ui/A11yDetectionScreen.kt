package pro.jayeshseth.devcontrols.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import pro.jayeshseth.devcontrols.util.A11yDetection
import pro.jayeshseth.devcontrols.util.AdbDetection

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun A11yDetectionScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var snapshot by remember { mutableStateOf<A11yDetection.Snapshot?>(null) }

    suspend fun runScan() {
        snapshot = withContext(Dispatchers.IO) { A11yDetection.scan(context) }
    }

    LaunchedEffect(Unit) { runScan() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Accessibility Detection") },
                navigationIcon = { TextButton(onClick = onBack) { Text("Back") } },
                actions = {
                    snapshot?.let { snap ->
                        TextButton(onClick = { copyReport(context, snap) }) { Text("Copy report") }
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
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 12.dp)
        ) {
            Spacer(Modifier.height(8.dp))
            val snap = snapshot
            if (snap == null) {
                Text("Scanning…", modifier = Modifier.padding(16.dp))
            } else {
                SummaryHeader(snap) { scope.launch { runScan() } }
                Spacer(Modifier.height(12.dp))

                GlobalFlags(snap)
                Spacer(Modifier.height(12.dp))

                Text(
                    "Bound services (${snap.services.size})",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(6.dp))
                if (snap.services.isEmpty()) {
                    Text(
                        "No accessibility services are bound.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    snap.services.forEach { svc ->
                        ServiceCard(svc)
                        Spacer(Modifier.height(10.dp))
                    }
                }

                Spacer(Modifier.height(16.dp))
                Text(
                    "Detection recipes for your prod app",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(6.dp))
                A11yDetection.snippets.forEach { (title, code) ->
                    SnippetCard(title, code) { copy(context, code) }
                    Spacer(Modifier.height(10.dp))
                }
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun severityColor(s: AdbDetection.Severity): Color = when (s) {
    AdbDetection.Severity.Clean -> Color(0xFF1B6F3C)
    AdbDetection.Severity.Suspicious -> Color(0xFFFFB300)
    AdbDetection.Severity.Tampered -> Color(0xFFE57373)
    AdbDetection.Severity.Unknown -> Color(0xFF9AA0A6)
}

@Composable
private fun SummaryHeader(snap: A11yDetection.Snapshot, onRescan: () -> Unit) {
    val label = when (snap.overall) {
        AdbDetection.Severity.Clean -> "No suspicious services"
        AdbDetection.Severity.Suspicious -> "Third-party service bound"
        AdbDetection.Severity.Tampered -> "Window-reading service active"
        AdbDetection.Severity.Unknown -> "Unknown"
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(Modifier.size(14.dp).background(severityColor(snap.overall), RoundedCornerShape(50)))
            Column(Modifier.weight(1f)) {
                Text(label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    "${snap.services.size} service(s) bound. The eyes behind Hide Dev / Kiosk show up here.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Button(onClick = onRescan) { Text("Re-scan") }
        }
    }
}

@Composable
private fun GlobalFlags(snap: A11yDetection.Snapshot) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp)) {
            FlagRow("accessibility_enabled", snap.accessibilityEnabled.toString(),
                if (snap.accessibilityEnabled == 1) AdbDetection.Severity.Suspicious else AdbDetection.Severity.Clean)
            FlagRow("AccessibilityManager.isEnabled", snap.managerEnabled.toString(),
                if (snap.managerEnabled) AdbDetection.Severity.Suspicious else AdbDetection.Severity.Clean)
            FlagRow("touch_exploration_enabled", snap.touchExploration.toString(), AdbDetection.Severity.Clean)
            Spacer(Modifier.height(6.dp))
            Text("enabled_accessibility_services", fontFamily = FontFamily.Monospace, fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(snap.rawEnabledServices, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
        }
    }
}

@Composable
private fun FlagRow(name: String, value: String, sev: AdbDetection.Severity) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 2.dp)) {
        Box(Modifier.size(8.dp).background(severityColor(sev), RoundedCornerShape(50)))
        Spacer(Modifier.width(8.dp))
        Text(name, fontFamily = FontFamily.Monospace, fontSize = 12.sp, modifier = Modifier.weight(1f))
        Text(value, fontFamily = FontFamily.Monospace, fontSize = 12.sp, color = severityColor(sev))
    }
}

@Composable
private fun ServiceCard(svc: A11yDetection.ServiceReading) {
    val color = severityColor(svc.severity)
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(10.dp).background(color, RoundedCornerShape(50)))
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        svc.label + if (svc.isOurs) "  (this app)" else "",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium
                    )
                    Text(svc.component, fontFamily = FontFamily.Monospace, fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(if (svc.isSystemApp) "system" else "3rd-party", fontSize = 11.sp, color = color)
            }
            Spacer(Modifier.height(6.dp))
            Box(
                Modifier.fillMaxWidth().background(Color(0xFF1A1A1A), RoundedCornerShape(6.dp)).padding(8.dp)
            ) {
                Column {
                    Text("retrieve-window-content = ${svc.canRetrieveWindowContent}",
                        color = if (svc.canRetrieveWindowContent) color else Color(0xFFCFCFCF),
                        fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                    Text("perform-gestures        = ${svc.canPerformGestures}",
                        color = if (svc.canPerformGestures) color else Color(0xFFCFCFCF),
                        fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                    Text("filter-key-events       = ${svc.canFilterKeyEvents}",
                        color = Color(0xFFCFCFCF), fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                    Text("feedback                = ${svc.feedbackType}",
                        color = Color(0xFFCFCFCF), fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                    Spacer(Modifier.height(4.dp))
                    Text(svc.reason, color = Color(0xFFCFCFCF), fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                }
            }
        }
    }
}

@Composable
private fun SnippetCard(title: String, code: String, onCopy: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f))
                TextButton(onClick = onCopy) { Text("Copy snippet") }
            }
            Spacer(Modifier.height(4.dp))
            Box(Modifier.fillMaxWidth().background(Color(0xFF1A1A1A), RoundedCornerShape(6.dp)).padding(8.dp)) {
                Text(code, color = Color(0xFFCFCFCF), fontFamily = FontFamily.Monospace, fontSize = 11.sp)
            }
        }
    }
}

private fun copy(context: Context, text: String) {
    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    cm.setPrimaryClip(ClipData.newPlainText("a11y detection", text))
    Toast.makeText(context, "Copied", Toast.LENGTH_SHORT).show()
}

private fun copyReport(context: Context, snap: A11yDetection.Snapshot) {
    val report = buildString {
        appendLine("# Accessibility Detection Report")
        appendLine("# ${java.time.LocalDateTime.now()}")
        appendLine("# overall: ${snap.overall}")
        appendLine()
        appendLine("accessibility_enabled = ${snap.accessibilityEnabled}")
        appendLine("AccessibilityManager.isEnabled = ${snap.managerEnabled}")
        appendLine("touch_exploration_enabled = ${snap.touchExploration}")
        appendLine("enabled_accessibility_services = ${snap.rawEnabledServices}")
        appendLine()
        snap.services.forEach { s ->
            appendLine("## ${s.label}  [${s.severity}]${if (s.isOurs) "  (this app)" else ""}")
            appendLine("# ${s.component}")
            appendLine("system=${s.isSystemApp} retrieveWindowContent=${s.canRetrieveWindowContent} performGestures=${s.canPerformGestures} filterKeyEvents=${s.canFilterKeyEvents} feedback=${s.feedbackType}")
            appendLine("# ${s.reason}")
            appendLine()
        }
    }
    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    cm.setPrimaryClip(ClipData.newPlainText("a11y detection report", report))
    Toast.makeText(context, "Report copied (${report.length} chars)", Toast.LENGTH_SHORT).show()
}
