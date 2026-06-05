package pro.jayeshseth.devcontrols.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Nested-scroll lab. A `NestedScrollConnection` intercepts the LazyColumn's scroll in
 * `onPreScroll` to collapse a header between [HEADER_MAX] and [HEADER_MIN] before the
 * list moves. The header title scales and the background parallaxes against the
 * collapse fraction — the from-scratch version of a CollapsingToolbar.
 */
private val HEADER_MAX = 240.dp
private val HEADER_MIN = 72.dp

@Composable
fun NestedScrollLabScreen(onBack: () -> Unit) {
    val density = LocalDensity.current
    val maxPx = with(density) { HEADER_MAX.toPx() }
    val minPx = with(density) { HEADER_MIN.toPx() }
    var headerPx by remember { mutableFloatStateOf(maxPx) }

    val connection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: androidx.compose.ui.input.nestedscroll.NestedScrollSource): Offset {
                val delta = available.y
                val newHeight = (headerPx + delta).coerceIn(minPx, maxPx)
                val consumed = newHeight - headerPx
                headerPx = newHeight
                // consume only what the header absorbed; rest goes to the list
                return if (consumed != 0f) Offset(0f, consumed) else Offset.Zero
            }
        }
    }

    val fraction = ((headerPx - minPx) / (maxPx - minPx)).coerceIn(0f, 1f)

    Column(Modifier.fillMaxSize()) {
        // Collapsing header
        Box(
            Modifier
                .fillMaxWidth()
                .height(with(density) { headerPx.toDp() })
                .background(Brush.linearGradient(listOf(Color(0xFF1A2980), Color(0xFFE100FF)))),
            contentAlignment = Alignment.BottomStart
        ) {
            // parallax: drifts up as it collapses
            Box(
                Modifier
                    .fillMaxSize()
                    .graphicsLayer { translationY = (1f - fraction) * -40f },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "Nested Scroll",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = (18 + 18 * fraction).sp
                )
            }
            Text(
                "Back",
                color = Color.White,
                modifier = Modifier.padding(12.dp).clickable { onBack() }
            )
        }

        LazyColumn(
            Modifier
                .fillMaxSize()
                .nestedScroll(connection)
        ) {
            items((1..60).toList()) { i ->
                Box(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                        .height(64.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Text("  Row $i", style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
    }
}
