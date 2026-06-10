package pro.jayeshseth.devcontrols.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin

private class MStroke(val from: Offset, val to: Offset, val color: Color)

/** Mandala pad — k-fold radial symmetry drawing with mirrored reflections. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun MandalaPadLabScreen(onBack: () -> Unit) {
    val strokes = remember { mutableStateListOf<MStroke>() }
    var folds by remember { mutableIntStateOf(8) }
    var last by remember { androidx.compose.runtime.mutableStateOf<Offset?>(null) }
    val hueBase = remember { intArrayOf(0) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Mandala Pad") }, navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }) },
        modifier = Modifier.fillMaxSize()
    ) { inner ->
        Column(Modifier.padding(inner).fillMaxSize()) {
            FlowRow(Modifier.padding(8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(4, 6, 8, 12, 16).forEach { f -> FilterChip(folds == f, { folds = f }, { Text("$f") }) }
                OutlinedButton(onClick = { strokes.clear() }) { Text("Clear") }
            }
            Box(Modifier.fillMaxSize().background(Color(0xFF07070F))
                .pointerInput(folds) {
                    detectDragGestures(
                        onDragStart = { last = it },
                        onDragEnd = { last = null }
                    ) { change, _ ->
                        val cur = change.position; val prev = last ?: cur
                        hueBase[0] = (hueBase[0] + 2) % 360
                        strokes.add(MStroke(prev, cur, Color.hsv(hueBase[0].toFloat(), 0.8f, 1f)))
                        last = cur
                    }
                }) {
                Canvas(Modifier.fillMaxSize()) {
                    val cx = size.width / 2f; val cy = size.height / 2f
                    fun rot(p: Offset, a: Float): Offset {
                        val dx = p.x - cx; val dy = p.y - cy
                        return Offset(cx + dx * cos(a) - dy * sin(a), cy + dx * sin(a) + dy * cos(a))
                    }
                    for (s in strokes) {
                        for (k in 0 until folds) {
                            val a = k * (2 * kotlin.math.PI.toFloat() / folds)
                            drawLine(s.color, rot(s.from, a), rot(s.to, a), 3f, cap = StrokeCap.Round)
                            // mirror
                            val mf = Offset(2 * cx - s.from.x, s.from.y); val mt = Offset(2 * cx - s.to.x, s.to.y)
                            drawLine(s.color.copy(alpha = 0.7f), rot(mf, a), rot(mt, a), 3f, cap = StrokeCap.Round)
                        }
                    }
                }
            }
        }
    }
}
