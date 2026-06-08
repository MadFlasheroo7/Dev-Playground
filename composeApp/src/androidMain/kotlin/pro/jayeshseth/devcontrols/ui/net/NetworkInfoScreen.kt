package pro.jayeshseth.devcontrols.ui.net

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily

/** Network info — transport, bandwidth, DNS, link addresses of the active connection. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NetworkInfoScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val rows = remember { gather(context) }
    Scaffold(
        topBar = { TopAppBar(title = { Text("Network Info") }, navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }) },
        modifier = Modifier.fillMaxSize()
    ) { inner ->
        LazyColumn(Modifier.padding(inner).fillMaxSize().padding(16.dp)) {
            items(rows) { (k, v) ->
                Column(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                    Text(k, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                    Text(v, style = MaterialTheme.typography.bodyMedium, fontFamily = FontFamily.Monospace)
                }
            }
        }
    }
}

private fun gather(context: Context): List<Pair<String, String>> {
    val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val net = cm.activeNetwork ?: return listOf("Status" to "No active network")
    val cap = cm.getNetworkCapabilities(net)
    val lp = cm.getLinkProperties(net)
    fun transport() = buildList {
        cap ?: return@buildList
        if (cap.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) add("Wi-Fi")
        if (cap.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) add("Cellular")
        if (cap.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) add("Ethernet")
        if (cap.hasTransport(NetworkCapabilities.TRANSPORT_VPN)) add("VPN")
    }.joinToString()
    return buildList {
        add("Transport" to transport())
        add("Downstream" to "${cap?.linkDownstreamBandwidthKbps ?: 0} kbps")
        add("Upstream" to "${cap?.linkUpstreamBandwidthKbps ?: 0} kbps")
        add("Validated" to "${cap?.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)}")
        add("Metered" to "${!(cap?.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED) ?: true)}")
        add("Interface" to (lp?.interfaceName ?: "?"))
        add("Link addresses" to (lp?.linkAddresses?.joinToString { it.toString() } ?: "?"))
        add("DNS servers" to (lp?.dnsServers?.joinToString { it.hostAddress ?: "" } ?: "?"))
        add("Domains" to (lp?.domains ?: "—"))
        add("Routes" to (lp?.routes?.joinToString("\n") { it.toString() } ?: "?"))
        add("Private DNS" to "${lp?.privateDnsServerName ?: "off/auto"}")
    }
}
