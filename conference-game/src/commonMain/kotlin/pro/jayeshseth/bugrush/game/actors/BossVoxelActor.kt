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
import kotlin.math.sqrt

/**
 * Boss Titan: OutOfMemoryError.
 * Features Phase 2 "Kernel Panic" mode at <= 40% HP with alarm siren, sweeping 5-laser barrage,
 * ground pounds, and fiery emergency voxel coronas.
 */
class BossVoxelActor(
    var posX: Float = 1000f,
    var posY: Float = 1000f,
    var posZ: Float = 0f,
    val maxHp: Int = 35
) : Actor, Visible, Dynamic {

    var groundZProvider: ((Float, Float) -> Float)? = null

    override val isAlwaysActive: Boolean get() = true
    override val isAlwaysVisible: Boolean get() = true

    var hp: Int = maxHp
    var isDefeated: Boolean = false
    var flashTimerMs: Int = 0
    var attackTimerMs: Int = 2200
    var stompTimerMs: Int = 3600

    val isEnraged: Boolean get() = hp <= maxHp / 2
    val isKernelPanic: Boolean get() = hp <= (maxHp * 0.40f).toInt()
    var hasTriggeredPhase2Minions: Boolean = false
    var hasTriggeredKernelPanic: Boolean = false

    private var walkProgress: Float = 0f
    private var coronaPulse: Float = 0f
    val collisionRadius: Float = 45f

    override val body = BoxBody(
        initialPosition = SceneOffset(posX.sceneUnit, posY.sceneUnit),
        initialSize = SceneSize(90f.sceneUnit, 90f.sceneUnit),
        initialPivot = SceneOffset(45f.sceneUnit, 45f.sceneUnit)
    )

    override val layerIndex: Int = 1
    override var drawingOrder: Float = -(posX + posY)

    override fun onAdded(kubriko: Kubriko) {}

    fun takeDamage(amount: Int): Boolean {
        hp -= amount
        flashTimerMs = 100
        if (hp <= 0) {
            isDefeated = true
            return true
        }
        return false
    }

    fun updateTowards(playerX: Float, playerY: Float, deltaTimeInMilliseconds: Int): List<ProjectileVoxelActor> {
        if (isDefeated) return emptyList()

        if (flashTimerMs > 0) flashTimerMs -= deltaTimeInMilliseconds
        coronaPulse += deltaTimeInMilliseconds * 0.015f

        val dx = playerX - posX
        val dy = playerY - posY
        val dist = sqrt(dx * dx + dy * dy)

        val speed = when {
            isKernelPanic -> 0.22f
            isEnraged -> 0.15f
            else -> 0.09f
        }

        if (dist > 75f) {
            posX += (dx / dist) * speed * deltaTimeInMilliseconds
            posY += (dy / dist) * speed * deltaTimeInMilliseconds
            walkProgress += deltaTimeInMilliseconds * (if (isKernelPanic) 0.014f else if (isEnraged) 0.009f else 0.005f)
            if (walkProgress > 2f * PI) walkProgress -= (2f * PI.toFloat())
        }

        groundZProvider?.invoke(posX, posY)?.let { posZ = it }

        body.position = SceneOffset(posX.sceneUnit, posY.sceneUnit)
        drawingOrder = -(posX + posY)

        val spawnedProjectiles = mutableListOf<ProjectileVoxelActor>()

        // 1. Projectile Attacks: 5-Laser Barrage in Kernel Panic, 3-way in Enraged, 1 in Normal
        attackTimerMs -= deltaTimeInMilliseconds
        val attackInterval = when {
            isKernelPanic -> 1000
            isEnraged -> 1600
            else -> 2600
        }

        if (attackTimerMs <= 0 && dist < 850f) {
            attackTimerMs = attackInterval
            val normDx = dx / dist
            val normDy = dy / dist
            val centerAngle = kotlin.math.atan2(normDy, normDx)

            when {
                isKernelPanic -> {
                    // Sweeping 5-Laser Barrage
                    val angles = listOf(
                        centerAngle - 0.44f,
                        centerAngle - 0.22f,
                        centerAngle,
                        centerAngle + 0.22f,
                        centerAngle + 0.44f
                    )
                    angles.forEach { a ->
                        spawnedProjectiles.add(
                            ProjectileVoxelActor(
                                posX = posX + cos(a) * 35f,
                                posY = posY + sin(a) * 35f,
                                dirX = cos(a),
                                dirY = sin(a),
                                isPlayerProjectile = false,
                                damage = 1,
                                isSuperCharge = true
                            )
                        )
                    }
                }
                isEnraged -> {
                    // 3-way memory-leak burst
                    listOf(centerAngle - 0.32f, centerAngle, centerAngle + 0.32f).forEach { a ->
                        spawnedProjectiles.add(
                            ProjectileVoxelActor(
                                posX = posX + cos(a) * 30f,
                                posY = posY + sin(a) * 30f,
                                dirX = cos(a),
                                dirY = sin(a),
                                isPlayerProjectile = false,
                                damage = 1
                            )
                        )
                    }
                }
                else -> {
                    spawnedProjectiles.add(
                        ProjectileVoxelActor(posX, posY, normDx, normDy, isPlayerProjectile = false, damage = 1)
                    )
                }
            }
        }

        // 2. Shockwave Ground Pound: 8 radial shockwaves
        if (isEnraged || isKernelPanic) {
            stompTimerMs -= deltaTimeInMilliseconds
            val stompInterval = if (isKernelPanic) 2400 else 3800
            if (stompTimerMs <= 0) {
                stompTimerMs = stompInterval
                for (i in 0 until 8) {
                    val angle = (i * PI.toFloat() / 4f)
                    spawnedProjectiles.add(
                        ProjectileVoxelActor(
                            posX = posX + cos(angle) * 40f,
                            posY = posY + sin(angle) * 40f,
                            dirX = cos(angle),
                            dirY = sin(angle),
                            isPlayerProjectile = false,
                            damage = 1,
                            isSuperCharge = isKernelPanic
                        )
                    )
                }
            }
        }

        return spawnedProjectiles
    }

    override fun update(deltaTimeInMilliseconds: Int) {}

    fun emitVoxelCuboids(): List<VoxelCuboid> {
        val cuboids = mutableListOf<VoxelCuboid>()

        val legSwing = sin(walkProgress) * 14f
        val isFlashing = flashTimerMs > 0

        val shellColor = when {
            isFlashing -> Color(0xFFFFFFFF)
            isKernelPanic -> Color(0xFFD50000) // Pure Emergency Crimson
            isEnraged -> Color(0xFFB71C1C)      // Deep Crimson
            else -> Color(0xFF4A148C)           // Deep Purple
        }
        val accentColor = when {
            isFlashing -> Color(0xFFFF5252)
            isKernelPanic -> Color(0xFFFF9100) // Blazing Orange
            isEnraged -> Color(0xFFFF5722)
            else -> Color(0xFFFF1744)
        }
        val eyeColor = when {
            isKernelPanic -> Color(0xFFFF1744)
            isEnraged -> Color(0xFFFF5252)
            else -> Color(0xFFFFEA00)
        }

        // 1. Base Shadow
        cuboids.add(VoxelCuboid(posX, posY, posZ + 0f, 68f, 68f, 3f, Color(0x44000000)))

        // 2. 6 Heavy Legs
        val legColor = if (isKernelPanic) Color(0xFF210000) else if (isEnraged) Color(0xFF3E2723) else Color(0xFF212121)
        cuboids.add(VoxelCuboid(posX - 38f, posY - 20f + legSwing, posZ + 2f, 14f, 12f, 14f, legColor))
        cuboids.add(VoxelCuboid(posX - 42f, posY, posZ + 2f, 14f, 12f, 14f, legColor))
        cuboids.add(VoxelCuboid(posX - 38f, posY + 20f - legSwing, posZ + 2f, 14f, 12f, 14f, legColor))

        cuboids.add(VoxelCuboid(posX + 38f, posY - 20f - legSwing, posZ + 2f, 14f, 12f, 14f, legColor))
        cuboids.add(VoxelCuboid(posX + 42f, posY, posZ + 2f, 14f, 12f, 14f, legColor))
        cuboids.add(VoxelCuboid(posX + 38f, posY + 20f + legSwing, posZ + 2f, 14f, 12f, 14f, legColor))

        // 3. Massive Carapace Shell
        cuboids.add(VoxelCuboid(posX, posY, posZ + 14f, 64f, 64f, 28f, shellColor))

        // 4. Armored Crown / Pulsing Dorsal Core
        cuboids.add(VoxelCuboid(posX, posY, posZ + 36f, 44f, 44f, 16f, accentColor))

        // 5. Heavy Horn Mandible
        val mandibleColor = if (isKernelPanic) Color(0xFFFF1744) else if (isEnraged) Color(0xFFD50000) else Color(0xFFC62828)
        cuboids.add(VoxelCuboid(posX, posY + 36f, posZ + 22f, 18f, 26f, 16f, mandibleColor))

        // 6. Glowing Eyes
        cuboids.add(VoxelCuboid(posX - 16f, posY + 30f, posZ + 30f, 10f, 8f, 8f, eyeColor))
        cuboids.add(VoxelCuboid(posX + 16f, posY + 30f, posZ + 30f, 10f, 8f, 8f, eyeColor))

        // 7. Kernel Panic Fiery Emergency Corona
        if (isKernelPanic) {
            val coronaZ = posZ + 44f + sin(coronaPulse) * 4f
            cuboids.add(VoxelCuboid(posX, posY, coronaZ, 30f, 30f, 10f, Color(0xFFFF1744)))
            cuboids.add(VoxelCuboid(posX - 22f, posY - 10f, coronaZ - 4f, 14f, 14f, 8f, Color(0xFFFF9100)))
            cuboids.add(VoxelCuboid(posX + 22f, posY - 10f, coronaZ - 4f, 14f, 14f, 8f, Color(0xFFFF9100)))
        } else if (isEnraged) {
            cuboids.add(VoxelCuboid(posX, posY - 10f, posZ + 48f, 20f, 20f, 8f, Color(0xFFFF9100)))
        }

        return cuboids
    }

    override fun DrawScope.draw() {}
}
