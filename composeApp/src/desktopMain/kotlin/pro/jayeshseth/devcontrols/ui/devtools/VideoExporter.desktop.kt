package pro.jayeshseth.devcontrols.ui.devtools

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.asComposeCanvas
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.jetbrains.skia.Surface
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import javax.imageio.IIOImage
import javax.imageio.ImageIO
import javax.imageio.ImageTypeSpecifier
import javax.imageio.metadata.IIOMetadataNode

@Composable
actual fun getPlatformContext(): Any? = null

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
    CoroutineScope(Dispatchers.Default).launch {
        try {
            val userHome = System.getProperty("user.home")
            val downloadsDir = File(userHome, "Downloads")
            val destDir = if (downloadsDir.exists()) downloadsDir else File(userHome)

            if (exportType == ExportType.VIDEO) {
                // Export as Animated GIF on Desktop
                val destFile = File(destDir, "$fileName.gif")
                val fos = FileOutputStream(destFile)

                val writer = ImageIO.getImageWritersByFormatName("gif").next()
                val writeParam = writer.defaultWriteParam

                val surface = Surface.makeRasterN32Premul(width, height)
                val skiaCanvas = surface.canvas
                val composeCanvas = skiaCanvas.asComposeCanvas()
                val drawScope = CanvasDrawScope()

                // Render first frame to initialize metadata
                surface.canvas.clear(0)
                drawScope.draw(
                    density = Density(1f),
                    layoutDirection = LayoutDirection.Ltr,
                    canvas = composeCanvas,
                    size = androidx.compose.ui.geometry.Size(width.toFloat(), height.toFloat())
                ) {
                    renderFrame(0)
                }

                val firstImage = surface.makeImageSnapshot()
                val firstPng = firstImage.encodeToData()?.bytes ?: throw RuntimeException("Failed to encode Skia image")
                val firstBuf = ImageIO.read(ByteArrayInputStream(firstPng))

                val imageType = ImageTypeSpecifier.createFromRenderedImage(firstBuf)
                val metadata = writer.getDefaultImageMetadata(imageType, writeParam)

                try {
                    // Configure GraphicControlExtension & Loop (Netscape 2.0)
                    val metaFormatName = metadata.nativeMetadataFormatName
                    val root = metadata.getAsTree(metaFormatName) as IIOMetadataNode

                    var gceNode = root.getElementsByTagName("GraphicControlExtension").item(0) as? IIOMetadataNode
                    if (gceNode == null) {
                        gceNode = IIOMetadataNode("GraphicControlExtension")
                        root.appendChild(gceNode)
                    }
                    gceNode.setAttribute("disposalMethod", "restoreToBackgroundColor")
                    gceNode.setAttribute("userInputFlag", "FALSE")
                    gceNode.setAttribute("transparentColorFlag", "FALSE")
                    gceNode.setAttribute("delayTime", (100 / fps).toString())
                    gceNode.setAttribute("transparentColorIndex", "0")

                    var appExtensionsNode = root.getElementsByTagName("ApplicationExtensions").item(0) as? IIOMetadataNode
                    if (appExtensionsNode == null) {
                        appExtensionsNode = IIOMetadataNode("ApplicationExtensions")
                        root.appendChild(appExtensionsNode)
                    }
                    val appExtensionNode = IIOMetadataNode("ApplicationExtension").apply {
                        setAttribute("applicationID", "NETSCAPE")
                        setAttribute("authenticationCode", "2.0")
                        userObject = byteArrayOf(0x1, 0x0, 0x0) // Loop count = 0 (infinite)
                    }
                    appExtensionsNode.appendChild(appExtensionNode)

                    metadata.setFromTree(metaFormatName, root)
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                val ios = ImageIO.createImageOutputStream(fos)
                writer.output = ios
                writer.prepareWriteSequence(null)

                // Write first frame
                writer.writeToSequence(IIOImage(firstBuf, null, metadata), writeParam)
                onProgress(1f / frameCount.toFloat())

                // Render and write subsequent frames
                for (i in 1 until frameCount) {
                    skiaCanvas.clear(0)
                    drawScope.draw(
                        density = Density(1f),
                        layoutDirection = LayoutDirection.Ltr,
                        canvas = composeCanvas,
                        size = androidx.compose.ui.geometry.Size(width.toFloat(), height.toFloat())
                    ) {
                        renderFrame(i)
                    }
                    val frameImage = surface.makeImageSnapshot()
                    val framePng = frameImage.encodeToData()?.bytes ?: throw RuntimeException("Failed to encode frame $i")
                    val frameBuf = ImageIO.read(ByteArrayInputStream(framePng))

                    writer.writeToSequence(IIOImage(frameBuf, null, metadata), writeParam)
                    onProgress((i + 1).toFloat() / frameCount.toFloat())
                }

                writer.endWriteSequence()
                ios.close()
                fos.close()

                onComplete(destFile.absolutePath)
            } else {
                // Export as PNG Zip on Desktop
                val destFile = File(destDir, "$fileName.zip")
                val fos = FileOutputStream(destFile)
                ZipOutputStream(fos).use { zos ->
                    val surface = Surface.makeRasterN32Premul(width, height)
                    val skiaCanvas = surface.canvas
                    val composeCanvas = skiaCanvas.asComposeCanvas()
                    val drawScope = CanvasDrawScope()

                    for (i in 0 until frameCount) {
                        skiaCanvas.clear(0)
                        drawScope.draw(
                            density = Density(1f),
                            layoutDirection = LayoutDirection.Ltr,
                            canvas = composeCanvas,
                            size = androidx.compose.ui.geometry.Size(width.toFloat(), height.toFloat())
                        ) {
                            renderFrame(i)
                        }
                        val frameImage = surface.makeImageSnapshot()
                        val framePng = frameImage.encodeToData()?.bytes ?: throw RuntimeException("Failed to encode frame $i")

                        val entry = ZipEntry("frame_${String.format("%03d", i)}.png")
                        zos.putNextEntry(entry)
                        zos.write(framePng)
                        zos.closeEntry()

                        onProgress((i + 1).toFloat() / frameCount.toFloat())
                    }
                }
                fos.close()
                onComplete(destFile.absolutePath)
            }
        } catch (e: Exception) {
            onError(e.message ?: "Unknown Desktop encoding error")
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

