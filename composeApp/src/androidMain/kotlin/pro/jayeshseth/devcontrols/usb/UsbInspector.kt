package pro.jayeshseth.devcontrols.usb

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbConstants
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.BatteryManager
import android.util.Log
import androidx.core.content.ContextCompat
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import java.io.File

/**
 * One-stop facade over `UsbManager` + the USB-related broadcasts + the
 * `/sys/class/power_supply/usb` sysfs tree (online, voltage, current, type).
 *
 * - [devices] is a Flow that re-emits the live list on every attach / detach.
 * - [cableState] is a Flow that tracks the phone's own USB cable state +
 *   currently exposed USB functions (MTP, PTP, ADB, RNDIS, MIDI, NCM, accessory).
 * - [readPowerProfile] is a one-shot sysfs read (voltage / current / source type).
 * - [requestPermission] / [hasPermission] expose the per-device permission flow
 *   so we can read serial numbers and open bulk pipes.
 */
class UsbInspector(private val ctx: Context) {

    private val um: UsbManager = ctx.getSystemService(Context.USB_SERVICE) as UsbManager

    // ---------------------------------------------------------------- devices

    val devices: Flow<List<UsbDeviceInfo>> = callbackFlow {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context, intent: Intent) {
                trySend(currentDevices())
            }
        }
        val filter = IntentFilter().apply {
            addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)
            addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
            addAction(USB_PERMISSION_ACTION)
        }
        ContextCompat.registerReceiver(
            ctx, receiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED
        )
        trySend(currentDevices())
        awaitClose { runCatching { ctx.unregisterReceiver(receiver) } }
    }.distinctUntilChanged()

    fun currentDevices(): List<UsbDeviceInfo> =
        um.deviceList.values.map { it.toInfo(um) }

    fun hasPermission(device: UsbDevice): Boolean = um.hasPermission(device)

    fun requestPermission(deviceName: String) {
        val device = um.deviceList[deviceName] ?: return
        if (um.hasPermission(device)) return
        val intent = PendingIntent.getBroadcast(
            ctx, 0,
            Intent(USB_PERMISSION_ACTION).setPackage(ctx.packageName),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )
        um.requestPermission(device, intent)
    }

    // ---------------------------------------------------------------- cable

    val cableState: Flow<UsbCableState> = callbackFlow {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context, intent: Intent) {
                trySend(snapshotCableState())
            }
        }
        val filter = IntentFilter().apply {
            addAction(USB_STATE_ACTION)
            addAction(Intent.ACTION_BATTERY_CHANGED)
            addAction(Intent.ACTION_POWER_CONNECTED)
            addAction(Intent.ACTION_POWER_DISCONNECTED)
        }
        ContextCompat.registerReceiver(
            ctx, receiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED
        )
        trySend(snapshotCableState())
        awaitClose { runCatching { ctx.unregisterReceiver(receiver) } }
    }.distinctUntilChanged()

    fun snapshotCableState(): UsbCableState {
        val usbSticky = ctx.registerReceiver(null, IntentFilter(USB_STATE_ACTION))
        val battSticky = ctx.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))

        val connected = usbSticky?.getBooleanExtra("connected", false) ?: false
        val configured = usbSticky?.getBooleanExtra("configured", false) ?: false
        val hostConnected = usbSticky?.getBooleanExtra("host_connected", false) ?: false
        val dataUnlocked = usbSticky?.getBooleanExtra("data_unlocked", false) ?: false

        val functions = buildList {
            usbSticky?.let { ex ->
                if (ex.getBooleanExtra("mtp", false)) add("MTP")
                if (ex.getBooleanExtra("ptp", false)) add("PTP")
                if (ex.getBooleanExtra("rndis", false)) add("RNDIS (USB tethering)")
                if (ex.getBooleanExtra("midi", false)) add("MIDI")
                if (ex.getBooleanExtra("ncm", false)) add("NCM (network)")
                if (ex.getBooleanExtra("accessory", false)) add("Accessory")
                if (ex.getBooleanExtra("adb", false)) add("ADB")
                if (ex.getBooleanExtra("uvc", false)) add("UVC (camera)")
                if (ex.getBooleanExtra("audio_source", false)) add("Audio source")
                if (ex.getBooleanExtra("unlocked", false)) add("Unlocked")
            }
        }

        val plugged = battSticky?.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1) ?: -1
        val pluggedAs = when (plugged) {
            0 -> PluggedAs.Nothing
            BatteryManager.BATTERY_PLUGGED_USB -> PluggedAs.Usb
            BatteryManager.BATTERY_PLUGGED_AC -> PluggedAs.Ac
            BatteryManager.BATTERY_PLUGGED_WIRELESS -> PluggedAs.Wireless
            BatteryManager.BATTERY_PLUGGED_DOCK -> PluggedAs.Dock
            -1 -> PluggedAs.Unknown
            else -> PluggedAs.Unknown
        }

        return UsbCableState(
            connected = connected,
            configured = configured,
            hostConnected = hostConnected,
            dataUnlocked = dataUnlocked,
            pluggedAs = pluggedAs,
            currentFunctions = functions,
            batteryPluggedRaw = plugged,
        )
    }

    // ---------------------------------------------------------------- power

    fun readPowerProfile(): UsbPowerProfile {
        fun readStr(path: String): String? = runCatching {
            File(path).takeIf { it.canRead() }?.readText()?.trim()
        }.getOrNull()
        fun readInt(path: String): Int? = readStr(path)?.toIntOrNull()

        val online = readInt("/sys/class/power_supply/usb/online")
        val voltageUv = readInt("/sys/class/power_supply/usb/voltage_now")
        val currentUa = readInt("/sys/class/power_supply/usb/current_now")
        val maxCurUa = readInt("/sys/class/power_supply/usb/current_max")
        val type = readStr("/sys/class/power_supply/usb/type")
        val realType = readStr("/sys/class/power_supply/usb/real_type")
        val pdProgress = readInt("/sys/class/power_supply/usb/pd_in_hard_reset")?.let { it != 0 }

        return UsbPowerProfile(
            online = online,
            voltageMv = voltageUv?.let { it / 1000 },
            currentMa = currentUa?.let { it / 1000 },
            maxCurrentMa = maxCurUa?.let { it / 1000 },
            type = type,
            realType = realType,
            ptInProgress = pdProgress,
        )
    }

    companion object {
        const val USB_STATE_ACTION = "android.hardware.usb.action.USB_STATE"
        const val USB_PERMISSION_ACTION = "pro.jayeshseth.devcontrols.usb.PERMISSION"

        /** Friendly name for `UsbConstants.USB_CLASS_*`. */
        fun classLabel(c: Int): String = when (c) {
            UsbConstants.USB_CLASS_PER_INTERFACE -> "Per-interface"
            UsbConstants.USB_CLASS_AUDIO -> "Audio"
            UsbConstants.USB_CLASS_CDC_DATA -> "CDC Data"
            UsbConstants.USB_CLASS_COMM -> "Communications"
            UsbConstants.USB_CLASS_CONTENT_SEC -> "Content security"
            UsbConstants.USB_CLASS_CSCID -> "Smartcard"
            UsbConstants.USB_CLASS_HID -> "HID"
            UsbConstants.USB_CLASS_HUB -> "Hub"
            UsbConstants.USB_CLASS_MASS_STORAGE -> "Mass storage"
            UsbConstants.USB_CLASS_MISC -> "Misc"
            UsbConstants.USB_CLASS_PHYSICA -> "Physical"
            UsbConstants.USB_CLASS_PRINTER -> "Printer"
            UsbConstants.USB_CLASS_STILL_IMAGE -> "Still image"
            UsbConstants.USB_CLASS_VENDOR_SPEC -> "Vendor-specific"
            UsbConstants.USB_CLASS_VIDEO -> "Video"
            UsbConstants.USB_CLASS_WIRELESS_CONTROLLER -> "Wireless controller"
            UsbConstants.USB_CLASS_APP_SPEC -> "App-specific"
            else -> "Class 0x%02x".format(c)
        }

        fun endpointTypeLabel(t: Int): String = when (t) {
            UsbConstants.USB_ENDPOINT_XFER_CONTROL -> "CONTROL"
            UsbConstants.USB_ENDPOINT_XFER_ISOC -> "ISOCHRONOUS"
            UsbConstants.USB_ENDPOINT_XFER_BULK -> "BULK"
            UsbConstants.USB_ENDPOINT_XFER_INT -> "INTERRUPT"
            else -> "type=$t"
        }

        fun directionLabel(d: Int): String =
            if (d == UsbConstants.USB_DIR_IN) "IN" else "OUT"

        private const val TAG = "UsbInspector"
    }
}

