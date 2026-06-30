package pro.jayeshseth.devcontrols.ui.shaders

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import pro.jayeshseth.devcontrols.platform.createRuntimeShaderEffect

private const val RIPPLE_AGSL_MULTIPLATFORM = """
uniform shader content;
uniform float2 iResolution;
uniform float iTime;
uniform float rippleCount;
uniform float ripples[24]; // 8 × (x, y, startTime)
half4 main(float2 fragCoord) {
    float2 off = float2(0.0);
    for (int i = 0; i < 8; i++) {
        if (float(i) >= rippleCount) break;
        float2 c = float2(ripples[i * 3], ripples[i * 3 + 1]);
        float age = iTime - ripples[i * 3 + 2];
        if (age < 0.0) continue;
        float2 d = fragCoord - c;
        float dist = length(d) + 0.001;
        float w = sin(dist * 0.09 - age * 7.0) * exp(-age * 2.2) * exp(-dist * 0.004) * 22.0;
        off += (d / dist) * w;
    }
    return content.eval(fragCoord + off);
}
"""

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TouchRippleWaterScreenMultiplatform(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Touch Ripple Water") },
                navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }
            )
        },
        modifier = Modifier.fillMaxSize()
    ) { inner ->
        Column(Modifier.padding(inner).fillMaxSize()) {
            RippleContentMultiplatform()
        }
    }
}

@Composable
private fun RippleContentMultiplatform() {
    val ripples = remember { FloatArray(24) }
    var head by remember { mutableIntStateOf(0) }
    var count by remember { mutableIntStateOf(0) }
    var time by remember { mutableFloatStateOf(0f) }
    var tick by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        var start = 0L
        while (true) {
            withFrameNanos {
                if (start == 0L) start = it
                time = (it - start) / 1e9f
                tick++
            }
        }
    }

    fun addRipple(x: Float, y: Float) {
        ripples[head * 3] = x
        ripples[head * 3 + 1] = y
        ripples[head * 3 + 2] = time
        head = (head + 1) % 8
        if (count < 8) count++
    }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val w = maxWidth.value
        val h = maxHeight.value
        val density = androidx.compose.ui.platform.LocalDensity.current
        val wPx = with(density) { maxWidth.toPx() }
        val hPx = with(density) { maxHeight.toPx() }
        
        // Re-read tick state to trigger recomposition / graphicsLayer invalidation every frame
        val currentTick = tick
        
        val effect = remember(time, count, wPx, hPx) {
            createRuntimeShaderEffect(
                shaderSource = RIPPLE_AGSL_MULTIPLATFORM,
                uniformNames = listOf("iResolution", "iTime", "rippleCount", "ripples"),
                uniforms = mapOf(
                    "iResolution" to Offset(wPx, hPx),
                    "iTime" to time,
                    "rippleCount" to count.toFloat(),
                    "ripples" to ripples
                ),
                inputShaderName = "content"
            )
        }

        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer {
                    if (effect != null) {
                        renderEffect = effect
                    }
                }
                .pointerInput(Unit) { detectTapGestures { addRipple(it.x, it.y) } }
                .pointerInput(Unit) { detectDragGestures { c, _ -> addRipple(c.position.x, c.position.y) } }
        ) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF0B0B16))
                    .padding(8.dp)
            ) {
                itemsIndexed((1..80).toList()) { index, item ->
                    val anim = remember { Animatable(0f) }
                    LaunchedEffect(Unit) {
                        anim.snapTo(0f)
                        kotlinx.coroutines.delay(index * 25L)
                        anim.animateTo(1f, tween(450))
                    }
                    
                    Box(
                        Modifier
                            .padding(6.dp)
                            .aspectRatio(1f)
                            .graphicsLayer {
                                alpha = anim.value
                                translationY = (1f - anim.value) * 50f
                                val s = 0.85f + anim.value * 0.15f
                                scaleX = s
                                scaleY = s
                            }
                            .background(
                                color = Color.hsv((item * 22f) % 360f, 0.65f, 0.9f),
                                shape = RoundedCornerShape(14.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "$item",
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
