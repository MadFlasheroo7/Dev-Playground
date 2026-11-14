package pro.jayeshseth.audiostudio.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pro.jayeshseth.audiostudio.AudioMeters
import pro.jayeshseth.audiostudio.AudioStudioController
import pro.jayeshseth.audiostudio.AudioStudioSettings
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun GarageBandPedalScreen(
    controller: AudioStudioController,
    modifier: Modifier = Modifier
) {
    val isRunning by controller.isRunning.collectAsState()
    val isHeadphones by controller.isHeadphonesConnected.collectAsState()
    val settings by controller.settings.collectAsState()
    val meters by controller.meters.collectAsState()

    Surface(
        modifier = modifier.fillMaxSize(),
        color = Color(0xFF141418) // Vintage brushed dark aluminum
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header: Title & Route Status
            TopHeader(isRunning = isRunning, isHeadphones = isHeadphones)

            // VU Level Meters
            VuMeterRack(meters = meters)

            // GarageBand Stompbox Knobs Grid
            MacroKnobsRack(
                settings = settings,
                onUpdate = { transform -> controller.updateSettings(transform) }
            )

            // Bottom Stompbox Switch: Live Monitor Toggle
            StompboxSwitch(
                isRunning = isRunning,
                liveMonitor = settings.liveMonitor,
                onToggleMonitor = {
                    if (!isRunning) {
                        controller.start()
                    } else {
                        controller.updateSettings { it.copy(liveMonitor = !it.liveMonitor) }
                    }
                },
                onStop = { controller.stop() }
            )
        }
    }
}

@Composable
private fun TopHeader(isRunning: Boolean, isHeadphones: Boolean) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(if (isRunning) Color(0xFF00E676) else Color(0xFF555555))
            )
            Text(
                text = "AUDIO STUDIO BOOSTER",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp,
                fontFamily = FontFamily.Monospace
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Device route safety badge
        val badgeColor = if (isHeadphones) Color(0xFF00E5FF) else Color(0xFFFFB300)
        val routeText = if (isHeadphones) "HEADPHONES DETECTED - STUDIO ULTRA-LOW LATENCY" else "SPEAKER DETECTED - AUTO AEC & LIMITER ENGAGED"

        Surface(
            color = badgeColor.copy(alpha = 0.15f),
            shape = RoundedCornerShape(12.dp),
            border = borderStroke(badgeColor.copy(alpha = 0.4f))
        ) {
            Text(
                text = routeText,
                color = badgeColor,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

@Composable
private fun borderStroke(color: Color) = androidx.compose.foundation.BorderStroke(1.dp, color)

@Composable
private fun VuMeterRack(meters: AudioMeters) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .border(1.dp, Color(0xFF2E2E38), RoundedCornerShape(8.dp)),
        color = Color(0xFF0A0A0E)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            VuBar(label = "INPUT ", peak = meters.inPeak, rms = meters.inRms)
            VuBar(label = "OUTPUT", peak = meters.outPeak, rms = meters.outRms)
        }
    }
}

@Composable
private fun VuBar(label: String, peak: Float, rms: Float) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = label,
            color = Color(0xFF888899),
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold
        )

        val animatedRms by animateFloatAsState(
            targetValue = rms.coerceIn(0f, 1f),
            animationSpec = spring(stiffness = 800f),
            label = "rms"
        )
        val animatedPeak by animateFloatAsState(
            targetValue = peak.coerceIn(0f, 1f),
            animationSpec = spring(stiffness = 400f),
            label = "peak"
        )

        Canvas(
            modifier = Modifier
                .weight(1f)
                .height(14.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(Color(0xFF181822))
        ) {
            val width = size.width
            val height = size.height

            // RMS Bar Gradient (Green -> Yellow -> Red)
            val rmsWidth = width * animatedRms
            if (rmsWidth > 0f) {
                drawRect(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color(0xFF00E676),
                            Color(0xFFFFEA00),
                            Color(0xFFFF1744)
                        ),
                        startX = 0f,
                        endX = width
                    ),
                    size = Size(rmsWidth, height)
                )
            }

            // Peak Hold Marker Line
            val peakX = width * animatedPeak
            if (peakX > 0f) {
                drawLine(
                    color = if (animatedPeak > 0.95f) Color(0xFFFF1744) else Color.White,
                    start = Offset(peakX, 0f),
                    end = Offset(peakX, height),
                    strokeWidth = 2.dp.toPx()
                )
            }
        }
    }
}

