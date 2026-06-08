package pro.jayeshseth.devcontrols.tile

import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.widget.Toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import pro.jayeshseth.devcontrols.data.KioskPrefs
import pro.jayeshseth.devcontrols.kiosk.AnimationScales
import pro.jayeshseth.devcontrols.kiosk.KioskAccessibilityService
import pro.jayeshseth.devcontrols.kiosk.KioskNotification
import pro.jayeshseth.devcontrols.util.PermissionUtils
import pro.jayeshseth.devcontrols.widget.ToggleActionCallback

/**
 * Start/stop toggle for the accessibility-driven kiosk mode. Tapping ON launches
 * the picked target app + activates the relaunch-on-home behavior. Tapping OFF
 * just deactivates.
 */
class KioskModeTile : TileService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val prefs by lazy { KioskPrefs.get(applicationContext) }

    override fun onStartListening() {
        super.onStartListening()
        refresh()
    }

    override fun onClick() {
        super.onClick()
        scope.launch {
            val (pkg, label) = prefs.targetSnapshot()
            val active = prefs.isKioskActiveOnce()

            if (active) {
                prefs.setKioskActive(false)
                KioskNotification.cancel(applicationContext)
                AnimationScales.restore(applicationContext)
                Toast.makeText(applicationContext, "Kiosk Mode stopped", Toast.LENGTH_SHORT).show()
                refresh()
                return@launch
            }

            if (pkg == null) {
                val pickerIntent = Intent().apply {
                    setClassName(applicationContext, "pro.jayeshseth.devcontrols.MainActivity")
                    putExtra(ToggleActionCallback.EXTRA_OPEN_KIOSK_PICKER, true)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                launchAndCollapse(pickerIntent)
                Toast.makeText(applicationContext, "Pick a Kiosk target first", Toast.LENGTH_SHORT).show()
                return@launch
            }

            if (!KioskAccessibilityService.isRunning()) {
                Toast.makeText(
                    applicationContext,
                    "Grant Accessibility permission first (see app)",
                    Toast.LENGTH_LONG
                ).show()
                val mainIntent = Intent().apply {
                    setClassName(applicationContext, "pro.jayeshseth.devcontrols.MainActivity")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                launchAndCollapse(mainIntent)
                return@launch
            }

            if (!PermissionUtils.hasPostNotifications(applicationContext)) {
                Toast.makeText(
                    applicationContext,
                    "Grant Notifications permission first — open Dev Controls to be prompted.",
                    Toast.LENGTH_LONG
                ).show()
                val mainIntent = Intent().apply {
                    setClassName(applicationContext, "pro.jayeshseth.devcontrols.MainActivity")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                launchAndCollapse(mainIntent)
                return@launch
            }

            prefs.setKioskActive(true)
            if (prefs.isFastAnimationsOnce()) {
                AnimationScales.disable(applicationContext)
            }
            KioskNotification.show(applicationContext, label ?: pkg)

            val launch = applicationContext.packageManager.getLaunchIntentForPackage(pkg)
            if (launch != null) {
                launch.addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or
                        Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
                )
                launchAndCollapse(launch)
            }
            Toast.makeText(
                applicationContext,
                "Kiosk Mode active — home bounces back to ${label ?: pkg}",
                Toast.LENGTH_LONG
            ).show()
            refresh()
        }
    }

    private fun launchAndCollapse(intent: Intent) {
        runCatching {
            if (Build.VERSION.SDK_INT >= 34) {
                val pi = PendingIntent.getActivity(
                    this, intent.hashCode(), intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                startActivityAndCollapse(pi)
            } else {
                @Suppress("DEPRECATION")
                startActivityAndCollapse(intent)
            }
        }.onFailure { startActivity(intent) }
    }

    private fun refresh() {
        val tile = qsTile ?: return
        val (pkg, label) = runBlocking { prefs.targetSnapshot() }
        val active = runBlocking { prefs.isKioskActiveOnce() }
        if (pkg == null) {
            tile.state = Tile.STATE_INACTIVE
            tile.subtitle = "Pick target"
        } else {
            tile.state = if (active) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
            tile.subtitle = if (active) "Locked to ${label ?: pkg}" else (label ?: pkg)
        }
        tile.updateTile()
    }
}
