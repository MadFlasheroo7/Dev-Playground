package pro.jayeshseth.bugrush.trivia

import kotlinx.serialization.Serializable

@Serializable
enum class QuestionTier(val displayName: String, val pointValue: Int) {
    TIER_1_EASY("Syntax & Basics", 500),
    TIER_2_MEDIUM("Coroutines & Compose", 1500),
    TIER_3_HARD("Advanced & Multiplatform", 3500)
}

@Serializable
enum class PowerUpType(
    val title: String,
    val description: String,
    val durationSeconds: Int
) {
    SPEED_MAGNET("Hyper Speed & Magnet", "Move 2x faster & vacuum all nearby gems!", 12),
    FREEZE_SHIELD("Bug Freeze & Shield", "Freezes all bugs in place & grants hazard immunity!", 10),
    MEGA_FRENZY_NUKE("5x Multiplier & Bug Nuke", "Instantly destroys all bugs & boosts score to 5x!", 15),
    OVERCHARGE_INVINCIBLE("Overcharge God Mode", "Full damage immunity & supercharged blaster!", 15)
}

@Serializable
data class TriviaQuestion(
    val id: String,
    val tier: QuestionTier,
    val powerUp: PowerUpType,
    val question: String,
    val options: List<String>,
    val correctIndex: Int,
    val explanation: String
)
