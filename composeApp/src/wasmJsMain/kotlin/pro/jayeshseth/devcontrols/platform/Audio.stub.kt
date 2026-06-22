package pro.jayeshseth.devcontrols.platform

actual class AudioPlayer actual constructor(sampleRate: Int, channels: Int) {

    actual fun start() {}
    actual fun write(buffer: ShortArray, offset: Int, size: Int) {}
    actual fun stop() {}
    actual fun release() {}
}

actual class AudioRecorder actual constructor(sampleRate: Int, bufferSize: Int) {
    actual val isInitialized: Boolean = false
    actual fun start() {}
    actual fun read(buffer: ShortArray, offset: Int, size: Int): Int = 0
    actual fun stop() {}
    actual fun release() {}
}
