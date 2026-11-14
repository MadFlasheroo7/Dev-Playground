package pro.jayeshseth.devcontrols.ui.hyprdeck

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs

@Composable
fun HyprDeckScreen(onBack: () -> Unit) {
    val controller = rememberHyprDeckController()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // --- Top Bar ---
        HyprDeckTopBar(
            controller = controller,
            onBack = onBack
        )

        if (!controller.isAvailable) {
            HyprNotAvailableBanner(controller)
        } else {
            // --- Monitor & Workspace Strip ---
            HyprTopologyStrip(controller)

            // --- Status Message Snack ---
            AnimatedVisibility(visible = controller.statusMessage != null) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = controller.statusMessage.orEmpty(),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        TextButton(onClick = { /* clear status */ }) {
                            Text("Dismiss", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }

            // --- Main 2-Pane Content ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Left Pane: Window Grid & Spatial Preview (62% width)
                Column(
                    modifier = Modifier
                        .weight(0.62f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Spatial Layout Canvas
                    SpatialLayoutPreview(
                        monitors = controller.monitors,
                        clients = controller.clients,
                        activeWindow = controller.activeWindow,
                        onSelectClient = { client ->
                            controller.selectedClient = client
                            controller.focusWindow(client.address)
                        }
                    )

                    // Search & Stats Bar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = controller.searchQuery,
                            onValueChange = { controller.searchQuery = it },
                            placeholder = { Text("Filter by title, class, PID, or address...") },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        if (controller.searchQuery.isNotEmpty()) {
                            OutlinedButton(
                                onClick = { controller.searchQuery = "" },
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Clear")
                            }
                        }
                    }

                    // Window Cards List
                    val filteredClients = controller.filteredClients
                    if (filteredClients.isEmpty()) {
                        Box(
                            modifier = Modifier.weight(1f).fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (controller.clients.isEmpty()) "No open windows detected in Hyprland" else "No windows matching filter",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.weight(1f).fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(filteredClients, key = { it.address }) { client ->
                                val isActive = controller.activeWindow?.address == client.address
                                HyprClientCard(
                                    client = client,
                                    isActive = isActive,
                                    workspaces = controller.workspaces,
                                    onFocus = { controller.focusWindow(client.address) },
                                    onMoveToWorkspace = { wsId -> controller.moveWindowToWorkspace(client.address, wsId) },
                                    onToggleFloating = { controller.toggleFloating(client.address) },
                                    onToggleFullscreen = { controller.toggleFullscreen(client.address) },
                                    onPin = { controller.pinWindow(client.address) },
                                    onClose = { controller.closeWindow(client.address) }
                                )
                            }
                        }
                    }
                }

                // Right Pane: Control Deck & Live Event Ticker (38% width)
                Column(
                    modifier = Modifier
                        .weight(0.38f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Quick Dispatcher Deck
                    HyprQuickDispatcher(
                        workspaces = controller.workspaces,
                        onSwitchWorkspace = { controller.switchWorkspace(it) }
                    )

                    // Live Event Stream Terminal
                    HyprEventStreamTerminal(
                        events = controller.events,
                        onClear = { controller.clearEvents() }
                    )
                }
            }
        }
    }
}

@Composable
fun HyprDeckTopBar(
    controller: HyprDeckController,
    onBack: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onBack,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("← Back")
                }

                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "HyprDeck",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(if (controller.isAvailable) Color(0xFF4CAF50) else Color(0xFFE53935))
                        )
                    }
                    Text(
                        text = if (controller.isAvailable) "Wayland IPC Director • sig: ${controller.instanceSignature.take(16)}…" else "Hyprland socket not connected",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Quick Badges
                if (controller.isAvailable) {
                    BadgeChip(label = "${controller.monitors.size} Mon")
                    BadgeChip(label = "${controller.workspaces.size} WS")
                    BadgeChip(label = "${controller.clients.size} Win")
                }

                Button(
                    onClick = { controller.refreshAll() },
                    shape = RoundedCornerShape(8.dp),
                    enabled = !controller.isRefreshing
                ) {
                    Text(if (controller.isRefreshing) "Refreshing…" else "Refresh")
                }
            }
        }
    }
}

