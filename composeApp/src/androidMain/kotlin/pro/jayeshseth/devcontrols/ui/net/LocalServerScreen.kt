package pro.jayeshseth.devcontrols.ui.net

import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import pro.jayeshseth.devcontrols.util.WifiUtils
import java.net.ServerSocket
import kotlin.concurrent.thread

/** Local HTTP server — serves a page on the LAN; hit it from your laptop's browser. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocalServerScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val ip = remember { WifiUtils.currentWifiIpv4(context) ?: "0.0.0.0" }
    val port = 8088
    var running by remember { mutableStateOf(false) }
    val log = remember { mutableStateListOf<String>() }
    var server by remember { mutableStateOf<ServerSocket?>(null) }

    fun start() {
        if (running) return
        running = true
        thread(name = "devserver") {
            runCatching {
                val ss = ServerSocket(port); server = ss
                while (running) {
                    val sock = ss.accept()
                    sock.use { s ->
                        val reader = s.getInputStream().bufferedReader()
                        val line = reader.readLine() ?: return@use
                        synchronized(log) { log.add(0, "${s.inetAddress.hostAddress}  $line") }
                        val body = "<html><body style='font-family:sans-serif;background:#111;color:#eee'>" +
                            "<h1>Dev Controls</h1><p>Served from your phone at $ip:$port</p>" +
                            "<p>Request: $line</p></body></html>"
                        val resp = "HTTP/1.1 200 OK\r\nContent-Type: text/html\r\nContent-Length: ${body.length}\r\nConnection: close\r\n\r\n$body"
                        s.getOutputStream().write(resp.toByteArray())
                    }
                }
            }.onFailure { synchronized(log) { log.add(0, "server stopped: ${it.message}") } }
        }
    }
    fun stop() { running = false; runCatching { server?.close() } }
    DisposableEffect(Unit) { onDispose { stop() } }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Local Server") }, navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }) },
        modifier = Modifier.fillMaxSize()
    ) { inner ->
        Column(Modifier.padding(inner).fillMaxSize().padding(16.dp)) {
            Text(if (running) "http://$ip:$port" else "Stopped", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            Text("Open that URL in a browser on the same Wi-Fi.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(8.dp))
            Button(onClick = { if (running) stop() else start() }) { Text(if (running) "Stop server" else "Start server") }
            Spacer(Modifier.height(12.dp))
            Text("Requests", style = MaterialTheme.typography.titleSmall)
            LazyColumn(Modifier.fillMaxWidth()) { items(log) { Text(it, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(vertical = 3.dp)) } }
        }
    }
}
