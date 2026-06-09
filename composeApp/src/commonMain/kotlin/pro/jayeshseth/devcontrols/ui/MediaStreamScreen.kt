package pro.jayeshseth.devcontrols.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import pro.jayeshseth.devcontrols.media.rememberMediaController
import pro.jayeshseth.media.MediaItem
import pro.jayeshseth.media.bestPlayable
import pro.jayeshseth.media.createMediaSource

/**
 * Search YouTube and play the audio of a tap — backed by the `:media` library (NewPipeExtractor on
 * Android/Desktop). Music-first: tapping a result resolves it and streams the best audio track.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaStreamScreen(onBack: () -> Unit) {
    val source = remember { createMediaSource() }
    val controller = rememberMediaController()
    val scope = rememberCoroutineScope()

    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<MediaItem>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var nowPlaying by remember { mutableStateOf<MediaItem?>(null) }

    val playing by controller.playing.collectAsState()
    val positionMs by controller.positionMs.collectAsState()
    val durationMs by controller.durationMs.collectAsState()

    DisposableEffect(Unit) { onDispose { controller.release() } }

    fun runSearch() {
        if (query.isBlank()) return
        loading = true; error = null
        scope.launch {
            runCatching { source.search(query.trim()) }
                .onSuccess { results = it; loading = false }
                .onFailure { error = it.message ?: "Search failed"; loading = false }
        }
    }

    fun playItem(item: MediaItem) {
        scope.launch {
            error = null
            runCatching {
                val url = source.resolve(item.id).bestPlayable()
                    ?: throw IllegalStateException("No playable stream (YouTube may have blocked extraction)")
                controller.play(url); nowPlaying = item
            }.onFailure { error = "Playback failed: ${it.message}" }
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Media (YouTube)") }, navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }) },
        bottomBar = {
            nowPlaying?.let { np ->
                Column(Modifier.fillMaxWidth().padding(12.dp)) {
                    HorizontalDivider()
                    Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(np.title, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                        TextButton(onClick = { if (playing) controller.pause() else controller.resume() }) { Text(if (playing) "❚❚" else "▶") }
                    }
                    Slider(
                        value = if (durationMs > 0) (positionMs.toFloat() / durationMs) else 0f,
                        onValueChange = { if (durationMs > 0) controller.seekTo((it * durationMs).toLong()) }
                    )
                    Text("${fmt(positionMs)} / ${fmt(durationMs)}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        },
        modifier = Modifier.fillMaxSize()
    ) { inner ->
        Column(Modifier.padding(inner).fillMaxSize().padding(horizontal = 16.dp)) {
            Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(query, { query = it }, label = { Text("Search YouTube") }, singleLine = true, modifier = Modifier.weight(1f))
                Button(onClick = { runSearch() }) { Text("Search") }
            }
            if (loading) LinearProgressIndicator(Modifier.fillMaxWidth())
            error?.let { Text("⚠ $it", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(vertical = 4.dp)) }
            LazyColumn(Modifier.fillMaxSize()) {
                items(results) { item ->
                    Column(
                        Modifier.fillMaxWidth().clickable { playItem(item) }.padding(vertical = 10.dp)
                    ) {
                        Text(item.title, maxLines = 2, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.Medium)
                        Text(
                            buildString { append(item.uploader); if (item.durationSec > 0) append("  ·  ${fmt(item.durationSec * 1000)}") },
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    HorizontalDivider()
                }
            }
        }
    }
}

private fun fmt(ms: Long): String {
    val total = ms / 1000
    val m = total / 60
    val s = total % 60
    return "$m:${s.toString().padStart(2, '0')}"
}
