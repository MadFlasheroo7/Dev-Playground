package pro.jayeshseth.devcontrols.platform

import android.content.Context
import android.content.SharedPreferences

actual object PersistentStorage {
    private var prefs: SharedPreferences? = null

    fun init(context: Context) {
        prefs = context.applicationContext.getSharedPreferences("devcontrols_prefs", Context.MODE_PRIVATE)
    }

    actual fun saveLong(key: String, value: Long) {
        prefs?.edit()?.putLong(key, value)?.apply()
    }

    actual fun getLong(key: String, defaultValue: Long): Long {
        return prefs?.getLong(key, defaultValue) ?: defaultValue
    }

    actual fun saveBoolean(key: String, value: Boolean) {
        prefs?.edit()?.putBoolean(key, value)?.apply()
    }

    actual fun getBoolean(key: String, defaultValue: Boolean): Boolean {
        return prefs?.getBoolean(key, defaultValue) ?: defaultValue
    }

    actual fun saveString(key: String, value: String) {
        prefs?.edit()?.putString(key, value)?.apply()
    }

    actual fun getString(key: String, defaultValue: String): String {
        return prefs?.getString(key, defaultValue) ?: defaultValue
    }
}
