package pro.jayeshseth.devcontrols.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs

/** Page-curl / book flip — drag horizontally to fold the top page over its left edge (rotationY). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PageCurlLabScreen(onBack: () -> Unit) {
    var page by remember { mutableIntStateOf(0) }
    var fold by remember { mutableFloatStateOf(0f) } // 0..1 of the flip
    val palette = listOf(Color(0xFF26A69A), Color(0xFFEF5350), Color(0xFF5C6BC0), Color(0xFFFFA726), Color(0xFF8D6E63))

    Scaffold(
        topBar = { TopAppBar(title = { Text("Page Curl") }, navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }) },
        modifier = Modifier.fillMaxSize()
    ) { inner ->
        Box(Modifier.padding(inner).fillMaxSize()
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragEnd = {
                        if (fold > 0.5f && page < palette.lastIndex) page++
                        fold = 0f
                    }
                ) { _, dx -> fold = (fold - dx / size.width).coerceIn(0f, 1f) }
            }
        ) {
            // next page underneath
            PageFace(palette[(page + 1).coerceAtMost(palette.lastIndex)], page + 2, Modifier.fillMaxSize())
            // current page folding over
            PageFace(
                palette[page], page + 1,
                Modifier.fillMaxSize().graphicsLayer {
                    transformOrigin = TransformOrigin(0f, 0.5f)
                    rotationY = fold * 100f
                    cameraDistance = 14f * density
                    shadowElevation = fold * 30f
                }
            )
            Text("Drag left to flip ›", Modifier.align(Alignment.BottomCenter).padding(16.dp), color = Color.White)
        }
    }
}

@Composable
private fun PageFace(color: Color, number: Int, modifier: Modifier) {
    Box(modifier.background(Brush.linearGradient(listOf(color, color.copy(alpha = 0.7f)))), contentAlignment = Alignment.Center) {
        Text("Page $number", color = Color.White, fontSize = 40.sp, fontWeight = FontWeight.Bold)
    }
}
