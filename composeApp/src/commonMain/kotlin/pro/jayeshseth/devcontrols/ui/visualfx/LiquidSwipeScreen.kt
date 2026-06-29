package pro.jayeshseth.devcontrols.ui.visualfx

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.sin

private val PAGES = listOf(
    Triple("Explore", Color(0xFF1565C0), "Drag from the right edge →"),
    Triple("Create", Color(0xFFAD1457), "The next page pours in"),
    Triple("Share", Color(0xFF2E7D32), "with a liquid edge"),
    Triple("Done", Color(0xFFE65100), "Loops back to start")
)

private class LiquidEdge(val p: Float, val cy: Float) : Shape {
    override fun createOutline(size: androidx.compose.ui.geometry.Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val w = size.width; val h = size.height
        val edge = w * (1f - p)
        val bulge = w * 0.22f * sin((p.coerceIn(0f, 1f)) * PI).toFloat()
        val path = Path().apply {
            moveTo(w, 0f); lineTo(edge, 0f)
            cubicTo(edge, cy - h * 0.22f, edge - bulge, cy - h * 0.12f, edge - bulge, cy)
            cubicTo(edge - bulge, cy + h * 0.12f, edge, cy + h * 0.22f, edge, h)
            lineTo(w, h); close()
        }
        return Outline.Generic(path)
    }
}

/** Liquid swipe — drag from the right; the next page reveals along a gooey curved edge with smooth spring release animations. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiquidSwipeScreen(onBack: () -> Unit) {
    var page by remember { mutableIntStateOf(0) }
    val progressAnim = remember { Animatable(0f) }
    var cy by remember { mutableFloatStateOf(0f) }
    val next = (page + 1) % PAGES.size
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        topBar = { TopAppBar(title = { Text("Liquid Swipe") }, navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }) },
        modifier = Modifier.fillMaxSize()
    ) { inner ->
        Box(
            Modifier
                .padding(inner)
                .fillMaxSize()
                .pointerInput(page) {
                    if (cy == 0f) cy = size.height / 2f
                    detectDragGestures(
                        onDragStart = { o -> cy = o.y },
                        onDragEnd = {
                            coroutineScope.launch {
                                if (progressAnim.value > 0.35f) {
                                    // Complete the sweep animation
                                    progressAnim.animateTo(
                                        targetValue = 1f,
                                        animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
                                    )
                                    page = next
                                    progressAnim.snapTo(0f)
                                } else {
                                    // Retract page back to edge
                                    progressAnim.animateTo(
                                        targetValue = 0f,
                                        animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
                                    )
                                }
                            }
                        },
                        onDragCancel = {
                            coroutineScope.launch {
                                progressAnim.animateTo(0f, spring(stiffness = Spring.StiffnessMedium))
                            }
                        }
                    ) { change, drag ->
                        change.consume()
                        coroutineScope.launch {
                            val newProgress = (progressAnim.value - drag.x / size.width).coerceIn(0f, 1f)
                            progressAnim.snapTo(newProgress)
                        }
                    }
                }
        ) {
            PagePanel(PAGES[page], Modifier.fillMaxSize())
            if (progressAnim.value > 0f) {
                PagePanel(PAGES[next], Modifier.fillMaxSize().clip(LiquidEdge(progressAnim.value, cy)))
            }
        }
    }
}

@Composable
private fun PagePanel(data: Triple<String, Color, String>, modifier: Modifier) {
    Column(modifier.background(data.second).padding(32.dp), verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center) {
        Text(data.first, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 44.sp)
        Text(data.third, color = Color(0xCCFFFFFF), fontSize = 18.sp)
    }
}
