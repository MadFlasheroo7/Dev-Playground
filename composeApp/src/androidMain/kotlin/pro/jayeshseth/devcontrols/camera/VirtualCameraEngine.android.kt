package pro.jayeshseth.devcontrols.camera

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.ImageBitmap

class AndroidVirtualCameraEngine : VirtualCameraEngine {
    private val _settings = mutableStateOf(VirtualCameraSettings())
    override val settings: State<VirtualCameraSettings> = _settings

    private val _status = mutableStateOf(VirtualCameraStatus(isCameraActive = true, activeDeviceName = "Android CameraX"))
    override val status: State<VirtualCameraStatus> = _status

    private val _processedFrame = mutableStateOf<ImageBitmap?>(null)
    override val processedFrame: State<ImageBitmap?> = _processedFrame

    private val _rawFrame = mutableStateOf<ImageBitmap?>(null)
    override val rawFrame: State<ImageBitmap?> = _rawFrame

    override fun updateSettings(settings: VirtualCameraSettings) {
        _settings.value = settings
    }

    override fun applyPreset(preset: CameraPreset) {
        val newSettings = when (preset) {
            CameraPreset.Default -> VirtualCameraSettings()
            CameraPreset.ProStudio -> VirtualCameraSettings(
                sharpness = 0.8f,
                contrast = 1.25f,
                brightness = 0.05f,
                denoise = 0.15f,
                claheAmount = 0.4f,
                colorTemperature = 0.2f
            )
            CameraPreset.BacklightFix -> VirtualCameraSettings(
                sharpness = 0.4f,
                contrast = 1.4f,
                brightness = 0.2f,
                claheAmount = 0.9f,
                colorTemperature = 0.1f
            )
            CameraPreset.LowLightDenoise -> VirtualCameraSettings(
                sharpness = 0.3f,
                contrast = 1.1f,
                brightness = 0.25f,
                denoise = 0.8f,
                claheAmount = 0.5f
            )
            CameraPreset.CinematicBokeh -> VirtualCameraSettings(
                sharpness = 0.6f,
                contrast = 1.2f,
                colorTemperature = 0.3f,
                backgroundBlur = 0.7f
            )
        }
        _settings.value = newSettings
    }

    override fun toggleVirtualCam(enabled: Boolean) {
        _settings.value = _settings.value.copy(isVirtualCamEnabled = enabled)
        _status.value = _status.value.copy(
            isVirtualCamActive = enabled,
            errorMessage = if (enabled) "Virtual camera output loopback is available on Linux desktop targets." else null
        )
    }

    override fun startCamera() {
        _status.value = _status.value.copy(isCameraActive = true)
    }

    override fun stopCamera() {
        _status.value = _status.value.copy(isCameraActive = false, isVirtualCamActive = false)
    }

    override fun selectCamera(index: Int) {
        _settings.value = _settings.value.copy(selectedCameraIndex = index)
    }
}

@Composable
actual fun rememberVirtualCameraEngine(): VirtualCameraEngine {
    val engine = remember { AndroidVirtualCameraEngine() }
    DisposableEffect(Unit) {
        onDispose { engine.stopCamera() }
    }
    return engine
}
