package pro.jayeshseth.bottomsheet

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * State object holding the persistent bottom sheet state and snackbar state for [CustomBottomSheetScaffold].
 */
@Stable
class BottomSheetScaffoldState(
    val bottomSheetState: BottomSheetState,
    val snackbarHostState: SnackbarHostState
)

/**
 * Creates and remembers a [BottomSheetScaffoldState].
 */
@Composable
fun rememberBottomSheetScaffoldState(
    bottomSheetState: BottomSheetState = rememberBottomSheetState(
        initialValue = SheetValue.Collapsed,
        skipHiddenState = true
    ),
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() }
): BottomSheetScaffoldState = remember(bottomSheetState, snackbarHostState) {
    BottomSheetScaffoldState(bottomSheetState, snackbarHostState)
}

/**
 * A persistent Bottom Sheet Scaffold where the sheet rests continuously at [peekHeight]
 * while coordinating layout and gestures alongside primary screen content.
 */
@Composable
fun CustomBottomSheetScaffold(
    sheetContent: @Composable ColumnScope.() -> Unit,
    modifier: Modifier = Modifier,
    scaffoldState: BottomSheetScaffoldState = rememberBottomSheetScaffoldState(),
    topBar: @Composable (() -> Unit)? = null,
    snackbarHost: @Composable (SnackbarHostState) -> Unit = { SnackbarHost(it) },
    peekHeight: SheetDimension = SheetDimension.Dp(72.dp),
    maxHeight: SheetDimension = SheetDimension.Fraction(0.92f),
    partiallyExpandedHeight: SheetDimension? = null,
    swipeToDismissEnabled: Boolean = false,
    shape: Shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
    containerColor: Color = MaterialTheme.colorScheme.surface,
    contentColor: Color = contentColorFor(containerColor),
    tonalElevation: Dp = 2.dp,
    dragHandle: @Composable (() -> Unit)? = { DefaultBottomSheetDragHandle() },
    animationConfig: SheetAnimationConfig? = null,
    statusBarTintEnabled: Boolean = false,
    sheetModifier: Modifier = Modifier,
    content: @Composable (PaddingValues) -> Unit
) {
    val density = LocalDensity.current
    val sheetState = scaffoldState.bottomSheetState
    if (animationConfig != null) {
        sheetState.animationConfig = animationConfig
    }
    sheetState.swipeToDismissEnabled = swipeToDismissEnabled

    val peekDp = when (peekHeight) {
        is SheetDimension.Dp -> peekHeight.value
        else -> 72.dp
    }

    Box(modifier = modifier.fillMaxSize()) {
        // Base Content Layer
        Column(modifier = Modifier.fillMaxSize()) {
            if (topBar != null) {
                topBar()
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                content(PaddingValues(bottom = peekDp))
            }
        }

        // Persistent Sheet Layer
        BottomSheetLayout(
            sheetState = sheetState,
            modifier = Modifier.fillMaxSize(),
            peekHeight = peekHeight,
            maxHeight = maxHeight,
            partiallyExpandedHeight = partiallyExpandedHeight,
            onDismissRequest = null,
            showScrim = false,
            shape = shape,
            containerColor = containerColor,
            contentColor = contentColor,
            tonalElevation = tonalElevation,
            dragHandle = dragHandle,
            statusBarTintEnabled = statusBarTintEnabled,
            predictiveBackEnabled = false,
            sheetModifier = sheetModifier,
            content = sheetContent
        )

        // Floating Snackbar Layer
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = peekDp + 16.dp)
        ) {
            snackbarHost(scaffoldState.snackbarHostState)
        }
    }
}
