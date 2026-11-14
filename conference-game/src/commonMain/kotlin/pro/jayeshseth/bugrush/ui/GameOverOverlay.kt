package pro.jayeshseth.bugrush.ui

import androidx.compose.animation.animateColor
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.math.sin

@Composable
fun GameOverOverlay(
    finalScore: Int,
    gemsCollected: Int,
    bugsSquashed: Int = 0,
    triviaCorrect: Int,
    triviaAttempted: Int,
    highestMultiplier: Int,
    playerHandle: String = "Dev_Kotlin",
    initialPlayerName: String = playerHandle,
    onTextFieldFocusChanged: (Boolean) -> Unit = {},
    onSubmitScore: (String) -> Unit = {},
    onViewLeaderboard: () -> Unit = { onSubmitScore(playerHandle) }
) {
    val activeHandle = playerHandle.ifBlank { initialPlayerName.ifBlank { "Dev_Kotlin" } }

    val rankTitle = when {
        finalScore >= 22000 -> "PRINCIPAL BUG HUNTER (GODLIKE)"
        finalScore >= 14000 -> "SENIOR KOTLIN ARCHITECT"
        finalScore >= 7000 -> "STAFF COROUTINES RUNNER"
        else -> "BYTE INITIATE"
    }

    val rankColor = when {
        finalScore >= 22000 -> Color(0xFFFFD600)
        finalScore >= 14000 -> Color(0xFF00E5FF)
        finalScore >= 7000 -> Color(0xFF76FF03)
        else -> Color(0xFF90A4AE)
    }

    // 1. Entrance Physics Animations
    val modalScale = remember { Animatable(0.85f) }
    val modalAlpha = remember { Animatable(0f) }
    var startScoreRoll by remember { mutableStateOf(false) }
    var startStatBars by remember { mutableStateOf(false) }
    val stampScale = remember { Animatable(2.3f) }
    val stampAlpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        modalAlpha.animateTo(1f, tween(300))
        modalScale.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        )
        delay(150)
        startScoreRoll = true
        delay(400)
        startStatBars = true
        delay(550)
        // Slam down the Rank Badge with heavy impact
        stampAlpha.snapTo(1f)
        stampScale.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMedium
            )
        )
    }

    // 2. Rolling Score Animation
    val animatedScore by animateIntAsState(
        targetValue = if (startScoreRoll) finalScore else 0,
        animationSpec = tween(durationMillis = 1400, easing = LinearEasing),
        label = "score_count_up"
    )

    // 3. Stat Bar Progress Animations
    val bugsProgress by animateFloatAsState(
        targetValue = if (startStatBars) (bugsSquashed / 50f).coerceIn(0.08f, 1f) else 0f,
        animationSpec = tween(900, easing = FastOutSlowInEasing),
        label = "bugs_progress"
    )
    val gemsProgress by animateFloatAsState(
        targetValue = if (startStatBars) (gemsCollected / 60f).coerceIn(0.08f, 1f) else 0f,
        animationSpec = tween(900, easing = FastOutSlowInEasing),
        label = "gems_progress"
    )
    val triviaProgress by animateFloatAsState(
        targetValue = if (startStatBars) {
            if (triviaAttempted > 0) (triviaCorrect.toFloat() / triviaAttempted).coerceIn(0.08f, 1f) else 0.08f
        } else 0f,
        animationSpec = tween(900, easing = FastOutSlowInEasing),
        label = "trivia_progress"
    )
    val multProgress by animateFloatAsState(
        targetValue = if (startStatBars) (highestMultiplier / 5f).coerceIn(0.2f, 1f) else 0f,
        animationSpec = tween(900, easing = FastOutSlowInEasing),
        label = "mult_progress"
    )

    // 4. Background Cyber-Confetti Dust
    val infiniteTransition = rememberInfiniteTransition(label = "game_over_infinite")
    val confettiAnim by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(tween(10000, easing = LinearEasing), RepeatMode.Restart),
        label = "confetti"
    )
    val buttonGlowColor by infiniteTransition.animateColor(
        initialValue = Color(0xFF00E5FF),
        targetValue = Color(0xFFFFD600),
        animationSpec = infiniteRepeatable(tween(750), RepeatMode.Reverse),
        label = "btn_glow"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xEE0A0E14)),
        contentAlignment = Alignment.Center
    ) {
        // Floating cyber confetti canvas
        Canvas(modifier = Modifier.fillMaxSize()) {
            val count = 28
            for (i in 0 until count) {
                val seedX = (i * 137.5f) % size.width
                val baseSpeed = 40f + (i % 5) * 15f
                val currentY = (size.height + (i * 90f) - ((confettiAnim * baseSpeed / 100f) % (size.height + 100f))) % size.height
                val driftX = seedX + sin((confettiAnim / 80f) + i) * 24f
                val pColor = when (i % 3) {
                    0 -> Color(0xFF00E5FF).copy(alpha = 0.45f)
                    1 -> Color(0xFFFFD600).copy(alpha = 0.45f)
                    else -> Color(0xFF00E676).copy(alpha = 0.45f)
                }
                val pSize = 4f + (i % 3) * 2f
                drawRect(
                    color = pColor,
                    topLeft = Offset(driftX, currentY),
                    size = Size(pSize, pSize)
                )
            }
        }

        // Modal Box with Spring Entrance (78% screen width)
        Column(
            modifier = Modifier
                .fillMaxWidth(0.78f)
                .widthIn(min = 680.dp, max = 920.dp)
                .graphicsLayer {
                    scaleX = modalScale.value
                    scaleY = modalScale.value
                    alpha = modalAlpha.value
                }
                .clip(RoundedCornerShape(22.dp))
                .background(Color(0xFF131A26))
                .border(2.dp, Color(0xFF00E5FF).copy(alpha = 0.85f), RoundedCornerShape(22.dp))
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header & Operator Chip
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "DIAGNOSTIC REPORT // RUN COMPLETE",
                    color = Color(0xFFFFD600),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = ArcadeTheme.pixelHeader,
                    letterSpacing = 1.sp
                )

                // Operator Pill
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF1E293B))
                        .border(1.dp, Color(0xFF00E5FF).copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "OPERATOR:",
                        color = Color(0xFF90A4AE),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = ArcadeTheme.terminalBody
                    )
                    Text(
                        text = activeHandle,
                        color = Color(0xFF00E5FF),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = ArcadeTheme.pixelHeader
                    )
                }
            }

            // Rolling Animated Score Callout
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "FINAL SCORE",
                    color = Color(0xFF80DEEA),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = ArcadeTheme.terminalBody,
                    letterSpacing = 2.sp
                )
                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "$animatedScore",
                        color = Color(0xFF00E5FF),
                        fontSize = 52.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = ArcadeTheme.pixelHeader,
                        letterSpacing = 2.sp
                    )
                    Text(
                        text = "PTS",
                        color = Color(0xFFFFD600),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = ArcadeTheme.pixelHeader,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
            }

            // Rank Badge Plaque
            Box(
                modifier = Modifier
                    .graphicsLayer {
                        scaleX = stampScale.value
                        scaleY = stampScale.value
                        alpha = stampAlpha.value
                    }
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF1A2333))
                    .border(2.dp, rankColor, RoundedCornerShape(10.dp))
                    .padding(horizontal = 24.dp, vertical = 10.dp)
            ) {
                Text(
                    text = "★ $rankTitle ★",
                    color = rankColor,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = ArcadeTheme.pixelHeader,
                    letterSpacing = 1.2.sp,
                    textAlign = TextAlign.Center
                )
            }

            // 4-Card Telemetry Grid with Animated Progress Bars
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF0C121D))
                    .border(1.dp, Color(0xFF1E2D42), RoundedCornerShape(14.dp))
                    .padding(18.dp),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                // Bugs Purged
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(text = "BUGS PURGED", color = Color(0xFF90A4AE), fontSize = 14.sp, fontWeight = FontWeight.Bold, fontFamily = ArcadeTheme.terminalBody)
                    Text(text = "$bugsSquashed 💥", color = Color(0xFFFF7043), fontSize = 18.sp, fontWeight = FontWeight.Bold, fontFamily = ArcadeTheme.pixelHeader)
                    Box(
                        modifier = Modifier
                            .width(110.dp)
                            .height(5.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(Color(0xFF263238))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(bugsProgress)
                                .height(5.dp)
                                .background(Brush.horizontalGradient(listOf(Color(0xFFFF7043), Color(0xFFFFAB91))))
                        )
                    }
                }

                // Gems Collected
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(text = "GEMS HARVESTED", color = Color(0xFF90A4AE), fontSize = 14.sp, fontWeight = FontWeight.Bold, fontFamily = ArcadeTheme.terminalBody)
                    Text(text = "$gemsCollected 💎", color = Color(0xFF00E5FF), fontSize = 18.sp, fontWeight = FontWeight.Bold, fontFamily = ArcadeTheme.pixelHeader)
                    Box(
                        modifier = Modifier
                            .width(110.dp)
                            .height(5.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(Color(0xFF263238))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(gemsProgress)
                                .height(5.dp)
                                .background(Brush.horizontalGradient(listOf(Color(0xFF00E5FF), Color(0xFF80D8FF))))
                        )
                    }
                }

                // Kotlin Q&A
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(text = "KOTLIN Q&A", color = Color(0xFF90A4AE), fontSize = 14.sp, fontWeight = FontWeight.Bold, fontFamily = ArcadeTheme.terminalBody)
                    Text(text = "$triviaCorrect / $triviaAttempted 🧠", color = Color(0xFF76FF03), fontSize = 18.sp, fontWeight = FontWeight.Bold, fontFamily = ArcadeTheme.pixelHeader)
                    Box(
                        modifier = Modifier
                            .width(110.dp)
                            .height(5.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(Color(0xFF263238))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(triviaProgress)
                                .height(5.dp)
                                .background(Brush.horizontalGradient(listOf(Color(0xFF76FF03), Color(0xFFB2FF59))))
                        )
                    }
                }

                // Peak Multiplier
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(text = "PEAK MULTIPLIER", color = Color(0xFF90A4AE), fontSize = 14.sp, fontWeight = FontWeight.Bold, fontFamily = ArcadeTheme.terminalBody)
                    Text(text = "${highestMultiplier}X 🔥", color = Color(0xFFFF9100), fontSize = 18.sp, fontWeight = FontWeight.Bold, fontFamily = ArcadeTheme.pixelHeader)
                    Box(
                        modifier = Modifier
                            .width(110.dp)
                            .height(5.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(Color(0xFF263238))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(multProgress)
                                .height(5.dp)
                                .background(Brush.horizontalGradient(listOf(Color(0xFFFF9100), Color(0xFFFFD54F))))
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Strictly One Button: [🏆 VIEW LEADERBOARD]
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFF00E5FF))
                        .border(2.dp, buttonGlowColor, RoundedCornerShape(14.dp))
                        .clickable {
                            onViewLeaderboard()
                        }
                        .padding(horizontal = 48.dp, vertical = 16.dp)
                ) {
                    Text(
                        text = "🏆 VIEW LEADERBOARD",
                        color = Color(0xFF0A0E14),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = ArcadeTheme.pixelHeader,
                        letterSpacing = 1.sp
                    )
                }

                Text(
                    text = "PRESS [SPACE] OR [ENTER] TO CONTINUE",
                    color = Color(0xFF78909C),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = ArcadeTheme.terminalBody,
                    letterSpacing = 0.5.sp
                )
            }
        }
    }
}

@Preview
@Composable
fun GameOverGodlikePreview() {
    GameOverOverlay(
        finalScore = 24800,
        gemsCollected = 52,
        bugsSquashed = 44,
        triviaCorrect = 10,
        triviaAttempted = 10,
        highestMultiplier = 5,
        playerHandle = "Dev_Kotlin_Pro",
        onViewLeaderboard = {}
    )
}

@Preview
@Composable
fun GameOverInitiatePreview() {
    GameOverOverlay(
        finalScore = 4200,
        gemsCollected = 12,
        bugsSquashed = 8,
        triviaCorrect = 2,
        triviaAttempted = 5,
        highestMultiplier = 2,
        playerHandle = "Newbie_Byte",
        onViewLeaderboard = {}
    )
}
