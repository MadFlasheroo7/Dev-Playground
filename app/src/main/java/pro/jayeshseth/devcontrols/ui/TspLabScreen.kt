package pro.jayeshseth.devcontrols.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlin.math.hypot

/** TSP — nearest-neighbour tour, then 2-opt improvement animated to a near-optimal loop. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TspLabScreen(onBack: () -> Unit) {
    var size by remember { mutableStateOf(IntSize.Zero) }
    val cities = remember { mutableStateListOf<Offset>() }
    val tour = remember { mutableStateListOf<Int>() }
    var len by remember { mutableIntStateOf(0) }
    var gen by remember { mutableIntStateOf(0) }

    fun dist(a: Offset, b: Offset) = hypot(a.x - b.x, a.y - b.y)
    fun tourLen(t: List<Int>) = t.indices.sumOf { dist(cities[t[it]], cities[t[(it + 1) % t.size]]).toDouble() }.toFloat()

    fun regen() {
        if (size == IntSize.Zero) return
        cities.clear()
        repeat(18) { cities.add(Offset(40f + Math.random().toFloat() * (size.width - 80), 120f + Math.random().toFloat() * (size.height - 200))) }
        // nearest neighbour
        val visited = BooleanArray(cities.size); val t = ArrayList<Int>(); var cur = 0
        visited[0] = true; t.add(0)
        repeat(cities.size - 1) {
            var best = -1; var bd = Float.MAX_VALUE
            for (j in cities.indices) if (!visited[j]) { val d = dist(cities[cur], cities[j]); if (d < bd) { bd = d; best = j } }
            visited[best] = true; t.add(best); cur = best
        }
        tour.clear(); tour.addAll(t); len = tourLen(t).toInt(); gen++
    }

    LaunchedEffect(size) { if (cities.isEmpty()) regen() }

    LaunchedEffect(gen) {
        // 2-opt passes
        var improved = true
        while (improved) {
            improved = false
            for (i in 0 until tour.size - 1) {
                for (k in i + 1 until tour.size) {
                    val a = cities[tour[i]]; val b = cities[tour[(i + 1) % tour.size]]
                    val c = cities[tour[k]]; val d = cities[tour[(k + 1) % tour.size]]
                    if (dist(a, b) + dist(c, d) > dist(a, c) + dist(b, d) + 0.01f) {
                        var lo = i + 1; var hi = k
                        while (lo < hi) { val tmp = tour[lo]; tour[lo] = tour[hi]; tour[hi] = tmp; lo++; hi-- }
                        len = tourLen(tour).toInt(); improved = true
                        delay(40)
                    }
                }
            }
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("TSP (2-opt)") }, navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }) },
        modifier = Modifier.fillMaxSize()
    ) { inner ->
        Column(Modifier.padding(inner).fillMaxSize()) {
            Row(Modifier.fillMaxWidth().padding(12.dp)) {
                Text("Tour length: $len", Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                Button(onClick = { regen() }) { Text("New cities") }
            }
            Box(Modifier.fillMaxSize().background(Color(0xFF0C0C14)).onSizeChanged { size = it }.drawBehind {
                if (tour.size == cities.size && tour.isNotEmpty()) {
                    for (i in tour.indices) {
                        val a = cities[tour[i]]; val b = cities[tour[(i + 1) % tour.size]]
                        drawLine(Color(0xFF22FFAA), a, b, strokeWidth = 3f)
                    }
                }
                cities.forEach { drawCircle(Color(0xFFFF6CAB), 8f, it) }
            })
        }
    }
}
