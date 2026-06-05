package pro.jayeshseth.devcontrols.ui.net

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily

private val TYPES = listOf("_http._tcp.", "_googlecast._tcp.", "_airplay._tcp.", "_ipp._tcp.", "_raop._tcp.", "_spotify-connect._tcp.")

/** NSD/mDNS discovery — finds Bonjour services advertised on your LAN. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NsdDiscoveryScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val found = remember { mutableStateListOf<String>() }

    DisposableEffect(Unit) {
        val nsd = context.getSystemService(Context.NSD_SERVICE) as NsdManager
        val listeners = TYPES.map { type ->
            object : NsdManager.DiscoveryListener {
                override fun onServiceFound(s: NsdServiceInfo) {
                    val entry = "${s.serviceName}  (${s.serviceType})"
                    synchronized(found) { if (!found.contains(entry)) found.add(0, entry) }
                }
                override fun onServiceLost(s: NsdServiceInfo) {}
                override fun onDiscoveryStarted(t: String) {}
                override fun onDiscoveryStopped(t: String) {}
                override fun onStartDiscoveryFailed(t: String, e: Int) {}
                override fun onStopDiscoveryFailed(t: String, e: Int) {}
            }.also { runCatching { nsd.discoverServices(type, NsdManager.PROTOCOL_DNS_SD, it) } }
        }
        onDispose { listeners.forEach { runCatching { nsd.stopServiceDiscovery(it) } } }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("NSD Discovery") }, navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }) },
        modifier = Modifier.fillMaxSize()
    ) { inner ->
        Column(Modifier.padding(inner).fillMaxSize().padding(16.dp)) {
            Text("Discovering Bonjour/mDNS services (Cast, AirPlay, printers, Spotify…) on this Wi-Fi.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(8.dp))
            Text("Found: ${found.size}", style = MaterialTheme.typography.titleSmall)
            LazyColumn(Modifier.fillMaxWidth()) {
                items(found) {
                    Text(it, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(vertical = 6.dp))
                    HorizontalDivider()
                }
            }
        }
    }
}
