package pro.jayeshseth.devcontrols.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.systemGestureExclusion
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

/** Gesture Exclusion — a drag strip near the screen edge that opts out of the back gesture. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GestureExclusionLabScreen(onBack: () -> Unit) {
    var exclude by remember { mutableStateOf(true) }
    var value by remember { mutableFloatStateOf(0.5f) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Gesture Exclusion") }, navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }) },
        modifier = Modifier.fillMaxSize()
    ) { inner ->
        Column(Modifier.padding(inner).fillMaxSize().padding(16.dp)) {
            androidx.compose.foundation.layout.Row(Modifier.fillMaxWidth().height(48.dp),
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically) {
                Text("Opt out of back gesture", style = MaterialTheme.typography.bodyLarge)
                Switch(checked = exclude, onCheckedChange = { exclude = it })
            }
            Text("Drag the strip horizontally from the very edge. With exclusion ON it won't trigger the system back; OFF, the OS steals the swipe.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(24.dp))
            Text("Value: ${(value * 100).roundToInt()}", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Box(
                Modifier.fillMaxWidth().height(80.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(12.dp))
                    .then(if (exclude) Modifier.systemGestureExclusion() else Modifier)
                    .pointerInput(Unit) {
                        detectHorizontalDragGestures { _, drag -> value = (value + drag / size.width).coerceIn(0f, 1f) }
                    },
                contentAlignment = Alignment.CenterStart
            ) {
                Box(Modifier.fillMaxWidth(value).height(80.dp)
                    .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp)))
                Text("  ⟷ drag me", Modifier.padding(12.dp), color = MaterialTheme.colorScheme.onPrimaryContainer)
            }
        }
    }
}
