package pro.jayeshseth.devcontrols.util

import android.content.Context
import android.content.pm.ApplicationInfo
import android.os.Build
import android.os.Debug
import android.provider.Settings
import java.io.File
import java.net.InetSocketAddress
import java.net.Socket

/**
 * Every signal a prod app can use to detect ADB activity / dev-mode tampering /
 * debugger presence. Each [Signal] runs without special permission — even your
 * locked-down Moneyview build can use any of these as-is.
 *
 * Severity reflects what an attacker would have to do to trigger it:
 *  - [Severity.Clean]: nothing suspicious
 *  - [Severity.Suspicious]: someone enabled a debug feature
 *  - [Severity.Tampered]: active debugging / tracing in progress
 */
object AdbDetection {

    enum class Severity { Clean, Suspicious, Tampered, Unknown }

    data class Reading(
        val value: String,
        val severity: Severity,
        val explanation: String,
    )

    data class Signal(
        val id: String,
        val title: String,
        val subtitle: String,
        val snippet: String,
        val probe: (Context) -> Reading,
    )

    val signals: List<Signal> = listOf(

        Signal(
            id = "adb_enabled",
            title = "Settings.Global.ADB_ENABLED",
            subtitle = "USB debugging toggle",
            snippet = """
                val adb = Settings.Global.getInt(contentResolver, "adb_enabled", 0)
                if (adb == 1) { /* USB debugging is on */ }
            """.trimIndent()
        ) { ctx ->
            val v = Settings.Global.getInt(ctx.contentResolver, "adb_enabled", 0)
            Reading(
                value = v.toString(),
                severity = if (v == 1) Severity.Suspicious else Severity.Clean,
                explanation = if (v == 1) "USB debugging is enabled."
                              else "USB debugging is off."
            )
        },

        Signal(
            id = "adb_wifi_enabled",
            title = "Settings.Global.ADB_WIFI_ENABLED",
            subtitle = "Wireless debugging (API 30+)",
            snippet = """
                val v = Settings.Global.getInt(contentResolver, "adb_wifi_enabled", 0)
                if (v == 1) { /* wireless ADB is on */ }
            """.trimIndent()
        ) { ctx ->
            val v = Settings.Global.getInt(ctx.contentResolver, "adb_wifi_enabled", 0)
            Reading(
                value = v.toString(),
                severity = if (v == 1) Severity.Suspicious else Severity.Clean,
                explanation = if (v == 1) "Wireless ADB is enabled."
                              else "Wireless ADB is off."
            )
        },

        Signal(
            id = "dev_settings_enabled",
            title = "Settings.Global.DEVELOPMENT_SETTINGS_ENABLED",
            subtitle = "Developer options master flag",
            snippet = """
                val v = Settings.Global.getInt(contentResolver, "development_settings_enabled", 0)
                if (v == 1) { /* developer options is on */ }
            """.trimIndent()
        ) { ctx ->
            val v = Settings.Global.getInt(ctx.contentResolver, "development_settings_enabled", 0)
            Reading(
                value = v.toString(),
                severity = if (v == 1) Severity.Suspicious else Severity.Clean,
                explanation = if (v == 1) "Developer options is enabled."
                              else "Developer options is off."
            )
        },

        Signal(
            id = "debugger_connected",
            title = "Debug.isDebuggerConnected()",
            subtitle = "Live JDWP attach",
            snippet = """
                if (android.os.Debug.isDebuggerConnected()) { /* JDWP attached now */ }
            """.trimIndent()
        ) { _ ->
            val v = Debug.isDebuggerConnected()
            Reading(
                value = v.toString(),
                severity = if (v) Severity.Tampered else Severity.Clean,
                explanation = if (v) "JDWP debugger is currently attached."
                              else "No JDWP debugger attached."
            )
        },

        Signal(
            id = "waiting_for_debugger",
            title = "Debug.waitingForDebugger()",
            subtitle = "App was launched with --start-debug",
            snippet = """
                if (android.os.Debug.waitingForDebugger()) { /* launched waiting for debugger */ }
            """.trimIndent()
        ) { _ ->
            val v = Debug.waitingForDebugger()
            Reading(
                value = v.toString(),
                severity = if (v) Severity.Tampered else Severity.Clean,
                explanation = if (v) "App is waiting for a debugger to attach."
                              else "Not in waiting-for-debugger state."
            )
        },

        Signal(
            id = "tracer_pid",
            title = "/proc/self/status TracerPid:",
            subtitle = "ptrace attach (frida etc.)",
            snippet = """
                val line = java.io.File("/proc/self/status").readLines()
                    .firstOrNull { it.startsWith("TracerPid:") } ?: ""
                val tracer = line.substringAfter(":").trim().toIntOrNull() ?: 0
                if (tracer != 0) { /* a process with that PID is ptracing us (frida-server, etc.) */ }
            """.trimIndent()
        ) { _ ->
            val line = runCatching {
                File("/proc/self/status").readLines()
                    .firstOrNull { it.startsWith("TracerPid:") }
            }.getOrNull()
            val tracer = line?.substringAfter(":")?.trim()?.toIntOrNull() ?: 0
            Reading(
                value = "TracerPid = $tracer",
                severity = if (tracer != 0) Severity.Tampered else Severity.Clean,
                explanation = if (tracer != 0)
                    "PID $tracer is ptrace-attached to this process. Almost always frida/lsposed/strace."
                else "No process is tracing us."
            )
        },

        Signal(
            id = "build_tags",
            title = "Build.TAGS",
            subtitle = "ROM build tag",
            snippet = """
                if (Build.TAGS?.contains("test-keys") == true) { /* custom / debug ROM */ }
            """.trimIndent()
        ) { _ ->
            val v = Build.TAGS ?: ""
            val bad = v.contains("test-keys")
            Reading(
                value = v.ifEmpty { "(empty)" },
                severity = if (bad) Severity.Suspicious else Severity.Clean,
                explanation = if (bad)
                    "ROM signed with test-keys (custom or eng build)."
                else "Production-signed ROM."
            )
        },

        Signal(
            id = "build_type",
            title = "Build.TYPE",
            subtitle = "user / userdebug / eng",
            snippet = """
                if (Build.TYPE != "user") { /* userdebug or eng build → non-prod ROM */ }
            """.trimIndent()
        ) { _ ->
            val v = Build.TYPE ?: ""
            val bad = v != "user"
            Reading(
                value = v.ifEmpty { "(empty)" },
                severity = if (bad) Severity.Suspicious else Severity.Clean,
                explanation = if (bad) "Non-prod build type."
                              else "Production build type."
            )
        },

        Signal(
            id = "self_debuggable",
            title = "ApplicationInfo.FLAG_DEBUGGABLE on self",
            subtitle = "Was your APK shipped debuggable?",
            snippet = """
                val dbg = (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
                if (dbg) { /* your release APK has android:debuggable=true — critical */ }
            """.trimIndent()
        ) { ctx ->
            val dbg = (ctx.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
            Reading(
                value = dbg.toString(),
                severity = if (dbg) Severity.Tampered else Severity.Clean,
                explanation = if (dbg)
                    "Your APK ships debuggable=true. `adb shell run-as <pkg>` works on it."
                else "APK is non-debuggable in release."
            )
        },

        Signal(
            id = "prop_ro_debuggable",
            title = "getprop ro.debuggable",
            subtitle = "Whole-ROM debuggable flag",
            snippet = """
                val v = systemPropGet("ro.debuggable") // private SystemProperties.get via reflection
                if (v == "1") { /* ROM is debuggable */ }
            """.trimIndent()
        ) { _ ->
            val v = getSystemProp("ro.debuggable") ?: "?"
            val bad = v == "1"
            Reading(
                value = v,
                severity = if (bad) Severity.Suspicious else Severity.Clean,
                explanation = if (bad) "ROM is built debuggable."
                              else "ROM is non-debuggable."
            )
        },

        Signal(
            id = "prop_secure",
            title = "getprop ro.secure",
            subtitle = "ADB-root protection flag",
            snippet = """
                val v = systemPropGet("ro.secure")
                if (v == "0") { /* root by default — most ROMs are 1 */ }
            """.trimIndent()
        ) { _ ->
            val v = getSystemProp("ro.secure") ?: "?"
            val bad = v == "0"
            Reading(
                value = v,
                severity = if (bad) Severity.Suspicious else Severity.Clean,
                explanation = if (bad) "ro.secure=0 — ADB has root by default."
                              else "ro.secure=1 — production-secure."
            )
        },

        Signal(
            id = "prop_adb_secure",
            title = "getprop ro.adb.secure",
            subtitle = "ADB key challenge enforcement",
            snippet = """
                val v = systemPropGet("ro.adb.secure")
                if (v == "0") { /* ADB doesn't require key challenge */ }
            """.trimIndent()
        ) { _ ->
            val v = getSystemProp("ro.adb.secure") ?: "?"
            val bad = v == "0"
            Reading(
                value = v,
                severity = if (bad) Severity.Suspicious else Severity.Clean,
                explanation = if (bad) "ADB bypass — no key challenge."
                              else "ADB enforces key challenges."
            )
        },

        Signal(
            id = "prop_init_adbd",
            title = "getprop init.svc.adbd",
            subtitle = "adbd service state",
            snippet = """
                val v = systemPropGet("init.svc.adbd")
                if (v == "running") { /* adbd daemon is up */ }
            """.trimIndent()
        ) { _ ->
            val v = getSystemProp("init.svc.adbd") ?: "?"
            val bad = v == "running"
            Reading(
                value = v,
                severity = if (bad) Severity.Suspicious else Severity.Clean,
                explanation = if (bad) "adbd is running — host may have ADB shell."
                              else "adbd is stopped."
            )
        },

        Signal(
            id = "adb_5555",
            title = "TCP probe 127.0.0.1:5555",
            subtitle = "Legacy ADB-over-TCP port",
            snippet = """
                val open = try {
                    java.net.Socket().use { it.connect(java.net.InetSocketAddress("127.0.0.1", 5555), 500); true }
                } catch (_: Throwable) { false }
                if (open) { /* adbd listening on the legacy 5555 port */ }
            """.trimIndent()
        ) { _ ->
            val open = runCatching {
                Socket().use { it.connect(InetSocketAddress("127.0.0.1", 5555), 500); true }
            }.getOrDefault(false)
            Reading(
                value = if (open) "OPEN" else "closed",
                severity = if (open) Severity.Tampered else Severity.Clean,
                explanation = if (open)
                    "adbd is accepting connections on the legacy 5555 port."
                else "Port 5555 is closed."
            )
        },

        Signal(
            id = "frida_default_port",
            title = "TCP probe 127.0.0.1:27042",
            subtitle = "Frida default server port",
            snippet = """
                val open = try {
                    java.net.Socket().use { it.connect(java.net.InetSocketAddress("127.0.0.1", 27042), 300); true }
                } catch (_: Throwable) { false }
                if (open) { /* frida-server is listening */ }
            """.trimIndent()
        ) { _ ->
            val open = runCatching {
                Socket().use { it.connect(InetSocketAddress("127.0.0.1", 27042), 300); true }
            }.getOrDefault(false)
            Reading(
                value = if (open) "OPEN" else "closed",
                severity = if (open) Severity.Tampered else Severity.Clean,
                explanation = if (open) "frida-server is listening on its default port (27042)."
                              else "Default frida-server port closed."
            )
        },

        Signal(
            id = "su_binaries",
            title = "su / Magisk binary in PATH",
            subtitle = "Common root signals",
            snippet = """
                val paths = listOf(
                    "/system/bin/su", "/system/xbin/su", "/sbin/su",
                    "/data/local/xbin/su", "/data/local/bin/su",
                    "/system/sd/xbin/su", "/system/bin/failsafe/su",
                    "/data/adb/magisk", "/sbin/.magisk"
                )
                if (paths.any { java.io.File(it).exists() }) { /* root indicator */ }
            """.trimIndent()
        ) { _ ->
            val paths = listOf(
                "/system/bin/su", "/system/xbin/su", "/sbin/su",
                "/data/local/xbin/su", "/data/local/bin/su",
                "/system/sd/xbin/su", "/system/bin/failsafe/su",
                "/data/adb/magisk", "/sbin/.magisk"
            )
            val hit = paths.filter { runCatching { File(it).exists() }.getOrDefault(false) }
            Reading(
                value = if (hit.isEmpty()) "(none)" else hit.joinToString(),
                severity = if (hit.isEmpty()) Severity.Clean else Severity.Tampered,
                explanation = if (hit.isEmpty()) "No root binaries found."
                              else "Root indicator paths present."
            )
        },
    )

    /**
     * Reflective `SystemProperties.get(String, String)` — public on all Android
     * versions but the class is `@hide`, so go via reflection.
     */
    private fun getSystemProp(key: String): String? = runCatching {
        val cls = Class.forName("android.os.SystemProperties")
        val m = cls.getMethod("get", String::class.java, String::class.java)
        (m.invoke(null, key, "") as? String)?.takeIf { it.isNotEmpty() }
    }.getOrNull()
}
