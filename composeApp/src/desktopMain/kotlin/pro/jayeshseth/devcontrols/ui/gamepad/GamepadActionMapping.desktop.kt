package pro.jayeshseth.devcontrols.ui.gamepad

import pro.jayeshseth.devcontrols.platform.PersistentStorage
import java.awt.Toolkit

data class GamepadAction(val id: String, val label: String)
data class ButtonMapping(val keyCode: Int, val keyLabel: String, val actionId: String)

object GamepadActions {
    val all = listOf(
        GamepadAction("NONE",          "No action"),
        GamepadAction("SOUND_BEEP",    "Audio: Beep"),
        GamepadAction("LOG",           "Log to Console"),
        GamepadAction("NAVIGATE_BACK", "Navigate Back (stub)"),
    )

    fun execute(actionId: String) {
        when (actionId) {
            "SOUND_BEEP" -> {
                runCatching {
                    Toolkit.getDefaultToolkit().beep()
                }
            }
            "LOG" -> {
                println("[GamepadPlayground] Mapped button fired: $actionId")
            }
            else -> {}
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
