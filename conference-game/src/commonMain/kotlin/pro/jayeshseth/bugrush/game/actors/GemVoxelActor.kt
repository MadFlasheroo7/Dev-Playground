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
import kotlin.math.PI
import kotlin.math.sin
import kotlin.math.sqrt

class GemVoxelActor(
    var posX: Float,
    var posY: Float,
    var posZ: Float = 0f,
    val pointValue: Int = 100
) : Actor, Visible, Dynamic {

    override val isAlwaysActive: Boolean get() = true
    override val isAlwaysVisible: Boolean get() = true

    private var animTime: Float = (posX + posY) % (2f * PI.toFloat())

    override val body = BoxBody(
        initialPosition = SceneOffset(posX.sceneUnit, posY.sceneUnit),
        initialSize = SceneSize(24f.sceneUnit, 24f.sceneUnit),
        initialPivot = SceneOffset(12f.sceneUnit, 12f.sceneUnit)
    )

    override val layerIndex: Int = 1
    override var drawingOrder: Float = 0f

    override fun onAdded(kubriko: Kubriko) {}

    override fun update(deltaTimeInMilliseconds: Int) {
        animTime += deltaTimeInMilliseconds * 0.003f
        if (animTime > 2f * PI) animTime -= (2f * PI.toFloat())

        body.position = SceneOffset(posX.sceneUnit, posY.sceneUnit)
        drawingOrder = -(posX + posY)
    }

    fun pullTowards(targetX: Float, targetY: Float, speed: Float) {
        val dx = targetX - posX
        val dy = targetY - posY
        val dist = sqrt(dx * dx + dy * dy)
        if (dist > 5f) {
            posX += (dx / dist) * speed
            posY += (dy / dist) * speed
        }
    }

    fun emitVoxelCuboids(): List<VoxelCuboid> {
        val hoverZ = 12f + sin(animTime * 3f) * 4f
        val gemColor = Color(0xFF00E5FF) // Kotlin Cyan
        val coreColor = Color(0xFF7F52FF) // Kotlin Purple

        return listOf(
            // Shadow
            VoxelCuboid(posX, posY, posZ + 0f, 16f, 16f, 1.5f, Color(0x22000000)),
            // Gem Core
            VoxelCuboid(posX, posY, posZ + hoverZ, 14f, 14f, 14f, gemColor),
            // Floating accent ring
            VoxelCuboid(posX, posY, posZ + hoverZ + 3f, 18f, 18f, 6f, coreColor)
        )
    }

    override fun DrawScope.draw() {}
}
