package pro.jayeshseth.bugrush.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isPrimaryPressed
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.IntSize
import com.pandulapeter.kubriko.KubrikoViewport
import pro.jayeshseth.bugrush.game.ConferenceGameStateHolder
import pro.jayeshseth.bugrush.game.managers.GameSessionState
import pro.jayeshseth.bugrush.game.renderer.CyberpunkArcadeShader
import pro.jayeshseth.bugrush.game.renderer.IsometricProjection
import pro.jayeshseth.bugrush.leaderboard.LeaderboardEntry
import pro.jayeshseth.bugrush.leaderboard.LocalLeaderboardManager
import io.github.shaderengine.core.rememberShaderController
import io.github.shaderengine.modifiers.shaderRenderEffect
import kotlinx.coroutines.delay

@Composable
fun ConferenceGameApp(
    stateHolder: ConferenceGameStateHolder = remember { ConferenceGameStateHolder() }
) {
    DisposableEffect(Unit) {
        onDispose { stateHolder.dispose() }
    }

    val arenaManager = stateHolder.arenaManager
    val textMeasurer = rememberTextMeasurer()
    arenaManager.textMeasurer = textMeasurer

    val sessionState by arenaManager.sessionState.collectAsState()
    val remainingTime by arenaManager.remainingTimeSeconds.collectAsState()
    val score by arenaManager.score.collectAsState()
    val multiplier by arenaManager.multiplier.collectAsState()
    val bugsSquashed by arenaManager.bugsSquashedTotal.collectAsState()
    val activePowerUp by arenaManager.activePowerUp.collectAsState()
    val powerUpRemaining by arenaManager.powerUpRemainingSeconds.collectAsState()
    val bossHp by arenaManager.bossHp.collectAsState()
    val bossWarningSiren by arenaManager.bossWarningSiren.collectAsState()
    val bossKernelPanic by arenaManager.bossKernelPanic.collectAsState()
    val killstreakBanner by arenaManager.killstreakBanner.collectAsState()
    val unlockedPerks by arenaManager.unlockedPerks.collectAsState()
    val perkOptions by arenaManager.perkSelectionOptions.collectAsState()
    val playerHandle by arenaManager.playerHandle.collectAsState()
    val selectedArchetype by arenaManager.selectedArchetype.collectAsState()
    val dashCooldownFraction by arenaManager.dashCooldownFraction.collectAsState()
    val zoomMultiplier by arenaManager.zoomMultiplier.collectAsState()
    val currentFloor by arenaManager.currentFloor.collectAsState()
    val bugsSquashedThisFloor by arenaManager.bugsSquashedThisFloor.collectAsState()
    val bugsTargetForElevator by arenaManager.bugsTargetForElevator.collectAsState()
    val isElevatorOnline by arenaManager.isElevatorOnline.collectAsState()
    val floorAscensionSplashTimerMs by arenaManager.floorAscensionSplashTimerMs.collectAsState()
    val floorAscensionTitle by arenaManager.floorAscensionTitle.collectAsState()
    val floorAscensionSubtitle by arenaManager.floorAscensionSubtitle.collectAsState()
    val nearestBugDistance by arenaManager.nearestBugDistance.collectAsState()
    val nearestBugAngleRad by arenaManager.nearestBugAngleRad.collectAsState()
    val elevatorDistance by arenaManager.elevatorDistance.collectAsState()
    val elevatorAngleRad by arenaManager.elevatorAngleRad.collectAsState()
    val activeBountyDistance by arenaManager.activeBountyDistance.collectAsState()
    val activeBountyAngleRad by arenaManager.activeBountyAngleRad.collectAsState()
    val activeBountyRemainingKills by arenaManager.activeBountyRemainingKills.collectAsState()
    val activeBountyTimeSeconds by arenaManager.activeBountyTimeSeconds.collectAsState()
    val bossDistance by arenaManager.bossDistance.collectAsState()
    val bossAngleRad by arenaManager.bossAngleRad.collectAsState()
    val triviaQuestion by arenaManager.currentTriviaQuestion.collectAsState()
    val triviaFeedback by arenaManager.triviaFeedback.collectAsState()
    val isMuted by stateHolder.isMuted.collectAsState()
    val leaderboardEntries by LocalLeaderboardManager.entries.collectAsState()
    val activeWeapon by arenaManager.activeWeapon.collectAsState()
    val firewallActive by arenaManager.isFirewallShieldActive.collectAsState()
    val firewallRechargeTimerMs by arenaManager.firewallRechargeTimerMs.collectAsState()

    val shaderController = rememberShaderController(CyberpunkArcadeShader.SHADER)

    val focusRequester = remember { FocusRequester() }
    var isShootingWithMouse by remember { mutableStateOf(false) }
    var containerSize by remember { mutableStateOf(IntSize.Zero) }


    LaunchedEffect(sessionState) {
        if (sessionState != GameSessionState.PLAYING) {
            isShootingWithMouse = false
        }
        if (!stateHolder.isTextFieldFocused) {
            focusRequester.requestFocus()
        }
    }

    // Continuous hold-to-fire loop while Left Mouse Button is depressed
    LaunchedEffect(isShootingWithMouse, sessionState) {
        while (isShootingWithMouse && sessionState == GameSessionState.PLAYING) {
            arenaManager.firePlayerWeapon()
            delay(40L)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0E14))
            .onSizeChanged {
                containerSize = it
                arenaManager.screenWidth = it.width.toFloat()
                arenaManager.screenHeight = it.height.toFloat()
            }
            .focusRequester(focusRequester)
            .focusable()
            .pointerInput(containerSize, sessionState) {
                if (sessionState != GameSessionState.PLAYING) return@pointerInput
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull() ?: continue
                        val pos = change.position

                        if (containerSize.width > 0 && containerSize.height > 0) {
                            val screenCenterX = containerSize.width * 0.5f
                            val screenCenterY = containerSize.height * 0.55f

                            val (worldX, worldY) = IsometricProjection.unproject(
                                screenX = pos.x,
                                screenY = pos.y,
                                groundZ = 0f,
                                cameraCenterX = arenaManager.smoothCameraX,
                                cameraCenterY = arenaManager.smoothCameraY,
                                screenCenterX = screenCenterX,
                                screenCenterY = screenCenterY,
                                zoom = arenaManager.smoothZoom,
                                cameraAngleRad = arenaManager.currentCameraAngleRad
                            )
                            arenaManager.updateMouseAim(worldX, worldY)
                        }

                        when (event.type) {
                            PointerEventType.Press -> {
                                if (sessionState == GameSessionState.PLAYING) {
                                    focusRequester.requestFocus()
                                    if (event.buttons.isSecondaryPressed) {
                                        arenaManager.triggerPlayerDash()
                                    } else if (event.buttons.isPrimaryPressed) {
                                        isShootingWithMouse = true
                                        arenaManager.firePlayerWeapon()
                                    }
                                }
                            }
                            PointerEventType.Release -> {
                                if (!event.buttons.isPrimaryPressed) {
                                    isShootingWithMouse = false
                                }
                            }
                        }
                    }
                }
            }
            .onKeyEvent { keyEvent ->
                stateHolder.handleKeyEvent(keyEvent)
            }
    ) {
        // 1. Live Kubriko Viewport & In-Game HUD (unified under hardware-accelerated Cyberpunk CRT Shader)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .shaderRenderEffect(
                    controller = shaderController,
                    uniforms = {
                        float("barrelWarp", CyberpunkArcadeShader.DEFAULT_BARREL_WARP)
                        float("scanlineDark", CyberpunkArcadeShader.DEFAULT_SCANLINE_DARK)
                        float("uSpecularBoost", CyberpunkArcadeShader.DEFAULT_SPECULAR_BOOST)
                        float("glitchIntensity", stateHolder.glitchIntensity)
                        float("crimsonAlert", stateHolder.crimsonAlert)
                        float("combatAberration", stateHolder.combatAberration)
                        float("shockwaveProgress", stateHolder.shockwaveProgress)
                        float("shockwaveIntensity", stateHolder.shockwaveIntensity)
                        vec2("shockwaveOrigin", stateHolder.shockwaveOrigin)
                    }
                )
        ) {
            KubrikoViewport(
                modifier = Modifier.fillMaxSize(),
                kubriko = stateHolder.kubriko
            )

            // 2. In-Game HUD (rendered inside CRT lens pass)
            if (sessionState == GameSessionState.PLAYING || sessionState == GameSessionState.PAUSED || sessionState == GameSessionState.TRIVIA_MODAL || (sessionState == GameSessionState.BRIEFING && arenaManager.isBriefingPausedFromGame)) {
                HUDOverlay(
                    remainingSeconds = remainingTime,
                    score = score,
                    multiplier = multiplier,
                    bugsSquashed = bugsSquashed,
                    activePowerUp = activePowerUp,
                    powerUpRemainingSeconds = powerUpRemaining,
                    bossHp = bossHp,
                    bossWarningSiren = bossWarningSiren,
                    bossKernelPanic = bossKernelPanic,
                    killstreakBanner = killstreakBanner,
                    unlockedPerks = unlockedPerks,
                    isMuted = isMuted,
                    onToggleMute = { stateHolder.toggleMute() },
                    activeArchetype = selectedArchetype,
                    dashCooldownFraction = dashCooldownFraction,
                    zoomMultiplier = zoomMultiplier,
                    currentFloor = currentFloor,
                    bugsSquashedThisFloor = bugsSquashedThisFloor,
                    bugsTargetForElevator = bugsTargetForElevator,
                    isElevatorOnline = isElevatorOnline,
                    floorAscensionSplashTimerMs = floorAscensionSplashTimerMs,
                    floorAscensionTitle = floorAscensionTitle,
                    floorAscensionSubtitle = floorAscensionSubtitle,
                    nearestBugDistance = nearestBugDistance,
                    nearestBugAngleRad = nearestBugAngleRad,
                    elevatorDistance = elevatorDistance,
                    elevatorAngleRad = elevatorAngleRad,
                    activeBountyDistance = activeBountyDistance,
                    activeBountyAngleRad = activeBountyAngleRad,
                    activeBountyRemainingKills = activeBountyRemainingKills,
                    activeBountyTimeSeconds = activeBountyTimeSeconds,
                    bossDistance = bossDistance,
                    bossAngleRad = bossAngleRad,
                    triviaFeedback = triviaFeedback,
                    activeWeapon = activeWeapon,
                    firewallActive = firewallActive,
                    firewallRechargeTimerMs = firewallRechargeTimerMs
                )
            }
        }

        // 3. Modal Overlays by State
        when (sessionState) {
            GameSessionState.STARTUP -> {
                val isLeaderboardOpen by arenaManager.isStartupLeaderboardVisible.collectAsState()
                val startupMenuIndex by arenaManager.startupMenuIndex.collectAsState()
                StartupOverlay(
                    leaderboardEntries = leaderboardEntries,
                    onStartGame = {
                        arenaManager.startBriefing(isPausedFromPlaying = false, isFromStartup = true)
                    },
                    isLeaderboardOpen = isLeaderboardOpen,
                    onToggleLeaderboard = { if (it) arenaManager.openStartupLeaderboard() else arenaManager.closeStartupLeaderboard() },
                    selectedMenuItem = startupMenuIndex,
                    onSelectMenuItem = { arenaManager.selectStartupMenuItem(it) }
                )
            }
            GameSessionState.NAME_ENTRY -> {
                NameEntryOverlay(
                    playerHandle = playerHandle,
                    onUpdatePlayerHandle = { arenaManager.updatePlayerHandle(it) },
                    onTextFieldFocusChanged = { stateHolder.isTextFieldFocused = it },
                    onConfirmName = { name ->
                        stateHolder.isTextFieldFocused = false
                        arenaManager.updatePlayerHandle(name)
                        arenaManager.startCountdown()
                    }
                )
            }
            GameSessionState.COUNTDOWN -> {
                CountdownOverlay(
                    soundPlayer = stateHolder.soundPlayer,
                    onCountdownComplete = {
                        arenaManager.startNewGame()
                    }
                )
            }
            GameSessionState.ATTRACT -> {
                val isLeaderboardOpen by arenaManager.isStartupLeaderboardVisible.collectAsState()
                val startupMenuIndex by arenaManager.startupMenuIndex.collectAsState()
                StartupOverlay(
                    leaderboardEntries = leaderboardEntries,
                    onStartGame = {
                        arenaManager.startBriefing(isPausedFromPlaying = false)
                    },
                    isLeaderboardOpen = isLeaderboardOpen,
                    onToggleLeaderboard = { if (it) arenaManager.openStartupLeaderboard() else arenaManager.closeStartupLeaderboard() },
                    selectedMenuItem = startupMenuIndex,
                    onSelectMenuItem = { arenaManager.selectStartupMenuItem(it) }
                )
            }
            GameSessionState.BRIEFING -> {
                val briefingSlide by arenaManager.briefingSlideIndex.collectAsState()
                MissionBriefingOverlay(
                    currentSlide = briefingSlide,
                    isPausedMode = arenaManager.isBriefingPausedFromGame,
                    onNextSlide = { arenaManager.nextBriefingSlide() },
                    onPrevSlide = { arenaManager.prevBriefingSlide() },
                    onSelectSlide = { arenaManager.setBriefingSlide(it) },
                    onDeploy = {
                        arenaManager.dismissBriefing()
                    },
                    onSkipAll = {
                        arenaManager.dismissBriefing()
                    }
                )
            }
            GameSessionState.TRIVIA_MODAL -> {
                val lockoutMs by arenaManager.triviaLockoutRemainingMs.collectAsState()
                triviaQuestion?.let { question ->
                    TriviaOverlay(
                        question = question,
                        perkOptions = perkOptions,
                        lockoutRemainingMs = lockoutMs,
                        onAnswerSelected = { index -> arenaManager.submitTriviaAnswer(index) },
                        onPerkSelected = { perk -> arenaManager.selectVersionPerk(perk) }
                    )
                }
            }
            GameSessionState.ROUND_OVER -> {
                GameOverOverlay(
                    finalScore = score,
                    gemsCollected = arenaManager.gemsCollectedTotal.value,
                    bugsSquashed = arenaManager.bugsSquashedTotal.value,
                    triviaCorrect = arenaManager.triviaCorrectCount.value,
                    triviaAttempted = arenaManager.triviaAttemptedCount.value,
                    highestMultiplier = multiplier,
                    playerHandle = playerHandle,
                    onViewLeaderboard = {
                        arenaManager.openStartupLeaderboard()
                    }
                )
            }
            GameSessionState.PAUSED -> {
                PauseOverlay(
                    onResume = { arenaManager.resumeGame() },
                    onQuitToTitle = { arenaManager.quitToStartup() }
                )
            }
            GameSessionState.PLAYING -> {}
        }
    }
}
