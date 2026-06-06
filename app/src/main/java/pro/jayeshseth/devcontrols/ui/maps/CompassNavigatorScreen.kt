package pro.jayeshseth.devcontrols.ui.maps

import android.annotation.SuppressLint
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import pro.jayeshseth.devcontrols.ui.PermGate

/** Compass navigator — points an arrow toward a target coordinate, with bearing + distance. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompassNavigatorScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = { TopAppBar(title = { Text("Compass Navigator") }, navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }) },
        modifier = Modifier.fillMaxSize()
    ) { inner ->
        Column(Modifier.padding(inner).fillMaxSize()) {
            PermGate(android.Manifest.permission.ACCESS_FINE_LOCATION, "Location is needed to navigate to a coordinate.") { NavContent() }
        }
    }
}

@SuppressLint("MissingPermission")
@Composable
private fun NavContent() {
    val context = LocalContext.current
    var heading by remember { mutableFloatStateOf(0f) }
    var here by remember { mutableStateOf<Location?>(null) }
    var tLat by remember { mutableStateOf("28.6139") }
    var tLon by remember { mutableStateOf("77.2090") }

    DisposableEffect(Unit) {
        val sm = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val rot = FloatArray(9); val ori = FloatArray(3)
        val sl = object : SensorEventListener {
            override fun onSensorChanged(e: SensorEvent) {
                SensorManager.getRotationMatrixFromVector(rot, e.values)
                SensorManager.getOrientation(rot, ori)
                val deg = ((Math.toDegrees(ori[0].toDouble()) + 360) % 360).toFloat()
                heading = heading + ((deg - heading + 540) % 360 - 180) * 0.15f
            }
            override fun onAccuracyChanged(s: Sensor?, a: Int) {}
        }
        sm.registerListener(sl, sm.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR), SensorManager.SENSOR_DELAY_UI)
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val ll = LocationListener { here = it }
        runCatching { here = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER) ?: lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER) }
        runCatching { lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000L, 1f, ll) }
        runCatching { lm.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 1000L, 1f, ll) }
        onDispose { sm.unregisterListener(sl); runCatching { lm.removeUpdates(ll) } }
    }

    val tla = tLat.toDoubleOrNull(); val tlo = tLon.toDoubleOrNull()
    val loc = here
    val bearing = if (loc != null && tla != null && tlo != null) GeoMath.bearing(loc.latitude, loc.longitude, tla, tlo) else 0.0
    val dist = if (loc != null && tla != null && tlo != null) GeoMath.distanceMeters(loc.latitude, loc.longitude, tla, tlo) else 0.0

    Column(Modifier.fillMaxSize().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Row { OutlinedTextField(tLat, { tLat = it }, label = { Text("target lat") }, singleLine = true, modifier = Modifier.weight(1f)); Spacer(Modifier.width(8.dp)); OutlinedTextField(tLon, { tLon = it }, label = { Text("target lon") }, singleLine = true, modifier = Modifier.weight(1f)) }
        Spacer(Modifier.height(16.dp))
        Box(Modifier.size(240.dp), contentAlignment = Alignment.Center) {
            val onSurf = MaterialTheme.colorScheme.onSurface
            Canvas(Modifier.fillMaxSize()) {
                val c = Offset(size.width / 2, size.height / 2); val r = size.minDimension / 2 * 0.9f
                drawCircle(onSurf.copy(alpha = 0.2f), r, c, style = Stroke(3f))
                // arrow points to (bearing - heading)
                val ang = Math.toRadians((bearing - heading).toDouble())
                rotate(Math.toDegrees(ang).toFloat(), c) {
                    val tip = Offset(c.x, c.y - r * 0.8f)
                    val p = Path().apply { moveTo(tip.x, tip.y); lineTo(c.x - 26f, c.y + 20f); lineTo(c.x, c.y); lineTo(c.x + 26f, c.y + 20f); close() }
                    drawPath(p, Color(0xFFFF5252))
                }
            }
            Text("${dist.let { GeoMath.fmtDist(it) }}", color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 120.dp))
        }
        Spacer(Modifier.height(16.dp))
        Text(if (loc == null) "Acquiring location…" else "Bearing %.0f° · heading %.0f°".format(bearing, heading), fontFamily = FontFamily.Monospace)
        if (loc != null) Text("You: %.5f, %.5f".format(loc.latitude, loc.longitude), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(8.dp))
        Text("Hold the phone flat; the red arrow points toward the target.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
