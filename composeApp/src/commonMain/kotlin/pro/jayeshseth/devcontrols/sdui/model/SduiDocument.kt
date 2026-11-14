package pro.jayeshseth.devcontrols.sdui.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/**
 * Top-level root document representing a Server-Driven UI screen or component subtree.
 *
 * @property id Unique identifier for this SDUI view.
 * @property version Schema version for backward/forward compatibility.
 * @property metadata Arbitrary metadata (title, analytics tag, author, etc.).
 * @property initialState Initial key-value reactive state store. Values can be numbers, booleans, strings, objects, or arrays.
 * @property root The root AST node rendered as the UI tree.
 */
@Serializable
data class SduiDocument(
    val id: String = "sdui_root",
    val version: Int = 1,
    val metadata: Map<String, String> = emptyMap(),
    val initialState: Map<String, JsonElement> = emptyMap(),
    val root: SduiNode,
    val bottomSheet: SduiBottomSheetSpec? = null
)
