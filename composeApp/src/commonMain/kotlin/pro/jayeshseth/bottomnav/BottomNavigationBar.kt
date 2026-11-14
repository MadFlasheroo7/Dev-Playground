/*
 * BottomNavigationBar.kt
 * ─────────────────────────────────────────────────────────────────────────────
 * Compose Multiplatform — commonMain (Android · Desktop · iOS)
 *
 * A passive bottom navigation bar library with a DSL content lambda API.
 * The core components do not run or lock down any animation specs.
 *
 * All animation states (position, scale, dragging status) are hoisted into
 * [BottomNavBarState], allowing developers to drive transitions from their end.
 *
 * Dependencies: Compose Multiplatform (runtime, foundation, material3, ui) only.
 *
 * ┌──────────────────────────────────────────────────────────────────┐
 * │  HOW TO REUSE IN ANOTHER PROJECT                                  │
 * │  1. Copy this single file into your project.                      │
 * │  2. Change the `package` line to match your package.              │
 * │  3. Import and use — zero other setup needed.                     │
 * └──────────────────────────────────────────────────────────────────┘
 */
package pro.jayeshseth.bottomnav

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.withFrameNanos
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.HorizontalAlignmentLine
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Measured
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import kotlinx.coroutines.isActive
import kotlin.math.PI
import kotlin.math.sin

// ─────────────────────────────────────────────────────────────────────────────
// 1. BottomNavBarState & rememberBottomNavBarState
// ─────────────────────────────────────────────────────────────────────────────

/**
 * State holder for bottom navigation bars.
 *
 * This class exposes raw animated states (`position`, `scale`, `leftPosition`, `rightPosition`, and `isDragging`),
 * letting developers customize and execute all transition animations from their end.
 */
@Stable
class BottomNavBarState(initialIndex: Int) {
    private var _selectedIndex by mutableStateOf(initialIndex)

    /** The currently selected tab index. Mutate this to trigger a transition. */
    var selectedIndex: Int
        get() = _selectedIndex
        set(value) {
            _selectedIndex = value
        }

    /**
     * The index-based offset position of the active pill indicator (0.0 to N.0).
     * Animate this property from the screen's end to track the logical target.
     */
    val position = Animatable(initialIndex.toFloat())

    /**
     * The animated left boundary index of the indicator pill (0.0 to N.0).
     * Driven by the client screen to create stretchy or sliding movements.
     */
    val leftPosition = Animatable(initialIndex.toFloat())

    /**
     * The animated right boundary index of the indicator pill (0.0 to N.0).
     * Driven by the client screen to create stretchy or sliding movements.
     */
    val rightPosition = Animatable(initialIndex.toFloat())

    /**
     * The scale multiplier of the active indicator pill.
     * Animate this property from the screen's end.
     */
    val scale = Animatable(1f)

    /**
     * True if the indicator is currently being dragged.
     */
    var isDragging by mutableStateOf(false)
        internal set

    companion object {
        val Saver: Saver<BottomNavBarState, *> = Saver(
            save = { it.selectedIndex },
            restore = { BottomNavBarState(initialIndex = it) }
        )
    }
}

