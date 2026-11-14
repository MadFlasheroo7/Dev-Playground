package pro.jayeshseth.devcontrols.sdui.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Polymorphic Actions executable upon events (click, drag, lifecycle, responses).
 */
@Serializable(with = SduiActionSerializer::class)
sealed interface SduiAction {

    @Serializable
    @SerialName("setState")
    data class SetState(
        val key: String,
        /** Dynamic expression evaluated against current state, e.g. "count + 1", "'active'", "true" */
        val valueExpr: String
    ) : SduiAction

    @Serializable
    @SerialName("toggleState")
    data class ToggleState(
        val key: String
    ) : SduiAction

    @Serializable
    @SerialName("animateTo")
    data class AnimateTo(
        val key: String,
        /** Target value expression, e.g. "isOpen ? 1.0 : 0.0" or "360" */
        val targetValueExpr: String,
        val animation: SduiAnimationSpec = SduiAnimationSpec.Spring()
    ) : SduiAction

    @Serializable
    @SerialName("navigate")
    data class Navigate(
        val destination: String,
        val isExternal: Boolean = false
    ) : SduiAction

    @Serializable
    @SerialName("apiRequest")
    data class ApiRequest(
        val url: String,
        val method: String = "GET",
        val headers: Map<String, String> = emptyMap(),
        val body: String? = null,
        val responseStateKey: String? = null,
        val onResponse: List<SduiAction> = emptyList(),
        val onError: List<SduiAction> = emptyList()
    ) : SduiAction

    @Serializable
    @SerialName("emitNativeEvent")
    data class EmitNativeEvent(
        val eventName: String,
        val payload: Map<String, String> = emptyMap()
    ) : SduiAction

    @Serializable
    @SerialName("conditional")
    data class Conditional(
        val conditionExpr: String,
        val thenActions: List<SduiAction> = emptyList(),
        val elseActions: List<SduiAction> = emptyList()
    ) : SduiAction

    @Serializable
    @SerialName("chain")
    data class Chain(
        val actions: List<SduiAction> = emptyList()
    ) : SduiAction

    @Serializable
    @SerialName("openBottomSheet")
    data class OpenBottomSheet(
        val sheetId: String = "sheet"
    ) : SduiAction

    @Serializable
    @SerialName("closeBottomSheet")
    data class CloseBottomSheet(
        val sheetId: String = "sheet"
    ) : SduiAction

    @Serializable
    @SerialName("unknown")
    data class Unknown(
        val rawType: String = ""
    ) : SduiAction
}
