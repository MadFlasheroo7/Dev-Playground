package pro.jayeshseth.bottomsheet

import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class SheetDimensionTest {

    private val testDensity = Density(density = 2f, fontScale = 1f)

    @Test
    fun testExactDpResolution() {
        val dim = SheetDimension.Dp(100.dp)
        val resolved = dim.resolvePx(containerHeightPx = 1000f, contentHeightPx = 400f, density = testDensity)
        // 100 dp * 2.0 density = 200 px
        assertEquals(200f, resolved)
    }

    @Test
    fun testFractionResolution() {
        val dim = SheetDimension.Fraction(0.6f)
        val resolved = dim.resolvePx(containerHeightPx = 1000f, contentHeightPx = 400f, density = testDensity)
        assertEquals(600f, resolved)
    }

    @Test
    fun testWrapContentResolution() {
        val dim = SheetDimension.WrapContent
        val resolved = dim.resolvePx(containerHeightPx = 1000f, contentHeightPx = 350f, density = testDensity)
        assertEquals(350f, resolved)
    }

    @Test
    fun testWrapContentZeroContentHeightFallbacks() {
        val dim = SheetDimension.WrapContent
        val resolved = dim.resolvePx(containerHeightPx = 1000f, contentHeightPx = 0f, density = testDensity)
        // Before layout has measured content height, fall back to containerHeightPx instead of 0
        assertEquals(1000f, resolved)
    }

    @Test
    fun testIllegalFractionThrows() {
        assertFailsWith<IllegalArgumentException> {
            SheetDimension.Fraction(-0.1f)
        }
        assertFailsWith<IllegalArgumentException> {
            SheetDimension.Fraction(1.1f)
        }
    }
}
