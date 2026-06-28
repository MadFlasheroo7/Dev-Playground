package pro.jayeshseth.devcontrols.ui.devtools

import android.content.ContentValues
import android.graphics.Bitmap
import android.graphics.Canvas
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

@Composable
actual fun getPlatformContext(): Any? = LocalContext.current

actual fun exportAnimationToVideo(
    context: Any?,
    width: Int,
    height: Int,
    frameCount: Int,
    fps: Int,
    fileName: String,
    exportType: ExportType,
    onProgress: (progress: Float) -> Unit,
    onComplete: (filePath: String) -> Unit,
    onError: (error: String) -> Unit,
    renderFrame: DrawScope.(frameIndex: Int) -> Unit
) {
    val ctx = context as? android.content.Context
    if (ctx == null) {
        onError("Android context is null")
        return
    }

    CoroutineScope(Dispatchers.Default).launch {
        var codec: MediaCodec? = null
        var muxer: MediaMuxer? = null
        var tempFile: File? = null
        var bitmap: Bitmap? = null
        try {
            val resolver = ctx.contentResolver

            if (exportType == ExportType.VIDEO) {
                // Export to MP4
                val outputDir = ctx.cacheDir
                tempFile = File.createTempFile("compose_anim_", ".mp4", outputDir)

                val format = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, width, height)
                format.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible)
                format.setInteger(MediaFormat.KEY_BIT_RATE, width * height * 5)
                format.setInteger(MediaFormat.KEY_FRAME_RATE, fps)
                format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)

                codec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC)
                codec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
                codec.start()

                muxer = MediaMuxer(tempFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
                var videoTrackIndex = -1
                var muxerStarted = false

                val bufferInfo = MediaCodec.BufferInfo()
                val inputFrameBytes = ByteArray(width * height * 3 / 2)
                val argbPixels = IntArray(width * height)

                bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                val androidCanvas = android.graphics.Canvas(bitmap)
                val composeCanvas = androidx.compose.ui.graphics.Canvas(androidCanvas)
                val drawScope = CanvasDrawScope()

                var frameIndex = 0
                var isOutputDone = false

                while (!isOutputDone) {
                    if (frameIndex < frameCount) {
                        val inputBufferIndex = codec.dequeueInputBuffer(10000)
                        if (inputBufferIndex >= 0) {
                            bitmap.eraseColor(0)
                            drawScope.draw(
                                density = Density(1f),
                                layoutDirection = LayoutDirection.Ltr,
                                canvas = composeCanvas,
                                size = androidx.compose.ui.geometry.Size(width.toFloat(), height.toFloat())
                            ) {
                                renderFrame(frameIndex)
                            }

                            bitmap.getPixels(argbPixels, 0, width, 0, 0, width, height)
                            encodeYUV420SP(inputFrameBytes, argbPixels, width, height)

                            val inputBuffer = codec.getInputBuffer(inputBufferIndex)!!
                            inputBuffer.clear()
                            inputBuffer.put(inputFrameBytes)

                            val presentationTimeUs = (frameIndex * 1_000_000L) / fps
                            codec.queueInputBuffer(
                                inputBufferIndex,
                                0,
                                inputFrameBytes.size,
                                presentationTimeUs,
                                if (frameIndex == frameCount - 1) MediaCodec.BUFFER_FLAG_END_OF_STREAM else 0
                            )

                            onProgress((frameIndex + 1).toFloat() / frameCount.toFloat())
                            frameIndex++
                        }
                    }

                    val outputBufferIndex = codec.dequeueOutputBuffer(bufferInfo, 10000)
                    if (outputBufferIndex >= 0) {
                        val encodedData = codec.getOutputBuffer(outputBufferIndex)!!

                        if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0) {
                            bufferInfo.size = 0
                        }

                        if (bufferInfo.size > 0) {
                            if (!muxerStarted) {
                                throw RuntimeException("Muxer not started before output buffer received")
                            }
                            encodedData.position(bufferInfo.offset)
                            encodedData.limit(bufferInfo.offset + bufferInfo.size)
                            muxer.writeSampleData(videoTrackIndex, encodedData, bufferInfo)
                        }

                        codec.releaseOutputBuffer(outputBufferIndex, false)

                        if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                            isOutputDone = true
                        }
                    } else if (outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                        if (muxerStarted) {
                            throw RuntimeException("Format changed twice")
                        }
                        val newFormat = codec.outputFormat
                        videoTrackIndex = muxer.addTrack(newFormat)
                        muxer.start()
                        muxerStarted = true
                    }
                }

                codec.stop()
                codec.release()
                codec = null

                muxer.stop()
                muxer.release()
                muxer = null

                // Save temp file to MediaStore (Movies/DevControls)
                val videoDetails = ContentValues().apply {
                    put(MediaStore.Video.Media.DISPLAY_NAME, "$fileName.mp4")
                    put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        put(MediaStore.Video.Media.RELATIVE_PATH, Environment.DIRECTORY_MOVIES + "/DevControls")
                        put(MediaStore.Video.Media.IS_PENDING, 1)
                    }
                }

                val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                } else {
                    MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                }

                val videoUri = resolver.insert(collection, videoDetails)
                if (videoUri == null) {
                    onError("Failed to insert video into MediaStore")
                    return@launch
                }

                resolver.openOutputStream(videoUri).use { os ->
                    if (os == null) {
                        onError("Failed to open output stream for video")
                        return@launch
                    }
                    FileInputStream(tempFile).use { fis ->
                        fis.copyTo(os)
                    }
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    videoDetails.clear()
                    videoDetails.put(MediaStore.Video.Media.IS_PENDING, 0)
                    resolver.update(videoUri, videoDetails, null, null)
                }

                onComplete("Movies/DevControls/$fileName.mp4")
            } else {
                // Export to PNG ZIP
                val zipDetails = ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, "$fileName.zip")
                    put(MediaStore.Downloads.MIME_TYPE, "application/zip")
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/DevControls")
                        put(MediaStore.Downloads.IS_PENDING, 1)
                    }
                }

                val zipCollection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                } else {
                    MediaStore.Downloads.EXTERNAL_CONTENT_URI
                }

                val zipUri = resolver.insert(zipCollection, zipDetails)
                if (zipUri == null) {
                    onError("Failed to create ZIP entry in MediaStore")
                    return@launch
                }

                resolver.openOutputStream(zipUri).use { os ->
                    if (os == null) {
                        onError("Failed to open output stream for ZIP")
                        return@launch
                    }
                    ZipOutputStream(os).use { zos ->
                        bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                        val androidCanvas = android.graphics.Canvas(bitmap)
                        val composeCanvas = androidx.compose.ui.graphics.Canvas(androidCanvas)
                        val drawScope = CanvasDrawScope()

                        for (i in 0 until frameCount) {
                            bitmap.eraseColor(0)
                            drawScope.draw(
                                density = Density(1f),
                                layoutDirection = LayoutDirection.Ltr,
                                canvas = composeCanvas,
                                size = androidx.compose.ui.geometry.Size(width.toFloat(), height.toFloat())
                            ) {
                                renderFrame(i)
                            }

                            val entry = ZipEntry("frame_${String.format("%03d", i)}.png")
                            zos.putNextEntry(entry)
                            val byteStream = ByteArrayOutputStream()
                            bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteStream)
                            zos.write(byteStream.toByteArray())
                            zos.closeEntry()

                            onProgress((i + 1).toFloat() / frameCount.toFloat())
                        }
                    }
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    zipDetails.clear()
                    zipDetails.put(MediaStore.Downloads.IS_PENDING, 0)
                    resolver.update(zipUri, zipDetails, null, null)
                }

                onComplete("Downloads/DevControls/$fileName.zip")
            }

        } catch (e: Exception) {
            onError(e.message ?: "Unknown Android encoding error")
        } finally {
            try {
                codec?.release()
            } catch (e: Exception) {}
            try {
                muxer?.release()
            } catch (e: Exception) {}
            try {
                tempFile?.delete()
            } catch (e: Exception) {}
            try {
                bitmap?.recycle()
            } catch (e: Exception) {}
        }
    }
}

