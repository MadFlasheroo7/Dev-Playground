package pro.jayeshseth.bugrush.game.actors

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.actor.Actor
import com.pandulapeter.kubriko.actor.traits.Dynamic
import com.pandulapeter.kubriko.actor.traits.Overlay
import com.pandulapeter.kubriko.actor.traits.Unique
import pro.jayeshseth.bugrush.game.managers.ArenaManager
import pro.jayeshseth.bugrush.game.renderer.IsometricProjection

/**
 * Single actor responsible for rendering the entire isometric voxel arena
 * directly inside Kubriko's viewport draw pipeline as an Overlay.
 * Incorporates camera screen shake, physics debris, and floating combat text badges.
 */
class ArenaVoxelRendererActor(
    private val arenaManager: ArenaManager
) : Actor, Overlay, Dynamic, Unique {

    override val layerIndex: Int = 0
    override val overlayDrawingOrder: Float = 0f
    override val isAlwaysActive: Boolean get() = true

    override fun onAdded(kubriko: Kubriko) {}

    override fun update(deltaTimeInMilliseconds: Int) {
        // Keeps renderer active on every tick
    }

    override fun DrawScope.drawToViewport() {
        val camX = arenaManager.smoothCameraX
        val camY = arenaManager.smoothCameraY
        val zoom = arenaManager.smoothZoom
        val camAngle = arenaManager.currentCameraAngleRad
        val shakeX = arenaManager.shakeOffsetX
        val shakeY = arenaManager.shakeOffsetY
        val screenCenterX = size.width * 0.5f + shakeX
        val screenCenterY = size.height * 0.55f + shakeY

        // 0. Draw Star Wars / Deep Space Shader Background with Camera Parallax
        arenaManager.spaceBackground.draw(
            scope = this,
            cameraX = camX + shakeX,
            cameraY = camY + shakeY,
            isKernelPanic = arenaManager.bossKernelPanic.value,
            viewportWidth = size.width,
            viewportHeight = size.height
        )

        // 1. Draw all depth-sorted 3D Voxel Cuboids (World, Entities, Debris)
        val cuboids = arenaManager.getAllVoxelCuboids()
        for (i in cuboids.indices) {
            IsometricProjection.drawCuboid(
                scope = this,
                cuboid = cuboids[i],
                cameraCenterX = camX,
                cameraCenterY = camY,
                screenCenterX = screenCenterX,
                screenCenterY = screenCenterY,
                zoom = zoom,
                viewportWidth = size.width,
                viewportHeight = size.height,
                cameraAngleRad = camAngle
            )
        }

        // 2. Mouse Aiming Reticle
        if (arenaManager.isMouseAiming) {
            IsometricProjection.drawAimReticle(
                scope = this,
                worldX = arenaManager.aimWorldX,
                worldY = arenaManager.aimWorldY,
                cameraCenterX = camX,
                cameraCenterY = camY,
                screenCenterX = screenCenterX,
                screenCenterY = screenCenterY,
                zoom = zoom,
                cameraAngleRad = camAngle
            )
        }

        // 3. Floating Combat Numbers & Badges
        val textMeasurer = arenaManager.textMeasurer
        val textList = arenaManager.getFloatingTextsSnapshot()
        for (ft in textList) {
            val screenPos = IsometricProjection.project(
                worldX = ft.worldX,
                worldY = ft.worldY,
                worldZ = ft.worldZ,
                cameraCenterX = camX,
                cameraCenterY = camY,
                screenCenterX = screenCenterX,
                screenCenterY = screenCenterY,
                zoom = zoom,
                cameraAngleRad = camAngle
            )

            if (IsometricProjection.isInViewport(screenPos.x, screenPos.y, size.width, size.height)) {
                val alpha = (ft.lifeMs.toFloat() / ft.maxLifeMs.toFloat()).coerceIn(0f, 1f)
                val textStyle = TextStyle(
                    color = ft.color.copy(alpha = alpha),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black
                )
                val measured = textMeasurer?.measure(ft.text, textStyle)
                val textWidth = measured?.size?.width?.toFloat() ?: (ft.text.length * 7.5f)
                val badgeWidth = (textWidth + 18f).coerceAtLeast(36f)
                val badgeHeight = 22f
                val topLeft = Offset(screenPos.x - badgeWidth * 0.5f, screenPos.y - badgeHeight * 0.5f)

                // Background Pill
                drawRoundRect(
                    color = Color(0xDD0D1117).copy(alpha = alpha * 0.85f),
                    topLeft = topLeft,
                    size = Size(badgeWidth, badgeHeight),
                    cornerRadius = CornerRadius(6f, 6f),
                    style = Fill
                )
                // Border Stroke
                drawRoundRect(
                    color = ft.color.copy(alpha = alpha),
                    topLeft = topLeft,
                    size = Size(badgeWidth, badgeHeight),
                    cornerRadius = CornerRadius(6f, 6f),
                    style = Stroke(width = 1.2f)
                )

                // Text
                textMeasurer?.let { tm ->
                    drawText(
                        textMeasurer = tm,
                        text = ft.text,
                        topLeft = Offset(topLeft.x + (badgeWidth - textWidth) * 0.5f, topLeft.y + 2f),
                        style = textStyle
                    )
                }
            }
        }
    }
}
