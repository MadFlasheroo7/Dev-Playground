package pro.jayeshseth.devcontrols.sdui.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Declarative animation specification mapping to Jetpack Compose animation specs.
 */
@Serializable(with = SduiAnimationSpecSerializer::class)
sealed interface SduiAnimationSpec {

    @Serializable
    @SerialName("spring")
    data class Spring(
        /** "highBouncy", "mediumBouncy", "lowBouncy", "noBouncy", or raw float ratio */
        val dampingRatio: String = "lowBouncy",
        /** "high", "medium", "low", "veryLow", or raw float stiffness */
        val stiffness: String = "medium"
    ) : SduiAnimationSpec

    @Serializable
    @SerialName("tween")
    data class Tween(
        val durationMillis: Int = 300,
        val delayMillis: Int = 0,
        /** "linear", "fastOutSlowIn", "linearOutSlowIn", "fastOutLinearIn", "cubicBezier" */
        val easing: String = "fastOutSlowIn",
        /** [x1, y1, x2, y2] control points if easing is "cubicBezier" */
        val cubicBezierPoints: List<Float>? = null
    ) : SduiAnimationSpec

    @Serializable
    @SerialName("keyframes")
    data class Keyframes(
        val durationMillis: Int = 1000,
        val delayMillis: Int = 0,
        /** Map of timestampMs to float value (e.g. { "0": 0.0, "500": 1.2, "1000": 1.0 }) */
        val keyframeValues: Map<Int, Float> = emptyMap()
    ) : SduiAnimationSpec

    @Serializable
    @SerialName("repeat")
    data class Repeatable(
        /** Iteration count; -1 or null means infinite repeat */
        val iterations: Int = -1,
        /** "restart" or "reverse" */
        val repeatMode: String = "restart",
        val animation: SduiAnimationSpec = Tween()
    ) : SduiAnimationSpec

    @Serializable
    @SerialName("snap")
    data class Snap(
        val delayMillis: Int = 0
    ) : SduiAnimationSpec
}

/**
 * Enter transition declaration for AnimatedVisibility.
 */
@Serializable
data class SduiEnterTransition(
    val fadeIn: Boolean = false,
    val fadeDurationMillis: Int = 300,
    val slideInVertically: Boolean = false,
    /** "up" (from bottom) or "down" (from top) */
    val slideVerticalDirection: String = "up",
    val slideInHorizontally: Boolean = false,
    /** "start", "end", "left", "right" */
    val slideHorizontalDirection: String = "start",
    val expandVertically: Boolean = false,
    val scaleIn: Boolean = false,
    val initialScale: Float = 0.8f
)

/**
 * Exit transition declaration for AnimatedVisibility.
 */
@Serializable
data class SduiExitTransition(
    val fadeOut: Boolean = false,
    val fadeDurationMillis: Int = 250,
    val slideOutVertically: Boolean = false,
    val slideVerticalDirection: String = "down",
    val slideOutHorizontally: Boolean = false,
    val slideHorizontalDirection: String = "end",
    val shrinkVertically: Boolean = false,
    val scaleOut: Boolean = false,
    val targetScale: Float = 0.8f
)
