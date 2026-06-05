package pro.jayeshseth.devcontrols.ui.camera

import android.content.Intent
import android.graphics.Bitmap
import android.util.Size
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Gallery — everything captured into Pictures|Movies/DevControls; tap to open, long-press n/a. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CameraGalleryScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val items by produceState(initialValue = emptyList<MediaSink.Item>()) {
        value = withContext(Dispatchers.IO) { MediaSink.listCaptures(context) }
    }
    Scaffold(
        topBar = { TopAppBar(title = { Text("Gallery (${items.size})") }, navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }) },
        modifier = Modifier.fillMaxSize()
    ) { inner ->
        if (items.isEmpty()) {
            Box(Modifier.padding(inner).fillMaxSize(), Alignment.Center) { Text("No captures yet — use Pro Mode or Video Recorder.") }
            return@Scaffold
        }
        LazyVerticalGrid(columns = GridCells.Adaptive(110.dp), modifier = Modifier.padding(inner).fillMaxSize().padding(4.dp)) {
            items(items) { item ->
                val thumb by produceState<Bitmap?>(initialValue = null, item.uri) {
                    value = withContext(Dispatchers.IO) {
                        runCatching { context.contentResolver.loadThumbnail(item.uri, Size(256, 256), null) }.getOrNull()
                    }
                }
                Box(Modifier.padding(3.dp).aspectRatio(1f).background(Color(0xFF1A1A22)).clickable {
                    runCatching {
                        context.startActivity(Intent(Intent.ACTION_VIEW).setDataAndType(item.uri, if (item.isVideo) "video/*" else "image/*")
                            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK))
                    }
                }, contentAlignment = Alignment.Center) {
                    val t = thumb
                    if (t != null) Image(t.asImageBitmap(), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                    else Text("…", color = Color.White)
                    if (item.isVideo) Text("▶", Modifier.align(Alignment.BottomEnd).padding(6.dp), color = Color.White, style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
}
