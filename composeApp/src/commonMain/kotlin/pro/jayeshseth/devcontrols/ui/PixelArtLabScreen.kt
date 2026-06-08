package pro.jayeshseth.devcontrols.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp

/** Pixel-art editor — paint a 16x16 grid; drag to draw, pick colors, clear. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PixelArtLabScreen(onBack: () -> Unit) {
    val grid = 16
    val pixels = remember { mutableStateListOf<Int>().apply { repeat(grid * grid) { add(Color.Transparent.toArgb()) } } }
    val palette = listOf(
        Color(0xFF000000), Color(0xFFFFFFFF), Color(0xFFE53935), Color(0xFFFB8C00),
        Color(0xFFFDD835), Color(0xFF43A047), Color(0xFF1E88E5), Color(0xFF8E24AA),
        Color(0xFF6D4C41), Color(0xFFEC407A), Color(0xFF00ACC1), Color.Transparent
    )
    var selected by remember { mutableIntStateOf(0) }

    fun paintAt(x: Float, y: Float, cellPx: Float) {
        val cx = (x / cellPx).toInt(); val cy = (y / cellPx).toInt()
        if (cx in 0 until grid && cy in 0 until grid) pixels[cy * grid + cx] = palette[selected].toArgb()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Pixel Art") },
                navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }
            )
        },
        modifier = Modifier.fillMaxWidth()
    ) { inner ->
        Column(Modifier.padding(inner).fillMaxWidth().padding(12.dp)) {
            Box(
                Modifier.fillMaxWidth().aspectRatio(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFFEDEDED))
                    .pointerInput(selected) {
                        val cellPx = size.width / grid.toFloat()
                        detectTapGestures { paintAt(it.x, it.y, cellPx) }
                    }
                    .pointerInput(selected) {
                        val cellPx = size.width / grid.toFloat()
                        detectDragGestures { change, _ -> paintAt(change.position.x, change.position.y, cellPx) }
                    }
                    .drawBehind {
                        val cellPx = size.width / grid
                        // checker for transparency
                        for (gy in 0 until grid) for (gx in 0 until grid) {
                            val c = Color(pixels[gy * grid + gx])
                            if (c.alpha == 0f) {
                                if ((gx + gy) % 2 == 0) drawRect(Color(0xFFDDDDDD), Offset(gx * cellPx, gy * cellPx), Size(cellPx, cellPx))
                            } else {
                                drawRect(c, Offset(gx * cellPx, gy * cellPx), Size(cellPx, cellPx))
                            }
                        }
                        // grid lines
                        for (i in 0..grid) {
                            drawLine(Color(0x22000000), Offset(i * cellPx, 0f), Offset(i * cellPx, size.height))
                            drawLine(Color(0x22000000), Offset(0f, i * cellPx), Offset(size.width, i * cellPx))
                        }
                    }
            )
            Spacer(Modifier.height(12.dp))
            Text("Palette", style = MaterialTheme.typography.labelMedium)
            Spacer(Modifier.height(6.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                palette.forEachIndexed { i, c ->
                    Box(
                        Modifier.size(28.dp).clip(CircleShape)
                            .background(if (c.alpha == 0f) Color(0xFFEDEDED) else c)
                            .border(if (selected == i) 3.dp else 1.dp,
                                if (selected == i) MaterialTheme.colorScheme.primary else Color(0x44000000), CircleShape)
                            .pointerInput(Unit) { detectTapGestures { selected = i } }
                    ) { if (c.alpha == 0f) Text("⌫", style = MaterialTheme.typography.labelSmall) }
                }
            }
            Spacer(Modifier.height(12.dp))
            Button(onClick = { for (i in pixels.indices) pixels[i] = Color.Transparent.toArgb() }) { Text("Clear") }
        }
    }
}
