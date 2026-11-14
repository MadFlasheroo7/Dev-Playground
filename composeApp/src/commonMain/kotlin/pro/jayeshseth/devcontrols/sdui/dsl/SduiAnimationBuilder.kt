package pro.jayeshseth.devcontrols.sdui.dsl

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import pro.jayeshseth.devcontrols.sdui.model.SduiAnimationSpec
import pro.jayeshseth.devcontrols.sdui.model.SduiAnimationSpecSerializer
import pro.jayeshseth.devcontrols.sdui.model.SduiEnterTransition
import pro.jayeshseth.devcontrols.sdui.model.SduiExitTransition
import pro.jayeshseth.devcontrols.sdui.model.defaultSduiJson

/**
 * Type-safe builder for keyframe values mapping timestamp (in milliseconds) to float values.
 */
@SduiDsl
class SduiKeyframesBuilder {
    private val keyframes = mutableMapOf<Int, Float>()

    fun at(timestampMs: Int, value: Float): SduiKeyframesBuilder {
        keyframes[timestampMs] = value
        return this
    }

    fun keyframe(timestampMs: Int, value: Float): SduiKeyframesBuilder = at(timestampMs, value)

    infix fun Int.at(value: Float) {
        at(this, value)
    }

    infix fun Int.at(value: Number) {
        at(this, value.toFloat())
    }

    fun build(): Map<Int, Float> = keyframes.toMap()
}

/**
 * Creates a default spring animation spec.
 */
fun spring(): SduiAnimationSpec.Spring = SduiAnimationSpec.Spring(
    dampingRatio = "lowBouncy",
    stiffness = "medium"
)

/**
 * Creates a spring animation spec with named preset ratio and stiffness.
 *
 * @param dampingRatio Predefined ratio ("highBouncy", "mediumBouncy", "lowBouncy", "noBouncy") or raw float as String
 * @param stiffness Predefined stiffness ("high", "medium", "low", "veryLow") or raw float as String
 */
fun spring(
    dampingRatio: String,
    stiffness: String = "medium"
): SduiAnimationSpec.Spring = SduiAnimationSpec.Spring(
    dampingRatio = dampingRatio,
    stiffness = stiffness
)

/**
 * Creates a spring animation spec with numeric damping and stiffness values.
 */
fun spring(
    dampingRatio: Float,
    stiffness: Float = 1500f
): SduiAnimationSpec.Spring = SduiAnimationSpec.Spring(
    dampingRatio = dampingRatio.toString(),
    stiffness = stiffness.toString()
)

/**
 * Creates a tween animation spec.
 *
 * @param durationMillis Duration of the animation in milliseconds.
 * @param delayMillis Delay before starting animation in milliseconds.
 * @param easing Easing curve ("linear", "fastOutSlowIn", "linearOutSlowIn", "fastOutLinearIn", "cubicBezier").
 * @param cubicBezierPoints Control points [x1, y1, x2, y2] if easing is "cubicBezier".
 */
fun tween(
    durationMillis: Int = 300,
    delayMillis: Int = 0,
    easing: String = "fastOutSlowIn",
    cubicBezierPoints: List<Float>? = null
): SduiAnimationSpec.Tween = SduiAnimationSpec.Tween(
    durationMillis = durationMillis,
    delayMillis = delayMillis,
    easing = easing,
    cubicBezierPoints = cubicBezierPoints
)

/**
 * Creates a cubic-bezier tween animation spec with explicit control points.
 */
fun tween(
    durationMillis: Int = 300,
    delayMillis: Int = 0,
    x1: Float,
    y1: Float,
    x2: Float,
    y2: Float
): SduiAnimationSpec.Tween = tween(
    durationMillis = durationMillis,
    delayMillis = delayMillis,
    easing = "cubicBezier",
    cubicBezierPoints = listOf(x1, y1, x2, y2)
)

/**
 * Creates a keyframes animation spec using a builder lambda.
 */
fun keyframes(
    durationMillis: Int = 1000,
    delayMillis: Int = 0,
    init: SduiKeyframesBuilder.() -> Unit
): SduiAnimationSpec.Keyframes = SduiAnimationSpec.Keyframes(
    durationMillis = durationMillis,
    delayMillis = delayMillis,
    keyframeValues = SduiKeyframesBuilder().apply(init).build()
)

