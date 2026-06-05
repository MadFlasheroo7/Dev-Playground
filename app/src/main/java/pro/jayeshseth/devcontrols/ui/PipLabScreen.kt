package pro.jayeshseth.devcontrols.ui

import android.app.Activity
import android.app.PictureInPictureParams
import android.util.Rational
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.cos
import kotlin.math.sin

/** Picture-in-Picture — enter PiP with an aspect ratio; a moving clock keeps rendering. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PipLabScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val activity = context as? Activity
    var secs by remember { mutableIntStateOf(0) }
    var frame by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        var last = 0L
        while (true) {
            withFrameNanos { t ->
                if (last == 0L) last = t
                if (t - last > 1_000_000_000L) { secs++; last = t }
                frame++
            }
        }
    }

    fun enterPip(ratio: Rational) {
        activity?.let {
            runCatching {
                it.enterPictureInPictureMode(PictureInPictureParams.Builder().setAspectRatio(ratio).build())
            }
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Picture-in-Picture") }, navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }) },
        modifier = Modifier.fillMaxSize()
    ) { inner ->
        Column(Modifier.padding(inner).fillMaxSize().padding(16.dp)) {
            Box(Modifier.fillMaxWidth().weight(1f).background(Color(0xFF0B0B16)).drawBehind {
                frame
                val cx = size.width / 2; val cy = size.height / 2
                val r = minOf(cx, cy) * 0.8f
                drawCircle(Color(0xFF22FFAA), r, Offset(cx, cy), style = androidx.compose.ui.graphics.drawscope.Stroke(4f))
                val ang = (secs % 60) / 60f * 6.2831853f - 1.5708f
                drawLine(Color.White, Offset(cx, cy), Offset(cx + cos(ang) * r, cy + sin(ang) * r), strokeWidth = 6f)
            }) {
                Text("$secs s", Modifier.align(Alignment.TopStart).padding(8.dp), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 24.sp)
            }
            Spacer(Modifier.height(12.dp))
            Text("Press a button, then leave the app — the window keeps animating.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(8.dp))
            androidx.compose.foundation.layout.Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { enterPip(Rational(16, 9)) }) { Text("PiP 16:9") }
                Button(onClick = { enterPip(Rational(1, 1)) }) { Text("PiP 1:1") }
                Button(onClick = { enterPip(Rational(9, 16)) }) { Text("PiP 9:16") }
            }
        }
    }
}
