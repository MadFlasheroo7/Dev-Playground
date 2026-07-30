package pro.jayeshseth.devcontrols.ui

import android.media.audiofx.Equalizer
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import pro.jayeshseth.devcontrols.data.AudioEffectsPrefs
import pro.jayeshseth.devcontrols.service.AudioEffectsService

// ── EQ metadata ───────────────────────────────────────────────────────────────

data class EqMeta(
    val bands: Int,
    val minMb: Int,
    val maxMb: Int,
    /** Center frequencies in Hz for each band. */
    val centerFreqsHz: List<Int>,
    /** Device preset names returned by [Equalizer.getPresetName]. */
    val presetNames: List<String>
)

private val DEFAULT_EQ_META = EqMeta(
    bands = 5,
    minMb = -1500,
    maxMb = 1500,
    centerFreqsHz = listOf(60, 230, 910, 3600, 14000),
    presetNames = listOf("Normal", "Classical", "Dance", "Flat", "Folk", "Heavy Metal", "Hip Hop", "Jazz", "Pop", "Rock")
)

/** Reads EQ metadata from a throw-away [Equalizer] instance on an IO thread.
 *  Falls back to sensible defaults if the device doesn't support the API. */
@Composable
private fun rememberEqMeta(): EqMeta {
    val context = LocalContext.current
    var meta by remember { mutableStateOf(DEFAULT_EQ_META) }
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            runCatching {
                val am = context.getSystemService(android.content.Context.AUDIO_SERVICE) as android.media.AudioManager
                val sessionId = am.generateAudioSessionId()
                val eq = Equalizer(0, sessionId)
                val range = eq.bandLevelRange
                EqMeta(
                    bands = eq.numberOfBands.toInt(),
                    minMb = range[0].toInt(),
                    maxMb = range[1].toInt(),
                    centerFreqsHz = (0 until eq.numberOfBands)
                        .map { eq.getCenterFreq(it.toShort()) / 1000 },
                    presetNames = (0 until eq.numberOfPresets)
                        .map { eq.getPresetName(it.toShort()) }
                ).also { eq.release() }
            }.onSuccess { meta = it }
            .onFailure { e ->
                android.util.Log.e("AudioEffectsScreen", "Failed to query EqMeta", e)
            }
        }
    }
    return meta
}

private fun formatHz(hz: Int): String = if (hz >= 1000) "${hz / 1000}K" else "$hz"

