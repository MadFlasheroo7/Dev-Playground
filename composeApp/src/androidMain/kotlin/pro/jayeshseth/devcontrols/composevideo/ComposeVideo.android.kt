package pro.jayeshseth.devcontrols.composevideo

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.nio.ByteBuffer

// ─────────────────────────────────────────────────────────────────────────────
// ComposeVideo — Android actual
// ─────────────────────────────────────────────────────────────────────────────

@Composable
actual fun ComposeVideo(
    controller: ComposeVideoController,
    modifier: Modifier,
    content: @Composable ComposeVideoScope.() -> Unit,
) {
    val graphicsLayer = rememberGraphicsLayer()

    Box(
        modifier = modifier.drawWithContent {
            graphicsLayer.record { this@drawWithContent.drawContent() }
            drawLayer(graphicsLayer)
        }
    ) {
        ComposeVideoScope(
            progress = controller.currentProgress,
            timeMs = controller.currentTimeMs,
            fps = controller.fps,
            durationMs = controller.durationMs,
        ).content()
    }

    // Playback engine
    LaunchedEffect(controller.isPlaying) {
        if (controller.isPlaying) {
            val frameMs = 1000L / controller.fps
            while (controller.isPlaying) {
                withFrameNanos { }
                controller.currentTimeMs += frameMs
                if (controller.currentTimeMs >= controller.durationMs) {
                    if (controller.isLooping) controller.currentTimeMs = 0L
                    else { controller.isPlaying = false; controller.currentTimeMs = controller.durationMs }
                }
            }
        }
    }

    // Register frame-capture callback: scrub time → recompose → toImageBitmap()
    LaunchedEffect(graphicsLayer) {
        controller.onCaptureFrame = { frameIdx ->
            controller.isPlaying = false
            controller.currentTimeMs = (frameIdx * 1000L) / controller.fps
            withFrameNanos { } // wait for recompose
            withFrameNanos { } // wait for draw into graphicsLayer
            val imageBitmap = graphicsLayer.toImageBitmap()
            val androidBitmap = Bitmap.createBitmap(
                imageBitmap.width, imageBitmap.height, Bitmap.Config.ARGB_8888
            )
            val pixels = IntArray(imageBitmap.width * imageBitmap.height)
            imageBitmap.readPixels(pixels, 0, 0, imageBitmap.width, imageBitmap.height)
            androidBitmap.setPixels(pixels, 0, imageBitmap.width, 0, 0, imageBitmap.width, imageBitmap.height)
            androidBitmap
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Export engine — suspend function called from the ComposeVideoScreen
// ─────────────────────────────────────────────────────────────────────────────

suspend fun exportComposeVideoAndroid(
    context: Context,
    controller: ComposeVideoController,
    onProgress: (Float) -> Unit,
    onComplete: (String) -> Unit,
    onError: (String) -> Unit,
) = withContext(Dispatchers.Default) {

    val captureFrame = controller.onCaptureFrame
        ?: run { onError("ComposeVideo not yet rendered — place it on screen before exporting"); return@withContext }

    val w = controller.resolution.width
    val h = controller.resolution.height
    val fps = controller.fps
    val totalFrames = controller.totalFrames

    val trimEffect = controller.effects.filterIsInstance<VideoEffect.Trim>().lastOrNull()
    val trimStartMs = trimEffect?.startMs ?: 0L
    val trimEndMs = trimEffect?.endMs ?: controller.durationMs

    var codec: MediaCodec? = null
    var muxer: MediaMuxer? = null
    var tempVideoFile: File? = null

    try {
        controller.exportPhase = "Encoding video…"
        tempVideoFile = File.createTempFile("cvid_", ".mp4", context.cacheDir)

        val format = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, w, h).apply {
            setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible)
            setInteger(MediaFormat.KEY_BIT_RATE, w * h * 4)
            setInteger(MediaFormat.KEY_FRAME_RATE, fps)
            setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
        }

        codec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC)
        codec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        codec.start()

        muxer = MediaMuxer(tempVideoFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        var videoTrackIdx = -1
        var muxerStarted = false
        val bufferInfo = MediaCodec.BufferInfo()

        val yuvBytes = ByteArray(w * h * 3 / 2)
        val argbPixels = IntArray(w * h)
        val effectBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)

        var frameIndex = 0
        var outputDone = false

        while (!outputDone) {
            if (frameIndex < totalFrames) {
                val frameTimeMs = (frameIndex * 1000L) / fps
                if (frameTimeMs in trimStartMs..trimEndMs) {
                    val inputIdx = codec.dequeueInputBuffer(10_000)
                    if (inputIdx >= 0) {
                        // Capture on main thread
                        val rawBitmap = withContext(Dispatchers.Main) {
                            @Suppress("UNCHECKED_CAST")
                            (captureFrame as suspend (Int) -> Bitmap)(frameIndex)
                        }

                        // Scale to target resolution
                        val scaled = if (rawBitmap.width != w || rawBitmap.height != h)
                            Bitmap.createScaledBitmap(rawBitmap, w, h, true) else rawBitmap

                        // Apply effects
                        val processed = applyEffectChain(scaled, controller.effects, w, h, effectBitmap)
                        processed.getPixels(argbPixels, 0, w, 0, 0, w, h)
                        encodeYuv420SP(yuvBytes, argbPixels, w, h)

                        val inBuf: ByteBuffer = codec.getInputBuffer(inputIdx)!!
                        inBuf.clear()
                        inBuf.put(yuvBytes)

                        val presentUs = (frameIndex * 1_000_000L) / fps
                        val flags = if (frameIndex == totalFrames - 1) MediaCodec.BUFFER_FLAG_END_OF_STREAM else 0
                        codec.queueInputBuffer(inputIdx, 0, yuvBytes.size, presentUs, flags)
                        onProgress((frameIndex + 1).toFloat() / totalFrames * 0.75f)
                        frameIndex++
                    }
                } else {
                    frameIndex++
                }
            }

            val outIdx = codec.dequeueOutputBuffer(bufferInfo, 10_000)
            when {
                outIdx >= 0 -> {
                    val outBuf = codec.getOutputBuffer(outIdx)!!
                    if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0) bufferInfo.size = 0
                    if (bufferInfo.size > 0 && muxerStarted) {
                        outBuf.position(bufferInfo.offset)
                        outBuf.limit(bufferInfo.offset + bufferInfo.size)
                        muxer.writeSampleData(videoTrackIdx, outBuf, bufferInfo)
                    }
                    codec.releaseOutputBuffer(outIdx, false)
                    if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) outputDone = true
                }
                outIdx == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                    if (!muxerStarted) {
                        videoTrackIdx = muxer.addTrack(codec.outputFormat)
                        muxer.start()
                        muxerStarted = true
                    }
                }
            }
        }

        codec.stop(); codec.release(); codec = null
        muxer.stop(); muxer.release(); muxer = null
        effectBitmap.recycle()

        // ── Audio mux ──────────────────────────────────────────────────────────
        val audioUri = controller.audioUriString?.let { Uri.parse(it) }
        val finalFile: File
        if (audioUri != null) {
            controller.exportPhase = "Muxing audio…"
            onProgress(0.85f)
            val merged = File.createTempFile("cvid_merged_", ".mp4", context.cacheDir)
            muxVideoAndAudio(context, tempVideoFile!!, audioUri, controller.audioOffsetMs, trimEndMs - trimStartMs, merged)
            tempVideoFile!!.delete()
            finalFile = merged
        } else {
            finalFile = tempVideoFile!!
        }

        // ── Save to MediaStore ─────────────────────────────────────────────────
        controller.exportPhase = "Saving…"
        onProgress(0.95f)

        val fileName = "${controller.outputFileName}.mp4"
        val values = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, fileName)
            put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Video.Media.RELATIVE_PATH, Environment.DIRECTORY_MOVIES + "/ComposeVideo")
                put(MediaStore.Video.Media.IS_PENDING, 1)
            }
        }
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
            MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        else MediaStore.Video.Media.EXTERNAL_CONTENT_URI

        val videoUri = context.contentResolver.insert(collection, values)
            ?: run { onError("Failed to create MediaStore entry"); finalFile.delete(); return@withContext }

        context.contentResolver.openOutputStream(videoUri)?.use { os ->
            FileInputStream(finalFile).use { it.copyTo(os) }
        } ?: run { onError("Failed to open output stream"); finalFile.delete(); return@withContext }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            values.clear(); values.put(MediaStore.Video.Media.IS_PENDING, 0)
            context.contentResolver.update(videoUri, values, null, null)
        }

        finalFile.delete()
        onProgress(1f)
        onComplete("Movies/ComposeVideo/$fileName")

    } catch (e: Exception) {
        onError(e.message ?: "Unknown error during export")
    } finally {
        try { codec?.release() } catch (_: Exception) {}
        try { muxer?.release() } catch (_: Exception) {}
        try { tempVideoFile?.delete() } catch (_: Exception) {}
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Audio muxing
// ─────────────────────────────────────────────────────────────────────────────

private fun muxVideoAndAudio(
    context: Context,
    videoFile: File,
    audioUri: Uri,
    audioOffsetMs: Long,
    audioDurationMs: Long,
    outputFile: File,
) {
    val muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
    val videoExt = MediaExtractor().apply { setDataSource(videoFile.absolutePath) }
    val audioExt = MediaExtractor().apply { setDataSource(context, audioUri, null) }

    var vTrack = -1
    for (i in 0 until videoExt.trackCount) {
        val fmt = videoExt.getTrackFormat(i)
        if (fmt.getString(MediaFormat.KEY_MIME)?.startsWith("video/") == true) {
            vTrack = muxer.addTrack(fmt); videoExt.selectTrack(i); break
        }
    }
    var aTrack = -1
    for (i in 0 until audioExt.trackCount) {
        val fmt = audioExt.getTrackFormat(i)
        if (fmt.getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true) {
            aTrack = muxer.addTrack(fmt); audioExt.selectTrack(i); break
        }
    }

    muxer.start()
    val buf = ByteBuffer.allocate(2 * 1024 * 1024)
    val info = MediaCodec.BufferInfo()

    if (vTrack >= 0) {
        while (true) {
            buf.clear()
            val sz = videoExt.readSampleData(buf, 0); if (sz < 0) break
            info.offset = 0; info.size = sz; info.presentationTimeUs = videoExt.sampleTime; info.flags = videoExt.sampleFlags
            muxer.writeSampleData(vTrack, buf, info); videoExt.advance()
        }
    }
    if (aTrack >= 0) {
        if (audioOffsetMs > 0) audioExt.seekTo(audioOffsetMs * 1000, MediaExtractor.SEEK_TO_CLOSEST_SYNC)
        val maxUs = audioDurationMs * 1000L
        val baseUs = audioExt.sampleTime
        while (true) {
            buf.clear()
            val sz = audioExt.readSampleData(buf, 0); if (sz < 0) break
            val relUs = audioExt.sampleTime - baseUs; if (relUs > maxUs) break
            info.offset = 0; info.size = sz; info.presentationTimeUs = relUs; info.flags = audioExt.sampleFlags
            muxer.writeSampleData(aTrack, buf, info); audioExt.advance()
        }
    }

    muxer.stop(); muxer.release(); videoExt.release(); audioExt.release()
}

// ─────────────────────────────────────────────────────────────────────────────
// Effect pipeline
// ─────────────────────────────────────────────────────────────────────────────

private fun applyEffectChain(src: Bitmap, effects: List<VideoEffect>, w: Int, h: Int, reusable: Bitmap): Bitmap {
    if (effects.none { it is VideoEffect.Brightness || it is VideoEffect.Contrast || it is VideoEffect.Saturation || it is VideoEffect.Blur }) return src

    val brightness = effects.filterIsInstance<VideoEffect.Brightness>().lastOrNull()?.factor ?: 1f
    val contrast   = effects.filterIsInstance<VideoEffect.Contrast>().lastOrNull()?.factor ?: 1f
    val saturation = effects.filterIsInstance<VideoEffect.Saturation>().lastOrNull()?.factor ?: 1f
    val blur       = effects.filterIsInstance<VideoEffect.Blur>().lastOrNull()

    var current = src
    val hasColorFx = brightness != 1f || contrast != 1f || saturation != 1f
    if (hasColorFx) {
        val matrix = ColorMatrix()
        matrix.setSaturation(saturation)
        val t = 255f * (1f - contrast) / 2f + (brightness - 1f) * 255f
        val s = contrast * brightness
        matrix.postConcat(ColorMatrix(floatArrayOf(s,0f,0f,0f,t, 0f,s,0f,0f,t, 0f,0f,s,0f,t, 0f,0f,0f,1f,0f)))
        reusable.eraseColor(0)
        Canvas(reusable).drawBitmap(current, 0f, 0f, Paint().apply { colorFilter = ColorMatrixColorFilter(matrix) })
        current = reusable
    }
    if (blur != null && blur.radius > 0) current = boxBlur(current, blur.radius.coerceIn(1, 25), w, h)
    return current
}

private fun boxBlur(src: Bitmap, radius: Int, w: Int, h: Int): Bitmap {
    val pixels = IntArray(w * h); src.getPixels(pixels, 0, w, 0, 0, w, h)
    val out = pixels.copyOf()
    for (y in 0 until h) for (x in 0 until w) {
        var r = 0; var g = 0; var b = 0; var count = 0
        for (dy in -radius..radius) for (dx in -radius..radius) {
            val px = pixels[(y+dy).coerceIn(0,h-1)*w+(x+dx).coerceIn(0,w-1)]
            r += (px shr 16) and 0xFF; g += (px shr 8) and 0xFF; b += px and 0xFF; count++
        }
        out[y*w+x] = (0xFF shl 24) or ((r/count) shl 16) or ((g/count) shl 8) or (b/count)
    }
    return Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888).also { it.setPixels(out, 0, w, 0, 0, w, h) }
}

private fun encodeYuv420SP(yuv: ByteArray, argb: IntArray, w: Int, h: Int) {
    val fs = w * h; var yi = 0; var uvi = fs; var idx = 0
    for (j in 0 until h) for (i in 0 until w) {
        val r=(argb[idx] and 0xff0000) ushr 16; val g=(argb[idx] and 0xff00) ushr 8; val b=argb[idx] and 0xff
        yuv[yi++] = (((66*r+129*g+25*b+128) ushr 8)+16).coerceIn(0,255).toByte()
        if (j%2==0 && idx%2==0) {
            yuv[uvi++] = (((-38*r-74*g+112*b+128) ushr 8)+128).coerceIn(0,255).toByte()
            yuv[uvi++] = (((112*r-94*g-18*b+128) ushr 8)+128).coerceIn(0,255).toByte()
        }
        idx++
    }
}
