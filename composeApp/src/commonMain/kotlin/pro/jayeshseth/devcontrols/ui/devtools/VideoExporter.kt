package pro.jayeshseth.devcontrols.ui.devtools

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.drawscope.DrawScope

enum class ExportType {
    VIDEO,   // MP4 on Android, Animated GIF on Desktop
    PNG_ZIP  // ZIP archive with PNG frame files
}

expect fun exportAnimationToVideo(
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
)

@Composable
expect fun getPlatformContext(): Any?

expect suspend fun callGeminiApiPlatform(
    apiKey: String,
    prompt: String,
    systemInstruction: String
): String
