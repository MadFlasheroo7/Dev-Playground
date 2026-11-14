package pro.jayeshseth.devcontrols.sdui.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Draw operations for Server-Driven Canvas rendering.
 */
@Serializable(with = SduiCanvasOpSerializer::class)
sealed interface SduiCanvasOp {

    @Serializable
    @SerialName("rect")
    data class Rect(
        val color: String = "#FF000000",
        val x: String = "0",
        val y: String = "0",
        val width: String = "100",
        val height: String = "100",
        val cornerRadius: Float = 0f,
        val isFill: Boolean = true,
        val strokeWidth: Float = 2f
    ) : SduiCanvasOp

    @Serializable
    @SerialName("circle")
    data class Circle(
        val color: String = "#FF000000",
        val centerX: String = "50",
        val centerY: String = "50",
        val radius: String = "40",
        val isFill: Boolean = true,
        val strokeWidth: Float = 2f
    ) : SduiCanvasOp

    @Serializable
    @SerialName("line")
    data class Line(
        val color: String = "#FF000000",
        val startX: String = "0",
        val startY: String = "0",
        val endX: String = "100",
        val endY: String = "100",
        val strokeWidth: Float = 3f,
        /** "round", "butt", "square" */
        val cap: String = "round"
    ) : SduiCanvasOp

    @Serializable
    @SerialName("arc")
    data class Arc(
        val color: String = "#FF000000",
        val x: String = "0",
        val y: String = "0",
        val width: String = "100",
        val height: String = "100",
        val startAngle: String = "0",
        val sweepAngle: String = "90",
        val useCenter: Boolean = false,
        val isFill: Boolean = false,
        val strokeWidth: Float = 3f
    ) : SduiCanvasOp

    @Serializable
    @SerialName("rotate")
    data class Rotate(
        val degrees: String = "0",
        val pivotX: String? = null,
        val pivotY: String? = null,
        val ops: List<SduiCanvasOp> = emptyList()
    ) : SduiCanvasOp

    @Serializable
    @SerialName("translate")
    data class Translate(
        val x: String = "0",
        val y: String = "0",
        val ops: List<SduiCanvasOp> = emptyList()
    ) : SduiCanvasOp

    @Serializable
    @SerialName("unknown")
    data class Unknown(
        val rawType: String = ""
    ) : SduiCanvasOp
}
