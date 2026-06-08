package pro.jayeshseth.devcontrols.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** AnimatedContent gallery — switch a number using each built-in transition. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AnimatedContentGalleryScreen(onBack: () -> Unit) {
    var count by remember { mutableIntStateOf(0) }
    var mode by remember { mutableIntStateOf(0) }
    val modes = listOf("Fade", "Slide H", "Slide V", "Scale", "Fade+Scale")

    Scaffold(
        topBar = { TopAppBar(title = { Text("AnimatedContent") }, navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }) },
        modifier = Modifier.fillMaxSize()
    ) { inner ->
        Column(Modifier.padding(inner).fillMaxSize().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                modes.forEachIndexed { i, m -> FilterChip(mode == i, { mode = i }, { Text(m) }) }
            }
            Spacer(Modifier.height(40.dp))
            AnimatedContent(
                targetState = count,
                transitionSpec = {
                    when (mode) {
                        1 -> (slideInHorizontally { it } togetherWith slideOutHorizontally { -it })
                        2 -> (slideInVertically { it } togetherWith slideOutVertically { -it })
                        3 -> (scaleIn() togetherWith scaleOut())
                        4 -> (fadeIn() + scaleIn(initialScale = 0.6f) togetherWith fadeOut() + scaleOut(targetScale = 0.6f))
                        else -> (fadeIn(tween(400)) togetherWith fadeOut(tween(400)))
                    }.using(SizeTransform(clip = false))
                },
                label = "ac"
            ) { value ->
                Box(Modifier.size(160.dp).background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(24.dp)), contentAlignment = Alignment.Center) {
                    Text("$value", fontSize = 72.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                }
            }
            Spacer(Modifier.height(40.dp))
            androidx.compose.material3.Button(onClick = { count++ }) { Text("Next") }
        }
    }
}
