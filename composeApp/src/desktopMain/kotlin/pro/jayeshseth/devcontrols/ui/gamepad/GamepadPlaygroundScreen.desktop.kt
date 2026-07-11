package pro.jayeshseth.devcontrols.ui.gamepad

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GamepadPlaygroundScreen(onBack: () -> Unit) {
    var state by remember { mutableStateOf(GamepadState()) }
    val mappings = remember { mutableStateListOf<ButtonMapping>().also { it.addAll(GamepadMappingStore.load()) } }

    fun triggerAction(keyCode: Int) {
        val mapping = mappings.firstOrNull { it.keyCode == keyCode } ?: return
        GamepadActions.execute(mapping.actionId)
    }

    // Linux Gamepad input listener loop (/dev/input/js0)
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            var fis: FileInputStream? = null
            while (isActive) {
                val jsFile = File("/dev/input/js0")
                if (jsFile.exists() && jsFile.canRead()) {
                    try {
                        state = state.copy(
                            isConnected = true,
                            deviceName = "Linux Controller (/dev/input/js0)",
                            connectionType = ConnectionType.USB_WIRED
                        )
                        fis = FileInputStream(jsFile)
                        val buffer = ByteArray(8)
                        while (isActive) {
                            val read = fis.read(buffer)
                            if (read == 8) {
                                val byteBuf = ByteBuffer.wrap(buffer).order(ByteOrder.LITTLE_ENDIAN)
                                val time = byteBuf.int
                                val value = byteBuf.short
                                val typeVal = byteBuf.get()
                                val number = byteBuf.get()

                                val type = typeVal.toInt() and 0x7F
                                val isInit = (typeVal.toInt() and 0x80) != 0

                                when (type) {
                                    1 -> { // Button
                                        val pressed = value.toInt() != 0
                                        state = when (number.toInt()) {
                                            0 -> state.copy(buttonA = pressed)
                                            1 -> state.copy(buttonB = pressed)
                                            2 -> state.copy(buttonX = pressed)
                                            3 -> state.copy(buttonY = pressed)
                                            4 -> state.copy(buttonL1 = pressed)
                                            5 -> state.copy(buttonR1 = pressed)
                                            6 -> state.copy(buttonSelect = pressed)
                                            7 -> state.copy(buttonStart = pressed)
                                            8 -> state.copy(buttonMode = pressed)
                                            9 -> state.copy(buttonL3 = pressed)
                                            10 -> state.copy(buttonR3 = pressed)
                                            else -> {
                                                val extras = state.extraButtons.toMutableMap()
                                                if (pressed) extras[number.toInt()] = true else extras.remove(number.toInt())
                                                state.copy(extraButtons = extras)
                                            }
                                        }
                                        if (pressed && !isInit) {
                                            val sType = when (number.toInt()) {
                                                0, 1, 2, 3 -> SoundType.MEMBRANE
                                                else -> SoundType.TACTILE
                                            }
                                            DesktopSoundManager.play(sType)
                                            triggerAction(number.toInt())
                                        }
                                    }
                                    2 -> { // Axis
                                        val floatVal = value.toFloat() / 32767f
                                        state = when (number.toInt()) {
                                            0 -> {
                                                val oldDist = Math.hypot(state.leftStickX.toDouble(), state.leftStickY.toDouble())
                                                val newDist = Math.hypot(floatVal.toDouble(), state.leftStickY.toDouble())
                                                if (oldDist < 0.96 && newDist >= 0.96) {
                                                    DesktopSoundManager.play(SoundType.STICK_GATE)
                                                } else if (newDist in 0.08..0.94) {
                                                    val oldStep = (oldDist / 0.16).toInt()
                                                    val newStep = (newDist / 0.16).toInt()
                                                    if (oldStep != newStep) DesktopSoundManager.play(SoundType.SLIDE_TICK)

                                                    val oldAngle = Math.atan2(state.leftStickY.toDouble(), state.leftStickX.toDouble())
                                                    val newAngle = Math.atan2(state.leftStickY.toDouble(), floatVal.toDouble())
                                                    val oldAngleStep = ((oldAngle + Math.PI) / (Math.PI / 6)).toInt()
                                                    val newAngleStep = ((newAngle + Math.PI) / (Math.PI / 6)).toInt()
                                                    if (oldAngleStep != newAngleStep && newDist > 0.25) {
                                                        DesktopSoundManager.play(SoundType.SLIDE_TICK)
                                                    }
                                                }
                                                state.copy(leftStickX = floatVal)
                                            }
                                            1 -> {
                                                val oldDist = Math.hypot(state.leftStickX.toDouble(), state.leftStickY.toDouble())
                                                val newDist = Math.hypot(state.leftStickX.toDouble(), floatVal.toDouble())
                                                if (oldDist < 0.96 && newDist >= 0.96) {
                                                    DesktopSoundManager.play(SoundType.STICK_GATE)
                                                } else if (newDist in 0.08..0.94) {
                                                    val oldStep = (oldDist / 0.16).toInt()
                                                    val newStep = (newDist / 0.16).toInt()
                                                    if (oldStep != newStep) DesktopSoundManager.play(SoundType.SLIDE_TICK)

                                                    val oldAngle = Math.atan2(state.leftStickY.toDouble(), state.leftStickX.toDouble())
                                                    val newAngle = Math.atan2(floatVal.toDouble(), state.leftStickX.toDouble())
                                                    val oldAngleStep = ((oldAngle + Math.PI) / (Math.PI / 6)).toInt()
                                                    val newAngleStep = ((newAngle + Math.PI) / (Math.PI / 6)).toInt()
                                                    if (oldAngleStep != newAngleStep && newDist > 0.25) {
                                                        DesktopSoundManager.play(SoundType.SLIDE_TICK)
                                                    }
                                                }
                                                state.copy(leftStickY = floatVal)
                                            }
                                            2 -> {
                                                val oldVal = state.l2Axis
                                                val newVal = (floatVal + 1f) / 2f
                                                if (oldVal < 0.95f && newVal >= 0.95f) {
                                                    DesktopSoundManager.play(SoundType.TRIGGER_BOTTOM)
                                                } else if (oldVal > 0.05f && newVal <= 0.05f) {
                                                    DesktopSoundManager.play(SoundType.TRIGGER_SNAP)
                                                } else if (newVal in 0.06f..0.94f) {
                                                    val oldStep = (oldVal * 8).toInt()
                                                    val newStep = (newVal * 8).toInt()
                                                    if (oldStep != newStep) DesktopSoundManager.play(SoundType.SLIDE_TICK)
                                                }
                                                state.copy(l2Axis = newVal, buttonL2 = floatVal > -0.5f)
                                            }
                                            3 -> {
                                                val oldDist = Math.hypot(state.rightStickX.toDouble(), state.rightStickY.toDouble())
                                                val newDist = Math.hypot(floatVal.toDouble(), state.rightStickY.toDouble())
                                                if (oldDist < 0.96 && newDist >= 0.96) {
                                                    DesktopSoundManager.play(SoundType.STICK_GATE)
                                                } else if (newDist in 0.08..0.94) {
                                                    val oldStep = (oldDist / 0.16).toInt()
                                                    val newStep = (newDist / 0.16).toInt()
                                                    if (oldStep != newStep) DesktopSoundManager.play(SoundType.SLIDE_TICK)

                                                    val oldAngle = Math.atan2(state.rightStickY.toDouble(), state.rightStickX.toDouble())
                                                    val newAngle = Math.atan2(state.rightStickY.toDouble(), floatVal.toDouble())
                                                    val oldAngleStep = ((oldAngle + Math.PI) / (Math.PI / 6)).toInt()
                                                    val newAngleStep = ((newAngle + Math.PI) / (Math.PI / 6)).toInt()
                                                    if (oldAngleStep != newAngleStep && newDist > 0.25) {
                                                        DesktopSoundManager.play(SoundType.SLIDE_TICK)
                                                    }
                                                }
                                                state.copy(rightStickX = floatVal)
                                            }
                                            4 -> {
                                                val oldDist = Math.hypot(state.rightStickX.toDouble(), state.rightStickY.toDouble())
                                                val newDist = Math.hypot(state.rightStickX.toDouble(), floatVal.toDouble())
                                                if (oldDist < 0.96 && newDist >= 0.96) {
                                                    DesktopSoundManager.play(SoundType.STICK_GATE)
                                                } else if (newDist in 0.08..0.94) {
                                                    val oldStep = (oldDist / 0.16).toInt()
                                                    val newStep = (newDist / 0.16).toInt()
                                                    if (oldStep != newStep) DesktopSoundManager.play(SoundType.SLIDE_TICK)

                                                    val oldAngle = Math.atan2(state.rightStickY.toDouble(), state.rightStickX.toDouble())
                                                    val newAngle = Math.atan2(floatVal.toDouble(), state.rightStickX.toDouble())
                                                    val oldAngleStep = ((oldAngle + Math.PI) / (Math.PI / 6)).toInt()
                                                    val newAngleStep = ((newAngle + Math.PI) / (Math.PI / 6)).toInt()
                                                    if (oldAngleStep != newAngleStep && newDist > 0.25) {
                                                        DesktopSoundManager.play(SoundType.SLIDE_TICK)
                                                    }
                                                }
                                                state.copy(rightStickY = floatVal)
                                            }
                                            5 -> {
                                                val oldVal = state.r2Axis
                                                val newVal = (floatVal + 1f) / 2f
                                                if (oldVal < 0.95f && newVal >= 0.95f) {
                                                    DesktopSoundManager.play(SoundType.TRIGGER_BOTTOM)
                                                } else if (oldVal > 0.05f && newVal <= 0.05f) {
                                                    DesktopSoundManager.play(SoundType.TRIGGER_SNAP)
                                                } else if (newVal in 0.06f..0.94f) {
                                                    val oldStep = (oldVal * 8).toInt()
                                                    val newStep = (newVal * 8).toInt()
                                                    if (oldStep != newStep) DesktopSoundManager.play(SoundType.SLIDE_TICK)
                                                }
                                                state.copy(r2Axis = newVal, buttonR2 = floatVal > -0.5f)
                                            }
                                            6 -> {
                                                val newValLeft = floatVal < -0.5f
                                                val newValRight = floatVal > 0.5f
                                                if ((newValLeft && !state.dpadLeft) || (newValRight && !state.dpadRight)) {
                                                    DesktopSoundManager.play(SoundType.TACTILE)
                                                }
                                                state.copy(dpadLeft = newValLeft, dpadRight = newValRight)
                                            }
                                            7 -> {
                                                val newValUp = floatVal < -0.5f
                                                val newValDown = floatVal > 0.5f
                                                if ((newValUp && !state.dpadUp) || (newValDown && !state.dpadDown)) {
                                                    DesktopSoundManager.play(SoundType.TACTILE)
                                                }
                                                state.copy(dpadUp = newValUp, dpadDown = newValDown)
                                            }
                                            else -> state
                                        }
                                    }
                                }
                            } else if (read == -1) {
                                break // EOF, device unplugged
                            }
                        }
                    } catch (e: Exception) {
                        state = GamepadState()
                    } finally {
                        runCatching { fis?.close() }
                    }
                } else {
                    state = GamepadState()
                }
                delay(1500)
            }
        }
    }

    var mappingTarget by remember { mutableStateOf<Pair<Int, String>?>(null) }
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Visual Pad", "State Monitor", "Mappings")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Gamepad Playground (Desktop)", fontWeight = FontWeight.Bold) },
                navigationIcon = { TextButton(onClick = onBack) { Text("Back") } },
            )
        },
        modifier = Modifier.fillMaxSize()
    ) { inner ->
        BoxWithConstraints(modifier = Modifier.padding(inner).fillMaxSize()) {
            val isWide = maxWidth > 760.dp

            if (isWide) {
                Row(modifier = Modifier.fillMaxSize()) {
                    Box(
                        modifier = Modifier
                            .weight(1.3f)
                            .fillMaxHeight()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column {
                            ConnectionBar(state)
                            Spacer(Modifier.height(8.dp))
                            VisualPadWidget(state = state)
                        }
                    }
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .fillMaxHeight()
                            .background(MaterialTheme.colorScheme.outlineVariant)
                    )
                    Column(
                        modifier = Modifier
                            .weight(0.9f)
                            .fillMaxHeight()
                    ) {
                        TabRow(selectedTabIndex = selectedTab) {
                            tabs.forEachIndexed { i, title ->
                                Tab(
                                    selected = selectedTab == i,
                                    onClick = { selectedTab = i },
                                    text = { Text(title) }
                                )
                            }
                        }
                        when (selectedTab) {
                            0 -> ActiveButtonsPanel(state = state, mappings = mappings, onMapRequest = { code, label -> mappingTarget = code to label })
                            1 -> StateMonitorTab(state = state)
                            2 -> MappingsTab(
                                state = state,
                                mappings = mappings,
                                onEdit = { code, label -> mappingTarget = code to label },
                                onDelete = { code ->
                                    mappings.removeAll { it.keyCode == code }
                                    GamepadMappingStore.remove(code)
                                }
                            )
                        }
                    }
                }
            } else {
                Column(modifier = Modifier.fillMaxSize()) {
                    ConnectionBar(state)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        VisualPadWidget(state = state)
                    }
                    TabRow(selectedTabIndex = selectedTab) {
                        tabs.forEachIndexed { i, title ->
                            Tab(
                                selected = selectedTab == i,
                                onClick = { selectedTab = i },
                                text = { Text(title) }
                            )
                        }
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1.2f)
                    ) {
                        when (selectedTab) {
                            0 -> ActiveButtonsPanel(state = state, mappings = mappings, onMapRequest = { code, label -> mappingTarget = code to label })
                            1 -> StateMonitorTab(state = state)
                            2 -> MappingsTab(
                                state = state,
                                mappings = mappings,
                                onEdit = { code, label -> mappingTarget = code to label },
                                onDelete = { code ->
                                    mappings.removeAll { it.keyCode == code }
                                    GamepadMappingStore.remove(code)
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    if (mappingTarget != null) {
        val (targetCode, targetLabel) = mappingTarget!!
        Dialog(onDismissRequest = { mappingTarget = null }) {
            Card(
                modifier = Modifier
                    .width(400.dp)
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                MappingDialogContent(
                    keyCode = targetCode,
                    keyLabel = targetLabel,
                    currentActionId = mappings.firstOrNull { it.keyCode == targetCode }?.actionId ?: "NONE",
                    onSave = { actionId ->
                        val m = ButtonMapping(targetCode, targetLabel, actionId)
                        val idx = mappings.indexOfFirst { it.keyCode == targetCode }
                        if (idx >= 0) mappings[idx] = m else mappings.add(m)
                        GamepadMappingStore.upsert(m)
                        mappingTarget = null
                    },
                    onDismiss = { mappingTarget = null }
                )
            }
        }
    }
}

@Composable
private fun ConnectionBar(state: GamepadState) {
    val dotColor by animateColorAsState(
        targetValue = if (state.isConnected) Color(0xFF1DB954) else MaterialTheme.colorScheme.error,
        label = "connColor"
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(Modifier.size(10.dp).clip(CircleShape).background(dotColor))
        Text(
            text = if (state.isConnected) state.deviceName else "No controller detected",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f)
        )
        if (state.isConnected) {
            Badge(containerColor = MaterialTheme.colorScheme.primaryContainer) {
                Text("USB / BT Controller", style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
private fun VisualPadWidget(state: GamepadState) {
    val infiniteTransition = rememberInfiniteTransition(label = "gradAnim")
    val hue by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(3000, easing = LinearEasing)),
        label = "hue"
    )

    fun pressedBrush(hueBase: Float): Brush = Brush.linearGradient(
        colors = listOf(
            Color.hsv((hue + hueBase) % 360f, 0.8f, 1f),
            Color.hsv((hue + hueBase + 120f) % 360f, 0.9f, 1f),
        )
    )

    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant
    val outlineColor = MaterialTheme.colorScheme.outline
    val textMeasurer = rememberTextMeasurer()

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1.6f)
            .clip(RoundedCornerShape(28.dp))
            .background(Color(0xFF0C0D0F))
            .border(1.5.dp, Color(0xFF1B1D22), RoundedCornerShape(28.dp))
    ) {
        val w = size.width
        val h = size.height

        // ── Glowing neon border ───────────────────────────────────────────
        val sweepBrush = Brush.sweepGradient(
            0.0f to Color.hsv(hue, 0.85f, 1f),
            0.3f to Color.hsv((hue + 120f) % 360f, 0.85f, 1f),
            0.6f to Color.hsv((hue + 240f) % 360f, 0.85f, 1f),
            1.0f to Color.hsv(hue, 0.85f, 1f),
            center = Offset(w / 2, h / 2)
        )

        val bodyPath = Path().apply {
            moveTo(w * 0.35f, h * 0.18f)
            lineTo(w * 0.65f, h * 0.18f)
            quadraticTo(w * 0.76f, h * 0.18f, w * 0.86f, h * 0.26f)
            quadraticTo(w * 0.96f, h * 0.38f, w * 0.94f, h * 0.65f)
            quadraticTo(w * 0.91f, h * 0.93f, w * 0.77f, h * 0.93f)
            quadraticTo(w * 0.68f, h * 0.82f, w * 0.61f, h * 0.72f)
            quadraticTo(w * 0.50f, h * 0.77f, w * 0.39f, h * 0.72f)
            quadraticTo(w * 0.32f, h * 0.82f, w * 0.23f, h * 0.93f)
            quadraticTo(w * 0.09f, h * 0.93f, w * 0.06f, h * 0.65f)
            quadraticTo(w * 0.04f, h * 0.38f, w * 0.14f, h * 0.26f)
            quadraticTo(w * 0.24f, h * 0.18f, w * 0.35f, h * 0.18f)
            close()
        }

        drawPath(path = bodyPath, brush = sweepBrush, style = Stroke(width = 8f))

        // Body backing (carbon/matte hardware gradient)
        val bodyGradient = Brush.linearGradient(
            colors = listOf(Color(0xFF2A2D35), Color(0xFF131417)),
            start = Offset(0f, 0f),
            end = Offset(0f, h)
        )
        drawPath(path = bodyPath, brush = bodyGradient)

        // Drop shadow bezel ring
        drawPath(path = bodyPath, color = Color.Black.copy(alpha = 0.45f), style = Stroke(width = 24f))

        // ── Xbox Guide Button (Glossy Pearl) ─────────────────────────────
        val guideCenter = Offset(w * 0.50f, h * 0.28f)
        val guideRadius = h * 0.072f
        drawCircle(
            brush = if (state.buttonMode) pressedBrush(240f) else Brush.radialGradient(
                colors = listOf(Color(0xFFFFFFFF), Color(0xFF9E9E9E), Color(0xFF424242)),
                center = guideCenter,
                radius = guideRadius
            ),
            radius = guideRadius,
            center = guideCenter
        )
        drawCircle(color = Color.Black.copy(alpha = 0.6f), radius = guideRadius, center = guideCenter, style = Stroke(3f))
        drawLabel(textMeasurer, "✖", guideCenter.x, guideCenter.y)

        // ── L2/R2 Mechanical Triggers ─────────────────────────────────────
        val l2TL = Offset(w * 0.08f, h * 0.02f)
        val l2Sz = Size(w * 0.12f, h * 0.14f)
        drawRoundRect(color = Color(0xFF15161A), topLeft = l2TL, size = l2Sz, cornerRadius = CornerRadius(10f))
        if (state.l2Axis > 0.01f) {
            drawRoundRect(
                brush = pressedBrush(30f),
                topLeft = l2TL,
                size = l2Sz.copy(width = l2Sz.width * state.l2Axis),
                cornerRadius = CornerRadius(10f)
            )
        }
        drawRoundRect(color = Color(0xFF2E303F), topLeft = l2TL, size = l2Sz, cornerRadius = CornerRadius(10f), style = Stroke(3f))
        drawLabel(textMeasurer, "LT", l2TL.x + l2Sz.width / 2, l2TL.y + l2Sz.height / 2)

        val r2Left = w * 0.80f
        val r2TL = Offset(r2Left, h * 0.02f)
        val r2Sz = Size(w * 0.12f, h * 0.14f)
        drawRoundRect(color = Color(0xFF15161A), topLeft = r2TL, size = r2Sz, cornerRadius = CornerRadius(10f))
        if (state.r2Axis > 0.01f) {
            val fillW = r2Sz.width * state.r2Axis
            drawRoundRect(
                brush = pressedBrush(30f),
                topLeft = Offset(r2Left + r2Sz.width - fillW, r2TL.y),
                size = r2Sz.copy(width = fillW),
                cornerRadius = CornerRadius(10f)
            )
        }
        drawRoundRect(color = Color(0xFF2E303F), topLeft = r2TL, size = r2Sz, cornerRadius = CornerRadius(10f), style = Stroke(3f))
        drawLabel(textMeasurer, "RT", r2TL.x + r2Sz.width / 2, r2TL.y + r2Sz.height / 2)

        // ── L1/R1 Hardware Bumpers ────────────────────────────────────────
        val l1TL = Offset(w * 0.09f, h * 0.18f)
        val l1Sz = Size(w * 0.13f, h * 0.08f)
        drawPadRect(if (state.buttonL1) pressedBrush(200f) else null, Color(0xFF2E3038), Color(0xFF4C4E58), l1TL, l1Sz, 12f)
        drawLabel(textMeasurer, "LB", l1TL.x + l1Sz.width / 2, l1TL.y + l1Sz.height / 2)

        val r1TL = Offset(w * 0.78f, h * 0.18f)
        val r1Sz = Size(w * 0.13f, h * 0.08f)
        drawPadRect(if (state.buttonR1) pressedBrush(200f) else null, Color(0xFF2E3038), Color(0xFF4C4E58), r1TL, r1Sz, 12f)
        drawLabel(textMeasurer, "RB", r1TL.x + r1Sz.width / 2, r1TL.y + r1Sz.height / 2)

        // ── 3D Concave Left Analog Stick ─────────────────────────────────
        val lsC = Offset(w * 0.28f, h * 0.40f)
        val lsR = h * 0.11f
        // Outer recess ring
        drawCircle(color = Color(0xFF0C0C0E), radius = lsR * 1.15f, center = lsC)
        drawCircle(color = Color(0xFF2D2E36), radius = lsR * 1.15f, center = lsC, style = Stroke(2.5f))
        // Stick body
        drawPadCircle(if (state.buttonL3) pressedBrush(60f) else null, Color(0xFF212328), Color(0xFF4C4E58), lsC, lsR)
        val lsDot = Offset(lsC.x + state.leftStickX * lsR * 0.58f, lsC.y + state.leftStickY * lsR * 0.58f)
        // Concave thumb grip
        drawCircle(color = Color(0xFF131417), radius = lsR * 0.5f, center = lsDot)
        drawCircle(color = Color.White.copy(alpha = 0.2f), radius = lsR * 0.5f, center = lsDot, style = Stroke(2f))
        drawLabel(textMeasurer, "L", lsDot.x, lsDot.y)

        // ── 3D Concave Right Analog Stick ────────────────────────────────
        val rsC = Offset(w * 0.61f, h * 0.62f)
        val rsR = h * 0.11f
        // Outer recess ring
        drawCircle(color = Color(0xFF0C0C0E), radius = rsR * 1.15f, center = rsC)
        drawCircle(color = Color(0xFF2D2E36), radius = rsR * 1.15f, center = rsC, style = Stroke(2.5f))
        // Stick body
        drawPadCircle(if (state.buttonR3) pressedBrush(60f) else null, Color(0xFF212328), Color(0xFF4C4E58), rsC, rsR)
        val rsDot = Offset(rsC.x + state.rightStickX * rsR * 0.58f, rsC.y + state.rightStickY * rsR * 0.58f)
        // Concave thumb grip
        drawCircle(color = Color(0xFF131417), radius = rsR * 0.5f, center = rsDot)
        drawCircle(color = Color.White.copy(alpha = 0.2f), radius = rsR * 0.5f, center = rsDot, style = Stroke(2f))
        drawLabel(textMeasurer, "R", rsDot.x, rsDot.y)

        // ── D-pad (Asymmetrical Circular Dish) ───────────────────────────
        val dcX = w * 0.38f
        val dcY = h * 0.62f
        val dishR = h * 0.11f
        drawCircle(color = Color(0xFF141518), radius = dishR, center = Offset(dcX, dcY))
        drawCircle(color = Color(0xFF2D2E36), radius = dishR, center = Offset(dcX, dcY), style = Stroke(2.5f))

        val arm = dishR * 0.85f
        val thick = dishR * 0.55f
        drawPadRect(
            brush = if (state.dpadLeft || state.dpadRight) pressedBrush(0f) else null,
            fallback = Color(0xFF2E3038), outline = Color(0xFF4D4F5A),
            topLeft = Offset(dcX - arm, dcY - thick / 2), size = Size(arm * 2, thick), corner = 6f
        )
        drawPadRect(
            brush = if (state.dpadUp || state.dpadDown) pressedBrush(0f) else null,
            fallback = Color(0xFF2E3038), outline = Color(0xFF4D4F5A),
            topLeft = Offset(dcX - thick / 2, dcY - arm), size = Size(thick, arm * 2), corner = 6f
        )
        drawLabel(textMeasurer, "◀", dcX - arm * 0.65f, dcY)
        drawLabel(textMeasurer, "▶", dcX + arm * 0.65f, dcY)
        drawLabel(textMeasurer, "▲", dcX, dcY - arm * 0.65f)
        drawLabel(textMeasurer, "▼", dcX, dcY + arm * 0.65f)

        // ── View / Menu Navigation Buttons ──────────────────────────────
        val viewTL = Offset(w * 0.43f, h * 0.40f)
        val menuTL = Offset(w * 0.53f, h * 0.40f)
        val midBtnSz = Size(w * 0.04f, h * 0.06f)
        drawPadRect(if (state.buttonSelect) pressedBrush(160f) else null, Color(0xFF2E2F38), Color(0xFF4C4D55), viewTL, midBtnSz, 6f)
        drawLabel(textMeasurer, "⧉", viewTL.x + midBtnSz.width / 2, viewTL.y + midBtnSz.height / 2)
        drawPadRect(if (state.buttonStart) pressedBrush(160f) else null, Color(0xFF2E2F38), Color(0xFF4C4D55), menuTL, midBtnSz, 6f)
        drawLabel(textMeasurer, "☰", menuTL.x + midBtnSz.width / 2, menuTL.y + midBtnSz.height / 2)

        // ── Glass/Gem Face Buttons (A B X Y) ──────────────────────────────
        val fcX = w * 0.72f
        val fcY = h * 0.40f
        val fcR = h * 0.065f
        val sep = fcR * 2.3f

        // B — Red Gem
        val bCenter = Offset(fcX + sep, fcY)
        drawCircle(color = Color(0x22FF5252), radius = fcR * 1.35f, center = bCenter)
        drawCircle(
            brush = if (state.buttonB) pressedBrush(0f) else Brush.radialGradient(
                colors = listOf(Color(0xFFFF8A80), Color(0xFFFF1744), Color(0xFF880E4F)),
                center = bCenter,
                radius = fcR
            ),
            radius = fcR,
            center = bCenter
        )
        drawLabel(textMeasurer, "B", bCenter.x, bCenter.y)

        // A — Green Gem
        val aCenter = Offset(fcX, fcY + sep)
        drawCircle(color = Color(0x2269F0AE), radius = fcR * 1.35f, center = aCenter)
        drawCircle(
            brush = if (state.buttonA) pressedBrush(120f) else Brush.radialGradient(
                colors = listOf(Color(0xFFB9F6CA), Color(0xFF00E676), Color(0xFF1B5E20)),
                center = aCenter,
                radius = fcR
            ),
            radius = fcR,
            center = aCenter
        )
        drawLabel(textMeasurer, "A", aCenter.x, aCenter.y)

        // X — Blue Gem
        val xCenter = Offset(fcX - sep, fcY)
        drawCircle(color = Color(0x2240C4FF), radius = fcR * 1.35f, center = xCenter)
        drawCircle(
            brush = if (state.buttonX) pressedBrush(220f) else Brush.radialGradient(
                colors = listOf(Color(0xFF80D8FF), Color(0xFF00B0FF), Color(0xFF0D47A1)),
                center = xCenter,
                radius = fcR
            ),
            radius = fcR,
            center = xCenter
        )
        drawLabel(textMeasurer, "X", xCenter.x, xCenter.y)

        // Y — Yellow Gem
        val yCenterBtn = Offset(fcX, fcY - sep)
        drawCircle(color = Color(0x22FFE57F), radius = fcR * 1.35f, center = yCenterBtn)
        drawCircle(
            brush = if (state.buttonY) pressedBrush(50f) else Brush.radialGradient(
                colors = listOf(Color(0xFFFFE57F), Color(0xFFFFD740), Color(0xFFF57F17)),
                center = yCenterBtn,
                radius = fcR
            ),
            radius = fcR,
            center = yCenterBtn
        )
        drawLabel(textMeasurer, "Y", yCenterBtn.x, yCenterBtn.y)
    }
}

@Composable
private fun ActiveButtonsPanel(
    state: GamepadState,
    mappings: List<ButtonMapping>,
    onMapRequest: (Int, String) -> Unit
) {
    val activeLabels = buildList {
        if (state.buttonA) add("A"); if (state.buttonB) add("B")
        if (state.buttonX) add("X"); if (state.buttonY) add("Y")
        if (state.buttonL1) add("LB"); if (state.buttonR1) add("RB")
        if (state.buttonL2) add("LT"); if (state.buttonR2) add("RT")
        if (state.buttonL3) add("L3"); if (state.buttonR3) add("R3")
        if (state.buttonStart) add("START"); if (state.buttonSelect) add("SEL")
        if (state.buttonMode) add("MODE")
        if (state.dpadUp) add("↑"); if (state.dpadDown) add("↓")
        if (state.dpadLeft) add("←"); if (state.dpadRight) add("→")
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Card(modifier = Modifier.fillMaxWidth()) {
            Row(
                Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text("Active:", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                if (activeLabels.isEmpty()) {
                    Text("No buttons pressed", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    activeLabels.forEach { label ->
                        Box(
                            Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer)
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                label,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }
        }

        if (state.extraButtons.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp)) {
                    Text("Macro / Extra Buttons", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    state.extraButtons.forEach { (code, down) ->
                        Row(
                            Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                Modifier.size(10.dp).clip(CircleShape)
                                    .background(if (down) Color(0xFF1DB954) else MaterialTheme.colorScheme.surfaceVariant)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "Button keycode: $code",
                                style = MaterialTheme.typography.bodyMedium,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.weight(1f)
                            )
                            TextButton(onClick = { onMapRequest(code, "Key_$code") }) { Text("Map Action") }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StateMonitorTab(state: GamepadState) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp)) {
                    Text("Connection", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    InfoRow("Device Name", if (state.isConnected) state.deviceName else "No controller detected")
                    InfoRow("Connection Type", state.connectionType.name)
                    InfoRow("Vendor ID", "0x${state.vendorId.toString(16).uppercase()}")
                    InfoRow("Product ID", "0x${state.productId.toString(16).uppercase()}")
                }
            }
        }
        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp)) {
                    Text("Buttons & Axes", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    InfoRow("Left Stick X", "%.3f".format(state.leftStickX))
                    InfoRow("Left Stick Y", "%.3f".format(state.leftStickY))
                    InfoRow("Right Stick X", "%.3f".format(state.rightStickX))
                    InfoRow("Right Stick Y", "%.3f".format(state.rightStickY))
                    InfoRow("Left Trigger (LT)", "%.3f".format(state.l2Axis))
                    InfoRow("Right Trigger (RT)", "%.3f".format(state.r2Axis))
                }
            }
        }
    }
}

@Composable
private fun InfoRow(key: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
        Text(key, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontFamily = FontFamily.Monospace, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun MappingsTab(
    state: GamepadState,
    mappings: List<ButtonMapping>,
    onEdit: (Int, String) -> Unit,
    onDelete: (Int) -> Unit,
) {
    val buttonsList = listOf(
        0 to "A Button",
        1 to "B Button",
        2 to "X Button",
        3 to "Y Button",
        4 to "LB (Left Bumper)",
        5 to "RB (Right Bumper)",
        6 to "Select Button",
        7 to "Start Button",
        8 to "Mode Button (Xbox Logo)",
        9 to "Left Stick click (L3)",
        10 to "Right Stick click (R3)"
    ) + state.extraButtons.keys.map { it to "Macro Button $it" }

    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 8.dp)) {
        items(buttonsList) { (code, label) ->
            val mapping = mappings.firstOrNull { it.keyCode == code }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(label, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                    Text(
                        mapping?.let { GamepadActions.all.firstOrNull { a -> a.id == it.actionId }?.label ?: it.actionId } ?: "No action mapped",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (mapping != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                TextButton(onClick = { onEdit(code, label) }) { Text("Map") }
                if (mapping != null) {
                    TextButton(onClick = { onDelete(code) }) { Text("✕") }
                }
            }
        }
    }
}

@Composable
private fun MappingDialogContent(
    keyCode: Int,
    keyLabel: String,
    currentActionId: String,
    onSave: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var selectedId by remember { mutableStateOf(currentActionId) }
    var dropdownOpen by remember { mutableStateOf(false) }
    val selectedAction = GamepadActions.all.firstOrNull { it.id == selectedId }

    Column(Modifier.padding(24.dp)) {
        Text("Map Controller Button", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        Text(keyLabel, style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(20.dp))
        Box {
            OutlinedButton(onClick = { dropdownOpen = true }, modifier = Modifier.fillMaxWidth()) {
                Text(selectedAction?.label ?: "Select action")
            }
            DropdownMenu(expanded = dropdownOpen, onDismissRequest = { dropdownOpen = false }) {
                GamepadActions.all.forEach { action ->
                    DropdownMenuItem(
                        text = { Text(action.label) },
                        onClick = { selectedId = action.id; dropdownOpen = false }
                    )
                }
            }
        }
        Spacer(Modifier.height(20.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text("Cancel") }
            Button(onClick = { onSave(selectedId) }, modifier = Modifier.weight(1f)) { Text("Save") }
        }
    }
}

private fun DrawScope.drawPadRect(
    brush: Brush?,
    fallback: Color,
    outline: Color,
    topLeft: Offset,
    size: Size,
    corner: Float,
) {
    drawRoundRect(color = fallback, topLeft = topLeft, size = size, cornerRadius = CornerRadius(corner))
    if (brush != null) {
        drawRoundRect(brush = brush, topLeft = topLeft, size = size, cornerRadius = CornerRadius(corner))
    }
    drawRoundRect(color = outline, topLeft = topLeft, size = size, cornerRadius = CornerRadius(corner), style = Stroke(2f))
}

private fun DrawScope.drawPadCircle(
    brush: Brush?,
    fallback: Color,
    outline: Color,
    center: Offset,
    radius: Float,
) {
    drawCircle(color = fallback, radius = radius, center = center)
    if (brush != null) {
        drawCircle(brush = brush, radius = radius, center = center)
    }
    drawCircle(color = outline, radius = radius, center = center, style = Stroke(2f))
}

private fun DrawScope.drawLabel(textMeasurer: androidx.compose.ui.text.TextMeasurer, text: String, cx: Float, cy: Float) {
    val textLayoutResult = textMeasurer.measure(
        text = text,
        style = TextStyle(
            color = Color.White,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.SansSerif
        )
    )
    drawText(
        textLayoutResult = textLayoutResult,
        topLeft = Offset(cx - textLayoutResult.size.width / 2, cy - textLayoutResult.size.height / 2)
    )
}

enum class SoundType {
    TACTILE,
    MEMBRANE,
    TRIGGER_BOTTOM,
    TRIGGER_SNAP,
    STICK_GATE,
    SLIDE_TICK
}

object DesktopSoundManager {
    fun play(type: SoundType) {
        Thread {
            try {
                val sampleRate = 44100f
                val durationMs = when (type) {
                    SoundType.TACTILE -> 20
                    SoundType.MEMBRANE -> 35
                    SoundType.TRIGGER_BOTTOM -> 40
                    SoundType.TRIGGER_SNAP -> 18
                    SoundType.STICK_GATE -> 12
                    SoundType.SLIDE_TICK -> 8
                }
                val numSamples = (sampleRate * (durationMs / 1000f)).toInt()
                val buffer = ByteArray(numSamples)
                
                val freq = when (type) {
                    SoundType.TACTILE -> 850.0
                    SoundType.MEMBRANE -> 180.0
                    SoundType.TRIGGER_BOTTOM -> 120.0
                    SoundType.TRIGGER_SNAP -> 680.0
                    SoundType.STICK_GATE -> 1300.0
                    SoundType.SLIDE_TICK -> 1600.0
                }
                
                val decay = when (type) {
                    SoundType.TACTILE -> 250.0
                    SoundType.MEMBRANE -> 90.0
                    SoundType.TRIGGER_BOTTOM -> 65.0
                    SoundType.TRIGGER_SNAP -> 380.0
                    SoundType.STICK_GATE -> 450.0
                    SoundType.SLIDE_TICK -> 600.0
                }
                
                val noiseLevel = when (type) {
                    SoundType.TACTILE -> 0.20
                    SoundType.MEMBRANE -> 0.0
                    SoundType.TRIGGER_BOTTOM -> 0.08
                    SoundType.TRIGGER_SNAP -> 0.15
                    SoundType.STICK_GATE -> 0.35
                    SoundType.SLIDE_TICK -> 0.10
                }
                
                val volume = when (type) {
                    SoundType.TACTILE -> 110.0
                    SoundType.MEMBRANE -> 100.0
                    SoundType.TRIGGER_BOTTOM -> 130.0
                    SoundType.TRIGGER_SNAP -> 70.0
                    SoundType.STICK_GATE -> 125.0
                    SoundType.SLIDE_TICK -> 25.0 // Very quiet slide click
                }
                
                for (i in 0 until numSamples) {
                    val t = i.toDouble() / sampleRate
                    val envelope = Math.exp(-t * decay)
                    val sine = Math.sin(2.0 * Math.PI * freq * t)
                    val noise = (Math.random() * 2.0 - 1.0) * noiseLevel
                    val value = ((sine + noise) * envelope * volume).toInt().coerceIn(-128, 127)
                    buffer[i] = value.toByte()
                }

                val format = javax.sound.sampled.AudioFormat(sampleRate, 8, 1, true, true)
                val info = javax.sound.sampled.DataLine.Info(javax.sound.sampled.SourceDataLine::class.java, format)
                val line = javax.sound.sampled.AudioSystem.getLine(info) as javax.sound.sampled.SourceDataLine
                line.open(format)
                line.start()
                line.write(buffer, 0, buffer.size)
                line.drain()
                line.close()
            } catch (e: Exception) {
                // Fail-safe system beep
                java.awt.Toolkit.getDefaultToolkit().beep()
            }
        }.start()
    }
}
