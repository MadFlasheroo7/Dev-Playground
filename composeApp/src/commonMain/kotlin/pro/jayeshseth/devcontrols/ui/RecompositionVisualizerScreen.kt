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

enum class RecompTab { HEIRARCHY_GLOW, MEMORY_SLOTS, KEY_INVALIDATION }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecompositionVisualizerScreen(onBack: () -> Unit) {
    var selectedTab by remember { mutableStateOf(RecompTab.HEIRARCHY_GLOW) }

    // Recomposition counters (simulated triggers to show glow behaviors)
    var parentRenderCount by remember { mutableStateOf(1) }
    var childARenderCount by remember { mutableStateOf(1) }
    var childBRenderCount by remember { mutableStateOf(1) }
    var childCRenderCount by remember { mutableStateOf(1) }

    var childASkipped by remember { mutableStateOf(false) }
    var childBSkipped by remember { mutableStateOf(false) }
    var childCSkipped by remember { mutableStateOf(false) }

    // Memory Slots Tab States
    var mutableStateValue by remember { mutableStateOf("Initial Value") }
    var normalVariableValue by remember { mutableStateOf(0) }
    var rememberStateValue by remember { mutableStateOf(0) }
    var rememberClickCount by remember { mutableStateOf(0) }

    // Key Invalidation Tab States
    var filterKey by remember { mutableStateOf("Android") }
    var unrelatedState by remember { mutableStateOf(0) }
    var calculationCount by remember { mutableStateOf(0) }

    val scrollState = rememberScrollState()

