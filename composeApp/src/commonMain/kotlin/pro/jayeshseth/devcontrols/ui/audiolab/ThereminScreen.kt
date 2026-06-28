package pro.jayeshseth.devcontrols.ui.audiolab

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import pro.jayeshseth.devcontrols.platform.AudioPlayer
import kotlin.math.PI
import kotlin.math.sin
import kotlin.concurrent.Volatile

private class ThereminState { @Volatile var freq = 0.0; @Volatile var amp = 0.0 }

/** Theremin — slide your finger: X controls pitch, Y controls volume (continuous AudioPlayer sine). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThereminScreen(onBack: () -> Unit) {
    val state = remember { ThereminState() }
    var touch by remember { mutableStateOf<Offset?>(null) }

    DisposableEffect(Unit) {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        scope.launch {
            val sr = 44100
            val player = AudioPlayer(sr)
            player.start()
            val n = 256; val buf = ShortArray(n); var phase = 0.0; var curAmp = 0.0
            try {
                while (isActive) {
                    for (i in 0 until n) {
                        curAmp += (state.amp - curAmp) * 0.001
                        val s = if (state.freq <= 0) 0.0 else sin(phase) * curAmp
                        buf[i] = (s * Short.MAX_VALUE).toInt().toShort()
                        if (state.freq > 0) { phase += 2 * PI * state.freq / sr; if (phase > 2 * PI) phase -= 2 * PI }
                    }
                    player.write(buf, 0, n)
                }
            } finally {
                runCatching { player.stop() }
                runCatching { player.release() }
            }
        }
        onDispose { scope.cancel() }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Theremin") }, navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }) },
        modifier = Modifier.fillMaxSize()
    ) { inner ->
        Column(Modifier.padding(inner).fillMaxSize()) {
            Text("Slide a finger — right = higher pitch, up = louder.", Modifier.padding(8.dp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Box(Modifier.fillMaxSize()
                .background(Brush.verticalGradient(listOf(Color(0xFF1A0033), Color(0xFF000022))))
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { o -> touch = o; state.freq = 110.0 + (o.x / size.width) * 880.0; state.amp = (1f - o.y / size.height).toDouble().coerceIn(0.0, 1.0) },
                        onDragEnd = { touch = null; state.amp = 0.0 }, onDragCancel = { touch = null; state.amp = 0.0 }
                    ) { ch, _ -> touch = ch.position; state.freq = 110.0 + (ch.position.x / size.width) * 880.0; state.amp = (1f - ch.position.y / size.height).toDouble().coerceIn(0.0, 1.0) }
                }
                .pointerInput(Unit) {
                    detectTapGestures(onPress = { o ->
                        touch = o; state.freq = 110.0 + (o.x / size.width) * 880.0; state.amp = (1f - o.y / size.height).toDouble().coerceIn(0.0, 1.0)
                        tryAwaitRelease(); touch = null; state.amp = 0.0
                    })
                }
                .drawBehind {
                    touch?.let {
                        drawCircle(Color(0x66FF4081), 70f, it)
                        drawCircle(Color(0xFFFF4081), 22f, it)
                    }
                })
        }
    }
}
