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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
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
import pro.jayeshseth.devcontrols.util.AdbDetection

/** ADB detection — surfaces whether USB/wireless debugging is exposed (defensive self-test). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdbDetectionScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val readings = remember { mutableStateMapOf<String, AdbDetection.Reading>() }
    var lastRunAt by remember { mutableStateOf(0L) }

    suspend fun runAll() {
        withContext(Dispatchers.IO) {
            AdbDetection.signals.forEach { s ->
                readings[s.id] = s.probe(context)
            }
        }
        lastRunAt = System.currentTimeMillis()
    }

    LaunchedEffect(Unit) { runAll() }

    val overall = remember(readings.size, lastRunAt) {
        when {
            readings.values.any { it.severity == AdbDetection.Severity.Tampered } -> AdbDetection.Severity.Tampered
            readings.values.any { it.severity == AdbDetection.Severity.Suspicious } -> AdbDetection.Severity.Suspicious
            readings.isEmpty() -> AdbDetection.Severity.Unknown
            else -> AdbDetection.Severity.Clean
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("ADB Detection") },
                navigationIcon = {
                    TextButton(onClick = onBack) { Text("Back") }
                },
                actions = {
                    TextButton(onClick = {
                        copyReport(context, readings)
                    }) { Text("Copy report") }
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
            SummaryCard(overall, readings.size) { scope.launch { runAll() } }
            Spacer(Modifier.height(12.dp))

            AdbDetection.signals.forEach { signal ->
                val reading = readings[signal.id]
                SignalCard(signal, reading) { copy(context, signal.snippet) }
                Spacer(Modifier.height(10.dp))
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun SummaryCard(overall: AdbDetection.Severity, count: Int, onRerun: () -> Unit) {
    val (label, color) = when (overall) {
        AdbDetection.Severity.Clean -> "All clean" to Color(0xFF1B6F3C)
        AdbDetection.Severity.Suspicious -> "Suspicious signals detected" to Color(0xFFFFB300)
        AdbDetection.Severity.Tampered -> "Active tampering detected" to Color(0xFFE57373)
        AdbDetection.Severity.Unknown -> "Running…" to Color(0xFF9AA0A6)
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
            Box(
                modifier = Modifier
                    .size(14.dp)
                    .background(color, RoundedCornerShape(50))
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    "$count signal${if (count == 1) "" else "s"} probed. Tap any to copy a drop-in Kotlin snippet.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Button(onClick = onRerun) { Text("Re-run") }
        }
    }
}

@Composable
private fun SignalCard(
    signal: AdbDetection.Signal,
    reading: AdbDetection.Reading?,
    onCopy: () -> Unit,
) {
    val color = when (reading?.severity) {
        AdbDetection.Severity.Clean -> Color(0xFF1B6F3C)
        AdbDetection.Severity.Suspicious -> Color(0xFFFFB300)
        AdbDetection.Severity.Tampered -> Color(0xFFE57373)
        AdbDetection.Severity.Unknown, null -> Color(0xFF9AA0A6)
    }
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(10.dp).background(color, RoundedCornerShape(50)))
                Spacer(Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(signal.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium, fontFamily = FontFamily.Monospace, fontSize = 13.sp)
                    Text(
                        signal.subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                TextButton(onClick = onCopy) { Text("Copy snippet") }
            }
            if (reading != null) {
                Spacer(Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF1A1A1A), RoundedCornerShape(6.dp))
                        .padding(8.dp)
                ) {
                    Column {
                        Text(
                            "value = ${reading.value}",
                            color = color,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            reading.explanation,
                            color = Color(0xFFCFCFCF),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }
    }
}

private fun copy(context: Context, text: String) {
    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    cm.setPrimaryClip(ClipData.newPlainText("adb detection", text))
    Toast.makeText(context, "Copied", Toast.LENGTH_SHORT).show()
}

private fun copyReport(
    context: Context,
    readings: Map<String, AdbDetection.Reading>,
) {
    val report = buildString {
        appendLine("# ADB Detection Report")
        appendLine("# ${java.time.LocalDateTime.now()}")
        appendLine()
        AdbDetection.signals.forEach { s ->
            val r = readings[s.id] ?: return@forEach
            appendLine("## ${s.title}  [${r.severity}]")
            appendLine("# ${s.subtitle}")
            appendLine("value = ${r.value}")
            appendLine("# ${r.explanation}")
            appendLine()
        }
    }
    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    cm.setPrimaryClip(ClipData.newPlainText("adb detection report", report))
    Toast.makeText(context, "Report copied (${report.length} chars)", Toast.LENGTH_SHORT).show()
}
