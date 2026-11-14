package pro.jayeshseth.bugrush.game.actors

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.actor.Actor
import com.pandulapeter.kubriko.actor.body.BoxBody
import com.pandulapeter.kubriko.actor.traits.Dynamic
import com.pandulapeter.kubriko.actor.traits.Visible
import pro.jayeshseth.bugrush.game.renderer.VoxelCuboid
import pro.jayeshseth.bugrush.trivia.QuestionTier
import com.pandulapeter.kubriko.helpers.extensions.sceneUnit
import com.pandulapeter.kubriko.types.SceneOffset
import com.pandulapeter.kubriko.types.SceneSize
import kotlin.math.PI
import kotlin.math.sin

class TerminalBeaconActor(
    val posX: Float,
    val posY: Float,
    val tier: QuestionTier
) : Actor, Visible, Dynamic {

    override val isAlwaysActive: Boolean get() = true
    override val isAlwaysVisible: Boolean get() = true

    private var pulseAnim: Float = 0f
    var isAvailable: Boolean = true

    override val body = BoxBody(
        initialPosition = SceneOffset(posX.sceneUnit, posY.sceneUnit),
        initialSize = SceneSize(44f.sceneUnit, 44f.sceneUnit),
        initialPivot = SceneOffset(22f.sceneUnit, 22f.sceneUnit)
    )

    override val layerIndex: Int = 1
    override var drawingOrder: Float = 0f

    override fun onAdded(kubriko: Kubriko) {}

    override fun update(deltaTimeInMilliseconds: Int) {
        pulseAnim += deltaTimeInMilliseconds * 0.004f
        if (pulseAnim > 2f * PI) pulseAnim -= (2f * PI.toFloat())

        body.position = SceneOffset(posX.sceneUnit, posY.sceneUnit)
        drawingOrder = -(posX + posY)
    }

    fun emitVoxelCuboids(): List<VoxelCuboid> {
        val tierColor = when (tier) {
            QuestionTier.TIER_1_EASY -> Color(0xFF00E676) // Bright Green
            QuestionTier.TIER_2_MEDIUM -> Color(0xFF2979FF) // Bright Blue
            QuestionTier.TIER_3_HARD -> Color(0xFFFFD600) // Bright Gold
        }

        val basePillarColor = Color(0xFF263238)
        val hoverZ = 28f + sin(pulseAnim * 2f) * 6f

        return listOf(
            // Foundation Base
            VoxelCuboid(posX, posY, 0f, 36f, 36f, 6f, basePillarColor),
            // Middle Pedestal
            VoxelCuboid(posX, posY, 6f, 20f, 20f, 16f, basePillarColor),
            // Terminal Screen Console
            VoxelCuboid(posX, posY, 22f, 26f, 26f, 4f, Color(0xFF37474F)),
            // Floating Holographic Core (pulsing)
            VoxelCuboid(posX, posY, hoverZ, 16f, 16f, 16f, tierColor),
            // Glowing Top Crown
            VoxelCuboid(posX, posY, hoverZ + 14f, 8f, 8f, 6f, tierColor)
        )
    }

    override fun DrawScope.draw() {}
}
