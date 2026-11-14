package pro.jayeshseth.devcontrols.sdui.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Core AST node hierarchy for Server-Driven Compose UI.
 */
@Serializable(with = SduiNodeSerializer::class)
sealed interface SduiNode {
    val id: String?
    val modifiers: List<SduiModifier>

    @Serializable
    @SerialName("box")
    data class Box(
        override val id: String? = null,
        override val modifiers: List<SduiModifier> = emptyList(),
        /** "topStart", "topCenter", "topEnd", "centerStart", "center", "centerEnd", "bottomStart", "bottomCenter", "bottomEnd" */
        val contentAlignment: String = "center",
        val children: List<SduiNode> = emptyList()
    ) : SduiNode

    @Serializable
    @SerialName("column")
    data class Column(
        override val id: String? = null,
        override val modifiers: List<SduiModifier> = emptyList(),
        /** "top", "bottom", "center", "spaceBetween", "spaceAround", "spaceEvenly" */
        val verticalArrangement: String = "top",
        val spacingDp: Float = 0f,
        /** "start", "centerHorizontally", "end" */
        val horizontalAlignment: String = "start",
        val children: List<SduiNode> = emptyList()
    ) : SduiNode

    @Serializable
    @SerialName("row")
    data class Row(
        override val id: String? = null,
        override val modifiers: List<SduiModifier> = emptyList(),
        /** "start", "end", "center", "spaceBetween", "spaceAround", "spaceEvenly" */
        val horizontalArrangement: String = "start",
        val spacingDp: Float = 0f,
        /** "top", "centerVertically", "bottom" */
        val verticalAlignment: String = "centerVertically",
        val children: List<SduiNode> = emptyList()
    ) : SduiNode

    @Serializable
    @SerialName("lazyColumn")
    data class LazyColumn(
        override val id: String? = null,
        override val modifiers: List<SduiModifier> = emptyList(),
        val verticalArrangement: String = "top",
        val spacingDp: Float = 0f,
        val contentPaddingDp: Float = 0f,
        val children: List<SduiNode> = emptyList()
    ) : SduiNode

    @Serializable
    @SerialName("lazyRow")
    data class LazyRow(
        override val id: String? = null,
        override val modifiers: List<SduiModifier> = emptyList(),
        val horizontalArrangement: String = "start",
        val spacingDp: Float = 0f,
        val contentPaddingDp: Float = 0f,
        val children: List<SduiNode> = emptyList()
    ) : SduiNode

    @Serializable
    @SerialName("lazyGrid")
    data class LazyVerticalGrid(
        override val id: String? = null,
        override val modifiers: List<SduiModifier> = emptyList(),
        val columns: Int = 2,
        val spacingDp: Float = 8f,
        val contentPaddingDp: Float = 0f,
        val children: List<SduiNode> = emptyList()
    ) : SduiNode

    @Serializable
    @SerialName("text")
    data class Text(
        override val id: String? = null,
        override val modifiers: List<SduiModifier> = emptyList(),
        /** Evaluated with expression interpolation e.g. "Value: ${count}" */
        val text: String,
        val fontSize: Float = 14f,
        /** "normal", "bold", "semiBold", "medium", "light" */
        val fontWeight: String = "normal",
        /** "normal", "italic" */
        val fontStyle: String = "normal",
        val color: String? = null,
        /** "start", "center", "end", "justify" */
        val textAlign: String = "start",
        val maxLines: Int? = null,
        /** "clip", "ellipsis" */
        val overflow: String = "clip"
    ) : SduiNode

    @Serializable
    @SerialName("image")
    data class Image(
        override val id: String? = null,
        override val modifiers: List<SduiModifier> = emptyList(),
        val url: String = "",
        val contentDescription: String? = null,
        /** "fit", "crop", "fillBounds", "inside" */
        val contentScale: String = "fit",
        /** Optional fallback vector icon name (e.g. "star", "heart", "settings", "check", "close", "arrowBack", "refresh", "search") */
        val vectorIcon: String? = null,
        val tintColor: String? = null
    ) : SduiNode

    @Serializable
    @SerialName("canvas")
    data class Canvas(
        override val id: String? = null,
        override val modifiers: List<SduiModifier> = emptyList(),
        val operations: List<SduiCanvasOp> = emptyList()
    ) : SduiNode

    @Serializable
    @SerialName("spacer")
    data class Spacer(
        override val id: String? = null,
        override val modifiers: List<SduiModifier> = emptyList(),
        val width: Float = 0f,
        val height: Float = 0f
    ) : SduiNode

    @Serializable
    @SerialName("textField")
    data class TextField(
        override val id: String? = null,
        override val modifiers: List<SduiModifier> = emptyList(),
        val valueStateKey: String,
        val label: String? = null,
        val placeholder: String? = null,
        val isPassword: Boolean = false,
        /** "text", "number", "email", "phone" */
        val keyboardType: String = "text",
        val maxLines: Int = 1
    ) : SduiNode

    @Serializable
    @SerialName("button")
    data class Button(
        override val id: String? = null,
        override val modifiers: List<SduiModifier> = emptyList(),
        val text: String? = null,
        val onClick: List<SduiAction> = emptyList(),
        /** "filled", "outlined", "elevated", "text" */
        val variant: String = "filled",
        val enabledExpr: String? = null,
        val children: List<SduiNode> = emptyList()
    ) : SduiNode

    @Serializable
    @SerialName("card")
    data class Card(
        override val id: String? = null,
        override val modifiers: List<SduiModifier> = emptyList(),
        val elevation: Float = 2f,
        val cornerRadius: Float = 12f,
        val backgroundColor: String? = null,
        val borderWidth: Float? = null,
        val borderColor: String? = null,
        val onClick: List<SduiAction> = emptyList(),
        val children: List<SduiNode> = emptyList()
    ) : SduiNode

    @Serializable
    @SerialName("animatedVisibility")
    data class AnimatedVisibility(
        override val id: String? = null,
        override val modifiers: List<SduiModifier> = emptyList(),
        /** Dynamic boolean expression, e.g. "isVisible && count > 0" */
        val visibleExpr: String = "true",
        val enter: SduiEnterTransition = SduiEnterTransition(fadeIn = true),
        val exit: SduiExitTransition = SduiExitTransition(fadeOut = true),
        val child: SduiNode
    ) : SduiNode

    @Serializable
    @SerialName("custom")
    data class Custom(
        override val id: String? = null,
        override val modifiers: List<SduiModifier> = emptyList(),
        val componentName: String,
        val props: Map<String, String> = emptyMap(),
        val children: List<SduiNode> = emptyList()
    ) : SduiNode

    @Serializable
    @SerialName("unknown")
    data class Unknown(
        override val id: String? = null,
        override val modifiers: List<SduiModifier> = emptyList(),
        val rawType: String = "unknown"
    ) : SduiNode
}
