package pro.jayeshseth.devcontrols.composevideo

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier

@Composable
actual fun ComposeVideo(
    controller: ComposeVideoController,
    modifier: Modifier,
    content: @Composable ComposeVideoScope.() -> Unit,
) {
    Box(modifier = modifier) {
        ComposeVideoScope(
            progress = controller.currentProgress,
            timeMs = controller.currentTimeMs,
            fps = controller.fps,
            durationMs = controller.durationMs,
        ).content()
    }

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
}
