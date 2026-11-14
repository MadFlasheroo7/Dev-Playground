package pro.jayeshseth.devcontrols.sdui.playground

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pro.jayeshseth.devcontrols.sdui.dsl.SduiDslGenerator
import pro.jayeshseth.devcontrols.sdui.model.SduiDocument
import pro.jayeshseth.devcontrols.sdui.model.defaultSduiJson
import pro.jayeshseth.devcontrols.sdui.render.SduiViewWithStore
import pro.jayeshseth.devcontrols.sdui.state.SduiActionEvent
import pro.jayeshseth.devcontrols.sdui.state.SduiActionRunner
import pro.jayeshseth.devcontrols.sdui.state.SduiStateStore

/**
 * In-App Server-Driven UI Studio & Playground.
 * Provides interactive preset switching, real-time JSON editing, live AST preview,
 * dynamic state inspection, and event logging.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SduiStudioScreen(onBack: () -> Unit) {
    val presets = remember {
        listOf(
            "Spring Counter" to SduiPresets.counterPreset,
            "Bottom Sheet" to SduiPresets.bottomSheetPreset,
            "Gesture Drag" to SduiPresets.gestureDragPreset,
            "E-Commerce" to SduiPresets.ecommercePreset,
            "Canvas Visuals" to SduiPresets.canvasGraphicsPreset,
            "Form Validation" to SduiPresets.formValidationPreset
        )
    }

    var selectedPresetIndex by remember { mutableStateOf(0) }
    var rawJson by remember(selectedPresetIndex) { mutableStateOf(presets[selectedPresetIndex].second) }
    var selectedTab by remember { mutableStateOf(0) } // 0: Preview, 1: JSON Editor, 2: State Inspector, 3: Action Log

    val actionLogs = remember { mutableStateListOf<SduiActionEvent>() }

    // Parse JSON into SduiDocument
    val parseResult = remember(rawJson) {
        try {
            val doc = defaultSduiJson.decodeFromString<SduiDocument>(rawJson)
            Result.success(doc)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // State Store and Action Runner hoisted for live inspection
    val coroutineScope = rememberCoroutineScope()
    val currentDoc = parseResult.getOrNull()
    val store = remember(currentDoc) {
        SduiStateStore().apply {
            if (currentDoc != null) {
                loadFromJsonElements(currentDoc.initialState)
            }
        }
    }
    val actionRunner = remember(store, coroutineScope) {
        SduiActionRunner(
            store = store,
            coroutineScope = coroutineScope,
            onLogEvent = { event ->
                actionLogs.add(0, event)
                if (actionLogs.size > 50) actionLogs.removeLast()
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Server-Driven UI Studio") },
                navigationIcon = { TextButton(onClick = onBack) { Text("Back") } },
                actions = {
                    Text(
                        text = if (parseResult.isSuccess) "✓ AST Valid" else "✗ Syntax Error",
                        color = if (parseResult.isSuccess) Color(0xFF00E676) else MaterialTheme.colorScheme.error,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(end = 16.dp)
                    )
                }
            )
        },
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            // Preset Selection Bar
            ScrollableTabRow(
                selectedTabIndex = selectedPresetIndex,
                edgePadding = 16.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                presets.forEachIndexed { index, (name, _) ->
                    Tab(
                        selected = selectedPresetIndex == index,
                        onClick = {
                            selectedPresetIndex = index
                            rawJson = presets[index].second
                            actionLogs.clear()
                        },
                        text = { Text(name, fontSize = 13.sp) }
                    )
                }
            }

            // View Mode Navigation
            TabRow(selectedTabIndex = selectedTab, modifier = Modifier.fillMaxWidth()) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Preview") }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Kotlin DSL") }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("JSON Editor") }
                )
                Tab(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    text = { Text("State (${store.state.size})") }
                )
                Tab(
                    selected = selectedTab == 4,
                    onClick = { selectedTab = 4 },
                    text = { Text("Events (${actionLogs.size})") }
                )
            }

            // Content Panel
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                when (selectedTab) {
                    0 -> {
                        // Live SDUI Preview
                        if (currentDoc != null) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState())
                            ) {
                                SduiViewWithStore(
                                    document = currentDoc,
                                    store = store,
                                    actionRunner = actionRunner,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        } else {
                            val error = parseResult.exceptionOrNull()
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Red.copy(alpha = 0.08f), RoundedCornerShape(8.dp))
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Cannot Render UI due to Parse Error:\n\n${error?.message}",
                                    color = MaterialTheme.colorScheme.error,
                                    fontSize = 14.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                    1 -> {
                        // Generated Kotlin DSL View
                        val dslCode = remember(currentDoc) {
                            if (currentDoc != null) {
                                try {
                                    SduiDslGenerator.generate(currentDoc)
                                } catch (e: Exception) {
                                    "// Error generating DSL: ${e.message}"
                                }
                            } else {
                                "// No valid document to generate DSL from"
                            }
                        }
                        Column(modifier = Modifier.fillMaxSize()) {
                            Text(
                                "Generated Type-Safe Compose Kotlin DSL:",
                                fontSize = 12.sp,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            OutlinedTextField(
                                value = dslCode,
                                onValueChange = {},
                                readOnly = true,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .weight(1f),
                                textStyle = androidx.compose.ui.text.TextStyle(
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 12.sp
                                )
                            )
                        }
                    }
                    2 -> {
                        // Live JSON Editor
                        Column(modifier = Modifier.fillMaxSize()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Edit schema below (auto-recompiles):", fontSize = 12.sp)
                                OutlinedButton(onClick = { rawJson = presets[selectedPresetIndex].second }) {
                                    Text("Reset Preset", fontSize = 12.sp)
                                }
                            }
                            OutlinedTextField(
                                value = rawJson,
                                onValueChange = { rawJson = it },
                                modifier = Modifier
                                    .fillMaxSize()
                                    .weight(1f),
                                textStyle = androidx.compose.ui.text.TextStyle(
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 12.sp
                                )
                            )
                        }
                    }
                    3 -> {
                        // State Store Inspector
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .padding(8.dp)
                        ) {
                            Text(
                                "Live Reactive State Variables",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            if (store.state.isEmpty()) {
                                Text("No active state variables.", color = Color.Gray)
                            } else {
                                store.state.forEach { (key, value) ->
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(12.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = key,
                                                fontWeight = FontWeight.Bold,
                                                fontFamily = FontFamily.Monospace,
                                                fontSize = 14.sp
                                            )
                                            Text(
                                                text = value?.toString() ?: "null",
                                                color = MaterialTheme.colorScheme.primary,
                                                fontFamily = FontFamily.Monospace,
                                                fontSize = 14.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    4 -> {
                        // Action Event Logger
                        if (actionLogs.isEmpty()) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("Interact with buttons or gestures in Preview to see fired actions.")
                            }
                        } else {
                            LazyColumn(modifier = Modifier.fillMaxSize()) {
                                items(actionLogs) { log ->
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                                    ) {
                                        Column(modifier = Modifier.padding(10.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text(
                                                    text = log.actionType,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.primary,
                                                    fontSize = 13.sp
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = log.details,
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
            }
        }
    }
}
