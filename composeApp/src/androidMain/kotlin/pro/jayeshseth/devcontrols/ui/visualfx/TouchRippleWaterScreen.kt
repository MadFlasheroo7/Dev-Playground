package pro.jayeshseth.devcontrols.ui.visualfx

import android.content.Context
import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.os.Build
import android.view.MotionEvent
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

private const val RIPPLE_AGSL = """
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

private const val VERTEX_SHADER_GLSL = """
attribute vec4 aPosition;
varying vec2 vPosition;
void main() {
    gl_Position = aPosition;
    vPosition = aPosition.xy;
}
"""

private const val FRAGMENT_SHADER_GLSL = """
precision mediump float;
varying vec2 vPosition;
uniform vec2 iResolution;
uniform float iTime;
uniform float rippleCount;
uniform float ripples[24];

float sdRoundedBox(vec2 p, vec2 b, vec4 r) {
    r.xy = (p.x > 0.0) ? r.xy : r.zw;
    r.x  = (p.y > 0.0) ? r.x  : r.y;
    vec2 q = abs(p) - b + r.x;
    return min(max(q.x, q.y), 0.0) + length(max(q, 0.0)) - r.x;
}

void main() {
    vec2 fragCoord = (vPosition * 0.5 + 0.5) * iResolution;
    fragCoord.y = iResolution.y - fragCoord.y;

    vec2 off = vec2(0.0);
    for (int i = 0; i < 8; i++) {
        if (float(i) >= rippleCount) break;
        vec2 c = vec2(ripples[i * 3], ripples[i * 3 + 1]);
        float age = iTime - ripples[i * 3 + 2];
        if (age < 0.0 || age > 3.0) continue;
        vec2 d = fragCoord - c;
        float dist = length(d) + 0.001;
        float w = sin(dist * 0.09 - age * 7.0) * exp(-age * 2.2) * exp(-dist * 0.004) * 22.0;
        off += (d / dist) * w;
    }
    
    vec2 distortedCoord = fragCoord + off;
    float cellSize = iResolution.x / 3.0;
    vec2 cell = floor(distortedCoord / cellSize);
    vec2 local = fract(distortedCoord / cellSize);
    
    float cellIdx = cell.y * 3.0 + cell.x;
    vec3 cardCol = 0.5 + 0.5 * cos(6.2831 * (cellIdx * 0.08 + vec3(0.0, 0.33, 0.67)));
    
    float margin = 0.08;
    float d = sdRoundedBox(local - 0.5, vec2(0.5 - margin), vec4(0.12));
    
    vec3 bgCol = vec3(0.04, 0.04, 0.08);
    vec3 col = d < 0.0 ? cardCol : bgCol;
    
    if (d < 0.0) {
        col *= (1.0 - 0.15 * smoothstep(-0.02, 0.0, d));
    }
    
    gl_FragColor = vec4(col, 1.0);
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
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                RippleContentAndroid13()
            } else {
                RippleContentGLSLFallback()
            }
        }
    }
}

@Composable
private fun RippleContentAndroid13() {
    val shader = remember { RuntimeShader(RIPPLE_AGSL) }
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
        ripples[head * 3] = x; ripples[head * 3 + 1] = y; ripples[head * 3 + 2] = time
        head = (head + 1) % 8; if (count < 8) count++
    }

    Box(
        Modifier.fillMaxSize()
            .graphicsLayer {
                tick // re-read each frame
                shader.setFloatUniform("iResolution", size.width, size.height)
                shader.setFloatUniform("iTime", time)
                shader.setFloatUniform("rippleCount", count.toFloat())
                shader.setFloatUniform("ripples", ripples)
                renderEffect = RenderEffect.createRuntimeShaderEffect(shader, "content").asComposeRenderEffect()
            }
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        event.changes.forEach { change ->
                            if (change.pressed) {
                                addRipple(change.position.x, change.position.y)
                            }
                        }
                    }
                }
            }
    ) {
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
}

