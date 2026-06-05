package pro.jayeshseth.devcontrols.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/**
 * DataStore for Focus Calls: master enable, auto-answer flag, the architecture
 * selector (A relaunch vs B default-dialer — mutually exclusive), the allow-list of
 * apps to stay in, and the transient `pending_return_package` stashed on RINGING and
 * relaunched on OFFHOOK by [pro.jayeshseth.devcontrols.call.PhoneStateReceiver].
 */
private val Context.focusCallsDataStore: DataStore<Preferences> by preferencesDataStore(name = "focus_calls_prefs")

class FocusCallsPrefs private constructor(private val context: Context) {
    private val store get() = context.focusCallsDataStore

    val focusCallsEnabled: Flow<Boolean> = store.data.map { it[KEY_ENABLED] ?: false }
    val autoAnswerEnabled: Flow<Boolean> = store.data.map { it[KEY_AUTO_ANSWER] ?: false }
    val archUseRelaunch: Flow<Boolean> = store.data.map { it[KEY_ARCH_RELAUNCH] ?: true }
    val archUseDialer: Flow<Boolean> = store.data.map { it[KEY_ARCH_DIALER] ?: false }
    val allowedPackages: Flow<Set<String>> = store.data.map { it[KEY_ALLOWED_PKGS] ?: emptySet() }
    val pendingReturnPackage: Flow<String?> = store.data.map { it[KEY_PENDING_RETURN] }

    suspend fun setFocusCallsEnabled(enabled: Boolean) {
        store.edit { it[KEY_ENABLED] = enabled }
    }

    suspend fun setAutoAnswer(enabled: Boolean) {
        store.edit { it[KEY_AUTO_ANSWER] = enabled }
    }

    /** Mutually exclusive radio: setting one true flips the other false. */
    suspend fun selectRelaunchArch() {
        store.edit {
            it[KEY_ARCH_RELAUNCH] = true
            it[KEY_ARCH_DIALER] = false
        }
    }

    suspend fun selectDialerArch() {
        store.edit {
            it[KEY_ARCH_RELAUNCH] = false
            it[KEY_ARCH_DIALER] = true
        }
    }

    suspend fun setAllowed(pkg: String, allowed: Boolean) {
        store.edit { prefs ->
            val current = prefs[KEY_ALLOWED_PKGS] ?: emptySet()
            prefs[KEY_ALLOWED_PKGS] = if (allowed) current + pkg else current - pkg
        }
    }

    suspend fun setPendingReturnPackage(pkg: String?) {
        store.edit { prefs ->
            if (pkg == null) prefs.remove(KEY_PENDING_RETURN)
            else prefs[KEY_PENDING_RETURN] = pkg
        }
    }

    suspend fun readSnapshot(): Snapshot {
        val prefs = store.data.first()
        return Snapshot(
            enabled = prefs[KEY_ENABLED] ?: false,
            autoAnswer = prefs[KEY_AUTO_ANSWER] ?: false,
            useRelaunch = prefs[KEY_ARCH_RELAUNCH] ?: true,
            useDialer = prefs[KEY_ARCH_DIALER] ?: false,
            allowedPackages = prefs[KEY_ALLOWED_PKGS] ?: emptySet(),
            pendingReturnPackage = prefs[KEY_PENDING_RETURN]
        )
    }

    data class Snapshot(
        val enabled: Boolean,
        val autoAnswer: Boolean,
        val useRelaunch: Boolean,
        val useDialer: Boolean,
        val allowedPackages: Set<String>,
        val pendingReturnPackage: String?,
    )

    companion object {
        private val KEY_ENABLED = booleanPreferencesKey("focus_calls_enabled")
        private val KEY_AUTO_ANSWER = booleanPreferencesKey("auto_answer_enabled")
        private val KEY_ARCH_RELAUNCH = booleanPreferencesKey("arch_use_relaunch")
        private val KEY_ARCH_DIALER = booleanPreferencesKey("arch_use_dialer")
        private val KEY_ALLOWED_PKGS = stringSetPreferencesKey("allowed_packages")
        private val KEY_PENDING_RETURN = stringPreferencesKey("pending_return_package")

        @Volatile
        private var instance: FocusCallsPrefs? = null

        fun get(context: Context): FocusCallsPrefs =
            instance ?: synchronized(this) {
                instance ?: FocusCallsPrefs(context.applicationContext).also { instance = it }
            }
    }
}
