package pro.jayeshseth.devcontrols.platform

/**
 * Platform-agnostic streaming audio output player.
 * Writes 16-bit PCM samples to the device audio output (mono or stereo).
 */
expect class AudioPlayer(sampleRate: Int = 44100, channels: Int = 1) {

    fun start()
    fun write(buffer: ShortArray, offset: Int, size: Int)
    fun stop()
    fun release()
}

/**
 * Platform-agnostic streaming audio input recorder.
 * Reads 16-bit mono PCM samples from the microphone input.
 */
expect class AudioRecorder(sampleRate: Int = 44100, bufferSize: Int = 2048) {
    val isInitialized: Boolean
    fun start()
    fun read(buffer: ShortArray, offset: Int, size: Int): Int
    fun stop()
    fun release()
}
