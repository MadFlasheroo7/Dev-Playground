package pro.jayeshseth.devcontrols.ui

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp

/** Sensors Dashboard — live readout of every available [SensorManager] sensor. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SensorsDashboardLabScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val sm = remember { context.getSystemService(Context.SENSOR_SERVICE) as SensorManager }
    val sensors = remember { sm.getSensorList(Sensor.TYPE_ALL).sortedBy { it.name } }
    val values = remember { androidx.compose.runtime.mutableStateMapOf<Int, FloatArray>() }

    DisposableEffect(Unit) {
        val listener = object : SensorEventListener {
            override fun onSensorChanged(e: SensorEvent) { values[e.sensor.type] = e.values.copyOf() }
            override fun onAccuracyChanged(s: Sensor?, a: Int) {}
        }
        sensors.forEach { sm.registerListener(listener, it, SensorManager.SENSOR_DELAY_UI) }
        onDispose { sm.unregisterListener(listener) }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Sensors (${sensors.size})") }, navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }) },
        modifier = Modifier.fillMaxSize()
    ) { inner ->
        LazyColumn(Modifier.padding(inner).fillMaxSize().padding(horizontal = 16.dp)) {
            items(sensors) { s ->
                val v = values[s.type]
                Column(Modifier.padding(vertical = 8.dp)) {
                    Text(s.name, style = MaterialTheme.typography.bodyMedium)
                    Text(
                        v?.joinToString("  ") { String.format("%.3f", it) } ?: "…",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontFamily = FontFamily.Monospace
                    )
                    Text("${s.vendor} · ${s.stringType?.removePrefix("android.sensor.") ?: ""}",
                        style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                HorizontalDivider()
            }
        }
    }
}
