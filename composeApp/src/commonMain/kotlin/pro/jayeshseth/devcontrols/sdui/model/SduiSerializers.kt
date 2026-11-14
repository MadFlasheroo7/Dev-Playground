package pro.jayeshseth.devcontrols.sdui.model

import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject

internal fun <T : Any> serializeWithDiscriminator(
    encoder: Encoder,
    value: T,
    type: String,
    serializer: KSerializer<T>
) {
    if (encoder is JsonEncoder) {
        val element = encoder.json.encodeToJsonElement(serializer, value)
        val map = LinkedHashMap<String, JsonElement>()
        map["type"] = JsonPrimitive(type)
        if (element is JsonObject) {
            map.putAll(element)
        }
        encoder.encodeJsonElement(JsonObject(map))
    } else {
        serializer.serialize(encoder, value)
    }
}

private fun JsonElement.extractType(): String? =
    ((this as? JsonObject)?.get("type") as? JsonPrimitive)?.content

object SduiNodeSerializer : KSerializer<SduiNode> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("pro.jayeshseth.devcontrols.sdui.model.SduiNode")

    fun selectDeserializer(element: JsonElement): DeserializationStrategy<SduiNode> {
        val type = element.extractType()?.lowercase()
        return when (type) {
            "box" -> SduiNode.Box.serializer()
            "column" -> SduiNode.Column.serializer()
            "row" -> SduiNode.Row.serializer()
            "lazycolumn", "lazy_column" -> SduiNode.LazyColumn.serializer()
            "lazyrow", "lazy_row" -> SduiNode.LazyRow.serializer()
            "lazygrid", "lazyverticalgrid", "lazy_grid", "lazy_vertical_grid" -> SduiNode.LazyVerticalGrid.serializer()
            "text" -> SduiNode.Text.serializer()
            "image" -> SduiNode.Image.serializer()
            "canvas" -> SduiNode.Canvas.serializer()
            "spacer" -> SduiNode.Spacer.serializer()
            "textfield", "text_field" -> SduiNode.TextField.serializer()
            "button" -> SduiNode.Button.serializer()
            "card" -> SduiNode.Card.serializer()
            "animatedvisibility", "animated_visibility" -> SduiNode.AnimatedVisibility.serializer()
            "custom" -> SduiNode.Custom.serializer()
            else -> SduiNode.Unknown.serializer()
        }
    }

    override fun deserialize(decoder: Decoder): SduiNode {
        val input = decoder as? JsonDecoder ?: error("Can only deserialize JSON")
        val tree = input.decodeJsonElement()
        val rawType = tree.extractType()
        val serializer = selectDeserializer(tree)
        return if (serializer == SduiNode.Unknown.serializer() && rawType != null && tree is JsonObject) {
            val map = LinkedHashMap(tree)
            map["rawType"] = JsonPrimitive(rawType)
            input.json.decodeFromJsonElement(serializer, JsonObject(map))
        } else {
            input.json.decodeFromJsonElement(serializer, tree)
        }
    }

    override fun serialize(encoder: Encoder, value: SduiNode) {
        val (serializer, type) = when (value) {
            is SduiNode.Box -> SduiNode.Box.serializer() to "box"
            is SduiNode.Column -> SduiNode.Column.serializer() to "column"
            is SduiNode.Row -> SduiNode.Row.serializer() to "row"
            is SduiNode.LazyColumn -> SduiNode.LazyColumn.serializer() to "lazyColumn"
            is SduiNode.LazyRow -> SduiNode.LazyRow.serializer() to "lazyRow"
            is SduiNode.LazyVerticalGrid -> SduiNode.LazyVerticalGrid.serializer() to "lazyGrid"
            is SduiNode.Text -> SduiNode.Text.serializer() to "text"
            is SduiNode.Image -> SduiNode.Image.serializer() to "image"
            is SduiNode.Canvas -> SduiNode.Canvas.serializer() to "canvas"
            is SduiNode.Spacer -> SduiNode.Spacer.serializer() to "spacer"
            is SduiNode.TextField -> SduiNode.TextField.serializer() to "textField"
            is SduiNode.Button -> SduiNode.Button.serializer() to "button"
            is SduiNode.Card -> SduiNode.Card.serializer() to "card"
            is SduiNode.AnimatedVisibility -> SduiNode.AnimatedVisibility.serializer() to "animatedVisibility"
            is SduiNode.Custom -> SduiNode.Custom.serializer() to "custom"
            is SduiNode.Unknown -> SduiNode.Unknown.serializer() to value.rawType
        }
        @Suppress("UNCHECKED_CAST")
        serializeWithDiscriminator(encoder, value, type, serializer as KSerializer<SduiNode>)
    }
}

