package pro.jayeshseth.devcontrols.util

import kotlinx.serialization.json.Json
import pro.jayeshseth.devcontrols.platform.DeckArchive
import pro.jayeshseth.devcontrols.ui.present.Deck

/**
 * Portable deck codec. The model is `@Serializable`, so we (de)serialize with kotlinx-serialization
 * (multiplatform) and wrap it in a `.cdeck` zip bundle via [DeckArchive] (a real container with
 * per-entry CRC). [read] sniffs the first byte so older files keep opening: `PK` => zip bundle,
 * `{` => raw JSON (legacy `.deck.json` / early `.cdeck`).
 */
object DeckJson {
    const val EXTENSION = "cdeck"

    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
        encodeDefaults = true
    }

    fun toJson(deck: Deck): String = json.encodeToString(Deck.serializer(), deck)

    fun fromJson(text: String): Deck = json.decodeFromString(Deck.serializer(), text)

    fun toBundle(deck: Deck): ByteArray = DeckArchive.pack(toJson(deck))

    fun read(bytes: ByteArray): Deck {
        val first = bytes.firstOrNull { !it.toInt().toChar().isWhitespace() }?.toInt()?.toChar()
        return when (first) {
            'P' -> fromJson(DeckArchive.unpack(bytes))
            '{' -> fromJson(bytes.decodeToString())
            else -> throw IllegalArgumentException("Unrecognized .cdeck file")
        }
    }
}
