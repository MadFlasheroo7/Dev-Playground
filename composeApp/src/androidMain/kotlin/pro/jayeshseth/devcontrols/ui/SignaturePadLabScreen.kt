package pro.jayeshseth.devcontrols.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp

private class Stroke2(val points: MutableList<Offset> = mutableListOf())

/** Signature pad — draw with the finger, adjustable width, clear, undo. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignaturePadLabScreen(onBack: () -> Unit) {
    val strokes = remember { mutableStateListOf<Stroke2>() }
    var current by remember { androidx.compose.runtime.mutableStateOf<Stroke2?>(null) }
    var width by remember { mutableFloatStateOf(6f) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Signature Pad") }, navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }) },
        modifier = Modifier.fillMaxSize()
    ) { inner ->
        Column(Modifier.padding(inner).fillMaxSize().padding(16.dp)) {
            Box(
                Modifier.fillMaxWidth().weight(1f).clip(RoundedCornerShape(12.dp)).background(Color.White)
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = { o -> current = Stroke2(mutableListOf(o)) },
                            onDragEnd = { current?.let { strokes.add(it) }; current = null }
                        ) { change, _ -> change.consume(); current?.points?.add(change.position) }
                    }
            ) {
                Canvas(Modifier.fillMaxSize()) {
                    fun draw(s: Stroke2) {
                        if (s.points.size < 2) return
                        val p = Path().apply {
                            moveTo(s.points[0].x, s.points[0].y)
                            for (i in 1 until s.points.size) lineTo(s.points[i].x, s.points[i].y)
                        }
                        drawPath(p, Color(0xFF101418), style = Stroke(width = width, cap = StrokeCap.Round))
                    }
                    strokes.forEach { draw(it) }
                    current?.let { draw(it) }
                }
            }
            Text("Pen width: ${width.toInt()}", style = MaterialTheme.typography.bodySmall)
            Slider(value = width, onValueChange = { width = it }, valueRange = 2f..24f)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { if (strokes.isNotEmpty()) strokes.removeAt(strokes.lastIndex) }) { Text("Undo") }
                Button(onClick = { strokes.clear() }) { Text("Clear") }
            }
        }
    }
}
