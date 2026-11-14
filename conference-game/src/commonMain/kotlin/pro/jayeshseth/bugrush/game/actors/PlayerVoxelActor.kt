package pro.jayeshseth.bugrush.game.actors

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.actor.Actor
import com.pandulapeter.kubriko.actor.body.BoxBody
import com.pandulapeter.kubriko.actor.traits.Dynamic
import com.pandulapeter.kubriko.actor.traits.Unique
import com.pandulapeter.kubriko.actor.traits.Visible
import pro.jayeshseth.bugrush.game.renderer.VoxelCuboid
import androidx.compose.ui.input.key.Key
import com.pandulapeter.kubriko.gamepadInput.GamepadButton
import com.pandulapeter.kubriko.gamepadInput.GamepadInputAware
import com.pandulapeter.kubriko.gamepadInput.GamepadState
import com.pandulapeter.kubriko.helpers.extensions.sceneUnit
import com.pandulapeter.kubriko.keyboardInput.KeyboardInputAware
import com.pandulapeter.kubriko.types.SceneOffset
import com.pandulapeter.kubriko.types.SceneSize
import kotlinx.collections.immutable.ImmutableSet
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

enum class PlayerArchetype(val title: String, val subtitle: String) {
    CYBER_CODER("Cyber-Coder", "Balanced • Rapid Byte Blaster & Jetpack"),
    KOTLIN_BOT("Kotlin Bot", "Speedster • Twin Pulse & Hover Boosters"),
    ARCH_DEV("Arch-Dev", "Heavy • Piercing Railgun & Fortified Shield")
}

enum class WeaponType(val displayName: String, val fireRateMs: Int, val ammoCost: Int) {
    RAPID_BYTE_BLASTER("Rapid Byte Blaster", 210, 1),
    TWIN_PULSE("Twin Pulse Blaster", 160, 2),
    HEAVY_RAILGUN("Heavy Railgun [2 DMG]", 360, 5),
    TRIPLE_SPREAD("Triple Spread Blaster", 130, 3),
    PLASMA_FLAK_CANNON("Plasma Flak Cannon [5-WAY]", 180, 5),
    NOVA_360_BLASTER("Nova 360 Blaster [16-WAY]", 450, 16)
}

