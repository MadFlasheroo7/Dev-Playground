package pro.jayeshseth.bottomsheet

import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Flexible dimension specification for bottom sheet sizing (peek height, max height, partial height).
 */
sealed interface SheetDimension {
    /** Fixed density-independent pixel dimension. */
    data class Dp(val value: androidx.compose.ui.unit.Dp) : SheetDimension

    /**
     * Fraction of container height (0.0f .. 1.0f).
     * For example, 0.5f specifies 50% of the available container height.
     */
    data class Fraction(val fraction: Float) : SheetDimension {
        init {
            require(fraction in 0f..1f) {
                "Fraction must be between 0f and 1f inclusive, but was $fraction"
            }
        }
    }

    /**
     * Sizes to fit the intrinsic measured height of the sheet content.
     */
    data object WrapContent : SheetDimension

    /**
     * Resolves this [SheetDimension] into physical pixels given the container height and measured content height.
     */
    fun resolvePx(containerHeightPx: Float, contentHeightPx: Float, density: Density): Float = when (this) {
        is Dp -> with(density) { value.toPx() }
        is Fraction -> containerHeightPx * this.fraction
        is WrapContent -> if (contentHeightPx > 0f) contentHeightPx else containerHeightPx
    }

    companion object {
        fun dp(value: androidx.compose.ui.unit.Dp): SheetDimension = Dp(value)
        fun fraction(fraction: Float): SheetDimension = Fraction(fraction)
        val wrap: SheetDimension = WrapContent
    }
}
