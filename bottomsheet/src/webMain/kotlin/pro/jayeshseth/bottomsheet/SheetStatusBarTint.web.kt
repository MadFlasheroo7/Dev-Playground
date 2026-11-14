package pro.jayeshseth.bottomsheet

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

@Composable
actual fun SheetStatusBarTintEffect(
    enabled: Boolean,
    isAtStatusBar: Boolean,
    sheetContainerColor: Color
) {
    // Web browsers do not manage system status bars directly
}
