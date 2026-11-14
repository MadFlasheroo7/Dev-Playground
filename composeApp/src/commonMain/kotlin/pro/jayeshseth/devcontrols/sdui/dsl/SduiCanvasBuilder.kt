package pro.jayeshseth.devcontrols.sdui.dsl

import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import pro.jayeshseth.devcontrols.sdui.model.SduiCanvasOp
import pro.jayeshseth.devcontrols.sdui.model.SduiCanvasOpSerializer
import pro.jayeshseth.devcontrols.sdui.model.defaultSduiJson

/**
 * Type-safe DSL builder for Server-Driven Canvas draw operations.
 */
@SduiDsl
class SduiCanvasBuilder {
    private val operations = mutableListOf<SduiCanvasOp>()

    fun add(op: SduiCanvasOp): SduiCanvasBuilder {
        operations.add(op)
        return this
    }

    fun addAll(elements: Collection<SduiCanvasOp>): SduiCanvasBuilder {
        operations.addAll(elements)
        return this
    }

    /**
     * Draws a rectangle on the canvas.
     */
    fun rect(
        color: String = "#FF000000",
        x: Float = 0f,
        y: Float = 0f,
        width: Float = 100f,
        height: Float = 100f,
        cornerRadius: Float = 0f,
        isFill: Boolean = true,
        strokeWidth: Float = 2f
    ) = add(
        SduiCanvasOp.Rect(
            color = color,
            x = x.toString(),
            y = y.toString(),
            width = width.toString(),
            height = height.toString(),
            cornerRadius = cornerRadius,
            isFill = isFill,
            strokeWidth = strokeWidth
        )
    )

    /**
     * Draws a rectangle using dynamic string expressions for coordinates and sizes.
     */
    fun rect(
        color: String = "#FF000000",
        x: String = "0",
        y: String = "0",
        width: String = "100",
        height: String = "100",
        cornerRadius: Float = 0f,
        isFill: Boolean = true,
        strokeWidth: Float = 2f
    ) = add(
        SduiCanvasOp.Rect(
            color = color,
            x = x,
            y = y,
            width = width,
            height = height,
            cornerRadius = cornerRadius,
            isFill = isFill,
            strokeWidth = strokeWidth
        )
    )

    /**
     * Draws a circle on the canvas.
     */
    fun circle(
        color: String = "#FF000000",
        centerX: Float = 50f,
        centerY: Float = 50f,
        radius: Float = 40f,
        isFill: Boolean = true,
        strokeWidth: Float = 2f
    ) = add(
        SduiCanvasOp.Circle(
            color = color,
            centerX = centerX.toString(),
            centerY = centerY.toString(),
            radius = radius.toString(),
            isFill = isFill,
            strokeWidth = strokeWidth
        )
    )

    /**
     * Draws a circle using dynamic string expressions for center coordinates and radius.
     */
    fun circle(
        color: String = "#FF000000",
        centerX: String = "50",
        centerY: String = "50",
        radius: String = "40",
        isFill: Boolean = true,
        strokeWidth: Float = 2f
    ) = add(
        SduiCanvasOp.Circle(
            color = color,
            centerX = centerX,
            centerY = centerY,
            radius = radius,
            isFill = isFill,
            strokeWidth = strokeWidth
        )
    )

    /**
     * Draws a line segment on the canvas.
     */
    fun line(
        color: String = "#FF000000",
        startX: Float = 0f,
        startY: Float = 0f,
        endX: Float = 100f,
        endY: Float = 100f,
        strokeWidth: Float = 3f,
        cap: String = "round"
    ) = add(
        SduiCanvasOp.Line(
            color = color,
            startX = startX.toString(),
            startY = startY.toString(),
            endX = endX.toString(),
            endY = endY.toString(),
            strokeWidth = strokeWidth,
            cap = cap
        )
    )

    /**
     * Draws a line segment using dynamic string expressions.
     */
    fun line(
        color: String = "#FF000000",
        startX: String = "0",
        startY: String = "0",
        endX: String = "100",
        endY: String = "100",
        strokeWidth: Float = 3f,
        cap: String = "round"
    ) = add(
        SduiCanvasOp.Line(
            color = color,
            startX = startX,
            startY = startY,
            endX = endX,
            endY = endY,
            strokeWidth = strokeWidth,
            cap = cap
        )
    )

