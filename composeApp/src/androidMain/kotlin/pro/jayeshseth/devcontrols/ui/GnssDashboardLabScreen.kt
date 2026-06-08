package pro.jayeshseth.devcontrols.ui

import android.annotation.SuppressLint
import android.content.Context
import android.location.GnssStatus
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp

/** GNSS Dashboard — fix (lat/long/alt/speed) + per-satellite C/N0 bars via GnssStatus. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GnssDashboardLabScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = { TopAppBar(title = { Text("GNSS Dashboard") }, navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }) },
        modifier = Modifier.fillMaxSize()
    ) { inner ->
        Column(Modifier.padding(inner).fillMaxSize()) {
            PermGate(android.Manifest.permission.ACCESS_FINE_LOCATION, "Location permission is needed for GPS/GNSS data.") {
                GnssContent()
            }
        }
    }
}

@SuppressLint("MissingPermission")
@Composable
private fun GnssContent() {
    val context = LocalContext.current
    val lm = remember { context.getSystemService(Context.LOCATION_SERVICE) as LocationManager }
    var loc by remember { mutableStateOf<Location?>(null) }
    var sats by remember { mutableStateOf<List<Triple<Int, Float, Boolean>>>(emptyList()) } // svid, cn0, usedInFix

    DisposableEffect(Unit) {
        val locListener = LocationListener { loc = it }
        val gnss = object : GnssStatus.Callback() {
            override fun onSatelliteStatusChanged(status: GnssStatus) {
                sats = (0 until status.satelliteCount).map {
                    Triple(status.getSvid(it), status.getCn0DbHz(it), status.usedInFix(it))
                }.sortedByDescending { it.second }
            }
        }
        runCatching { lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000L, 0f, locListener) }
        runCatching { lm.registerGnssStatusCallback(gnss, null) }
        onDispose { runCatching { lm.removeUpdates(locListener) }; runCatching { lm.unregisterGnssStatusCallback(gnss) } }
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                val l = loc
                Text(if (l != null) "%.6f, %.6f".format(l.latitude, l.longitude) else "Acquiring fix…",
                    style = MaterialTheme.typography.titleMedium, fontFamily = FontFamily.Monospace)
                if (l != null) {
                    Text("alt ${"%.0f".format(l.altitude)} m   speed ${"%.1f".format(l.speed)} m/s   acc ±${"%.0f".format(l.accuracy)} m",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        val used = sats.count { it.third }
        Text("Satellites: ${sats.size} in view, $used in fix", style = MaterialTheme.typography.titleSmall)
        Spacer(Modifier.height(8.dp))
        Box(Modifier.fillMaxWidth().weight(1f).drawBehind {
            if (sats.isEmpty()) return@drawBehind
            val bw = size.width / sats.size
            sats.forEachIndexed { i, (_, cn0, usedInFix) ->
                val h = (cn0 / 50f).coerceIn(0f, 1f) * size.height
                drawRect(if (usedInFix) Color(0xFF4CAF50) else Color(0xFF90A4AE),
                    Offset(i * bw + 1, size.height - h), Size(bw - 2, h))
            }
        })
        Text("Bars = C/N0 (signal, dB-Hz). Green = used in fix. Go outdoors for a fix.",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
