package pro.jayeshseth.devcontrols.ui.toys

import android.media.AudioManager
import android.media.ToneGenerator
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/** Bubble wrap — tap each bubble to pop it (with a click). Satisfying. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BubbleWrapScreen(onBack: () -> Unit) {
    val popped = remember { androidx.compose.runtime.mutableStateMapOf<Int, Boolean>() }
    val tone = remember { ToneGenerator(AudioManager.STREAM_MUSIC, 90) }
    DisposableEffect(Unit) { onDispose { tone.release() } }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Bubble Wrap") }, navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }) },
        modifier = Modifier.fillMaxSize()
    ) { inner ->
        Column(Modifier.padding(inner).fillMaxSize()) {
            Text("Pop them all. (${popped.size} popped)", Modifier.padding(12.dp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            LazyVerticalGrid(columns = GridCells.Adaptive(56.dp), modifier = Modifier.fillMaxSize().padding(8.dp)) {
                items((0 until 160).toList()) { i ->
                    val isPopped = popped[i] == true
                    Box(Modifier.padding(5.dp).aspectRatio(1f), contentAlignment = Alignment.Center) {
                        Box(
                            Modifier.fillMaxSize().scale(if (isPopped) 0.7f else 1f).clip(CircleShape)
                                .background(if (isPopped) MaterialTheme.colorScheme.surfaceVariant else Color(0xFF80DEEA))
                                .clickable(enabled = !isPopped) { popped[i] = true; tone.startTone(ToneGenerator.TONE_PROP_BEEP, 40) }
                        )
                    }
                }
            }
        }
    }
}
