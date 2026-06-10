package pro.jayeshseth.devcontrols.ui.timedate

import pro.jayeshseth.devcontrols.util.pad

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

/** Pomodoro — 25-min focus / 5-min break cycles with a ring countdown. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PomodoroScreen(onBack: () -> Unit) {
    var working by remember { mutableStateOf(true) }
    var remaining by remember { mutableIntStateOf(25 * 60) }
    var running by remember { mutableStateOf(false) }
    var cycles by remember { mutableIntStateOf(0) }
    val total = if (working) 25 * 60 else 5 * 60

    LaunchedEffect(running) {
        while (running) {
            delay(1000)
            if (remaining > 0) remaining-- else {
                if (working) { cycles++; working = false; remaining = 5 * 60 } else { working = true; remaining = 25 * 60 }
            }
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Pomodoro") }, navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }) },
        modifier = Modifier.fillMaxSize()
    ) { inner ->
        Column(Modifier.padding(inner).fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(Modifier.height(20.dp))
            Text(if (working) "Focus" else "Break", style = MaterialTheme.typography.titleLarge, color = if (working) Color(0xFFE53935) else Color(0xFF43A047))
            Spacer(Modifier.height(16.dp))
            Box(contentAlignment = Alignment.Center) {
                val ringColor = if (working) Color(0xFFE53935) else Color(0xFF43A047)
                val track = MaterialTheme.colorScheme.surfaceVariant
                Canvas(Modifier.size(220.dp)) {
                    val st = 18f
                    drawArc(track, -90f, 360f, false, style = Stroke(st, cap = StrokeCap.Round), size = Size(size.width - st, size.height - st), topLeft = androidx.compose.ui.geometry.Offset(st / 2, st / 2))
                    drawArc(ringColor, -90f, 360f * remaining / total, false, style = Stroke(st, cap = StrokeCap.Round), size = Size(size.width - st, size.height - st), topLeft = androidx.compose.ui.geometry.Offset(st / 2, st / 2))
                }
                Text("${(remaining / 60).pad(2)}:${(remaining % 60).pad(2)}", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 44.sp)
            }
            Spacer(Modifier.height(24.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = { running = !running }) { Text(if (running) "Pause" else "Start") }
                OutlinedButton(onClick = { running = false; working = true; remaining = 25 * 60 }) { Text("Reset") }
            }
            Spacer(Modifier.height(16.dp))
            Text("Completed focus sessions: $cycles", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
