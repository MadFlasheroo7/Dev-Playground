package pro.jayeshseth.devcontrols.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

/** Custom expandable bottom sheet — drag the handle (or release past the midpoint) to snap. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BottomSheetLabScreen(onBack: () -> Unit) {
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Bottom Sheet") },
                navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }
            )
        },
        modifier = Modifier.fillMaxSize()
    ) { inner ->
        BoxWithConstraints(Modifier.padding(inner).fillMaxSize().background(Color(0xFF101018))) {
            val sheetH = with(density) { (maxHeight * 0.72f).toPx() }
            val peek = with(density) { 130.dp.toPx() }
            val collapsed = sheetH - peek
            val offset = remember { Animatable(collapsed) }

            Text("Drag the sheet handle ↓", color = Color.White, modifier = Modifier.align(Alignment.TopCenter).padding(24.dp))

            Box(
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(with(density) { sheetH.toDp() })
                    .graphicsLayer { translationY = offset.value }
                    .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .pointerInput(Unit) {
                        detectVerticalDragGestures(
                            onVerticalDrag = { _, dy -> scope.launch { offset.snapTo((offset.value + dy).coerceIn(0f, collapsed)) } },
                            onDragEnd = {
                                val target = if (offset.value < collapsed / 2f) 0f else collapsed
                                scope.launch { offset.animateTo(target, spring()) }
                            }
                        )
                    }
            ) {
                Column(Modifier.fillMaxSize().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(Modifier.size(width = 44.dp, height = 5.dp).clip(RoundedCornerShape(3.dp)).background(Color.Gray))
                    Spacer(Modifier.height(16.dp))
                    Text("Expandable sheet", style = MaterialTheme.typography.titleLarge)
                    Text("Drag this handle up to expand, down to collapse. Releases snap to the nearest state.",
                        style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(16.dp))
                    repeat(6) { Text("• Detail row ${it + 1}", modifier = Modifier.padding(vertical = 6.dp)) }
                }
            }
        }
    }
}
