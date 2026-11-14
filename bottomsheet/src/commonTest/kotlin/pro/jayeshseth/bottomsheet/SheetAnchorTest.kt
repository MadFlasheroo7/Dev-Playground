package pro.jayeshseth.bottomsheet

import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SheetAnchorTest {

    private val density = Density(density = 2f, fontScale = 1f)

    @Test
    fun testAnchorCalculationsWithStandardValues() {
        val containerH = 1000f
        val contentH = 600f
        val maxHeight = SheetDimension.Fraction(0.9f) // 900px
        val peekHeight = SheetDimension.Dp(80.dp) // 160px
        val partialHeight = SheetDimension.Fraction(0.5f) // 500px

        val maxAllowedPx = maxHeight.resolvePx(containerH, contentH, density).coerceIn(0f, containerH)
        assertEquals(900f, maxAllowedPx)

        val effectiveContentPx = minOf(contentH, maxAllowedPx)
        assertEquals(600f, effectiveContentPx)

        val hiddenY = containerH
        assertEquals(1000f, hiddenY)

        val expandedY = (containerH - effectiveContentPx).coerceAtLeast(containerH - maxAllowedPx).coerceAtLeast(0f)
        assertEquals(400f, expandedY)

        val peekPx = peekHeight.resolvePx(containerH, contentH, density)
        assertEquals(160f, peekPx)
        val collapsedY = (containerH - peekPx).coerceIn(expandedY, containerH)
        assertEquals(840f, collapsedY)

        val partialPx = partialHeight.resolvePx(containerH, contentH, density)
        assertEquals(500f, partialPx)
        val partialY = (containerH - partialPx).coerceIn(expandedY, containerH)
        assertEquals(500f, partialY)

        // Expanded is highest (smallest Y), then Partial, then Collapsed, then Hidden
        assertTrue(expandedY < partialY)
        assertTrue(partialY < collapsedY)
        assertTrue(collapsedY < hiddenY)
    }

    @Test
    fun testContentLargerThanContainerClampsExpandedToZero() {
        val containerH = 800f
        val contentH = 1200f
        val maxHeight = SheetDimension.Fraction(1.0f)

        val maxAllowedPx = maxHeight.resolvePx(containerH, contentH, density).coerceIn(0f, containerH)
        assertEquals(800f, maxAllowedPx)

        val effectiveContentPx = minOf(contentH, maxAllowedPx)
        assertEquals(800f, effectiveContentPx)

        val expandedY = (containerH - effectiveContentPx).coerceAtLeast(0f)
        assertEquals(0f, expandedY)
    }

    @Test
    fun testDynamicContentHeightObserverFollowsAnchor() {
        val state = BottomSheetState(initialValue = SheetValue.Expanded)
        val initialAnchors = mapOf(
            SheetValue.Expanded to 400f,
            SheetValue.Hidden to 1000f
        )
        state.updateAnchors(initialAnchors, containerH = 1000f, contentH = 600f)
        assertEquals(400f, state.offset)

        // Content height increases from 600 to 800 -> expanded anchor moves from 400 to 200
        val updatedAnchors = mapOf(
            SheetValue.Expanded to 200f,
            SheetValue.Hidden to 1000f
        )
        state.updateAnchors(updatedAnchors, containerH = 1000f, contentH = 800f)

        // The state offset automatically updates to the new anchor without jump!
        assertEquals(200f, state.offset)
    }
}
