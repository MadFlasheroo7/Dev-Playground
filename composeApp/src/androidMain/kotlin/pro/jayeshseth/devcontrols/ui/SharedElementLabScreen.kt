package pro.jayeshseth.devcontrols.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private data class Item(val id: Int, val title: String, val color: Color)

/** Shared-element transition — list → detail with a hero card that morphs bounds & color. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
fun SharedElementLabScreen(onBack: () -> Unit) {
    val items = remember {
        listOf(
            Item(0, "Aurora", Color(0xFF26C6DA)), Item(1, "Ember", Color(0xFFEF5350)),
            Item(2, "Violet", Color(0xFFAB47BC)), Item(3, "Meadow", Color(0xFF66BB6A)),
            Item(4, "Amber", Color(0xFFFFCA28))
        )
    }
    var selected by remember { mutableStateOf<Item?>(null) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Shared Element") }, navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }) },
        modifier = Modifier.fillMaxSize()
    ) { inner ->
        Box(Modifier.padding(inner).fillMaxSize()) {
            SharedTransitionLayout {
                AnimatedContent(targetState = selected, label = "se",
                    transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(300)) }) { sel ->
                    if (sel == null) {
                        LazyColumn(Modifier.fillMaxSize().padding(12.dp)) {
                            items(items) { item ->
                                Row(
                                    Modifier.fillMaxWidth().padding(vertical = 8.dp).clickable { selected = item },
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(Modifier
                                        .sharedElement(rememberSharedContentState("box-${item.id}"), this@AnimatedContent)
                                        .size(56.dp).clip(RoundedCornerShape(14.dp)).background(item.color))
                                    Spacer(Modifier.size(16.dp))
                                    Text(item.title,
                                        Modifier.sharedElement(rememberSharedContentState("txt-${item.id}"), this@AnimatedContent),
                                        style = MaterialTheme.typography.titleMedium)
                                }
                            }
                        }
                    } else {
                        Column(Modifier.fillMaxSize().clickable { selected = null }.padding(20.dp)) {
                            Box(Modifier
                                .sharedElement(rememberSharedContentState("box-${sel.id}"), this@AnimatedContent)
                                .fillMaxWidth().height(220.dp).clip(RoundedCornerShape(24.dp)).background(sel.color))
                            Spacer(Modifier.height(20.dp))
                            Text(sel.title,
                                Modifier.sharedElement(rememberSharedContentState("txt-${sel.id}"), this@AnimatedContent),
                                fontSize = 40.sp, fontWeight = FontWeight.Bold)
                            Text("Tap anywhere to go back. The card and title animate their bounds between states.",
                                Modifier.padding(top = 12.dp), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}
