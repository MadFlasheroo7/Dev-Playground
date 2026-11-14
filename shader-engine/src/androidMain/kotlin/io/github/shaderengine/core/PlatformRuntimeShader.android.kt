package io.github.shaderengine.core

import android.graphics.RenderEffect as AndroidRenderEffect
import android.graphics.RuntimeShader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RenderEffect
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asComposeRenderEffect

actual class PlatformRuntimeShader actual constructor(agslSource: String) {
    private val delegate: AndroidShaderDelegate? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        try {
            AndroidTiramisuShaderDelegate(agslSource)
        } catch (e: Throwable) {
            android.util.Log.e("PlatformRuntimeShader", "AGSL compilation failed for shader:\n$agslSource", e)
            null
        }
    } else {
        null
    }

    internal fun getNativeShader(): android.graphics.Shader? = delegate?.getNativeShader()

    actual fun setFloatUniform(name: String, v1: Float) {
        delegate?.setFloatUniform(name, v1)
    }

    actual fun setFloatUniform(name: String, v1: Float, v2: Float) {
        delegate?.setFloatUniform(name, v1, v2)
    }

    actual fun setFloatUniform(name: String, v1: Float, v2: Float, v3: Float) {
        delegate?.setFloatUniform(name, v1, v2, v3)
    }

    actual fun setFloatUniform(name: String, v1: Float, v2: Float, v3: Float, v4: Float) {
        delegate?.setFloatUniform(name, v1, v2, v3, v4)
    }

    actual fun setIntUniform(name: String, v1: Int) {
        delegate?.setIntUniform(name, v1)
    }

    actual fun setInputShader(name: String, shader: PlatformRuntimeShader) {
        delegate?.setInputShader(name, shader)
    }

    actual fun setInputShader(name: String, imageBitmap: ImageBitmap) {
        delegate?.setInputBitmap(name, imageBitmap)
    }

    actual fun setInputBitmap(name: String, bitmap: ImageBitmap) {
        delegate?.setInputBitmap(name, bitmap)
    }

    actual fun toBrush(): Brush {
        return delegate?.toBrush() ?: SolidColor(Color.Transparent)
    }

    actual fun toRenderEffect(
        contentUniformName: String,
        innerEffect: RenderEffect?
    ): RenderEffect? {
        return delegate?.toRenderEffect(contentUniformName, innerEffect)
    }
}

private interface AndroidShaderDelegate {
    fun getNativeShader(): android.graphics.Shader?
    fun setFloatUniform(name: String, v1: Float)
    fun setFloatUniform(name: String, v1: Float, v2: Float)
    fun setFloatUniform(name: String, v1: Float, v2: Float, v3: Float)
    fun setFloatUniform(name: String, v1: Float, v2: Float, v3: Float, v4: Float)
    fun setIntUniform(name: String, v1: Int)
    fun setInputShader(name: String, shader: PlatformRuntimeShader)
    fun setInputBitmap(name: String, bitmap: ImageBitmap)
    fun toBrush(): Brush
    fun toRenderEffect(contentUniformName: String, innerEffect: RenderEffect?): RenderEffect?
}

private val UNIFORM_REGEX = Regex("""uniform\s+\w+\s+(\w+)""")

private fun extractUniformNames(source: String): Set<String> {
    return UNIFORM_REGEX.findAll(source).map { it.groupValues[1] }.toSet()
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
private class AndroidTiramisuShaderDelegate(agslSource: String) : AndroidShaderDelegate {
    private val declaredUniforms = extractUniformNames(agslSource)
    private val runtimeShader = RuntimeShader(agslSource)
    private val cachedBrush = ShaderBrush(runtimeShader)

    override fun getNativeShader(): android.graphics.Shader = runtimeShader

    override fun setFloatUniform(name: String, v1: Float) {
        if (name !in declaredUniforms) return
        runCatching { runtimeShader.setFloatUniform(name, v1) }
    }

    override fun setFloatUniform(name: String, v1: Float, v2: Float) {
        if (name !in declaredUniforms) return
        runCatching { runtimeShader.setFloatUniform(name, v1, v2) }
    }

    override fun setFloatUniform(name: String, v1: Float, v2: Float, v3: Float) {
        if (name !in declaredUniforms) return
        runCatching { runtimeShader.setFloatUniform(name, v1, v2, v3) }
    }

    override fun setFloatUniform(name: String, v1: Float, v2: Float, v3: Float, v4: Float) {
        if (name !in declaredUniforms) return
        runCatching { runtimeShader.setFloatUniform(name, v1, v2, v3, v4) }
    }

    override fun setIntUniform(name: String, v1: Int) {
        if (name !in declaredUniforms) return
        runCatching { runtimeShader.setIntUniform(name, v1) }
    }

    override fun setInputShader(name: String, shader: PlatformRuntimeShader) {
        if (name !in declaredUniforms) return
        val childNative = shader.getNativeShader() ?: return
        runCatching {
            runtimeShader.setInputShader(name, childNative)
        }
    }

    override fun setInputBitmap(name: String, bitmap: ImageBitmap) {
        if (name !in declaredUniforms) return
        runCatching {
            val androidBitmap = bitmap.asAndroidBitmap()
            val bitmapShader = android.graphics.BitmapShader(androidBitmap, android.graphics.Shader.TileMode.CLAMP, android.graphics.Shader.TileMode.CLAMP)
            runtimeShader.setInputShader(name, bitmapShader)
        }
    }

    override fun toBrush(): Brush = cachedBrush

    override fun toRenderEffect(
        contentUniformName: String,
        innerEffect: RenderEffect?
    ): RenderEffect? {
        return try {
            val shaderEffect = AndroidRenderEffect.createRuntimeShaderEffect(
                runtimeShader,
                contentUniformName
            )
            val finalEffect = if (innerEffect != null) {
                AndroidRenderEffect.createChainEffect(
                    shaderEffect,
                    innerEffect.asAndroidRenderEffect()
                )
            } else {
                shaderEffect
            }
            finalEffect.asComposeRenderEffect()
        } catch (e: Throwable) {
            null
        }
    }
}
