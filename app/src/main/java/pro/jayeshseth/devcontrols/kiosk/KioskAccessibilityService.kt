package pro.jayeshseth.devcontrols.kiosk

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.os.SystemClock
import android.provider.Settings
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import pro.jayeshseth.devcontrols.data.DevSettingsRepository
import pro.jayeshseth.devcontrols.data.ForceDarkPrefs
import pro.jayeshseth.devcontrols.data.HideDevPrefs
import pro.jayeshseth.devcontrols.data.KioskPrefs

/**
 * Multi-purpose accessibility service. Two unrelated behaviours share the same
 * window-state-changed event stream because both react to "a new app came to
 * the foreground":
 *
 * 1. Kiosk relaunch — if Kiosk Mode is active and the user swiped home, bounce
 *    them back to the picked target.
 * 2. Hide Dev — if the foreground app is on the Hide-Dev allow-list, silently
 *    set DEVELOPMENT_SETTINGS_ENABLED and ADB_ENABLED to 0; when a non-target
 *    app comes back, restore the saved values. Mimics Geto-style per-app
 *    dev-options hiding without needing Shizuku.
 * 3. Force Dark — if the foreground app is on the Force-Dark allow-list,
 *    toggle Settings.Secure.accessibility_display_inversion_enabled to 1 to
 *    invert system colours (an effective dark-mode shim for apps without
 *    their own dark theme). Restored on leaving the target app.
 */
class KioskAccessibilityService : AccessibilityService() {

    private val launcherPackages: Set<String> by lazy { resolveLauncherPackages() }
    private val ioScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    @Volatile private var lastRelaunchAt: Long = 0L

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        Log.d(TAG, "service connected. launchers=$launcherPackages")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        event ?: return
        val type = event.eventType
        if (type != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED &&
            type != AccessibilityEvent.TYPE_WINDOWS_CHANGED
        ) return

        val pkg = event.packageName?.toString() ?: return

        // Ignore our own UI and the system shade for both handlers.
        if (pkg == packageName) return
        if (pkg.isSystemUi()) return

        handleKiosk(pkg)
        handleHideDev(pkg)
        handleForceDark(pkg)
    }

    override fun onInterrupt() {}

    override fun onDestroy() {
        if (instance === this) instance = null
        super.onDestroy()
    }

    private fun handleKiosk(pkg: String) {
        val prefs = KioskPrefs.get(applicationContext)
        val (targetPkg, _) = runBlocking { prefs.targetSnapshot() }
        if (targetPkg == null) return
        if (pkg == targetPkg) return

        val active = runBlocking { prefs.isKioskActiveOnce() }
        if (!active) return
        if (pkg !in launcherPackages) return

        val now = SystemClock.uptimeMillis()
        if (now - lastRelaunchAt < DEBOUNCE_MS) return
        lastRelaunchAt = now

        val launch = packageManager.getLaunchIntentForPackage(targetPkg) ?: return
        launch.addFlags(
            Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or
                Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
        )
        runCatching { startActivity(launch) }
            .onFailure { Log.w(TAG, "kiosk relaunch failed", it) }
    }

    private fun handleHideDev(pkg: String) {
        val prefs = HideDevPrefs.get(applicationContext)
        if (!runBlocking { prefs.isEnabledOnce() }) return
        val targets = runBlocking { prefs.targetsOnce() }
        if (targets.isEmpty()) return
        val includeWifi = runBlocking { prefs.includeWifiAdbOnce() }

        val repo = DevSettingsRepository.get(applicationContext)
        val isTarget = pkg in targets

        ioScope.launch {
            val esc = prefs.readEscrow()
            if (isTarget) {
                if (!esc.active) {
                    val currentDev = repo.currentDeveloperOptions()
                    val currentAdb = repo.currentUsbDebugging()
                    val currentWifi = repo.currentWirelessDebugging()
                    Log.d(
                        TAG,
                        "hideDev: $pkg foreground — saving dev=$currentDev adb=$currentAdb wifi=$currentWifi (includeWifi=$includeWifi)"
                    )
                    prefs.snapshotEscrow(currentDev, currentAdb, currentWifi)
                    if (currentDev) repo.setDeveloperOptions(false)
                    if (currentAdb) repo.setUsbDebugging(false)
                    if (includeWifi && currentWifi) repo.setWirelessDebugging(false)
                }
            } else {
                if (esc.active) {
                    Log.d(
                        TAG,
                        "hideDev: $pkg foreground — restoring dev=${esc.savedDev} adb=${esc.savedAdb} wifi=${esc.savedWifiAdb}"
                    )
                    repo.setDeveloperOptions(esc.savedDev)
                    repo.setUsbDebugging(esc.savedAdb)
                    if (includeWifi) repo.setWirelessDebugging(esc.savedWifiAdb)
                    prefs.clearEscrow()
                }
            }
        }
    }

    private fun handleForceDark(pkg: String) {
        val prefs = ForceDarkPrefs.get(applicationContext)
        if (!runBlocking { prefs.isEnabledOnce() }) return
        val targets = runBlocking { prefs.targetsOnce() }
        if (targets.isEmpty()) return

        val resolver = applicationContext.contentResolver
        val isTarget = pkg in targets

        ioScope.launch {
            val (escrowActive, savedInversion) = prefs.readEscrow()
            if (isTarget) {
                if (!escrowActive) {
                    val current = Settings.Secure.getInt(
                        resolver, INVERSION_KEY, 0
                    )
                    Log.d(TAG, "forceDark: $pkg foreground — saving inversion=$current → 1")
                    prefs.snapshotEscrow(current)
                    if (current != 1) {
                        Settings.Secure.putInt(resolver, INVERSION_KEY, 1)
                    }
                }
            } else {
                if (escrowActive) {
                    Log.d(TAG, "forceDark: $pkg foreground — restoring inversion=$savedInversion")
                    Settings.Secure.putInt(resolver, INVERSION_KEY, savedInversion)
                    prefs.clearEscrow()
                }
            }
        }
    }

    private fun String.isSystemUi(): Boolean =
        this == "com.android.systemui" || startsWith("com.android.systemui.")

    private fun resolveLauncherPackages(): Set<String> {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        return packageManager.queryIntentActivities(intent, 0)
            .map { it.activityInfo.packageName }
            .toSet()
    }

    companion object {
        private const val TAG = "KioskA11y"
        private const val DEBOUNCE_MS = 250L
        private const val INVERSION_KEY = "accessibility_display_inversion_enabled"

        @Volatile
        private var instance: KioskAccessibilityService? = null

        fun isRunning(): Boolean = instance != null
    }
}
