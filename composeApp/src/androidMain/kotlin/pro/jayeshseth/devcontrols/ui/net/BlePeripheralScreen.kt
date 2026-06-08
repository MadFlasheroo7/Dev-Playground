package pro.jayeshseth.devcontrols.ui.net

import android.annotation.SuppressLint
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattServer
import android.bluetooth.BluetoothGattServerCallback
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothManager
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.content.Context
import android.os.Build
import android.os.ParcelUuid
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import pro.jayeshseth.devcontrols.ui.PermGateMulti
import java.util.UUID

private val SERVICE_UUID = UUID.fromString("0000d1ce-0000-1000-8000-00805f9b34fb")
private val CHAR_UUID = UUID.fromString("0000d1cf-0000-1000-8000-00805f9b34fb")

/** BLE peripheral — advertise a custom GATT service so other devices can discover this phone. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BlePeripheralScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = { TopAppBar(title = { Text("BLE Peripheral") }, navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }) },
        modifier = Modifier.fillMaxSize()
    ) { inner ->
        Column(Modifier.padding(inner).fillMaxSize()) {
            val perms = if (Build.VERSION.SDK_INT >= 31)
                arrayOf(android.Manifest.permission.BLUETOOTH_ADVERTISE, android.Manifest.permission.BLUETOOTH_CONNECT)
            else arrayOf(android.Manifest.permission.BLUETOOTH)
            PermGateMulti(perms, "Bluetooth permissions are needed to advertise.") { BleContent() }
        }
    }
}

@SuppressLint("MissingPermission")
@Composable
private fun BleContent() {
    val context = LocalContext.current
    var status by remember { mutableStateOf("Starting…") }
    var connections by remember { mutableStateOf(0) }

    DisposableEffect(Unit) {
        val mgr = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val adapter = mgr.adapter
        var server: BluetoothGattServer? = null
        var advertiser = adapter?.bluetoothLeAdvertiser
        val advCb = object : AdvertiseCallback() {
            override fun onStartSuccess(s: AdvertiseSettings?) { status = "Advertising as 'DevControls'\nService $SERVICE_UUID" }
            override fun onStartFailure(errorCode: Int) { status = "Advertise failed (code $errorCode)" }
        }
        if (adapter == null || !adapter.isEnabled) {
            status = "Enable Bluetooth first."
        } else {
            runCatching {
                server = mgr.openGattServer(context, object : BluetoothGattServerCallback() {
                    override fun onConnectionStateChange(device: android.bluetooth.BluetoothDevice?, st: Int, newState: Int) {
                        connections += if (newState == android.bluetooth.BluetoothProfile.STATE_CONNECTED) 1 else -1
                    }
                })
                val svc = BluetoothGattService(SERVICE_UUID, BluetoothGattService.SERVICE_TYPE_PRIMARY)
                svc.addCharacteristic(BluetoothGattCharacteristic(CHAR_UUID, BluetoothGattCharacteristic.PROPERTY_READ, BluetoothGattCharacteristic.PERMISSION_READ))
                server?.addService(svc)
                advertiser?.startAdvertising(
                    AdvertiseSettings.Builder().setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY).setConnectable(true).build(),
                    AdvertiseData.Builder().setIncludeDeviceName(true).addServiceUuid(ParcelUuid(SERVICE_UUID)).build(),
                    advCb
                )
            }.onFailure { status = "Error: ${it.message}" }
        }
        onDispose { runCatching { advertiser?.stopAdvertising(advCb) }; runCatching { server?.close() } }
    }

    Column(Modifier.padding(16.dp)) {
        Text(status, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(12.dp))
        Text("Connected centrals: ${connections.coerceAtLeast(0)}", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(12.dp))
        Text("Scan from another phone (e.g. nRF Connect) — you'll see 'DevControls' advertising the custom service.",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
