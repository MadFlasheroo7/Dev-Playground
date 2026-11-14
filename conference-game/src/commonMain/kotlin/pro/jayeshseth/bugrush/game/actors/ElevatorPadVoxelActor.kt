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
import kotlin.math.cos
import kotlin.math.sin

/**
 * Holographic Extraction / Elevator Pad that activates when the floor's objective is cleared.
 * Stepping onto the active pad ascends the player to the next floor.
 */
class ElevatorPadVoxelActor(
    val posX: Float,
    val posY: Float,
    val posZ: Float = 0f,
    var isActive: Boolean = false,
    var onStepOnElevator: () -> Unit = {}
) : Actor, Visible, Dynamic {

    override val isAlwaysActive: Boolean get() = true
    override val isAlwaysVisible: Boolean get() = true

    override val body = BoxBody(
        initialPosition = SceneOffset(posX.sceneUnit, posY.sceneUnit),
        initialSize = SceneSize(64f.sceneUnit, 64f.sceneUnit),
        initialPivot = SceneOffset(32f.sceneUnit, 32f.sceneUnit)
    )

    override val layerIndex: Int = 1
    override var drawingOrder: Float = -(posX + posY)

    private var animTime: Float = 0f
    private var hasTriggered: Boolean = false
    val activationRadius: Float = 38f

    override fun onAdded(kubriko: Kubriko) {}

    override fun update(deltaTimeInMilliseconds: Int) {
        animTime += deltaTimeInMilliseconds * 0.004f
        if (animTime > 2f * PI) animTime -= (2f * PI.toFloat())
    }

    fun checkPlayerProximity(playerX: Float, playerY: Float) {
        if (!isActive || hasTriggered) return
        val dx = playerX - posX
        val dy = playerY - posY
        val distSq = dx * dx + dy * dy
        if (distSq <= activationRadius * activationRadius) {
            hasTriggered = true
            onStepOnElevator()
        }
    }

    fun emitVoxelCuboids(): List<VoxelCuboid> {
        val cuboids = mutableListOf<VoxelCuboid>()
        val pulse = (sin(animTime * 3f) + 1f) * 0.5f

        val ringColor = if (isActive) {
            Color(0xFF00E5FF).copy(alpha = 0.6f + 0.4f * pulse)
        } else {
            Color(0xFF37474F).copy(alpha = 0.5f)
        }

        val coreColor = if (isActive) {
            Color(0xFF00E676)
        } else {
            Color(0xFF1E2838)
        }

        // 1. Base Platform Pad (Dark Hexagon/Octagon Pad)
        cuboids.add(VoxelCuboid(posX, posY, posZ + 0f, 60f, 60f, 4f, Color(0xFF101820), isFloor = true))

        // 2. Outer Warning Border
        cuboids.add(VoxelCuboid(posX, posY, posZ + 2f, 54f, 54f, 3f, ringColor))

        // 3. Central Teleport Core
        cuboids.add(VoxelCuboid(posX, posY, posZ + 4f, 36f, 36f, 3f, coreColor))

        // 4. Chevron Corner Markers
        val chevronOffset = 22f
        val markerColor = if (isActive) Color(0xFFFFD600) else Color(0xFF455A64)
        cuboids.add(VoxelCuboid(posX - chevronOffset, posY - chevronOffset, posZ + 5f, 6f, 6f, 3f, markerColor))
        cuboids.add(VoxelCuboid(posX + chevronOffset, posY - chevronOffset, posZ + 5f, 6f, 6f, 3f, markerColor))
        cuboids.add(VoxelCuboid(posX - chevronOffset, posY + chevronOffset, posZ + 5f, 6f, 6f, 3f, markerColor))
        cuboids.add(VoxelCuboid(posX + chevronOffset, posY + chevronOffset, posZ + 5f, 6f, 6f, 3f, markerColor))

        // 5. Active Vertical Sky Beacon & Rotating Particle Rings
        if (isActive) {
            // Ascending energy pillar
            val beaconAlpha = 0.28f + 0.22f * pulse
            cuboids.add(
                VoxelCuboid(
                    x = posX,
                    y = posY,
                    z = posZ + 8f,
                    sizeX = 26f,
                    sizeY = 26f,
                    sizeZ = 85f,
                    baseColor = Color(0xFF00E5FF).copy(alpha = beaconAlpha)
                )
            )

            // Orbiting holographic rings
            val ringCount = 3
            for (i in 0 until ringCount) {
                val ringAngle = animTime * 2f + (i * PI.toFloat() * 2f / ringCount)
                val rx = posX + cos(ringAngle) * 20f
                val ry = posY + sin(ringAngle) * 20f
                val rz = posZ + 14f + i * 22f + sin(animTime * 4f + i) * 4f
                cuboids.add(
                    VoxelCuboid(
                        x = rx,
                        y = ry,
                        z = rz,
                        sizeX = 6f,
                        sizeY = 6f,
                        sizeZ = 6f,
                        baseColor = Color(0xFF76FF03)
                    )
                )
            }
        }

        return cuboids
    }

    override fun DrawScope.draw() {}
}
