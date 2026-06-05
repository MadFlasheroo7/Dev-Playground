package pro.jayeshseth.devcontrols.ui

import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * RenderEffect chain lab (API 31+). Build a chain of platform [RenderEffect]s —
 * gaussian blur, a saturation color-matrix, and a translate offset — and apply it to
 * live content through `Modifier.graphicsLayer { renderEffect = ... }`. Toggle each
 * stage and scrub its parameter to see how `createChainEffect` composes them.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RenderEffectLabScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("RenderEffect Chain") },
                navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }
            )
        },
        modifier = Modifier.fillMaxSize()
    ) { inner ->
        Column(Modifier.padding(inner).fillMaxSize().padding(16.dp)) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
                Text("RenderEffect needs Android 12 (API 31)+.")
                return@Column
            }

            var blurOn by remember { mutableStateOf(true) }
            var blur by remember { mutableFloatStateOf(12f) }
            var satOn by remember { mutableStateOf(false) }
            var sat by remember { mutableFloatStateOf(0f) }
            var offsetOn by remember { mutableStateOf(false) }
            var off by remember { mutableFloatStateOf(20f) }

            val effect = remember(blurOn, blur, satOn, sat, offsetOn, off) {
                buildChain(blurOn, blur, satOn, sat, offsetOn, off)?.asComposeRenderEffect()
            }

            Box(
                Modifier
                    .fillMaxWidth()
                    .height(240.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .graphicsLayer { renderEffect = effect }
                    .background(
                        Brush.linearGradient(listOf(Color(0xFF1A2980), Color(0xFFE100FF), Color(0xFFFFC371)))
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text("RenderEffect", color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(Modifier.height(16.dp))
            EffectRow("Gaussian blur", blurOn, { blurOn = it }, blur, 0f..40f) { blur = it }
            EffectRow("Saturation (0=gray, 2=vivid)", satOn, { satOn = it }, sat, 0f..2f) { sat = it }
            EffectRow("Offset (px)", offsetOn, { offsetOn = it }, off, 0f..120f) { off = it }

            Spacer(Modifier.height(12.dp))
            Text(
                "Stages compose via RenderEffect.createChainEffect(outer, inner) — each enabled " +
                    "stage wraps the previous. Order here: offset ← saturation ← blur ← content.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun EffectRow(
    label: String, on: Boolean, onToggle: (Boolean) -> Unit,
    value: Float, range: ClosedFloatingPointRange<Float>, onValue: (Float) -> Unit
) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Text(label, style = MaterialTheme.typography.bodyMedium)
            Switch(on, onToggle)
        }
        Slider(value = value, onValueChange = onValue, valueRange = range, enabled = on)
    }
}

private fun buildChain(
    blurOn: Boolean, blur: Float, satOn: Boolean, sat: Float, offsetOn: Boolean, off: Float
): RenderEffect? {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return null
    var current: RenderEffect? = null
    if (blurOn && blur > 0f) {
        current = RenderEffect.createBlurEffect(blur, blur, Shader.TileMode.CLAMP)
    }
    if (satOn) {
        val s = sat
        val inv = 1f - s
        // luminance-weighted saturation matrix
        val lr = 0.213f * inv; val lg = 0.715f * inv; val lb = 0.072f * inv
        val m = floatArrayOf(
            lr + s, lg, lb, 0f, 0f,
            lr, lg + s, lb, 0f, 0f,
            lr, lg, lb + s, 0f, 0f,
            0f, 0f, 0f, 1f, 0f
        )
        val cf = android.graphics.ColorMatrixColorFilter(m)
        current = if (current != null)
            RenderEffect.createColorFilterEffect(cf, current)
        else RenderEffect.createColorFilterEffect(cf)
    }
    if (offsetOn) {
        current = if (current != null)
            RenderEffect.createOffsetEffect(off, off, current)
        else RenderEffect.createOffsetEffect(off, off)
    }
    return current
}
