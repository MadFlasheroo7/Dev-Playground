package pro.jayeshseth.devcontrols.sdui.state

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.ui.unit.IntSize
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import pro.jayeshseth.devcontrols.sdui.model.SduiAction
import pro.jayeshseth.devcontrols.sdui.model.SduiAnimationSpec

/**
 * Event log entry recorded when actions execute.
 */
data class SduiActionEvent(
    val timestamp: Long,
    val actionType: String,
    val details: String
)

/**
 * Executes polymorphic SDUI actions against the reactive [SduiStateStore].
 */
class SduiActionRunner(
    private val store: SduiStateStore,
    private val coroutineScope: CoroutineScope,
    private val onNavigate: (destination: String) -> Unit = {},
    private val onNativeEvent: (event: String, payload: Map<String, String>) -> Unit = { _, _ -> },
    private val onLogEvent: (SduiActionEvent) -> Unit = {}
) {
    fun execute(actions: List<SduiAction>) {
        actions.forEach { execute(it) }
    }

    fun execute(action: SduiAction) {
        when (action) {
            is SduiAction.SetState -> {
                val evaluated = store.eval(action.valueExpr)
                store.set(action.key, evaluated)
                log("SetState", "${action.key} = $evaluated (from '${action.valueExpr}')")
            }
            is SduiAction.ToggleState -> {
                store.toggle(action.key)
                log("ToggleState", "${action.key} -> ${store.get(action.key)}")
            }
            is SduiAction.AnimateTo -> {
                val targetFloat = store.evalFloat(action.targetValueExpr, 0f)
                val currentFloat = store.evalFloat(action.key, 0f)
                log("AnimateTo", "${action.key}: $currentFloat -> $targetFloat via ${action.animation}")
                coroutineScope.launch {
                    val animatable = Animatable(currentFloat)
                    val spec = toComposeAnimationSpec(action.animation)
                    animatable.animateTo(
                        targetValue = targetFloat,
                        animationSpec = spec
                    ) {
                        store.set(action.key, value)
                    }
                }
            }
            is SduiAction.Navigate -> {
                log("Navigate", "destination: ${action.destination} (external=${action.isExternal})")
                onNavigate(action.destination)
            }
            is SduiAction.EmitNativeEvent -> {
                log("EmitNativeEvent", "event: ${action.eventName}, payload: ${action.payload}")
                onNativeEvent(action.eventName, action.payload)
            }
            is SduiAction.Conditional -> {
                val isTrue = store.evalBoolean(action.conditionExpr, false)
                log("Conditional", "'${action.conditionExpr}' evaluated to $isTrue")
                if (isTrue) {
                    execute(action.thenActions)
                } else {
                    execute(action.elseActions)
                }
            }
            is SduiAction.Chain -> {
                log("Chain", "Executing ${action.actions.size} actions")
                execute(action.actions)
            }
            is SduiAction.ApiRequest -> {
                log("ApiRequest", "${action.method} ${action.url}")
                // Placeholder/hook for server request
                if (action.responseStateKey != null) {
                    store.set(action.responseStateKey, "{\"status\":\"ok\"}")
                }
                execute(action.onResponse)
            }
            is SduiAction.OpenBottomSheet -> {
                store.set("isBottomSheetOpen", true)
                store.set("${action.sheetId}_isOpen", true)
                store.set(action.sheetId, true)
                log("OpenBottomSheet", "sheetId: ${action.sheetId}")
            }
            is SduiAction.CloseBottomSheet -> {
                store.set("isBottomSheetOpen", false)
                store.set("${action.sheetId}_isOpen", false)
                store.set(action.sheetId, false)
                log("CloseBottomSheet", "sheetId: ${action.sheetId}")
            }
            is SduiAction.Unknown -> {
                log("UnknownAction", "Skipped unknown action: ${action.rawType}")
            }
        }
    }

    private fun log(type: String, details: String) {
        val event = SduiActionEvent(
            timestamp = 0L,
            actionType = type,
            details = details
        )
        onLogEvent(event)
    }

    companion object {
        fun toComposeAnimationSpec(spec: SduiAnimationSpec): androidx.compose.animation.core.AnimationSpec<Float> {
            return when (spec) {
                is SduiAnimationSpec.Spring -> {
                    val damping = when (spec.dampingRatio.lowercase()) {
                        "highbouncy" -> Spring.DampingRatioHighBouncy
                        "mediumbouncy" -> Spring.DampingRatioMediumBouncy
                        "lowbouncy" -> Spring.DampingRatioLowBouncy
                        "nobouncy" -> Spring.DampingRatioNoBouncy
                        else -> spec.dampingRatio.toFloatOrNull() ?: Spring.DampingRatioMediumBouncy
                    }
                    val stiffness = when (spec.stiffness.lowercase()) {
                        "high" -> Spring.StiffnessHigh
                        "medium" -> Spring.StiffnessMedium
                        "low" -> Spring.StiffnessLow
                        "verylow" -> Spring.StiffnessVeryLow
                        else -> spec.stiffness.toFloatOrNull() ?: Spring.StiffnessMedium
                    }
                    spring(dampingRatio = damping, stiffness = stiffness)
                }
                is SduiAnimationSpec.Tween -> {
                    val easing = when (spec.easing.lowercase()) {
                        "linear" -> LinearEasing
                        "fastoutslowin" -> FastOutSlowInEasing
                        "linearoutslowin" -> LinearOutSlowInEasing
                        "fastoutlinearin" -> FastOutLinearInEasing
                        "cubicbezier" -> {
                            val pts = spec.cubicBezierPoints
                            if (pts != null && pts.size >= 4) {
                                CubicBezierEasing(pts[0], pts[1], pts[2], pts[3])
                            } else {
                                FastOutSlowInEasing
                            }
                        }
                        else -> FastOutSlowInEasing
                    }
                    tween(
                        durationMillis = spec.durationMillis,
                        delayMillis = spec.delayMillis,
                        easing = easing
                    )
                }
                is SduiAnimationSpec.Keyframes -> {
                    tween(durationMillis = spec.durationMillis, delayMillis = spec.delayMillis)
                }
                is SduiAnimationSpec.Repeatable -> {
                    toComposeAnimationSpec(spec.animation)
                }
                is SduiAnimationSpec.Snap -> {
                    androidx.compose.animation.core.snap(delayMillis = spec.delayMillis)
                }
            }
        }

        fun toComposeIntSizeAnimationSpec(spec: SduiAnimationSpec): FiniteAnimationSpec<IntSize> {
            return when (spec) {
                is SduiAnimationSpec.Spring -> {
                    val damping = when (spec.dampingRatio.lowercase()) {
                        "highbouncy" -> Spring.DampingRatioHighBouncy
                        "mediumbouncy" -> Spring.DampingRatioMediumBouncy
                        "lowbouncy" -> Spring.DampingRatioLowBouncy
                        "nobouncy" -> Spring.DampingRatioNoBouncy
                        else -> spec.dampingRatio.toFloatOrNull() ?: Spring.DampingRatioMediumBouncy
                    }
                    val stiffness = when (spec.stiffness.lowercase()) {
                        "high" -> Spring.StiffnessHigh
                        "medium" -> Spring.StiffnessMedium
                        "low" -> Spring.StiffnessLow
                        "verylow" -> Spring.StiffnessVeryLow
                        else -> spec.stiffness.toFloatOrNull() ?: Spring.StiffnessMedium
                    }
                    spring(dampingRatio = damping, stiffness = stiffness)
                }
                is SduiAnimationSpec.Tween -> {
                    val easing = when (spec.easing.lowercase()) {
                        "linear" -> LinearEasing
                        "fastoutslowin" -> FastOutSlowInEasing
                        "linearoutslowin" -> LinearOutSlowInEasing
                        "fastoutlinearin" -> FastOutLinearInEasing
                        "cubicbezier" -> {
                            val pts = spec.cubicBezierPoints
                            if (pts != null && pts.size >= 4) {
                                CubicBezierEasing(pts[0], pts[1], pts[2], pts[3])
                            } else {
                                FastOutSlowInEasing
                            }
                        }
                        else -> FastOutSlowInEasing
                    }
                    tween(
                        durationMillis = spec.durationMillis,
                        delayMillis = spec.delayMillis,
                        easing = easing
                    )
                }
                is SduiAnimationSpec.Keyframes -> {
                    tween(durationMillis = spec.durationMillis, delayMillis = spec.delayMillis)
                }
                is SduiAnimationSpec.Repeatable -> {
                    toComposeIntSizeAnimationSpec(spec.animation)
                }
                is SduiAnimationSpec.Snap -> {
                    androidx.compose.animation.core.snap(delayMillis = spec.delayMillis)
                }
            }
        }
    }
}
