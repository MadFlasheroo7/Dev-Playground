package pro.jayeshseth.devcontrols.widget

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.updateAll
import kotlinx.coroutines.flow.first
import pro.jayeshseth.devcontrols.data.DevSettingsRepository
import pro.jayeshseth.devcontrols.data.FocusCallsPrefs
import pro.jayeshseth.devcontrols.data.KioskPrefs
import pro.jayeshseth.devcontrols.kiosk.AnimationScales
import pro.jayeshseth.devcontrols.kiosk.KioskAccessibilityService
import pro.jayeshseth.devcontrols.kiosk.KioskNotification
import pro.jayeshseth.devcontrols.util.PermissionUtils

object ToggleKeys {
    const val DEV_OPTIONS = "dev_options"
    const val USB_DEBUGGING = "usb_debugging"
    const val WIRELESS_DEBUGGING = "wireless_debugging"
    const val NEVER_TIMEOUT = "never_timeout"
    const val KIOSK_MODE = "kiosk_mode"
    const val FOCUS_CALLS = "focus_calls"

    val KEY_TOGGLE = ActionParameters.Key<String>("toggle")
}

class ToggleActionCallback : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val which = parameters[ToggleKeys.KEY_TOGGLE] ?: return
        val repo = DevSettingsRepository.get(context)
        val focus = FocusCallsPrefs.get(context)
        when (which) {
            ToggleKeys.DEV_OPTIONS ->
                repo.setDeveloperOptions(!repo.currentDeveloperOptions())
            ToggleKeys.USB_DEBUGGING ->
                repo.setUsbDebugging(!repo.currentUsbDebugging())
            ToggleKeys.WIRELESS_DEBUGGING ->
                repo.setWirelessDebugging(!repo.currentWirelessDebugging())
            ToggleKeys.NEVER_TIMEOUT ->
                repo.setNeverTimeout(!repo.currentNeverTimeout())
            ToggleKeys.KIOSK_MODE ->
                toggleKiosk(context)
            ToggleKeys.FOCUS_CALLS -> {
                val current = focus.focusCallsEnabled.first()
                focus.setFocusCallsEnabled(!current)
            }
        }
        DevControlsWidget().updateAll(context)
    }

    private suspend fun toggleKiosk(context: Context) {
        val kiosk = KioskPrefs.get(context)
        val (pkg, label) = kiosk.targetSnapshot()
        val currentlyActive = kiosk.isKioskActiveOnce()

        if (currentlyActive) {
            kiosk.setKioskActive(false)
            KioskNotification.cancel(context)
            AnimationScales.restore(context)
            Toast.makeText(context, "Kiosk Mode stopped", Toast.LENGTH_SHORT).show()
            return
        }

        if (pkg == null) {
            val pickerIntent = Intent().apply {
                setClassName(context, "pro.jayeshseth.devcontrols.MainActivity")
                putExtra(EXTRA_OPEN_KIOSK_PICKER, true)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(pickerIntent)
            Toast.makeText(context, "Pick a Kiosk target first", Toast.LENGTH_SHORT).show()
            return
        }

        if (!KioskAccessibilityService.isRunning()) {
            Toast.makeText(
                context,
                "Grant the Accessibility permission first (see app)",
                Toast.LENGTH_LONG
            ).show()
            val mainIntent = Intent().apply {
                setClassName(context, "pro.jayeshseth.devcontrols.MainActivity")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(mainIntent)
            return
        }

        if (!PermissionUtils.hasPostNotifications(context)) {
            Toast.makeText(
                context,
                "Grant the Notifications permission first — Kiosk needs the persistent Stop notification. Open Dev Controls to be prompted.",
                Toast.LENGTH_LONG
            ).show()
            val mainIntent = Intent().apply {
                setClassName(context, "pro.jayeshseth.devcontrols.MainActivity")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(mainIntent)
            return
        }

        kiosk.setKioskActive(true)
        if (kiosk.isFastAnimationsOnce()) {
            AnimationScales.disable(context)
        }
        KioskNotification.show(context, label ?: pkg)

        val launch = context.packageManager.getLaunchIntentForPackage(pkg)
        if (launch != null) {
            launch.addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or
                    Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
            )
            context.startActivity(launch)
        }
        Toast.makeText(
            context,
            "Kiosk Mode active — home/recents bounce back to ${label ?: pkg}",
            Toast.LENGTH_LONG
        ).show()
    }

    companion object {
        const val EXTRA_OPEN_KIOSK_PICKER = "open_kiosk_picker"
    }
}
