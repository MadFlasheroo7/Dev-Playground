package pro.jayeshseth.bottomsheet

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

@Composable
actual fun SheetStatusBarTintEffect(
    enabled: Boolean,
    isAtStatusBar: Boolean,
    sheetContainerColor: Color
) {
    // Desktop platforms do not have mobile status bars
}
