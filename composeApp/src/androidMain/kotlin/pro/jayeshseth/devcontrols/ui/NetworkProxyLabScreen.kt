package pro.jayeshseth.devcontrols.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import pro.jayeshseth.devcontrols.ui.adbtools.AdbRunner
import pro.jayeshseth.devcontrols.ui.adbtools.AdbToolScaffold
import pro.jayeshseth.devcontrols.util.WifiUtils

/**
 * Network & Proxy Lab — set a system HTTP proxy (route this device's traffic through
 * Burp / mitmproxy on your laptop), toggle radios, private DNS, and data-saver. All via
 * UID-2000 shell. Built for security-testing your own apps.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun NetworkProxyLabScreen(onBack: () -> Unit) {
    AdbToolScaffold("Network & Proxy", onBack) { runner ->
        val context = LocalContext.current
        val lanIp = remember { WifiUtils.currentWifiIpv4(context) }

        var host by remember { mutableStateOf(lanIp ?: "") }
        var port by remember { mutableStateOf("8080") }
        var currentProxy by remember { mutableStateOf("…") }
        var dns by remember { mutableStateOf("?") }

        fun refresh() {
            runner.run("settings get global http_proxy", toast = false) {
                currentProxy = it.output.trim().ifBlank { "(none)" }.let { v -> if (v == "null") "(none)" else v }
            }
            runner.run("settings get global private_dns_mode", toast = false) { dns = it.output.trim() }
        }
        LaunchedEffect(runner.connected) { if (runner.connected) refresh() }

        Spacer(Modifier.height(12.dp))
        SectionCard("HTTP proxy") {
            Text("This device's Wi-Fi IP: ${lanIp ?: "unknown"}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("Current: $currentProxy", fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(host, { host = it }, label = { Text("Proxy host") }, singleLine = true, modifier = Modifier.weight(2f))
                OutlinedTextField(port, { port = it }, label = { Text("Port") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = {
                    runner.run("settings put global http_proxy ${host.trim()}:${port.trim()}") { refresh() }
                }, enabled = host.isNotBlank() && port.isNotBlank()) { Text("Set proxy") }
                OutlinedButton(onClick = { runner.run("settings put global http_proxy :0") { refresh() } }) { Text("Clear") }
            }
            Text("Point host at the laptop running Burp/mitmproxy on the same Wi-Fi; install its CA to intercept TLS.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 6.dp))
        }

        Spacer(Modifier.height(12.dp))
        SectionCard("Radios") {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CmdChip("Wi-Fi on", runner) { run("svc wifi enable") }
                CmdChip("Wi-Fi off", runner) { run("svc wifi disable") }
                CmdChip("Data on", runner) { run("svc data enable") }
                CmdChip("Data off", runner) { run("svc data disable") }
                CmdChip("Airplane on", runner) { run("cmd connectivity airplane-mode enable") }
                CmdChip("Airplane off", runner) { run("cmd connectivity airplane-mode disable") }
            }
        }

        Spacer(Modifier.height(12.dp))
        SectionCard("Private DNS  (current: $dns)") {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CmdChip("Off", runner) { run("settings put global private_dns_mode off") { refresh() } }
                CmdChip("Auto", runner) { run("settings put global private_dns_mode opportunistic") { refresh() } }
                CmdChip("Cloudflare", runner) {
                    run("settings put global private_dns_specifier one.one.one.one") {}
                    run("settings put global private_dns_mode hostname") { refresh() }
                }
            }
        }

        Spacer(Modifier.height(12.dp))
        SectionCard("Data saver (background restriction)") {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CmdChip("Restrict bg ON", runner) { run("cmd netpolicy set restrict-background true") }
                CmdChip("Restrict bg OFF", runner) { run("cmd netpolicy set restrict-background false") }
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
internal fun SectionCard(title: String, content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontFamily = FontFamily.Default)
            Spacer(Modifier.height(8.dp))
            content()
        }
    }
}

@Composable
internal fun CmdChip(label: String, runner: AdbRunner, action: AdbRunner.() -> Unit) {
    AssistChip(onClick = { runner.action() }, label = { Text(label) }, enabled = !runner.busy)
}
