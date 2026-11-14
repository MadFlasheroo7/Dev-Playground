package io.github.shaderengine.core

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.RenderEffect
import androidx.compose.ui.graphics.ShaderBrush

/**
 * Platform abstraction bridging Android's [android.graphics.RuntimeShader] (API 33+)
 * and Skia's [org.jetbrains.skia.RuntimeEffect] on Desktop, iOS, and Web.
 */
expect class PlatformRuntimeShader(agslSource: String) {
    fun setFloatUniform(name: String, v1: Float)
    fun setFloatUniform(name: String, v1: Float, v2: Float)
    fun setFloatUniform(name: String, v1: Float, v2: Float, v3: Float)
    fun setFloatUniform(name: String, v1: Float, v2: Float, v3: Float, v4: Float)
    fun setIntUniform(name: String, v1: Int)
    fun setInputShader(name: String, shader: PlatformRuntimeShader)
    fun setInputShader(name: String, imageBitmap: androidx.compose.ui.graphics.ImageBitmap)
    fun setInputBitmap(name: String, bitmap: androidx.compose.ui.graphics.ImageBitmap)

    /**
     * Creates a Compose [Brush] for generative canvases (DrawScope).
     */
    fun toBrush(): Brush

    /**
     * Creates a Compose [RenderEffect] for UI post-processing (graphicsLayer).
     * Optionally chains this shader after a preceding [innerEffect] (e.g. BlurEffect).
     * Returns null if the underlying platform does not support runtime shader render effects (e.g., Android < API 33).
     */
    fun toRenderEffect(
        contentUniformName: String = "content",
        innerEffect: RenderEffect? = null
    ): RenderEffect?
}
