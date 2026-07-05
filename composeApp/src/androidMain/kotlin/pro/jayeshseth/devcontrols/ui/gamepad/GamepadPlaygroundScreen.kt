package pro.jayeshseth.devcontrols.ui.gamepad

import android.hardware.input.InputManager
import android.view.KeyEvent as AKeyEvent
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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

// ─────────────────────────────────────────────────────────────────────────────
// Entry point
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GamepadPlaygroundScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val inputManager = remember {
        context.getSystemService(android.content.Context.INPUT_SERVICE) as InputManager
    }
    val manager = remember { GamepadManager(inputManager) }
    DisposableEffect(manager) {
        GamepadManagerHolder.register(manager)
        onDispose {
            GamepadManagerHolder.unregister()
            manager.dispose()
        }
    }

    val state by manager.state.collectAsState()
    val rawKeys by manager.rawKeys.collectAsState()

    // Load persisted mappings into memory
    val mappings = remember { mutableStateListOf<ButtonMapping>().also { it.addAll(GamepadMappingStore.load()) } }

    // Execute mapped action on key down
    LaunchedEffect(rawKeys) {
        val (code, down) = rawKeys ?: return@LaunchedEffect
        if (!down) return@LaunchedEffect
        val mapping = mappings.firstOrNull { it.keyCode == code } ?: return@LaunchedEffect
        GamepadActions.execute(mapping.actionId, context)
    }

    // Mapping bottom sheet
    var mappingTarget by remember { mutableStateOf<Pair<Int, String>?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Visual Pad", "State Monitor", "Mappings")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Gamepad Playground") },
                navigationIcon = { TextButton(onClick = onBack) { Text("Back") } },
            )
        },
        modifier = Modifier.fillMaxSize()
    ) { inner ->
        Column(Modifier.padding(inner).fillMaxSize()) {
            ConnectionBar(state)

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
                0 -> VisualPadTab(
                    state = state,
                    mappings = mappings,
                    onMapRequest = { code, label -> mappingTarget = code to label }
                )
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

    if (mappingTarget != null) {
        val (targetCode, targetLabel) = mappingTarget!!
        ModalBottomSheet(
            onDismissRequest = { mappingTarget = null },
            sheetState = sheetState
        ) {
            MappingSheet(
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

// ─────────────────────────────────────────────────────────────────────────────
// Connection bar
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ConnectionBar(state: GamepadState) {
    val dotColor by animateColorAsState(
        targetValue = if (state.isConnected) Color(0xFF1DB954) else MaterialTheme.colorScheme.error,
        label = "connColor"
    )
    val typeLabel = when (state.connectionType) {
        ConnectionType.BLUETOOTH  -> "Bluetooth"
        ConnectionType.USB_DONGLE -> "USB Dongle"
        ConnectionType.USB_WIRED  -> "USB Wired"
        ConnectionType.NONE       -> "—"
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant)
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
                Text(typeLabel, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// TAB 1 — Visual Gamepad Silhouette
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun VisualPadTab(
    state: GamepadState,
    mappings: List<ButtonMapping>,
    onMapRequest: (Int, String) -> Unit,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "gradAnim")
    val hue by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(2000, easing = LinearEasing)),
        label = "hue"
    )

    fun pressedBrush(hueBase: Float): Brush = Brush.linearGradient(
        colors = listOf(
            Color.hsv((hue + hueBase) % 360f, 0.85f, 1f),
            Color.hsv((hue + hueBase + 120f) % 360f, 0.90f, 1f),
        )
    )

    val surfaceColor = MaterialTheme.colorScheme.surface
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant
    val outlineColor = MaterialTheme.colorScheme.outline

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // ── Canvas silhouette ─────────────────────────────────────────────
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1.75f)
                .clip(RoundedCornerShape(20.dp))
                .background(MaterialTheme.colorScheme.surface)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(20.dp))
        ) {
            val w = size.width
            val h = size.height

            // Body
            drawRoundRect(
                color = surfaceVariant,
                topLeft = Offset(w * 0.05f, h * 0.18f),
                size = Size(w * 0.90f, h * 0.70f),
                cornerRadius = CornerRadius(60f, 60f)
            )
            // Left grip bulge
            drawRoundRect(
                color = surfaceVariant,
                topLeft = Offset(w * 0.06f, h * 0.55f),
                size = Size(w * 0.22f, h * 0.40f),
                cornerRadius = CornerRadius(40f, 40f)
            )
            // Right grip bulge
            drawRoundRect(
                color = surfaceVariant,
                topLeft = Offset(w * 0.72f, h * 0.55f),
                size = Size(w * 0.22f, h * 0.40f),
                cornerRadius = CornerRadius(40f, 40f)
            )

            // ── L2 trigger fill bar ───────────────────────────────────────
            val l2TL = Offset(w * 0.07f, h * 0.02f)
            val l2Sz = Size(w * 0.14f, h * 0.13f)
            drawRoundRect(color = Color(0x22FF6600), topLeft = l2TL, size = l2Sz, cornerRadius = CornerRadius(12f))
            if (state.l2Axis > 0.01f) {
                drawRoundRect(
                    brush = pressedBrush(30f),
                    topLeft = l2TL,
                    size = l2Sz.copy(width = l2Sz.width * state.l2Axis),
                    cornerRadius = CornerRadius(12f)
                )
            }
            drawRoundRect(color = outlineColor, topLeft = l2TL, size = l2Sz, cornerRadius = CornerRadius(12f), style = Stroke(2f))
            drawLabel("L2", l2TL.x + l2Sz.width / 2, l2TL.y + l2Sz.height / 2)

            // ── R2 trigger fill bar ───────────────────────────────────────
            val r2Left = w * 0.79f
            val r2TL = Offset(r2Left, h * 0.02f)
            val r2Sz = Size(w * 0.14f, h * 0.13f)
            drawRoundRect(color = Color(0x22FF6600), topLeft = r2TL, size = r2Sz, cornerRadius = CornerRadius(12f))
            if (state.r2Axis > 0.01f) {
                val fillW = r2Sz.width * state.r2Axis
                drawRoundRect(
                    brush = pressedBrush(30f),
                    topLeft = Offset(r2Left + r2Sz.width - fillW, r2TL.y),
                    size = r2Sz.copy(width = fillW),
                    cornerRadius = CornerRadius(12f)
                )
            }
            drawRoundRect(color = outlineColor, topLeft = r2TL, size = r2Sz, cornerRadius = CornerRadius(12f), style = Stroke(2f))
            drawLabel("R2", r2TL.x + r2Sz.width / 2, r2TL.y + r2Sz.height / 2)

            // ── L1 shoulder ───────────────────────────────────────────────
            val l1TL = Offset(w * 0.07f, h * 0.17f)
            val l1Sz = Size(w * 0.14f, h * 0.09f)
            drawPadRect(if (state.buttonL1) pressedBrush(200f) else null, surfaceVariant, outlineColor, l1TL, l1Sz, 10f)
            drawLabel("L1", l1TL.x + l1Sz.width / 2, l1TL.y + l1Sz.height / 2)

            // ── R1 shoulder ───────────────────────────────────────────────
            val r1TL = Offset(w * 0.79f, h * 0.17f)
            val r1Sz = Size(w * 0.14f, h * 0.09f)
            drawPadRect(if (state.buttonR1) pressedBrush(200f) else null, surfaceVariant, outlineColor, r1TL, r1Sz, 10f)
            drawLabel("R1", r1TL.x + r1Sz.width / 2, r1TL.y + r1Sz.height / 2)

            // ── Center: Select / Mode / Start ─────────────────────────────
            val centerY = h * 0.43f
            val btnSz = Size(w * 0.09f, h * 0.09f)

            val selTL = Offset(w * 0.34f, centerY)
            drawPadRect(if (state.buttonSelect) pressedBrush(160f) else null, surfaceVariant, outlineColor, selTL, btnSz, 8f)
            drawLabel("SEL", selTL.x + btnSz.width / 2, selTL.y + btnSz.height / 2)

            drawPadCircle(if (state.buttonMode) pressedBrush(240f) else null, surfaceVariant, outlineColor, Offset(w * 0.50f, centerY + h * 0.01f), h * 0.065f)
            drawLabel("⏵", w * 0.50f, centerY + h * 0.01f)

            val staTL = Offset(w * 0.57f, centerY)
            drawPadRect(if (state.buttonStart) pressedBrush(160f) else null, surfaceVariant, outlineColor, staTL, btnSz, 8f)
            drawLabel("STA", staTL.x + btnSz.width / 2, staTL.y + btnSz.height / 2)

            // ── Left Analog Stick ─────────────────────────────────────────
            val lsC = Offset(w * 0.28f, h * 0.60f)
            val lsR = h * 0.12f
            drawPadCircle(if (state.buttonL3) pressedBrush(60f) else null, surfaceVariant, outlineColor, lsC, lsR)
            val lsDot = Offset(lsC.x + state.leftStickX * lsR * 0.62f, lsC.y + state.leftStickY * lsR * 0.62f)
            drawCircle(color = if (state.buttonL3) Color.White else outlineColor, radius = lsR * 0.20f, center = lsDot)
            drawLabel("L", lsC.x, lsC.y)

            // ── Right Analog Stick ────────────────────────────────────────
            val rsC = Offset(w * 0.60f, h * 0.66f)
            val rsR = h * 0.12f
            drawPadCircle(if (state.buttonR3) pressedBrush(60f) else null, surfaceVariant, outlineColor, rsC, rsR)
            val rsDot = Offset(rsC.x + state.rightStickX * rsR * 0.62f, rsC.y + state.rightStickY * rsR * 0.62f)
            drawCircle(color = if (state.buttonR3) Color.White else outlineColor, radius = rsR * 0.20f, center = rsDot)
            drawLabel("R", rsC.x, rsC.y)

            // ── D-pad ─────────────────────────────────────────────────────
            val dcX = w * 0.20f
            val dcY = h * 0.68f
            val arm = h * 0.095f
            val thick = h * 0.075f
            // Horizontal
            drawPadRect(
                brush = if (state.dpadLeft || state.dpadRight) pressedBrush(0f) else null,
                fallback = surfaceVariant, outline = outlineColor,
                topLeft = Offset(dcX - arm, dcY - thick / 2), size = Size(arm * 2, thick), corner = 4f
            )
            // Vertical
            drawPadRect(
                brush = if (state.dpadUp || state.dpadDown) pressedBrush(0f) else null,
                fallback = surfaceVariant, outline = outlineColor,
                topLeft = Offset(dcX - thick / 2, dcY - arm), size = Size(thick, arm * 2), corner = 4f
            )
            // Individual direction glow dots
            if (state.dpadLeft)  drawCircle(Color.White.copy(alpha = 0.45f), thick / 3.5f, Offset(dcX - arm * 0.58f, dcY))
            if (state.dpadRight) drawCircle(Color.White.copy(alpha = 0.45f), thick / 3.5f, Offset(dcX + arm * 0.58f, dcY))
            if (state.dpadUp)    drawCircle(Color.White.copy(alpha = 0.45f), thick / 3.5f, Offset(dcX, dcY - arm * 0.58f))
            if (state.dpadDown)  drawCircle(Color.White.copy(alpha = 0.45f), thick / 3.5f, Offset(dcX, dcY + arm * 0.58f))
            drawLabel("◀", dcX - arm * 0.58f, dcY)
            drawLabel("▶", dcX + arm * 0.58f, dcY)
            drawLabel("▲", dcX, dcY - arm * 0.58f)
            drawLabel("▼", dcX, dcY + arm * 0.58f)

            // ── Face buttons (A B X Y) ────────────────────────────────────
            val fcX = w * 0.78f
            val fcY = h * 0.58f
            val fcR = h * 0.072f
            val sep = fcR * 2.4f

            // B — right, red tint
            drawPadCircle(if (state.buttonB) pressedBrush(0f) else null, Color(0x22EF5350), outlineColor, Offset(fcX + sep, fcY), fcR)
            drawLabel("B", fcX + sep, fcY)
            // A — bottom, green tint
            drawPadCircle(if (state.buttonA) pressedBrush(120f) else null, Color(0x2266BB6A), outlineColor, Offset(fcX, fcY + sep), fcR)
            drawLabel("A", fcX, fcY + sep)
            // X — left, blue tint
            drawPadCircle(if (state.buttonX) pressedBrush(220f) else null, Color(0x2242A5F5), outlineColor, Offset(fcX - sep, fcY), fcR)
            drawLabel("X", fcX - sep, fcY)
            // Y — top, yellow tint
            drawPadCircle(if (state.buttonY) pressedBrush(50f) else null, Color(0x22FFCA28), outlineColor, Offset(fcX, fcY - sep), fcR)
            drawLabel("Y", fcX, fcY - sep)
        }

        Spacer(Modifier.height(12.dp))

        // ── Active buttons banner ─────────────────────────────────────────
        val activeLabels = buildList {
            if (state.buttonA) add("A"); if (state.buttonB) add("B")
            if (state.buttonX) add("X"); if (state.buttonY) add("Y")
            if (state.buttonL1) add("L1"); if (state.buttonR1) add("R1")
            if (state.buttonL2) add("L2"); if (state.buttonR2) add("R2")
            if (state.buttonL3) add("L3"); if (state.buttonR3) add("R3")
            if (state.buttonStart) add("START"); if (state.buttonSelect) add("SEL")
            if (state.buttonMode) add("MODE")
            if (state.dpadUp) add("↑"); if (state.dpadDown) add("↓")
            if (state.dpadLeft) add("←"); if (state.dpadRight) add("→")
        }

        Card(Modifier.fillMaxWidth()) {
            Row(
                Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    "Active:",
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(end = 4.dp)
                )
                if (activeLabels.isEmpty()) {
                    Text(
                        "none",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    activeLabels.forEach { label ->
                        Box(
                            Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer)
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                label,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }
        }

        // ── Macro / extra buttons ─────────────────────────────────────────
        if (state.extraButtons.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp)) {
                    Text(
                        "Macro / Extra Buttons",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(6.dp))
                    state.extraButtons.forEach { (code, down) ->
                        val label = AKeyEvent.keyCodeToString(code)
                        Row(
                            Modifier.fillMaxWidth().padding(vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                Modifier.size(10.dp).clip(CircleShape)
                                    .background(if (down) Color(0xFF1DB954) else MaterialTheme.colorScheme.surfaceVariant)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "$label (code=$code)",
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.weight(1f)
                            )
                            TextButton(onClick = { onMapRequest(code, label) }) { Text("Map") }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// TAB 2 — State Monitor
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun StateMonitorTab(state: GamepadState) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp)) {
                    SectionTitle("Connection")
                    InfoRow("Device",    state.deviceName)
                    InfoRow("Type",      state.connectionType.name)
                    InfoRow("DeviceID",  state.deviceId.toString())
                    InfoRow("VendorID",  "0x${state.vendorId.toString(16).uppercase()}")
                    InfoRow("ProductID", "0x${state.productId.toString(16).uppercase()}")
                }
            }
        }
        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp)) {
                    SectionTitle("Buttons")
                    val buttons = listOf(
                        "A" to state.buttonA,
                        "B" to state.buttonB,
                        "X" to state.buttonX,
                        "Y" to state.buttonY,
                        "L1" to state.buttonL1,
                        "R1" to state.buttonR1,
                        "L2" to state.buttonL2,
                        "R2" to state.buttonR2,
                        "L3 (stick)" to state.buttonL3,
                        "R3 (stick)" to state.buttonR3,
                        "Start" to state.buttonStart,
                        "Select" to state.buttonSelect,
                        "Mode / Guide" to state.buttonMode,
                        "D-pad Up" to state.dpadUp,
                        "D-pad Down" to state.dpadDown,
                        "D-pad Left" to state.dpadLeft,
                        "D-pad Right" to state.dpadRight,
                    )
                    buttons.forEach { (name, pressed) ->
                        InfoRow(name, if (pressed) "▶ PRESSED" else "·", highlight = pressed)
                    }
                }
            }
        }
        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp)) {
                    SectionTitle("Axes")
                    InfoRow("Left X",  "%.3f".format(state.leftStickX))
                    InfoRow("Left Y",  "%.3f".format(state.leftStickY))
                    InfoRow("Right X", "%.3f".format(state.rightStickX))
                    InfoRow("Right Y", "%.3f".format(state.rightStickY))
                    InfoRow("L2 axis", "%.3f".format(state.l2Axis))
                    InfoRow("R2 axis", "%.3f".format(state.r2Axis))
                    InfoRow("HAT X",   "%.3f".format(state.hatX))
                    InfoRow("HAT Y",   "%.3f".format(state.hatY))
                }
            }
        }
        if (state.extraButtons.isNotEmpty()) {
            item {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(14.dp)) {
                        SectionTitle("Extra / Macro Keycodes")
                        state.extraButtons.forEach { (code, down) ->
                            InfoRow(AKeyEvent.keyCodeToString(code), if (down) "▶ PRESSED" else "·", highlight = down)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
    Spacer(Modifier.height(6.dp))
}

@Composable
private fun InfoRow(key: String, value: String, highlight: Boolean = false) {
    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Text(key, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
        Text(
            value,
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace,
            color = if (highlight) Color(0xFF1DB954) else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// TAB 3 — Mappings
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun MappingsTab(
    state: GamepadState,
    mappings: List<ButtonMapping>,
    onEdit: (Int, String) -> Unit,
    onDelete: (Int) -> Unit,
) {
    val standardButtons = listOf(
        AKeyEvent.KEYCODE_BUTTON_A      to "A",
        AKeyEvent.KEYCODE_BUTTON_B      to "B",
        AKeyEvent.KEYCODE_BUTTON_X      to "X",
        AKeyEvent.KEYCODE_BUTTON_Y      to "Y",
        AKeyEvent.KEYCODE_BUTTON_L1     to "L1",
        AKeyEvent.KEYCODE_BUTTON_R1     to "R1",
        AKeyEvent.KEYCODE_BUTTON_L2     to "L2",
        AKeyEvent.KEYCODE_BUTTON_R2     to "R2",
        AKeyEvent.KEYCODE_BUTTON_THUMBL to "L3 (Stick click)",
        AKeyEvent.KEYCODE_BUTTON_THUMBR to "R3 (Stick click)",
        AKeyEvent.KEYCODE_BUTTON_START  to "Start",
        AKeyEvent.KEYCODE_BUTTON_SELECT to "Select",
        AKeyEvent.KEYCODE_BUTTON_MODE   to "Mode (Guide)",
        AKeyEvent.KEYCODE_DPAD_UP       to "D-pad Up",
        AKeyEvent.KEYCODE_DPAD_DOWN     to "D-pad Down",
        AKeyEvent.KEYCODE_DPAD_LEFT     to "D-pad Left",
        AKeyEvent.KEYCODE_DPAD_RIGHT    to "D-pad Right",
    ) + state.extraButtons.keys.map { it to AKeyEvent.keyCodeToString(it) }

    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 8.dp)) {
        items(standardButtons) { (code, label) ->
            val mapping = mappings.firstOrNull { it.keyCode == code }
            val actionLabel = mapping?.let {
                GamepadActions.all.firstOrNull { a -> a.id == it.actionId }?.label ?: it.actionId
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                    Text(
                        actionLabel ?: "No action",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (mapping != null)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant
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

// ─────────────────────────────────────────────────────────────────────────────
// Mapping bottom sheet
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun MappingSheet(
    keyCode: Int,
    keyLabel: String,
    currentActionId: String,
    onSave: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var selectedId by remember { mutableStateOf(currentActionId) }
    var dropdownOpen by remember { mutableStateOf(false) }
    val selectedAction = GamepadActions.all.firstOrNull { it.id == selectedId }

    Column(Modifier.fillMaxWidth().padding(24.dp)) {
        Text("Map Button", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(keyLabel, style = MaterialTheme.typography.bodyLarge)
        Text(
            "keyCode = $keyCode",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontFamily = FontFamily.Monospace
        )
        Spacer(Modifier.height(20.dp))
        Text("Action", style = MaterialTheme.typography.labelLarge)
        Spacer(Modifier.height(6.dp))
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
        Spacer(Modifier.height(24.dp))
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// DrawScope helpers
// ─────────────────────────────────────────────────────────────────────────────

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

private fun DrawScope.drawLabel(text: String, cx: Float, cy: Float) {
    drawContext.canvas.nativeCanvas.drawText(
        text,
        cx,
        cy + 10f,
        android.graphics.Paint().apply {
            color = android.graphics.Color.WHITE
            textSize = 22f
            textAlign = android.graphics.Paint.Align.CENTER
            isAntiAlias = true
            setShadowLayer(5f, 0f, 0f, android.graphics.Color.BLACK)
        }
    )
}
