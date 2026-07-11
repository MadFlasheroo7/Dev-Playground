package pro.jayeshseth.devcontrols.ui.gamepad

import pro.jayeshseth.devcontrols.platform.PersistentStorage
import java.awt.Toolkit

data class GamepadAction(val id: String, val label: String)
data class ButtonMapping(val keyCode: Int, val keyLabel: String, val actionId: String)

object GamepadActions {
    val all = listOf(
        GamepadAction("NONE",             "No action"),
        GamepadAction("SOUND_BEEP",       "Audio: Beep"),
        GamepadAction("MEDIA_PLAY_PAUSE", "Media: Play / Pause"),
        GamepadAction("MEDIA_NEXT",       "Media: Next Track"),
        GamepadAction("MEDIA_PREV",       "Media: Previous Track"),
        GamepadAction("VOLUME_UP",        "Volume: Increase"),
        GamepadAction("VOLUME_DOWN",      "Volume: Decrease"),
        GamepadAction("VOLUME_MUTE",      "Volume: Mute / Unmute"),
        GamepadAction("LOG",              "Log to Console"),
    )

    fun execute(actionId: String) {
        when (actionId) {
            "SOUND_BEEP" -> {
                runCatching {
                    Toolkit.getDefaultToolkit().beep()
                }
            }
            "MEDIA_PLAY_PAUSE" -> runCommand("playerctl play-pause")
            "MEDIA_NEXT"       -> runCommand("playerctl next")
            "MEDIA_PREV"       -> runCommand("playerctl previous")
            "VOLUME_UP"        -> {
                if (!runCommand("pactl set-sink-volume @DEFAULT_SINK@ +5%")) {
                    runCommand("amixer set Master 5%+")
                }
            }
            "VOLUME_DOWN"      -> {
                if (!runCommand("pactl set-sink-volume @DEFAULT_SINK@ -5%")) {
                    runCommand("amixer set Master 5%-")
                }
            }
            "VOLUME_MUTE"      -> {
                if (!runCommand("pactl set-sink-mute @DEFAULT_SINK@ toggle")) {
                    runCommand("amixer set Master toggle")
                }
            }
            "LOG" -> {
                println("[GamepadPlayground] Mapped button fired: $actionId")
            }
            else -> {}
        }
    }

    private fun runCommand(cmd: String): Boolean {
        return try {
            val process = Runtime.getRuntime().exec(cmd.split(" ").toTypedArray())
            process.waitFor() == 0
        } catch (e: Exception) {
            false
        }
    }
}

object GamepadMappingStore {
    private const val KEY = "gamepad_mappings_desktop_v1"

    fun load(): List<ButtonMapping> {
        val raw = PersistentStorage.getString(KEY, "")
        if (raw.isEmpty()) return emptyList()
        return raw.split("|").mapNotNull { entry ->
            val parts = entry.split(":")
            if (parts.size == 3) ButtonMapping(
                keyCode  = parts[0].toIntOrNull() ?: return@mapNotNull null,
                keyLabel = parts[1],
                actionId = parts[2],
            ) else null
        }
    }

    fun save(mappings: List<ButtonMapping>) {
        val raw = mappings.joinToString("|") { "${it.keyCode}:${it.keyLabel}:${it.actionId}" }
        PersistentStorage.saveString(KEY, raw)
    }

    fun upsert(mapping: ButtonMapping) {
        val current = load().toMutableList()
        val idx = current.indexOfFirst { it.keyCode == mapping.keyCode }
        if (idx >= 0) current[idx] = mapping else current.add(mapping)
        save(current)
    }

    fun remove(keyCode: Int) {
        save(load().filter { it.keyCode != keyCode })
    }
}
