package pro.jayeshseth.devcontrols.platform

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.animation.core.withInfiniteAnimationFrameMillis
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import pro.jayeshseth.devcontrols.ui.present.Deck
import pro.jayeshseth.devcontrols.util.DeckExport
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
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
actual fun rememberDeckFileIo(): DeckFileIo {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var pendingBytes by remember { mutableStateOf<ByteArray?>(null) }
    var pendingOpen by remember { mutableStateOf<((ByteArray?) -> Unit)?>(null) }
    val saver = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { uri ->
        val b = pendingBytes; pendingBytes = null
        if (uri != null && b != null) scope.launch(Dispatchers.IO) {
            runCatching { ctx.contentResolver.openOutputStream(uri)!!.use { it.write(b) } }
        }
    }
    val opener = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        val cb = pendingOpen; pendingOpen = null
        scope.launch {
            val bytes = if (uri != null) withContext(Dispatchers.IO) { ctx.contentResolver.openInputStream(uri)?.use { it.readBytes() } } else null
            cb?.invoke(bytes)
        }
    }
    return remember {
        object : DeckFileIo {
            override fun save(suggestedName: String, bytes: ByteArray) { pendingBytes = bytes; saver.launch(suggestedName) }
            override fun open(onLoaded: (ByteArray?) -> Unit) { pendingOpen = onLoaded; opener.launch(arrayOf("application/octet-stream", "application/json", "*/*")) }
        }
    }
}

@Composable
actual fun rememberDeckExporter(): DeckExporter? {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    return remember {
        object : DeckExporter {
            override fun export(kind: ExportKind, deck: Deck, onStatus: (String) -> Unit) {
                scope.launch {
                    val ok = withContext(Dispatchers.IO) {
                        when (kind) {
                            ExportKind.PDF -> DeckExport.exportPdf(ctx, deck) != null
                            ExportKind.PPTX -> DeckExport.exportPptx(ctx, deck) != null
                            ExportKind.PNG -> DeckExport.exportPng(ctx, deck).isNotEmpty()
                        }
                    }
                    onStatus(if (ok) "Saved to Downloads" else "Export failed")
                }
            }
        }
    }
}

@Composable
actual fun rememberToaster(): (String) -> Unit {
    val ctx = LocalContext.current
    return remember { { msg: String -> Toast.makeText(ctx, msg, Toast.LENGTH_SHORT).show() } }
}

@Composable
actual fun rememberHapticPulse(): () -> Unit {
    val ctx = LocalContext.current
    val vibrator = remember {
        (ctx.getSystemService(android.content.Context.VIBRATOR_MANAGER_SERVICE) as? android.os.VibratorManager)?.defaultVibrator
    }
    return remember {
        {
            runCatching {
                vibrator?.vibrate(android.os.VibrationEffect.createOneShot(25, android.os.VibrationEffect.DEFAULT_AMPLITUDE))
            }
        }
    }
}

@Composable
actual fun MicrophonePermissionGate(rationale: String, content: @Composable () -> Unit) {
    pro.jayeshseth.devcontrols.ui.PermGate(
        permission = android.Manifest.permission.RECORD_AUDIO,
        rationale = rationale,
        content = content
    )
}



@Composable
actual fun ShaderBackground(source: String, modifier: Modifier, fallback: @Composable () -> Unit) {
    if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.TIRAMISU) {
        Box(modifier) { fallback() }; return
    }
    val shader = remember(source) { runCatching { android.graphics.RuntimeShader(source) }.getOrNull() }
    if (shader == null) { Box(modifier) { fallback() }; return }
    val brush = remember(shader) { ShaderBrush(shader) }
    val time by produceState(0f) { while (true) { withInfiniteAnimationFrameMillis { value = it / 1000f } } }
    Box(modifier.drawBehind {
        runCatching {
            shader.setFloatUniform("iResolution", size.width, size.height)
            shader.setFloatUniform("iTime", time)
            drawRect(brush)
        }
    })
}

@Composable
actual fun PresentationMode() {
    val ctx = LocalContext.current
    DisposableEffect(Unit) {
        var c: android.content.Context = ctx
        while (c is android.content.ContextWrapper && c !is android.app.Activity) c = c.baseContext
        val activity = c as? android.app.Activity
        val prevOrientation = activity?.requestedOrientation
        activity?.requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE

        val window = activity?.window
        val controller = window?.let { w ->
            androidx.core.view.WindowCompat.getInsetsController(w, w.decorView)
        }
        controller?.systemBarsBehavior =
            androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller?.hide(androidx.core.view.WindowInsetsCompat.Type.systemBars())

        onDispose {
            if (prevOrientation != null) activity.requestedOrientation = prevOrientation
            controller?.show(androidx.core.view.WindowInsetsCompat.Type.systemBars())
        }
    }
}

actual fun getPlatformTimeMillis(): Long = System.currentTimeMillis()
