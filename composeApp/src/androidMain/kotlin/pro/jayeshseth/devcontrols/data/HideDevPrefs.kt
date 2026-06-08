package pro.jayeshseth.devcontrols.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.hideDevDataStore: DataStore<Preferences> by preferencesDataStore(name = "hide_dev_prefs")

class HideDevPrefs private constructor(private val context: Context) {
    private val store get() = context.hideDevDataStore

    val enabled: Flow<Boolean> = store.data.map { it[KEY_ENABLED] ?: false }
    val targetPackages: Flow<Set<String>> = store.data.map { it[KEY_TARGETS] ?: emptySet() }
    val includeWifiAdb: Flow<Boolean> = store.data.map { it[KEY_INCLUDE_WIFI] ?: false }
    /** Whether we have currently-hidden state in escrow (i.e. a target is foreground right now). */
    val currentlyHiding: Flow<Boolean> = store.data.map { it[KEY_HIDING_ACTIVE] ?: false }

    suspend fun isEnabledOnce(): Boolean = enabled.first()
    suspend fun targetsOnce(): Set<String> = targetPackages.first()
    suspend fun includeWifiAdbOnce(): Boolean = includeWifiAdb.first()

    suspend fun setEnabled(value: Boolean) {
        store.edit { it[KEY_ENABLED] = value }
    }

    suspend fun setIncludeWifiAdb(value: Boolean) {
        store.edit { it[KEY_INCLUDE_WIFI] = value }
    }

    suspend fun setTargetIncluded(pkg: String, included: Boolean) {
        store.edit { prefs ->
            val current = prefs[KEY_TARGETS] ?: emptySet()
            prefs[KEY_TARGETS] = if (included) current + pkg else current - pkg
        }
    }

    suspend fun snapshotEscrow(devOptsBefore: Boolean, adbBefore: Boolean, wifiAdbBefore: Boolean) {
        store.edit { prefs ->
            prefs[KEY_HIDING_ACTIVE] = true
            prefs[KEY_SAVED_DEV] = devOptsBefore
            prefs[KEY_SAVED_ADB] = adbBefore
            prefs[KEY_SAVED_WIFI] = wifiAdbBefore
        }
    }

    /** (escrowActive, savedDev, savedAdb, savedWifiAdb) */
    suspend fun readEscrow(): Escrow {
        val prefs = store.data.first()
        return Escrow(
            active = prefs[KEY_HIDING_ACTIVE] ?: false,
            savedDev = prefs[KEY_SAVED_DEV] ?: true,
            savedAdb = prefs[KEY_SAVED_ADB] ?: true,
            savedWifiAdb = prefs[KEY_SAVED_WIFI] ?: true,
        )
    }

    data class Escrow(
        val active: Boolean,
        val savedDev: Boolean,
        val savedAdb: Boolean,
        val savedWifiAdb: Boolean,
    )

    suspend fun clearEscrow() {
        store.edit { prefs ->
            prefs.remove(KEY_HIDING_ACTIVE)
            prefs.remove(KEY_SAVED_DEV)
            prefs.remove(KEY_SAVED_ADB)
            prefs.remove(KEY_SAVED_WIFI)
        }
    }

    companion object {
        private val KEY_ENABLED = booleanPreferencesKey("hide_dev_enabled")
        private val KEY_TARGETS = stringSetPreferencesKey("hide_dev_targets")
        private val KEY_INCLUDE_WIFI = booleanPreferencesKey("hide_dev_include_wifi_adb")
        private val KEY_HIDING_ACTIVE = booleanPreferencesKey("hide_dev_active")
        private val KEY_SAVED_DEV = booleanPreferencesKey("hide_dev_saved_dev")
        private val KEY_SAVED_ADB = booleanPreferencesKey("hide_dev_saved_adb")
        private val KEY_SAVED_WIFI = booleanPreferencesKey("hide_dev_saved_wifi_adb")

        @Volatile
        private var instance: HideDevPrefs? = null

        fun get(context: Context): HideDevPrefs =
            instance ?: synchronized(this) {
                instance ?: HideDevPrefs(context.applicationContext).also { instance = it }
            }
    }
}
