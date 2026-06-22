package pro.jayeshseth.devcontrols.ui

import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

// Represents a route registered on our Ktor server
private data class MockRoute(
    val method: String,
    val path: String,
    val status: Int,
    val responseBody: String,
    val isSystem: Boolean = false
)

private enum class KtorTab { CLIENT, WS_CHAT, PLUGINS, ROUTES, CONSOLE }

@OptIn(ExperimentalMaterial3Api::class, ExperimentalTextApi::class)
@Composable
fun KtorPlaygroundScreen(onBack: () -> Unit) {
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    val textMeasurer = rememberTextMeasurer()
    val keyboardController = LocalSoftwareKeyboardController.current

    // Active tab
    var activeTab by remember { mutableStateOf(KtorTab.CLIENT) }

    // Ktor Server Plugin Toggles
    var isCallLoggingEnabled by remember { mutableStateOf(true) }
    var isRateLimitingEnabled by remember { mutableStateOf(false) }
    var isAuthEnabled by remember { mutableStateOf(false) }
    var isContentNegotiationEnabled by remember { mutableStateOf(true) }
    var isStatusPagesEnabled by remember { mutableStateOf(true) }

    // Rate Limiter Token Bucket State
    var currentTokens by remember { mutableStateOf(5) }
    val maxTokens = 5

    // WebSocket state
    var isWsConnected by remember { mutableStateOf(false) }
    var wsInputText by remember { mutableStateOf("") }
    val wsMessages = remember { mutableStateListOf<Pair<String, String>>() } // sender to message
    var isWsSending by remember { mutableStateOf(false) }
    var wsPacketProgress by remember { mutableStateOf(0f) }
    var wsPacketDirectionForward by remember { mutableStateOf(true) } // true: Client -> Server, false: Server -> Client

    // Token bucket refill coroutine loop
    LaunchedEffect(isRateLimitingEnabled) {
        if (isRateLimitingEnabled) {
            while (true) {
                delay(3000)
                if (currentTokens < maxTokens) {
                    currentTokens++
                }
            }
        } else {
            currentTokens = maxTokens
        }
    }

    // Route configuration state
    val routes = remember {
        mutableStateListOf(
            MockRoute("GET", "/users", 200, "[\n  {\"id\": 1, \"name\": \"Alice\"},\n  {\"id\": 2, \"name\": \"Bob\"}\n]", isSystem = true),
            MockRoute("POST", "/users", 201, "{\n  \"status\": \"success\",\n  \"message\": \"User Alice registered!\"\n}", isSystem = true),
            MockRoute("GET", "/secure-data", 200, "{\n  \"confidential\": \"Ktor uses Kotlin coroutines for ultra-fast async APIs!\"\n}", isSystem = true),
            MockRoute("GET", "/error", 500, "Exception thrown inside route handler: Database Connection Failed!", isSystem = true)
        )
    }

    // Client Request Fields
    var requestMethod by remember { mutableStateOf("GET") }
    var requestPath by remember { mutableStateOf("/users") }
    var authHeader by remember { mutableStateOf("") }
    var requestBody by remember { mutableStateOf("{\n  \"name\": \"Alice\"\n}") }

    // Route Builder Popup State
    var showAddRouteDialog by remember { mutableStateOf(false) }
    var newMethod by remember { mutableStateOf("GET") }
    var newPath by remember { mutableStateOf("/items") }
    var newStatus by remember { mutableStateOf("200") }
    var newBody by remember { mutableStateOf("{\n  \"message\": \"Success\"\n}") }

    // Simulation/Animation engine state
    var isRequestInProgress by remember { mutableStateOf(false) }
    var packetProgress by remember { mutableStateOf(0f) } // 0.0 to 1.0
    var packetColor by remember { mutableStateOf(Color(0xFF00E5FF)) } // Cyan
    var activeStageIndex by remember { mutableStateOf(-1) }
    var animationSuccess by remember { mutableStateOf<Boolean?>(null) }
    var interceptionNodeIndex by remember { mutableStateOf(-1) }
    var finalResponseCode by remember { mutableStateOf<Int?>(null) }
    var finalResponseBody by remember { mutableStateOf("") }

    // Terminal console log list
    val consoleLogs = remember {
        mutableStateListOf(
            "[sys] Ktor Engine initialized.",
            "[sys] Listening for incoming mock API traffic on port 8080...",
            "[server] Installed plugin: CallLogging",
            "[server] Installed plugin: ContentNegotiation",
            "[server] Installed plugin: StatusPages"
        )
    }

    // Custom pipeline stages representation
    val stages = listOf(
        "Client",          // 0
        "Call Setup",      // 1
        "CallLogging",     // 2
        "RateLimiting",    // 3
        "Authentication",  // 4
        "ContentNegotiation", // 5
        "Routing",         // 6
        "StatusPages",     // 7
        "Response"         // 8
    )

    // Helper to print timestamped logs
    fun printLog(tag: String, message: String, isError: Boolean = false) {
        val prefix = if (isError) "[ERROR][$tag]" else "[$tag]"
        consoleLogs.add("$prefix $message")
    }

    // WebSocket helper actions
    fun connectWebSocket() {
        if (isWsConnected) return
        coroutineScope.launch {
            printLog("client", "Establishing WebSocket handshake with ws://localhost:8080/chat...")
            delay(800)
            isWsConnected = true
            printLog("ws", "WebSocket connection opened successfully.")
            printLog("server", "Session started: Client connected to /chat via WS protocol.")
        }
    }

    fun disconnectWebSocket() {
        if (!isWsConnected) return
        coroutineScope.launch {
            printLog("client", "Closing WebSocket connection...")
            delay(500)
            isWsConnected = false
            printLog("ws", "WebSocket connection closed.")
            printLog("server", "Session closed: Client disconnected from /chat.")
        }
    }

    fun sendWsMessage() {
        if (!isWsConnected || wsInputText.isBlank() || isWsSending) return
        val msg = wsInputText
        wsInputText = ""
        coroutineScope.launch {
            isWsSending = true
            wsPacketDirectionForward = true
            wsPacketProgress = 0f
            wsMessages.add("Client" to msg)
            printLog("client", "WS Send Frame: TextFrame(\"$msg\")")
            printLog("server", "Received WS frame: \"$msg\"")

            // Animate client -> server
            val animSteps = 20
            for (step in 1..animSteps) {
                wsPacketProgress = step / animSteps.toFloat()
                delay(20)
            }
            wsPacketProgress = 1f
            delay(300)
            isWsSending = false

            // Server process and reply
            delay(300)
            val reply = "Echo: $msg"
            
            // Start return trip
            isWsSending = true
            wsPacketDirectionForward = false
            wsPacketProgress = 0f
            
            // Animate server -> client
            for (step in 1..animSteps) {
                wsPacketProgress = step / animSteps.toFloat()
                delay(20)
            }
            wsPacketProgress = 1f
            delay(200)
            
            wsMessages.add("Server" to reply)
            printLog("ws", "WS Receive Frame: TextFrame(\"$reply\")")
            isWsSending = false
        }
    }


    // Execute Client HTTP Call simulation
    fun sendHttpRequest() {
        if (isRequestInProgress) return
        
        coroutineScope.launch {
            // Reset state
            isRequestInProgress = true
            packetProgress = 0f
            packetColor = Color(0xFF00E5FF)
            activeStageIndex = 0
            animationSuccess = null
            interceptionNodeIndex = -1
            finalResponseCode = null
            finalResponseBody = ""

            printLog("client", "Sending HTTP $requestMethod request to mock://localhost:8080$requestPath")
            
            // Animation segments: total stages 9. We animate transition from stage i to i+1
            var hasFailed = false
            var currentException = false

            for (stageIdx in 0..8) {
                activeStageIndex = stageIdx
                
                // Animate packet sliding to the current stage node
                val targetProgress = stageIdx / 8f
                val startProgress = packetProgress
                val animSteps = 15
                for (step in 1..animSteps) {
                    packetProgress = startProgress + (targetProgress - startProgress) * (step / animSteps.toFloat())
                    delay(20)
                }
                packetProgress = targetProgress
                delay(300) // Pause at the node for inspection

                // Perform Ktor pipeline logic checks at each node
                when (stageIdx) {
                    0 -> { // Client origin
                        // no-op
                    }
                    1 -> { // Call Setup
                        printLog("ktor", "Pipeline: Setup call context, parsed query parameters & target URL.")
                    }
                    2 -> { // CallLogging
                        if (isCallLoggingEnabled) {
                            printLog("logging", "$requestMethod $requestPath - IP: 127.0.0.1 - Agent: KtorHttpClient/2.0")
                        } else {
                            printLog("ktor", "CallLogging plugin is bypassed (not installed).")
                        }
                    }
                    3 -> { // RateLimiting
                        if (isRateLimitingEnabled) {
                            printLog("rate-limiting", "Plugin: Checking available request tokens ($currentTokens/$maxTokens)...")
                            if (currentTokens <= 0) {
                                printLog("rate-limiting", "Limit exceeded! 0 tokens remaining. Blocking request.", isError = true)
                                hasFailed = true
                                interceptionNodeIndex = 3
                                finalResponseCode = 429
                                finalResponseBody = "{\n  \"error\": \"Too Many Requests\",\n  \"message\": \"Rate limit exceeded. Token bucket empty. Refilling in progress...\"\n}"
                            } else {
                                currentTokens--
                                printLog("rate-limiting", "Token consumed. Remaining tokens: $currentTokens/$maxTokens")
                            }
                        } else {
                            printLog("ktor", "RateLimiting plugin is bypassed (not installed).")
                        }
                    }
                    4 -> { // Authentication
                        if (isAuthEnabled) {
                            printLog("auth", "Plugin: Verifying client authorization headers...")
                            val hasBearer = authHeader.trim().startsWith("Bearer ", ignoreCase = true)
                            if (!hasBearer) {
                                printLog("auth", "Challenge failed! Authorization Bearer token is missing.", isError = true)
                                hasFailed = true
                                interceptionNodeIndex = 4
                                finalResponseCode = 401
                                finalResponseBody = "{\n  \"error\": \"Unauthorized\",\n  \"message\": \"No valid Bearer token provided in Authorization header.\"\n}"
                            } else {
                                printLog("auth", "Authorized client credentials accepted.")
                            }
                        } else {
                            printLog("ktor", "Authentication plugin is bypassed (not installed).")
                        }
                    }
                    5 -> { // ContentNegotiation
                        if (isContentNegotiationEnabled) {
                            if (requestMethod in listOf("POST", "PUT")) {
                                printLog("content-negotiation", "Plugin: Deserializing request JSON body...")
                                val trimBody = requestBody.trim()
                                val isValidJson = trimBody.startsWith("{") && trimBody.endsWith("}")
                                if (!isValidJson) {
                                    printLog("content-negotiation", "Parsing failed! Request body is not formatted as valid JSON.", isError = true)
                                    hasFailed = true
                                    interceptionNodeIndex = 5
                                    finalResponseCode = 400
                                    finalResponseBody = "{\n  \"error\": \"Bad Request\",\n  \"message\": \"Content Negotiation failed: Request payload must be a valid JSON object.\"\n}"
                                } else {
                                    printLog("content-negotiation", "Payload parsed successfully.")
                                }
                            }
                        } else {
                            printLog("ktor", "ContentNegotiation plugin is bypassed (not installed).")
                        }
                    }
                    6 -> { // Routing
                        printLog("routing", "Pipeline: Resolving route path matching...")
                        val matched = routes.find { it.method.equals(requestMethod, true) && it.path == requestPath }
                        if (matched == null) {
                            printLog("routing", "No route matching $requestMethod $requestPath found.", isError = true)
                            hasFailed = true
                            interceptionNodeIndex = 6
                            finalResponseCode = 404
                            finalResponseBody = "{\n  \"error\": \"Not Found\",\n  \"message\": \"The mock Ktor server does not have a route registered for $requestMethod $requestPath\"\n}"
                        } else if (matched.path == "/error") {
                            printLog("routing", "Matched route /error. Simulating unhandled route exception!", isError = true)
                            currentException = true
                            if (isStatusPagesEnabled) {
                                printLog("routing", "Forwarding exception to StatusPages for interception.")
                            } else {
                                // Server crash
                                hasFailed = true
                                interceptionNodeIndex = 6
                                finalResponseCode = 500
                                finalResponseBody = "Unhandled Server Exception: java.lang.IllegalStateException: Database Connection Failed!\n" +
                                        "\tat pro.jayeshseth.devcontrols.ui.KtorPlaygroundScreenKt.mockEndpoint(KtorPlaygroundScreen.kt:322)\n" +
                                        "\tat io.ktor.server.routing.Route.handle(Route.kt:45)"
                                printLog("server", "Fatal exception occurred in application loop! Respond 500 Internal Error.", isError = true)
                            }
                        } else {
                            printLog("routing", "Successfully resolved route mapping: ${matched.method} ${matched.path}")
                            finalResponseCode = matched.status
                            finalResponseBody = matched.responseBody
                        }
                    }
                    7 -> { // StatusPages Exception Handling
                        if (currentException && isStatusPagesEnabled) {
                            printLog("status-pages", "Intercepted IllegalStateException(\"Database Connection Failed!\"). Mapping to custom error status...")
                            finalResponseCode = 500
                            finalResponseBody = "{\n  \"status\": 500,\n  \"error\": \"Internal Server Error\",\n  \"exception\": \"IllegalStateException\",\n  \"message\": \"Database Connection Failed! (Handled by StatusPages plugin)\"\n}"
                        } else {
                            printLog("ktor", "StatusPages: Request completed without exceptions.")
                        }
                    }
                    8 -> { // Response
                        printLog("ktor", "Pipeline: Preparing output response context ($finalResponseCode).")
                    }
                }

                if (hasFailed) break
            }

            // Animate request return trip
            if (hasFailed) {
                animationSuccess = false
                packetColor = Color(0xFFFF007F) // Pink/Red fail

                // Slide backward from interception node to client origin
                val startProgress = packetProgress
                val animSteps = 20
                for (step in 1..animSteps) {
                    packetProgress = startProgress * (1f - step / animSteps.toFloat())
                    delay(15)
                }
                packetProgress = 0f
                printLog("client", "Request failed. Received Response Code: $finalResponseCode")
            } else {
                animationSuccess = true
                packetColor = Color(0xFF00FF00) // Green success

                // Final slide back to client showing success
                val animSteps = 20
                for (step in 1..animSteps) {
                    packetProgress = 1f - (step / animSteps.toFloat())
                    delay(15)
                }
                packetProgress = 0f
                printLog("client", "Request succeeded! Received Response Code: $finalResponseCode")
            }
            
            isRequestInProgress = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ktor Server Pipeline Visualizer", style = MaterialTheme.typography.titleMedium, color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Text("< Back", color = Color(0xFF00E5FF), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF06060E))
            )
        },
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .background(Color(0xFF06060E))
        ) {
            // Pipeline Diagram Panel (takes top 40% height)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.4f)
                    .background(Color(0xFF0A0A14))
                    .border(1.dp, Color(0xFF1E1E2E))
                    .padding(8.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Ktor Application Request/Response Pipeline",
                        color = Color(0xFF64748B),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 8.dp, top = 4.dp)
                    )

                    // Canvas Pipeline
                    Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val w = size.width
                            val h = size.height
                            val paddingX = 40f
                            val centerY = h * 0.5f
                            val nodeCount = stages.size
                            val stepX = (w - 2 * paddingX) / (nodeCount - 1)

                            // 1. Draw WebSocket persistent tunnel if connected
                            if (isWsConnected) {
                                val startX = paddingX
                                val endX = paddingX + 6 * stepX // Stage 6 is Routing
                                val controlY = centerY - 50f
                                
                                val wsPath = androidx.compose.ui.graphics.Path().apply {
                                    moveTo(startX, centerY)
                                    quadraticBezierTo(
                                        (startX + endX) / 2f, controlY,
                                        endX, centerY
                                    )
                                }
                                
                                // Glowing background path
                                drawPath(
                                    path = wsPath,
                                    color = Color(0x2200FF00),
                                    style = Stroke(width = 12f)
                                )
                                
                                // Dotted connection path
                                drawPath(
                                    path = wsPath,
                                    color = Color(0xFF00FF00),
                                    style = Stroke(
                                        width = 3f,
                                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 12f), if (isWsSending) -wsPacketProgress * 80f else 0f)
                                    )
                                )
                            }

                            // 2. Draw pipeline main connection wire
                            stages.indices.forEach { i ->
                                if (i < nodeCount - 1) {
                                    val startX = paddingX + i * stepX
                                    val endX = paddingX + (i + 1) * stepX
                                    
                                    val isNextPluginEnabled = when (i + 1) {
                                        2 -> isCallLoggingEnabled
                                        3 -> isRateLimitingEnabled
                                        4 -> isAuthEnabled
                                        5 -> isContentNegotiationEnabled
                                        7 -> isStatusPagesEnabled
                                        else -> true
                                    }

                                    drawLine(
                                        color = if (isNextPluginEnabled) Color(0xFF1E1E35) else Color(0x331E1E35),
                                        start = Offset(startX, centerY),
                                        end = Offset(endX, centerY),
                                        strokeWidth = 3f,
                                        pathEffect = if (!isNextPluginEnabled) PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f) else null
                                    )
                                }
                            }

                            // 3. Draw nodes representing stages
                            stages.forEachIndexed { i, label ->
                                val nodeX = paddingX + i * stepX
                                val isEnabledPlugin = when (i) {
                                    2 -> isCallLoggingEnabled
                                    3 -> isRateLimitingEnabled
                                    4 -> isAuthEnabled
                                    5 -> isContentNegotiationEnabled
                                    7 -> isStatusPagesEnabled
                                    else -> true
                                }
                                val isActiveNode = activeStageIndex == i

                                val color = when {
                                    !isEnabledPlugin -> Color(0xFF27273F) // Faded disabled
                                    isActiveNode -> packetColor // Glow neon when active
                                    else -> Color(0xFF00E5FF).copy(alpha = 0.6f) // Idle cyan
                                }

                                val radius = if (isActiveNode) 18f else 12f

                                // Draw node outline glow
                                if (isActiveNode) {
                                    drawCircle(
                                        color = color.copy(alpha = 0.25f),
                                        radius = radius * 2.2f,
                                        center = Offset(nodeX, centerY)
                                    )
                                }

                                drawCircle(
                                    color = color,
                                    radius = radius,
                                    center = Offset(nodeX, centerY)
                                )

                                drawCircle(
                                    color = Color(0xFF06060E),
                                    radius = radius * 0.5f,
                                    center = Offset(nodeX, centerY)
                                )
                            }

                            // 4. Draw active request packet particle along pipeline
                            if (isRequestInProgress) {
                                val packetX = paddingX + packetProgress * (w - 2 * paddingX)
                                // draw glowing particle
                                drawCircle(
                                    color = packetColor.copy(alpha = 0.35f),
                                    radius = 24f,
                                    center = Offset(packetX, centerY)
                                )
                                drawCircle(
                                    color = packetColor,
                                    radius = 8f,
                                    center = Offset(packetX, centerY)
                                )
                            }

                            // 5. Draw active WebSocket packet particle if sending
                            if (isWsSending) {
                                val startX = paddingX
                                val endX = paddingX + 6 * stepX
                                val controlY = centerY - 50f
                                
                                val t = wsPacketProgress
                                val omt = 1f - t
                                
                                val p0 = if (wsPacketDirectionForward) Offset(startX, centerY) else Offset(endX, centerY)
                                val p1 = Offset((startX + endX) / 2f, controlY)
                                val p2 = if (wsPacketDirectionForward) Offset(endX, centerY) else Offset(startX, centerY)
                                
                                val particleX = omt * omt * p0.x + 2 * omt * t * p1.x + t * t * p2.x
                                val particleY = omt * omt * p0.y + 2 * omt * t * p1.y + t * t * p2.y
                                
                                drawCircle(
                                    color = Color(0xFF00FF00).copy(alpha = 0.35f),
                                    radius = 20f,
                                    center = Offset(particleX, particleY)
                                )
                                drawCircle(
                                    color = Color(0xFF00FF00),
                                    radius = 7f,
                                    center = Offset(particleX, particleY)
                                )
                            }
                        }

                        // Labels floating above/below nodes
                        val density = LocalDensity.current
                        Box(modifier = Modifier.fillMaxSize()) {
                            stages.forEachIndexed { i, label ->
                                val isActiveNode = activeStageIndex == i
                                val isEnabledPlugin = when (i) {
                                    2 -> isCallLoggingEnabled
                                    3 -> isRateLimitingEnabled
                                    4 -> isAuthEnabled
                                    5 -> isContentNegotiationEnabled
                                    7 -> isStatusPagesEnabled
                                    else -> true
                                }

                                val displayLabel = when (label) {
                                    "ContentNegotiation" -> "Negotiation"
                                    "Authentication" -> "Auth"
                                    "RateLimiting" -> "RateLimit"
                                    else -> label
                                }

                                val labelColor = when {
                                    !isEnabledPlugin -> Color(0xFF475569)
                                    isActiveNode -> Color.White
                                    else -> Color(0xFF94A3B8)
                                }

                                val isBelow = i % 2 == 1

                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .drawBehind {
                                            val w = this.size.width
                                            val h = this.size.height
                                            val paddingX = 40f
                                            val centerY = h * 0.5f
                                            val nodeCount = stages.size
                                            val stepX = (w - 2 * paddingX) / (nodeCount - 1)
                                            val nodeX = paddingX + i * stepX

                                            val yOffset = if (isBelow) 30f else -45f
                                            
                                            // Draw text label
                                            val textStyle = TextStyle(
                                                color = labelColor,
                                                fontSize = 9.sp,
                                                fontWeight = if (isActiveNode) FontWeight.Bold else FontWeight.Normal,
                                                fontFamily = FontFamily.Monospace
                                            )
                                            val textLayout = textMeasurer.measure(
                                                text = AnnotatedString(displayLabel),
                                                style = textStyle
                                            )
                                            drawText(
                                                textLayoutResult = textLayout,
                                                topLeft = Offset(nodeX - textLayout.size.width / 2f, centerY + yOffset)
                                            )
                                        }
                                )
                            }
                        }
                    }

                    // Display active pipeline status text
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isRequestInProgress) "Call active in stage: ${stages.getOrNull(activeStageIndex) ?: "none"}" else "Server is idle.",
                            color = if (isRequestInProgress) Color(0xFF00E5FF) else Color(0xFF475569),
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        finalResponseCode?.let { code ->
                            val color = if (code in 200..299) Color(0xFF00FF00) else Color(0xFFFF007F)
                            Text(
                                text = "HTTP Response: $code",
                                color = color,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }

            // Tab Selector Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF0D0D18))
                    .border(1.dp, Color(0xFF1E1E2E)),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                KtorTab.values().forEach { tab ->
                    val selected = activeTab == tab
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { activeTab = tab }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = tab.name.replace("_", " "),
                            color = if (selected) Color(0xFF00E5FF) else Color(0xFF64748B),
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            // Lower Panels (takes 60% height)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.6f)
                    .background(Color(0xFF06060C))
            ) {
                when (activeTab) {
                    KtorTab.CLIENT -> {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp)
                                .verticalScroll(rememberScrollState())
                        ) {
                            Text("Mock HTTP Client Request Dispatcher", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Spacer(Modifier.height(12.dp))

                            // Token Bucket display
                            if (isRateLimitingEnabled) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFF0D0D18))
                                        .border(1.dp, Color(0xFF1E1E2F))
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text("Rate Limit Token Bucket", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        Text("Refills 1 token every 3s", color = Color(0xFF64748B), fontSize = 10.sp)
                                    }
                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        for (i in 1..maxTokens) {
                                            val active = i <= currentTokens
                                            val color = if (active) Color(0xFF00FF00) else Color(0xFF27273F)
                                            Box(
                                                modifier = Modifier
                                                    .size(width = 16.dp, height = 24.dp)
                                                    .clip(RoundedCornerShape(3.dp))
                                                    .background(color)
                                            )
                                        }
                                    }
                                }
                                Spacer(Modifier.height(12.dp))
                            }

                            // Method & Path Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // HTTP Method Selector
                                Box(
                                    modifier = Modifier
                                        .weight(0.3f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFF1E1E2F))
                                        .clickable {
                                            requestMethod = when (requestMethod) {
                                                "GET" -> "POST"
                                                "POST" -> "PUT"
                                                "PUT" -> "DELETE"
                                                else -> "GET"
                                            }
                                        }
                                        .padding(horizontal = 12.dp, vertical = 14.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    val color = when (requestMethod) {
                                        "GET" -> Color(0xFF00E5FF)
                                        "POST" -> Color(0xFF00FF00)
                                        "PUT" -> Color(0xFFFFEA00)
                                        else -> Color(0xFFFF007F)
                                    }
                                    Text(requestMethod, color = color, fontWeight = FontWeight.Bold)
                                }
                                
                                Spacer(Modifier.width(12.dp))

                                // Endpoint input text field
                                OutlinedTextField(
                                    value = requestPath,
                                    onValueChange = { requestPath = it },
                                    label = { Text("API Path", color = Color(0xFF94A3B8)) },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                        focusedBorderColor = Color(0xFF00E5FF),
                                        unfocusedBorderColor = Color(0xFF1E1E2E)
                                    ),
                                    modifier = Modifier.weight(0.7f),
                                    maxLines = 1
                                )
                            }

                            Spacer(Modifier.height(12.dp))

                            // Auth Header Text Field
                            OutlinedTextField(
                                value = authHeader,
                                onValueChange = { authHeader = it },
                                label = { Text("Authorization Header (bearer authorization)", color = Color(0xFF94A3B8)) },
                                placeholder = { Text("Bearer ktor-token-123", color = Color(0xFF475569)) },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedBorderColor = Color(0xFF00E5FF),
                                    unfocusedBorderColor = Color(0xFF1E1E2E)
                                ),
                                modifier = Modifier.fillMaxWidth(),
                                maxLines = 1
                            )

                            Spacer(Modifier.height(12.dp))

                            // Request body JSON box (only useful for POST/PUT)
                            if (requestMethod in listOf("POST", "PUT")) {
                                OutlinedTextField(
                                    value = requestBody,
                                    onValueChange = { requestBody = it },
                                    label = { Text("Request Body (JSON)", color = Color(0xFF94A3B8)) },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                        focusedBorderColor = Color(0xFF00E5FF),
                                        unfocusedBorderColor = Color(0xFF1E1E2E)
                                    ),
                                    textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 12.sp),
                                    modifier = Modifier.fillMaxWidth().height(100.dp)
                                )
                                Spacer(Modifier.height(16.dp))
                            }

                            // Trigger HTTP Call
                            Button(
                                onClick = { sendHttpRequest() },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF)),
                                modifier = Modifier.fillMaxWidth().height(50.dp),
                                enabled = !isRequestInProgress
                            ) {
                                Text("Send HTTP Request ⚡", color = Color.Black, fontWeight = FontWeight.Bold)
                            }

                            if (finalResponseCode != null) {
                                Spacer(Modifier.height(20.dp))
                                Text("Response Payload Received", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Spacer(Modifier.height(6.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color(0xFF090912))
                                        .border(1.dp, Color(0xFF1E1E2E))
                                        .padding(12.dp)
                                ) {
                                    Text(
                                        text = finalResponseBody,
                                        color = if (finalResponseCode!! in 200..299) Color(0xFF00FF00) else Color(0xFFFF007F),
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                    }

                    KtorTab.WS_CHAT -> {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("WebSocket Bi-Directional Client", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Button(
                                    onClick = {
                                        if (isWsConnected) disconnectWebSocket() else connectWebSocket()
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isWsConnected) Color(0xFFFF007F) else Color(0xFF00FF00)
                                    ),
                                    modifier = Modifier.height(36.dp)
                                ) {
                                    Text(if (isWsConnected) "Disconnect" else "Connect", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }
                            Spacer(Modifier.height(10.dp))

                            if (!isWsConnected) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFF0D0D18))
                                        .border(1.dp, Color(0xFF1E1E2F))
                                        .padding(16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("🔌 WebSocket Disconnected", color = Color(0xFF64748B), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        Spacer(Modifier.height(8.dp))
                                        Text(
                                            text = "Click 'Connect' to open a persistent TCP connection to ws://localhost:8080/chat. This enables bi-directional real-time frame exchanges.",
                                            color = Color(0xFF475569),
                                            fontSize = 11.sp,
                                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                            modifier = Modifier.padding(horizontal = 24.dp)
                                        )
                                    }
                                }
                            } else {
                                val chatScroll = rememberScrollState()
                                LaunchedEffect(wsMessages.size) {
                                    chatScroll.scrollTo(chatScroll.maxValue)
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .weight(1f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color(0xFF000000))
                                            .border(1.dp, Color(0xFF1E1E2F))
                                            .padding(10.dp)
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .verticalScroll(chatScroll),
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            if (wsMessages.isEmpty()) {
                                                Box(
                                                    modifier = Modifier.fillMaxSize(),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        "Connection established! Send a message below.",
                                                        color = Color(0xFF475569),
                                                        fontSize = 11.sp,
                                                        fontFamily = FontFamily.Monospace
                                                    )
                                                }
                                            } else {
                                                wsMessages.forEach { (sender, msg) ->
                                                    val isClient = sender == "Client"
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = if (isClient) Arrangement.End else Arrangement.Start
                                                    ) {
                                                        Box(
                                                            modifier = Modifier
                                                                .widthIn(max = 240.dp)
                                                                .clip(
                                                                    RoundedCornerShape(
                                                                        topStart = 12.dp,
                                                                        topEnd = 12.dp,
                                                                        bottomStart = if (isClient) 12.dp else 0.dp,
                                                                        bottomEnd = if (isClient) 0.dp else 12.dp
                                                                    )
                                                                )
                                                                .background(
                                                                    if (isClient) Color(0xFF00384D) else Color(0xFF0F3D1C)
                                                                )
                                                                .border(
                                                                    1.dp,
                                                                    if (isClient) Color(0xFF00E5FF).copy(alpha = 0.5f) else Color(0xFF00FF00).copy(alpha = 0.5f),
                                                                    RoundedCornerShape(
                                                                        topStart = 12.dp,
                                                                        topEnd = 12.dp,
                                                                        bottomStart = if (isClient) 12.dp else 0.dp,
                                                                        bottomEnd = if (isClient) 0.dp else 12.dp
                                                                    )
                                                                )
                                                                .padding(horizontal = 12.dp, vertical = 8.dp)
                                                        ) {
                                                            Column {
                                                                Text(
                                                                    text = sender.uppercase(),
                                                                    color = if (isClient) Color(0xFF00E5FF) else Color(0xFF00FF00),
                                                                    fontSize = 8.sp,
                                                                    fontWeight = FontWeight.Bold,
                                                                    fontFamily = FontFamily.Monospace
                                                                )
                                                                Spacer(Modifier.height(2.dp))
                                                                Text(
                                                                    text = msg,
                                                                    color = Color.White,
                                                                    fontSize = 12.sp,
                                                                    fontFamily = FontFamily.Monospace
                                                                )
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                    
                                    Spacer(Modifier.height(8.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        OutlinedTextField(
                                            value = wsInputText,
                                            onValueChange = { wsInputText = it },
                                            placeholder = { Text("Type a WebSocket message...", color = Color(0xFF475569), fontSize = 12.sp) },
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedTextColor = Color.White,
                                                unfocusedTextColor = Color.White,
                                                focusedBorderColor = Color(0xFF00FF00),
                                                unfocusedBorderColor = Color(0xFF1E1E2E)
                                            ),
                                            textStyle = TextStyle(fontSize = 12.sp),
                                            modifier = Modifier.weight(1f),
                                            maxLines = 1,
                                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                                            keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                                                onSend = {
                                                    sendWsMessage()
                                                    keyboardController?.hide()
                                                }
                                            )
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Button(
                                            onClick = {
                                                sendWsMessage()
                                                keyboardController?.hide()
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FF00)),
                                            enabled = wsInputText.isNotBlank() && !isWsSending,
                                            modifier = Modifier.height(50.dp)
                                        ) {
                                            Text("Send", color = Color.Black, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    KtorTab.PLUGINS -> {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text("Ktor Middleware Server Plugins", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            
                            // 1. CallLogging
                            PluginToggleRow(
                                title = "CallLogging",
                                description = "Logs incoming requests and status metrics into the application log terminal console.",
                                checked = isCallLoggingEnabled,
                                onCheckedChange = {
                                    isCallLoggingEnabled = it
                                    printLog("server", "Plugin CallLogging " + if (it) "installed." else "uninstalled.")
                                }
                            )

                            // 1.5 RateLimiting
                            PluginToggleRow(
                                title = "RateLimiting (Token Bucket)",
                                description = "Limits API request frequency. Rejects requests with 429 Too Many Requests when the token bucket runs dry.",
                                checked = isRateLimitingEnabled,
                                onCheckedChange = {
                                    isRateLimitingEnabled = it
                                    printLog("server", "Plugin RateLimiting " + if (it) "installed." else "uninstalled.")
                                }
                            )

                            // 2. Authentication
                            PluginToggleRow(
                                title = "Authentication (JWT / Bearer)",
                                description = "Intercepts requests and verifies credentials. Rejects requests lacking a valid 'Bearer' authentication header.",
                                checked = isAuthEnabled,
                                onCheckedChange = {
                                    isAuthEnabled = it
                                    printLog("server", "Plugin Authentication " + if (it) "installed." else "uninstalled.")
                                }
                            )

                            // 3. ContentNegotiation
                            PluginToggleRow(
                                title = "ContentNegotiation (kotlinx-serialization)",
                                description = "Validates content format in POST/PUT operations. Rejects requests with malformed JSON payloads.",
                                checked = isContentNegotiationEnabled,
                                onCheckedChange = {
                                    isContentNegotiationEnabled = it
                                    printLog("server", "Plugin ContentNegotiation " + if (it) "installed." else "uninstalled.")
                                }
                            )

                            // 4. StatusPages
                            PluginToggleRow(
                                title = "StatusPages (Global Exceptions Handler)",
                                description = "Catches server exceptions globally and translates them into formatted error JSON structures, protecting logs.",
                                checked = isStatusPagesEnabled,
                                onCheckedChange = {
                                    isStatusPagesEnabled = it
                                    printLog("server", "Plugin StatusPages " + if (it) "installed." else "uninstalled.")
                                }
                            )
                        }
                    }

                    KtorTab.ROUTES -> {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Registered Ktor API Endpoints", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Button(
                                    onClick = { showAddRouteDialog = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FF00))
                                ) {
                                    Text("+ Route", color = Color.Black, fontWeight = FontWeight.Bold)
                                }
                            }
                            Spacer(Modifier.height(12.dp))
                            
                            Column(
                                modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                routes.forEach { route ->
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color(0xFF1E1E2F))
                                            .border(1.dp, Color(0xFF27273F))
                                            .padding(12.dp)
                                    ) {
                                        Column {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    val color = when (route.method) {
                                                        "GET" -> Color(0xFF00E5FF)
                                                        "POST" -> Color(0xFF00FF00)
                                                        "PUT" -> Color(0xFFFFEA00)
                                                        else -> Color(0xFFFF007F)
                                                    }
                                                    Text(route.method, color = color, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                                    Spacer(Modifier.width(8.dp))
                                                    Text(route.path, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                                }
                                                Text("Status: ${route.status}", color = Color(0xFF94A3B8), fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                                            }
                                            Spacer(Modifier.height(6.dp))
                                            Text(
                                                text = route.responseBody.take(150) + if (route.responseBody.length > 150) "..." else "",
                                                color = Color(0xFF64748B),
                                                fontSize = 11.sp,
                                                fontFamily = FontFamily.Monospace
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    KtorTab.CONSOLE -> {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Server Console Logs", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text(
                                    "Clear",
                                    color = Color(0xFF00E5FF),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.clickable { consoleLogs.clear() }
                                )
                            }
                            
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFF000000))
                                    .border(1.dp, Color(0xFF1E1E2E))
                                    .padding(10.dp)
                            ) {
                                val consoleScroll = rememberScrollState()
                                LaunchedEffect(consoleLogs.size) {
                                    consoleScroll.scrollTo(consoleScroll.maxValue)
                                }
                                
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .verticalScroll(consoleScroll)
                                ) {
                                    consoleLogs.forEach { log ->
                                        val color = when {
                                            log.contains("[ERROR]") -> Color(0xFFFF007F)
                                            log.contains("[sys]") -> Color(0xFF94A3B8)
                                            log.contains("[auth]") || log.contains("[content-negotiation]") || log.contains("[rate-limiting]") -> Color(0xFFFFEA00)
                                            log.contains("[routing]") || log.contains("[logging]") || log.contains("[ws]") -> Color(0xFF00FF00)
                                            else -> Color(0xFF00E5FF)
                                        }
                                        Text(
                                            text = log,
                                            color = color,
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 11.sp,
                                            modifier = Modifier.padding(vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Add Route Dialog Popup
    if (showAddRouteDialog) {
        AlertDialog(
            onDismissRequest = { showAddRouteDialog = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val statusInt = newStatus.toIntOrNull() ?: 200
                        routes.add(MockRoute(newMethod, newPath, statusInt, newBody))
                        printLog("server", "Registered custom HTTP endpoint: $newMethod $newPath responding with $statusInt")
                        showAddRouteDialog = false
                    }
                ) {
                    Text("Add Route", color = Color(0xFF00FF00))
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddRouteDialog = false }) {
                    Text("Cancel", color = Color(0xFFFF007F))
                }
            },
            title = { Text("Register Custom Route Endpoint", color = Color.White, fontSize = 16.sp) },
            containerColor = Color(0xFF0D0D18),
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("HTTP Method", color = Color(0xFF94A3B8), fontSize = 13.sp)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf("GET", "POST", "PUT", "DELETE").forEach { m ->
                                val isSel = newMethod == m
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(if (isSel) Color(0xFF00E5FF) else Color(0xFF1E1E2F))
                                        .clickable { newMethod = m }
                                        .padding(horizontal = 8.dp, vertical = 6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(m, color = if (isSel) Color.Black else Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    OutlinedTextField(
                        value = newPath,
                        onValueChange = { newPath = it },
                        label = { Text("Path", color = Color(0xFF94A3B8)) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF00E5FF),
                            unfocusedBorderColor = Color(0xFF1E1E2E)
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 1
                    )

                    OutlinedTextField(
                        value = newStatus,
                        onValueChange = { newStatus = it },
                        label = { Text("Response Status Code", color = Color(0xFF94A3B8)) },
                        keyboardOptions = KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF00E5FF),
                            unfocusedBorderColor = Color(0xFF1E1E2E)
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 1
                    )

                    OutlinedTextField(
                        value = newBody,
                        onValueChange = { newBody = it },
                        label = { Text("Response Payload (JSON or text)", color = Color(0xFF94A3B8)) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF00E5FF),
                            unfocusedBorderColor = Color(0xFF1E1E2E)
                        ),
                        textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 11.sp),
                        modifier = Modifier.fillMaxWidth().height(100.dp)
                    )
                }
            }
        )
    }
}

@Composable
private fun PluginToggleRow(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF0D0D18))
            .border(1.dp, Color(0xFF1E1E2F))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(0.75f)) {
            Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Spacer(Modifier.height(4.dp))
            Text(description, color = Color(0xFF64748B), fontSize = 11.sp, lineHeight = 14.sp)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color(0xFF00E5FF),
                checkedTrackColor = Color(0xFF00E5FF).copy(alpha = 0.3f),
                uncheckedThumbColor = Color(0xFF64748B),
                uncheckedTrackColor = Color(0xFF1E1E2E)
            ),
            modifier = Modifier.weight(0.25f, fill = false)
        )
    }
}