class PlayerVoxelActor(
    initialX: Float = 1900f,
    initialY: Float = 1900f,
    var archetype: PlayerArchetype = PlayerArchetype.CYBER_CODER
) : Actor, Visible, Dynamic, Unique, KeyboardInputAware, GamepadInputAware {

    override val isAlwaysActive: Boolean get() = true
    override val isAlwaysVisible: Boolean get() = true

    var posX: Float = initialX
    var posY: Float = initialY
    var posZ: Float = 0f
    var groundZProvider: (Float, Float) -> Float = { _, _ -> 0f }
    var headingX: Float = 0f
    var headingY: Float = 1f // Face south initially

    val collisionRadius: Float = 22f
    var canMoveTo: (x: Float, y: Float, radius: Float) -> Boolean = { _, _, _ -> true }
    var cameraAngleRad: Float = 0f
    var stunTimerRemainingMs: Int = 0
    val isStunned: Boolean get() = stunTimerRemainingMs > 0
    var invulnerableTimerMs: Int = 0
    var isEntranceShieldActive: Boolean = false
    var entranceShieldTimerMs: Int = 0
    val isInvulnerable: Boolean get() = isDashing || invulnerableTimerMs > 0 || isEntranceShieldActive

    var isSpeedBoostActive: Boolean = false
    var isShieldActive: Boolean = (archetype == PlayerArchetype.ARCH_DEV)
    var shootCooldownMs: Int = 0
    var hasAmmo: () -> Boolean = { true }

    // Version Upgrade Perks
    var hasGarbageCollectorDrone: Boolean = false
    var hasCoroutinesTurrets: Boolean = false
    var hasJitCompiler: Boolean = false
    var hasRefactorFirewall: Boolean = false
    var isFirewallShieldActive: Boolean = false
    var firewallRechargeTimerMs: Int = 0

    // Active Weapon (supports cheat-code cycling across all 5 guns)
    var activeWeapon: WeaponType = when (archetype) {
        PlayerArchetype.KOTLIN_BOT -> WeaponType.TWIN_PULSE
        PlayerArchetype.ARCH_DEV -> WeaponType.HEAVY_RAILGUN
        PlayerArchetype.CYBER_CODER -> WeaponType.RAPID_BYTE_BLASTER
    }

    fun cycleWeapon(): WeaponType {
        val weapons = WeaponType.values()
        val nextIdx = (activeWeapon.ordinal + 1) % weapons.size
        activeWeapon = weapons[nextIdx]
        return activeWeapon
    }

    // Aim Heading (supports independent mouse aiming, twin-stick, and target assist)
    var aimHeadingX: Float = 0f
    var aimHeadingY: Float = 1f
    var isAimOverridden: Boolean = false

    // Dash Ability
    var dashCooldownMs: Int = 0
    var dashTimerMs: Int = 0
    val isDashing: Boolean get() = dashTimerMs > 0
    val maxDashCooldownMs: Int = 850
    private var dashDirX: Float = 0f
    private var dashDirY: Float = 1f

    private var walkAnimProgress: Float = 0f
    private var isMoving: Boolean = false
    var isPaused: Boolean = false
    var isMovementLocked: Boolean = false

    private var keyMoveX = 0f
    private var keyMoveY = 0f
    private var padMoveX = 0f
    private var padMoveY = 0f
    private var directMoveX = 0f
    private var directMoveY = 0f

    var onFireWeaponTriggered: () -> Unit = {}
    var onDashTriggered: () -> Unit = {}

    fun setDirectMove(dx: Float, dy: Float) {
        directMoveX = dx
        directMoveY = dy
    }

    fun setAimDirection(dirX: Float, dirY: Float) {
        val len = sqrt(dirX * dirX + dirY * dirY)
        if (len > 0.001f) {
            aimHeadingX = dirX / len
            aimHeadingY = dirY / len
            isAimOverridden = true
        }
    }

    fun applyKnockback(dirX: Float, dirY: Float, force: Float = 70f) {
        val targetX = (posX + dirX * force).coerceIn(80f, 3720f)
        val targetY = (posY + dirY * force).coerceIn(80f, 3720f)
        if (canMoveTo(targetX, targetY, collisionRadius)) {
            posX = targetX
            posY = targetY
        } else if (canMoveTo(targetX, posY, collisionRadius)) {
            posX = targetX
        } else if (canMoveTo(posX, targetY, collisionRadius)) {
            posY = targetY
        }
    }

    fun triggerHit(stunDurationMs: Int = 180, iFramesMs: Int = 1200) {
        stunTimerRemainingMs = stunDurationMs
        invulnerableTimerMs = iFramesMs
    }

    fun triggerDash(): Boolean {
        if (dashCooldownMs <= 0 && !isStunned && !isMovementLocked) {
            dashCooldownMs = maxDashCooldownMs
            dashTimerMs = 240

            // Determine dash vector from active inputs if moving, else last heading
            val moveX = keyMoveX + padMoveX + directMoveX
            val moveY = keyMoveY + padMoveY + directMoveY
            val moveLen = sqrt(moveX * moveX + moveY * moveY)
            if (moveLen > 0.08f) {
                val inputDirX = moveX / moveLen
                val inputDirY = moveY / moveLen
                if (cameraAngleRad != 0f) {
                    val cosA = cos(cameraAngleRad)
                    val sinA = sin(cameraAngleRad)
                    dashDirX = inputDirX * cosA + inputDirY * sinA
                    dashDirY = -inputDirX * sinA + inputDirY * cosA
                } else {
                    dashDirX = inputDirX
                    dashDirY = inputDirY
                }
            } else {
                val headLen = sqrt(headingX * headingX + headingY * headingY).coerceAtLeast(0.001f)
                dashDirX = headingX / headLen
                dashDirY = headingY / headLen
            }
            headingX = dashDirX
            headingY = dashDirY
            return true
        }
        return false
    }

    override val body = BoxBody(
        initialPosition = SceneOffset(initialX.sceneUnit, initialY.sceneUnit),
        initialSize = SceneSize(40f.sceneUnit, 40f.sceneUnit),
        initialPivot = SceneOffset(20f.sceneUnit, 20f.sceneUnit)
    )

    override val layerIndex: Int = 1
    override var drawingOrder: Float = 0f

    override fun onAdded(kubriko: Kubriko) {}

    fun canShoot(): Boolean = !isStunned && shootCooldownMs <= 0 && hasAmmo()

    fun fireWeapon(
        isSpreadShot: Boolean = false,
        overrideDirX: Float? = null,
        overrideDirY: Float? = null
    ): List<ProjectileVoxelActor> {
        val (rawX, rawY) = when {
            overrideDirX != null && overrideDirY != null -> Pair(overrideDirX, overrideDirY)
            isAimOverridden -> Pair(aimHeadingX, aimHeadingY)
            else -> Pair(headingX, headingY)
        }
        val len = sqrt(rawX * rawX + rawY * rawY).coerceAtLeast(0.001f)
        val nx = rawX / len
        val ny = rawY / len

        val projectiles = mutableListOf<ProjectileVoxelActor>()
        val weaponToFire = if (isSpreadShot) WeaponType.TRIPLE_SPREAD else activeWeapon
        val cooldownMod = if (hasJitCompiler) 0.65f else 1.0f

        when (weaponToFire) {
            WeaponType.PLASMA_FLAK_CANNON -> {
                shootCooldownMs = (WeaponType.PLASMA_FLAK_CANNON.fireRateMs * cooldownMod).toInt()
                val angle = kotlin.math.atan2(ny, nx)
                listOf(angle - 0.44f, angle - 0.22f, angle, angle + 0.22f, angle + 0.44f).forEach { a ->
                    projectiles.add(
                        ProjectileVoxelActor(
                            posX = posX + cos(a) * 26f,
                            posY = posY + sin(a) * 26f,
                            dirX = cos(a),
                            dirY = sin(a),
                            isPlayerProjectile = true,
                            damage = 2,
                            isSuperCharge = true
                        )
                    )
                }
            }
            WeaponType.TRIPLE_SPREAD -> {
                shootCooldownMs = (WeaponType.TRIPLE_SPREAD.fireRateMs * cooldownMod).toInt()
                val angle = kotlin.math.atan2(ny, nx)
                val angles = listOf(angle - 0.28f, angle, angle + 0.28f)
                angles.forEach { a ->
                    projectiles.add(
                        ProjectileVoxelActor(
                            posX = posX + cos(a) * 25f,
                            posY = posY + sin(a) * 25f,
                            dirX = cos(a),
                            dirY = sin(a),
                            isPlayerProjectile = true,
                            damage = 1,
                            isSuperCharge = true
                        )
                    )
                }
            }
            WeaponType.TWIN_PULSE -> {
                shootCooldownMs = (WeaponType.TWIN_PULSE.fireRateMs * cooldownMod).toInt()
                val perpX = -ny * 8f
                val perpY = nx * 8f
                projectiles.add(
                    ProjectileVoxelActor(
                        posX = posX + nx * 22f + perpX,
                        posY = posY + ny * 22f + perpY,
                        dirX = nx,
                        dirY = ny,
                        isPlayerProjectile = true,
                        damage = 1
                    )
                )
                projectiles.add(
                    ProjectileVoxelActor(
                        posX = posX + nx * 22f - perpX,
                        posY = posY + ny * 22f - perpY,
                        dirX = nx,
                        dirY = ny,
                        isPlayerProjectile = true,
                        damage = 1
                    )
                )
            }
            WeaponType.HEAVY_RAILGUN -> {
                shootCooldownMs = (WeaponType.HEAVY_RAILGUN.fireRateMs * cooldownMod).toInt()
                projectiles.add(
                    ProjectileVoxelActor(
                        posX = posX + nx * 28f,
                        posY = posY + ny * 28f,
                        dirX = nx,
                        dirY = ny,
                        isPlayerProjectile = true,
                        damage = 2,
                        isSuperCharge = true
                    )
                )
            }
            WeaponType.NOVA_360_BLASTER -> {
                shootCooldownMs = (WeaponType.NOVA_360_BLASTER.fireRateMs * cooldownMod).toInt()
                val count = 16
                val step = (2 * PI / count).toFloat()
                for (i in 0 until count) {
                    val a = i * step
                    projectiles.add(
                        ProjectileVoxelActor(
                            posX = posX + cos(a) * 26f,
                            posY = posY + sin(a) * 26f,
                            dirX = cos(a),
                            dirY = sin(a),
                            isPlayerProjectile = true,
                            damage = 1,
                            isSuperCharge = true
                        )
                    )
                }
            }
            WeaponType.RAPID_BYTE_BLASTER -> {
                shootCooldownMs = (WeaponType.RAPID_BYTE_BLASTER.fireRateMs * cooldownMod).toInt()
                projectiles.add(
                    ProjectileVoxelActor(
                        posX = posX + nx * 25f,
                        posY = posY + ny * 25f,
                        dirX = nx,
                        dirY = ny,
                        isPlayerProjectile = true,
                        damage = 1
                    )
                )
            }
        }
        return projectiles
    }

    fun pushOutOf(centerX: Float, centerY: Float, minDistance: Float) {
        val dx = posX - centerX
        val dy = posY - centerY
        val dist = sqrt(dx * dx + dy * dy)
        if (dist < minDistance && dist > 0.001f) {
            val overlap = minDistance - dist
            val targetX = (posX + (dx / dist) * overlap).coerceIn(60f, 3740f)
            val targetY = (posY + (dy / dist) * overlap).coerceIn(60f, 3740f)
            if (canMoveTo(targetX, targetY, collisionRadius)) {
                posX = targetX
                posY = targetY
            }
        }
    }

    override fun handleActiveKeys(activeKeys: ImmutableSet<Key>) {
        if (isMovementLocked) {
            keyMoveX = 0f
            keyMoveY = 0f
            return
        }

        var dx = 0f
        var dy = 0f

        if (Key.W in activeKeys || Key.DirectionUp in activeKeys) dy -= 1f
        if (Key.S in activeKeys || Key.DirectionDown in activeKeys) dy += 1f
        if (Key.A in activeKeys || Key.DirectionLeft in activeKeys) dx -= 1f
        if (Key.D in activeKeys || Key.DirectionRight in activeKeys) dx += 1f

        keyMoveX = dx
        keyMoveY = dy

        if (Key.Spacebar in activeKeys || Key.J in activeKeys) {
            onFireWeaponTriggered()
        }
        if (Key.K in activeKeys || Key.C in activeKeys || Key.E in activeKeys) {
            if (triggerDash()) {
                onDashTriggered()
            }
        }
    }

    var onGamepadButton: (GamepadButton) -> Unit = {}

    override fun handleGamepadState(gamepad: GamepadState) {
        if (isMovementLocked) {
            padMoveX = 0f
            padMoveY = 0f
            return
        }

        val deadzone = 0.15f
        var stickX = if (kotlin.math.abs(gamepad.leftStickX) > deadzone) gamepad.leftStickX else 0f
        var stickY = if (kotlin.math.abs(gamepad.leftStickY) > deadzone) gamepad.leftStickY else 0f

        if (gamepad.isPressed(GamepadButton.DPAD_UP)) stickY -= 1f
        if (gamepad.isPressed(GamepadButton.DPAD_DOWN)) stickY += 1f
        if (gamepad.isPressed(GamepadButton.DPAD_LEFT)) stickX -= 1f
        if (gamepad.isPressed(GamepadButton.DPAD_RIGHT)) stickX += 1f

        padMoveX = stickX.coerceIn(-1f, 1f)
        padMoveY = stickY.coerceIn(-1f, 1f)

        // Right trigger or shoulder button fires weapon
        if (gamepad.rightTrigger > 0.5f || gamepad.isPressed(GamepadButton.RIGHT_SHOULDER)) {
            onFireWeaponTriggered()
        }

        // Twin-Stick Gamepad Right Stick Aiming & Auto-Fire
        val rightDeadzone = 0.25f
        if (kotlin.math.abs(gamepad.rightStickX) > rightDeadzone || kotlin.math.abs(gamepad.rightStickY) > rightDeadzone) {
            setAimDirection(gamepad.rightStickX, gamepad.rightStickY)
            onFireWeaponTriggered()
        }
    }

    override fun onGamepadButtonPressed(gamepad: GamepadState, button: GamepadButton) {
        if (button == GamepadButton.WEST || button == GamepadButton.RIGHT_SHOULDER || button == GamepadButton.RIGHT_TRIGGER) {
            onFireWeaponTriggered()
        }
        if (button == GamepadButton.SOUTH || button == GamepadButton.LEFT_SHOULDER || button == GamepadButton.EAST) {
            if (triggerDash()) {
                onDashTriggered()
            }
        }
        onGamepadButton(button)
    }

    override fun onGamepadButtonReleased(gamepad: GamepadState, button: GamepadButton) {}

    override fun update(deltaTimeInMilliseconds: Int) {
        if (isPaused) {
            isMoving = false
            return
        }

        if (shootCooldownMs > 0) {
            shootCooldownMs -= deltaTimeInMilliseconds
        }

        if (dashCooldownMs > 0) {
            dashCooldownMs -= deltaTimeInMilliseconds
        }

        if (dashTimerMs > 0) {
            dashTimerMs -= deltaTimeInMilliseconds
        }

        if (invulnerableTimerMs > 0) {
            invulnerableTimerMs -= deltaTimeInMilliseconds
        }

        if (entranceShieldTimerMs > 0) {
            entranceShieldTimerMs -= deltaTimeInMilliseconds
            if (entranceShieldTimerMs <= 0) {
                entranceShieldTimerMs = 0
                isEntranceShieldActive = false
            }
        }

        if (hasRefactorFirewall && !isFirewallShieldActive) {
            firewallRechargeTimerMs -= deltaTimeInMilliseconds
            if (firewallRechargeTimerMs <= 0) {
                isFirewallShieldActive = true
            }
        }

        if (stunTimerRemainingMs > 0) {
            stunTimerRemainingMs -= deltaTimeInMilliseconds
            return
        }

        if (isDashing) {
            // High-speed autonomous dash propulsion (covering ~230 units in 240ms)
            val dashSpeed = 0.95f
            val stepDist = dashSpeed * deltaTimeInMilliseconds
            val targetX = (posX + dashDirX * stepDist).coerceIn(60f, 3740f)
            val targetY = (posY + dashDirY * stepDist).coerceIn(60f, 3740f)
            if (canMoveTo(targetX, targetY, collisionRadius)) {
                posX = targetX
                posY = targetY
            } else {
                dashTimerMs = 0 // Hit wall, halt dash immediately to prevent crossing
            }
            isMoving = true
            walkAnimProgress += deltaTimeInMilliseconds * 0.02f
        } else {
            if (isMovementLocked) {
                keyMoveX = 0f
                keyMoveY = 0f
                padMoveX = 0f
                padMoveY = 0f
                directMoveX = 0f
                directMoveY = 0f
            }
            // Combine inputs from keyboard, gamepad, and direct window events
            var moveX = keyMoveX + padMoveX + directMoveX
            var moveY = keyMoveY + padMoveY + directMoveY

            val len = sqrt(moveX * moveX + moveY * moveY)
            if (len > 0.05f) {
                val normalizedSpeed = if (len > 1f) 1f else len
                val inputDirX = (moveX / len) * normalizedSpeed
                val inputDirY = (moveY / len) * normalizedSpeed

                // Rotate input direction by camera angle so pressing W/Up always moves up on screen
                val worldDirX: Float
                val worldDirY: Float
                if (cameraAngleRad != 0f) {
                    val cosA = cos(cameraAngleRad)
                    val sinA = sin(cameraAngleRad)
                    worldDirX = inputDirX * cosA + inputDirY * sinA
                    worldDirY = -inputDirX * sinA + inputDirY * cosA
                } else {
                    worldDirX = inputDirX
                    worldDirY = inputDirY
                }

                val archetypeBaseSpeed = when (archetype) {
                    PlayerArchetype.KOTLIN_BOT -> 0.44f
                    PlayerArchetype.ARCH_DEV -> 0.30f
                    PlayerArchetype.CYBER_CODER -> 0.36f
                }
                val speedBoostMult = if (isSpeedBoostActive) 1.5f else 1.0f
                val jitSpeedMult = if (hasJitCompiler) 1.25f else 1.0f

                val effectiveSpeed = archetypeBaseSpeed * speedBoostMult * jitSpeedMult
                val stepDist = effectiveSpeed * deltaTimeInMilliseconds
                val dx = worldDirX * stepDist
                val dy = worldDirY * stepDist

                val targetX = (posX + dx).coerceIn(60f, 3740f)
                val targetY = (posY + dy).coerceIn(60f, 3740f)

                if (canMoveTo(targetX, targetY, collisionRadius)) {
                    posX = targetX
                    posY = targetY
                } else {
                    // Smooth diagonal wall sliding
                    if (dx != 0f && canMoveTo(targetX, posY, collisionRadius)) {
                        posX = targetX
                    }
                    if (dy != 0f && canMoveTo(posX, targetY, collisionRadius)) {
                        posY = targetY
                    }
                }

                headingX = worldDirX
                headingY = worldDirY
                isMoving = true

                walkAnimProgress += deltaTimeInMilliseconds * 0.012f
                if (walkAnimProgress > 2f * PI) walkAnimProgress -= (2f * PI.toFloat())
            } else {
                isMoving = false
                walkAnimProgress = 0f
            }
        }
        val targetGroundZ = groundZProvider(posX, posY)
        posZ += (targetGroundZ - posZ) * (0.02f * deltaTimeInMilliseconds).coerceIn(0.1f, 0.5f)

        body.position = SceneOffset(posX.sceneUnit, posY.sceneUnit)
        drawingOrder = -(posX + posY)
    }

    /**
     * Emits all 3D voxel cuboids composing the player character model.
     */
    fun emitVoxelCuboids(): List<VoxelCuboid> {
        // Blinking arcade visual effect during invulnerability i-frames
        if (invulnerableTimerMs > 0 && !isDashing && (invulnerableTimerMs / 70) % 2 == 0) {
            return emptyList()
        }

        val cuboids = mutableListOf<VoxelCuboid>()

        val legSwing = if (isMoving) sin(walkAnimProgress) * 10f else 0f
        val bobZ = if (isMoving) kotlin.math.abs(sin(walkAnimProgress * 2f)) * 3f else 0f

        // Dash Trail - Vivid afterimages
        if (isDashing) {
            val trailColor = when (archetype) {
                PlayerArchetype.KOTLIN_BOT -> Color(0xFF00E5FF)
                PlayerArchetype.ARCH_DEV -> Color(0xFFFFD600)
                PlayerArchetype.CYBER_CODER -> Color(0xFF7F52FF)
            }
            cuboids.add(VoxelCuboid(posX - dashDirX * 18f, posY - dashDirY * 18f, 10f, 18f, 18f, 18f, trailColor.copy(alpha = 0.75f)))
            cuboids.add(VoxelCuboid(posX - dashDirX * 36f, posY - dashDirY * 36f, 8f, 14f, 14f, 14f, trailColor.copy(alpha = 0.50f)))
            cuboids.add(VoxelCuboid(posX - dashDirX * 54f, posY - dashDirY * 54f, 6f, 10f, 10f, 10f, trailColor.copy(alpha = 0.25f)))
        }

        // JIT Compiler Afterburner Thrusters when moving
        if (hasJitCompiler && isMoving) {
            val flameColor = if (Random.nextBoolean()) Color(0xFFFF9100) else Color(0xFFFF3D00)
            cuboids.add(VoxelCuboid(posX - headingX * 12f, posY - headingY * 12f, 14f + bobZ, 6f, 6f, 6f, flameColor))
        }

        // Refactor Firewall Shield Bubble
        if (isFirewallShieldActive) {
            val shieldPulse = (sin(walkAnimProgress * 3f) * 2f)
            cuboids.add(VoxelCuboid(posX, posY, 16f, 34f + shieldPulse, 34f + shieldPulse, 34f, Color(0x6600E5FF)))
        }

        when (archetype) {
            PlayerArchetype.KOTLIN_BOT -> {
                // Mascot Robot: Hovering, metallic body, cyan visor
                val chassisColor = if (isStunned) Color(0xFFFF5252) else Color(0xFFCFD8DC)
                val neonCyan = Color(0xFF00E5FF)
                val hoverRingZ = 6f + sin(walkAnimProgress * 2f) * 2f

                // Shadow
                cuboids.add(VoxelCuboid(posX, posY, 0f, 26f, 26f, 2f, Color(0x33000000)))
                // Hover Thruster Ring
                cuboids.add(VoxelCuboid(posX, posY, hoverRingZ, 22f, 22f, 4f, neonCyan))
                // Main Torso Chassis
                cuboids.add(VoxelCuboid(posX, posY, hoverRingZ + 6f, 24f, 20f, 22f, chassisColor))
                // Robot Head / Display Screen
                cuboids.add(VoxelCuboid(posX, posY, hoverRingZ + 28f, 20f, 18f, 16f, Color(0xFF263238)))
                // Glowing Eye Strip
                val eyeX = posX + headingX * 8f
                val eyeY = posY + headingY * 8f
                cuboids.add(VoxelCuboid(eyeX, eyeY, hoverRingZ + 32f, 12f, 6f, 4f, Color(0xFF00E676)))
                // Twin Pulse Blasters on shoulders
                cuboids.add(VoxelCuboid(posX - 14f, posY, hoverRingZ + 18f, 6f, 14f, 6f, neonCyan))
                cuboids.add(VoxelCuboid(posX + 14f, posY, hoverRingZ + 18f, 6f, 14f, 6f, neonCyan))
            }
            PlayerArchetype.ARCH_DEV -> {
                // Heavy Armor: Dark navy, gold trims, heavy shield, shoulder railgun
                val armorColor = if (isStunned) Color(0xFFFF5252) else Color(0xFF1A237E)
                val goldTrim = Color(0xFFFFD600)

                // Shadow
                cuboids.add(VoxelCuboid(posX, posY, 0f, 32f, 32f, 2f, Color(0x33000000)))
                // Heavy Boots
                cuboids.add(VoxelCuboid(posX - 7f, posY + legSwing * 0.3f, 2f, 10f, 10f, 14f, Color(0xFF212121)))
                cuboids.add(VoxelCuboid(posX + 7f, posY - legSwing * 0.3f, 2f, 10f, 10f, 14f, Color(0xFF212121)))
                // Armored Torso + Shoulder Pauldrons
                cuboids.add(VoxelCuboid(posX, posY, 16f + bobZ, 28f, 20f, 24f, armorColor))
                cuboids.add(VoxelCuboid(posX - 15f, posY, 26f + bobZ, 8f, 10f, 10f, goldTrim))
                cuboids.add(VoxelCuboid(posX + 15f, posY, 26f + bobZ, 8f, 10f, 10f, goldTrim))
                // Heavy Railgun Cannon
                val cannonX = posX + headingX * 16f
                val cannonY = posY + headingY * 16f
                cuboids.add(VoxelCuboid(cannonX, cannonY, 24f + bobZ, 12f, 12f, 14f, Color(0xFF455A64)))
                // Helmet & Gold Visor
                cuboids.add(VoxelCuboid(posX, posY, 40f + bobZ, 20f, 20f, 18f, armorColor))
                cuboids.add(VoxelCuboid(posX + headingX * 8f, posY + headingY * 8f, 44f + bobZ, 12f, 8f, 6f, goldTrim))
            }
            PlayerArchetype.CYBER_CODER -> {
                // Cyber Coder: Kotlin Purple, Cap, Jetpack, Cyan Visor
                val primaryColor = when {
                    isStunned -> Color(0xFFFF5252)
                    isShieldActive -> Color(0xFF00E5FF)
                    isSpeedBoostActive -> Color(0xFFFFEA00)
                    else -> Color(0xFF7F52FF)
                }
                val secondaryColor = Color(0xFF212121)
                val skinColor = Color(0xFFFFCC80)

                // Shadow
                cuboids.add(VoxelCuboid(posX, posY, 0f, 28f, 28f, 2f, Color(0x33000000)))
                // Legs
                cuboids.add(VoxelCuboid(posX - 6f, posY + legSwing * 0.4f, 2f, 8f, 8f, 14f, secondaryColor))
                cuboids.add(VoxelCuboid(posX + 6f, posY - legSwing * 0.4f, 2f, 8f, 8f, 14f, secondaryColor))
                // Torso / Hoodie
                cuboids.add(VoxelCuboid(posX, posY, 16f + bobZ, 22f, 16f, 20f, primaryColor))
                // Jetpack Backpack
                cuboids.add(VoxelCuboid(posX - headingX * 8f, posY - headingY * 8f, 18f + bobZ, 12f, 10f, 14f, Color(0xFF37474F)))
                // Blaster
                val blasterX = posX + headingX * 14f
                val blasterY = posY + headingY * 14f
                cuboids.add(VoxelCuboid(blasterX, blasterY, 18f + bobZ, 8f, 8f, 8f, Color(0xFF00E5FF)))
                // Head & Cap
                cuboids.add(VoxelCuboid(posX, posY, 38f + bobZ, 18f, 18f, 16f, skinColor))
                cuboids.add(VoxelCuboid(posX, posY, 48f + bobZ, 20f, 20f, 8f, primaryColor))
                // Visor
                val visorX = posX + (if (headingX >= 0) 6f else -6f)
                val visorY = posY + (if (headingY >= 0) 6f else -6f)
                cuboids.add(VoxelCuboid(visorX, visorY, 42f + bobZ, 8f, 8f, 6f, Color(0xFF00E5FF)))
            }
        }

        // Entrance Invulnerability Shield Sphere & Cyberpunk Teleport Beam-In
        if (isEntranceShieldActive) {
            val shieldPulse = sin(entranceShieldTimerMs * 0.008f) * 0.15f + 0.85f
            val shieldSize = 52f * shieldPulse
            cuboids.add(
                VoxelCuboid(
                    x = posX,
                    y = posY,
                    z = 4f,
                    sizeX = shieldSize,
                    sizeY = shieldSize,
                    sizeZ = 46f,
                    baseColor = Color(0x5500E5FF)
                )
            )
            cuboids.add(
                VoxelCuboid(
                    x = posX,
                    y = posY,
                    z = 16f,
                    sizeX = shieldSize * 0.8f,
                    sizeY = shieldSize * 0.8f,
                    sizeZ = 22f,
                    baseColor = Color(0x7780D8FF)
                )
            )
            // Teleport descending beam from sky during initial 1.5 seconds (5000ms down to 3500ms)
            if (entranceShieldTimerMs > 3500) {
                val beamAlpha = ((entranceShieldTimerMs - 3500) / 1500f).coerceIn(0f, 1f)
                val beamProgress = 1f - beamAlpha
                val beamZ = 30f + (1f - beamProgress) * 35f
                cuboids.add(
                    VoxelCuboid(
                        x = posX,
                        y = posY,
                        z = beamZ,
                        sizeX = 14f,
                        sizeY = 14f,
                        sizeZ = 120f,
                        baseColor = Color(0xAA00E5FF).copy(alpha = 0.85f * beamAlpha)
                    )
                )
                // Expanding arrival shockwave ring on the floor as hero touches down
                val ringRadius = 18f + beamProgress * 32f
                cuboids.add(
                    VoxelCuboid(
                        x = posX,
                        y = posY,
                        z = 1f,
                        sizeX = ringRadius,
                        sizeY = ringRadius,
                        sizeZ = 2f,
                        baseColor = Color(0xFF00E5FF).copy(alpha = 0.7f * (1f - beamProgress))
                    )
                )
            }
        }

        if (posZ != 0f) {
            return cuboids.map { it.copy(z = it.z + posZ) }
        }

        return cuboids
    }

    override fun DrawScope.draw() {}
}
