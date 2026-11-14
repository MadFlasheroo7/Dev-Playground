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

class DataRelayTowerActor(
    val posX: Float,
    val posY: Float,
    val posZ: Float = 0f
) : Actor, Visible, Dynamic {

    override val isAlwaysActive: Boolean get() = true
    override val isAlwaysVisible: Boolean get() = true

    val collisionRadius: Float = 24f
    var beaconTimer: Float = 0f

    override val body = BoxBody(
        initialPosition = SceneOffset(posX.sceneUnit, posY.sceneUnit),
        initialSize = SceneSize(60f.sceneUnit, 60f.sceneUnit),
        initialPivot = SceneOffset(30f.sceneUnit, 30f.sceneUnit)
    )

    override val layerIndex: Int = 1
    override var drawingOrder: Float = -(posX + posY)

    override fun onAdded(kubriko: Kubriko) {}

    override fun update(deltaTimeInMilliseconds: Int) {
        beaconTimer += deltaTimeInMilliseconds * 0.005f
    }

    fun emitVoxelCuboids(): List<VoxelCuboid> {
        val darkChassis = Color(0xFF151C24)
        val midChassis = Color(0xFF1E2834)
        val metalAccent = Color(0xFF2E3D4E)
        val conduitCyan = Color(0xFF00E5FF)
        val conduitAmber = Color(0xFFFFD600)

        // Blinking beacon lamp on top
        val isBlinkOn = sin(beaconTimer * 6f) > 0.15f
        val beaconColor = if (isBlinkOn) Color(0xFFFF1744) else Color(0x44FF1744)
        val beaconCoreColor = if (isBlinkOn) Color(0xFFFF8A80) else Color(0x33FF8A80)

        val cuboids = mutableListOf<VoxelCuboid>()

        // 1. Shadow / Base Foundation
        cuboids.add(VoxelCuboid(posX, posY, 0f, 44f, 44f, 2f, Color(0x33000000)))

        // 2. Heavy Reinforced Concrete/Steel Footings (4 corner pylons)
        cuboids.add(VoxelCuboid(posX - 13f, posY - 13f, 0f, 10f, 10f, 6f, metalAccent))
        cuboids.add(VoxelCuboid(posX + 13f, posY - 13f, 0f, 10f, 10f, 6f, metalAccent))
        cuboids.add(VoxelCuboid(posX - 13f, posY + 13f, 0f, 10f, 10f, 6f, metalAccent))
        cuboids.add(VoxelCuboid(posX + 13f, posY + 13f, 0f, 10f, 10f, 6f, metalAccent))

        // 3. Lower Chassis Base (Width: 32x32, Height: 30)
        cuboids.add(VoxelCuboid(posX, posY, 4f, 32f, 32f, 30f, darkChassis))

        // 4. Lower Illuminated Conduit Ribs along the 4 vertical faces
        cuboids.add(VoxelCuboid(posX, posY - 16f, 6f, 14f, 2f, 24f, conduitCyan))
        cuboids.add(VoxelCuboid(posX, posY + 16f, 6f, 14f, 2f, 24f, conduitCyan))
        cuboids.add(VoxelCuboid(posX - 16f, posY, 6f, 2f, 14f, 24f, conduitCyan))
        cuboids.add(VoxelCuboid(posX + 16f, posY, 6f, 2f, 14f, 24f, conduitCyan))

        // 5. Mid-level Collar / Service Gantry Platform
        cuboids.add(VoxelCuboid(posX, posY, 34f, 36f, 36f, 4f, metalAccent))

        // 6. Mid Section Tower Column (Tapered: 24x24, Height: 36)
        cuboids.add(VoxelCuboid(posX, posY, 38f, 24f, 24f, 36f, midChassis))

        // 7. Mid Illuminated Conduit Ribs & Power Core Vent
        cuboids.add(VoxelCuboid(posX, posY - 12f, 42f, 8f, 2f, 26f, conduitAmber))
        cuboids.add(VoxelCuboid(posX, posY + 12f, 42f, 8f, 2f, 26f, conduitAmber))
        cuboids.add(VoxelCuboid(posX - 12f, posY, 42f, 2f, 8f, 26f, conduitCyan))
        cuboids.add(VoxelCuboid(posX + 12f, posY, 42f, 2f, 8f, 26f, conduitCyan))

        // 8. Microwave Data Transceiver Dish / Array Ring (Width: 34x34, Height: 6)
        cuboids.add(VoxelCuboid(posX, posY, 74f, 34f, 34f, 6f, metalAccent))
        cuboids.add(VoxelCuboid(posX, posY, 76f, 20f, 20f, 6f, darkChassis))

        // 9. Upper Spire Column (Width: 14x14, Height: 20, Z: 80 to 100)
        cuboids.add(VoxelCuboid(posX, posY, 80f, 14f, 14f, 20f, midChassis))
        cuboids.add(VoxelCuboid(posX, posY, 82f, 16f, 4f, 14f, conduitCyan))
        cuboids.add(VoxelCuboid(posX, posY, 82f, 4f, 16f, 14f, conduitCyan))

        // 10. Antenna Needle Mast (Width: 6x6, Height: 10, Z: 100 to 110)
        cuboids.add(VoxelCuboid(posX, posY, 100f, 6f, 6f, 10f, metalAccent))

        // 11. Animated Blinking Beacon Lamp Summit (Total height ~118 units, within 90-120 units)
        cuboids.add(VoxelCuboid(posX, posY, 110f, 10f, 10f, 8f, beaconColor))
        cuboids.add(VoxelCuboid(posX, posY, 112f, 4f, 4f, 4f, beaconCoreColor))

        if (posZ != 0f) {
            return cuboids.map { it.copy(z = it.z + posZ) }
        }
        return cuboids
    }

    override fun DrawScope.draw() {}
}
