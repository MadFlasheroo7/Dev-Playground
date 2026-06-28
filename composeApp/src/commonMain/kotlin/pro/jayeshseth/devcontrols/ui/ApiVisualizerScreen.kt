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

enum class ApiMode { REST, GRAPHQL, WEBSOCKET }

data class ApiPacket(
    val id: Int,
    val label: String,
    var progress: Float, // 0.0 to 1.0 (left to right)
    var isReturning: Boolean = false, // returning right to left
    val color: Color
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApiVisualizerScreen(onBack: () -> Unit) {
    var selectedMode by remember { mutableStateOf(ApiMode.REST) }
    val scope = rememberCoroutineScope()

    // Simulation states
    val packets = remember { mutableStateListOf<ApiPacket>() }
    var overfetchSizeKb by remember { mutableStateOf(0f) }
    var timeElapsedMs by remember { mutableStateOf(0L) }
    var isWebSocketConnected by remember { mutableStateOf(false) }

    var logs = remember { mutableStateListOf<String>() }
    var packetCounter by remember { mutableStateOf(1) }

    // Reset helper
    fun resetSim() {
        packets.clear()
        overfetchSizeKb = 0f
        timeElapsedMs = 0L
        isWebSocketConnected = false
        logs.clear()
        logs.add("Simulator reset.")
    }

    LaunchedEffect(selectedMode) {
        resetSim()
        if (selectedMode == ApiMode.WEBSOCKET) {
            logs.add("Establish persistent WebSocket handshake...")
        }
    }

    // Packet animation clock
    LaunchedEffect(Unit) {
        while (true) {
            delay(50)
            val iterator = packets.iterator()
            while (iterator.hasNext()) {
                val p = iterator.next()
                if (p.isReturning) {
                    p.progress -= 0.08f
                    if (p.progress <= 0.0f) {
                        logs.add("Client received response: ${p.label}")
                        iterator.remove()
                    }
                } else {
                    p.progress += 0.08f
                    if (p.progress >= 1.0f) {
                        p.progress = 1.0f
                        p.isReturning = true // bounce back
                        logs.add("Server processed: ${p.label}")
                    }
                }
            }
        }
    }

    fun triggerRestQuery() {
        scope.launch {
            resetSim()
            logs.add("Triggering REST HTTP request batch...")
            val start = pro.jayeshseth.devcontrols.platform.getPlatformTimeMillis()

            // Request 1: Users
            val p1 = ApiPacket(packetCounter++, "GET /users/1", 0f, false, Color(0xFFFFCC00))
            packets.add(p1)
            overfetchSizeKb += 12.4f
            delay(500)

            // Request 2: Posts
            val p2 = ApiPacket(packetCounter++, "GET /posts?userId=1", 0f, false, Color(0xFF26C6DA))
            packets.add(p2)
            overfetchSizeKb += 48.2f
            delay(500)

            // Request 3: Comments
            val p3 = ApiPacket(packetCounter++, "GET /comments?postId=5", 0f, false, Color(0xFF66BB6A))
            packets.add(p3)
            overfetchSizeKb += 92.1f

            timeElapsedMs = pro.jayeshseth.devcontrols.platform.getPlatformTimeMillis() - start
            logs.add("REST finished. Total payload size: 152.7 KB (Overfetched unneeded fields).")
        }
    }

    fun triggerGraphQLQuery() {
        scope.launch {
            resetSim()
            logs.add("Posting single GraphQL query structure to /graphql...")
            val start = pro.jayeshseth.devcontrols.platform.getPlatformTimeMillis()

            val p = ApiPacket(packetCounter++, "POST /graphql (Query)", 0f, false, Color(0xFFE57373))
            packets.add(p)
            overfetchSizeKb += 8.1f // Exact payload only

            timeElapsedMs = pro.jayeshseth.devcontrols.platform.getPlatformTimeMillis() - start
            logs.add("GraphQL completed. Total payload size: 8.1 KB (No overfetching).")
        }
    }

    fun pushWebSocketMessage(fromClient: Boolean) {
        if (!isWebSocketConnected) {
            logs.add("Error: Establish connection first!")
            return
        }
        val label = if (fromClient) "Client Msg" else "Server Push"
        val color = if (fromClient) Color(0xFF81C784) else Color(0xFFFFB74D)
        val p = ApiPacket(
            id = packetCounter++,
            label = label,
            progress = if (fromClient) 0f else 1f,
            isReturning = !fromClient, // Server push goes right to left immediately
            color = color
        )
        packets.add(p)
        logs.add("$label sent across persistent frame pipe.")
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("REST vs GraphQL vs WebSockets", fontWeight = FontWeight.Bold) },
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
            // Mode Tabs
            TabRow(selectedTabIndex = selectedMode.ordinal) {
                ApiMode.values().forEach { mode ->
                    Tab(
                        selected = selectedMode == mode,
                        onClick = { selectedMode = mode },
                        text = {
                            Text(
                                text = mode.name,
                                fontWeight = if (selectedMode == mode) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // Visualizer Canvas Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "${selectedMode.name} Pipeline Simulation",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.height(12.dp))

                    // Pipeline Drawing
                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp)
                            .background(Color(0xFF1E1E1E), shape = RoundedCornerShape(8.dp))
                    ) {
                        val w = size.width
                        val h = size.height

                        // Client Label
                        drawRect(Color(0xFF29B6F6), Offset(20f, h / 2 - 25f), androidx.compose.ui.geometry.Size(70f, 50f))
                        // Server Label
                        drawRect(Color(0xFFE57373), Offset(w - 90f, h / 2 - 25f), androidx.compose.ui.geometry.Size(70f, 50f))

                        // Connection pipe
                        if (selectedMode == ApiMode.WEBSOCKET && isWebSocketConnected) {
                            drawLine(
                                color = Color(0xFF66BB6A),
                                start = Offset(90f, h / 2),
                                end = Offset(w - 90f, h / 2),
                                strokeWidth = 8f
                            )
                        } else {
                            drawLine(
                                color = Color.DarkGray,
                                start = Offset(90f, h / 2),
                                end = Offset(w - 90f, h / 2),
                                strokeWidth = 4f,
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                            )
                        }

                        // Packets drawing
                        packets.forEach { p ->
                            val px = 90f + (w - 180f) * p.progress
                            drawCircle(p.color, 14f, Offset(px, h / 2))
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    // Data metrics
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "Data Transferred: ${((overfetchSizeKb * 10).toInt() / 10f)} KB",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Roundtrip overhead: ${timeElapsedMs}ms",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Trigger buttons
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Interactive Sandbox", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(12.dp))

                    when (selectedMode) {
                        ApiMode.REST -> {
                            Button(
                                onClick = { triggerRestQuery() },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("GET User Profile (3 HTTP calls)")
                            }
                        }
                        ApiMode.GRAPHQL -> {
                            Button(
                                onClick = { triggerGraphQLQuery() },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("POST Query (Single endpoint query)")
                            }
                        }
                        ApiMode.WEBSOCKET -> {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        isWebSocketConnected = !isWebSocketConnected
                                        if (isWebSocketConnected) {
                                            logs.add("WebSocket Connected. Session open.")
                                        } else {
                                            logs.add("WebSocket Disconnected.")
                                        }
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(if (isWebSocketConnected) "Close WebSocket" else "Open WebSocket")
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = { pushWebSocketMessage(true) },
                                    modifier = Modifier.weight(1f),
                                    enabled = isWebSocketConnected
                                ) {
                                    Text("Send Client Frame")
                                }
                                Button(
                                    onClick = { pushWebSocketMessage(false) },
                                    modifier = Modifier.weight(1f),
                                    enabled = isWebSocketConnected
                                ) {
                                    Text("Push Server Event")
                                }
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
                            text = getApiLesson(selectedMode),
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
                            "Request Structure Code",
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF80CBC4),
                            style = MaterialTheme.typography.titleSmall
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = getApiCode(selectedMode),
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
                    Text("Session Activity Logs", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
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

fun getApiLesson(mode: ApiMode): String {
    return when (mode) {
        ApiMode.REST -> {
            "REST maps resources to distinct URL paths. To compile nested profiles, clients make multiple sequential roundtrips. This introduces HTTP header overhead and causes overfetching (loading unneeded details) or underfetching."
        }
        ApiMode.GRAPHQL -> {
            "GraphQL queries data using a single POST endpoint. Clients write a JSON query declaring the exact data structure needed. The server parses the schema tree and returns a single customized JSON payload matching that structure."
        }
        ApiMode.WEBSOCKET -> {
            "WebSockets establish a persistent two-way connection channel. Once paired, client and server broadcast data frames instantly without HTTP handshakes, making it ideal for chat sessions or real-time ticker feeds."
        }
    }
}

fun getApiCode(mode: ApiMode): String {
    return when (mode) {
        ApiMode.REST -> {
            "// 3 Requests\n" +
            "GET /users/1\n" +
            "--> returns entire user object\n\n" +
            "GET /posts?userId=1\n" +
            "--> returns all user posts\n\n" +
            "GET /comments?postId=5\n" +
            "--> returns comments list"
        }
        ApiMode.GRAPHQL -> {
            "// Single Query Request\n" +
            "POST /graphql\n" +
            "query {\n" +
            "  user(id: 1) {\n" +
            "    name\n" +
            "    posts {\n" +
            "      title\n" +
            "    }\n" +
            "  }\n" +
            "}"
        }
        ApiMode.WEBSOCKET -> {
            "// WebSocket Pipeline\n" +
            "connection.connect()\n" +
            "// Send message frames\n" +
            "connection.send(\"ping\")\n\n" +
            "// Receive server push\n" +
            "connection.onMessage {\n" +
            "    println(it)\n" +
            "}"
        }
    }
}
