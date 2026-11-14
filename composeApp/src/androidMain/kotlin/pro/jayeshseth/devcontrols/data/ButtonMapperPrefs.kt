package pro.jayeshseth.devcontrols.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach

enum class TriggerCondition(val label: String) {
    ALWAYS("Always"),
    LOCKED("Screen Locked"),
    UNLOCKED("Screen Unlocked");

    companion object {
        fun fromName(name: String?): TriggerCondition {
            return entries.firstOrNull { it.name == name } ?: ALWAYS
        }
    }
}

enum class ButtonAction(val label: String, val icon: String) {
    DEFAULT("Default / Do Nothing", "⚪"),
    // Global Accessibility Actions
    SCREENSHOT("Take Screenshot", "📸"),
    NOTIFICATIONS("Notification Shade", "🔔"),
    QUICK_SETTINGS("Quick Settings", "⚡"),
    LOCK_SCREEN("Lock Screen", "🔒"),
    RECENTS("Recent Apps", "🗂️"),
    HOME("Home Screen", "🏠"),
    BACK("Back", "◀️"),
    POWER_DIALOG("Power Menu", "⏻"),
    SPLIT_SCREEN("Split Screen", "◫"),
    // Sound & Device Controls
    CYCLE_RINGER("Cycle Ringer Mode", "🔕"),
    TOGGLE_DND("Toggle Do Not Disturb", "⛔"),
    TOGGLE_AUTO_ROTATE("Toggle Auto-Rotate", "🔄"),
    TOGGLE_FLASHLIGHT("Toggle Flashlight", "🔦"),
    TOGGLE_MUTE("Toggle Mute", "🔇"),
    PLAY_PAUSE("Play / Pause Media", "⏯️"),
    SKIP_NEXT("Next Track", "⏭️"),
    SKIP_PREV("Previous Track", "⏮️"),
    // Shortcuts
    LAUNCH_CAMERA("Launch Camera", "📷"),
    OPEN_ASSISTANT("Open Assistant", "🎙️"),
    LAUNCH_APP("Launch App...", "🚀"),
    OPEN_DEV_OPTIONS("Developer Options", "🛠️");

    companion object {
        fun fromName(name: String?): ButtonAction {
            return entries.firstOrNull { it.name == name } ?: DEFAULT
        }
    }
}

data class TriggerConfig(
    val action: ButtonAction = ButtonAction.DEFAULT,
    val condition: TriggerCondition = TriggerCondition.ALWAYS,
    val extraPackage: String? = null,
    val extraLabel: String? = null
) {
    fun shouldTrigger(isLocked: Boolean): Boolean {
        if (action == ButtonAction.DEFAULT) return false
        return when (condition) {
            TriggerCondition.ALWAYS -> true
            TriggerCondition.LOCKED -> isLocked
            TriggerCondition.UNLOCKED -> !isLocked
        }
    }
}

private val Context.buttonMapperDataStore: DataStore<Preferences> by preferencesDataStore(name = "button_mapper_prefs")

class ButtonMapperPrefs private constructor(private val context: Context) {
    private val store get() = context.buttonMapperDataStore
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    object Keys {
        const val POWER_DOUBLE = "power_double"
        const val POWER_TRIPLE = "power_triple"
        const val VOL_UP_DOUBLE = "vol_up_double"
        const val VOL_UP_LONG = "vol_up_long"
        const val VOL_DOWN_DOUBLE = "vol_down_double"
        const val VOL_DOWN_LONG = "vol_down_long"
        const val VOL_CHORD = "vol_chord"
        const val HEADSET_SINGLE = "headset_single"
        const val HEADSET_DOUBLE = "headset_double"
        const val HEADSET_LONG = "headset_long"
        const val ASSIST_KEY = "assist_key"
        const val CAMERA_KEY = "camera_key"
        val HAPTIC_FEEDBACK = booleanPreferencesKey("haptic_feedback")

        // Legacy keys for backwards compatibility
        val KEY_VOL_UP_LOCKED = stringPreferencesKey("vol_up_locked_action")
        val KEY_VOL_UP_UNLOCKED = stringPreferencesKey("vol_up_unlocked_action")
        val KEY_VOL_DOWN_LOCKED = stringPreferencesKey("vol_down_locked_action")
        val KEY_VOL_DOWN_UNLOCKED = stringPreferencesKey("vol_down_unlocked_action")
    }

