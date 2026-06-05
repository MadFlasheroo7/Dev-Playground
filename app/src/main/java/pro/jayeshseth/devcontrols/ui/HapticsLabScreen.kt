package pro.jayeshseth.devcontrols.ui

import android.os.Build
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import pro.jayeshseth.devcontrols.haptics.HapticsEngine
import pro.jayeshseth.devcontrols.haptics.HapticsPresets

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HapticsLabScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val view = LocalView.current
    val engine = remember { HapticsEngine(context) }

    var usage by remember { mutableStateOf(HapticsEngine.UsageDefaults.TOUCH) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Haptics Lab") },
                navigationIcon = {
                    TextButton(onClick = onBack) { Text("Back") }
                },
                actions = {
                    TextButton(onClick = { engine.cancelAll() }) { Text("STOP") }
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
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            CapabilitiesCard(engine)
            Spacer(Modifier.height(16.dp))

            UsagePicker(usage) { usage = it }
            Spacer(Modifier.height(16.dp))

            PredefinedEffectsCard(engine, usage)
            Spacer(Modifier.height(16.dp))

            PrimitivesCard(engine, usage)
            Spacer(Modifier.height(16.dp))

            OneShotCard(engine, usage)
            Spacer(Modifier.height(16.dp))

            WaveformBuilderCard(engine, usage)
            Spacer(Modifier.height(16.dp))

            CompositionBuilderCard(engine, usage)
            Spacer(Modifier.height(16.dp))

            HapticFeedbackConstantsCard(view)
            Spacer(Modifier.height(16.dp))

            PresetsCard(engine)
            Spacer(Modifier.height(16.dp))

            MultiActuatorCard(engine, usage)
            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun CapabilitiesCard(engine: HapticsEngine) {
    val effects = remember { engine.predefinedEffectSupport() }
    val primitives = remember { engine.primitiveSupport() }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Device capabilities", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(6.dp))
            InfoRow("hasVibrator", engine.hasVibrator.toString())
            InfoRow("hasAmplitudeControl", engine.hasAmplitudeControl.toString())
            InfoRow("vibrator IDs", engine.vibratorIds.toList().toString())
            Spacer(Modifier.height(6.dp))
            Text("Predefined effects", style = MaterialTheme.typography.titleSmall)
            HapticsEngine.Effects.all.forEach { (name, id) ->
                val s = effects[id] ?: HapticsEngine.Support.UNKNOWN
                val (label, color) = when (s) {
                    HapticsEngine.Support.YES -> "supported" to Color(0xFF1B6F3C)
                    HapticsEngine.Support.NO -> "not supported" to MaterialTheme.colorScheme.error
                    HapticsEngine.Support.UNKNOWN -> "unknown" to MaterialTheme.colorScheme.onSurfaceVariant
                }
                Row(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                    Text(name, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace)
                    Text(label, style = MaterialTheme.typography.bodySmall, color = color)
                }
            }
            Spacer(Modifier.height(6.dp))
            Text("Primitives (composition API)", style = MaterialTheme.typography.titleSmall)
            HapticsEngine.Primitives.all.forEach { (name, id) ->
                val supported = primitives[id] == true
                Row(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                    Text(name, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace)
                    Text(
                        if (supported) "supported" else "not supported",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (supported) Color(0xFF1B6F3C) else MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
private fun InfoRow(k: String, v: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Text(k, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
        Text(v, style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UsagePicker(current: Int, onChange: (Int) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val label = HapticsEngine.UsageDefaults.all.firstOrNull { it.second == current }?.first
        ?: "TOUCH"
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("VibrationAttributes.USAGE", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            Text(
                "Tells the system whether this is haptic feedback (always plays), a notification (subject to ring profile), an alarm (bypasses DND), etc.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            Box {
                OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
                    Text(label)
                }
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    HapticsEngine.UsageDefaults.all.forEach { (n, id) ->
                        DropdownMenuItem(
                            text = { Text(n) },
                            onClick = {
                                onChange(id); expanded = false
                            }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PredefinedEffectsCard(engine: HapticsEngine, usage: Int) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Predefined effects", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            Text(
                "Hardware-tuned per-vendor presets. Always crisp (or no-op) regardless of device.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(HapticsEngine.Effects.all) { (name, id) ->
                    AssistChip(
                        onClick = { engine.predefined(id, usage) },
                        label = { Text(name.removePrefix("EFFECT_")) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PrimitivesCard(engine: HapticsEngine, usage: Int) {
    val support = remember { engine.primitiveSupport() }
    var scale by remember { mutableStateOf(1.0f) }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Primitives (single-shot)", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            Text(
                "Composition-API primitives played one at a time with a scale slider. Combine many via the Composition Builder below.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            Text("Scale: ${"%.2f".format(scale)}", style = MaterialTheme.typography.bodySmall)
            Slider(value = scale, onValueChange = { scale = it }, valueRange = 0f..1f)
            Spacer(Modifier.height(4.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(HapticsEngine.Primitives.all) { (name, id) ->
                    val supported = support[id] == true
                    AssistChip(
                        onClick = {
                            engine.composition(
                                listOf(HapticsEngine.CompositionStep(id, scale, 0)),
                                usage
                            )
                        },
                        label = { Text(name.removePrefix("PRIMITIVE_")) },
                        enabled = supported
                    )
                }
            }
        }
    }
}

@Composable
private fun OneShotCard(engine: HapticsEngine, usage: Int) {
    var durationMs by remember { mutableStateOf(80f) }
    var amplitude by remember { mutableStateOf(160f) }
    var useDefaultAmp by remember { mutableStateOf(false) }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("One-shot", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            Text(
                "VibrationEffect.createOneShot(duration, amplitude). Amplitude is 1–255; toggle the switch to use DEFAULT_AMPLITUDE.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            Text("Duration: ${durationMs.toInt()} ms", style = MaterialTheme.typography.bodySmall)
            Slider(value = durationMs, onValueChange = { durationMs = it }, valueRange = 1f..5000f)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    if (useDefaultAmp) "Amplitude: DEFAULT" else "Amplitude: ${amplitude.toInt()}",
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodySmall
                )
                Switch(checked = useDefaultAmp, onCheckedChange = { useDefaultAmp = it })
            }
            if (!useDefaultAmp) {
                Slider(value = amplitude, onValueChange = { amplitude = it }, valueRange = 1f..255f)
            }
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = {
                    val amp = if (useDefaultAmp) android.os.VibrationEffect.DEFAULT_AMPLITUDE else amplitude.toInt()
                    engine.oneShot(durationMs.toLong(), amp, usage)
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Vibrate") }
        }
    }
}

@Composable
private fun WaveformBuilderCard(engine: HapticsEngine, usage: Int) {
    val segments = remember { mutableStateListOf(WaveSegment(50, 0), WaveSegment(120, 200), WaveSegment(80, 0), WaveSegment(120, 255)) }
    var repeatIdx by remember { mutableStateOf("-1") }
    var useAmplitudes by remember { mutableStateOf(true) }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Waveform builder", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            Text(
                "createWaveform(timings, amplitudes?, repeat). Each row is one segment. " +
                    "Without amplitudes the array alternates off/on starting with OFF.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Use per-segment amplitudes", modifier = Modifier.weight(1f))
                Switch(checked = useAmplitudes, onCheckedChange = { useAmplitudes = it })
            }
            Spacer(Modifier.height(4.dp))
            segments.forEachIndexed { i, seg ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text("#$i", modifier = Modifier.width(28.dp), style = MaterialTheme.typography.bodySmall)
                    OutlinedTextField(
                        value = seg.durationMs.toString(),
                        onValueChange = { txt -> segments[i] = seg.copy(durationMs = txt.toIntOrNull() ?: 0) },
                        label = { Text("ms") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = seg.amplitude.toString(),
                        onValueChange = { txt -> segments[i] = seg.copy(amplitude = txt.toIntOrNull() ?: 0) },
                        label = { Text("amp") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        enabled = useAmplitudes
                    )
                    TextButton(onClick = { segments.removeAt(i) }) { Text("✕") }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { segments.add(WaveSegment(100, 200)) }) { Text("Add segment") }
                OutlinedTextField(
                    value = repeatIdx,
                    onValueChange = { repeatIdx = it.filter { c -> c.isDigit() || c == '-' } },
                    label = { Text("repeat index (-1 = no)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        val timings = segments.map { it.durationMs.toLong().coerceAtLeast(0) }.toLongArray()
                        val r = repeatIdx.toIntOrNull() ?: -1
                        if (useAmplitudes) {
                            val amps = segments.map { it.amplitude.coerceIn(0, 255) }.toIntArray()
                            engine.waveform(timings, amps, r, usage)
                        } else {
                            engine.waveform(timings, null, r, usage)
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) { Text("Play") }
                OutlinedButton(onClick = { engine.cancel() }) { Text("Stop") }
            }
        }
    }
}

private data class WaveSegment(val durationMs: Int, val amplitude: Int)

@Composable
private fun CompositionBuilderCard(engine: HapticsEngine, usage: Int) {
    val support = remember { engine.primitiveSupport() }
    val available = remember { HapticsEngine.Primitives.all.filter { support[it.second] == true } }
    val steps = remember { mutableStateListOf<HapticsEngine.CompositionStep>() }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Composition builder", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            Text(
                "VibrationEffect.startComposition().addPrimitive(id, scale, delay). " +
                    "Tap a chip below to append a primitive at scale 1.0, delay 50 ms; edit values inline.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(available) { (name, id) ->
                    AssistChip(
                        onClick = {
                            steps.add(HapticsEngine.CompositionStep(id, 1.0f, 50))
                        },
                        label = { Text("+ " + name.removePrefix("PRIMITIVE_")) }
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            steps.forEachIndexed { i, step ->
                val name = HapticsEngine.Primitives.all.firstOrNull { it.second == step.primitiveId }?.first?.removePrefix("PRIMITIVE_") ?: "?"
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        "#$i $name",
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace
                    )
                    OutlinedTextField(
                        value = "%.2f".format(step.scale),
                        onValueChange = { txt ->
                            val v = txt.toFloatOrNull() ?: step.scale
                            steps[i] = step.copy(scale = v.coerceIn(0f, 1f))
                        },
                        label = { Text("scale") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.width(96.dp)
                    )
                    OutlinedTextField(
                        value = step.delayMs.toString(),
                        onValueChange = { txt ->
                            val v = txt.toIntOrNull() ?: step.delayMs
                            steps[i] = step.copy(delayMs = v.coerceAtLeast(0))
                        },
                        label = { Text("ms") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.width(80.dp)
                    )
                    TextButton(onClick = { steps.removeAt(i) }) { Text("✕") }
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { engine.composition(steps.toList(), usage) },
                    enabled = steps.isNotEmpty(),
                    modifier = Modifier.weight(1f)
                ) { Text("Compose & play") }
                OutlinedButton(onClick = { steps.clear() }) { Text("Clear") }
            }
        }
    }
}

@Composable
private fun HapticFeedbackConstantsCard(view: View) {
    val items = remember { buildHapticFeedbackConstants() }
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("HapticFeedbackConstants (View.performHapticFeedback)", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            Text(
                "These map onto vendor-tuned device haptics. Same set the platform uses for keyboards, gesture detection, toggles, etc.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(items) { (name, id) ->
                    AssistChip(
                        onClick = { view.performHapticFeedback(id) },
                        label = { Text(name) }
                    )
                }
            }
        }
    }
}

private fun buildHapticFeedbackConstants(): List<Pair<String, Int>> {
    val base = mutableListOf(
        "LONG_PRESS" to HapticFeedbackConstants.LONG_PRESS,
        "VIRTUAL_KEY" to HapticFeedbackConstants.VIRTUAL_KEY,
        "KEYBOARD_TAP" to HapticFeedbackConstants.KEYBOARD_TAP,
        "CLOCK_TICK" to HapticFeedbackConstants.CLOCK_TICK,
        "CONTEXT_CLICK" to HapticFeedbackConstants.CONTEXT_CLICK,
        "KEYBOARD_PRESS" to HapticFeedbackConstants.KEYBOARD_PRESS,
        "KEYBOARD_RELEASE" to HapticFeedbackConstants.KEYBOARD_RELEASE,
        "TEXT_HANDLE_MOVE" to HapticFeedbackConstants.TEXT_HANDLE_MOVE,
        "GESTURE_START" to HapticFeedbackConstants.GESTURE_START,
        "GESTURE_END" to HapticFeedbackConstants.GESTURE_END,
        "CONFIRM" to HapticFeedbackConstants.CONFIRM,
        "REJECT" to HapticFeedbackConstants.REJECT,
        "DRAG_START" to HapticFeedbackConstants.DRAG_START,
        "SEGMENT_TICK" to HapticFeedbackConstants.SEGMENT_TICK,
        "SEGMENT_FREQUENT_TICK" to HapticFeedbackConstants.SEGMENT_FREQUENT_TICK,
    )
    if (Build.VERSION.SDK_INT >= 34) {
        // TOGGLE_ON / TOGGLE_OFF added in API 34
        runCatching {
            val cls = HapticFeedbackConstants::class.java
            base += "TOGGLE_ON" to cls.getField("TOGGLE_ON").getInt(null)
            base += "TOGGLE_OFF" to cls.getField("TOGGLE_OFF").getInt(null)
        }
    }
    if (Build.VERSION.SDK_INT >= 35) {
        runCatching {
            val cls = HapticFeedbackConstants::class.java
            base += "GESTURE_THRESHOLD_ACTIVATE" to cls.getField("GESTURE_THRESHOLD_ACTIVATE").getInt(null)
            base += "GESTURE_THRESHOLD_DEACTIVATE" to cls.getField("GESTURE_THRESHOLD_DEACTIVATE").getInt(null)
        }
    }
    return base
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PresetsCard(engine: HapticsEngine) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Presets", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            Text(
                "Hand-tuned UX patterns + a few playful ones (heartbeat, morse SOS, drumroll, explosion).",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            HapticsPresets.all.forEach { preset ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(preset.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                        Text(
                            preset.subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    OutlinedButton(onClick = { preset.play(engine) }) { Text("Play") }
                }
                HorizontalDivider()
            }
        }
    }
}

@Composable
private fun MultiActuatorCard(engine: HapticsEngine, usage: Int) {
    val ids = remember { engine.vibratorIds.toList() }
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Multi-actuator", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            Text(
                "Devices like some Samsung S-series ship multiple vibration motors. Fire any one directly.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            if (ids.isEmpty()) {
                Text("No actuators reported.", style = MaterialTheme.typography.bodySmall)
            } else {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(ids) { id ->
                        AssistChip(
                            onClick = {
                                engine.vibrateOnActuator(
                                    id,
                                    android.os.VibrationEffect.createOneShot(80, 200),
                                    usage
                                )
                            },
                            label = { Text("ID $id") }
                        )
                    }
                }
            }
        }
    }
}
