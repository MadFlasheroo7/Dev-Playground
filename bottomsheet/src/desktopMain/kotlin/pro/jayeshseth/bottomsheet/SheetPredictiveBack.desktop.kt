package pro.jayeshseth.bottomsheet

import androidx.compose.runtime.Composable

@Composable
actual fun SheetPredictiveBackHandler(
    enabled: Boolean,
    state: BottomSheetState,
    onDismiss: () -> Unit
) {
    // Desktop platforms handle navigation via UI controls or keyboard shortcuts
}
