package pro.jayeshseth.bugrush.game.actors

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.actor.Actor
import com.pandulapeter.kubriko.actor.body.BoxBody
import com.pandulapeter.kubriko.actor.traits.Dynamic
import com.pandulapeter.kubriko.actor.traits.Visible
import pro.jayeshseth.bugrush.game.renderer.VoxelCuboid
import com.pandulapeter.kubriko.helpers.extensions.sceneUnit
import com.pandulapeter.kubriko.types.SceneOffset
import com.pandulapeter.kubriko.types.SceneSize
import kotlin.math.cos
import kotlin.math.sin

class TriviaShardVoxelActor(
    var posX: Float,
    var posY: Float,
    val tier: Int = 1
) : Actor, Visible, Dynamic {

    override val isAlwaysActive: Boolean get() = true
    override val isAlwaysVisible: Boolean get() = true

    var isCollected: Boolean = false
    private var animTime: Float = 0f

    override val body = BoxBody(
        initialPosition = SceneOffset(posX.sceneUnit, posY.sceneUnit),
        initialSize = SceneSize(28f.sceneUnit, 28f.sceneUnit),
        initialPivot = SceneOffset(14f.sceneUnit, 14f.sceneUnit)
    )

    override val layerIndex: Int = 1
    override var drawingOrder: Float = -(posX + posY)

    override fun onAdded(kubriko: Kubriko) {}

    override fun update(deltaTimeInMilliseconds: Int) {
        if (isCollected) return
        animTime += deltaTimeInMilliseconds * 0.0035f
    }

    fun emitVoxelCuboids(): List<VoxelCuboid> {
        if (isCollected) return emptyList()

        val hoverZ = 12f + sin(animTime * 3f) * 4f
        val shardColor = when (tier) {
            3 -> Color(0xFFFF9100) // Fierce Orange / Tier 3
            2 -> Color(0xFF00E5FF) // Cyber Cyan / Tier 2
            else -> Color(0xFFFFD600) // Electric Gold / Tier 1
        }
        val innerGlow = Color(0xFFFFFFFF)

        val rotOffset = cos(animTime * 2.5f) * 4f

        return listOf(
            // Holographic floor beacon shadow
            VoxelCuboid(posX, posY, 0f, 22f, 22f, 2f, Color(0x3300E5FF)),
            // Outer Levitating Data Cube
            VoxelCuboid(posX + rotOffset, posY - rotOffset, hoverZ, 16f, 16f, 16f, shardColor),
            // Inner Core Data Chip
            VoxelCuboid(posX, posY, hoverZ + 3f, 8f, 8f, 10f, innerGlow),
            // Surrounding Floating Data Bits
            VoxelCuboid(posX - 10f, posY + 10f, hoverZ - 2f, 4f, 4f, 4f, shardColor),
            VoxelCuboid(posX + 10f, posY - 10f, hoverZ + 6f, 4f, 4f, 4f, shardColor)
        )
    }

    override fun DrawScope.draw() {}
}
