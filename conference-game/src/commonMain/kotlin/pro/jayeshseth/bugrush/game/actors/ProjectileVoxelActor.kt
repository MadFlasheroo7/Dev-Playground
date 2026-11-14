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

class ProjectileVoxelActor(
    var posX: Float,
    var posY: Float,
    val dirX: Float,
    val dirY: Float,
    val isPlayerProjectile: Boolean = true,
    val damage: Int = 1,
    val isSuperCharge: Boolean = false,
    val originX: Float = posX,
    val originY: Float = posY
) : Actor, Visible, Dynamic {

    override val isAlwaysActive: Boolean get() = true
    override val isAlwaysVisible: Boolean get() = true

    var isDestroyed: Boolean = false
    var isFrozen: Boolean = false
    private var lifeRemainingMs: Int = 2200
    private val speed: Float = if (isPlayerProjectile) 0.92f else 0.45f

    override val body = BoxBody(
        initialPosition = SceneOffset(posX.sceneUnit, posY.sceneUnit),
        initialSize = SceneSize(12f.sceneUnit, 12f.sceneUnit),
        initialPivot = SceneOffset(6f.sceneUnit, 6f.sceneUnit)
    )

    override val layerIndex: Int = 1
    override var drawingOrder: Float = -(posX + posY)

    override fun onAdded(kubriko: Kubriko) {}

    override fun update(deltaTimeInMilliseconds: Int) {
        if (isDestroyed || isFrozen) return

        lifeRemainingMs -= deltaTimeInMilliseconds
        if (lifeRemainingMs <= 0) {
            isDestroyed = true
            return
        }

        posX += dirX * speed * deltaTimeInMilliseconds
        posY += dirY * speed * deltaTimeInMilliseconds

        if (posX < 60f || posX > 3740f || posY < 60f || posY > 3740f) {
            isDestroyed = true
        }

        body.position = SceneOffset(posX.sceneUnit, posY.sceneUnit)
        drawingOrder = -(posX + posY)
    }

    fun emitVoxelCuboids(): List<VoxelCuboid> {
        val bulletColor = when {
            !isPlayerProjectile -> Color(0xFFFF3D00) // Enemy laser red
            isSuperCharge -> Color(0xFFFFD600) // Golden frenzy laser
            else -> Color(0xFF00E5FF) // Kotlin Cyan energy bolt
        }
        val size = if (isSuperCharge) 14f else 9f
        return listOf(
            VoxelCuboid(
                x = posX,
                y = posY,
                z = 18f,
                sizeX = size,
                sizeY = size,
                sizeZ = size,
                baseColor = bulletColor
            )
        )
    }

    override fun DrawScope.draw() {}
}
