package pro.jayeshseth.devcontrols.platform

import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import pro.jayeshseth.devcontrols.ui.present.Deck
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.runtime.produceState
import androidx.compose.animation.core.withInfiniteAnimationFrameMillis
import androidx.compose.runtime.getValue

// Browser Fullscreen API bridge — js() is target-specific, so expect/actual (identical js + wasmJs actuals).
internal expect fun enterFullscreen()
internal expect fun exitFullscreen()

// No java.util.zip on wasm — store the deck JSON uncompressed. DeckJson.read() handles the
// '{' (raw JSON) branch, so these files still open on JVM platforms too.
actual object DeckArchive {
    actual fun pack(json: String): ByteArray = json.encodeToByteArray()
    actual fun unpack(bytes: ByteArray): String = bytes.decodeToString()
}

@Composable
actual fun rememberDeckFileIo(): DeckFileIo = remember {
    object : DeckFileIo {
        override fun save(suggestedName: String, bytes: ByteArray) { println("[DevControls] save not yet supported on web: $suggestedName") }
        override fun open(onLoaded: (ByteArray?) -> Unit) { onLoaded(null) }
    }
}

@Composable
actual fun rememberDeckExporter(): DeckExporter? = null

@Composable
actual fun rememberToaster(): (String) -> Unit = remember { { msg: String -> println("[DevControls] $msg") } }

@Composable
actual fun rememberHapticPulse(): () -> Unit = remember { {} }

@Composable
actual fun MicrophonePermissionGate(rationale: String, content: @Composable () -> Unit) {
    content()
}

// Enter browser fullscreen while the deck is presenting; restore on exit. (Fullscreen needs a recent
// user gesture — entering present mode from a tap provides it.)
@Composable
actual fun PresentationMode() {
    DisposableEffect(Unit) {
        enterFullscreen()
        onDispose { exitFullscreen() }
    }
}

@Composable
actual fun ShaderBackground(source: String, modifier: Modifier, fallback: @Composable () -> Unit) {
    val effect = remember(source) {
        runCatching { org.jetbrains.skia.RuntimeEffect.makeForShader(source) }.getOrNull()
    }
    if (effect == null) { Box(modifier) { fallback() }; return }
    val time by produceState(0f) {
        while (true) { withInfiniteAnimationFrameMillis { value = it / 1000f } }
    }
    Box(
        modifier.drawBehind {
            runCatching {
                val w = size.width; val h = size.height
                
                // Pack uniforms: w, h, time (12 bytes)
                val bytes = ByteArray(12)
                fun writeFloat(f: Float, offset: Int) {
                    val bits = f.toRawBits()
                    bytes[offset] = (bits and 0xFF).toByte()
                    bytes[offset + 1] = ((bits shr 8) and 0xFF).toByte()
                    bytes[offset + 2] = ((bits shr 16) and 0xFF).toByte()
                    bytes[offset + 3] = ((bits shr 24) and 0xFF).toByte()
                }
                writeFloat(w, 0)
                writeFloat(h, 4)
                writeFloat(time, 8)
                
                val shader = effect.makeShader(
                    org.jetbrains.skia.Data.makeFromBytes(bytes), null, null
                )
                val paint = org.jetbrains.skia.Paint().apply { this.shader = shader }
                drawIntoCanvas { it.nativeCanvas.drawRect(org.jetbrains.skia.Rect.makeWH(w, h), paint) }
            }
        }
    )
}

@Composable
actual fun CustomShaderBox(
    shaderSource: String,
    modifier: Modifier,
    uniformNames: List<String>,
    uniforms: Map<String, Any>,
    fallback: @Composable () -> Unit
) {
    val effect = remember(shaderSource) {
        runCatching { org.jetbrains.skia.RuntimeEffect.makeForShader(shaderSource) }.getOrNull()
    }
    if (effect == null) { Box(modifier) { fallback() }; return }
    Box(
        modifier.drawBehind {
            runCatching {
                val w = size.width; val h = size.height
                
                var floatCount = 0
                uniformNames.forEach { name ->
                    val value = uniforms[name]
                    when (value) {
                        is Float -> floatCount += 1
                        is androidx.compose.ui.geometry.Offset -> floatCount += 2
                        is androidx.compose.ui.graphics.Color -> floatCount += 4
                        is FloatArray -> floatCount += value.size
                    }
                }
                
                val bytes = ByteArray(floatCount * 4)
                var offset = 0
                
                fun writeFloat(f: Float) {
                    val bits = f.toRawBits()
                    bytes[offset] = (bits and 0xFF).toByte()
                    bytes[offset + 1] = ((bits shr 8) and 0xFF).toByte()
                    bytes[offset + 2] = ((bits shr 16) and 0xFF).toByte()
                    bytes[offset + 3] = ((bits shr 24) and 0xFF).toByte()
                    offset += 4
                }
                
                uniformNames.forEach { name ->
                    val value = uniforms[name]
                    when (value) {
                        is Float -> writeFloat(value)
                        is androidx.compose.ui.geometry.Offset -> {
                            writeFloat(value.x)
                            writeFloat(value.y)
                        }
                        is androidx.compose.ui.graphics.Color -> {
                            writeFloat(value.red)
                            writeFloat(value.green)
                            writeFloat(value.blue)
                            writeFloat(value.alpha)
                        }
                        is FloatArray -> {
                            value.forEach { writeFloat(it) }
                        }
                    }
                }
                
                val shader = effect.makeShader(
                    org.jetbrains.skia.Data.makeFromBytes(bytes), null, null
                )
                val paint = org.jetbrains.skia.Paint().apply { this.shader = shader }
                drawIntoCanvas { it.nativeCanvas.drawRect(org.jetbrains.skia.Rect.makeWH(w, h), paint) }
            }
        }
    )
}

actual class PlatformShader(val effect: org.jetbrains.skia.RuntimeEffect)

actual fun createPlatformShader(shaderSource: String): PlatformShader? {
    return runCatching { PlatformShader(org.jetbrains.skia.RuntimeEffect.makeForShader(shaderSource)) }.getOrNull()
}

actual fun createRuntimeShaderEffect(
    shader: PlatformShader,
    uniforms: Map<String, Any>,
    inputShaderName: String
): androidx.compose.ui.graphics.RenderEffect? {
    try {
        val builder = org.jetbrains.skia.RuntimeShaderBuilder(shader.effect)
        uniforms.forEach { (name, value) ->
            when (value) {
                is Float -> builder.uniform(name, value)
                is androidx.compose.ui.geometry.Offset -> builder.uniform(name, value.x, value.y)
                is androidx.compose.ui.graphics.Color -> {
                    builder.uniform(name, value.red, value.green, value.blue, value.alpha)
                }
                is FloatArray -> builder.uniform(name, value)
            }
        }
        val imageFilter = org.jetbrains.skia.ImageFilter.makeRuntimeShader(
            runtimeShaderBuilder = builder,
            shaderName = inputShaderName,
            input = null
        )
        return imageFilter.asComposeRenderEffect()
    } catch (e: Exception) {
        return null
    }
}

private fun jsDateNow(): Double = js("Date.now()")

actual fun getPlatformTimeMillis(): Long = jsDateNow().toLong()
