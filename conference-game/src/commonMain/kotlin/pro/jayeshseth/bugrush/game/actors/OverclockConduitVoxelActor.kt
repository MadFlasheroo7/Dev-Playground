package pro.jayeshseth.bugrush.game.actors

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.actor.Actor
import com.pandulapeter.kubriko.actor.body.BoxBody
import com.pandulapeter.kubriko.actor.traits.Dynamic
import com.pandulapeter.kubriko.actor.traits.Visible
import pro.jayeshseth.bugrush.game.renderer.VoxelCuboid
import pro.jayeshseth.bugrush.trivia.PowerUpType
import com.pandulapeter.kubriko.helpers.extensions.sceneUnit
import com.pandulapeter.kubriko.types.SceneOffset
import com.pandulapeter.kubriko.types.SceneSize
import kotlin.math.PI
import kotlin.math.sin

/**
 * Overclock Power Conduit platform.
 * Stepping on a charged conduit grants a 6-second temporary buff (Hyper-Dash or Overcharge).
 * Recharges after a 25-second cooldown.
 */
class OverclockConduitVoxelActor(
    val posX: Float,
    val posY: Float,
    val posZ: Float = 0f,
    val powerUpType: PowerUpType = PowerUpType.SPEED_MAGNET
) : Actor, Visible, Dynamic {

    override val isAlwaysActive: Boolean get() = true
    override val isAlwaysVisible: Boolean get() = true

    override val body = BoxBody(
        initialPosition = SceneOffset(posX.sceneUnit, posY.sceneUnit),
        initialSize = SceneSize(54f.sceneUnit, 54f.sceneUnit),
        initialPivot = SceneOffset(27f.sceneUnit, 27f.sceneUnit)
    )

    override val layerIndex: Int = 1
    override var drawingOrder: Float = -(posX + posY)

    var isCharged: Boolean = true
    var rechargeTimerMs: Int = 0
    val rechargeDurationMs: Int = 25000
    val activationRadius: Float = 32f

    private var animTime: Float = 0f

    override fun onAdded(kubriko: Kubriko) {}

    override fun update(deltaTimeInMilliseconds: Int) {
        animTime += deltaTimeInMilliseconds * 0.005f
        if (animTime > 2f * PI) animTime -= (2f * PI.toFloat())

        if (!isCharged) {
            rechargeTimerMs -= deltaTimeInMilliseconds
            if (rechargeTimerMs <= 0) {
                isCharged = true
                rechargeTimerMs = 0
            }
        }
    }

    /**
     * Checks if player stepped onto the conduit while charged.
     * Activates the boost and begins 25s recharge cooldown.
     */
    fun checkActivation(playerX: Float, playerY: Float): Boolean {
        if (!isCharged) return false
        val dx = playerX - posX
        val dy = playerY - posY
        if (dx * dx + dy * dy <= activationRadius * activationRadius) {
            isCharged = false
            rechargeTimerMs = rechargeDurationMs
            return true
        }
        return false
    }

    fun emitVoxelCuboids(): List<VoxelCuboid> {
        val cuboids = mutableListOf<VoxelCuboid>()
        val pulse = (sin(animTime * 4f) + 1f) * 0.5f

        val themeColor = when (powerUpType) {
            PowerUpType.SPEED_MAGNET -> Color(0xFF00E5FF) // Cyan
            PowerUpType.FREEZE_SHIELD -> Color(0xFF76FF03) // Neon Green
            PowerUpType.MEGA_FRENZY_NUKE -> Color(0xFFFF9100) // Amber / Flame
            PowerUpType.OVERCHARGE_INVINCIBLE -> Color(0xFFFF007F) // Hot Pink
        }

        val activeColor = if (isCharged) themeColor else Color(0xFF37474F)
        val coilColor = if (isCharged) Color(0xFFFFD600) else Color(0xFF263238)

        // 1. Base Plate
        cuboids.add(VoxelCuboid(posX, posY, posZ + 0f, 52f, 52f, 4f, Color(0xFF101924), isFloor = true))

        // 2. Power Grid Border
        val borderAlpha = if (isCharged) 0.6f + 0.4f * pulse else 0.3f
        cuboids.add(VoxelCuboid(posX, posY, posZ + 2f, 46f, 46f, 2f, activeColor.copy(alpha = borderAlpha)))

        // 3. Central Power Pad
        cuboids.add(VoxelCuboid(posX, posY, posZ + 4f, 32f, 32f, 3f, Color(0xFF16212E)))

        // 4. Surging Induction Coils (4 corners)
        val coilOffset = 18f
        cuboids.add(VoxelCuboid(posX - coilOffset, posY - coilOffset, posZ + 3f, 8f, 8f, 10f, coilColor))
        cuboids.add(VoxelCuboid(posX + coilOffset, posY - coilOffset, posZ + 3f, 8f, 8f, 10f, coilColor))
        cuboids.add(VoxelCuboid(posX - coilOffset, posY + coilOffset, posZ + 3f, 8f, 8f, 10f, coilColor))
        cuboids.add(VoxelCuboid(posX + coilOffset, posY + coilOffset, posZ + 3f, 8f, 8f, 10f, coilColor))

        // 5. Floating Overclock Core (hovering when charged)
        if (isCharged) {
            val hoverZ = posZ + 12f + sin(animTime * 3f) * 4f
            cuboids.add(
                VoxelCuboid(
                    x = posX,
                    y = posY,
                    z = hoverZ,
                    sizeX = 14f,
                    sizeY = 14f,
                    sizeZ = 14f,
                    baseColor = activeColor
                )
            )
            // Energy ring around core
            cuboids.add(
                VoxelCuboid(
                    x = posX,
                    y = posY,
                    z = hoverZ + 4f,
                    sizeX = 22f,
                    sizeY = 22f,
                    sizeZ = 3f,
                    baseColor = Color(0xFF00E5FF).copy(alpha = 0.5f + 0.3f * pulse)
                )
            )
        }

        return cuboids
    }

    override fun DrawScope.draw() {}
}
