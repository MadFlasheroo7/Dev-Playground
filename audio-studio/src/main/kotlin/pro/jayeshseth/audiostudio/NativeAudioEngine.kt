package pro.jayeshseth.audiostudio

internal class NativeAudioEngine {
    companion object {
        init {
            System.loadLibrary("audio_studio_engine")
        }
    }

    external fun nativeCreate(): Long
    external fun nativeDestroy(enginePtr: Long)
    external fun nativeStart(enginePtr: Long, speakerAec: Boolean): Boolean
    external fun nativeStop(enginePtr: Long)
    external fun nativeIsRunning(enginePtr: Long): Boolean
    external fun nativeUpdateParams(
        enginePtr: Long,
        boostDb: Float,
        toneLowDb: Float,
        toneMidDb: Float,
        toneHighDb: Float,
        compThreshDb: Float,
        compRatio: Float,
        compMakeupDb: Float,
        reverbSize: Float,
        reverbWet: Float,
        liveMonitor: Boolean,
        speakerAec: Boolean
    )
    external fun nativeGetInputPeak(enginePtr: Long): Float
    external fun nativeGetInputRms(enginePtr: Long): Float
    external fun nativeGetOutputPeak(enginePtr: Long): Float
    external fun nativeGetOutputRms(enginePtr: Long): Float
}
