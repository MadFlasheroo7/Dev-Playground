package pro.jayeshseth.devcontrols.ui.gamepad

import android.hardware.input.InputManager
import android.os.Build
import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.abs

class GamepadManager(
    private val inputManager: InputManager
) : InputManager.InputDeviceListener {

    private val _state = MutableStateFlow(GamepadState())
    val state: StateFlow<GamepadState> = _state.asStateFlow()

    /** Emits (keyCode, isDown) for every key event — used for macro detection and action dispatch. */
    private val _rawKeys = MutableStateFlow<Pair<Int, Boolean>?>(null)
    val rawKeys: StateFlow<Pair<Int, Boolean>?> = _rawKeys.asStateFlow()

    init {
        inputManager.registerInputDeviceListener(this, null)
        detectConnectedGamepad()
    }

    fun dispose() {
        inputManager.unregisterInputDeviceListener(this)
    }

    // ── InputDeviceListener ────────────────────────────────────────────────

    override fun onInputDeviceAdded(deviceId: Int) { detectConnectedGamepad() }
    override fun onInputDeviceRemoved(deviceId: Int) {
        if (_state.value.deviceId == deviceId) _state.value = GamepadState()
    }
    override fun onInputDeviceChanged(deviceId: Int) { detectConnectedGamepad() }

    private fun detectConnectedGamepad() {
        val ids = InputDevice.getDeviceIds()
        for (id in ids) {
            val dev = InputDevice.getDevice(id) ?: continue
            val sources = dev.sources
            val isGamepad  = (sources and InputDevice.SOURCE_GAMEPAD)  == InputDevice.SOURCE_GAMEPAD
            val isJoystick = (sources and InputDevice.SOURCE_JOYSTICK) == InputDevice.SOURCE_JOYSTICK
            if ((isGamepad || isJoystick) && !dev.isVirtual) {
                _state.value = _state.value.copy(
                    deviceId       = id,
                    deviceName     = dev.name,
                    isConnected    = true,
                    connectionType = resolveConnectionType(dev),
                    vendorId       = dev.vendorId,
                    productId      = dev.productId,
                )
                return
            }
        }
        // No gamepad found — reset
        _state.value = GamepadState()
    }

    private fun resolveConnectionType(dev: InputDevice): ConnectionType {
        // Try API 31+ bluetoothAddress via reflection to avoid hard compile dependency
        if (Build.VERSION.SDK_INT >= 31) {
            runCatching {
                val addr = InputDevice::class.java.getMethod("getBluetoothAddress").invoke(dev) as? String
                if (!addr.isNullOrEmpty()) return ConnectionType.BLUETOOTH
            }
        }
        val name = dev.name.lowercase()
        return when {
            name.contains("bluetooth") || name.contains(" bt ") || name.contains("-bt") ->
                ConnectionType.BLUETOOTH
            name.contains("dongle") || name.contains("receiver") || name.contains("wireless") ->
                ConnectionType.USB_DONGLE
            else -> ConnectionType.USB_WIRED
        }
    }

    // ── Event dispatch — call from Activity overrides ─────────────────────

    /** Hook into Activity.dispatchKeyEvent — returns true if event was consumed. */
    fun onKeyEvent(event: KeyEvent): Boolean {
        val dev = InputDevice.getDevice(event.deviceId) ?: return false
        val sources = dev.sources
        val isGamepad  = (sources and InputDevice.SOURCE_GAMEPAD)  == InputDevice.SOURCE_GAMEPAD
        val isJoystick = (sources and InputDevice.SOURCE_JOYSTICK) == InputDevice.SOURCE_JOYSTICK
        if (!isGamepad && !isJoystick) return false

        val down = event.action == KeyEvent.ACTION_DOWN
        _rawKeys.value = event.keyCode to down
        _state.value = applyKey(_state.value, event.keyCode, down)
        return true
    }

    /** Hook into Activity.dispatchGenericMotionEvent — returns true if event was consumed. */
    fun onMotionEvent(event: MotionEvent): Boolean {
        val dev = InputDevice.getDevice(event.deviceId) ?: return false
        if ((dev.sources and InputDevice.SOURCE_JOYSTICK) != InputDevice.SOURCE_JOYSTICK) return false

        val hatX = event.getAxisValue(MotionEvent.AXIS_HAT_X)
        val hatY = event.getAxisValue(MotionEvent.AXIS_HAT_Y)

        _state.value = _state.value.copy(
            leftStickX  = getCenteredAxis(event, dev, MotionEvent.AXIS_X),
            leftStickY  = getCenteredAxis(event, dev, MotionEvent.AXIS_Y),
            rightStickX = getCenteredAxis(event, dev, MotionEvent.AXIS_Z),
            rightStickY = getCenteredAxis(event, dev, MotionEvent.AXIS_RZ),
            l2Axis      = event.getAxisValue(MotionEvent.AXIS_LTRIGGER).coerceIn(0f, 1f),
            r2Axis      = event.getAxisValue(MotionEvent.AXIS_RTRIGGER).coerceIn(0f, 1f),
            hatX        = hatX,
            hatY        = hatY,
            // D-pad via HAT axis (common on most controllers)
            dpadLeft    = hatX < -0.5f,
            dpadRight   = hatX >  0.5f,
            dpadUp      = hatY < -0.5f,
            dpadDown    = hatY >  0.5f,
        )
        return true
    }

    private fun getCenteredAxis(event: MotionEvent, dev: InputDevice, axis: Int): Float {
        val range = dev.getMotionRange(axis, event.source) ?: return 0f
        val value = event.getAxisValue(axis)
        return if (abs(value) > range.flat) value else 0f
    }

    private fun applyKey(s: GamepadState, keyCode: Int, down: Boolean): GamepadState = when (keyCode) {
        KeyEvent.KEYCODE_BUTTON_A       -> s.copy(buttonA      = down)
        KeyEvent.KEYCODE_BUTTON_B       -> s.copy(buttonB      = down)
        KeyEvent.KEYCODE_BUTTON_X       -> s.copy(buttonX      = down)
        KeyEvent.KEYCODE_BUTTON_Y       -> s.copy(buttonY      = down)
        KeyEvent.KEYCODE_BUTTON_L1      -> s.copy(buttonL1     = down)
        KeyEvent.KEYCODE_BUTTON_R1      -> s.copy(buttonR1     = down)
        KeyEvent.KEYCODE_BUTTON_L2      -> s.copy(buttonL2     = down)
        KeyEvent.KEYCODE_BUTTON_R2      -> s.copy(buttonR2     = down)
        KeyEvent.KEYCODE_BUTTON_THUMBL  -> s.copy(buttonL3     = down)
        KeyEvent.KEYCODE_BUTTON_THUMBR  -> s.copy(buttonR3     = down)
        KeyEvent.KEYCODE_BUTTON_START   -> s.copy(buttonStart  = down)
        KeyEvent.KEYCODE_BUTTON_SELECT  -> s.copy(buttonSelect = down)
        KeyEvent.KEYCODE_BUTTON_MODE    -> s.copy(buttonMode   = down)
        KeyEvent.KEYCODE_DPAD_UP        -> s.copy(dpadUp       = down)
        KeyEvent.KEYCODE_DPAD_DOWN      -> s.copy(dpadDown     = down)
        KeyEvent.KEYCODE_DPAD_LEFT      -> s.copy(dpadLeft     = down)
        KeyEvent.KEYCODE_DPAD_RIGHT     -> s.copy(dpadRight    = down)
        else -> {
            val extras = s.extraButtons.toMutableMap()
            if (down) extras[keyCode] = true else extras.remove(keyCode)
            s.copy(extraButtons = extras)
        }
    }
}
