package pro.jayeshseth.devcontrols.platform

import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.DataLine
import javax.sound.sampled.SourceDataLine
import javax.sound.sampled.TargetDataLine
import java.nio.ByteBuffer
import java.nio.ByteOrder

actual class AudioPlayer actual constructor(sampleRate: Int, channels: Int) {
    private var line: SourceDataLine? = null
    private val format = AudioFormat(sampleRate.toFloat(), 16, channels, true, false)

    init {
        runCatching {
            val info = DataLine.Info(SourceDataLine::class.java, format)
            line = AudioSystem.getLine(info) as SourceDataLine
            line?.open(format, 4096)
        }
    }


    actual fun start() {
        line?.start()
    }

    actual fun write(buffer: ShortArray, offset: Int, size: Int) {
        val l = line ?: return
        val byteBuf = ByteBuffer.allocate(size * 2).order(ByteOrder.LITTLE_ENDIAN)
        for (i in offset until (offset + size)) {
            byteBuf.putShort(buffer[i])
        }
        l.write(byteBuf.array(), 0, size * 2)
    }

    actual fun stop() {
        runCatching {
            line?.stop()
            line?.flush()
        }
    }

    actual fun release() {
        runCatching {
            line?.close()
        }
    }
}

actual class AudioRecorder actual constructor(sampleRate: Int, bufferSize: Int) {
    private var line: TargetDataLine? = null
    private val format = AudioFormat(sampleRate.toFloat(), 16, 1, true, false)

    actual val isInitialized: Boolean
        get() = line != null

    init {
        runCatching {
            val info = DataLine.Info(TargetDataLine::class.java, format)
            line = AudioSystem.getLine(info) as TargetDataLine
            line?.open(format, maxOf(bufferSize * 2, 4096))
        }
    }

    actual fun start() {
        line?.start()
    }

    actual fun read(buffer: ShortArray, offset: Int, size: Int): Int {
        val l = line ?: return -1
        val bytesToRead = size * 2
        val byteBuf = ByteArray(bytesToRead)
        val bytesRead = l.read(byteBuf, 0, bytesToRead)
        if (bytesRead <= 0) return bytesRead
        val shortsRead = bytesRead / 2
        val wrap = ByteBuffer.wrap(byteBuf, 0, bytesRead).order(ByteOrder.LITTLE_ENDIAN)
        for (i in 0 until shortsRead) {
            buffer[offset + i] = wrap.getShort()
        }
        return shortsRead
    }

    actual fun stop() {
        runCatching {
            line?.stop()
            line?.flush()
        }
    }

    actual fun release() {
        runCatching {
            line?.close()
        }
    }
}
