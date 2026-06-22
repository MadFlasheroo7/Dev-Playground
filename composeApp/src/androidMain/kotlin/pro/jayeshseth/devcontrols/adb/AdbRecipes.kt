package pro.jayeshseth.devcontrols.adb

/**
 * Pre-canned shell commands. Each recipe builds a command string. Some need
 * extra UI input (a target package, a permission name) so they return null
 * from [build] until the user supplies it via [requiresInput].
 */
data class AdbRecipe(
    val id: String,
    val title: String,
    val subtitle: String,
    val build: () -> String,
    val confirm: String? = null,
)

object AdbRecipes {

    val all: List<AdbRecipe> = listOf(
        AdbRecipe(
            id = "screencap",
            title = "Take screenshot",
            subtitle = "Saved to /sdcard/Pictures",
            build = {
                val ts = System.currentTimeMillis()
                "mkdir -p /sdcard/Pictures && screencap -p /sdcard/Pictures/devcontrols_$ts.png && echo /sdcard/Pictures/devcontrols_$ts.png"
            }
        ),
        AdbRecipe(
            id = "screenrecord_30",
            title = "Screen record 30s",
            subtitle = "Saved to /sdcard/Movies",
            build = {
                val ts = System.currentTimeMillis()
                "mkdir -p /sdcard/Movies && screenrecord --time-limit 30 /sdcard/Movies/devcontrols_$ts.mp4 && echo /sdcard/Movies/devcontrols_$ts.mp4"
            }
        ),
        AdbRecipe(
            id = "list_user_apps",
            title = "List user apps",
            subtitle = "pm list packages -3",
            build = { "pm list packages -3" }
        ),
        AdbRecipe(
            id = "foreground",
            title = "Foreground window",
            subtitle = "dumpsys window | grep mCurrentFocus",
            build = { "dumpsys window | grep mCurrentFocus" }
        ),
        AdbRecipe(
            id = "logcat_200",
            title = "Logcat snapshot",
            subtitle = "Last 200 lines",
            build = { "logcat -d -t 200" }
        ),
        AdbRecipe(
            id = "top_activity",
            title = "Top activity",
            subtitle = "dumpsys activity top | head -200",
            build = { "dumpsys activity top | head -200" }
        ),
        AdbRecipe(
            id = "airplane_on",
            title = "Airplane mode ON",
            subtitle = "cmd connectivity airplane-mode enable",
            build = { "cmd connectivity airplane-mode enable" }
        ),
        AdbRecipe(
            id = "airplane_off",
            title = "Airplane mode OFF",
            subtitle = "cmd connectivity airplane-mode disable",
            build = { "cmd connectivity airplane-mode disable" }
        ),
        AdbRecipe(
            id = "key_home",
            title = "Send HOME key",
            subtitle = "input keyevent HOME",
            build = { "input keyevent HOME" }
        ),
        AdbRecipe(
            id = "key_back",
            title = "Send BACK key",
            subtitle = "input keyevent BACK",
            build = { "input keyevent BACK" }
        ),
        AdbRecipe(
            id = "systemui_restart",
            title = "Restart SystemUI",
            subtitle = "pkill -TERM com.android.systemui",
            build = { "pkill -TERM com.android.systemui" },
            confirm = "This will momentarily blank the status bar and nav buttons. Continue?"
        ),
        AdbRecipe(
            id = "reboot",
            title = "Reboot device",
            subtitle = "svc power reboot",
            build = { "svc power reboot" },
            confirm = "Reboot the phone right now?"
        ),
        AdbRecipe(
            id = "grant_self_perms",
            title = "Grant our own perms",
            subtitle = "Self-grant common ADB perms",
            build = {
                """
                pm grant pro.jayeshseth.devcontrols android.permission.WRITE_SECURE_SETTINGS &&
                pm grant pro.jayeshseth.devcontrols android.permission.READ_PHONE_STATE &&
                pm grant pro.jayeshseth.devcontrols android.permission.ANSWER_PHONE_CALLS &&
                pm grant pro.jayeshseth.devcontrols android.permission.WATCH_APPOPS &&
                appops set pro.jayeshseth.devcontrols WRITE_SETTINGS allow &&
                appops set pro.jayeshseth.devcontrols GET_USAGE_STATS allow &&
                appops set pro.jayeshseth.devcontrols ACCESS_RESTRICTED_SETTINGS allow &&
                echo OK
                """.trimIndent().replace("\n", " ")
            }
        ),
        AdbRecipe(
            id = "model",
            title = "Device model",
            subtitle = "getprop ro.product.model",
            build = { "getprop ro.product.model" }
        ),
    )
}