    private fun Preferences.readTrigger(key: String, legacyFallbackKey: Preferences.Key<String>? = null): TriggerConfig {
        val actionStr = this[stringPreferencesKey("${key}_action")]
        val condStr = this[stringPreferencesKey("${key}_condition")]
        val pkg = this[stringPreferencesKey("${key}_pkg")]
        val label = this[stringPreferencesKey("${key}_label")]

        val action = if (actionStr != null) {
            ButtonAction.fromName(actionStr)
        } else if (legacyFallbackKey != null && this[legacyFallbackKey] != null) {
            ButtonAction.fromName(this[legacyFallbackKey])
        } else {
            // Secondary legacy check for vol up / down unlocked
            val secondaryLegacy = when (key) {
                Keys.VOL_UP_LONG -> this[Keys.KEY_VOL_UP_UNLOCKED]
                Keys.VOL_DOWN_LONG -> this[Keys.KEY_VOL_DOWN_UNLOCKED]
                else -> null
            }
            if (secondaryLegacy != null) {
                ButtonAction.fromName(secondaryLegacy)
            } else {
                ButtonAction.DEFAULT
            }
        }

        return TriggerConfig(
            action = action,
            condition = TriggerCondition.fromName(condStr),
            extraPackage = pkg,
            extraLabel = label
        )
    }

    // --- Reactive flows for UI ---
    val powerDoubleFlow: Flow<TriggerConfig> = store.data.map { it.readTrigger(Keys.POWER_DOUBLE) }
    val powerTripleFlow: Flow<TriggerConfig> = store.data.map { it.readTrigger(Keys.POWER_TRIPLE) }
    val volUpDoubleFlow: Flow<TriggerConfig> = store.data.map { it.readTrigger(Keys.VOL_UP_DOUBLE) }
    val volUpLongFlow: Flow<TriggerConfig> = store.data.map { it.readTrigger(Keys.VOL_UP_LONG, Keys.KEY_VOL_UP_LOCKED) }
    val volDownDoubleFlow: Flow<TriggerConfig> = store.data.map { it.readTrigger(Keys.VOL_DOWN_DOUBLE) }
    val volDownLongFlow: Flow<TriggerConfig> = store.data.map { it.readTrigger(Keys.VOL_DOWN_LONG, Keys.KEY_VOL_DOWN_LOCKED) }
    val volChordFlow: Flow<TriggerConfig> = store.data.map { it.readTrigger(Keys.VOL_CHORD) }
    val headsetSingleFlow: Flow<TriggerConfig> = store.data.map { it.readTrigger(Keys.HEADSET_SINGLE) }
    val headsetDoubleFlow: Flow<TriggerConfig> = store.data.map { it.readTrigger(Keys.HEADSET_DOUBLE) }
    val headsetLongFlow: Flow<TriggerConfig> = store.data.map { it.readTrigger(Keys.HEADSET_LONG) }
    val assistKeyFlow: Flow<TriggerConfig> = store.data.map { it.readTrigger(Keys.ASSIST_KEY) }
    val cameraKeyFlow: Flow<TriggerConfig> = store.data.map { it.readTrigger(Keys.CAMERA_KEY) }
    val hapticFeedbackFlow: Flow<Boolean> = store.data.map { it[Keys.HAPTIC_FEEDBACK] ?: true }

    // --- Cached snapshots (zero overhead, no runBlocking) for key event handlers ---
    @Volatile var cachedPowerDouble: TriggerConfig = TriggerConfig()
        private set
    @Volatile var cachedPowerTriple: TriggerConfig = TriggerConfig()
        private set
    @Volatile var cachedVolUpDouble: TriggerConfig = TriggerConfig()
        private set
    @Volatile var cachedVolUpLong: TriggerConfig = TriggerConfig()
        private set
    @Volatile var cachedVolDownDouble: TriggerConfig = TriggerConfig()
        private set
    @Volatile var cachedVolDownLong: TriggerConfig = TriggerConfig()
        private set
    @Volatile var cachedVolChord: TriggerConfig = TriggerConfig()
        private set
    @Volatile var cachedHeadsetSingle: TriggerConfig = TriggerConfig()
        private set
    @Volatile var cachedHeadsetDouble: TriggerConfig = TriggerConfig()
        private set
    @Volatile var cachedHeadsetLong: TriggerConfig = TriggerConfig()
        private set
    @Volatile var cachedAssistKey: TriggerConfig = TriggerConfig()
        private set
    @Volatile var cachedCameraKey: TriggerConfig = TriggerConfig()
        private set
    @Volatile var cachedHapticFeedback: Boolean = true
        private set

    // Legacy backwards compatibility properties
    val cachedVolUpLocked: ButtonAction get() = cachedVolUpLong.action
    val cachedVolUpUnlocked: ButtonAction get() = cachedVolUpLong.action
    val cachedVolDownLocked: ButtonAction get() = cachedVolDownLong.action
    val cachedVolDownUnlocked: ButtonAction get() = cachedVolDownLong.action

