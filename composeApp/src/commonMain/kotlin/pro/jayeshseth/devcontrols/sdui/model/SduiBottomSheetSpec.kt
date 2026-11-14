package pro.jayeshseth.devcontrols.sdui.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/**
 * Server-driven specification for a dynamic ModalBottomSheet.
 *
 * @property id Unique identifier for the sheet.
 * @property content The UI tree rendered inside the bottom sheet.
 * @property isOpenExpr Reactive boolean expression determining if the sheet is displayed.
 * @property initialState Optional localized initial state scoped to this sheet.
 * @property skipPartiallyExpanded Whether to skip the half-expanded state when opening.
 * @property dragHandleVisible Whether the top drag handle indicator is visible.
 * @property peekHeightDp Optional peek height in dp.
 * @property containerColor Sheet background hex color (e.g. "#1C1B1F").
 * @property scrimColor Backdrop scrim hex color (e.g. "#99000000").
 * @property cornerRadiusDp Top corner radius in dp.
 * @property onDismissActions Actions executed when the sheet is dismissed (swipe down or tap scrim).
 */
@Serializable
data class SduiBottomSheetSpec(
    val id: String = "sheet",
    val content: SduiNode,
    val isOpenExpr: String = "false",
    val initialState: Map<String, JsonElement> = emptyMap(),
    val skipPartiallyExpanded: Boolean = false,
    val dragHandleVisible: Boolean = true,
    val peekHeightDp: Float = 0f,
    val containerColor: String? = null,
    val scrimColor: String? = null,
    val cornerRadiusDp: Float = 24f,
    val onDismissActions: List<SduiAction> = emptyList()
)
