package pro.jayeshseth.bottomsheet

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.view.Window
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.WindowInsetsControllerCompat

private fun findWindow(view: android.view.View): Window? {
    var parent = view.parent
    while (parent != null) {
        if (parent is DialogWindowProvider) {
            return parent.window
        }
        parent = parent.parent
    }
    var ctx = view.context
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx.window
        ctx = ctx.baseContext
    }
    return null
}

@Composable
actual fun SheetStatusBarTintEffect(
    enabled: Boolean,
    isAtStatusBar: Boolean,
    sheetContainerColor: Color
) {
    if (!enabled) return
    val view = LocalView.current
    val window = remember(view) { findWindow(view) } ?: return
    val controller = remember(window) { WindowInsetsControllerCompat(window, window.decorView) }
    val initialLightBars = remember(window) { controller.isAppearanceLightStatusBars }

    DisposableEffect(enabled, isAtStatusBar, sheetContainerColor, window) {
        if (isAtStatusBar) {
            val isLightSheet = sheetContainerColor.luminance() >= 0.5f
            controller.isAppearanceLightStatusBars = isLightSheet
        } else {
            controller.isAppearanceLightStatusBars = initialLightBars
        }

        onDispose {
            controller.isAppearanceLightStatusBars = initialLightBars
        }
    }
}
