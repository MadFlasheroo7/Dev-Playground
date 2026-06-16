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

enum class IndexSearchType { TABLE_SCAN, B_TREE_INDEX }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DbIndexVisualizerScreen(onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    var selectedSearch by remember { mutableStateOf(IndexSearchType.TABLE_SCAN) }

    // Sorted database records
    val dbRecords = remember {
        listOf(
            2, 5, 8, 12, 16, 21, 25, 29, 33, 37,
            42, 45, 49, 53, 58, 62, 67, 73, 77, 81,
            85, 89, 92, 95, 98, 101, 105, 109, 112, 115,
            118, 122
        )
    }

    var targetValue by remember { mutableStateOf(73) }
    var currentIndexScanning by remember { mutableStateOf(-1) }
    var currentStepsTaken by remember { mutableStateOf(0) }
    var isSearching by remember { mutableStateOf(false) }

    // Binary search boundaries
    var binaryLeft by remember { mutableStateOf(0) }
    var binaryRight by remember { mutableStateOf(dbRecords.size - 1) }

    var logs = remember { mutableStateListOf<String>() }

    fun resetSim() {
        currentIndexScanning = -1
        currentStepsTaken = 0
        isSearching = false
        binaryLeft = 0
        binaryRight = dbRecords.size - 1
        logs.clear()
        logs.add("Database loaded with ${dbRecords.size} sorted rows.")
    }

    LaunchedEffect(selectedSearch, targetValue) {
        resetSim()
    }

    fun startTableScan() {
        scope.launch {
            resetSim()
            isSearching = true
            logs.add("Initiating full table scan for value: $targetValue...")

            for (i in dbRecords.indices) {
                if (!isSearching) break
                currentIndexScanning = i
                currentStepsTaken++
                delay(150) // animation delay

                if (dbRecords[i] == targetValue) {
                    logs.add("Row found at Index #$i in $currentStepsTaken database lookup steps!")
                    break
                }
            }
            isSearching = false
        }
    }

    fun startBinarySearch() {
        scope.launch {
            resetSim()
            isSearching = true
            logs.add("Querying using B-Tree index structure (Binary Search logic)...")

            var left = 0
            var right = dbRecords.size - 1

            while (left <= right) {
                if (!isSearching) break
                val mid = left + (right - left) / 2
                currentIndexScanning = mid
                binaryLeft = left
                binaryRight = right
                currentStepsTaken++
                delay(600) // longer delay to see the splits

                val midVal = dbRecords[mid]
                logs.add("Step $currentStepsTaken: Checking index #$mid (value $midVal). Range is now [$left, $right]")

                if (midVal == targetValue) {
                    logs.add("Row found at Index #$mid in just $currentStepsTaken steps!")
                    break
                }

                if (midVal < targetValue) {
                    left = mid + 1
                    logs.add("Discarding indices $binaryLeft to $mid (target is larger)")
                } else {
                    right = mid - 1
                    logs.add("Discarding indices $mid to $binaryRight (target is smaller)")
                }
            }
            isSearching = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Database Indexing Simulator", fontWeight = FontWeight.Bold) },
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
            // Tab Selector
            TabRow(selectedTabIndex = selectedSearch.ordinal) {
                IndexSearchType.values().forEach { type ->
                    Tab(
                        selected = selectedSearch == type,
                        onClick = { selectedSearch = type },
                        text = {
                            Text(
                                text = when (type) {
                                    IndexSearchType.TABLE_SCAN -> "Sequential Scan"
                                    IndexSearchType.B_TREE_INDEX -> "B-Tree Index"
                                },
                                fontWeight = if (selectedSearch == type) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // Selection controls
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Search Target: $targetValue", fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(21, 49, 73, 98, 115).forEach { v ->
                            FilterChip(
                                selected = targetValue == v,
                                onClick = { targetValue = v },
                                label = { Text("Val: $v") }
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Database Visualizer Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Database Table Grid (32 Rows)",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "Lookups: $currentStepsTaken",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.secondary,
                            fontSize = 14.sp
                        )
                    }
                    Spacer(Modifier.height(12.dp))

                    // Grid layout of database records
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        val chunks = dbRecords.chunked(8)
                        chunks.forEachIndexed { rowIndex, rowItems ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                rowItems.forEachIndexed { itemIndex, valItem ->
                                    val absoluteIndex = rowIndex * 8 + itemIndex
                                    val isScanning = absoluteIndex == currentIndexScanning
                                    val isWithinRange = selectedSearch == IndexSearchType.B_TREE_INDEX &&
                                            absoluteIndex in binaryLeft..binaryRight

                                    val cellColor = when {
                                        isScanning -> Color(0xFFFFCC00) // scanning (yellow)
                                        valItem == targetValue && absoluteIndex == currentIndexScanning -> Color(0xFF66BB6A) // found (green)
                                        selectedSearch == IndexSearchType.B_TREE_INDEX && !isWithinRange -> Color.DarkGray.copy(alpha = 0.2f) // discarded (grey)
                                        else -> Color(0xFF2E2E2E) // default active
                                    }

                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(35.dp)
                                            .background(cellColor, RoundedCornerShape(4.dp))
                                            .border(
                                                1.dp,
                                                if (valItem == targetValue) Color(0xFF66BB6A) else Color.Transparent,
                                                RoundedCornerShape(4.dp)
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = valItem.toString(),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (cellColor == Color.White) Color.Black else Color.White
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Trigger Search
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Box(modifier = Modifier.padding(16.dp)) {
                    Button(
                        onClick = {
                            if (selectedSearch == IndexSearchType.TABLE_SCAN) startTableScan()
                            else startBinarySearch()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isSearching
                    ) {
                        Text(if (isSearching) "Searching..." else "Execute Lookup")
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
                            text = getIndexLesson(selectedSearch),
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
                            "Algorithmic Loop Code",
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF80CBC4),
                            style = MaterialTheme.typography.titleSmall
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = getIndexCode(selectedSearch),
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
                    Text("Database Execution logs", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
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

fun getIndexLesson(type: IndexSearchType): String {
    return when (type) {
        IndexSearchType.TABLE_SCAN -> {
            "A Full Table Scan is the database fallback when there is no index.\n\n" +
            "Key Observations:\n" +
            "• Search scans sequentially from row 1 to row N.\n" +
            "• Time Complexity: O(N). Extremely slow on large tables (million rows requires million comparisons)."
        }
        IndexSearchType.B_TREE_INDEX -> {
            "B-Tree Indexes store references in a balanced tree structure sorted logically.\n\n" +
            "Key Observations:\n" +
            "• Uses division search (Binary Search logic).\n" +
            "• Each comparison cuts the search space in half.\n" +
            "• Time Complexity: O(log N). A table of 1 million rows only requires maximum 20 step operations."
        }
    }
}

fun getIndexCode(type: IndexSearchType): String {
    return when (type) {
        IndexSearchType.TABLE_SCAN -> {
            "// Sequential full table scan\n" +
            "fun scan(table: List<Int>, target: Int) {\n" +
            "    for (row in table) {\n" +
            "        if (row == target) {\n" +
            "            return found\n" +
            "        }\n" +
            "    }\n" +
            "}"
        }
        IndexSearchType.B_TREE_INDEX -> {
            "// Logarithmic index lookup\n" +
            "fun query(table: List<Int>, target: Int) {\n" +
            "    var L = 0\n" +
            "    var R = table.size - 1\n" +
            "    while (L <= R) {\n" +
            "        val mid = (L + R) / 2\n" +
            "        if (table[mid] == target) return mid\n" +
            "        if (table[mid] < target) L = mid + 1\n" +
            "        else R = mid - 1\n" +
            "    }\n" +
            "}"
        }
    }
}
