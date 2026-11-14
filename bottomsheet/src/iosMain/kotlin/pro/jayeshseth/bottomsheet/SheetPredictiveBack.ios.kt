package pro.jayeshseth.bottomsheet

import androidx.compose.runtime.Composable

@Composable
actual fun SheetPredictiveBackHandler(
    enabled: Boolean,
    state: BottomSheetState,
    onDismiss: () -> Unit
) {
    // iOS gesture navigation is managed by UINavigationController in native host
}
