package pro.jayeshseth.devcontrols.ui

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/** Accordion — single-open expandable sections with animated height + rotating chevron. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccordionLabScreen(onBack: () -> Unit) {
    val faq = listOf(
        "What is this?" to "A gallery of UI building blocks, each rendered live in Compose.",
        "Does it use any libraries?" to "Almost entirely platform + Compose; only graphics-shapes, CameraX and zxing where needed.",
        "How is state kept?" to "Scroll position and which categories are open survive navigating into a lab and back.",
        "Can I theme it?" to "The Mesh Components screen shows a full kit themed by a Compose mesh gradient."
    )
    var open by remember { mutableIntStateOf(-1) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Accordion") },
                navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }
            )
        },
        modifier = Modifier.fillMaxSize()
    ) { inner ->
        Column(Modifier.padding(inner).fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
            faq.forEachIndexed { i, (q, a) ->
                val isOpen = i == open
                val rot by animateFloatAsState(if (isOpen) 180f else 0f, label = "chev")
                Column(
                    Modifier.fillMaxWidth().padding(vertical = 6.dp)
                        .clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.surfaceVariant)
                        .animateContentSize()
                ) {
                    Row(
                        Modifier.fillMaxWidth().clickable { open = if (isOpen) -1 else i }.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(q, Modifier.weight(1f), fontWeight = FontWeight.Bold)
                        Text("⌄", Modifier.rotate(rot), style = MaterialTheme.typography.titleLarge)
                    }
                    if (isOpen) {
                        Text(a, Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}
