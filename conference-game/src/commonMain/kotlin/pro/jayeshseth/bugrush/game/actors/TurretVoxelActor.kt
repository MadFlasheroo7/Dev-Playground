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
 * Stationary coroutines automated blaster turret deployed upon dashing.
 * Lasts 8 seconds, auto-targeting and eliminating nearby bugs within 380 units.
 */
class TurretVoxelActor(
    val posX: Float,
    val posY: Float,
    var remainingLifeMs: Int = 8000
) : Actor, Visible, Dynamic {

    override val isAlwaysActive: Boolean get() = true
    override val isAlwaysVisible: Boolean get() = true

    var isExpired: Boolean = false
    private var fireCooldownMs: Int = 300
    private var aimAngle: Float = 0f

    override val body = BoxBody(
        initialPosition = SceneOffset(posX.sceneUnit, posY.sceneUnit),
        initialSize = SceneSize(28f.sceneUnit, 28f.sceneUnit),
        initialPivot = SceneOffset(14f.sceneUnit, 14f.sceneUnit)
    )

    override val layerIndex: Int = 1
    override var drawingOrder: Float = -(posX + posY)

    override fun onAdded(kubriko: Kubriko) {}

    fun updateAndCombat(deltaTimeMs: Int, nearbyBugs: List<BugHazardActor>): List<ProjectileVoxelActor> {
        remainingLifeMs -= deltaTimeMs
        if (remainingLifeMs <= 0) {
            isExpired = true
            return emptyList()
        }

        aimAngle += deltaTimeMs * 0.004f

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
                if (dist <= 380f && dist > 0.001f) {
                    fireCooldownMs = 450
                    val dirX = dx / dist
                    val dirY = dy / dist
                    aimAngle = kotlin.math.atan2(dirY, dirX)
                    return listOf(
                        ProjectileVoxelActor(
                            posX = posX + dirX * 16f,
                            posY = posY + dirY * 16f,
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
        val pedestalColor = Color(0xFF263238)
        val turretColor = Color(0xFF37474F)
        val barrelColor = Color(0xFF00E5FF)
        val lightColor = if ((remainingLifeMs / 200) % 2 == 0) Color(0xFF00E676) else Color(0xFF76FF03)

        return listOf(
            // Base Shadow
            VoxelCuboid(posX, posY, 0f, 24f, 24f, 2f, Color(0x44000000)),
            // Terminal Pedestal
            VoxelCuboid(posX, posY, 2f, 20f, 20f, 12f, pedestalColor),
            // Glowing Terminal Status Light
            VoxelCuboid(posX, posY, 14f, 10f, 10f, 3f, lightColor),
            // Rotating Turret Swivel Head
            VoxelCuboid(posX, posY, 17f, 16f, 16f, 10f, turretColor),
            // Blaster Barrel
            VoxelCuboid(
                x = posX + cos(aimAngle) * 10f,
                y = posY + sin(aimAngle) * 10f,
                z = 21f,
                sizeX = 6f,
                sizeY = 6f,
                sizeZ = 6f,
                baseColor = barrelColor
            )
        )
    }

    override fun DrawScope.draw() {}
}
