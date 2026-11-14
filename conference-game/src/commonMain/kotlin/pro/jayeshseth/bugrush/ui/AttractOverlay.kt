package pro.jayeshseth.bugrush.ui

import androidx.compose.animation.animateColor
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pro.jayeshseth.bugrush.game.actors.PlayerArchetype
import pro.jayeshseth.bugrush.leaderboard.LeaderboardEntry

@Composable
fun AttractOverlay(
    leaderboardEntries: List<LeaderboardEntry>,
    selectedArchetype: PlayerArchetype = PlayerArchetype.CYBER_CODER,
    playerHandle: String = "Dev_Kotlin_42",
    onUpdatePlayerHandle: (String) -> Unit = {},
    onTextFieldFocusChanged: (Boolean) -> Unit = {},
    onSelectArchetype: (PlayerArchetype) -> Unit = {},
    onOpenBriefing: () -> Unit = {},
    onStartGame: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "attract_infinite")

    // 1. Shimmering Gold Crown for #1 Rank
    val crownShimmerColor by infiniteTransition.animateColor(
        initialValue = Color(0xFFFFD700),
        targetValue = Color(0xFFFFF59D),
        animationSpec = infiniteRepeatable(tween(600), RepeatMode.Reverse),
        label = "crown_shimmer"
    )

    // 2. Leaderboard Telemetry Laser Scanner Sweep (Top to Bottom)
    val laserYFraction by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2600, easing = LinearEasing), RepeatMode.Restart),
        label = "laser_scanner"
    )

    // 3. Pulsating Neon Start Button
    val startButtonScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(tween(700, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "start_scale"
    )
    val startButtonGlow by infiniteTransition.animateColor(
        initialValue = Color(0xFF00E5FF),
        targetValue = Color(0xFF00E676),
        animationSpec = infiniteRepeatable(tween(700), RepeatMode.Reverse),
        label = "start_color"
    )

    // 4. Hero Card Circuit Glow
    val heroCircuitGlow by infiniteTransition.animateColor(
        initialValue = Color(0xFF00E5FF),
        targetValue = Color(0xFF7F52FF),
        animationSpec = infiniteRepeatable(tween(800), RepeatMode.Reverse),
        label = "hero_circuit"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xE60D1117)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .width(860.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0xFF161F2E))
                .border(2.dp, Color(0xFF7F52FF), RoundedCornerShape(24.dp))
                .padding(26.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Title Header
            Text(
                text = "KOTLIN BUG RUSH",
                color = Color(0xFF00E5FF),
                fontSize = 28.sp,
                fontWeight = FontWeight.Black,
                fontFamily = ArcadeTheme.pixelHeader,
                letterSpacing = 2.sp
            )
            Text(
                text = "TECH CONFERENCE ARCADE • 2-MINUTE PROCEDURAL RUN",
                color = Color(0xFF7F52FF),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = ArcadeTheme.terminalBody,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Developer Handle Input
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF0F1722))
                    .border(1.dp, Color(0xFF00E5FF), RoundedCornerShape(12.dp))
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "OPERATOR HANDLE:",
                    color = Color(0xFF90A4AE),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                OutlinedTextField(
                    value = playerHandle,
                    onValueChange = { if (it.length <= 18) onUpdatePlayerHandle(it) },
                    singleLine = true,
                    modifier = Modifier
                        .width(320.dp)
                        .onFocusChanged { onTextFieldFocusChanged(it.isFocused) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color(0xFF00E5FF),
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF00E5FF),
                        unfocusedBorderColor = Color(0xFF263238)
                    )
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Archetype Selector Cards with Spring Hover/Selection
            Text(
                text = "CHOOSE YOUR HERO ARCHETYPE",
                color = Color(0xFFB0BEC5),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                PlayerArchetype.entries.forEachIndexed { idx, archetype ->
                    val isSelected = archetype == selectedArchetype
                    val cardScale by animateFloatAsState(
                        targetValue = if (isSelected) 1.04f else 1.0f,
                        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                        label = "card_scale_${archetype.name}"
                    )
                    val cardBorder = if (isSelected) heroCircuitGlow else Color(0xFF263238)
                    val cardBg = if (isSelected) Color(0xFF1B2F44) else Color(0xFF131A24)

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .graphicsLayer {
                                scaleX = cardScale
                                scaleY = cardScale
                            }
                            .clip(RoundedCornerShape(12.dp))
                            .background(cardBg)
                            .border(2.dp, cardBorder, RoundedCornerShape(12.dp))
                            .clickable { onSelectArchetype(archetype) }
                            .padding(12.dp)
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${idx + 1}. ${archetype.title}",
                                    color = if (isSelected) Color(0xFF00E5FF) else Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Black
                                )
                                if (isSelected) {
                                    Text(
                                        text = "SELECTED",
                                        color = Color(0xFF00E676),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = archetype.subtitle,
                                color = Color(0xFF90A4AE),
                                fontSize = 11.sp,
                                lineHeight = 14.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Leaderboard Table with Holographic Laser Scanner
            Text(
                text = "🏆 TOP BOOTH RUNNERS",
                color = Color(0xFFFFD600),
                fontSize = 14.sp,
                fontWeight = FontWeight.ExtraBold
            )

            Spacer(modifier = Modifier.height(6.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF0F1520))
                    .border(1.dp, Color(0xFF263238), RoundedCornerShape(12.dp))
                    .padding(8.dp)
            ) {
                if (leaderboardEntries.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = "No runs recorded yet. Be the first to claim #1 rank!",
                            color = Color(0xFF78909C),
                            fontSize = 13.sp
                        )
                    }
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        itemsIndexed(leaderboardEntries.take(10)) { index, entry ->
                            val rankColor = when (index) {
                                0 -> crownShimmerColor
                                1 -> Color(0xFFE0E0E0)
                                2 -> Color(0xFFFFB74D)
                                else -> Color(0xFF90A4AE)
                            }
                            val rankTag = when (index) {
                                0 -> "👑 #1"
                                1 -> "🥈 #2"
                                2 -> "🥉 #3"
                                else -> "#${index + 1}"
                            }
                            val rowBg = when (index) {
                                0 -> Color(0x33FFD600)
                                1 -> Color(0x22C0C0C0)
                                2 -> Color(0x22CD7F32)
                                else -> Color(0xFF1B2432)
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(rowBg)
                                    .padding(horizontal = 12.dp, vertical = 5.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = rankTag,
                                        color = rankColor,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        modifier = Modifier.width(46.dp)
                                    )
                                    Text(
                                        text = entry.playerName,
                                        color = Color.White,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "${entry.triviaCorrect}/${entry.triviaAttempted} Q&A",
                                        color = Color(0xFF80D8FF),
                                        fontSize = 11.sp,
                                        modifier = Modifier.padding(end = 12.dp)
                                    )
                                    Text(
                                        text = "${entry.score} PTS",
                                        color = Color(0xFF00E676),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }
                            }
                        }
                    }
                }

                // Laser Scan Line Sweep
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val scanY = size.height * laserYFraction
                    val beamHeight = 16f
                    drawRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color(0x2200E5FF), Color(0xAA00E5FF)),
                            startY = scanY - beamHeight,
                            endY = scanY
                        ),
                        topLeft = Offset(0f, scanY - beamHeight),
                        size = Size(size.width, beamHeight)
                    )
                    drawLine(
                        color = Color(0xFF00E5FF).copy(alpha = 0.9f),
                        start = Offset(0f, scanY),
                        end = Offset(size.width, scanY),
                        strokeWidth = 2f
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Start & Field Manual Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Pulsating Neon Retro-Arcade Start Button
                Box(
                    modifier = Modifier
                        .graphicsLayer {
                            scaleX = startButtonScale
                            scaleY = startButtonScale
                        }
                        .clip(RoundedCornerShape(14.dp))
                        .background(startButtonGlow)
                        .border(2.dp, Color.White.copy(alpha = 0.8f), RoundedCornerShape(14.dp))
                        .clickable { onStartGame() }
                        .padding(horizontal = 28.dp, vertical = 13.dp)
                ) {
                    Text(
                        text = "START RUN (2:00) [SPACE]",
                        color = Color(0xFF0A0E14),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                // Glowing Field Manual Briefing Button
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFF162334))
                        .border(2.dp, Color(0xFF00E5FF), RoundedCornerShape(14.dp))
                        .clickable { onOpenBriefing() }
                        .padding(horizontal = 20.dp, vertical = 13.dp)
                ) {
                    Text(
                        text = "📖 FIELD MANUAL [H / F1]",
                        color = Color(0xFF00E5FF),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Controls: WASD: Move • Aim + Shoot: Mouse / Space • Dash: RMB / C / E • Tab / R: Camera • Z / O: Zoom • Ctrl: Guns • Shift: Buffs • M: Audio",
                color = Color(0xFF78909C),
                fontSize = 11.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Preview
@Composable
fun AttractOverlayPreview() {
    AttractOverlay(
        leaderboardEntries = listOf(
            LeaderboardEntry(playerName = "KotlinMaster_99", score = 24500, gemsCollected = 42, triviaCorrect = 10, triviaAttempted = 10, highestMultiplier = 5, timestamp = 1700000000000L),
            LeaderboardEntry(playerName = "AndroidDev_Pro", score = 18200, gemsCollected = 31, triviaCorrect = 7, triviaAttempted = 8, highestMultiplier = 4, timestamp = 1700000000000L),
            LeaderboardEntry(playerName = "CoroutinesNinja", score = 12900, gemsCollected = 22, triviaCorrect = 5, triviaAttempted = 6, highestMultiplier = 3, timestamp = 1700000000000L),
            LeaderboardEntry(playerName = "ComposeChampion", score = 8400, gemsCollected = 15, triviaCorrect = 3, triviaAttempted = 4, highestMultiplier = 2, timestamp = 1700000000000L)
        ),
        selectedArchetype = PlayerArchetype.CYBER_CODER,
        playerHandle = "Dev_Kotlin_42",
        onUpdatePlayerHandle = {},
        onTextFieldFocusChanged = {},
        onSelectArchetype = {},
        onStartGame = {}
    )
}
