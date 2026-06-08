package pro.jayeshseth.devcontrols.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.abs

/** N-Queens — watch the backtracking search place and retract queens. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NQueensLabScreen(onBack: () -> Unit) {
    var nF by remember { mutableFloatStateOf(8f) }
    val n = nF.toInt()
    val queens = remember { mutableStateListOf<Int>() } // queens[row] = col, size grows/shrinks
    var attempts by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()
    var job by remember { androidx.compose.runtime.mutableStateOf<Job?>(null) }

    fun start() {
        job?.cancel()
        queens.clear(); attempts = 0
        job = scope.launch {
            suspend fun place(row: Int): Boolean {
                if (row == n) return true
                for (col in 0 until n) {
                    if (!isActive) return false
                    val ok = (0 until row).all { r -> queens[r] != col && abs(queens[r] - col) != row - r }
                    if (ok) {
                        queens.add(col); attempts++
                        delay(60)
                        if (place(row + 1)) return true
                        queens.removeAt(queens.size - 1)
                        delay(20)
                    }
                }
                return false
            }
            place(0)
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("N-Queens") }, navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }) },
        modifier = Modifier.fillMaxSize()
    ) { inner ->
        Column(Modifier.padding(inner).fillMaxSize().padding(16.dp)) {
            Text("N = $n   ·   placements tried: $attempts", style = MaterialTheme.typography.bodyMedium)
            Slider(value = nF, onValueChange = { nF = it; job?.cancel(); queens.clear() }, valueRange = 4f..10f, steps = 5)
            Button(onClick = { start() }) { Text("Solve") }
            Box(Modifier.fillMaxWidth().aspectRatio(1f).padding(top = 16.dp)) {
                Column(Modifier.fillMaxSize()) {
                    for (r in 0 until n) {
                        Row(Modifier.weight(1f).fillMaxWidth()) {
                            for (c in 0 until n) {
                                val dark = (r + c) % 2 == 1
                                val hasQueen = r < queens.size && queens[r] == c
                                Box(Modifier.weight(1f).fillMaxSize()
                                    .background(if (dark) Color(0xFF6D4C41) else Color(0xFFD7CCC8)),
                                    contentAlignment = Alignment.Center) {
                                    if (hasQueen) Text("♛", color = Color(0xFF1B5E20), style = MaterialTheme.typography.titleLarge)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
