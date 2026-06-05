package pro.jayeshseth.devcontrols.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Collapsing parallax header — the hero image scrolls at half speed and fades as you scroll. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParallaxHeaderLabScreen(onBack: () -> Unit) {
    val state = rememberLazyListState()
    val headerH = 240.dp

    Scaffold(
        topBar = { TopAppBar(title = { Text("Parallax Header") }, navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }) },
        modifier = Modifier.fillMaxSize()
    ) { inner ->
        Box(Modifier.padding(inner).fillMaxSize()) {
            // header tracks the first item's scroll offset (only valid while item 0 visible)
            val offsetPx = if (state.firstVisibleItemIndex == 0) state.firstVisibleItemScrollOffset.toFloat() else 1e6f
            Box(
                Modifier.fillMaxWidth().height(headerH)
                    .graphicsLayer {
                        translationY = -offsetPx * 0.5f // parallax: half speed
                        alpha = (1f - (offsetPx / 600f)).coerceIn(0f, 1f)
                    }
                    .background(Brush.linearGradient(listOf(Color(0xFFFF6CAB), Color(0xFF7366FF)))),
                contentAlignment = Alignment.Center
            ) {
                Text("Parallax", color = Color.White, fontSize = 40.sp, fontWeight = FontWeight.Bold)
            }
            LazyColumn(state = state, modifier = Modifier.fillMaxSize()) {
                item { androidx.compose.foundation.layout.Spacer(Modifier.height(headerH)) }
                items((1..40).toList()) { i ->
                    Text("List row $i — scroll up and watch the header glide and fade.",
                        Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface).padding(16.dp),
                        style = MaterialTheme.typography.bodyLarge)
                    HorizontalDivider()
                }
            }
        }
    }
}
