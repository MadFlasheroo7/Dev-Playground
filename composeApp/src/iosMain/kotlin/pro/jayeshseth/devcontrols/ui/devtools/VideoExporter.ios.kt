package pro.jayeshseth.devcontrols.ui.devtools

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.drawscope.DrawScope

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
    onError("Video export is not supported on iOS at this time.")
}

actual suspend fun callGeminiApiPlatform(
    apiKey: String,
    prompt: String,
    systemInstruction: String
): String {
    throw RuntimeException("AI API is not supported on iOS at this time.")
}
