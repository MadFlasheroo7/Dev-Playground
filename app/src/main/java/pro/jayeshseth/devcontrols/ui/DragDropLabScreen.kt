package pro.jayeshseth.devcontrols.ui

import android.content.ClipData
import androidx.compose.foundation.background
import androidx.compose.foundation.draganddrop.dragAndDropSource
import androidx.compose.foundation.draganddrop.dragAndDropTarget
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draganddrop.DragAndDropEvent
import androidx.compose.ui.draganddrop.DragAndDropTarget
import androidx.compose.ui.draganddrop.DragAndDropTransferData
import androidx.compose.ui.draganddrop.toAndroidDragEvent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Drag-and-drop between two lists using the platform Compose DnD API. Long-press an
 * item to start a drag (it carries its label as [ClipData]); drop it onto the other
 * column's [dragAndDropTarget] to move it there. Targets highlight while hovered.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DragDropLabScreen(onBack: () -> Unit) {
    val available = remember { mutableStateListOf("Apple", "Banana", "Cherry", "Date", "Elderberry") }
    val selected = remember { mutableStateListOf<String>() }

    fun move(item: String, toSelected: Boolean) {
        available.remove(item); selected.remove(item)
        if (toSelected) selected.add(item) else available.add(item)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Drag & Drop") },
                navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }
            )
        },
        modifier = Modifier.fillMaxSize()
    ) { inner ->
        Column(Modifier.padding(inner).fillMaxSize().padding(12.dp)) {
            Text("Long-press an item and drag it to the other column.", style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                DropColumn("Available", available, toSelected = false, onMove = ::move, modifier = Modifier.weight(1f).fillMaxHeight())
                DropColumn("Selected", selected, toSelected = true, onMove = ::move, modifier = Modifier.weight(1f).fillMaxHeight())
            }
        }
    }
}

@Composable
private fun DropColumn(
    title: String,
    items: List<String>,
    toSelected: Boolean,
    onMove: (String, Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    var hovered by remember { mutableStateOf(false) }
    val target = remember {
        object : DragAndDropTarget {
            override fun onEntered(event: DragAndDropEvent) { hovered = true }
            override fun onExited(event: DragAndDropEvent) { hovered = false }
            override fun onEnded(event: DragAndDropEvent) { hovered = false }
            override fun onDrop(event: DragAndDropEvent): Boolean {
                val text = event.toAndroidDragEvent().clipData?.getItemAt(0)?.text?.toString() ?: return false
                onMove(text, toSelected)
                hovered = false
                return true
            }
        }
    }

    Column(
        modifier
            .background(
                if (hovered) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                RoundedCornerShape(16.dp)
            )
            .dragAndDropTarget(shouldStartDragAndDrop = { true }, target = target)
            .padding(10.dp)
    ) {
        Text(title, style = MaterialTheme.typography.titleSmall)
        Spacer(Modifier.height(8.dp))
        items.forEach { item ->
            Text(
                item,
                color = Color.White,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(10.dp))
                    .padding(14.dp)
                    .dragAndDropSource(
                        transferData = { DragAndDropTransferData(ClipData.newPlainText("item", item)) }
                    )
            )
        }
    }
}
