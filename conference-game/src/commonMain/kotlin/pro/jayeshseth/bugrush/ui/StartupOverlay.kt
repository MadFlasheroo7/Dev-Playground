package pro.jayeshseth.bugrush.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pro.jayeshseth.bugrush.game.actors.BugHazardActor
import pro.jayeshseth.bugrush.game.actors.BugType
import pro.jayeshseth.bugrush.game.actors.PlayerArchetype
import pro.jayeshseth.bugrush.game.actors.PlayerVoxelActor
import pro.jayeshseth.bugrush.game.renderer.VoxelCuboid
import pro.jayeshseth.bugrush.leaderboard.LeaderboardEntry
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

// ── Arcade CRT Color Palette ───────────────────────────────────────────────────
private val CrtBackground = Color(0xFF1A1A3E)
private val CrtBackgroundDark = Color(0xFF0E0E2A)
private val ArcadeOrange = Color(0xFFFF8C00)
private val ArcadeAmber = Color(0xFFFFAA00)
private val ArcadeWhite = Color(0xFFE0E0E0)
private val ArcadeCyan = Color(0xFF00E5FF)
private val ArcadeGreen = Color(0xFF00E676)
private val ArcadeRed = Color(0xFFFF4444)
private val ArcadeDimWhite = Color(0xFF888899)
private val ArcadeGold = Color(0xFFFFD700)
private val ArcadeSilver = Color(0xFFC0C0C0)
private val ArcadeBronze = Color(0xFFCD7F32)

/**
 * Classic arcade CRT-style startup screen.
 *
 * No cards, no panels, no borders, no rounded corners. Just raw CRT phosphor text
 * on a dark indigo background with scanline overlay, exactly like a real arcade cabinet
 * attract screen from the 80s/90s.
 *
 * Layout:
 * - Left side: 1UP / HI-SCORE / 2UP header, title, cursor menu, credits, hotkeys
 * - Right side: 3D voxel diorama (borderless, integrated into CRT screen)
 */
