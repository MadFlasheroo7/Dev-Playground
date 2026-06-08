package pro.jayeshseth.devcontrols.usb

/** Snapshot of one USB device visible through `UsbManager.getDeviceList()`. */
data class UsbDeviceInfo(
    val deviceName: String,        // path-like, e.g. /dev/bus/usb/001/002
    val vendorId: Int,
    val productId: Int,
    val deviceClass: Int,
    val deviceSubclass: Int,
    val deviceProtocol: Int,
    val manufacturerName: String?,
    val productName: String?,
    val serialNumber: String?,      // null unless USB permission granted
    val version: String?,
    val deviceId: Int,
    val hasPermission: Boolean,
    val interfaces: List<UsbInterfaceInfo>,
) {
    val vidPid: String get() = "%04x:%04x".format(vendorId, productId)
}

data class UsbInterfaceInfo(
    val id: Int,
    val alternateSetting: Int,
    val interfaceClass: Int,
    val interfaceSubclass: Int,
    val interfaceProtocol: Int,
    val name: String?,
    val endpoints: List<UsbEndpointInfo>,
)

data class UsbEndpointInfo(
    val address: Int,
    val direction: Int,        // UsbConstants.USB_DIR_IN / USB_DIR_OUT
    val type: Int,             // UsbConstants.USB_ENDPOINT_XFER_*
    val attributes: Int,
    val maxPacketSize: Int,
    val interval: Int,
)

/** State of the USB cable (device-side: phone being plugged in as the device). */
data class UsbCableState(
    val connected: Boolean,
    val configured: Boolean,
    val hostConnected: Boolean,
    val dataUnlocked: Boolean,
    val pluggedAs: PluggedAs,
    val currentFunctions: List<String>,
    val batteryPluggedRaw: Int,
)

enum class PluggedAs { Nothing, Usb, Ac, Wireless, Dock, Unknown }

/** Power-supply data scraped from /sys/class/power_supply/usb/... */
data class UsbPowerProfile(
    val online: Int?,             // 1 = connected to a source
    val voltageMv: Int?,          // millivolts (raw is microvolts)
    val currentMa: Int?,          // milliamps (raw is microamps)
    val maxCurrentMa: Int?,
    val type: String?,            // USB, USB_PD, USB_DCP (charger), Mains, BPP, ...
    val realType: String?,        // some kernels expose real_type vs type
    val ptInProgress: Boolean?,   // PD in progress flag (if present)
)
