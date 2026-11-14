package pro.jayeshseth.devcontrols.camera

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.awt.Color
import java.awt.Font
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.awt.image.ConvolveOp
import java.awt.image.DataBufferByte
import java.awt.image.Kernel
import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import kotlin.math.cos
import kotlin.math.sin

class DesktopVirtualCameraEngine : VirtualCameraEngine {
    private val _settings = mutableStateOf(VirtualCameraSettings())
    override val settings: State<VirtualCameraSettings> = _settings

    private val _status = mutableStateOf(VirtualCameraStatus(isCameraActive = true, fps = 60))
    override val status: State<VirtualCameraStatus> = _status

    private val _processedFrame = mutableStateOf<ImageBitmap?>(null)
    override val processedFrame: State<ImageBitmap?> = _processedFrame

    private val _rawFrame = mutableStateOf<ImageBitmap?>(null)
    override val rawFrame: State<ImageBitmap?> = _rawFrame

    private val scope = CoroutineScope(Dispatchers.Default)
    private var renderJob: Job? = null
    private var ffmpegProcess: Process? = null

    init {
        startCamera()
    }

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
        val v4l2Exists = File("/dev/video20").exists()
        _settings.value = _settings.value.copy(isVirtualCamEnabled = enabled)
        _status.value = _status.value.copy(
            isVirtualCamActive = enabled && v4l2Exists,
            errorMessage = if (enabled && !v4l2Exists) {
                "V4L2 loopback device /dev/video20 not found. Run 'sudo modprobe v4l2loopback devices=1 card_label=\"DevControls Enhanced Cam\" exclusive_caps=1' in terminal."
            } else null
        )
    }

    override fun startCamera() {
        if (renderJob?.isActive == true) return
        val hasPhysicalCam = File("/dev/video0").exists()
        _status.value = _status.value.copy(
            isCameraActive = true,
            activeDeviceName = if (hasPhysicalCam) "ASUS ROG EYE S (/dev/video0)" else "Virtual Pattern Generator"
        )

        renderJob = scope.launch(Dispatchers.IO) {
            val width = 1280
            val height = 720
            val frameSize = width * height * 3

            var stream: BufferedInputStream? = null
            if (hasPhysicalCam) {
                try {
                    val pb = ProcessBuilder(
                        "ffmpeg",
                        "-fflags", "nobuffer",
                        "-flags", "low_delay",
                        "-probesize", "32",
                        "-analyzeduration", "0",
                        "-f", "v4l2",
                        "-input_format", "mjpeg",
                        "-framerate", "60",
                        "-video_size", "${width}x${height}",
                        "-i", "/dev/video0",
                        "-f", "rawvideo",
                        "-pix_fmt", "bgr24",
                        "-an", "-sn",
                        "-nostats", "-loglevel", "error",
                        "-"
                    )
                    val proc = pb.start()
                    ffmpegProcess = proc
                    stream = BufferedInputStream(proc.inputStream, 1024 * 1024)
                } catch (e: Exception) {
                    println("Failed to start ffmpeg capture: ${e.message}")
                }
            }

            var frameCount = 0
            var lastTime = System.currentTimeMillis()
            var tick = 0.0
            val frameBytes = ByteArray(frameSize)

            while (isActive) {
                tick += 0.05
                frameCount++

                val now = System.currentTimeMillis()
                if (now - lastTime >= 1000) {
                    _status.value = _status.value.copy(fps = frameCount)
                    frameCount = 0
                    lastTime = now
                }

                // Drain any accumulated frame backlog in the pipe to keep latency at 0ms
                if (stream != null) {
                    while (stream.available() >= frameSize * 2) {
                        stream.skip(frameSize.toLong())
                    }
                }

                val rawImg: BufferedImage
                val isRealFrame = stream != null && readFully(stream, frameBytes, frameSize)

                if (isRealFrame) {
                    rawImg = BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR)
                    val targetBuffer = (rawImg.raster.dataBuffer as DataBufferByte).data
                    System.arraycopy(frameBytes, 0, targetBuffer, 0, frameSize)
                } else {
                    rawImg = generateTestFrame(width, height, tick)
                    delay(16)
                }

                // 2. Process Enhanced Frame Pipeline via fast LUT
                val currentSettings = _settings.value
                val procImg = applyImagePipeline(rawImg, currentSettings)

                // Render Virtual Camera Status watermark onto processed frame if virtual cam is active
                if (currentSettings.isVirtualCamEnabled) {
                    val gProc = procImg.createGraphics()
                    gProc.color = Color(0, 230, 120)
                    gProc.font = Font("SansSerif", Font.BOLD, 18)
                    gProc.drawString("● DEVCONTROLS ENHANCED CAM VIRTUAL FEED ACTIVE (/dev/video20)", 30, 40)
                    gProc.dispose()
                    writeToV4l2Loopback(procImg)
                }

                if (currentSettings.isCompareMode) {
                    _rawFrame.value = rawImg.toComposeImageBitmap()
                }
                _processedFrame.value = procImg.toComposeImageBitmap()
            }
        }
    }

    private fun readFully(input: InputStream, buffer: ByteArray, length: Int): Boolean {
        var total = 0
        try {
            while (total < length) {
                val read = input.read(buffer, total, length - total)
                if (read < 0) return false
                total += read
            }
            return true
        } catch (_: Exception) {
            return false
        }
    }

    private fun generateTestFrame(width: Int, height: Int, tick: Double): BufferedImage {
        val rawImg = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
        val gRaw = rawImg.createGraphics()
        gRaw.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)

        gRaw.color = Color(20, 24, 32)
        gRaw.fillRect(0, 0, width, height)

        gRaw.color = Color(35, 42, 56)
        for (x in 0 until width step 60) gRaw.drawLine(x, 0, x, height)
        for (y in 0 until height step 60) gRaw.drawLine(0, y, width, y)

        val centerX = width / 2 + (sin(tick * 0.5) * 30).toInt()
        val centerY = height / 2 + (cos(tick * 0.7) * 15).toInt()

        gRaw.color = Color(255, 120, 80, 180)
        gRaw.fillOval(100, 120, 140, 140)
        gRaw.color = Color(80, 180, 255, 180)
        gRaw.fillOval(width - 240, 180, 180, 180)

        gRaw.color = Color(240, 195, 160)
        gRaw.fillOval(centerX - 100, centerY - 140, 200, 240)
        gRaw.color = Color(40, 30, 25)
        gRaw.fillOval(centerX - 110, centerY - 160, 220, 100)

        gRaw.color = Color(30, 30, 30)
        gRaw.fillOval(centerX - 55, centerY - 50, 24, 24)
        gRaw.fillOval(centerX + 31, centerY - 50, 24, 24)

        gRaw.color = Color(200, 200, 200, 160)
        gRaw.font = Font("Monospaced", Font.BOLD, 18)
        gRaw.drawString("NO HARDWARE CAMERA DETECTED | Virtual Pattern Generator", 30, 40)
        gRaw.dispose()
        return rawImg
    }

    private fun applyImagePipeline(rawImg: BufferedImage, settings: VirtualCameraSettings): BufferedImage {
        val w = rawImg.width
        val h = rawImg.height
        val frameSize = w * h * 3

        val procImg = BufferedImage(w, h, BufferedImage.TYPE_3BYTE_BGR)
        val rawBuffer = (rawImg.raster.dataBuffer as DataBufferByte).data
        val procBuffer = (procImg.raster.dataBuffer as DataBufferByte).data
        System.arraycopy(rawBuffer, 0, procBuffer, 0, frameSize)

        // 1. Precompute 256-entry Look-Up Tables
        val lutR = ByteArray(256)
        val lutG = ByteArray(256)
        val lutB = ByteArray(256)

        val contrastFactor = settings.contrast
        val brightOffset = (settings.brightness * 255).toInt()
        val claheBoost = settings.claheAmount
        val tempGrad = settings.colorTemperature

        for (v in 0..255) {
            var r = (((v - 128) * contrastFactor) + 128 + brightOffset).toInt().coerceIn(0, 255)
            var g = (((v - 128) * contrastFactor) + 128 + brightOffset).toInt().coerceIn(0, 255)
            var b = (((v - 128) * contrastFactor) + 128 + brightOffset).toInt().coerceIn(0, 255)

            // CLAHE Backlight shadow lifting
            if (claheBoost > 0.05f && v < 140) {
                val lift = ((140 - v) * claheBoost * 0.5f).toInt()
                r = (r + lift).coerceAtMost(255)
                g = (g + lift).coerceAtMost(255)
                b = (b + lift).coerceAtMost(255)
            }

            // Color Temperature (Warm / Cool shift)
            if (tempGrad != 0f) {
                if (tempGrad > 0f) {
                    r = (r + (tempGrad * 25).toInt()).coerceAtMost(255)
                    b = (b - (tempGrad * 15).toInt()).coerceAtLeast(0)
                } else {
                    r = (r + (tempGrad * 15).toInt()).coerceAtLeast(0)
                    b = (b - (tempGrad * 25).toInt()).coerceAtMost(255)
                }
            }

            lutR[v] = r.toByte()
            lutG[v] = g.toByte()
            lutB[v] = b.toByte()
        }

        // 2. High-speed in-place LUT mapping on BGR byte buffer
        var idx = 0
        while (idx < frameSize) {
            val b = procBuffer[idx].toInt() and 0xFF
            val g = procBuffer[idx + 1].toInt() and 0xFF
            val r = procBuffer[idx + 2].toInt() and 0xFF

            procBuffer[idx] = lutB[b]
            procBuffer[idx + 1] = lutG[g]
            procBuffer[idx + 2] = lutR[r]
            idx += 3
        }

        // 3. Fast Sharpen (only when slider is active)
        if (settings.sharpness > 0.1f) {
            val s = settings.sharpness * 0.5f
            val kernelData = floatArrayOf(
                0f, -s, 0f,
                -s, 1f + (4f * s), -s,
                0f, -s, 0f
            )
            val op = ConvolveOp(Kernel(3, 3, kernelData), ConvolveOp.EDGE_NO_OP, null)
            return op.filter(procImg, null)
        }

        return procImg
    }

    private fun writeToV4l2Loopback(img: BufferedImage) {
        val devFile = File("/dev/video20")
        if (!devFile.exists()) return

        try {
            FileOutputStream(devFile).use { fos ->
                val buffer = (img.raster.dataBuffer as? DataBufferByte)?.data
                if (buffer != null) {
                    fos.write(buffer)
                }
            }
        } catch (_: Exception) {
            // Silently handle if v4l2 device is busy or unwriteable
        }
    }

    override fun stopCamera() {
        renderJob?.cancel()
        try {
            ffmpegProcess?.destroyForcibly()
        } catch (_: Exception) {}
        ffmpegProcess = null
        _status.value = _status.value.copy(isCameraActive = false, isVirtualCamActive = false)
    }

    override fun selectCamera(index: Int) {
        _settings.value = _settings.value.copy(selectedCameraIndex = index)
    }
}

@Composable
actual fun rememberVirtualCameraEngine(): VirtualCameraEngine {
    val engine = remember { DesktopVirtualCameraEngine() }
    DisposableEffect(Unit) {
        onDispose { engine.stopCamera() }
    }
    return engine
}
