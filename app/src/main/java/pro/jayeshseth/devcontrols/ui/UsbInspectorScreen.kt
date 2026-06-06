package pro.jayeshseth.devcontrols.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import pro.jayeshseth.devcontrols.usb.PluggedAs
import pro.jayeshseth.devcontrols.usb.UsbCableState
import pro.jayeshseth.devcontrols.usb.UsbDeviceInfo
import pro.jayeshseth.devcontrols.usb.UsbInspector
import pro.jayeshseth.devcontrols.usb.UsbPowerProfile

/** USB inspector — lists connected USB devices and their descriptors. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UsbInspectorScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val inspector = remember { UsbInspector(context) }

    val cable by inspector.cableState.collectAsState(initial = inspector.snapshotCableState())
    val devices by inspector.devices.collectAsState(initial = inspector.currentDevices())

    val power by produceState<UsbPowerProfile?>(initialValue = inspector.readPowerProfile()) {
        while (true) {
            value = inspector.readPowerProfile()
            delay(1500)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("USB Inspector") },
                navigationIcon = {
                    TextButton(onClick = onBack) { Text("Back") }
                }
            )
        },
        modifier = Modifier.fillMaxSize()
    ) { inner ->
        Column(
            modifier = Modifier
                .padding(inner)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            CableCard(cable)
            Spacer(Modifier.height(12.dp))
            PowerCard(power)
            Spacer(Modifier.height(12.dp))
            DevicesCard(devices, onRequestPerm = { inspector.requestPermission(it) })
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun CableCard(s: UsbCableState) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .background(
                            if (s.connected) Color(0xFF4CAF50) else Color(0xFF9AA0A6),
                            RoundedCornerShape(50)
                        )
                )
                Spacer(Modifier.width(8.dp))
                Text("Cable state", style = MaterialTheme.typography.titleMedium)
            }
            Spacer(Modifier.height(6.dp))
            InfoRow("connected", s.connected.toString())
            InfoRow("configured", s.configured.toString())
            InfoRow("host_connected (we're the device, PC sees us)", s.hostConnected.toString())
            InfoRow("data_unlocked", s.dataUnlocked.toString())
            InfoRow("plugged as", s.pluggedAs.name)
            InfoRow(
                "active functions",
                if (s.currentFunctions.isEmpty()) "(charging only or none)"
                else s.currentFunctions.joinToString(", ")
            )
        }
    }
}

@Composable
private fun PowerCard(p: UsbPowerProfile?) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Power supply (sysfs)", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            Text(
                "/sys/class/power_supply/usb/* — re-read every 1.5 s. Some kernels gate these behind permission; null = unreadable.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(6.dp))
            if (p == null) {
                Text("No power-supply data available.", style = MaterialTheme.typography.bodySmall)
            } else {
                InfoRow("online", p.online?.toString() ?: "—")
                InfoRow("voltage", p.voltageMv?.let { "$it mV" } ?: "—")
                InfoRow("current draw", p.currentMa?.let { "$it mA" } ?: "—")
                InfoRow("current max", p.maxCurrentMa?.let { "$it mA" } ?: "—")
                InfoRow("type", p.type ?: "—")
                InfoRow("real_type", p.realType ?: "—")
                InfoRow("pd_in_hard_reset", p.ptInProgress?.toString() ?: "—")
                p.voltageMv?.let { v ->
                    p.currentMa?.let { i ->
                        val watts = (v * i) / 1_000_000.0
                        InfoRow("instant power", "%.2f W".format(watts))
                    }
                }
            }
        }
    }
}

@Composable
private fun DevicesCard(
    devices: List<UsbDeviceInfo>,
    onRequestPerm: (String) -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Connected USB devices (OTG host mode)",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Plug a device in via an OTG adapter to see it here. Live-updates on attach / detach.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            if (devices.isEmpty()) {
                Text(
                    "No USB devices currently connected.",
                    style = MaterialTheme.typography.bodySmall
                )
            } else {
                devices.forEachIndexed { idx, d ->
                    DeviceBlock(d, onRequestPerm)
                    if (idx < devices.lastIndex) {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun DeviceBlock(d: UsbDeviceInfo, onRequestPerm: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Column(modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded }) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    d.productName ?: "Unknown device",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    "${d.manufacturerName ?: "?"} · VID:PID = ${d.vidPid}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontFamily = FontFamily.Monospace
                )
            }
            Text(if (expanded) "▴" else "▾", style = MaterialTheme.typography.titleMedium)
        }
        InfoRow("device class", "${d.deviceClass} (${UsbInspector.classLabel(d.deviceClass)})")
        InfoRow("subclass / protocol", "${d.deviceSubclass} / ${d.deviceProtocol}")
        InfoRow("path", d.deviceName)
        InfoRow("version", d.version ?: "—")
        InfoRow("serial", d.serialNumber ?: if (d.hasPermission) "(none)" else "(need permission)")

        if (!d.hasPermission) {
            Spacer(Modifier.height(6.dp))
            OutlinedButton(
                onClick = { onRequestPerm(d.deviceName) },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Request USB permission") }
        }

        if (expanded) {
            Spacer(Modifier.height(8.dp))
            Text(
                "${d.interfaces.size} interface${if (d.interfaces.size == 1) "" else "s"}",
                style = MaterialTheme.typography.titleSmall
            )
            d.interfaces.forEach { iface ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text(
                            "iface #${iface.id} · ${UsbInspector.classLabel(iface.interfaceClass)} · sub=${iface.interfaceSubclass} proto=${iface.interfaceProtocol} alt=${iface.alternateSetting}",
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace
                        )
                        iface.name?.let {
                            Text("name: $it", style = MaterialTheme.typography.bodySmall)
                        }
                        iface.endpoints.forEach { ep ->
                            Text(
                                "ep 0x%02x  %-3s  %-11s  max=%d  interval=%d".format(
                                    ep.address,
                                    UsbInspector.directionLabel(ep.direction),
                                    UsbInspector.endpointTypeLabel(ep.type),
                                    ep.maxPacketSize,
                                    ep.interval
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoRow(key: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Text(
            key,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodySmall
        )
        Text(
            value,
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
