package pro.jayeshseth.devcontrols.util

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.content.pm.ApplicationInfo
import android.provider.Settings
import android.view.accessibility.AccessibilityManager

/**
 * The mirror image of [AdbDetection], but for accessibility services. This is the
 * "eyes" privilege: any bound accessibility service with
 * `canRetrieveWindowContent` can read what every other app draws, and one with
 * `canPerformGestures` can tap/swipe on their behalf. That is exactly the surface
 * our own Kiosk / Hide Dev / Force Dark service rides on.
 *
 * A hardened prod app (Moneyview) uses these same reads to notice that *some*
 * service — possibly a tampering tool — is watching it. Every probe runs with no
 * special permission, so the snippets drop straight into a locked-down build.
 *
 * Severity reuses [AdbDetection.Severity]:
 *  - [AdbDetection.Severity.Clean]: nothing, or a system service (TalkBack, etc.)
 *  - [AdbDetection.Severity.Suspicious]: a third-party service is bound
 *  - [AdbDetection.Severity.Tampered]: a third-party service that can read window
 *    content (and especially inject gestures) — the high-risk shape
 */
object A11yDetection {

    data class ServiceReading(
        val component: String,
        val packageName: String,
        val label: String,
        val isSystemApp: Boolean,
        val isOurs: Boolean,
        val canRetrieveWindowContent: Boolean,
        val canPerformGestures: Boolean,
        val canFilterKeyEvents: Boolean,
        val feedbackType: String,
        val severity: AdbDetection.Severity,
        val reason: String,
    )

    data class Snapshot(
        val accessibilityEnabled: Int,
        val managerEnabled: Boolean,
        val touchExploration: Boolean,
        val rawEnabledServices: String,
        val services: List<ServiceReading>,
        val overall: AdbDetection.Severity,
    )

    /** Each is (title, copyable Kotlin) — the detection recipes Moneyview can adopt. */
    val snippets: List<Pair<String, String>> = listOf(
        "Master flag" to """
            // Cheapest check: is the accessibility subsystem on at all?
            val on = Settings.Secure.getInt(contentResolver, "accessibility_enabled", 0) == 1
            if (on) { /* at least one service is (or was) enabled */ }
        """.trimIndent(),

        "Enumerate bound services" to """
            val am = getSystemService(AccessibilityManager::class.java)
            val risky = am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
                .filter { info ->
                    val ai = info.resolveInfo.serviceInfo.applicationInfo
                    val system = (ai.flags and (ApplicationInfo.FLAG_SYSTEM or ApplicationInfo.FLAG_UPDATED_SYSTEM_APP)) != 0
                    val readsContent = (info.capabilities and
                        AccessibilityServiceInfo.CAPABILITY_CAN_RETRIEVE_WINDOW_CONTENT) != 0
                    !system && readsContent          // third-party that can read your screen
                }
            if (risky.isNotEmpty()) { /* a non-system service can read this window */ }
        """.trimIndent(),

        "Raw setting string" to """
            // Same data the system stores; cheaper than the AccessibilityManager IPC.
            val raw = Settings.Secure.getString(contentResolver, "enabled_accessibility_services") ?: ""
            val components = raw.split(':').filter { it.isNotBlank() }
            // Flag any component whose package is not a known-good allow-list entry.
        """.trimIndent(),

        "React at runtime" to """
            // Catch a service that is toggled on *after* your app launched.
            val am = getSystemService(AccessibilityManager::class.java)
            am.addAccessibilityStateChangeListener { enabled ->
                if (enabled) { /* re-run the enumeration above */ }
            }
        """.trimIndent(),
    )

