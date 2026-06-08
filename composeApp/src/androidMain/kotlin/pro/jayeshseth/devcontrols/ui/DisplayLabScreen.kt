package pro.jayeshseth.devcontrols.ui

import android.app.ActivityManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import android.os.PowerManager
import android.view.Display
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

/**
 * Display Lab (category D). The headline feature is a live **refresh-rate monitor**:
 * measured Hz from [withFrameNanos] frame deltas, the reported [Display.refreshRate],
 * the current/supported [Display.Mode]s (with one-tap switching via
 * `LayoutParams.preferredDisplayModeId`), and a long **scrollable sample list** to
 * fling so you can watch the panel jump to its peak rate and settle back.
 *
 * Plus quick system-introspection cards: thermal status, app-standby bucket, and the
 * historical process-exit reasons (why the app was last killed).
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun DisplayLabScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val view = LocalView.current
    val display = view.display

    // --- live measured refresh rate ---
    var measuredHz by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(Unit) {
        var last = 0L
        var ema = 0f
        while (true) {
            withFrameNanos { now ->
                if (last != 0L) {
                    val dt = now - last
                    if (dt > 0) {
                        val inst = 1_000_000_000f / dt
                        ema = if (ema == 0f) inst else ema * 0.9f + inst * 0.1f
                        measuredHz = ema
                    }
                }
                last = now
            }
        }
    }

    val modes = remember { display?.supportedModes?.toList().orEmpty().sortedBy { it.refreshRate } }
    var requestedModeId by remember { mutableStateOf(display?.mode?.modeId ?: -1) }

    fun requestMode(id: Int) {
        val activity = context.findActivityForDisplay() ?: return
        val lp = activity.window.attributes
        lp.preferredDisplayModeId = id
        activity.window.attributes = lp
        requestedModeId = id
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Display Lab") },
                navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }
            )
        },
        modifier = Modifier.fillMaxSize()
    ) { inner ->
        Column(Modifier.padding(inner).fillMaxSize().padding(horizontal = 12.dp)) {

            // ---- refresh readout ----
            Card(Modifier.fillMaxWidth().padding(top = 8.dp)) {
                Column(Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "${measuredHz.roundToInt()}",
                            fontSize = 44.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text("  Hz measured", fontSize = 16.sp)
                    }
                    Text(
                        "reported Display.refreshRate = ${"%.1f".format(display?.refreshRate ?: 0f)} Hz",
                        fontFamily = FontFamily.Monospace, fontSize = 12.sp
                    )
                    display?.mode?.let { m ->
                        Text(
                            "current mode = ${m.physicalWidth}×${m.physicalHeight} @ ${"%.1f".format(m.refreshRate)} (id ${m.modeId})",
                            fontFamily = FontFamily.Monospace, fontSize = 12.sp
                        )
                    }
                }
            }

            // ---- mode switcher ----
            if (modes.size > 1) {
                Text("Request a display mode", style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(top = 12.dp, bottom = 4.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    modes.forEach { m ->
                        FilterChip(
                            selected = requestedModeId == m.modeId,
                            onClick = { requestMode(m.modeId) },
                            label = { Text("${"%.0f".format(m.refreshRate)}Hz") }
                        )
                    }
                }
            }

            SystemCard()

            Text(
                "Fling the list — watch the measured Hz jump to the panel's peak, then settle when idle.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            // ---- scrollable sample list ----
            val rows = remember { (1..400).toList() }
            LazyColumn(Modifier.fillMaxSize()) {
                items(rows) { i ->
                    val hue = (i * 37) % 360
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .height(56.dp)
                            .background(Color.hsv(hue.toFloat(), 0.5f, 0.9f), RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Text(
                            "  Sample row #$i",
                            color = Color.Black,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SystemCard() {
    val context = LocalContext.current
    val info = remember {
        buildSystemIntrospection(context)
    }
    Card(
        Modifier.fillMaxWidth().padding(top = 12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(Modifier.padding(14.dp)) {
            Text("System introspection", fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            info.forEach { line ->
                Text(line, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
            }
        }
    }
}

private fun buildSystemIntrospection(context: Context): List<String> {
    val out = mutableListOf<String>()

    // Thermal status
    val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
    val thermal = pm?.currentThermalStatus
    out += "thermalStatus = ${thermal?.let { thermalName(it) } ?: "?"}"

    // App standby bucket
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
        out += "standbyBucket = ${usm?.appStandbyBucket?.let { bucketName(it) } ?: "?"}"
    }

    // Last process exit reasons
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        val reasons = runCatching {
            am?.getHistoricalProcessExitReasons(context.packageName, 0, 3)
        }.getOrNull().orEmpty()
        if (reasons.isEmpty()) {
            out += "lastExit = (none recorded yet)"
        } else {
            reasons.forEach { r ->
                out += "exit: ${exitReasonName(r.reason)} status=${r.status} \"${r.description ?: ""}\""
            }
        }
    }
    return out
}

private fun thermalName(s: Int): String = when (s) {
    PowerManager.THERMAL_STATUS_NONE -> "NONE"
    PowerManager.THERMAL_STATUS_LIGHT -> "LIGHT"
    PowerManager.THERMAL_STATUS_MODERATE -> "MODERATE"
    PowerManager.THERMAL_STATUS_SEVERE -> "SEVERE"
    PowerManager.THERMAL_STATUS_CRITICAL -> "CRITICAL"
    PowerManager.THERMAL_STATUS_EMERGENCY -> "EMERGENCY"
    PowerManager.THERMAL_STATUS_SHUTDOWN -> "SHUTDOWN"
    else -> "status $s"
}

private fun bucketName(b: Int): String = when (b) {
    UsageStatsManager.STANDBY_BUCKET_ACTIVE -> "ACTIVE"
    UsageStatsManager.STANDBY_BUCKET_WORKING_SET -> "WORKING_SET"
    UsageStatsManager.STANDBY_BUCKET_FREQUENT -> "FREQUENT"
    UsageStatsManager.STANDBY_BUCKET_RARE -> "RARE"
    UsageStatsManager.STANDBY_BUCKET_RESTRICTED -> "RESTRICTED"
    else -> "bucket $b"
}

private fun exitReasonName(r: Int): String = when (r) {
    android.app.ApplicationExitInfo.REASON_ANR -> "ANR"
    android.app.ApplicationExitInfo.REASON_CRASH -> "CRASH"
    android.app.ApplicationExitInfo.REASON_CRASH_NATIVE -> "CRASH_NATIVE"
    android.app.ApplicationExitInfo.REASON_LOW_MEMORY -> "LOW_MEMORY"
    android.app.ApplicationExitInfo.REASON_USER_REQUESTED -> "USER_REQUESTED"
    android.app.ApplicationExitInfo.REASON_USER_STOPPED -> "USER_STOPPED"
    android.app.ApplicationExitInfo.REASON_SIGNALED -> "SIGNALED"
    android.app.ApplicationExitInfo.REASON_OTHER -> "OTHER"
    android.app.ApplicationExitInfo.REASON_EXIT_SELF -> "EXIT_SELF"
    android.app.ApplicationExitInfo.REASON_DEPENDENCY_DIED -> "DEPENDENCY_DIED"
    else -> "reason $r"
}

private fun Context.findActivityForDisplay(): android.app.Activity? {
    var c: Context? = this
    while (c is ContextWrapper) {
        if (c is android.app.Activity) return c
        c = c.baseContext
    }
    return null
}
