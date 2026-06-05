package pro.jayeshseth.devcontrols.ui.toys

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlin.random.Random

/** White noise — generate white / pink / brown noise via AudioTrack for focus/sleep. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun WhiteNoiseScreen(onBack: () -> Unit) {
    var playing by remember { mutableStateOf(false) }
    var type by remember { mutableIntStateOf(0) } // 0 white, 1 pink, 2 brown
    val typeRef = remember { intArrayOf(0) }
    typeRef[0] = type

    DisposableEffect(playing) {
        if (!playing) return@DisposableEffect onDispose { }
        val sr = 44100
        val minBuf = AudioTrack.getMinBufferSize(sr, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT)
        val track = AudioTrack(
            AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build(),
            AudioFormat.Builder().setSampleRate(sr).setChannelMask(AudioFormat.CHANNEL_OUT_MONO).setEncoding(AudioFormat.ENCODING_PCM_16BIT).build(),
            maxOf(minBuf, 4096), AudioTrack.MODE_STREAM, AudioManager.AUDIO_SESSION_ID_GENERATE
        )
        val thread = Thread {
            track.play()
            val buf = ShortArray(2048)
            var last = 0.0; var brown = 0.0
            while (playing) {
                for (i in buf.indices) {
                    val w = Random.nextDouble(-1.0, 1.0)
                    val s = when (typeRef[0]) {
                        1 -> { last = 0.98 * last + 0.02 * w; last * 3.0 }        // pink-ish (low-pass)
                        2 -> { brown = (brown + 0.02 * w).coerceIn(-1.0, 1.0); brown * 3.5 } // brown
                        else -> w
                    }
                    buf[i] = (s.coerceIn(-1.0, 1.0) * 0.25 * Short.MAX_VALUE).toInt().toShort()
                }
                track.write(buf, 0, buf.size)
            }
            runCatching { track.stop(); track.release() }
        }
        thread.start()
        onDispose { /* playing=false stops loop */ }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("White Noise") }, navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }) },
        modifier = Modifier.fillMaxSize()
    ) { inner ->
        Column(Modifier.padding(inner).fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(Modifier.height(20.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("White", "Pink", "Brown").forEachIndexed { i, n -> FilterChip(type == i, { type = i }, { Text(n) }) }
            }
            Spacer(Modifier.height(24.dp))
            Button(onClick = { playing = !playing }) { Text(if (playing) "Stop" else "Play") }
            Spacer(Modifier.height(12.dp))
            Text("Generated live with AudioTrack. Pink/brown are filtered white noise.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
