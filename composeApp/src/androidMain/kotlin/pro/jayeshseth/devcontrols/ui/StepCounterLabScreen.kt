package pro.jayeshseth.devcontrols.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Step counter — TYPE_STEP_COUNTER since you opened the screen (needs ACTIVITY_RECOGNITION). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StepCounterLabScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val needPerm = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
    var granted by remember {
        mutableStateOf(!needPerm || android.content.pm.PackageManager.PERMISSION_GRANTED ==
            context.checkSelfPermission(Manifest.permission.ACTIVITY_RECOGNITION))
    }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted = it }
    var baseline by remember { mutableFloatStateOf(-1f) }
    var steps by remember { mutableFloatStateOf(0f) }

    DisposableEffect(granted) {
        if (!granted) return@DisposableEffect onDispose { }
        val sm = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        val sensor = sm?.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
        val listener = object : SensorEventListener {
            override fun onSensorChanged(e: SensorEvent) {
                val total = e.values[0]
                if (baseline < 0f) baseline = total
                steps = total - baseline
            }
            override fun onAccuracyChanged(s: Sensor?, a: Int) {}
        }
        if (sensor != null) sm.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_UI)
        onDispose { sm?.unregisterListener(listener) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Step Counter") },
                navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }
            )
        },
        modifier = Modifier.fillMaxSize()
    ) { inner ->
        Column(Modifier.padding(inner).fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            if (!granted) {
                Text("Needs the Physical Activity permission to read the step sensor.")
                Button(onClick = { launcher.launch(Manifest.permission.ACTIVITY_RECOGNITION) }) { Text("Grant") }
                return@Column
            }
            Spacer(Modifier.height(64.dp))
            Text("${steps.toInt()}", fontSize = 80.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Text("steps since opening", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Text("Walk around — the hardware step counter updates in bursts.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
