package pro.jayeshseth.devcontrols.ui

import androidx.compose.animation.*
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class ThreadTask(
    val id: Int,
    val name: String,
    val dispatcher: String,
    var progress: Float, // 0.0 to 1.0
    val isBlocking: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DispatcherVisualizerScreen(onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    var systemClock by remember { mutableStateOf(0f) }
    var isUiFrozen by remember { mutableStateOf(false) }

    // Simulation Task Lists
    val queuedTasks = remember { mutableStateListOf<ThreadTask>() }
    val mainActiveTask = remember { mutableStateOf<ThreadTask?>(null) }
    val defaultActiveTasks = remember { mutableStateListOf<ThreadTask>() }
    val ioActiveTasks = remember { mutableStateListOf<ThreadTask>() }

    var taskCounter by remember { mutableStateOf(1) }
    var logs = remember { mutableStateListOf<String>() }

    // Renders the smooth UI clock to visually show freeze/stutter
    LaunchedEffect(isUiFrozen) {
        while (true) {
            delay(16) // ~60fps ticks
            if (!isUiFrozen) {
                systemClock = (systemClock + 0.016f) % 100f
            }
        }
    }

    // Engine task processor ticks every 100ms
    LaunchedEffect(isUiFrozen) {
        while (true) {
            delay(100)
            if (isUiFrozen) continue

            // 1. Process active tasks progress
            mainActiveTask.value?.let {
                it.progress += 0.1f
                if (it.progress >= 1.0f) {
                    logs.add("Main thread completed task: ${it.name}")
                    mainActiveTask.value = null
                }
            }

            val defaultIterator = defaultActiveTasks.iterator()
            while (defaultIterator.hasNext()) {
                val t = defaultIterator.next()
                t.progress += 0.08f
                if (t.progress >= 1.0f) {
                    logs.add("Default Thread Pool completed computation: ${t.name}")
                    defaultIterator.remove()
                }
            }

            val ioIterator = ioActiveTasks.iterator()
            while (ioIterator.hasNext()) {
                val t = ioIterator.next()
                t.progress += 0.05f
                if (t.progress >= 1.0f) {
                    logs.add("IO Thread Pool finished request: ${t.name}")
                    ioIterator.remove()
                }
            }

            // 2. Schedule tasks from queue
            if (queuedTasks.isNotEmpty()) {
                val nextTask = queuedTasks.first()
                when (nextTask.dispatcher) {
                    "Main" -> {
                        if (mainActiveTask.value == null) {
                            mainActiveTask.value = nextTask
                            queuedTasks.removeAt(0)
                            logs.add("Main Thread picked up: ${nextTask.name}")
                        }
                    }
                    "Default" -> {
                        if (defaultActiveTasks.size < 4) { // Max 4 concurrent threads in simulation
                            defaultActiveTasks.add(nextTask)
                            queuedTasks.removeAt(0)
                            logs.add("Default Pool Thread assigned: ${nextTask.name}")
                        }
                    }
                    "IO" -> {
                        if (ioActiveTasks.size < 6) { // Max 6 threads
                            ioActiveTasks.add(nextTask)
                            queuedTasks.removeAt(0)
                            logs.add("IO Pool Thread assigned: ${nextTask.name}")
                        }
                    }
                }
            }
        }
    }

    fun submitTask(disp: String, isBlocking: Boolean = false) {
        val t = ThreadTask(
            id = taskCounter++,
            name = "Task #${taskCounter} ($disp)",
            dispatcher = disp,
            progress = 0f,
            isBlocking = isBlocking
        )
        queuedTasks.add(t)
        logs.add("Enqueued task on Dispatchers.$disp: ${t.name}")
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Coroutine Dispatchers & Threads", fontWeight = FontWeight.Bold) },
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
            // Live clock to demonstrate UI freezes
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isUiFrozen) Color(0xFFC62828) else MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "Main UI Thread Frame Timer",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Text(
                            text = if (isUiFrozen) "UI FROZEN! Blocker thread is sleeping." else "UI Thread is running smoothly (60 FPS).",
                            fontSize = 11.sp
                        )
                    }
                    Text(
                        text = String.format("%.2f s", systemClock),
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 28.sp
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // Thread Pool visualization cards
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Queue Column
                Column(
                    modifier = Modifier
                        .weight(0.8f)
                        .background(Color(0xFF2E2E2E), RoundedCornerShape(8.dp))
                        .padding(8.dp)
                        .height(280.dp)
                ) {
                    Text("Task Queue", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color.Gray)
                    Spacer(Modifier.height(8.dp))
                    queuedTasks.forEach { task ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp)
                                .background(getDispColor(task.dispatcher), RoundedCornerShape(4.dp))
                                .padding(4.dp)
                        ) {
                            Text(task.name, fontSize = 9.sp, color = Color.Black)
                        }
                    }
                }

                // Dispatchers pools
                Column(
                    modifier = Modifier
                        .weight(2.2f)
                        .background(Color(0xFF1E1E1E), RoundedCornerShape(8.dp))
                        .padding(8.dp)
                        .height(280.dp)
                ) {
                    Text("Thread Pools (Dispatchers)", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color.Gray)
                    Spacer(Modifier.height(12.dp))

                    // Dispatchers.Main
                    Text("Dispatchers.Main (UI Thread)", fontWeight = FontWeight.Bold, fontSize = 10.sp, color = Color(0xFFFFCC00))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(40.dp)
                            .border(1.dp, Color(0xFFFFCC00).copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                            .padding(4.dp)
                    ) {
                        mainActiveTask.value?.let {
                            LinearProgressIndicator(
                                progress = { it.progress },
                                modifier = Modifier.fillMaxWidth(),
                                color = Color(0xFFFFCC00)
                            )
                            Text(it.name, fontSize = 9.sp, color = Color.White, modifier = Modifier.align(Alignment.BottomStart))
                        } ?: Text("Idle / Rendering Frames", fontSize = 9.sp, color = Color.DarkGray)
                    }

                    Spacer(Modifier.height(12.dp))

                    // Dispatchers.Default
                    Text("Dispatchers.Default (CPU)", fontWeight = FontWeight.Bold, fontSize = 10.sp, color = Color(0xFF26C6DA))
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        (0..3).forEach { index ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(25.dp)
                                    .border(1.dp, Color(0xFF26C6DA).copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                                    .padding(2.dp)
                            ) {
                                if (defaultActiveTasks.size > index) {
                                    val task = defaultActiveTasks[index]
                                    LinearProgressIndicator(
                                        progress = { task.progress },
                                        modifier = Modifier.fillMaxWidth(),
                                        color = Color(0xFF26C6DA)
                                    )
                                    Text(task.name, fontSize = 8.sp, color = Color.White, modifier = Modifier.align(Alignment.CenterStart))
                                } else {
                                    Text("Idle worker thread", fontSize = 8.sp, color = Color.DarkGray, modifier = Modifier.align(Alignment.CenterStart))
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    // Dispatchers.IO
                    Text("Dispatchers.IO (Network/Disk)", fontWeight = FontWeight.Bold, fontSize = 10.sp, color = Color(0xFF66BB6A))
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        (0..2).forEach { index ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(25.dp)
                                    .border(1.dp, Color(0xFF66BB6A).copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                                    .padding(2.dp)
                            ) {
                                if (ioActiveTasks.size > index) {
                                    val task = ioActiveTasks[index]
                                    LinearProgressIndicator(
                                        progress = { task.progress },
                                        modifier = Modifier.fillMaxWidth(),
                                        color = Color(0xFF66BB6A)
                                    )
                                    Text(task.name, fontSize = 8.sp, color = Color.White, modifier = Modifier.align(Alignment.CenterStart))
                                } else {
                                    Text("Idle IO pool worker", fontSize = 8.sp, color = Color.DarkGray, modifier = Modifier.align(Alignment.CenterStart))
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Sandbox Controls Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Simulate Submissions", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { submitTask("Default") },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Submit CPU")
                        }

                        Button(
                            onClick = { submitTask("IO") },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Submit IO")
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    Text("Suspend vs Block Main Thread (Visual Test)", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                // Suspension: uses coroutine delay
                                logs.add("Suspended Main Thread using delay(2000)... Timer keeps running!")
                                scope.launch {
                                    delay(2000)
                                    logs.add("Main Thread suspension finished.")
                                }
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Suspend (delay)")
                        }

                        Button(
                            onClick = {
                                // Block UI thread: freezes simulated timer
                                logs.add("BLOCKED Main Thread via Thread.sleep()! Timer freezes.")
                                isUiFrozen = true
                                scope.launch {
                                    delay(2500) // Simulate Thread.sleep blocking logic in UI representation
                                    isUiFrozen = false
                                    logs.add("Main Thread unblocked.")
                                }
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text("Block (sleep)")
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Dual columns explaining concepts & showing code snippets
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Tutorial text
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
                            text = "Dispatchers define which thread pool coroutines execute on:\n\n" +
                            "• Dispatchers.Main: Runs on UI thread. Handles drawing, layouts. Blocking this stops all rendering.\n\n" +
                            "• Dispatchers.Default: Tailored for heavy CPU work. Thread pool size is tied to number of cores.\n\n" +
                            "• Dispatchers.IO: Reserved for network requests, database transactions, disk reads/writes. Pool size is dynamically increased.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Code snippet view
                Card(
                    modifier = Modifier.weight(1.2f),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "Kotlin Coroutines Code",
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF80CBC4),
                            style = MaterialTheme.typography.titleSmall
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "// CPU Bound Work\n" +
                            "withContext(Dispatchers.Default) {\n" +
                            "    val result = doHeavyMath()\n" +
                            "}\n\n" +
                            "// IO Bound Work\n" +
                            "withContext(Dispatchers.IO) {\n" +
                            "    val data = downloadApi()\n" +
                            "}\n\n" +
                            "// Non-blocking Suspend\n" +
                            "suspend fun fetch() {\n" +
                            "    delay(2000L) // keeps UI alive\n" +
                            "}",
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
                    Text("Execution Logs", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp)
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

fun getDispColor(disp: String): Color {
    return when (disp) {
        "Main" -> Color(0xFFFFCC00)
        "Default" -> Color(0xFF26C6DA)
        "IO" -> Color(0xFF66BB6A)
        else -> Color.Gray
    }
}
