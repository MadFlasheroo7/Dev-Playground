package pro.jayeshseth.bottomsheet

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * Core bottom sheet container powering both modal and persistent sheet implementations.
 *
 * Provides:
 * - Dynamic anchor computation based on measured container and sheet heights.
 * - Coordinated nested scroll interop with [androidx.compose.foundation.lazy.LazyColumn] and other scrollables.
 * - Fluid velocity-based flings and directional settling physics.
 * - Rubber-band overscroll resistance when pulled beyond anchor bounds.
 * - Interactive Predictive Back scaling and corner morphing on Android 14/15+.
 * - Dynamic contrast switching for system status bar icons.
 */
@Composable
fun BottomSheetLayout(
    sheetState: BottomSheetState,
    modifier: Modifier = Modifier,
    peekHeight: SheetDimension = SheetDimension.Dp(0.dp),
    maxHeight: SheetDimension = SheetDimension.Fraction(0.95f),
    partiallyExpandedHeight: SheetDimension? = null,
    onDismissRequest: (() -> Unit)? = null,
    showScrim: Boolean = true,
    scrimColor: Color = Color.Black.copy(alpha = 0.5f),
    shape: Shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
    containerColor: Color = Color(0xFF1E1E2E),
    contentColor: Color = Color.White,
    tonalElevation: Dp = 1.dp,
    dragHandle: @Composable (() -> Unit)? = { DefaultBottomSheetDragHandle() },
    statusBarTintEnabled: Boolean = true,
    predictiveBackEnabled: Boolean = true,
    sheetModifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val velocityTracker = remember { VelocityTracker() }

    // Status bar dynamic tinting threshold check (~56dp from top)
    val statusBarThresholdPx = with(density) { 56.dp.toPx() }
    val isAtStatusBar = remember(statusBarThresholdPx, sheetState) {
        derivedStateOf {
            sheetState.isNearTopThreshold(statusBarThresholdPx) && sheetState.isVisible
        }
    }
    SheetStatusBarTintEffect(
        enabled = statusBarTintEnabled,
        isAtStatusBar = isAtStatusBar.value,
        sheetContainerColor = containerColor
    )

    // Android 14/15+ Predictive back handler
    SheetPredictiveBackHandler(
        enabled = predictiveBackEnabled,
        state = sheetState,
        onDismiss = {
            scope.launch {
                sheetState.hide()
                onDismissRequest?.invoke()
            }
        }
    )

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val containerHPx = constraints.maxHeight.toFloat()
        var measuredContentHPx by remember { mutableStateOf(0f) }

        // Recalculate anchors whenever layout constraints or measured content dimensions update
        LaunchedEffect(containerHPx, measuredContentHPx, peekHeight, maxHeight, partiallyExpandedHeight, density) {
            if (containerHPx <= 0f) return@LaunchedEffect

            val maxAllowedPx = maxHeight.resolvePx(containerHPx, measuredContentHPx, density)
                .coerceIn(0f, containerHPx)
            val effectiveContentPx = if (measuredContentHPx > 0f) {
                minOf(measuredContentHPx, maxAllowedPx)
            } else {
                maxAllowedPx
            }

            val hiddenY = containerHPx
            val expandedY = (containerHPx - effectiveContentPx).coerceAtLeast(containerHPx - maxAllowedPx).coerceAtLeast(0f)

            val newAnchors = mutableMapOf<SheetValue, Float>()
            if (!sheetState.skipHiddenState) {
                newAnchors[SheetValue.Hidden] = hiddenY
            }
            newAnchors[SheetValue.Expanded] = expandedY

            // Peek / Collapsed anchor
            val peekPx = peekHeight.resolvePx(containerHPx, measuredContentHPx, density).coerceAtLeast(0f)
            if (peekPx > 0f) {
                val collapsedY = (containerHPx - peekPx).coerceIn(expandedY, containerHPx)
                if (collapsedY < hiddenY) {
                    newAnchors[SheetValue.Collapsed] = collapsedY
                }
            }

            // Partially expanded anchor
            if (partiallyExpandedHeight != null && !sheetState.skipPartiallyExpanded) {
                val partialPx = partiallyExpandedHeight.resolvePx(containerHPx, measuredContentHPx, density)
                val partialY = (containerHPx - partialPx).coerceIn(expandedY, containerHPx)
                if (partialY > expandedY && partialY < (newAnchors[SheetValue.Collapsed] ?: hiddenY)) {
                    newAnchors[SheetValue.PartiallyExpanded] = partialY
                }
            }

            sheetState.updateAnchors(newAnchors, containerHPx, effectiveContentPx)
        }

        // 1. Scrim layer
        if (showScrim && sheetState.isVisible) {
            val pbConfig = sheetState.animationConfig.predictiveBackConfig
            val pbFactor = if (sheetState.isPredictiveBackActive) {
                (1f - sheetState.predictiveBackProgress * pbConfig.scrimFadeMultiplier).coerceIn(0f, 1f)
            } else 1f

            val scrimAlpha = (sheetState.progress * scrimColor.alpha * pbFactor).coerceIn(0f, 1f)

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = scrimAlpha }
                    .background(scrimColor.copy(alpha = 1f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        if (sheetState.scrimDismissEnabled) {
                            scope.launch {
                                sheetState.hide()
                                onDismissRequest?.invoke()
                            }
                        }
                    }
            )
        }

        // 2. Sheet Container (kept mounted across visibility states for SharedTransitionLayout tracking)
        val pbConfig = sheetState.animationConfig.predictiveBackConfig
        val pbProgress = sheetState.predictiveBackProgress
        val isPbActive = sheetState.isPredictiveBackActive && pbConfig.enabled

        val scale = if (isPbActive) (1f - pbProgress * pbConfig.maxScaleReduction).coerceIn(0.5f, 1f) else 1f
        val pbExtraY = if (isPbActive) with(density) { pbConfig.maxDownwardShiftDp.toPx() * pbProgress } else 0f

        val effectiveTopRadius = if (isPbActive && shape is RoundedCornerShape) {
            val base = 28.dp
            base + pbConfig.morphCornerRadiusDp * pbProgress
        } else 28.dp

        val dynamicShape = if (shape is RoundedCornerShape) {
            RoundedCornerShape(topStart = effectiveTopRadius, topEnd = effectiveTopRadius)
        } else shape

        val nestedScrollMod = if (sheetState.isVisible) {
            Modifier.nestedScroll(sheetState.nestedScrollConnection)
        } else {
            Modifier
        }

        Surface(
            modifier = sheetModifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .heightIn(max = with(density) { maxHeight.resolvePx(containerHPx, measuredContentHPx, density).toDp() })
                .layout { measurable, constraints ->
                    val placeable = measurable.measure(constraints)
                    val isLookahead = this.isLookingAhead
                    layout(placeable.width, placeable.height) {
                        val y = if (isLookahead) {
                            val target = sheetState.targetValue
                            val targetAnchor = sheetState.anchors[target]
                                ?: if (target == SheetValue.Hidden) {
                                    sheetState.anchors[SheetValue.Hidden] ?: containerHPx
                                } else {
                                    sheetState.anchors[SheetValue.Expanded] ?: sheetState.requireOffset()
                                }
                            targetAnchor.roundToInt()
                        } else {
                            (sheetState.requireOffset() + pbExtraY).roundToInt()
                        }
                        placeable.place(0, y)
                    }
                }
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    alpha = if (!sheetState.hasOffset || !sheetState.isVisible) 0f else 1f
                }
                .then(nestedScrollMod)
                .pointerInput(sheetState, sheetState.isVisible) {
                    if (!sheetState.isVisible) return@pointerInput
                    detectVerticalDragGestures(
                        onDragStart = {
                            velocityTracker.resetTracking()
                        },
                        onVerticalDrag = { change, dragAmount ->
                            change.consume()
                            velocityTracker.addPosition(change.uptimeMillis, change.position)
                            sheetState.dispatchRawDelta(dragAmount)
                        },
                        onDragEnd = {
                            val vy = velocityTracker.calculateVelocity().y
                            scope.launch {
                                sheetState.settle(vy)
                                if (sheetState.currentValue == SheetValue.Hidden) {
                                    onDismissRequest?.invoke()
                                }
                            }
                        },
                        onDragCancel = {
                            scope.launch {
                                sheetState.settle(0f)
                            }
                        }
                    )
                }
                .onSizeChanged { size ->
                    measuredContentHPx = size.height.toFloat()
                },
            shape = dynamicShape,
            color = containerColor,
            contentColor = contentColor,
            tonalElevation = tonalElevation
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (dragHandle != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .pointerInput(sheetState, sheetState.isVisible) {
                                if (!sheetState.isVisible) return@pointerInput
                                detectVerticalDragGestures(
                                    onDragStart = { velocityTracker.resetTracking() },
                                    onVerticalDrag = { change, dragAmount ->
                                        change.consume()
                                        velocityTracker.addPosition(change.uptimeMillis, change.position)
                                        sheetState.dispatchRawDelta(dragAmount)
                                    },
                                    onDragEnd = {
                                        val vy = velocityTracker.calculateVelocity().y
                                        scope.launch {
                                            sheetState.settle(vy)
                                            if (sheetState.currentValue == SheetValue.Hidden) {
                                                onDismissRequest?.invoke()
                                            }
                                        }
                                    },
                                    onDragCancel = {
                                        scope.launch { sheetState.settle(0f) }
                                    }
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        dragHandle()
                    }
                }
                content()
            }
        }
    }
}
