package pro.jayeshseth.devcontrols.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.media.audiofx.AudioEffect
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.LoudnessEnhancer
import android.media.audiofx.PresetReverb
import android.media.audiofx.Virtualizer
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import pro.jayeshseth.devcontrols.MainActivity
import pro.jayeshseth.devcontrols.data.AudioEffectsPrefs

/**
 * Persistent foreground service that applies system-wide audio effects dynamically.
 *
 * It operates on two layers:
 *  1. **Global Fallback (Session 0)**: Applies to the primary output mix.
 *  2. **Active App Hooking**: Registers a dynamic BroadcastReceiver to intercept
 *     [AudioEffect.ACTION_OPEN_AUDIO_EFFECT_CONTROL_SESSION] and
 *     [AudioEffect.ACTION_CLOSE_AUDIO_EFFECT_CONTROL_SESSION] broadcasts.
 *     Whenever Spotify, YouTube Music, or other media players start, they broadcast their
 *     active AudioSessionId. We intercept this, instantiate a dedicated AudioEffect chain
 *     for that specific session, and apply the user's EQ/DSP settings directly.
 *
 * For Android 14+ (target SDK 36), it explicitly launches as [ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE].
 */
class AudioEffectsService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // ── Active Sessions Stack ──────────────────────────────────────────────────

    private class AudioSessionEffects(
        val sessionId: Int,
        val equalizer: Equalizer?,
        val bassBoost: BassBoost?,
        val virtualizer: Virtualizer?,
        val loudnessEnhancer: LoudnessEnhancer?,
        val presetReverb: PresetReverb?
    ) {
        fun release() {
            runCatching { equalizer?.release() }
            runCatching { bassBoost?.release() }
            runCatching { virtualizer?.release() }
            runCatching { loudnessEnhancer?.release() }
            runCatching { presetReverb?.release() }
        }
    }

    private val activeSessions = mutableMapOf<Int, AudioSessionEffects>()

    // ── Preference Caches for instant init of new sessions ─────────────────────

    @Volatile private var cachedEqEnabled = true
    @Volatile private var cachedEqPreset = -1
    @Volatile private var cachedEqBandLevels = emptyList<Int>()
    @Volatile private var cachedBassEnabled = true
    @Volatile private var cachedBassStrength = 400
    @Volatile private var cachedVirtEnabled = false
    @Volatile private var cachedVirtStrength = 500
    @Volatile private var cachedLoudEnabled = false
    @Volatile private var cachedLoudGain = 300
    @Volatile private var cachedReverbEnabled = false
    @Volatile private var cachedReverbPreset = 0

    // ── Audio Session Broadcast Receiver ──────────────────────────────────────

    private val audioSessionReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val sessionId = intent.getIntExtra(AudioEffect.EXTRA_AUDIO_SESSION, -1)
            if (sessionId <= 0) return

            when (intent.action) {
                AudioEffect.ACTION_OPEN_AUDIO_EFFECT_CONTROL_SESSION -> {
                    val pkg = intent.getStringExtra(AudioEffect.EXTRA_PACKAGE_NAME) ?: "unknown"
                    Log.d(TAG, "Audio session OPENED: id=$sessionId pkg=$pkg")
                    synchronized(activeSessions) {
                        createEffectsForSession(sessionId)
                    }
                }
                AudioEffect.ACTION_CLOSE_AUDIO_EFFECT_CONTROL_SESSION -> {
                    Log.d(TAG, "Audio session CLOSED: id=$sessionId")
                    synchronized(activeSessions) {
                        removeEffectsForSession(sessionId)
                    }
                }
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        isRunning = true
        createNotificationChannel()

        // Pass explicit FGS type for Android 14+ compatibility (SDK 36)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIF_ID,
                buildNotification(),
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(NOTIF_ID, buildNotification())
        }

        // Initialize with Global Session 0 fallback
        synchronized(activeSessions) {
            createEffectsForSession(0)
        }

        registerSessionReceiver()
        observePrefs()
        Log.d(TAG, "Service started — dynamic audio effects engine ready")
    }

    private fun registerSessionReceiver() {
        val filter = IntentFilter().apply {
            addAction(AudioEffect.ACTION_OPEN_AUDIO_EFFECT_CONTROL_SESSION)
            addAction(AudioEffect.ACTION_CLOSE_AUDIO_EFFECT_CONTROL_SESSION)
        }
        // Exported receiver is required to intercept broadcasts from third party apps
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(audioSessionReceiver, filter, RECEIVER_EXPORTED)
        } else {
            registerReceiver(audioSessionReceiver, filter)
        }
    }

    // ── Effect Lifecycle ────────────────────────────────────────────────────────

    private fun createEffectsForSession(sessionId: Int) {
        if (activeSessions.containsKey(sessionId)) return

        val eq = runCatching {
            Equalizer(Int.MAX_VALUE, sessionId)
        }.onFailure { Log.w(TAG, "Equalizer failed for session $sessionId", it) }.getOrNull()

        val bb = runCatching {
            BassBoost(Int.MAX_VALUE, sessionId)
        }.onFailure { Log.w(TAG, "BassBoost failed for session $sessionId", it) }.getOrNull()

        val vi = runCatching {
            Virtualizer(Int.MAX_VALUE, sessionId)
        }.onFailure { Log.w(TAG, "Virtualizer failed for session $sessionId", it) }.getOrNull()

        val le = runCatching {
            LoudnessEnhancer(sessionId)
        }.onFailure { Log.w(TAG, "LoudnessEnhancer failed for session $sessionId", it) }.getOrNull()

        val pr = runCatching {
            PresetReverb(Int.MAX_VALUE, sessionId)
        }.onFailure { Log.w(TAG, "PresetReverb failed for session $sessionId", it) }.getOrNull()

        val sessionEffects = AudioSessionEffects(sessionId, eq, bb, vi, le, pr)
        
        // 1. Configure all settings parameters first
        applyCurrentPrefsToSession(sessionEffects)

        // 2. Enable effects only after settings are applied
        runCatching { eq?.enabled = cachedEqEnabled }
        runCatching { bb?.enabled = cachedBassEnabled }
        runCatching { vi?.enabled = cachedVirtEnabled }
        runCatching { le?.enabled = cachedLoudEnabled }
        runCatching { pr?.enabled = cachedReverbEnabled }

        activeSessions[sessionId] = sessionEffects
        Log.d(TAG, "Initialized and enabled DSP stack on session $sessionId")
    }

    private fun removeEffectsForSession(sessionId: Int) {
        val effects = activeSessions.remove(sessionId)
        effects?.release()
    }

    private fun applyCurrentPrefsToSession(effects: AudioSessionEffects) {
        if (cachedEqPreset >= 0) {
            runCatching { effects.equalizer?.usePreset(cachedEqPreset.toShort()) }
        } else {
            cachedEqBandLevels.forEachIndexed { i, mb ->
                val eq = effects.equalizer ?: return@forEachIndexed
                if (i < eq.numberOfBands) {
                    runCatching { eq.setBandLevel(i.toShort(), mb.toShort()) }
                }
            }
        }
        runCatching { effects.bassBoost?.setStrength(cachedBassStrength.toShort()) }
        runCatching { effects.virtualizer?.setStrength(cachedVirtStrength.toShort()) }
        runCatching { effects.loudnessEnhancer?.setTargetGain(cachedLoudGain) }
        runCatching { effects.presetReverb?.preset = cachedReverbPreset.toShort() }
    }

    // ── Prefs observation — update active session objects in real-time ─────────

    private fun observePrefs() {
        val prefs = AudioEffectsPrefs.get(applicationContext)

        scope.launch {
            prefs.eqEnabled.collect { en ->
                cachedEqEnabled = en
                synchronized(activeSessions) {
                    activeSessions.values.forEach { runCatching { it.equalizer?.enabled = en } }
                }
            }
        }
        scope.launch {
            prefs.eqBandLevels.collect { levels ->
                cachedEqBandLevels = levels
                if (cachedEqPreset < 0) {
                    synchronized(activeSessions) {
                        activeSessions.values.forEach { session ->
                            val eq = session.equalizer ?: return@forEach
                            levels.forEachIndexed { i, mb ->
                                if (i < eq.numberOfBands) {
                                    runCatching { eq.setBandLevel(i.toShort(), mb.toShort()) }
                                }
                            }
                        }
                    }
                }
            }
        }
        scope.launch {
            prefs.eqPreset.collect { preset ->
                cachedEqPreset = preset
                if (preset >= 0) {
                    synchronized(activeSessions) {
                        activeSessions.values.forEach {
                            runCatching { it.equalizer?.usePreset(preset.toShort()) }
                        }
                    }
                }
            }
        }
        scope.launch {
            prefs.bassBoostEnabled.collect { en ->
                cachedBassEnabled = en
                synchronized(activeSessions) {
                    activeSessions.values.forEach { runCatching { it.bassBoost?.enabled = en } }
                }
            }
        }
        scope.launch {
            prefs.bassBoostStrength.collect { strength ->
                cachedBassStrength = strength
                synchronized(activeSessions) {
                    activeSessions.values.forEach { runCatching { it.bassBoost?.setStrength(strength.toShort()) } }
                }
            }
        }
        scope.launch {
            prefs.virtualizerEnabled.collect { en ->
                cachedVirtEnabled = en
                synchronized(activeSessions) {
                    activeSessions.values.forEach { runCatching { it.virtualizer?.enabled = en } }
                }
            }
        }
        scope.launch {
            prefs.virtualizerStrength.collect { strength ->
                cachedVirtStrength = strength
                synchronized(activeSessions) {
                    activeSessions.values.forEach { runCatching { it.virtualizer?.setStrength(strength.toShort()) } }
                }
            }
        }
        scope.launch {
            prefs.loudnessEnabled.collect { en ->
                cachedLoudEnabled = en
                synchronized(activeSessions) {
                    activeSessions.values.forEach { runCatching { it.loudnessEnhancer?.enabled = en } }
                }
            }
        }
        scope.launch {
            prefs.loudnessGain.collect { gainMb ->
                cachedLoudGain = gainMb
                synchronized(activeSessions) {
                    activeSessions.values.forEach { runCatching { it.loudnessEnhancer?.setTargetGain(gainMb) } }
                }
            }
        }
        scope.launch {
            prefs.reverbEnabled.collect { en ->
                cachedReverbEnabled = en
                synchronized(activeSessions) {
                    activeSessions.values.forEach { runCatching { it.presetReverb?.enabled = en } }
                }
            }
        }
        scope.launch {
            prefs.reverbPreset.collect { preset ->
                cachedReverbPreset = preset
                synchronized(activeSessions) {
                    activeSessions.values.forEach { runCatching { it.presetReverb?.preset = preset.toShort() } }
                }
            }
        }
    }

    // ── Service Lifecycle ───────────────────────────────────────────────────────

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) stopSelf()
        return START_STICKY
    }

    override fun onDestroy() {
        isRunning = false
        scope.cancel()
        runCatching { unregisterReceiver(audioSessionReceiver) }
        synchronized(activeSessions) {
            activeSessions.values.forEach { it.release() }
            activeSessions.clear()
        }
        Log.d(TAG, "Service destroyed — all effects released")
        super.onDestroy()
    }

    // ── Notification ────────────────────────────────────────────────────────────

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Audio Effects",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "System-wide DSP effect chain"
            setShowBadge(false)
        }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun buildNotification(): Notification {
        val openIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val stopIntent = PendingIntent.getService(
            this, 1,
            Intent(this, AudioEffectsService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_silent_mode_off)
            .setContentTitle("Audio Effects Active")
            .setContentText("System-wide EQ, Bass Boost & more")
            .setContentIntent(openIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop", stopIntent)
            .setOngoing(true)
            .setSilent(true)
            .build()
    }

    companion object {
        private const val TAG       = "AudioEffectsService"
        private const val CHANNEL_ID = "audio_effects_ch"
        private const val NOTIF_ID  = 2001
        private const val ACTION_STOP = "pro.jayeshseth.devcontrols.STOP_AUDIO_FX"

        @Volatile var isRunning: Boolean = false
            private set

        fun start(context: Context) {
            context.startForegroundService(Intent(context, AudioEffectsService::class.java))
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, AudioEffectsService::class.java))
        }
    }
}
