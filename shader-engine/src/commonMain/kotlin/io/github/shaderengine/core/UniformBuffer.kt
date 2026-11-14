package io.github.shaderengine.core

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import kotlin.jvm.JvmInline

/**
 * Zero-allocation inline wrapper around [PlatformRuntimeShader] that provides
 * type-safe uniform setter methods without allocating on the heap during draw passes.
 */
@JvmInline
value class UniformBuffer(@PublishedApi internal val nativeShader: PlatformRuntimeShader) {
    fun float(name: String, v: Float) = nativeShader.setFloatUniform(name, v)
    fun vec2(name: String, x: Float, y: Float) = nativeShader.setFloatUniform(name, x, y)
    fun vec2(name: String, offset: Offset) = nativeShader.setFloatUniform(name, offset.x, offset.y)
    fun vec3(name: String, x: Float, y: Float, z: Float) = nativeShader.setFloatUniform(name, x, y, z)
    fun vec4(name: String, x: Float, y: Float, z: Float, w: Float) = nativeShader.setFloatUniform(name, x, y, z, w)
    fun vec4(name: String, color: Color) = nativeShader.setFloatUniform(name, color.red, color.green, color.blue, color.alpha)
    fun int(name: String, v: Int) = nativeShader.setIntUniform(name, v)
    fun shader(name: String, shader: PlatformRuntimeShader) = nativeShader.setInputShader(name, shader)
    fun shader(name: String, imageBitmap: ImageBitmap) = nativeShader.setInputShader(name, imageBitmap)
    fun bitmap(name: String, bitmap: ImageBitmap) = nativeShader.setInputBitmap(name, bitmap)
}
