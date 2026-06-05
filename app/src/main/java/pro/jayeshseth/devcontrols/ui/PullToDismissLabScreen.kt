package pro.jayeshseth.devcontrols.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

/** Pull-to-dismiss sheet — drag the sheet down past a threshold to dismiss; it springs back otherwise. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PullToDismissLabScreen(onBack: () -> Unit) {
    var visible by remember { mutableStateOf(true) }
    val offsetY = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = { TopAppBar(title = { Text("Pull to Dismiss") }, navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }) },
        modifier = Modifier.fillMaxSize()
    ) { inner ->
        Box(Modifier.padding(inner).fillMaxSize().background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)), contentAlignment = Alignment.Center) {
            if (!visible) {
                Button(onClick = { scope.launch { offsetY.snapTo(0f); visible = true } }) { Text("Bring sheet back") }
            } else {
                Box(
                    Modifier.fillMaxWidth(0.85f).height(420.dp)
                        .graphicsLayer { translationY = offsetY.value; alpha = (1f - offsetY.value / 900f).coerceIn(0f, 1f) }
                        .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(28.dp))
                        .pointerInput(Unit) {
                            detectVerticalDragGestures(
                                onDragEnd = {
                                    scope.launch {
                                        if (offsetY.value > 260f) {
                                            offsetY.animateTo(1200f, spring()); visible = false
                                        } else offsetY.animateTo(0f, spring())
                                    }
                                }
                            ) { _, dy -> scope.launch { offsetY.snapTo((offsetY.value + dy).coerceAtLeast(0f)) } }
                        },
                    contentAlignment = Alignment.TopCenter
                ) {
                    Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(Modifier.padding(8.dp).height(5.dp).fillMaxWidth(0.25f).background(MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.4f), RoundedCornerShape(3.dp)))
                        Text("Drag me down to dismiss", Modifier.padding(top = 24.dp), style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        Text("Release past the threshold to fling it away; otherwise it springs back.",
                            Modifier.padding(top = 12.dp), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                }
            }
        }
    }
}
