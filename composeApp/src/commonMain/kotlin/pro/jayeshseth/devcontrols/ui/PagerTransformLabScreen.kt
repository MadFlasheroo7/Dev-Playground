package pro.jayeshseth.devcontrols.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.absoluteValue

/**
 * Pager transform gallery. A [HorizontalPager] where each page applies a
 * `graphicsLayer` effect keyed off its distance-from-settled offset — Cube, Depth,
 * Flip, or Coverflow. Swipe and switch effects to feel each one.
 */
private enum class PagerFx { Cube, Depth, Flip, Coverflow }

private val pageColors = listOf(
    Color(0xFFFF5F6D), Color(0xFF2193B0), Color(0xFF92FE9D),
    Color(0xFFFFC371), Color(0xFF7F00FF), Color(0xFF26D0CE)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PagerTransformLabScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Pager Transforms") },
                navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }
            )
        },
        modifier = Modifier.fillMaxSize()
    ) { inner ->
        var fx by remember { mutableStateOf(PagerFx.Cube) }
        val state = rememberPagerState(pageCount = { pageColors.size })

        Column(Modifier.padding(inner).fillMaxSize().padding(16.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PagerFx.entries.forEach { e -> FilterChip(fx == e, { fx = e }, { Text(e.name) }) }
            }
            Spacer(Modifier.height(16.dp))
            HorizontalPager(
                state = state,
                modifier = Modifier.fillMaxWidth().height(420.dp)
            ) { page ->
                val offset = ((state.currentPage - page) + state.currentPageOffsetFraction)
                Box(
                    Modifier
                        .fillMaxSize()
                        .padding(8.dp)
                        .graphicsLayer { applyFx(fx, offset) }
                        .clip(RoundedCornerShape(24.dp))
                        .background(Brush.verticalGradient(listOf(pageColors[page], Color.Black))),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Page ${page + 1}", color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.height(12.dp))
            Text(
                "offset = (currentPage − page) + currentPageOffsetFraction drives each page's transform.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun androidx.compose.ui.graphics.GraphicsLayerScope.applyFx(fx: PagerFx, offset: Float) {
    val abs = offset.absoluteValue
    when (fx) {
        PagerFx.Cube -> {
            cameraDistance = 16f * density
            transformOrigin = TransformOrigin(if (offset > 0f) 0f else 1f, 0.5f)
            rotationY = offset * -75f
        }
        PagerFx.Depth -> {
            if (offset > 0f) {
                // outgoing page: fade + shrink, stay in place
                alpha = 1f - abs
                val s = 0.8f + 0.2f * (1f - abs)
                scaleX = s; scaleY = s
                translationX = size.width * offset
            } else {
                translationX = 0f
            }
        }
        PagerFx.Flip -> {
            cameraDistance = 20f * density
            rotationY = offset * 180f
            alpha = if (abs > 0.5f) 0f else 1f
        }
        PagerFx.Coverflow -> {
            cameraDistance = 14f * density
            rotationY = offset * 45f
            val s = 0.75f + 0.25f * (1f - abs.coerceIn(0f, 1f))
            scaleX = s; scaleY = s
        }
    }
}
