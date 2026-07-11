package pro.jayeshseth.devcontrols.ui.maps

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Paint as NativePaint
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.net.wifi.WifiManager
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.*
import pro.jayeshseth.devcontrols.ui.PermGate
import kotlin.math.*

// ─────────────────────────────────────────────────────────────────────────────
// Data Models
// ─────────────────────────────────────────────────────────────────────────────

/** A single WiFi RSSI sample at a tracked world-coordinate position. */
data class WifiSample(
    val x: Float,       // world East  (metres, origin = start point)
    val y: Float,       // world North (metres)
    val rssi: Int,      // dBm (typically -30 to -100)
    val ssid: String,
)

// ─────────────────────────────────────────────────────────────────────────────
// Design Tokens
// ─────────────────────────────────────────────────────────────────────────────
private val BgDeep        = Color(0xFF060D1F)
private val BgCanvas      = Color(0xFF0B1525)
private val SurfaceGlass  = Color(0x14FFFFFF)
private val BorderGlass   = Color(0x26FFFFFF)
private val TextPrimary   = Color.White
private val TextSecondary = Color(0x99FFFFFF)
private val TextMuted     = Color(0x55FFFFFF)
private val GridMinor     = Color(0x0DFFFFFF)
private val GridMajor     = Color(0x22FFFFFF)

// RSSI colour gradient stops  (-100 dBm → 0 = deep red, -50 dBm → 1 = bright green)
private val HeatRed    = Color(0xFFFF1744)
private val HeatOrange = Color(0xFFFF6D00)
private val HeatYellow = Color(0xFFFFEA00)
private val HeatLime   = Color(0xFF76FF03)
private val HeatGreen  = Color(0xFF00E676)

/**
 * Maps an RSSI dBm value to a heatmap colour.
 * Calibrated for typical indoor WiFi:
 *   ≥ -50  = Excellent (green)
 *   -60    = Good      (lime)
 *   -70    = Fair      (yellow)
 *   -80    = Poor      (orange)
 *   ≤ -90  = Very poor (red)
 */
private fun rssiToColor(rssi: Int): Color {
    val t = ((rssi + 90f) / 40f).coerceIn(0f, 1f)  // 0 at -90, 1 at -50
    return when {
        t >= 0.75f -> lerp(HeatLime,   HeatGreen,  (t - 0.75f) / 0.25f)
        t >= 0.50f -> lerp(HeatYellow, HeatLime,   (t - 0.50f) / 0.25f)
        t >= 0.25f -> lerp(HeatOrange, HeatYellow, (t - 0.25f) / 0.25f)
        else       -> lerp(HeatRed,    HeatOrange,  t           / 0.25f)
    }
}

private fun rssiLabel(rssi: Int) = when {
    rssi >= -50 -> "Excellent"
    rssi >= -60 -> "Good"
    rssi >= -70 -> "Fair"
    rssi >= -80 -> "Poor"
    else        -> "Very Poor"
}

private fun lerp(a: Color, b: Color, t: Float): Color {
    val f = t.coerceIn(0f, 1f)
    return Color(a.red + (b.red - a.red) * f, a.green + (b.green - a.green) * f, a.blue + (b.blue - a.blue) * f, 1f)
}

/**
 * Inverse-Distance Weighting interpolation. Returns the estimated RSSI at
 * world position (qx, qy) given a list of samples, or null if no samples.
 */
private fun idwRssi(qx: Float, qy: Float, samples: List<WifiSample>, power: Float = 2f): Float? {
    if (samples.isEmpty()) return null
    var weightedSum = 0.0
    var totalWeight = 0.0
    for (s in samples) {
        val d = sqrt((qx - s.x).pow(2) + (qy - s.y).pow(2)).toDouble()
        if (d < 0.05) return s.rssi.toFloat()  // exact hit
        val w = 1.0 / d.pow(power.toDouble())
        weightedSum += w * s.rssi
        totalWeight += w
    }
    return if (totalWeight > 0.0) (weightedSum / totalWeight).toFloat() else null
}

