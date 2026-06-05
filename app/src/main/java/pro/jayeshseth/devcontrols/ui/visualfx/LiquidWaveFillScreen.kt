package pro.jayeshseth.devcontrols.ui.visualfx

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.sin

/** Liquid wave fill — water sloshes to fill a circle to the chosen level (loader / battery). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiquidWaveFillScreen(onBack: () -> Unit) {
    var level by remember { mutableFloatStateOf(0.6f) }
    var phase by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(Unit) { while (true) { withFrameNanos { phase += 0.08f } } }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Liquid Wave Fill") }, navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }) },
        modifier = Modifier.fillMaxSize()
    ) { inner ->
        Column(Modifier.padding(inner).fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(Modifier.height(20.dp))
            Box(Modifier.size(260.dp).clip(CircleShape).background(Color(0xFF10131A)).drawBehind {
                val waterTop = size.height * (1f - level)
                val amp = 12f
                fun wave(off: Float, color: Color) {
                    val p = Path().apply {
                        moveTo(0f, size.height)
                        lineTo(0f, waterTop)
                        var x = 0f
                        while (x <= size.width) { lineTo(x, waterTop + sin(x * 0.025f + phase + off) * amp); x += 6f }
                        lineTo(size.width, size.height); close()
                    }
                    drawPath(p, color)
                }
                wave(1.6f, Color(0x664FC3F7))
                wave(0f, Color(0xCC29B6F6))
            }, contentAlignment = Alignment.Center) {
                Text("${(level * 100).toInt()}%", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 40.sp)
            }
            Spacer(Modifier.height(24.dp))
            Text("Level", style = MaterialTheme.typography.labelMedium)
            Slider(value = level, onValueChange = { level = it }, modifier = Modifier.fillMaxWidth())
        }
    }
}
