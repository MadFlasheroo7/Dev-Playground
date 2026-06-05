package pro.jayeshseth.devcontrols.admin

import android.app.admin.DeviceAdminReceiver

/**
 * Stub DeviceAdminReceiver. Presence enables a future `adb shell dpm set-device-owner
 * pro.jayeshseth.devcontrols/.admin.KioskAdminReceiver` upgrade so the app can take over
 * lock-task management without a manifest rewrite. Requires the device to have no
 * other accounts/users when device-owner is set.
 */
class KioskAdminReceiver : DeviceAdminReceiver()
