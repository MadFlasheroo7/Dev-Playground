package pro.jayeshseth.devcontrols.camera

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.State
import androidx.compose.ui.graphics.ImageBitmap

@Immutable
data class VirtualCameraSettings(
    val sharpness: Float = 0.5f,        // 0.0f .. 2.0f (Unsharp mask intensity)
    val contrast: Float = 1.0f,         // 0.5f .. 2.0f
    val brightness: Float = 0.0f,       // -0.5f .. 0.5f
    val denoise: Float = 0.2f,          // 0.0f .. 1.0f
    val claheAmount: Float = 0.3f,      // 0.0f .. 1.0f (Backlight / HDR balance)
    val colorTemperature: Float = 0.0f, // -1.0f (Cool) .. 1.0f (Warm)
    val backgroundBlur: Float = 0.0f,   // 0.0f .. 1.0f (Bokeh blur)
    val isVirtualCamEnabled: Boolean = false,
    val selectedCameraIndex: Int = 0,
    val isCompareMode: Boolean = false
)

@Immutable
data class VirtualCameraStatus(
    val isCameraActive: Boolean = false,
    val isVirtualCamActive: Boolean = false,
    val activeDeviceName: String = "Default Camera",
    val activeResolution: String = "1920x1080",
    val fps: Int = 0,
    val virtualDevicePath: String = "/dev/video20",
    val errorMessage: String? = null
)

enum class CameraPreset {
    Default,
    ProStudio,
    BacklightFix,
    LowLightDenoise,
    CinematicBokeh
}

interface VirtualCameraEngine {
    val settings: State<VirtualCameraSettings>
    val status: State<VirtualCameraStatus>
    val processedFrame: State<ImageBitmap?>
    val rawFrame: State<ImageBitmap?>

    fun updateSettings(settings: VirtualCameraSettings)
    fun applyPreset(preset: CameraPreset)
    fun toggleVirtualCam(enabled: Boolean)
    fun startCamera()
    fun stopCamera()
    fun selectCamera(index: Int)
}

@Composable
expect fun rememberVirtualCameraEngine(): VirtualCameraEngine
