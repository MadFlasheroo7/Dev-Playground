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

class TechBarricadeActor(
    val posX: Float,
    val posY: Float,
    val posZ: Float = 0f,
    val isHorizontal: Boolean = true
) : Actor, Visible, Dynamic {

    override val isAlwaysActive: Boolean get() = true
    override val isAlwaysVisible: Boolean get() = true

    val collisionRadius: Float = 20f
    var ledTimer: Float = 0f

    override val body = BoxBody(
        initialPosition = SceneOffset(posX.sceneUnit, posY.sceneUnit),
        initialSize = SceneSize(50f.sceneUnit, 50f.sceneUnit),
        initialPivot = SceneOffset(25f.sceneUnit, 25f.sceneUnit)
    )

    override val layerIndex: Int = 1
    override var drawingOrder: Float = -(posX + posY)

    override fun onAdded(kubriko: Kubriko) {}

    override fun update(deltaTimeInMilliseconds: Int) {
        ledTimer += deltaTimeInMilliseconds * 0.005f
    }

    fun emitVoxelCuboids(): List<VoxelCuboid> {
        val baseColor = Color(0xFF1B232C)
        val armorColor = Color(0xFF283542)
        val capColor = Color(0xFF37474F)
        val hazardYellow = Color(0xFFFFD600)
        val hazardDark = Color(0xFF212121)

        val ledPulse = (sin(ledTimer * 5f) * 0.5f + 0.5f)
        val ledCyan = Color(0xFF00E5FF).copy(alpha = 0.4f + 0.6f * ledPulse)
        val ledAmber = Color(0xFFFF9100).copy(alpha = 0.4f + 0.6f * ledPulse)

        val cuboids = mutableListOf<VoxelCuboid>()

        if (isHorizontal) {
            // Shadow
            cuboids.add(VoxelCuboid(posX, posY, 0f, 52f, 20f, 2f, Color(0x33000000)))

            // Base Chassis (48 x 16 x 6)
            cuboids.add(VoxelCuboid(posX, posY, 0f, 48f, 16f, 6f, baseColor))

            // Sloped Armored Plating (44 x 14 x 8, Z: 6 to 14)
            cuboids.add(VoxelCuboid(posX, posY, 6f, 44f, 14f, 8f, armorColor))

            // Top Reinforced Rail / Cap (40 x 10 x 4, Z: 14 to 18)
            cuboids.add(VoxelCuboid(posX, posY, 14f, 40f, 10f, 4f, capColor))

            // Hazard Stripes across the front & back faces (Z: 6 to 12)
            val stripeWidth = 8f
            for (i in 0 until 4) {
                val stripeX = posX - 12f + i * stripeWidth
                val stripeCol = if (i % 2 == 0) hazardYellow else hazardDark
                cuboids.add(VoxelCuboid(stripeX, posY - 7f, 6f, stripeWidth - 1f, 2f, 6f, stripeCol))
                cuboids.add(VoxelCuboid(stripeX, posY + 7f, 6f, stripeWidth - 1f, 2f, 6f, stripeCol))
            }

            // Corner Warning LEDs (at the 4 corners, Z: 14 to 18)
            cuboids.add(VoxelCuboid(posX - 21f, posY - 6f, 14f, 4f, 3f, 4f, ledCyan))
            cuboids.add(VoxelCuboid(posX + 21f, posY - 6f, 14f, 4f, 3f, 4f, ledCyan))
            cuboids.add(VoxelCuboid(posX - 21f, posY + 6f, 14f, 4f, 3f, 4f, ledAmber))
            cuboids.add(VoxelCuboid(posX + 21f, posY + 6f, 14f, 4f, 3f, 4f, ledAmber))
        } else {
            // Vertical orientation (Width: 16, Depth: 48, Height: 18)
            // Shadow
            cuboids.add(VoxelCuboid(posX, posY, 0f, 20f, 52f, 2f, Color(0x33000000)))

            // Base Chassis (16 x 48 x 6)
            cuboids.add(VoxelCuboid(posX, posY, 0f, 16f, 48f, 6f, baseColor))

            // Sloped Armored Plating (14 x 44 x 8, Z: 6 to 14)
            cuboids.add(VoxelCuboid(posX, posY, 6f, 14f, 44f, 8f, armorColor))

            // Top Reinforced Rail / Cap (10 x 40 x 4, Z: 14 to 18)
            cuboids.add(VoxelCuboid(posX, posY, 14f, 10f, 40f, 4f, capColor))

            // Hazard Stripes across the side faces
            val stripeHeight = 8f
            for (i in 0 until 4) {
                val stripeY = posY - 12f + i * stripeHeight
                val stripeCol = if (i % 2 == 0) hazardYellow else hazardDark
                cuboids.add(VoxelCuboid(posX - 7f, stripeY, 6f, 2f, stripeHeight - 1f, 6f, stripeCol))
                cuboids.add(VoxelCuboid(posX + 7f, stripeY, 6f, 2f, stripeHeight - 1f, 6f, stripeCol))
            }

            // Corner Warning LEDs
            cuboids.add(VoxelCuboid(posX - 6f, posY - 21f, 14f, 3f, 4f, 4f, ledCyan))
            cuboids.add(VoxelCuboid(posX - 6f, posY + 21f, 14f, 3f, 4f, 4f, ledCyan))
            cuboids.add(VoxelCuboid(posX + 6f, posY - 21f, 14f, 3f, 4f, 4f, ledAmber))
            cuboids.add(VoxelCuboid(posX + 6f, posY + 21f, 14f, 3f, 4f, 4f, ledAmber))
        }

        if (posZ != 0f) {
            return cuboids.map { it.copy(z = it.z + posZ) }
        }
        return cuboids
    }

    override fun DrawScope.draw() {}
}
