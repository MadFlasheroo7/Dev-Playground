package io.github.shaderengine

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ShaderBrush
import io.github.shaderengine.audio.AudioSpectrumManager
import io.github.shaderengine.camera.CameraStreamSampler
import io.github.shaderengine.core.PlatformRuntimeShader
import io.github.shaderengine.core.ShaderController
import io.github.shaderengine.core.ShaderPreprocessor
import io.github.shaderengine.pipeline.ShaderPipeline
import io.github.shaderengine.pipeline.buildShaderPipeline
import io.github.shaderengine.presets.AudioCymaticsPreset
import io.github.shaderengine.presets.ChromaticFerrofluidPreset
import io.github.shaderengine.presets.CosmicNebulaPreset
import io.github.shaderengine.presets.CyberpunkHologramPreset
import io.github.shaderengine.presets.FrostedAcrylicPreset
import io.github.shaderengine.presets.FrostedPrismGlassPreset
import io.github.shaderengine.presets.LiquidMercuryPreset
import io.github.shaderengine.presets.RaymarchedCorePreset
import io.github.shaderengine.presets.ShockwaveRipplePreset
import io.github.shaderengine.sensor.PlatformMotionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ShaderEngineTests {

    @Test
    fun testPlatformMotionManagerDesktopParallaxAxes() {
        val manager = PlatformMotionManager()
        manager.start()

        val bounds = Offset(1000f, 1000f)
        // Moving cursor to the right (x=800, y=500)
        manager.onPointerParallax(Offset(800f, 500f), bounds)
        // tilt.x should be positive (horizontal roll towards right)
        assertTrue(manager.tilt.x > 0f, "Expected positive horizontal tilt.x when cursor moves right, but got ${manager.tilt.x}")
        // tilt.y should remain near 0 (no vertical pitch)
        assertEquals(0f, manager.tilt.y, 0.001f, "Expected tilt.y to be ~0 when moving strictly horizontally")

        // Moving cursor downwards (x=500, y=800)
        manager.stop()
        manager.start()
        manager.onPointerParallax(Offset(500f, 800f), bounds)
        assertTrue(manager.tilt.y > 0f, "Expected positive vertical tilt.y when cursor moves down, but got ${manager.tilt.y}")
        assertEquals(0f, manager.tilt.x, 0.001f, "Expected tilt.x to be ~0 when moving strictly vertically")

        manager.stop()
        assertEquals(Offset.Zero, manager.tilt)
    }

    @Test
    fun testShaderPreprocessorInjectsPi() {
        val testShader = """
            half4 main(float2 fragCoord) {
                return half4(PI, 0.0, 0.0, 1.0);
            }
        """.trimIndent()
        val processed = ShaderPreprocessor.preprocessForSkia(testShader)
        assertTrue(processed.contains("const float PI = 3.14159265359;"))
    }

    @Test
    fun testShaderPreprocessorDoesNotDoubleInjectPi() {
        val testShader = """
            const float PI = 3.14159265359;
            half4 main(float2 fragCoord) {
                return half4(PI, 0.0, 0.0, 1.0);
            }
        """.trimIndent()
        val processed = ShaderPreprocessor.preprocessForSkia(testShader)
        assertEquals(1, Regex("""const float PI""").findAll(processed).count())
    }

    @Test
    fun testAllPresetsCompileOnDesktopSkia() {
        val presets = listOf(
            "FrostedAcrylic" to FrostedAcrylicPreset.SHADER,
            "CyberpunkHologram" to CyberpunkHologramPreset.SHADER,
            "LiquidMercury" to LiquidMercuryPreset.SHADER,
            "ChromaticFerrofluid" to ChromaticFerrofluidPreset.SHADER,
            "FrostedPrismGlass" to FrostedPrismGlassPreset.SHADER,
            "ShockwaveRipple" to ShockwaveRipplePreset.SHADER,
            "RaymarchedCore" to RaymarchedCorePreset.SHADER,
            "CosmicNebula" to CosmicNebulaPreset.SHADER,
            "MagnifyingDome" to io.github.shaderengine.presets.MagnifyingDomePreset.SHADER,
            "ThermalFlir" to io.github.shaderengine.presets.ThermalFlirPreset.SHADER,
            "RainyWindow" to io.github.shaderengine.presets.RainyWindowPreset.SHADER,
            "NightVision" to io.github.shaderengine.presets.NightVisionPreset.SHADER,
            "AudioCymatics" to AudioCymaticsPreset.SHADER
        )

        for ((name, code) in presets) {
            val shader = PlatformRuntimeShader(code)
            val brush = shader.toBrush()
            assertTrue(brush is ShaderBrush, "Expected ShaderBrush for preset $name, but got $brush")
        }
    }

    @Test
    fun testTransitionPresetsCompileOnDesktopSkia() {
        for (preset in io.github.shaderengine.presets.TransitionPreset.entries.filter { !it.isCanvas }) {
            val shader = PlatformRuntimeShader(preset.shaderCode)
            val dummyChildA = PlatformRuntimeShader("""
                half4 main(float2 fragCoord) {
                    return half4(1.0, 0.0, 0.0, 1.0);
                }
            """.trimIndent())
            val dummyChildB = PlatformRuntimeShader("""
                half4 main(float2 fragCoord) {
                    return half4(0.0, 1.0, 0.0, 1.0);
                }
            """.trimIndent())

            shader.setInputShader("imageA", dummyChildA)
            shader.setInputShader("imageB", dummyChildB)
            shader.setFloatUniform("progress", 0.5f)
            shader.setFloatUniform("resolution", 800f, 600f)
            shader.setFloatUniform("time", 1.0f)

            val brush = shader.toBrush()
            assertTrue(brush is ShaderBrush, "Expected ShaderBrush for transition preset: ${preset.title}, but got $brush")
        }
    }

    @Test
    fun testTransitionPresetsCatalogIntegrity() {
        val entries = io.github.shaderengine.presets.TransitionPreset.entries
        assertEquals(41, entries.size, "Expected exactly 41 transition presets in the catalog")
        assertEquals(5, entries.count { it.category == io.github.shaderengine.presets.TransitionCategory.FLUIDS })
        assertEquals(6, entries.count { it.category == io.github.shaderengine.presets.TransitionCategory.ORIGAMI_3D })
        assertEquals(6, entries.count { it.category == io.github.shaderengine.presets.TransitionCategory.WARPS_PHYSICS })
        assertEquals(6, entries.count { it.category == io.github.shaderengine.presets.TransitionCategory.RETRO_GLITCH })
        assertEquals(4, entries.count { it.category == io.github.shaderengine.presets.TransitionCategory.LIGHT_PYRO })
        assertEquals(14, entries.count { it.category == io.github.shaderengine.presets.TransitionCategory.CANVAS })

        for (preset in entries.filter { it.isCanvas }) {
            assertTrue(preset.isCanvas)
            assertNotNull(preset.canvasType)
            assertEquals(io.github.shaderengine.presets.TransitionCategory.CANVAS, preset.category)
        }
    }

    @Test
    fun testTransitionPresetsWithInputBitmapOnDesktopSkia() {
        val progressPoints = listOf(0.0f, 0.25f, 0.5f, 0.75f, 1.0f)
        for (preset in io.github.shaderengine.presets.TransitionPreset.entries.filter { !it.isCanvas }) {
            val shader = PlatformRuntimeShader(preset.shaderCode)
            val bitmapA = ImageBitmap(64, 64)
            val bitmapB = ImageBitmap(64, 64)

            shader.setInputBitmap("imageA", bitmapA)
            shader.setInputBitmap("imageB", bitmapB)
            shader.setFloatUniform("resolution", 64f, 64f)
            shader.setFloatUniform("time", 1.0f)

            for (p in progressPoints) {
                shader.setFloatUniform("progress", p)
                val brush = shader.toBrush()
                assertTrue(brush is ShaderBrush, "Expected ShaderBrush for transition preset with ImageBitmap inputs: ${preset.title} at progress $p, but got $brush")
            }
        }
    }

    @Test
    fun testUniformBufferAndController() {
        val shader = PlatformRuntimeShader(LiquidMercuryPreset.SHADER)
        val scope = CoroutineScope(Dispatchers.Unconfined)
        val controller = ShaderController(LiquidMercuryPreset.SHADER, scope)

        controller.updateTilt(roll = 0.5f, pitch = -0.3f)
        assertEquals(0.5f, controller.tilt.x)
        assertEquals(-0.3f, controller.tilt.y)
        assertEquals(0.5f, controller.gyroVector.x)
        assertEquals(-0.3f, controller.gyroVector.y)

        controller.updatePointer(
            position = Offset(100f, 200f),
            velocity = Offset(10f, 20f),
            isPressed = true
        )
        assertEquals(Offset(100f, 200f), controller.rawPointer)
        assertEquals(Offset(10f, 20f), controller.pointerVelocity)

        // Ensure setting uniforms works without crashing
        controller.uniformBuffer.vec2("uResolution", 800f, 600f)
        controller.uniformBuffer.float("uTime", 1.5f)
        controller.uniformBuffer.float("uSpecularBoost", 2.5f)
    }

    @Test
    fun testAudioSpectrumManager() {
        val manager = AudioSpectrumManager()
        manager.update(bassVal = 0.8f, midVal = 0.5f, trebleVal = 0.3f)
        assertEquals(0.8f, manager.bass)
        assertEquals(0.5f, manager.mid)
        assertEquals(0.3f, manager.treble)
        assertEquals(0.0f, manager.waveform)
        assertTrue(manager.energy > 0f)

        // Clamping test
        manager.update(bassVal = 1.5f, midVal = -0.2f, trebleVal = 0.0f, waveformVal = -1.5f)
        assertEquals(1.0f, manager.bass)
        assertEquals(0.0f, manager.mid)
        assertEquals(0.0f, manager.treble)
        assertEquals(-1.0f, manager.waveform)

        // Simulation generator test
        manager.simulate(timeSeconds = 2.0f)
        assertTrue(manager.bass in 0f..1f)
        assertTrue(manager.mid in 0f..1f)
        assertTrue(manager.treble in 0f..1f)
        assertTrue(manager.energy in 0f..1f)
        assertTrue(manager.waveform in -1f..1f)
    }

    @Test
    fun testShaderPipelineChaining() {
        val pipeline = buildShaderPipeline {
            pass(FrostedAcrylicPreset.SHADER)
            pass(CyberpunkHologramPreset.SHADER)
        }
        assertEquals(2, pipeline.passes.size)
        val effect = pipeline.toCompositeRenderEffect()
        assertNotNull(effect, "Expected non-null chained RenderEffect from pipeline")
    }

    @Test
    fun testCameraStreamSampler() {
        val sampler = CameraStreamSampler(width = 320, height = 240)
        sampler.generateSyntheticFrame(timeSeconds = 1.0f)
        val frame = sampler.currentFrame
        assertNotNull(frame)
        assertEquals(320, frame.width)
        assertEquals(240, frame.height)

        val scope = CoroutineScope(Dispatchers.Unconfined)
        val controller = ShaderController(FrostedAcrylicPreset.SHADER, scope)
        sampler.bindToShader(controller, "cameraStream")
    }
}