private fun UsbDevice.toInfo(um: UsbManager): UsbDeviceInfo {
    val grantedSerial = if (um.hasPermission(this)) {
        runCatching { serialNumber }.getOrNull()
    } else null

    val interfaceInfos = (0 until interfaceCount).map { i ->
        val iface = getInterface(i)
        val eps = (0 until iface.endpointCount).map { j ->
            val ep = iface.getEndpoint(j)
            UsbEndpointInfo(
                address = ep.address,
                direction = ep.direction,
                type = ep.type,
                attributes = ep.attributes,
                maxPacketSize = ep.maxPacketSize,
                interval = ep.interval,
            )
        }
        UsbInterfaceInfo(
            id = iface.id,
            alternateSetting = iface.alternateSetting,
            interfaceClass = iface.interfaceClass,
            interfaceSubclass = iface.interfaceSubclass,
            interfaceProtocol = iface.interfaceProtocol,
            name = iface.name,
            endpoints = eps,
        )
    }
    return UsbDeviceInfo(
        deviceName = deviceName,
        vendorId = vendorId,
        productId = productId,
        deviceClass = deviceClass,
        deviceSubclass = deviceSubclass,
        deviceProtocol = deviceProtocol,
        manufacturerName = manufacturerName,
        productName = productName,
        serialNumber = grantedSerial,
        version = version,
        deviceId = deviceId,
        hasPermission = um.hasPermission(this),
        interfaces = interfaceInfos,
    )
}
