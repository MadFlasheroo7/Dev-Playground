package pro.jayeshseth.devcontrols.ui.maps

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Paint as NativePaint
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
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
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
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

/**
 * A single wall hit at a given compass azimuth.
 * World coords: X = East (+right), Y = North (+up), origin = phone.
 */
data class PolarPoint(
    val azimuthDeg: Float,
    val distance: Float,
    val signalStrength: Float,
) {
    // North = 0°, clockwise positive (standard compass)
    // Convert to Cartesian: X = East, Y = North
    val x: Float = distance * sin(Math.toRadians(azimuthDeg.toDouble())).toFloat()
    val y: Float = distance * cos(Math.toRadians(azimuthDeg.toDouble())).toFloat()
}

enum class ChirpMode(val label: String, val f0: Float, val f1: Float) {
    ULTRASONIC("Stealth (18–21 kHz)", 18_000f, 21_000f),
    AUDIBLE("Audible (1.5–4.5 kHz)", 1_500f, 4_500f)
}

// ─────────────────────────────────────────────────────────────────────────────
// Design Tokens
// ─────────────────────────────────────────────────────────────────────────────
private val BgDeep        = Color(0xFF060D1F)
private val BgCanvas      = Color(0xFF0B1525)
private val SurfaceGlass  = Color(0x14FFFFFF)
private val BorderGlass   = Color(0x26FFFFFF)
private val AccentEmerald = Color(0xFF00FFCC)
private val AccentBlue    = Color(0xFF4FC3F7)
private val AccentAmber   = Color(0xFFFFAA00)
private val AccentRed     = Color(0xFFFF5370)
private val TextPrimary   = Color.White
private val TextSecondary = Color(0x99FFFFFF)
private val TextMuted     = Color(0x55FFFFFF)
private val GridMinor     = Color(0x0DFFFFFF)
private val GridMajor     = Color(0x22FFFFFF)

private fun lerpColor(a: Color, b: Color, t: Float): Color {
    val f = t.coerceIn(0f, 1f)
    return Color(a.red + (b.red - a.red) * f, a.green + (b.green - a.green) * f, a.blue + (b.blue - a.blue) * f, 1f)
}

// Angular gap threshold: gaps wider than this become room openings (doorways)
private const val GAP_THRESHOLD_DEG = 22f

// ─────────────────────────────────────────────────────────────────────────────
// Wall Segment Builder (gap-aware)
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Splits the polar point list into continuous wall segments. If consecutive
 * azimuths are more than [GAP_THRESHOLD_DEG] apart, that gap becomes a room
 * opening (e.g., a doorway or unmeasured region). Handles 360° wrap-around.
 */
private fun buildWallSegments(points: List<PolarPoint>): List<List<PolarPoint>> {
    if (points.size < 2) return emptyList()
    val sorted = points.sortedBy { it.azimuthDeg }
    val segs   = mutableListOf<MutableList<PolarPoint>>()
    var cur    = mutableListOf(sorted[0])

    for (i in 1 until sorted.size) {
        val gap = sorted[i].azimuthDeg - sorted[i - 1].azimuthDeg
        if (gap > GAP_THRESHOLD_DEG) {
            segs.add(cur)
            cur = mutableListOf(sorted[i])
        } else {
            cur.add(sorted[i])
        }
    }
    segs.add(cur)

    // Handle wrap-around (last → first across 360°)
    if (segs.size >= 2) {
        val lastAz  = segs.last().last().azimuthDeg
        val firstAz = segs.first().first().azimuthDeg + 360f
        if (firstAz - lastAz <= GAP_THRESHOLD_DEG) {
            val merged = (segs.removeLast() + segs.removeFirst()).toMutableList()
            segs.add(0, merged)
        }
    }
    return segs.filter { it.size >= 2 }
}

/** Convert world metres (East/North) to canvas pixels. Y is flipped (screen Y grows down). */
private fun worldToScreen(wx: Float, wy: Float, originX: Float, originY: Float, scale: Float) =
    Offset(originX + wx * scale, originY - wy * scale)

