package pro.jayeshseth.devcontrols.sdui.dsl

import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import pro.jayeshseth.devcontrols.sdui.model.SduiAction
import pro.jayeshseth.devcontrols.sdui.model.SduiAnimationSpec
import pro.jayeshseth.devcontrols.sdui.model.SduiModifier
import pro.jayeshseth.devcontrols.sdui.model.SduiModifierSerializer
import pro.jayeshseth.devcontrols.sdui.model.defaultSduiJson

/**
 * Type-safe builder for Compose SDUI modifier chains.
 */
@SduiDsl
class SduiModifierBuilder {
    private val modifiers = mutableListOf<SduiModifier>()

    fun add(modifier: SduiModifier): SduiModifierBuilder {
        modifiers.add(modifier)
        return this
    }

    fun addAll(elements: Collection<SduiModifier>): SduiModifierBuilder {
        modifiers.addAll(elements)
        return this
    }

    // --- Padding ---

    fun padding(all: Float) = add(SduiModifier.Padding(all = all.toString()))
    fun padding(all: Int) = padding(all.toFloat())
    fun padding(all: String) = add(SduiModifier.Padding(all = all))

    fun padding(horizontal: Float = 0f, vertical: Float = 0f) =
        add(SduiModifier.Padding(horizontal = horizontal.toString(), vertical = vertical.toString()))

    fun padding(horizontal: Int, vertical: Int) =
        padding(horizontal.toFloat(), vertical.toFloat())

    fun padding(horizontal: String? = null, vertical: String? = null) =
        add(SduiModifier.Padding(horizontal = horizontal, vertical = vertical))

    fun padding(start: Float = 0f, top: Float = 0f, end: Float = 0f, bottom: Float = 0f) =
        add(
            SduiModifier.Padding(
                start = start.toString(),
                top = top.toString(),
                end = end.toString(),
                bottom = bottom.toString()
            )
        )

    fun padding(start: Int, top: Int, end: Int, bottom: Int) =
        padding(start.toFloat(), top.toFloat(), end.toFloat(), bottom.toFloat())

    fun padding(start: String? = null, top: String? = null, end: String? = null, bottom: String? = null) =
        add(SduiModifier.Padding(start = start, top = top, end = end, bottom = bottom))

    // --- Size ---

    fun size(width: Float, height: Float) =
        add(SduiModifier.Size(width = width.toString(), height = height.toString()))

    fun size(size: Float) = size(size, size)
    fun size(size: Int) = size(size.toFloat(), size.toFloat())
    fun size(width: Int, height: Int) = size(width.toFloat(), height.toFloat())
    fun size(sizeExpr: String) = size(width = sizeExpr, height = sizeExpr)

    fun size(
        width: String? = null,
        height: String? = null,
        minWidth: String? = null,
        maxWidth: String? = null,
        minHeight: String? = null,
        maxHeight: String? = null,
        fillMaxWidth: String? = null,
        fillMaxHeight: String? = null,
        fillMaxSize: String? = null,
        aspectRatio: String? = null,
        weight: String? = null
    ) = add(
        SduiModifier.Size(
            width = width,
            height = height,
            minWidth = minWidth,
            maxWidth = maxWidth,
            minHeight = minHeight,
            maxHeight = maxHeight,
            fillMaxWidth = fillMaxWidth,
            fillMaxHeight = fillMaxHeight,
            fillMaxSize = fillMaxSize,
            aspectRatio = aspectRatio,
            weight = weight
        )
    )

    fun width(width: Float) = add(SduiModifier.Size(width = width.toString()))
    fun width(width: String) = add(SduiModifier.Size(width = width))

    fun height(height: Float) = add(SduiModifier.Size(height = height.toString()))
    fun height(height: String) = add(SduiModifier.Size(height = height))

    fun minWidth(minWidth: Float) = add(SduiModifier.Size(minWidth = minWidth.toString()))
    fun minWidth(minWidth: String) = add(SduiModifier.Size(minWidth = minWidth))

    fun maxWidth(maxWidth: Float) = add(SduiModifier.Size(maxWidth = maxWidth.toString()))
    fun maxWidth(maxWidth: String) = add(SduiModifier.Size(maxWidth = maxWidth))

    fun minHeight(minHeight: Float) = add(SduiModifier.Size(minHeight = minHeight.toString()))
    fun minHeight(minHeight: String) = add(SduiModifier.Size(minHeight = minHeight))

    fun maxHeight(maxHeight: Float) = add(SduiModifier.Size(maxHeight = maxHeight.toString()))
    fun maxHeight(maxHeight: String) = add(SduiModifier.Size(maxHeight = maxHeight))

