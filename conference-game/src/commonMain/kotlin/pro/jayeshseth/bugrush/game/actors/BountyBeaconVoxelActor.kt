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
 * District Bounty Beacon terminal.
 * Stepping on or interacting with the beacon triggers a 15-second timed frenzy bounty ("PURGE 8 BUGS IN 15s").
 * Completing the bounty awards 5000 bonus PTS and increments the score multiplier.
 */
class BountyBeaconVoxelActor(
    val posX: Float,
    val posY: Float,
    val posZ: Float = 0f,
    val districtName: String,
    val targetKills: Int = 8,
    val bountyTimeLimitMs: Int = 15000
) : Actor, Visible, Dynamic {

    override val isAlwaysActive: Boolean get() = true
    override val isAlwaysVisible: Boolean get() = true

    val collisionRadius: Float = 20f
    val triggerRadius: Float = 36f

    override val body = BoxBody(
        initialPosition = SceneOffset(posX.sceneUnit, posY.sceneUnit),
        initialSize = SceneSize(44f.sceneUnit, 44f.sceneUnit),
        initialPivot = SceneOffset(22f.sceneUnit, 22f.sceneUnit)
    )

    override val layerIndex: Int = 1
    override var drawingOrder: Float = -(posX + posY)

    var isActive: Boolean = false
    var isCompleted: Boolean = false
    var remainingTimeMs: Int = 0
    var currentKills: Int = 0
    var cooldownRemainingMs: Int = 0

    var animTime: Float = 0f

    val isReadyToTrigger: Boolean get() = !isActive && cooldownRemainingMs <= 0

    override fun onAdded(kubriko: Kubriko) {}

    override fun update(deltaTimeInMilliseconds: Int) {
        animTime += deltaTimeInMilliseconds * 0.005f
        if (animTime > 2f * PI) animTime -= (2f * PI.toFloat())

        if (isActive) {
            remainingTimeMs -= deltaTimeInMilliseconds
            if (remainingTimeMs <= 0) {
                // Bounty failed / expired
                isActive = false
                cooldownRemainingMs = 20000 // 20s cooldown before retry
            }
        } else if (cooldownRemainingMs > 0) {
            cooldownRemainingMs = (cooldownRemainingMs - deltaTimeInMilliseconds).coerceAtLeast(0)
        }
    }

    /**
     * Attempts to trigger the bounty if the player is within range.
     * Returns true if newly activated.
     */
    fun checkInteraction(playerX: Float, playerY: Float): Boolean {
        if (!isReadyToTrigger) return false
        val dx = playerX - posX
        val dy = playerY - posY
        if (dx * dx + dy * dy <= triggerRadius * triggerRadius) {
            startBounty()
            return true
        }
        return false
    }

    fun startBounty() {
        isActive = true
        isCompleted = false
        currentKills = 0
        remainingTimeMs = bountyTimeLimitMs
    }

    /**
     * Records a bug kill towards this active bounty.
     * Returns true if this kill completes the bounty.
     */
    fun recordKill(): Boolean {
        if (!isActive) return false
        currentKills += 1
        if (currentKills >= targetKills) {
            isActive = false
            isCompleted = true
            cooldownRemainingMs = 30000 // 30s cooldown after successful bounty
            return true
        }
        return false
    }

    fun emitVoxelCuboids(): List<VoxelCuboid> {
        val cuboids = mutableListOf<VoxelCuboid>()
        val pulse = (sin(animTime * 4.5f) + 1f) * 0.5f

        val themeColor = when {
            isActive -> Color(0xFFFFD600) // Gold frenzy when active
            isReadyToTrigger -> Color(0xFF00E5FF) // Cyan when ready
            else -> Color(0xFF455A64) // Dark steel when on cooldown
        }

        val baseColor = Color(0xFF1E2833)
        val consoleColor = Color(0xFF2B3A48)

        // 1. Shadow
        cuboids.add(VoxelCuboid(posX, posY, posZ + 0f, 38f, 38f, 2f, Color(0x33000000)))

        // 2. Heavy Hex Foundation
        cuboids.add(VoxelCuboid(posX, posY, posZ + 1f, 36f, 36f, 5f, baseColor))

        // 3. Central Column Pedestal
        cuboids.add(VoxelCuboid(posX, posY, posZ + 6f, 22f, 22f, 16f, consoleColor))

        // 4. Terminal Console Cap
        cuboids.add(VoxelCuboid(posX, posY, posZ + 22f, 28f, 28f, 4f, baseColor))

        // 5. Holographic Terminal Screen
        val screenAlpha = if (isActive) 0.8f + 0.2f * pulse else 0.5f
        cuboids.add(VoxelCuboid(posX, posY, posZ + 25f, 20f, 20f, 2f, themeColor.copy(alpha = screenAlpha)))

        // 6. Floating Holographic Target Crest
        val hoverZ = posZ + 36f + sin(animTime * 3f) * 4f
        cuboids.add(
            VoxelCuboid(
                x = posX,
                y = posY,
                z = hoverZ,
                sizeX = 14f,
                sizeY = 14f,
                sizeZ = 14f,
                baseColor = themeColor
            )
        )

        // 7. Rotating Holographic Reticle Orbits (when active or ready)
        if (isActive || isReadyToTrigger) {
            val ringCount = 3
            for (i in 0 until ringCount) {
                val angle = animTime * 3f + (i * PI.toFloat() * 2f / ringCount)
                val rx = posX + cos(angle) * 16f
                val ry = posY + sin(angle) * 16f
                val rz = hoverZ + sin(animTime * 5f + i) * 3f
                cuboids.add(
                    VoxelCuboid(
                        x = rx,
                        y = ry,
                        z = rz,
                        sizeX = 4f,
                        sizeY = 4f,
                        sizeZ = 4f,
                        baseColor = if (isActive) Color(0xFFFF1744) else Color(0xFF76FF03)
                    )
                )
            }
        }

        return cuboids
    }

    override fun DrawScope.draw() {}
}
