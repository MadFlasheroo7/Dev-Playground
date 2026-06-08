package pro.jayeshseth.devcontrols.data

import android.content.Context
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.glance.appwidget.updateAll
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class DevSettingsRepository(private val context: Context) {

    private val resolver = context.contentResolver
    private val handler = Handler(Looper.getMainLooper())
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val timeoutPrefs = TimeoutPrefs(context)

    val developerOptionsEnabled: Flow<Boolean> =
        globalIntFlow(Settings.Global.DEVELOPMENT_SETTINGS_ENABLED).asBoolean()

    val usbDebuggingEnabled: Flow<Boolean> =
        globalIntFlow(Settings.Global.ADB_ENABLED).asBoolean()

    val wirelessDebuggingEnabled: Flow<Boolean> =
        globalIntFlow(ADB_WIFI_ENABLED).asBoolean()

    val kioskModeEnabled: Flow<Boolean> =
        globalIntFlow(LOCK_TO_APP_ENABLED).asBoolean()

    val screenTimeoutMs: Flow<Long> =
        systemIntFlow(Settings.System.SCREEN_OFF_TIMEOUT).asLong()

    val neverTimeoutEnabled: Flow<Boolean> = callbackFlow {
        val emit: () -> Unit = {
            val current = Settings.System.getInt(
                resolver, Settings.System.SCREEN_OFF_TIMEOUT, TimeoutPrefs.DEFAULT_TIMEOUT_MS.toInt()
            )
            trySend(current.toLong() == TimeoutPrefs.NEVER_TIMEOUT_MS)
        }
        val observer = object : ContentObserver(handler) {
            override fun onChange(selfChange: Boolean, uri: Uri?) {
                val v = Settings.System.getInt(
                    resolver, Settings.System.SCREEN_OFF_TIMEOUT, TimeoutPrefs.DEFAULT_TIMEOUT_MS.toInt()
                ).toLong()
                if (v != TimeoutPrefs.NEVER_TIMEOUT_MS) {
                    scope.launch { timeoutPrefs.savePreviousTimeout(v) }
                }
                emit()
            }
        }
        resolver.registerContentObserver(
            Settings.System.getUriFor(Settings.System.SCREEN_OFF_TIMEOUT), false, observer
        )
        emit()
        awaitClose { resolver.unregisterContentObserver(observer) }
    }.distinctUntilChanged()

    suspend fun setDeveloperOptions(enabled: Boolean): Result<Unit> = runCatching {
        Settings.Global.putInt(
            resolver, Settings.Global.DEVELOPMENT_SETTINGS_ENABLED, if (enabled) 1 else 0
        )
        Unit
    }

    suspend fun setUsbDebugging(enabled: Boolean): Result<Unit> = runCatching {
        Settings.Global.putInt(
            resolver, Settings.Global.ADB_ENABLED, if (enabled) 1 else 0
        )
        Unit
    }

    suspend fun setWirelessDebugging(enabled: Boolean): Result<Unit> = runCatching {
        Settings.Global.putInt(resolver, ADB_WIFI_ENABLED, if (enabled) 1 else 0)
        Unit
    }

    suspend fun setKioskMode(enabled: Boolean): Result<Unit> = runCatching {
        Settings.Global.putInt(resolver, LOCK_TO_APP_ENABLED, if (enabled) 1 else 0)
        Unit
    }

    suspend fun setNeverTimeout(enabled: Boolean): Result<Unit> = runCatching {
        if (enabled) {
            val current = Settings.System.getInt(
                resolver, Settings.System.SCREEN_OFF_TIMEOUT, TimeoutPrefs.DEFAULT_TIMEOUT_MS.toInt()
            ).toLong()
            if (current != TimeoutPrefs.NEVER_TIMEOUT_MS) {
                timeoutPrefs.savePreviousTimeout(current)
            }
            Settings.System.putInt(
                resolver, Settings.System.SCREEN_OFF_TIMEOUT, Int.MAX_VALUE
            )
        } else {
            val restore = timeoutPrefs.readPreviousTimeoutOnce()
            Settings.System.putInt(resolver, Settings.System.SCREEN_OFF_TIMEOUT, restore.toInt())
        }
        Unit
    }

    fun currentDeveloperOptions(): Boolean =
        Settings.Global.getInt(resolver, Settings.Global.DEVELOPMENT_SETTINGS_ENABLED, 0) == 1

    fun currentUsbDebugging(): Boolean =
        Settings.Global.getInt(resolver, Settings.Global.ADB_ENABLED, 0) == 1

    fun currentWirelessDebugging(): Boolean =
        Settings.Global.getInt(resolver, ADB_WIFI_ENABLED, 0) == 1

    fun currentKioskMode(): Boolean =
        Settings.Global.getInt(resolver, LOCK_TO_APP_ENABLED, 0) == 1

    fun currentNeverTimeout(): Boolean =
        Settings.System.getInt(
            resolver, Settings.System.SCREEN_OFF_TIMEOUT, TimeoutPrefs.DEFAULT_TIMEOUT_MS.toInt()
        ).toLong() == TimeoutPrefs.NEVER_TIMEOUT_MS

    fun currentScreenTimeoutMs(): Long =
        Settings.System.getInt(
            resolver, Settings.System.SCREEN_OFF_TIMEOUT, TimeoutPrefs.DEFAULT_TIMEOUT_MS.toInt()
        ).toLong()

    private fun globalIntFlow(key: String): Flow<Int> = callbackFlow {
        val read = {
            trySend(Settings.Global.getInt(resolver, key, 0))
        }
        val observer = object : ContentObserver(handler) {
            override fun onChange(selfChange: Boolean, uri: Uri?) { read() }
        }
        resolver.registerContentObserver(Settings.Global.getUriFor(key), false, observer)
        read()
        awaitClose { resolver.unregisterContentObserver(observer) }
    }.distinctUntilChanged()

    private fun systemIntFlow(key: String): Flow<Int> = callbackFlow {
        val read = {
            trySend(Settings.System.getInt(resolver, key, 0))
        }
        val observer = object : ContentObserver(handler) {
            override fun onChange(selfChange: Boolean, uri: Uri?) { read() }
        }
        resolver.registerContentObserver(Settings.System.getUriFor(key), false, observer)
        read()
        awaitClose { resolver.unregisterContentObserver(observer) }
    }.distinctUntilChanged()

    private fun Flow<Int>.asBoolean(): Flow<Boolean> = map { it == 1 }
    private fun Flow<Int>.asLong(): Flow<Long> = map { it.toLong() }

    /**
     * Registers a single ContentObserver per tracked Settings key that re-renders all
     * pinned Glance widget instances when the underlying value changes from anywhere
     * (this app, Settings UI, ADB). Should be called once from Application.onCreate.
     */
    fun registerWidgetRefresh() {
        val keys = listOf(
            Settings.Global.getUriFor(Settings.Global.DEVELOPMENT_SETTINGS_ENABLED),
            Settings.Global.getUriFor(Settings.Global.ADB_ENABLED),
            Settings.Global.getUriFor(ADB_WIFI_ENABLED),
            Settings.Global.getUriFor(LOCK_TO_APP_ENABLED),
            Settings.System.getUriFor(Settings.System.SCREEN_OFF_TIMEOUT),
        )
        val observer = object : ContentObserver(handler) {
            override fun onChange(selfChange: Boolean, uri: Uri?) {
                scope.launch {
                    runCatching {
                        pro.jayeshseth.devcontrols.widget.DevControlsWidget().updateAll(context)
                    }
                }
            }
        }
        keys.forEach { uri ->
            resolver.registerContentObserver(uri, false, observer)
        }
    }

    companion object {
        // Settings.Global.ADB_WIFI_ENABLED is @hide but the string constant is stable
        const val ADB_WIFI_ENABLED = "adb_wifi_enabled"

        // Settings.Global.LOCK_TO_APP_ENABLED also @hide but stable
        const val LOCK_TO_APP_ENABLED = "lock_to_app_enabled"

        @Volatile
        private var instance: DevSettingsRepository? = null

        fun get(context: Context): DevSettingsRepository =
            instance ?: synchronized(this) {
                instance ?: DevSettingsRepository(context.applicationContext).also { instance = it }
            }
    }
}