// ─────────────────────────────────────────────────────────────────────────────
// Screen Entry
// ─────────────────────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AcousticSlamScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Room Scanner", fontWeight = FontWeight.Bold, color = TextPrimary, style = MaterialTheme.typography.titleLarge)
                        Text("Acoustic 360° Floor Plan Mapper", color = TextSecondary, style = MaterialTheme.typography.labelSmall)
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
                permission = android.Manifest.permission.RECORD_AUDIO,
                rationale  = "Microphone access is required to capture acoustic echoes for room mapping."
            ) {
                RoomScannerContent()
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Main Content
// ─────────────────────────────────────────────────────────────────────────────
@SuppressLint("MissingPermission")
@Composable
private fun RoomScannerContent() {
    val context = LocalContext.current
    val scope   = rememberCoroutineScope()

    var isScanning     by remember { mutableStateOf(false) }
    var chirpMode      by remember { mutableStateOf(ChirpMode.ULTRASONIC) }
    var noiseThreshold by remember { mutableFloatStateOf(0.08f) }
    var maxRange       by remember { mutableFloatStateOf(8f) }

    // Azimuth from rotation vector sensor (0–360°, North=0, clockwise)
    var currentAzimuth by remember { mutableFloatStateOf(0f) }

    // Best wall hit per azimuth degree bucket
    val polarMap = remember { mutableStateMapOf<Int, PolarPoint>() }

    var chirpsCount  by remember { mutableIntStateOf(0) }
    var lastDistance by remember { mutableFloatStateOf(0f) }

    // Scale: pixels per metre. Pan: world-origin offset in screen pixels.
    var scale     by remember { mutableFloatStateOf(60f) }

    // ── Sensor ────────────────────────────────────────────────────────────────
    DisposableEffect(isScanning) {
        if (!isScanning) return@DisposableEffect onDispose {}

        val sm          = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val rotMatrix   = FloatArray(9)
        val orientation = FloatArray(3)

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                if (event.sensor.type == Sensor.TYPE_GAME_ROTATION_VECTOR) {
                    SensorManager.getRotationMatrixFromVector(rotMatrix, event.values)
                    SensorManager.getOrientation(rotMatrix, orientation)
                    val deg = Math.toDegrees(orientation[0].toDouble()).toFloat()
                    currentAzimuth = ((deg % 360) + 360) % 360
                }
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }
        sm.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR)
            ?.let { sm.registerListener(listener, it, SensorManager.SENSOR_DELAY_GAME) }

        onDispose { sm.unregisterListener(listener) }
    }

    // ── Audio Pipeline ────────────────────────────────────────────────────────
    DisposableEffect(isScanning, chirpMode, noiseThreshold, maxRange) {
        if (!isScanning) return@DisposableEffect onDispose {}

        val sampleRate      = 44_100
        val chirpDurationMs = 10
        val chirpSamples    = sampleRate * chirpDurationMs / 1_000

        val refChirp = FloatArray(chirpSamples) { i ->
            val t = i.toDouble() / sampleRate
            val k = (chirpMode.f1 - chirpMode.f0) / (chirpDurationMs / 1_000.0)
            sin(2.0 * PI * (chirpMode.f0 * t + 0.5 * k * t * t)).toFloat()
        }
        val refChirpShort = ShortArray(chirpSamples) { i ->
            (refChirp[i] * Short.MAX_VALUE).toInt().toShort()
        }

        val minBufPlay = AudioTrack.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT)
        val track = AudioTrack(
            AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build(),
            AudioFormat.Builder().setSampleRate(sampleRate).setChannelMask(AudioFormat.CHANNEL_OUT_MONO).setEncoding(AudioFormat.ENCODING_PCM_16BIT).build(),
            maxOf(minBufPlay, chirpSamples * 2), AudioTrack.MODE_STATIC, AudioManager.AUDIO_SESSION_ID_GENERATE
        )
        track.write(refChirpShort, 0, refChirpShort.size)

        val recordWindowSamples = 8_820
        val minBufRec = AudioRecord.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
        val record = AudioRecord(
            MediaRecorder.AudioSource.MIC, sampleRate,
            AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT,
            maxOf(minBufRec, recordWindowSamples * 2)
        )

        val job = scope.launch(Dispatchers.Default) {
            val recBuf = ShortArray(recordWindowSamples)
            record.startRecording()

            while (isActive) {
                track.stop(); track.play()
                val read = record.read(recBuf, 0, recBuf.size)
                if (read > 0) {
                    chirpsCount++
                    val recF = FloatArray(read) { i -> recBuf[i].toFloat() / Short.MAX_VALUE }

                    // Cross-correlation
                    val corr = FloatArray(read - chirpSamples + 1)
                    var maxC = 0f
                    for (k in corr.indices) {
                        var s = 0f
                        for (i in refChirp.indices) s += refChirp[i] * recF[k + i]
                        corr[k] = s
                        if (abs(s) > maxC) maxC = abs(s)
                    }
                    if (maxC > 0f) for (k in corr.indices) corr[k] /= maxC

                    // Direct bleed peak
                    var bleedIdx = 0; var bleedVal = 0f
                    for (k in 0 until minOf(600, corr.size)) {
                        val v = abs(corr[k]); if (v > bleedVal) { bleedVal = v; bleedIdx = k }
                    }

                    // Echo peak (after 88-sample blanking ≈ 34 cm)
                    var echoIdx = -1; var echoVal = 0f
                    val startS = bleedIdx + 88
                    if (startS < corr.size) {
                        for (k in startS until corr.size) {
                            val v = abs(corr[k])
                            if (v > echoVal && v > noiseThreshold) {
                                val prev = abs(corr[k - 1])
                                val next = if (k + 1 < corr.size) abs(corr[k + 1]) else 0f
                                if (v >= prev && v >= next) { echoVal = v; echoIdx = k }
                            }
                        }
                    }

                    if (echoIdx != -1) {
                        val dist = ((echoIdx - bleedIdx).toFloat() / sampleRate) * 343f / 2f
                        if (dist in 0.3f..maxRange) {
                            lastDistance = dist
                            val az     = currentAzimuth
                            val bucket = az.toInt() % 360
                            val pt     = PolarPoint(az, dist, echoVal)
                            withContext(Dispatchers.Main) {
                                val ex = polarMap[bucket]
                                if (ex == null || dist < ex.distance) polarMap[bucket] = pt
                            }
                        }
                    }
                }
                delay(150)
            }
        }

        onDispose {
            job.cancel()
            runCatching { record.stop(); record.release() }
            runCatching { track.stop(); track.release() }
        }
    }

    val coverage = (polarMap.size / 360f * 100f).coerceIn(0f, 100f)

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        ScanStatusPanel(
            isScanning     = isScanning,
            pointCount     = polarMap.size,
            coverage       = coverage,
            lastDist       = lastDistance,
            currentAzimuth = currentAzimuth,
            onToggle = {
                if (isScanning) {
                    isScanning = false
                } else {
                    polarMap.clear(); chirpsCount = 0; lastDistance = 0f; isScanning = true
                }
            },
            onClear = { polarMap.clear(); lastDistance = 0f }
        )

        FloorPlanCanvas(
            modifier       = Modifier.weight(1f).fillMaxWidth(),
            points         = polarMap.values.toList(),
            azimuth        = currentAzimuth,
            scale          = scale,
            isScanning     = isScanning,
            onScaleChange  = { scale = (scale * it).coerceIn(18f, 220f) }
        )

        ScanSettingsPanel(
            chirpMode         = chirpMode,
            onChirpModeChange = { chirpMode = it },
            noiseThreshold    = noiseThreshold,
            onNoiseChange     = { noiseThreshold = it },
            maxRange          = maxRange,
            onMaxRangeChange  = { maxRange = it }
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Status Panel
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun ScanStatusPanel(
    isScanning: Boolean,
    pointCount: Int,
    coverage: Float,
    lastDist: Float,
    currentAzimuth: Float,
    onToggle: () -> Unit,
    onClear: () -> Unit,
) {
    GlassCard {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        if (isScanning) "● SCANNING — Rotate phone 360°" else "IDLE — Ready to scan",
                        color = if (isScanning) AccentEmerald else TextMuted,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelLarge
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                        MiniStat("Points", "$pointCount")
                        MiniStat("Coverage", "%.0f%%".format(coverage))
                        MiniStat("Heading", "%.0f°".format(currentAzimuth))
                        if (lastDist > 0f) MiniStat("Last wall", "%.2fm".format(lastDist))
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (!isScanning && pointCount > 0) {
                        OutlinedButton(
                            onClick = onClear,
                            border = BorderStroke(1.dp, AccentRed.copy(alpha = 0.6f)),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text("Clear", color = AccentRed, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        }
                    }
                    Button(
                        onClick = onToggle,
                        colors = ButtonDefaults.buttonColors(containerColor = if (isScanning) AccentRed else AccentEmerald, contentColor = Color.Black),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(if (isScanning) "Stop" else "Start Scan", fontWeight = FontWeight.Bold)
                    }
                }
            }
            if (isScanning || coverage > 0f) {
                LinearProgressIndicator(
                    progress  = { (coverage / 100f).coerceIn(0f, 1f) },
                    modifier  = Modifier.fillMaxWidth().clip(RoundedCornerShape(4.dp)).height(4.dp),
                    color     = AccentEmerald,
                    trackColor = BorderGlass
                )
            }
        }
    }
}

