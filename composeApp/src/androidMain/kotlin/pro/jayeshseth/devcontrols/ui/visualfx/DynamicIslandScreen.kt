package pro.jayeshseth.devcontrols.ui.visualfx

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

private enum class Island { COMPACT, EXPANDED, ACTIVITY }

/** Dynamic Island — a morphing pill that springs between compact / expanded / activity states. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DynamicIslandScreen(onBack: () -> Unit) {
    var state by remember { mutableIntStateOf(0) }
    val islands = Island.entries
    val cur = islands[state]
    val w by animateDpAsState(when (cur) { Island.COMPACT -> 130.dp; Island.EXPANDED -> 340.dp; Island.ACTIVITY -> 250.dp }, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessLow), label = "w")
    val h by animateDpAsState(when (cur) { Island.COMPACT -> 38.dp; Island.EXPANDED -> 160.dp; Island.ACTIVITY -> 60.dp }, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessLow), label = "h")

    Scaffold(
        topBar = { TopAppBar(title = { Text("Dynamic Island") }, navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }) },
        modifier = Modifier.fillMaxSize()
    ) { inner ->
        Column(Modifier.padding(inner).fillMaxSize().padding(top = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                Modifier.width(w).height(h).clip(RoundedCornerShape(32.dp)).background(Color.Black)
                    .clickable { state = (state + 1) % islands.size }.padding(12.dp),
                contentAlignment = Alignment.Center
            ) {
                AnimatedContent(targetState = cur, transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "island") { s ->
                    when (s) {
                        Island.COMPACT -> Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Box(Modifier.size(14.dp).clip(RoundedCornerShape(50)).background(Color(0xFF4CAF50)))
                            Box(Modifier.size(14.dp).clip(RoundedCornerShape(50)).background(Color(0xFF222222)))
                        }
                        Island.EXPANDED -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Now Playing", color = Color(0xFFBBBBBB), style = MaterialTheme.typography.labelSmall)
                            Text("Midnight City", color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                                Text("⏮", color = Color.White); Text("⏸", color = Color.White); Text("⏭", color = Color.White)
                            }
                        }
                        Island.ACTIVITY -> Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Text("🚕", style = MaterialTheme.typography.titleLarge)
                            Text("Arriving · 3 min", color = Color.White, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
            Spacer(Modifier.height(40.dp))
            Text("Tap the island to cycle states.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