@Composable
fun BadgeChip(label: String) {
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        shape = RoundedCornerShape(6.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun HyprNotAvailableBanner(controller: HyprDeckController) {
    Box(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(0.6f),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Hyprland Wayland IPC Unavailable",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
                Text(
                    text = "Could not locate active Hyprland Unix domain sockets at \$XDG_RUNTIME_DIR/hypr/.\nMake sure you are logged into a Hyprland Wayland session.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
                Button(onClick = { controller.start() }) {
                    Text("Retry Discovery")
                }
            }
        }
    }
}

@Composable
fun HyprTopologyStrip(controller: HyprDeckController) {
    val scroll = rememberScrollState()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(scroll)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Monitor Chips
        controller.monitors.forEach { mon ->
            val isFocused = mon.focused
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (isFocused) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.clickable { controller.focusMonitor(mon.name) }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text("🖥️ ${mon.name}", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    Text(
                        "${mon.width}x${mon.height}@${mon.refreshRate.toInt()}Hz",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (isFocused) {
                        Text("• Focused", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }

        Spacer(Modifier.width(8.dp))

        // All Filter Pill
        val isAllSelected = controller.selectedWorkspaceFilter == null
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = if (isAllSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.clickable { controller.selectedWorkspaceFilter = null }
        ) {
            Text(
                "All Workspaces (${controller.clients.size})",
                style = MaterialTheme.typography.labelMedium,
                color = if (isAllSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
            )
        }

        // Workspace Pills
        controller.workspaces.forEach { ws ->
            val isSelected = controller.selectedWorkspaceFilter == ws.id
            val hasFocus = controller.monitors.any { it.activeWorkspace.id == ws.id }
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = when {
                    isSelected -> MaterialTheme.colorScheme.primary
                    hasFocus -> MaterialTheme.colorScheme.tertiaryContainer
                    else -> MaterialTheme.colorScheme.surfaceVariant
                },
                modifier = Modifier.clickable { controller.selectedWorkspaceFilter = ws.id }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        "WS ${ws.name}",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (hasFocus) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                    )
                    Badge(
                        containerColor = if (isSelected) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.outlineVariant
                    ) {
                        Text("${ws.windows}")
                    }
                }
            }
        }
    }
}

@Composable
fun SpatialLayoutPreview(
    monitors: List<HyprMonitor>,
    clients: List<HyprClient>,
    activeWindow: HyprActiveWindow?,
    onSelectClient: (HyprClient) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().height(160.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Spatial Layout Map",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "Click geometry to focus window",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }

            Spacer(Modifier.height(6.dp))

            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val canvasW = constraints.maxWidth.toFloat()
                val canvasH = constraints.maxHeight.toFloat()

                // Calculate bounding box across monitors
                val totalMonW = monitors.sumOf { it.width }.coerceAtLeast(1920)
                val maxMonH = monitors.maxOfOrNull { it.height }?.coerceAtLeast(1080) ?: 1080

                val scaleX = (canvasW - 20f) / totalMonW
                val scaleY = (canvasH - 10f) / maxMonH
                val scale = minOf(scaleX, scaleY).coerceIn(0.01f, 1f)

                val activeAddr = activeWindow?.address.orEmpty()

                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable {
                            // Focus monitor or nearest client
                        }
                ) {
                    var curMonX = 10f
                    // Draw monitors
                    monitors.forEach { mon ->
                        val mw = mon.width * scale
                        val mh = mon.height * scale
                        val my = 5f

                        // Monitor outline
                        drawRect(
                            color = if (mon.focused) Color(0xFF388E3C).copy(alpha = 0.15f) else Color.DarkGray.copy(alpha = 0.1f),
                            topLeft = Offset(curMonX, my),
                            size = Size(mw, mh)
                        )
                        drawRect(
                            color = if (mon.focused) Color(0xFF4CAF50) else Color.Gray.copy(alpha = 0.4f),
                            topLeft = Offset(curMonX, my),
                            size = Size(mw, mh),
                            style = Stroke(width = if (mon.focused) 2f else 1f)
                        )

                        // Draw windows belonging to this monitor
                        val monClients = clients.filter { it.monitor == mon.id }
                        monClients.forEach { c ->
                            val wx = curMonX + (c.x - mon.x).coerceAtLeast(0) * scale
                            val wy = my + (c.y - mon.y).coerceAtLeast(0) * scale
                            val ww = (c.width * scale).coerceAtLeast(8f)
                            val wh = (c.height * scale).coerceAtLeast(8f)

                            val isWinActive = c.address == activeAddr
                            val winColor = getColorForClass(c.displayClass)

                            drawRect(
                                color = if (isWinActive) winColor.copy(alpha = 0.8f) else winColor.copy(alpha = 0.35f),
                                topLeft = Offset(wx, wy),
                                size = Size(ww, wh)
                            )
                            drawRect(
                                color = if (isWinActive) Color.White else winColor,
                                topLeft = Offset(wx, wy),
                                size = Size(ww, wh),
                                style = Stroke(width = if (isWinActive) 2.5f else 1f)
                            )
                        }

                        curMonX += mw + 10f
                    }
                }
            }
        }
    }
}

