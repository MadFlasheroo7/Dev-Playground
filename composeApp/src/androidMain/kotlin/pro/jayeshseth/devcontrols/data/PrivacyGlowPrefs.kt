package pro.jayeshseth.devcontrols.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.privacyGlowDataStore: DataStore<Preferences> by preferencesDataStore(name = "privacy_glow_prefs")

class PrivacyGlowPrefs private constructor(private val context: Context) {
    private val store get() = context.privacyGlowDataStore

    val hudEnabled: Flow<Boolean> = store.data.map { it[KEY_HUD_ENABLED] ?: false }

    suspend fun setHudEnabled(enabled: Boolean) {
        store.edit { it[KEY_HUD_ENABLED] = enabled }
    }

    companion object {
        private val KEY_HUD_ENABLED = booleanPreferencesKey("hud_enabled")

        @Volatile
        private var instance: PrivacyGlowPrefs? = null

        fun get(context: Context): PrivacyGlowPrefs =
            instance ?: synchronized(this) {
                instance ?: PrivacyGlowPrefs(context.applicationContext).also { instance = it }
            }
    }
}
