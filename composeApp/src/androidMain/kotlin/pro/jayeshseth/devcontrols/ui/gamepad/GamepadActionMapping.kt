package pro.jayeshseth.devcontrols.ui.gamepad

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.widget.Toast
import pro.jayeshseth.devcontrols.platform.PersistentStorage

data class GamepadAction(val id: String, val label: String)
data class ButtonMapping(val keyCode: Int, val keyLabel: String, val actionId: String)

object GamepadActions {
    val all = listOf(
        GamepadAction("NONE",          "No action"),
        GamepadAction("HAPTIC_CLICK",  "Haptic: Click"),
        GamepadAction("HAPTIC_HEAVY",  "Haptic: Heavy"),
        GamepadAction("HAPTIC_TICK",   "Haptic: Tick"),
        GamepadAction("TOAST",         "Show Toast"),
        GamepadAction("LOG",           "Log to Logcat"),
        GamepadAction("NAVIGATE_BACK", "Navigate Back (stub)"),
    )

    fun execute(actionId: String, context: Context) {
        when (actionId) {
            "HAPTIC_CLICK" -> vibrate(context, 50, 200)
            "HAPTIC_HEAVY" -> vibrate(context, 120, 255)
            "HAPTIC_TICK"  -> vibrate(context, 20, 150)
            "TOAST"        -> Toast.makeText(context, "Button mapped!", Toast.LENGTH_SHORT).show()
            "LOG"          -> android.util.Log.d("GamepadPlayground", "Mapped button fired")
            else           -> {}
        }
    }

    @Suppress("DEPRECATION")
    private fun vibrate(context: Context, durationMs: Long, amplitude: Int) {
        if (Build.VERSION.SDK_INT >= 31) {
            val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vm.defaultVibrator.vibrate(VibrationEffect.createOneShot(durationMs, amplitude))
        } else {
            val v = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            if (Build.VERSION.SDK_INT >= 26) {
                v.vibrate(VibrationEffect.createOneShot(durationMs, amplitude))
            } else {
                v.vibrate(durationMs)
            }
        }
    }
}

object GamepadMappingStore {
    private const val KEY = "gamepad_mappings_v1"

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
