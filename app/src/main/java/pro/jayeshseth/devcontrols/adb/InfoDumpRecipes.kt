package pro.jayeshseth.devcontrols.adb

/**
 * A bundle of ADB shell commands that mine everything a non-root attacker can
 * see about an arbitrary installed app. Designed for self-recon of your own
 * production app to validate defence-in-depth (the inputs an attacker with
 * ADB shell access — UID 2000 — could harvest).
 */
data class DumpSection(
    val id: String,
    val title: String,
    val subtitle: String,
    val command: (pkg: String) -> String,
    val riskHint: String? = null,
)

object InfoDumpRecipes {

    val sections: List<DumpSection> = listOf(

        DumpSection(
            id = "meta",
            title = "Package metadata",
            subtitle = "versionCode, targetSdk, installer, install/update times, flags",
            command = { pkg ->
                "dumpsys package $pkg | sed -n '/Package \\[$pkg\\]/,/Activity Resolver Table/p' | head -100"
            },
            riskHint = "Check FLAG_DEBUGGABLE, allowBackup, networkSecurityConfig"
        ),

        DumpSection(
            id = "paths",
            title = "APK paths",
            subtitle = "base.apk + split APKs (pullable via adb pull)",
            command = { pkg -> "pm path $pkg" },
            riskHint = "Anyone with shell can copy your APK and reverse-engineer it"
        ),

        DumpSection(
            id = "sigs",
            title = "Signing certs",
            subtitle = "SHA-256 of the signing cert",
            command = { pkg -> "dumpsys package $pkg | grep -A1 -E 'Signatures|signing'" },
            riskHint = "Your prod cert hash. Verify in-app at runtime against a hardcoded value (native preferred)."
        ),

        DumpSection(
            id = "perms_install",
            title = "Install-time permissions",
            subtitle = "Permissions granted at install (normal-level)",
            command = { pkg ->
                "dumpsys package $pkg | sed -n '/install permissions:/,/runtime permissions:/p' | head -40"
            }
        ),

        DumpSection(
            id = "perms_runtime",
            title = "Runtime permissions granted",
            subtitle = "Dangerous + appop perms the user (or ADB) granted",
            command = { pkg ->
                "dumpsys package $pkg | sed -n '/runtime permissions:/,/Hidden system packages:/p' | head -60"
            },
            riskHint = "If WRITE_SECURE_SETTINGS or others were granted via 'pm grant', the user can detect those"
        ),

        DumpSection(
            id = "components",
            title = "Exported components (attack surface)",
            subtitle = "Activities/services/receivers/providers callable from outside",
            command = { pkg ->
                "dumpsys package $pkg | grep -B2 -A2 'exported=true' | head -150"
            },
            riskHint = "Each exported component is a callable surface. Audit for auth bypass, intent-redirect, deep-link injection."
        ),

        DumpSection(
            id = "providers",
            title = "Content providers + authorities",
            subtitle = "Cross-process data exchange surface",
            command = { pkg ->
                "dumpsys package $pkg | grep -E 'authority|grantUriPermissions|ContentProvider' | head -40"
            }
        ),

        DumpSection(
            id = "native",
            title = "Native libraries (.so)",
            subtitle = "Bundled native code — possible JNI targets",
            command = { pkg ->
                "ls -la /data/app/*$pkg*/lib/*/ 2>/dev/null; ls -la /data/app/*$pkg*/lib/ 2>/dev/null"
            },
            riskHint = "RASP/anti-frida usually lives here. Frida hooks these symbols."
        ),

        DumpSection(
            id = "extstor",
            title = "External storage (/sdcard/Android/data)",
            subtitle = "Files the app dropped on scoped external storage — readable to ADB",
            command = { pkg ->
                """
                echo "=== files (first 100) ===" ;
                find /sdcard/Android/data/$pkg -type f 2>/dev/null | head -100 ;
                echo "" ;
                echo "=== total size ===" ;
                du -sh /sdcard/Android/data/$pkg 2>/dev/null ;
                echo "" ;
                echo "=== per-subdir ===" ;
                du -sh /sdcard/Android/data/$pkg/*/ 2>/dev/null
                """.trimIndent().replace("\n", " ")
            },
            riskHint = "Anything written here is readable via 'adb pull'. Never store secrets / PII in scoped external."
        ),

        DumpSection(
            id = "media",
            title = "Media storage (/sdcard/Android/media)",
            subtitle = "Same readable-to-shell story as scoped external",
            command = { pkg ->
                "find /sdcard/Android/media/$pkg -type f 2>/dev/null | head -50; du -sh /sdcard/Android/media/$pkg 2>/dev/null"
            }
        ),

        DumpSection(
            id = "runas",
            title = "Run-as probe (debuggable check)",
            subtitle = "Can shell read /data/data/<pkg>/ directly?",
            command = { pkg ->
                """
                run-as $pkg ls -la /data/data/$pkg/ 2>&1 | head -30 ;
                echo "" ;
                run-as $pkg ls -la /data/data/$pkg/shared_prefs/ 2>&1 | head -30 ;
                echo "" ;
                run-as $pkg ls -la /data/data/$pkg/databases/ 2>&1 | head -30 ;
                echo "" ;
                run-as $pkg ls -la /data/data/$pkg/files/ 2>&1 | head -30
                """.trimIndent().replace("\n", " ")
            },
            riskHint = "If 'run-as: package is not debuggable' appears = good. If it lists files, your prod APK ships debuggable=true (CRITICAL)."
        ),

        DumpSection(
            id = "process",
            title = "Process state",
            subtitle = "PID, UID, memory, cmdline",
            command = { pkg ->
                """
                PID=`pidof $pkg` ;
                if [ -z "${'$'}PID" ]; then echo "(not running)"; exit 0; fi ;
                echo "PID=${'$'}PID" ;
                cat /proc/${'$'}PID/cmdline 2>/dev/null | tr '\0' ' ' ; echo ;
                cat /proc/${'$'}PID/status 2>/dev/null | grep -E 'Name|Pid|Uid|VmRSS|VmSize|Threads' ;
                echo "=== fd count ===" ;
                ls /proc/${'$'}PID/fd 2>/dev/null | wc -l
                """.trimIndent().replace("\n", " ")
            }
        ),

        DumpSection(
            id = "net",
            title = "Open network sockets",
            subtitle = "Live TCP/UDP connections by this app's UID",
            command = { pkg ->
                """
                UID=`dumpsys package $pkg | grep 'userId=' | head -1 | sed 's/.*userId=//' | awk '{print ${'$'}1}'` ;
                if [ -z "${'$'}UID" ]; then echo "uid not found"; exit 0; fi ;
                echo "UID=${'$'}UID" ;
                echo "=== /proc/net/tcp6 ===" ;
                awk -v u=${'$'}UID '${'$'}8 == u' /proc/net/tcp6 | head -20 ;
                echo "=== /proc/net/tcp ===" ;
                awk -v u=${'$'}UID '${'$'}8 == u' /proc/net/tcp | head -20 ;
                echo "=== /proc/net/udp ===" ;
                awk -v u=${'$'}UID '${'$'}8 == u' /proc/net/udp | head -10
                """.trimIndent().replace("\n", " ")
            },
            riskHint = "Each socket = a remote endpoint your app talks to. Cross-reference with your expected API hosts."
        ),

        DumpSection(
            id = "services",
            title = "Running services",
            subtitle = "Foreground/background services the app has up",
            command = { pkg -> "dumpsys activity services $pkg | head -80" }
        ),

        DumpSection(
            id = "scan",
            title = "Sensitive-string scan in shared storage",
            subtitle = "Greps readable files for JWT, Bearer, api_key, secret, token, password",
            command = { pkg ->
                // One logical line. Use -regex instead of -name with parens to
                // avoid shell-escape gymnastics that don't survive newline→space.
                "find /sdcard/Android/data/$pkg /sdcard/Android/media/$pkg -type f -regex '.*\\.\\(txt\\|json\\|xml\\|log\\|prefs?\\|properties\\)$' 2>/dev/null | xargs -I{} grep -lE 'eyJ|Bearer |api_key|apikey|api-key|secret|token=|password=|client_secret|refresh_token' {} 2>/dev/null | head -40"
            },
            riskHint = "Any HIT here = secrets leaking to disk in a place ADB shell can read without run-as."
        ),

        DumpSection(
            id = "logs",
            title = "Logcat hits for this app",
            subtitle = "Last 200 log lines from this PID",
            command = { pkg ->
                """
                PID=`pidof $pkg` ;
                if [ -z "${'$'}PID" ]; then echo "(not running, no live log)"; exit 0; fi ;
                logcat -d --pid=${'$'}PID -t 200
                """.trimIndent().replace("\n", " ")
            },
            riskHint = "If anything sensitive prints here in release, that's recoverable via 'adb logcat'."
        ),
    )
}