@Composable
fun StartupOverlay(
    leaderboardEntries: List<LeaderboardEntry>,
    onStartGame: () -> Unit,
    isLeaderboardOpen: Boolean = false,
    onToggleLeaderboard: ((Boolean) -> Unit)? = null,
    selectedMenuItem: Int = 0,
    onSelectMenuItem: ((Int) -> Unit)? = null
) {
    var localShowLeaderboard by remember { mutableStateOf(false) }
    val showLeaderboard = if (onToggleLeaderboard != null) isLeaderboardOpen else localShowLeaderboard
    val setShowLeaderboard: (Boolean) -> Unit = { open ->
        localShowLeaderboard = open
        onToggleLeaderboard?.invoke(open)
    }

    var localSelectedMenuItem by remember { mutableStateOf(0) }
    val currentMenuItem = if (onSelectMenuItem != null) selectedMenuItem else localSelectedMenuItem
    val selectMenu: (Int) -> Unit = { idx ->
        localSelectedMenuItem = idx
        onSelectMenuItem?.invoke(idx)
    }

    val infiniteTransition = rememberInfiniteTransition(label = "arcade_crt")

    // Blinking cursor / INSERT COIN flash (on 500ms, off 500ms)
    val blinkAlpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(tween(500, easing = LinearEasing), RepeatMode.Reverse),
        label = "blink"
    )

    // Title color pulse between orange and amber
    val titlePulse by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1200, easing = LinearEasing), RepeatMode.Reverse),
        label = "title_pulse"
    )

    // Diorama 360° continuous rotation
    val dioramaRotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * PI.toFloat(),
        animationSpec = infiniteRepeatable(tween(11000, easing = LinearEasing), RepeatMode.Restart),
        label = "diorama_rotation"
    )

    // Scanline scroll offset for subtle movement
    val scanlineOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 4f,
        animationSpec = infiniteRepeatable(tween(2000, easing = LinearEasing), RepeatMode.Restart),
        label = "scanline_scroll"
    )

    val titleColor = lerp(ArcadeOrange, ArcadeAmber, titlePulse)
    val highScore = leaderboardEntries.maxOfOrNull { it.score } ?: 0

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CrtBackground)
    ) {
        // ── CRT Phosphor Grid Background (Authentic arcade monitor aesthetic) ───
        Canvas(modifier = Modifier.fillMaxSize()) {
            val gridStep = 44f
            val gridColor = Color(0x224455AA)
            var gx = 0f
            while (gx < size.width) {
                drawLine(
                    color = gridColor,
                    start = Offset(gx, 0f),
                    end = Offset(gx, size.height),
                    strokeWidth = 1f
                )
                gx += gridStep
            }
            var gy = 0f
            while (gy < size.height) {
                drawLine(
                    color = gridColor,
                    start = Offset(0f, gy),
                    end = Offset(size.width, gy),
                    strokeWidth = 1f
                )
                gy += gridStep
            }
        }

        // ── Main Content ────────────────────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = if (showLeaderboard) Arrangement.Center else Arrangement.Start
        ) {
            // ════════════════════════════════════════════════════════════════
            // MAIN TEXT UI (Takes 78% of screen when leaderboard is open)
            // ════════════════════════════════════════════════════════════════
            Column(
                modifier = if (showLeaderboard) {
                    Modifier
                        .fillMaxWidth(0.78f)
                        .fillMaxHeight()
                        .padding(horizontal = 24.dp, vertical = 20.dp)
                } else {
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(horizontal = 32.dp, vertical = 24.dp)
                },
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // ── Top: 1UP / HI-SCORE / 2UP Header ───────────────────────
                Column(
                    modifier = Modifier.fillMaxWidth(if (showLeaderboard) 0.95f else 1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "1UP",
                            color = ArcadeWhite,
                            fontSize = 18.sp,
                            fontFamily = ArcadeTheme.pixelHeader,
                            fontWeight = FontWeight.Normal
                        )
                        Text(
                            text = "HI-SCORE",
                            color = ArcadeRed,
                            fontSize = 18.sp,
                            fontFamily = ArcadeTheme.pixelHeader,
                            fontWeight = FontWeight.Normal
                        )
                        Text(
                            text = "2UP",
                            color = ArcadeWhite,
                            fontSize = 18.sp,
                            fontFamily = ArcadeTheme.pixelHeader,
                            fontWeight = FontWeight.Normal
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "00",
                            color = ArcadeWhite,
                            fontSize = 18.sp,
                            fontFamily = ArcadeTheme.pixelHeader,
                            fontWeight = FontWeight.Normal
                        )
                        Text(
                            text = "%06d".format(highScore),
                            color = ArcadeWhite,
                            fontSize = 18.sp,
                            fontFamily = ArcadeTheme.pixelHeader,
                            fontWeight = FontWeight.Normal
                        )
                        Text(
                            text = "00",
                            color = ArcadeWhite,
                            fontSize = 18.sp,
                            fontFamily = ArcadeTheme.pixelHeader,
                            fontWeight = FontWeight.Normal
                        )
                    }
                }

                // ── Center: Title + Menu ────────────────────────────────────
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Big chunky arcade title
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "KOTLIN",
                        color = titleColor,
                        fontSize = 36.sp,
                        fontFamily = ArcadeTheme.pixelHeader,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        letterSpacing = 4.sp
                    )
                    Text(
                        text = "BUG RUSH",
                        color = titleColor,
                        fontSize = 36.sp,
                        fontFamily = ArcadeTheme.pixelHeader,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        letterSpacing = 4.sp
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Subtitle
                    Text(
                        text = "ARCADE REBOOT EDITION",
                        color = ArcadeCyan,
                        fontSize = 12.sp,
                        fontFamily = ArcadeTheme.pixelHeader,
                        fontWeight = FontWeight.Normal,
                        textAlign = TextAlign.Center,
                        letterSpacing = 2.sp
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    if (!showLeaderboard) {
                        // ── Menu Items (text lines with > cursor) ───────────
                        Column(
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            horizontalAlignment = Alignment.Start,
                            modifier = Modifier.fillMaxWidth(0.7f)
                        ) {
                            ArcadeMenuItem(
                                text = "START GAME",
                                isSelected = currentMenuItem == 0,
                                blinkAlpha = blinkAlpha,
                                onClick = {
                                    selectMenu(0)
                                    onStartGame()
                                }
                            )
                            ArcadeMenuItem(
                                text = "LEADERBOARD",
                                isSelected = currentMenuItem == 1,
                                blinkAlpha = blinkAlpha,
                                onClick = {
                                    selectMenu(1)
                                    setShowLeaderboard(true)
                                }
                            )
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // Blinking INSERT COIN / PRESS START text
                        Text(
                            text = "PRESS START",
                            color = ArcadeAmber.copy(alpha = blinkAlpha),
                            fontSize = 20.sp,
                            fontFamily = ArcadeTheme.pixelHeader,
                            fontWeight = FontWeight.Normal,
                            textAlign = TextAlign.Center,
                            letterSpacing = 2.sp
                        )
                    } else {
                        // ── Leaderboard View ────────────────────────────────
                        ArcadeLeaderboard(
                            entries = leaderboardEntries,
                            onBack = { setShowLeaderboard(false) }
                        )
                    }
                }

                // ── Bottom: CREDIT + Hotkeys ────────────────────────────────
                Column(
                    modifier = Modifier.fillMaxWidth(if (showLeaderboard) 0.95f else 1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Hotkey hints
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (showLeaderboard) "[SPACE/ENTER/ESC] BACK" else "[SPACE/ENTER] PLAY",
                            color = ArcadeDimWhite,
                            fontSize = 12.sp,
                            fontFamily = ArcadeTheme.pixelHeader,
                            fontWeight = FontWeight.Normal
                        )
                        Spacer(modifier = Modifier.width(20.dp))
                        Text(
                            text = "[L] RANKS",
                            color = ArcadeDimWhite,
                            fontSize = 12.sp,
                            fontFamily = ArcadeTheme.pixelHeader,
                            fontWeight = FontWeight.Normal
                        )
                        Spacer(modifier = Modifier.width(20.dp))
                        Text(
                            text = "[M] MUTE",
                            color = ArcadeDimWhite,
                            fontSize = 12.sp,
                            fontFamily = ArcadeTheme.pixelHeader,
                            fontWeight = FontWeight.Normal
                        )
                    }

                    // CREDIT text
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Text(
                            text = "CREDIT  01",
                            color = ArcadeWhite,
                            fontSize = 16.sp,
                            fontFamily = ArcadeTheme.pixelHeader,
                            fontWeight = FontWeight.Normal
                        )
                    }
                }
            }

            // ════════════════════════════════════════════════════════════════
            // RIGHT SIDE: 3D Voxel Diorama (borderless, CRT-integrated)
            // ════════════════════════════════════════════════════════════════
            if (!showLeaderboard) {
                Box(
                    modifier = Modifier
                        .weight(0.95f)
                        .fillMaxHeight()
                ) {
                // Live Diorama Actor Instances
                val playerModel = remember { PlayerVoxelActor(0f, 0f, PlayerArchetype.CYBER_CODER) }
                val swarmerModel = remember { BugHazardActor(0f, 0f, headingAngle = 0f, bugType = BugType.SWARMER) }
                val deadlockModel = remember { BugHazardActor(0f, 0f, headingAngle = 0f, bugType = BugType.DEADLOCK) }

                // Advance crawling / walking leg animations for diorama
                swarmerModel.update(16)
                deadlockModel.update(16)

                // Dynamic orbit trajectory for enemies
                val swarmerAngle = dioramaRotationAngle * 1.5f
                val swarmerRadius = 65f + sin(dioramaRotationAngle * 3f) * 10f
                swarmerModel.posX = cos(swarmerAngle) * swarmerRadius
                swarmerModel.posY = sin(swarmerAngle) * swarmerRadius
                swarmerModel.headingAngle = atan2(-swarmerModel.posY, -swarmerModel.posX)

                val deadlockAngle = dioramaRotationAngle * 0.8f + 2.8f
                val deadlockRadius = 90f + cos(dioramaRotationAngle * 2.5f) * 12f
                deadlockModel.posX = cos(deadlockAngle) * deadlockRadius
                deadlockModel.posY = sin(deadlockAngle) * deadlockRadius
                deadlockModel.headingAngle = atan2(-deadlockModel.posY, -deadlockModel.posX)

                // Player character aims at swarmer
                val aimVectorX = swarmerModel.posX - playerModel.posX
                val aimVectorY = swarmerModel.posY - playerModel.posY
                val aimDist = kotlin.math.hypot(aimVectorX, aimVectorY).coerceAtLeast(0.001f)
                playerModel.headingX = aimVectorX / aimDist
                playerModel.headingY = aimVectorY / aimDist

                // Pedestal cuboids
                val pedestalCuboids = remember {
                    listOf(
                        VoxelCuboid(
                            x = 0f, y = 0f, z = -8f,
                            sizeX = 220f, sizeY = 220f, sizeZ = 8f,
                            baseColor = Color(0xFF161640),
                            isFloor = true
                        ),
                        VoxelCuboid(
                            x = 0f, y = 0f, z = 0f,
                            sizeX = 180f, sizeY = 180f, sizeZ = 4f,
                            baseColor = Color(0xFF1E1E50),
                            isFloor = true
                        )
                    )
                }

                val combinedCuboids = remember(dioramaRotationAngle) {
                    pedestalCuboids + playerModel.emitVoxelCuboids() + swarmerModel.emitVoxelCuboids() + deadlockModel.emitVoxelCuboids()
                }

                // Borderless diorama viewport — blends into CRT background
                VoxelActorCanvas(
                    cuboids = combinedCuboids,
                    modifier = Modifier.fillMaxSize(),
                    zoom = 1.35f,
                    cameraAngleRad = dioramaRotationAngle,
                    backgroundColor = CrtBackground
                )

                // Diorama label overlays (no panels, just raw text)
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    // Top label
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "TARGET: SYSTEM ANOMALIES",
                            color = ArcadeCyan.copy(alpha = 0.7f),
                            fontSize = 8.sp,
                            fontFamily = ArcadeTheme.pixelHeader,
                            fontWeight = FontWeight.Normal
                        )
                        Text(
                            text = "POINTS == AMMO",
                            color = ArcadeGreen.copy(alpha = 0.7f),
                            fontSize = 8.sp,
                            fontFamily = ArcadeTheme.pixelHeader,
                            fontWeight = FontWeight.Normal
                        )
                    }

                    // Bottom label
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Text(
                            text = "3D VOXEL ENGINE",
                            color = ArcadeDimWhite.copy(alpha = 0.5f),
                            fontSize = 7.sp,
                            fontFamily = ArcadeTheme.pixelHeader,
                            fontWeight = FontWeight.Normal
                        )
                        Text(
                            text = "SIMULATION: ACTIVE",
                            color = ArcadeAmber.copy(alpha = if (blinkAlpha > 0.5f) 0.7f else 0.3f),
                            fontSize = 7.sp,
                            fontFamily = ArcadeTheme.pixelHeader,
                            fontWeight = FontWeight.Normal
                        )
                    }
                }
            }
        }
    }

        // ── CRT Scanline Overlay (covers entire screen) ─────────────────────
        Canvas(modifier = Modifier.fillMaxSize()) {
            val lineSpacing = 3f
            val offset = scanlineOffset % lineSpacing
            var y = offset
            while (y < size.height) {
                drawLine(
                    color = Color.Black.copy(alpha = 0.15f),
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = 1f
                )
                y += lineSpacing
            }

            // Subtle vignette darkening at edges
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.Transparent,
                        Color.Black.copy(alpha = 0.3f)
                    ),
                    center = Offset(size.width * 0.5f, size.height * 0.5f),
                    radius = size.width * 0.7f
                ),
                size = size
            )
        }
    }
}

