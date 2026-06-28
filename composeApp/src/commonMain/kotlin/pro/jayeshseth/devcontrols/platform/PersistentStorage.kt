package pro.jayeshseth.devcontrols.platform

expect object PersistentStorage {
    fun saveLong(key: String, value: Long)
    fun getLong(key: String, defaultValue: Long): Long
    fun saveBoolean(key: String, value: Boolean)
    fun getBoolean(key: String, defaultValue: Boolean): Boolean
    fun saveString(key: String, value: String)
    fun getString(key: String, defaultValue: String): String
}
