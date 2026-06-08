package pro.jayeshseth.devcontrols.adb

/**
 * A handful of manifest permissions that are actually gated by an appop rather
 * than a runtime grant. For those, `pm grant <perm>` is a no-op and you have to
 * `appops set <pkg> <op> allow`. Lookup map for translating either way.
 */
object PermissionMapping {

    /** Permission name → appop name. If a permission is in this map, prefer appops. */
    val PERMISSION_TO_APPOP: Map<String, String> = mapOf(
        "android.permission.WRITE_SETTINGS" to "WRITE_SETTINGS",
        "android.permission.PACKAGE_USAGE_STATS" to "GET_USAGE_STATS",
        "android.permission.SYSTEM_ALERT_WINDOW" to "SYSTEM_ALERT_WINDOW",
        "android.permission.MANAGE_EXTERNAL_STORAGE" to "MANAGE_EXTERNAL_STORAGE",
        "android.permission.REQUEST_INSTALL_PACKAGES" to "REQUEST_INSTALL_PACKAGES",
        "android.permission.LOADER_USAGE_STATS" to "LOADER_USAGE_STATS",
        "android.permission.SCHEDULE_EXACT_ALARM" to "SCHEDULE_EXACT_ALARM",
        "android.permission.USE_EXACT_ALARM" to "USE_EXACT_ALARM",
        "android.permission.ACCESS_NOTIFICATION_POLICY" to "ACCESS_NOTIFICATION_POLICY",
        "android.permission.BIND_NOTIFICATION_LISTENER_SERVICE" to "BIND_NOTIFICATION_LISTENER_SERVICE",
        "android.permission.MANAGE_MEDIA" to "MANAGE_MEDIA",
        "android.permission.ACCESS_MEDIA_LOCATION" to "ACCESS_MEDIA_LOCATION",
    )

    /** Common appops worth exposing as quick-toggle chips even when the
     *  package doesn't declare them as permissions. */
    val COMMON_APPOPS: List<String> = listOf(
        "WRITE_SETTINGS",
        "GET_USAGE_STATS",
        "SYSTEM_ALERT_WINDOW",
        "MANAGE_EXTERNAL_STORAGE",
        "REQUEST_INSTALL_PACKAGES",
        "ACCESS_RESTRICTED_SETTINGS",
        "SCHEDULE_EXACT_ALARM",
    )
}