/**
 * A single menu item rendered as plain arcade text with a blinking `>` cursor.
 * No boxes, no backgrounds, no borders — just text, like a real arcade select screen.
 */
@Composable
private fun ArcadeMenuItem(
    text: String,
    isSelected: Boolean,
    blinkAlpha: Float,
    onClick: () -> Unit
) {
    val cursorText = if (isSelected) ">" else " "
    val textColor = if (isSelected) ArcadeOrange else ArcadeWhite

    Row(
        modifier = Modifier
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onClick() }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start
    ) {
        // Blinking cursor
        Text(
            text = cursorText,
            color = ArcadeOrange.copy(alpha = if (isSelected) blinkAlpha else 0f),
            fontSize = 22.sp,
            fontFamily = ArcadeTheme.pixelHeader,
            fontWeight = FontWeight.Normal,
            modifier = Modifier.width(32.dp)
        )
        Text(
            text = text,
            color = textColor,
            fontSize = 22.sp,
            fontFamily = ArcadeTheme.pixelHeader,
            fontWeight = FontWeight.Normal,
            letterSpacing = 1.sp
        )
    }
}

/**
 * Classic arcade-style leaderboard table — no panels, no cards.
 * Plain pixel-font text rows with rank numbers, names, and scores.
 */
@Composable
private fun ArcadeLeaderboard(
    entries: List<LeaderboardEntry>,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header
        Text(
            text = "- TOP OPERATORS HALL OF FAME -",
            color = ArcadeAmber,
            fontSize = 18.sp,
            fontFamily = ArcadeTheme.pixelHeader,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            letterSpacing = 2.sp
        )

        Spacer(modifier = Modifier.height(4.dp))

        // Column header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "RANK",
                color = ArcadeCyan,
                fontSize = 15.sp,
                fontFamily = ArcadeTheme.pixelHeader,
                modifier = Modifier.width(72.dp)
            )
            Text(
                text = "OPERATOR NAME",
                color = ArcadeCyan,
                fontSize = 15.sp,
                fontFamily = ArcadeTheme.pixelHeader,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "Q&A",
                color = ArcadeCyan,
                fontSize = 15.sp,
                fontFamily = ArcadeTheme.pixelHeader,
                modifier = Modifier.width(80.dp),
                textAlign = TextAlign.End
            )
            Text(
                text = "SCORE",
                color = ArcadeCyan,
                fontSize = 15.sp,
                fontFamily = ArcadeTheme.pixelHeader,
                modifier = Modifier.width(130.dp),
                textAlign = TextAlign.End
            )
        }

        // Divider line
        Text(
            text = "═".repeat(45),
            color = ArcadeCyan.copy(alpha = 0.45f),
            fontSize = 14.sp,
            fontFamily = ArcadeTheme.pixelHeader,
            modifier = Modifier.fillMaxWidth()
        )

        if (entries.isEmpty()) {
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "NO RUNS RECORDED YET",
                color = ArcadeDimWhite,
                fontSize = 16.sp,
                fontFamily = ArcadeTheme.pixelHeader,
                textAlign = TextAlign.Center
            )
            Text(
                text = "BE THE FIRST LEGEND!",
                color = ArcadeOrange,
                fontSize = 16.sp,
                fontFamily = ArcadeTheme.pixelHeader,
                textAlign = TextAlign.Center
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 260.dp, max = 420.dp)
            ) {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    itemsIndexed(entries) { index, entry ->
                        val rankColor = when (index) {
                            0 -> ArcadeGold
                            1 -> ArcadeSilver
                            2 -> ArcadeBronze
                            else -> ArcadeWhite
                        }
                        val rankText = "#${index + 1}"

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = rankText,
                                color = rankColor,
                                fontSize = 16.sp,
                                fontFamily = ArcadeTheme.pixelHeader,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.width(72.dp)
                            )
                            Text(
                                text = entry.playerName,
                                color = ArcadeWhite,
                                fontSize = 18.sp,
                                fontFamily = ArcadeTheme.terminalBody,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = "${entry.triviaCorrect}/${entry.triviaAttempted}",
                                color = ArcadeCyan,
                                fontSize = 18.sp,
                                fontFamily = ArcadeTheme.terminalBody,
                                modifier = Modifier.width(80.dp),
                                textAlign = TextAlign.End
                            )
                            Text(
                                text = "${entry.score}",
                                color = ArcadeGreen,
                                fontSize = 16.sp,
                                fontFamily = ArcadeTheme.pixelHeader,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.width(130.dp),
                                textAlign = TextAlign.End
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "[▲/▼] SCROLL TO VIEW ALL RUNS (${entries.size} RECORDED)",
                color = ArcadeDimWhite,
                fontSize = 11.sp,
                fontFamily = ArcadeTheme.pixelHeader,
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Back option — arcade style, just text with cursor
        Text(
            text = "> BACK TO MAIN MENU",
            color = ArcadeOrange,
            fontSize = 18.sp,
            fontFamily = ArcadeTheme.pixelHeader,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { onBack() }
                .padding(vertical = 6.dp)
        )
    }
}

/** Simple float lerp for color channel interpolation. */
private fun lerp(a: Color, b: Color, fraction: Float): Color {
    return Color(
        red = a.red + (b.red - a.red) * fraction,
        green = a.green + (b.green - a.green) * fraction,
        blue = a.blue + (b.blue - a.blue) * fraction,
        alpha = a.alpha + (b.alpha - a.alpha) * fraction
    )
}
