package pro.jayeshseth.devcontrols.platform

import androidx.compose.animation.core.withInfiniteAnimationFrameMillis
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
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

@Composable
actual fun rememberDeckExporter(): DeckExporter? = remember {
    object : DeckExporter {
        override fun export(kind: ExportKind, deck: Deck, onStatus: (String) -> Unit) {
            val safe = deck.title.replace(Regex("[^A-Za-z0-9_-]"), "_").ifBlank { "deck" }
            when (kind) {
                ExportKind.PDF -> saveOne("$safe.pdf", DeckExportAwt.exportPdf(deck), onStatus)
                ExportKind.PPTX -> saveOne("$safe.pptx", DeckExportAwt.exportPptx(deck), onStatus)
                ExportKind.PNG -> {
                    val fd = FileDialog(null as Frame?, "Export PNGs (base name)", FileDialog.SAVE).apply { file = "$safe.png"; isVisible = true }
                    val dir = fd.directory; val name = fd.file
                    if (dir == null || name == null) { onStatus("Cancelled"); return }
                    val base = name.removeSuffix(".png")
                    runCatching {
                        deck.slides.forEachIndexed { i, s -> File(dir, "${base}_${i + 1}.png").writeBytes(DeckExportAwt.png(s)) }
                    }.onSuccess { onStatus("Saved ${deck.slides.size} PNGs to $dir") }
                        .onFailure { onStatus("Export failed") }
                }
            }
        }

        private fun saveOne(suggested: String, bytes: ByteArray, onStatus: (String) -> Unit) {
            val fd = FileDialog(null as Frame?, "Export", FileDialog.SAVE).apply { file = suggested; isVisible = true }
            val dir = fd.directory; val name = fd.file
            if (dir != null && name != null) {
                runCatching { File(dir, name).writeBytes(bytes) }
                    .onSuccess { onStatus("Saved ${File(dir, name).absolutePath}") }
                    .onFailure { onStatus("Export failed") }
            } else onStatus("Cancelled")
        }
    }
}

@Composable
actual fun rememberToaster(): (String) -> Unit = remember { { msg: String -> println("[DevControls] $msg") } }

@Composable
actual fun rememberHapticPulse(): () -> Unit = remember { {} }

@Composable
actual fun MicrophonePermissionGate(rationale: String, content: @Composable () -> Unit) {
    content()
}



@Composable
actual fun ShaderBackground(source: String, modifier: Modifier, fallback: @Composable () -> Unit) {
    val effect = remember(source) {
        runCatching { org.jetbrains.skia.RuntimeEffect.makeForShader(source) }.getOrNull()
    }
    if (effect == null) { Box(modifier) { fallback() }; return }
    val time by produceState(0f) {
        while (true) { withInfiniteAnimationFrameMillis { value = it / 1000f } }
    }
    Box(
        modifier.drawBehind {
            runCatching {
                val w = size.width; val h = size.height
                val bytes = java.nio.ByteBuffer.allocate(12).order(java.nio.ByteOrder.LITTLE_ENDIAN)
                    .putFloat(w).putFloat(h).putFloat(time).array()
                val shader = effect.makeShader(
                    org.jetbrains.skia.Data.makeFromBytes(bytes), null, null
                )
                val paint = org.jetbrains.skia.Paint().apply { this.shader = shader }
                drawIntoCanvas { it.nativeCanvas.drawRect(org.jetbrains.skia.Rect.makeWH(w, h), paint) }
            }
        }
    )
}

/** Set by the desktop entry point so [PresentationMode] can toggle the window to fullscreen. */
val LocalWindowState = androidx.compose.runtime.staticCompositionLocalOf<androidx.compose.ui.window.WindowState?> { null }

@Composable
actual fun PresentationMode() {
    val ws = LocalWindowState.current
    androidx.compose.runtime.DisposableEffect(Unit) {
        val previous = ws?.placement
        ws?.placement = androidx.compose.ui.window.WindowPlacement.Fullscreen
        onDispose { if (previous != null) ws.placement = previous }
    }
}
