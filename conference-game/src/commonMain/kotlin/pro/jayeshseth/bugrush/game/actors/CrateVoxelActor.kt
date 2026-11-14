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

class CrateVoxelActor(
    val posX: Float,
    val posY: Float,
    val posZ: Float = 0f,
    val containsTriviaShard: Boolean = false
) : Actor, Visible, Dynamic {

    override val isAlwaysActive: Boolean get() = true
    override val isAlwaysVisible: Boolean get() = true

    var isDestroyed: Boolean = false
    var hp: Int = 2
    val collisionRadius: Float = 22f

    fun takeDamage(amount: Int): Boolean {
        hp -= amount
        if (hp <= 0) {
            isDestroyed = true
        }
        return isDestroyed
    }

    override val body = BoxBody(
        initialPosition = SceneOffset(posX.sceneUnit, posY.sceneUnit),
        initialSize = SceneSize(36f.sceneUnit, 36f.sceneUnit),
        initialPivot = SceneOffset(18f.sceneUnit, 18f.sceneUnit)
    )

    override val layerIndex: Int = 1
    override var drawingOrder: Float = -(posX + posY)

    override fun onAdded(kubriko: Kubriko) {}

    override fun update(deltaTimeInMilliseconds: Int) {
        // Static obstacle
    }

    fun emitVoxelCuboids(): List<VoxelCuboid> {
        if (isDestroyed) return emptyList()

        val woodColor = Color(0xFF8D6E63) // Warm Oak Wood
        val metalColor = if (containsTriviaShard) Color(0xFF00E5FF) else Color(0xFF5D4037) // Cyan trim if holding shard

        return listOf(
            // Shadow
            VoxelCuboid(posX, posY, 0f, 32f, 32f, 2f, Color(0x33000000)),
            // Base Crate Body
            VoxelCuboid(posX, posY, 2f, 30f, 30f, 28f, woodColor),
            // Metal Corner Trim / Banding
            VoxelCuboid(posX, posY, 14f, 32f, 32f, 4f, metalColor),
            VoxelCuboid(posX, posY, 28f, 31f, 31f, 3f, metalColor),
            // Center Logo / Kotlin Indicator
            VoxelCuboid(posX, posY, 30f, 10f, 10f, 2f, if (containsTriviaShard) Color(0xFFFFD600) else Color(0xFFB0BEC5))
        ).let { list ->
            if (posZ != 0f) list.map { it.copy(z = it.z + posZ) } else list
        }
    }

    override fun DrawScope.draw() {}
}
