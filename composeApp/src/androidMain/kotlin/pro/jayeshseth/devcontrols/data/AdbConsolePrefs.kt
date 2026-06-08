package pro.jayeshseth.devcontrols.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * DataStore for the embedded-ADB console: the `paired` flag (whether we've completed
 * the one-time TLS pair with the device's own wireless-debug server) and the recent
 * command history surfaced in [pro.jayeshseth.devcontrols.ui.AdbcScreen].
 */
private val Context.adbConsoleDataStore: DataStore<Preferences> by preferencesDataStore(name = "adb_console_prefs")

class AdbConsolePrefs private constructor(private val context: Context) {
    private val store get() = context.adbConsoleDataStore

    val paired: Flow<Boolean> = store.data.map { it[KEY_PAIRED] ?: false }
    val history: Flow<List<String>> = store.data.map { prefs ->
        prefs[KEY_HISTORY]?.split(SEP)?.filter { it.isNotBlank() } ?: emptyList()
    }

    suspend fun markPaired(paired: Boolean) {
        store.edit { it[KEY_PAIRED] = paired }
    }

    suspend fun pushHistory(cmd: String) {
        if (cmd.isBlank()) return
        store.edit { prefs ->
            val current = prefs[KEY_HISTORY]?.split(SEP)?.filter { it.isNotBlank() } ?: emptyList()
            val deduped = listOf(cmd) + current.filter { it != cmd }
            prefs[KEY_HISTORY] = deduped.take(MAX_HISTORY).joinToString(SEP)
        }
    }

    companion object {
        private const val SEP = ""
        private const val MAX_HISTORY = 50
        private val KEY_PAIRED = booleanPreferencesKey("paired")
        private val KEY_HISTORY = stringPreferencesKey("history")

        @Volatile
        private var instance: AdbConsolePrefs? = null

        fun get(context: Context): AdbConsolePrefs =
            instance ?: synchronized(this) {
                instance ?: AdbConsolePrefs(context.applicationContext).also { instance = it }
            }
    }
}
