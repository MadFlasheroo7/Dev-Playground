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
import kotlin.math.sin

class ServerRackVoxelActor(
    val posX: Float,
    val posY: Float,
    val posZ: Float = 0f
) : Actor, Visible, Dynamic {

    override val isAlwaysActive: Boolean get() = true
    override val isAlwaysVisible: Boolean get() = true

    val collisionRadius: Float = 26f
    private var blinkTimer: Float = 0f

    override val body = BoxBody(
        initialPosition = SceneOffset(posX.sceneUnit, posY.sceneUnit),
        initialSize = SceneSize(50f.sceneUnit, 50f.sceneUnit),
        initialPivot = SceneOffset(25f.sceneUnit, 25f.sceneUnit)
    )

    override val layerIndex: Int = 1
    override var drawingOrder: Float = -(posX + posY)

    override fun onAdded(kubriko: Kubriko) {}

    override fun update(deltaTimeInMilliseconds: Int) {
        blinkTimer += deltaTimeInMilliseconds * 0.005f
    }

    fun emitVoxelCuboids(): List<VoxelCuboid> {
        val rackDark = Color(0xFF1E2530)
        val rackAccent = Color(0xFF263238)
        val ledBlue = Color(0xFF00E5FF)
        val ledGreen = Color(0xFF00E676)
        val ledBlink = if (sin(blinkTimer * 4f) > 0f) Color(0xFFFFD600) else Color(0xFF263238)

        return listOf(
            // Shadow
            VoxelCuboid(posX, posY, 0f, 38f, 38f, 2f, Color(0x33000000)),
            // Base Server Chassis
            VoxelCuboid(posX, posY, 2f, 34f, 34f, 62f, rackDark),
            // Front Server Panels / Shelves
            VoxelCuboid(posX, posY - 2f, 14f, 30f, 6f, 10f, rackAccent),
            VoxelCuboid(posX, posY - 2f, 30f, 30f, 6f, 10f, rackAccent),
            VoxelCuboid(posX, posY - 2f, 46f, 30f, 6f, 10f, rackAccent),
            // LED Activity Lights
            VoxelCuboid(posX - 10f, posY - 4f, 20f, 4f, 2f, 4f, ledGreen),
            VoxelCuboid(posX - 2f, posY - 4f, 20f, 4f, 2f, 4f, ledBlue),
            VoxelCuboid(posX + 6f, posY - 4f, 20f, 4f, 2f, 4f, ledBlink),

            VoxelCuboid(posX - 10f, posY - 4f, 36f, 4f, 2f, 4f, ledBlue),
            VoxelCuboid(posX - 2f, posY - 4f, 36f, 4f, 2f, 4f, ledGreen),
            VoxelCuboid(posX + 6f, posY - 4f, 36f, 4f, 2f, 4f, ledBlink),

            VoxelCuboid(posX - 10f, posY - 4f, 52f, 4f, 2f, 4f, ledGreen),
            VoxelCuboid(posX + 6f, posY - 4f, 52f, 4f, 2f, 4f, ledBlue)
        ).let { list ->
            if (posZ != 0f) list.map { it.copy(z = it.z + posZ) } else list
        }
    }

    override fun DrawScope.draw() {}
}
