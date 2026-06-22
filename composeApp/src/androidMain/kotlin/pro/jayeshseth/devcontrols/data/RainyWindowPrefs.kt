package pro.jayeshseth.devcontrols.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.rainyWindowDataStore: DataStore<Preferences> by preferencesDataStore(name = "rainy_window_prefs")

class RainyWindowPrefs private constructor(private val context: Context) {
    private val store get() = context.rainyWindowDataStore

    val overlayEnabled: Flow<Boolean> = store.data.map { it[KEY_OVERLAY_ENABLED] ?: false }
    val rainIntensity: Flow<Float> = store.data.map { it[KEY_RAIN_INTENSITY] ?: 0.5f }
    val glassMisty: Flow<Float> = store.data.map { it[KEY_GLASS_MISTY] ?: 0.3f }
    val dropSpeed: Flow<Float> = store.data.map { it[KEY_DROP_SPEED] ?: 1.0f }
    val refraction: Flow<Float> = store.data.map { it[KEY_REFRACTION] ?: 0.2f }

    suspend fun setOverlayEnabled(enabled: Boolean) {
        store.edit { it[KEY_OVERLAY_ENABLED] = enabled }
    }

    suspend fun setRainIntensity(value: Float) {
        store.edit { it[KEY_RAIN_INTENSITY] = value }
    }

    suspend fun setGlassMisty(value: Float) {
        store.edit { it[KEY_GLASS_MISTY] = value }
    }

    suspend fun setDropSpeed(value: Float) {
        store.edit { it[KEY_DROP_SPEED] = value }
    }

    suspend fun setRefraction(value: Float) {
        store.edit { it[KEY_REFRACTION] = value }
    }

    companion object {
        private val KEY_OVERLAY_ENABLED = booleanPreferencesKey("overlay_enabled")
        private val KEY_RAIN_INTENSITY = floatPreferencesKey("rain_intensity")
        private val KEY_GLASS_MISTY = floatPreferencesKey("glass_misty")
        private val KEY_DROP_SPEED = floatPreferencesKey("drop_speed")
        private val KEY_REFRACTION = floatPreferencesKey("refraction")

        @Volatile
        private var instance: RainyWindowPrefs? = null

        fun get(context: Context): RainyWindowPrefs =
            instance ?: synchronized(this) {
                instance ?: RainyWindowPrefs(context.applicationContext).also { instance = it }
            }
    }
}
