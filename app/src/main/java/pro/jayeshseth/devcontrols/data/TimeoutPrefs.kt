package pro.jayeshseth.devcontrols.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/**
 * Escrow for the never-timeout toggle: stores the screen-off timeout that was in
 * effect before it was swapped to MAX, so disabling never-timeout restores the
 * user's real value rather than a guessed default. See ARCHITECTURE.md → escrow pattern.
 */
private val Context.timeoutDataStore: DataStore<Preferences> by preferencesDataStore(name = "timeout_prefs")

class TimeoutPrefs(private val context: Context) {
    private val previousTimeoutKey = longPreferencesKey("previous_timeout_ms")

    val previousTimeout: Flow<Long> = context.timeoutDataStore.data.map { prefs ->
        prefs[previousTimeoutKey] ?: DEFAULT_TIMEOUT_MS
    }

    suspend fun savePreviousTimeout(valueMs: Long) {
        if (valueMs == Int.MAX_VALUE.toLong()) return
        context.timeoutDataStore.edit { prefs ->
            prefs[previousTimeoutKey] = valueMs
        }
    }

    suspend fun readPreviousTimeoutOnce(): Long =
        previousTimeout.first()

    companion object {
        const val DEFAULT_TIMEOUT_MS = 30_000L
        const val NEVER_TIMEOUT_MS = Int.MAX_VALUE.toLong()
    }
}
