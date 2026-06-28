package pro.jayeshseth.devcontrols.platform

import platform.Foundation.NSUserDefaults

actual object PersistentStorage {
    actual fun saveLong(key: String, value: Long) {
        NSUserDefaults.standardUserDefaults.setInteger(value, forKey = key)
    }

    actual fun getLong(key: String, defaultValue: Long): Long {
        val defaults = NSUserDefaults.standardUserDefaults
        return if (defaults.objectForKey(key) != null) {
            defaults.integerForKey(key)
        } else {
            defaultValue
        }
    }

    actual fun saveBoolean(key: String, value: Boolean) {
        NSUserDefaults.standardUserDefaults.setBoolean(value, forKey = key)
    }

    actual fun getBoolean(key: String, defaultValue: Boolean): Boolean {
        val defaults = NSUserDefaults.standardUserDefaults
        return if (defaults.objectForKey(key) != null) {
            defaults.boolForKey(key)
        } else {
            defaultValue
        }
    }

    actual fun saveString(key: String, value: String) {
        NSUserDefaults.standardUserDefaults.setObject(value, forKey = key)
    }

    actual fun getString(key: String, defaultValue: String): String {
        return NSUserDefaults.standardUserDefaults.stringForKey(key) ?: defaultValue
    }
}
