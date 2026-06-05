package pro.jayeshseth.devcontrols.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

private class KCard(val id: Int, val text: String, initialCol: Int) {
    var col by mutableStateOf(initialCol)
}

/** Kanban — drag a card horizontally past a threshold to move it between columns. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KanbanLabScreen(onBack: () -> Unit) {
    val titles = listOf("To Do", "Doing", "Done")
    val cards = remember {
        mutableStateListOf(
            KCard(1, "Wire ADB gate", 0), KCard(2, "Spectrum FFT", 0), KCard(3, "Glitch shader", 1),
            KCard(4, "PiP window", 1), KCard(5, "Calendar range", 2), KCard(6, "Tag input", 0)
        )
    }
    Scaffold(
        topBar = { TopAppBar(title = { Text("Kanban") }, navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }) },
        modifier = Modifier.fillMaxSize()
    ) { inner ->
        Column(Modifier.padding(inner).fillMaxSize().padding(8.dp)) {
            Text("Drag a card sideways to move columns.", style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(8.dp))
            Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                titles.forEachIndexed { ci, title ->
                    Column(Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)).padding(6.dp)) {
                        Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, modifier = Modifier.padding(4.dp))
                        Spacer(Modifier.height(4.dp))
                        cards.filter { it.col == ci }.forEach { card ->
                            DraggableCard(card.text,
                                canLeft = ci > 0, canRight = ci < titles.lastIndex,
                                onLeft = { card.col = ci - 1 }, onRight = { card.col = ci + 1 })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DraggableCard(text: String, canLeft: Boolean, canRight: Boolean, onLeft: () -> Unit, onRight: () -> Unit) {
    var dragX by remember { mutableFloatStateOf(0f) }
    Card(
        Modifier.fillMaxWidth().padding(vertical = 4.dp)
            .graphicsLayer { translationX = dragX }
            .pointerInput(canLeft, canRight) {
                detectDragGestures(
                    onDragEnd = {
                        when {
                            dragX < -120f && canLeft -> onLeft()
                            dragX > 120f && canRight -> onRight()
                        }
                        dragX = 0f
                    },
                    onDragCancel = { dragX = 0f }
                ) { change, drag -> change.consume(); dragX += drag.x }
            }
    ) {
        Text(text, Modifier.padding(12.dp), style = MaterialTheme.typography.bodyMedium)
    }
}
