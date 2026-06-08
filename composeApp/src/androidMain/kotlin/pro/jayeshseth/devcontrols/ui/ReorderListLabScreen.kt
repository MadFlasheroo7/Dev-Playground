package pro.jayeshseth.devcontrols.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import kotlin.math.roundToInt

/**
 * Reorderable drag list. Long-press an item to lift it, drag to reorder. The dragged
 * item translates with the finger while the items it passes animate aside to open a
 * gap; the actual list reorder commits on release — so the gesture stays attached to a
 * single, stable node the whole drag.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReorderListLabScreen(onBack: () -> Unit) {
    val density = LocalDensity.current
    val itemHeight = 64.dp
    val gap = 8.dp
    val slotPx = with(density) { (itemHeight + gap).toPx() }
    val items = remember { mutableStateListOf(*Array(8) { "Item ${it + 1}" }) }

    var dragIndex by remember { mutableIntStateOf(-1) }
    var dragOffset by remember { mutableFloatStateOf(0f) }
    val target = if (dragIndex < 0) -1
        else (dragIndex + (dragOffset / slotPx).roundToInt()).coerceIn(0, items.size - 1)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Reorder List") },
                navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }
            )
        },
        modifier = Modifier.fillMaxSize()
    ) { inner ->
        Column(Modifier.padding(inner).fillMaxSize().padding(16.dp)) {
            Text("Long-press an item, then drag to reorder.", style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Box(Modifier.fillMaxSize().padding(top = 12.dp)) {
                Column {
                    items.forEachIndexed { i, label ->
                        val dragging = i == dragIndex
                        // shift items between source and target to make room
                        val shift = when {
                            dragIndex < 0 -> 0f
                            dragIndex < target && i > dragIndex && i <= target -> -slotPx
                            dragIndex > target && i < dragIndex && i >= target -> slotPx
                            else -> 0f
                        }
                        val animShift by animateFloatAsState(if (dragging) 0f else shift, label = "shift")
                        Box(
                            Modifier
                                .padding(bottom = gap)
                                .fillMaxWidth()
                                .height(itemHeight)
                                .zIndex(if (dragging) 1f else 0f)
                                .graphicsLayer {
                                    translationY = if (dragging) dragOffset else animShift
                                    if (dragging) { scaleX = 1.03f; scaleY = 1.03f }
                                }
                                .background(
                                    if (dragging) MaterialTheme.colorScheme.primaryContainer
                                    else MaterialTheme.colorScheme.surfaceVariant,
                                    RoundedCornerShape(12.dp)
                                )
                                .pointerInput(items.size) {
                                    detectDragGesturesAfterLongPress(
                                        onDragStart = { dragIndex = i; dragOffset = 0f },
                                        onDrag = { change, drag -> change.consume(); dragOffset += drag.y },
                                        onDragEnd = {
                                            if (dragIndex >= 0 && target != dragIndex) {
                                                items.add(target, items.removeAt(dragIndex))
                                            }
                                            dragIndex = -1; dragOffset = 0f
                                        },
                                        onDragCancel = { dragIndex = -1; dragOffset = 0f }
                                    )
                                },
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Text("  ☰  $label", color = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }
            }
        }
    }
}
