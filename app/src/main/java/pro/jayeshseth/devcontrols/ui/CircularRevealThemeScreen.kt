package pro.jayeshseth.devcontrols.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.foundation.clickable
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.layout.boundsInParent
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import kotlin.math.hypot

/**
 * Circular-reveal theme switcher. Tapping the toggle animates an expanding circle that
 * wipes the incoming theme over the current one, originating at the toggle's position —
 * the polished dark/light transition. The incoming layer is drawn on top and clipped to
 * a growing circle via `clipPath`; on completion the new theme is committed.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CircularRevealThemeScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Circular Reveal") },
                navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }
            )
        },
        modifier = Modifier.fillMaxSize()
    ) { inner ->
        val scope = rememberCoroutineScope()
        var dark by remember { mutableStateOf(false) }
        var incoming by remember { mutableStateOf(false) }   // theme being revealed
        var revealing by remember { mutableStateOf(false) }
        var origin by remember { mutableStateOf(Offset.Zero) }
        var togglePos by remember { mutableStateOf(Offset.Zero) }
        val radius = remember { Animatable(0f) }

        Box(Modifier.padding(inner).fillMaxSize()) {
            ThemedSurface(
                dark = dark,
                onToggle = {
                    if (revealing) return@ThemedSurface
                    origin = togglePos
                    incoming = !dark
                    revealing = true
                    scope.launch {
                        radius.snapTo(0f)
                        // expand to cover the farthest corner
                        val target = 2200f
                        radius.animateTo(target, tween(620))
                        dark = incoming
                        revealing = false
                    }
                },
                onTogglePositioned = { togglePos = it },
                modifier = Modifier.fillMaxSize()
            )

            if (revealing) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .drawWithContent {
                            val path = Path().apply { addOval(Rect(origin, radius.value)) }
                            clipPath(path) { this@drawWithContent.drawContent() }
                        }
                ) {
                    ThemedSurface(dark = incoming, onToggle = {}, onTogglePositioned = {}, modifier = Modifier.fillMaxSize())
                }
            }
        }
    }
}

@Composable
private fun ThemedSurface(
    dark: Boolean,
    onToggle: () -> Unit,
    onTogglePositioned: (Offset) -> Unit,
    modifier: Modifier = Modifier,
) {
    val bg = if (dark) Color(0xFF0D1117) else Color(0xFFF5F5F5)
    val fg = if (dark) Color(0xFFE6EDF3) else Color(0xFF101317)
    val accent = if (dark) Color(0xFF58A6FF) else Color(0xFF1565C0)

    Box(modifier.background(bg)) {
        Column(Modifier.fillMaxSize().padding(24.dp)) {
            Text(if (dark) "Dark theme" else "Light theme", color = fg, fontSize = 28.sp, fontWeight = FontWeight.Bold)
            Text("Tap the button to wipe to the other theme.", color = fg.copy(alpha = 0.7f))
            Column(Modifier.padding(top = 16.dp)) {
                repeat(3) {
                    Box(
                        Modifier
                            .padding(vertical = 6.dp)
                            .background(accent.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                            .padding(16.dp)
                    ) { Text("Card ${it + 1}", color = fg) }
                }
            }
        }
        // toggle button bottom-end
        Box(
            Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp)
                .onGloballyPositioned {
                    val b = it.boundsInParent()
                    onTogglePositioned(Offset(b.center.x, b.center.y))
                }
                .clip(RoundedCornerShape(28.dp))
                .background(accent)
                .clickable { onToggle() }
                .padding(horizontal = 22.dp, vertical = 16.dp)
        ) {
            Text(if (dark) "☀ Light" else "🌙 Dark", color = Color.White, fontWeight = FontWeight.Bold)
        }
    }
}
