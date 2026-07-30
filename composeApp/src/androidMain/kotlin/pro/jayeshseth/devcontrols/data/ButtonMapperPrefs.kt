package pro.jayeshseth.devcontrols.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
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

enum class ButtonAction(val label: String, val icon: String) {
    DEFAULT("Default Volume", "🔊"),
    PLAY_PAUSE("Play/Pause Media", "⏯️"),
    SKIP_NEXT("Next Track", "⏭️"),
    SKIP_PREV("Previous Track", "⏮️"),
    TOGGLE_FLASHLIGHT("Toggle Flashlight", "🔦"),
    TOGGLE_MUTE("Toggle Mute", "🔕");

    companion object {
        fun fromName(name: String?): ButtonAction {
            return entries.firstOrNull { it.name == name } ?: DEFAULT
        }
    }
}

private val Context.buttonMapperDataStore: DataStore<Preferences> by preferencesDataStore(name = "button_mapper_prefs")

class ButtonMapperPrefs private constructor(private val context: Context) {
    private val store get() = context.buttonMapperDataStore
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // --- Reactive flows for UI ---
    val volUpLockedAction: Flow<ButtonAction> = store.data.map {
        ButtonAction.fromName(it[KEY_VOL_UP_LOCKED])
    }
    val volUpUnlockedAction: Flow<ButtonAction> = store.data.map {
        ButtonAction.fromName(it[KEY_VOL_UP_UNLOCKED])
    }
    val volDownLockedAction: Flow<ButtonAction> = store.data.map {
        ButtonAction.fromName(it[KEY_VOL_DOWN_LOCKED])
    }
    val volDownUnlockedAction: Flow<ButtonAction> = store.data.map {
        ButtonAction.fromName(it[KEY_VOL_DOWN_UNLOCKED])
    }

    // --- Cached snapshot (no runBlocking required) for service use ---
    @Volatile var cachedVolUpLocked: ButtonAction = ButtonAction.DEFAULT
        private set
    @Volatile var cachedVolUpUnlocked: ButtonAction = ButtonAction.DEFAULT
        private set
    @Volatile var cachedVolDownLocked: ButtonAction = ButtonAction.DEFAULT
        private set
    @Volatile var cachedVolDownUnlocked: ButtonAction = ButtonAction.DEFAULT
        private set

    init {
        // Keep caches warm in background — no main-thread blocking needed
        volUpLockedAction.onEach { cachedVolUpLocked = it }.launchIn(scope)
        volUpUnlockedAction.onEach { cachedVolUpUnlocked = it }.launchIn(scope)
        volDownLockedAction.onEach { cachedVolDownLocked = it }.launchIn(scope)
        volDownUnlockedAction.onEach { cachedVolDownUnlocked = it }.launchIn(scope)
    }

    suspend fun setVolUpLockedAction(action: ButtonAction) {
        store.edit { it[KEY_VOL_UP_LOCKED] = action.name }
    }

    suspend fun setVolUpUnlockedAction(action: ButtonAction) {
        store.edit { it[KEY_VOL_UP_UNLOCKED] = action.name }
    }

    suspend fun setVolDownLockedAction(action: ButtonAction) {
        store.edit { it[KEY_VOL_DOWN_LOCKED] = action.name }
    }

    suspend fun setVolDownUnlockedAction(action: ButtonAction) {
        store.edit { it[KEY_VOL_DOWN_UNLOCKED] = action.name }
    }

    companion object {
        private val KEY_VOL_UP_LOCKED = stringPreferencesKey("vol_up_locked_action")
        private val KEY_VOL_UP_UNLOCKED = stringPreferencesKey("vol_up_unlocked_action")
        private val KEY_VOL_DOWN_LOCKED = stringPreferencesKey("vol_down_locked_action")
        private val KEY_VOL_DOWN_UNLOCKED = stringPreferencesKey("vol_down_unlocked_action")

        @Volatile
        private var instance: ButtonMapperPrefs? = null

        fun get(context: Context): ButtonMapperPrefs =
            instance ?: synchronized(this) {
                instance ?: ButtonMapperPrefs(context.applicationContext).also { instance = it }
            }
    }
}

