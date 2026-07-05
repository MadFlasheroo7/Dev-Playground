package pro.jayeshseth.devcontrols.ui.gamepad

/**
 * Activity-level bridge so [MainActivity.dispatchKeyEvent] and
 * [MainActivity.dispatchGenericMotionEvent] can forward events to whichever
 * [GamepadManager] is currently active (i.e. the Gamepad Playground screen is open).
 *
 * The screen calls [register] on enter and [unregister] on dispose.
 */
object GamepadManagerHolder {
    @Volatile
    var active: GamepadManager? = null

    fun register(manager: GamepadManager) {
        active = manager
    }

    fun unregister() {
        active = null
    }
}
