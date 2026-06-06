package pro.jayeshseth.devcontrols.ui.maps

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pro.jayeshseth.devcontrols.ui.PermGate

/** GPS tracker — record your path, total distance, current speed; draws the route live. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GpsTrackerScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = { TopAppBar(title = { Text("GPS Tracker") }, navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }) },
        modifier = Modifier.fillMaxSize()
    ) { inner ->
        Column(Modifier.padding(inner).fillMaxSize()) {
            PermGate(android.Manifest.permission.ACCESS_FINE_LOCATION, "Location is needed to track your route.") { TrackerContent() }
        }
    }
}

@SuppressLint("MissingPermission")
@Composable
private fun TrackerContent() {
    val context = LocalContextHolder()
    val pts = remember { mutableStateListOf<Pair<Double, Double>>() }
    var dist by remember { mutableDoubleStateOf(0.0) }
    var speed by remember { mutableFloatStateOf(0f) }
    var tracking by remember { mutableStateOf(false) }

    DisposableEffect(tracking) {
        if (!tracking) return@DisposableEffect onDispose { }
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val ll = LocationListener { loc: Location ->
            val last = pts.lastOrNull()
            if (last != null) dist += GeoMath.distanceMeters(last.first, last.second, loc.latitude, loc.longitude)
            pts.add(loc.latitude to loc.longitude)
            speed = loc.speed
        }
        runCatching { lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000L, 0f, ll) }
        onDispose { runCatching { lm.removeUpdates(ll) } }
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
            Stat("Distance", GeoMath.fmtDist(dist))
            Stat("Speed", "%.1f m/s".format(speed))
            Stat("Points", "${pts.size}")
        }
        Spacer(Modifier.height(12.dp))
        Box(Modifier.fillMaxWidth().weight(1f).background(Color(0xFF0C0E16)).drawBehind {
            if (pts.size < 2) return@drawBehind
            val lats = pts.map { it.first }; val lons = pts.map { it.second }
            val minLa = lats.min(); val maxLa = lats.max(); val minLo = lons.min(); val maxLo = lons.max()
            val spanLa = (maxLa - minLa).coerceAtLeast(1e-6); val spanLo = (maxLo - minLo).coerceAtLeast(1e-6)
            val pad = 40f
            fun sx(lo: Double) = pad + ((lo - minLo) / spanLo).toFloat() * (size.width - 2 * pad)
            fun sy(la: Double) = size.height - pad - ((la - minLa) / spanLa).toFloat() * (size.height - 2 * pad)
            for (i in 1 until pts.size) drawLine(Color(0xFF22FFAA), Offset(sx(pts[i - 1].second), sy(pts[i - 1].first)), Offset(sx(pts[i].second), sy(pts[i].first)), 4f)
            drawCircle(Color(0xFFFF5252), 8f, Offset(sx(pts.last().second), sy(pts.last().first)))
        })
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { tracking = !tracking }, modifier = Modifier.weight(1f)) { Text(if (tracking) "Stop" else "Start tracking") }
            OutlinedButton(onClick = { pts.clear(); dist = 0.0 }) { Text("Reset") }
        }
        Spacer(Modifier.height(8.dp))
        Text("Walk around with GPS — the route auto-scales to fit. Needs a clear sky for a fix.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun Stat(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontWeight = FontWeight.Bold, fontSize = 18.sp, fontFamily = FontFamily.Monospace)
    }
}

@Composable
private fun LocalContextHolder(): Context = androidx.compose.ui.platform.LocalContext.current
