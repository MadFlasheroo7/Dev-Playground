package pro.jayeshseth.devcontrols.platform

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder

actual class AudioPlayer actual constructor(sampleRate: Int, channels: Int) {
    private val track: AudioTrack

    init {
        val channelMask = if (channels == 2) AudioFormat.CHANNEL_OUT_STEREO else AudioFormat.CHANNEL_OUT_MONO
        val minBuf = AudioTrack.getMinBufferSize(
            sampleRate,
            channelMask,
            AudioFormat.ENCODING_PCM_16BIT
        )
        track = AudioTrack(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build(),
            AudioFormat.Builder()
                .setSampleRate(sampleRate)
                .setChannelMask(channelMask)
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .build(),
            maxOf(minBuf, 2048),
            AudioTrack.MODE_STREAM,
            AudioManager.AUDIO_SESSION_ID_GENERATE
        )
    }


    actual fun start() {
        track.play()
    }

    actual fun write(buffer: ShortArray, offset: Int, size: Int) {
        track.write(buffer, offset, size)
    }

    actual fun stop() {
        runCatching { track.stop() }
    }

    actual fun release() {
        runCatching { track.release() }
    }
}

actual class AudioRecorder actual constructor(sampleRate: Int, bufferSize: Int) {
    private val record: AudioRecord?

    actual val isInitialized: Boolean
        get() = record != null && record.state == AudioRecord.STATE_INITIALIZED

    init {
        val minBuf = AudioRecord.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        record = runCatching {
            AudioRecord(
                MediaRecorder.AudioSource.MIC,
                sampleRate,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                maxOf(minBuf, bufferSize * 2)
            )
        }.getOrNull()
    }

    actual fun start() {
        record?.startRecording()
    }

    actual fun read(buffer: ShortArray, offset: Int, size: Int): Int {
        return record?.read(buffer, offset, size) ?: -1
    }

    actual fun stop() {
        runCatching { record?.stop() }
    }

    actual fun release() {
        runCatching { record?.release() }
    }
}
