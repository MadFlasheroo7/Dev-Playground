package pro.jayeshseth.bugrush.game.actors

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.actor.Actor
import com.pandulapeter.kubriko.actor.body.BoxBody
import com.pandulapeter.kubriko.actor.traits.Dynamic
import com.pandulapeter.kubriko.actor.traits.Visible
import com.pandulapeter.kubriko.helpers.extensions.sceneUnit
import com.pandulapeter.kubriko.types.SceneOffset
import com.pandulapeter.kubriko.types.SceneSize
import pro.jayeshseth.bugrush.game.renderer.VoxelCuboid
import kotlin.math.PI
import kotlin.math.sin

/**
 * Floating, spinning 3D voxel gun pickup actor in the arena.
 * Walking over it equips the weapon archetype and removes the pickup.
 */
class GunDropVoxelActor(
    var posX: Float,
    var posY: Float,
    var posZ: Float = 0f,
    val weaponType: WeaponType
) : Actor, Visible, Dynamic {

    override val isAlwaysActive: Boolean get() = true
    override val isAlwaysVisible: Boolean get() = true

    var isCollected: Boolean = false
    val collisionRadius: Float = 24f
    private var animTime: Float = 0f

    override val body = BoxBody(
        initialPosition = SceneOffset(posX.sceneUnit, posY.sceneUnit),
        initialSize = SceneSize(28f.sceneUnit, 28f.sceneUnit),
        initialPivot = SceneOffset(14f.sceneUnit, 14f.sceneUnit)
    )

    override val layerIndex: Int = 1
    override var drawingOrder: Float = -(posX + posY)

    override fun onAdded(kubriko: Kubriko) {}

    override fun update(deltaTimeInMilliseconds: Int) {
        if (isCollected) return
        animTime += deltaTimeInMilliseconds * 0.003f
        if (animTime > 2f * PI) animTime -= (2f * PI.toFloat())
        drawingOrder = -(posX + posY)
    }

    /**
     * Emits 3D voxel cuboids representing the floating weapon pickup
     * with glowing pedestal, weapon chassis, and emitter.
     */
    fun emitVoxelCuboids(): List<VoxelCuboid> {
        if (isCollected) return emptyList()

        val bobZ = sin(animTime * 2f) * 4f + 14f

        val glowColor = when (weaponType) {
            WeaponType.RAPID_BYTE_BLASTER -> Color(0xFF00E5FF)
            WeaponType.TWIN_PULSE -> Color(0xFF00E5FF) // Neon Cyan
            WeaponType.TRIPLE_SPREAD -> Color(0xFFFFB300) // Bright Amber
            WeaponType.HEAVY_RAILGUN -> Color(0xFFFF1744) // Crimson Red
            WeaponType.PLASMA_FLAK_CANNON -> Color(0xFF00E676) // Acid Green
            WeaponType.NOVA_360_BLASTER -> Color(0xFFD500F9) // Holographic Magenta / Purple
        }

        val baseColor = when (weaponType) {
            WeaponType.RAPID_BYTE_BLASTER -> Color(0xFF37474F)
            WeaponType.TWIN_PULSE -> Color(0xFF00B0FF)
            WeaponType.TRIPLE_SPREAD -> Color(0xFFFF8F00)
            WeaponType.HEAVY_RAILGUN -> Color(0xFFB71C1C)
            WeaponType.PLASMA_FLAK_CANNON -> Color(0xFF00C853)
            WeaponType.NOVA_360_BLASTER -> Color(0xFFAA00FF)
        }

        val cuboids = mutableListOf<VoxelCuboid>()

        // Floor holographic beacon pedestal / shadow
        cuboids.add(
            VoxelCuboid(
                x = posX,
                y = posY,
                z = 1f,
                sizeX = 26f,
                sizeY = 26f,
                sizeZ = 2f,
                baseColor = glowColor.copy(alpha = 0.35f),
                isFloor = true
            )
        )
        // Pulsing core beam
        cuboids.add(
            VoxelCuboid(
                x = posX,
                y = posY,
                z = 3f,
                sizeX = 14f,
                sizeY = 14f,
                sizeZ = 4f,
                baseColor = glowColor.copy(alpha = 0.65f)
            )
        )

        // Floating Gun Shape (archetype body + barrels)
        // Central weapon receiver
        cuboids.add(
            VoxelCuboid(
                x = posX,
                y = posY,
                z = bobZ,
                sizeX = 18f,
                sizeY = 14f,
                sizeZ = 10f,
                baseColor = baseColor
            )
        )
        // Glowing barrel / emitter
        cuboids.add(
            VoxelCuboid(
                x = posX,
                y = posY + 8f,
                z = bobZ + 1f,
                sizeX = 8f,
                sizeY = 12f,
                sizeZ = 7f,
                baseColor = glowColor
            )
        )
        // Top holographic glow crest
        cuboids.add(
            VoxelCuboid(
                x = posX,
                y = posY,
                z = bobZ + 7f,
                sizeX = 12f,
                sizeY = 10f,
                sizeZ = 4f,
                baseColor = glowColor.copy(alpha = 0.85f)
            )
        )

        return cuboids
    }

    override fun DrawScope.draw() {}
}
