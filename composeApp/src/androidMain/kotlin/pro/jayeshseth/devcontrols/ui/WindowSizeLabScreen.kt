package pro.jayeshseth.devcontrols.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Window-size / adaptive-layout lab. Computes width & height **size classes** the same
 * way Material3's `WindowSizeClass` does (Compact <600dp, Medium <840dp, Expanded
 * otherwise) straight from [BoxWithConstraints] — no `androidx.window` dependency. The
 * content adapts: Expanded/Medium width shows a side-by-side list-detail; Compact
 * stacks. Rotate or enter split-screen to watch the class flip live.
 */
private enum class SizeClass { Compact, Medium, Expanded }

private fun widthClass(w: Dp) = when {
    w < 600.dp -> SizeClass.Compact
    w < 840.dp -> SizeClass.Medium
    else -> SizeClass.Expanded
}
private fun heightClass(h: Dp) = when {
    h < 480.dp -> SizeClass.Compact
    h < 900.dp -> SizeClass.Medium
    else -> SizeClass.Expanded
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WindowSizeLabScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Window Size") },
                navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }
            )
        },
        modifier = Modifier.fillMaxSize()
    ) { inner ->
        BoxWithConstraints(Modifier.padding(inner).fillMaxSize().padding(12.dp)) {
            val w = maxWidth
            val h = maxHeight
            val wc = widthClass(w)
            val hc = heightClass(h)
            val wide = wc != SizeClass.Compact

            Column(Modifier.fillMaxSize()) {
                Card(
                    Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(Modifier.padding(14.dp)) {
                        Text("Window size classes", fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(4.dp))
                        Text("available = ${w.value.toInt()} × ${h.value.toInt()} dp", fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                        Text("width  class = $wc", fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                        Text("height class = $hc", fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                        Text("layout = ${if (wide) "list-detail (side by side)" else "stacked"}", fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                    }
                }

                Spacer(Modifier.height(12.dp))
                if (wide) {
                    Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Pane("List", MaterialTheme.colorScheme.primaryContainer, Modifier.weight(1f).fillMaxHeight())
                        Pane("Detail", MaterialTheme.colorScheme.secondaryContainer, Modifier.weight(2f).fillMaxHeight())
                    }
                } else {
                    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Pane("List", MaterialTheme.colorScheme.primaryContainer, Modifier.fillMaxWidth().weight(1f))
                        Pane("Detail", MaterialTheme.colorScheme.secondaryContainer, Modifier.fillMaxWidth().weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun Pane(label: String, color: androidx.compose.ui.graphics.Color, modifier: Modifier) {
    Box(modifier.background(color, RoundedCornerShape(16.dp))) {
        Text(label, Modifier.padding(16.dp), fontWeight = FontWeight.Bold)
    }
}