@Composable
private fun MacroKnobsRack(
    settings: AudioStudioSettings,
    onUpdate: ((AudioStudioSettings) -> AudioStudioSettings) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            // Knob 1: Boost / Warmth (0 to 24 dB)
            TactileKnob(
                label = "BOOST",
                valueDisplay = "+${settings.boostDb.toInt()} dB",
                normalizedValue = settings.boostDb / 24f,
                accentColor = Color(0xFFFF5252),
                onValueChange = { norm ->
                    onUpdate { it.copy(boostDb = norm * 24f) }
                }
            )

            // Knob 2: Tone (-1.0 to 1.0)
            val toneLabel = when {
                settings.toneTilt < -0.2f -> "WARM"
                settings.toneTilt > 0.2f -> "BRIGHT"
                else -> "FLAT"
            }
            TactileKnob(
                label = "TONE",
                valueDisplay = toneLabel,
                normalizedValue = (settings.toneTilt + 1f) / 2f,
                accentColor = Color(0xFF40C4FF),
                onValueChange = { norm ->
                    onUpdate { it.copy(toneTilt = (norm * 2f) - 1f) }
                }
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            // Knob 3: Compressor (0.0 to 1.0)
            TactileKnob(
                label = "COMPRESS",
                valueDisplay = "${(settings.compIntensity * 100).toInt()}%",
                normalizedValue = settings.compIntensity,
                accentColor = Color(0xFFFFD740),
                onValueChange = { norm ->
                    onUpdate { it.copy(compIntensity = norm) }
                }
            )

            // Knob 4: Reverb / Space (0.0 to 1.0)
            TactileKnob(
                label = "REVERB",
                valueDisplay = "${(settings.reverbDepth * 100).toInt()}%",
                normalizedValue = settings.reverbDepth,
                accentColor = Color(0xFFE040FB),
                onValueChange = { norm ->
                    onUpdate { it.copy(reverbDepth = norm) }
                }
            )
        }
    }
}

@Composable
private fun TactileKnob(
    label: String,
    valueDisplay: String,
    normalizedValue: Float,
    accentColor: Color,
    onValueChange: (Float) -> Unit
) {
    var dragAccumulator by remember { mutableFloatStateOf(0f) }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            color = Color(0xFFCCCCCC),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )

        Spacer(modifier = Modifier.height(6.dp))

        Box(
            modifier = Modifier
                .size(80.dp)
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { dragAccumulator = normalizedValue },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            // Vertical drag: dragging up increases, down decreases
                            dragAccumulator = (dragAccumulator - (dragAmount.y / 200f)).coerceIn(0f, 1f)
                            onValueChange(dragAccumulator)
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val center = Offset(size.width / 2f, size.height / 2f)
                val radius = (size.minDimension / 2f) - 6.dp.toPx()

                // Background track arc (from 135 deg to 405 deg = 270 deg range)
                drawArc(
                    color = Color(0xFF262630),
                    startAngle = 135f,
                    sweepAngle = 270f,
                    useCenter = false,
                    topLeft = Offset(center.x - radius, center.y - radius),
                    size = Size(radius * 2f, radius * 2f),
                    style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round)
                )

                // Active glowing value arc
                val sweep = normalizedValue.coerceIn(0f, 1f) * 270f
                drawArc(
                    color = accentColor,
                    startAngle = 135f,
                    sweepAngle = sweep,
                    useCenter = false,
                    topLeft = Offset(center.x - radius, center.y - radius),
                    size = Size(radius * 2f, radius * 2f),
                    style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round)
                )

                // Inner metallic dial body
                val dialRadius = radius - 8.dp.toPx()
                drawCircle(
                    color = Color(0xFF1E1E26),
                    radius = dialRadius,
                    center = center
                )
                drawCircle(
                    color = Color(0xFF383848),
                    radius = dialRadius,
                    center = center,
                    style = Stroke(width = 1.5.dp.toPx())
                )

                // Indicator needle dot
                val currentAngleRad = Math.toRadians((135f + sweep).toDouble())
                val needleDistance = dialRadius * 0.7f
                val needleOffset = Offset(
                    x = center.x + (needleDistance * cos(currentAngleRad)).toFloat(),
                    y = center.y + (needleDistance * sin(currentAngleRad)).toFloat()
                )
                drawCircle(
                    color = accentColor,
                    radius = 3.5.dp.toPx(),
                    center = needleOffset
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = valueDisplay,
            color = accentColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
private fun StompboxSwitch(
    isRunning: Boolean,
    liveMonitor: Boolean,
    onToggleMonitor: () -> Unit,
    onStop: () -> Unit
) {
    val active = isRunning && liveMonitor
    val buttonColor by animateColorAsState(
        targetValue = if (active) Color(0xFF00E676) else Color(0xFF2A2A38),
        label = "stomp_color"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Metallic Stompbox footswitch button
        Box(
            modifier = Modifier
                .size(72.dp)
                .shadow(elevation = if (active) 16.dp else 4.dp, shape = CircleShape)
                .clip(CircleShape)
                .background(Color(0xFF3A3A48))
                .border(3.dp, Color(0xFF666678), CircleShape)
                .padding(6.dp)
                .clip(CircleShape)
                .background(buttonColor)
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDrag = { change, _ -> change.consume() }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            IconButton(
                onClick = onToggleMonitor,
                modifier = Modifier.fillMaxSize()
            ) {
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(if (active) Color.White else Color(0xFF141418))
                )
            }
        }

        Text(
            text = if (active) "LIVE MONITOR ON" else "MONITOR BYPASSED / OFF",
            color = if (active) Color(0xFF00E676) else Color(0xFF888899),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.5.sp,
            fontFamily = FontFamily.Monospace
        )

        if (isRunning) {
            TextButton(onClick = onStop) {
                Text(
                    text = "DISCONNECT AUDIO ENGINE",
                    color = Color(0xFFFF5252),
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}
