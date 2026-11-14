package pro.jayeshseth.bugrush.leaderboard

import kotlinx.serialization.Serializable

@Serializable
data class LeaderboardEntry(
    val playerName: String,
    val score: Int,
    val gemsCollected: Int,
    val triviaCorrect: Int,
    val triviaAttempted: Int,
    val highestMultiplier: Int,
    val timestamp: Long
)
