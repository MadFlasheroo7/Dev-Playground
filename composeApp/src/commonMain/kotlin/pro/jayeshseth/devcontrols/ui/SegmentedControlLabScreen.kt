package pro.jayeshseth.devcontrols.ui

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/** Segmented control with a sliding pill indicator that springs to the selected segment. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SegmentedControlLabScreen(onBack: () -> Unit) {
    val items = listOf("Day", "Week", "Month", "Year")
    var sel by remember { mutableIntStateOf(0) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Segmented Control") },
                navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }
            )
        },
        modifier = Modifier.fillMaxSize()
    ) { inner ->
        Column(Modifier.padding(inner).fillMaxSize().padding(24.dp)) {
            BoxWithConstraints(
                Modifier.fillMaxWidth().height(52.dp).clip(RoundedCornerShape(26.dp)).background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                val segW = maxWidth / items.size
                val pillX by animateDpAsState(segW * sel, spring(), label = "pill")
                Box(
                    Modifier.offset(x = pillX).width(segW).fillMaxSize().padding(4.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .background(Brush.horizontalGradient(listOf(Color(0xFF7F00FF), Color(0xFF00C9FF))))
                )
                Row(Modifier.fillMaxSize()) {
                    items.forEachIndexed { i, label ->
                        Box(Modifier.width(segW).fillMaxSize().clickable { sel = i }, contentAlignment = Alignment.Center) {
                            Text(label, color = if (i == sel) Color.White else MaterialTheme.colorScheme.onSurface,
                                fontWeight = if (i == sel) FontWeight.Bold else FontWeight.Normal)
                        }
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
            Text("Selected: ${items[sel]}", style = MaterialTheme.typography.titleMedium)
        }
    }
}
