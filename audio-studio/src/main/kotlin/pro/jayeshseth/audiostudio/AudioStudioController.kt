package pro.jayeshseth.audiostudio

import android.content.Context
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class AudioMeters(
    val inPeak: Float = 0f,
    val inRms: Float = 0f,
    val outPeak: Float = 0f,
    val outRms: Float = 0f
)

data class AudioStudioSettings(
    val boostDb: Float = 6f,        // 0 to 24 dB
    val toneTilt: Float = 0f,       // -1.0 (warm bass) to +1.0 (bright crisp)
    val compIntensity: Float = 0.5f,// 0.0 (off) to 1.0 (heavy studio crush)
    val reverbDepth: Float = 0.2f,  // 0.0 (dry) to 1.0 (wet space)
    val liveMonitor: Boolean = true,
    val speakerSafeMode: Boolean = true
)

class AudioStudioController(private val context: Context) {
    private val native = NativeAudioEngine()
    private var enginePtr: Long = 0L

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var meterJob: Job? = null

    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

    private val _isHeadphonesConnected = MutableStateFlow(false)
    val isHeadphonesConnected: StateFlow<Boolean> = _isHeadphonesConnected.asStateFlow()

    private val _settings = MutableStateFlow(AudioStudioSettings())
    val settings: StateFlow<AudioStudioSettings> = _settings.asStateFlow()

    private val _meters = MutableStateFlow(AudioMeters())
    val meters: StateFlow<AudioMeters> = _meters.asStateFlow()

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager

    private val deviceCallback = object : AudioDeviceCallback() {
        override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>?) {
            checkAudioRouting()
        }

        override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>?) {
            checkAudioRouting()
        }
    }

    init {
        enginePtr = native.nativeCreate()
        audioManager?.registerAudioDeviceCallback(deviceCallback, null)
        checkAudioRouting()
        syncParamsToEngine()
    }

    private fun checkAudioRouting() {
        val manager = audioManager ?: return
        val devices = manager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
        val hasHeadphones = devices.any { device ->
            device.type == AudioDeviceInfo.TYPE_WIRED_HEADSET ||
            device.type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES ||
            device.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP ||
            device.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO ||
            device.type == AudioDeviceInfo.TYPE_USB_HEADSET ||
            device.type == AudioDeviceInfo.TYPE_USB_DEVICE
        }
        _isHeadphonesConnected.value = hasHeadphones
    }

    fun start(): Boolean {
        if (enginePtr == 0L) return false
        val useSpeakerAec = !_isHeadphonesConnected.value && _settings.value.speakerSafeMode
        val success = native.nativeStart(enginePtr, useSpeakerAec)
        if (success) {
            _isRunning.value = true
            startMeterPolling()
        }
        return success
    }

    fun stop() {
        if (enginePtr == 0L) return
        meterJob?.cancel()
        meterJob = null
        native.nativeStop(enginePtr)
        _isRunning.value = false
        _meters.value = AudioMeters()
    }

    fun updateSettings(transform: (AudioStudioSettings) -> AudioStudioSettings) {
        _settings.update(transform)
        syncParamsToEngine()
    }

    private fun syncParamsToEngine() {
        if (enginePtr == 0L) return
        val s = _settings.value

        // Map Tone Tilt (-1.0 to 1.0) to Low/Mid/High biquad gains in dB
        val bassDb = -s.toneTilt * 6f
        val midDb = 0f
        val trebleDb = s.toneTilt * 6f

        // Map Compressor Intensity (0.0 to 1.0) to Threshold, Ratio, and Makeup
        val compThresh = -10f - (s.compIntensity * 20f) // -10 dB down to -30 dB
        val compRatio = 2f + (s.compIntensity * 6f)     // 2:1 up to 8:1
        val compMakeup = s.compIntensity * 6f           // 0 dB up to +6 dB

        // Map Reverb Depth
        val reverbWet = s.reverbDepth * 0.7f
        val reverbSize = 0.3f + (s.reverbDepth * 0.5f)

        val useSpeakerAec = !_isHeadphonesConnected.value && s.speakerSafeMode

        native.nativeUpdateParams(
            enginePtr = enginePtr,
            boostDb = s.boostDb,
            toneLowDb = bassDb,
            toneMidDb = midDb,
            toneHighDb = trebleDb,
            compThreshDb = compThresh,
            compRatio = compRatio,
            compMakeupDb = compMakeup,
            reverbSize = reverbSize,
            reverbWet = reverbWet,
            liveMonitor = s.liveMonitor,
            speakerAec = useSpeakerAec
        )
    }

    private fun startMeterPolling() {
        meterJob?.cancel()
        meterJob = scope.launch {
            while (isActive && _isRunning.value) {
                if (enginePtr != 0L) {
                    val inPeak = native.nativeGetInputPeak(enginePtr)
                    val inRms = native.nativeGetInputRms(enginePtr)
                    val outPeak = native.nativeGetOutputPeak(enginePtr)
                    val outRms = native.nativeGetOutputRms(enginePtr)
                    _meters.value = AudioMeters(inPeak, inRms, outPeak, outRms)
                }
                delay(16) // ~60 FPS update rate
            }
        }
    }

    fun release() {
        stop()
        audioManager?.unregisterAudioDeviceCallback(deviceCallback)
        if (enginePtr != 0L) {
            native.nativeDestroy(enginePtr)
            enginePtr = 0L
        }
    }
}
