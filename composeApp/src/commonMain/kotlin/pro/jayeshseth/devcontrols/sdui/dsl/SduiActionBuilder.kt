package pro.jayeshseth.devcontrols.sdui.dsl

import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import pro.jayeshseth.devcontrols.sdui.model.SduiAction
import pro.jayeshseth.devcontrols.sdui.model.SduiActionSerializer
import pro.jayeshseth.devcontrols.sdui.model.SduiAnimationSpec
import pro.jayeshseth.devcontrols.sdui.model.defaultSduiJson

/**
 * Type-safe DSL builder for event actions (onClick, onResponse, etc.).
 */
@SduiDsl
class SduiActionBuilder {
    private val actions = mutableListOf<SduiAction>()

    fun add(action: SduiAction): SduiActionBuilder {
        actions.add(action)
        return this
    }

    fun addAll(elements: Collection<SduiAction>): SduiActionBuilder {
        actions.addAll(elements)
        return this
    }

    /**
     * Updates reactive state via an expression evaluated in the state context.
     * e.g. setState("count", "count + 1") or setState("status", "'active'")
     */
    fun setState(key: String, valueExpr: String) =
        add(SduiAction.SetState(key = key, valueExpr = valueExpr))

    /**
     * Sets reactive state to a numeric constant.
     */
    fun setState(key: String, value: Number) =
        setState(key, value.toString())

    /**
     * Sets reactive state to a boolean constant.
     */
    fun setState(key: String, value: Boolean) =
        setState(key, value.toString())

    /**
     * Sets reactive state to a string literal (automatically wrapped in quotes).
     */
    fun setStateLiteral(key: String, literal: String) =
        setState(key, "'$literal'")

    /**
     * Toggles a boolean reactive state key.
     */
    fun toggleState(key: String) =
        add(SduiAction.ToggleState(key = key))

    /**
     * Smoothly animates a state key to a target value using an animation spec.
     */
    fun animateTo(
        key: String,
        targetValueExpr: String,
        animation: SduiAnimationSpec = spring()
    ) = add(
        SduiAction.AnimateTo(
            key = key,
            targetValueExpr = targetValueExpr,
            animation = animation
        )
    )

    /**
     * Smoothly animates a state key to a numeric value.
     */
    fun animateTo(
        key: String,
        targetValue: Number,
        animation: SduiAnimationSpec = spring()
    ) = animateTo(key, targetValue.toString(), animation)

    /**
     * Navigates to an internal destination route or external deep link / URL.
     */
    fun navigate(destination: String, isExternal: Boolean = false) =
        add(SduiAction.Navigate(destination = destination, isExternal = isExternal))

    /**
     * Dispatches an asynchronous HTTP request with callback action chains.
     */
    fun apiRequest(
        url: String,
        method: String = "GET",
        headers: Map<String, String> = emptyMap(),
        body: String? = null,
        responseStateKey: String? = null,
        onResponse: (SduiActionBuilder.() -> Unit)? = null,
        onError: (SduiActionBuilder.() -> Unit)? = null
    ) = add(
        SduiAction.ApiRequest(
            url = url,
            method = method,
            headers = headers,
            body = body,
            responseStateKey = responseStateKey,
            onResponse = onResponse?.let { SduiActionBuilder().apply(it).build() } ?: emptyList(),
            onError = onError?.let { SduiActionBuilder().apply(it).build() } ?: emptyList()
        )
    )

    /**
     * Emits a native event to the host platform bridge.
     */
    fun emitNativeEvent(eventName: String, payload: Map<String, String> = emptyMap()) =
        add(SduiAction.EmitNativeEvent(eventName = eventName, payload = payload))

    /**
     * Emits a native event with vararg key-value pairs.
     */
    fun emitNativeEvent(eventName: String, vararg payload: Pair<String, String>) =
        emitNativeEvent(eventName = eventName, payload = payload.toMap())

    /**
     * Conditionally executes actions based on an expression.
     */
    fun conditional(
        conditionExpr: String,
        thenActions: SduiActionBuilder.() -> Unit,
        elseActions: (SduiActionBuilder.() -> Unit)? = null
    ) = add(
        SduiAction.Conditional(
            conditionExpr = conditionExpr,
            thenActions = SduiActionBuilder().apply(thenActions).build(),
            elseActions = elseActions?.let { SduiActionBuilder().apply(it).build() } ?: emptyList()
        )
    )

    /**
     * Executes a chain of actions in sequence.
     */
    fun chain(actions: SduiActionBuilder.() -> Unit) =
        add(SduiAction.Chain(actions = SduiActionBuilder().apply(actions).build()))

    /**
     * Opens a server-driven modal bottom sheet.
     */
    fun openBottomSheet(sheetId: String = "sheet") =
        add(SduiAction.OpenBottomSheet(sheetId = sheetId))

    /**
     * Closes a server-driven modal bottom sheet.
     */
    fun closeBottomSheet(sheetId: String = "sheet") =
        add(SduiAction.CloseBottomSheet(sheetId = sheetId))

    fun build(): List<SduiAction> = actions.toList()
}

/**
 * Builds an action list for an onClick handler.
 */
fun onClick(init: SduiActionBuilder.() -> Unit): List<SduiAction> =
    SduiActionBuilder().apply(init).build()

/**
 * Builds an action list.
 */
fun actions(init: SduiActionBuilder.() -> Unit): List<SduiAction> =
    SduiActionBuilder().apply(init).build()

/**
 * Serializes a list of [SduiAction] into a JSON string.
 */
fun List<SduiAction>.toJson(json: Json = defaultSduiJson): String =
    json.encodeToString(ListSerializer(SduiActionSerializer), this)

/**
 * Serializes a list of [SduiAction] into a [JsonElement].
 */
fun List<SduiAction>.toJsonElement(json: Json = defaultSduiJson): JsonElement =
    json.encodeToJsonElement(ListSerializer(SduiActionSerializer), this)

/**
 * Serializes a single [SduiAction] into a JSON string.
 */
fun SduiAction.toJson(json: Json = defaultSduiJson): String =
    json.encodeToString(SduiActionSerializer, this)

/**
 * Serializes a single [SduiAction] into a [JsonElement].
 */
fun SduiAction.toJsonElement(json: Json = defaultSduiJson): JsonElement =
    json.encodeToJsonElement(SduiActionSerializer, this)
