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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColor
import androidx.compose.animation.animateColorAsState
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
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pro.jayeshseth.bugrush.game.actors.PlayerArchetype
import pro.jayeshseth.bugrush.game.actors.WeaponType
import pro.jayeshseth.bugrush.trivia.PowerUpType
import pro.jayeshseth.bugrush.trivia.VersionPerk

@Composable
fun HUDOverlay(
    remainingSeconds: Int,
    score: Int,
    multiplier: Int,
    bugsSquashed: Int,
    activePowerUp: PowerUpType?,
    powerUpRemainingSeconds: Int,
    bossHp: Pair<Int, Int>?,
    bossWarningSiren: Boolean = false,
    bossKernelPanic: Boolean = false,
    killstreakBanner: String? = null,
    unlockedPerks: Set<VersionPerk> = emptySet(),
    isMuted: Boolean = false,
    onToggleMute: () -> Unit = {},
    activeArchetype: PlayerArchetype = PlayerArchetype.CYBER_CODER,
    dashCooldownFraction: Float = 0f,
    zoomMultiplier: Float = 1.0f,
    currentFloor: Int = 1,
    bugsSquashedThisFloor: Int = 0,
    bugsTargetForElevator: Int = 8,
    isElevatorOnline: Boolean = false,
    floorAscensionSplashTimerMs: Int = 0,
    floorAscensionTitle: String = "",
    floorAscensionSubtitle: String = "",
    nearestBugDistance: Float? = null,
    nearestBugAngleRad: Float? = null,
    elevatorDistance: Float? = null,
    elevatorAngleRad: Float? = null,
    activeBountyDistance: Float? = null,
    activeBountyAngleRad: Float? = null,
    activeBountyRemainingKills: Int? = null,
    activeBountyTimeSeconds: Int? = null,
    bossDistance: Float? = null,
    bossAngleRad: Float? = null,
    triviaFeedback: String?,
    activeWeapon: WeaponType = WeaponType.RAPID_BYTE_BLASTER,
    firewallActive: Boolean = false,
    firewallRechargeTimerMs: Int = 0
) {
    val infiniteTransition = rememberInfiniteTransition(label = "hud_infinite")

    // 1. Animated Rolling Score & Scale Bump on Point Pickup
    val animatedScore by animateIntAsState(
        targetValue = score,
        animationSpec = tween(durationMillis = 350, easing = LinearEasing),
        label = "score_anim"
    )
    val scoreScaleAnim = remember { Animatable(1f) }
    LaunchedEffect(score) {
        if (score > 0) {
            scoreScaleAnim.animateTo(1.18f, tween(80))
            scoreScaleAnim.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
        }
    }

    // 2. Multiplier Animation & 5x Overdrive Glow
    val multiplierScaleAnim = remember { Animatable(1f) }
    LaunchedEffect(multiplier) {
        if (multiplier > 1) {
            multiplierScaleAnim.animateTo(1.25f, tween(90))
            multiplierScaleAnim.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
        }
    }
    val overdriveBorderGlow by infiniteTransition.animateColor(
        initialValue = Color(0xFFFFD600),
        targetValue = Color(0xFF00E5FF),
        animationSpec = infiniteRepeatable(tween(500), RepeatMode.Reverse),
        label = "overdrive_glow"
    )

    // 3. Low Time Emergency Heartbeat Pulse (<= 15s)
    val isLowTime = remainingSeconds <= 15
    val lowTimeHeartbeatScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "low_time_scale"
    )
    val lowTimeRedGlow by infiniteTransition.animateColor(
        initialValue = Color(0xFFFF1744),
        targetValue = Color(0xFFFF5252),
        animationSpec = infiniteRepeatable(tween(400), RepeatMode.Reverse),
        label = "low_time_glow"
    )

    // 4. Dash Charge Animation & Ready Flare
    val isDashReady = dashCooldownFraction <= 0.05f
    val dashCharge = (1f - dashCooldownFraction).coerceIn(0f, 1f)
    val animatedDashCharge by animateFloatAsState(
        targetValue = dashCharge,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "dash_charge_smooth"
    )
    val dashReadyPulse by infiniteTransition.animateFloat(
        initialValue = 0.75f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(450), RepeatMode.Reverse),
        label = "dash_pulse"
    )

    // 5. Waypoint Radar Sonar Ping Wave
    val radarPulseRadius by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1300, easing = LinearEasing)),
        label = "radar_wave"
    )

    // 6. Active Perk Micro-Animations
    val droneSpinDeg by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(1600, easing = LinearEasing)),
        label = "drone_spin"
    )
    val turretCrosshairPulse by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(tween(600), RepeatMode.Reverse),
        label = "turret_pulse"
    )
    val jitFlickerAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(250), RepeatMode.Reverse),
        label = "jit_flicker"
    )
    val firewallShieldAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(800), RepeatMode.Reverse),
        label = "shield_shimmer"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // ---------------------------------------------------------
        // 1. TOP-LEFT: Player Archetype, Dash Gauge & Weapon Card
        // ---------------------------------------------------------
        Column(
            modifier = Modifier
                .align(Alignment.TopStart)
                .widthIn(max = 280.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Archetype Badge & Telemetry Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xD90A111C))
                    .border(1.5.dp, Color(0xFF00E5FF).copy(alpha = 0.8f), RoundedCornerShape(14.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF00E5FF))
                            )
                            Text(
                                text = activeArchetype.title.uppercase(),
                                color = Color(0xFF00E5FF),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = ArcadeTheme.pixelHeader
                            )
                        }
                        // Audio Mute toggle chip
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isMuted) Color(0x44FF1744) else Color(0x3300E5FF))
                                .border(1.dp, if (isMuted) Color(0xFFFF5252) else Color(0xFF00E5FF), RoundedCornerShape(6.dp))
                                .clickable { onToggleMute() }
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = if (isMuted) "🔇 [M]" else "🔊 [M]",
                                color = if (isMuted) Color(0xFFFF5252) else Color(0xFF00E5FF),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = activeArchetype.subtitle.split("•").firstOrNull()?.trim() ?: "Hero",
                            color = Color(0xFF80DEEA),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                        val zoomFormatted = ((zoomMultiplier * 100).toInt() / 100f).toString()
                        Text(
                            text = "🔍 ${zoomFormatted}x [Z/O]",
                            color = Color(0xFFB0BEC5),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // Animated Dash Cooldown Charge Gauge
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xD90A111C))
                    .border(
                        1.5.dp,
                        if (isDashReady) Color(0xFF00E676).copy(alpha = dashReadyPulse) else Color(0xFF00E5FF).copy(alpha = 0.5f),
                        RoundedCornerShape(12.dp)
                    )
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "⚡",
                                color = if (isDashReady) Color(0xFF00E676) else Color(0xFFFFD600),
                                fontSize = 12.sp,
                                modifier = if (isDashReady) Modifier.scale(dashReadyPulse) else Modifier
                            )
                            Text(
                                text = if (isDashReady) "DASH READY" else "CHARGING ${(dashCharge * 100).toInt()}%",
                                color = if (isDashReady) Color(0xFF00E676) else Color(0xFF80DEEA),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = ArcadeTheme.pixelHeader
                            )
                        }
                        Text(
                            text = "[K / C / R-CLICK]",
                            color = Color(0xFF64B5F6),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    // Animated Charge Bar
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(Color(0xFF1B2A3D))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(animatedDashCharge)
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(
                                    if (isDashReady) Color(0xFF00E676) else Color(0xFF00E5FF)
                                )
                        )
                    }
                }
            }

            // Active Weapon Card with Slide Transition and [CTRL] Cycle Hint
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xD90A111C))
                    .border(1.5.dp, Color(0xFFFFB300).copy(alpha = 0.8f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AnimatedContent(
                        targetState = activeWeapon,
                        transitionSpec = {
                            (slideInVertically { height -> height } + fadeIn()).togetherWith(
                                slideOutVertically { height -> -height } + fadeOut()
                            )
                        },
                        label = "weapon_anim"
                    ) { targetWp ->
                        val fireModeTag = when (targetWp) {
                            WeaponType.RAPID_BYTE_BLASTER -> "RAPID [AUTO]"
                            WeaponType.TWIN_PULSE -> "TWIN PULSE"
                            WeaponType.HEAVY_RAILGUN -> "PIERCING [2 DMG]"
                            WeaponType.TRIPLE_SPREAD -> "3-WAY SPREAD"
                            WeaponType.PLASMA_FLAK_CANNON -> "5-WAY FLAK"
                            WeaponType.NOVA_360_BLASTER -> "16-WAY NOVA"
                        }
                        val costTag = when (targetWp) {
                            WeaponType.RAPID_BYTE_BLASTER -> "-1 PT / SHOT"
                            WeaponType.TWIN_PULSE -> "-2 PTS / SHOT"
                            WeaponType.TRIPLE_SPREAD -> "-3 PTS / SHOT"
                            WeaponType.HEAVY_RAILGUN, WeaponType.PLASMA_FLAK_CANNON -> "-5 PTS / SHOT"
                            WeaponType.NOVA_360_BLASTER -> "-16 PTS / SHOT"
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(text = "🔫", fontSize = 11.sp)
                                Text(
                                    text = targetWp.displayName.uppercase(),
                                    color = Color(0xFFFFD54F),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontFamily = ArcadeTheme.pixelHeader
                                )
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = fireModeTag,
                                    color = Color(0xFFFFE082),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = ArcadeTheme.terminalBody
                                )
                                Text(
                                    text = "• $costTag",
                                    color = Color(0xFF00E5FF),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Black,
                                    fontFamily = ArcadeTheme.terminalBody
                                )
                            }
                        }
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0x33FFB300))
                            .border(1.dp, Color(0xFFFFB300), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "[CTRL] CYCLE",
                            color = Color(0xFFFFD54F),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = ArcadeTheme.pixelHeader
                        )
                    }
                }
            }
        }

        // ---------------------------------------------------------
        // 2. TOP-CENTER: Sleek Floating Glass Capsule with Heartbeat Pulse
        // ---------------------------------------------------------
        val safeSeconds = remainingSeconds.coerceAtLeast(0)
        val minutes = safeSeconds / 60
        val seconds = safeSeconds % 60
        val timerString = "${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}"
        val quotaProgress = (bugsSquashedThisFloor.toFloat() / bugsTargetForElevator.coerceAtLeast(1).toFloat()).coerceIn(0f, 1f)
        val animatedQuotaProgress by animateFloatAsState(
            targetValue = quotaProgress,
            animationSpec = spring(),
            label = "quota_anim"
        )

        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .widthIn(max = 440.dp)
                .then(if (isLowTime) Modifier.scale(lowTimeHeartbeatScale) else Modifier),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (isLowTime) Color(0xEA2A0A10) else Color(0xEA0A111C))
                    .border(
                        2.dp,
                        if (isLowTime) lowTimeRedGlow else Color(0xFF00E5FF).copy(alpha = 0.7f),
                        RoundedCornerShape(16.dp)
                    )
                    .padding(horizontal = 18.dp, vertical = 8.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val isBountyRunning = activeBountyRemainingKills != null
                    val isOverclockSplash = floorAscensionSplashTimerMs > 0
                    val isOverclockState = isOverclockSplash || isElevatorOnline
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Floor / District Status
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0x3300E5FF))
                                .border(1.dp, Color(0xFF00E5FF), RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "🏛️ FL $currentFloor • SECTOR $currentFloor",
                                color = Color(0xFF00E5FF),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Countdown Timer (Pulsating Crimson when <= 15s)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            if (isLowTime) {
                                Text(text = "🚨", fontSize = 14.sp)
                            }
                            Text(
                                text = timerString,
                                color = if (isLowTime) lowTimeRedGlow else Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = ArcadeTheme.pixelHeader
                            )
                        }

                        // Objective Status Badge
                        val badgeBg = when {
                            isBountyRunning -> Color(0x33FFD600)
                            isOverclockState -> Color(0x3300E676)
                            else -> Color(0x33FF9100)
                        }
                        val badgeBorder = when {
                            isBountyRunning -> Color(0xFFFFD600)
                            isOverclockState -> Color(0xFF00E676)
                            else -> Color(0xFFFF9100)
                        }
                        val badgeText = when {
                            isBountyRunning -> "🎯 BOUNTY: ${activeBountyRemainingKills} LEFT (${activeBountyTimeSeconds ?: 0}s)"
                            isOverclockState -> "⚡ OVERCLOCK ACTIVE"
                            else -> "⚡ $bugsSquashedThisFloor/$bugsTargetForElevator"
                        }
                        val badgeColor = when {
                            isBountyRunning -> Color(0xFFFFD600)
                            isOverclockState -> Color(0xFF00E676)
                            else -> Color(0xFFFFB74D)
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(badgeBg)
                                .border(1.dp, badgeBorder, RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = badgeText,
                                color = badgeColor,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Objective Progress Meter & Waypoint Compass Arrow with Sonar Pulse
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Progress Bar Track
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(Color(0xFF1B2A3D))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(animatedQuotaProgress)
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(if (isOverclockState) Color(0xFF00E676) else if (isBountyRunning) Color(0xFFFFD600) else Color(0xFF00E5FF))
                            )
                        }

                        // Waypoint Navigation Compass with Radar Ping
                        val radarColor = when {
                            activeBountyDistance != null -> Color(0xFFFFD600)
                            bossDistance != null -> Color(0xFFFF1744)
                            isOverclockState -> Color(0xFF76FF03)
                            nearestBugDistance != null -> Color(0xFFFF5252)
                            else -> Color(0xFF00E5FF)
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Canvas(modifier = Modifier.size(12.dp)) {
                                val center = Offset(size.width / 2f, size.height / 2f)
                                val maxR = size.minDimension / 2f
                                drawCircle(
                                    color = radarColor,
                                    radius = maxR * radarPulseRadius,
                                    alpha = (1f - radarPulseRadius).coerceIn(0f, 1f)
                                )
                                drawCircle(
                                    color = radarColor,
                                    radius = 2.5.dp.toPx()
                                )
                            }
                            if (activeBountyDistance != null && activeBountyAngleRad != null) {
                                val bountyArrow = getDirectionArrow(activeBountyAngleRad)
                                val bountyDist = "${(activeBountyDistance / 10).toInt()}m"
                                Text(
                                    text = "BOUNTY: $bountyArrow $bountyDist",
                                    color = Color(0xFFFFD600),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black
                                )
                            } else if (bossDistance != null && bossAngleRad != null) {
                                val bossArrow = getDirectionArrow(bossAngleRad)
                                val bDist = "${(bossDistance / 10).toInt()}m"
                                Text(
                                    text = "TITAN: $bossArrow $bDist",
                                    color = Color(0xFFFF1744),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black
                                )
                            } else if (nearestBugDistance != null && nearestBugAngleRad != null) {
                                val bugArrow = getDirectionArrow(nearestBugAngleRad)
                                val bugDist = "${(nearestBugDistance / 10).toInt()}m"
                                Text(
                                    text = "BUG: $bugArrow $bugDist",
                                    color = Color(0xFFFF8A80),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            } else if (elevatorDistance != null && elevatorAngleRad != null) {
                                val elevArrow = getDirectionArrow(elevatorAngleRad)
                                val elevDist = "${(elevatorDistance / 10).toInt()}m"
                                Text(
                                    text = "WARP: $elevArrow $elevDist",
                                    color = Color(0xFF00E5FF),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        // ---------------------------------------------------------
        // 3. TOP-RIGHT: Arcade Score, Combo Multiplier & Bugs Squashed
        // ---------------------------------------------------------
        val multiplierColor = when (multiplier) {
            5 -> Color(0xFFFFD600)
            4 -> Color(0xFFFF9100)
            3 -> Color(0xFF00E5FF)
            2 -> Color(0xFF76FF03)
            else -> Color(0xFF90A4AE)
        }

        Column(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .widthIn(max = 280.dp),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Arcade Score Counter with Animated Roll and Scale Bounce
            val isLowAmmo = animatedScore <= 15
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xD90A111C))
                    .border(
                        1.5.dp,
                        if (isLowAmmo) Color(0xFFFF5252) else Color(0xFF00E5FF).copy(alpha = 0.8f),
                        RoundedCornerShape(14.dp)
                    )
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Dev Pun Code Badge
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0x3300E5FF))
                                .border(1.dp, Color(0xFF00E5FF).copy(alpha = 0.8f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "POINTS == AMMO",
                                color = Color(0xFF00E5FF),
                                fontFamily = ArcadeTheme.pixelHeader,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black
                            )
                        }

                        Text(
                            text = if (animatedScore <= 0) "AMMO DEPLETED" else "AMMO // SCORE",
                            color = if (isLowAmmo) Color(0xFFFF5252) else Color(0xFF90A4AE),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = ArcadeTheme.terminalBody
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "$animatedScore",
                            color = if (isLowAmmo) Color(0xFFFF5252) else Color(0xFF00E5FF),
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = ArcadeTheme.pixelHeader,
                            modifier = Modifier.scale(scoreScaleAnim.value)
                        )
                        Text(
                            text = "PTS",
                            color = if (isLowAmmo) Color(0xFFFF8A80) else Color(0xFF80DEEA),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = ArcadeTheme.pixelHeader,
                            modifier = Modifier.padding(bottom = 3.dp)
                        )
                    }

                    if (isLowAmmo && animatedScore > 0) {
                        Text(
                            text = "⚠️ LOW AMMO • PURGE BUGS / GEMS",
                            color = Color(0xFFFF5252),
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }

            // Dynamic Combo Multiplier Bar with Scale Pop and Overdrive Glow
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .scale(multiplierScaleAnim.value)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xD90A111C))
                    .border(
                        2.dp,
                        if (multiplier == 5) overdriveBorderGlow else multiplierColor,
                        RoundedCornerShape(12.dp)
                    )
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        for (tier in 1..5) {
                            val dotActive = multiplier >= tier
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(if (dotActive) multiplierColor else Color(0xFF263238))
                            )
                        }
                    }
                    Text(
                        text = if (multiplier == 5) "🔥 5X MAX OVERDRIVE!" else "${multiplier}X COMBO",
                        color = if (multiplier == 5) overdriveBorderGlow else multiplierColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = ArcadeTheme.pixelHeader
                    )
                }
            }

            // Squashed Bugs Counter
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xD90A111C))
                    .border(1.dp, Color(0xFFFF7043).copy(alpha = 0.6f), RoundedCornerShape(10.dp))
                    .padding(horizontal = 12.dp, vertical = 5.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "💥 SQUASHED",
                        color = Color(0xFFB0BEC5),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "$bugsSquashed",
                        color = Color(0xFFFF7043),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }

            // Active Power-Up Card
            if (activePowerUp != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xD90A111C))
                        .border(1.5.dp, Color(0xFF00E5FF), RoundedCornerShape(10.dp))
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "⚡ ${activePowerUp.title.uppercase()}",
                            color = Color(0xFF00E5FF),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            text = "${powerUpRemainingSeconds}s",
                            color = Color(0xFFFFD600),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }
        }

        // ---------------------------------------------------------
        // 4. BOTTOM-LEFT: Horizontally Docked Roguelite Perk Chips with Micro-Animations
        // ---------------------------------------------------------
        Row(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0xD90A111C))
                .border(1.5.dp, Color(0xFF00E5FF).copy(alpha = 0.6f), RoundedCornerShape(14.dp))
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "UPGRADES:",
                color = Color(0xFF80DEEA),
                fontSize = 10.sp,
                fontWeight = FontWeight.Black
            )

            val allPerks = VersionPerk.values()
            allPerks.forEach { perk ->
                val isUnlocked = perk in unlockedPerks
                val isFirewallReady = perk == VersionPerk.REFACTOR_FIREWALL && firewallActive
                val chipBorder = when {
                    isFirewallReady -> Color(0xFF00E5FF).copy(alpha = firewallShieldAlpha)
                    isUnlocked -> Color(0xFF00E5FF)
                    else -> Color(0xFF263238)
                }
                val chipBg = if (isUnlocked) Color(0x3300E5FF) else Color(0x22111822)

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(chipBg)
                        .border(1.dp, chipBorder, RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            when (perk) {
                                VersionPerk.GARBAGE_COLLECTOR_DRONE -> {
                                    Text(
                                        text = "🤖",
                                        fontSize = 10.sp,
                                        modifier = if (isUnlocked) Modifier.rotate(droneSpinDeg) else Modifier
                                    )
                                    Text(
                                        text = "GC DRONE",
                                        color = if (isUnlocked) Color(0xFF00E5FF) else Color(0xFF546E7A),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                VersionPerk.COROUTINES_TURRET -> {
                                    Text(
                                        text = "🗼",
                                        fontSize = 10.sp,
                                        modifier = if (isUnlocked) Modifier.scale(turretCrosshairPulse) else Modifier
                                    )
                                    Text(
                                        text = "TURRET",
                                        color = if (isUnlocked) Color(0xFF00E5FF) else Color(0xFF546E7A),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                VersionPerk.JIT_COMPILER -> {
                                    Text(
                                        text = "⚡",
                                        fontSize = 10.sp,
                                        modifier = if (isUnlocked) Modifier.graphicsLayer { alpha = jitFlickerAlpha } else Modifier
                                    )
                                    Text(
                                        text = "JIT",
                                        color = if (isUnlocked) Color(0xFFFFD600) else Color(0xFF546E7A),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                VersionPerk.REFACTOR_FIREWALL -> {
                                    Text(
                                        text = "🛡️",
                                        fontSize = 10.sp,
                                        modifier = if (isFirewallReady) Modifier.scale(firewallShieldAlpha) else Modifier
                                    )
                                    Text(
                                        text = "FIREWALL",
                                        color = if (isUnlocked) Color(0xFF00E5FF) else Color(0xFF546E7A),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                        val statusText = when {
                            !isUnlocked -> "LOCKED"
                            perk == VersionPerk.REFACTOR_FIREWALL && firewallActive -> "READY"
                            perk == VersionPerk.REFACTOR_FIREWALL && !firewallActive -> "${((firewallRechargeTimerMs + 999) / 1000).coerceAtLeast(1)}s"
                            else -> "ACTIVE"
                        }
                        Text(
                            text = statusText,
                            color = if (isUnlocked) Color(0xFF00E676) else Color(0xFF455A64),
                            fontSize = 8.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        // ---------------------------------------------------------
        // 5. CENTER SCREEN: Floating Holographic Alert Banners
        // ---------------------------------------------------------
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 70.dp),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier.widthIn(max = 520.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Critical Boss Warning Siren or Kernel Panic
                if (bossKernelPanic) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFD50000))
                            .border(2.dp, Color(0xFFFF9100), RoundedCornerShape(12.dp))
                            .padding(vertical = 10.dp, horizontal = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "🚨 KERNEL PANIC DETECTED • TITAN 5-LASER SWEEP OVERDRIVE! 🚨",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                } else if (bossWarningSiren) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFD50000))
                            .border(2.dp, Color(0xFFFFEA00), RoundedCornerShape(12.dp))
                            .padding(vertical = 10.dp, horizontal = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "🚨 CRITICAL BREACH: OutOfMemoryError TITAN HAS SPAWNED! 🚨",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }

                // Boss Health Bar
                if (bossHp != null) {
                    val (currentHp, maxHp) = bossHp
                    val hpFraction = (currentHp.toFloat() / maxHp.toFloat()).coerceIn(0f, 1f)
                    val isEnraged = currentHp <= maxHp / 2

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xE621002A))
                            .border(
                                2.dp,
                                if (bossKernelPanic) Color(0xFFFF1744) else if (isEnraged) Color(0xFFFF5252) else Color(0xFFAB47BC),
                                RoundedCornerShape(12.dp)
                            )
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = when {
                                    bossKernelPanic -> "🔥 TITAN: KERNEL PANIC (5-LASER SWEEP)"
                                    isEnraged -> "🔥 TITAN ENRAGED (PHASE 2 MINIONS)"
                                    else -> "⚠️ OutOfMemoryError TITAN"
                                },
                                color = if (bossKernelPanic) Color(0xFFFF1744) else if (isEnraged) Color(0xFFFF8A80) else Color(0xFFFF80AB),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black
                            )
                            Text(
                                text = "$currentHp / $maxHp HP",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(10.dp)
                                .clip(RoundedCornerShape(5.dp))
                                .background(Color(0xFF37474F))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(hpFraction)
                                    .height(10.dp)
                                    .clip(RoundedCornerShape(5.dp))
                                    .background(if (bossKernelPanic) Color(0xFFFF1744) else if (isEnraged) Color(0xFFFF5252) else Color(0xFFE040FB))
                            )
                        }
                    }
                }

                // Killstreak Fanfare Ribbon
                if (killstreakBanner != null) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF1E2838))
                            .border(2.dp, Color(0xFFFFD600), RoundedCornerShape(10.dp))
                            .padding(horizontal = 24.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "🔥 $killstreakBanner 🔥",
                            color = Color(0xFFFFD600),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = ArcadeTheme.pixelHeader
                        )
                    }
                }

                // Floor Ascension Cinematic Splash
                if (floorAscensionSplashTimerMs > 0 && floorAscensionTitle.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xEE0A121D))
                            .border(3.dp, Color(0xFF00E5FF), RoundedCornerShape(16.dp))
                            .padding(horizontal = 24.dp, vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "🚀 $floorAscensionTitle 🚀",
                                color = Color(0xFF00E5FF),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = ArcadeTheme.pixelHeader
                            )
                            Text(
                                text = floorAscensionSubtitle,
                                color = Color(0xFFFFD600),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = ArcadeTheme.terminalBody
                            )
                        }
                    }
                }

                // Trivia Feedback Toast
                if (triviaFeedback != null) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xEE1E2838))
                            .border(1.dp, Color(0xFFFFD54F), RoundedCornerShape(8.dp))
                            .padding(horizontal = 14.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = triviaFeedback,
                            color = Color(0xFFFFD54F),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

private fun getDirectionArrow(angleRad: Float): String {
    val deg = ((Math.toDegrees(angleRad.toDouble()) % 360 + 360) % 360).toFloat()
    return when {
        deg in 22.5f..67.5f -> "↘"
        deg in 67.5f..112.5f -> "↓"
        deg in 112.5f..157.5f -> "↙"
        deg in 157.5f..202.5f -> "←"
        deg in 202.5f..247.5f -> "↖"
        deg in 247.5f..292.5f -> "↑"
        deg in 292.5f..337.5f -> "↗"
        else -> "→"
    }
}

@Preview
@Composable
fun HUDOverlayGameplayPreview() {
    HUDOverlay(
        remainingSeconds = 85,
        score = 8450,
        multiplier = 3,
        bugsSquashed = 14,
        activePowerUp = null,
        powerUpRemainingSeconds = 0,
        bossHp = null,
        killstreakBanner = "TRIPLE SQUASH! 3X MULTIPLIER",
        unlockedPerks = setOf(VersionPerk.GARBAGE_COLLECTOR_DRONE),
        isMuted = false,
        activeArchetype = PlayerArchetype.CYBER_CODER,
        dashCooldownFraction = 0.45f,
        nearestBugDistance = 320f,
        nearestBugAngleRad = 0.8f,
        triviaFeedback = "CORRECT! MULTIPLIER BOOST + SPEED ACTIVATED",
        activeWeapon = WeaponType.RAPID_BYTE_BLASTER,
        firewallActive = false,
        firewallRechargeTimerMs = 0
    )
}

@Preview
@Composable
fun HUDOverlayBossSirenPreview() {
    HUDOverlay(
        remainingSeconds = 48,
        score = 15200,
        multiplier = 4,
        bugsSquashed = 26,
        activePowerUp = PowerUpType.FREEZE_SHIELD,
        powerUpRemainingSeconds = 6,
        bossHp = Pair(18, 25),
        bossWarningSiren = true,
        bossKernelPanic = false,
        killstreakBanner = null,
        unlockedPerks = setOf(VersionPerk.GARBAGE_COLLECTOR_DRONE, VersionPerk.COROUTINES_TURRET),
        isMuted = false,
        activeArchetype = PlayerArchetype.ARCH_DEV,
        dashCooldownFraction = 0f,
        nearestBugDistance = null,
        nearestBugAngleRad = null,
        triviaFeedback = null,
        activeWeapon = WeaponType.HEAVY_RAILGUN,
        firewallActive = false,
        firewallRechargeTimerMs = 0
    )
}

@Preview
@Composable
fun HUDOverlayBossKernelPanicPreview() {
    HUDOverlay(
        remainingSeconds = 12,
        score = 21900,
        multiplier = 5,
        bugsSquashed = 38,
        activePowerUp = PowerUpType.MEGA_FRENZY_NUKE,
        powerUpRemainingSeconds = 8,
        bossHp = Pair(8, 25),
        bossWarningSiren = false,
        bossKernelPanic = true,
        killstreakBanner = "SUPERCHARGED! ALL PERKS ONLINE",
        unlockedPerks = VersionPerk.values().toSet(),
        isMuted = false,
        activeArchetype = PlayerArchetype.KOTLIN_BOT,
        dashCooldownFraction = 0f,
        nearestBugDistance = 180f,
        nearestBugAngleRad = -1.2f,
        triviaFeedback = "KERNEL PANIC ENGAGED! BEWARE 5-LASER BARRAGE!",
        activeWeapon = WeaponType.TWIN_PULSE,
        firewallActive = true,
        firewallRechargeTimerMs = 0
    )
}
