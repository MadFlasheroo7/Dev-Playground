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
 * Dynamic Laser Sweep Fence with twin emitter pylons.
 * Cycles ON (4000ms) and OFF (2000ms).
 * Vaporizes bugs that cross the laser line while active.
 */
class LaserFenceVoxelActor(
    val x1: Float,
    val y1: Float,
    val x2: Float,
    val y2: Float,
    val posZ: Float = 0f,
    val onDurationMs: Int = 4000,
    val offDurationMs: Int = 2000,
    val laserColor: Color = Color(0xFFFF1744)
) : Actor, Visible, Dynamic {

    override val isAlwaysActive: Boolean get() = true
    override val isAlwaysVisible: Boolean get() = true

    val midX = (x1 + x2) / 2f
    val midY = (y1 + y2) / 2f

    override val body = BoxBody(
        initialPosition = SceneOffset(midX.sceneUnit, midY.sceneUnit),
        initialSize = SceneSize(120f.sceneUnit, 120f.sceneUnit),
        initialPivot = SceneOffset(60f.sceneUnit, 60f.sceneUnit)
    )

    override val layerIndex: Int = 1
    override var drawingOrder: Float = -(midX + midY)

    var cycleTimerMs: Int = 0
    var isLaserActive: Boolean = true
    val pylonRadius: Float = 16f
    private var animTime: Float = 0f

    override fun onAdded(kubriko: Kubriko) {}

    override fun update(deltaTimeInMilliseconds: Int) {
        animTime += deltaTimeInMilliseconds * 0.008f
        if (animTime > 2f * PI) animTime -= (2f * PI.toFloat())

        cycleTimerMs += deltaTimeInMilliseconds
        val totalCycle = onDurationMs + offDurationMs
        val phase = cycleTimerMs % totalCycle
        isLaserActive = phase < onDurationMs
    }

    /**
     * Checks if a world position (px, py) intersects the active laser beam segment.
     */
    fun isPointNearBeam(px: Float, py: Float, tolerance: Float = 20f): Boolean {
        if (!isLaserActive) return false
        val dx = x2 - x1
        val dy = y2 - y1
        val lengthSq = dx * dx + dy * dy
        if (lengthSq == 0f) {
            val distSq = (px - x1) * (px - x1) + (py - y1) * (py - y1)
            return distSq <= tolerance * tolerance
        }
        val t = (((px - x1) * dx + (py - y1) * dy) / lengthSq).coerceIn(0f, 1f)
        val projX = x1 + t * dx
        val projY = y1 + t * dy
        val distSq = (px - projX) * (px - projX) + (py - projY) * (py - projY)
        return distSq <= tolerance * tolerance
    }

    fun emitVoxelCuboids(): List<VoxelCuboid> {
        val cuboids = mutableListOf<VoxelCuboid>()
        val pulse = (sin(animTime * 6f) + 1f) * 0.5f

        val pylonColor = Color(0xFF21272E)
        val pylonTrim = Color(0xFF37474F)
        val emitterGlow = if (isLaserActive) laserColor else Color(0xFF455A64)

        // Helper to emit a single emitter pylon
        fun emitPylon(px: Float, py: Float) {
            // Shadow
            cuboids.add(VoxelCuboid(px, py, posZ + 0f, 24f, 24f, 2f, Color(0x33000000)))
            // Base Pedestal
            cuboids.add(VoxelCuboid(px, py, posZ + 1f, 22f, 22f, 6f, pylonColor))
            // Column Shaft
            cuboids.add(VoxelCuboid(px, py, posZ + 7f, 14f, 14f, 18f, pylonTrim))
            // Emitter Head
            cuboids.add(VoxelCuboid(px, py, posZ + 25f, 18f, 18f, 8f, pylonColor))
            // Glowing Focus Lens
            cuboids.add(VoxelCuboid(px, py, posZ + 28f, 10f, 10f, 6f, emitterGlow))
        }

        emitPylon(x1, y1)
        emitPylon(x2, y2)

        // Dynamic Laser Beam Segments
        if (isLaserActive) {
            val segments = 8
            val beamAlpha = 0.75f + 0.25f * pulse
            val activeBeamColor = laserColor.copy(alpha = beamAlpha)
            val coreColor = Color.White.copy(alpha = 0.9f)

            for (i in 0..segments) {
                val frac = i.toFloat() / segments.toFloat()
                val bx = x1 + (x2 - x1) * frac
                val by = y1 + (y2 - y1) * frac
                val bz = posZ + 30f

                // Outer Laser Aura
                cuboids.add(
                    VoxelCuboid(
                        x = bx,
                        y = by,
                        z = bz - 2f,
                        sizeX = 14f,
                        sizeY = 14f,
                        sizeZ = 8f,
                        baseColor = activeBeamColor
                    )
                )
                // Intense Central Core
                cuboids.add(
                    VoxelCuboid(
                        x = bx,
                        y = by,
                        z = bz,
                        sizeX = 6f,
                        sizeY = 6f,
                        sizeZ = 4f,
                        baseColor = coreColor
                    )
                )
            }
        }

        return cuboids
    }

    override fun DrawScope.draw() {}
}
