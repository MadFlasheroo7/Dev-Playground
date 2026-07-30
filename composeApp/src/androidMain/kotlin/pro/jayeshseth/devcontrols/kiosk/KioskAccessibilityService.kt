package pro.jayeshseth.devcontrols.kiosk

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.provider.Settings
import android.util.Log
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import pro.jayeshseth.devcontrols.data.ButtonAction
import pro.jayeshseth.devcontrols.data.ButtonMapperPrefs
import pro.jayeshseth.devcontrols.data.DevSettingsRepository
import pro.jayeshseth.devcontrols.data.ForceDarkPrefs
import pro.jayeshseth.devcontrols.data.HideDevPrefs
import pro.jayeshseth.devcontrols.data.KioskPrefs
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CameraCharacteristics
import android.content.BroadcastReceiver
import android.content.IntentFilter
import android.database.ContentObserver

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

    private val mainHandler = Handler(Looper.getMainLooper())
    private var activeKeyCode: Int = KeyEvent.KEYCODE_UNKNOWN
    private var isVolumeKeyLongPressed = false
    private var activeAction: ButtonAction = ButtonAction.DEFAULT
    private var torchActive = false

    private val volumeLongPressRunnable = Runnable {
        isVolumeKeyLongPressed = true
        performMappedAction(activeAction)
    }

    // ── Screen-off volume monitoring ──────────────────────────────────────────
    @Volatile private var isScreenOff = false
    private var screenOffLastVolume = -1
    private var screenOffOriginalVolume = -1
    private var screenOffConsecutiveChanges = 0
    private var screenOffChangeDirection = 0   // +1 = up, -1 = down
    private var screenOffFirstChangeTime = 0L

    /**
     * Fires whenever the system music volume changes.
     * When screen is off this is the only way to detect that a volume key was pressed.
     * We count consecutive same-direction changes within [SCREEN_OFF_WINDOW_MS] ms;
     * once the count reaches [SCREEN_OFF_LONG_PRESS_STEPS] we treat it as a long-press,
     * restore the original volume silently, and execute the mapped action.
     */
    private val volumeContentObserver = object : ContentObserver(mainHandler) {
        override fun onChange(selfChange: Boolean) {
            if (!isScreenOff) return
            onScreenOffVolumeChanged()
        }
    }

    /** Tracks screen on/off to activate the ContentObserver path only when needed. */
    private val screenStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val am = getSystemService(Context.AUDIO_SERVICE) as AudioManager
            when (intent.action) {
                Intent.ACTION_SCREEN_OFF -> {
                    isScreenOff = true
                    screenOffLastVolume = am.getStreamVolume(AudioManager.STREAM_MUSIC)
                    resetScreenOffDetection()
                    Log.d(TAG, "Screen OFF — volume monitor armed")
                }
                Intent.ACTION_SCREEN_ON -> {
                    isScreenOff = false
                    resetScreenOffDetection()
                    Log.d(TAG, "Screen ON — volume monitor disarmed")
                }
            }
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        Log.d(TAG, "service connected. launchers=$launcherPackages")
        // Sync initial screen state in case service starts while screen is already off
        val pm = getSystemService(Context.POWER_SERVICE) as android.os.PowerManager
        isScreenOff = !pm.isInteractive
        if (isScreenOff) {
            screenOffLastVolume = (getSystemService(Context.AUDIO_SERVICE) as AudioManager)
                .getStreamVolume(AudioManager.STREAM_MUSIC)
        }
        setupScreenOffMonitoring()
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
        teardownScreenOffMonitoring()
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

    override fun onKeyEvent(event: KeyEvent?): Boolean {
        event ?: return false
        val keyCode = event.keyCode
        if (keyCode != KeyEvent.KEYCODE_VOLUME_UP && keyCode != KeyEvent.KEYCODE_VOLUME_DOWN) {
            return super.onKeyEvent(event)
        }

        val action = event.action
        // Use the cached prefs — no blocking or coroutines needed on the main thread
        val mapperPrefs = ButtonMapperPrefs.get(applicationContext)

        if (action == KeyEvent.ACTION_DOWN) {
            if (event.repeatCount == 0) {
                // Determine if locked — purely synchronous calls, no blocking
                val km = getSystemService(Context.KEYGUARD_SERVICE) as android.app.KeyguardManager
                val pm = getSystemService(Context.POWER_SERVICE) as android.os.PowerManager
                val isLocked = km.isKeyguardLocked || !pm.isInteractive

                // Read from in-memory cache — instant, no deadlock
                // Fallback: if this state's action is DEFAULT, try the other state's action.
                // This means configuring a "Locked" action also applies when Unlocked
                // unless an explicit Unlocked action is separately configured.
                val buttonAction = if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
                    val locked = mapperPrefs.cachedVolUpLocked
                    val unlocked = mapperPrefs.cachedVolUpUnlocked
                    if (isLocked) {
                        locked.takeIf { it != ButtonAction.DEFAULT } ?: unlocked
                    } else {
                        unlocked.takeIf { it != ButtonAction.DEFAULT } ?: locked
                    }
                } else {
                    val locked = mapperPrefs.cachedVolDownLocked
                    val unlocked = mapperPrefs.cachedVolDownUnlocked
                    if (isLocked) {
                        locked.takeIf { it != ButtonAction.DEFAULT } ?: unlocked
                    } else {
                        unlocked.takeIf { it != ButtonAction.DEFAULT } ?: locked
                    }
                }
                Log.d(TAG, "Vol key $keyCode | isLocked=$isLocked → action=${buttonAction.name}")

                activeAction = buttonAction
                activeKeyCode = keyCode
                isVolumeKeyLongPressed = false

                if (buttonAction == ButtonAction.DEFAULT) {
                    // Pass through — let system handle volume normally
                    return false
                }

                // Intercept: schedule long-press detection
                mainHandler.removeCallbacks(volumeLongPressRunnable)
                mainHandler.postDelayed(volumeLongPressRunnable, 500L)
                return true
            } else {
                // Repeat events for held key — only consume if we own this press
                return activeAction != ButtonAction.DEFAULT
            }
        } else if (action == KeyEvent.ACTION_UP) {
            mainHandler.removeCallbacks(volumeLongPressRunnable)
            if (activeKeyCode == keyCode) {
                activeKeyCode = KeyEvent.KEYCODE_UNKNOWN
                if (activeAction != ButtonAction.DEFAULT) {
                    if (!isVolumeKeyLongPressed) {
                        // Short tap on a remapped key → still adjust volume normally
                        adjustSystemVolume(keyCode)
                    }
                    return true
                }
            }
        }
        return super.onKeyEvent(event)
    }

    private fun performMappedAction(action: ButtonAction) {
        Log.d(TAG, "Executing mapped action: ${action.name}")
        when (action) {
            ButtonAction.PLAY_PAUSE -> dispatchMediaKey(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)
            ButtonAction.SKIP_NEXT -> dispatchMediaKey(KeyEvent.KEYCODE_MEDIA_NEXT)
            ButtonAction.SKIP_PREV -> dispatchMediaKey(KeyEvent.KEYCODE_MEDIA_PREVIOUS)
            ButtonAction.TOGGLE_FLASHLIGHT -> toggleFlashlight()
            ButtonAction.TOGGLE_MUTE -> toggleAudioMute()
            else -> {}
        }
    }

    private fun dispatchMediaKey(mediaKeyCode: Int) {
        val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val downEvent = KeyEvent(KeyEvent.ACTION_DOWN, mediaKeyCode)
        val upEvent = KeyEvent(KeyEvent.ACTION_UP, mediaKeyCode)
        audioManager.dispatchMediaKeyEvent(downEvent)
        audioManager.dispatchMediaKeyEvent(upEvent)
    }

    private fun toggleFlashlight() {
        val cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
        try {
            val cameraId = cameraManager.cameraIdList.firstOrNull { id ->
                val characteristics = cameraManager.getCameraCharacteristics(id)
                characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            }
            if (cameraId != null) {
                torchActive = !torchActive
                cameraManager.setTorchMode(cameraId, torchActive)
                Log.d(TAG, "Flashlight toggled to: $torchActive")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error toggling flashlight", e)
        }
    }

    private fun toggleAudioMute() {
        val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                val isMuted = audioManager.isStreamMute(AudioManager.STREAM_MUSIC)
                audioManager.adjustStreamVolume(
                    AudioManager.STREAM_MUSIC,
                    if (isMuted) AudioManager.ADJUST_UNMUTE else AudioManager.ADJUST_MUTE,
                    AudioManager.FLAG_SHOW_UI
                )
            } else {
                val current = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                if (current > 0) {
                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 0, AudioManager.FLAG_SHOW_UI)
                } else {
                    val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, max / 3, AudioManager.FLAG_SHOW_UI)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error toggling audio mute", e)
        }
    }

    private fun adjustSystemVolume(keyCode: Int) {
        val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val direction = if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
            AudioManager.ADJUST_RAISE
        } else {
            AudioManager.ADJUST_LOWER
        }
        audioManager.adjustStreamVolume(
            AudioManager.STREAM_MUSIC,
            direction,
            AudioManager.FLAG_SHOW_UI
        )
    }

    private fun String.isSystemUi(): Boolean =
        this == "com.android.systemui" || startsWith("com.android.systemui.")

    private fun resolveLauncherPackages(): Set<String> {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        return packageManager.queryIntentActivities(intent, 0)
            .map { it.activityInfo.packageName }
            .toSet()
    }

    // ── Screen-off monitoring setup / teardown ───────────────────────────────

    private fun setupScreenOffMonitoring() {
        contentResolver.registerContentObserver(
            Settings.System.CONTENT_URI, true, volumeContentObserver
        )
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_SCREEN_ON)
        }
        registerReceiver(screenStateReceiver, filter)
    }

    private fun teardownScreenOffMonitoring() {
        runCatching { contentResolver.unregisterContentObserver(volumeContentObserver) }
        runCatching { unregisterReceiver(screenStateReceiver) }
    }

    private fun resetScreenOffDetection() {
        screenOffConsecutiveChanges = 0
        screenOffChangeDirection = 0
        screenOffFirstChangeTime = 0L
    }

    /**
     * Called by [volumeContentObserver] whenever the system music volume changes while screen is off.
     *
     * Long-press detection algorithm:
     *  - Tracks consecutive changes in the **same** direction within [SCREEN_OFF_WINDOW_MS] ms.
     *  - Once [SCREEN_OFF_LONG_PRESS_STEPS] changes are detected the gesture is confirmed.
     *  - The original volume is silently restored so the user hears no net change.
     *  - [AudioManager.dispatchMediaKeyEvent] routes the media action directly to whatever
     *    music app (Spotify, YouTube Music, etc.) currently holds audio focus — no
     *    MediaSession priority fighting needed.
     */
    private fun onScreenOffVolumeChanged() {
        val am = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val current = am.getStreamVolume(AudioManager.STREAM_MUSIC)

        if (screenOffLastVolume < 0) {
            screenOffLastVolume = current
            return
        }

        val rawDiff = current - screenOffLastVolume
        if (rawDiff == 0) return

        val direction = if (rawDiff > 0) 1 else -1
        val now = SystemClock.uptimeMillis()
        val elapsed = if (screenOffFirstChangeTime > 0L) now - screenOffFirstChangeTime else 0L
        val isNewGesture = direction != screenOffChangeDirection || elapsed > SCREEN_OFF_WINDOW_MS

        if (isNewGesture) {
            // Record the volume *before* this first step so we can restore it exactly
            screenOffOriginalVolume = screenOffLastVolume
            screenOffConsecutiveChanges = 1
            screenOffChangeDirection = direction
            screenOffFirstChangeTime = now
        } else {
            screenOffConsecutiveChanges++
        }
        screenOffLastVolume = current

        if (screenOffConsecutiveChanges < SCREEN_OFF_LONG_PRESS_STEPS) return

        // ── Long press confirmed ──────────────────────────────────────────────
        val keyCode = if (direction > 0) KeyEvent.KEYCODE_VOLUME_UP else KeyEvent.KEYCODE_VOLUME_DOWN
        val prefs = ButtonMapperPrefs.get(applicationContext)
        val action = if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
            prefs.cachedVolUpLocked.takeIf { it != ButtonAction.DEFAULT } ?: prefs.cachedVolUpUnlocked
        } else {
            prefs.cachedVolDownLocked.takeIf { it != ButtonAction.DEFAULT } ?: prefs.cachedVolDownUnlocked
        }

        if (action == ButtonAction.DEFAULT) return

        Log.d(TAG, "Screen-off long press: dir=$direction steps=$screenOffConsecutiveChanges → ${action.name}")

        // Restore volume silently — rawDiff = 0 on next observer call, safe to reset state first
        am.setStreamVolume(AudioManager.STREAM_MUSIC, screenOffOriginalVolume, 0)
        screenOffLastVolume = screenOffOriginalVolume
        resetScreenOffDetection()

        performMappedAction(action)
    }

    companion object {
        private const val TAG = "KioskA11y"
        private const val DEBOUNCE_MS = 250L
        private const val INVERSION_KEY = "accessibility_display_inversion_enabled"

        /** Number of consecutive same-direction volume steps to confirm a long-press when screen is off. */
        private const val SCREEN_OFF_LONG_PRESS_STEPS = 3

        /**
         * Max time window (ms) for all steps of a single long-press gesture.
         * Key-repeat initial delay is ~500 ms, subsequent repeats ~100 ms,
         * so 3 steps span ~600 ms. 800 ms gives comfortable headroom.
         */
        private const val SCREEN_OFF_WINDOW_MS = 800L

        @Volatile
        private var instance: KioskAccessibilityService? = null

        fun isRunning(): Boolean = instance != null
    }
}
