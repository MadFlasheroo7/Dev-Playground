package pro.jayeshseth.bottomsheet

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * Platform-specific dynamic status bar tint effect.
 *
 * When [isAtStatusBar] is true (sheet reached status bar threshold), automatically switches
 * the status bar contrast so icons remain clearly legible against [sheetContainerColor].
 * When the sheet is dragged down below the threshold or dismissed, smoothly reverts to the
 * host window's original status bar icon contrast.
 */
@Composable
expect fun SheetStatusBarTintEffect(
    enabled: Boolean,
    isAtStatusBar: Boolean,
    sheetContainerColor: Color
)
