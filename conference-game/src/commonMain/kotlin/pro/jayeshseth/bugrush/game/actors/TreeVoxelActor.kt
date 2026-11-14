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

class TreeVoxelActor(
    val posX: Float,
    val posY: Float,
    val posZ: Float = 0f
) : Actor, Visible, Dynamic {

    override val isAlwaysActive: Boolean get() = true
    override val isAlwaysVisible: Boolean get() = true

    val collisionRadius: Float = 26f

    override val body = BoxBody(
        initialPosition = SceneOffset(posX.sceneUnit, posY.sceneUnit),
        initialSize = SceneSize(80f.sceneUnit, 80f.sceneUnit),
        initialPivot = SceneOffset(40f.sceneUnit, 40f.sceneUnit)
    )

    override val layerIndex: Int = 1
    override var drawingOrder: Float = -(posX + posY)

    override fun onAdded(kubriko: Kubriko) {}

    override fun update(deltaTimeInMilliseconds: Int) {
        // Static scenery
    }

    fun emitVoxelCuboids(): List<VoxelCuboid> {
        val cuboids = mutableListOf<VoxelCuboid>()

        // 1. Root Connector Pads (PCB solder pads on ground)
        cuboids.add(VoxelCuboid(posX - 18f, posY, 0f, 14f, 10f, 2f, Color(0xFF00B0FF)))
        cuboids.add(VoxelCuboid(posX + 18f, posY, 0f, 14f, 10f, 2f, Color(0xFF00B0FF)))
        cuboids.add(VoxelCuboid(posX, posY - 18f, 0f, 10f, 14f, 2f, Color(0xFF00E5FF)))
        cuboids.add(VoxelCuboid(posX, posY + 18f, 0f, 10f, 14f, 2f, Color(0xFF00E5FF)))

        // 2. Main Cyber Pylon Trunk (Dark Titanium Silicon Column)
        cuboids.add(
            VoxelCuboid(
                x = posX,
                y = posY,
                z = 0f,
                sizeX = 24f,
                sizeY = 24f,
                sizeZ = 46f,
                baseColor = Color(0xFF1B2430)
            )
        )

        // 3. Glowing Vertical Bus Lines on Trunk Faces
        cuboids.add(VoxelCuboid(posX, posY - 11f, 4f, 6f, 3f, 38f, Color(0xFF00E5FF)))
        cuboids.add(VoxelCuboid(posX, posY + 11f, 4f, 6f, 3f, 38f, Color(0xFF00E5FF)))
        cuboids.add(VoxelCuboid(posX - 11f, posY, 4f, 3f, 6f, 38f, Color(0xFF7C4DFF)))
        cuboids.add(VoxelCuboid(posX + 11f, posY, 4f, 3f, 6f, 38f, Color(0xFF7C4DFF)))

        // 4. Lower Tier: Geometric Quantum Capacitor Slab (Deep Cyber Slate with Cyan Trim)
        cuboids.add(
            VoxelCuboid(
                x = posX,
                y = posY,
                z = 42f,
                sizeX = 76f,
                sizeY = 76f,
                sizeZ = 20f,
                baseColor = Color(0xFF101C2B)
            )
        )
        // Neon corner nodes on lower tier
        cuboids.add(VoxelCuboid(posX - 32f, posY - 32f, 46f, 10f, 10f, 12f, Color(0xFF00E5FF)))
        cuboids.add(VoxelCuboid(posX + 32f, posY + 32f, 46f, 10f, 10f, 12f, Color(0xFF00E5FF)))
        cuboids.add(VoxelCuboid(posX + 32f, posY - 32f, 46f, 10f, 10f, 12f, Color(0xFF7C4DFF)))
        cuboids.add(VoxelCuboid(posX - 32f, posY + 32f, 46f, 10f, 10f, 12f, Color(0xFF7C4DFF)))

        // 5. Middle Tier: Matrix Emerald Heatsink Canopy
        cuboids.add(
            VoxelCuboid(
                x = posX,
                y = posY,
                z = 62f,
                sizeX = 52f,
                sizeY = 52f,
                sizeZ = 20f,
                baseColor = Color(0xFF00E676)
            )
        )
        // Dark heatsink core inset
        cuboids.add(
            VoxelCuboid(
                x = posX,
                y = posY,
                z = 64f,
                sizeX = 36f,
                sizeY = 36f,
                sizeZ = 18f,
                baseColor = Color(0xFF0B301B)
            )
        )

        // 6. Crown Tier: Floating Holographic Prism
        cuboids.add(
            VoxelCuboid(
                x = posX,
                y = posY,
                z = 82f,
                sizeX = 30f,
                sizeY = 30f,
                sizeZ = 16f,
                baseColor = Color(0xFF7F52FF)
            )
        )

        // 7. Apex Diode Spire Beacon (Illuminated Gold/Amber Core)
        cuboids.add(
            VoxelCuboid(
                x = posX,
                y = posY,
                z = 98f,
                sizeX = 12f,
                sizeY = 12f,
                sizeZ = 14f,
                baseColor = Color(0xFFFFEA00)
            )
        )

        if (posZ != 0f) {
            return cuboids.map { it.copy(z = it.z + posZ) }
        }

        return cuboids
    }

    override fun DrawScope.draw() {}
}
