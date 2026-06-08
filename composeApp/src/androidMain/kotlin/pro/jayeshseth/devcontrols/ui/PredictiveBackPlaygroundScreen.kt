package pro.jayeshseth.devcontrols.ui

import android.os.Build
import androidx.activity.BackEventCompat
import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.flow.collect
import kotlin.coroutines.cancellation.CancellationException

private enum class BackPhase { Idle, Started, Progressing, Invoked, Cancelled }

private data class BackLog(
    val whenMs: Long,
    val type: String,
    val touchX: Float?,
    val touchY: Float?,
    val progress: Float?,
    val edge: Int?,
)

/** Predictive-back gesture playground (Android 14 back preview). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PredictiveBackPlaygroundScreen(onBack: () -> Unit) {
    var enabled by remember { mutableStateOf(true) }
    var navigateOnCommit by remember { mutableStateOf(false) }

    var transformSlide by remember { mutableStateOf(true) }
    var transformScale by remember { mutableStateOf(true) }
    var transformRotate by remember { mutableStateOf(false) }
    var transformFade by remember { mutableStateOf(false) }
    var useInterpolated by remember { mutableStateOf(false) }

    var phase by remember { mutableStateOf(BackPhase.Idle) }
    var lastEvent by remember { mutableStateOf<BackEventCompat?>(null) }
    var startTimeMs by remember { mutableStateOf(0L) }
    var endTimeMs by remember { mutableStateOf(0L) }
    var gestureCount by remember { mutableStateOf(0) }
    var commitCount by remember { mutableStateOf(0) }
    var cancelCount by remember { mutableStateOf(0) }

    val events = remember { mutableStateListOf<BackLog>() }

    PredictiveBackHandler(enabled = enabled) { progressFlow ->
        phase = BackPhase.Started
        startTimeMs = System.currentTimeMillis()
        endTimeMs = 0L
        gestureCount += 1
        events.add(BackLog(startTimeMs, "STARTED", null, null, null, null))
        try {
            progressFlow.collect { event ->
                phase = BackPhase.Progressing
                lastEvent = event
                events.add(
                    BackLog(
                        whenMs = System.currentTimeMillis(),
                        type = "PROGRESS",
                        touchX = event.touchX,
                        touchY = event.touchY,
                        progress = event.progress,
                        edge = event.swipeEdge,
                    )
                )
            }
            // Flow completed → committed
            phase = BackPhase.Invoked
            endTimeMs = System.currentTimeMillis()
            commitCount += 1
            events.add(BackLog(endTimeMs, "INVOKED", null, null, null, null))
            if (navigateOnCommit) onBack()
        } catch (e: CancellationException) {
            phase = BackPhase.Cancelled
            endTimeMs = System.currentTimeMillis()
            cancelCount += 1
            events.add(BackLog(endTimeMs, "CANCELLED", null, null, null, null))
            throw e
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Predictive Back") },
                navigationIcon = {
                    TextButton(onClick = onBack) { Text("Back") }
                },
                actions = {
                    TextButton(onClick = {
                        events.clear()
                        lastEvent = null
                        phase = BackPhase.Idle
                        gestureCount = 0
                        commitCount = 0
                        cancelCount = 0
                        startTimeMs = 0; endTimeMs = 0
                    }) { Text("Reset") }
                }
            )
        },
        modifier = Modifier.fillMaxSize()
    ) { inner ->
        Column(
            modifier = Modifier
                .padding(inner)
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            CapabilityCard(enabled, navigateOnCommit,
                onEnabledChange = { enabled = it },
                onCommitChange = { navigateOnCommit = it }
            )

            Spacer(Modifier.height(10.dp))
            StateCard(
                phase = phase,
                last = lastEvent,
                startTimeMs = startTimeMs,
                endTimeMs = endTimeMs,
                gestureCount = gestureCount,
                commitCount = commitCount,
                cancelCount = cancelCount,
                useInterpolated = useInterpolated,
                onInterpolatedChange = { useInterpolated = it }
            )

            Spacer(Modifier.height(10.dp))
            VisualPreviewCard(
                last = lastEvent,
                phase = phase,
                slide = transformSlide,
                scale = transformScale,
                rotate = transformRotate,
                fade = transformFade,
                interpolate = useInterpolated,
                onToggle = { which, value ->
                    when (which) {
                        "slide" -> transformSlide = value
                        "scale" -> transformScale = value
                        "rotate" -> transformRotate = value
                        "fade" -> transformFade = value
                    }
                }
            )

            Spacer(Modifier.height(10.dp))
            TouchpadCard(last = lastEvent)

            Spacer(Modifier.height(10.dp))
            EventLogCard(events = events, modifier = Modifier.weight(1f, fill = false))
        }
    }
}

@Composable
private fun CapabilityCard(
    enabled: Boolean,
    navigateOnCommit: Boolean,
    onEnabledChange: (Boolean) -> Unit,
    onCommitChange: (Boolean) -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text("Capability", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(2.dp))
            InfoRow("API level", Build.VERSION.SDK_INT.toString())
            InfoRow("OnBackAnimationCallback supported", (Build.VERSION.SDK_INT >= 34).toString())
            InfoRow("OnBackInvokedCallback supported", (Build.VERSION.SDK_INT >= 33).toString())
            InfoRow("enableOnBackInvokedCallback (manifest)", "true")
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("PredictiveBackHandler enabled")
                Switch(checked = enabled, onCheckedChange = onEnabledChange)
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Commit closes screen")
                    Text(
                        "If on, completing the gesture pops back like normal. If off, the playground stays open.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(checked = navigateOnCommit, onCheckedChange = onCommitChange)
            }
        }
    }
}

@Composable
private fun StateCard(
    phase: BackPhase,
    last: BackEventCompat?,
    startTimeMs: Long,
    endTimeMs: Long,
    gestureCount: Int,
    commitCount: Int,
    cancelCount: Int,
    useInterpolated: Boolean,
    onInterpolatedChange: (Boolean) -> Unit,
) {
    val rawProgress = last?.progress ?: 0f
    val interpolated by remember(rawProgress) {
        derivedStateOf { FastOutSlowInEasing.transform(rawProgress) }
    }
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                PhaseDot(phase)
                Spacer(Modifier.width(8.dp))
                Text(
                    "State: ${phase.name}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
            }
            Spacer(Modifier.height(6.dp))
            InfoRow("touchX", "%.1f px".format(last?.touchX ?: 0f))
            InfoRow("touchY", "%.1f px".format(last?.touchY ?: 0f))
            InfoRow("swipeEdge", edgeLabel(last?.swipeEdge))
            InfoRow("raw progress", "%.3f".format(rawProgress))
            InfoRow("interpolated (fast-out-slow-in)", "%.3f".format(interpolated))
            Spacer(Modifier.height(6.dp))
            LinearProgressIndicator(
                progress = { (if (useInterpolated) interpolated else rawProgress).coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth().height(6.dp)
            )
            Spacer(Modifier.height(6.dp))
            val duration = if (startTimeMs > 0L) {
                val end = if (endTimeMs > 0L) endTimeMs else System.currentTimeMillis()
                "${end - startTimeMs} ms"
            } else "—"
            InfoRow("last gesture duration", duration)
            InfoRow(
                "counters (gestures · commits · cancels)",
                "$gestureCount · $commitCount · $cancelCount"
            )
            Spacer(Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Use interpolated progress on preview")
                Switch(checked = useInterpolated, onCheckedChange = onInterpolatedChange)
            }
        }
    }
}

@Composable
private fun PhaseDot(phase: BackPhase) {
    val c = when (phase) {
        BackPhase.Idle -> Color(0xFF9AA0A6)
        BackPhase.Started -> Color(0xFFFFB300)
        BackPhase.Progressing -> Color(0xFF3949AB)
        BackPhase.Invoked -> Color(0xFF4CAF50)
        BackPhase.Cancelled -> Color(0xFFE57373)
    }
    Box(
        modifier = Modifier
            .size(12.dp)
            .background(c, RoundedCornerShape(50))
    )
}

@Composable
private fun VisualPreviewCard(
    last: BackEventCompat?,
    phase: BackPhase,
    slide: Boolean,
    scale: Boolean,
    rotate: Boolean,
    fade: Boolean,
    interpolate: Boolean,
    onToggle: (String, Boolean) -> Unit,
) {
    val density = LocalDensity.current
    val raw = last?.progress ?: 0f
    val progress = if (interpolate) FastOutSlowInEasing.transform(raw) else raw
    val edge = last?.swipeEdge ?: BackEventCompat.EDGE_LEFT
    val dir = if (edge == BackEventCompat.EDGE_LEFT) 1f else -1f

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text("Visual preview", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            Text(
                "Swipe from either edge — the card below applies the selected transforms in real time based on the predictive-back progress value.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(10.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.7f)
                        .height(120.dp)
                        .graphicsLayer {
                            if (slide) {
                                translationX = with(density) { (progress * 80.dp.toPx() * dir) }
                            }
                            if (scale) {
                                val s = 1f - progress * 0.25f
                                scaleX = s
                                scaleY = s
                            }
                            if (rotate) {
                                rotationZ = progress * 10f * dir
                            }
                            if (fade) {
                                alpha = 1f - progress * 0.5f
                            }
                        }
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        when (phase) {
                            BackPhase.Idle -> "Idle"
                            BackPhase.Started -> "Started"
                            BackPhase.Progressing -> "Progress %.0f%%".format(progress * 100f)
                            BackPhase.Invoked -> "Invoked ✓"
                            BackPhase.Cancelled -> "Cancelled ✕"
                        },
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ToggleChip("slide", slide) { onToggle("slide", it) }
                ToggleChip("scale", scale) { onToggle("scale", it) }
                ToggleChip("rotate", rotate) { onToggle("rotate", it) }
                ToggleChip("fade", fade) { onToggle("fade", it) }
            }
        }
    }
}

@Composable
private fun ToggleChip(label: String, on: Boolean, onChange: (Boolean) -> Unit) {
    OutlinedButton(
        onClick = { onChange(!on) },
        colors = if (on)
            androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            )
        else
            androidx.compose.material3.ButtonDefaults.outlinedButtonColors()
    ) { Text(label) }
}

@Composable
private fun TouchpadCard(last: BackEventCompat?) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text("Live touch position", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            Text(
                "Mini-map of your phone's screen — dot moves with the back-gesture touch.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(10.dp))
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(0.45f) // typical phone aspect
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF1E1E1E))
            ) {
                val containerW = maxWidth
                val containerH = maxHeight
                // Map last.touchX/Y (device px) onto the mini-map.
                val tx = last?.touchX ?: 0f
                val ty = last?.touchY ?: 0f
                val res = androidx.compose.ui.platform.LocalContext.current.resources
                val deviceW = res.displayMetrics.widthPixels.toFloat().coerceAtLeast(1f)
                val deviceH = res.displayMetrics.heightPixels.toFloat().coerceAtLeast(1f)
                val nx = (tx / deviceW).coerceIn(0f, 1f)
                val ny = (ty / deviceH).coerceIn(0f, 1f)

                val xDp = containerW * nx
                val yDp = containerH * ny
                val edge = last?.swipeEdge

                // Highlight the active edge while the gesture is live.
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .width(6.dp)
                        .fillMaxHeight()
                        .background(
                            if (edge == BackEventCompat.EDGE_LEFT)
                                Color(0xFF3FFF8C).copy(alpha = 0.75f)
                            else Color.Transparent
                        )
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .width(6.dp)
                        .fillMaxHeight()
                        .background(
                            if (edge == BackEventCompat.EDGE_RIGHT)
                                Color(0xFF3FFF8C).copy(alpha = 0.75f)
                            else Color.Transparent
                        )
                )

                // Touch dot
                Box(
                    modifier = Modifier
                        .offset(x = xDp - 8.dp, y = yDp - 8.dp)
                        .size(16.dp)
                        .background(Color(0xFF3FFF8C), RoundedCornerShape(50))
                )
            }
        }
    }
}

@Composable
private fun EventLogCard(events: List<BackLog>, modifier: Modifier = Modifier) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Event log (${events.size})", style = MaterialTheme.typography.titleMedium)
            }
            Spacer(Modifier.height(8.dp))
            if (events.isEmpty()) {
                Text(
                    "Swipe from either edge to see events. Slow swipes generate more PROGRESS events. Release before threshold → CANCELLED. Complete → INVOKED.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                LazyColumn(modifier = Modifier.fillMaxWidth().height(220.dp)) {
                    items(events.asReversed()) { entry -> EventRow(entry) }
                }
            }
        }
    }
}

@Composable
private fun EventRow(e: BackLog) {
    val color = when (e.type) {
        "STARTED" -> Color(0xFFFFB300)
        "PROGRESS" -> Color(0xFF81D4FA)
        "INVOKED" -> Color(0xFF81C784)
        "CANCELLED" -> Color(0xFFE57373)
        else -> Color(0xFFCFCFCF)
    }
    val text = when (e.type) {
        "PROGRESS" -> "%-8s p=%.3f x=%.0f y=%.0f edge=%s".format(
            e.type, e.progress ?: 0f, e.touchX ?: 0f, e.touchY ?: 0f, edgeLabel(e.edge)
        )
        else -> e.type
    }
    Text(
        text,
        style = MaterialTheme.typography.bodySmall,
        color = color,
        fontFamily = FontFamily.Monospace,
        fontSize = 11.sp
    )
}

@Composable
private fun InfoRow(k: String, v: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Text(
            k, modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodySmall
        )
        Text(
            v,
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun edgeLabel(edge: Int?): String = when (edge) {
    BackEventCompat.EDGE_LEFT -> "LEFT (0)"
    BackEventCompat.EDGE_RIGHT -> "RIGHT (1)"
    null -> "—"
    else -> "edge=$edge"
}