/**
 * Creates a keyframes animation spec from an existing map of timestamp to value.
 */
fun keyframes(
    values: Map<Int, Float>,
    durationMillis: Int = 1000,
    delayMillis: Int = 0
): SduiAnimationSpec.Keyframes = SduiAnimationSpec.Keyframes(
    durationMillis = durationMillis,
    delayMillis = delayMillis,
    keyframeValues = values
)

/**
 * Creates a repeatable animation spec.
 *
 * @param iterations Number of repetitions (-1 for infinite).
 * @param repeatMode "restart" or "reverse".
 * @param animation Wrapped child animation spec.
 */
fun repeatable(
    iterations: Int = -1,
    repeatMode: String = "restart",
    animation: SduiAnimationSpec = tween()
): SduiAnimationSpec.Repeatable = SduiAnimationSpec.Repeatable(
    iterations = iterations,
    repeatMode = repeatMode,
    animation = animation
)

/**
 * Creates a snap animation spec.
 */
fun snap(delayMillis: Int = 0): SduiAnimationSpec.Snap = SduiAnimationSpec.Snap(delayMillis = delayMillis)

/**
 * Creates an Enter transition specification for AnimatedVisibility.
 */
fun enterTransition(
    fadeIn: Boolean = false,
    fadeDurationMillis: Int = 300,
    slideInVertically: Boolean = false,
    slideVerticalDirection: String = "up",
    slideInHorizontally: Boolean = false,
    slideHorizontalDirection: String = "start",
    expandVertically: Boolean = false,
    scaleIn: Boolean = false,
    initialScale: Float = 0.8f
): SduiEnterTransition = SduiEnterTransition(
    fadeIn = fadeIn,
    fadeDurationMillis = fadeDurationMillis,
    slideInVertically = slideInVertically,
    slideVerticalDirection = slideVerticalDirection,
    slideInHorizontally = slideInHorizontally,
    slideHorizontalDirection = slideHorizontalDirection,
    expandVertically = expandVertically,
    scaleIn = scaleIn,
    initialScale = initialScale
)

/**
 * Creates an Exit transition specification for AnimatedVisibility.
 */
fun exitTransition(
    fadeOut: Boolean = false,
    fadeDurationMillis: Int = 250,
    slideOutVertically: Boolean = false,
    slideVerticalDirection: String = "down",
    slideOutHorizontally: Boolean = false,
    slideHorizontalDirection: String = "end",
    shrinkVertically: Boolean = false,
    scaleOut: Boolean = false,
    targetScale: Float = 0.8f
): SduiExitTransition = SduiExitTransition(
    fadeOut = fadeOut,
    fadeDurationMillis = fadeDurationMillis,
    slideOutVertically = slideOutVertically,
    slideVerticalDirection = slideVerticalDirection,
    slideOutHorizontally = slideOutHorizontally,
    slideHorizontalDirection = slideHorizontalDirection,
    shrinkVertically = shrinkVertically,
    scaleOut = scaleOut,
    targetScale = targetScale
)

/**
 * Serializes [SduiAnimationSpec] into a formatted JSON string.
 */
fun SduiAnimationSpec.toJson(json: Json = defaultSduiJson): String =
    json.encodeToString(SduiAnimationSpecSerializer, this)

/**
 * Serializes [SduiAnimationSpec] into a [JsonElement].
 */
fun SduiAnimationSpec.toJsonElement(json: Json = defaultSduiJson): JsonElement =
    json.encodeToJsonElement(SduiAnimationSpecSerializer, this)

/**
 * Serializes [SduiEnterTransition] into a JSON string.
 */
fun SduiEnterTransition.toJson(json: Json = defaultSduiJson): String =
    json.encodeToString(SduiEnterTransition.serializer(), this)

/**
 * Serializes [SduiExitTransition] into a JSON string.
 */
fun SduiExitTransition.toJson(json: Json = defaultSduiJson): String =
    json.encodeToString(SduiExitTransition.serializer(), this)