object SduiModifierSerializer : KSerializer<SduiModifier> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("pro.jayeshseth.devcontrols.sdui.model.SduiModifier")

    fun selectDeserializer(element: JsonElement): DeserializationStrategy<SduiModifier> {
        val type = element.extractType()?.lowercase()
        return when (type) {
            "padding" -> SduiModifier.Padding.serializer()
            "size" -> SduiModifier.Size.serializer()
            "background" -> SduiModifier.Background.serializer()
            "border" -> SduiModifier.Border.serializer()
            "clip" -> SduiModifier.Clip.serializer()
            "shadow" -> SduiModifier.Shadow.serializer()
            "blur" -> SduiModifier.Blur.serializer()
            "graphicslayer", "graphics_layer" -> SduiModifier.GraphicsLayer.serializer()
            "clickable" -> SduiModifier.Clickable.serializer()
            "gesturedrag", "gesture_drag" -> SduiModifier.GestureDrag.serializer()
            "animatecontentsize", "animate_content_size" -> SduiModifier.AnimateContentSize.serializer()
            else -> SduiModifier.Unknown.serializer()
        }
    }

    override fun deserialize(decoder: Decoder): SduiModifier {
        val input = decoder as? JsonDecoder ?: error("Can only deserialize JSON")
        val tree = input.decodeJsonElement()
        val rawType = tree.extractType()
        val serializer = selectDeserializer(tree)
        return if (serializer == SduiModifier.Unknown.serializer() && rawType != null && tree is JsonObject) {
            val map = LinkedHashMap(tree)
            map["rawType"] = JsonPrimitive(rawType)
            input.json.decodeFromJsonElement(serializer, JsonObject(map))
        } else {
            input.json.decodeFromJsonElement(serializer, tree)
        }
    }

    override fun serialize(encoder: Encoder, value: SduiModifier) {
        val (serializer, type) = when (value) {
            is SduiModifier.Padding -> SduiModifier.Padding.serializer() to "padding"
            is SduiModifier.Size -> SduiModifier.Size.serializer() to "size"
            is SduiModifier.Background -> SduiModifier.Background.serializer() to "background"
            is SduiModifier.Border -> SduiModifier.Border.serializer() to "border"
            is SduiModifier.Clip -> SduiModifier.Clip.serializer() to "clip"
            is SduiModifier.Shadow -> SduiModifier.Shadow.serializer() to "shadow"
            is SduiModifier.Blur -> SduiModifier.Blur.serializer() to "blur"
            is SduiModifier.GraphicsLayer -> SduiModifier.GraphicsLayer.serializer() to "graphicsLayer"
            is SduiModifier.Clickable -> SduiModifier.Clickable.serializer() to "clickable"
            is SduiModifier.GestureDrag -> SduiModifier.GestureDrag.serializer() to "gestureDrag"
            is SduiModifier.AnimateContentSize -> SduiModifier.AnimateContentSize.serializer() to "animateContentSize"
            is SduiModifier.Unknown -> SduiModifier.Unknown.serializer() to value.rawType
        }
        @Suppress("UNCHECKED_CAST")
        serializeWithDiscriminator(encoder, value, type, serializer as KSerializer<SduiModifier>)
    }
}

