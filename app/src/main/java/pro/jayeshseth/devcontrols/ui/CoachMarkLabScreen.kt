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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.unit.dp

/** Coach-mark — a scrim that cuts a spotlight hole over a target and shows a callout. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CoachMarkLabScreen(onBack: () -> Unit) {
    var step by remember { mutableIntStateOf(-1) } // -1 off, 0..n targets
    val targets = remember { mutableMapOf<Int, Rect>() }
    val labels = listOf("This adds an item.", "Your saved count lives here.", "Open settings from this button.")

    Scaffold(
        topBar = { TopAppBar(title = { Text("Coach-mark") }, navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }) },
        modifier = Modifier.fillMaxSize()
    ) { inner ->
        Box(Modifier.padding(inner).fillMaxSize()) {
            Column(Modifier.fillMaxSize().padding(16.dp)) {
                Button(onClick = { step = 0 }) { Text("Start tour") }
                Spacer(Modifier.height(40.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.onGloballyPositioned { targets[1] = it.boundsInRootRect() }
                        .background(MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(8.dp)).padding(16.dp)) {
                        Text("Saved: 12")
                    }
                    FloatingActionButton(onClick = {}, modifier = Modifier.onGloballyPositioned { targets[0] = it.boundsInRootRect() }) {
                        Text("+", style = MaterialTheme.typography.headlineSmall)
                    }
                }
                Spacer(Modifier.weight(1f))
                Button(onClick = {}, modifier = Modifier.onGloballyPositioned { targets[2] = it.boundsInRootRect() }) { Text("Settings") }
            }

            if (step in labels.indices) {
                val target = targets[step]
                Box(Modifier.fillMaxSize().graphicsLayer { alpha = 0.99f }.drawBehind {
                    drawRect(Color(0xCC000000))
                    target?.let {
                        drawRoundRect(Color.Transparent, topLeft = Offset(it.left - 8, it.top - 8),
                            size = androidx.compose.ui.geometry.Size(it.width + 16, it.height + 16),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(16f), blendMode = BlendMode.Clear)
                        drawRoundRect(Color.White, topLeft = Offset(it.left - 8, it.top - 8),
                            size = androidx.compose.ui.geometry.Size(it.width + 16, it.height + 16),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(16f), style = Stroke(3f))
                    }
                })
                Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                    Card {
                        Column(Modifier.padding(16.dp)) {
                            Text(labels[step], style = MaterialTheme.typography.bodyLarge)
                            Spacer(Modifier.height(12.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                TextButton(onClick = { step = -1 }) { Text("Skip") }
                                Button(onClick = { step = if (step + 1 in labels.indices) step + 1 else -1 }) {
                                    Text(if (step == labels.lastIndex) "Done" else "Next")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun androidx.compose.ui.layout.LayoutCoordinates.boundsInRootRect(): Rect {
    val pos = positionInRoot()
    return Rect(pos.x, pos.y, pos.x + size.width, pos.y + size.height)
}
