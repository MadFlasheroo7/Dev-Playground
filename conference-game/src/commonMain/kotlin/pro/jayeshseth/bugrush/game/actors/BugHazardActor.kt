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
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

enum class BugType {
    NORMAL,        // Standard red bug (1 HP, balanced)
    MEMORY_LEAK,   // Toxic green bug (2 HP, fast, drops byte shards)
    SWARMER,       // StackOverflow micro-bug (1 HP, high speed, spawns in packs)
    PHANTOM,       // NullPointerException phantom (2 HP, purple, stealth cloaking with flicker)
    DEADLOCK       // Armored beetle (3 HP, heavy front armor deflecting regular frontal shots)
}

enum class DamageResult {
    HIT,
    KILLED,
    DEFLECTED
}

class BugHazardActor(
    var posX: Float,
    var posY: Float,
    var posZ: Float = 0f,
    var headingAngle: Float = Random.nextFloat() * 6.28f,
    val bugType: BugType = BugType.NORMAL
) : Actor, Visible, Dynamic {

    constructor(
        posX: Float,
        posY: Float,
        headingAngle: Float = Random.nextFloat() * 6.28f,
        isMemoryLeak: Boolean
    ) : this(
        posX = posX,
        posY = posY,
        headingAngle = headingAngle,
        bugType = if (isMemoryLeak) BugType.MEMORY_LEAK else BugType.NORMAL
    )

    override val isAlwaysActive: Boolean get() = true
    override val isAlwaysVisible: Boolean get() = true

    val isMemoryLeak: Boolean get() = bugType == BugType.MEMORY_LEAK
    val isSwarmer: Boolean get() = bugType == BugType.SWARMER
    val isPhantom: Boolean get() = bugType == BugType.PHANTOM
    val isDeadlock: Boolean get() = bugType == BugType.DEADLOCK

    var isFrozen: Boolean = false
    var isPaused: Boolean = false
    var hp: Int = when (bugType) {
        BugType.DEADLOCK -> 3
        BugType.MEMORY_LEAK, BugType.PHANTOM -> 2
        BugType.NORMAL, BugType.SWARMER -> 1
    }
    var flashTimerMs: Int = 0
    val collisionRadius: Float = if (isSwarmer) 14f else 20f
    var canMoveTo: ((Float, Float) -> Boolean)? = null

    private var changeDirectionTimerMs: Int = Random.nextInt(1000, 3000)
    private var legAnim: Float = 0f
    private var phantomFlickerTimer: Float = 0f

    override val body = BoxBody(
        initialPosition = SceneOffset(posX.sceneUnit, posY.sceneUnit),
        initialSize = SceneSize(if (isSwarmer) 22f.sceneUnit else 32f.sceneUnit, if (isSwarmer) 22f.sceneUnit else 32f.sceneUnit),
        initialPivot = SceneOffset(16f.sceneUnit, 16f.sceneUnit)
    )

    override val layerIndex: Int = 1
    override var drawingOrder: Float = 0f

    override fun onAdded(kubriko: Kubriko) {}

    fun takeDamage(
        amount: Int,
        bulletDirX: Float? = null,
        bulletDirY: Float? = null,
        isHeavyRailgun: Boolean = false
    ): DamageResult {
        // Frontal shield deflection for DEADLOCK beetles unless pierced by heavy railgun
        if (isDeadlock && !isHeavyRailgun && bulletDirX != null && bulletDirY != null) {
            val hx = cos(headingAngle)
            val hy = sin(headingAngle)
            val dot = hx * bulletDirX + hy * bulletDirY
            if (dot < -0.22f) {
                // Bullet hit the heavy frontal armor
                flashTimerMs = 80
                return DamageResult.DEFLECTED
            }
        }

        hp -= amount
        flashTimerMs = 110
        return if (hp <= 0) DamageResult.KILLED else DamageResult.HIT
    }

    // Overload for backward compatibility
    fun takeDamage(amount: Int): Boolean =
        takeDamage(amount, null, null, false) == DamageResult.KILLED

    fun pushOutOf(centerX: Float, centerY: Float, minDistance: Float) {
        val dx = posX - centerX
        val dy = posY - centerY
        val dist = sqrt(dx * dx + dy * dy)
        if (dist < minDistance && dist > 0.001f) {
            val overlap = minDistance - dist
            posX += (dx / dist) * overlap
            posY += (dy / dist) * overlap
            headingAngle += 1.57f // Bounce direction
        }
    }

    var hasSwarmTarget: Boolean = false

    fun updateSwarmAI(
        playerX: Float,
        playerY: Float,
        deltaTimeInMilliseconds: Int,
        neighbors: List<BugHazardActor> = emptyList()
    ) {
        hasSwarmTarget = true
        if (isFrozen || isPaused) return

        val dx = playerX - posX
        val dy = playerY - posY
        val targetAngle = kotlin.math.atan2(dy, dx)

        // Type-specific tactical aggression
        val desiredAngle = when (bugType) {
            BugType.SWARMER -> {
                // High-frequency pincer wobble
                val wobble = sin(legAnim * 2.5f) * 0.6f
                targetAngle + wobble
            }
            BugType.PHANTOM -> {
                // Cloaking flanker angle offset
                val flank = if ((posX.toInt() and 1) == 0) 0.55f else -0.55f
                targetAngle + flank
            }
            BugType.DEADLOCK -> {
                // Frontal phalanx march directly toward target
                targetAngle
            }
            BugType.MEMORY_LEAK -> {
                // Persistent pursuit with toxic weaving
                val weave = sin(legAnim * 1.5f) * 0.35f
                targetAngle + weave
            }
            BugType.NORMAL -> {
                // Direct homing pursuit
                targetAngle
            }
        }

        // Flocking separation from neighbor bugs
        var sepX = 0f
        var sepY = 0f
        var sepCount = 0
        val sepRadius = collisionRadius * 2.5f
        val sepRadiusSq = sepRadius * sepRadius

        for (neighbor in neighbors) {
            if (neighbor !== this) {
                val ndx = posX - neighbor.posX
                if (abs(ndx) > sepRadius) continue
                val ndy = posY - neighbor.posY
                if (abs(ndy) > sepRadius) continue
                val distSq = ndx * ndx + ndy * ndy
                if (distSq in 0.0001f..sepRadiusSq) {
                    val ndist = sqrt(distSq)
                    sepX += (ndx / ndist) * (sepRadius - ndist)
                    sepY += (ndy / ndist) * (sepRadius - ndist)
                    sepCount++
                }
            }
        }

        val finalAngle = if (sepCount > 0) {
            val sepLen = sqrt(sepX * sepX + sepY * sepY).coerceAtLeast(0.001f)
            val dirX = cos(desiredAngle) * 0.75f + (sepX / sepLen) * 0.25f
            val dirY = sin(desiredAngle) * 0.75f + (sepY / sepLen) * 0.25f
            kotlin.math.atan2(dirY, dirX)
        } else {
            desiredAngle
        }

        // Steer headingAngle smoothly towards finalAngle
        val pi = kotlin.math.PI.toFloat()
        val angleDiff = (finalAngle - headingAngle + pi * 3f) % (2f * pi) - pi
        val turnSpeed = when (bugType) {
            BugType.SWARMER -> 0.010f
            BugType.DEADLOCK -> 0.003f
            else -> 0.006f
        }
        if (deltaTimeInMilliseconds > 0) {
            val factor = (turnSpeed * deltaTimeInMilliseconds).coerceIn(0.01f, 0.5f)
            headingAngle += angleDiff * factor
        }
    }

    override fun update(deltaTimeInMilliseconds: Int) {
        if (flashTimerMs > 0) flashTimerMs -= deltaTimeInMilliseconds
        if (isFrozen || isPaused) return

        phantomFlickerTimer += deltaTimeInMilliseconds * 0.006f

        if (!hasSwarmTarget) {
            changeDirectionTimerMs -= deltaTimeInMilliseconds
            if (changeDirectionTimerMs <= 0) {
                val deviation = if (isSwarmer) 2.2f else 1.5f
                headingAngle += (Random.nextFloat() - 0.5f) * deviation
                changeDirectionTimerMs = if (isSwarmer) Random.nextInt(600, 1800) else Random.nextInt(1500, 4000)
            }
        }

        val baseSpeed = when (bugType) {
            BugType.SWARMER -> 0.24f
            BugType.MEMORY_LEAK -> 0.18f
            BugType.PHANTOM -> 0.15f
            BugType.NORMAL -> 0.13f
            BugType.DEADLOCK -> 0.08f
        }

        val nextX = posX + cos(headingAngle) * baseSpeed * deltaTimeInMilliseconds
        val nextY = posY + sin(headingAngle) * baseSpeed * deltaTimeInMilliseconds

        // Steer/bounce off procedural map perimeter
        val distCenter = sqrt((nextX - 1900f) * (nextX - 1900f) + (nextY - 1900f) * (nextY - 1900f))
        val isWalkable = canMoveTo?.invoke(nextX, nextY) ?: (distCenter <= 1650f)
        if (!isWalkable || nextX < 140f || nextX > 3660f || nextY < 140f || nextY > 3660f) {
            val toCenterAngle = kotlin.math.atan2(1900f - posY, 1900f - posX)
            headingAngle = toCenterAngle + (Random.nextFloat() - 0.5f) * 0.4f
            posX += cos(toCenterAngle) * 3f
            posY += sin(toCenterAngle) * 3f
        } else {
            posX = nextX
            posY = nextY
        }

        legAnim += deltaTimeInMilliseconds * (if (isSwarmer) 0.035f else 0.015f)
        body.position = SceneOffset(posX.sceneUnit, posY.sceneUnit)
        drawingOrder = -(posX + posY)
    }

    fun emitVoxelCuboids(): List<VoxelCuboid> {
        val isFlashing = flashTimerMs > 0
        val cloakAlpha = if (isPhantom && !isFlashing) (0.28f + 0.62f * abs(sin(phantomFlickerTimer))) else 1.0f

        val bugColor = when {
            isFlashing -> Color.White
            isFrozen -> Color(0xFF80D8FF)
            isMemoryLeak -> Color(0xFF00E676) // Toxic green
            isSwarmer -> Color(0xFFFFD600)    // Rapid amber
            isPhantom -> Color(0xFFB388FF).copy(alpha = cloakAlpha) // Cloaked purple
            isDeadlock -> Color(0xFF00E5FF)   // Heavy cyan armor
            else -> Color(0xFFFF1744)         // Normal red
        }

        val shellColor = when {
            isFlashing -> Color(0xFFFFD54F)
            isFrozen -> Color(0xFF00B0FF)
            isMemoryLeak -> Color(0xFF1B5E20)
            isSwarmer -> Color(0xFFFF8F00)
            isPhantom -> Color(0xFF4A148C).copy(alpha = cloakAlpha)
            isDeadlock -> Color(0xFF263238)
            else -> Color(0xFFB71C1C)
        }

        val legSwing = if (!isFrozen && !isPaused) sin(legAnim) * (if (isSwarmer) 6f else 4f) else 0f
        val sizeScale = if (isSwarmer) 0.7f else 1.0f

        val cuboids = mutableListOf<VoxelCuboid>()

        // 1. Shadow
        cuboids.add(VoxelCuboid(posX, posY, 0f, 24f * sizeScale, 24f * sizeScale, 2f, Color(0x33000000).copy(alpha = 0.2f * cloakAlpha)))

        // 2. Left and Right Legs
        cuboids.add(VoxelCuboid(posX - 10f * sizeScale, posY + legSwing, 4f, 6f * sizeScale, 6f * sizeScale, 8f, bugColor))
        cuboids.add(VoxelCuboid(posX + 10f * sizeScale, posY - legSwing, 4f, 6f * sizeScale, 6f * sizeScale, 8f, bugColor))

        // 3. Main Abdomen / Shell
        cuboids.add(VoxelCuboid(posX, posY, 6f, 20f * sizeScale, 18f * sizeScale, 14f * sizeScale, shellColor))

        // 4. Head and Glowing Bug Eyes
        val headX = posX + cos(headingAngle) * 8f * sizeScale
        val headY = posY + sin(headingAngle) * 8f * sizeScale
        cuboids.add(VoxelCuboid(headX, headY, 12f * sizeScale, 10f * sizeScale, 10f * sizeScale, 10f * sizeScale, bugColor))
        cuboids.add(VoxelCuboid(posX + cos(headingAngle) * 11f * sizeScale, posY + sin(headingAngle) * 11f * sizeScale, 14f * sizeScale, 4f, 4f, 4f, Color(0xFFFFEB3B).copy(alpha = cloakAlpha)))

        // 5. Special Type Details
        if (isDeadlock) {
            // Heavy frontal deflection shield
            val shieldX = posX + cos(headingAngle) * 13f
            val shieldY = posY + sin(headingAngle) * 13f
            cuboids.add(VoxelCuboid(shieldX, shieldY, 8f, 16f, 8f, 18f, Color(0xFF00E5FF)))
            cuboids.add(VoxelCuboid(posX, posY, 18f, 22f, 20f, 6f, Color(0xFF455A64)))
        }

        if (posZ != 0f) {
            return cuboids.map { it.copy(z = it.z + posZ) }
        }

        return cuboids
    }

    override fun DrawScope.draw() {}
}
