package pro.jayeshseth.bugrush.audio

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.Clip
import javax.sound.sampled.DataLine
import javax.sound.sampled.FloatControl

/**
 * Procedural 8-bit chiptune audio player for JVM Desktop.
 * Plays mathematically synthesized in-memory waveforms with zero external file dependencies.
 * Tolerates headless / audio-less environments gracefully without crashing.
 *
 * Guarantees strict single-clip music playback with zero audio overlap or thread leaks.
 */
class DesktopChiptuneAudioPlayer : SoundPlayer {

    enum class MusicType { TITLE, BGM }

    private val format = AudioFormat(44100f, 16, 1, true, false)

    // SFX pool for quick fire-and-forget sound effects
    private val sfxExecutor = Executors.newCachedThreadPool { runnable ->
        Thread(runnable, "chiptune-sfx-worker").apply { isDaemon = true }
    }

    // Dedicated SINGLE-THREAD executor for background music to strictly serialize transitions
    // and guarantee that no two music clips can ever play simultaneously
    private val musicExecutor = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "chiptune-music-worker").apply { isDaemon = true }
    }

    private val sfxBuffers = ConcurrentHashMap<SoundEffect, ByteArray>()
    private var bgmBuffer: ByteArray? = null
    private var titleBuffer: ByteArray? = null

    // Single active music clip and its type - guarantees only ONE background stream exists
    private var activeMusicClip: Clip? = null
    private var currentMusicType: MusicType? = null
    private var targetMusicType: MusicType? = null
    private var isMusicPaused: Boolean = false

    @Volatile
    private var _isMuted: Boolean = false
    override val isMuted: Boolean get() = _isMuted

    init {
        // Pre-synthesize buffers in background
        sfxExecutor.submit {
            try {
                for (sfx in SoundEffect.values()) {
                    sfxBuffers[sfx] = ChiptuneSynthesizer.synthesizeSfx(sfx)
                }
                bgmBuffer = ChiptuneSynthesizer.synthesizeBgm()
                titleBuffer = ChiptuneSynthesizer.synthesizeTitleTheme()
            } catch (_: Throwable) {
                // Ignore synthesis issues
            }
        }
    }

    override fun playSfx(sfx: SoundEffect) {
        if (_isMuted) return

        val buffer = sfxBuffers[sfx] ?: try {
            ChiptuneSynthesizer.synthesizeSfx(sfx).also { sfxBuffers[sfx] = it }
        } catch (_: Throwable) {
            return
        }

        sfxExecutor.submit {
            try {
                val info = DataLine.Info(Clip::class.java, format)
                if (AudioSystem.isLineSupported(info)) {
                    val clip = AudioSystem.getLine(info) as Clip
                    clip.open(format, buffer, 0, buffer.size)
                    clip.addLineListener { event ->
                        if (event.type == javax.sound.sampled.LineEvent.Type.STOP) {
                            sfxExecutor.submit {
                                try {
                                    clip.close()
                                } catch (_: Throwable) {}
                            }
                        }
                    }
                    clip.start()
                }
            } catch (_: Throwable) {
                // Graceful fallback for audio-less headless environments
            }
        }
    }

    private fun setClipVolume(clip: Clip?, linearVolume: Float) {
        if (clip == null || !clip.isOpen) return
        try {
            if (clip.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
                val gainControl = clip.getControl(FloatControl.Type.MASTER_GAIN) as FloatControl
                val minGain = gainControl.minimum
                val maxGain = 0f.coerceAtMost(gainControl.maximum)
                if (linearVolume <= 0.001f) {
                    gainControl.value = minGain
                } else {
                    val dB = (20.0 * kotlin.math.log10(linearVolume.toDouble().coerceIn(0.0001, 1.0))).toFloat()
                    gainControl.value = dB.coerceIn(minGain, maxGain)
                }
            } else if (clip.isControlSupported(FloatControl.Type.VOLUME)) {
                val volControl = clip.getControl(FloatControl.Type.VOLUME) as FloatControl
                val target = (volControl.minimum + (volControl.maximum - volControl.minimum) * linearVolume).coerceIn(volControl.minimum, volControl.maximum)
                volControl.value = target
            }
        } catch (_: Throwable) {}
    }

    private fun fadeOutAndClose(clip: Clip?, durationMs: Long = 300L) {
        if (clip == null || !clip.isOpen) return
        try {
            if (clip.isRunning) {
                val steps = 8
                val stepDelay = (durationMs / steps).coerceAtLeast(10L)
                for (i in (steps - 1) downTo 0) {
                    setClipVolume(clip, i.toFloat() / steps)
                    try { Thread.sleep(stepDelay) } catch (_: Throwable) { break }
                }
                try { clip.stop() } catch (_: Throwable) {}
            }
            try { clip.close() } catch (_: Throwable) {}
        } catch (_: Throwable) {}
    }

    private fun fadeIn(clip: Clip?, durationMs: Long = 600L) {
        if (clip == null || !clip.isOpen) return
        setClipVolume(clip, 0.05f)
        val steps = 10
        val stepDelay = (durationMs / steps).coerceAtLeast(10L)
        for (i in 1..steps) {
            setClipVolume(clip, i.toFloat() / steps)
            try { Thread.sleep(stepDelay) } catch (_: Throwable) { break }
        }
    }

    private fun playMusic(type: MusicType) {
        targetMusicType = type
        isMusicPaused = false
        if (_isMuted) {
            musicExecutor.submit {
                fadeOutAndClose(activeMusicClip, 200L)
                activeMusicClip = null
                currentMusicType = null
            }
            return
        }

        musicExecutor.submit {
            try {
                // If the requested music is already active and running, ignore duplicate requests
                if (currentMusicType == type && activeMusicClip?.isRunning == true) {
                    return@submit
                }

                // Strictly fade out and close any previously running music clip before starting the new one
                val oldClip = activeMusicClip
                if (oldClip != null) {
                    fadeOutAndClose(oldClip, 300L)
                    activeMusicClip = null
                    currentMusicType = null
                }

                if (targetMusicType != type || _isMuted || isMusicPaused) return@submit

                val buffer = when (type) {
                    MusicType.TITLE -> titleBuffer ?: ChiptuneSynthesizer.synthesizeTitleTheme().also { titleBuffer = it }
                    MusicType.BGM -> bgmBuffer ?: ChiptuneSynthesizer.synthesizeBgm().also { bgmBuffer = it }
                }

                if (targetMusicType != type || _isMuted || isMusicPaused) return@submit

                val info = DataLine.Info(Clip::class.java, format)
                if (AudioSystem.isLineSupported(info)) {
                    val newClip = AudioSystem.getLine(info) as Clip
                    newClip.open(format, buffer, 0, buffer.size)
                    if (targetMusicType != type || _isMuted || isMusicPaused) {
                        try { newClip.close() } catch (_: Throwable) {}
                        return@submit
                    }
                    setClipVolume(newClip, 0.05f)
                    newClip.loop(Clip.LOOP_CONTINUOUSLY)
                    activeMusicClip = newClip
                    currentMusicType = type
                    fadeIn(newClip, 600L)
                }
            } catch (_: Throwable) {
                // Graceful fallback
            }
        }
    }

    override fun startTitleTheme() {
        playMusic(MusicType.TITLE)
    }

    override fun stopTitleTheme() {
        if (targetMusicType == MusicType.TITLE) {
            targetMusicType = null
            isMusicPaused = false
            musicExecutor.submit {
                if (currentMusicType == MusicType.TITLE) {
                    fadeOutAndClose(activeMusicClip, 350L)
                    activeMusicClip = null
                    currentMusicType = null
                }
            }
        }
    }

    override fun startBgm() {
        playMusic(MusicType.BGM)
    }

    override fun stopBgm() {
        if (targetMusicType == MusicType.BGM) {
            targetMusicType = null
            isMusicPaused = false
            musicExecutor.submit {
                if (currentMusicType == MusicType.BGM) {
                    fadeOutAndClose(activeMusicClip, 350L)
                    activeMusicClip = null
                    currentMusicType = null
                }
            }
        }
    }

    override fun pauseBgm() {
        if (targetMusicType == MusicType.BGM) {
            isMusicPaused = true
            musicExecutor.submit {
                val clip = activeMusicClip
                if (clip != null && clip.isOpen && clip.isRunning) {
                    // Smoothly fade down volume over 250ms, then pause
                    val steps = 8
                    val stepDelay = 30L
                    for (i in (steps - 1) downTo 0) {
                        setClipVolume(clip, i.toFloat() / steps)
                        try { Thread.sleep(stepDelay) } catch (_: Throwable) { break }
                    }
                    try { clip.stop() } catch (_: Throwable) {}
                }
            }
        }
    }

    override fun resumeBgm() {
        if (targetMusicType == MusicType.BGM && isMusicPaused && !_isMuted) {
            isMusicPaused = false
            musicExecutor.submit {
                val clip = activeMusicClip
                if (clip != null && clip.isOpen && !clip.isRunning) {
                    setClipVolume(clip, 0.05f)
                    clip.loop(Clip.LOOP_CONTINUOUSLY)
                    fadeIn(clip, 400L)
                } else if (clip == null || !clip.isOpen) {
                    playMusic(MusicType.BGM)
                }
            }
        }
    }

    override fun toggleMute() {
        _isMuted = !_isMuted
        if (_isMuted) {
            musicExecutor.submit {
                fadeOutAndClose(activeMusicClip, 150L)
                activeMusicClip = null
                currentMusicType = null
            }
        } else {
            val restore = targetMusicType ?: MusicType.TITLE
            playMusic(restore)
        }
    }
}
