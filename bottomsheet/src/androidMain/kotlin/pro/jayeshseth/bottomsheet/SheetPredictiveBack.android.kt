package pro.jayeshseth.bottomsheet

import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Composable
import kotlinx.coroutines.CancellationException

@Composable
actual fun SheetPredictiveBackHandler(
    enabled: Boolean,
    state: BottomSheetState,
    onDismiss: () -> Unit
) {
    val isPredictiveEnabled = enabled && state.animationConfig.predictiveBackConfig.enabled && state.isVisible

    PredictiveBackHandler(enabled = isPredictiveEnabled) { progressFlow ->
        state.isPredictiveBackActive = true
        try {
            progressFlow.collect { backEvent ->
                state.predictiveBackProgress = backEvent.progress
            }
            // User committed the swipe gesture: dismiss sheet
            try {
                onDismiss()
            } finally {
                state.isPredictiveBackActive = false
                state.predictiveBackProgress = 0f
            }
        } catch (e: CancellationException) {
            // User cancelled / released back swipe: spring back
            try {
                val anim = Animatable(state.predictiveBackProgress)
                anim.animateTo(
                    targetValue = 0f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioLowBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    )
                ) {
                    state.predictiveBackProgress = value
                }
            } finally {
                state.isPredictiveBackActive = false
                state.predictiveBackProgress = 0f
            }
            throw e
        }
    }
}
