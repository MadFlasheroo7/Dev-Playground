package pro.jayeshseth.devcontrols.ui.gamepad

enum class ConnectionType { NONE, BLUETOOTH, USB_DONGLE, USB_WIRED }

data class GamepadState(
    val deviceId: Int = -1,
    val deviceName: String = "No controller detected",
    val isConnected: Boolean = false,
    val connectionType: ConnectionType = ConnectionType.NONE,
    val vendorId: Int = 0,
    val productId: Int = 0,

    // Face buttons
    val buttonA: Boolean = false,
    val buttonB: Boolean = false,
    val buttonX: Boolean = false,
    val buttonY: Boolean = false,

    // Shoulder / trigger buttons
    val buttonL1: Boolean = false,
    val buttonR1: Boolean = false,
    val buttonL2: Boolean = false,
    val buttonR2: Boolean = false,

    // Stick clicks
    val buttonL3: Boolean = false,
    val buttonR3: Boolean = false,

    // Center buttons
    val buttonStart: Boolean = false,
    val buttonSelect: Boolean = false,
    val buttonMode: Boolean = false,

    // D-pad buttons
    val dpadUp: Boolean = false,
    val dpadDown: Boolean = false,
    val dpadLeft: Boolean = false,
    val dpadRight: Boolean = false,

    // Analog axes — range [-1..1] for sticks, [0..1] for triggers
    val leftStickX: Float = 0f,
    val leftStickY: Float = 0f,
    val rightStickX: Float = 0f,
    val rightStickY: Float = 0f,
    val l2Axis: Float = 0f,
    val r2Axis: Float = 0f,
    val hatX: Float = 0f,
    val hatY: Float = 0f,

    // Extra / macro keycodes: keyCode -> isPressed
    val extraButtons: Map<Int, Boolean> = emptyMap(),
)

/** Keycodes considered "standard" — everything else goes to extraButtons */
val standardKeyCodes: Set<Int> = setOf(
    android.view.KeyEvent.KEYCODE_BUTTON_A,
    android.view.KeyEvent.KEYCODE_BUTTON_B,
    android.view.KeyEvent.KEYCODE_BUTTON_X,
    android.view.KeyEvent.KEYCODE_BUTTON_Y,
    android.view.KeyEvent.KEYCODE_BUTTON_L1,
    android.view.KeyEvent.KEYCODE_BUTTON_R1,
    android.view.KeyEvent.KEYCODE_BUTTON_L2,
    android.view.KeyEvent.KEYCODE_BUTTON_R2,
    android.view.KeyEvent.KEYCODE_BUTTON_THUMBL,
    android.view.KeyEvent.KEYCODE_BUTTON_THUMBR,
    android.view.KeyEvent.KEYCODE_BUTTON_START,
    android.view.KeyEvent.KEYCODE_BUTTON_SELECT,
    android.view.KeyEvent.KEYCODE_BUTTON_MODE,
    android.view.KeyEvent.KEYCODE_DPAD_UP,
    android.view.KeyEvent.KEYCODE_DPAD_DOWN,
    android.view.KeyEvent.KEYCODE_DPAD_LEFT,
    android.view.KeyEvent.KEYCODE_DPAD_RIGHT,
)
