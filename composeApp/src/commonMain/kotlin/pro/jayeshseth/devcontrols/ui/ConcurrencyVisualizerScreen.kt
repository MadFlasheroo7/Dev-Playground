package pro.jayeshseth.devcontrols.ui

import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class NodeState { ACTIVE, CANCELLED, FAILED }

data class JobNode(
    val id: String,
    val label: String,
    var state: NodeState = NodeState.ACTIVE,
    var colorGlow: Color = Color.Green
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConcurrencyVisualizerScreen(onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    var isSupervisor by remember { mutableStateOf(false) }

    // Job structure states
    val parentNode = remember { mutableStateOf(JobNode("parent", "Parent Scope")) }
    val child1Node = remember { mutableStateOf(JobNode("child1", "Child Job 1")) }
    val child2Node = remember { mutableStateOf(JobNode("child2", "Child Job 2")) }
    val subChild1ANode = remember { mutableStateOf(JobNode("sub1a", "Sub-Child 1A")) }
    val subChild1BNode = remember { mutableStateOf(JobNode("sub1b", "Sub-Child 1B")) }

    val logs = remember { mutableStateListOf<String>() }

    fun resetTree() {
        parentNode.value = JobNode("parent", "Parent Scope")
        child1Node.value = JobNode("child1", "Child Job 1")
        child2Node.value = JobNode("child2", "Child Job 2")
        subChild1ANode.value = JobNode("sub1a", "Sub-Child 1A")
        subChild1BNode.value = JobNode("sub1b", "Sub-Child 1B")
        logs.clear()
        logs.add("Hierarchy initialized.")
    }

    LaunchedEffect(isSupervisor) {
        resetTree()
    }

    // Cancellation triggers
    fun cancelParent() {
        scope.launch {
            logs.add("Cancelling Parent Scope...")
            parentNode.value = parentNode.value.copy(state = NodeState.CANCELLED, colorGlow = Color.Gray)
            delay(300)
            child1Node.value = child1Node.value.copy(state = NodeState.CANCELLED, colorGlow = Color.Gray)
            child2Node.value = child2Node.value.copy(state = NodeState.CANCELLED, colorGlow = Color.Gray)
            delay(300)
            subChild1ANode.value = subChild1ANode.value.copy(state = NodeState.CANCELLED, colorGlow = Color.Gray)
            subChild1BNode.value = subChild1BNode.value.copy(state = NodeState.CANCELLED, colorGlow = Color.Gray)
            logs.add("Parent cancellation propagated to all descendants.")
        }
    }

    fun failChild1() {
        scope.launch {
            logs.add("Child 1 threw Exception!")
            child1Node.value = child1Node.value.copy(state = NodeState.FAILED, colorGlow = Color.Red)
            delay(300)

            // Propagate up to Parent
            if (isSupervisor) {
                logs.add("SupervisorJob boundary: Exception blocked. Parent remains active.")
                // Child 1's children get cancelled because Child 1 failed
                subChild1ANode.value = subChild1ANode.value.copy(state = NodeState.CANCELLED, colorGlow = Color.Gray)
                subChild1BNode.value = subChild1BNode.value.copy(state = NodeState.CANCELLED, colorGlow = Color.Gray)
            } else {
                logs.add("Exception propagating up to Parent Scope...")
                parentNode.value = parentNode.value.copy(state = NodeState.FAILED, colorGlow = Color.Red)
                delay(300)
                // Parent failure cancels siblings
                logs.add("Parent failure cancels other children scopes (Structured Concurrency).")
                child2Node.value = child2Node.value.copy(state = NodeState.CANCELLED, colorGlow = Color.Gray)
                subChild1ANode.value = subChild1ANode.value.copy(state = NodeState.CANCELLED, colorGlow = Color.Gray)
                subChild1BNode.value = subChild1BNode.value.copy(state = NodeState.CANCELLED, colorGlow = Color.Gray)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Structured Concurrency & Cancellation", fontWeight = FontWeight.Bold) },
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
            // Interactive Graph Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "Coroutine Job Hierarchy Tree",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.height(24.dp))

                    // Draw layout hierarchy tree
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(240.dp)
                    ) {
                        // Drawing connecting lines
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val w = size.width
                            val parentX = w / 2
                            val parentY = 30f

                            val child1X = w / 4
                            val child2X = (w / 4) * 3
                            val childY = 180f

                            val subChildAX = w / 8
                            val subChildBX = (w / 8) * 3
                            val subChildY = 330f

                            // Lines Parent -> Child 1 & 2
                            drawLine(
                                color = Color.Gray,
                                start = Offset(parentX, parentY + 30f),
                                end = Offset(child1X, childY - 30f),
                                strokeWidth = 3f
                            )
                            drawLine(
                                color = Color.Gray,
                                start = Offset(parentX, parentY + 30f),
                                end = Offset(child2X, childY - 30f),
                                strokeWidth = 3f
                            )

                            // Lines Child 1 -> Sub-Child A & B
                            drawLine(
                                color = Color.Gray,
                                start = Offset(child1X, childY + 30f),
                                end = Offset(subChildAX, subChildY - 30f),
                                strokeWidth = 3f
                            )
                            drawLine(
                                color = Color.Gray,
                                start = Offset(child1X, childY + 30f),
                                end = Offset(subChildBX, subChildY - 30f),
                                strokeWidth = 3f
                            )
                        }

                        // Render Parent node
                        JobNodeView(
                            node = parentNode.value,
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .offset(y = 10.dp)
                        )

                        // Render Child 1 & 2 nodes
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.Center)
                                .offset(y = (-10).dp),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            JobNodeView(node = child1Node.value)
                            JobNodeView(node = child2Node.value)
                        }

                        // Render Sub-Child 1A & 1B nodes
                        Row(
                            modifier = Modifier
                                .fillMaxWidth(0.5f)
                                .align(Alignment.BottomStart)
                                .offset(x = 10.dp, y = (-10).dp),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            JobNodeView(node = subChild1ANode.value)
                            JobNodeView(node = subChild1BNode.value)
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
                    Text(
                        "Structured Concurrency Sandbox",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Scope Parent Job Strategy:",
                            modifier = Modifier.weight(1f),
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Standard", fontSize = 11.sp, fontWeight = if (!isSupervisor) FontWeight.Bold else FontWeight.Normal)
                            Switch(
                                checked = isSupervisor,
                                onCheckedChange = { isSupervisor = it },
                                modifier = Modifier.padding(horizontal = 4.dp)
                            )
                            Text("SupervisorJob", fontSize = 11.sp, fontWeight = if (isSupervisor) FontWeight.Bold else FontWeight.Normal)
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { cancelParent() },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Gray)
                        ) {
                            Text("Cancel Parent")
                        }

                        Button(
                            onClick = { failChild1() },
                            modifier = Modifier.weight(1.2f),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text("Fail Child 1")
                        }

                        OutlinedButton(
                            onClick = { resetTree() },
                            modifier = Modifier.weight(0.8f)
                        ) {
                            Text("Reset")
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
                            text = getConcurrencyLessonText(isSupervisor),
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
                            "Structured Scope Code",
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF80CBC4),
                            style = MaterialTheme.typography.titleSmall
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = getConcurrencySnippet(isSupervisor),
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
                    Text("Concurrency Event Logs", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
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

@Composable
fun JobNodeView(node: JobNode, modifier: Modifier = Modifier) {
    val nodeBgColor = when (node.state) {
        NodeState.ACTIVE -> Color(0xFF1B5E20)
        NodeState.CANCELLED -> Color(0xFF424242)
        NodeState.FAILED -> Color(0xFFB71C1C)
    }

    Box(
        modifier = modifier
            .width(110.dp)
            .height(50.dp)
            .background(nodeBgColor, RoundedCornerShape(8.dp))
            .border(2.dp, node.colorGlow, RoundedCornerShape(8.dp))
            .padding(4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = node.label,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 10.sp
        )
    }
}

fun getConcurrencyLessonText(isSupervisor: Boolean): String {
    return if (isSupervisor) {
        "A SupervisorJob changes cancellation propagation behavior: children exceptions do NOT travel up to cancel the parent.\n\n" +
        "Key Observations:\n" +
        "• If Child 1 fails, the Exception is stopped at parent boundary. Parent Scope remains Active (Green).\n" +
        "• Sibling Child 2 continues execution unaffected."
    } else {
        "Under standard Structured Concurrency, cancellation propagates downwards, but failures/exceptions propagate upwards.\n\n" +
        "Key Observations:\n" +
        "• Cancelling Parent cancels all children.\n" +
        "• If Child 1 throws an Exception, the failure travels up to cancel the Parent, which immediately cancels Child 2 (sibling) to prevent orphaned leak tasks."
    }
}

fun getConcurrencySnippet(isSupervisor: Boolean): String {
    return if (isSupervisor) {
        "// Supervisor Scope\n" +
        "val scope = CoroutineScope(\n" +
        "    SupervisorJob() + Dispatchers.Main\n" +
        ")\n\n" +
        "scope.launch {\n" +
        "    // Child 1 (Throws Error)\n" +
        "    throw Exception()\n" +
        "}\n\n" +
        "scope.launch {\n" +
        "    // Child 2\n" +
        "    // Kept alive & running!\n" +
        "}"
    } else {
        "// Standard Coroutine Scope\n" +
        "val scope = CoroutineScope(\n" +
        "    Job() + Dispatchers.Main\n" +
        ")\n\n" +
        "scope.launch {\n" +
        "    // Child 1 (Throws Error)\n" +
        "    throw Exception()\n" +
        "}\n\n" +
        "scope.launch {\n" +
        "    // Child 2\n" +
        "    // Cancelled automatically\n" +
        "}"
    }
}
