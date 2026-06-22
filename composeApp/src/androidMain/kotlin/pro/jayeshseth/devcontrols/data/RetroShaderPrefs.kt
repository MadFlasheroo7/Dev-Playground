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

private val Context.retroShaderDataStore: DataStore<Preferences> by preferencesDataStore(name = "retro_shader_prefs")

class RetroShaderPrefs private constructor(private val context: Context) {
    private val store get() = context.retroShaderDataStore

    val overlayEnabled: Flow<Boolean> = store.data.map { it[KEY_OVERLAY_ENABLED] ?: false }
    val curvature: Flow<Float> = store.data.map { it[KEY_CURVATURE] ?: 0.12f }
    val scanlineIntensity: Flow<Float> = store.data.map { it[KEY_SCANLINE_INTENSITY] ?: 0.6f }
    val noiseIntensity: Flow<Float> = store.data.map { it[KEY_NOISE_INTENSITY] ?: 0.3f }
    val phosphorMode: Flow<Float> = store.data.map { it[KEY_PHOSPHOR_MODE] ?: 0f }
    val warpEnabled: Flow<Boolean> = store.data.map { it[KEY_WARP_ENABLED] ?: false }

    suspend fun setOverlayEnabled(enabled: Boolean) {
        store.edit { it[KEY_OVERLAY_ENABLED] = enabled }
    }

    suspend fun setCurvature(value: Float) {
        store.edit { it[KEY_CURVATURE] = value }
    }

    suspend fun setScanlineIntensity(value: Float) {
        store.edit { it[KEY_SCANLINE_INTENSITY] = value }
    }

    suspend fun setNoiseIntensity(value: Float) {
        store.edit { it[KEY_NOISE_INTENSITY] = value }
    }

    suspend fun setPhosphorMode(value: Float) {
        store.edit { it[KEY_PHOSPHOR_MODE] = value }
    }

    suspend fun setWarpEnabled(enabled: Boolean) {
        store.edit { it[KEY_WARP_ENABLED] = enabled }
    }

    companion object {
        private val KEY_OVERLAY_ENABLED = booleanPreferencesKey("overlay_enabled")
        private val KEY_CURVATURE = floatPreferencesKey("curvature")
        private val KEY_SCANLINE_INTENSITY = floatPreferencesKey("scanline_intensity")
        private val KEY_NOISE_INTENSITY = floatPreferencesKey("noise_intensity")
        private val KEY_PHOSPHOR_MODE = floatPreferencesKey("phosphor_mode")
        private val KEY_WARP_ENABLED = booleanPreferencesKey("warp_enabled")

        @Volatile
        private var instance: RetroShaderPrefs? = null

        fun get(context: Context): RetroShaderPrefs =
            instance ?: synchronized(this) {
                instance ?: RetroShaderPrefs(context.applicationContext).also { instance = it }
            }
    }
}
