package pro.jayeshseth.devcontrols.ui.visualfx

import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.os.Build
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlin.math.exp
import kotlin.math.sin
import kotlin.math.sqrt

private const val RIPPLE_AGSL = """
uniform shader content;
uniform float2 iResolution;
uniform float iTime;
uniform float rippleCount;
uniform float rippleX[8];
uniform float rippleY[8];
uniform float rippleTime[8];
half4 main(float2 fragCoord) {
    float2 off = float2(0.0);
    for (int i = 0; i < 8; i++) {
        if (float(i) >= rippleCount) break;
        float2 c = float2(rippleX[i], rippleY[i]);
        float age = iTime - rippleTime[i];
        if (age < 0.0) continue;
        float2 d = fragCoord - c;
        float dist = length(d) + 0.001;
        float w = sin(dist * 0.09 - age * 7.0) * exp(-age * 2.2) * exp(-dist * 0.004) * 22.0;
        off += (d / dist) * w;
    }
    return content.eval(fragCoord + off);
}
"""

/** Touch-ripple water — tap/drag to send ripples that distort the live UI (content shader). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TouchRippleWaterScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = { TopAppBar(title = { Text("Touch Ripple Water") }, navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }) },
        modifier = Modifier.fillMaxSize()
    ) { inner ->
        Column(Modifier.padding(inner).fillMaxSize()) {
            RippleContent()
        }
    }
}

@Composable
private fun RippleContent() {
    val isSupported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
    val shader = remember { if (isSupported) RuntimeShader(RIPPLE_AGSL) else null }
    val rippleX = remember { FloatArray(8) }
    val rippleY = remember { FloatArray(8) }
    val rippleTime = remember { FloatArray(8) }
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
        rippleX[head] = x
        rippleY[head] = y
        rippleTime[head] = time
        head = (head + 1) % 8
        if (count < 8) count++
    }

    if (isSupported && shader != null) {
        // ── AGSL path: RuntimeShader content distortion (Android 13+) ──
        Box(
            Modifier.fillMaxSize()
                .graphicsLayer {
                    tick // re-read each frame
                    shader.setFloatUniform("iResolution", size.width, size.height)
                    shader.setFloatUniform("iTime", time)
                    shader.setFloatUniform("rippleCount", count.toFloat())
                    shader.setFloatUniform("rippleX", rippleX)
                    shader.setFloatUniform("rippleY", rippleY)
                    shader.setFloatUniform("rippleTime", rippleTime)
                    renderEffect = RenderEffect.createRuntimeShaderEffect(shader, "content").asComposeRenderEffect()
                }
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent(PointerEventPass.Initial)
                            event.changes.forEach { change ->
                                val x = change.position.x
                                val y = change.position.y
                                val isDown = change.pressed && !change.previousPressed
                                val isMove = change.pressed && change.previousPressed

                                if (isDown) {
                                    addRipple(x, y)
                                } else if (isMove) {
                                    val lastIdx = (head - 1 + 8) % 8
                                    val lastX = rippleX[lastIdx]
                                    val lastY = rippleY[lastIdx]
                                    val lastT = rippleTime[lastIdx]
                                    val dx = x - lastX
                                    val dy = y - lastY
                                    val dist = sqrt(dx * dx + dy * dy)
                                    if (count == 0 || (dist > 40f && time - lastT > 0.08f)) {
                                        addRipple(x, y)
                                    }
                                }
                            }
                        }
                    }
                }
        ) {
            GridContent()
        }
    } else {
        // ── Fallback: drawBitmapMesh distortion (Android < 13) ──
        val view = LocalView.current
        var snapshot by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
        var contentBounds by remember { mutableStateOf(androidx.compose.ui.geometry.Rect.Zero) }

        // Mesh configuration — 40×40 gives smooth distortion with 1681 vertices
        val meshW = 40
        val meshH = 40
        val vertCount = (meshW + 1) * (meshH + 1)
        val verts = remember { FloatArray(vertCount * 2) }

        // Check if any ripple is currently animating
        val hasActiveRipples = run {
            var active = false
            for (i in 0 until count) {
                val age = time - rippleTime[i]
                if (age in 0f..3f) {
                    active = true
                    break
                }
            }
            active
        }

        // Recycle snapshot when all ripples expire
        LaunchedEffect(hasActiveRipples) {
            if (!hasActiveRipples && snapshot != null) {
                snapshot?.recycle()
                snapshot = null
            }
        }

        // Helper: capture the content region via View.draw() (synchronous, main thread)
        fun captureContent(): android.graphics.Bitmap? {
            val b = contentBounds
            val w = b.width.toInt()
            val h = b.height.toInt()
            if (w <= 0 || h <= 0) return null
            val bmp = android.graphics.Bitmap.createBitmap(w, h, android.graphics.Bitmap.Config.ARGB_8888)
            val canvas = android.graphics.Canvas(bmp)
            canvas.translate(-b.left, -b.top)
            view.draw(canvas)
            return bmp
        }

        Box(Modifier.fillMaxSize()) {
            // Content layer
            Box(
                Modifier.fillMaxSize()
                    .onGloballyPositioned { coords ->
                        contentBounds = coords.boundsInWindow()
                    }
                    .graphicsLayer {
                        // Hide original content while distortion overlay is active
                        alpha = if (hasActiveRipples && snapshot != null) 0f else 1f
                    }
                    .pointerInput(Unit) {
                        awaitPointerEventScope {
                            while (true) {
                                val event = awaitPointerEvent(PointerEventPass.Initial)
                                event.changes.forEach { change ->
                                    val x = change.position.x
                                    val y = change.position.y
                                    val isDown = change.pressed && !change.previousPressed
                                    val isMove = change.pressed && change.previousPressed

                                    if (isDown) {
                                        // Capture snapshot on first touch of a new ripple burst
                                        if (snapshot == null) {
                                            snapshot = captureContent()
                                        }
                                        addRipple(x, y)
                                    } else if (isMove) {
                                        val lastIdx = (head - 1 + 8) % 8
                                        val lastX = rippleX[lastIdx]
                                        val lastY = rippleY[lastIdx]
                                        val lastT = rippleTime[lastIdx]
                                        val dx = x - lastX
                                        val dy = y - lastY
                                        val dist = sqrt(dx * dx + dy * dy)
                                        if (count == 0 || (dist > 40f && time - lastT > 0.08f)) {
                                            addRipple(x, y)
                                        }
                                    }
                                }
                            }
                        }
                    }
            ) {
                GridContent()
            }

            // Distortion overlay — draws the warped snapshot via drawBitmapMesh
            if (hasActiveRipples && snapshot != null) {
                @Suppress("UNUSED_VARIABLE")
                val currentTick = tick // read to ensure recomposition each frame
                androidx.compose.foundation.Canvas(Modifier.fillMaxSize()) {
                    val bmp = snapshot ?: return@Canvas
                    val w = bmp.width.toFloat()
                    val h = bmp.height.toFloat()

                    // Compute displaced mesh vertices — same formula as AGSL shader
                    for (j in 0..meshH) {
                        for (i in 0..meshW) {
                            val idx = (j * (meshW + 1) + i) * 2
                            val px = w * i / meshW
                            val py = h * j / meshH
                            var offX = 0f
                            var offY = 0f
                            for (r in 0 until count) {
                                val age = time - rippleTime[r]
                                if (age < 0f || age > 3f) continue
                                val dx = px - rippleX[r]
                                val dy = py - rippleY[r]
                                val d = sqrt(dx * dx + dy * dy) + 0.001f
                                val wt = (sin((d * 0.09f - age * 7f).toDouble()) *
                                        exp((-age * 2.2f).toDouble()) *
                                        exp((-d * 0.004f).toDouble()) * 22.0).toFloat()
                                offX += (dx / d) * wt
                                offY += (dy / d) * wt
                            }
                            verts[idx] = px + offX
                            verts[idx + 1] = py + offY
                        }
                    }

                    drawIntoCanvas { canvas ->
                        canvas.nativeCanvas.drawBitmapMesh(
                            bmp, meshW, meshH, verts, 0, null, 0, null
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GridContent() {
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = Modifier.fillMaxSize().background(Color(0xFF0B0B16)).padding(8.dp)
    ) {
        itemsIndexed((1..80).toList()) { index, item ->
            val anim = remember { Animatable(0f) }
            LaunchedEffect(Unit) {
                anim.snapTo(0f)
                kotlinx.coroutines.delay(index * 25L)
                anim.animateTo(1f, tween(450))
            }
            Box(
                Modifier.padding(6.dp).aspectRatio(1f)
                    .graphicsLayer {
                        alpha = anim.value
                        translationY = (1f - anim.value) * 50f
                        val s = 0.85f + anim.value * 0.15f
                        scaleX = s; scaleY = s
                    }
                    .background(Color.hsv((item * 25f) % 360f, 0.6f, 0.95f), RoundedCornerShape(14.dp)),
                Alignment.Center
            ) { Text("$item", color = Color.White, fontWeight = FontWeight.Bold) }
        }
    }
}