object SduiActionSerializer : KSerializer<SduiAction> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("pro.jayeshseth.devcontrols.sdui.model.SduiAction")

    fun selectDeserializer(element: JsonElement): DeserializationStrategy<SduiAction> {
        val type = element.extractType()?.lowercase()
        return when (type) {
            "setstate", "set_state" -> SduiAction.SetState.serializer()
            "togglestate", "toggle_state" -> SduiAction.ToggleState.serializer()
            "animateto", "animate_to" -> SduiAction.AnimateTo.serializer()
            "navigate" -> SduiAction.Navigate.serializer()
            "apirequest", "api_request" -> SduiAction.ApiRequest.serializer()
            "emitnativeevent", "emit_native_event" -> SduiAction.EmitNativeEvent.serializer()
            "conditional" -> SduiAction.Conditional.serializer()
            "chain" -> SduiAction.Chain.serializer()
            "openbottomsheet", "open_bottom_sheet" -> SduiAction.OpenBottomSheet.serializer()
            "closebottomsheet", "close_bottom_sheet" -> SduiAction.CloseBottomSheet.serializer()
            else -> SduiAction.Unknown.serializer()
        }
    }

    override fun deserialize(decoder: Decoder): SduiAction {
        val input = decoder as? JsonDecoder ?: error("Can only deserialize JSON")
        val tree = input.decodeJsonElement()
        val rawType = tree.extractType()
        val serializer = selectDeserializer(tree)
        return if (serializer == SduiAction.Unknown.serializer() && rawType != null && tree is JsonObject) {
            val map = LinkedHashMap(tree)
            map["rawType"] = JsonPrimitive(rawType)
            input.json.decodeFromJsonElement(serializer, JsonObject(map))
        } else {
            input.json.decodeFromJsonElement(serializer, tree)
        }
    }

    override fun serialize(encoder: Encoder, value: SduiAction) {
        val (serializer, type) = when (value) {
            is SduiAction.SetState -> SduiAction.SetState.serializer() to "setState"
            is SduiAction.ToggleState -> SduiAction.ToggleState.serializer() to "toggleState"
            is SduiAction.AnimateTo -> SduiAction.AnimateTo.serializer() to "animateTo"
            is SduiAction.Navigate -> SduiAction.Navigate.serializer() to "navigate"
            is SduiAction.ApiRequest -> SduiAction.ApiRequest.serializer() to "apiRequest"
            is SduiAction.EmitNativeEvent -> SduiAction.EmitNativeEvent.serializer() to "emitNativeEvent"
            is SduiAction.Conditional -> SduiAction.Conditional.serializer() to "conditional"
            is SduiAction.Chain -> SduiAction.Chain.serializer() to "chain"
            is SduiAction.OpenBottomSheet -> SduiAction.OpenBottomSheet.serializer() to "openBottomSheet"
            is SduiAction.CloseBottomSheet -> SduiAction.CloseBottomSheet.serializer() to "closeBottomSheet"
            is SduiAction.Unknown -> SduiAction.Unknown.serializer() to value.rawType
        }
        @Suppress("UNCHECKED_CAST")
        serializeWithDiscriminator(encoder, value, type, serializer as KSerializer<SduiAction>)
    }
}