    fun fillMaxWidth(fraction: Float = 1f) = add(SduiModifier.Size(fillMaxWidth = fraction.toString()))
    fun fillMaxWidth(fractionExpr: String) = add(SduiModifier.Size(fillMaxWidth = fractionExpr))

    fun fillMaxHeight(fraction: Float = 1f) = add(SduiModifier.Size(fillMaxHeight = fraction.toString()))
    fun fillMaxHeight(fractionExpr: String) = add(SduiModifier.Size(fillMaxHeight = fractionExpr))

    fun fillMaxSize(fraction: Float = 1f) = add(SduiModifier.Size(fillMaxSize = fraction.toString()))
    fun fillMaxSize(fractionExpr: String) = add(SduiModifier.Size(fillMaxSize = fractionExpr))

    fun aspectRatio(ratio: Float) = add(SduiModifier.Size(aspectRatio = ratio.toString()))
    fun aspectRatio(ratioExpr: String) = add(SduiModifier.Size(aspectRatio = ratioExpr))

    fun weight(weight: Float) = add(SduiModifier.Size(weight = weight.toString()))
    fun weight(weightExpr: String) = add(SduiModifier.Size(weight = weightExpr))

    // --- Background ---

    fun background(color: String, cornerRadius: Float? = null) =
        add(SduiModifier.Background(color = color, cornerRadius = cornerRadius))

    fun backgroundGradient(
        gradientColors: List<String>,
        gradientAngle: Float = 0f,
        gradientType: String = "linearGradient",
        cornerRadius: Float? = null
    ) = add(
        SduiModifier.Background(
            gradientColors = gradientColors,
            gradientAngle = gradientAngle,
            gradientType = gradientType,
            cornerRadius = cornerRadius
        )
    )

    fun background(
        color: String? = null,
        gradientColors: List<String>? = null,
        gradientAngle: Float = 0f,
        gradientType: String = "solid",
        cornerRadius: Float? = null
    ) = add(
        SduiModifier.Background(
            color = color,
            gradientColors = gradientColors,
            gradientAngle = gradientAngle,
            gradientType = gradientType,
            cornerRadius = cornerRadius
        )
    )

    // --- Border ---

    fun border(
        width: Float = 1f,
        color: String = "#FF888888",
        cornerRadius: Float = 0f,
        shape: String = "rounded"
    ) = add(
        SduiModifier.Border(
            width = width.toString(),
            color = color,
            cornerRadius = cornerRadius.toString(),
            shape = shape
        )
    )

    fun border(
        width: String = "1",
        color: String = "#FF888888",
        cornerRadius: String = "0",
        shape: String = "rounded"
    ) = add(
        SduiModifier.Border(
            width = width,
            color = color,
            cornerRadius = cornerRadius,
            shape = shape
        )
    )

    // --- Clip ---

    fun clip(shape: String = "rounded", cornerRadius: Float = 0f) =
        add(SduiModifier.Clip(shape = shape, cornerRadius = cornerRadius.toString()))

    fun clip(shape: String = "rounded", cornerRadius: String = "0") =
        add(SduiModifier.Clip(shape = shape, cornerRadius = cornerRadius))

    fun clipCircle() = clip(shape = "circle", cornerRadius = "0")

    fun clipRounded(cornerRadius: Float) = clip(shape = "rounded", cornerRadius = cornerRadius)

    // --- Shadow ---

    fun shadow(
        elevation: Float = 4f,
        shape: String = "rounded",
        cornerRadius: Float = 0f,
        clip: Boolean = false,
        ambientColor: String? = null,
        spotColor: String? = null
    ) = add(
        SduiModifier.Shadow(
            elevation = elevation.toString(),
            shape = shape,
            cornerRadius = cornerRadius.toString(),
            clip = clip,
            ambientColor = ambientColor,
            spotColor = spotColor
        )
    )

    fun shadow(
        elevation: String = "4",
        shape: String = "rounded",
        cornerRadius: String = "0",
        clip: Boolean = false,
        ambientColor: String? = null,
        spotColor: String? = null
    ) = add(
        SduiModifier.Shadow(
            elevation = elevation,
            shape = shape,
            cornerRadius = cornerRadius,
            clip = clip,
            ambientColor = ambientColor,
            spotColor = spotColor
        )
    )

    // --- Blur ---

    fun blur(radius: Float = 10f) = add(SduiModifier.Blur(radius = radius.toString()))
    fun blur(radius: String = "10") = add(SduiModifier.Blur(radius = radius))

    // --- GraphicsLayer ---

