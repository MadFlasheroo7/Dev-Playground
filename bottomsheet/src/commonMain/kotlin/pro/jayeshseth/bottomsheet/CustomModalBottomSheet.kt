package pro.jayeshseth.bottomsheet

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * An in-tree Compose Multiplatform Modal Bottom Sheet built from scratch.
 *
 * Key features:
 * - Rendered directly within the caller's composition tree by default (or above the entire window
 *   UI via [useWindowPopup]), enabling flawless shared element transitions via [androidx.compose.animation.SharedTransitionScope].
 * - Independent toggles for [swipeToDismissEnabled] and [scrimDismissEnabled].
 * - Customizable entrance, exit, snap, and scrim animation specifications.
 * - Android 14/15+ interactive predictive back scaling, downward translation, and corner morphing.
 * - Dynamic system status bar contrast switching on Android.
 * - Coordinated nested scrolling with scrollable children (e.g. LazyColumn).
 */
@Composable
fun CustomModalBottomSheet(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    sheetState: BottomSheetState = rememberBottomSheetState(initialValue = SheetValue.Hidden),
    peekHeight: SheetDimension = SheetDimension.Dp(0.dp),
    maxHeight: SheetDimension = SheetDimension.Fraction(0.95f),
    partiallyExpandedHeight: SheetDimension? = null,
    swipeToDismissEnabled: Boolean = true,
    scrimDismissEnabled: Boolean = true,
    shape: Shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
    containerColor: Color = MaterialTheme.colorScheme.surface,
    contentColor: Color = contentColorFor(containerColor),
    tonalElevation: Dp = 1.dp,
    scrimColor: Color = Color.Black.copy(alpha = 0.5f),
    dragHandle: @Composable (() -> Unit)? = { DefaultBottomSheetDragHandle() },
    statusBarTintEnabled: Boolean = true,
    predictiveBackEnabled: Boolean = true,
    animationConfig: SheetAnimationConfig? = null,
    sheetModifier: Modifier = Modifier,
    useWindowPopup: Boolean = false,
    content: @Composable ColumnScope.() -> Unit
) {
    if (animationConfig != null) {
        sheetState.animationConfig = animationConfig
    }
    sheetState.swipeToDismissEnabled = swipeToDismissEnabled
    sheetState.scrimDismissEnabled = scrimDismissEnabled

    val sheetContent: @Composable () -> Unit = {
        BottomSheetLayout(
            sheetState = sheetState,
            modifier = modifier,
            peekHeight = peekHeight,
            maxHeight = maxHeight,
            partiallyExpandedHeight = partiallyExpandedHeight,
            onDismissRequest = onDismissRequest,
            showScrim = true,
            scrimColor = scrimColor,
            shape = shape,
            containerColor = containerColor,
            contentColor = contentColor,
            tonalElevation = tonalElevation,
            dragHandle = dragHandle,
            statusBarTintEnabled = statusBarTintEnabled,
            predictiveBackEnabled = predictiveBackEnabled,
            sheetModifier = sheetModifier,
            content = content
        )
    }

    if (useWindowPopup) {
        if (sheetState.isVisible || sheetState.targetValue != SheetValue.Hidden) {
            ModalSheetWindow(
                onDismissRequest = { if (scrimDismissEnabled) onDismissRequest() }
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    sheetContent()
                }
            }
        }
    } else {
        sheetContent()
    }
}