    fun scan(context: Context): Snapshot {
        val resolver = context.contentResolver
        val accessibilityEnabled = Settings.Secure.getInt(resolver, "accessibility_enabled", 0)
        val touch = Settings.Secure.getInt(resolver, "touch_exploration_enabled", 0) == 1
        val raw = Settings.Secure.getString(resolver, "enabled_accessibility_services") ?: ""

        val am = context.getSystemService(AccessibilityManager::class.java)
        val managerEnabled = am?.isEnabled == true
        val pm = context.packageManager

        val services = am
            ?.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
            ?.map { info ->
                val si = info.resolveInfo.serviceInfo
                val ai = si.applicationInfo
                val isSystem =
                    (ai.flags and (ApplicationInfo.FLAG_SYSTEM or ApplicationInfo.FLAG_UPDATED_SYSTEM_APP)) != 0
                val isOurs = si.packageName == context.packageName
                val readsContent =
                    (info.capabilities and AccessibilityServiceInfo.CAPABILITY_CAN_RETRIEVE_WINDOW_CONTENT) != 0
                val gestures =
                    (info.capabilities and AccessibilityServiceInfo.CAPABILITY_CAN_PERFORM_GESTURES) != 0
                val keyEvents =
                    (info.capabilities and AccessibilityServiceInfo.CAPABILITY_CAN_REQUEST_FILTER_KEY_EVENTS) != 0

                val (severity, reason) = when {
                    isOurs -> AdbDetection.Severity.Tampered to
                        "This is Dev Controls' own service — the engine behind Kiosk / Hide Dev / Force Dark."
                    !isSystem && readsContent && gestures -> AdbDetection.Severity.Tampered to
                        "Third-party service can read window content AND inject taps/swipes. Highest-risk shape."
                    !isSystem && readsContent -> AdbDetection.Severity.Suspicious to
                        "Third-party service can read window content."
                    !isSystem -> AdbDetection.Severity.Suspicious to
                        "Third-party accessibility service is bound."
                    else -> AdbDetection.Severity.Clean to
                        "System-signed accessibility service (e.g. TalkBack)."
                }

                ServiceReading(
                    component = "${si.packageName}/${si.name}",
                    packageName = si.packageName,
                    label = runCatching { info.resolveInfo.loadLabel(pm).toString() }
                        .getOrDefault(si.packageName),
                    isSystemApp = isSystem,
                    isOurs = isOurs,
                    canRetrieveWindowContent = readsContent,
                    canPerformGestures = gestures,
                    canFilterKeyEvents = keyEvents,
                    feedbackType = feedbackString(info.feedbackType),
                    severity = severity,
                    reason = reason,
                )
            }
            ?.sortedByDescending { it.severity.ordinal }
            ?: emptyList()

        val overall = when {
            services.any { it.severity == AdbDetection.Severity.Tampered } -> AdbDetection.Severity.Tampered
            services.any { it.severity == AdbDetection.Severity.Suspicious } -> AdbDetection.Severity.Suspicious
            else -> AdbDetection.Severity.Clean
        }

        return Snapshot(
            accessibilityEnabled = accessibilityEnabled,
            managerEnabled = managerEnabled,
            touchExploration = touch,
            rawEnabledServices = raw.ifEmpty { "(none)" },
            services = services,
            overall = overall,
        )
    }

    private fun feedbackString(mask: Int): String {
        if (mask == AccessibilityServiceInfo.FEEDBACK_ALL_MASK) return "ALL"
        val parts = buildList {
            if (mask and AccessibilityServiceInfo.FEEDBACK_SPOKEN != 0) add("spoken")
            if (mask and AccessibilityServiceInfo.FEEDBACK_HAPTIC != 0) add("haptic")
            if (mask and AccessibilityServiceInfo.FEEDBACK_AUDIBLE != 0) add("audible")
            if (mask and AccessibilityServiceInfo.FEEDBACK_VISUAL != 0) add("visual")
            if (mask and AccessibilityServiceInfo.FEEDBACK_GENERIC != 0) add("generic")
            if (mask and AccessibilityServiceInfo.FEEDBACK_BRAILLE != 0) add("braille")
        }
        return if (parts.isEmpty()) "(none)" else parts.joinToString("+")
    }
}
