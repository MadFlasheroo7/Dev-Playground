package pro.jayeshseth.devcontrols.util

import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import pro.jayeshseth.devcontrols.ui.present.Deck
import pro.jayeshseth.devcontrols.ui.present.ElementType
import pro.jayeshseth.devcontrols.ui.present.Entrance
import pro.jayeshseth.devcontrols.ui.present.ShapeKind
import pro.jayeshseth.devcontrols.ui.present.Slide
import pro.jayeshseth.devcontrols.ui.present.SlideElement
import pro.jayeshseth.devcontrols.ui.present.TransitionIn

/**
 * Portable deck codec. A deck is plain data, so we serialize to JSON (no custom binary format
 * needed) and store it as a `.deck.json` file. A [VERSION] tag + defensive parsing keep older
 * files loadable as the model evolves; enums round-trip by name, so unknown values fall back
 * to a sane default instead of throwing.
 */
object DeckJson {
    const val VERSION = 1
    const val EXTENSION = "cdeck"
    const val FORMAT = "cdeck"

    /**
     * Writes a `.cdeck` as a zip bundle (`PK…` magic, per-entry CRC) holding a manifest and the
     * deck model. A zip — not raw JSON — so the file is a distinct, integrity-checked container:
     * a truncated/corrupt file fails loudly on read instead of silently parsing partial text.
     * `media/` for embedded assets slots in here later without changing the reader.
     */
    fun toBundle(deck: Deck): ByteArray {
        val baos = ByteArrayOutputStream()
        ZipOutputStream(baos).use { zip ->
            fun entry(name: String, content: String) {
                zip.putNextEntry(ZipEntry(name)); zip.write(content.toByteArray(Charsets.UTF_8)); zip.closeEntry()
            }
            entry("manifest.json", JSONObject().put("format", FORMAT).put("version", VERSION).toString())
            entry("deck.json", toJson(deck))
        }
        return baos.toByteArray()
    }

    /**
     * Reads a `.cdeck` by sniffing its container so old files keep opening: `PK` => zip bundle
     * (the current format), `{` => raw JSON (legacy `.deck.json` / early `.cdeck`).
     */
    fun read(bytes: ByteArray): Deck {
        val firstNonWs = bytes.firstOrNull { !Character.isWhitespace(it.toInt()) }?.toInt()?.toChar()
        return when (firstNonWs) {
            'P' -> readBundle(bytes)
            '{' -> fromJson(bytes.toString(Charsets.UTF_8))
            else -> throw IllegalArgumentException("Unrecognized .cdeck file")
        }
    }

    private fun readBundle(bytes: ByteArray): Deck {
        ZipInputStream(ByteArrayInputStream(bytes)).use { zip ->
            var e = zip.nextEntry
            while (e != null) {
                if (e.name == "deck.json") return fromJson(zip.readBytes().toString(Charsets.UTF_8))
                zip.closeEntry()
                e = zip.nextEntry
            }
        }
        throw IllegalArgumentException("No deck.json inside .cdeck bundle")
    }

    fun toJson(deck: Deck): String {
        val root = JSONObject()
        root.put("format", FORMAT)
        root.put("version", VERSION)
        root.put("title", deck.title)
        val slides = JSONArray()
        deck.slides.forEach { s ->
            val so = JSONObject()
            so.put("id", s.id)
            so.put("bgColors", JSONArray().also { a -> s.bgColors.forEach { a.put(it) } })
            so.put("transitionIn", s.transitionIn.name)
            so.put("notes", s.notes)
            val els = JSONArray()
            s.elements.forEach { e ->
                val eo = JSONObject()
                eo.put("id", e.id)
                eo.put("type", e.type.name)
                eo.put("x", e.x.toDouble()); eo.put("y", e.y.toDouble())
                eo.put("w", e.w.toDouble()); eo.put("h", e.h.toDouble())
                e.text?.let { eo.put("text", it) }
                eo.put("bullets", JSONArray().also { a -> e.bullets.forEach { a.put(it) } })
                eo.put("colorArgb", e.colorArgb)
                eo.put("fontSize", e.fontSize)
                eo.put("centerAlign", e.centerAlign)
                eo.put("weight", e.weight)
                eo.put("entrance", e.entrance.name)
                e.sharedKey?.let { eo.put("sharedKey", it) }
                eo.put("shape", e.shape.name)
                els.put(eo)
            }
            so.put("elements", els)
            slides.put(so)
        }
        root.put("slides", slides)
        return root.toString(2)
    }

    fun fromJson(text: String): Deck {
        val root = JSONObject(text)
        val slidesArr = root.getJSONArray("slides")
        val slides = ArrayList<Slide>(slidesArr.length())
        for (i in 0 until slidesArr.length()) {
            val so = slidesArr.getJSONObject(i)
            val bgArr = so.getJSONArray("bgColors")
            val bg = ArrayList<Long>(bgArr.length()).apply { for (j in 0 until bgArr.length()) add(bgArr.getLong(j)) }
            val elsArr = so.getJSONArray("elements")
            val els = ArrayList<SlideElement>(elsArr.length())
            for (j in 0 until elsArr.length()) {
                val eo = elsArr.getJSONObject(j)
                val bArr = eo.optJSONArray("bullets")
                val bullets = ArrayList<String>().apply {
                    if (bArr != null) for (k in 0 until bArr.length()) add(bArr.getString(k))
                }
                els.add(
                    SlideElement(
                        id = eo.getString("id"),
                        type = enumOr(eo.optString("type"), ElementType.BODY),
                        x = eo.getDouble("x").toFloat(), y = eo.getDouble("y").toFloat(),
                        w = eo.getDouble("w").toFloat(), h = eo.getDouble("h").toFloat(),
                        text = if (eo.has("text")) eo.getString("text") else null,
                        bullets = bullets,
                        colorArgb = eo.optLong("colorArgb", 0xFFFFFFFF),
                        fontSize = eo.optInt("fontSize", 24),
                        centerAlign = eo.optBoolean("centerAlign", false),
                        weight = eo.optInt("weight", 400),
                        entrance = enumOr(eo.optString("entrance"), Entrance.NONE),
                        sharedKey = if (eo.has("sharedKey")) eo.getString("sharedKey") else null,
                        shape = enumOr(eo.optString("shape"), ShapeKind.RECT),
                    )
                )
            }
            slides.add(
                Slide(
                    id = so.getString("id"),
                    bgColors = bg,
                    transitionIn = enumOr(so.optString("transitionIn"), TransitionIn.FADE),
                    elements = els,
                    notes = so.optString("notes", ""),
                )
            )
        }
        return Deck(root.optString("title", "Imported Deck"), slides)
    }

    private inline fun <reified T : Enum<T>> enumOr(name: String?, default: T): T =
        runCatching { enumValueOf<T>(name ?: "") }.getOrDefault(default)
}