    val volUpLockedAction: Flow<ButtonAction> = volUpLongFlow.map { it.action }
    val volUpUnlockedAction: Flow<ButtonAction> = volUpLongFlow.map { it.action }
    val volDownLockedAction: Flow<ButtonAction> = volDownLongFlow.map { it.action }
    val volDownUnlockedAction: Flow<ButtonAction> = volDownLongFlow.map { it.action }

    init {
        // Keep caches warm in background
        powerDoubleFlow.onEach { cachedPowerDouble = it }.launchIn(scope)
        powerTripleFlow.onEach { cachedPowerTriple = it }.launchIn(scope)
        volUpDoubleFlow.onEach { cachedVolUpDouble = it }.launchIn(scope)
        volUpLongFlow.onEach { cachedVolUpLong = it }.launchIn(scope)
        volDownDoubleFlow.onEach { cachedVolDownDouble = it }.launchIn(scope)
        volDownLongFlow.onEach { cachedVolDownLong = it }.launchIn(scope)
        volChordFlow.onEach { cachedVolChord = it }.launchIn(scope)
        headsetSingleFlow.onEach { cachedHeadsetSingle = it }.launchIn(scope)
        headsetDoubleFlow.onEach { cachedHeadsetDouble = it }.launchIn(scope)
        headsetLongFlow.onEach { cachedHeadsetLong = it }.launchIn(scope)
        assistKeyFlow.onEach { cachedAssistKey = it }.launchIn(scope)
        cameraKeyFlow.onEach { cachedCameraKey = it }.launchIn(scope)
        hapticFeedbackFlow.onEach { cachedHapticFeedback = it }.launchIn(scope)
    }

    suspend fun updateTrigger(key: String, config: TriggerConfig) {
        // Update in-memory volatile cache immediately for 0-latency key interception
        when (key) {
            Keys.POWER_DOUBLE -> cachedPowerDouble = config
            Keys.POWER_TRIPLE -> cachedPowerTriple = config
            Keys.VOL_UP_DOUBLE -> cachedVolUpDouble = config
            Keys.VOL_UP_LONG -> cachedVolUpLong = config
            Keys.VOL_DOWN_DOUBLE -> cachedVolDownDouble = config
            Keys.VOL_DOWN_LONG -> cachedVolDownLong = config
            Keys.VOL_CHORD -> cachedVolChord = config
            Keys.HEADSET_SINGLE -> cachedHeadsetSingle = config
            Keys.HEADSET_DOUBLE -> cachedHeadsetDouble = config
            Keys.HEADSET_LONG -> cachedHeadsetLong = config
            Keys.ASSIST_KEY -> cachedAssistKey = config
            Keys.CAMERA_KEY -> cachedCameraKey = config
        }

        store.edit { prefs ->
            prefs[stringPreferencesKey("${key}_action")] = config.action.name
            prefs[stringPreferencesKey("${key}_condition")] = config.condition.name
            if (config.extraPackage != null) {
                prefs[stringPreferencesKey("${key}_pkg")] = config.extraPackage
            } else {
                prefs.remove(stringPreferencesKey("${key}_pkg"))
            }
            if (config.extraLabel != null) {
                prefs[stringPreferencesKey("${key}_label")] = config.extraLabel
            } else {
                prefs.remove(stringPreferencesKey("${key}_label"))
            }
        }
    }

    suspend fun setHapticFeedback(enabled: Boolean) {
        cachedHapticFeedback = enabled
        store.edit { it[Keys.HAPTIC_FEEDBACK] = enabled }
    }

    suspend fun setVolUpLockedAction(action: ButtonAction) {
        updateTrigger(Keys.VOL_UP_LONG, cachedVolUpLong.copy(action = action))
    }

    suspend fun setVolUpUnlockedAction(action: ButtonAction) {
        updateTrigger(Keys.VOL_UP_LONG, cachedVolUpLong.copy(action = action))
    }

    suspend fun setVolDownLockedAction(action: ButtonAction) {
        updateTrigger(Keys.VOL_DOWN_LONG, cachedVolDownLong.copy(action = action))
    }

    suspend fun setVolDownUnlockedAction(action: ButtonAction) {
        updateTrigger(Keys.VOL_DOWN_LONG, cachedVolDownLong.copy(action = action))
    }

    companion object {
        @Volatile
        private var instance: ButtonMapperPrefs? = null

        fun get(context: Context): ButtonMapperPrefs =
            instance ?: synchronized(this) {
                instance ?: ButtonMapperPrefs(context.applicationContext).also { instance = it }
            }
    }
}