    // Helper to flash glow states
    fun triggerRecomp(target: String) {
        parentRenderCount++
        when (target) {
            "A" -> {
                childARenderCount++
                childASkipped = false
                childBSkipped = true
                childCSkipped = true
            }
            "B" -> {
                childBRenderCount++
                childBSkipped = false
                childASkipped = true
                childCSkipped = true
            }
            "NONE" -> {
                childASkipped = true
                childBSkipped = true
                childCSkipped = true
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Compose Recomposition & Remember Lab", fontWeight = FontWeight.Bold) },
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
                .verticalScroll(scrollState)
        ) {
            // Tab Selector
            TabRow(selectedTabIndex = selectedTab.ordinal) {
                RecompTab.values().forEach { tab ->
                    Tab(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        text = {
                            Text(
                                text = when (tab) {
                                    RecompTab.HEIRARCHY_GLOW -> "Recomposition Glow"
                                    RecompTab.MEMORY_SLOTS -> "Remember Slots"
                                    RecompTab.KEY_INVALIDATION -> "Key Invalidation"
                                },
                                fontWeight = if (selectedTab == tab) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // Main Interactive Simulator Box
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    when (selectedTab) {
                        RecompTab.HEIRARCHY_GLOW -> {
                            Text(
                                "UI Node Hierarchy & Glow Tracker",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "Red = Executed Recomposition (re-ran the body)\nGreen = Skipped Recomposition (inputs stable, cached UI returned)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                            Spacer(Modifier.height(12.dp))

                            // Root / Parent Box
                            SimulatedGlowBox(
                                label = "Parent App Container (Recomps: $parentRenderCount)",
                                count = parentRenderCount,
                                skipped = false,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                                    .background(Color(0xFF1E1E1E), shape = RoundedCornerShape(12.dp))
                                    .padding(16.dp)
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        // Child A
                                        SimulatedGlowBox(
                                            label = "Child Card A\n(Inputs: State A)\n(Runs: $childARenderCount)",
                                            count = childARenderCount,
                                            skipped = childASkipped,
                                            modifier = Modifier
                                                .weight(1f)
                                                .border(
                                                    1.dp,
                                                    MaterialTheme.colorScheme.outlineVariant,
                                                    RoundedCornerShape(8.dp)
                                                )
                                                .background(Color(0xFF2E2E2E), shape = RoundedCornerShape(8.dp))
                                                .padding(12.dp)
                                        ) {}

                                        // Child B
                                        SimulatedGlowBox(
                                            label = "Child Card B\n(Inputs: State B)\n(Runs: $childBRenderCount)",
                                            count = childBRenderCount,
                                            skipped = childBSkipped,
                                            modifier = Modifier
                                                .weight(1f)
                                                .border(
                                                    1.dp,
                                                    MaterialTheme.colorScheme.outlineVariant,
                                                    RoundedCornerShape(8.dp)
                                                )
                                                .background(Color(0xFF2E2E2E), shape = RoundedCornerShape(8.dp))
                                                .padding(12.dp)
                                        ) {}
                                    }

                                    // Static Child C
                                    SimulatedGlowBox(
                                        label = "Static Child C (No state inputs, fully stable)\n(Runs: $childCRenderCount)",
                                        count = childCRenderCount,
                                        skipped = childCSkipped,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .border(
                                                1.dp,
                                                MaterialTheme.colorScheme.outlineVariant,
                                                RoundedCornerShape(8.dp)
                                            )
                                            .background(Color(0xFF2E2E2E), shape = RoundedCornerShape(8.dp))
                                            .padding(12.dp)
                                    ) {}
                                }
                            }
                        }

                        RecompTab.MEMORY_SLOTS -> {
                            Text(
                                "Compose Positional Memory Slots",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "Demonstrates how 'remember' stores values inside the slot-table during Composition.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(16.dp))

                            // Sandbox Comparison
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .background(Color(0xFF2D2D2D), RoundedCornerShape(8.dp))
                                        .padding(12.dp)
                                ) {
                                    Text("Raw (No remember)", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    Spacer(Modifier.height(8.dp))
                                    Text("Value: $normalVariableValue", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                    Spacer(Modifier.height(4.dp))
                                    Text("Resets to 0 on every recomposition.", fontSize = 10.sp, color = Color.Gray)
                                }

                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .background(Color(0xFF1E3A2F), RoundedCornerShape(8.dp))
                                        .padding(12.dp)
                                ) {
                                    Text("Cached (remember)", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF66BB6A))
                                    Spacer(Modifier.height(8.dp))
                                    Text("Value: $rememberStateValue", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF81C784))
                                    Spacer(Modifier.height(4.dp))
                                    Text("Stored across recomposition ticks.", fontSize = 10.sp, color = Color.Gray)
                                }
                            }

                            Spacer(Modifier.height(16.dp))

                            // Interactive slot addresses
                            Text("Memory Table Slot Allocation Schema:", style = MaterialTheme.typography.titleSmall)
                            Spacer(Modifier.height(8.dp))

                            MemorySlotRow(address = "Slot #001", key = "mutableStateOf", value = mutableStateValue)
                            MemorySlotRow(address = "Slot #002", key = "remember { count }", value = rememberStateValue.toString())
                            MemorySlotRow(address = "Slot #003", key = "unrelatedState", value = normalVariableValue.toString())
                        }

                        RecompTab.KEY_INVALIDATION -> {
                            Text(
                                "remember(key) Invalidation Lab",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.height(12.dp))

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFF2E2E2E), RoundedCornerShape(8.dp))
                                    .padding(16.dp)
                            ) {
                                Text("Key Variable value: '$filterKey'", fontWeight = FontWeight.Bold)
                                Text("Unrelated State value: $unrelatedState")
                                Spacer(Modifier.height(12.dp))

                                val computedValue = remember(filterKey) {
                                    calculationCount++
                                    "Processed results for $filterKey"
                                }

                                Text("Cached Heavy Calculation Output:", fontSize = 12.sp, color = Color.Gray)
                                Text(
                                    text = computedValue,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFFFCC00)
                                )
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    "Total times recalculation ran: $calculationCount",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // State trigger controllers
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Interactive Triggers",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(12.dp))

                    when (selectedTab) {
                        RecompTab.HEIRARCHY_GLOW -> {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = { triggerRecomp("A") },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Mutate State A")
                                }

                                Button(
                                    onClick = { triggerRecomp("B") },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Mutate State B")
                                }
                            }
                            Spacer(Modifier.height(8.dp))

                            OutlinedButton(
                                onClick = { triggerRecomp("NONE") },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Trigger Unrelated Container Recompose")
                            }
                        }

                        RecompTab.MEMORY_SLOTS -> {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        normalVariableValue++
                                        rememberStateValue++
                                        mutableStateValue = "Updated Count: $normalVariableValue"
                                    },
                                    modifier = Modifier.weight(1.2f)
                                ) {
                                    Text("Increment & Trigger Recompose")
                                }

                                OutlinedButton(
                                    onClick = {
                                        // Reset normal local variable scope variables (will reset to 0 in Compose)
                                        normalVariableValue = 0
                                        mutableStateValue = "Initial Value"
                                    },
                                    modifier = Modifier.weight(0.8f)
                                ) {
                                    Text("Clear Local")
                                }
                            }
                        }

