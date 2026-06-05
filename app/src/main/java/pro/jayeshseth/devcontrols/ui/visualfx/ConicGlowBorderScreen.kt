package pro.jayeshseth.devcontrols.ui.visualfx

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.dp

/** Conic glow border — a rotating rainbow sweep-gradient outline around a card. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConicGlowBorderScreen(onBack: () -> Unit) {
    var angle by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(Unit) { while (true) { withFrameNanos { angle = (angle + 1.5f) % 360f } } }
    val colors = listOf(Color(0xFFFF2D95), Color(0xFFFFC371), Color(0xFF18FFFF), Color(0xFF69F0AE), Color(0xFFE040FB), Color(0xFFFF2D95))

    Scaffold(
        topBar = { TopAppBar(title = { Text("Conic Glow Border") }, navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }) },
        modifier = Modifier.fillMaxSize()
    ) { inner ->
        Column(Modifier.padding(inner).fillMaxSize().padding(32.dp), verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                Modifier.fillMaxWidth().height(220.dp)
                    .drawBehind {
                        val sweep = Brush.sweepGradient(colors, center = Offset(size.width / 2, size.height / 2))
                        // glow: thick blurred-ish via several rotating strokes
                        rotate(angle) {
                            drawRoundRect(sweep, topLeft = Offset(0f, 0f), size = size,
                                cornerRadius = androidx.compose.ui.geometry.CornerRadius(28f), style = Stroke(width = 10f))
                        }
                    }
                    .padding(10.dp)
                    .background(MaterialTheme.colorScheme.surface, androidx.compose.foundation.shape.RoundedCornerShape(22.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text("Glowing", style = MaterialTheme.typography.headlineMedium)
            }
            Text("The conic gradient outline rotates continuously.", Modifier.padding(top = 20.dp), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
