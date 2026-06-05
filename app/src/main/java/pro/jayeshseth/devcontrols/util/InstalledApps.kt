package pro.jayeshseth.devcontrols.util

import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager

data class AppEntry(val packageName: String, val label: String)

/**
 * Shared loader for the launchable user apps shown in every picker (kiosk target,
 * Hide Dev / Force Dark / Cloak allow-lists, Focus Calls allow-list, `adb_pm`).
 * Filters out system apps and packages with no launcher entry. Requires
 * `QUERY_ALL_PACKAGES` (declared in the manifest).
 */
object InstalledApps {
    fun userApps(pm: PackageManager): List<AppEntry> {
        val all = pm.getInstalledApplications(PackageManager.GET_META_DATA)
        return all.asSequence()
            .filter { isUserApp(it) }
            .filter { pm.getLaunchIntentForPackage(it.packageName) != null }
            .map { AppEntry(it.packageName, pm.getApplicationLabel(it).toString()) }
            .sortedBy { it.label.lowercase() }
            .toList()
    }

    /** Every installed package (system included), for the low-level ADB/package tools. */
    fun allApps(pm: PackageManager): List<AppEntry> {
        return pm.getInstalledApplications(PackageManager.GET_META_DATA).asSequence()
            .map { AppEntry(it.packageName, runCatching { pm.getApplicationLabel(it).toString() }.getOrDefault(it.packageName)) }
            .sortedBy { it.label.lowercase() }
            .toList()
    }

    private fun isUserApp(info: ApplicationInfo): Boolean {
        val isSystem = (info.flags and ApplicationInfo.FLAG_SYSTEM) != 0
        val isUpdatedSystem = (info.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0
        return !isSystem || isUpdatedSystem
    }
}
