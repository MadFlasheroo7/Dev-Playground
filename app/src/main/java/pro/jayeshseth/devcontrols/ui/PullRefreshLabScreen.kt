package pro.jayeshseth.devcontrols.ui

import androidx.compose.animation.core.animate
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/** Custom elastic pull-to-refresh built on a NestedScrollConnection (no Material helper). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PullRefreshLabScreen(onBack: () -> Unit) {
    val density = LocalDensity.current
    val threshold = with(density) { 80.dp.toPx() }
    val maxPull = threshold * 1.7f
    var pull by remember { mutableFloatStateOfSafe() }
    var refreshing by remember { mutableStateOf(false) }
    var seed by remember { mutableIntStateOf(0) }
    val items = remember(seed) { (1..20).map { "Item ${seed * 20 + it}" } }

    val conn = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (available.y < 0 && pull > 0f) { val d = available.y; pull = (pull + d).coerceAtLeast(0f); return Offset(0f, d) }
                return Offset.Zero
            }
            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                if (available.y > 0f && !refreshing) { pull = (pull + available.y * 0.5f).coerceAtMost(maxPull); return Offset(0f, available.y) }
                return Offset.Zero
            }
            override suspend fun onPreFling(available: Velocity): Velocity {
                if (!refreshing && pull > threshold) {
                    refreshing = true
                    animate(pull, threshold) { v, _ -> pull = v }
                    delay(1300); seed++; refreshing = false
                }
                animate(pull, 0f) { v, _ -> pull = v }
                return Velocity.Zero
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Pull to Refresh") },
                navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }
            )
        },
        modifier = Modifier.fillMaxSize()
    ) { inner ->
        Box(Modifier.padding(inner).fillMaxSize().nestedScroll(conn)) {
            // indicator
            Box(Modifier.align(Alignment.TopCenter).graphicsLayer { translationY = pull - with(density) { 56.dp.toPx() } }.size(48.dp),
                contentAlignment = Alignment.Center) {
                if (refreshing) CircularProgressIndicator(Modifier.size(34.dp), strokeWidth = 3.dp)
                else Text("↻", modifier = Modifier.rotate(pull / threshold * 270f),
                    style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
            }
            LazyColumn(Modifier.fillMaxSize().graphicsLayer { translationY = pull }) {
                items(items) { s ->
                    Box(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp).height(64.dp)
                        .clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.CenterStart) { Text("  $s") }
                }
            }
        }
    }
}

private fun mutableFloatStateOfSafe() = androidx.compose.runtime.mutableFloatStateOf(0f)
