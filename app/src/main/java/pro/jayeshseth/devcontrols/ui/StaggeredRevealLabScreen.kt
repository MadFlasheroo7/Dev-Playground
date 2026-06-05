package pro.jayeshseth.devcontrols.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/** Staggered reveal — grid tiles fade + rise in sequence; replay to re-run. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StaggeredRevealLabScreen(onBack: () -> Unit) {
    var runKey by remember { mutableIntStateOf(0) }
    val items = remember { (1..24).toList() }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Staggered Reveal") }, navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }) },
        modifier = Modifier.fillMaxSize()
    ) { inner ->
        Column(Modifier.padding(inner).fillMaxSize().padding(12.dp)) {
            Button(onClick = { runKey++ }) { Text("Replay") }
            LazyVerticalGrid(columns = GridCells.Fixed(3), modifier = Modifier.fillMaxSize().padding(top = 12.dp)) {
                items(items) { i ->
                    val anim = remember(runKey, i) { Animatable(0f) }
                    LaunchedEffect(runKey, i) {
                        anim.snapTo(0f)
                        kotlinx.coroutines.delay(i * 45L)
                        anim.animateTo(1f, tween(420))
                    }
                    Box(Modifier.padding(6.dp).aspectRatio(1f)
                        .graphicsLayer { alpha = anim.value; translationY = (1f - anim.value) * 60f; val s = 0.8f + anim.value * 0.2f; scaleX = s; scaleY = s }
                        .background(Color.hsv((i * 15f) % 360f, 0.6f, 0.9f), RoundedCornerShape(14.dp)),
                        contentAlignment = Alignment.Center) {
                        Text("$i", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
