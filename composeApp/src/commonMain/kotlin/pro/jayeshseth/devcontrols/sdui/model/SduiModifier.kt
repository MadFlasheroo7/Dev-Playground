package pro.jayeshseth.devcontrols.sdui.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Polymorphic Modifier declarations for Compose SDUI nodes.
 * String fields support dynamic expressions (e.g. "${isOpen ? 24 : 12}", "${alphaValue}").
 */
@Serializable(with = SduiModifierSerializer::class)
sealed interface SduiModifier {

    @Serializable
    @SerialName("padding")
    data class Padding(
        val all: String? = null,
        val horizontal: String? = null,
        val vertical: String? = null,
        val start: String? = null,
        val top: String? = null,
        val end: String? = null,
        val bottom: String? = null
    ) : SduiModifier

    @Serializable
    @SerialName("size")
    data class Size(
        val width: String? = null,
        val height: String? = null,
        val minWidth: String? = null,
        val maxWidth: String? = null,
        val minHeight: String? = null,
        val maxHeight: String? = null,
        val fillMaxWidth: String? = null,
        val fillMaxHeight: String? = null,
        val fillMaxSize: String? = null,
        val aspectRatio: String? = null,
        val weight: String? = null
    ) : SduiModifier

    @Serializable
    @SerialName("background")
    data class Background(
        val color: String? = null,
        /** Linear gradient colors (hex list) */
        val gradientColors: List<String>? = null,
        /** Gradient angle in degrees (e.g. 0 = left->right, 90 = top->bottom, 45 = diagonal) */
        val gradientAngle: Float = 0f,
        /** "solid", "linearGradient", "radialGradient" */
        val gradientType: String = "solid",
        val cornerRadius: Float? = null
    ) : SduiModifier

    @Serializable
    @SerialName("border")
    data class Border(
        val width: String = "1",
        val color: String = "#FF888888",
        val cornerRadius: String = "0",
        /** "rounded", "circle", "cutCorner" */
        val shape: String = "rounded"
    ) : SduiModifier

    @Serializable
    @SerialName("clip")
    data class Clip(
        /** "rounded", "circle", "cutCorner" */
        val shape: String = "rounded",
        val cornerRadius: String = "0"
    ) : SduiModifier

    @Serializable
    @SerialName("shadow")
    data class Shadow(
        val elevation: String = "4",
        val shape: String = "rounded",
        val cornerRadius: String = "0",
        val clip: Boolean = false,
        val ambientColor: String? = null,
        val spotColor: String? = null
    ) : SduiModifier

    @Serializable
    @SerialName("blur")
    data class Blur(
        val radius: String = "10"
    ) : SduiModifier

    @Serializable
    @SerialName("graphicsLayer")
    data class GraphicsLayer(
        val scaleX: String? = null,
        val scaleY: String? = null,
        val rotationX: String? = null,
        val rotationY: String? = null,
        val rotationZ: String? = null,
        val translationX: String? = null,
        val translationY: String? = null,
        val alpha: String? = null,
        val transformOriginX: String? = null,
        val transformOriginY: String? = null,
        val clip: Boolean = false
    ) : SduiModifier

    @Serializable
    @SerialName("clickable")
    data class Clickable(
        val actions: List<SduiAction> = emptyList(),
        val rippleEnabled: Boolean = true
    ) : SduiModifier

    @Serializable
    @SerialName("gestureDrag")
    data class GestureDrag(
        /** "horizontal", "vertical", "both" */
        val orientation: String = "horizontal",
        val minOffset: Float = -500f,
        val maxOffset: Float = 500f,
        /** State key to sync drag offset into */
        val targetStateKey: String = "dragOffset",
        val snapBack: Boolean = true
    ) : SduiModifier

    @Serializable
    @SerialName("animateContentSize")
    data class AnimateContentSize(
        val animation: SduiAnimationSpec = SduiAnimationSpec.Spring()
    ) : SduiModifier

    @Serializable
    @SerialName("unknown")
    data class Unknown(
        val rawType: String = ""
    ) : SduiModifier
}
