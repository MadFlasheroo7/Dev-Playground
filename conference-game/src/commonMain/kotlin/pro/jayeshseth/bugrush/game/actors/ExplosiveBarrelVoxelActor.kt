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

/**
 * Explosive Byte Barrel that can be detonated by player projectiles or chain reactions.
 * Detonation triggers an AoE shockwave dealing 500 damage in a 140-unit radius.
 */
class ExplosiveBarrelVoxelActor(
    val posX: Float,
    val posY: Float,
    val posZ: Float = 0f,
    var hp: Int = 2,
    val maxHp: Int = 2
) : Actor, Visible, Dynamic {

    override val isAlwaysActive: Boolean get() = true
    override val isAlwaysVisible: Boolean get() = true

    var isDetonated: Boolean = false
    var isDestroyed: Boolean = false
    val collisionRadius: Float = 18f
    val explosionRadius: Float = 140f
    val explosionDamage: Int = 500

    private var pulseAnim: Float = 0f

    override val body = BoxBody(
        initialPosition = SceneOffset(posX.sceneUnit, posY.sceneUnit),
        initialSize = SceneSize(36f.sceneUnit, 36f.sceneUnit),
        initialPivot = SceneOffset(18f.sceneUnit, 18f.sceneUnit)
    )

    override val layerIndex: Int = 1
    override var drawingOrder: Float = -(posX + posY)

    override fun onAdded(kubriko: Kubriko) {}

    override fun update(deltaTimeInMilliseconds: Int) {
        pulseAnim += deltaTimeInMilliseconds * 0.006f
        if (pulseAnim > 2f * PI) pulseAnim -= (2f * PI.toFloat())
    }

    /**
     * Decreases barrel HP. Returns true if this damage caused the barrel to detonate.
     */
    fun takeDamage(amount: Int): Boolean {
        if (isDetonated || isDestroyed) return false
        hp -= amount
        if (hp <= 0) {
            detonate()
            return true
        }
        return false
    }

    fun detonate() {
        isDetonated = true
        isDestroyed = true
    }

    fun emitVoxelCuboids(): List<VoxelCuboid> {
        if (isDestroyed) return emptyList()

        val hazardYellow = Color(0xFFFFD600)
        val steelDark = Color(0xFF263238)
        val warningStripe = Color(0xFFE65100)
        val pulse = (sin(pulseAnim * 5f) + 1f) * 0.5f
        val criticalCore = Color(0xFFFF1744).copy(alpha = 0.7f + 0.3f * pulse)

        val cuboids = mutableListOf<VoxelCuboid>()

        // 1. Shadow
        cuboids.add(VoxelCuboid(posX, posY, posZ + 0f, 32f, 32f, 2f, Color(0x33000000)))

        // 2. Base Reinforcement Ring
        cuboids.add(VoxelCuboid(posX, posY, posZ + 1f, 28f, 28f, 4f, steelDark))

        // 3. Lower Hazard Tier
        cuboids.add(VoxelCuboid(posX, posY, posZ + 5f, 26f, 26f, 6f, hazardYellow))

        // 4. Center Belt Warning Tier
        cuboids.add(VoxelCuboid(posX, posY, posZ + 11f, 28f, 28f, 6f, warningStripe))

        // 5. Upper Hazard Tier
        cuboids.add(VoxelCuboid(posX, posY, posZ + 17f, 26f, 26f, 6f, hazardYellow))

        // 6. Top Valve Lid & Metal Ring
        cuboids.add(VoxelCuboid(posX, posY, posZ + 23f, 28f, 28f, 3f, steelDark))

        // 7. Critical Volatile Core & Valve Node (pulsing red/amber)
        cuboids.add(VoxelCuboid(posX, posY, posZ + 26f, 12f, 12f, 4f, criticalCore))

        // 8. Lateral Hazard Stripe Corners
        cuboids.add(VoxelCuboid(posX - 12f, posY, posZ + 11f, 3f, 6f, 6f, Color(0xFF212121)))
        cuboids.add(VoxelCuboid(posX + 12f, posY, posZ + 11f, 3f, 6f, 6f, Color(0xFF212121)))
        cuboids.add(VoxelCuboid(posX, posY - 12f, posZ + 11f, 6f, 3f, 6f, Color(0xFF212121)))
        cuboids.add(VoxelCuboid(posX, posY + 12f, posZ + 11f, 6f, 3f, 6f, Color(0xFF212121)))

        return cuboids
    }

    override fun DrawScope.draw() {}
}
