package pro.jayeshseth.devcontrols.ui.timedate

import pro.jayeshseth.devcontrols.util.fmt
import pro.jayeshseth.devcontrols.util.hex2
import pro.jayeshseth.devcontrols.util.pad
import pro.jayeshseth.devcontrols.util.nowMillis
import pro.jayeshseth.devcontrols.platform.PersistentStorage

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

/** Stopwatch — start/stop, laps, reset; centisecond precision; persists across app kills and device reboots. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StopwatchScreen(onBack: () -> Unit) {
    var running by remember { mutableStateOf(PersistentStorage.getBoolean("stopwatch_running", false)) }
    var elapsed by remember { mutableLongStateOf(PersistentStorage.getLong("stopwatch_elapsed", 0L)) }
    var lastStart by remember { mutableLongStateOf(PersistentStorage.getLong("stopwatch_last_start", 0L)) }
    
    val savedLaps = remember {
        PersistentStorage.getString("stopwatch_laps", "").split(",")
            .filter { it.isNotEmpty() }
            .mapNotNull { it.toLongOrNull() }
    }
    val laps = remember { mutableStateListOf<Long>().apply { addAll(savedLaps) } }

    fun saveLapsToStorage() {
        PersistentStorage.saveString("stopwatch_laps", laps.joinToString(","))
    }

    LaunchedEffect(running) {
        if (running) {
            if (lastStart == 0L) {
                lastStart = nowMillis() - elapsed
                PersistentStorage.saveLong("stopwatch_last_start", lastStart)
            }
            while (running) {
                elapsed = nowMillis() - lastStart
                PersistentStorage.saveLong("stopwatch_elapsed", elapsed)
                delay(33)
            }
        }
    }

    fun fmt(ms: Long): String {
        val days = ms / 86400000L
        val hours = (ms % 86400000L) / 3600000L
        val minutes = (ms % 3600000L) / 60000L
        val seconds = (ms % 60000L) / 1000L
        val hundredths = (ms % 1000L) / 10L
        
        return if (days > 0) {
            "${days}d ${hours.pad(2)}h ${minutes.pad(2)}m ${seconds.pad(2)}s"
        } else if (hours > 0) {
            "${hours.pad(2)}:${minutes.pad(2)}:${seconds.pad(2)}.${hundredths.pad(2)}"
        } else {
            "${minutes.pad(2)}:${seconds.pad(2)}.${hundredths.pad(2)}"
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Stopwatch") }, navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }) },
        modifier = Modifier.fillMaxSize()
    ) { inner ->
        Column(Modifier.padding(inner).fillMaxSize().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(Modifier.height(20.dp))
            Text(
                text = fmt(elapsed),
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = if (elapsed >= 86400000L) 36.sp else 56.sp
            )
            Spacer(Modifier.height(24.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = {
                    running = !running
                    PersistentStorage.saveBoolean("stopwatch_running", running)
                    if (!running) {
                        // Stopped, save the final elapsed and reset lastStart
                        PersistentStorage.saveLong("stopwatch_elapsed", elapsed)
                        lastStart = 0L
                        PersistentStorage.saveLong("stopwatch_last_start", 0L)
                    } else {
                        // Starting, set lastStart
                        lastStart = nowMillis() - elapsed
                        PersistentStorage.saveLong("stopwatch_last_start", lastStart)
                    }
                }) {
                    Text(if (running) "Stop" else "Start")
                }
                OutlinedButton(onClick = {
                    if (running) {
                        laps.add(0, elapsed)
                        saveLapsToStorage()
                    } else {
                        elapsed = 0L
                        lastStart = 0L
                        laps.clear()
                        PersistentStorage.saveBoolean("stopwatch_running", false)
                        PersistentStorage.saveLong("stopwatch_elapsed", 0L)
                        PersistentStorage.saveLong("stopwatch_last_start", 0L)
                        PersistentStorage.saveString("stopwatch_laps", "")
                    }
                }) {
                    Text(if (running) "Lap" else "Reset")
                }
            }
            Spacer(Modifier.height(16.dp))
            LazyColumn(Modifier.fillMaxWidth()) {
                items(laps.withIndex().toList()) { (i, lap) ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Lap ${laps.size - i}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(fmt(lap), fontFamily = FontFamily.Monospace)
                    }
                }
            }
        }
    }
}

