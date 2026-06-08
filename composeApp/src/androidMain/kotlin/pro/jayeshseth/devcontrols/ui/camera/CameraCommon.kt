package pro.jayeshseth.devcontrols.ui.camera

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import pro.jayeshseth.devcontrols.ui.PermGate
import java.io.OutputStream

/** Scaffold for every Camera-category screen: top bar + back + CAMERA permission gate. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CameraScreenScaffold(title: String, onBack: () -> Unit, rationale: String = "Camera access is required.", content: @Composable () -> Unit) {
    Scaffold(
        topBar = { TopAppBar(title = { Text(title) }, navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }) },
        modifier = Modifier.fillMaxSize()
    ) { inner ->
        Column(Modifier.padding(inner).fillMaxSize()) {
            PermGate(android.Manifest.permission.CAMERA, rationale) { content() }
        }
    }
}

/** Saves captures to MediaStore under Pictures|Movies/DevControls. No storage perm on API 29+. */
object MediaSink {
    fun saveImage(context: Context, name: String, mime: String, writer: (OutputStream) -> Unit): Uri? {
        val dir = (if (mime.startsWith("video")) Environment.DIRECTORY_MOVIES else Environment.DIRECTORY_PICTURES) + "/DevControls"
        val collection = if (mime.startsWith("video")) MediaStore.Video.Media.EXTERNAL_CONTENT_URI else MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, mime)
            put(MediaStore.MediaColumns.RELATIVE_PATH, dir)
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }
        val resolver = context.contentResolver
        val uri = resolver.insert(collection, values) ?: return null
        runCatching { resolver.openOutputStream(uri)?.use(writer) }.onFailure { resolver.delete(uri, null, null); return null }
        values.clear(); values.put(MediaStore.MediaColumns.IS_PENDING, 0)
        resolver.update(uri, values, null, null)
        return uri
    }

    data class Item(val uri: Uri, val name: String, val isVideo: Boolean, val dateMs: Long)

    fun listCaptures(context: Context): List<Item> {
        val out = ArrayList<Item>()
        for (video in listOf(false, true)) {
            val collection = if (video) MediaStore.Video.Media.EXTERNAL_CONTENT_URI else MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            val proj = arrayOf(MediaStore.MediaColumns._ID, MediaStore.MediaColumns.DISPLAY_NAME, MediaStore.MediaColumns.DATE_ADDED)
            val sel = "${MediaStore.MediaColumns.RELATIVE_PATH} LIKE ?"
            runCatching {
                context.contentResolver.query(collection, proj, sel, arrayOf("%DevControls%"), "${MediaStore.MediaColumns.DATE_ADDED} DESC")?.use { c ->
                    val idC = c.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
                    val nameC = c.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
                    val dateC = c.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_ADDED)
                    while (c.moveToNext()) {
                        out.add(Item(Uri.withAppendedPath(collection, c.getLong(idC).toString()), c.getString(nameC), video, c.getLong(dateC) * 1000))
                    }
                }
            }
        }
        return out.sortedByDescending { it.dateMs }
    }
}
