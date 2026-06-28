package pro.jayeshseth.devcontrols.platform

import kotlinx.browser.window

actual object PersistentStorage {
    actual fun saveLong(key: String, value: Long) {
        window.localStorage.setItem(key, value.toString())
    }

    actual fun getLong(key: String, defaultValue: Long): Long {
        return window.localStorage.getItem(key)?.toLongOrNull() ?: defaultValue
    }

    actual fun saveBoolean(key: String, value: Boolean) {
        window.localStorage.setItem(key, value.toString())
    }

    actual fun getBoolean(key: String, defaultValue: Boolean): Boolean {
        return window.localStorage.getItem(key)?.toBooleanStrictOrNull() ?: defaultValue
    }

    actual fun saveString(key: String, value: String) {
        window.localStorage.setItem(key, value)
    }

    actual fun getString(key: String, defaultValue: String): String {
        return window.localStorage.getItem(key) ?: defaultValue
    }
}
