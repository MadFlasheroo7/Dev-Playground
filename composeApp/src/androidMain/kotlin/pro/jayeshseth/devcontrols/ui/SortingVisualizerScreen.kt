package pro.jayeshseth.devcontrols.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlin.random.Random

private enum class SortAlgo { Bubble, Insertion, Selection, Quick }

/** Sorting visualizer — bars animate through the chosen algorithm; compared bars glow. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SortingVisualizerScreen(onBack: () -> Unit) {
    val n = 64
    val arr = remember { IntArray(n) { it + 1 } }
    var version by remember { mutableIntStateOf(0) }
    var hiA by remember { mutableIntStateOf(-1) }
    var hiB by remember { mutableIntStateOf(-1) }
    var algo by remember { mutableStateOf(SortAlgo.Bubble) }
    var running by remember { mutableStateOf(false) }

    fun shuffle() {
        for (i in n - 1 downTo 1) { val j = Random.nextInt(i + 1); val t = arr[i]; arr[i] = arr[j]; arr[j] = t }
        hiA = -1; hiB = -1; version++
    }
    LaunchedEffect(Unit) { shuffle() }

    LaunchedEffect(running) {
        if (!running) return@LaunchedEffect
        suspend fun step(a: Int, b: Int) { hiA = a; hiB = b; version++; delay(6) }
        fun swap(a: Int, b: Int) { val t = arr[a]; arr[a] = arr[b]; arr[b] = t; version++ }
        suspend fun bubble() { for (i in 0 until n) for (j in 0 until n - 1 - i) { step(j, j + 1); if (arr[j] > arr[j + 1]) swap(j, j + 1) } }
        suspend fun insertion() { for (i in 1 until n) { var j = i; while (j > 0) { step(j - 1, j); if (arr[j - 1] > arr[j]) { swap(j - 1, j); j-- } else break } } }
        suspend fun selection() { for (i in 0 until n) { var m = i; for (j in i + 1 until n) { step(m, j); if (arr[j] < arr[m]) m = j }; swap(i, m) } }
        suspend fun qs(lo: Int, hi: Int) { if (lo >= hi) return; val pivot = arr[hi]; var i = lo; for (j in lo until hi) { step(j, hi); if (arr[j] < pivot) { swap(i, j); i++ } }; swap(i, hi); qs(lo, i - 1); qs(i + 1, hi) }
        when (algo) {
            SortAlgo.Bubble -> bubble(); SortAlgo.Insertion -> insertion()
            SortAlgo.Selection -> selection(); SortAlgo.Quick -> qs(0, n - 1)
        }
        hiA = -1; hiB = -1; version++; running = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Sorting") },
                navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }
            )
        },
        modifier = Modifier.fillMaxSize()
    ) { inner ->
        Column(Modifier.padding(inner).fillMaxSize().padding(12.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                SortAlgo.entries.forEach { a -> FilterChip(algo == a, { if (!running) algo = a }, { Text(a.name) }) }
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { running = !running }) { Text(if (running) "Stop" else "Run") }
                OutlinedButton(onClick = { shuffle() }, enabled = !running) { Text("Shuffle") }
            }
            Spacer(Modifier.height(12.dp))
            Box(
                Modifier.fillMaxSize().background(Color(0xFF0A0A12)).drawBehind {
                    version
                    val bw = size.width / n
                    for (i in 0 until n) {
                        val h = arr[i] / n.toFloat() * size.height
                        val col = if (i == hiA || i == hiB) Color.White else Color(0xFF18FFFF)
                        drawRect(col, topLeft = Offset(i * bw, size.height - h), size = Size(bw - 1f, h))
                    }
                }
            )
        }
    }
}