@Composable
private fun RippleContentGLSLFallback() {
    val startTime = remember { System.nanoTime() }
    val ripples = remember { FloatArray(24) }
    var head by remember { mutableIntStateOf(0) }
    var count by remember { mutableIntStateOf(0) }
    var viewWidth by remember { mutableFloatStateOf(0f) }
    var viewHeight by remember { mutableFloatStateOf(0f) }

    fun addRipple(x: Float, y: Float) {
        val time = (System.nanoTime() - startTime) / 1e9f
        ripples[head * 3] = x
        ripples[head * 3 + 1] = y
        ripples[head * 3 + 2] = time
        head = (head + 1) % 8
        if (count < 8) count++
    }

    class RippleRenderer : GLSurfaceView.Renderer {
        private var program = 0
        private var positionHandle = 0
        private var resolutionHandle = 0
        private var timeHandle = 0
        private var rippleCountHandle = 0
        private var ripplesHandle = 0

        private val vertexBuffer: FloatBuffer = ByteBuffer.allocateDirect(8 * 4).run {
            order(ByteOrder.nativeOrder())
            asFloatBuffer().apply {
                put(floatArrayOf(
                    -1f, -1f,
                     1f, -1f,
                    -1f,  1f,
                     1f,  1f
                ))
                position(0)
            }
        }

        override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
            GLES20.glClearColor(0.04f, 0.04f, 0.08f, 1f)
            val vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, VERTEX_SHADER_GLSL)
            val fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, FRAGMENT_SHADER_GLSL)
            program = GLES20.glCreateProgram().apply {
                GLES20.glAttachShader(this, vertexShader)
                GLES20.glAttachShader(this, fragmentShader)
                GLES20.glLinkProgram(this)
            }
            positionHandle = GLES20.glGetAttribLocation(program, "aPosition")
            resolutionHandle = GLES20.glGetUniformLocation(program, "iResolution")
            timeHandle = GLES20.glGetUniformLocation(program, "iTime")
            rippleCountHandle = GLES20.glGetUniformLocation(program, "rippleCount")
            ripplesHandle = GLES20.glGetUniformLocation(program, "ripples")
        }

        override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
            GLES20.glViewport(0, 0, width, height)
            viewWidth = width.toFloat()
            viewHeight = height.toFloat()
        }

        override fun onDrawFrame(gl: GL10?) {
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
            GLES20.glUseProgram(program)
            GLES20.glEnableVertexAttribArray(positionHandle)
            GLES20.glVertexAttribPointer(positionHandle, 2, GLES20.GL_FLOAT, false, 8, vertexBuffer)
            
            GLES20.glUniform2f(resolutionHandle, viewWidth, viewHeight)
            val elapsed = (System.nanoTime() - startTime) / 1e9f
            GLES20.glUniform1f(timeHandle, elapsed)
            GLES20.glUniform1f(rippleCountHandle, count.toFloat())
            GLES20.glUniform1fv(ripplesHandle, 24, ripples, 0)
            
            GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)
            GLES20.glDisableVertexAttribArray(positionHandle)
        }

        private fun loadShader(type: Int, shaderCode: String): Int {
            return GLES20.glCreateShader(type).also { shader ->
                GLES20.glShaderSource(shader, shaderCode)
                GLES20.glCompileShader(shader)
            }
        }
    }

    AndroidView(
        factory = { ctx ->
            object : GLSurfaceView(ctx) {
                override fun onTouchEvent(event: MotionEvent): Boolean {
                    when (event.actionMasked) {
                        MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                            addRipple(event.x, event.y)
                            return true
                        }
                    }
                    return super.onTouchEvent(event)
                }
            }.apply {
                setEGLContextClientVersion(2)
                setRenderer(RippleRenderer())
                renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
            }
        },
        modifier = Modifier.fillMaxSize()
    )
}