// ── Screen ────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioEffectsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs = remember { AudioEffectsPrefs.get(context) }
    val eqMeta = rememberEqMeta()

    var serviceRunning by remember { mutableStateOf(AudioEffectsService.isRunning) }
    LaunchedEffect(Unit) {
        while (true) { serviceRunning = AudioEffectsService.isRunning; delay(500) }
    }

    val eqEnabled by prefs.eqEnabled.collectAsState(initial = true)
    val eqPreset by prefs.eqPreset.collectAsState(initial = -1)
    val savedBands by prefs.eqBandLevels.collectAsState(initial = emptyList())
    val bassEnabled by prefs.bassBoostEnabled.collectAsState(initial = true)
    val bassStrength by prefs.bassBoostStrength.collectAsState(initial = 400)
    val virtEnabled by prefs.virtualizerEnabled.collectAsState(initial = false)
    val virtStrength by prefs.virtualizerStrength.collectAsState(initial = 500)
    val loudEnabled by prefs.loudnessEnabled.collectAsState(initial = false)
    val loudGain by prefs.loudnessGain.collectAsState(initial = 300)
    val reverbEnabled by prefs.reverbEnabled.collectAsState(initial = false)
    val reverbPreset by prefs.reverbPreset.collectAsState(initial = 0)

    // Merge saved bands with default (all-zero flat) if sizes don't match
    val bandLevels = remember(savedBands, eqMeta) {
        if (savedBands.size == eqMeta.bands) savedBands else List(eqMeta.bands) { 0 }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Audio Effects") },
                navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }
            )
        }
    ) { inner ->
        Column(
            modifier = Modifier
                .padding(inner)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // ── Master control ────────────────────────────────────────────────
            MasterControlCard(
                isRunning = serviceRunning,
                onStart = { AudioEffectsService.start(context) },
                onStop  = { AudioEffectsService.stop(context) }
            )

            if (!serviceRunning) {
                Card(colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )) {
                    Text(
                        "⚠️  Start the service above — effects only apply while it's running.",
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }

            // ── Equalizer ─────────────────────────────────────────────────────
            EqualizerCard(
                enabled = eqEnabled,
                onEnabledChange = { scope.launch { prefs.setEqEnabled(it) } },
                meta = eqMeta,
                bandLevels = bandLevels,
                selectedPreset = eqPreset,
                onPresetSelect = { idx ->
                    scope.launch {
                        prefs.setEqPreset(idx)
                        // Read the actual band levels this preset sets so the slider UI updates too
                        withContext(Dispatchers.IO) {
                            runCatching {
                                val am = context.getSystemService(android.content.Context.AUDIO_SERVICE) as android.media.AudioManager
                                val sessionId = am.generateAudioSessionId()
                                val eq = Equalizer(0, sessionId)
                                eq.usePreset(idx.toShort())
                                val levels = (0 until eq.numberOfBands)
                                    .map { eq.getBandLevel(it.toShort()).toInt() }
                                eq.release()
                                prefs.setEqBandLevels(levels)
                            }
                        }.onFailure { e ->
                            android.util.Log.e("AudioEffectsScreen", "Failed to query preset levels", e)
                        }
                    }
                },
                onBandLevelChange = { band, level ->
                    val updated = bandLevels.toMutableList()
                    if (band < updated.size) updated[band] = level
                    scope.launch {
                        prefs.setEqBandLevels(updated)
                        if (eqPreset >= 0) prefs.setEqPreset(-1) // switch to Custom
                    }
                }
            )

            // ── Bass Boost ────────────────────────────────────────────────────
            EffectCard("🎸  Bass Boost", bassEnabled,
                { scope.launch { prefs.setBassBoostEnabled(it) } }) {
                EffectSlider(
                    label = "Strength",
                    displayValue = "${bassStrength / 10}%",
                    value = bassStrength / 1000f,
                    onValueChange = { scope.launch { prefs.setBassBoostStrength((it * 1000).toInt().coerceIn(0, 1000)) } }
                )
            }

            // ── Virtualizer ───────────────────────────────────────────────────
            EffectCard("🌐  Virtualizer", virtEnabled,
                { scope.launch { prefs.setVirtualizerEnabled(it) } }) {
                EffectSlider(
                    label = "Width",
                    displayValue = "${virtStrength / 10}%",
                    value = virtStrength / 1000f,
                    onValueChange = { scope.launch { prefs.setVirtualizerStrength((it * 1000).toInt().coerceIn(0, 1000)) } }
                )
            }

            // ── Loudness Enhancer ─────────────────────────────────────────────
            EffectCard("📢  Loudness Enhancer", loudEnabled,
                { scope.launch { prefs.setLoudnessEnabled(it) } }) {
                EffectSlider(
                    label = "Gain",
                    displayValue = "+${String.format("%.1f", loudGain / 100f)} dB",
                    value = loudGain / 1000f,
                    onValueChange = { scope.launch { prefs.setLoudnessGain((it * 1000).toInt().coerceIn(0, 1000)) } }
                )
            }

            // ── Reverb ────────────────────────────────────────────────────────
            val reverbNames = listOf("None", "Large Room", "Medium Room", "Small Room", "Large Hall", "Medium Hall", "Plate")
            EffectCard("🏛️  Reverb", reverbEnabled,
                { scope.launch { prefs.setReverbEnabled(it) } }) {
                ReverbPicker(
                    names = reverbNames,
                    selected = reverbPreset,
                    onSelect = { scope.launch { prefs.setReverbPreset(it) } }
                )
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

// ── Master card ───────────────────────────────────────────────────────────────

@Composable
private fun MasterControlCard(isRunning: Boolean, onStart: () -> Unit, onStop: () -> Unit) {
    val gradient = Brush.horizontalGradient(
        colors = listOf(Color(0xFF6650A4), Color(0xFF0288D1))
    )
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .background(gradient)
                .padding(20.dp)
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "System Audio Effects",
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier
                                .size(8.dp)
                                .background(
                                    if (isRunning) Color(0xFF69F0AE) else Color(0xFFFF5252),
                                    CircleShape
                                )
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            if (isRunning) "DSP chain active" else "Service stopped",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                }
                Button(
                    onClick = if (isRunning) onStop else onStart,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isRunning) Color.White.copy(alpha = 0.2f) else Color.White,
                        contentColor   = if (isRunning) Color.White else Color(0xFF6650A4)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(if (isRunning) "Stop" else "Start", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

// ── Equalizer card ────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EqualizerCard(
    enabled: Boolean,
    onEnabledChange: (Boolean) -> Unit,
    meta: EqMeta,
    bandLevels: List<Int>,
    selectedPreset: Int,
    onPresetSelect: (Int) -> Unit,
    onBandLevelChange: (band: Int, levelMb: Int) -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("🎛️  Equalizer", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Switch(checked = enabled, onCheckedChange = onEnabledChange)
            }

            AnimatedVisibility(enabled, enter = expandVertically(), exit = shrinkVertically()) {
                Column {
                    Spacer(Modifier.height(12.dp))

                    // Frequency response curve
                    EqCurveCanvas(
                        bandLevels = bandLevels,
                        minMb = meta.minMb,
                        maxMb = meta.maxMb,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(110.dp)
                    )

                    Spacer(Modifier.height(16.dp))

                    // Preset selector
                    if (meta.presetNames.isNotEmpty()) {
                        var expanded by remember { mutableStateOf(false) }
                        val label = if (selectedPreset < 0 || selectedPreset >= meta.presetNames.size)
                            "Custom" else meta.presetNames[selectedPreset]

                        ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                            OutlinedTextField(
                                value = label,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Preset") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                                modifier = Modifier.fillMaxWidth().menuAnchor()
                            )
                            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                                meta.presetNames.forEachIndexed { i, name ->
                                    DropdownMenuItem(
                                        text = { Text(name) },
                                        onClick = { onPresetSelect(i); expanded = false }
                                    )
                                }
                            }
                        }
                        Spacer(Modifier.height(16.dp))
                    }

                    // Per-band sliders
                    Text(
                        "Frequency Bands",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))

                    bandLevels.forEachIndexed { i, level ->
                        val freqHz = meta.centerFreqsHz.getOrElse(i) { 0 }
                        val dbValue = level / 100f // 100 mb ≈ 1 dB

                        Row(
                            Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Frequency label
                            Text(
                                formatHz(freqHz),
                                modifier = Modifier.width(36.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            // Slider
                            Slider(
                                value = level.toFloat(),
                                onValueChange = { onBandLevelChange(i, it.toInt()) },
                                valueRange = meta.minMb.toFloat()..meta.maxMb.toFloat(),
                                modifier = Modifier.weight(1f)
                            )
                            // dB label
                            Text(
                                "${if (dbValue >= 0) "+" else ""}${String.format("%.1f", dbValue)}",
                                modifier = Modifier.width(44.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = when {
                                    level > 50  -> MaterialTheme.colorScheme.primary
                                    level < -50 -> MaterialTheme.colorScheme.error
                                    else        -> MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

// ── EQ Frequency response canvas ──────────────────────────────────────────────

@Composable
private fun EqCurveCanvas(
    bandLevels: List<Int>,
    minMb: Int,
    maxMb: Int,
    modifier: Modifier = Modifier
) {
    val primary     = MaterialTheme.colorScheme.primary
    val surfaceVar  = MaterialTheme.colorScheme.surfaceVariant

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val n = bandLevels.size
        if (n == 0) return@Canvas

        fun levelToY(mb: Int): Float {
            val norm = (mb.coerceIn(minMb, maxMb) - minMb).toFloat() / (maxMb - minMb)
            return h * (1f - norm)
        }

        val zeroY = levelToY(0)
        val xs = if (n == 1) listOf(w / 2f)
                 else (0 until n).map { i -> i.toFloat() / (n - 1).toFloat() * w }
        val ys = bandLevels.map { levelToY(it) }

        // Background tint
        drawRect(color = surfaceVar.copy(alpha = 0.25f))

        // Zero-line grid
        drawLine(Color.White.copy(alpha = 0.12f), Offset(0f, zeroY), Offset(w, zeroY), 1.dp.toPx())

        // Build cubic bezier path through band points
        val curve = Path().apply {
            moveTo(xs[0], ys[0])
            for (i in 1 until n) {
                val cx = (xs[i - 1] + xs[i]) / 2f
                cubicTo(cx, ys[i - 1], cx, ys[i], xs[i], ys[i])
            }
        }

        // Fill gradient below curve
        val fill = Path().apply {
            addPath(curve)
            lineTo(xs.last(), h)
            lineTo(xs.first(), h)
            close()
        }
        drawPath(
            fill,
            Brush.verticalGradient(
                colors = listOf(primary.copy(alpha = 0.30f), Color.Transparent),
                startY = 0f, endY = h
            )
        )

        // Curve stroke
        drawPath(
            curve,
            color = primary,
            style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        // Control-point dots at each band
        xs.zip(ys).forEach { (x, y) ->
            drawCircle(primary.copy(alpha = 0.25f), radius = 9.dp.toPx(), center = Offset(x, y))
            drawCircle(primary,                    radius = 4.5.dp.toPx(), center = Offset(x, y))
            drawCircle(Color.White,                radius = 2.dp.toPx(),   center = Offset(x, y))
        }
    }
}

// ── Generic effect card ───────────────────────────────────────────────────────

@Composable
private fun EffectCard(
    title: String,
    enabled: Boolean,
    onEnabledChange: (Boolean) -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Switch(checked = enabled, onCheckedChange = onEnabledChange)
            }
            AnimatedVisibility(enabled, enter = expandVertically(), exit = shrinkVertically()) {
                Column {
                    Spacer(Modifier.height(12.dp))
                    content()
                }
            }
        }
    }
}

// ── Shared slider row ─────────────────────────────────────────────────────────

@Composable
private fun EffectSlider(label: String, displayValue: String, value: Float, onValueChange: (Float) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(displayValue, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
    }
    Spacer(Modifier.height(4.dp))
    Slider(value = value.coerceIn(0f, 1f), onValueChange = onValueChange)
}

// ── Reverb preset picker ──────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReverbPicker(names: List<String>, selected: Int, onSelect: (Int) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = names.getOrElse(selected) { "None" },
            onValueChange = {},
            readOnly = true,
            label = { Text("Room Preset") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            names.forEachIndexed { i, name ->
                DropdownMenuItem(text = { Text(name) }, onClick = { onSelect(i); expanded = false })
            }
        }
    }
}
