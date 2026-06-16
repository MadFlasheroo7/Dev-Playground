package pro.jayeshseth.devcontrols.ui

import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class TcpTab { OSI_ENCAPSULATION, HANDSHAKE }
enum class HandshakeState { CLOSED, SYN_SENT, SYN_ACK_RECEIVED, ESTABLISHED, TIMEOUT }

data class HandshakePacket(
    val type: String, // SYN, SYN-ACK, ACK
    var progress: Float, // 0.0 to 1.0
    val isReturning: Boolean = false,
    val color: Color
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TcpVisualizerScreen(onBack: () -> Unit) {
    var selectedTab by remember { mutableStateOf(TcpTab.OSI_ENCAPSULATION) }
    val scope = rememberCoroutineScope()

    // OSI states
    var osiLayerActive by remember { mutableStateOf(-1) }
    var packetRepresentation by remember { mutableStateOf("DATA: {\"hello\": \"world\"}") }
    var isSendingOsi by remember { mutableStateOf(false) }

    // Handshake states
    var connectionState by remember { mutableStateOf(HandshakeState.CLOSED) }
    val handshakePackets = remember { mutableStateListOf<HandshakePacket>() }
    var isPacketLossEnabled by remember { mutableStateOf(false) }
    var logs = remember { mutableStateListOf<String>() }

    fun resetSim() {
        osiLayerActive = -1
        packetRepresentation = "DATA: {\"hello\": \"world\"}"
        isSendingOsi = false
        connectionState = HandshakeState.CLOSED
        handshakePackets.clear()
        logs.clear()
        logs.add("Simulator initialized.")
    }

    LaunchedEffect(selectedTab) {
        resetSim()
    }

    // Packet clock for handshake
    LaunchedEffect(Unit) {
        while (true) {
            delay(50)
            val iterator = handshakePackets.iterator()
            while (iterator.hasNext()) {
                val p = iterator.next()
                if (p.isReturning) {
                    p.progress -= 0.06f
                    if (p.progress <= 0f) {
                        // Reached client
                        if (p.type == "SYN-ACK") {
                            connectionState = HandshakeState.SYN_ACK_RECEIVED
                            logs.add("Client received SYN-ACK. Sending ACK to Server.")
                            // Send ACK
                            scope.launch {
                                delay(300)
                                handshakePackets.add(HandshakePacket("ACK", 0f, false, Color(0xFF66BB6A)))
                            }
                        }
                        iterator.remove()
                    }
                } else {
                    p.progress += 0.06f
                    if (p.progress >= 1f) {
                        // Reached server
                        if (p.type == "SYN") {
                            logs.add("Server received SYN. Generating SYN-ACK...")
                            // Send SYN-ACK back
                            scope.launch {
                                delay(300)
                                if (isPacketLossEnabled) {
                                    logs.add("Packet Loss Active: SYN-ACK dropped midway!")
                                    connectionState = HandshakeState.TIMEOUT
                                    delay(2000)
                                    logs.add("Client timeout waiting for SYN-ACK. Retrying SYN...")
                                    handshakePackets.add(HandshakePacket("SYN", 0f, false, Color(0xFFFFCC00)))
                                } else {
                                    handshakePackets.add(HandshakePacket("SYN-ACK", 1f, true, Color(0xFF26C6DA)))
                                }
                            }
                        } else if (p.type == "ACK") {
                            connectionState = HandshakeState.ESTABLISHED
                            logs.add("Server received ACK! Connection is now ESTABLISHED.")
                        }
                        iterator.remove()
                    }
                }
            }
        }
    }

    fun startOsiAnimation() {
        scope.launch {
            if (isSendingOsi) return@launch
            resetSim()
            isSendingOsi = true

            // Application
            osiLayerActive = 0
            packetRepresentation = "DATA: {\"hello\": \"world\"}"
            logs.add("Application Layer: Added payload JSON.")
            delay(800)

            // Presentation
            osiLayerActive = 1
            packetRepresentation = "ENCRYPTED: eFozNTk="
            logs.add("Presentation Layer: Encrypted and formatted syntax.")
            delay(800)

            // Session
            osiLayerActive = 2
            packetRepresentation = "[SessionID: 994] | eFozNTk="
            logs.add("Session Layer: Added session token tracker.")
            delay(800)

            // Transport
            osiLayerActive = 3
            packetRepresentation = "[Port Source: 8080 | Dest: 80] [TCP] | eFozNTk="
            logs.add("Transport Layer: Wrapped TCP segment headers.")
            delay(800)

            // Network
            osiLayerActive = 4
            packetRepresentation = "[IP Src: 192.168.1.5 | Dst: 8.8.8.8] [TCP] | eFozNTk="
            logs.add("Network Layer: Appended IP packet headers.")
            delay(800)

            // Data Link
            osiLayerActive = 5
            packetRepresentation = "[MAC Src: 00:0a | Dst: ff:ff] [IP] [TCP] | eFozNTk="
            logs.add("Data Link Layer: Appended MAC ethernet frame header.")
            delay(800)

            // Physical
            osiLayerActive = 6
            packetRepresentation = "01010111 01101111 01110010 01101100 01100100"
            logs.add("Physical Layer: Modulating frame to raw binary bits over copper/radio waves.")
            delay(1200)

            isSendingOsi = false
            logs.add("Transmission finished.")
        }
    }

    fun triggerHandshake() {
        resetSim()
        connectionState = HandshakeState.SYN_SENT
        logs.add("Client initiates connection: Sending SYN packet.")
        handshakePackets.add(HandshakePacket("SYN", 0f, false, Color(0xFFFFCC00)))
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("TCP Handshake & OSI Layers", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    TextButton(onClick = onBack) { Text("Back") }
                }
            )
        },
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // Tabs
            TabRow(selectedTabIndex = selectedTab.ordinal) {
                TcpTab.values().forEach { tab ->
                    Tab(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        text = {
                            Text(
                                text = when (tab) {
                                    TcpTab.OSI_ENCAPSULATION -> "OSI Encapsulation"
                                    TcpTab.HANDSHAKE -> "TCP Handshake"
                                }
                            )
                        }
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // Visualizer Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    when (selectedTab) {
                        TcpTab.OSI_ENCAPSULATION -> {
                            Text(
                                "OSI Data Encapsulation Stack",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.height(12.dp))

                            // Show packet representation
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFF1E1E1E), RoundedCornerShape(8.dp))
                                    .padding(8.dp)
                            ) {
                                Text(
                                    text = packetRepresentation,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    color = Color(0xFFFFCC00)
                                )
                            }

                            Spacer(Modifier.height(12.dp))

                            // Draw stacked layers
                            val layers = listOf(
                                "7. Application Layer", "6. Presentation Layer", "5. Session Layer",
                                "4. Transport Layer", "3. Network Layer", "2. Data Link Layer",
                                "1. Physical Layer"
                            )
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                layers.forEachIndexed { index, name ->
                                    val isActive = index == osiLayerActive
                                    val bg = when {
                                        isActive -> MaterialTheme.colorScheme.primary
                                        index < osiLayerActive -> Color(0xFF3E3E3E)
                                        else -> Color(0xFF2E2E2E)
                                    }

                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(30.dp)
                                            .background(bg, RoundedCornerShape(4.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            name,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            color = Color.White
                                        )
                                    }
                                }
                            }
                        }

                        TcpTab.HANDSHAKE -> {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "3-Way Handshake",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Box(
                                    modifier = Modifier
                                        .background(
                                            when (connectionState) {
                                                HandshakeState.ESTABLISHED -> Color(0xFF2E7D32)
                                                HandshakeState.TIMEOUT -> Color(0xFFC62828)
                                                else -> Color.Gray
                                            },
                                            RoundedCornerShape(4.dp)
                                        )
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        connectionState.name,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                            Spacer(Modifier.height(16.dp))

                            // Client-Server Pipe Animation
                            Canvas(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(100.dp)
                                    .background(Color(0xFF1E1E1E), shape = RoundedCornerShape(8.dp))
                            ) {
                                val w = size.width
                                val h = size.height

                                // Draw endpoints
                                drawRect(Color(0xFF29B6F6), Offset(20f, h / 2 - 25f), androidx.compose.ui.geometry.Size(70f, 50f))
                                drawRect(Color(0xFFE57373), Offset(w - 90f, h / 2 - 25f), androidx.compose.ui.geometry.Size(70f, 50f))

                                // Connection track
                                drawLine(
                                    color = Color.DarkGray,
                                    start = Offset(90f, h / 2),
                                    end = Offset(w - 90f, h / 2),
                                    strokeWidth = 4f,
                                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                                )

                                // Packets
                                handshakePackets.forEach { p ->
                                    val px = 90f + (w - 180f) * p.progress
                                    drawCircle(p.color, 12f, Offset(px, h / 2))
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Controls
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Simulation Trigger", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(12.dp))

                    when (selectedTab) {
                        TcpTab.OSI_ENCAPSULATION -> {
                            Button(
                                onClick = { startOsiAnimation() },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = !isSendingOsi
                            ) {
                                Text("Send Packet (Enroute Encapsulation)")
                            }
                        }
                        TcpTab.HANDSHAKE -> {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "Simulate Packet Loss:",
                                    modifier = Modifier.weight(1f),
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp
                                )
                                Switch(
                                    checked = isPacketLossEnabled,
                                    onCheckedChange = { isPacketLossEnabled = it }
                                )
                            }
                            Spacer(Modifier.height(8.dp))
                            Button(
                                onClick = { triggerHandshake() },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Establish TCP Session")
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Explanation & Snippets
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Tutorial card
                Card(
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "Concept Lesson",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = getTcpLesson(selectedTab),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Code snippet card
                Card(
                    modifier = Modifier.weight(1.2f),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "Segment Header Details",
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF80CBC4),
                            style = MaterialTheme.typography.titleSmall
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = getTcpCode(selectedTab),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = Color(0xFFECEFF1)
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Logs
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("TCP / OSI Log Activity", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .clip(RoundedCornerShape(6.dp))
                            .padding(8.dp)
                    ) {
                        val logScrollState = rememberScrollState()
                        LaunchedEffect(logs.size) {
                            logScrollState.animateScrollTo(logScrollState.maxValue)
                        }
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(logScrollState)
                        ) {
                            logs.forEach { log ->
                                Text(
                                    text = log,
                                    fontSize = 12.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

fun getTcpLesson(tab: TcpTab): String {
    return when (tab) {
        TcpTab.OSI_ENCAPSULATION -> {
            "OSI Data Encapsulation represents how networking layers package data.\n\n" +
            "As data moves down (from Application to Physical), each layer wraps the payload in its own headers (e.g. TCP port numbers, IP addresses, MAC addresses). Decapsulation strips these headers at the receiver."
        }
        TcpTab.HANDSHAKE -> {
            "TCP is a connection-oriented protocol requiring a 3-way handshake before transferring data:\n\n" +
            "1. Client sends SYN (Synchronize).\n" +
            "2. Server responds with SYN-ACK (Synchronize-Acknowledge).\n" +
            "3. Client sends ACK (Acknowledge).\n" +
            "If packet loss occurs, the client's timeout triggers retransmission."
        }
    }
}

fun getTcpCode(tab: TcpTab): String {
    return when (tab) {
        TcpTab.OSI_ENCAPSULATION -> {
            "// Encapsulated Frame\n" +
            "[Ethernet MAC Header]\n" +
            "  -> [IPv4 Network Header]\n" +
            "    -> [TCP Transport Header]\n" +
            "      -> [Session SessionID]\n" +
            "        -> [Encryption metadata]\n" +
            "          -> [Raw Payload Data]"
        }
        TcpTab.HANDSHAKE -> {
            "// TCP Handshake Packet Flags\n" +
            "Packet 1: SYN\n" +
            "  Seq = X, ACK = 0, Flags = SYN\n\n" +
            "Packet 2: SYN-ACK\n" +
            "  Seq = Y, ACK = X+1, Flags = SYN+ACK\n\n" +
            "Packet 3: ACK\n" +
            "  Seq = X+1, ACK = Y+1, Flags = ACK"
        }
    }
}
