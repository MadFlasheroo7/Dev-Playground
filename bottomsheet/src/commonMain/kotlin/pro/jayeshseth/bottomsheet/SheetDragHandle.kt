package pro.jayeshseth.bottomsheet

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Visual styling options for bottom sheet drag handles.
 */
enum class SheetDragHandleStyle {
    Pill,
    WidePill,
    ThinBar,
    AccentPill,
    Hidden
}

/**
 * Standard Material-style pill drag handle for the bottom sheet.
 *
 * Provides a comfortable touch target while displaying a clean visual bar.
 */
@Composable
fun DefaultBottomSheetDragHandle(
    modifier: Modifier = Modifier,
    width: Dp = 36.dp,
    height: Dp = 4.dp,
    color: Color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
    verticalPadding: Dp = 12.dp
) {
    Box(
        modifier = modifier
            .padding(vertical = verticalPadding),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(width = width, height = height)
                .clip(RoundedCornerShape(height / 2f))
                .background(color)
        )
    }
}

/**
 * Configurable bottom sheet drag handle supporting presets like [SheetDragHandleStyle.Pill],
 * [SheetDragHandleStyle.WidePill], [SheetDragHandleStyle.ThinBar], [SheetDragHandleStyle.AccentPill],
 * and [SheetDragHandleStyle.Hidden].
 */
@Composable
fun SheetDragHandle(
    style: SheetDragHandleStyle = SheetDragHandleStyle.Pill,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
    verticalPadding: Dp = 12.dp
) {
    if (style == SheetDragHandleStyle.Hidden) return

    val (width, height, resolvedColor) = when (style) {
        SheetDragHandleStyle.Pill -> Triple(36.dp, 4.dp, color)
        SheetDragHandleStyle.WidePill -> Triple(64.dp, 5.dp, color)
        SheetDragHandleStyle.ThinBar -> Triple(48.dp, 2.dp, color.copy(alpha = 0.35f))
        SheetDragHandleStyle.AccentPill -> Triple(40.dp, 4.dp, MaterialTheme.colorScheme.primary)
        SheetDragHandleStyle.Hidden -> return
    }

    DefaultBottomSheetDragHandle(
        modifier = modifier,
        width = width,
        height = height,
        color = resolvedColor,
        verticalPadding = verticalPadding
    )
}