object SduiCanvasOpSerializer : KSerializer<SduiCanvasOp> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("pro.jayeshseth.devcontrols.sdui.model.SduiCanvasOp")

    fun selectDeserializer(element: JsonElement): DeserializationStrategy<SduiCanvasOp> {
        val type = element.extractType()?.lowercase()
        return when (type) {
            "rect" -> SduiCanvasOp.Rect.serializer()
            "circle" -> SduiCanvasOp.Circle.serializer()
            "line" -> SduiCanvasOp.Line.serializer()
            "arc" -> SduiCanvasOp.Arc.serializer()
            "rotate" -> SduiCanvasOp.Rotate.serializer()
            "translate" -> SduiCanvasOp.Translate.serializer()
            else -> SduiCanvasOp.Unknown.serializer()
        }
    }

    override fun deserialize(decoder: Decoder): SduiCanvasOp {
        val input = decoder as? JsonDecoder ?: error("Can only deserialize JSON")
        val tree = input.decodeJsonElement()
        val rawType = tree.extractType()
        val serializer = selectDeserializer(tree)
        return if (serializer == SduiCanvasOp.Unknown.serializer() && rawType != null && tree is JsonObject) {
            val map = LinkedHashMap(tree)
            map["rawType"] = JsonPrimitive(rawType)
            input.json.decodeFromJsonElement(serializer, JsonObject(map))
        } else {
            input.json.decodeFromJsonElement(serializer, tree)
        }
    }

    override fun serialize(encoder: Encoder, value: SduiCanvasOp) {
        val (serializer, type) = when (value) {
            is SduiCanvasOp.Rect -> SduiCanvasOp.Rect.serializer() to "rect"
            is SduiCanvasOp.Circle -> SduiCanvasOp.Circle.serializer() to "circle"
            is SduiCanvasOp.Line -> SduiCanvasOp.Line.serializer() to "line"
            is SduiCanvasOp.Arc -> SduiCanvasOp.Arc.serializer() to "arc"
            is SduiCanvasOp.Rotate -> SduiCanvasOp.Rotate.serializer() to "rotate"
            is SduiCanvasOp.Translate -> SduiCanvasOp.Translate.serializer() to "translate"
            is SduiCanvasOp.Unknown -> SduiCanvasOp.Unknown.serializer() to value.rawType
        }
        @Suppress("UNCHECKED_CAST")
        serializeWithDiscriminator(encoder, value, type, serializer as KSerializer<SduiCanvasOp>)
    }
}

object SduiAnimationSpecSerializer : KSerializer<SduiAnimationSpec> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("pro.jayeshseth.devcontrols.sdui.model.SduiAnimationSpec")

    fun selectDeserializer(element: JsonElement): DeserializationStrategy<SduiAnimationSpec> {
        val type = element.extractType()?.lowercase()
        return when (type) {
            "spring" -> SduiAnimationSpec.Spring.serializer()
            "tween" -> SduiAnimationSpec.Tween.serializer()
            "keyframes" -> SduiAnimationSpec.Keyframes.serializer()
            "repeat", "repeatable" -> SduiAnimationSpec.Repeatable.serializer()
            "snap" -> SduiAnimationSpec.Snap.serializer()
            else -> SduiAnimationSpec.Spring.serializer()
        }
    }

    override fun deserialize(decoder: Decoder): SduiAnimationSpec {
        val input = decoder as? JsonDecoder ?: error("Can only deserialize JSON")
        val tree = input.decodeJsonElement()
        val serializer = selectDeserializer(tree)
        return input.json.decodeFromJsonElement(serializer, tree)
    }

    override fun serialize(encoder: Encoder, value: SduiAnimationSpec) {
        val (serializer, type) = when (value) {
            is SduiAnimationSpec.Spring -> SduiAnimationSpec.Spring.serializer() to "spring"
            is SduiAnimationSpec.Tween -> SduiAnimationSpec.Tween.serializer() to "tween"
            is SduiAnimationSpec.Keyframes -> SduiAnimationSpec.Keyframes.serializer() to "keyframes"
            is SduiAnimationSpec.Repeatable -> SduiAnimationSpec.Repeatable.serializer() to "repeat"
            is SduiAnimationSpec.Snap -> SduiAnimationSpec.Snap.serializer() to "snap"
        }
        @Suppress("UNCHECKED_CAST")
        serializeWithDiscriminator(encoder, value, type, serializer as KSerializer<SduiAnimationSpec>)
    }
}

/**
 * Standard configured JSON parser instance for SDUI.
 */
val defaultSduiJson = Json {
    ignoreUnknownKeys = true
    isLenient = true
    prettyPrint = true
    encodeDefaults = true
    coerceInputValues = true
}
