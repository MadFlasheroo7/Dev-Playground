package pro.jayeshseth.devcontrols.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.forceDarkDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "force_dark_prefs"
)

class ForceDarkPrefs private constructor(private val context: Context) {
    private val store get() = context.forceDarkDataStore

    val enabled: Flow<Boolean> = store.data.map { it[KEY_ENABLED] ?: false }
    val targetPackages: Flow<Set<String>> = store.data.map { it[KEY_TARGETS] ?: emptySet() }
    /** True while we currently have inversion forced ON because a target is foregrounded. */
    val currentlyForcing: Flow<Boolean> = store.data.map { it[KEY_ACTIVE] ?: false }

    suspend fun isEnabledOnce(): Boolean = enabled.first()
    suspend fun targetsOnce(): Set<String> = targetPackages.first()

    suspend fun setEnabled(value: Boolean) {
        store.edit { it[KEY_ENABLED] = value }
    }

    suspend fun setTargetIncluded(pkg: String, included: Boolean) {
        store.edit { prefs ->
            val current = prefs[KEY_TARGETS] ?: emptySet()
            prefs[KEY_TARGETS] = if (included) current + pkg else current - pkg
        }
    }

    /** Save the pre-force inversion value so we can restore it when leaving target apps. */
    suspend fun snapshotEscrow(savedInversion: Int) {
        store.edit { prefs ->
            prefs[KEY_ACTIVE] = true
            prefs[KEY_SAVED_INVERSION] = savedInversion
        }
    }

    suspend fun readEscrow(): Pair<Boolean, Int> {
        val prefs = store.data.first()
        return (prefs[KEY_ACTIVE] ?: false) to (prefs[KEY_SAVED_INVERSION] ?: 0)
    }

    suspend fun clearEscrow() {
        store.edit { prefs ->
            prefs.remove(KEY_ACTIVE)
            prefs.remove(KEY_SAVED_INVERSION)
        }
    }

    companion object {
        private val KEY_ENABLED = booleanPreferencesKey("force_dark_enabled")
        private val KEY_TARGETS = stringSetPreferencesKey("force_dark_targets")
        private val KEY_ACTIVE = booleanPreferencesKey("force_dark_active")
        private val KEY_SAVED_INVERSION = intPreferencesKey("force_dark_saved_inversion")

        @Volatile
        private var instance: ForceDarkPrefs? = null

        fun get(context: Context): ForceDarkPrefs =
            instance ?: synchronized(this) {
                instance ?: ForceDarkPrefs(context.applicationContext).also { instance = it }
            }
    }
}
