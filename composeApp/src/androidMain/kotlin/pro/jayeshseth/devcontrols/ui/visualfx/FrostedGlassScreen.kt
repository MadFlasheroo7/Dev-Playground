package pro.jayeshseth.devcontrols.ui.visualfx

import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

/** Frosted glass — drag the sheet up; the content behind blurs progressively (RenderEffect). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FrostedGlassScreen(onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    val reveal = remember { Animatable(0f) } // 0 = sheet hidden, 1 = full

    Scaffold(
        topBar = { TopAppBar(title = { Text("Frosted Glass") }, navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }) },
        modifier = Modifier.fillMaxSize()
    ) { inner ->
        Box(Modifier.padding(inner).fillMaxSize()) {
            val blurRadius = reveal.value * 28f
            // background content, blurred by reveal amount
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier.fillMaxSize().padding(12.dp).graphicsLayer {
                    if (Build.VERSION.SDK_INT >= 31 && blurRadius > 0.1f) {
                        renderEffect = RenderEffect.createBlurEffect(blurRadius, blurRadius, Shader.TileMode.CLAMP).asComposeRenderEffect()
                    }
                }
            ) {
                items((1..30).toList()) { i ->
                    Box(Modifier.padding(4.dp).aspectRatio(1f).clip(RoundedCornerShape(12.dp)).background(Color.hsv((i * 24f) % 360f, 0.6f, 0.9f)), Alignment.Center) {
                        Text("$i", color = Color.White)
                    }
                }
            }
            // dim overlay scales with reveal
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = reveal.value * 0.3f)))
            // draggable frosted sheet
            Box(
                Modifier.fillMaxWidth().fillMaxHeight(0.1f + reveal.value * 0.75f).align(Alignment.BottomCenter)
                    .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.7f))
                    .pointerInput(Unit) {
                        detectVerticalDragGestures(
                            onDragEnd = { scope.launch { reveal.animateTo(if (reveal.value > 0.5f) 1f else 0f) } }
                        ) { _, dy -> scope.launch { reveal.snapTo((reveal.value - dy / size.height).coerceIn(0f, 1f)) } }
                    }
            ) {
                Column(Modifier.fillMaxSize().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(Modifier.height(5.dp).fillMaxWidth(0.2f).clip(RoundedCornerShape(3.dp)).background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)))
                    Text("Frosted sheet", Modifier.padding(top = 16.dp), style = MaterialTheme.typography.titleLarge)
                    Text("Drag up/down — the grid behind blurs as the sheet rises.", Modifier.padding(top = 8.dp), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

