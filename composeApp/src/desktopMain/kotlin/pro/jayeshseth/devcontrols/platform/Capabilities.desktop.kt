package pro.jayeshseth.devcontrols.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import pro.jayeshseth.devcontrols.ui.present.Deck
import java.awt.FileDialog
import java.awt.Frame
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

actual object DeckArchive {
    actual fun pack(json: String): ByteArray {
        val baos = ByteArrayOutputStream()
        ZipOutputStream(baos).use { zip ->
            zip.putNextEntry(ZipEntry("manifest.json")); zip.write("""{"format":"cdeck","version":1}""".toByteArray()); zip.closeEntry()
            zip.putNextEntry(ZipEntry("deck.json")); zip.write(json.toByteArray()); zip.closeEntry()
        }
        return baos.toByteArray()
    }

    actual fun unpack(bytes: ByteArray): String {
        ZipInputStream(ByteArrayInputStream(bytes)).use { zip ->
            var e = zip.nextEntry
            while (e != null) {
                if (e.name == "deck.json") return zip.readBytes().toString(Charsets.UTF_8)
                zip.closeEntry(); e = zip.nextEntry
            }
        }
        throw IllegalArgumentException("No deck.json inside .cdeck bundle")
    }
}

@Composable
actual fun rememberDeckFileIo(): DeckFileIo = remember {
    object : DeckFileIo {
        override fun save(suggestedName: String, bytes: ByteArray) {
            val fd = FileDialog(null as Frame?, "Save deck", FileDialog.SAVE).apply { file = suggestedName; isVisible = true }
            val dir = fd.directory; val name = fd.file
            if (dir != null && name != null) runCatching { File(dir, name).writeBytes(bytes) }
        }

        override fun open(onLoaded: (ByteArray?) -> Unit) {
            val fd = FileDialog(null as Frame?, "Open deck", FileDialog.LOAD).apply { isVisible = true }
            val dir = fd.directory; val name = fd.file
            onLoaded(if (dir != null && name != null) runCatching { File(dir, name).readBytes() }.getOrNull() else null)
        }
    }
}

// Desktop has no built-in PDF/PPTX/MediaStore export yet.
@Composable
actual fun rememberDeckExporter(): DeckExporter? = null

@Composable
actual fun rememberToaster(): (String) -> Unit = remember { { msg: String -> println("[DevControls] $msg") } }

@Composable
actual fun LockLandscape() { /* no-op on desktop */ }