// ─────────────────────────────────────────────────────────────────────────────
// Screen Entry
// ─────────────────────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WifiHeatmapScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("WiFi Signal Heatmap", fontWeight = FontWeight.Bold, color = TextPrimary, style = MaterialTheme.typography.titleLarge)
                        Text("Walk around to map signal strength", color = TextSecondary, style = MaterialTheme.typography.labelSmall)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Text("←", color = TextPrimary, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0x33000000))
            )
        },
        containerColor = BgDeep
    ) { pv ->
        Box(modifier = Modifier.fillMaxSize().background(BgDeep).padding(pv)) {
            PermGate(
                permission = android.Manifest.permission.ACCESS_FINE_LOCATION,
                rationale  = "Location permission is required on Android 8+ to read WiFi network information (SSID & RSSI)."
            ) {
                HeatmapContent()
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Main Content
// ─────────────────────────────────────────────────────────────────────────────
@SuppressLint("MissingPermission")
@Composable
private fun HeatmapContent() {
    val context = LocalContext.current
    val scope   = rememberCoroutineScope()

    var isMapping      by remember { mutableStateOf(false) }
    var strideLength   by remember { mutableFloatStateOf(0.65f) }
    var gridResolution by remember { mutableFloatStateOf(0.4f) }  // metres per heatmap cell

    // PDR position tracking
    var currentX   by remember { mutableFloatStateOf(0f) }
    var currentY   by remember { mutableFloatStateOf(0f) }
    var headingX   by remember { mutableFloatStateOf(0f) }
    var headingY   by remember { mutableFloatStateOf(1f) }  // facing North initially

    // Accumulated samples
    val samples = remember { mutableStateListOf<WifiSample>() }

    // Live WiFi status
    var liveRssi by remember { mutableIntStateOf(-100) }
    var liveSsid by remember { mutableStateOf("") }
    var stepsCount by remember { mutableIntStateOf(0) }

    // Canvas view state
    var scale     by remember { mutableFloatStateOf(70f) }
    var panOffset by remember { mutableStateOf(Offset.Zero) }

    // ── Sensor: rotation vector + step detector ────────────────────────────
    DisposableEffect(isMapping, strideLength) {
        if (!isMapping) return@DisposableEffect onDispose {}

        val sm        = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val rotMatrix = FloatArray(9)
        val orient    = FloatArray(3)

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                when (event.sensor.type) {
                    Sensor.TYPE_GAME_ROTATION_VECTOR -> {
                        SensorManager.getRotationMatrixFromVector(rotMatrix, event.values)
                        SensorManager.getOrientation(rotMatrix, orient)
                        val az = orient[0].toFloat()    // azimuth in radians
                        headingX = sin(az)
                        headingY = cos(az)
                    }
                    Sensor.TYPE_STEP_DETECTOR -> {
                        stepsCount++
                        currentX += headingX * strideLength
                        currentY += headingY * strideLength
                    }
                }
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        sm.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR)
            ?.let { sm.registerListener(listener, it, SensorManager.SENSOR_DELAY_GAME) }
        sm.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR)
            ?.let { sm.registerListener(listener, it, SensorManager.SENSOR_DELAY_FASTEST) }

        onDispose { sm.unregisterListener(listener) }
    }

    // ── WiFi RSSI polling loop ─────────────────────────────────────────────
    DisposableEffect(isMapping) {
        if (!isMapping) return@DisposableEffect onDispose {}

        @Suppress("DEPRECATION")
        val wm = context.getSystemService(Context.WIFI_SERVICE) as WifiManager

        val job = scope.launch {
            while (isActive) {
                @Suppress("DEPRECATION")
                val info = wm.connectionInfo
                val rssi = info?.rssi ?: -100
                @Suppress("DEPRECATION")
                val ssid = info?.ssid?.removeSurrounding("\"") ?: ""

                liveRssi = rssi
                liveSsid = ssid

                if (rssi > -100 && ssid.isNotEmpty()) {
                    val px = currentX
                    val py = currentY
                    samples.add(WifiSample(px, py, rssi, ssid))
                }
                delay(800L)   // sample every 0.8s — fast enough without hammering WiFi
            }
        }

        onDispose { job.cancel() }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {

        // ── Status Card ────────────────────────────────────────────────────
        HeatmapStatusCard(
            isMapping    = isMapping,
            ssid         = liveSsid,
            rssi         = liveRssi,
            sampleCount  = samples.size,
            steps        = stepsCount,
            posX         = currentX,
            posY         = currentY,
            onToggle = {
                if (isMapping) {
                    isMapping = false
                } else {
                    samples.clear()
                    currentX = 0f; currentY = 0f
                    stepsCount = 0
                    panOffset = Offset.Zero
                    isMapping = true
                }
            },
            onClear = {
                samples.clear()
                currentX = 0f; currentY = 0f; stepsCount = 0
            }
        )

        // ── Heatmap Canvas ─────────────────────────────────────────────────
        HeatmapCanvas(
            modifier       = Modifier.weight(1f).fillMaxWidth(),
            samples        = samples.toList(),
            currentX       = currentX,
            currentY       = currentY,
            isMapping      = isMapping,
            scale          = scale,
            panOffset      = panOffset,
            gridResolution = gridResolution,
            onScaleChange  = { scale = (scale * it).coerceIn(20f, 250f) },
            onPanChange    = { panOffset += it }
        )

        // ── Settings Card ──────────────────────────────────────────────────
        HeatmapSettingsCard(
            strideLength        = strideLength,
            onStrideLengthChange = { strideLength = it },
            gridResolution      = gridResolution,
            onGridResolutionChange = { gridResolution = it },
            onResetView         = { panOffset = Offset.Zero; scale = 70f }
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Status Card
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun HeatmapStatusCard(
    isMapping: Boolean,
    ssid: String,
    rssi: Int,
    sampleCount: Int,
    steps: Int,
    posX: Float,
    posY: Float,
    onToggle: () -> Unit,
    onClear: () -> Unit,
) {
    val rssiColor = if (rssi > -100) rssiToColor(rssi) else TextMuted

    GlassCard {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    // SSID + live RSSI bar
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            if (ssid.isNotEmpty()) "📶 $ssid" else if (isMapping) "No WiFi connected" else "IDLE",
                            color = if (ssid.isNotEmpty()) TextPrimary else TextMuted,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        // Live RSSI chip
                        Box(
                            modifier = Modifier
                                .border(BorderStroke(1.dp, rssiColor.copy(alpha = 0.5f)), RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                if (rssi > -100) "$rssi dBm · ${rssiLabel(rssi)}" else "—",
                                color = rssiColor,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        HeatMiniStat("Samples", "$sampleCount")
                        HeatMiniStat("Steps", "$steps")
                    }
                    if (isMapping) {
                        Text(
                            "Pos: X=%.1fm  Y=%.1fm".format(posX, posY),
                            color = TextMuted,
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (!isMapping && sampleCount > 0) {
                        OutlinedButton(
                            onClick = onClear,
                            border = BorderStroke(1.dp, HeatRed.copy(alpha = 0.6f)),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text("Clear", color = HeatRed, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        }
                    }
                    Button(
                        onClick = onToggle,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isMapping) HeatRed else HeatGreen,
                            contentColor   = Color.Black
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(if (isMapping) "Stop" else "Start Mapping", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun HeatMiniStat(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = TextPrimary, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
        Text(label, color = TextMuted, style = MaterialTheme.typography.labelSmall)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Heatmap Canvas
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun HeatmapCanvas(
    modifier: Modifier,
    samples: List<WifiSample>,
    currentX: Float,
    currentY: Float,
    isMapping: Boolean,
    scale: Float,
    panOffset: Offset,
    gridResolution: Float,
    onScaleChange: (Float) -> Unit,
    onPanChange: (Offset) -> Unit,
) {
    // Pulse animation for position marker
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue  = 0.7f,
        targetValue   = 1.3f,
        animationSpec = infiniteRepeatable(tween(900, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label         = "pulseScale"
    )

    Box(
        modifier = modifier
            .border(BorderStroke(1.dp, BorderGlass), RoundedCornerShape(24.dp))
            .clip(RoundedCornerShape(24.dp))
            .background(BgCanvas)
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    onPanChange(pan)
                    onScaleChange(zoom)
                }
            }
    ) {
        androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
            val originX = size.width  / 2f + panOffset.x
            val originY = size.height / 2f + panOffset.y

            // ── Cartesian grid ────────────────────────────────────────────
            val xStart = floor(-originX / scale).toInt() - 1
            val xEnd   = ceil((size.width - originX) / scale).toInt() + 1
            for (gx in xStart..xEnd) {
                val sx = originX + gx * scale
                val major = gx % 5 == 0
                drawLine(if (major) GridMajor else GridMinor, Offset(sx, 0f), Offset(sx, size.height), if (major) 1.dp.toPx() else 0.5.dp.toPx())
            }
            val yStart = floor(-(size.height - originY) / scale).toInt() - 1
            val yEnd   = ceil(originY / scale).toInt() + 1
            for (gy in yStart..yEnd) {
                val sy = originY - gy * scale
                val major = gy % 5 == 0
                drawLine(if (major) GridMajor else GridMinor, Offset(0f, sy), Offset(size.width, sy), if (major) 1.dp.toPx() else 0.5.dp.toPx())
            }

            // Grid labels
            val labelPaint = NativePaint().apply {
                color = android.graphics.Color.argb(65, 255, 255, 255)
                textSize = 22f; isAntiAlias = true
            }
            for (gx in xStart..xEnd) {
                if (gx % 5 == 0 && gx != 0) {
                    val sx = originX + gx * scale
                    if (sx in 0f..size.width) drawContext.canvas.nativeCanvas.drawText("${gx}m", sx + 4, originY - 4, labelPaint)
                }
            }
            for (gy in yStart..yEnd) {
                if (gy % 5 == 0 && gy != 0) {
                    val sy = originY - gy * scale
                    if (sy in 0f..size.height) drawContext.canvas.nativeCanvas.drawText("${gy}m", originX + 4, sy - 4, labelPaint)
                }
            }

            // Origin cross
            drawLine(GridMajor.copy(alpha = 0.55f), Offset(originX, 0f), Offset(originX, size.height), 1.dp.toPx())
            drawLine(GridMajor.copy(alpha = 0.55f), Offset(0f, originY), Offset(size.width, originY), 1.dp.toPx())

            // ── Heatmap grid cells (IDW interpolation) ────────────────────
            if (samples.size >= 2) {
                // Determine visible world bounding box
                val worldMinX = (-originX / scale) - gridResolution
                val worldMaxX = ((size.width - originX) / scale) + gridResolution
                val worldMinY = (-(size.height - originY) / scale) - gridResolution
                val worldMaxY = (originY / scale) + gridResolution

                // Only use nearby samples for speed (within 6m of each cell)
                val cellSizePx = gridResolution * scale

                var wx = worldMinX
                while (wx <= worldMaxX) {
                    var wy = worldMinY
                    while (wy <= worldMaxY) {
                        // Fast pre-filter: only IDW with samples within 5m
                        val nearby = samples.filter { s ->
                            abs(s.x - wx) < 5f && abs(s.y - wy) < 5f
                        }
                        if (nearby.isNotEmpty()) {
                            val interp = idwRssi(wx, wy, nearby) ?: run { wy += gridResolution; continue }
                            val col    = rssiToColor(interp.toInt())

                            val sx = originX + wx * scale
                            val sy = originY - wy * scale

                            drawRect(
                                color   = col.copy(alpha = 0.55f),
                                topLeft = Offset(sx - cellSizePx / 2f, sy - cellSizePx / 2f),
                                size    = Size(cellSizePx, cellSizePx)
                            )
                        }
                        wy += gridResolution
                    }
                    wx += gridResolution
                }
            }

            // ── Walk path ─────────────────────────────────────────────────
            if (samples.size >= 2) {
                for (i in 1 until samples.size) {
                    val a = samples[i - 1]; val b = samples[i]
                    drawLine(
                        color       = Color.White.copy(alpha = 0.25f),
                        start       = Offset(originX + a.x * scale, originY - a.y * scale),
                        end         = Offset(originX + b.x * scale, originY - b.y * scale),
                        strokeWidth = 1.5.dp.toPx()
                    )
                }
            }

            // ── Sample dots ───────────────────────────────────────────────
            samples.forEach { s ->
                val sx = originX + s.x * scale
                val sy = originY - s.y * scale
                if (sx < -10 || sx > size.width + 10 || sy < -10 || sy > size.height + 10) return@forEach
                val col = rssiToColor(s.rssi)
                drawCircle(col,                  radius = 3.dp.toPx(), center = Offset(sx, sy))
                drawCircle(col.copy(alpha = 0.3f), radius = 6.dp.toPx(), center = Offset(sx, sy))
            }

            // ── Current position marker ───────────────────────────────────
            val px = originX + currentX * scale
            val py = originY - currentY * scale
            if (px in -20f..(size.width + 20) && py in -20f..(size.height + 20)) {
                drawCircle(
                    color  = Color.White.copy(alpha = if (isMapping) 0.7f * pulseScale else 0.4f),
                    radius = 18.dp.toPx(),
                    center = Offset(px, py),
                    style  = Stroke(2.dp.toPx())
                )
                drawCircle(Color.White, radius = 6.dp.toPx(), center = Offset(px, py))
            }
        }

        // ── Idle hint ─────────────────────────────────────────────────────
        if (!isMapping && samples.isEmpty()) {
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("📶", fontSize = 44.sp)
                Text(
                    "Press Start Mapping\nthen walk around the room\nto build the signal heatmap",
                    color     = TextSecondary,
                    style     = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center
                )
            }
        }

        // ── RSSI Legend bar ───────────────────────────────────────────────
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
                .border(BorderStroke(1.dp, BorderGlass), RoundedCornerShape(12.dp))
                .drawBehind { drawRect(SurfaceGlass) }
                .padding(10.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text("Signal", color = TextSecondary, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                // Gradient bar
                Box(
                    modifier = Modifier
                        .width(16.dp)
                        .height(80.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(
                            Brush.verticalGradient(
                                listOf(HeatGreen, HeatLime, HeatYellow, HeatOrange, HeatRed)
                            )
                        )
                )
                Text("-50", color = HeatGreen,  style = MaterialTheme.typography.labelSmall)
                Text("dBm", color = TextMuted,  style = MaterialTheme.typography.labelSmall)
                Text("-90", color = HeatRed,    style = MaterialTheme.typography.labelSmall)
            }
        }

        // ── Drag / zoom hint ──────────────────────────────────────────────
        Row(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(12.dp)
                .border(BorderStroke(1.dp, BorderGlass), RoundedCornerShape(10.dp))
                .drawBehind { drawRect(SurfaceGlass) }
                .padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment     = Alignment.CenterVertically
        ) {
            Text("⬚ You", color = TextSecondary, style = MaterialTheme.typography.labelSmall)
            Text("Drag · Pinch to zoom", color = TextMuted, style = MaterialTheme.typography.labelSmall)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Settings Card
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun HeatmapSettingsCard(
    strideLength: Float,
    onStrideLengthChange: (Float) -> Unit,
    gridResolution: Float,
    onGridResolutionChange: (Float) -> Unit,
    onResetView: () -> Unit,
) {
    GlassCard {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Settings", color = TextPrimary, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
                TextButton(onClick = onResetView) {
                    Text("⌖ Reset View", color = TextSecondary, style = MaterialTheme.typography.labelMedium)
                }
            }

            // Stride length
            HeatSettingRow(
                label      = "Stride Length",
                subLabel   = "Metres per step (adjust to your walking pace)",
                valueText  = "%.2fm".format(strideLength),
                valueWidth = 52.dp,
                onMinus    = { onStrideLengthChange((strideLength - 0.05f).coerceAtLeast(0.3f)) },
                onPlus     = { onStrideLengthChange((strideLength + 0.05f).coerceAtMost(1.5f)) }
            )

            // Heatmap resolution
            HeatSettingRow(
                label      = "Cell Size",
                subLabel   = "Smaller = finer heatmap (slower to render)",
                valueText  = "%.1fm".format(gridResolution),
                valueWidth = 44.dp,
                onMinus    = { onGridResolutionChange((gridResolution - 0.1f).coerceAtLeast(0.2f)) },
                onPlus     = { onGridResolutionChange((gridResolution + 0.1f).coerceAtMost(1.0f)) }
            )
        }
    }
}

@Composable
private fun HeatSettingRow(
    label: String,
    subLabel: String,
    valueText: String,
    valueWidth: androidx.compose.ui.unit.Dp,
    onMinus: () -> Unit,
    onPlus: () -> Unit,
) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label,    color = TextSecondary, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
            Text(subLabel, color = TextMuted,     style = MaterialTheme.typography.labelSmall)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onMinus, contentPadding = PaddingValues(horizontal = 8.dp)) { Text("−", color = HeatGreen, fontSize = 20.sp) }
            Text(valueText, color = TextPrimary, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center, modifier = Modifier.width(valueWidth))
            TextButton(onClick = onPlus,  contentPadding = PaddingValues(horizontal = 8.dp)) { Text("+", color = HeatGreen, fontSize = 20.sp) }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Shared Glass Card
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun GlassCard(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .border(BorderStroke(1.dp, BorderGlass), RoundedCornerShape(20.dp))
            .drawBehind { drawRect(SurfaceGlass) }
            .padding(16.dp)
    ) { content() }
}
