package pro.jayeshseth.devcontrols.platform

import androidx.compose.runtime.Composable
import pro.jayeshseth.devcontrols.ui.present.Deck

/** Pack/unpack the `.cdeck` zip container. JVM (Android/desktop) use java.util.zip; web/iOS stub. */
expect object DeckArchive {
    fun pack(json: String): ByteArray
    fun unpack(bytes: ByteArray): String
}

/** Save bytes to / open bytes from a user-chosen file (SAF on Android, native dialog on desktop). */
interface DeckFileIo {
    fun save(suggestedName: String, bytes: ByteArray)
    fun open(onLoaded: (ByteArray?) -> Unit)
}

@Composable
expect fun rememberDeckFileIo(): DeckFileIo

enum class ExportKind { PDF, PPTX, PNG }

/** Renders the deck to a file format. `null` on platforms without export support. */
interface DeckExporter {
    fun export(kind: ExportKind, deck: Deck, onStatus: (String) -> Unit)
}

@Composable
expect fun rememberDeckExporter(): DeckExporter?

/** A short transient message (Toast on Android; console/snackbar elsewhere). */
@Composable
expect fun rememberToaster(): (String) -> Unit

/**
 * While composed, puts the host into presentation mode — fullscreen + landscape — and restores the
 * previous state on exit. Android: immersive (hidden system bars) + sensor-landscape. Desktop:
 * fullscreen window. Web: fullscreen + orientation lock. iOS: stub.
 */
@Composable
expect fun PresentationMode()
