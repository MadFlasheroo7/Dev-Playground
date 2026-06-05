package pro.jayeshseth.devcontrols.util

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Process

object ForegroundAppDetector {

    /**
     * Returns the package name of the most-recently foregrounded app in the last 30 s,
     * or null if not granted PACKAGE_USAGE_STATS or no events found.
     */
    fun currentForegroundPackage(context: Context): String? {
        if (!hasUsageStatsPermission(context)) return null
        val mgr = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
            ?: return null
        val now = System.currentTimeMillis()
        val events = mgr.queryEvents(now - 30_000L, now)
        val ev = UsageEvents.Event()
        var lastPackage: String? = null
        while (events.hasNextEvent()) {
            events.getNextEvent(ev)
            if (ev.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND) {
                lastPackage = ev.packageName
            }
        }
        return lastPackage
    }

    fun hasUsageStatsPermission(context: Context): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager
            ?: return false
        val mode = appOps.unsafeCheckOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            context.packageName
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }
}