                        RecompTab.KEY_INVALIDATION -> {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        filterKey = if (filterKey == "Android") "Kotlin" else "Android"
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Change Key Value")
                                }

                                OutlinedButton(
                                    onClick = {
                                        unrelatedState++
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Mutate Unrelated State")
                                }
                            }
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
                            text = getRecompTutorialText(selectedTab),
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
                            "Kotlin Compose Code",
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF80CBC4),
                            style = MaterialTheme.typography.titleSmall
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = getRecompCodeSnippet(selectedTab),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = Color(0xFFECEFF1)
                        )
                    }
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

// Custom wrapper to show red / green glows
@Composable
fun SimulatedGlowBox(
    label: String,
    count: Int,
    skipped: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    var lastCount by remember { mutableStateOf(count) }
    var glowActive by remember { mutableStateOf(false) }
    var glowColor by remember { mutableStateOf(Color.Red) }

    LaunchedEffect(count, skipped) {
        if (count > lastCount) {
            glowColor = if (skipped) Color.Green else Color.Red
            glowActive = true
            delay(500)
            glowActive = false
            lastCount = count
        }
    }

    Box(modifier = modifier) {
        Column {
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(Modifier.height(8.dp))
            Box(modifier = Modifier.fillMaxWidth()) {
                content()
            }
        }

        if (glowActive) {
            Box(
                Modifier
                    .matchParentSize()
                    .background(glowColor.copy(alpha = 0.25f))
                    .border(2.dp, glowColor, RoundedCornerShape(8.dp))
            )
        }
    }
}

@Composable
fun MemorySlotRow(address: String, key: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .background(Color(0xFF2C2C2C), RoundedCornerShape(6.dp))
            .padding(8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(address, fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = Color.Gray)
        Text(key, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF80CBC4))
        Text(value, fontFamily = FontFamily.Monospace, fontSize = 12.sp, color = Color(0xFFFFCC00))
    }
}

fun getRecompTutorialText(tab: RecompTab): String {
    return when (tab) {
        RecompTab.HEIRARCHY_GLOW -> {
            "Compose uses smart recomposition: only the widgets whose dependencies/states have changed are re-executed.\n\n" +
            "Key Observations:\n" +
            "• Mutating State A causes Parent Container and Child Card A to recompose (flashes Red).\n" +
            "• Child Card B and Child Card C skip execution entirely because their inputs are stable and unchanged (flashes Green)."
        }
        RecompTab.MEMORY_SLOTS -> {
            "A standard local variable is re-allocated to its default value every time the composable runs.\n\n" +
            "To save state across recompositions, we allocate variables inside Compose's internal Slot Table database via 'remember'. The values persist as long as the composable stays in the Layout Tree."
        }
        RecompTab.KEY_INVALIDATION -> {
            "Sometimes calculations depend on specific state factors (like database filters).\n\n" +
            "By writing remember(key) { calculation }, we tell Compose to cache the result inside the slot. The code block will skip execution unless the key variable changes, preventing heavy operations from slowing frame rate."
        }
    }
}

fun getRecompCodeSnippet(tab: RecompTab): String {
    return when (tab) {
        RecompTab.HEIRARCHY_GLOW -> {
            "@Composable\n" +
            "fun ParentContainer() {\n" +
            "    var stateA by remember { mutableStateOf(0) }\n" +
            "    var stateB by remember { mutableStateOf(0) }\n\n" +
            "    Column {\n" +
            "        ChildA(stateA)\n" +
            "        ChildB(stateB)\n" +
            "        StaticChildC() // stable\n" +
            "    }\n" +
            "}"
        }
        RecompTab.MEMORY_SLOTS -> {
            "@Composable\n" +
            "fun CounterApp() {\n" +
            "    // Resets on recompose\n" +
            "    var rawVal = 0 \n\n" +
            "    // Persists in slot table\n" +
            "    var cachedVal by remember {\n" +
            "         mutableStateOf(0) \n" +
            "    }\n" +
            "}"
        }
        RecompTab.KEY_INVALIDATION -> {
            "@Composable\n" +
            "fun QueryApp(filterKey: String) {\n" +
            "    // Recalculates ONLY when\n" +
            "    // filterKey changes\n" +
            "    val output = remember(filterKey) {\n" +
            "         runHeavyCalculation(filterKey)\n" +
            "    }\n" +
            "}"
        }
    }
}
