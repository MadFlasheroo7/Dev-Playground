package pro.jayeshseth.devcontrols.kiosk

import android.accessibilityservice.AccessibilityService
import android.app.KeyguardManager
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.database.ContentObserver
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.MediaStore
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
import pro.jayeshseth.devcontrols.data.TriggerConfig
import kotlin.math.abs

/**
 * Multi-purpose accessibility service handling:
 * 1. Kiosk relaunch
 * 2. Hide Dev options & ADB per-app
 * 3. Force Dark display inversion per-app
 * 4. Hardware System Button Mapping:
 *    - Hybrid Power button detection (rapid screen on/off transitions + KEYCODE_POWER)
 *    - Volume Rocker (double-press & long-press)
 *    - Volume Chord (simultaneous Vol Up + Vol Down)
 *    - Headset & Bluetooth media buttons (single, double, long-press)
 *    - Special keys (Assist, Camera)
 *    - Haptic feedback & full global/system/app action dispatching.
 */
class KioskAccessibilityService : AccessibilityService() {

    private val launcherPackages: Set<String> by lazy { resolveLauncherPackages() }
    private val ioScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    @Volatile private var lastRelaunchAt: Long = 0L

    private val mainHandler = Handler(Looper.getMainLooper())
    private var torchActive = false

