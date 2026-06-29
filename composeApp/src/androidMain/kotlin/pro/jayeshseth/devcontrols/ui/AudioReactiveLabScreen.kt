package pro.jayeshseth.devcontrols.ui

import android.Manifest
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.coroutines.coroutineContext
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.ln
import kotlin.math.sin

private const val BANDS = 40
private const val FFT_SIZE = 1024

enum class VisualizerStyle {
    BARS, CIRCLE_WAVE, PARTICLE_ORBIT
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioReactiveLabScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var granted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted = it }
    var bands by remember { mutableStateOf(FloatArray(BANDS)) }
    var error by remember { mutableStateOf("") }
    var selectedStyle by remember { mutableStateOf(VisualizerStyle.BARS) }
    var simulateMode by remember { mutableStateOf(false) }

    // Simulated Beat Generator
    LaunchedEffect(simulateMode) {
        if (!simulateMode) return@LaunchedEffect
        var tick = 0f
        while (isActive) {
            val simulated = FloatArray(BANDS) { i ->
                // Bass frequencies (lower indices) react to a simulated heavy beat
                val base = if (i < 8) {
                    val beat = sin(tick * 0.2f) * 0.5f + 0.5f
                    if (beat > 0.8f) 0.9f else 0.2f
                } else {
                    0.1f
                }
                // High frequencies get wavy noise
                val noise = sin((tick + i) * 0.15f) * 0.3f + 0.3f
                (base + noise).coerceIn(0f, 1f)
            }
            bands = simulated
            tick += 1.2f
            kotlinx.coroutines.delay(25)
        }
    }

    // Real Audio Capture Loop
    LaunchedEffect(granted, simulateMode) {
        if (!granted || simulateMode) return@LaunchedEffect
        withContext(Dispatchers.Default) {
            val sampleRate = 44100
            val minBuf = AudioRecord.getMinBufferSize(
                sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT
            )
            val record = runCatching {
                AudioRecord(
                    MediaRecorder.AudioSource.MIC, sampleRate,
                    AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT,
                    maxOf(minBuf, FFT_SIZE * 2)
                )
            }.getOrElse { error = it.message ?: "AudioRecord init failed"; return@withContext }

            if (record.state != AudioRecord.STATE_INITIALIZED) {
                error = "AudioRecord not initialized"; record.release(); return@withContext
            }

            val window = FloatArray(FFT_SIZE) { 0.5f - 0.5f * cos(2.0 * Math.PI * it / (FFT_SIZE - 1)).toFloat() }
            val buf = ShortArray(FFT_SIZE)
            val re = FloatArray(FFT_SIZE)
            val im = FloatArray(FFT_SIZE)
            var smoothed = FloatArray(BANDS)

            runCatching {
                record.startRecording()
                while (coroutineContext.isActive && !simulateMode) {
                    var off = 0
                    while (off < FFT_SIZE) {
                        val n = record.read(buf, off, FFT_SIZE - off)
                        if (n <= 0) break
                        off += n
                    }
                    for (i in 0 until FFT_SIZE) { re[i] = (buf[i] / 32768f) * window[i]; im[i] = 0f }
                    fft(re, im)

                    val half = FFT_SIZE / 2
                    val raw = FloatArray(BANDS)
                    for (b in 0 until BANDS) {
                        val start = 1 + b * (half - 1) / BANDS
                        val end = 1 + (b + 1) * (half - 1) / BANDS
                        var sum = 0f; var c = 0; var k = start
                        while (k < end) { sum += hypot(re[k], im[k]); c++; k++ }
                        raw[b] = if (c > 0) ln(1f + sum / c) else 0f
                    }
                    val mx = raw.maxOrNull() ?: 0f
                    val norm = if (mx > 0.05f) FloatArray(BANDS) { raw[it] / mx } else FloatArray(BANDS)
                    smoothed = FloatArray(BANDS) { smoothed[it] * 0.65f + norm[it] * 0.35f }
                    bands = smoothed
                }
            }.also {
                runCatching { record.stop() }
                runCatching { record.release() }
            }
        }
    }

    // Peak fall logic for BARS style
    var peaks by remember { mutableStateOf(FloatArray(BANDS)) }
    LaunchedEffect(bands) {
        val nextPeaks = FloatArray(BANDS)
        for (i in 0 until BANDS) {
            val currentVal = bands.getOrElse(i) { 0f }
            val prevPeak = peaks.getOrElse(i) { 0f }
            nextPeaks[i] = maxOf(currentVal, prevPeak - 0.015f)
        }
        peaks = nextPeaks
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Audio Reactive Visualizer") },
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
        ) {
            if (!granted && !simulateMode) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "This visualizer listens through the microphone and runs an FFT to render a live audio spectrum.",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                        Button(
                            onClick = { launcher.launch(Manifest.permission.RECORD_AUDIO) },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text("Grant Microphone Permission")
                        }
                    }
                }
            }

            if (error.isNotEmpty()) {
                Text("Error: $error", color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(bottom = 8.dp))
            }

            // Visualizer Canvas Box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF0A0B10))
                    .drawBehind {
                        val current = bands
                        val n = current.size
                        val gap = 4f
                        val center = Offset(size.width / 2f, size.height / 2f)

                        when (selectedStyle) {
                            VisualizerStyle.BARS -> {
                                val barW = (size.width - gap * (n - 1)) / n
                                for (i in 0 until n) {
                                    val h = current[i] * (size.height * 0.8f)
                                    val x = i * (barW + gap)
                                    // Draw equalizing bars
                                    drawRoundRect(
                                        brush = Brush.verticalGradient(
                                            listOf(Color(0xFF18FFFF), Color(0xFF7F00FF)),
                                            startY = size.height - h, endY = size.height
                                        ),
                                        topLeft = Offset(x, size.height - h),
                                        size = Size(barW, h),
                                        cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
                                    )
                                    // Draw falling peaks
                                    val peakH = peaks[i] * (size.height * 0.8f)
                                    drawRect(
                                        color = Color(0xFF18FFFF).copy(alpha = 0.8f),
                                        topLeft = Offset(x, (size.height - peakH - 3.dp.toPx()).coerceAtLeast(0f)),
                                        size = Size(barW, 3.dp.toPx())
                                    )
                                }
                            }
                            VisualizerStyle.CIRCLE_WAVE -> {
                                val radius = size.width.coerceAtMost(size.height) * 0.22f
                                val path = Path()
                                val steps = 180
                                val angleStep = 360f / steps

                                for (step in 0..steps) {
                                    val angleDeg = step * angleStep
                                    val angleRad = Math.toRadians(angleDeg.toDouble())
                                    // Map step to frequency bands
                                    val bandIdx = (step % n)
                                    val intensity = current[bandIdx]
                                    val offsetRadius = radius + (intensity * 45f)

                                    val x = center.x + (offsetRadius * cos(angleRad)).toFloat()
                                    val y = center.y + (offsetRadius * sin(angleRad)).toFloat()

                                    if (step == 0) {
                                        path.moveTo(x, y)
                                    } else {
                                        path.lineTo(x, y)
                                    }
                                }
                                path.close()

                                // Draw circular neon glow
                                drawPath(
                                    path = path,
                                    brush = Brush.radialGradient(
                                        colors = listOf(Color(0xFF00FF87), Color(0xFF60EFFF)),
                                        center = center,
                                        radius = radius * 1.5f
                                    ),
                                    style = Stroke(width = 4.dp.toPx())
                                )
                            }
                            VisualizerStyle.PARTICLE_ORBIT -> {
                                // Draw a field of orbiting particles reacting to audio intensity
                                val avgIntensity = current.average().toFloat()
                                val baseRadius = size.width.coerceAtMost(size.height) * 0.2f
                                val activeRadius = baseRadius + (avgIntensity * 120f)

                                // Central core
                                drawCircle(
                                    brush = Brush.radialGradient(
                                        colors = listOf(Color(0xFFFF007F), Color.Transparent),
                                        center = center,
                                        radius = activeRadius * 0.5f
                                    ),
                                    radius = activeRadius * 0.5f
                                )

                                val numParticles = 60
                                for (i in 0 until numParticles) {
                                    val angle = (i * (360f / numParticles)) + (avgIntensity * 180f)
                                    val rad = Math.toRadians(angle.toDouble())
                                    // Individual particle offsets based on frequency bands
                                    val pRadius = activeRadius + (current[i % n] * 30f)
                                    val px = center.x + (pRadius * cos(rad)).toFloat()
                                    val py = center.y + (pRadius * sin(rad)).toFloat()

                                    drawCircle(
                                        color = Color(0xFFFFE000),
                                        radius = 3.dp.toPx() + (current[i % n] * 4.dp.toPx()),
                                        center = Offset(px, py)
                                    )
                                }
                            }
                        }
                    }
            )

            Spacer(Modifier.height(16.dp))

            // Configuration panel
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Style Selector
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Visualizer Style", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            VisualizerStyle.entries.forEach { style ->
                                FilterChip(
                                    selected = selectedStyle == style,
                                    onClick = { selectedStyle = style },
                                    label = {
                                        Text(
                                            when (style) {
                                                VisualizerStyle.BARS -> "Equalizer"
                                                VisualizerStyle.CIRCLE_WAVE -> "Circle Wave"
                                                VisualizerStyle.PARTICLE_ORBIT -> "Particle Orbit"
                                            }
                                        )
                                    }
                                )
                            }
                        }
                    }

                    // Simulation mode switch
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Simulate Beat", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                            Text("Generate simulated beats instead of using the microphone.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Spacer(Modifier.width(16.dp))
                        Switch(
                            checked = simulateMode,
                            onCheckedChange = { simulateMode = it }
                        )
                    }
                }
            }
        }
    }
}

private fun fft(re: FloatArray, im: FloatArray) {
    val n = re.size
    var j = 0
    for (i in 1 until n) {
        var bit = n shr 1
        while (j and bit != 0) { j = j xor bit; bit = bit shr 1 }
        j = j or bit
        if (i < j) {
            val tr = re[i]; re[i] = re[j]; re[j] = tr
            val ti = im[i]; im[i] = im[j]; im[j] = ti
        }
    }
    var len = 2
    while (len <= n) {
        val ang = -2.0 * Math.PI / len
        val wr = cos(ang).toFloat(); val wi = sin(ang).toFloat()
        var i = 0
        while (i < n) {
            var curR = 1f; var curI = 0f
            for (k in 0 until len / 2) {
                val aR = re[i + k]; val aI = im[i + k]
                val bR = re[i + k + len / 2]; val bI = im[i + k + len / 2]
                val tR = bR * curR - bI * curI; val tI = bR * curI + bI * curR
                re[i + k] = aR + tR; im[i + k] = aI + tI
                re[i + k + len / 2] = aR - tR; im[i + k + len / 2] = aI - tI
                val nR = curR * wr - curI * wi; curI = curR * wi + curI * wr; curR = nR
            }
            i += len
        }
        len = len shl 1
    }
}
