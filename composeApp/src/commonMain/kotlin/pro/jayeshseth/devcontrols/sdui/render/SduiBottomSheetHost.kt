package pro.jayeshseth.devcontrols.sdui.render

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import pro.jayeshseth.devcontrols.sdui.model.SduiBottomSheetSpec

/**
 * Server-driven host for rendering a Material 3 [ModalBottomSheet] controlled by
 * the SDUI document specification and reactive state.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SduiBottomSheetHost(
    sheet: SduiBottomSheetSpec?,
    modifier: Modifier = Modifier
) {
    if (sheet == null) return

    val context = LocalSduiContext.current
    val isOpen = context.store.evalBoolean(sheet.isOpenExpr, false)

    if (isOpen) {
        val sheetState = rememberModalBottomSheetState(
            skipPartiallyExpanded = sheet.skipPartiallyExpanded
        )

        val containerColor = sheet.containerColor?.let { SduiColorParser.parse(it) }
            ?: BottomSheetDefaults.ContainerColor
        val scrimColor = sheet.scrimColor?.let { SduiColorParser.parse(it) }
            ?: BottomSheetDefaults.ScrimColor

        ModalBottomSheet(
            onDismissRequest = {
                if (sheet.onDismissActions.isNotEmpty()) {
                    context.actionRunner.execute(sheet.onDismissActions)
                }
                context.store.set("isBottomSheetOpen", false)
                context.store.set("${sheet.id}_isOpen", false)
                context.store.set(sheet.id, false)
            },
            sheetState = sheetState,
            shape = RoundedCornerShape(
                topStart = sheet.cornerRadiusDp.dp,
                topEnd = sheet.cornerRadiusDp.dp
            ),
            containerColor = containerColor,
            scrimColor = scrimColor,
            dragHandle = if (sheet.dragHandleVisible) {
                { BottomSheetDefaults.DragHandle() }
            } else null,
            modifier = modifier
        ) {
            SduiNodeRenderer(sheet.content)
        }
    }
}
