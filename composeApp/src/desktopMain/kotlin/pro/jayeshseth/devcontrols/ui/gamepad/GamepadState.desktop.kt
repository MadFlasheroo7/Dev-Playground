package pro.jayeshseth.devcontrols.ui.gamepad

enum class ConnectionType { NONE, BLUETOOTH, USB_DONGLE, USB_WIRED }

data class GamepadState(
    val deviceId: Int = -1,
    val deviceName: String = "Keyboard Controller (Desktop)",
    val isConnected: Boolean = true,
    val connectionType: ConnectionType = ConnectionType.USB_WIRED,
    val vendorId: Int = 0x1234,
    val productId: Int = 0x5678,

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