    fun graphicsLayer(
        scaleX: String? = null,
        scaleY: String? = null,
        rotationX: String? = null,
        rotationY: String? = null,
        rotationZ: String? = null,
        translationX: String? = null,
        translationY: String? = null,
        alpha: String? = null,
        transformOriginX: String? = null,
        transformOriginY: String? = null,
        clip: Boolean = false
    ) = add(
        SduiModifier.GraphicsLayer(
            scaleX = scaleX,
            scaleY = scaleY,
            rotationX = rotationX,
            rotationY = rotationY,
            rotationZ = rotationZ,
            translationX = translationX,
            translationY = translationY,
            alpha = alpha,
            transformOriginX = transformOriginX,
            transformOriginY = transformOriginY,
            clip = clip
        )
    )

    fun graphicsLayer(
        scaleX: Float? = null,
        scaleY: Float? = null,
        rotationZ: Float? = null,
        translationX: Float? = null,
        translationY: Float? = null,
        alpha: Float? = null,
        clip: Boolean = false
    ) = graphicsLayer(
        scaleX = scaleX?.toString(),
        scaleY = scaleY?.toString(),
        rotationZ = rotationZ?.toString(),
        translationX = translationX?.toString(),
        translationY = translationY?.toString(),
        alpha = alpha?.toString(),
        clip = clip
    )

    fun scale(scale: Float) = graphicsLayer(scaleX = scale, scaleY = scale)
    fun scale(scaleExpr: String) = graphicsLayer(scaleX = scaleExpr, scaleY = scaleExpr)

    fun alpha(alpha: Float) = graphicsLayer(alpha = alpha)
    fun alpha(alphaExpr: String) = graphicsLayer(alpha = alphaExpr)

    fun rotation(rotation: Float) = graphicsLayer(rotationZ = rotation)
    fun rotation(rotationExpr: String) = graphicsLayer(rotationZ = rotationExpr)

    // --- Clickable ---

    fun clickable(rippleEnabled: Boolean = true, actions: SduiActionBuilder.() -> Unit): SduiModifierBuilder {
        val actionList = SduiActionBuilder().apply(actions).build()
        return add(SduiModifier.Clickable(actions = actionList, rippleEnabled = rippleEnabled))
    }

    fun clickable(actions: List<SduiAction>, rippleEnabled: Boolean = true): SduiModifierBuilder =
        add(SduiModifier.Clickable(actions = actions, rippleEnabled = rippleEnabled))

    fun clickable(vararg actions: SduiAction, rippleEnabled: Boolean = true): SduiModifierBuilder =
        add(SduiModifier.Clickable(actions = actions.toList(), rippleEnabled = rippleEnabled))

    // --- GestureDrag ---

    fun gestureDrag(
        orientation: String = "horizontal",
        minOffset: Float = -500f,
        maxOffset: Float = 500f,
        targetStateKey: String = "dragOffset",
        snapBack: Boolean = true
    ) = add(
        SduiModifier.GestureDrag(
            orientation = orientation,
            minOffset = minOffset,
            maxOffset = maxOffset,
            targetStateKey = targetStateKey,
            snapBack = snapBack
        )
    )

    // --- AnimateContentSize ---

    fun animateContentSize(animation: SduiAnimationSpec = spring()) =
        add(SduiModifier.AnimateContentSize(animation = animation))

    fun build(): List<SduiModifier> = modifiers.toList()
}

/**
 * Builds a list of SDUI modifiers.
 */
fun sduiModifier(init: SduiModifierBuilder.() -> Unit): List<SduiModifier> =
    SduiModifierBuilder().apply(init).build()

/**
 * Alias for sduiModifier.
 */
fun modifier(init: SduiModifierBuilder.() -> Unit): List<SduiModifier> =
    sduiModifier(init)

/**
 * Serializes a list of [SduiModifier] into a JSON string.
 */
fun List<SduiModifier>.toJson(json: Json = defaultSduiJson): String =
    json.encodeToString(ListSerializer(SduiModifierSerializer), this)

/**
 * Serializes a list of [SduiModifier] into a [JsonElement].
 */
fun List<SduiModifier>.toJsonElement(json: Json = defaultSduiJson): JsonElement =
    json.encodeToJsonElement(ListSerializer(SduiModifierSerializer), this)

/**
 * Serializes a single [SduiModifier] into a JSON string.
 */
fun SduiModifier.toJson(json: Json = defaultSduiJson): String =
    json.encodeToString(SduiModifierSerializer, this)

/**
 * Serializes a single [SduiModifier] into a [JsonElement].
 */
fun SduiModifier.toJsonElement(json: Json = defaultSduiJson): JsonElement =
    json.encodeToJsonElement(SduiModifierSerializer, this)
