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
import kotlin.math.sqrt

/**
 * Autonomous floating neon companion drone orbiting the player.
 * Automatically suctions nearby gems and fires micro-bytecode projectiles at bugs every 600ms.
 */
class CompanionDroneVoxelActor(
    var posX: Float,
    var posY: Float
) : Actor, Visible, Dynamic {

    override val isAlwaysActive: Boolean get() = true
    override val isAlwaysVisible: Boolean get() = true

    var orbitAngle: Float = 0f
    var posZ: Float = 26f
    private var fireCooldownMs: Int = 600
    val suctionRadius: Float = 140f

    override val body = BoxBody(
        initialPosition = SceneOffset(posX.sceneUnit, posY.sceneUnit),
        initialSize = SceneSize(20f.sceneUnit, 20f.sceneUnit),
        initialPivot = SceneOffset(10f.sceneUnit, 10f.sceneUnit)
    )

    override val layerIndex: Int = 1
    override var drawingOrder: Float = -(posX + posY)

    override fun onAdded(kubriko: Kubriko) {}

    fun updateOrbitAndCombat(
        playerX: Float,
        playerY: Float,
        deltaTimeMs: Int,
        nearbyBugs: List<BugHazardActor>
    ): List<ProjectileVoxelActor> {
        orbitAngle += deltaTimeMs * 0.0035f
        posX = playerX + cos(orbitAngle) * 55f
        posY = playerY + sin(orbitAngle) * 55f
        posZ = 24f + sin(orbitAngle * 2.5f) * 4f

        body.position = SceneOffset(posX.sceneUnit, posY.sceneUnit)
        drawingOrder = -(posX + posY)

        // Automatic Micro-Bytecode Projectile targeting nearest bug within 350 units
        fireCooldownMs -= deltaTimeMs
        if (fireCooldownMs <= 0 && nearbyBugs.isNotEmpty()) {
            val closestBug = nearbyBugs.minByOrNull { bug ->
                val dx = bug.posX - posX
                val dy = bug.posY - posY
                dx * dx + dy * dy
            }
            if (closestBug != null) {
                val dx = closestBug.posX - posX
                val dy = closestBug.posY - posY
                val dist = sqrt(dx * dx + dy * dy)
                if (dist <= 350f && dist > 0.001f) {
                    fireCooldownMs = 600
                    val dirX = dx / dist
                    val dirY = dy / dist
                    return listOf(
                        ProjectileVoxelActor(
                            posX = posX,
                            posY = posY,
                            dirX = dirX,
                            dirY = dirY,
                            isPlayerProjectile = true,
                            damage = 1
                        )
                    )
                }
            }
        }

        return emptyList()
    }

    override fun update(deltaTimeInMilliseconds: Int) {}

    fun emitVoxelCuboids(): List<VoxelCuboid> {
        val neonCyan = Color(0xFF00E5FF)
        val deepSlate = Color(0xFF1E2838)
        val coreGlow = Color(0xFF76FF03)

        return listOf(
            // Shadow on ground
            VoxelCuboid(posX, posY, 0f, 14f, 14f, 2f, Color(0x33000000)),
            // Outer Chassis Cube
            VoxelCuboid(posX, posY, posZ, 14f, 14f, 14f, deepSlate),
            // Glowing Inner Core
            VoxelCuboid(posX, posY, posZ + 3f, 8f, 8f, 8f, coreGlow),
            // Orbiting Mini Antenna Voxels
            VoxelCuboid(posX + cos(orbitAngle * 3f) * 10f, posY + sin(orbitAngle * 3f) * 10f, posZ + 5f, 4f, 4f, 4f, neonCyan),
            VoxelCuboid(posX - cos(orbitAngle * 3f) * 10f, posY - sin(orbitAngle * 3f) * 10f, posZ + 5f, 4f, 4f, 4f, neonCyan)
        )
    }

    override fun DrawScope.draw() {}
}
