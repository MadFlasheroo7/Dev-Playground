package io.github.shaderengine.pipeline

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.RenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import io.github.shaderengine.core.PlatformRuntimeShader
import io.github.shaderengine.core.ShaderController

/**
 * Multi-pass hardware post-processing pipeline.
 * Evaluates multiple runtime shader passes sequentially on the GPU, where each pass
 * consumes the output of the preceding pass as its [contentUniformName] sampler.
 *
 * Example:
 * ```kotlin
 * val pipeline = rememberShaderPipeline {
 *     addPass(FrostedAcrylicPreset.SHADER)       // Pass 1: Dual Kawase Blur
 *     addPass(MagnifyingDomePreset.SHADER)        // Pass 2: Refractive Fisheye Loupe
 *     addPass(CyberpunkHologramPreset.SHADER)     // Pass 3: Phosphor Scanlines
 * }
 *
 * Box(modifier = Modifier.shaderPipeline(pipeline)) {
 *     // Clickable Composable UI filtered through all 3 chained passes
 * }
 * ```
 */
@Stable
class ShaderPipeline(
    val passes: List<ShaderPass>
) {
    /**
     * Builds a single composite [RenderEffect] chaining all passes together.
     * Returns null if any pass fails compilation or if the platform lacks RenderEffect support.
     */
    fun toCompositeRenderEffect(
        contentUniformName: String = "content",
        baseEffect: RenderEffect? = null,
        width: Float = 0f,
        height: Float = 0f,
        timeSeconds: Float = 0f,
        pointer: Offset = Offset.Zero
    ): RenderEffect? {
        if (passes.isEmpty()) return baseEffect

        var currentEffect: RenderEffect? = baseEffect
        for (pass in passes) {
            if (width > 0f && height > 0f) {
                pass.shader.setFloatUniform("resolution", width, height)
                pass.shader.setFloatUniform("uResolution", width, height)
            }
            pass.shader.setFloatUniform("time", timeSeconds)
            pass.shader.setFloatUniform("uTime", timeSeconds)
            pass.shader.setFloatUniform("pointer", pointer.x, pointer.y)
            pass.shader.setFloatUniform("uPointer", pointer.x, pointer.y)
            pass.updateUniforms()
            currentEffect = pass.shader.toRenderEffect(
                contentUniformName = contentUniformName,
                innerEffect = currentEffect
            ) ?: return null
        }
        return currentEffect
    }
}

/**
 * Individual pass within a [ShaderPipeline].
 */
@Stable
class ShaderPass(
    val shader: PlatformRuntimeShader,
    private val onUpdateUniforms: (PlatformRuntimeShader.() -> Unit)? = null
) {
    fun updateUniforms() {
        onUpdateUniforms?.invoke(shader)
    }
}

/**
 * DSL Builder for creating multi-pass shader pipelines.
 */
class ShaderPipelineBuilder {
    private val passList = ArrayList<ShaderPass>()

    /** Alias for [addPass] matching standard shader pipeline DSL. */
    fun pass(
        shaderSource: String,
        onUpdateUniforms: (PlatformRuntimeShader.() -> Unit)? = null
    ): ShaderPass = addPass(shaderSource, onUpdateUniforms)

    fun pass(
        shader: PlatformRuntimeShader,
        onUpdateUniforms: (PlatformRuntimeShader.() -> Unit)? = null
    ): ShaderPass = addPass(shader, onUpdateUniforms)

    /**
     * Appends a shader source pass with optional uniform configuration.
     */
    fun addPass(
        shaderSource: String,
        onUpdateUniforms: (PlatformRuntimeShader.() -> Unit)? = null
    ): ShaderPass {
        val shader = PlatformRuntimeShader(shaderSource)
        val pass = ShaderPass(shader, onUpdateUniforms)
        passList.add(pass)
        return pass
    }

    /**
     * Appends an existing [PlatformRuntimeShader] instance.
     */
    fun addPass(
        shader: PlatformRuntimeShader,
        onUpdateUniforms: (PlatformRuntimeShader.() -> Unit)? = null
    ): ShaderPass {
        val pass = ShaderPass(shader, onUpdateUniforms)
        passList.add(pass)
        return pass
    }

    internal fun build(): ShaderPipeline = ShaderPipeline(passList)
}

/**
 * Builds a [ShaderPipeline] outside of Composable scope (useful in unit tests & workers).
 */
fun buildShaderPipeline(
    builder: ShaderPipelineBuilder.() -> Unit
): ShaderPipeline = ShaderPipelineBuilder().apply(builder).build()

/**
 * Remembers a multi-pass [ShaderPipeline] constructed via [ShaderPipelineBuilder].
 */
@Composable
fun rememberShaderPipeline(
    vararg keys: Any?,
    builder: ShaderPipelineBuilder.() -> Unit
): ShaderPipeline {
    return remember(*keys) {
        ShaderPipelineBuilder().apply(builder).build()
    }
}

/**
 * Compose modifier applying a multi-pass [ShaderPipeline] to a Composable hierarchy
 * with zero recompositions per frame.
 */
fun Modifier.shaderPipeline(
    pipeline: ShaderPipeline,
    controller: ShaderController? = null
): Modifier {
    return this.graphicsLayer {
        val effect = pipeline.toCompositeRenderEffect(
            width = size.width,
            height = size.height,
            timeSeconds = controller?.timeSeconds ?: 0f,
            pointer = controller?.smoothPointer ?: Offset.Zero
        )
        if (effect != null) {
            renderEffect = effect
        }
    }
}
