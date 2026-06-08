package pro.jayeshseth.devcontrols.ui

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Drawing / whiteboard lab. A raw pointer loop collects each stroke's points **with
 * pressure**, so the stroke width tracks how hard a stylus presses (segments are drawn
 * at a width interpolated from the two endpoints' pressure). Undo pops the last stroke;
 * Export captures the canvas via a [rememberGraphicsLayer] and writes a PNG to the
 * gallery (MediaStore, no permission needed on API 29+).
 */
private data class InkPoint(val pos: Offset, val width: Float)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DrawingLabScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val strokes = remember { androidx.compose.runtime.mutableStateListOf<List<InkPoint>>() }
    var current by remember { mutableStateOf<List<InkPoint>>(emptyList()) }
    val graphicsLayer = rememberGraphicsLayer()

    fun widthFor(pressure: Float): Float {
        val p = if (pressure <= 0f) 0.5f else pressure.coerceIn(0f, 1f)
        return 3f + p * 22f
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Drawing") },
                navigationIcon = { TextButton(onClick = onBack) { Text("Back") } },
                actions = {
                    TextButton(onClick = { if (strokes.isNotEmpty()) strokes.removeAt(strokes.size - 1) }) { Text("Undo") }
                    TextButton(onClick = { strokes.clear() }) { Text("Clear") }
                }
            )
        },
        modifier = Modifier.fillMaxSize()
    ) { inner ->
        Column(Modifier.padding(inner).fillMaxSize()) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .drawWithContent {
                        graphicsLayer.record {
                            drawRect(Color.White)
                            strokes.forEach { drawStroke(it) }
                            if (current.size > 1) drawStroke(current)
                        }
                        drawLayer(graphicsLayer)
                    }
                    .pointerInput(Unit) {
                        awaitPointerEventScope {
                            while (true) {
                                val down = awaitFirstDown()
                                val pts = mutableListOf(InkPoint(down.position, widthFor(down.pressure)))
                                current = pts.toList()
                                down.consume()
                                var dragging = true
                                while (dragging) {
                                    val event = awaitPointerEvent()
                                    event.changes.forEach { ch ->
                                        if (ch.pressed) {
                                            pts.add(InkPoint(ch.position, widthFor(ch.pressure)))
                                            ch.consume()
                                        }
                                    }
                                    current = pts.toList()
                                    dragging = event.changes.any { it.pressed }
                                }
                                strokes.add(pts.toList())
                                current = emptyList()
                            }
                        }
                    }
            )
            Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = {
                    scope.launch {
                        val bitmap = graphicsLayer.toImageBitmap().asAndroidBitmap()
                        val ok = withContext(Dispatchers.IO) { saveToGallery(context, bitmap) }
                        Toast.makeText(context, if (ok) "Saved to Pictures/DevControls" else "Save failed", Toast.LENGTH_SHORT).show()
                    }
                }) { Text("Export PNG") }
                Text(
                    "Use a stylus to feel pressure-varying width.",
                    style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                    color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun DrawScope.drawStroke(pts: List<InkPoint>) {
    for (i in 1 until pts.size) {
        drawLine(
            color = Color(0xFF111111),
            start = pts[i - 1].pos,
            end = pts[i].pos,
            strokeWidth = (pts[i - 1].width + pts[i].width) / 2f,
            cap = StrokeCap.Round
        )
    }
}

private fun saveToGallery(context: Context, bitmap: Bitmap): Boolean = runCatching {
    val values = ContentValues().apply {
        put(MediaStore.Images.Media.DISPLAY_NAME, "drawing_${System.currentTimeMillis()}.png")
        put(MediaStore.Images.Media.MIME_TYPE, "image/png")
        put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/DevControls")
    }
    val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
        ?: return false
    context.contentResolver.openOutputStream(uri)?.use { out ->
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
    }
    true
}.getOrDefault(false)
