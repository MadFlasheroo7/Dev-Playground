package pro.jayeshseth.devcontrols.ui

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.wifi.WifiManager
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp

/** Wi-Fi Scanner — nearby APs with SSID, RSSI bars, channel/frequency, security. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WifiScannerLabScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = { TopAppBar(title = { Text("Wi-Fi Scanner") }, navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }) },
        modifier = Modifier.fillMaxSize()
    ) { inner ->
        Column(Modifier.padding(inner).fillMaxSize()) {
            PermGate(android.Manifest.permission.ACCESS_FINE_LOCATION, "Location permission is required to list nearby Wi-Fi networks.") {
                WifiContent()
            }
        }
    }
}

private data class Ap(val ssid: String, val bssid: String, val rssi: Int, val freq: Int, val caps: String)

@SuppressLint("MissingPermission")
@Composable
private fun WifiContent() {
    val context = LocalContext.current
    val wifi = remember { context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager }
    var aps by remember { mutableStateOf<List<Ap>>(emptyList()) }

    fun read() {
        aps = runCatching {
            wifi.scanResults.map { Ap(it.SSID.ifBlank { "(hidden)" }, it.BSSID, it.level, it.frequency, it.capabilities) }
                .sortedByDescending { it.rssi }
        }.getOrDefault(emptyList())
    }

    DisposableEffect(Unit) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context?, i: Intent?) { read() }
        }
        context.registerReceiver(receiver, IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION))
        read()
        @Suppress("DEPRECATION") runCatching { wifi.startScan() }
        onDispose { runCatching { context.unregisterReceiver(receiver) } }
    }

    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("${aps.size} networks", style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
            TextButton(onClick = { @Suppress("DEPRECATION") runCatching { wifi.startScan() }; read() }) { Text("Rescan") }
        }
        LazyColumn(Modifier.fillMaxSize().padding(horizontal = 12.dp)) {
            items(aps) { ap ->
                Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(ap.ssid, style = MaterialTheme.typography.bodyMedium)
                        Text("${ap.bssid} · ${channelOf(ap.freq)} · ${security(ap.caps)}",
                            style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontFamily = FontFamily.Monospace)
                    }
                    Text("${ap.rssi} dBm", style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.width(8.dp))
                    Box(Modifier.width(40.dp).height(10.dp).padding(start = 4.dp)) {
                        val frac = ((ap.rssi + 100) / 60f).coerceIn(0f, 1f)
                        androidx.compose.foundation.Canvas(Modifier.fillMaxSize()) {
                            drawRect(Color(0x33888888), size = size)
                            drawRect(if (frac > 0.5f) Color(0xFF4CAF50) else Color(0xFFFFB300),
                                size = androidx.compose.ui.geometry.Size(size.width * frac, size.height))
                        }
                    }
                }
                HorizontalDivider()
            }
        }
    }
}

private fun channelOf(freq: Int): String = when {
    freq in 2412..2484 -> "2.4GHz ch${(freq - 2407) / 5}"
    freq in 5160..5885 -> "5GHz ch${(freq - 5000) / 5}"
    freq >= 5955 -> "6GHz"
    else -> "$freq MHz"
}

private fun security(caps: String): String = when {
    caps.contains("WPA3") || caps.contains("SAE") -> "WPA3"
    caps.contains("WPA2") || caps.contains("RSN") -> "WPA2"
    caps.contains("WPA") -> "WPA"
    caps.contains("WEP") -> "WEP"
    else -> "Open"
}
