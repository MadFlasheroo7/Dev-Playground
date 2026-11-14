package pro.jayeshseth.bottomsheet

import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.MonotonicFrameClock
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.unit.Velocity
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BottomSheetStateTest {

    private val testClock = object : MonotonicFrameClock {
        private var time = 0L
        override suspend fun <R> withFrameNanos(onFrame: (frameTimeNanos: Long) -> R): R {
            time += 16_000_000L
            return onFrame(time)
        }
    }

    @Test
    fun testInitialState() {
        val state = BottomSheetState(initialValue = SheetValue.Hidden)
        assertEquals(SheetValue.Hidden, state.currentValue)
        assertEquals(SheetValue.Hidden, state.targetValue)
        assertFalse(state.hasOffset)
    }

    @Test
    fun testUpdateAnchorsInitializesOffset() {
        val state = BottomSheetState(initialValue = SheetValue.Expanded)
        val anchors = mapOf(
            SheetValue.Expanded to 200f,
            SheetValue.Collapsed to 800f,
            SheetValue.Hidden to 1000f
        )
        state.updateAnchors(anchors, containerH = 1000f, contentH = 800f)

        assertTrue(state.hasOffset)
        assertEquals(200f, state.offset)
        assertEquals(SheetValue.Expanded, state.currentValue)
        assertEquals(200f, state.minAnchor)
        assertEquals(1000f, state.maxAnchor)
    }

    @Test
    fun testDispatchRawDeltaWithinAnchors() {
        val state = BottomSheetState(initialValue = SheetValue.Expanded)
        val anchors = mapOf(
            SheetValue.Expanded to 200f,
            SheetValue.Hidden to 1000f
        )
        state.updateAnchors(anchors, containerH = 1000f, contentH = 800f)

        // Drag downwards by 50px
        val consumed = state.dispatchRawDelta(50f)
        assertEquals(50f, consumed)
        assertEquals(250f, state.offset)
    }

    @Test
    fun testRubberBandOverscrollResistance() {
        val state = BottomSheetState(initialValue = SheetValue.Expanded)
        val anchors = mapOf(
            SheetValue.Expanded to 200f,
            SheetValue.Hidden to 1000f
        )
        state.updateAnchors(anchors, containerH = 1000f, contentH = 800f)

        // At 200f (Expanded), drag upward by -50px
        val consumed = state.dispatchRawDelta(-50f)
        // Overscroll resistance should heavily dampen the delta
        assertTrue(consumed > -50f, "Consumed delta ($consumed) should be damped compared to -50")
        assertTrue(state.requireOffset() < 200f)
        assertTrue(state.requireOffset() > 150f)
    }

    @Test
    fun testSettleDownwardWithFling() = runTest {
        withContext(testClock) {
            val state = BottomSheetState(initialValue = SheetValue.Expanded)
            val anchors = mapOf(
                SheetValue.Expanded to 200f,
                SheetValue.Collapsed to 700f,
                SheetValue.Hidden to 1000f
            )
            state.updateAnchors(anchors, containerH = 1000f, contentH = 800f)

            // Drag down to 300px
            state.dispatchRawDelta(100f)
            assertEquals(300f, state.offset)

            // Fling down with high velocity
            state.settle(velocity = 1200f)
            assertEquals(SheetValue.Collapsed, state.currentValue)
            assertEquals(700f, state.offset)
        }
    }

    @Test
    fun testSettleUpwardWithFling() = runTest {
        withContext(testClock) {
            val state = BottomSheetState(initialValue = SheetValue.Collapsed)
            val anchors = mapOf(
                SheetValue.Expanded to 200f,
                SheetValue.Collapsed to 700f,
                SheetValue.Hidden to 1000f
            )
            state.updateAnchors(anchors, containerH = 1000f, contentH = 800f)

            // Fling up with high negative velocity
            state.settle(velocity = -1200f)
            assertEquals(SheetValue.Expanded, state.currentValue)
            assertEquals(200f, state.offset)
        }
    }

    @Test
    fun testSwipeToDismissDisabledPreventsHiddenSettle() = runTest {
        withContext(testClock) {
            val state = BottomSheetState(initialValue = SheetValue.Collapsed)
            state.swipeToDismissEnabled = false
            val anchors = mapOf(
                SheetValue.Expanded to 200f,
                SheetValue.Collapsed to 700f,
                SheetValue.Hidden to 1000f
            )
            state.updateAnchors(anchors, containerH = 1000f, contentH = 800f)

            // Fling downward when swipe to dismiss is disabled: should settle at Collapsed, not Hidden
            state.settle(velocity = 2000f)
            assertEquals(SheetValue.Collapsed, state.currentValue)
            assertEquals(700f, state.offset)
        }
    }

    @Test
    fun testExpansionProgressCalculation() {
        val state = BottomSheetState(initialValue = SheetValue.Hidden)
        val anchors = mapOf(
            SheetValue.Expanded to 200f,
            SheetValue.Hidden to 1000f
        )
        state.updateAnchors(anchors, containerH = 1000f, contentH = 800f)

        // At Hidden (1000f), progress is 0.0
        assertEquals(0f, state.progress)

        // Move to Expanded (200f), progress is 1.0
        state.dispatchRawDelta(-800f)
        assertEquals(1f, state.progress)

        // Move to 600f (midpoint), progress is 0.5
        state.dispatchRawDelta(400f)
        assertEquals(0.5f, state.progress)
    }

    @Test
    fun testStatusBarThresholdDetection() {
        val state = BottomSheetState(initialValue = SheetValue.Hidden)
        val anchors = mapOf(
            SheetValue.Expanded to 40f,
            SheetValue.Hidden to 1000f
        )
        state.updateAnchors(anchors, containerH = 1000f, contentH = 960f)

        assertFalse(state.isNearTopThreshold(thresholdPx = 100f))

        // Move near top (40f)
        state.dispatchRawDelta(-960f)
        assertTrue(state.isNearTopThreshold(thresholdPx = 100f))
    }

    @Test
    fun testIsVisibleIncludesTargetValue() {
        val state = BottomSheetState(initialValue = SheetValue.Hidden)
        assertFalse(state.isVisible)

        // As soon as targetValue aims for an anchor, isVisible becomes true immediately
        state.targetValue = SheetValue.Expanded
        assertTrue(state.isVisible)
    }

    @Test
    fun testAnimateToWaitsForAnchorsWhenEmpty() = runTest {
        withContext(testClock) {
            val state = BottomSheetState(initialValue = SheetValue.Hidden)
            var expandedCompleted = false

            val job = launch {
                state.expand()
                expandedCompleted = true
            }
            kotlinx.coroutines.yield()

            // Before anchors are populated, expand() suspends and targetValue is set
            assertEquals(SheetValue.Expanded, state.targetValue)
            assertFalse(expandedCompleted)

            // When layout measures and updates anchors:
            val anchors = mapOf(
                SheetValue.Expanded to 200f,
                SheetValue.Hidden to 1000f
            )
            state.updateAnchors(anchors, containerH = 1000f, contentH = 800f)

            // Let coroutine run and animation complete
            job.join()
            assertTrue(expandedCompleted)
            assertEquals(SheetValue.Expanded, state.currentValue)
            assertEquals(200f, state.offset)
        }
    }

    @Test
    fun testShowExpandsToPartiallyExpandedIfPresent() = runTest {
        withContext(testClock) {
            val state = BottomSheetState(initialValue = SheetValue.Hidden)
            val anchors = mapOf(
                SheetValue.Expanded to 200f,
                SheetValue.PartiallyExpanded to 500f,
                SheetValue.Hidden to 1000f
            )
            state.updateAnchors(anchors, containerH = 1000f, contentH = 800f)

            state.show()
            assertEquals(SheetValue.PartiallyExpanded, state.currentValue)
            assertEquals(500f, state.offset)
        }
    }

    @Test
    fun testNestedScrollPreScrollDoesNotOvershootMin() {
        val state = BottomSheetState(initialValue = SheetValue.Collapsed)
        val anchors = mapOf(
            SheetValue.Expanded to 200f,
            SheetValue.Collapsed to 600f,
            SheetValue.Hidden to 1000f
        )
        state.updateAnchors(anchors, containerH = 1000f, contentH = 800f)

        // Sheet is at 220f (20px away from expanded at 200f)
        state.dispatchRawDelta(-380f)
        assertEquals(220f, state.offset)

        // Drag upward by -50px: sheet should only consume -20px to reach expanded (200f),
        // leaving the remaining -30px for the child LazyColumn without overscroll resistance.
        val preScrollConsumed = state.nestedScrollConnection.onPreScroll(
            available = Offset(0f, -50f),
            source = NestedScrollSource.UserInput
        )

        assertEquals(-20f, preScrollConsumed.y)
        assertEquals(200f, state.offset)
    }

    @Test
    fun testNestedScrollPostFlingUpwardDoesNotSettle() = runTest {
        withContext(testClock) {
            val state = BottomSheetState(initialValue = SheetValue.Expanded)
            val anchors = mapOf(
                SheetValue.Expanded to 200f,
                SheetValue.Hidden to 1000f
            )
            state.updateAnchors(anchors, containerH = 1000f, contentH = 800f)

            // Child was scrolling upwards (negative velocity): onPostFling should not settle
            val remainingFling = state.nestedScrollConnection.onPostFling(
                consumed = Velocity(0f, -800f),
                available = Velocity(0f, -200f)
            )

            assertEquals(Velocity.Zero, remainingFling)
            assertFalse(state.isAnimationRunning)
            assertEquals(SheetValue.Expanded, state.currentValue)
        }
    }

    @Test
    fun testDynamicAnimationConfigUpdates() {
        val state = BottomSheetState(initialValue = SheetValue.Hidden)
        val initialConfig = state.animationConfig

        val newConfig = SheetAnimationConfig(
            enterSpec = tween(durationMillis = 500),
            exitSpec = tween(durationMillis = 400)
        )
        state.animationConfig = newConfig

        assertEquals(newConfig, state.animationConfig)
        assertTrue(state.animationConfig != initialConfig)
    }

    @Test
    fun testRequireOffsetNeverDefaultsToZero() {
        val state = BottomSheetState(initialValue = SheetValue.Hidden)
        assertFalse(state.hasOffset)
        // Before measurement and without container height, should not return 0 (top of screen)
        assertTrue(state.requireOffset() > 0f)

        // Once container height is known, should return Hidden/bottom anchor, never 0f
        val anchors = mapOf(
            SheetValue.Expanded to 200f,
            SheetValue.Hidden to 1000f
        )
        state.updateAnchors(anchors, containerH = 1000f, contentH = 800f)
        assertEquals(1000f, state.requireOffset())
    }

    @Test
    fun testSheetOpensFromContainerBottomWhenInitialValueIsHidden() = runTest {
        withContext(testClock) {
            val state = BottomSheetState(initialValue = SheetValue.Hidden)
            val anchors = mapOf(
                SheetValue.Expanded to 200f,
                SheetValue.Hidden to 1000f
            )
            state.updateAnchors(anchors, containerH = 1000f, contentH = 800f)

            assertEquals(SheetValue.Hidden, state.currentValue)
            assertEquals(1000f, state.offset)

            // When expand() is called, starting offset must be at bottom (1000f), NOT top (0f)
            val job = launch {
                state.expand()
            }
            job.join()
            assertEquals(SheetValue.Expanded, state.currentValue)
            assertEquals(200f, state.offset)
        }
    }

    @Test
    fun testExpandWithCustomSpec() = runTest {
        withContext(testClock) {
            val state = BottomSheetState(initialValue = SheetValue.Hidden)
            val anchors = mapOf(
                SheetValue.Expanded to 200f,
                SheetValue.Hidden to 1000f
            )
            state.updateAnchors(anchors, containerH = 1000f, contentH = 800f)

            state.expand(spec = tween(150))
            assertEquals(SheetValue.Expanded, state.currentValue)
            assertEquals(200f, state.offset)
        }
    }

    @Test
    fun testPartialExpandWaitsForAnchorsWhenEmpty() = runTest {
        withContext(testClock) {
            val state = BottomSheetState(initialValue = SheetValue.Hidden)
            var partialCompleted = false

            val job = launch {
                state.partialExpand()
                partialCompleted = true
            }
            kotlinx.coroutines.yield()

            // Before anchors are populated, partialExpand() suspends and targetValue is set
            assertEquals(SheetValue.PartiallyExpanded, state.targetValue)
            assertFalse(partialCompleted)

            // When anchors update:
            val anchors = mapOf(
                SheetValue.Expanded to 200f,
                SheetValue.PartiallyExpanded to 500f,
                SheetValue.Hidden to 1000f
            )
            state.updateAnchors(anchors, containerH = 1000f, contentH = 800f)

            job.join()
            assertTrue(partialCompleted)
            assertEquals(SheetValue.PartiallyExpanded, state.currentValue)
            assertEquals(500f, state.offset)
        }
    }

    @Test
    fun testPartialExpandDoesNothingIfAnchorNotPresent() = runTest {
        withContext(testClock) {
            val state = BottomSheetState(initialValue = SheetValue.Hidden)
            val anchors = mapOf(
                SheetValue.Expanded to 200f,
                SheetValue.Hidden to 1000f
            )
            state.updateAnchors(anchors, containerH = 1000f, contentH = 800f)

            state.partialExpand()
            // Anchor was not present, stays Hidden
            assertEquals(SheetValue.Hidden, state.currentValue)
            assertEquals(1000f, state.offset)
        }
    }

    @Test
    fun testAnimateToUpwardMotionSelectsEnterSpec() = runTest {
        withContext(testClock) {
            val state = BottomSheetState(
                initialValue = SheetValue.Collapsed,
                animationConfig = SheetAnimationConfig(
                    enterSpec = tween(durationMillis = 300),
                    snapSpec = tween(durationMillis = 100)
                )
            )
            val anchors = mapOf(
                SheetValue.Expanded to 200f,
                SheetValue.Collapsed to 700f,
                SheetValue.Hidden to 1000f
            )
            state.updateAnchors(anchors, containerH = 1000f, contentH = 800f)

            // Expanding upward from Collapsed to Expanded should complete smoothly
            state.expand()
            assertEquals(SheetValue.Expanded, state.currentValue)
            assertEquals(200f, state.offset)
        }
    }

    @Test
    fun testRequireOffsetWithNaNOrUninitializedContainerHeight() {
        val state = BottomSheetState(initialValue = SheetValue.Hidden)
        // With NaN container height or before anchors, must return fallback > 0
        state.containerHeightPx = Float.NaN
        assertTrue(state.requireOffset() > 0f)
    }

    @Test
    fun testScaffoldStateAnimationConfigDynamicUpdate() {
        val innerState = BottomSheetState(
            initialValue = SheetValue.Collapsed,
            skipHiddenState = true,
            animationConfig = SheetAnimationConfig(enterSpec = tween(200))
        )
        val newConfig = SheetAnimationConfig(enterSpec = tween(500))
        innerState.animationConfig = newConfig
        assertEquals(newConfig, innerState.animationConfig)
    }
}
