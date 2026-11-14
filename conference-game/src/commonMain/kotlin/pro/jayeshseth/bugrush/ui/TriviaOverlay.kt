package pro.jayeshseth.bugrush.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pro.jayeshseth.bugrush.trivia.PowerUpType
import pro.jayeshseth.bugrush.trivia.QuestionTier
import pro.jayeshseth.bugrush.trivia.TriviaQuestion
import pro.jayeshseth.bugrush.trivia.VersionPerk

@Composable
fun TriviaOverlay(
    question: TriviaQuestion,
    perkOptions: List<VersionPerk>? = null,
    lockoutRemainingMs: Int = 0,
    onAnswerSelected: (Int) -> Unit,
    onPerkSelected: (VersionPerk) -> Unit = {}
) {
    val tierColor = when (question.tier) {
        QuestionTier.TIER_1_EASY -> Color(0xFF00E676)
        QuestionTier.TIER_2_MEDIUM -> Color(0xFF2979FF)
        QuestionTier.TIER_3_HARD -> Color(0xFFFFD600)
    }

    val optionKeys = listOf("1 / A", "2 / B", "3 / X", "4 / Y")
    val isLocked = lockoutRemainingMs > 0

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0x770A0E14)),
        contentAlignment = Alignment.Center
    ) {
        if (perkOptions != null && perkOptions.isNotEmpty()) {
            // Roguelite Perk Upgrade Selection Modal
            Column(
                modifier = Modifier
                    .width(720.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFF161F2E))
                    .border(2.dp, Color(0xFF00E5FF), RoundedCornerShape(20.dp))
                    .padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header Badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0x3300E5FF))
                        .border(1.dp, Color(0xFF00E5FF), RoundedCornerShape(8.dp))
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "🚀 COMPILE VERSION UPGRADE • CHOOSE 1 PERK",
                        color = Color(0xFF00E5FF),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = ArcadeTheme.pixelHeader
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "CORRECT ANSWER! INSTALL ROGUELITE MODULE:",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = ArcadeTheme.pixelHeader,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(20.dp))

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    perkOptions.forEachIndexed { index, perk ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF1F2D40))
                                .border(1.5.dp, Color(0xFF00E5FF), RoundedCornerShape(12.dp))
                                .clickable { onPerkSelected(perk) }
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFF00E5FF))
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = "${index + 1}",
                                    color = Color(0xFF0A0E14),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Black,
                                    fontFamily = ArcadeTheme.pixelHeader
                                )
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = perk.title,
                                        color = Color.White,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = ArcadeTheme.pixelHeader
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = perk.badge,
                                        color = Color(0xFF00E676),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontFamily = ArcadeTheme.pixelHeader
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = perk.subtitle,
                                    color = Color(0xFFB0BEC5),
                                    fontSize = 13.sp,
                                    fontFamily = ArcadeTheme.terminalBody
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Select with mouse, keyboard (1-3), or controller face buttons",
                    color = Color(0xFF90A4AE),
                    fontSize = 12.sp,
                    fontFamily = ArcadeTheme.terminalBody
                )
            }
        } else {
            // Trivia Question Modal
            Column(
                modifier = Modifier
                    .width(680.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFF161F2E))
                    .border(2.dp, tierColor, RoundedCornerShape(20.dp))
                    .padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Tier Badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(tierColor.copy(alpha = 0.2f))
                        .border(1.dp, tierColor, RoundedCornerShape(8.dp))
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "${question.tier.displayName.uppercase()} • REWARD: ${question.powerUp.title.uppercase()} + PERK UPGRADE",
                        color = tierColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = ArcadeTheme.pixelHeader
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // High-Tension Danger Alert Banner
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0x33FF1744))
                        .border(1.dp, Color(0xFFFF1744), RoundedCornerShape(8.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "⚠️ WARNING: VULNERABLE STATE • 2X DAMAGE FROM BUGS!",
                        color = Color(0xFFFF5252),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = ArcadeTheme.pixelHeader
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Decryption Lockout Progress Bar
                if (isLocked) {
                    val sec = lockoutRemainingMs / 1000
                    val tenths = (lockoutRemainingMs % 1000) / 100
                    val progress = (2000 - lockoutRemainingMs).coerceIn(0, 2000) / 2000f
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "DECRYPTING SYSTEM... ${sec}.${tenths}s",
                            color = Color(0xFF00E5FF),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = ArcadeTheme.pixelHeader
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(Color(0xFF0F1722))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(fraction = progress)
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(Color(0xFF00E5FF))
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }

                // Question Text
                Text(
                    text = question.question,
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Normal,
                    fontFamily = ArcadeTheme.terminalBody,
                    textAlign = TextAlign.Center,
                    lineHeight = 26.sp
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Options List
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    question.options.forEachIndexed { index, optionText ->
                        val optionAlpha = if (isLocked) 0.35f else 1.0f
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF202D40).copy(alpha = optionAlpha))
                                .border(1.dp, if (isLocked) Color(0xFF334764).copy(alpha = 0.4f) else Color(0xFF334764), RoundedCornerShape(12.dp))
                                .clickable(enabled = !isLocked) { onAnswerSelected(index) }
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFF0F1722).copy(alpha = optionAlpha))
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = optionKeys.getOrElse(index) { "$index" },
                                    color = if (isLocked) tierColor.copy(alpha = 0.4f) else tierColor,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = ArcadeTheme.pixelHeader
                                )
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            Text(
                                text = optionText,
                                color = if (isLocked) Color(0xFFECEFF1).copy(alpha = 0.4f) else Color(0xFFECEFF1),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Normal,
                                fontFamily = ArcadeTheme.terminalBody
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = if (isLocked) "DECRYPTION LOCKOUT IN PROGRESS • AVOID ATTACKING BUGS" else "Select with mouse, keyboard (1-4 or A-D), or controller face buttons",
                    color = if (isLocked) Color(0xFFFF5252) else Color(0xFF90A4AE),
                    fontSize = 12.sp,
                    fontFamily = ArcadeTheme.terminalBody
                )
            }
        }
    }
}

@Preview
@Composable
fun TriviaQuestionModalPreview() {
    TriviaOverlay(
        question = TriviaQuestion(
            id = "q_preview_1",
            tier = QuestionTier.TIER_2_MEDIUM,
            powerUp = PowerUpType.SPEED_MAGNET,
            question = "What does the 'crossinline' modifier do on a higher-order lambda parameter?",
            options = listOf(
                "Prevents non-local returns from the passed lambda",
                "Forces execution on a separate coroutine thread",
                "Caches the compiled bytecode across multiple calls",
                "Converts the lambda into a Java Functional Interface"
            ),
            correctIndex = 0,
            explanation = "crossinline preserves inlining benefits while disallowing non-local returns."
        ),
        perkOptions = null,
        onAnswerSelected = {},
        onPerkSelected = {}
    )
}

@Preview
@Composable
fun TriviaPerkSelectionModalPreview() {
    TriviaOverlay(
        question = TriviaQuestion(
            id = "q_preview_2",
            tier = QuestionTier.TIER_3_HARD,
            powerUp = PowerUpType.OVERCHARGE_INVINCIBLE,
            question = "Dummy question",
            options = listOf("A", "B", "C", "D"),
            correctIndex = 0,
            explanation = ""
        ),
        perkOptions = listOf(
            VersionPerk.GARBAGE_COLLECTOR_DRONE,
            VersionPerk.COROUTINES_TURRET,
            VersionPerk.JIT_COMPILER
        ),
        onAnswerSelected = {},
        onPerkSelected = {}
    )
}
