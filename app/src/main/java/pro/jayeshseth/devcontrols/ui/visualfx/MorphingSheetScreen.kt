package pro.jayeshseth.devcontrols.ui.visualfx

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/** Morphing sheet — a card that container-transforms into a full sheet and back. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MorphingSheetScreen(onBack: () -> Unit) {
    var open by remember { mutableStateOf(false) }
    val margin by animateDpAsState(if (open) 0.dp else 28.dp, tween(420), label = "m")
    val corner by animateDpAsState(if (open) 0.dp else 24.dp, tween(420), label = "c")
    val color by animateColorAsState(if (open) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.primaryContainer, tween(420), label = "col")

    Scaffold(
        topBar = { TopAppBar(title = { Text("Morphing Sheet") }, navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }) },
        modifier = Modifier.fillMaxSize()
    ) { inner ->
        Box(Modifier.padding(inner).fillMaxSize()) {
            Box(
                Modifier.fillMaxSize().padding(margin).clip(RoundedCornerShape(corner)).background(color)
                    .clickable { open = !open }.padding(20.dp)
            ) {
                if (!open) {
                    Column {
                        Text("Tap to expand", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        Text("Container transform", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                } else {
                    Column(Modifier.fillMaxSize()) {
                        Text("Expanded sheet", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.headlineSmall)
                        Spacer(Modifier.height(12.dp))
                        repeat(6) { i -> Text("Detail row ${i + 1} — the card grew its bounds + corner into this sheet.", Modifier.padding(vertical = 6.dp)) }
                        Spacer(Modifier.height(12.dp))
                        Text("Tap anywhere to collapse.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}
