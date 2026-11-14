package pro.jayeshseth.devcontrols.sdui.dsl

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import pro.jayeshseth.devcontrols.sdui.model.SduiDocument
import pro.jayeshseth.devcontrols.sdui.model.SduiNode
import pro.jayeshseth.devcontrols.sdui.model.defaultSduiJson

/**
 * Converts arbitrary Kotlin primitives, collections, and maps into kotlinx.serialization [JsonElement].
 */
fun Any?.toJsonElement(): JsonElement {
    return when (this) {
        null -> JsonNull
        is JsonElement -> this
        is Boolean -> JsonPrimitive(this)
        is Number -> JsonPrimitive(this)
        is String -> JsonPrimitive(this)
        is Char -> JsonPrimitive(this.toString())
        is BooleanArray -> JsonArray(this.map { JsonPrimitive(it) })
        is ByteArray -> JsonArray(this.map { JsonPrimitive(it) })
        is ShortArray -> JsonArray(this.map { JsonPrimitive(it) })
        is IntArray -> JsonArray(this.map { JsonPrimitive(it) })
        is LongArray -> JsonArray(this.map { JsonPrimitive(it) })
        is FloatArray -> JsonArray(this.map { JsonPrimitive(it) })
        is DoubleArray -> JsonArray(this.map { JsonPrimitive(it) })
        is CharArray -> JsonArray(this.map { JsonPrimitive(it.toString()) })
        is Array<*> -> JsonArray(this.map { it.toJsonElement() })
        is Iterable<*> -> JsonArray(this.map { it.toJsonElement() })
        is Map<*, *> -> JsonObject(this.entries.associate { (k, v) -> k.toString() to v.toJsonElement() })
        else -> JsonPrimitive(this.toString())
    }
}

/**
 * Type-safe builder for top-level [SduiDocument] instances.
 */
