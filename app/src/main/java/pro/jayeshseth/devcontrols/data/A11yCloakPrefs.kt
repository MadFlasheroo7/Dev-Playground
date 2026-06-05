package pro.jayeshseth.devcontrols.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.a11yCloakDataStore: DataStore<Preferences> by preferencesDataStore(name = "a11y_cloak_prefs")

/**
 * Config + escrow for the Accessibility Cloak. The cloak unbinds our own
 * accessibility service while a target app is foreground so that the target's
 * detection logic sees a clean [enabled_accessibility_services] list, then
 * restores it on leave.
 *
 * The escrow stores the *exact* setting strings we overwrote so restore is
 * byte-for-byte — important because the system regenerates the list and we must
 * not accidentally drop a different service the user enabled.
 */
class A11yCloakPrefs private constructor(private val context: Context) {
    private val store get() = context.a11yCloakDataStore

    val enabled: Flow<Boolean> = store.data.map { it[KEY_ENABLED] ?: false }
    val targetPackages: Flow<Set<String>> = store.data.map { it[KEY_TARGETS] ?: emptySet() }
    /** Also zero developer-options + USB debugging while cloaked (a11y-based Hide Dev can't, since we're unbound). */
    val alsoHideDev: Flow<Boolean> = store.data.map { it[KEY_ALSO_HIDE_DEV] ?: false }
    /** Whether the foreground poller service is currently running. */
    val serviceRunning: Flow<Boolean> = store.data.map { it[KEY_SERVICE_RUNNING] ?: false }
    /** Whether a target is foreground right now and we are actively cloaked. */
    val cloakActive: Flow<Boolean> = store.data.map { it[KEY_CLOAK_ACTIVE] ?: false }

    suspend fun isEnabledOnce(): Boolean = enabled.first()
    suspend fun targetsOnce(): Set<String> = targetPackages.first()
    suspend fun alsoHideDevOnce(): Boolean = alsoHideDev.first()

    suspend fun setEnabled(value: Boolean) { store.edit { it[KEY_ENABLED] = value } }
    suspend fun setAlsoHideDev(value: Boolean) { store.edit { it[KEY_ALSO_HIDE_DEV] = value } }
    suspend fun setServiceRunning(value: Boolean) { store.edit { it[KEY_SERVICE_RUNNING] = value } }

    suspend fun setTargetIncluded(pkg: String, included: Boolean) {
        store.edit { prefs ->
            val current = prefs[KEY_TARGETS] ?: emptySet()
            prefs[KEY_TARGETS] = if (included) current + pkg else current - pkg
        }
    }

    suspend fun snapshotEscrow(
        savedServices: String,
        savedAccessibilityEnabled: Int,
        savedDev: Boolean,
        savedAdb: Boolean,
    ) {
        store.edit { prefs ->
            prefs[KEY_CLOAK_ACTIVE] = true
            prefs[KEY_SAVED_SERVICES] = savedServices
            prefs[KEY_SAVED_ACCESS_ENABLED] = savedAccessibilityEnabled
            prefs[KEY_SAVED_DEV] = savedDev
            prefs[KEY_SAVED_ADB] = savedAdb
        }
    }

    suspend fun readEscrow(): Escrow {
        val prefs = store.data.first()
        return Escrow(
            active = prefs[KEY_CLOAK_ACTIVE] ?: false,
            savedServices = prefs[KEY_SAVED_SERVICES] ?: "",
            savedAccessibilityEnabled = prefs[KEY_SAVED_ACCESS_ENABLED] ?: 1,
            savedDev = prefs[KEY_SAVED_DEV] ?: true,
            savedAdb = prefs[KEY_SAVED_ADB] ?: true,
        )
    }

    suspend fun clearEscrow() {
        store.edit { prefs ->
            prefs.remove(KEY_CLOAK_ACTIVE)
            prefs.remove(KEY_SAVED_SERVICES)
            prefs.remove(KEY_SAVED_ACCESS_ENABLED)
            prefs.remove(KEY_SAVED_DEV)
            prefs.remove(KEY_SAVED_ADB)
        }
    }

    data class Escrow(
        val active: Boolean,
        val savedServices: String,
        val savedAccessibilityEnabled: Int,
        val savedDev: Boolean,
        val savedAdb: Boolean,
    )

    companion object {
        private val KEY_ENABLED = booleanPreferencesKey("cloak_enabled")
        private val KEY_TARGETS = stringSetPreferencesKey("cloak_targets")
        private val KEY_ALSO_HIDE_DEV = booleanPreferencesKey("cloak_also_hide_dev")
        private val KEY_SERVICE_RUNNING = booleanPreferencesKey("cloak_service_running")
        private val KEY_CLOAK_ACTIVE = booleanPreferencesKey("cloak_active")
        private val KEY_SAVED_SERVICES = stringPreferencesKey("cloak_saved_services")
        private val KEY_SAVED_ACCESS_ENABLED = intPreferencesKey("cloak_saved_access_enabled")
        private val KEY_SAVED_DEV = booleanPreferencesKey("cloak_saved_dev")
        private val KEY_SAVED_ADB = booleanPreferencesKey("cloak_saved_adb")

        @Volatile
        private var instance: A11yCloakPrefs? = null

        fun get(context: Context): A11yCloakPrefs =
            instance ?: synchronized(this) {
                instance ?: A11yCloakPrefs(context.applicationContext).also { instance = it }
            }
    }
}