/** Creates and remembers a [BottomNavBarState] that survives activity recreation. */
@Composable
fun rememberBottomNavBarState(initialIndex: Int = 0): BottomNavBarState {
    return rememberSaveable(saver = BottomNavBarState.Saver) {
        BottomNavBarState(initialIndex = initialIndex)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 2. DSL Definition
// ─────────────────────────────────────────────────────────────────────────────

@DslMarker
annotation class BottomNavBarDsl

@Immutable
internal data class BottomNavItemConfig(
    val label:         String,
    val onClick:       () -> Unit,
    val gradientStart: Color,
    val gradientEnd:   Color,
)

@BottomNavBarDsl
class BottomNavBarScope internal constructor() {
    internal val items = mutableListOf<BottomNavItemConfig>()

    /** Renders a single tab item. */
    fun item(
        onClick:       () -> Unit,
        label:         String,
        gradientStart: Color = Color.Unspecified,
        gradientEnd:   Color = Color.Unspecified,
    ) {
        items += BottomNavItemConfig(label, onClick, gradientStart, gradientEnd)
    }
}

internal object BottomNavRowScope : RowScope {
    override fun Modifier.weight(weight: Float, fill: Boolean): Modifier = this
    override fun Modifier.align(alignment: Alignment.Vertical): Modifier = this
    override fun Modifier.alignBy(alignmentLine: HorizontalAlignmentLine): Modifier = this
    override fun Modifier.alignBy(alignmentLineBlock: (Measured) -> Int): Modifier = this
    override fun Modifier.alignByBaseline(): Modifier = this
}

// ─────────────────────────────────────────────────────────────────────────────
// 3. Defaults
// ─────────────────────────────────────────────────────────────────────────────

object BottomNavigationBarDefaults {
    val palette: List<Pair<Color, Color>> = listOf(
        Color(0xFFF27121) to Color(0xFFE94057),
        Color(0xFF00F2FE) to Color(0xFF4FACFE),
        Color(0xFFFF0844) to Color(0xFFFFB199),
        Color(0xFFF5AF19) to Color(0xFFE100FF),
        Color(0xFF22FFAA) to Color(0xFF4C6FFF),
        Color(0xFFFF5C8A) to Color(0xFFFFC857),
        Color(0xFFAB47BC) to Color(0xFF4C6FFF),
        Color(0xFF22D3A6) to Color(0xFF4C6FFF),
    )

    const val WobbleAmplitude: Float = 0.035f
    val CornerRadius: Dp = 28.dp
    val Height: Dp = 56.dp
}

// ─────────────────────────────────────────────────────────────────────────────
// 4. BottomNavPill & Indicator Scope
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Scope provided to indicator composables rendered inside [BottomNavigationBar],
 * [NavigationBarLayout], [SegmentedButtonRow], and [SegmentedButtonLayout].
 */
@Immutable
class BottomNavIndicatorScope(
    val scale: Float,
    val gradientStart: Color,
    val gradientEnd: Color,
    val cornerRadius: Dp,
    val isDragging: Boolean,
    val modifier: Modifier = Modifier
)

/**
 * Standard background pill component for [BottomNavigationBar] and layout variants.
 * Can be plugged into custom `indicator` slot lambdas or used standalone.
 */
@Composable
fun BottomNavPill(
    scope: BottomNavIndicatorScope,
    modifier: Modifier = Modifier,
) {
    BottomNavPill(
        scale = scope.scale,
        gradientStart = scope.gradientStart,
        gradientEnd = scope.gradientEnd,
        cornerRadius = scope.cornerRadius,
        isDragging = scope.isDragging,
        modifier = scope.modifier.then(modifier)
    )
}

@Composable
fun BottomNavPill(
    scale:         Float,
    gradientStart: Color,
    gradientEnd:   Color,
    cornerRadius:  Dp,
    isDragging:    Boolean,
    modifier:      Modifier = Modifier,
) {
    val pillShape = RoundedCornerShape((cornerRadius.value - 4f).coerceAtLeast(0f).dp)
    Box(
        modifier
            .padding(4.dp)
            .scale(scale.coerceIn(0.5f, 1.5f))
            .shadow(elevation = if (isDragging) 12.dp else 4.dp, shape = pillShape)
            .background(Brush.horizontalGradient(listOf(gradientStart, gradientEnd)), pillShape)
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// 5. BottomNavigationBar  — Passive BoxWithConstraints component
// ─────────────────────────────────────────────────────────────────────────────

/**
 * A passive bottom navigation bar that reads layout/animation values directly from [state].
 * All slide/scale/stretch transitions are animated by the developer from their screen.
 */
@Composable
fun BottomNavigationBar(
    state:              BottomNavBarState,
    modifier:           Modifier = Modifier,
    wobbleAmplitude:    Float    = BottomNavigationBarDefaults.WobbleAmplitude,
    cornerRadius:       Dp       = BottomNavigationBarDefaults.CornerRadius,
    height:             Dp       = BottomNavigationBarDefaults.Height,
    trackColor:         Color    = Color.Unspecified,
    instantDragMode:    Boolean  = false,
    isScrollable:       Boolean  = false,
    minItemWidth:       Dp       = 90.dp,
    indicator:          @Composable (BottomNavIndicatorScope) -> Unit = { BottomNavPill(it) },
    content:            BottomNavBarScope.() -> Unit,
) {
    val recompositionCounter = remember { RecompositionCounter() }
    recompositionCounter.count++
    println("[BottomNavigationBar] Recomposed count: ${recompositionCounter.count}")

    val scope = BottomNavBarScope()
    scope.content()
    val items = scope.items

    require(items.isNotEmpty()) {
        "BottomNavigationBar requires at least one item() call inside the content block."
    }

    val selectedIndex = state.selectedIndex.coerceIn(items.indices)
    val latestItems         by rememberUpdatedState(items)
    val latestSelectedIndex by rememberUpdatedState(selectedIndex)

    val haptic         = LocalHapticFeedback.current
    val density        = LocalDensity.current
    val coroutineScope = rememberCoroutineScope()

    val resolvedTrackColor =
        if (trackColor == Color.Unspecified) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        else trackColor

    val (targetStart, targetEnd) = run {
        val item    = items[selectedIndex]
        val palette = BottomNavigationBarDefaults.palette[selectedIndex % BottomNavigationBarDefaults.palette.size]
        val start   = if (item.gradientStart == Color.Unspecified) palette.first  else item.gradientStart
        val end     = if (item.gradientEnd   == Color.Unspecified) palette.second else item.gradientEnd
        start to end
    }
    val startColor by animateColorAsState(targetStart, spring(stiffness = Spring.StiffnessLow), label = "pillGradStart")
    val endColor   by animateColorAsState(targetEnd,   spring(stiffness = Spring.StiffnessLow), label = "pillGradEnd")

    val scrollState = rememberScrollState()
    var latestViewportPointerX by remember { mutableFloatStateOf(-1f) }

    LaunchedEffect(selectedIndex, isScrollable) {
        if (isScrollable) {
            val minSegPx = with(density) { minItemWidth.toPx() }
            val targetPx = (selectedIndex * minSegPx).toInt()
            scrollState.animateScrollTo(targetPx)
        }
    }

    LaunchedEffect(state.isDragging, isScrollable) {
        if (state.isDragging && isScrollable) {
            val edgeZonePx = with(density) { 56.dp.toPx() }
            val minSegPx = with(density) { minItemWidth.toPx() }
            val totalWidthPx = minSegPx * items.size
            while (isActive && state.isDragging) {
                val ptrX = latestViewportPointerX
                if (ptrX >= 0f) {
                    val maxScroll = scrollState.maxValue.toFloat()
                    val currentScroll = scrollState.value.toFloat()
                    val viewportWidthPx = (totalWidthPx - maxScroll).coerceAtLeast(1f)

                    if (ptrX < edgeZonePx && currentScroll > 0f) {
                        val factor = ((edgeZonePx - ptrX) / edgeZonePx).coerceIn(0.1f, 1f)
                        val deltaScroll = -16f * factor * (density.density)
                        scrollState.scrollBy(deltaScroll)

                        val currentPillPx = state.position.value * minSegPx + deltaScroll
                        val newIndex = (currentPillPx / minSegPx).coerceIn(0f, (items.size - 1).toFloat())
                        state.position.snapTo(newIndex)
                        state.leftPosition.snapTo(newIndex)
                        state.rightPosition.snapTo(newIndex)

                        val crossedIdx = (newIndex + 0.5f).toInt().coerceIn(0, latestItems.size - 1)
                        if (crossedIdx != latestSelectedIndex) {
                            latestItems[crossedIdx].onClick()
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        }
                    } else if (ptrX > (viewportWidthPx - edgeZonePx) && currentScroll < maxScroll) {
                        val factor = ((ptrX - (viewportWidthPx - edgeZonePx)) / edgeZonePx).coerceIn(0.1f, 1f)
                        val deltaScroll = 16f * factor * (density.density)
                        scrollState.scrollBy(deltaScroll)

                        val currentPillPx = state.position.value * minSegPx + deltaScroll
                        val newIndex = (currentPillPx / minSegPx).coerceIn(0f, (items.size - 1).toFloat())
                        state.position.snapTo(newIndex)
                        state.leftPosition.snapTo(newIndex)
                        state.rightPosition.snapTo(newIndex)

                        val crossedIdx = (newIndex + 0.5f).toInt().coerceIn(0, latestItems.size - 1)
                        if (crossedIdx != latestSelectedIndex) {
                            latestItems[crossedIdx].onClick()
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        }
                    }
                }
                withFrameNanos { }
            }
        } else {
            latestViewportPointerX = -1f
        }
    }

    val trackShape = RoundedCornerShape(cornerRadius)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .background(resolvedTrackColor, trackShape)
            .clip(trackShape)
    ) {
        val contentModifier = if (isScrollable) {
            Modifier.fillMaxSize().horizontalScroll(scrollState)
        } else {
            Modifier.fillMaxSize()
        }

        Box(contentModifier) {
            val innerModifier = if (isScrollable) {
                Modifier.width(minItemWidth * items.size).fillMaxHeight()
            } else {
                Modifier.fillMaxSize()
            }

            BoxWithConstraints(innerModifier) {
                val widthPx = with(density) { maxWidth.toPx() }
                val segWPx  = widthPx / items.size

            Box(
                Modifier
                    .fillMaxSize()
                    .pointerInput(items.size, instantDragMode) {
                        val onStart = { offset: Offset ->
                            val hit = (offset.x / segWPx).toInt().coerceIn(0, latestItems.size - 1)
                            if (hit == latestSelectedIndex) {
                                state.isDragging = true
                                latestViewportPointerX = offset.x - scrollState.value
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            }
                        }

                        val onDrag = { change: PointerInputChange, delta: Offset ->
                            change.consume()
                            if (state.isDragging) {
                                latestViewportPointerX = change.position.x - scrollState.value
                                coroutineScope.launch {
                                    val newOffset = (state.position.value * segWPx + delta.x)
                                        .coerceIn(0f, widthPx - segWPx)
                                    val newIndex = newOffset / segWPx
                                    state.position.snapTo(newIndex)
                                    val crossedIdx = (newIndex + 0.5f).toInt().coerceIn(0, latestItems.size - 1)
                                    if (crossedIdx != latestSelectedIndex) {
                                        latestItems[crossedIdx].onClick()
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    }
                                }
                            }
                        }

                        val onEnd = {
                            latestViewportPointerX = -1f
                            if (state.isDragging) {
                                state.isDragging = false
                                val committed = (state.position.value + 0.5f).toInt().coerceIn(0, latestItems.size - 1)
                                latestItems[committed].onClick()
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            }
                        }

                        val onCancel = {
                            latestViewportPointerX = -1f
                            if (state.isDragging) {
                                state.isDragging = false
                            }
                        }

                        if (instantDragMode) {
                            detectDragGestures(
                                onDragStart  = onStart,
                                onDrag       = onDrag,
                                onDragEnd    = onEnd,
                                onDragCancel = onCancel,
                            )
                        } else {
                            detectDragGesturesAfterLongPress(
                                onDragStart  = onStart,
                                onDrag       = onDrag,
                                onDragEnd    = onEnd,
                                onDragCancel = onCancel,
                            )
                        }
                    }
            ) {
                val left = state.leftPosition.value
                val right = state.rightPosition.value

                val pillOffsetDp = with(density) { (left * segWPx).toDp() }
                val pillWidthDp = with(density) { ((right - left + 1f) * segWPx).toDp() }

                val progress = (left + right) / 2f
                val wobble = if (state.isDragging || state.position.isRunning)
                    sin(progress * PI * 2.0).toFloat() * wobbleAmplitude
                else 0f

                indicator(
                    BottomNavIndicatorScope(
                        scale         = state.scale.value + wobble,
                        gradientStart = startColor,
                        gradientEnd   = endColor,
                        cornerRadius  = cornerRadius,
                        isDragging    = state.isDragging,
                        modifier      = Modifier
                            .offset(x = pillOffsetDp)
                            .width(pillWidthDp)
                            .fillMaxSize(),
                    )
                )

                Row(Modifier.fillMaxSize()) {
                    items.forEachIndexed { index, item ->
                        val isSelected = index == selectedIndex
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxSize()
                                .clickable(enabled = !state.isDragging) {
                                    item.onClick()
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text       = item.label,
                                color      = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                fontSize   = 15.sp,
                                modifier   = Modifier.graphicsLayer {
                                    val s = if (isSelected) 1.1f else 1.0f
                                    scaleX = s; scaleY = s
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}
}

// ─────────────────────────────────────────────────────────────────────────────
// 6. NavigationBarLayout  — Passive Custom Layout Component
// ─────────────────────────────────────────────────────────────────────────────

/**
 * A passive bottom navigation bar that uses custom layout positioning.
 * Measures and places items in a single pass without using subcomposition.
 */
@Composable
fun NavigationBarLayout(
    state:              BottomNavBarState,
    modifier:           Modifier = Modifier,
    wobbleAmplitude:    Float    = BottomNavigationBarDefaults.WobbleAmplitude,
    cornerRadius:       Dp       = BottomNavigationBarDefaults.CornerRadius,
    height:             Dp       = BottomNavigationBarDefaults.Height,
    trackColor:         Color    = Color.Unspecified,
    instantDragMode:    Boolean  = false,
    isScrollable:       Boolean  = false,
    minItemWidth:       Dp       = 90.dp,
    indicator:          @Composable (BottomNavIndicatorScope) -> Unit = { BottomNavPill(it) },
    content:            BottomNavBarScope.() -> Unit,
) {
    val recompositionCounter = remember { RecompositionCounter() }
    recompositionCounter.count++
    println("[NavigationBarLayout] Recomposed count: ${recompositionCounter.count}")

    val scope = BottomNavBarScope()
    scope.content()
    val items = scope.items

    require(items.isNotEmpty()) {
        "NavigationBarLayout requires at least one item() call inside the content block."
    }

    val selectedIndex = state.selectedIndex.coerceIn(items.indices)
    val latestItems         by rememberUpdatedState(items)
    val latestSelectedIndex by rememberUpdatedState(selectedIndex)

    val density        = LocalDensity.current
    val haptic         = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()

    val resolvedTrackColor =
        if (trackColor == Color.Unspecified) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        else trackColor

    val (targetStart, targetEnd) = run {
        val item    = items[selectedIndex]
        val palette = BottomNavigationBarDefaults.palette[selectedIndex % BottomNavigationBarDefaults.palette.size]
        val start   = if (item.gradientStart == Color.Unspecified) palette.first  else item.gradientStart
        val end     = if (item.gradientEnd   == Color.Unspecified) palette.second else item.gradientEnd
        start to end
    }
    val startColor by animateColorAsState(targetStart, spring(stiffness = Spring.StiffnessLow), label = "layoutPillGradStart")
    val endColor   by animateColorAsState(targetEnd,   spring(stiffness = Spring.StiffnessLow), label = "layoutPillGradEnd")

    val scrollState = rememberScrollState()
    var latestViewportPointerX by remember { mutableFloatStateOf(-1f) }

    LaunchedEffect(selectedIndex, isScrollable) {
        if (isScrollable) {
            val minSegPx = with(density) { minItemWidth.toPx() }
            val targetPx = (selectedIndex * minSegPx).toInt()
            scrollState.animateScrollTo(targetPx)
        }
    }

    LaunchedEffect(state.isDragging, isScrollable) {
        if (state.isDragging && isScrollable) {
            val edgeZonePx = with(density) { 56.dp.toPx() }
            val minSegPx = with(density) { minItemWidth.toPx() }
            val totalWidthPx = minSegPx * items.size
            while (isActive && state.isDragging) {
                val ptrX = latestViewportPointerX
                if (ptrX >= 0f) {
                    val maxScroll = scrollState.maxValue.toFloat()
                    val currentScroll = scrollState.value.toFloat()
                    val viewportWidthPx = (totalWidthPx - maxScroll).coerceAtLeast(1f)

                    if (ptrX < edgeZonePx && currentScroll > 0f) {
                        val factor = ((edgeZonePx - ptrX) / edgeZonePx).coerceIn(0.1f, 1f)
                        val deltaScroll = -16f * factor * (density.density)
                        scrollState.scrollBy(deltaScroll)

                        val currentPillPx = state.position.value * minSegPx + deltaScroll
                        val newIndex = (currentPillPx / minSegPx).coerceIn(0f, (items.size - 1).toFloat())
                        state.position.snapTo(newIndex)
                        state.leftPosition.snapTo(newIndex)
                        state.rightPosition.snapTo(newIndex)

                        val crossedIdx = (newIndex + 0.5f).toInt().coerceIn(0, latestItems.size - 1)
                        if (crossedIdx != latestSelectedIndex) {
                            latestItems[crossedIdx].onClick()
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        }
                    } else if (ptrX > (viewportWidthPx - edgeZonePx) && currentScroll < maxScroll) {
                        val factor = ((ptrX - (viewportWidthPx - edgeZonePx)) / edgeZonePx).coerceIn(0.1f, 1f)
                        val deltaScroll = 16f * factor * (density.density)
                        scrollState.scrollBy(deltaScroll)

                        val currentPillPx = state.position.value * minSegPx + deltaScroll
                        val newIndex = (currentPillPx / minSegPx).coerceIn(0f, (items.size - 1).toFloat())
                        state.position.snapTo(newIndex)
                        state.leftPosition.snapTo(newIndex)
                        state.rightPosition.snapTo(newIndex)

                        val crossedIdx = (newIndex + 0.5f).toInt().coerceIn(0, latestItems.size - 1)
                        if (crossedIdx != latestSelectedIndex) {
                            latestItems[crossedIdx].onClick()
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        }
                    }
                }
                withFrameNanos { }
            }
        } else {
            latestViewportPointerX = -1f
        }
    }

    var segmentWidthPx by remember { mutableStateOf(0f) }

    val gestureModifier = Modifier.pointerInput(items.size, instantDragMode) {
        val onStart = { offset: Offset ->
            if (segmentWidthPx > 0f) {
                val hit = (offset.x / segmentWidthPx).toInt().coerceIn(0, latestItems.size - 1)
                if (hit == latestSelectedIndex) {
                    state.isDragging = true
                    latestViewportPointerX = offset.x - scrollState.value
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                }
            }
        }

        val onDrag = { change: PointerInputChange, delta: Offset ->
            change.consume()
            if (state.isDragging && segmentWidthPx > 0f) {
                latestViewportPointerX = change.position.x - scrollState.value
                coroutineScope.launch {
                    val newIndex = (state.position.value + delta.x / segmentWidthPx)
                        .coerceIn(0f, (latestItems.size - 1).toFloat())
                    state.position.snapTo(newIndex)
                    val crossedIdx = (newIndex + 0.5f).toInt().coerceIn(0, latestItems.size - 1)
                    if (crossedIdx != latestSelectedIndex) {
                        latestItems[crossedIdx].onClick()
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    }
                }
            }
        }

        val onEnd = {
            latestViewportPointerX = -1f
            if (state.isDragging) {
                state.isDragging = false
                val committed = (state.position.value + 0.5f).toInt().coerceIn(0, latestItems.size - 1)
                latestItems[committed].onClick()
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            }
        }

        val onCancel = {
            latestViewportPointerX = -1f
            if (state.isDragging) {
                state.isDragging = false
            }
        }

        if (instantDragMode) {
            detectDragGestures(
                onDragStart  = onStart,
                onDrag       = onDrag,
                onDragEnd    = onEnd,
                onDragCancel = onCancel,
            )
        } else {
            detectDragGesturesAfterLongPress(
                onDragStart  = onStart,
                onDrag       = onDrag,
                onDragEnd    = onEnd,
                onDragCancel = onCancel,
            )
        }
    }

    val trackShape = RoundedCornerShape(cornerRadius)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .background(resolvedTrackColor, trackShape)
            .clip(trackShape)
    ) {
        val scrollModifier = if (isScrollable) {
            Modifier.fillMaxSize().horizontalScroll(scrollState)
        } else {
            Modifier.fillMaxSize()
        }

        Box(scrollModifier) {
            val layoutModifier = if (isScrollable) {
                Modifier.width(minItemWidth * items.size).fillMaxHeight()
            } else {
                Modifier.fillMaxSize()
            }

            Layout(
                modifier = layoutModifier.then(gestureModifier),
                content = {
                    val left = state.leftPosition.value
                    val right = state.rightPosition.value
                    val progress = (left + right) / 2f
                    val wobble = if (state.isDragging || state.position.isRunning)
                        sin(progress * PI * 2.0).toFloat() * wobbleAmplitude
                    else 0f

                    indicator(
                        BottomNavIndicatorScope(
                            scale         = state.scale.value + wobble,
                            gradientStart = startColor,
                            gradientEnd   = endColor,
                            cornerRadius  = cornerRadius,
                            isDragging    = state.isDragging,
                            modifier      = Modifier.fillMaxSize(),
                        )
                    )

                    items.forEachIndexed { index, item ->
                        val isSelected = index == selectedIndex
                        Box(
                            modifier = Modifier
                                .clickable(enabled = !state.isDragging) {
                                    item.onClick()
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text       = item.label,
                                color      = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                fontSize   = 15.sp,
                                modifier   = Modifier.graphicsLayer {
                                    val s = if (isSelected) 1.1f else 1.0f
                                    scaleX = s; scaleY = s
                                },
                            )
                        }
                    }
                }
            ) { measurables, constraints ->
                val totalWidth = constraints.maxWidth
                val totalHeight = constraints.maxHeight

                val itemCount = items.size
                val segmentWidth = if (isScrollable) {
                    with(density) { minItemWidth.toPx() }.toInt().coerceAtLeast(1)
                } else {
                    totalWidth / itemCount
                }
                segmentWidthPx = segmentWidth.toFloat()

                val left = state.leftPosition.value
                val right = state.rightPosition.value
                val dynamicPillWidth = ((right - left + 1f) * segmentWidth).toInt()

                val pillMeasurable = measurables[0]
                val pillPlaceable = pillMeasurable.measure(
                    constraints.copy(
                        minWidth  = dynamicPillWidth,
                        maxWidth  = dynamicPillWidth,
                        minHeight = totalHeight,
                        maxHeight = totalHeight
                    )
                )

                val itemPlaceables = (1..itemCount).map { i ->
                    measurables[i].measure(
                        constraints.copy(
                            minWidth  = segmentWidth,
                            maxWidth  = segmentWidth,
                            minHeight = totalHeight,
                            maxHeight = totalHeight
                        )
                    )
                }

                layout(totalWidth, totalHeight) {
                    val pillX = (left * segmentWidth).toInt()
                    pillPlaceable.placeRelative(pillX, 0)

                    itemPlaceables.forEachIndexed { index, placeable ->
                        placeable.placeRelative(index * segmentWidth, 0)
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 7. Variant 3: SegmentedButtonRow (takes @Composable RowScope.() -> Unit content)
// ─────────────────────────────────────────────────────────────────────────────

/**
 * A 3rd variant of the segmented control / navigation bar component that accepts direct
 * `@Composable RowScope.() -> Unit` content instead of a custom DSL scope.
 *
 * Items placed inside the RowScope (e.g. `Box(Modifier.weight(1f))`) are laid out
 * in standard Row layout while the background stretchy/sliding indicator pill, spring/tween
 * physics, wobble jiggle effects, scale animations, and drag gestures function with 100% parity.
 */
@Composable
fun SegmentedButtonRow(
    state:               BottomNavBarState,
    modifier:            Modifier = Modifier,
    itemCount:           Int      = 4,
    wobbleAmplitude:     Float    = BottomNavigationBarDefaults.WobbleAmplitude,
    cornerRadius:        Dp       = BottomNavigationBarDefaults.CornerRadius,
    height:              Dp       = BottomNavigationBarDefaults.Height,
    trackColor:          Color    = Color.Unspecified,
    activeGradientStart: Color    = Color.Unspecified,
    activeGradientEnd:   Color    = Color.Unspecified,
    instantDragMode:     Boolean  = false,
    isScrollable:        Boolean  = false,
    minItemWidth:        Dp       = 90.dp,
    onItemSelected:      ((index: Int) -> Unit)? = null,
    indicator:           @Composable (BottomNavIndicatorScope) -> Unit = { BottomNavPill(it) },
    content:             @Composable RowScope.() -> Unit,
) {
    val recompositionCounter = remember { RecompositionCounter() }
    recompositionCounter.count++
    println("[SegmentedButtonRow] Recomposed count: ${recompositionCounter.count}")

    val selectedIndex = state.selectedIndex.coerceIn(0 until itemCount)
    val latestSelectedIndex  by rememberUpdatedState(selectedIndex)
    val latestOnItemSelected by rememberUpdatedState(onItemSelected)

    val haptic         = LocalHapticFeedback.current
    val density        = LocalDensity.current
    val coroutineScope = rememberCoroutineScope()

    val resolvedTrackColor =
        if (trackColor == Color.Unspecified) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        else trackColor

    val (targetStart, targetEnd) = run {
        val palette = BottomNavigationBarDefaults.palette[selectedIndex % BottomNavigationBarDefaults.palette.size]
        val start   = if (activeGradientStart == Color.Unspecified) palette.first  else activeGradientStart
        val end     = if (activeGradientEnd   == Color.Unspecified) palette.second else activeGradientEnd
        start to end
    }
    val startColor by animateColorAsState(targetStart, spring(stiffness = Spring.StiffnessLow), label = "rowPillGradStart")
    val endColor   by animateColorAsState(targetEnd,   spring(stiffness = Spring.StiffnessLow), label = "rowPillGradEnd")

    val scrollState = rememberScrollState()
    var latestViewportPointerX by remember { mutableFloatStateOf(-1f) }

    LaunchedEffect(selectedIndex, isScrollable) {
        if (isScrollable) {
            val minSegPx = with(density) { minItemWidth.toPx() }
            val targetPx = (selectedIndex * minSegPx).toInt()
            scrollState.animateScrollTo(targetPx)
        }
    }

    LaunchedEffect(state.isDragging, isScrollable) {
        if (state.isDragging && isScrollable) {
            val edgeZonePx = with(density) { 56.dp.toPx() }
            val minSegPx = with(density) { minItemWidth.toPx() }
            val totalWidthPx = minSegPx * itemCount
            while (isActive && state.isDragging) {
                val ptrX = latestViewportPointerX
                if (ptrX >= 0f) {
                    val maxScroll = scrollState.maxValue.toFloat()
                    val currentScroll = scrollState.value.toFloat()
                    val viewportWidthPx = (totalWidthPx - maxScroll).coerceAtLeast(1f)

                    if (ptrX < edgeZonePx && currentScroll > 0f) {
                        val factor = ((edgeZonePx - ptrX) / edgeZonePx).coerceIn(0.1f, 1f)
                        val deltaScroll = -16f * factor * (density.density)
                        scrollState.scrollBy(deltaScroll)

                        val currentPillPx = state.position.value * minSegPx + deltaScroll
                        val newPillPx = currentPillPx.coerceIn(0f, (itemCount - 1) * minSegPx)
                        val newIndex = newPillPx / minSegPx
                        state.position.snapTo(newIndex)
                        state.leftPosition.snapTo(newIndex)
                        state.rightPosition.snapTo(newIndex)

                        val crossedIdx = (newIndex + 0.5f).toInt().coerceIn(0, itemCount - 1)
                        if (crossedIdx != latestSelectedIndex) {
                            state.selectedIndex = crossedIdx
                            latestOnItemSelected?.invoke(crossedIdx)
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        }
                    } else if (ptrX > (viewportWidthPx - edgeZonePx) && currentScroll < maxScroll) {
                        val factor = ((ptrX - (viewportWidthPx - edgeZonePx)) / edgeZonePx).coerceIn(0.1f, 1f)
                        val deltaScroll = 16f * factor * (density.density)
                        scrollState.scrollBy(deltaScroll)

                        val currentPillPx = state.position.value * minSegPx + deltaScroll
                        val newPillPx = currentPillPx.coerceIn(0f, (itemCount - 1) * minSegPx)
                        val newIndex = newPillPx / minSegPx
                        state.position.snapTo(newIndex)
                        state.leftPosition.snapTo(newIndex)
                        state.rightPosition.snapTo(newIndex)

                        val crossedIdx = (newIndex + 0.5f).toInt().coerceIn(0, itemCount - 1)
                        if (crossedIdx != latestSelectedIndex) {
                            state.selectedIndex = crossedIdx
                            latestOnItemSelected?.invoke(crossedIdx)
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        }
                    }
                }
                withFrameNanos { }
            }
        } else {
            latestViewportPointerX = -1f
        }
    }

    val trackShape = RoundedCornerShape(cornerRadius)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .background(resolvedTrackColor, trackShape)
            .clip(trackShape)
    ) {
        val scrollModifier = if (isScrollable) {
            Modifier.fillMaxSize().horizontalScroll(scrollState)
        } else {
            Modifier.fillMaxSize()
        }

        Box(scrollModifier) {
            val innerModifier = if (isScrollable) {
                Modifier.width(minItemWidth * itemCount).fillMaxHeight()
            } else {
                Modifier.fillMaxSize()
            }

            BoxWithConstraints(innerModifier) {
                val widthPx = with(density) { maxWidth.toPx() }
                val segWPx  = widthPx / itemCount.coerceAtLeast(1)

            Box(
                Modifier
                    .fillMaxSize()
                    .pointerInput(itemCount, instantDragMode) {
                        val onStart = { offset: Offset ->
                            val hit = (offset.x / segWPx).toInt().coerceIn(0, itemCount - 1)
                            if (hit == latestSelectedIndex) {
                                state.isDragging = true
                                latestViewportPointerX = offset.x - scrollState.value
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            }
                        }

                        val onDrag = { change: PointerInputChange, delta: Offset ->
                            change.consume()
                            if (state.isDragging) {
                                latestViewportPointerX = change.position.x - scrollState.value
                                coroutineScope.launch {
                                    val newOffset = (state.position.value * segWPx + delta.x)
                                        .coerceIn(0f, widthPx - segWPx)
                                    val newIndex = newOffset / segWPx
                                    state.position.snapTo(newIndex)
                                    val crossedIdx = (newIndex + 0.5f).toInt().coerceIn(0, itemCount - 1)
                                    if (crossedIdx != latestSelectedIndex) {
                                        state.selectedIndex = crossedIdx
                                        latestOnItemSelected?.invoke(crossedIdx)
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    }
                                }
                            }
                        }

                        val onEnd = {
                            latestViewportPointerX = -1f
                            if (state.isDragging) {
                                state.isDragging = false
                                val committed = (state.position.value + 0.5f).toInt().coerceIn(0, itemCount - 1)
                                state.selectedIndex = committed
                                latestOnItemSelected?.invoke(committed)
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            }
                        }

                        val onCancel = {
                            latestViewportPointerX = -1f
                            if (state.isDragging) {
                                state.isDragging = false
                            }
                        }

                        if (instantDragMode) {
                            detectDragGestures(
                                onDragStart  = onStart,
                                onDrag       = onDrag,
                                onDragEnd    = onEnd,
                                onDragCancel = onCancel,
                            )
                        } else {
                            detectDragGesturesAfterLongPress(
                                onDragStart  = onStart,
                                onDrag       = onDrag,
                                onDragEnd    = onEnd,
                                onDragCancel = onCancel,
                            )
                        }
                    }
            ) {
                val left = state.leftPosition.value
                val right = state.rightPosition.value

                val pillOffsetDp = with(density) { (left * segWPx).toDp() }
                val pillWidthDp = with(density) { ((right - left + 1f) * segWPx).toDp() }

                val progress = (left + right) / 2f
                val wobble = if (state.isDragging || state.position.isRunning)
                    sin(progress * PI * 2.0).toFloat() * wobbleAmplitude
                else 0f

                indicator(
                    BottomNavIndicatorScope(
                        scale         = state.scale.value + wobble,
                        gradientStart = startColor,
                        gradientEnd   = endColor,
                        cornerRadius  = cornerRadius,
                        isDragging    = state.isDragging,
                        modifier      = Modifier
                            .offset(x = pillOffsetDp)
                            .width(pillWidthDp)
                            .fillMaxSize(),
                    )
                )

                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    content = content
                )
            }
        }
    }
}
}

// ─────────────────────────────────────────────────────────────────────────────
// 8. Variant 4: SegmentedButtonLayout (built using custom Layout with @Composable RowScope.() -> Unit)
// ─────────────────────────────────────────────────────────────────────────────

/**
 * A 4th variant of the segmented navigation bar component that combines the custom [Layout] engine
 * (measuring and placing background indicator pill and items directly) with arbitrary
 * `@Composable RowScope.() -> Unit` content.
 *
 * Items passed inside the `content` block are measured and placed dynamically using Compose
 * [Layout] measure policy while spring physics, stretchy pill calculations, wobble jiggle,
 * drag gestures, haptic feedback, and edge-scrolling operate with 100% feature parity.
 */
@Composable
fun SegmentedButtonLayout(
    state:               BottomNavBarState,
    modifier:            Modifier = Modifier,
    itemCount:           Int      = 4,
    wobbleAmplitude:     Float    = BottomNavigationBarDefaults.WobbleAmplitude,
    cornerRadius:        Dp       = BottomNavigationBarDefaults.CornerRadius,
    height:              Dp       = BottomNavigationBarDefaults.Height,
    trackColor:          Color    = Color.Unspecified,
    activeGradientStart: Color    = Color.Unspecified,
    activeGradientEnd:   Color    = Color.Unspecified,
    instantDragMode:     Boolean  = false,
    isScrollable:        Boolean  = false,
    minItemWidth:        Dp       = 90.dp,
    onItemSelected:      ((index: Int) -> Unit)? = null,
    indicator:           @Composable (BottomNavIndicatorScope) -> Unit = { BottomNavPill(it) },
    content:             @Composable RowScope.() -> Unit,
) {
    val recompositionCounter = remember { RecompositionCounter() }
    recompositionCounter.count++
    println("[SegmentedButtonLayout] Recomposed count: ${recompositionCounter.count}")

    val selectedIndex = state.selectedIndex.coerceIn(0 until itemCount)
    val latestSelectedIndex  by rememberUpdatedState(selectedIndex)
    val latestOnItemSelected by rememberUpdatedState(onItemSelected)

    val haptic         = LocalHapticFeedback.current
    val density        = LocalDensity.current
    val coroutineScope = rememberCoroutineScope()

    val resolvedTrackColor =
        if (trackColor == Color.Unspecified) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        else trackColor

    val (targetStart, targetEnd) = run {
        val palette = BottomNavigationBarDefaults.palette[selectedIndex % BottomNavigationBarDefaults.palette.size]
        val start   = if (activeGradientStart == Color.Unspecified) palette.first  else activeGradientStart
        val end     = if (activeGradientEnd   == Color.Unspecified) palette.second else activeGradientEnd
        start to end
    }
    val startColor by animateColorAsState(targetStart, spring(stiffness = Spring.StiffnessLow), label = "layoutRowPillGradStart")
    val endColor   by animateColorAsState(targetEnd,   spring(stiffness = Spring.StiffnessLow), label = "layoutRowPillGradEnd")

    val scrollState = rememberScrollState()
    var latestViewportPointerX by remember { mutableFloatStateOf(-1f) }

    LaunchedEffect(selectedIndex, isScrollable) {
        if (isScrollable) {
            val minSegPx = with(density) { minItemWidth.toPx() }
            val targetPx = (selectedIndex * minSegPx).toInt()
            scrollState.animateScrollTo(targetPx)
        }
    }

    LaunchedEffect(state.isDragging, isScrollable) {
        if (state.isDragging && isScrollable) {
            val edgeZonePx = with(density) { 56.dp.toPx() }
            val minSegPx = with(density) { minItemWidth.toPx() }
            val totalWidthPx = minSegPx * itemCount
            while (isActive && state.isDragging) {
                val ptrX = latestViewportPointerX
                if (ptrX >= 0f) {
                    val maxScroll = scrollState.maxValue.toFloat()
                    val currentScroll = scrollState.value.toFloat()
                    val viewportWidthPx = (totalWidthPx - maxScroll).coerceAtLeast(1f)

                    if (ptrX < edgeZonePx && currentScroll > 0f) {
                        val factor = ((edgeZonePx - ptrX) / edgeZonePx).coerceIn(0.1f, 1f)
                        val deltaScroll = -16f * factor * (density.density)
                        scrollState.scrollBy(deltaScroll)

                        val currentPillPx = state.position.value * minSegPx + deltaScroll
                        val newPillPx = currentPillPx.coerceIn(0f, (itemCount - 1) * minSegPx)
                        val newIndex = newPillPx / minSegPx
                        state.position.snapTo(newIndex)
                        state.leftPosition.snapTo(newIndex)
                        state.rightPosition.snapTo(newIndex)

                        val crossedIdx = (newIndex + 0.5f).toInt().coerceIn(0, itemCount - 1)
                        if (crossedIdx != latestSelectedIndex) {
                            state.selectedIndex = crossedIdx
                            latestOnItemSelected?.invoke(crossedIdx)
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        }
                    } else if (ptrX > (viewportWidthPx - edgeZonePx) && currentScroll < maxScroll) {
                        val factor = ((ptrX - (viewportWidthPx - edgeZonePx)) / edgeZonePx).coerceIn(0.1f, 1f)
                        val deltaScroll = 16f * factor * (density.density)
                        scrollState.scrollBy(deltaScroll)

                        val currentPillPx = state.position.value * minSegPx + deltaScroll
                        val newPillPx = currentPillPx.coerceIn(0f, (itemCount - 1) * minSegPx)
                        val newIndex = newPillPx / minSegPx
                        state.position.snapTo(newIndex)
                        state.leftPosition.snapTo(newIndex)
                        state.rightPosition.snapTo(newIndex)

                        val crossedIdx = (newIndex + 0.5f).toInt().coerceIn(0, itemCount - 1)
                        if (crossedIdx != latestSelectedIndex) {
                            state.selectedIndex = crossedIdx
                            latestOnItemSelected?.invoke(crossedIdx)
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        }
                    }
                }
                withFrameNanos { }
            }
        } else {
            latestViewportPointerX = -1f
        }
    }

    var segmentWidthPx by remember { mutableFloatStateOf(0f) }

    val trackShape = RoundedCornerShape(cornerRadius)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .background(resolvedTrackColor, trackShape)
            .clip(trackShape)
    ) {
        val scrollModifier = if (isScrollable) {
            Modifier.fillMaxSize().horizontalScroll(scrollState)
        } else {
            Modifier.fillMaxSize()
        }

        Box(scrollModifier) {
            val layoutModifier = if (isScrollable) {
                Modifier.width(minItemWidth * itemCount).fillMaxHeight()
            } else {
                Modifier.fillMaxSize()
            }

            val gestureModifier = Modifier.pointerInput(itemCount, instantDragMode) {
                val onStart = { offset: Offset ->
                    if (segmentWidthPx > 0f) {
                        val hit = (offset.x / segmentWidthPx).toInt().coerceIn(0, itemCount - 1)
                        if (hit == latestSelectedIndex) {
                            state.isDragging = true
                            latestViewportPointerX = offset.x - scrollState.value
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        }
                    }
                }

                val onDrag = { change: PointerInputChange, delta: Offset ->
                    change.consume()
                    if (state.isDragging && segmentWidthPx > 0f) {
                        latestViewportPointerX = change.position.x - scrollState.value
                        coroutineScope.launch {
                            val totalW = segmentWidthPx * itemCount
                            val newOffset = (state.position.value * segmentWidthPx + delta.x)
                                .coerceIn(0f, totalW - segmentWidthPx)
                            val newIndex = newOffset / segmentWidthPx
                            state.position.snapTo(newIndex)
                            val crossedIdx = (newIndex + 0.5f).toInt().coerceIn(0, itemCount - 1)
                            if (crossedIdx != latestSelectedIndex) {
                                state.selectedIndex = crossedIdx
                                latestOnItemSelected?.invoke(crossedIdx)
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            }
                        }
                    }
                }

                val onEnd = {
                    latestViewportPointerX = -1f
                    if (state.isDragging) {
                        state.isDragging = false
                        val committed = (state.position.value + 0.5f).toInt().coerceIn(0, itemCount - 1)
                        state.selectedIndex = committed
                        latestOnItemSelected?.invoke(committed)
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    }
                }

                val onCancel = {
                    latestViewportPointerX = -1f
                    if (state.isDragging) {
                        state.isDragging = false
                    }
                }

                if (instantDragMode) {
                    detectDragGestures(
                        onDragStart  = onStart,
                        onDrag       = onDrag,
                        onDragEnd    = onEnd,
                        onDragCancel = onCancel,
                    )
                } else {
                    detectDragGesturesAfterLongPress(
                        onDragStart  = onStart,
                        onDrag       = onDrag,
                        onDragEnd    = onEnd,
                        onDragCancel = onCancel,
                    )
                }
            }

            Layout(
                modifier = layoutModifier.then(gestureModifier),
                content = {
                    val left = state.leftPosition.value
                    val right = state.rightPosition.value
                    val progress = (left + right) / 2f
                    val wobble = if (state.isDragging || state.position.isRunning)
                        sin(progress * PI * 2.0).toFloat() * wobbleAmplitude
                    else 0f

                    indicator(
                        BottomNavIndicatorScope(
                            scale         = state.scale.value + wobble,
                            gradientStart = startColor,
                            gradientEnd   = endColor,
                            cornerRadius  = cornerRadius,
                            isDragging    = state.isDragging,
                            modifier      = Modifier.fillMaxSize(),
                        )
                    )

                    BottomNavRowScope.content()
                }
            ) { measurables, constraints ->
                val totalWidth = constraints.maxWidth
                val totalHeight = constraints.maxHeight

                val actualItemCount = itemCount.coerceAtLeast(1)
                val segmentWidth = if (isScrollable) {
                    with(density) { minItemWidth.toPx() }.toInt().coerceAtLeast(1)
                } else {
                    totalWidth / actualItemCount
                }
                segmentWidthPx = segmentWidth.toFloat()

                val left = state.leftPosition.value
                val right = state.rightPosition.value
                val dynamicPillWidth = ((right - left + 1f) * segmentWidth).toInt()

                val pillMeasurable = measurables[0]
                val pillPlaceable = pillMeasurable.measure(
                    constraints.copy(
                        minWidth  = dynamicPillWidth,
                        maxWidth  = dynamicPillWidth,
                        minHeight = totalHeight,
                        maxHeight = totalHeight
                    )
                )

                val itemMeasurables = measurables.drop(1)
                val itemPlaceables = itemMeasurables.map { measurable ->
                    measurable.measure(
                        constraints.copy(
                            minWidth  = segmentWidth,
                            maxWidth  = segmentWidth,
                            minHeight = totalHeight,
                            maxHeight = totalHeight
                        )
                    )
                }

                layout(totalWidth, totalHeight) {
                    val pillX = (left * segmentWidth).toInt()
                    pillPlaceable.placeRelative(pillX, 0)

                    itemPlaceables.forEachIndexed { index, placeable ->
                        placeable.placeRelative(index * segmentWidth, 0)
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 9. Recomposition tracker
// ─────────────────────────────────────────────────────────────────────────────

internal class RecompositionCounter {
    var count = 0
}


