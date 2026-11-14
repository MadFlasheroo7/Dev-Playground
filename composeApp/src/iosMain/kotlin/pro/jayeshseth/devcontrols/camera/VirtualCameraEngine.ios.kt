package pro.jayeshseth.devcontrols.camera

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.ImageBitmap

class IosVirtualCameraEngine : VirtualCameraEngine {
    private val _settings = mutableStateOf(VirtualCameraSettings())
    override val settings: State<VirtualCameraSettings> = _settings

    private val _status = mutableStateOf(VirtualCameraStatus(isCameraActive = true, activeDeviceName = "iOS Camera"))
    override val status: State<VirtualCameraStatus> = _status

    private val _processedFrame = mutableStateOf<ImageBitmap?>(null)
    override val processedFrame: State<ImageBitmap?> = _processedFrame

    private val _rawFrame = mutableStateOf<ImageBitmap?>(null)
    override val rawFrame: State<ImageBitmap?> = _rawFrame

    override fun updateSettings(settings: VirtualCameraSettings) {
        _settings.value = settings
    }

    override fun applyPreset(preset: CameraPreset) {}

    override fun toggleVirtualCam(enabled: Boolean) {
        _settings.value = _settings.value.copy(isVirtualCamEnabled = enabled)
    }

    override fun startCamera() {
        _status.value = _status.value.copy(isCameraActive = true)
    }

    override fun stopCamera() {
        _status.value = _status.value.copy(isCameraActive = false)
    }

    override fun selectCamera(index: Int) {}
}

@Composable
actual fun rememberVirtualCameraEngine(): VirtualCameraEngine {
    return remember { IosVirtualCameraEngine() }
}
