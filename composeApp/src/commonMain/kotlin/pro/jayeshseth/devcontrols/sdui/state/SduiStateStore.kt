package pro.jayeshseth.devcontrols.sdui.state

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.SnapshotStateMap
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.longOrNull
import pro.jayeshseth.devcontrols.sdui.eval.SduiExpressionEvaluator

/**
 * Reactive state store powering dynamic SDUI expressions, animations, and state updates.
 * Backed by Compose's [SnapshotStateMap] so reads inside composables automatically track
 * and trigger minimal recompositions.
 */
class SduiStateStore(
    initialData: Map<String, Any?> = emptyMap()
) {
    val state: SnapshotStateMap<String, Any?> = mutableStateMapOf<String, Any?>().apply {
        putAll(initialData)
    }

    fun get(key: String): Any? = state[key]

    fun set(key: String, value: Any?) {
        state[key] = value
    }

    fun toggle(key: String) {
        val curr = state[key]
        state[key] = !SduiExpressionEvaluator.isTruthy(curr)
    }

    fun remove(key: String) {
        state.remove(key)
    }

    fun clear() {
        state.clear()
    }

    fun loadFromRaw(map: Map<String, Any?>) {
        state.clear()
        state.putAll(map)
    }

    fun loadFromJsonElements(map: Map<String, JsonElement>) {
        state.clear()
        map.forEach { (k, v) ->
            state[k] = jsonElementToAny(v)
        }
    }

    fun eval(expr: String?): Any? {
        return SduiExpressionEvaluator.evaluate(expr, state)
    }

    fun evalString(expr: String?, defaultValue: String = ""): String {
        return SduiExpressionEvaluator.evaluateString(expr, state, defaultValue)
    }

    fun evalFloat(expr: String?, defaultValue: Float = 0f): Float {
        return SduiExpressionEvaluator.evaluateFloat(expr, state, defaultValue)
    }

    fun evalDouble(expr: String?, defaultValue: Double = 0.0): Double {
        return SduiExpressionEvaluator.evaluateDouble(expr, state, defaultValue)
    }

    fun evalBoolean(expr: String?, defaultValue: Boolean = false): Boolean {
        return SduiExpressionEvaluator.evaluateBoolean(expr, state, defaultValue)
    }

    fun snapshot(): Map<String, Any?> = state.toMap()

    companion object {
        fun jsonElementToAny(element: JsonElement): Any? {
            return when (element) {
                is JsonNull -> null
                is JsonPrimitive -> {
                    element.booleanOrNull
                        ?: element.longOrNull
                        ?: element.doubleOrNull
                        ?: element.content
                }
                is JsonArray -> element.map { jsonElementToAny(it) }
                is JsonObject -> element.mapValues { jsonElementToAny(it.value) }
            }
        }
    }
}

/**
 * Remembers a reactive [SduiStateStore] with the given initial state map.
 */
@Composable
fun rememberSduiStateStore(initialState: Map<String, JsonElement> = emptyMap()): SduiStateStore {
    return remember(initialState) {
        SduiStateStore().apply {
            loadFromJsonElements(initialState)
        }
    }
}
