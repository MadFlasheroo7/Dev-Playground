package pro.jayeshseth.bottomsheet

import androidx.compose.runtime.Composable

/**
 * Handles system back events and interactive predictive back animations.
 *
 * On Android 14/15+, intercepts predictive back swipes, driving real-time gesture progress
 * in [state] so the sheet morphs scale, position, and corners before dismissing or snapping back.
 */
@Composable
expect fun SheetPredictiveBackHandler(
    enabled: Boolean,
    state: BottomSheetState,
    onDismiss: () -> Unit
)
