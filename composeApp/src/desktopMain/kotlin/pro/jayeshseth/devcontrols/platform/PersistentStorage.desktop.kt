package pro.jayeshseth.devcontrols.platform

import java.util.prefs.Preferences

actual object PersistentStorage {
    private val prefs = Preferences.userRoot().node("pro.jayeshseth.devcontrols")

    actual fun saveLong(key: String, value: Long) {
        prefs.putLong(key, value)
        try { prefs.flush() } catch (e: Exception) {}
    }

    actual fun getLong(key: String, defaultValue: Long): Long {
        return prefs.getLong(key, defaultValue)
    }

    actual fun saveBoolean(key: String, value: Boolean) {
        prefs.putBoolean(key, value)
        try { prefs.flush() } catch (e: Exception) {}
    }

    actual fun getBoolean(key: String, defaultValue: Boolean): Boolean {
        return prefs.getBoolean(key, defaultValue)
    }

    actual fun saveString(key: String, value: String) {
        prefs.put(key, value)
        try { prefs.flush() } catch (e: Exception) {}
    }

    actual fun getString(key: String, defaultValue: String): String {
        return prefs.get(key, defaultValue)
    }
}