@Composable
fun HyprClientCard(
    client: HyprClient,
    isActive: Boolean,
    workspaces: List<HyprWorkspace>,
    onFocus: () -> Unit,
    onMoveToWorkspace: (Int) -> Unit,
    onToggleFloating: () -> Unit,
    onToggleFullscreen: () -> Unit,
    onPin: () -> Unit,
    onClose: () -> Unit
) {
    val borderColor by animateColorAsState(
        if (isActive) MaterialTheme.colorScheme.primary else Color.Transparent
    )

    var showMoveMenu by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(2.dp, borderColor, RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(
            containerColor = if (isActive) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            // Header Row: App class + title + active badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    val appColor = getColorForClass(client.displayClass)
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(appColor),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = client.displayClass.take(1).uppercase(),
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.labelMedium
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = client.displayName,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${client.displayClass} • PID: ${client.pid} • ${client.width}x${client.height} @ (${client.x}, ${client.y})",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (isActive) {
                        Surface(
                            color = MaterialTheme.colorScheme.primary,
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                "ACTIVE",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            "WS ${client.workspace.name}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            // Tags row: Floating / Fullscreen / Pinned / XWayland
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (client.floating) StatusTag("Floating", Color(0xFF0288D1))
                if (client.isFullscreen) StatusTag("Fullscreen", Color(0xFF7B1FA2))
                if (client.pinned) StatusTag("Pinned", Color(0xFFE65100))
                if (client.xwayland) StatusTag("XWayland", Color(0xFFC2185B))
                Text(
                    client.address,
                    fontFamily = FontFamily.Monospace,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onFocus,
                    modifier = Modifier.height(32.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp)
                ) {
                    Text("Focus", style = MaterialTheme.typography.labelSmall)
                }

                Box {
                    OutlinedButton(
                        onClick = { showMoveMenu = true },
                        modifier = Modifier.height(32.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp)
                    ) {
                        Text("Move WS ▾", style = MaterialTheme.typography.labelSmall)
                    }
                    DropdownMenu(
                        expanded = showMoveMenu,
                        onDismissRequest = { showMoveMenu = false }
                    ) {
                        (1..10).forEach { wsId ->
                            DropdownMenuItem(
                                text = { Text("Move to Workspace $wsId") },
                                onClick = {
                                    showMoveMenu = false
                                    onMoveToWorkspace(wsId)
                                }
                            )
                        }
                    }
                }

                OutlinedButton(
                    onClick = onToggleFloating,
                    modifier = Modifier.height(32.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp)
                ) {
                    Text(if (client.floating) "Tile" else "Float", style = MaterialTheme.typography.labelSmall)
                }

                OutlinedButton(
                    onClick = onToggleFullscreen,
                    modifier = Modifier.height(32.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp)
                ) {
                    Text(if (client.isFullscreen) "Unfull" else "Full", style = MaterialTheme.typography.labelSmall)
                }

                OutlinedButton(
                    onClick = onPin,
                    modifier = Modifier.height(32.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp)
                ) {
                    Text(if (client.pinned) "Unpin" else "Pin", style = MaterialTheme.typography.labelSmall)
                }

                Spacer(Modifier.weight(1f))

                Button(
                    onClick = onClose,
                    modifier = Modifier.height(32.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    contentPadding = PaddingValues(horizontal = 10.dp)
                ) {
                    Text("Close", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

@Composable
fun StatusTag(text: String, color: Color) {
    Surface(
        color = color.copy(alpha = 0.15f),
        shape = RoundedCornerShape(4.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.5f))
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

@Composable
fun HyprQuickDispatcher(
    workspaces: List<HyprWorkspace>,
    onSwitchWorkspace: (Int) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                "Workspace Jump Pad",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                (1..5).forEach { wsId ->
                    Button(
                        onClick = { onSwitchWorkspace(wsId) },
                        modifier = Modifier.weight(1f).height(34.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text("$wsId", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                (6..10).forEach { wsId ->
                    Button(
                        onClick = { onSwitchWorkspace(wsId) },
                        modifier = Modifier.weight(1f).height(34.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text("$wsId", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
    }
}

@Composable
fun HyprEventStreamTerminal(
    events: List<HyprEvent>,
    onClear: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().fillMaxHeight(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF141416)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFF00E676))
                    )
                    Text(
                        "SOCKET2 STREAM",
                        color = Color(0xFF00E676),
                        fontFamily = FontFamily.Monospace,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "(${events.size})",
                        color = Color.Gray,
                        fontFamily = FontFamily.Monospace,
                        style = MaterialTheme.typography.labelSmall
                    )
                }

                TextButton(
                    onClick = onClear,
                    contentPadding = PaddingValues(horizontal = 8.dp)
                ) {
                    Text("Clear", color = Color.LightGray, style = MaterialTheme.typography.labelSmall)
                }
            }

            Spacer(Modifier.height(6.dp))

            val listState = rememberLazyListState()
            LaunchedEffect(events.size) {
                if (events.isNotEmpty()) {
                    listState.animateScrollToItem(events.size - 1)
                }
            }

            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(events) { ev ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = ev.formattedTime,
                            color = Color(0xFF757575),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp
                        )
                        Text(
                            text = ev.name,
                            color = getEventColor(ev.name),
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                        Text(
                            text = ">> ${ev.data}",
                            color = Color(0xFFECEFF1),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

fun getColorForClass(className: String): Color {
    val hash = abs(className.hashCode())
    val colors = listOf(
        Color(0xFF1E88E5), // Blue
        Color(0xFF8E24AA), // Purple
        Color(0xFF00897B), // Teal
        Color(0xFFFB8C00), // Amber
        Color(0xFFE53935), // Red
        Color(0xFF3949AB), // Indigo
        Color(0xFF00ACC1), // Cyan
        Color(0xFF43A047), // Green
    )
    return colors[hash % colors.size]
}

fun getEventColor(eventName: String): Color {
    return when (eventName) {
        "openwindow" -> Color(0xFF69F0AE)
        "closewindow" -> Color(0xFFFF5252)
        "workspace" -> Color(0xFF40C4FF)
        "focusedmon" -> Color(0xFFFFD740)
        "activewindow", "activewindowv2" -> Color(0xFFE040FB)
        "movewindow" -> Color(0xFFFFAB40)
        "fullscreen" -> Color(0xFFB388FF)
        else -> Color(0xFF80D8FF)
    }
}