@Composable
private fun MiniStat(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = TextPrimary, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
        Text(label, color = TextMuted, style = MaterialTheme.typography.labelSmall)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Floor Plan Canvas  (pan + zoom, Cartesian grid, gap-aware polygon)
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun FloorPlanCanvas(
    modifier: Modifier,
    points: List<PolarPoint>,
    azimuth: Float,
    scale: Float,
    isScanning: Boolean,
    onScaleChange: (Float) -> Unit,
) {
    // Pan offset in screen pixels (world origin may drift as user pans)
    var panOffset by remember { mutableStateOf(Offset.Zero) }

    // Smooth sweep line animation
    val animAzimuth by animateFloatAsState(
        targetValue   = azimuth,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label         = "sweepAngle"
    )

    // Pulsing glow on the phone marker
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue  = 0.25f,
        targetValue   = 0.75f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label         = "pulseAlpha"
    )

    // Room dimensions (Cartesian bounding box)
    val roomWidth  = if (points.size >= 5) (points.maxOf { it.x } - points.minOf { it.x }) else 0f
    val roomDepth  = if (points.size >= 5) (points.maxOf { it.y } - points.minOf { it.y }) else 0f

    Box(
        modifier = modifier
            .border(BorderStroke(1.dp, BorderGlass), RoundedCornerShape(24.dp))
            .clip(RoundedCornerShape(24.dp))
            .background(BgCanvas)
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    panOffset += pan
                    onScaleChange(zoom)
                }
            }
    ) {

        // ── Main Canvas ───────────────────────────────────────────────────────
        androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
            // World origin (phone position) in screen coordinates
            val originX = size.width  / 2f + panOffset.x
            val originY = size.height / 2f + panOffset.y

            // ─── Cartesian grid ────────────────────────────────────────────
            val gridStep = scale   // 1 metre per grid cell

            // Grid lines — vertical (constant world X)
            val xStart = floor(-originX / gridStep).toInt() - 1
            val xEnd   = ceil((size.width - originX) / gridStep).toInt() + 1
            for (gx in xStart..xEnd) {
                val sx = originX + gx * gridStep
                val isMajor = gx % 5 == 0
                drawLine(
                    color       = if (isMajor) GridMajor else GridMinor,
                    start       = Offset(sx, 0f),
                    end         = Offset(sx, size.height),
                    strokeWidth = if (isMajor) 1.dp.toPx() else 0.5.dp.toPx()
                )
            }

            // Grid lines — horizontal (constant world Y)
            val yStart = floor(-(size.height - originY) / gridStep).toInt() - 1
            val yEnd   = ceil(originY / gridStep).toInt() + 1
            for (gy in yStart..yEnd) {
                val sy = originY - gy * gridStep
                val isMajor = gy % 5 == 0
                drawLine(
                    color       = if (isMajor) GridMajor else GridMinor,
                    start       = Offset(0f, sy),
                    end         = Offset(size.width, sy),
                    strokeWidth = if (isMajor) 1.dp.toPx() else 0.5.dp.toPx()
                )
            }

            // Grid distance labels (on major lines, metres)
            val labelPaint = NativePaint().apply {
                color     = android.graphics.Color.argb(70, 255, 255, 255)
                textSize  = 22f
                isAntiAlias = true
            }
            for (gx in xStart..xEnd) {
                if (gx % 5 == 0 && gx != 0) {
                    val sx = originX + gx * gridStep
                    if (sx in 8f..(size.width - 8f)) {
                        drawContext.canvas.nativeCanvas.drawText(
                            "${gx}m", sx + 4, originY - 4, labelPaint
                        )
                    }
                }
            }
            for (gy in yStart..yEnd) {
                if (gy % 5 == 0 && gy != 0) {
                    val sy = originY - gy * gridStep
                    if (sy in 8f..(size.height - 8f)) {
                        drawContext.canvas.nativeCanvas.drawText(
                            "${gy}m", originX + 4, sy - 4, labelPaint
                        )
                    }
                }
            }

            // ─── Origin axis cross ─────────────────────────────────────────
            drawLine(GridMajor.copy(alpha = 0.5f), Offset(originX, 0f), Offset(originX, size.height), 1.dp.toPx())
            drawLine(GridMajor.copy(alpha = 0.5f), Offset(0f, originY), Offset(size.width, originY), 1.dp.toPx())

            // ─── Radar sweep (while scanning) ──────────────────────────────
            if (isScanning) {
                val sweepRad = Math.toRadians(animAzimuth.toDouble() - 90.0)
                val sweepLen = minOf(size.width, size.height) * 0.45f
                val sweepEnd = Offset(
                    originX + (sweepLen * cos(sweepRad)).toFloat(),
                    originY + (sweepLen * sin(sweepRad)).toFloat()
                )
                drawLine(
                    brush       = Brush.linearGradient(
                        listOf(AccentEmerald.copy(alpha = 0.9f), AccentEmerald.copy(alpha = 0f)),
                        start = Offset(originX, originY), end = sweepEnd
                    ),
                    start       = Offset(originX, originY),
                    end         = sweepEnd,
                    strokeWidth = 2.dp.toPx()
                )
            }

            // ─── Wall segments (gap-aware polygon) ────────────────────────
            val segments = buildWallSegments(points)
            val isClosed = segments.size == 1 && points.size >= 20

            segments.forEach { seg ->
                val screenPts = seg.map { pt -> worldToScreen(pt.x, pt.y, originX, originY, scale) }

                val wallPath = Path().apply {
                    moveTo(screenPts.first().x, screenPts.first().y)
                    screenPts.drop(1).forEach { lineTo(it.x, it.y) }
                    if (isClosed) close()
                }

                // Room fill (only when fully closed)
                if (isClosed) {
                    drawPath(
                        path  = wallPath,
                        brush = Brush.radialGradient(
                            listOf(AccentEmerald.copy(alpha = 0.07f), AccentBlue.copy(alpha = 0.03f), Color.Transparent),
                            center = Offset(originX, originY),
                            radius = scale * 6f
                        )
                    )
                }

                // Wall outline — solid, with rounded corners
                drawPath(
                    path  = wallPath,
                    color = AccentEmerald.copy(alpha = 0.85f),
                    style = Stroke(
                        width      = 2.5.dp.toPx(),
                        pathEffect = PathEffect.cornerPathEffect(8.dp.toPx())
                    )
                )

                // Opening end caps (dots at the ends of each non-closed segment)
                if (!isClosed) {
                    listOf(screenPts.first(), screenPts.last()).forEach { cap ->
                        drawCircle(AccentAmber.copy(alpha = 0.7f), radius = 5.dp.toPx(), center = cap)
                    }
                }
            }

            // ─── Measurement dots ──────────────────────────────────────────
            points.forEach { pt ->
                val sp = worldToScreen(pt.x, pt.y, originX, originY, scale)
                // Cull off-screen dots
                if (sp.x < -20 || sp.x > size.width + 20 || sp.y < -20 || sp.y > size.height + 20) return@forEach

                val distRatio = (pt.distance / maxOf(roomWidth, roomDepth, 1f)).coerceIn(0f, 1f)
                val col       = lerpColor(AccentEmerald, AccentBlue, distRatio)
                val alpha     = (pt.signalStrength * 1.8f).coerceIn(0.35f, 1f)

                drawCircle(col.copy(alpha = alpha),        radius = 3.5.dp.toPx(), center = sp)
                drawCircle(col.copy(alpha = alpha * 0.2f), radius = 7.dp.toPx(),   center = sp)
            }

            // ─── Phone origin marker ───────────────────────────────────────
            // Outer pulse ring
            drawCircle(
                color  = AccentAmber.copy(alpha = if (isScanning) pulseAlpha else 0.4f),
                radius = 18.dp.toPx(),
                center = Offset(originX, originY),
                style  = Stroke(2.dp.toPx())
            )
            // Solid centre dot
            drawCircle(AccentAmber, radius = 6.dp.toPx(), center = Offset(originX, originY))

            // Direction pointer
            if (isScanning) {
                val dirRad  = Math.toRadians(animAzimuth.toDouble() - 90.0)
                val arrowLen = 30.dp.toPx()
                drawLine(
                    color       = AccentAmber.copy(alpha = 0.9f),
                    start       = Offset(originX, originY),
                    end         = Offset(originX + (arrowLen * cos(dirRad)).toFloat(), originY + (arrowLen * sin(dirRad)).toFloat()),
                    strokeWidth = 2.5.dp.toPx()
                )
            }

            // ─── Compass rose (top-right corner, fixed in screen space) ───
            val compX = size.width - 36.dp.toPx()
            val compY = 36.dp.toPx()
            val compR = 14.dp.toPx()
            val northRad = Math.toRadians(animAzimuth.toDouble() - 90.0)
            // N arrow (red)
            drawLine(
                color       = AccentRed.copy(alpha = 0.9f),
                start       = Offset(compX, compY),
                end         = Offset(compX - (compR * cos(northRad)).toFloat(), compY - (compR * sin(northRad)).toFloat()),
                strokeWidth = 2.dp.toPx()
            )
            // S arrow (white)
            drawLine(
                color       = TextSecondary.copy(alpha = 0.5f),
                start       = Offset(compX, compY),
                end         = Offset(compX + (compR * cos(northRad)).toFloat(), compY + (compR * sin(northRad)).toFloat()),
                strokeWidth = 2.dp.toPx()
            )
            drawCircle(GridMajor, radius = compR + 2.dp.toPx(), center = Offset(compX, compY), style = Stroke(1.dp.toPx()))
            val compassPaint = NativePaint().apply {
                color = android.graphics.Color.argb(200, 255, 80, 80)
                textSize = 20f; textAlign = NativePaint.Align.CENTER; isAntiAlias = true
            }
            val nLabelX = compX - (compR * 1.6f * cos(northRad)).toFloat()
            val nLabelY = compY - (compR * 1.6f * sin(northRad)).toFloat() + 7f
            drawContext.canvas.nativeCanvas.drawText("N", nLabelX, nLabelY, compassPaint)
        }

        // ── Room Dimensions Overlay ───────────────────────────────────────────
        if (roomWidth > 0.5f && roomDepth > 0.5f) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(12.dp)
                    .border(BorderStroke(1.dp, BorderGlass), RoundedCornerShape(8.dp))
                    .drawBehind { drawRect(SurfaceGlass) }
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
                Column {
                    Text(
                        "~%.1fm × %.1fm".format(roomWidth, roomDepth),
                        color      = AccentEmerald,
                        style      = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "≈ %.0f m²".format(roomWidth * roomDepth),
                        color = TextSecondary,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }

        // ── Reset View Button ─────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(12.dp)
                .border(BorderStroke(1.dp, BorderGlass), RoundedCornerShape(8.dp))
                .drawBehind { drawRect(SurfaceGlass) }
                .clickable { panOffset = Offset.Zero }
                .padding(horizontal = 10.dp, vertical = 5.dp)
        ) {
            Text("⌖ Reset View", color = TextSecondary, style = MaterialTheme.typography.labelSmall)
        }

        // ── Idle hint ─────────────────────────────────────────────────────────
        if (!isScanning && points.isEmpty()) {
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("📡", fontSize = 44.sp)
                Text(
                    "Press Start Scan\nthen slowly rotate phone 360°\nto map the room walls",
                    color     = TextSecondary,
                    style     = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center
                )
            }
        }

        // ── Legend ────────────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(12.dp)
                .border(BorderStroke(1.dp, BorderGlass), RoundedCornerShape(12.dp))
                .drawBehind { drawRect(SurfaceGlass) }
                .padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment     = Alignment.CenterVertically
        ) {
            LegendDot(AccentAmber,        "You")
            LegendDot(AccentEmerald,      "Close wall")
            LegendDot(AccentBlue,         "Far wall")
            LegendDot(AccentAmber.copy(alpha=0.7f), "Opening")
            Text("Drag to pan · Pinch to zoom", color = TextMuted, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(Modifier.size(8.dp).background(color, CircleShape))
        Text(label, color = TextSecondary, style = MaterialTheme.typography.labelSmall)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Settings Panel
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun ScanSettingsPanel(
    chirpMode: ChirpMode,
    onChirpModeChange: (ChirpMode) -> Unit,
    noiseThreshold: Float,
    onNoiseChange: (Float) -> Unit,
    maxRange: Float,
    onMaxRangeChange: (Float) -> Unit,
) {
    GlassCard {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            // Signal mode
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Signal Mode", color = TextSecondary, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                Row(modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(Color(0x33000000)).padding(2.dp)) {
                    ChirpMode.values().forEach { mode ->
                        val active = chirpMode == mode
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (active) AccentEmerald else Color.Transparent)
                                .clickable { onChirpModeChange(mode) }
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(mode.label, color = if (active) Color.Black else TextSecondary, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Echo sensitivity
            SettingRow(
                label       = "Echo Sensitivity",
                subLabel    = "Lower = more sensitive (more noise)",
                valueText   = "%.2f".format(noiseThreshold),
                valueWidth  = 44.dp,
                onDecrement = { onNoiseChange((noiseThreshold - 0.01f).coerceAtLeast(0.01f)) },
                onIncrement = { onNoiseChange((noiseThreshold + 0.01f).coerceAtMost(0.50f)) }
            )

            // Max range
            SettingRow(
                label       = "Max Range",
                valueText   = "%.1fm".format(maxRange),
                valueWidth  = 52.dp,
                onDecrement = { onMaxRangeChange((maxRange - 0.5f).coerceAtLeast(1f)) },
                onIncrement = { onMaxRangeChange((maxRange + 0.5f).coerceAtMost(15f)) }
            )
        }
    }
}

@Composable
private fun SettingRow(
    label: String,
    subLabel: String = "",
    valueText: String,
    valueWidth: androidx.compose.ui.unit.Dp,
    onDecrement: () -> Unit,
    onIncrement: () -> Unit,
) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Column {
            Text(label, color = TextSecondary, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
            if (subLabel.isNotEmpty()) Text(subLabel, color = TextMuted, style = MaterialTheme.typography.labelSmall)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onDecrement, contentPadding = PaddingValues(horizontal = 8.dp)) { Text("−", color = AccentEmerald, fontSize = 20.sp) }
            Text(valueText, color = TextPrimary, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center, modifier = Modifier.width(valueWidth))
            TextButton(onClick = onIncrement, contentPadding = PaddingValues(horizontal = 8.dp)) { Text("+", color = AccentEmerald, fontSize = 20.sp) }
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
