package pro.jayeshseth.bottomsheet

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Android 14/15+ Predictive Back interactive morphing settings.
 *
 * @param enabled Whether predictive back intercept is active.
 * @param maxScaleReduction Maximum scale down of sheet container (e.g. 0.08f scales down to 0.92f).
 * @param maxDownwardShiftDp Maximum downward translation during predictive drag.
 * @param morphCornerRadiusDp Additional top corner radius dynamically added during gesture progress.
 * @param scrimFadeMultiplier Multiplier for scrim opacity reduction during gesture progress.
 */
data class PredictiveBackConfig(
    val enabled: Boolean = true,
    val maxScaleReduction: Float = 0.08f,
    val maxDownwardShiftDp: Dp = 40.dp,
    val morphCornerRadiusDp: Dp = 16.dp,
    val scrimFadeMultiplier: Float = 0.5f
)

/**
 * Unified animation configuration for bottom sheet entrance, exit, anchor snapping,
 * scrim fading, and interactive predictive back gestures.
 */
data class SheetAnimationConfig(
    val enterSpec: AnimationSpec<Float> = spring(
        dampingRatio = Spring.DampingRatioLowBouncy,
        stiffness = Spring.StiffnessMediumLow
    ),
    val exitSpec: AnimationSpec<Float> = spring(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMedium
    ),
    val snapSpec: AnimationSpec<Float> = spring(
        dampingRatio = Spring.DampingRatioLowBouncy,
        stiffness = Spring.StiffnessMediumLow
    ),
    val scrimSpec: AnimationSpec<Float> = tween(durationMillis = 280),
    val predictiveBackConfig: PredictiveBackConfig = PredictiveBackConfig()
)
