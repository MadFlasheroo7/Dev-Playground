package pro.jayeshseth.bugrush.game

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import com.pandulapeter.kubriko.Kubriko
import pro.jayeshseth.bugrush.audio.NoOpSoundPlayer
import pro.jayeshseth.bugrush.audio.SoundPlayer
import pro.jayeshseth.bugrush.game.actors.PlayerArchetype
import pro.jayeshseth.bugrush.game.managers.ArenaManager
import pro.jayeshseth.bugrush.game.managers.GameSessionState
import pro.jayeshseth.bugrush.game.renderer.NoOpSpaceBackground
import pro.jayeshseth.bugrush.game.renderer.SpaceBackground
import com.pandulapeter.kubriko.gamepadInput.GamepadInputManager
import com.pandulapeter.kubriko.keyboardInput.KeyboardInputManager
import com.pandulapeter.kubriko.manager.ActorManager
import com.pandulapeter.kubriko.manager.StateManager
import com.pandulapeter.kubriko.manager.ViewportManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class ConferenceGameStateHolder(
    val soundPlayer: SoundPlayer = NoOpSoundPlayer,
    val spaceBackground: SpaceBackground = NoOpSpaceBackground
) {

    val arenaManager = ArenaManager(soundPlayer = soundPlayer, spaceBackground = spaceBackground)
    val keyboardInputManager = KeyboardInputManager.newInstance()
    val gamepadInputManager = GamepadInputManager.newInstance()
    val actorManager = ActorManager.newInstance(
        shouldUpdateActorsWhileNotRunning = true,
        shouldPutFarAwayActorsToSleep = false,
    )
    val viewportManager = ViewportManager.newInstance()
    val stateManager = StateManager.newInstance()

    val kubriko = Kubriko.newInstance(
        actorManager,
        viewportManager,
        stateManager,
        keyboardInputManager,
        gamepadInputManager,
        arenaManager
    )

    private val heldMovementKeys = mutableSetOf<Key>()

    var isTextFieldFocused: Boolean = false

    private val _isMuted = MutableStateFlow(soundPlayer.isMuted)
    val isMuted = _isMuted.asStateFlow()

    // Reactive shader animation states for post-processing stack
    var shockwaveOrigin by mutableStateOf(Offset.Zero)
        private set
    var shockwaveProgress by mutableFloatStateOf(1f) // 1f = inactive/completed
        private set
    var shockwaveIntensity by mutableFloatStateOf(0f)
        private set
    var glitchIntensity by mutableFloatStateOf(0f)
        private set
    var crimsonAlert by mutableFloatStateOf(0f)
        private set
    var combatAberration by mutableFloatStateOf(0f)
        private set

    private var glitchTimerMs: Int = 0
    private var crimsonTimerMs: Int = 0
    private var combatAberrationTimerMs: Int = 0

    init {
        // Wire reactive hooks from ArenaManager to shader drivers
        arenaManager.onTriggerShockwave = { sx, sy, intensity ->
            triggerShockwave(sx, sy, intensity)
        }
        arenaManager.onTriggerGlitch = { dur, int ->
            triggerGlitch(dur, int)
        }
        arenaManager.onTriggerCrimsonAlert = { dur, int ->
            triggerCrimsonAlert(dur, int)
        }
        arenaManager.onTriggerCombatImpact = { int ->
            triggerCombatImpact(int)
        }
        arenaManager.onUpdateShader = { dt ->
            updateShaderState(dt)
        }
    }

    fun triggerShockwave(screenX: Float = -1f, screenY: Float = -1f, intensity: Float = 1.0f) {
        shockwaveOrigin = if (screenX >= 0f && screenY >= 0f) Offset(screenX, screenY) else Offset.Zero
        shockwaveIntensity = intensity
        shockwaveProgress = 0f
    }

    fun triggerGlitch(durationMs: Int = 320, intensity: Float = 0.75f) {
        glitchIntensity = intensity.coerceIn(0f, 1f)
        glitchTimerMs = durationMs
    }

    fun triggerCrimsonAlert(durationMs: Int = 650, intensity: Float = 0.85f) {
        crimsonAlert = intensity.coerceIn(0f, 1f)
        crimsonTimerMs = durationMs
    }

    fun triggerCombatImpact(intensity: Float = 0.6f) {
        combatAberration = intensity.coerceIn(0f, 1f)
        combatAberrationTimerMs = 450
    }

    fun updateShaderState(deltaTimeMs: Int) {
        // Advance kinetic shockwave
        if (shockwaveProgress < 1f) {
            val progressDelta = deltaTimeMs / 500f // ~500ms duration
            shockwaveProgress = (shockwaveProgress + progressDelta).coerceAtMost(1f)
            if (shockwaveProgress >= 1f) {
                shockwaveIntensity = 0f
            }
        }

        // Decay glitch jitter
        if (glitchTimerMs > 0) {
            glitchTimerMs -= deltaTimeMs
            glitchIntensity = (glitchTimerMs / 320f).coerceIn(0f, 1f)
        } else {
            glitchIntensity = 0f
        }

        // Decay crimson alert flare
        if (crimsonTimerMs > 0) {
            crimsonTimerMs -= deltaTimeMs
            crimsonAlert = (crimsonTimerMs / 650f).coerceIn(0f, 1f)
        } else {
            crimsonAlert = 0f
        }

        // Decay combat chromatic aberration
        if (combatAberrationTimerMs > 0) {
            combatAberrationTimerMs -= deltaTimeMs
            combatAberration = (combatAberrationTimerMs / 450f).coerceIn(0f, 1f)
        } else {
            combatAberration = 0f
        }
    }

    fun toggleMute() {
        arenaManager.toggleMute()
        _isMuted.value = if (soundPlayer === NoOpSoundPlayer) !_isMuted.value else arenaManager.isMuted
    }

    fun handleKey(
        key: Key,
        type: KeyEventType = KeyEventType.KeyDown,
        isShiftPressed: Boolean = false,
        isCtrlPressed: Boolean = false
    ): Boolean {
        // If the user is currently typing in a text field, bypass all game hotkeys
        if (isTextFieldFocused) return false

        if (type == KeyEventType.KeyUp) {
            if (key in heldMovementKeys) {
                heldMovementKeys.remove(key)
                updateHeldMovement()
                return true
            }
            return false
        }

        if (type != KeyEventType.KeyDown) return false

        // M Key toggles audio mute across all game states (when not typing in a text field)
        if (key == Key.M) {
            toggleMute()
            return true
        }

        when (arenaManager.sessionState.value) {
            GameSessionState.STARTUP -> {
                when (key) {
                    Key.DirectionUp, Key.W -> {
                        arenaManager.moveStartupMenuUp()
                        return true
                    }
                    Key.DirectionDown, Key.S -> {
                        arenaManager.moveStartupMenuDown()
                        return true
                    }
                    Key.One -> { arenaManager.selectArchetype(PlayerArchetype.CYBER_CODER); return true }
                    Key.Two -> { arenaManager.selectArchetype(PlayerArchetype.KOTLIN_BOT); return true }
                    Key.Three -> { arenaManager.selectArchetype(PlayerArchetype.ARCH_DEV); return true }
                    Key.Spacebar, Key.Enter -> {
                        if (arenaManager.isStartupLeaderboardVisible.value) {
                            arenaManager.closeStartupLeaderboard()
                            return true
                        }
                        when (arenaManager.startupMenuIndex.value) {
                            0 -> arenaManager.startBriefing(isPausedFromPlaying = false, isFromStartup = true)
                            1 -> arenaManager.openStartupLeaderboard()
                        }
                        return true
                    }
                    Key.L -> {
                        arenaManager.toggleStartupLeaderboard()
                        return true
                    }
                    Key.Escape -> {
                        if (arenaManager.isStartupLeaderboardVisible.value) {
                            arenaManager.closeStartupLeaderboard()
                            return true
                        }
                    }
                    Key.Tab, Key.R -> {
                        if (isShiftPressed) arenaManager.rotateCameraCounterClockwise() else arenaManager.rotateCameraClockwise()
                        return true
                    }
                    Key.Z -> { arenaManager.zoomIn(); return true }
                    Key.O -> { arenaManager.zoomOut(); return true }
                    Key.Zero, Key.NumPad0 -> { arenaManager.resetZoom(); return true }
                    else -> {}
                }
            }
            GameSessionState.NAME_ENTRY -> {
                if (key == Key.Enter || key == Key.NumPadEnter) {
                    isTextFieldFocused = false
                    arenaManager.startCountdown()
                    return true
                }
            }
            GameSessionState.COUNTDOWN -> {}
            GameSessionState.ATTRACT -> {
                when (key) {
                    Key.One -> { arenaManager.selectArchetype(PlayerArchetype.CYBER_CODER); return true }
                    Key.Two -> { arenaManager.selectArchetype(PlayerArchetype.KOTLIN_BOT); return true }
                    Key.Three -> { arenaManager.selectArchetype(PlayerArchetype.ARCH_DEV); return true }
                    Key.L -> { arenaManager.toggleStartupLeaderboard(); return true }
                    Key.Escape -> { arenaManager.closeStartupLeaderboard(); return true }
                    Key.H, Key.F1 -> { arenaManager.startBriefing(isPausedFromPlaying = false); return true }
                    else -> {}
                }

                if (key in setOf(Key.Spacebar, Key.Enter, Key.W, Key.A, Key.S, Key.D, Key.DirectionUp, Key.DirectionDown, Key.DirectionLeft, Key.DirectionRight)) {
                    arenaManager.startBriefing(isPausedFromPlaying = false)
                    return true
                }
            }
            GameSessionState.BRIEFING -> {
                when (key) {
                    Key.Spacebar, Key.Enter -> {
                        arenaManager.dismissBriefing()
                        return true
                    }
                    Key.DirectionRight, Key.D -> {
                        arenaManager.nextBriefingSlide()
                        return true
                    }
                    Key.DirectionLeft, Key.A -> {
                        arenaManager.prevBriefingSlide()
                        return true
                    }
                    Key.One -> {
                        arenaManager.setBriefingSlide(0)
                        return true
                    }
                    Key.Two -> {
                        arenaManager.setBriefingSlide(1)
                        return true
                    }
                    Key.Three -> {
                        arenaManager.setBriefingSlide(2)
                        return true
                    }
                    Key.Four -> {
                        arenaManager.setBriefingSlide(3)
                        return true
                    }
                    Key.Five -> {
                        arenaManager.setBriefingSlide(4)
                        return true
                    }
                    Key.Escape, Key.H, Key.F1 -> {
                        arenaManager.dismissBriefing()
                        return true
                    }
                }
            }
            GameSessionState.TRIVIA_MODAL -> {
                // If in Roguelite Perk selection mode:
                arenaManager.perkSelectionOptions.value?.let { perks ->
                    when (key) {
                        Key.One, Key.NumPad1, Key.A -> if (perks.isNotEmpty()) { arenaManager.selectVersionPerk(perks[0]); return true }
                        Key.Two, Key.NumPad2, Key.B -> if (perks.size > 1) { arenaManager.selectVersionPerk(perks[1]); return true }
                        Key.Three, Key.NumPad3, Key.C -> if (perks.size > 2) { arenaManager.selectVersionPerk(perks[2]); return true }
                        else -> {}
                    }
                    return false
                }

                // Normal Trivia answering (respect 2-second lockout delay):
                if (arenaManager.triviaLockoutRemainingMs.value > 0) {
                    return false
                }

                when (key) {
                    Key.One, Key.NumPad1, Key.A -> { arenaManager.submitTriviaAnswer(0); return true }
                    Key.Two, Key.NumPad2, Key.B -> { arenaManager.submitTriviaAnswer(1); return true }
                    Key.Three, Key.NumPad3, Key.C, Key.X -> { arenaManager.submitTriviaAnswer(2); return true }
                    Key.Four, Key.NumPad4, Key.D, Key.Y -> { arenaManager.submitTriviaAnswer(3); return true }
                }
            }
            GameSessionState.PLAYING -> {
                if (key == Key.Escape) {
                    heldMovementKeys.clear()
                    arenaManager.pauseGame()
                    return true
                }

                // Field Manual Pause & Briefing
                if (key == Key.H || key == Key.F1) {
                    heldMovementKeys.clear()
                    arenaManager.startBriefing(isPausedFromPlaying = true)
                    return true
                }

                // Cheat Codes: Ctrl = Switch Weapon, Shift = Cycle Buff
                if (key in setOf(Key.CtrlLeft, Key.CtrlRight)) {
                    arenaManager.cheatCycleWeapon()
                    return true
                }
                if (key in setOf(Key.ShiftLeft, Key.ShiftRight)) {
                    arenaManager.cheatCycleBuff()
                    return true
                }

                // Dynamic Camera Rotation (Tab or R for Clockwise, Shift+Tab for Counter-Clockwise)
                if (key == Key.Tab || key == Key.R) {
                    if (isShiftPressed) {
                        arenaManager.rotateCameraCounterClockwise()
                    } else {
                        arenaManager.rotateCameraClockwise()
                    }
                    return true
                }

                // Unbounded Zoom Controls (Z: Zoom In, O: Zoom Out, 0: Reset Zoom)
                if (key == Key.Z) {
                    arenaManager.zoomIn()
                    return true
                }
                if (key == Key.O) {
                    arenaManager.zoomOut()
                    return true
                }
                if (key == Key.Zero) {
                    arenaManager.resetZoom()
                    return true
                }

                if (key in setOf(Key.Spacebar, Key.J, Key.Enter)) {
                    arenaManager.firePlayerWeapon()
                    return true
                }
                if (key in setOf(Key.K, Key.C, Key.E)) {
                    arenaManager.triggerPlayerDash()
                    return true
                }
                if (key in setOf(Key.W, Key.A, Key.S, Key.D, Key.DirectionUp, Key.DirectionDown, Key.DirectionLeft, Key.DirectionRight)) {
                    heldMovementKeys.add(key)
                    updateHeldMovement()
                    return true
                }
            }
            GameSessionState.PAUSED -> {
                when (key) {
                    Key.Escape, Key.Spacebar, Key.Enter -> {
                        heldMovementKeys.clear()
                        arenaManager.resumeGame()
                        return true
                    }
                    Key.Q -> {
                        heldMovementKeys.clear()
                        arenaManager.quitToStartup()
                        return true
                    }
                    else -> {}
                }
            }
            GameSessionState.ROUND_OVER -> {
                if (type == KeyEventType.KeyDown && (key == Key.Enter || key == Key.Spacebar || key == Key.Escape)) {
                    arenaManager.openStartupLeaderboard()
                    return true
                }
            }
        }
        return false
    }

    fun handleKeyEvent(keyEvent: KeyEvent): Boolean {
        if (isTextFieldFocused) return false
        return handleKey(
            key = keyEvent.key,
            type = keyEvent.type,
            isShiftPressed = keyEvent.isShiftPressed,
            isCtrlPressed = keyEvent.isCtrlPressed
        )
    }

    private fun updateHeldMovement() {
        var dx = 0f
        var dy = 0f
        if (Key.W in heldMovementKeys || Key.DirectionUp in heldMovementKeys) dy -= 1f
        if (Key.S in heldMovementKeys || Key.DirectionDown in heldMovementKeys) dy += 1f
        if (Key.A in heldMovementKeys || Key.DirectionLeft in heldMovementKeys) dx -= 1f
        if (Key.D in heldMovementKeys || Key.DirectionRight in heldMovementKeys) dx += 1f
        arenaManager.setDirectMove(dx, dy)
    }

    fun dispose() {
        soundPlayer.stopBgm()
        soundPlayer.stopTitleTheme()
        kubriko.dispose()
    }
}