@SduiDsl
class SduiDocumentBuilder(
    val id: String = "sdui_root",
    val version: Int = 1
) : SduiChildrenBuilder() {

    private val _metadata = mutableMapOf<String, String>()
    private val _initialState = mutableMapOf<String, JsonElement>()

    /** Explicitly specified root node */
    var root: SduiNode? = null

    /** Optional Server-Driven Modal Bottom Sheet */
    var bottomSheet: pro.jayeshseth.devcontrols.sdui.model.SduiBottomSheetSpec? = null

    /**
     * Sets the bottom sheet directly.
     */
    fun bottomSheet(spec: pro.jayeshseth.devcontrols.sdui.model.SduiBottomSheetSpec) {
        this.bottomSheet = spec
    }

    /**
     * Configures a server-driven modal bottom sheet using a builder lambda.
     */
    fun bottomSheet(
        id: String = "sheet",
        isOpenExpr: String = "isBottomSheetOpen",
        initialState: Map<String, Any?> = emptyMap(),
        skipPartiallyExpanded: Boolean = false,
        dragHandleVisible: Boolean = true,
        peekHeightDp: Float = 0f,
        containerColor: String? = null,
        scrimColor: String? = null,
        cornerRadiusDp: Float = 24f,
        onDismiss: (SduiActionBuilder.() -> Unit)? = null,
        content: SduiChildrenBuilder.() -> Unit
    ) {
        val builder = object : SduiChildrenBuilder() {}
        builder.content()
        val nodes = builder.buildNodes()
        val sheetContent = when {
            nodes.size == 1 -> nodes.first()
            nodes.size > 1 -> SduiNode.Column(children = nodes)
            else -> SduiNode.Spacer()
        }
        this.bottomSheet = pro.jayeshseth.devcontrols.sdui.model.SduiBottomSheetSpec(
            id = id,
            content = sheetContent,
            isOpenExpr = isOpenExpr,
            initialState = initialState.mapValues { it.value.toJsonElement() },
            skipPartiallyExpanded = skipPartiallyExpanded,
            dragHandleVisible = dragHandleVisible,
            peekHeightDp = peekHeightDp,
            containerColor = containerColor,
            scrimColor = scrimColor,
            cornerRadiusDp = cornerRadiusDp,
            onDismissActions = onDismiss?.let { SduiActionBuilder().apply(it).build() } ?: emptyList()
        )
    }

    /**
     * Alias for [bottomSheet].
     */
    fun sduiBottomSheet(
        id: String = "sheet",
        isOpenExpr: String = "isBottomSheetOpen",
        initialState: Map<String, Any?> = emptyMap(),
        skipPartiallyExpanded: Boolean = false,
        dragHandleVisible: Boolean = true,
        peekHeightDp: Float = 0f,
        containerColor: String? = null,
        scrimColor: String? = null,
        cornerRadiusDp: Float = 24f,
        onDismiss: (SduiActionBuilder.() -> Unit)? = null,
        content: SduiChildrenBuilder.() -> Unit
    ) = bottomSheet(
        id = id,
        isOpenExpr = isOpenExpr,
        initialState = initialState,
        skipPartiallyExpanded = skipPartiallyExpanded,
        dragHandleVisible = dragHandleVisible,
        peekHeightDp = peekHeightDp,
        containerColor = containerColor,
        scrimColor = scrimColor,
        cornerRadiusDp = cornerRadiusDp,
        onDismiss = onDismiss,
        content = content
    )

    /**
     * Sets metadata key-value pair.
     */
    fun metadata(key: String, value: String) {
        _metadata[key] = value
    }

    /**
     * Sets metadata using vararg pairs.
     */
    fun metadata(vararg pairs: Pair<String, String>) {
        for ((k, v) in pairs) {
            _metadata[k] = v
        }
    }

    /**
     * Sets metadata from a map.
     */
    fun metadata(map: Map<String, String>) {
        _metadata.putAll(map)
    }

    /**
     * Adds an entry to the reactive initial state store.
     * Primitives, maps, and lists are automatically converted to JsonElements.
     */
    fun initialState(key: String, value: Any?) {
        _initialState[key] = value.toJsonElement()
    }

    /**
     * Adds multiple entries to initial state using vararg pairs.
     */
    fun initialState(vararg pairs: Pair<String, Any?>) {
        for ((k, v) in pairs) {
            _initialState[k] = v.toJsonElement()
        }
    }

    /**
     * Adds multiple entries to initial state from a map.
     */
    fun initialState(map: Map<String, Any?>) {
        for ((k, v) in map) {
            _initialState[k] = v.toJsonElement()
        }
    }

    /**
     * Alias for [initialState].
     */
    fun state(key: String, value: Any?) = initialState(key, value)
    fun state(vararg pairs: Pair<String, Any?>) = initialState(*pairs)
    fun state(map: Map<String, Any?>) = initialState(map)

    /**
     * Explicitly sets the root AST node.
     */
    fun root(node: SduiNode) {
        this.root = node
    }

    /**
     * Explicitly sets the root AST node using a lambda.
     */
    fun root(block: () -> SduiNode) {
        this.root = block()
    }

    /**
     * Builds the [SduiDocument]. If [root] was not set explicitly, uses child nodes declared
     * directly in the document body.
     */
    fun build(): SduiDocument {
        val declaredChildren = super.buildNodes()
        val finalRoot = root ?: when {
            declaredChildren.size == 1 -> declaredChildren.first()
            declaredChildren.size > 1 -> SduiNode.Column(children = declaredChildren)
            else -> error("SduiDocument '$id' has no root node. Declare a root node such as sduiColumn { ... } or root { ... }.")
        }

        return SduiDocument(
            id = id,
            version = version,
            metadata = _metadata.toMap(),
            initialState = _initialState.toMap(),
            root = finalRoot,
            bottomSheet = bottomSheet
        )
    }
}

/**
 * Top-level entry point for creating Server-Driven UI documents using Compose-like Kotlin DSL.
 */
fun sduiDocument(
    id: String = "sdui_root",
    version: Int = 1,
    init: SduiDocumentBuilder.() -> Unit
): SduiDocument = SduiDocumentBuilder(id = id, version = version).apply(init).build()

/**
 * Serializes [SduiDocument] into a formatted JSON string using the SDUI JSON serializer.
 */
fun SduiDocument.toJson(json: Json = defaultSduiJson): String =
    json.encodeToString(SduiDocument.serializer(), this)

/**
 * Serializes [SduiDocument] into a [JsonElement] structure.
 */
fun SduiDocument.toJsonElement(json: Json = defaultSduiJson): JsonElement =
    json.encodeToJsonElement(SduiDocument.serializer(), this)

/**
 * Serializes [SduiDocumentBuilder] directly into a formatted JSON string.
 */
fun SduiDocumentBuilder.toJson(json: Json = defaultSduiJson): String =
    build().toJson(json)

/**
 * Serializes [SduiDocumentBuilder] directly into a [JsonElement] structure.
 */
fun SduiDocumentBuilder.toJsonElement(json: Json = defaultSduiJson): JsonElement =
    build().toJsonElement(json)