    /**
     * Draws an arc on the canvas.
     */
    fun arc(
        color: String = "#FF000000",
        x: Float = 0f,
        y: Float = 0f,
        width: Float = 100f,
        height: Float = 100f,
        startAngle: Float = 0f,
        sweepAngle: Float = 90f,
        useCenter: Boolean = false,
        isFill: Boolean = false,
        strokeWidth: Float = 3f
    ) = add(
        SduiCanvasOp.Arc(
            color = color,
            x = x.toString(),
            y = y.toString(),
            width = width.toString(),
            height = height.toString(),
            startAngle = startAngle.toString(),
            sweepAngle = sweepAngle.toString(),
            useCenter = useCenter,
            isFill = isFill,
            strokeWidth = strokeWidth
        )
    )

    /**
     * Draws an arc using dynamic string expressions.
     */
    fun arc(
        color: String = "#FF000000",
        x: String = "0",
        y: String = "0",
        width: String = "100",
        height: String = "100",
        startAngle: String = "0",
        sweepAngle: String = "90",
        useCenter: Boolean = false,
        isFill: Boolean = false,
        strokeWidth: Float = 3f
    ) = add(
        SduiCanvasOp.Arc(
            color = color,
            x = x,
            y = y,
            width = width,
            height = height,
            startAngle = startAngle,
            sweepAngle = sweepAngle,
            useCenter = useCenter,
            isFill = isFill,
            strokeWidth = strokeWidth
        )
    )

    /**
     * Rotates subsequent canvas operations around an optional pivot point.
     */
    fun rotate(
        degrees: Float,
        pivotX: Float? = null,
        pivotY: Float? = null,
        ops: SduiCanvasBuilder.() -> Unit
    ) = add(
        SduiCanvasOp.Rotate(
            degrees = degrees.toString(),
            pivotX = pivotX?.toString(),
            pivotY = pivotY?.toString(),
            ops = SduiCanvasBuilder().apply(ops).build()
        )
    )

    /**
     * Rotates subsequent canvas operations using dynamic expressions.
     */
    fun rotate(
        degrees: String,
        pivotX: String? = null,
        pivotY: String? = null,
        ops: SduiCanvasBuilder.() -> Unit
    ) = add(
        SduiCanvasOp.Rotate(
            degrees = degrees,
            pivotX = pivotX,
            pivotY = pivotY,
            ops = SduiCanvasBuilder().apply(ops).build()
        )
    )

    /**
     * Translates coordinates for subsequent canvas operations.
     */
    fun translate(
        x: Float,
        y: Float,
        ops: SduiCanvasBuilder.() -> Unit
    ) = add(
        SduiCanvasOp.Translate(
            x = x.toString(),
            y = y.toString(),
            ops = SduiCanvasBuilder().apply(ops).build()
        )
    )

    /**
     * Translates coordinates using dynamic expressions.
     */
    fun translate(
        x: String,
        y: String,
        ops: SduiCanvasBuilder.() -> Unit
    ) = add(
        SduiCanvasOp.Translate(
            x = x,
            y = y,
            ops = SduiCanvasBuilder().apply(ops).build()
        )
    )

    fun build(): List<SduiCanvasOp> = operations.toList()
}

/**
 * Convenience builder for canvas draw operations.
 */
fun canvasOperations(init: SduiCanvasBuilder.() -> Unit): List<SduiCanvasOp> =
    SduiCanvasBuilder().apply(init).build()

/**
 * Serializes a list of [SduiCanvasOp] into a JSON string.
 */
fun List<SduiCanvasOp>.toJson(json: Json = defaultSduiJson): String =
    json.encodeToString(ListSerializer(SduiCanvasOpSerializer), this)

/**
 * Serializes a list of [SduiCanvasOp] into a [JsonElement].
 */
fun List<SduiCanvasOp>.toJsonElement(json: Json = defaultSduiJson): JsonElement =
    json.encodeToJsonElement(ListSerializer(SduiCanvasOpSerializer), this)

/**
 * Serializes a single [SduiCanvasOp] into a JSON string.
 */
fun SduiCanvasOp.toJson(json: Json = defaultSduiJson): String =
    json.encodeToString(SduiCanvasOpSerializer, this)

/**
 * Serializes a single [SduiCanvasOp] into a [JsonElement].
 */
fun SduiCanvasOp.toJsonElement(json: Json = defaultSduiJson): JsonElement =
    json.encodeToJsonElement(SduiCanvasOpSerializer, this)
