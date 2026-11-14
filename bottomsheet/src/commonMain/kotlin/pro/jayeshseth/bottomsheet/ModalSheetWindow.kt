package pro.jayeshseth.bottomsheet

import androidx.compose.runtime.Composable

/**
 * Platform-aware full-screen overlay window for modal bottom sheets.
 * Breaks completely out of parent layout hierarchies, clips, and bounds (identical to Material 3).
 */
@Composable
internal expect fun ModalSheetWindow(
    onDismissRequest: () -> Unit,
    content: @Composable () -> Unit
)
