package io.github.shaderengine.core

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RenderEffect
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.asComposeShader
import androidx.compose.ui.graphics.asSkiaBitmap
import org.jetbrains.skia.FilterTileMode
import org.jetbrains.skia.ImageFilter
import org.jetbrains.skia.RuntimeEffect
import org.jetbrains.skia.RuntimeShaderBuilder

actual class PlatformRuntimeShader actual constructor(agslSource: String) {
    private val skslSource = ShaderPreprocessor.preprocessForSkia(agslSource)
    private val effect = try {
        RuntimeEffect.makeForShader(skslSource)
    } catch (e: Throwable) {
        println("================ SKSL COMPILATION ERROR ================")
        println(e.message)
        println("========================================================")
        null
    }
    private val builder = effect?.let { RuntimeShaderBuilder(it) }

    internal fun makeSkiaShader(): org.jetbrains.skia.Shader? = builder?.makeShader()

    actual fun setFloatUniform(name: String, v1: Float) {
        runCatching { builder?.uniform(name, v1) }
    }

    actual fun setFloatUniform(name: String, v1: Float, v2: Float) {
        runCatching { builder?.uniform(name, v1, v2) }
    }

    actual fun setFloatUniform(name: String, v1: Float, v2: Float, v3: Float) {
        runCatching {
            builder?.uniform(name, v1, v2, v3)
        }
    }

    actual fun setFloatUniform(name: String, v1: Float, v2: Float, v3: Float, v4: Float) {
        runCatching { builder?.uniform(name, v1, v2, v3, v4) }
    }

    actual fun setIntUniform(name: String, v1: Int) {
        runCatching { builder?.uniform(name, v1) }
    }

    actual fun setInputShader(name: String, shader: PlatformRuntimeShader) {
        val b = builder ?: return
        runCatching {
            val childShader = shader.makeSkiaShader() ?: return
            b.child(name, childShader)
        }
    }

    actual fun setInputShader(name: String, imageBitmap: androidx.compose.ui.graphics.ImageBitmap) {
        setInputBitmap(name, imageBitmap)
    }

    actual fun setInputBitmap(name: String, bitmap: androidx.compose.ui.graphics.ImageBitmap) {
        val b = builder ?: return
        runCatching {
            val skiaBitmap = bitmap.asSkiaBitmap()
            val skiaShader = skiaBitmap.makeShader(FilterTileMode.CLAMP, FilterTileMode.CLAMP)
            b.child(name, skiaShader)
        }
    }

    actual fun toBrush(): Brush {
        val b = builder ?: return SolidColor(Color.Transparent)
        return try {
            val skiaShader = b.makeShader()
            ShaderBrush(skiaShader.asComposeShader())
        } catch (e: Throwable) {
            SolidColor(Color.Transparent)
        }
    }

    actual fun toRenderEffect(
        contentUniformName: String,
        innerEffect: RenderEffect?
    ): RenderEffect? {
        val b = builder ?: return null
        return try {
            val imageFilter = ImageFilter.makeRuntimeShader(
                runtimeShaderBuilder = b,
                shaderName = contentUniformName,
                input = null
            )
            imageFilter.asComposeRenderEffect()
        } catch (e: Throwable) {
            println("================ toRenderEffect ERROR ================")
            println(e.message)
            println("=====================================================")
            null
        }
    }
}