private fun encodeYUV420SP(yuv420sp: ByteArray, argb: IntArray, width: Int, height: Int) {
    val frameSize = width * height
    var yIndex = 0
    var uvIndex = frameSize
    var index = 0
    for (j in 0 until height) {
        for (i in 0 until width) {
            val r = (argb[index] and 0xff0000) ushr 16
            val g = (argb[index] and 0xff00) ushr 8
            val b = (argb[index] and 0xff)

            var y = ((66 * r + 129 * g + 25 * b + 128) ushr 8) + 16
            var u = ((-38 * r - 74 * g + 112 * b + 128) ushr 8) + 128
            var v = ((112 * r - 94 * g - 18 * b + 128) ushr 8) + 128

            y = if (y < 0) 0 else if (y > 255) 255 else y
            u = if (u < 0) 0 else if (u > 255) 255 else u
            v = if (v < 0) 0 else if (v > 255) 255 else v

            yuv420sp[yIndex++] = y.toByte()
            if (j % 2 == 0 && index % 2 == 0) {
                yuv420sp[uvIndex++] = u.toByte()
                yuv420sp[uvIndex++] = v.toByte()
            }
            index++
        }
    }
}

actual suspend fun callGeminiApiPlatform(
    apiKey: String,
    prompt: String,
    systemInstruction: String
): String = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
    val url = java.net.URL("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=$apiKey")
    val connection = url.openConnection() as java.net.HttpURLConnection
    connection.requestMethod = "POST"
    connection.setRequestProperty("Content-Type", "application/json")
    connection.doOutput = true

    val fullPrompt = "$systemInstruction\n\nUser prompt: $prompt"
    val escapedPrompt = fullPrompt.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n")
    val jsonRequest = "{\"contents\": [{\"parts\": [{\"text\": \"$escapedPrompt\"}]}], \"generationConfig\": {\"responseMimeType\": \"application/json\"}}"

    connection.outputStream.use { os ->
        os.write(jsonRequest.toByteArray(Charsets.UTF_8))
    }

    val responseCode = connection.responseCode
    if (responseCode == java.net.HttpURLConnection.HTTP_OK) {
        val reader = java.io.BufferedReader(java.io.InputStreamReader(connection.inputStream))
        val response = java.lang.StringBuilder()
        var line: String?
        while (reader.readLine().also { line = it } != null) {
            response.append(line)
        }
        reader.close()
        response.toString()
    } else {
        val reader = java.io.BufferedReader(java.io.InputStreamReader(connection.errorStream ?: connection.inputStream))
        val errorResponse = java.lang.StringBuilder()
        var line: String?
        while (reader.readLine().also { line = it } != null) {
            errorResponse.append(line)
        }
        reader.close()
        throw RuntimeException("API error ($responseCode): $errorResponse")
    }
}