    private val torchCallback: CameraManager.TorchCallback? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        object : CameraManager.TorchCallback() {
            override fun onTorchModeChanged(cameraId: String, enabled: Boolean) {
                torchActive = enabled
            }
        }
    } else null

    // ── Power Button Detection State ──────────────────────────────────────────
    private var lastPowerEventTime = 0L
    private var lastPowerKeyEventTime = 0L
    private var powerClickCount = 0
    private var powerGestureInitialLocked = false

    private val powerClickEvaluator = Runnable {
        val count = powerClickCount
        powerClickCount = 0
        val initialLocked = powerGestureInitialLocked
        val prefs = ButtonMapperPrefs.get(applicationContext)

        Log.d(TAG, "Evaluating power clicks: count=$count initialLocked=$initialLocked")
        when {
            count >= 3 && prefs.cachedPowerTriple.shouldTrigger(initialLocked) -> {
                performMappedAction(prefs.cachedPowerTriple)
            }
            count >= 2 && prefs.cachedPowerDouble.shouldTrigger(initialLocked) -> {
                performMappedAction(prefs.cachedPowerDouble)
            }
        }
    }

    // ── Volume Rocker & Chord State ───────────────────────────────────────────
    private var isVolUpPressed = false
    private var isVolDownPressed = false
    private var isChordTriggered = false

    private var activeVolKeyCode = KeyEvent.KEYCODE_UNKNOWN
    private var isVolLongPressed = false
    private var volPressDownTime = 0L
    private var isVolDoubleTapCandidate = false
    private var activeVolLongConfig: TriggerConfig = TriggerConfig()

    private var pendingVolTapKeyCode = KeyEvent.KEYCODE_UNKNOWN
    private var lastVolUpTapTime = 0L
    private var lastVolDownTapTime = 0L

    private val volLongPressRunnable = Runnable {
        isVolLongPressed = true
        performMappedAction(activeVolLongConfig)
    }

    private val volSingleTapRunnable = Runnable {
        val key = pendingVolTapKeyCode
        pendingVolTapKeyCode = KeyEvent.KEYCODE_UNKNOWN
        isVolDoubleTapCandidate = false
        if (key != KeyEvent.KEYCODE_UNKNOWN) {
            adjustSystemVolume(key)
        }
    }

    // ── Headset / Media Key State ─────────────────────────────────────────────
    private var isHeadsetLongPressed = false
    private var lastHeadsetTapTime = 0L
    private var pendingHeadsetSingleTap = false

    private val headsetLongPressRunnable = Runnable {
        isHeadsetLongPressed = true
        val prefs = ButtonMapperPrefs.get(applicationContext)
        val isLocked = isDeviceLockedOrScreenOff()
        if (prefs.cachedHeadsetLong.shouldTrigger(isLocked)) {
            performMappedAction(prefs.cachedHeadsetLong)
        }
    }

    private val headsetSingleTapRunnable = Runnable {
        pendingHeadsetSingleTap = false
        val prefs = ButtonMapperPrefs.get(applicationContext)
        val isLocked = isDeviceLockedOrScreenOff()
        if (prefs.cachedHeadsetSingle.shouldTrigger(isLocked)) {
            performMappedAction(prefs.cachedHeadsetSingle)
        } else {
            dispatchMediaKey(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)
        }
    }

    // ── Screen-off volume monitoring ──────────────────────────────────────────
    @Volatile private var isScreenOff = false
    private var screenOffLastVolume = -1
    private var screenOffOriginalVolume = -1
    private var screenOffConsecutiveChanges = 0
    private var screenOffChangeDirection = 0   // +1 = up, -1 = down
    private var screenOffFirstChangeTime = 0L

    private val volumeContentObserver = object : ContentObserver(mainHandler) {
        override fun onChange(selfChange: Boolean) {
            if (!isScreenOff) return
            onScreenOffVolumeChanged()
        }
    }

    /** Tracks screen on/off for power transitions and volume observer */
    private val screenStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val am = getSystemService(Context.AUDIO_SERVICE) as AudioManager
            val now = SystemClock.uptimeMillis()
            when (intent.action) {
                Intent.ACTION_SCREEN_OFF -> {
                    isScreenOff = true
                    screenOffLastVolume = am.getStreamVolume(AudioManager.STREAM_MUSIC)
                    resetScreenOffDetection()
                    Log.d(TAG, "Screen OFF")
                    // Dedupe if already counted by KEYCODE_POWER in onKeyEvent within 500ms
                    if (now - lastPowerKeyEventTime >= 500L) {
                        onPowerClickDetected(fromKeyEvent = false, isScreenOffTransition = true)
                    }
                }
                Intent.ACTION_SCREEN_ON -> {
                    isScreenOff = false
                    resetScreenOffDetection()
                    Log.d(TAG, "Screen ON")
                    if (now - lastPowerKeyEventTime >= 500L) {
                        onPowerClickDetected(fromKeyEvent = false, isScreenOffTransition = false)
                    }
                }
            }
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        Log.d(TAG, "Service connected. Launchers=$launcherPackages")
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
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

        if (pkg == packageName) return
        if (pkg.isSystemUi()) return

        handleKiosk(pkg)
        handleHideDev(pkg)
        handleForceDark(pkg)
    }

    override fun onInterrupt() {}

    override fun onDestroy() {
        teardownScreenOffMonitoring()
        mainHandler.removeCallbacksAndMessages(null)
        if (instance === this) instance = null
        super.onDestroy()
    }

    private fun isDeviceLockedOrScreenOff(): Boolean {
        val km = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        return km.isKeyguardLocked || !pm.isInteractive
    }

    // ── Power Click Detection ─────────────────────────────────────────────────

    private fun onPowerClickDetected(fromKeyEvent: Boolean, isScreenOffTransition: Boolean = false) {
        val now = SystemClock.uptimeMillis()
        if (fromKeyEvent) {
            lastPowerKeyEventTime = now
        }

        if (now - lastPowerEventTime < POWER_DEDUPE_WINDOW_MS) {
            return
        }
        lastPowerEventTime = now

        val prefs = ButtonMapperPrefs.get(applicationContext)

        if (powerClickCount == 0) {
            // Capture lock state at the start of the power click gesture sequence
            powerGestureInitialLocked = if (fromKeyEvent) {
                isDeviceLockedOrScreenOff()
            } else if (isScreenOffTransition) {
                // Transitioning from screen ON to OFF; check if keyguard was locked before transition
                val km = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
                km.isKeyguardLocked
            } else {
                // Transitioning from screen OFF to ON; device was off/locked
                true
            }
        }

        val initialLocked = powerGestureInitialLocked
        val hasDouble = prefs.cachedPowerDouble.shouldTrigger(initialLocked)
        val hasTriple = prefs.cachedPowerTriple.shouldTrigger(initialLocked)

        if (!hasDouble && !hasTriple) {
            powerClickCount = 0
            return
        }

        powerClickCount++
        mainHandler.removeCallbacks(powerClickEvaluator)

        if (powerClickCount >= 3 && hasTriple) {
            powerClickCount = 0
            performMappedAction(prefs.cachedPowerTriple)
        } else {
            mainHandler.postDelayed(powerClickEvaluator, POWER_WINDOW_MS)
        }
    }

    // ── Hardware Key Event Interception ───────────────────────────────────────

    override fun onKeyEvent(event: KeyEvent?): Boolean {
        event ?: return false
        val keyCode = event.keyCode
        val action = event.action
        val prefs = ButtonMapperPrefs.get(applicationContext)
        val isLocked = isDeviceLockedOrScreenOff()

        // 1. Power Button
        if (keyCode == KeyEvent.KEYCODE_POWER) {
            if (action == KeyEvent.ACTION_DOWN && event.repeatCount == 0) {
                onPowerClickDetected(fromKeyEvent = true)
            }
            val hasCustom = prefs.cachedPowerDouble.shouldTrigger(isLocked) ||
                prefs.cachedPowerTriple.shouldTrigger(isLocked)
            return if (hasCustom) true else super.onKeyEvent(event)
        }

        // 2. Volume Buttons & Volume Chord
        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP || keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            if (action == KeyEvent.ACTION_DOWN) {
                if (event.repeatCount == 0) {
                    if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) isVolUpPressed = true
                    if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) isVolDownPressed = true

                    // Check simultaneous Volume Chord
                    if (isVolUpPressed && isVolDownPressed) {
                        isChordTriggered = true
                        mainHandler.removeCallbacks(volLongPressRunnable)
                        mainHandler.removeCallbacks(volSingleTapRunnable)
                        pendingVolTapKeyCode = KeyEvent.KEYCODE_UNKNOWN
                        isVolDoubleTapCandidate = false
                        if (prefs.cachedVolChord.shouldTrigger(isLocked)) {
                            performMappedAction(prefs.cachedVolChord)
                        }
                        return true
                    }

                    val longConfig = if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) prefs.cachedVolUpLong else prefs.cachedVolDownLong
                    val doubleConfig = if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) prefs.cachedVolUpDouble else prefs.cachedVolDownDouble
                    val chordActive = prefs.cachedVolChord.shouldTrigger(isLocked)

                    val hasCustomAction = longConfig.shouldTrigger(isLocked) ||
                        doubleConfig.shouldTrigger(isLocked) ||
                        chordActive

                    if (!hasCustomAction) {
                        return super.onKeyEvent(event)
                    }

                    // If a single-tap was pending for this key, cancel its timer so it doesn't fire while held down
                    if (pendingVolTapKeyCode == keyCode) {
                        mainHandler.removeCallbacks(volSingleTapRunnable)
                        isVolDoubleTapCandidate = true
                    } else {
                        isVolDoubleTapCandidate = false
                    }

                    activeVolKeyCode = keyCode
                    isVolLongPressed = false
                    volPressDownTime = SystemClock.uptimeMillis()

                    if (longConfig.shouldTrigger(isLocked)) {
                        activeVolLongConfig = longConfig
                        mainHandler.removeCallbacks(volLongPressRunnable)
                        mainHandler.postDelayed(volLongPressRunnable, LONG_PRESS_TIMEOUT_MS)
                    }
                    return true
                } else {
                    // Repeat events
                    val longConfig = if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) prefs.cachedVolUpLong else prefs.cachedVolDownLong
                    if (!longConfig.shouldTrigger(isLocked) && !isChordTriggered) {
                        adjustSystemVolume(keyCode)
                        return true
                    }
                    return isChordTriggered || isVolLongPressed || (activeVolKeyCode == keyCode)
                }
            } else if (action == KeyEvent.ACTION_UP) {
                mainHandler.removeCallbacks(volLongPressRunnable)
                if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) isVolUpPressed = false
                if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) isVolDownPressed = false

                if (isChordTriggered) {
                    if (!isVolUpPressed && !isVolDownPressed) {
                        isChordTriggered = false
                    }
                    return true
                }

                if (activeVolKeyCode == keyCode) {
                    activeVolKeyCode = KeyEvent.KEYCODE_UNKNOWN
                    if (isVolLongPressed) {
                        isVolLongPressed = false
                        isVolDoubleTapCandidate = false
                        pendingVolTapKeyCode = KeyEvent.KEYCODE_UNKNOWN
                        return true
                    }

                    val now = SystemClock.uptimeMillis()
                    val pressDuration = now - volPressDownTime

                    // If button was held down past LONG_PRESS_TIMEOUT_MS, it is not a tap
                    if (pressDuration > LONG_PRESS_TIMEOUT_MS) {
                        isVolDoubleTapCandidate = false
                        pendingVolTapKeyCode = KeyEvent.KEYCODE_UNKNOWN
                        return true
                    }

                    // Short release: check for double tap
                    val doubleConfig = if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) prefs.cachedVolUpDouble else prefs.cachedVolDownDouble
                    if (doubleConfig.shouldTrigger(isLocked)) {
                        val lastTap = if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) lastVolUpTapTime else lastVolDownTapTime
                        if (isVolDoubleTapCandidate || (now - lastTap <= DOUBLE_TAP_WINDOW_MS && pendingVolTapKeyCode == keyCode)) {
                            // Double-press confirmed!
                            mainHandler.removeCallbacks(volSingleTapRunnable)
                            pendingVolTapKeyCode = KeyEvent.KEYCODE_UNKNOWN
                            isVolDoubleTapCandidate = false
                            if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) lastVolUpTapTime = 0L else lastVolDownTapTime = 0L
                            performMappedAction(doubleConfig)
                            return true
                        } else {
                            // First tap recorded
                            if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) lastVolUpTapTime = now else lastVolDownTapTime = now
                            pendingVolTapKeyCode = keyCode
                            isVolDoubleTapCandidate = false
                            mainHandler.removeCallbacks(volSingleTapRunnable)
                            mainHandler.postDelayed(volSingleTapRunnable, DOUBLE_TAP_WINDOW_MS)
                            return true
                        }
                    } else {
                        // Double-tap not configured: immediately adjust volume without waiting
                        isVolDoubleTapCandidate = false
                        adjustSystemVolume(keyCode)
                        return true
                    }
                }
            }
            return super.onKeyEvent(event)
        }

        // 3. Headset / Media Buttons
        if (keyCode == KeyEvent.KEYCODE_HEADSETHOOK ||
            keyCode == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE ||
            keyCode == KeyEvent.KEYCODE_MEDIA_PLAY ||
            keyCode == KeyEvent.KEYCODE_MEDIA_PAUSE
        ) {
            val hasSingle = prefs.cachedHeadsetSingle.shouldTrigger(isLocked)
            val hasDouble = prefs.cachedHeadsetDouble.shouldTrigger(isLocked)
            val hasLong = prefs.cachedHeadsetLong.shouldTrigger(isLocked)

            if (!hasSingle && !hasDouble && !hasLong) {
                return super.onKeyEvent(event)
            }

            if (action == KeyEvent.ACTION_DOWN) {
                if (event.repeatCount == 0) {
                    isHeadsetLongPressed = false
                    // If a single tap was pending, cancel the runnable so it won't fire during the second tap
                    if (pendingHeadsetSingleTap) {
                        mainHandler.removeCallbacks(headsetSingleTapRunnable)
                    }
                    if (hasLong) {
                        mainHandler.removeCallbacks(headsetLongPressRunnable)
                        mainHandler.postDelayed(headsetLongPressRunnable, LONG_PRESS_TIMEOUT_MS)
                    }
                    return true
                }
                return isHeadsetLongPressed
            } else if (action == KeyEvent.ACTION_UP) {
                mainHandler.removeCallbacks(headsetLongPressRunnable)
                if (isHeadsetLongPressed) {
                    isHeadsetLongPressed = false
                    pendingHeadsetSingleTap = false
                    return true
                }

                if (hasDouble) {
                    val now = SystemClock.uptimeMillis()
                    if (pendingHeadsetSingleTap && now - lastHeadsetTapTime <= DOUBLE_TAP_WINDOW_MS) {
                        mainHandler.removeCallbacks(headsetSingleTapRunnable)
                        pendingHeadsetSingleTap = false
                        lastHeadsetTapTime = 0L
                        performMappedAction(prefs.cachedHeadsetDouble)
                        return true
                    } else {
                        lastHeadsetTapTime = now
                        pendingHeadsetSingleTap = true
                        mainHandler.removeCallbacks(headsetSingleTapRunnable)
                        mainHandler.postDelayed(headsetSingleTapRunnable, DOUBLE_TAP_WINDOW_MS)
                        return true
                    }
                } else if (hasSingle) {
                    performMappedAction(prefs.cachedHeadsetSingle)
                    return true
                }
            }
            return super.onKeyEvent(event)
        }

        // 4. Special Keys: Assist & Camera
        if (keyCode == KeyEvent.KEYCODE_ASSIST || keyCode == KeyEvent.KEYCODE_VOICE_ASSIST) {
            if (prefs.cachedAssistKey.shouldTrigger(isLocked)) {
                if (action == KeyEvent.ACTION_DOWN && event.repeatCount == 0) {
                    performMappedAction(prefs.cachedAssistKey)
                }
                return true
            }
        }

        if (keyCode == KeyEvent.KEYCODE_CAMERA) {
            if (prefs.cachedCameraKey.shouldTrigger(isLocked)) {
                if (action == KeyEvent.ACTION_DOWN && event.repeatCount == 0) {
                    performMappedAction(prefs.cachedCameraKey)
                }
                return true
            }
        }

        return super.onKeyEvent(event)
    }

    // ── Action Dispatcher ─────────────────────────────────────────────────────

    private fun performMappedAction(config: TriggerConfig) {
        if (config.action == ButtonAction.DEFAULT) return
        val prefs = ButtonMapperPrefs.get(applicationContext)
        if (prefs.cachedHapticFeedback) {
            performHapticFeedback()
        }
        Log.d(TAG, "Executing mapped action: ${config.action.name}")
        when (config.action) {
            ButtonAction.DEFAULT -> {}
            // Global Accessibility Actions
            ButtonAction.SCREENSHOT -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    performGlobalAction(GLOBAL_ACTION_TAKE_SCREENSHOT)
                }
            }
            ButtonAction.NOTIFICATIONS -> performGlobalAction(GLOBAL_ACTION_NOTIFICATIONS)
            ButtonAction.QUICK_SETTINGS -> performGlobalAction(GLOBAL_ACTION_QUICK_SETTINGS)
            ButtonAction.LOCK_SCREEN -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN)
                }
            }
            ButtonAction.RECENTS -> performGlobalAction(GLOBAL_ACTION_RECENTS)
            ButtonAction.HOME -> performGlobalAction(GLOBAL_ACTION_HOME)
            ButtonAction.BACK -> performGlobalAction(GLOBAL_ACTION_BACK)
            ButtonAction.POWER_DIALOG -> performGlobalAction(GLOBAL_ACTION_POWER_DIALOG)
            ButtonAction.SPLIT_SCREEN -> performGlobalAction(GLOBAL_ACTION_TOGGLE_SPLIT_SCREEN)

            // Sound & Device Controls
            ButtonAction.CYCLE_RINGER -> cycleRingerMode()
            ButtonAction.TOGGLE_DND -> toggleDoNotDisturb()
            ButtonAction.TOGGLE_AUTO_ROTATE -> toggleAutoRotate()
            ButtonAction.TOGGLE_FLASHLIGHT -> toggleFlashlight()
            ButtonAction.TOGGLE_MUTE -> toggleAudioMute()
            ButtonAction.PLAY_PAUSE -> dispatchMediaKey(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)
            ButtonAction.SKIP_NEXT -> dispatchMediaKey(KeyEvent.KEYCODE_MEDIA_NEXT)
            ButtonAction.SKIP_PREV -> dispatchMediaKey(KeyEvent.KEYCODE_MEDIA_PREVIOUS)

            // App Launch Shortcuts
            ButtonAction.LAUNCH_CAMERA -> launchCamera()
            ButtonAction.OPEN_ASSISTANT -> openAssistant()
            ButtonAction.LAUNCH_APP -> launchCustomApp(config.extraPackage)

            // Developer Tools
            ButtonAction.OPEN_DEV_OPTIONS -> openDeveloperOptions()
        }
    }

    private fun performHapticFeedback() {
        runCatching {
            @Suppress("DEPRECATION")
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vm?.defaultVibrator ?: (getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator)
            } else {
                getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
            if (vibrator?.hasVibrator() == true) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(
                        VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE)
                    )
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(50)
                }
            }
        }.onFailure { Log.w(TAG, "Haptic vibration failed", it) }
    }

    private fun cycleRingerMode() {
        val am = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val hasDndPermission = nm.isNotificationPolicyAccessGranted
        runCatching {
            val nextMode = when (am.ringerMode) {
                AudioManager.RINGER_MODE_NORMAL -> AudioManager.RINGER_MODE_VIBRATE
                AudioManager.RINGER_MODE_VIBRATE -> {
                    if (hasDndPermission) AudioManager.RINGER_MODE_SILENT else AudioManager.RINGER_MODE_NORMAL
                }
                else -> AudioManager.RINGER_MODE_NORMAL
            }
            am.ringerMode = nextMode
        }.onFailure { Log.w(TAG, "Failed to cycle ringer mode", it) }
    }

    private fun toggleDoNotDisturb() {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (nm.isNotificationPolicyAccessGranted) {
            val current = nm.currentInterruptionFilter
            val target = if (current == NotificationManager.INTERRUPTION_FILTER_ALL) {
                NotificationManager.INTERRUPTION_FILTER_PRIORITY
            } else {
                NotificationManager.INTERRUPTION_FILTER_ALL
            }
            runCatching { nm.setInterruptionFilter(target) }
        } else {
            val intent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            runCatching { startActivity(intent) }
        }
    }

    private fun toggleAutoRotate() {
        val resolver = applicationContext.contentResolver
        runCatching {
            val current = Settings.System.getInt(resolver, Settings.System.ACCELEROMETER_ROTATION, 0)
            val target = if (current == 1) 0 else 1
            Settings.System.putInt(resolver, Settings.System.ACCELEROMETER_ROTATION, target)
        }.onFailure { Log.w(TAG, "Failed to toggle auto rotate", it) }
    }

    private fun launchCamera() {
        val isLocked = isDeviceLockedOrScreenOff()
        if (isLocked) {
            val secureIntent = Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA_SECURE).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            if (secureIntent.resolveActivity(packageManager) != null) {
                val started = runCatching { startActivity(secureIntent); true }.getOrDefault(false)
                if (started) return
            }
        }
        val intent = Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { startActivity(intent) }
    }

    private fun openAssistant() {
        val voiceIntent = Intent(Intent.ACTION_VOICE_COMMAND).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { startActivity(voiceIntent) }
            .onFailure {
                val assistIntent = Intent(Intent.ACTION_ASSIST).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                runCatching { startActivity(assistIntent) }
            }
    }

    private fun launchCustomApp(pkg: String?) {
        if (pkg.isNullOrBlank()) return
        val intent = packageManager.getLaunchIntentForPackage(pkg)?.apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
        }
        if (intent != null) {
            runCatching { startActivity(intent) }
                .onFailure { Log.w(TAG, "Failed to launch custom app: $pkg", it) }
        }
    }

    private fun openDeveloperOptions() {
        val intent = Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { startActivity(intent) }
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
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val current = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                val isMuted = audioManager.isStreamMute(AudioManager.STREAM_MUSIC) || current == 0
                if (isMuted) {
                    audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_UNMUTE, AudioManager.FLAG_SHOW_UI)
                    if (audioManager.getStreamVolume(AudioManager.STREAM_MUSIC) == 0) {
                        val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, (max / 4).coerceAtLeast(1), AudioManager.FLAG_SHOW_UI)
                    }
                } else {
                    audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_MUTE, AudioManager.FLAG_SHOW_UI)
                }
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

    // ── Kiosk / HideDev / ForceDark Handlers ───────────────────────────────────

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
            .onFailure { Log.w(TAG, "Kiosk relaunch failed", it) }
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            torchCallback?.let { cb ->
                val cm = getSystemService(Context.CAMERA_SERVICE) as CameraManager
                runCatching { cm.registerTorchCallback(cb, mainHandler) }
            }
        }
    }

    private fun teardownScreenOffMonitoring() {
        runCatching { contentResolver.unregisterContentObserver(volumeContentObserver) }
        runCatching { unregisterReceiver(screenStateReceiver) }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            torchCallback?.let { cb ->
                val cm = getSystemService(Context.CAMERA_SERVICE) as CameraManager
                runCatching { cm.unregisterTorchCallback(cb) }
            }
        }
    }

    private fun resetScreenOffDetection() {
        screenOffConsecutiveChanges = 0
        screenOffChangeDirection = 0
        screenOffFirstChangeTime = 0L
    }

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
            screenOffOriginalVolume = screenOffLastVolume
            screenOffConsecutiveChanges = 1
            screenOffChangeDirection = direction
            screenOffFirstChangeTime = now
        } else {
            screenOffConsecutiveChanges++
        }
        screenOffLastVolume = current

        if (screenOffConsecutiveChanges < SCREEN_OFF_LONG_PRESS_STEPS) return

        val prefs = ButtonMapperPrefs.get(applicationContext)
        val isLocked = isDeviceLockedOrScreenOff()
        val config = if (direction > 0) prefs.cachedVolUpLong else prefs.cachedVolDownLong

        if (!config.shouldTrigger(isLocked)) return

        Log.d(TAG, "Screen-off long press: dir=$direction steps=$screenOffConsecutiveChanges → ${config.action.name}")

        am.setStreamVolume(AudioManager.STREAM_MUSIC, screenOffOriginalVolume, 0)
        screenOffLastVolume = screenOffOriginalVolume
        resetScreenOffDetection()

        performMappedAction(config)
    }

    companion object {
        private const val TAG = "KioskA11y"
        private const val DEBOUNCE_MS = 250L
        private const val INVERSION_KEY = "accessibility_display_inversion_enabled"
        private const val SCREEN_OFF_LONG_PRESS_STEPS = 3
        private const val SCREEN_OFF_WINDOW_MS = 800L
        private const val LONG_PRESS_TIMEOUT_MS = 500L
        private const val DOUBLE_TAP_WINDOW_MS = 350L
        private const val POWER_WINDOW_MS = 500L
        private const val POWER_DEDUPE_WINDOW_MS = 150L

        @Volatile
        private var instance: KioskAccessibilityService? = null

        fun isRunning(): Boolean = instance != null
    }
}
