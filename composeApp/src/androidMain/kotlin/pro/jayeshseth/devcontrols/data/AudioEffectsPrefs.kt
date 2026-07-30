package pro.jayeshseth.devcontrols.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.audioEffectsDataStore: DataStore<Preferences>
    by preferencesDataStore(name = "audio_effects_prefs")

/**
 * Persists all system-wide audio effect settings via DataStore.
 *
 * Each setting maps to an [AudioEffect] parameter in [AudioEffectsService]:
 *  - [eqEnabled] / [eqBandLevels] / [eqPreset]  → [android.media.audiofx.Equalizer]
 *  - [bassBoostEnabled] / [bassBoostStrength]     → [android.media.audiofx.BassBoost]
 *  - [virtualizerEnabled] / [virtualizerStrength] → [android.media.audiofx.Virtualizer]
 *  - [loudnessEnabled] / [loudnessGain]           → [android.media.audiofx.LoudnessEnhancer]
 *  - [reverbEnabled] / [reverbPreset]             → [android.media.audiofx.PresetReverb]
 */
class AudioEffectsPrefs private constructor(private val context: Context) {

    private val store get() = context.audioEffectsDataStore

    // ── Equalizer ──────────────────────────────────────────────────────────────
    val eqEnabled: Flow<Boolean> = store.data.map { it[KEY_EQ_ENABLED] ?: true }
    /** Device-preset index; -1 = custom (user-adjusted bands). */
    val eqPreset: Flow<Int> = store.data.map { it[KEY_EQ_PRESET] ?: -1 }
    /** Comma-separated band levels in millibels. Empty = all bands at 0 mb (flat). */
    val eqBandLevels: Flow<List<Int>> = store.data.map {
        val raw = it[KEY_EQ_BAND_LEVELS] ?: ""
        if (raw.isEmpty()) emptyList() else raw.split(",").mapNotNull(String::toIntOrNull)
    }

    // ── Bass Boost ─────────────────────────────────────────────────────────────
    val bassBoostEnabled: Flow<Boolean> = store.data.map { it[KEY_BASS_ENABLED] ?: true }
    /** Strength 0–1000 (0 = off, 1000 = max). */
    val bassBoostStrength: Flow<Int> = store.data.map { it[KEY_BASS_STRENGTH] ?: 400 }

    // ── Virtualizer ────────────────────────────────────────────────────────────
    val virtualizerEnabled: Flow<Boolean> = store.data.map { it[KEY_VIRT_ENABLED] ?: false }
    /** Strength 0–1000. */
    val virtualizerStrength: Flow<Int> = store.data.map { it[KEY_VIRT_STRENGTH] ?: 500 }

    // ── Loudness Enhancer ──────────────────────────────────────────────────────
    val loudnessEnabled: Flow<Boolean> = store.data.map { it[KEY_LOUD_ENABLED] ?: false }
    /** Target gain in millibels (100 mb ≈ 1 dB). Range 0–1000 mb. */
    val loudnessGain: Flow<Int> = store.data.map { it[KEY_LOUD_GAIN] ?: 300 }

    // ── Preset Reverb ──────────────────────────────────────────────────────────
    val reverbEnabled: Flow<Boolean> = store.data.map { it[KEY_REVERB_ENABLED] ?: false }
    /** [android.media.audiofx.PresetReverb] preset constant 0–6. */
    val reverbPreset: Flow<Int> = store.data.map { it[KEY_REVERB_PRESET] ?: 0 }

    // ── Setters ────────────────────────────────────────────────────────────────
    suspend fun setEqEnabled(v: Boolean) = store.edit { it[KEY_EQ_ENABLED] = v }
    suspend fun setEqPreset(v: Int) = store.edit { it[KEY_EQ_PRESET] = v }
    suspend fun setEqBandLevels(levels: List<Int>) =
        store.edit { it[KEY_EQ_BAND_LEVELS] = levels.joinToString(",") }

    suspend fun setBassBoostEnabled(v: Boolean) = store.edit { it[KEY_BASS_ENABLED] = v }
    suspend fun setBassBoostStrength(v: Int) = store.edit { it[KEY_BASS_STRENGTH] = v }

    suspend fun setVirtualizerEnabled(v: Boolean) = store.edit { it[KEY_VIRT_ENABLED] = v }
    suspend fun setVirtualizerStrength(v: Int) = store.edit { it[KEY_VIRT_STRENGTH] = v }

    suspend fun setLoudnessEnabled(v: Boolean) = store.edit { it[KEY_LOUD_ENABLED] = v }
    suspend fun setLoudnessGain(v: Int) = store.edit { it[KEY_LOUD_GAIN] = v }

    suspend fun setReverbEnabled(v: Boolean) = store.edit { it[KEY_REVERB_ENABLED] = v }
    suspend fun setReverbPreset(v: Int) = store.edit { it[KEY_REVERB_PRESET] = v }

    companion object {
        private val KEY_EQ_ENABLED     = booleanPreferencesKey("eq_enabled")
        private val KEY_EQ_PRESET      = intPreferencesKey("eq_preset")
        private val KEY_EQ_BAND_LEVELS = stringPreferencesKey("eq_band_levels")
        private val KEY_BASS_ENABLED   = booleanPreferencesKey("bass_enabled")
        private val KEY_BASS_STRENGTH  = intPreferencesKey("bass_strength")
        private val KEY_VIRT_ENABLED   = booleanPreferencesKey("virt_enabled")
        private val KEY_VIRT_STRENGTH  = intPreferencesKey("virt_strength")
        private val KEY_LOUD_ENABLED   = booleanPreferencesKey("loud_enabled")
        private val KEY_LOUD_GAIN      = intPreferencesKey("loud_gain")
        private val KEY_REVERB_ENABLED = booleanPreferencesKey("reverb_enabled")
        private val KEY_REVERB_PRESET  = intPreferencesKey("reverb_preset")

        @Volatile private var instance: AudioEffectsPrefs? = null

        fun get(context: Context): AudioEffectsPrefs =
            instance ?: synchronized(this) {
                instance ?: AudioEffectsPrefs(context.applicationContext).also { instance = it }
            }
    }
}
