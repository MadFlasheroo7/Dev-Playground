package pro.jayeshseth.devcontrols.ui.net

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
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket

private val COMMON_PORTS = listOf(21 to "ftp", 22 to "ssh", 23 to "telnet", 53 to "dns", 80 to "http", 139 to "smb", 443 to "https", 445 to "smb", 3306 to "mysql", 3389 to "rdp", 5555 to "adb", 8080 to "http-alt", 8443 to "https-alt", 62078 to "ios")

/** Ping & port scan — reachability + open common TCP ports on a host. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PingScanScreen(onBack: () -> Unit) {
    var host by remember { mutableStateOf("192.168.1.1") }
    var ping by remember { mutableStateOf("") }
    var scanning by remember { mutableStateOf(false) }
    val open = remember { mutableStateListOf<String>() }
    val scope = rememberCoroutineScope()

    fun run() {
        scope.launch {
            open.clear(); ping = "pinging…"; scanning = true
            ping = withContext(Dispatchers.IO) {
                runCatching {
                    val t0 = System.currentTimeMillis()
                    val ok = InetAddress.getByName(host).isReachable(1500)
                    if (ok) "reachable in ${System.currentTimeMillis() - t0} ms" else "no ICMP reply (host may still be up)"
                }.getOrElse { "error: ${it.message}" }
            }
            withContext(Dispatchers.IO) {
                for ((p, name) in COMMON_PORTS) {
                    val isOpen = runCatching { Socket().use { it.connect(InetSocketAddress(host, p), 400); true } }.getOrDefault(false)
                    if (isOpen) open.add("$p  $name")
                }
            }
            scanning = false
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Ping & Port Scan") }, navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }) },
        modifier = Modifier.fillMaxSize()
    ) { inner ->
        Column(Modifier.padding(inner).fillMaxSize().padding(16.dp)) {
            Row {
                OutlinedTextField(host, { host = it }, label = { Text("Host / IP") }, singleLine = true, modifier = Modifier.weight(1f))
                Spacer(Modifier.width(8.dp))
                Button(onClick = { run() }, enabled = !scanning, modifier = Modifier.padding(top = 8.dp)) { Text(if (scanning) "…" else "Scan") }
            }
            Spacer(Modifier.height(8.dp))
            Text(ping, color = MaterialTheme.colorScheme.primary, fontFamily = FontFamily.Monospace)
            Spacer(Modifier.height(8.dp))
            Text("Open ports (${open.size})", style = MaterialTheme.typography.titleSmall)
            LazyColumn(Modifier.fillMaxWidth()) {
                items(open) { Text(it, fontFamily = FontFamily.Monospace, color = Color(0xFF4CAF50), modifier = Modifier.padding(vertical = 3.dp)) }
            }
        }
    }
}
