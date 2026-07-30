package pro.jayeshseth.devcontrols.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/**
 * DataStore for Kiosk Mode: the picked target (package + cached label) and the
 * `kioskActive` flag. Read once via `targetSnapshot()` / `isKioskActiveOnce()` from
 * [pro.jayeshseth.devcontrols.kiosk.KioskAccessibilityService], collected as Flows
 * by the UI and the [pro.jayeshseth.devcontrols.widget.WidgetRefresher].
 */
private val Context.kioskDataStore: DataStore<Preferences> by preferencesDataStore(name = "kiosk_prefs")

class KioskPrefs private constructor(private val context: Context) {
    private val store get() = context.kioskDataStore

    val targetPackage: Flow<String?> = store.data.map { it[KEY_TARGET_PKG] }
    val targetLabel: Flow<String?> = store.data.map { it[KEY_TARGET_LABEL] }
    val kioskActive: Flow<Boolean> = store.data.map { it[KEY_ACTIVE] ?: false }
    val fastAnimations: Flow<Boolean> = store.data.map { it[KEY_FAST_ANIM] ?: false }
    val volumeKeyMediaOverride: Flow<Boolean> = store.data.map { it[KEY_VOL_MEDIA_OVERRIDE] ?: false }

    suspend fun isKioskActiveOnce(): Boolean = kioskActive.first()
    suspend fun isFastAnimationsOnce(): Boolean = fastAnimations.first()
    suspend fun isVolumeKeyMediaOverrideOnce(): Boolean = volumeKeyMediaOverride.first()

    suspend fun setKioskActive(active: Boolean) {
        store.edit { it[KEY_ACTIVE] = active }
    }

    suspend fun setFastAnimations(enabled: Boolean) {
        store.edit { it[KEY_FAST_ANIM] = enabled }
    }

    suspend fun setVolumeKeyMediaOverride(enabled: Boolean) {
        store.edit { it[KEY_VOL_MEDIA_OVERRIDE] = enabled }
    }

    suspend fun setTarget(pkg: String?, label: String?) {
        store.edit { prefs ->
            if (pkg == null) {
                prefs.remove(KEY_TARGET_PKG)
                prefs.remove(KEY_TARGET_LABEL)
            } else {
                prefs[KEY_TARGET_PKG] = pkg
                if (label != null) prefs[KEY_TARGET_LABEL] = label
            }
        }
    }

    suspend fun targetSnapshot(): Pair<String?, String?> {
        val prefs = store.data.first()
        return prefs[KEY_TARGET_PKG] to prefs[KEY_TARGET_LABEL]
    }

    companion object {
        private val KEY_TARGET_PKG = stringPreferencesKey("kiosk_target_package")
        private val KEY_TARGET_LABEL = stringPreferencesKey("kiosk_target_label")
        private val KEY_ACTIVE = booleanPreferencesKey("kiosk_active")
        private val KEY_FAST_ANIM = booleanPreferencesKey("kiosk_fast_animations")
        private val KEY_VOL_MEDIA_OVERRIDE = booleanPreferencesKey("kiosk_vol_media_override")

        @Volatile
        private var instance: KioskPrefs? = null

        fun get(context: Context): KioskPrefs =
            instance ?: synchronized(this) {
                instance ?: KioskPrefs(context.applicationContext).also { instance = it }
            }
    }
}
