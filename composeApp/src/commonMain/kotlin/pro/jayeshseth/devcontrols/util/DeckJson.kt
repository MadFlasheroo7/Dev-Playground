package pro.jayeshseth.devcontrols.util

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.add
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.float
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import pro.jayeshseth.devcontrols.platform.DeckArchive
import pro.jayeshseth.devcontrols.ui.present.Deck
import pro.jayeshseth.devcontrols.ui.present.ElementType
import pro.jayeshseth.devcontrols.ui.present.Entrance
import pro.jayeshseth.devcontrols.ui.present.ShapeKind
import pro.jayeshseth.devcontrols.ui.present.Slide
import pro.jayeshseth.devcontrols.ui.present.SlideElement
import pro.jayeshseth.devcontrols.ui.present.TransitionIn

/**
 * Portable deck codec. Built on the kotlinx-serialization-json *runtime* (JsonElement DSL), NOT the
 * `@Serializable` compiler plugin — the plugin currently crashes the Kotlin/Wasm frontend, and the
 * runtime DSL is fully multiplatform. Wrapped in a `.cdeck` zip via [DeckArchive]; [read] sniffs
 * `PK` (zip) vs `{` (raw JSON), so older files and the web's uncompressed bundles both open.
 */
object DeckJson {
    const val EXTENSION = "cdeck"

    private val json = Json { prettyPrint = true }

    fun toJson(deck: Deck): String {
        val root = buildJsonObject {
            put("format", "cdeck")
            put("version", 1)
            put("title", deck.title)
            putJsonArray("slides") {
                deck.slides.forEach { s ->
                    addJsonObject {
                        put("id", s.id)
                        putJsonArray("bgColors") { s.bgColors.forEach { add(it) } }
                        put("transitionIn", s.transitionIn.name)
                        put("notes", s.notes)
                        s.bgShader?.let { put("bgShader", it) }
                        s.bgCanvas?.let { put("bgCanvas", it) }
                        putJsonArray("elements") {
                            s.elements.forEach { e ->
                                addJsonObject {
                                    put("id", e.id)
                                    put("type", e.type.name)
                                    put("x", e.x); put("y", e.y); put("w", e.w); put("h", e.h)
                                    e.text?.let { put("text", it) }
                                    putJsonArray("bullets") { e.bullets.forEach { add(it) } }
                                    put("colorArgb", e.colorArgb)
                                    put("fontSize", e.fontSize)
                                    put("centerAlign", e.centerAlign)
                                    put("weight", e.weight)
                                    put("entrance", e.entrance.name)
                                    e.sharedKey?.let { put("sharedKey", it) }
                                    put("shape", e.shape.name)
                                }
                            }
                        }
                    }
                }
            }
        }
        return json.encodeToString(JsonObject.serializer(), root)
    }

    fun fromJson(text: String): Deck {
        val root = Json.parseToJsonElement(text).jsonObject
        val slides = root["slides"]!!.jsonArray.map { se ->
            val so = se.jsonObject
            Slide(
                id = so["id"]!!.jsonPrimitive.content,
                bgColors = so["bgColors"]!!.jsonArray.map { it.jsonPrimitive.long },
                transitionIn = enumOr(so["transitionIn"]?.jsonPrimitive?.contentOrNull, TransitionIn.FADE),
                elements = so["elements"]!!.jsonArray.map { ee ->
                    val eo = ee.jsonObject
                    SlideElement(
                        id = eo["id"]!!.jsonPrimitive.content,
                        type = enumOr(eo["type"]?.jsonPrimitive?.contentOrNull, ElementType.BODY),
                        x = eo["x"]!!.jsonPrimitive.float,
                        y = eo["y"]!!.jsonPrimitive.float,
                        w = eo["w"]!!.jsonPrimitive.float,
                        h = eo["h"]!!.jsonPrimitive.float,
                        text = eo["text"]?.jsonPrimitive?.contentOrNull,
                        bullets = eo["bullets"]?.jsonArray?.map { it.jsonPrimitive.content } ?: emptyList(),
                        colorArgb = eo["colorArgb"]?.jsonPrimitive?.long ?: 0xFFFFFFFF,
                        fontSize = eo["fontSize"]?.jsonPrimitive?.int ?: 24,
                        centerAlign = eo["centerAlign"]?.jsonPrimitive?.boolean ?: false,
                        weight = eo["weight"]?.jsonPrimitive?.int ?: 400,
                        entrance = enumOr(eo["entrance"]?.jsonPrimitive?.contentOrNull, Entrance.NONE),
                        sharedKey = eo["sharedKey"]?.jsonPrimitive?.contentOrNull,
                        shape = enumOr(eo["shape"]?.jsonPrimitive?.contentOrNull, ShapeKind.RECT),
                    )
                },
                notes = so["notes"]?.jsonPrimitive?.contentOrNull ?: "",
                bgShader = so["bgShader"]?.jsonPrimitive?.contentOrNull,
                bgCanvas = so["bgCanvas"]?.jsonPrimitive?.contentOrNull,
            )
        }
        return Deck(root["title"]?.jsonPrimitive?.contentOrNull ?: "Imported Deck", slides)
    }

    fun toBundle(deck: Deck): ByteArray = DeckArchive.pack(toJson(deck))

    fun read(bytes: ByteArray): Deck {
        val first = bytes.firstOrNull { !it.toInt().toChar().isWhitespace() }?.toInt()?.toChar()
        return when (first) {
            'P' -> fromJson(DeckArchive.unpack(bytes))
            '{' -> fromJson(bytes.decodeToString())
            else -> throw IllegalArgumentException("Unrecognized .cdeck file")
        }
    }

    private inline fun <reified T : Enum<T>> enumOr(name: String?, default: T): T =
        runCatching { enumValueOf<T>(name ?: "") }.getOrDefault(default)
}
