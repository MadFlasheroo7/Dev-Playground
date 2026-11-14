package io.github.shaderengine.transitions

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.zIndex
import io.github.shaderengine.core.rememberShaderController
import io.github.shaderengine.modifiers.generativeShaderCanvas
import io.github.shaderengine.presets.TransitionPreset
import kotlinx.coroutines.launch

/**
 * Declarative Compose container for dual-input AGSL and SkSL transitions.
 *
 * Captures two distinct Composable hierarchies ([fromContent] and [toContent])
 * into GPU hardware layers and blends them in real time using a dual-input runtime shader
 * ([uniform shader imageA; uniform shader imageB;]).
 *
 * Supports interactive horizontal drag scrubbing with spring snap physics.
 */
@Composable
fun ShaderTransitionLayout(
    progress: Float,
    preset: TransitionPreset,
    modifier: Modifier = Modifier,
    onProgressChange: ((Float) -> Unit)? = null,
    fromContent: @Composable () -> Unit,
    toContent: @Composable () -> Unit
) {
    if (preset.isCanvas) {
        CanvasTransitionLayout(
            progress = progress,
            preset = preset,
            modifier = modifier,
            onProgressChange = onProgressChange,
            fromContent = fromContent,
            toContent = toContent
        )
        return
    }

    val animProgress = remember { Animatable(progress) }
    var isDragging by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val layerA = rememberGraphicsLayer()
    val layerB = rememberGraphicsLayer()
    var bitmapA by remember { mutableStateOf<ImageBitmap?>(null) }
    var bitmapB by remember { mutableStateOf<ImageBitmap?>(null) }
    var currentSize by remember { mutableStateOf(IntSize.Zero) }

    // Sync external progress changes when not actively dragging
    LaunchedEffect(progress) {
        if (!isDragging) {
            animProgress.snapTo(progress)
        }
    }

    // Refresh captured bitmaps when size changes or layers are ready
    LaunchedEffect(layerA, layerB, currentSize) {
        if (currentSize.width > 0 && currentSize.height > 0) {
            withFrameNanos { }
            withFrameNanos { }
            try {
                bitmapA = layerA.toImageBitmap()
                bitmapB = layerB.toImageBitmap()
            } catch (_: Throwable) {
            }
        }
    }

    val currentP = animProgress.value
    val isTransitioning = currentP > 0.001f && currentP < 0.999f

    // Recapture snapshots whenever transitioning begins to ensure fresh composable state
    LaunchedEffect(isTransitioning) {
        if (isTransitioning) {
            try {
                bitmapA = layerA.toImageBitmap()
                bitmapB = layerB.toImageBitmap()
            } catch (_: Throwable) {
            }
        }
    }

    Box(
        modifier = modifier
            .onSizeChanged { currentSize = it }
            .pointerInput(preset) {
                detectHorizontalDragGestures(
                    onDragStart = {
                        isDragging = true
                        scope.launch {
                            try {
                                bitmapA = layerA.toImageBitmap()
                                bitmapB = layerB.toImageBitmap()
                            } catch (_: Throwable) {
                            }
                        }
                    },
                    onDragEnd = {
                        scope.launch {
                            try {
                                val target = if (animProgress.value >= 0.5f) 1f else 0f
                                animProgress.animateTo(
                                    targetValue = target,
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioMediumBouncy,
                                        stiffness = Spring.StiffnessLow
                                    )
                                ) {
                                    onProgressChange?.invoke(value)
                                }
                            } finally {
                                isDragging = false
                            }
                        }
                    },
                    onDragCancel = {
                        isDragging = false
                    },
                    onHorizontalDrag = { _, dragAmount ->
                        val width = currentSize.width.toFloat().coerceAtLeast(1f)
                        val next = (animProgress.value + dragAmount / width).coerceIn(0f, 1f)
                        scope.launch {
                            animProgress.snapTo(next)
                            onProgressChange?.invoke(next)
                        }
                    }
                )
            }
    ) {
        // Slot A: Outgoing content
        Box(
            modifier = Modifier
                .fillMaxSize()
                .zIndex(if (currentP < 0.5f) 1f else 0f)
                .drawWithContent {
                    layerA.record {
                        this@drawWithContent.drawContent()
                    }
                    if (currentP <= 0.001f) {
                        drawLayer(layerA)
                    }
                }
        ) {
            fromContent()
        }

        // Slot B: Incoming content
        Box(
            modifier = Modifier
                .fillMaxSize()
                .zIndex(if (currentP >= 0.5f) 1f else 0f)
                .drawWithContent {
                    layerB.record {
                        this@drawWithContent.drawContent()
                    }
                    if (currentP >= 0.999f) {
                        drawLayer(layerB)
                    }
                }
        ) {
            toContent()
        }

        // Transition Overlay: Dual-Input AGSL/SkSL GPU Pass
        if (isTransitioning) {
            val controller = rememberShaderController(preset.shaderCode)
            val bA = bitmapA
            val bB = bitmapB

            var boundBitmapA by remember(preset) { mutableStateOf<ImageBitmap?>(null) }
            var boundBitmapB by remember(preset) { mutableStateOf<ImageBitmap?>(null) }

            if (bA != null && bB != null && (boundBitmapA !== bA || boundBitmapB !== bB)) {
                controller.uniformBuffer.bitmap("imageA", bA)
                controller.uniformBuffer.bitmap("imageB", bB)
                boundBitmapA = bA
                boundBitmapB = bB
            }

            if (bA != null && bB != null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .zIndex(2f)
                        .generativeShaderCanvas(controller) {
                            float("progress", currentP)
                            float("uProgress", currentP)
                        }
                )
            } else {
                // Fallback crossfade while layer snapshots are being captured
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .zIndex(2f)
                ) {
                    if (currentP < 0.5f) {
                        drawLayer(layerA)
                    } else {
                        drawLayer(layerB)
                    }
                }
            }
        }
    }
}
