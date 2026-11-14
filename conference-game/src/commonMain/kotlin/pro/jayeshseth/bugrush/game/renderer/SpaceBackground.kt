package pro.jayeshseth.bugrush.game.renderer

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope

interface SpaceBackground {
    fun draw(
        scope: DrawScope,
        cameraX: Float,
        cameraY: Float,
        isKernelPanic: Boolean,
        viewportWidth: Float,
        viewportHeight: Float
    )
}

object NoOpSpaceBackground : SpaceBackground {
    override fun draw(
        scope: DrawScope,
        cameraX: Float,
        cameraY: Float,
        isKernelPanic: Boolean,
        viewportWidth: Float,
        viewportHeight: Float
    ) {
        scope.drawRect(Color(0xFF0A0E14))
    }
}
