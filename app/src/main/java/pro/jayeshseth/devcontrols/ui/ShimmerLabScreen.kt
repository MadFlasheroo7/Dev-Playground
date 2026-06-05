package pro.jayeshseth.devcontrols.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.size
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Shimmer skeletons. A diagonal sheen [Brush] sweeps across grey placeholder shapes
 * (the standard "content is loading" effect). Toggle **Loaded** to swap the skeletons
 * for real content.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShimmerLabScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Shimmer Skeletons") },
                navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }
            )
        },
        modifier = Modifier.fillMaxSize()
    ) { inner ->
        var loaded by remember { mutableStateOf(false) }
        val progress by rememberInfiniteTransition(label = "s").animateFloat(
            0f, 1f, infiniteRepeatable(tween(1200, easing = LinearEasing), RepeatMode.Restart), label = "p"
        )

        Column(Modifier.padding(inner).fillMaxSize().padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Loaded", Modifier.weight(1f))
                Switch(loaded, { loaded = it })
            }
            Spacer(Modifier.height(12.dp))
            repeat(5) {
                Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (loaded) {
                        Box(Modifier.size(56.dp).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.primary))
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Loaded item ${it + 1}", style = MaterialTheme.typography.titleMedium)
                            Text("Real content has arrived.", style = MaterialTheme.typography.bodySmall)
                        }
                    } else {
                        Box(Modifier.size(56.dp).clip(RoundedCornerShape(12.dp)).shimmer(progress))
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(Modifier.fillMaxWidth(0.7f).height(16.dp).clip(RoundedCornerShape(4.dp)).shimmer(progress))
                            Box(Modifier.fillMaxWidth().height(12.dp).clip(RoundedCornerShape(4.dp)).shimmer(progress))
                        }
                    }
                }
            }
        }
    }
}

private fun Modifier.shimmer(progress: Float): Modifier = this.drawWithCache {
    val w = size.width
    val sheen = w * 0.6f
    val x = -sheen + progress * (w + 2 * sheen)
    val brush = Brush.linearGradient(
        colors = listOf(Color(0xFF2A2A2A), Color(0xFF4A4A4A), Color(0xFF2A2A2A)),
        start = Offset(x, 0f),
        end = Offset(x + sheen, size.height)
    )
    onDrawBehind { drawRect(brush) }
}
