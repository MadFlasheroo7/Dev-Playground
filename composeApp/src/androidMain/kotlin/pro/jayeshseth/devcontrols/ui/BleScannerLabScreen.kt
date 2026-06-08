package pro.jayeshseth.devcontrols.ui

import android.Manifest
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/** BLE scanner — lists nearby advertisements (name, address, RSSI). Needs BLUETOOTH_SCAN. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BleScannerLabScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val perms = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
        arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT)
    else arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
    var granted by remember {
        mutableStateOf(perms.all { context.checkSelfPermission(it) == android.content.pm.PackageManager.PERMISSION_GRANTED })
    }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { res ->
        granted = res.values.all { it }
    }
    var scanning by remember { mutableStateOf(false) }
    val devices = remember { mutableStateMapOf<String, Pair<String, Int>>() }  // addr -> (name, rssi)

    DisposableEffect(scanning, granted) {
        if (!scanning || !granted) return@DisposableEffect onDispose { }
        val mgr = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        val scanner = mgr?.adapter?.takeIf { it.isEnabled }?.bluetoothLeScanner
        val cb = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                val name = runCatching { result.device.name }.getOrNull() ?: result.scanRecord?.deviceName ?: "(unknown)"
                devices[result.device.address] = name to result.rssi
            }
        }
        runCatching { scanner?.startScan(cb) }
        onDispose { runCatching { scanner?.stopScan(cb) } }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("BLE Scanner") },
                navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }
            )
        },
        modifier = Modifier.fillMaxSize()
    ) { inner ->
        Column(Modifier.padding(inner).fillMaxSize().padding(12.dp)) {
            if (!granted) {
                Text("Bluetooth scan permission required.")
                Button(onClick = { launcher.launch(perms) }) { Text("Grant") }
                return@Column
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { devices.clear(); scanning = !scanning }) { Text(if (scanning) "Stop" else "Scan") }
                Text("${devices.size} found", Modifier.padding(12.dp))
            }
            LazyColumn(Modifier.fillMaxSize().padding(top = 8.dp)) {
                items(devices.entries.sortedByDescending { it.value.second }) { (addr, v) ->
                    Box(Modifier.fillMaxWidth().padding(vertical = 4.dp).clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant).padding(12.dp)) {
                        Column {
                            Text(v.first, fontWeight = FontWeight.Bold)
                            Text("$addr · ${v.second} dBm", fontFamily = FontFamily.Monospace,
                                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}
