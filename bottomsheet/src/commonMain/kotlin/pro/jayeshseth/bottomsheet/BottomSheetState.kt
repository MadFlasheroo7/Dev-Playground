package pro.jayeshseth.bottomsheet

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.unit.Velocity
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import kotlin.math.abs

/**
 * State holding the positioning, gesture physics, anchors, and animation progress of a bottom sheet.
 */
@Stable
class BottomSheetState(
    val initialValue: SheetValue = SheetValue.Hidden,
    animationConfig: SheetAnimationConfig = SheetAnimationConfig(),
    val confirmValueChange: (SheetValue) -> Boolean = { true },
    val skipHiddenState: Boolean = false,
    val skipPartiallyExpanded: Boolean = false,
) {
    /** Dynamic animation configuration for enter, exit, snap, and settled transitions. */
    var animationConfig: SheetAnimationConfig by mutableStateOf(animationConfig)

    /** The current resting [SheetValue] of the sheet. */
    var currentValue: SheetValue by mutableStateOf(initialValue)
        internal set

    /** The target [SheetValue] being animated or dragged toward. */
    var targetValue: SheetValue by mutableStateOf(initialValue)
        internal set

    /** Whether swipe to dismiss downward gesture is enabled. */
    var swipeToDismissEnabled: Boolean by mutableStateOf(true)

    /** Whether tapping the scrim overlay triggers dismissal. */
    var scrimDismissEnabled: Boolean by mutableStateOf(true)

    /** Synchronous backing offset in physical pixels from top of container. */
    internal var rawOffset: Float by mutableFloatStateOf(Float.NaN)

    /** Current pixel offset from the top of the container, or null before initial measurement. */
    var hasOffset: Boolean by mutableStateOf(false)
        internal set

    val offset: Float?
        get() = if (hasOffset && !rawOffset.isNaN()) rawOffset else null

    fun requireOffset(): Float {
        return if (hasOffset && !rawOffset.isNaN()) {
            rawOffset
        } else if (containerHeightPx > 0f) {
            anchors[initialValue] ?: anchors[SheetValue.Hidden] ?: containerHeightPx
        } else {
            10000f
        }
    }

    /** Current computed anchor offsets for valid sheet values. */
    var anchors: Map<SheetValue, Float> by mutableStateOf(emptyMap())
        internal set

    private val anchorUpdateSignal = kotlinx.coroutines.flow.MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    /** Whether an animation (snap, settle, enter, exit) is actively running. */
    var isAnimationRunning: Boolean by mutableStateOf(false)
        internal set

    /** Whether the sheet is currently visible (either not Hidden or animating out of Hidden). */
    val isVisible: Boolean by derivedStateOf {
        currentValue != SheetValue.Hidden ||
            targetValue != SheetValue.Hidden ||
            (hasOffset && anchors[SheetValue.Hidden]?.let { rawOffset < it - 0.5f } == true)
    }

    /** Measured dimensions for anchor evaluation. */
    var containerHeightPx: Float by mutableFloatStateOf(0f)
        internal set
    var contentHeightPx: Float by mutableFloatStateOf(0f)
        internal set

    /** Minimum anchor offset (highest visual position, typically [SheetValue.Expanded]). */
    val minAnchor: Float?
        get() = anchors.values.minOrNull()

    /** Maximum anchor offset (lowest visual position, typically [SheetValue.Hidden] or [SheetValue.Collapsed]). */
    val maxAnchor: Float?
        get() = anchors.values.maxOrNull()

    /** Predictive back interactive gesture progress (0.0 .. 1.0). */
    var predictiveBackProgress: Float by mutableFloatStateOf(0f)
        internal set

    /** Whether a predictive back gesture is currently actively dragging the sheet. */
    var isPredictiveBackActive: Boolean by mutableStateOf(false)
        internal set

    /**
     * Normalized progress (0.0 .. 1.0) of sheet expansion.
     * 0.0 indicates collapsed/hidden, 1.0 indicates fully expanded.
     */
    val progress: Float by derivedStateOf {
        val expandedY = anchors[SheetValue.Expanded] ?: 0f
        val bottomY = anchors[SheetValue.Hidden]
            ?: anchors[SheetValue.Collapsed]
            ?: containerHeightPx
        val range = bottomY - expandedY
        if (range <= 0.001f) 1f
        else {
            val curr = offset ?: bottomY
            ((bottomY - curr) / range).coerceIn(0f, 1f)
        }
    }

    /**
     * Status bar threshold detection: true when the sheet offset has expanded to within thresholdPx of the top.
     */
    fun isNearTopThreshold(thresholdPx: Float): Boolean {
        val curr = offset ?: return false
        return curr <= thresholdPx
    }

    /**
     * Dispatches a raw vertical drag delta synchronously in physical pixels.
     * Applies smooth rubber-band resistance when dragged beyond the valid anchor limits.
     *
     * @return Consumed vertical delta in pixels.
     */
    fun dispatchRawDelta(delta: Float): Float {
        if (!hasOffset || anchors.isEmpty()) return 0f
        val current = rawOffset
        val topLimit = minAnchor ?: 0f
        val bottomLimit = if (swipeToDismissEnabled) {
            anchors[SheetValue.Hidden] ?: containerHeightPx
        } else {
            anchors[SheetValue.Collapsed] ?: anchors[SheetValue.Expanded] ?: containerHeightPx
        }

        val effectiveDelta = when {
            // Dragging up beyond top limit (Expanded)
            delta < 0f && current <= topLimit -> {
                val overscroll = topLimit - current
                val factor = (1f / (1f + overscroll * 0.005f)) * 0.35f
                delta * factor
            }
            // Dragging down beyond bottom limit
            delta > 0f && current >= bottomLimit -> {
                val overscroll = current - bottomLimit
                val factor = (1f / (1f + overscroll * 0.005f)) * 0.35f
                delta * factor
            }
            else -> {
                val newCandidate = current + delta
                if (newCandidate < topLimit) {
                    val normal = topLimit - current
                    val over = (delta - normal) * 0.35f
                    normal + over
                } else if (newCandidate > bottomLimit) {
                    val normal = bottomLimit - current
                    val over = (delta - normal) * 0.35f
                    normal + over
                } else {
                    delta
                }
            }
        }

        rawOffset = current + effectiveDelta
        return effectiveDelta
    }

    /**
     * Settles the sheet to the closest or directionally appropriate anchor given the release fling [velocity].
     */
    suspend fun settle(velocity: Float) {
        if (!hasOffset || anchors.isEmpty()) return
        val current = rawOffset
        val validAnchors = anchors.filter { (value, _) ->
            if (!swipeToDismissEnabled && value == SheetValue.Hidden) false
            else true
        }
        if (validAnchors.isEmpty()) return

        val targetAnchorEntry: Map.Entry<SheetValue, Float> = when {
            // Strong downward fling (velocity > 600 px/s)
            velocity > 600f -> {
                val below = validAnchors.filter { it.value > current + 10f }
                if (below.isNotEmpty()) {
                    below.minByOrNull { it.value }!!
                } else {
                    validAnchors.maxByOrNull { it.value }!!
                }
            }
            // Strong upward fling (velocity < -600 px/s)
            velocity < -600f -> {
                val above = validAnchors.filter { it.value < current - 10f }
                if (above.isNotEmpty()) {
                    above.maxByOrNull { it.value }!!
                } else {
                    validAnchors.minByOrNull { it.value }!!
                }
            }
            // Low velocity release: settle to closest projected rest point
            else -> {
                val projected = current + (velocity * 0.12f)
                validAnchors.minByOrNull { abs(it.value - projected) }!!
            }
        }

        val candidateValue = targetAnchorEntry.key
        val settledValue = if (confirmValueChange(candidateValue)) candidateValue else currentValue
        val targetY = anchors[settledValue] ?: targetAnchorEntry.value

        targetValue = settledValue
        val spec = when {
            settledValue == SheetValue.Hidden -> animationConfig.exitSpec
            currentValue == SheetValue.Hidden || targetY < current -> animationConfig.enterSpec
            else -> animationConfig.snapSpec
        }

        isAnimationRunning = true
        try {
            val anim = Animatable(rawOffset)
            anim.animateTo(
                targetValue = targetY,
                animationSpec = spec,
                initialVelocity = velocity
            ) {
                rawOffset = value
            }
            rawOffset = targetY
            currentValue = settledValue
        } catch (_: CancellationException) {
            // Cancelled by another gesture or animation
        } finally {
            isAnimationRunning = false
        }
    }

    /**
     * Smoothly animates the sheet to [target] using [spec] or the configured animation spec.
     */
    suspend fun animateTo(target: SheetValue, spec: AnimationSpec<Float>? = null) {
        if (!confirmValueChange(target)) return
        targetValue = target

        // If anchors are not yet populated (e.g. before first layout measurement), wait until they are.
        if (anchors.isEmpty()) {
            anchorUpdateSignal.first()
        }

        val targetY = anchors[target] ?: run {
            currentValue = target
            return
        }
        val animationSpec = spec ?: when {
            target == SheetValue.Hidden -> animationConfig.exitSpec
            currentValue == SheetValue.Hidden || (!rawOffset.isNaN() && targetY < rawOffset) -> animationConfig.enterSpec
            else -> animationConfig.snapSpec
        }

        isAnimationRunning = true
        try {
            // Ensure starting offset is valid and starts from offscreen bottom when opening from Hidden
            val candidateHiddenY = anchors[SheetValue.Hidden]
                ?: anchors[SheetValue.Collapsed]
                ?: maxAnchor
                ?: if (!containerHeightPx.isNaN() && containerHeightPx > 0f) containerHeightPx else 10000f
            val validHiddenY = if (!candidateHiddenY.isNaN() && candidateHiddenY > 0f) candidateHiddenY else 10000f

            if (currentValue == SheetValue.Hidden || !hasOffset || rawOffset.isNaN()) {
                rawOffset = validHiddenY
                hasOffset = true
            }
            val startOffset = if (rawOffset.isNaN()) validHiddenY else rawOffset
            rawOffset = startOffset
            val anim = Animatable(startOffset)
            anim.animateTo(targetValue = targetY, animationSpec = animationSpec) {
                rawOffset = value
            }
            rawOffset = targetY
            currentValue = target
        } catch (_: CancellationException) {
            // Gesture interrupted
        } finally {
            isAnimationRunning = false
        }
    }

    /**
     * Snaps the sheet immediately to [target] without animation.
     */
    suspend fun snapTo(target: SheetValue) {
        if (!confirmValueChange(target)) return
        targetValue = target
        if (anchors.isEmpty()) {
            anchorUpdateSignal.first()
        }
        val targetY = anchors[target] ?: run {
            currentValue = target
            return
        }
        rawOffset = targetY
        currentValue = target
    }

    /**
     * Animates sheet into view: expands to [SheetValue.PartiallyExpanded] if defined and not skipped,
     * otherwise animates directly to [SheetValue.Expanded].
     */
    suspend fun show(spec: AnimationSpec<Float>? = null) {
        if (anchors.isEmpty()) {
            anchorUpdateSignal.first()
        }
        val target = if (!skipPartiallyExpanded && anchors.containsKey(SheetValue.PartiallyExpanded)) {
            SheetValue.PartiallyExpanded
        } else {
            SheetValue.Expanded
        }
        animateTo(target, spec)
    }

    /** Animates sheet to [SheetValue.Expanded]. */
    suspend fun expand(spec: AnimationSpec<Float>? = null) = animateTo(SheetValue.Expanded, spec)

    /** Animates sheet to [SheetValue.Collapsed] if available, otherwise [SheetValue.Hidden]. */
    suspend fun collapse(spec: AnimationSpec<Float>? = null) {
        if (anchors.isEmpty()) {
            anchorUpdateSignal.first()
        }
        val target = if (anchors.containsKey(SheetValue.Collapsed)) SheetValue.Collapsed else SheetValue.Hidden
        animateTo(target, spec)
    }

    /** Animates sheet to [SheetValue.PartiallyExpanded] if available. */
    suspend fun partialExpand(spec: AnimationSpec<Float>? = null) {
        if (!confirmValueChange(SheetValue.PartiallyExpanded)) return
        targetValue = SheetValue.PartiallyExpanded
        if (anchors.isEmpty()) {
            anchorUpdateSignal.first()
        }
        if (anchors.containsKey(SheetValue.PartiallyExpanded)) {
            animateTo(SheetValue.PartiallyExpanded, spec)
        } else {
            targetValue = currentValue
        }
    }

    /** Animates sheet to [SheetValue.Hidden]. */
    suspend fun hide(spec: AnimationSpec<Float>? = null) {
        if (!skipHiddenState) {
            animateTo(SheetValue.Hidden, spec)
        }
    }

    /**
     * Updates the dynamic anchors and recalculates resting offset without visual jumps.
     */
    internal fun updateAnchors(
        newAnchors: Map<SheetValue, Float>,
        containerH: Float,
        contentH: Float
    ) {
        val oldAnchors = anchors
        containerHeightPx = containerH
        contentHeightPx = contentH
        anchors = newAnchors
        anchorUpdateSignal.tryEmit(Unit)

        if (!hasOffset || rawOffset.isNaN()) {
            val initialY = newAnchors[initialValue] ?: newAnchors[SheetValue.Hidden] ?: containerH
            rawOffset = initialY
            hasOffset = true
            currentValue = initialValue
            if (targetValue == initialValue) {
                targetValue = initialValue
            }
            return
        }

        // If resting at an anchor whose offset moved due to content resize:
        val currentAnchorY = newAnchors[currentValue]
        if (currentAnchorY != null && !isAnimationRunning) {
            rawOffset = currentAnchorY
        }
    }

    /**
     * Coordinates nested scrolling inside child scrollable components (e.g. LazyColumn).
     *
     * Behavior:
     * - Scrolling up expands the sheet first until fully expanded; then child scrolls content.
     * - Scrolling down scrolls child list to top; once at top, subsequent downward drag collapses or dismisses the sheet.
     */
    val nestedScrollConnection: NestedScrollConnection = object : NestedScrollConnection {
        override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
            val delta = available.y
            val min = minAnchor ?: 0f
            val curr = requireOffset()
            // If dragging upward (delta < 0) and sheet is not yet at Expanded:
            return if (delta < 0f && curr > min + 0.5f) {
                // Consume only up to the minimum (expanded) anchor without overscroll resistance,
                // passing the remaining delta to child scrollables (e.g. LazyColumn).
                val maxConsume = min - curr
                val deltaToConsume = maxOf(delta, maxConsume)
                val consumed = dispatchRawDelta(deltaToConsume)
                Offset(0f, consumed)
            } else {
                Offset.Zero
            }
        }

        override fun onPostScroll(
            consumed: Offset,
            available: Offset,
            source: NestedScrollSource
        ): Offset {
            val delta = available.y
            // Child didn't consume downward scroll (it reached top): sheet collapses/dismisses
            return if (delta > 0f) {
                val sheetConsumed = dispatchRawDelta(delta)
                Offset(0f, sheetConsumed)
            } else {
                Offset.Zero
            }
        }

        override suspend fun onPreFling(available: Velocity): Velocity {
            val vy = available.y
            val min = minAnchor ?: 0f
            val curr = requireOffset()
            return if (vy < 0f && curr > min + 0.5f) {
                settle(vy)
                available
            } else {
                Velocity.Zero
            }
        }

        override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
            val vy = available.y
            return if (vy > 0f) {
                settle(vy)
                available
            } else {
                Velocity.Zero
            }
        }
    }

    companion object {
        fun Saver(
            animationConfig: SheetAnimationConfig,
            confirmValueChange: (SheetValue) -> Boolean,
            skipHiddenState: Boolean,
            skipPartiallyExpanded: Boolean
        ): Saver<BottomSheetState, SheetValue> = Saver(
            save = { it.currentValue },
            restore = { savedValue ->
                BottomSheetState(
                    initialValue = savedValue,
                    animationConfig = animationConfig,
                    confirmValueChange = confirmValueChange,
                    skipHiddenState = skipHiddenState,
                    skipPartiallyExpanded = skipPartiallyExpanded
                )
            }
        )
    }
}

/**
 * Creates and remembers a [BottomSheetState].
 */
@Composable
fun rememberBottomSheetState(
    initialValue: SheetValue = SheetValue.Hidden,
    animationConfig: SheetAnimationConfig = SheetAnimationConfig(),
    confirmValueChange: (SheetValue) -> Boolean = { true },
    skipHiddenState: Boolean = false,
    skipPartiallyExpanded: Boolean = false,
): BottomSheetState {
    val state = rememberSaveable(
        saver = BottomSheetState.Saver(
            animationConfig = animationConfig,
            confirmValueChange = confirmValueChange,
            skipHiddenState = skipHiddenState,
            skipPartiallyExpanded = skipPartiallyExpanded
        )
    ) {
        BottomSheetState(
            initialValue = initialValue,
            animationConfig = animationConfig,
            confirmValueChange = confirmValueChange,
            skipHiddenState = skipHiddenState,
            skipPartiallyExpanded = skipPartiallyExpanded
        )
    }
    state.animationConfig = animationConfig
    return state
}
