package pro.jayeshseth.bugrush.game.managers

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextMeasurer
import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.gamepadInput.GamepadButton
import pro.jayeshseth.bugrush.audio.NoOpSoundPlayer
import pro.jayeshseth.bugrush.audio.SoundEffect
import pro.jayeshseth.bugrush.audio.SoundPlayer
import pro.jayeshseth.bugrush.game.actors.ArenaVoxelRendererActor
import pro.jayeshseth.bugrush.game.actors.BossVoxelActor
import pro.jayeshseth.bugrush.game.actors.BugHazardActor
import pro.jayeshseth.bugrush.game.actors.BugType
import pro.jayeshseth.bugrush.game.actors.CompanionDroneVoxelActor
import pro.jayeshseth.bugrush.game.actors.CrateVoxelActor
import pro.jayeshseth.bugrush.game.actors.DamageResult
import pro.jayeshseth.bugrush.game.actors.DataRelayTowerActor
import pro.jayeshseth.bugrush.game.actors.BountyBeaconVoxelActor
import pro.jayeshseth.bugrush.game.actors.ElevatorPadVoxelActor
import pro.jayeshseth.bugrush.game.actors.ExplosiveBarrelVoxelActor
import pro.jayeshseth.bugrush.game.actors.GemVoxelActor
import pro.jayeshseth.bugrush.game.actors.GunDropVoxelActor
import pro.jayeshseth.bugrush.game.actors.LaserFenceVoxelActor
import pro.jayeshseth.bugrush.game.actors.OverclockConduitVoxelActor
import pro.jayeshseth.bugrush.game.actors.PlayerArchetype
import pro.jayeshseth.bugrush.game.actors.PlayerVoxelActor
import pro.jayeshseth.bugrush.game.actors.ProjectileVoxelActor
import pro.jayeshseth.bugrush.game.actors.ServerRackVoxelActor
import pro.jayeshseth.bugrush.game.actors.TechBarricadeActor
import pro.jayeshseth.bugrush.game.actors.TreeVoxelActor
import pro.jayeshseth.bugrush.game.actors.TriviaShardVoxelActor
import pro.jayeshseth.bugrush.game.actors.TurretVoxelActor
import pro.jayeshseth.bugrush.game.actors.WeaponType
import pro.jayeshseth.bugrush.game.map.PerlinNoise
import pro.jayeshseth.bugrush.game.renderer.NoOpSpaceBackground
import pro.jayeshseth.bugrush.game.renderer.SpaceBackground
import pro.jayeshseth.bugrush.game.renderer.VoxelCuboid
import pro.jayeshseth.bugrush.trivia.KotlinQuestionBank
import pro.jayeshseth.bugrush.trivia.PowerUpType
import pro.jayeshseth.bugrush.trivia.QuestionTier
import pro.jayeshseth.bugrush.leaderboard.LeaderboardEntry
import pro.jayeshseth.bugrush.leaderboard.LocalLeaderboardManager
import pro.jayeshseth.bugrush.trivia.TriviaQuestion
import pro.jayeshseth.bugrush.trivia.VersionPerk
import com.pandulapeter.kubriko.manager.ActorManager
import com.pandulapeter.kubriko.manager.Manager
import com.pandulapeter.kubriko.manager.StateManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random


data class DebrisParticle(
    var x: Float,
    var y: Float,
    var z: Float,
    var vx: Float,
    var vy: Float,
    var vz: Float,
    val size: Float,
    val color: Color,
    var lifeMs: Int,
    val maxLifeMs: Int
)

data class FloatingCombatText(
    var worldX: Float,
    var worldY: Float,
    var worldZ: Float,
    val text: String,
    val color: Color,
    var lifeMs: Int,
    val maxLifeMs: Int = 1000
)

data class FloorTrace(
    val x: Float,
    val y: Float,
    val z: Float = -2f,
    val sizeX: Float,
    val sizeY: Float,
    val sizeZ: Float = 2f,
    val baseColor: Color,
    val pulsePhase: Float = 0f,
    val pulseSpeed: Float = 2.5f,
    val minAlpha: Float = 0.35f,
    val maxAlpha: Float = 0.95f
) {
    val cachedCuboid: VoxelCuboid = VoxelCuboid(
        x = x,
        y = y,
        z = z,
        sizeX = sizeX,
        sizeY = sizeY,
        sizeZ = sizeZ,
        baseColor = baseColor.copy(alpha = (minAlpha + maxAlpha) * 0.5f),
        isFloor = true
    )
}

class ArenaManager(
    var soundPlayer: SoundPlayer = NoOpSoundPlayer,
    val spaceBackground: SpaceBackground = NoOpSpaceBackground
) : Manager() {

    private val actorManager by manager<ActorManager>()
    private val stateManager by manager<StateManager>()

    private val _sessionState = MutableStateFlow(GameSessionState.STARTUP)
    val sessionState = _sessionState.asStateFlow()
    var isBriefingFromStartupFlow: Boolean = false

    private val _isStartupLeaderboardVisible = MutableStateFlow(false)
    val isStartupLeaderboardVisible = _isStartupLeaderboardVisible.asStateFlow()

    fun toggleStartupLeaderboard() {
        _isStartupLeaderboardVisible.value = !_isStartupLeaderboardVisible.value
    }

    fun openStartupLeaderboard() {
        _isStartupLeaderboardVisible.value = true
        _sessionState.value = GameSessionState.STARTUP
    }

    fun closeStartupLeaderboard() {
        _isStartupLeaderboardVisible.value = false
    }

    private val _startupMenuIndex = MutableStateFlow(0)
    val startupMenuIndex = _startupMenuIndex.asStateFlow()

    fun moveStartupMenuUp() {
        _startupMenuIndex.value = 0
    }

    fun moveStartupMenuDown() {
        _startupMenuIndex.value = 1
    }

    fun selectStartupMenuItem(index: Int) {
        _startupMenuIndex.value = index.coerceIn(0, 1)
    }

    private val _remainingTimeSeconds = MutableStateFlow(120)
    val remainingTimeSeconds = _remainingTimeSeconds.asStateFlow()

    private val _score = MutableStateFlow(0)
    val score = _score.asStateFlow()

    private val _multiplier = MutableStateFlow(1)
    val multiplier = _multiplier.asStateFlow()

    private val _streakGems = MutableStateFlow(0)
    val streakGems = _streakGems.asStateFlow()

    private val _gemsCollectedTotal = MutableStateFlow(0)
    val gemsCollectedTotal = _gemsCollectedTotal.asStateFlow()

    private val _bugsSquashedTotal = MutableStateFlow(0)
    val bugsSquashedTotal = _bugsSquashedTotal.asStateFlow()

    private val _triviaCorrectCount = MutableStateFlow(0)
    val triviaCorrectCount = _triviaCorrectCount.asStateFlow()

    private val _triviaAttemptedCount = MutableStateFlow(0)
    val triviaAttemptedCount = _triviaAttemptedCount.asStateFlow()

    private val _activePowerUp = MutableStateFlow<PowerUpType?>(null)
    val activePowerUp = _activePowerUp.asStateFlow()

    private val _powerUpRemainingSeconds = MutableStateFlow(0)
    val powerUpRemainingSeconds = _powerUpRemainingSeconds.asStateFlow()

    private val _bossHp = MutableStateFlow<Pair<Int, Int>?>(null) // (current, max)
    val bossHp = _bossHp.asStateFlow()

    private val _bossWarningSiren = MutableStateFlow(false)
    val bossWarningSiren = _bossWarningSiren.asStateFlow()

    private val _bossKernelPanic = MutableStateFlow(false)
    val bossKernelPanic = _bossKernelPanic.asStateFlow()

    private val _selectedArchetype = MutableStateFlow(PlayerArchetype.CYBER_CODER)
    val selectedArchetype = _selectedArchetype.asStateFlow()

    private val _dashCooldownFraction = MutableStateFlow(0f)
    val dashCooldownFraction = _dashCooldownFraction.asStateFlow()

    private val _hasPlayerFiredFirstBullet = MutableStateFlow(false)
    val hasPlayerFiredFirstBullet = _hasPlayerFiredFirstBullet.asStateFlow()

    private val _currentTriviaQuestion = MutableStateFlow<TriviaQuestion?>(null)
    val currentTriviaQuestion = _currentTriviaQuestion.asStateFlow()

    private val _triviaFeedback = MutableStateFlow<String?>(null)
    val triviaFeedback = _triviaFeedback.asStateFlow()

    private val _triviaLockoutRemainingMs = MutableStateFlow(0)
    val triviaLockoutRemainingMs = _triviaLockoutRemainingMs.asStateFlow()

    // Conference Developer Handle
    private val _playerHandle = MutableStateFlow("Dev_Kotlin_${Random.nextInt(10, 99)}")
    val playerHandle = _playerHandle.asStateFlow()

    fun updatePlayerHandle(newHandle: String) {
        _playerHandle.value = newHandle
    }

    fun setScoreForTest(value: Int) {
        _score.value = value
    }

    fun setRemainingTimeForTest(seconds: Int) {
        _remainingTimeSeconds.value = seconds
    }

    // Killstreak tracking & Banners
    private val _killstreak = MutableStateFlow(0)
    val killstreak = _killstreak.asStateFlow()

    private val _killstreakBanner = MutableStateFlow<String?>(null)
    val killstreakBanner = _killstreakBanner.asStateFlow()
    private var killstreakBannerTimerMs = 0

    // Roguelite Perks
    private val _unlockedPerks = MutableStateFlow<Set<VersionPerk>>(emptySet())
    val unlockedPerks = _unlockedPerks.asStateFlow()

    private val _perkSelectionOptions = MutableStateFlow<List<VersionPerk>?>(null)
    val perkSelectionOptions = _perkSelectionOptions.asStateFlow()

    // Screen Shake
    var shakeIntensity: Float = 0f
    val shakeOffsetX: Float get() = if (shakeIntensity > 0.01f) (Random.nextFloat() * 2f - 1f) * shakeIntensity else 0f
    val shakeOffsetY: Float get() = if (shakeIntensity > 0.01f) (Random.nextFloat() * 2f - 1f) * shakeIntensity else 0f

    fun addScreenShake(amount: Float) {
        shakeIntensity = (shakeIntensity + amount).coerceAtMost(32f)
    }

    // Floating Combat Text & Physics Debris Particles
    private val floatingTexts = mutableListOf<FloatingCombatText>()
    private val debrisParticles = mutableListOf<DebrisParticle>()
    var textMeasurer: TextMeasurer? = null

    fun getFloatingTextsSnapshot(): List<FloatingCombatText> =
        synchronized(floatingTexts) { floatingTexts.toList() }

    fun addFloatingText(x: Float, y: Float, text: String, color: Color, durationMs: Int = 900) {
        synchronized(floatingTexts) {
            floatingTexts.add(FloatingCombatText(x, y, 22f, text, color, durationMs, durationMs))
        }
    }

    fun spawnDebris(x: Float, y: Float, z: Float, color: Color, count: Int = 8, speed: Float = 0.25f, size: Float = 6f) {
        synchronized(debrisParticles) {
            for (i in 0 until count) {
                val angle = Random.nextFloat() * 6.28318f
                val horizSpeed = (Random.nextFloat() * 0.8f + 0.2f) * speed
                val vx = cos(angle) * horizSpeed
                val vy = sin(angle) * horizSpeed
                val vz = (Random.nextFloat() * 0.5f + 0.35f) * speed
                val life = Random.nextInt(400, 950)
                debrisParticles.add(
                    DebrisParticle(x, y, z, vx, vy, vz, size, color, life, life)
                )
            }
        }
    }

    var player = PlayerVoxelActor(1900f, 1900f, archetype = _selectedArchetype.value)

    // Active Weapon & Defensive Shield States
    private val _activeWeapon = MutableStateFlow(player.activeWeapon)
    val activeWeapon = _activeWeapon.asStateFlow()

    private val _isFirewallShieldActive = MutableStateFlow(player.isFirewallShieldActive)
    val isFirewallShieldActive = _isFirewallShieldActive.asStateFlow()

    private val _firewallRechargeTimerMs = MutableStateFlow(0)
    val firewallRechargeTimerMs = _firewallRechargeTimerMs.asStateFlow()

    // Reactive Shader Callbacks & Screen Projection
    var onTriggerShockwave: ((screenX: Float, screenY: Float, intensity: Float) -> Unit)? = null
    var onTriggerGlitch: ((durationMs: Int, intensity: Float) -> Unit)? = null
    var onTriggerCrimsonAlert: ((durationMs: Int, intensity: Float) -> Unit)? = null
    var onTriggerCombatImpact: ((intensity: Float) -> Unit)? = null
    var onUpdateShader: ((deltaTimeMs: Int) -> Unit)? = null

    var screenWidth: Float = 1920f
    var screenHeight: Float = 1080f

    fun projectWorldToScreen(worldX: Float, worldY: Float, groundZ: Float = 0f): Pair<Float, Float> {
        val scX = screenWidth * 0.5f
        val scY = screenHeight * 0.55f
        val offset = pro.jayeshseth.bugrush.game.renderer.IsometricProjection.project(
            worldX = worldX,
            worldY = worldY,
            worldZ = groundZ,
            cameraCenterX = smoothCameraX,
            cameraCenterY = smoothCameraY,
            screenCenterX = scX,
            screenCenterY = scY,
            zoom = smoothZoom,
            cameraAngleRad = currentCameraAngleRad
        )
        return Pair(offset.x, offset.y)
    }

    fun isInView(worldX: Float, worldY: Float, groundZ: Float = 0f, margin: Float = 40f): Boolean {
        if (screenWidth <= 0f || screenHeight <= 0f) {
            val dx = worldX - player.posX
            val dy = worldY - player.posY
            return (dx * dx + dy * dy) <= (1100f * 1100f)
        }
        val (sx, sy) = projectWorldToScreen(worldX, worldY, groundZ)
        return pro.jayeshseth.bugrush.game.renderer.IsometricProjection.isInViewport(
            screenX = sx,
            screenY = sy,
            viewportWidth = screenWidth,
            viewportHeight = screenHeight,
            margin = margin
        )
    }

    fun isBugInView(bug: BugHazardActor, margin: Float = 40f): Boolean =
        isInView(bug.posX, bug.posY, bug.posZ, margin)

    fun isCrateInView(crate: CrateVoxelActor, margin: Float = 40f): Boolean =
        isInView(crate.posX, crate.posY, crate.posZ, margin)

    fun isBossInView(boss: BossVoxelActor, margin: Float = 70f): Boolean =
        isInView(boss.posX, boss.posY, boss.posZ, margin)

    data class RangeDamageResult(
        val damage: Int,
        val isBoosted: Boolean,
        val isFalloff: Boolean,
        val distance: Float
    )

    fun calculateRangeDamage(
        baseDamage: Int,
        distance: Float,
        isHeavyRailgun: Boolean = false
    ): RangeDamageResult {
        return when {
            distance <= 250f -> RangeDamageResult(
                damage = baseDamage + 1,
                isBoosted = true,
                isFalloff = false,
                distance = distance
            )
            distance <= 600f -> RangeDamageResult(
                damage = baseDamage,
                isBoosted = false,
                isFalloff = false,
                distance = distance
            )
            else -> RangeDamageResult(
                damage = if (isHeavyRailgun || baseDamage >= 2) (baseDamage - 1).coerceAtLeast(1) else 1,
                isBoosted = false,
                isFalloff = isHeavyRailgun || baseDamage >= 2,
                distance = distance
            )
        }
    }

    internal fun updateBugSwarmAI(deltaTimeMs: Int) {
        val shouldAttract = _hasPlayerFiredFirstBullet.value
        bugs.forEach { bug ->
            if (shouldAttract && bug.bugType == BugType.NORMAL && isBugInView(bug)) {
                bug.updateSwarmAI(player.posX, player.posY, deltaTimeMs, bugs)
            } else {
                bug.hasSwarmTarget = false
            }
        }
    }

    fun triggerWorldShockwave(worldX: Float, worldY: Float, intensity: Float = 0.85f) {
        val (sx, sy) = projectWorldToScreen(worldX, worldY)
        onTriggerShockwave?.invoke(sx, sy, intensity)
    }

    fun calculateScreenAngleTo(targetWorldX: Float, targetWorldY: Float): Float {
        val relX = targetWorldX - player.posX
        val relY = targetWorldY - player.posY
        val cosA = cos(currentCameraAngleRad)
        val sinA = sin(currentCameraAngleRad)
        val rotX = relX * cosA - relY * sinA
        val rotY = relX * sinA + relY * cosA
        val screenDx = (rotX - rotY) * 0.5f * pro.jayeshseth.bugrush.game.renderer.IsometricProjection.TILE_WIDTH_MULTIPLIER
        val screenDy = (rotX + rotY) * 0.5f * pro.jayeshseth.bugrush.game.renderer.IsometricProjection.TILE_HEIGHT_MULTIPLIER
        return kotlin.math.atan2(screenDy, screenDx)
    }
    val bugs = mutableListOf<BugHazardActor>()
    private val gems = mutableListOf<GemVoxelActor>()
    val trees = mutableListOf<TreeVoxelActor>()
    val serverRacks = mutableListOf<ServerRackVoxelActor>()
    val dataRelayTowers = mutableListOf<DataRelayTowerActor>()
    val techBarricades = mutableListOf<TechBarricadeActor>()
    val crates = mutableListOf<CrateVoxelActor>()
    private val triviaShards = mutableListOf<TriviaShardVoxelActor>()
    val gunDrops = mutableListOf<GunDropVoxelActor>()
    val projectiles = mutableListOf<ProjectileVoxelActor>()
    private val turrets = mutableListOf<TurretVoxelActor>()
    val explosiveBarrels = mutableListOf<ExplosiveBarrelVoxelActor>()
    val overclockConduits = mutableListOf<OverclockConduitVoxelActor>()
    val laserFences = mutableListOf<LaserFenceVoxelActor>()
    val bountyBeacons = mutableListOf<BountyBeaconVoxelActor>()
    private var drone: CompanionDroneVoxelActor? = null
    var boss: BossVoxelActor? = null
    private var hasSpawnedBoss = false
    private var bossSirenTimerMs = 0
    private var bossTelegraphMs = 0

    // Procedural Perlin Map Data
    private var perlinNoise = PerlinNoise()
    val proceduralFloorTiles = mutableListOf<VoxelCuboid>()
    val proceduralFloorTraces = mutableListOf<FloorTrace>()
    private val proceduralWalls = mutableListOf<VoxelCuboid>()
    val walkableCells = mutableSetOf<Pair<Int, Int>>()
    val isCellWalkable = Array(37) { BooleanArray(37) }
    val cellHeights = mutableMapOf<Pair<Int, Int>, Float>()
    val ramps = mutableSetOf<Pair<Pair<Int, Int>, Pair<Int, Int>>>()
    private var cloudAnimTime: Float = 0f
    var arenaPulseTime: Float = 0f

    // Floor Progression State
    val currentFloor = MutableStateFlow(1)
    val bugsSquashedThisFloor = MutableStateFlow(0)
    val bugsTargetForElevator = MutableStateFlow(8)
    val isElevatorOnline = MutableStateFlow(false)
    var elevatorPad: ElevatorPadVoxelActor? = null

    // Floor Transition Splash Banner
    val floorAscensionSplashTimerMs = MutableStateFlow(0)
    val floorAscensionTitle = MutableStateFlow("")
    val floorAscensionSubtitle = MutableStateFlow("")

    // Waypoint Navigation & Bounty Telemetry
    val nearestBugDistance = MutableStateFlow<Float?>(null)
    val nearestBugAngleRad = MutableStateFlow<Float?>(null)
    val elevatorDistance = MutableStateFlow<Float?>(null)
    val elevatorAngleRad = MutableStateFlow<Float?>(null)
    val activeBountyDistance = MutableStateFlow<Float?>(null)
    val activeBountyAngleRad = MutableStateFlow<Float?>(null)
    val activeBountyRemainingKills = MutableStateFlow<Int?>(null)
    val activeBountyTimeSeconds = MutableStateFlow<Int?>(null)
    val bossDistance = MutableStateFlow<Float?>(null)
    val bossAngleRad = MutableStateFlow<Float?>(null)

    data class FloorTheme(
        val floorNumber: Int,
        val title: String,
        val subtitle: String,
        val primaryTraceColor: Color,
        val secondaryTraceColor: Color,
        val floorBaseColor: Color,
        val elevatedPlateauColor: Color,
        val highDeckColor: Color,
        val stairColor: Color,
        val stairStripeColor: Color,
        val wallColor: Color,
        val wallCapColor: Color,
        val bugTarget: Int
    )

    fun getFloorTheme(floor: Int): FloorTheme {
        return when ((floor - 1) % 4) {
            0 -> FloorTheme(
                floorNumber = floor,
                title = "SILICON VALLEY",
                subtitle = "Neutralize 8 rogue memory leaks to power extraction",
                primaryTraceColor = Color(0xFF00E5FF),
                secondaryTraceColor = Color(0x6600E5FF),
                floorBaseColor = Color(0xFF14202B),
                elevatedPlateauColor = Color(0xFF1E3545),
                highDeckColor = Color(0xFF264A63),
                stairColor = Color(0xFF00B0FF),
                stairStripeColor = Color(0xFF76FF03),
                wallColor = Color(0xFF21272E),
                wallCapColor = Color(0xFF00E5FF),
                bugTarget = 8
            )
            1 -> FloorTheme(
                floorNumber = floor,
                title = "DEEP MEMORY BANK",
                subtitle = "Corrupted bytecode threads detected • 12 bugs to activate elevator",
                primaryTraceColor = Color(0xFFFF007F),
                secondaryTraceColor = Color(0x66FF007F),
                floorBaseColor = Color(0xFF180E28),
                elevatedPlateauColor = Color(0xFF2A1545),
                highDeckColor = Color(0xFF3F1D68),
                stairColor = Color(0xFFD500F9),
                stairStripeColor = Color(0xFFFF4081),
                wallColor = Color(0xFF221133),
                wallCapColor = Color(0xFFFF007F),
                bugTarget = 12
            )
            2 -> FloorTheme(
                floorNumber = floor,
                title = "OVERCLOCKED CORE",
                subtitle = "Severe thermal throttling! • 16 bugs to extract before meltdown",
                primaryTraceColor = Color(0xFFFF6D00),
                secondaryTraceColor = Color(0x66FF3D00),
                floorBaseColor = Color(0xFF241008),
                elevatedPlateauColor = Color(0xFF3D1A0D),
                highDeckColor = Color(0xFF5A2510),
                stairColor = Color(0xFFFF9100),
                stairStripeColor = Color(0xFFFFD600),
                wallColor = Color(0xFF33140A),
                wallCapColor = Color(0xFFFF3D00),
                bugTarget = 16
            )
            else -> FloorTheme(
                floorNumber = floor,
                title = "QUANTUM SINGULARITY",
                subtitle = "WARNING: OutOfMemoryError TITAN incoming • Survive & eliminate 20 bugs",
                primaryTraceColor = Color(0xFF00FF66),
                secondaryTraceColor = Color(0x6600E676),
                floorBaseColor = Color(0xFF060F0A),
                elevatedPlateauColor = Color(0xFF0E2419),
                highDeckColor = Color(0xFF163D2A),
                stairColor = Color(0xFF00E676),
                stairStripeColor = Color(0xFF69F0AE),
                wallColor = Color(0xFF0B1F14),
                wallCapColor = Color(0xFF00FF66),
                bugTarget = 20
            )
        }
    }

    init {
        player.onDashTriggered = { onPlayerDashExecuted(player.posX, player.posY) }
        generateMapCells()
    }

    fun getGroundZ(worldX: Float, worldY: Float): Float = 0f

    // Procedural Perlin Arena Boundary Collision Check (O(1) grid lookup against random irregular borders)
    fun isPointWalkable(px: Float, py: Float): Boolean {
        val step = 100f
        val gridMin = 100f
        val gx = ((px - gridMin + step * 0.5f) / step).toInt()
        val gy = ((py - gridMin + step * 0.5f) / step).toInt()
        if (gx in 0..36 && gy in 0..36) {
            return isCellWalkable[gx][gy]
        }
        return false
    }

    fun isStepWalkable(fromX: Float, fromY: Float, toX: Float, toY: Float): Boolean {
        return isPointWalkable(toX, toY)
    }

    fun isPositionWalkable(x: Float, y: Float, radius: Float = 16f): Boolean {
        if (!isPointWalkable(x, y)) return false
        if (!isPointWalkable(x - radius, y)) return false
        if (!isPointWalkable(x + radius, y)) return false
        if (!isPointWalkable(x, y - radius)) return false
        if (!isPointWalkable(x, y + radius)) return false
        val diag = radius * 0.707f
        if (!isPointWalkable(x - diag, y - diag)) return false
        if (!isPointWalkable(x + diag, y - diag)) return false
        if (!isPointWalkable(x - diag, y + diag)) return false
        if (!isPointWalkable(x + diag, y + diag)) return false
        return true
    }

    // Dynamic Camera State (Rotation, Velocity Lookahead, and Dynamic Zoom)
    private var cameraAngleIndex: Int = 0
    var targetCameraAngleRad: Float = 0f
    var currentCameraAngleRad: Float = 0f
    var smoothCameraX: Float = 1900f
    var smoothCameraY: Float = 1900f
    var smoothZoom: Float = 0.52f * DEFAULT_ZOOM_MULTIPLIER

    // Manual & Dynamic Zoom Control (Z: Zoom In, O: Zoom Out, 0: Reset)
    private val _zoomMultiplier = MutableStateFlow(DEFAULT_ZOOM_MULTIPLIER)
    val zoomMultiplier = _zoomMultiplier.asStateFlow()
    var targetZoomMultiplier: Float = DEFAULT_ZOOM_MULTIPLIER

    fun formatZoom(value: Float): String {
        return ((value * 100).toInt() / 100f).toString()
    }

    fun zoomIn() {
        targetZoomMultiplier = (targetZoomMultiplier * 1.25f).coerceAtMost(100f)
        _triviaFeedback.value = "🔍 ZOOM: ${formatZoom(targetZoomMultiplier)}x"
        soundPlayer.playSfx(SoundEffect.LASER_PEW)
    }

    fun zoomOut() {
        targetZoomMultiplier = (targetZoomMultiplier / 1.25f).coerceAtLeast(0.005f)
        _triviaFeedback.value = "🔍 ZOOM: ${formatZoom(targetZoomMultiplier)}x"
        soundPlayer.playSfx(SoundEffect.DASH_WHOOSH)
    }

    fun resetZoom() {
        targetZoomMultiplier = DEFAULT_ZOOM_MULTIPLIER
        _triviaFeedback.value = "🔍 ZOOM RESET: ${formatZoom(DEFAULT_ZOOM_MULTIPLIER)}x"
        soundPlayer.playSfx(SoundEffect.UPGRADE_UNLOCKED)
    }

    fun rotateCameraClockwise() {
        cameraAngleIndex = (cameraAngleIndex + 1) % 4
        targetCameraAngleRad += (kotlin.math.PI.toFloat() * 0.5f)
        val deg = cameraAngleIndex * 90
        _triviaFeedback.value = "🎥 CAMERA ROTATED: $deg°"
        soundPlayer.playSfx(SoundEffect.UPGRADE_UNLOCKED)
    }

    fun rotateCameraCounterClockwise() {
        cameraAngleIndex = (cameraAngleIndex + 3) % 4
        targetCameraAngleRad -= (kotlin.math.PI.toFloat() * 0.5f)
        val deg = cameraAngleIndex * 90
        _triviaFeedback.value = "🎥 CAMERA ROTATED: $deg°"
        soundPlayer.playSfx(SoundEffect.UPGRADE_UNLOCKED)
    }

    // Mouse Aim Coordinates & Reticle
    var aimWorldX: Float = 1000f
    var aimWorldY: Float = 1000f
    var isMouseAiming: Boolean = false

    fun updateMouseAim(worldX: Float, worldY: Float) {
        aimWorldX = worldX
        aimWorldY = worldY
        isMouseAiming = true
        player.setAimDirection(worldX - player.posX, worldY - player.posY)
    }

    fun clearMouseAim() {
        isMouseAiming = false
        player.isAimOverridden = false
    }

    private var timeAccumulatorMs = 0
    private var powerUpAccumulatorMs = 0
    private var gemRespawnTimerMs = 0
    private var bugRespawnTimerMs = 0

    public override fun onInitialize(kubriko: Kubriko) {
        stateManager.isFocused
            .onEach(stateManager::updateIsRunning)
            .launchIn(scope)
        stateManager.updateIsRunning(true)
        startStartupMode()
    }

    fun selectArchetype(archetype: PlayerArchetype) {
        _selectedArchetype.value = archetype
        player.archetype = archetype
        player.activeWeapon = when (archetype) {
            PlayerArchetype.KOTLIN_BOT -> WeaponType.TWIN_PULSE
            PlayerArchetype.ARCH_DEV -> WeaponType.HEAVY_RAILGUN
            PlayerArchetype.CYBER_CODER -> WeaponType.RAPID_BYTE_BLASTER
        }
        _activeWeapon.value = player.activeWeapon
    }

    fun toggleMute() {
        soundPlayer.toggleMute()
    }

    val isMuted: Boolean get() = soundPlayer.isMuted

    fun setDirectMove(dx: Float, dy: Float) {
        if (_sessionState.value == GameSessionState.ATTRACT && (dx != 0f || dy != 0f)) {
            startBriefing()
            return
        }
        if (_sessionState.value == GameSessionState.BRIEFING) return
        val worldDx: Float
        val worldDy: Float
        if (currentCameraAngleRad != 0f) {
            val cosA = cos(currentCameraAngleRad)
            val sinA = sin(currentCameraAngleRad)
            worldDx = dx * cosA + dy * sinA
            worldDy = -dx * sinA + dy * cosA
        } else {
            worldDx = dx
            worldDy = dy
        }
        player.setDirectMove(worldDx, worldDy)
    }

    fun triggerPlayerDash() {
        if (_sessionState.value != GameSessionState.PLAYING) return
        val dashX = player.posX
        val dashY = player.posY
        if (player.triggerDash()) {
            onPlayerDashExecuted(dashX, dashY)
        }
    }

    fun onPlayerDashExecuted(dashX: Float = player.posX, dashY: Float = player.posY) {
        soundPlayer.playSfx(SoundEffect.DASH_WHOOSH)
        addScreenShake(3.5f)
        spawnDebris(dashX, dashY, 10f, Color(0xFF00E5FF), count = 6, speed = 0.18f, size = 4f)
        triggerWorldShockwave(dashX, dashY, intensity = 0.85f)
        onTriggerCombatImpact?.invoke(0.5f)

        // If Coroutines Turret perk is active, deploy turret at dash start point!
        if (player.hasCoroutinesTurrets) {
            val turret = TurretVoxelActor(dashX, dashY)
            turrets.add(turret)
            actorManager.add(turret)
            addFloatingText(dashX, dashY, "TURRET DEPLOYED!", Color(0xFF76FF03))
        }
    }

    fun cheatCycleWeapon() {
        if (_sessionState.value != GameSessionState.PLAYING) return
        val newWeapon = player.cycleWeapon()
        _activeWeapon.value = newWeapon
        _triviaFeedback.value = "🔫 CHEAT: Switched to ${newWeapon.displayName}"
    }

    private var cheatBuffIndex = 0

    fun cheatCycleBuff() {
        if (_sessionState.value != GameSessionState.PLAYING) return
        val buffs = PowerUpType.values()
        cheatBuffIndex = (cheatBuffIndex + 1) % buffs.size
        val buff = buffs[cheatBuffIndex]
        activatePowerUp(buff)
        _triviaFeedback.value = "⚡ CHEAT: Activated ${buff.title} (15s)"
    }

    private var lastNoAmmoFeedbackMs: Long = 0L

    fun firePlayerWeapon(explicitDirX: Float? = null, explicitDirY: Float? = null) {
        if (_sessionState.value != GameSessionState.PLAYING) return

        val isSpread = _activePowerUp.value == PowerUpType.SPEED_MAGNET
        val cost = if (isSpread) 3 else player.activeWeapon.ammoCost
        if (_score.value <= 0 || _score.value < cost) {
            val now = timeAccumulatorMs.toLong() + (_remainingTimeSeconds.value * 1000L)
            if (kotlin.math.abs(now - lastNoAmmoFeedbackMs) > 600L || lastNoAmmoFeedbackMs == 0L) {
                lastNoAmmoFeedbackMs = now
                addFloatingText(player.posX, player.posY - 15f, "NO AMMO!", Color(0xFFFF5252), durationMs = 600)
                soundPlayer.playSfx(SoundEffect.DEFLECT)
            }
            return
        }

        if (!player.canShoot()) return

        _score.value = (_score.value - cost).coerceAtLeast(0)

        var aimX = explicitDirX
        var aimY = explicitDirY

        // Smart Target Assist for keyboard & gamepad when no mouse cursor aim is active
        if (aimX == null && aimY == null && !isMouseAiming) {
            val target = findSmartTarget(player.posX, player.posY, player.headingX, player.headingY)
            if (target != null) {
                val dx = target.first - player.posX
                val dy = target.second - player.posY
                val len = sqrt(dx * dx + dy * dy).coerceAtLeast(0.001f)
                aimX = dx / len
                aimY = dy / len
            }
        }

        val newBullets = player.fireWeapon(isSpreadShot = isSpread, overrideDirX = aimX, overrideDirY = aimY)
        if (newBullets.isNotEmpty()) {
            _hasPlayerFiredFirstBullet.value = true
        }
        projectiles.addAll(newBullets)
        runCatching { actorManager.add(newBullets) }

        // Procedural Audio SFX according to weapon
        when (player.activeWeapon) {
            WeaponType.HEAVY_RAILGUN -> {
                soundPlayer.playSfx(SoundEffect.RAILGUN_BLAST)
                addScreenShake(4.5f)
            }
            WeaponType.PLASMA_FLAK_CANNON -> {
                soundPlayer.playSfx(SoundEffect.FLAK_BURST)
                addScreenShake(3.5f)
            }
            WeaponType.NOVA_360_BLASTER -> {
                soundPlayer.playSfx(SoundEffect.FLAK_BURST)
                addScreenShake(6f)
            }
            else -> {
                soundPlayer.playSfx(SoundEffect.LASER_PEW)
            }
        }
    }

    private fun findSmartTarget(px: Float, py: Float, hx: Float, hy: Float): Pair<Float, Float>? {
        val hlen = sqrt(hx * hx + hy * hy).coerceAtLeast(0.001f)
        val nhx = hx / hlen
        val nhy = hy / hlen

        // Priority 1: Boss if active and within 700 units
        boss?.let { b ->
            if (!b.isDefeated) {
                val d = distance(px, py, b.posX, b.posY)
                if (d < 700f) return Pair(b.posX, b.posY)
            }
        }

        // Priority 2: Memory Leak Bugs
        val memoryLeakBug = bugs.filter { it.isMemoryLeak }.minByOrNull { distance(px, py, it.posX, it.posY) }
        if (memoryLeakBug != null && distance(px, py, memoryLeakBug.posX, memoryLeakBug.posY) < 550f) {
            val dx = memoryLeakBug.posX - px
            val dy = memoryLeakBug.posY - py
            val d = distance(px, py, memoryLeakBug.posX, memoryLeakBug.posY)
            val dot = (nhx * dx + nhy * dy) / d
            if (dot > 0.35f || d < 220f) {
                return Pair(memoryLeakBug.posX, memoryLeakBug.posY)
            }
        }

        // Priority 3: Normal / Swarmer / Phantom Bugs
        val closestBug = bugs.minByOrNull { distance(px, py, it.posX, it.posY) }
        if (closestBug != null && distance(px, py, closestBug.posX, closestBug.posY) < 500f) {
            val dx = closestBug.posX - px
            val dy = closestBug.posY - py
            val d = distance(px, py, closestBug.posX, closestBug.posY)
            val dot = (nhx * dx + nhy * dy) / d
            if (dot > 0.30f || d < 200f) {
                return Pair(closestBug.posX, closestBug.posY)
            }
        }

        // Priority 4: Crates
        val closestCrate = crates.filter { !it.isDestroyed }.minByOrNull { distance(px, py, it.posX, it.posY) }
        if (closestCrate != null && distance(px, py, closestCrate.posX, closestCrate.posY) < 350f) {
            val dx = closestCrate.posX - px
            val dy = closestCrate.posY - py
            val d = distance(px, py, closestCrate.posX, closestCrate.posY)
            val dot = (nhx * dx + nhy * dy) / d
            if (dot > 0.40f) {
                return Pair(closestCrate.posX, closestCrate.posY)
            }
        }

        return null
    }

    var isBriefingPausedFromGame: Boolean = false
        private set

    private val _briefingSlideIndex = MutableStateFlow(0)
    val briefingSlideIndex: StateFlow<Int> = _briefingSlideIndex.asStateFlow()

    fun nextBriefingSlide() {
        if (_briefingSlideIndex.value < 4) {
            _briefingSlideIndex.value++
        } else {
            dismissBriefing()
        }
    }

    fun prevBriefingSlide() {
        if (_briefingSlideIndex.value > 0) {
            _briefingSlideIndex.value--
        }
    }

    fun setBriefingSlide(slide: Int) {
        _briefingSlideIndex.value = slide.coerceIn(0, 4)
    }

    fun startStartupMode() {
        soundPlayer.stopBgm()
        soundPlayer.startTitleTheme()
        isBriefingPausedFromGame = false
        isBriefingFromStartupFlow = true
        _isStartupLeaderboardVisible.value = false
        _startupMenuIndex.value = 0
        _sessionState.value = GameSessionState.STARTUP
        clearArena()
        spawnInitialEntities(isStartup = true)
    }

    fun startAttractMode() {
        soundPlayer.stopBgm()
        soundPlayer.startTitleTheme()
        isBriefingPausedFromGame = false
        isBriefingFromStartupFlow = false
        _isStartupLeaderboardVisible.value = false
        _startupMenuIndex.value = 0
        _sessionState.value = GameSessionState.ATTRACT
        clearArena()
        spawnInitialEntities(isStartup = true)
    }

    fun startNameEntry() {
        soundPlayer.stopTitleTheme()
        _sessionState.value = GameSessionState.NAME_ENTRY
    }

    fun startCountdown() {
        soundPlayer.stopTitleTheme()
        _sessionState.value = GameSessionState.COUNTDOWN
    }

    fun startBriefing(isPausedFromPlaying: Boolean = false, isFromStartup: Boolean = false) {
        soundPlayer.stopTitleTheme()
        isBriefingPausedFromGame = isPausedFromPlaying
        isBriefingFromStartupFlow = isFromStartup || (!isPausedFromPlaying && (_sessionState.value == GameSessionState.STARTUP || isBriefingFromStartupFlow))
        _briefingSlideIndex.value = 0
        setDirectMove(0f, 0f)
        if (isPausedFromPlaying) {
            player.isPaused = true
            player.setDirectMove(0f, 0f)
            bugs.forEach { it.isPaused = true }
            projectiles.forEach { it.isFrozen = true }
            soundPlayer.pauseBgm()
        }
        _sessionState.value = GameSessionState.BRIEFING
    }

    fun dismissBriefing() {
        if (isBriefingPausedFromGame) {
            isBriefingPausedFromGame = false
            _sessionState.value = GameSessionState.PLAYING
            player.isPaused = false
            bugs.forEach { it.isPaused = false }
            projectiles.forEach { it.isFrozen = false }
            soundPlayer.resumeBgm()
        } else if (isBriefingFromStartupFlow) {
            isBriefingFromStartupFlow = false
            _sessionState.value = GameSessionState.NAME_ENTRY
        } else {
            startNewGame()
        }
    }

    fun startNewGame() {
        isBriefingPausedFromGame = false
        _hasPlayerFiredFirstBullet.value = false
        perlinNoise = PerlinNoise(seed = Random.nextLong())
        _remainingTimeSeconds.value = 120
        _score.value = 100
        _multiplier.value = 1
        _streakGems.value = 0
        _gemsCollectedTotal.value = 0
        _bugsSquashedTotal.value = 0
        _triviaCorrectCount.value = 0
        _triviaAttemptedCount.value = 0
        _activePowerUp.value = null
        _powerUpRemainingSeconds.value = 0
        _bossHp.value = null
        _bossWarningSiren.value = false
        _bossKernelPanic.value = false
        _currentTriviaQuestion.value = null
        _triviaFeedback.value = null
        _killstreak.value = 0
        _killstreakBanner.value = null
        killstreakBannerTimerMs = 0
        _unlockedPerks.value = emptySet()
        _perkSelectionOptions.value = null
        hasSpawnedBoss = false
        bossSirenTimerMs = 0
        bossTelegraphMs = 0
        shakeIntensity = 0f

        cameraAngleIndex = 0
        targetCameraAngleRad = 0f
        currentCameraAngleRad = 0f
        smoothCameraX = 1900f
        smoothCameraY = 1900f
        _zoomMultiplier.value = DEFAULT_ZOOM_MULTIPLIER
        targetZoomMultiplier = DEFAULT_ZOOM_MULTIPLIER
        smoothZoom = 0.52f * DEFAULT_ZOOM_MULTIPLIER

        currentFloor.value = 1
        bugsSquashedThisFloor.value = 0
        val floor1Theme = getFloorTheme(1)
        bugsTargetForElevator.value = 15
        isElevatorOnline.value = false
        floorAscensionTitle.value = "FLOOR 1: ${floor1Theme.title}"
        floorAscensionSubtitle.value = floor1Theme.subtitle
        floorAscensionSplashTimerMs.value = 2500

        timeAccumulatorMs = 0
        powerUpAccumulatorMs = 0
        gemRespawnTimerMs = 0
        bugRespawnTimerMs = 0

        clearArena()
        spawnInitialEntities()

        player.isEntranceShieldActive = true
        player.entranceShieldTimerMs = 5000
        player.hasAmmo = {
            val cost = if (_activePowerUp.value == PowerUpType.SPEED_MAGNET) 3 else player.activeWeapon.ammoCost
            _score.value >= cost
        }
        addFloatingText(player.posX, player.posY, "🛡️ ENTRANCE SHIELD (5s)", Color(0xFF00E5FF), durationMs = 2000)

        _sessionState.value = GameSessionState.PLAYING
        soundPlayer.startBgm()
    }

    fun ascendToNextFloor() {
        val nextFloor = currentFloor.value + 1
        currentFloor.value = nextFloor
        bugsSquashedThisFloor.value = 0
        isElevatorOnline.value = false
        elevatorPad?.isActive = false

        val theme = getFloorTheme(nextFloor)
        bugsTargetForElevator.value = theme.bugTarget
        floorAscensionTitle.value = "⚡ SYSTEM OVERCLOCK: TIER UPGRADE! (SECTOR $nextFloor)"
        floorAscensionSubtitle.value = "${theme.title} • ${theme.subtitle}"
        floorAscensionSplashTimerMs.value = 3500

        val bonus = 2500 * _multiplier.value
        _score.value += bonus
        _multiplier.value = (_multiplier.value + 1).coerceAtMost(5)
        soundPlayer.playSfx(SoundEffect.UPGRADE_UNLOCKED)
        addScreenShake(14f)
        addFloatingText(player.posX, player.posY, "⚡ SYSTEM OVERCLOCK! +$bonus PTS", Color(0xFFFFD600))
        _triviaFeedback.value = "⚡ SYSTEM OVERCLOCK! +$bonus PTS • SECTOR $nextFloor: ${theme.title}!"

        spawnGems(25)
        spawnBugs(8)

        if (nextFloor >= 4 && !hasSpawnedBoss && _remainingTimeSeconds.value <= 60) {
            triggerBossEncounter()
        }
    }

    fun triggerBossEncounter(force: Boolean = false) {
        if (hasSpawnedBoss) return
        if (!force && _remainingTimeSeconds.value > 60) return
        hasSpawnedBoss = true
        val bossX = 1900f
        val bossY = 350f
        val newBoss = BossVoxelActor(bossX, bossY, posZ = 0f, maxHp = 35)
        boss = newBoss
        runCatching { actorManager.add(newBoss) }
        _bossHp.value = Pair(newBoss.hp, newBoss.maxHp)
        _bossWarningSiren.value = true
        bossSirenTimerMs = 5500
        bossTelegraphMs = 2500
        soundPlayer.playSfx(SoundEffect.BOSS_SPAWN_SIREN)
        addScreenShake(12f)
        _triviaFeedback.value = "🚨 TITAN ALERT: OutOfMemoryError DETECTED IN SECTOR! PREPARE DEFENSES!"
        addFloatingText(bossX, bossY, "TITAN ARRIVAL!", Color(0xFFFF1744), durationMs = 2000)
    }

    fun pauseGame() {
        if (_sessionState.value == GameSessionState.PLAYING) {
            _sessionState.value = GameSessionState.PAUSED
            player.isPaused = true
            player.setDirectMove(0f, 0f)
            bugs.forEach { it.isPaused = true }
            projectiles.forEach { it.isFrozen = true }
            soundPlayer.pauseBgm()
        }
    }

    fun resumeGame() {
        if (_sessionState.value == GameSessionState.PAUSED) {
            _sessionState.value = GameSessionState.PLAYING
            player.isPaused = false
            bugs.forEach { it.isPaused = false }
            projectiles.forEach { it.isFrozen = false }
            soundPlayer.resumeBgm()
        }
    }

    fun quitToStartup() {
        clearArena()
        startStartupMode()
    }

    public override fun onUpdate(deltaTimeInMilliseconds: Int) {
        arenaPulseTime += deltaTimeInMilliseconds * 0.001f
        when (_sessionState.value) {
            GameSessionState.STARTUP -> {}
            GameSessionState.NAME_ENTRY -> {}
            GameSessionState.COUNTDOWN -> {}
            GameSessionState.ATTRACT -> {}
            GameSessionState.BRIEFING -> {}
            GameSessionState.PLAYING,
            GameSessionState.TRIVIA_MODAL -> {
                updatePlayingSession(deltaTimeInMilliseconds)
            }
            GameSessionState.PAUSED -> {}
            GameSessionState.ROUND_OVER -> {}
        }
    }

    fun update(deltaTimeInMilliseconds: Int) = onUpdate(deltaTimeInMilliseconds)

    private fun updatePlayingSession(deltaTimeMs: Int) {
        // Dynamic Camera Angle Interpolation
        val angleDiff = targetCameraAngleRad - currentCameraAngleRad
        if (kotlin.math.abs(angleDiff) > 0.001f) {
            currentCameraAngleRad += angleDiff * (0.008f * deltaTimeMs).coerceIn(0.01f, 0.25f)
        } else {
            currentCameraAngleRad = targetCameraAngleRad
        }
        val twoPi = 2f * kotlin.math.PI.toFloat()
        if (currentCameraAngleRad >= twoPi && targetCameraAngleRad >= twoPi) {
            currentCameraAngleRad -= twoPi
            targetCameraAngleRad -= twoPi
        } else if (currentCameraAngleRad <= -twoPi && targetCameraAngleRad <= -twoPi) {
            currentCameraAngleRad += twoPi
            targetCameraAngleRad += twoPi
        }
        player.cameraAngleRad = currentCameraAngleRad

        // Check player proximity to elevator extraction pad (backward compatibility)
        elevatorPad?.checkPlayerProximity(player.posX, player.posY)

        // 1. Overclock Power Conduits
        for (conduit in overclockConduits) {
            if (conduit.checkActivation(player.posX, player.posY)) {
                activatePowerUp(conduit.powerUpType, durationSeconds = 6)
                soundPlayer.playSfx(SoundEffect.UPGRADE_UNLOCKED)
                addScreenShake(6f)
                spawnDebris(conduit.posX, conduit.posY, 10f, Color(0xFFFF007F), count = 16, speed = 0.4f)
                val buffLabel = when (conduit.powerUpType) {
                    PowerUpType.SPEED_MAGNET -> "HYPER-SPEED"
                    PowerUpType.FREEZE_SHIELD -> "FREEZE-SHIELD"
                    PowerUpType.MEGA_FRENZY_NUKE -> "FRENZY-NUKE"
                    PowerUpType.OVERCHARGE_INVINCIBLE -> "OVERCHARGE"
                }
                addFloatingText(player.posX, player.posY, "⚡ OVERCLOCK: $buffLabel!", Color(0xFFFF007F))
                _triviaFeedback.value = "⚡ OVERCLOCK CONDUIT ENGAGED: $buffLabel (6s)!"
            }
        }

        // 3. Dynamic Laser Sweep Fences
        for (fence in laserFences) {
            if (fence.isLaserActive) {
                val bugsCrossing = bugs.filter { it.hp > 0 && fence.isPointNearBeam(it.posX, it.posY, 20f) }
                bugsCrossing.forEach { bug ->
                    bug.hp = 0
                    _bugsSquashedTotal.value += 1
                    bugsSquashedThisFloor.value += 1

                    bountyBeacons.filter { it.isActive }.forEach { beacon ->
                        if (beacon.recordKill()) {
                            val bountyPts = 5000 * _multiplier.value
                            _score.value += bountyPts
                            _multiplier.value = (_multiplier.value + 1).coerceAtMost(5)
                            soundPlayer.playSfx(SoundEffect.UPGRADE_UNLOCKED)
                            addScreenShake(15f)
                            addFloatingText(player.posX, player.posY, "🎯 BOUNTY CLAIMED! +$bountyPts PTS", Color(0xFFFFD600))
                            _triviaFeedback.value = "🏆 BOUNTY COMPLETE! +$bountyPts PTS • MULTIPLIER UP!"
                        }
                    }

                    _score.value += 150 * _multiplier.value
                    _streakGems.value += 1
                    soundPlayer.playSfx(SoundEffect.BUG_POP)
                    addScreenShake(4f)
                    spawnDebris(bug.posX, bug.posY, 12f, Color(0xFFFF1744), count = 12, speed = 0.4f)
                    addFloatingText(bug.posX, bug.posY, "⚡ VAPORIZED!", Color(0xFFFF1744))
                    val gem = GemVoxelActor(bug.posX, bug.posY, pointValue = 100)
                    gems.add(gem)
                    runCatching { actorManager.add(gem) }

                    if (bugsSquashedThisFloor.value >= bugsTargetForElevator.value) {
                        ascendToNextFloor()
                    }
                }
                if (bugsCrossing.isNotEmpty()) {
                    bugs.removeAll(bugsCrossing)
                    runCatching { actorManager.remove(bugsCrossing) }
                }
            }
        }

        // 4. District Bounty Beacons
        for (beacon in bountyBeacons) {
            if (beacon.checkInteraction(player.posX, player.posY)) {
                soundPlayer.playSfx(SoundEffect.UPGRADE_UNLOCKED)
                addScreenShake(8f)
                spawnDebris(beacon.posX, beacon.posY, 14f, Color(0xFFFFD600), count = 20, speed = 0.5f)
                addFloatingText(beacon.posX, beacon.posY, "🎯 BOUNTY: ${beacon.districtName}!", Color(0xFFFFD600))
                _triviaFeedback.value = "🎯 BOUNTY STARTED: PURGE ${beacon.targetKills} BUGS IN 15s!"
                spawnBugs(6)
            }
        }

        // 5. Waypoint Navigation Telemetry (Priority 1: Active Bounty, Priority 2: Titan Boss, Priority 3: Nearest Bug)
        val activeBeacon = bountyBeacons.firstOrNull { it.isActive }
        if (activeBeacon != null) {
            val d = distance(player.posX, player.posY, activeBeacon.posX, activeBeacon.posY)
            activeBountyDistance.value = d
            activeBountyAngleRad.value = calculateScreenAngleTo(activeBeacon.posX, activeBeacon.posY)
            activeBountyRemainingKills.value = (activeBeacon.targetKills - activeBeacon.currentKills).coerceAtLeast(0)
            activeBountyTimeSeconds.value = (activeBeacon.remainingTimeMs / 1000).coerceAtLeast(0)
        } else {
            activeBountyDistance.value = null
            activeBountyAngleRad.value = null
            activeBountyRemainingKills.value = null
            activeBountyTimeSeconds.value = null
        }

        val activeBoss = boss
        if (activeBoss != null && !activeBoss.isDefeated) {
            val d = distance(player.posX, player.posY, activeBoss.posX, activeBoss.posY)
            bossDistance.value = d
            bossAngleRad.value = calculateScreenAngleTo(activeBoss.posX, activeBoss.posY)
        } else {
            bossDistance.value = null
            bossAngleRad.value = null
        }

        val livingBugs = bugs.filter { it.hp > 0 }
        if (livingBugs.isNotEmpty()) {
            val nearest = livingBugs.minByOrNull { distance(player.posX, player.posY, it.posX, it.posY) }
            if (nearest != null) {
                val d = distance(player.posX, player.posY, nearest.posX, nearest.posY)
                nearestBugDistance.value = d
                nearestBugAngleRad.value = calculateScreenAngleTo(nearest.posX, nearest.posY)
            }
        } else {
            nearestBugDistance.value = null
            nearestBugAngleRad.value = null
        }

        // Waypoint to elevator pad, active bounty beacon, or active boss
        if (elevatorPad != null && elevatorPad?.isActive == true) {
            elevatorPad?.let { pad ->
                val d = distance(player.posX, player.posY, pad.posX, pad.posY)
                elevatorDistance.value = d
                elevatorAngleRad.value = calculateScreenAngleTo(pad.posX, pad.posY)
            }
        } else if (activeBeacon != null) {
            elevatorDistance.value = activeBountyDistance.value
            elevatorAngleRad.value = activeBountyAngleRad.value
        } else if (activeBoss != null && !activeBoss.isDefeated) {
            elevatorDistance.value = bossDistance.value
            elevatorAngleRad.value = bossAngleRad.value
        } else {
            elevatorDistance.value = null
            elevatorAngleRad.value = null
        }

        // Decrement floor ascension splash timer
        if (floorAscensionSplashTimerMs.value > 0) {
            floorAscensionSplashTimerMs.value = (floorAscensionSplashTimerMs.value - deltaTimeMs).coerceAtLeast(0)
        }

        // Camera Velocity & Heading Lookahead
        val targetCamX = player.posX + player.headingX * 60f
        val targetCamY = player.posY + player.headingY * 60f
        val camLerp = (0.006f * deltaTimeMs).coerceIn(0.01f, 0.25f)
        smoothCameraX += (targetCamX - smoothCameraX) * camLerp
        smoothCameraY += (targetCamY - smoothCameraY) * camLerp

        // Dynamic Combat Zoom combined with User-Controlled Unbounded Zoom
        val currentZM = _zoomMultiplier.value
        val newZM = currentZM + (targetZoomMultiplier - currentZM) * (0.015f * deltaTimeMs).coerceIn(0.02f, 0.4f)
        _zoomMultiplier.value = newZM

        val combatBaseZoom = when {
            boss?.isKernelPanic == true -> 0.45f
            boss != null && !boss!!.isDefeated -> 0.48f
            player.isDashing || player.isSpeedBoostActive -> 0.47f
            else -> 0.52f
        }
        val targetZoom = combatBaseZoom * newZM
        val zoomLerp = (0.01f * deltaTimeMs).coerceIn(0.02f, 0.3f)
        smoothZoom += (targetZoom - smoothZoom) * zoomLerp

        // 1. Session Timer (120s)
        timeAccumulatorMs += deltaTimeMs
        if (timeAccumulatorMs >= 1000) {
            timeAccumulatorMs -= 1000
            val newTime = _remainingTimeSeconds.value - 1
            _remainingTimeSeconds.value = newTime
            if (newTime <= 0) {
                endRound()
                return
            }
        }

        // Trivia Lockout countdown in real-time
        if (_sessionState.value == GameSessionState.TRIVIA_MODAL && _triviaLockoutRemainingMs.value > 0) {
            _triviaLockoutRemainingMs.value = (_triviaLockoutRemainingMs.value - deltaTimeMs).coerceAtLeast(0)
        }

        // 2. Screen Shake Decay & Cloud Animation
        cloudAnimTime += deltaTimeMs * 0.0004f
        if (shakeIntensity > 0f) {
            shakeIntensity = (shakeIntensity - deltaTimeMs * 0.028f).coerceAtLeast(0f)
        }

        // 3. Killstreak Banner Decay
        if (killstreakBannerTimerMs > 0) {
            killstreakBannerTimerMs -= deltaTimeMs
            if (killstreakBannerTimerMs <= 0) {
                _killstreakBanner.value = null
            }
        }

        // 4. Update Floating Combat Texts & Debris Particles
        synchronized(floatingTexts) {
            val deadTexts = mutableListOf<FloatingCombatText>()
            for (ft in floatingTexts) {
                ft.lifeMs -= deltaTimeMs
                if (ft.lifeMs <= 0) {
                    deadTexts.add(ft)
                } else {
                    ft.worldZ += 0.038f * deltaTimeMs
                }
            }
            floatingTexts.removeAll(deadTexts)
        }

        synchronized(debrisParticles) {
            val deadParticles = mutableListOf<DebrisParticle>()
            for (p in debrisParticles) {
                p.lifeMs -= deltaTimeMs
                if (p.lifeMs <= 0) {
                    deadParticles.add(p)
                    continue
                }
                p.x += p.vx * deltaTimeMs
                p.y += p.vy * deltaTimeMs
                p.z += p.vz * deltaTimeMs
                p.vz -= 0.0009f * deltaTimeMs // Gravity
                if (p.z < 0f) {
                    p.z = 0f
                    p.vz = -p.vz * 0.45f // Bounce
                    p.vx *= 0.72f
                    p.vy *= 0.72f
                }
            }
            debrisParticles.removeAll(deadParticles)
        }

        // 5. Dash Cooldown Gauge & Reactive Shader drivers
        _dashCooldownFraction.value = (player.dashCooldownMs.toFloat() / player.maxDashCooldownMs.toFloat()).coerceIn(0f, 1f)
        _activeWeapon.value = player.activeWeapon
        _isFirewallShieldActive.value = player.isFirewallShieldActive
        _firewallRechargeTimerMs.value = player.firewallRechargeTimerMs
        onUpdateShader?.invoke(deltaTimeMs)

        // 6. Boss Warning Siren Timer
        if (bossSirenTimerMs > 0) {
            bossSirenTimerMs -= deltaTimeMs
            if (bossSirenTimerMs <= 0) {
                _bossWarningSiren.value = false
            }
        }

        // 7. Power-Up Duration Timer
        if (_activePowerUp.value != null) {
            powerUpAccumulatorMs += deltaTimeMs
            if (powerUpAccumulatorMs >= 1000) {
                powerUpAccumulatorMs -= 1000
                val remaining = _powerUpRemainingSeconds.value - 1
                _powerUpRemainingSeconds.value = remaining
                if (remaining <= 0) {
                    deactivatePowerUp()
                }
            }
        }

        // 8. Companion Drone Update & Auto-Combat
        drone?.let { d ->
            val droneShots = d.updateOrbitAndCombat(player.posX, player.posY, deltaTimeMs, bugs)
            if (droneShots.isNotEmpty()) {
                projectiles.addAll(droneShots)
                runCatching { actorManager.add(droneShots) }
                soundPlayer.playSfx(SoundEffect.LASER_PEW)
            }

            // Suction gems towards drone
            gems.forEach { gem ->
                val dist = distance(d.posX, d.posY, gem.posX, gem.posY)
                if (dist < d.suctionRadius) {
                    gem.pullTowards(player.posX, player.posY, 0.65f * deltaTimeMs)
                }
            }
        }

        // 9. Automated Turrets Update
        val expiredTurrets = mutableListOf<TurretVoxelActor>()
        turrets.forEach { t ->
            val turretShots = t.updateAndCombat(deltaTimeMs, bugs)
            if (turretShots.isNotEmpty()) {
                projectiles.addAll(turretShots)
                runCatching { actorManager.add(turretShots) }
                soundPlayer.playSfx(SoundEffect.LASER_PEW)
            }
            if (t.isExpired) {
                expiredTurrets.add(t)
            }
        }
        if (expiredTurrets.isNotEmpty()) {
            turrets.removeAll(expiredTurrets)
            runCatching { actorManager.remove(expiredTurrets) }
        }

        // 10. Boss Wave Trigger at remainingTime <= 60s
        if (!hasSpawnedBoss && _remainingTimeSeconds.value <= 60) {
            triggerBossEncounter()
        }

        // 11. Update Boss AI & Phase 2 Kernel Panic Mode
        if (bossTelegraphMs > 0) {
            bossTelegraphMs -= deltaTimeMs
        } else {
            boss?.let { b ->
                if (!b.isDefeated) {
                    val bossProjectiles = b.updateTowards(player.posX, player.posY, deltaTimeMs)
                    if (bossProjectiles.isNotEmpty()) {
                        projectiles.addAll(bossProjectiles)
                        actorManager.add(bossProjectiles)
                    }

                    // Phase 2 Minion Swarm Trigger
                    if (b.isEnraged && !b.hasTriggeredPhase2Minions) {
                        b.hasTriggeredPhase2Minions = true
                        _triviaFeedback.value = "⚡ TITAN ENRAGED! Crimson Phase & Minion Swarm!"
                        soundPlayer.playSfx(SoundEffect.BOSS_SPAWN_SIREN)
                        addScreenShake(10f)
                        for (i in 0 until 4) {
                            spawnSingleBug(type = BugType.MEMORY_LEAK)
                        }
                    }

                    // Phase 2 "Kernel Panic" Trigger (HP <= 40%)
                    if (b.isKernelPanic && !b.hasTriggeredKernelPanic) {
                        b.hasTriggeredKernelPanic = true
                        _bossKernelPanic.value = true
                        soundPlayer.playSfx(SoundEffect.KERNEL_PANIC)
                        addScreenShake(18f)
                        _triviaFeedback.value = "🚨 KERNEL PANIC DETECTED! TITAN ENTERING 5-LASER SWEEP OVERDRIVE! 🚨"
                        addFloatingText(b.posX, b.posY, "KERNEL PANIC OVERDRIVE!", Color(0xFFFF1744), durationMs = 1600)
                        spawnDebris(b.posX, b.posY, 40f, Color(0xFFFF1744), count = 24, speed = 0.45f)
                        triggerWorldShockwave(b.posX, b.posY, intensity = 1.0f)
                        onTriggerGlitch?.invoke(850, 1.0f)
                        onTriggerCrimsonAlert?.invoke(1200, 1.0f)
                        onTriggerCombatImpact?.invoke(1.0f)
                    }

                    // Boss vs Player contact (with i-frames & knockback)
                    if (!player.isInvulnerable && distance(player.posX, player.posY, b.posX, b.posY) < player.collisionRadius + b.collisionRadius) {
                        val kx = player.posX - b.posX
                        val ky = player.posY - b.posY
                        val klen = sqrt(kx * kx + ky * ky).coerceAtLeast(0.001f)
                        onPlayerHit(kx / klen, ky / klen, isBossHit = true)
                    }
                }
            }
        }

        // 12. Environmental Collisions (Server Racks, Trees, Crates, Relay Towers, Barricades)
        serverRacks.forEach { rack ->
            player.pushOutOf(rack.posX, rack.posY, player.collisionRadius + rack.collisionRadius)
            bugs.forEach { bug -> bug.pushOutOf(rack.posX, rack.posY, bug.collisionRadius + rack.collisionRadius) }
        }

        trees.forEach { tree ->
            player.pushOutOf(tree.posX, tree.posY, player.collisionRadius + tree.collisionRadius)
            bugs.forEach { bug -> bug.pushOutOf(tree.posX, tree.posY, bug.collisionRadius + tree.collisionRadius) }
        }

        crates.forEach { crate ->
            if (!crate.isDestroyed) {
                player.pushOutOf(crate.posX, crate.posY, player.collisionRadius + crate.collisionRadius)
                bugs.forEach { bug -> bug.pushOutOf(crate.posX, crate.posY, bug.collisionRadius + crate.collisionRadius) }
            }
        }

        dataRelayTowers.forEach { tower ->
            player.pushOutOf(tower.posX, tower.posY, player.collisionRadius + tower.collisionRadius)
            bugs.forEach { bug -> bug.pushOutOf(tower.posX, tower.posY, bug.collisionRadius + tower.collisionRadius) }
        }

        techBarricades.forEach { barricade ->
            player.pushOutOf(barricade.posX, barricade.posY, player.collisionRadius + barricade.collisionRadius)
            bugs.forEach { bug -> bug.pushOutOf(barricade.posX, barricade.posY, bug.collisionRadius + barricade.collisionRadius) }
        }

        explosiveBarrels.forEach { barrel ->
            if (!barrel.isDestroyed) {
                player.pushOutOf(barrel.posX, barrel.posY, player.collisionRadius + barrel.collisionRadius)
                bugs.forEach { bug -> bug.pushOutOf(barrel.posX, barrel.posY, bug.collisionRadius + barrel.collisionRadius) }
            }
        }

        bountyBeacons.forEach { beacon ->
            player.pushOutOf(beacon.posX, beacon.posY, player.collisionRadius + beacon.collisionRadius)
            bugs.forEach { bug -> bug.pushOutOf(beacon.posX, beacon.posY, bug.collisionRadius + beacon.collisionRadius) }
        }

        // 13. Bug Swarm AI Update: Only red bugs (BugType.NORMAL) and only bugs currently in view steer toward player
        updateBugSwarmAI(deltaTimeMs)

        // 14. Progressive Wave Escalation & High-Density Bug Spawning
        bugRespawnTimerMs += deltaTimeMs
        val isLateWave = _remainingTimeSeconds.value <= 60
        val maxBugs = if (isLateWave) 55 else 42
        val respawnInterval = if (isLateWave) 900 else 1500

        if (bugRespawnTimerMs >= respawnInterval && bugs.size < maxBugs) {
            bugRespawnTimerMs = 0
            val spawnType = selectProgressiveBugType()
            spawnSingleBug(type = spawnType)
            spawnSingleBug(type = selectProgressiveBugType())
            // Swarmers spawn in packs of 3
            if (spawnType == BugType.SWARMER) {
                spawnSingleBug(type = BugType.SWARMER)
            }
        }

        // 14. Gem Respawns & Magnet
        gemRespawnTimerMs += deltaTimeMs
        if (gemRespawnTimerMs >= 3200 && gems.size < 40) {
            gemRespawnTimerMs = 0
            spawnGems(6)
        }
        val isMagnetActive = _activePowerUp.value == PowerUpType.SPEED_MAGNET
        if (isMagnetActive) {
            val magnetRadius = 380f
            gems.forEach { gem ->
                val dist = distance(player.posX, player.posY, gem.posX, gem.posY)
                if (dist < magnetRadius) {
                    gem.pullTowards(player.posX, player.posY, 0.55f * deltaTimeMs)
                }
            }
        }

        // 15. Gem Pickups
        val pickupRadius = 38f
        val collectedGems = mutableListOf<GemVoxelActor>()
        gems.forEach { gem ->
            if (distance(player.posX, player.posY, gem.posX, gem.posY) < pickupRadius) {
                collectedGems.add(gem)
                onGemCollected(gem)
            }
        }
        if (collectedGems.isNotEmpty()) {
            gems.removeAll(collectedGems)
            runCatching { actorManager.remove(collectedGems) }
        }

        // 16. Trivia Shard Pickups (Triggers Question Modal!)
        val collectedShards = mutableListOf<TriviaShardVoxelActor>()
        triviaShards.forEach { shard ->
            if (!shard.isCollected && distance(player.posX, player.posY, shard.posX, shard.posY) < 38f) {
                shard.isCollected = true
                collectedShards.add(shard)
                triggerShardTrivia(shard.tier)
            }
        }
        if (collectedShards.isNotEmpty()) {
            triviaShards.removeAll(collectedShards)
            runCatching { actorManager.remove(collectedShards) }
        }

        // 16b. Gun Drop Pickups (Immediately equips weapon archetype!)
        val collectedDrops = mutableListOf<GunDropVoxelActor>()
        gunDrops.forEach { drop ->
            if (!drop.isCollected && distance(player.posX, player.posY, drop.posX, drop.posY) < (player.collisionRadius + drop.collisionRadius)) {
                drop.isCollected = true
                collectedDrops.add(drop)
                player.activeWeapon = drop.weaponType
                _activeWeapon.value = drop.weaponType
                soundPlayer.playSfx(SoundEffect.UPGRADE_UNLOCKED)
                addScreenShake(3.5f)
                addFloatingText(drop.posX, drop.posY, "+ ${drop.weaponType.displayName.uppercase()} EQUIPPED!", Color(0xFF00E5FF), durationMs = 1500)
                _triviaFeedback.value = "🔫 WEAPON EQUIPPED: ${drop.weaponType.displayName}!"
            }
        }
        if (collectedDrops.isNotEmpty()) {
            gunDrops.removeAll(collectedDrops)
            runCatching { actorManager.remove(collectedDrops) }
        }

        // 17. Bug vs Player Collisions
        bugs.forEach { bug ->
            val dist = distance(player.posX, player.posY, bug.posX, bug.posY)
            val minDist = player.collisionRadius + bug.collisionRadius
            if (dist < minDist) {
                player.pushOutOf(bug.posX, bug.posY, minDist)
                bug.pushOutOf(player.posX, player.posY, minDist)
                if (!player.isInvulnerable && !player.isStunned && _activePowerUp.value != PowerUpType.FREEZE_SHIELD && !bug.isFrozen) {
                    val kx = player.posX - bug.posX
                    val ky = player.posY - bug.posY
                    val klen = sqrt(kx * kx + ky * ky).coerceAtLeast(0.001f)
                    onPlayerHit(kx / klen, ky / klen, isBossHit = false)
                }
            }
        }

        // 18. Projectile Collisions (Bullets vs Bugs, Crates, Boss, Racks & Trees)
        val bulletsToRemove = mutableListOf<ProjectileVoxelActor>()
        val bugsToKill = mutableListOf<BugHazardActor>()
        val cratesToDestroy = mutableListOf<CrateVoxelActor>()

        projectiles.forEach { bullet ->
            if (bullet.isDestroyed) {
                bulletsToRemove.add(bullet)
                return@forEach
            }

            // Despawn off-screen player bullets
            if (bullet.isPlayerProjectile && !isInView(bullet.posX, bullet.posY, margin = 160f)) {
                bullet.isDestroyed = true
                bulletsToRemove.add(bullet)
                return@forEach
            }

            // Bullet vs Server Racks & Trees
            for (rack in serverRacks) {
                if (distance(bullet.posX, bullet.posY, rack.posX, rack.posY) < rack.collisionRadius + 8f) {
                    bullet.isDestroyed = true
                    bulletsToRemove.add(bullet)
                    spawnDebris(bullet.posX, bullet.posY, 12f, Color(0xFF37474F), count = 3, size = 3f)
                    return@forEach
                }
            }
            for (tree in trees) {
                if (distance(bullet.posX, bullet.posY, tree.posX, tree.posY) < tree.collisionRadius + 8f) {
                    bullet.isDestroyed = true
                    bulletsToRemove.add(bullet)
                    spawnDebris(bullet.posX, bullet.posY, 12f, Color(0xFF00E676), count = 3, size = 3f)
                    return@forEach
                }
            }
            for (tower in dataRelayTowers) {
                if (distance(bullet.posX, bullet.posY, tower.posX, tower.posY) < tower.collisionRadius + 8f) {
                    bullet.isDestroyed = true
                    bulletsToRemove.add(bullet)
                    spawnDebris(bullet.posX, bullet.posY, 12f, Color(0xFF00E5FF), count = 3, size = 3f)
                    return@forEach
                }
            }
            for (barricade in techBarricades) {
                if (distance(bullet.posX, bullet.posY, barricade.posX, barricade.posY) < barricade.collisionRadius + 8f) {
                    bullet.isDestroyed = true
                    bulletsToRemove.add(bullet)
                    spawnDebris(bullet.posX, bullet.posY, 10f, Color(0xFFFFD600), count = 3, size = 3f)
                    return@forEach
                }
            }

            if (bullet.isPlayerProjectile) {
                // Bullet vs Explosive Barrel
                for (barrel in explosiveBarrels) {
                    if (!barrel.isDestroyed && distance(bullet.posX, bullet.posY, barrel.posX, barrel.posY) < barrel.collisionRadius + 10f) {
                        bullet.isDestroyed = true
                        bulletsToRemove.add(bullet)
                        if (barrel.takeDamage(1)) {
                            detonateBarrel(barrel)
                        } else {
                            soundPlayer.playSfx(SoundEffect.DEFLECT)
                            addScreenShake(3f)
                            spawnDebris(barrel.posX, barrel.posY, 8f, Color(0xFFFFD600), count = 6, speed = 0.3f)
                        }
                        return@forEach
                    }
                }

                // Bullet vs Bounty Beacon
                for (beacon in bountyBeacons) {
                    if (beacon.isReadyToTrigger && distance(bullet.posX, bullet.posY, beacon.posX, beacon.posY) < beacon.collisionRadius + 12f) {
                        bullet.isDestroyed = true
                        bulletsToRemove.add(bullet)
                        beacon.startBounty()
                        soundPlayer.playSfx(SoundEffect.UPGRADE_UNLOCKED)
                        addScreenShake(8f)
                        spawnDebris(beacon.posX, beacon.posY, 14f, Color(0xFFFFD600), count = 20, speed = 0.5f)
                        addFloatingText(beacon.posX, beacon.posY, "🎯 BOUNTY: ${beacon.districtName}!", Color(0xFFFFD600))
                        _triviaFeedback.value = "🎯 BOUNTY STARTED: PURGE ${beacon.targetKills} BUGS IN 15s!"
                        spawnBugs(6)
                        return@forEach
                    }
                }

                // Bullet vs Destructible Crate
                for (crate in crates) {
                    if (!crate.isDestroyed && isCrateInView(crate) && distance(bullet.posX, bullet.posY, crate.posX, crate.posY) < crate.collisionRadius + 10f) {
                        bullet.isDestroyed = true
                        bulletsToRemove.add(bullet)

                        val dist = distance(bullet.originX, bullet.originY, crate.posX, crate.posY)
                        val calcDmg = calculateRangeDamage(bullet.damage, dist, bullet.damage >= 2)
                        val destroyed = crate.takeDamage(calcDmg.damage)

                        if (destroyed) {
                            cratesToDestroy.add(crate)
                            spawnDebris(crate.posX, crate.posY, 14f, Color(0xFFFFB300), count = 12, speed = 0.3f)
                            addScreenShake(if (calcDmg.isBoosted) 3.5f else 2f)

                            val shatterText = if (calcDmg.isBoosted) "-${calcDmg.damage} HP BOOST!" else "-${calcDmg.damage} HP"
                            addFloatingText(crate.posX, crate.posY, shatterText, if (calcDmg.isBoosted) Color(0xFFFFD600) else Color(0xFFFFB300))

                            // Drop Trivia Shard or Gems
                            if (crate.containsTriviaShard) {
                                val shardTier = Random.nextInt(1, 4)
                                val shard = TriviaShardVoxelActor(crate.posX, crate.posY, tier = shardTier)
                                triviaShards.add(shard)
                                runCatching { actorManager.add(shard) }
                                _triviaFeedback.value = "📦 CRATE HACKED! Kotlin Byte Shard Dropped!"
                                addFloatingText(crate.posX, crate.posY - 12f, "BYTE SHARD!", Color(0xFF00E5FF))
                            } else {
                                for (i in 0 until 3) {
                                    val offsetDist = 20f + i * 10f
                                    val gem = GemVoxelActor(crate.posX + offsetDist, crate.posY + offsetDist, pointValue = 120)
                                    gems.add(gem)
                                    runCatching { actorManager.add(gem) }
                                }
                            }
                            // 50% chance to drop random weapon
                            if (Random.nextFloat() < 0.50f) {
                                spawnRandomGunDrop(crate.posX, crate.posY)
                            }
                        } else {
                            soundPlayer.playSfx(SoundEffect.DEFLECT)
                            addScreenShake(1.5f)
                            spawnDebris(crate.posX, crate.posY, 10f, Color(0xFF8D6E63), count = 6, speed = 0.25f, size = 3f)
                            addFloatingText(crate.posX, crate.posY, "-${calcDmg.damage} HP", Color(0xFFFFB300))
                        }
                        return@forEach
                    }
                }

                // Bullet vs Bug
                for (bug in bugs) {
                    if (bug.hp > 0 && isBugInView(bug) && distance(bullet.posX, bullet.posY, bug.posX, bug.posY) < bug.collisionRadius + 16f) {
                        bullet.isDestroyed = true
                        bulletsToRemove.add(bullet)

                        val dist = distance(bullet.originX, bullet.originY, bug.posX, bug.posY)
                        val calcDmg = calculateRangeDamage(bullet.damage, dist, bullet.damage >= 2)

                        val damageResult = bug.takeDamage(
                            amount = calcDmg.damage,
                            bulletDirX = bullet.dirX,
                            bulletDirY = bullet.dirY,
                            isHeavyRailgun = bullet.damage >= 2
                        )

                        when (damageResult) {
                            DamageResult.DEFLECTED -> {
                                soundPlayer.playSfx(SoundEffect.DEFLECT)
                                addScreenShake(2f)
                                addFloatingText(bug.posX, bug.posY, "DEFLECTED!", Color(0xFF00E5FF))
                                spawnDebris(bullet.posX, bullet.posY, 12f, Color(0xFF00E5FF), count = 5, size = 4f)
                            }
                            DamageResult.KILLED -> {
                                bugsToKill.add(bug)
                                _bugsSquashedTotal.value += 1
                                bugsSquashedThisFloor.value += 1

                                bountyBeacons.filter { it.isActive }.forEach { beacon ->
                                    if (beacon.recordKill()) {
                                        val bountyPts = 5000 * _multiplier.value
                                        _score.value += bountyPts
                                        _multiplier.value = (_multiplier.value + 1).coerceAtMost(5)
                                        soundPlayer.playSfx(SoundEffect.UPGRADE_UNLOCKED)
                                        addScreenShake(15f)
                                        addFloatingText(player.posX, player.posY, "🎯 BOUNTY CLAIMED! +$bountyPts PTS", Color(0xFFFFD600))
                                        _triviaFeedback.value = "🏆 BOUNTY COMPLETE! +$bountyPts PTS • MULTIPLIER UP!"
                                    }
                                }

                                if (bugsSquashedThisFloor.value >= bugsTargetForElevator.value) {
                                    ascendToNextFloor()
                                }
                                val points = when (bug.bugType) {
                                    BugType.DEADLOCK -> 350
                                    BugType.MEMORY_LEAK -> 250
                                    BugType.PHANTOM -> 220
                                    BugType.SWARMER -> 100
                                    BugType.NORMAL -> 150
                                }
                                _score.value += points * _multiplier.value
                                _streakGems.value += 1

                                soundPlayer.playSfx(SoundEffect.BUG_POP)
                                addScreenShake(if (calcDmg.isBoosted) 4.5f else 3f)

                                // Voxel Shatter Debris burst matching bug type color
                                val bugColor = when (bug.bugType) {
                                    BugType.MEMORY_LEAK -> Color(0xFF00E676)
                                    BugType.DEADLOCK -> Color(0xFF00E5FF)
                                    BugType.PHANTOM -> Color(0xFFB388FF)
                                    BugType.SWARMER -> Color(0xFFFFD600)
                                    BugType.NORMAL -> Color(0xFFFF1744)
                                }
                                spawnDebris(bug.posX, bug.posY, 12f, bugColor, count = 12, speed = 0.28f)
                                addFloatingText(bug.posX, bug.posY, "+${points * _multiplier.value} PTS", Color(0xFF00E676))
                                if (calcDmg.isBoosted) {
                                    val boostText = if (calcDmg.damage >= 3) "-${calcDmg.damage} CRIT BOOST!" else "-${calcDmg.damage} HP BOOST!"
                                    addFloatingText(bug.posX, bug.posY - 15f, boostText, Color(0xFFFFD600))
                                }

                                // Shockwave on elite bug kills or supercharge attacks
                                if (bug.isMemoryLeak || bug.isDeadlock || bug.isPhantom) {
                                    triggerWorldShockwave(bug.posX, bug.posY, intensity = 0.65f)
                                    onTriggerCombatImpact?.invoke(0.7f)
                                } else if (bullet.isSuperCharge || bullet.damage >= 2) {
                                    triggerWorldShockwave(bug.posX, bug.posY, intensity = 0.8f)
                                    onTriggerCombatImpact?.invoke(0.6f)
                                } else {
                                    onTriggerCombatImpact?.invoke(0.3f)
                                }

                                // Killstreak progression
                                registerKill()

                                // Drops
                                if (bug.isMemoryLeak && Random.nextFloat() < 0.65f) {
                                    val shard = TriviaShardVoxelActor(bug.posX, bug.posY, tier = Random.nextInt(2, 4))
                                    triviaShards.add(shard)
                                    runCatching { actorManager.add(shard) }
                                    _triviaFeedback.value = "💾 MEMORY LEAK ELIMINATED! Byte Shard Dropped!"
                                    addFloatingText(bug.posX, bug.posY, "BYTE SHARD!", Color(0xFF00E5FF))
                                } else if (Random.nextBoolean()) {
                                    val dropGem = GemVoxelActor(bug.posX, bug.posY, pointValue = 150)
                                    gems.add(dropGem)
                                    runCatching { actorManager.add(dropGem) }
                                }

                                // Gun drops: 10% on red bugs, 30% on tiny colored bugs
                                val gunDropChance = if (bug.bugType == BugType.NORMAL) 0.10f else 0.30f
                                if (Random.nextFloat() < gunDropChance) {
                                    spawnRandomGunDrop(bug.posX, bug.posY)
                                }
                            }
                            DamageResult.HIT -> {
                                soundPlayer.playSfx(SoundEffect.PLAYER_HIT)
                                addScreenShake(if (calcDmg.isBoosted) 2.5f else 1.5f)
                                spawnDebris(bullet.posX, bullet.posY, 12f, Color.White, count = 4, size = 3f)
                                if (calcDmg.isBoosted) {
                                    val boostText = if (calcDmg.damage >= 3) "-${calcDmg.damage} CRIT BOOST!" else "-${calcDmg.damage} HP BOOST!"
                                    addFloatingText(bug.posX, bug.posY, boostText, Color(0xFFFFD600))
                                } else {
                                    addFloatingText(bug.posX, bug.posY, "-${calcDmg.damage} HP", Color(0xFFFFD54F))
                                }
                            }
                        }
                        return@forEach
                    }
                }

                // Bullet vs Boss
                boss?.let { b ->
                    if (!bullet.isDestroyed && !b.isDefeated && isBossInView(b) && distance(bullet.posX, bullet.posY, b.posX, b.posY) < b.collisionRadius + 14f) {
                        bullet.isDestroyed = true
                        bulletsToRemove.add(bullet)
                        val dist = distance(bullet.originX, bullet.originY, b.posX, b.posY)
                        val calcDmg = calculateRangeDamage(bullet.damage, dist, bullet.damage >= 2)
                        val dead = b.takeDamage(calcDmg.damage)
                        _bossHp.value = Pair(b.hp.coerceAtLeast(0), b.maxHp)
                        _score.value += 100 * _multiplier.value
                        addScreenShake(if (calcDmg.isBoosted) 4f else 3f)
                        spawnDebris(bullet.posX, bullet.posY, 25f, Color(0xFFFF5252), count = 6, size = 4f)
                        val critText = if (calcDmg.isBoosted) "-${calcDmg.damage} CRIT BOOST!" else "-${calcDmg.damage} CRIT!"
                        addFloatingText(b.posX, b.posY, critText, Color(0xFFFFD600))

                        if (dead) {
                            _score.value += 6000 * _multiplier.value
                            _bossHp.value = null
                            _bossKernelPanic.value = false
                            soundPlayer.playSfx(SoundEffect.TRIVIA_CORRECT)
                            addScreenShake(24f)
                            spawnDebris(b.posX, b.posY, 30f, Color(0xFFFF1744), count = 40, speed = 0.5f, size = 8f)
                            addFloatingText(b.posX, b.posY, "TITAN SLAIN! +6000 PTS!", Color(0xFFFFD600), durationMs = 2000)
                            _triviaFeedback.value = "🏆 OutOfMemory TITAN DEFEATED! +6000 PTS!"
                            triggerWorldShockwave(b.posX, b.posY, intensity = 1.0f)
                            onTriggerCombatImpact?.invoke(1.0f)
                            runCatching { actorManager.remove(b) }
                            boss = null
                            // Spawn victory gems ring
                            for (i in 0 until 14) {
                                val angle = i * (6.28318f / 14f)
                                val gx = (b.posX + cos(angle) * 130f).coerceIn(120f, 3680f)
                                val gy = (b.posY + sin(angle) * 130f).coerceIn(120f, 3680f)
                                val vicGem = GemVoxelActor(gx, gy, pointValue = 300)
                                gems.add(vicGem)
                                runCatching { actorManager.add(vicGem) }
                            }
                        }
                        return@forEach
                    }
                }
            } else {
                // Enemy Bullet vs Player (Invulnerable while dashing or in i-frames!)
                if (!player.isInvulnerable && distance(bullet.posX, bullet.posY, player.posX, player.posY) < player.collisionRadius + 8f) {
                    bullet.isDestroyed = true
                    bulletsToRemove.add(bullet)
                    onPlayerHit(bullet.dirX, bullet.dirY, isBossHit = false)
                }
            }
        }

        if (bulletsToRemove.isNotEmpty()) {
            projectiles.removeAll(bulletsToRemove)
            runCatching { actorManager.remove(bulletsToRemove) }
        }
        if (bugsToKill.isNotEmpty()) {
            bugs.removeAll(bugsToKill)
            runCatching { actorManager.remove(bugsToKill) }
        }
        if (cratesToDestroy.isNotEmpty()) {
            crates.removeAll(cratesToDestroy)
            runCatching { actorManager.remove(cratesToDestroy) }
        }
    }

    private fun selectProgressiveBugType(): BugType {
        val remaining = _remainingTimeSeconds.value
        return when {
            remaining > 90 -> {
                // 0-30s: Normal bugs + Swarmers
                if (Random.nextFloat() < 0.35f) BugType.SWARMER else BugType.NORMAL
            }
            remaining > 60 -> {
                // 30-60s: Phantoms + Armored Deadlocks + Memory Leaks
                val roll = Random.nextFloat()
                when {
                    roll < 0.30f -> BugType.PHANTOM
                    roll < 0.60f -> BugType.DEADLOCK
                    roll < 0.85f -> BugType.MEMORY_LEAK
                    else -> BugType.NORMAL
                }
            }
            else -> {
                // 60s+: All variants
                BugType.values().random()
            }
        }
    }

    private fun registerKill() {
        val streak = _killstreak.value + 1
        _killstreak.value = streak

        when (streak) {
            5 -> {
                _killstreakBanner.value = "🔥 DOUBLE COMMIT! (5 STREAK)"
                killstreakBannerTimerMs = 2400
                soundPlayer.playSfx(SoundEffect.UPGRADE_UNLOCKED)
                addScreenShake(4f)
            }
            10 -> {
                _killstreakBanner.value = "⚡ TRIPLE THREAD! (10 STREAK)"
                killstreakBannerTimerMs = 2400
                soundPlayer.playSfx(SoundEffect.UPGRADE_UNLOCKED)
                addScreenShake(6f)
            }
            15 -> {
                _killstreakBanner.value = "🚀 COMPILATION SPREE! (15 STREAK)"
                killstreakBannerTimerMs = 2600
                soundPlayer.playSfx(SoundEffect.UPGRADE_UNLOCKED)
                addScreenShake(8f)
            }
            20 -> {
                _killstreakBanner.value = "👑 GODLIKE REFACTOR! (20 STREAK)"
                killstreakBannerTimerMs = 3000
                soundPlayer.playSfx(SoundEffect.UPGRADE_UNLOCKED)
                addScreenShake(12f)
            }
        }
    }

    fun onGemCollected(gem: GemVoxelActor) {
        soundPlayer.playSfx(SoundEffect.GEM_CHIME)
        val currentMult = _multiplier.value
        val pointAward = 50 * currentMult
        _score.value += pointAward
        _gemsCollectedTotal.value += 1

        val currentStreak = _streakGems.value + 1
        _streakGems.value = currentStreak

        if (currentStreak >= 30) _multiplier.value = 5
        else if (currentStreak >= 20) _multiplier.value = 4
        else if (currentStreak >= 10) _multiplier.value = 3
        else if (currentStreak >= 5) _multiplier.value = 2

        addFloatingText(gem.posX, gem.posY, "+$pointAward PTS", Color(0xFF00E5FF))
        spawnDebris(gem.posX, gem.posY, 8f, Color(0xFF00E5FF), count = 4, size = 3f)
    }

    fun killBug(bug: BugHazardActor, isAoEOrLaser: Boolean = false) {
        bug.hp = 0
        bugs.remove(bug)
        runCatching { actorManager.remove(bug) }
        _bugsSquashedTotal.value += 1
        bugsSquashedThisFloor.value += 1

        bountyBeacons.filter { it.isActive }.forEach { beacon ->
            if (beacon.recordKill()) {
                val bountyPts = 5000 * _multiplier.value
                _score.value += bountyPts
                _multiplier.value = (_multiplier.value + 1).coerceAtMost(5)
                soundPlayer.playSfx(SoundEffect.UPGRADE_UNLOCKED)
                addScreenShake(15f)
                addFloatingText(player.posX, player.posY, "🎯 BOUNTY CLAIMED! +$bountyPts PTS", Color(0xFFFFD600))
                _triviaFeedback.value = "🏆 BOUNTY COMPLETE! +$bountyPts PTS • MULTIPLIER UP!"
            }
        }

        val points = 25
        _score.value += points * _multiplier.value
        _streakGems.value += 1

        soundPlayer.playSfx(SoundEffect.BUG_POP)
        addScreenShake(if (isAoEOrLaser) 5f else 3f)

        val gem = GemVoxelActor(bug.posX, bug.posY, pointValue = 50)
        gems.add(gem)
        runCatching { actorManager.add(gem) }

        val bugColor = when (bug.bugType) {
            BugType.MEMORY_LEAK -> Color(0xFF00E676)
            BugType.DEADLOCK -> Color(0xFF00E5FF)
            BugType.PHANTOM -> Color(0xFFB388FF)
            BugType.SWARMER -> Color(0xFFFFD600)
            BugType.NORMAL -> Color(0xFFFF1744)
        }
        spawnDebris(bug.posX, bug.posY, 10f, bugColor, count = 10, speed = 0.35f)
        addFloatingText(bug.posX, bug.posY, "+${points * _multiplier.value} PTS", Color(0xFF00E676))

        if (bugsSquashedThisFloor.value >= bugsTargetForElevator.value) {
            ascendToNextFloor()
        }
    }

    fun detonateBarrel(barrel: ExplosiveBarrelVoxelActor) {
        if (barrel.isDetonated) return
        barrel.detonate()
        soundPlayer.playSfx(SoundEffect.BUG_POP)
        addScreenShake(20f)
        spawnDebris(barrel.posX, barrel.posY, 16f, Color(0xFFFF3D00), count = 25, speed = 0.7f, size = 6f)
        spawnDebris(barrel.posX, barrel.posY, 12f, Color(0xFFFFD600), count = 20, speed = 0.5f, size = 4f)
        addFloatingText(barrel.posX, barrel.posY, "💥 CRITICAL DETONATION! 500 DMG", Color(0xFFFF3D00))
        triggerWorldShockwave(barrel.posX, barrel.posY, intensity = 0.9f)
        onTriggerCombatImpact?.invoke(0.85f)

        _score.value += 150 * _multiplier.value

        // 1. AoE Damage to all bugs within 140f
        val affectedBugs = bugs.filter { it.hp > 0 && distance(it.posX, it.posY, barrel.posX, barrel.posY) <= barrel.explosionRadius }.toList()
        affectedBugs.forEach { bug ->
            killBug(bug, isAoEOrLaser = true)
        }

        // Damage Boss Titan if in blast radius
        boss?.let { b ->
            if (!b.isDefeated && distance(b.posX, b.posY, barrel.posX, barrel.posY) <= barrel.explosionRadius + b.collisionRadius) {
                val dead = b.takeDamage(10)
                _bossHp.value = Pair(b.hp.coerceAtLeast(0), b.maxHp)
                _score.value += 500 * _multiplier.value
                soundPlayer.playSfx(SoundEffect.RAILGUN_BLAST)
                addScreenShake(12f)
                spawnDebris(barrel.posX, barrel.posY, 25f, Color(0xFFFF5252), count = 15, size = 6f)
                addFloatingText(b.posX, b.posY, "-10 HP BARREL BLAST!", Color(0xFFFFD600))
                if (dead) {
                    _score.value += 6000 * _multiplier.value
                    _bossHp.value = null
                    _bossKernelPanic.value = false
                    soundPlayer.playSfx(SoundEffect.TRIVIA_CORRECT)
                    addScreenShake(24f)
                    spawnDebris(b.posX, b.posY, 30f, Color(0xFFFF1744), count = 40, speed = 0.5f, size = 8f)
                    addFloatingText(b.posX, b.posY, "TITAN SLAIN! +6000 PTS!", Color(0xFFFFD600), durationMs = 2000)
                    _triviaFeedback.value = "🏆 OutOfMemory TITAN DEFEATED! +6000 PTS!"
                    triggerWorldShockwave(b.posX, b.posY, intensity = 1.0f)
                    onTriggerCombatImpact?.invoke(1.0f)
                    runCatching { actorManager.remove(b) }
                    boss = null
                    for (i in 0 until 14) {
                        val angle = i * (6.28318f / 14f)
                        val gx = (b.posX + cos(angle) * 130f).coerceIn(120f, 3680f)
                        val gy = (b.posY + sin(angle) * 130f).coerceIn(120f, 3680f)
                        val vicGem = GemVoxelActor(gx, gy, pointValue = 300)
                        gems.add(vicGem)
                        runCatching { actorManager.add(vicGem) }
                    }
                }
            }
        }

        // 2. Chain reaction to other nearby barrels
        val nearbyBarrels = explosiveBarrels.filter { !it.isDetonated && distance(it.posX, it.posY, barrel.posX, barrel.posY) <= barrel.explosionRadius }
        nearbyBarrels.forEach { other ->
            detonateBarrel(other)
        }

        // 3. Destroy crates within explosion radius
        val nearbyCrates = crates.filter { !it.isDestroyed && distance(it.posX, it.posY, barrel.posX, barrel.posY) <= barrel.explosionRadius }
        nearbyCrates.forEach { crate ->
            crate.isDestroyed = true
            spawnDebris(crate.posX, crate.posY, 14f, Color(0xFFFFB300), count = 12, speed = 0.3f)
            val gem = GemVoxelActor(crate.posX, crate.posY, pointValue = 120)
            gems.add(gem)
            runCatching { actorManager.add(gem) }
            if (Random.nextFloat() < 0.50f) {
                spawnRandomGunDrop(crate.posX, crate.posY)
            }
        }
    }

    fun onPlayerHit(knockbackDirX: Float = 0f, knockbackDirY: Float = 0f, isBossHit: Boolean = false) {
        if (player.isInvulnerable) return

        // Refactor Firewall perk blocks 1 incoming hit!
        if (player.hasRefactorFirewall && player.isFirewallShieldActive) {
            player.isFirewallShieldActive = false
            player.firewallRechargeTimerMs = 20000
            soundPlayer.playSfx(SoundEffect.RAILGUN_BLAST)
            addScreenShake(5f)
            spawnDebris(player.posX, player.posY, 18f, Color(0xFF00E5FF), count = 14, speed = 0.35f, size = 5f)
            addFloatingText(player.posX, player.posY, "🛡️ FIREWALL BLOCKED!", Color(0xFF00E5FF), durationMs = 1200)
            return
        }

        soundPlayer.playSfx(SoundEffect.PLAYER_HIT)
        val stunTime = if (isBossHit) 190 else 150
        val iFrames = if (isBossHit) 1400 else 1100
        val knockbackForce = if (isBossHit) 80f else 40f

        player.triggerHit(stunDurationMs = stunTime, iFramesMs = iFrames)
        player.applyKnockback(knockbackDirX, knockbackDirY, force = knockbackForce)
        addScreenShake(if (isBossHit) 14f else 8f)
        spawnDebris(player.posX, player.posY, 16f, Color(0xFFFF5252), count = 8, size = 4f)
        addFloatingText(player.posX, player.posY, "HIT!", Color(0xFFFF5252))

        val isTrivia = _sessionState.value == GameSessionState.TRIVIA_MODAL
        val penalty = if (isTrivia) 100 else 50
        _score.value = (_score.value - penalty).coerceAtLeast(0)
        val penaltyText = if (isTrivia) "-100 PTS! [2X DAMAGE]" else "-50 PTS!"
        addFloatingText(player.posX, player.posY - 20f, penaltyText, Color(0xFFFF1744))

        triggerWorldShockwave(player.posX, player.posY, intensity = 0.75f)
        onTriggerGlitch?.invoke(if (isBossHit) 600 else 350, if (isBossHit) 0.95f else 0.7f)
        onTriggerCrimsonAlert?.invoke(if (isBossHit) 900 else 600, if (isBossHit) 0.95f else 0.75f)
        onTriggerCombatImpact?.invoke(0.9f)

        _killstreak.value = 0
        _streakGems.value = (_streakGems.value / 2)
        if (_activePowerUp.value != PowerUpType.MEGA_FRENZY_NUKE) {
            _multiplier.value = (_multiplier.value - 1).coerceAtLeast(1)
        }
    }

    fun rollRandomWeaponType(): WeaponType {
        val roll = Random.nextFloat() * 100f
        return when {
            roll < 25f -> WeaponType.TWIN_PULSE // 25%
            roll < 50f -> WeaponType.TRIPLE_SPREAD // 25%
            roll < 70f -> WeaponType.HEAVY_RAILGUN // 20%
            roll < 90f -> WeaponType.PLASMA_FLAK_CANNON // 20%
            else -> WeaponType.NOVA_360_BLASTER // 10%
        }
    }

    fun spawnRandomGunDrop(x: Float, y: Float): GunDropVoxelActor {
        val weapon = rollRandomWeaponType()
        val drop = GunDropVoxelActor(x, y, posZ = 0f, weaponType = weapon)
        gunDrops.add(drop)
        runCatching { actorManager.add(drop) }
        return drop
    }

    private fun triggerShardTrivia(tierLevel: Int) {
        val tier = when (tierLevel) {
            3 -> QuestionTier.TIER_3_HARD
            2 -> QuestionTier.TIER_2_MEDIUM
            else -> QuestionTier.TIER_1_EASY
        }
        val question = KotlinQuestionBank.getRandomQuestion(tier)
        _currentTriviaQuestion.value = question
        _sessionState.value = GameSessionState.TRIVIA_MODAL
        player.isMovementLocked = true
        player.setDirectMove(0f, 0f)
        _triviaLockoutRemainingMs.value = 2000
    }

    fun submitTriviaAnswer(selectedIndex: Int) {
        if (_triviaLockoutRemainingMs.value > 0) return
        val question = _currentTriviaQuestion.value ?: return
        _triviaAttemptedCount.value += 1

        if (selectedIndex == question.correctIndex) {
            soundPlayer.playSfx(SoundEffect.TRIVIA_CORRECT)
            _triviaCorrectCount.value += 1
            _score.value += question.tier.pointValue
            _triviaFeedback.value = "CORRECT! +${question.tier.pointValue} PTS & ${question.powerUp.title} ACTIVATED!"
            activatePowerUp(question.powerUp)

            // 90% chance to drop random weapon near the player on correct trivia answer
            if (Random.nextFloat() < 0.90f) {
                val dropAngle = Random.nextFloat() * 6.28318f
                val dropDist = Random.nextFloat() * 35f + 25f
                spawnRandomGunDrop(
                    (player.posX + cos(dropAngle) * dropDist).coerceIn(100f, 3700f),
                    (player.posY + sin(dropAngle) * dropDist).coerceIn(100f, 3700f)
                )
            }

            // Prepare 3-perk "Version Upgrade" roguelite perk selection!
            val available = VersionPerk.values().filterNot { _unlockedPerks.value.contains(it) }
            if (available.isNotEmpty()) {
                val perksToShow = available.shuffled().take(3)
                _perkSelectionOptions.value = perksToShow
                return // Stay in TRIVIA_MODAL to show perk selection
            } else {
                // All perks unlocked: bonus score
                _score.value += 1000
                _triviaFeedback.value = "ALL VERSION UPGRADES MASTERED! +1000 BONUS PTS!"
            }
        } else {
            soundPlayer.playSfx(SoundEffect.TRIVIA_WRONG)
            _triviaFeedback.value = "INCORRECT! Hack failed."
        }

        player.isMovementLocked = false
        _sessionState.value = GameSessionState.PLAYING
        _currentTriviaQuestion.value = null
        _perkSelectionOptions.value = null
    }

    fun selectVersionPerk(perk: VersionPerk) {
        unlockPerk(perk)
        _perkSelectionOptions.value = null
        _currentTriviaQuestion.value = null
        player.isMovementLocked = false
        _sessionState.value = GameSessionState.PLAYING
    }

    fun unlockPerk(perk: VersionPerk) {
        _unlockedPerks.value = _unlockedPerks.value + perk
        soundPlayer.playSfx(SoundEffect.UPGRADE_UNLOCKED)
        addScreenShake(5f)
        addFloatingText(player.posX, player.posY, "PERK: ${perk.title}!", Color(0xFF00E5FF), durationMs = 1500)
        _triviaFeedback.value = "UPGRADE INSTALLED: ${perk.title} • ${perk.subtitle}"

        when (perk) {
            VersionPerk.GARBAGE_COLLECTOR_DRONE -> {
                player.hasGarbageCollectorDrone = true
                val newDrone = CompanionDroneVoxelActor(player.posX, player.posY)
                drone = newDrone
                runCatching { actorManager.add(newDrone) }
            }
            VersionPerk.COROUTINES_TURRET -> {
                player.hasCoroutinesTurrets = true
            }
            VersionPerk.JIT_COMPILER -> {
                player.hasJitCompiler = true
            }
            VersionPerk.REFACTOR_FIREWALL -> {
                player.hasRefactorFirewall = true
                player.isFirewallShieldActive = true
            }
        }
    }

    fun activatePowerUp(powerUp: PowerUpType, durationSeconds: Int = powerUp.durationSeconds) {
        _activePowerUp.value = powerUp
        _powerUpRemainingSeconds.value = durationSeconds
        powerUpAccumulatorMs = 0

        when (powerUp) {
            PowerUpType.OVERCHARGE_INVINCIBLE -> {
                player.invulnerableTimerMs = durationSeconds * 1000
                player.isSpeedBoostActive = true
                player.isShieldActive = true
                _multiplier.value = 5
            }
            PowerUpType.SPEED_MAGNET -> {
                player.isSpeedBoostActive = true
            }
            PowerUpType.FREEZE_SHIELD -> {
                player.isShieldActive = true
                bugs.forEach { it.isFrozen = true }
            }
            PowerUpType.MEGA_FRENZY_NUKE -> {
                _multiplier.value = 5
                _score.value += bugs.size * 300
                actorManager.remove(bugs)
                bugs.clear()
                spawnBugs(6)
            }
        }
    }

    private fun deactivatePowerUp() {
        _activePowerUp.value = null
        player.isSpeedBoostActive = false
        player.isShieldActive = (player.archetype == PlayerArchetype.ARCH_DEV)
        if (player.invulnerableTimerMs > 1500) {
            player.invulnerableTimerMs = 0
        }
        bugs.forEach { it.isFrozen = false }
        _multiplier.value = if (_streakGems.value >= 10) 3 else if (_streakGems.value >= 5) 2 else 1
    }

    internal fun endRound() {
        soundPlayer.stopBgm()
        soundPlayer.startTitleTheme()
        _sessionState.value = GameSessionState.ROUND_OVER
        LocalLeaderboardManager.addEntry(
            LeaderboardEntry(
                playerName = playerHandle.value.ifBlank { "Dev_Kotlin" },
                score = _score.value,
                gemsCollected = _gemsCollectedTotal.value,
                triviaCorrect = _triviaCorrectCount.value,
                triviaAttempted = _triviaAttemptedCount.value,
                highestMultiplier = _multiplier.value,
                timestamp = System.currentTimeMillis()
            )
        )
    }

    private fun clearArena() {
        runCatching { actorManager.removeAll() }
        bugs.clear()
        gems.clear()
        trees.clear()
        serverRacks.clear()
        dataRelayTowers.clear()
        techBarricades.clear()
        crates.clear()
        explosiveBarrels.clear()
        overclockConduits.clear()
        laserFences.clear()
        bountyBeacons.clear()
        triviaShards.clear()
        gunDrops.clear()
        projectiles.clear()
        turrets.clear()
        drone = null
        boss = null
        hasSpawnedBoss = false
        player.isMovementLocked = false
        _triviaLockoutRemainingMs.value = 0
        proceduralFloorTiles.clear()
        proceduralFloorTraces.clear()
        proceduralWalls.clear()
        walkableCells.clear()
        synchronized(debrisParticles) { debrisParticles.clear() }
        synchronized(floatingTexts) { floatingTexts.clear() }
    }

    private fun spawnInitialEntities(isStartup: Boolean = false) {
        spawnMapStructures()

        player = PlayerVoxelActor(1900f, 1900f, archetype = _selectedArchetype.value)
        player.groundZProvider = ::getGroundZ
        player.posZ = getGroundZ(1900f, 1900f)
        player.onGamepadButton = ::handleGamepadButton
        player.onFireWeaponTriggered = ::firePlayerWeapon
        player.onDashTriggered = { onPlayerDashExecuted(player.posX, player.posY) }
        player.canMoveTo = ::isPositionWalkable
        player.cameraAngleRad = currentCameraAngleRad
        runCatching {
            actorManager.add(player)
            actorManager.add(ArenaVoxelRendererActor(this))
        }
        _activeWeapon.value = player.activeWeapon
        _isFirewallShieldActive.value = player.isFirewallShieldActive

        player.hasAmmo = {
            val cost = if (_activePowerUp.value == PowerUpType.SPEED_MAGNET) 3 else player.activeWeapon.ammoCost
            _score.value >= cost
        }

        if (!isStartup) {
            spawnGems(80)
            spawnBugs(38)
        }
    }

    fun generateMapCells(
        candidateRacks: MutableList<Pair<Float, Float>> = mutableListOf(),
        candidateTrees: MutableList<Pair<Float, Float>> = mutableListOf(),
        candidateCrates: MutableList<Pair<Float, Float>> = mutableListOf(),
        candidateBarrels: MutableList<Pair<Float, Float>> = mutableListOf(),
        candidateBarricades: MutableList<Pair<Float, Float>> = mutableListOf()
    ) {
        proceduralFloorTiles.clear()
        proceduralFloorTraces.clear()
        proceduralWalls.clear()
        walkableCells.clear()
        cellHeights.clear()
        ramps.clear()

        val theme = getFloorTheme(currentFloor.value)
        val step = 100f
        val gridMin = 100f
        val gridMax = 3700f
        val numSteps = ((gridMax - gridMin) / step).toInt() + 1 // 37 (gx in 0..36, gy in 0..36)
        val center = 1900f

        // Phase 1: Determine walkable cells and sample Perlin / fBm noise for candidate props
        for (gx in 0 until numSteps) {
            for (gy in 0 until numSteps) {
                isCellWalkable[gx][gy] = false
            }
        }

        // Procedural organic border noise seeds
        val seedOffX = perlinNoise.noise2D(77.11f, 88.22f) * 40f
        val seedOffY = perlinNoise.noise2D(99.33f, 33.44f) * 40f

        for (gx in 0 until numSteps) {
            for (gy in 0 until numSteps) {
                val worldX = gridMin + gx * step
                val worldY = gridMin + gy * step
                val distCenter = distance(worldX, worldY, center, center)

                // 1. Guaranteed central core plaza (distCenter <= 550f)
                val inCore = distCenter <= 550f

                // 2. Guaranteed grand highways along cardinal axes within 1450f
                val inHighway = (kotlin.math.abs(worldX - center) <= 150f || kotlin.math.abs(worldY - center) <= 150f) && distCenter <= 1450f

                // 3. Multi-frequency organic border noise
                val angle = kotlin.math.atan2(worldY - center, worldX - center)
                val polarNoise1 = perlinNoise.noise2D(kotlin.math.cos(angle) * 1.6f + seedOffX, kotlin.math.sin(angle) * 1.6f + seedOffY)
                val polarNoise2 = perlinNoise.noise2D(kotlin.math.cos(angle * 3f) * 2.2f, kotlin.math.sin(angle * 3f) * 2.2f) * 0.4f
                val cartesianNoise = perlinNoise.noise2D(worldX * 0.0016f + seedOffX, worldY * 0.0016f + seedOffY)
                val fineJitter = perlinNoise.noise2D(worldX * 0.0045f, worldY * 0.0045f) * 0.25f

                // Dynamic boundary radius varying between ~1100f and ~1720f (irregular non-circular shape)
                val dynamicRadius = 1420f + (polarNoise1 * 260f) + (polarNoise2 * 140f) + (cartesianNoise * 180f) + (fineJitter * 80f)

                // 4. Fractal Brownian motion density mask for organic bays, inlets, and promontories
                val densityMask = perlinNoise.fbm2D(worldX * 0.0012f + seedOffX, worldY * 0.0012f + seedOffY, octaves = 3)

                val inArena = gx in 1..35 && gy in 1..35 && (
                    inCore ||
                    inHighway ||
                    (distCenter <= dynamicRadius && densityMask > 0.30f)
                )

                if (!inArena) continue

                isCellWalkable[gx][gy] = true
                val cell = Pair(gx, gy)
                walkableCells.add(cell)
                cellHeights[cell] = 0f

                // Candidate props placement: sample Perlin noise for organic clustering while keeping center clear
                if (distCenter in 450f..1550f) {
                    val pNoise = perlinNoise.noise2D(gx * 0.18f, gy * 0.18f)
                    val fbmVal = perlinNoise.fbm2D(gx * 0.14f, gy * 0.14f, octaves = 3)
                    val absNoise = kotlin.math.abs(pNoise)
                    when {
                        pNoise > 0.35f || fbmVal > 0.70f -> candidateRacks.add(Pair(worldX, worldY))
                        pNoise < -0.35f || fbmVal < 0.30f -> candidateTrees.add(Pair(worldX, worldY))
                        fbmVal in 0.45f..0.55f -> candidateBarricades.add(Pair(worldX, worldY))
                        absNoise in 0.10f..0.22f -> candidateCrates.add(Pair(worldX, worldY))
                        absNoise in 0.22f..0.34f -> candidateBarrels.add(Pair(worldX, worldY))
                    }
                }
            }
        }

        // Phase 2: Emit floor tiles & District Micro-Biomes (flat plane at Z = -6f, top surface at Z = 0f)
        for (gx in 0 until numSteps) {
            for (gy in 0 until numSteps) {
                val worldX = gridMin + gx * step
                val worldY = gridMin + gy * step
                val cell = Pair(gx, gy)

                if (walkableCells.contains(cell)) {
                    val distCenter = distance(worldX, worldY, center, center)

                    when {
                        // 1. Central Grand CPU Core Plaza (distCenter <= 550f)
                        distCenter <= 550f -> {
                            val isCpuCore = distCenter <= 250f
                            val floorCol = if (isCpuCore) Color(0xFF0D1B2A) else Color(0xFF162330)
                            proceduralFloorTiles.add(
                                VoxelCuboid(worldX, worldY, -6f, 96f, 96f, 6f, floorCol, isFloor = true)
                            )
                            // Concentric Gold Heat-Sink Traces (0xFFFFD600)
                            val goldPhase = if (isCpuCore) (gx + gy) * 0.4f else distCenter * 0.015f
                            proceduralFloorTraces.add(
                                FloorTrace(
                                    x = worldX,
                                    y = worldY,
                                    z = -2f,
                                    sizeX = if (isCpuCore) 80f else 88f,
                                    sizeY = if (isCpuCore) 8f else 10f,
                                    sizeZ = 2f,
                                    baseColor = Color(0xFFFFD600),
                                    pulsePhase = goldPhase,
                                    pulseSpeed = 2.5f,
                                    minAlpha = 0.45f,
                                    maxAlpha = 0.95f
                                )
                            )
                            // Cyan Crosshairs along center axes (gx == 18 || gy == 18)
                            if (gx == 18) {
                                proceduralFloorTraces.add(
                                    FloorTrace(
                                        x = worldX,
                                        y = worldY,
                                        z = -2f,
                                        sizeX = 16f,
                                        sizeY = 96f,
                                        sizeZ = 2f,
                                        baseColor = Color(0xFF00E5FF),
                                        pulsePhase = 0f,
                                        pulseSpeed = 2.5f,
                                        minAlpha = 0.60f,
                                        maxAlpha = 1.0f
                                    )
                                )
                            }
                            if (gy == 18) {
                                proceduralFloorTraces.add(
                                    FloorTrace(
                                        x = worldX,
                                        y = worldY,
                                        z = -2f,
                                        sizeX = 96f,
                                        sizeY = 16f,
                                        sizeZ = 2f,
                                        baseColor = Color(0xFF00E5FF),
                                        pulsePhase = 0f,
                                        pulseSpeed = 2.5f,
                                        minAlpha = 0.60f,
                                        maxAlpha = 1.0f
                                    )
                                )
                            }
                        }

                        // 2. Ring Concourse: Circular Cyber-Road at radius 1000f connecting all 4 districts
                        kotlin.math.abs(distCenter - 1000f) <= 90f -> {
                            proceduralFloorTiles.add(
                                VoxelCuboid(worldX, worldY, -6f, 96f, 96f, 6f, Color(0xFF192330), isFloor = true)
                            )
                            // Glowing Amber Guidance Nodes / Roadway Chevrons
                            proceduralFloorTraces.add(
                                FloorTrace(
                                    x = worldX,
                                    y = worldY,
                                    z = -2f,
                                    sizeX = 32f,
                                    sizeY = 32f,
                                    sizeZ = 2f,
                                    baseColor = Color(0xFFFFB300),
                                    pulsePhase = distCenter * 0.02f,
                                    pulseSpeed = 3f,
                                    minAlpha = 0.40f,
                                    maxAlpha = 0.95f
                                )
                            )
                        }

                        // 3. Grand Circuit Highways: 4 illuminated cardinal highways (4-tile-wide pathways)
                        (gx in 17..20) || (gy in 17..20) -> {
                            proceduralFloorTiles.add(
                                VoxelCuboid(worldX, worldY, -6f, 96f, 96f, 6f, Color(0xFF16202A), isFloor = true)
                            )
                            // Glowing Cyan Center Divider along highway mid-lines
                            if (gx == 18) {
                                proceduralFloorTraces.add(
                                    FloorTrace(
                                        x = worldX,
                                        y = worldY,
                                        z = -2f,
                                        sizeX = 14f,
                                        sizeY = 96f,
                                        sizeZ = 2f,
                                        baseColor = Color(0xFF00E5FF),
                                        pulsePhase = gy * 0.25f,
                                        pulseSpeed = 3f,
                                        minAlpha = 0.45f,
                                        maxAlpha = 1.0f
                                    )
                                )
                            }
                            if (gy == 18) {
                                proceduralFloorTraces.add(
                                    FloorTrace(
                                        x = worldX,
                                        y = worldY,
                                        z = -2f,
                                        sizeX = 96f,
                                        sizeY = 14f,
                                        sizeZ = 2f,
                                        baseColor = Color(0xFF00E5FF),
                                        pulsePhase = gx * 0.25f,
                                        pulseSpeed = 3f,
                                        minAlpha = 0.45f,
                                        maxAlpha = 1.0f
                                    )
                                )
                            }
                            // Highway Border / Shoulder Caution Strips
                            if (gx == 17 || gx == 20) {
                                proceduralFloorTraces.add(
                                    FloorTrace(
                                        x = worldX,
                                        y = worldY,
                                        z = -2f,
                                        sizeX = 6f,
                                        sizeY = 96f,
                                        sizeZ = 2f,
                                        baseColor = Color(0xFFFFD600),
                                        pulsePhase = 0f,
                                        pulseSpeed = 2f,
                                        minAlpha = 0.4f,
                                        maxAlpha = 0.8f
                                    )
                                )
                            }
                            if (gy == 17 || gy == 20) {
                                proceduralFloorTraces.add(
                                    FloorTrace(
                                        x = worldX,
                                        y = worldY,
                                        z = -2f,
                                        sizeX = 96f,
                                        sizeY = 6f,
                                        sizeZ = 2f,
                                        baseColor = Color(0xFFFFD600),
                                        pulsePhase = 0f,
                                        pulseSpeed = 2f,
                                        minAlpha = 0.4f,
                                        maxAlpha = 0.8f
                                    )
                                )
                            }
                        }

                        // Procedural Organic Micro-Biomes sampled from Perlin fBm Noise
                        else -> {
                            val biomeNoise = perlinNoise.fbm2D(gx * 0.12f, gy * 0.12f, octaves = 3)
                            val traceNoise = perlinNoise.noise2D(gx * 0.25f + 13f, gy * 0.25f + 13f)

                            when {
                                // 4. Server Farm & Data Vaults
                                biomeNoise >= 0.62f -> {
                                    proceduralFloorTiles.add(
                                        VoxelCuboid(worldX, worldY, -6f, 96f, 96f, 6f, Color(0xFF141F30), isFloor = true)
                                    )
                                    if (traceNoise > 0.12f) {
                                        proceduralFloorTraces.add(
                                            FloorTrace(
                                                x = worldX,
                                                y = worldY,
                                                z = -2f,
                                                sizeX = 12f,
                                                sizeY = 96f,
                                                sizeZ = 2f,
                                                baseColor = Color(0xFF00E5FF),
                                                pulsePhase = gy * 0.35f,
                                                pulseSpeed = 2.5f,
                                                minAlpha = 0.40f,
                                                maxAlpha = 0.95f
                                            )
                                        )
                                    } else if (traceNoise < -0.2f) {
                                        proceduralFloorTraces.add(
                                            FloorTrace(
                                                x = worldX,
                                                y = worldY,
                                                z = -2f,
                                                sizeX = 56f,
                                                sizeY = 30f,
                                                sizeZ = 2f,
                                                baseColor = Color(0xFF0A1018),
                                                pulsePhase = 0f,
                                                minAlpha = 0.9f,
                                                maxAlpha = 0.9f
                                            )
                                        )
                                    }
                                }

                                // 5. Industrial Power Substation
                                biomeNoise in 0.48f..0.62f -> {
                                    proceduralFloorTiles.add(
                                        VoxelCuboid(worldX, worldY, -6f, 96f, 96f, 6f, Color(0xFF221A15), isFloor = true)
                                    )
                                    val hazardCol = if (traceNoise > 0f) Color(0xFFFFD600) else Color(0xFF2A2015)
                                    proceduralFloorTraces.add(
                                        FloorTrace(
                                            x = worldX,
                                            y = worldY,
                                            z = -2f,
                                            sizeX = 74f,
                                            sizeY = 18f,
                                            sizeZ = 2f,
                                            baseColor = hazardCol,
                                            pulsePhase = (gx - gy) * 0.4f,
                                            pulseSpeed = 3f,
                                            minAlpha = 0.40f,
                                            maxAlpha = 0.95f
                                        )
                                    )
                                }

                                // 6. Quarantine Scrapyard
                                biomeNoise in 0.35f..0.48f -> {
                                    proceduralFloorTiles.add(
                                        VoxelCuboid(worldX, worldY, -6f, 96f, 96f, 6f, Color(0xFF151D18), isFloor = true)
                                    )
                                    if (traceNoise > 0.08f) {
                                        proceduralFloorTraces.add(
                                            FloorTrace(
                                                x = worldX,
                                                y = worldY,
                                                z = -2f,
                                                sizeX = 46f,
                                                sizeY = 46f,
                                                sizeZ = 2f,
                                                baseColor = Color(0xFF00E676),
                                                pulsePhase = (gx + gy) * 0.5f,
                                                pulseSpeed = 1.8f,
                                                minAlpha = 0.40f,
                                                maxAlpha = 0.95f
                                            )
                                        )
                                    }
                                }

                                // 7. Quantum Cyber-Garden
                                biomeNoise in 0.22f..0.35f -> {
                                    proceduralFloorTiles.add(
                                        VoxelCuboid(worldX, worldY, -6f, 96f, 96f, 6f, Color(0xFF20132B), isFloor = true)
                                    )
                                    if (traceNoise > 0f) {
                                        proceduralFloorTraces.add(
                                            FloorTrace(
                                                x = worldX,
                                                y = worldY,
                                                z = -2f,
                                                sizeX = 14f,
                                                sizeY = 76f,
                                                sizeZ = 2f,
                                                baseColor = Color(0xFFE040FB),
                                                pulsePhase = (gx + gy) * 0.4f,
                                                pulseSpeed = 2f,
                                                minAlpha = 0.40f,
                                                maxAlpha = 0.95f
                                            )
                                        )
                                    }
                                }

                                // 8. Outer Megamap Checkered Tech Plaza
                                else -> {
                                    val isCheckered = (gx + gy) % 2 == 0
                                    val col = if (isCheckered) theme.elevatedPlateauColor else theme.floorBaseColor
                                    proceduralFloorTiles.add(
                                        VoxelCuboid(worldX, worldY, -6f, 96f, 96f, 6f, col, isFloor = true)
                                    )
                                    if (traceNoise > 0.15f) {
                                        proceduralFloorTraces.add(
                                            FloorTrace(
                                                x = worldX,
                                                y = worldY,
                                                z = -2f,
                                                sizeX = 24f,
                                                sizeY = 24f,
                                                sizeZ = 2f,
                                                baseColor = theme.primaryTraceColor,
                                                pulsePhase = (gx + gy) * 0.3f,
                                                pulseSpeed = 2f,
                                                minAlpha = 0.30f,
                                                maxAlpha = 0.70f
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // Outer Digital Abyss Floor Tile
                    proceduralFloorTiles.add(
                        VoxelCuboid(worldX, worldY, -30f, 96f, 96f, 4f, Color(0xFF060A0F), isFloor = true)
                    )
                    if ((gx + gy) % 3 == 0) {
                        proceduralFloorTiles.add(
                            VoxelCuboid(worldX, worldY, -28f, 96f, 6f, 2f, theme.secondaryTraceColor.copy(alpha = 0.25f), isFloor = true)
                        )
                    }
                }
            }
        }

        // Phase 3: Outer Perimeter Boundary Walls with Neon Glowing Warning Caps
        for ((gx, gy) in walkableCells) {
            val wx = gridMin + gx * step
            val wy = gridMin + gy * step

            val directions = listOf(
                Triple(0, -1, Pair(wx, wy - 46f)),
                Triple(0, 1, Pair(wx, wy + 46f)),
                Triple(-1, 0, Pair(wx - 46f, wy)),
                Triple(1, 0, Pair(wx + 46f, wy))
            )
            for ((dx, dy, wallPos) in directions) {
                val cellB = Pair(gx + dx, gy + dy)
                if (!walkableCells.contains(cellB)) {
                    // Outer perimeter wall at flat Z = 0f
                    val szX = if (dx == 0) 96f else 12f
                    val szY = if (dy == 0) 96f else 12f
                    proceduralWalls.add(VoxelCuboid(wallPos.first, wallPos.second, 0f, szX, szY, 26f, theme.wallColor))
                    proceduralWalls.add(VoxelCuboid(wallPos.first, wallPos.second, 26f, if (dx == 0) 96f else 6f, if (dy == 0) 96f else 6f, 4f, theme.wallCapColor))
                }
            }
        }
    }

    private fun spawnMapStructures() {
        val candidateRacks = mutableListOf<Pair<Float, Float>>()
        val candidateTrees = mutableListOf<Pair<Float, Float>>()
        val candidateCrates = mutableListOf<Pair<Float, Float>>()
        val candidateBarrels = mutableListOf<Pair<Float, Float>>()
        val candidateBarricades = mutableListOf<Pair<Float, Float>>()
        generateMapCells(candidateRacks, candidateTrees, candidateCrates, candidateBarrels, candidateBarricades)

        // Clear existing map props before repopulating
        serverRacks.clear()
        trees.clear()
        crates.clear()
        techBarricades.clear()
        explosiveBarrels.clear()
        overclockConduits.clear()
        laserFences.clear()
        bountyBeacons.clear()

        // Place Holographic Extraction Beacon at North Plaza Terminal (1900f, 450f, Z=0f)
        val padX = 1900f
        val padY = 450f
        val newElevator = ElevatorPadVoxelActor(
            posX = padX,
            posY = padY,
            posZ = 0f,
            isActive = isElevatorOnline.value,
            onStepOnElevator = ::ascendToNextFloor
        )
        elevatorPad = newElevator
        runCatching { actorManager.add(newElevator) }

        val occupied = mutableSetOf<Pair<Float, Float>>()
        occupied.add(Pair(padX, padY))

        // Helper to pick candidates with minimum separation distance
        fun pickSpacedCandidates(
            candidates: List<Pair<Float, Float>>,
            count: Int,
            minSeparation: Float
        ): List<Pair<Float, Float>> {
            val selected = mutableListOf<Pair<Float, Float>>()
            for (c in candidates) {
                if (selected.size >= count) break
                val tooClose = occupied.any { distance(c.first, c.second, it.first, it.second) < minSeparation } ||
                        selected.any { distance(c.first, c.second, it.first, it.second) < minSeparation }
                if (!tooClose) {
                    selected.add(c)
                    occupied.add(c)
                }
            }
            return selected
        }

        // 1. Procedural Server Racks (14 racks)
        val sortedRacks = candidateRacks.sortedByDescending { (x, y) -> perlinNoise.noise2D(x * 0.01f, y * 0.01f) }
        val chosenRacks = pickSpacedCandidates(sortedRacks, count = 14, minSeparation = 100f)
        serverRacks.addAll(chosenRacks.map { (x, y) -> ServerRackVoxelActor(x, y, posZ = 0f) })

        // 2. Procedural Trees (14 trees)
        val sortedTrees = candidateTrees.sortedByDescending { (x, y) -> perlinNoise.noise2D(y * 0.01f, x * 0.01f) }
        val chosenTrees = pickSpacedCandidates(sortedTrees, count = 14, minSeparation = 90f)
        trees.addAll(chosenTrees.map { (x, y) -> TreeVoxelActor(x, y, posZ = 0f) })

        // 3. Procedural Crates (18 crates)
        val sortedCrates = candidateCrates.sortedByDescending { (x, y) -> perlinNoise.fbm2D(x * 0.005f, y * 0.005f) }
        val chosenCrates = pickSpacedCandidates(sortedCrates, count = 18, minSeparation = 65f)
        crates.addAll(chosenCrates.mapIndexed { idx, (x, y) -> CrateVoxelActor(x, y, posZ = 0f, containsTriviaShard = (idx < 6)) })

        // 4. Procedural Explosive Byte Barrels (14 barrels)
        val sortedBarrels = candidateBarrels.sortedByDescending { (x, y) -> perlinNoise.noise2D(x * 0.02f, y * 0.02f) }
        val chosenBarrels = pickSpacedCandidates(sortedBarrels, count = 14, minSeparation = 120f)
        explosiveBarrels.addAll(chosenBarrels.map { (x, y) -> ExplosiveBarrelVoxelActor(x, y, posZ = 0f) })

        // 5. Central Defensive Barricades & Procedural Tech Barricades
        val centerBarricades = listOf(
            TechBarricadeActor(1900f, 1480f, posZ = 0f, isHorizontal = true),
            TechBarricadeActor(1900f, 2320f, posZ = 0f, isHorizontal = true),
            TechBarricadeActor(1480f, 1900f, posZ = 0f, isHorizontal = false),
            TechBarricadeActor(2320f, 1900f, posZ = 0f, isHorizontal = false)
        )
        techBarricades.addAll(centerBarricades)
        centerBarricades.forEach { occupied.add(Pair(it.posX, it.posY)) }

        val sortedBarricades = candidateBarricades.sortedByDescending { (x, y) -> perlinNoise.noise2D(y * 0.015f, x * 0.015f) }
        val chosenBarricades = pickSpacedCandidates(sortedBarricades, count = 8, minSeparation = 100f).toMutableList()
        if (chosenBarricades.size < 8) {
            for (cand in sortedBarricades) {
                if (chosenBarricades.size >= 8) break
                if (cand !in chosenBarricades && cand !in occupied) {
                    chosenBarricades.add(cand)
                    occupied.add(cand)
                }
            }
        }
        techBarricades.addAll(chosenBarricades.mapIndexed { idx, (x, y) ->
            TechBarricadeActor(x, y, posZ = 0f, isHorizontal = (idx % 2 == 0))
        })

        val s = 100f
        val gm = 100f
        fun snapToWalkable(x: Float, y: Float): Pair<Float, Float> {
            if (isPointWalkable(x, y)) return Pair(x, y)
            val walkable = walkableCells.toList()
            if (walkable.isEmpty()) return Pair(1900f, 1900f)
            val nearest = walkable.minByOrNull { distance(x, y, gm + it.first * s, gm + it.second * s) }
            return if (nearest != null) Pair(gm + nearest.first * s, gm + nearest.second * s) else Pair(1900f, 1900f)
        }

        // 6. Data Relay Towers (3 towers in outer districts)
        val towerPositions = listOf(
            Pair(1500f, 850f),
            Pair(1900f, 650f),
            Pair(2300f, 850f)
        )
        dataRelayTowers.addAll(towerPositions.map { (x, y) ->
            val p = snapToWalkable(x, y)
            DataRelayTowerActor(p.first, p.second, posZ = 0f)
        })

        // 7. Overclock Power Conduits (1 in each district quadrant)
        val conduitPositions = listOf(
            Pair(1700f, 950f) to PowerUpType.SPEED_MAGNET,
            Pair(2650f, 1900f) to PowerUpType.OVERCHARGE_INVINCIBLE,
            Pair(2100f, 2850f) to PowerUpType.FREEZE_SHIELD,
            Pair(1150f, 1900f) to PowerUpType.MEGA_FRENZY_NUKE
        )
        overclockConduits.addAll(conduitPositions.map { (pos, pType) ->
            val p = snapToWalkable(pos.first, pos.second)
            OverclockConduitVoxelActor(p.first, p.second, posZ = 0f, powerUpType = pType)
        })

        // 8. Dynamic Laser Sweep Fences across highway choke points
        val newFences = listOf(
            LaserFenceVoxelActor(1800f, 1420f, 2000f, 1420f, posZ = 0f, onDurationMs = 4000, offDurationMs = 2000),
            LaserFenceVoxelActor(1800f, 2380f, 2000f, 2380f, posZ = 0f, onDurationMs = 4000, offDurationMs = 2000),
            LaserFenceVoxelActor(2380f, 1800f, 2380f, 2000f, posZ = 0f, onDurationMs = 4000, offDurationMs = 2000),
            LaserFenceVoxelActor(1420f, 1800f, 1420f, 2000f, posZ = 0f, onDurationMs = 4000, offDurationMs = 2000)
        )
        laserFences.addAll(newFences)

        // 9. District Bounty Beacons (1 in each district)
        val newBeacons = listOf(
            BountyBeaconVoxelActor(1900f, 900f, posZ = 0f, districtName = "NORTH SERVER FARM", targetKills = 8),
            BountyBeaconVoxelActor(2900f, 1900f, posZ = 0f, districtName = "EAST SUBSTATION", targetKills = 8),
            BountyBeaconVoxelActor(1900f, 2900f, posZ = 0f, districtName = "SOUTH QUARANTINE", targetKills = 8),
            BountyBeaconVoxelActor(900f, 1900f, posZ = 0f, districtName = "WEST CYBER-GARDEN", targetKills = 8)
        )
        bountyBeacons.addAll(newBeacons.map { beacon ->
            val p = snapToWalkable(beacon.posX, beacon.posY)
            BountyBeaconVoxelActor(p.first, p.second, posZ = 0f, districtName = beacon.districtName, targetKills = beacon.targetKills)
        })

        // Register actors with actorManager
        runCatching {
            actorManager.add(serverRacks)
            actorManager.add(trees)
            actorManager.add(crates)
            actorManager.add(dataRelayTowers)
            actorManager.add(techBarricades)
            actorManager.add(explosiveBarrels)
            actorManager.add(overclockConduits)
            actorManager.add(laserFences)
            actorManager.add(bountyBeacons)
        }
    }

    fun handleGamepadButton(button: GamepadButton) {
        when (_sessionState.value) {
            GameSessionState.ATTRACT -> {
                when (button) {
                    GamepadButton.WEST -> selectArchetype(PlayerArchetype.CYBER_CODER)
                    GamepadButton.NORTH -> selectArchetype(PlayerArchetype.KOTLIN_BOT)
                    GamepadButton.EAST -> selectArchetype(PlayerArchetype.ARCH_DEV)
                    GamepadButton.START, GamepadButton.SOUTH -> startBriefing()
                    GamepadButton.SELECT -> startBriefing()
                    else -> {}
                }
            }
            GameSessionState.BRIEFING -> {
                when (button) {
                    GamepadButton.START, GamepadButton.SOUTH, GamepadButton.EAST -> dismissBriefing()
                    GamepadButton.DPAD_RIGHT, GamepadButton.RIGHT_SHOULDER -> nextBriefingSlide()
                    GamepadButton.DPAD_LEFT, GamepadButton.LEFT_SHOULDER -> prevBriefingSlide()
                    else -> {}
                }
            }
            GameSessionState.TRIVIA_MODAL -> {
                _perkSelectionOptions.value?.let { perks ->
                    when (button) {
                        GamepadButton.SOUTH -> if (perks.isNotEmpty()) selectVersionPerk(perks[0])
                        GamepadButton.EAST -> if (perks.size > 1) selectVersionPerk(perks[1])
                        GamepadButton.WEST -> if (perks.size > 2) selectVersionPerk(perks[2])
                        else -> {}
                    }
                    return
                }

                if (_triviaLockoutRemainingMs.value > 0) return

                when (button) {
                    GamepadButton.SOUTH -> submitTriviaAnswer(0)
                    GamepadButton.EAST -> submitTriviaAnswer(1)
                    GamepadButton.WEST -> submitTriviaAnswer(2)
                    GamepadButton.NORTH -> submitTriviaAnswer(3)
                    else -> {}
                }
            }
            GameSessionState.PLAYING -> {
                when (button) {
                    GamepadButton.START, GamepadButton.SELECT -> startBriefing(isPausedFromPlaying = true)
                    else -> {}
                }
            }
            else -> {}
        }
    }

    private fun spawnGems(count: Int) {
        val newGems = mutableListOf<GemVoxelActor>()
        val step = 100f
        val gridMin = 100f
        val walkableList = walkableCells.toList()
        for (i in 0 until count) {
            val cell = if (walkableList.isNotEmpty()) walkableList.random() else Pair(18, 18)
            val gx = (gridMin + cell.first * step + Random.nextInt(-35, 35)).coerceIn(120f, 3680f)
            val gy = (gridMin + cell.second * step + Random.nextInt(-35, 35)).coerceIn(120f, 3680f)
            val gz = cellHeights[cell] ?: 0f
            newGems.add(GemVoxelActor(gx, gy, posZ = gz))
        }
        gems.addAll(newGems)
        runCatching { actorManager.add(newGems) }
    }

    fun spawnSingleBug(type: BugType? = null) {
        val selectedType = type ?: selectProgressiveBugType()
        val step = 100f
        val gridMin = 100f
        val walkableList = walkableCells.filter { (gx, gy) ->
            val wx = gridMin + gx * step
            val wy = gridMin + gy * step
            distance(player.posX, player.posY, wx, wy) > 400f
        }
        val cell = if (walkableList.isNotEmpty()) walkableList.random() else Pair(5, 5)
        val bx = (gridMin + cell.first * step + Random.nextInt(-25, 25)).coerceIn(120f, 3680f)
        val by = (gridMin + cell.second * step + Random.nextInt(-25, 25)).coerceIn(120f, 3680f)
        val bz = cellHeights[cell] ?: 0f
        val bug = BugHazardActor(bx, by, posZ = bz, bugType = selectedType)
        bug.canMoveTo = ::isPointWalkable
        bugs.add(bug)
        runCatching { actorManager.add(bug) }
    }

    private fun spawnBugs(count: Int) {
        for (i in 0 until count) {
            spawnSingleBug()
        }
    }

    private fun emitKotlinCrestCuboids(time: Float): List<VoxelCuboid> {
        val list = mutableListOf<VoxelCuboid>()
        val cx = 1900f
        val cy = 1900f
        val hoverZ = 32f + sin(time * 3f) * 4f
        val pulse = (sin(time * 4f) * 0.5f + 0.5f)
        val alpha = 0.80f + 0.20f * pulse

        // Holographic Base Emitter Rings on CPU Core Floor (flush with flat walkable floor at Z = 0f)
        list.add(VoxelCuboid(cx, cy, -6f, 54f, 54f, 6f, Color(0xFF1E2838), isFloor = true))
        list.add(VoxelCuboid(cx, cy, -3f, 38f, 38f, 3f, Color(0xFF00E5FF).copy(alpha = alpha), isFloor = true))
        list.add(VoxelCuboid(cx, cy, -1f, 22f, 22f, 1f, Color(0xFFFFD600).copy(alpha = alpha), isFloor = true))

        // Vertical Hologram Projector Beam (semi-transparent, projecting upward from Z = 0f)
        list.add(VoxelCuboid(cx, cy, 0f, 10f, 10f, hoverZ, Color(0x337F52FF)))

        // Kotlin "K" Spine (Left Column, height 36 units)
        list.add(VoxelCuboid(cx - 10f, cy, hoverZ, 8f, 8f, 12f, Color(0xFF7F52FF).copy(alpha = alpha)))
        list.add(VoxelCuboid(cx - 10f, cy, hoverZ + 12f, 8f, 8f, 12f, Color(0xFF9C27B0).copy(alpha = alpha)))
        list.add(VoxelCuboid(cx - 10f, cy, hoverZ + 24f, 8f, 8f, 12f, Color(0xFFC711E1).copy(alpha = alpha)))

        // Center Junction
        list.add(VoxelCuboid(cx - 2f, cy, hoverZ + 14f, 8f, 8f, 8f, Color(0xFFAB47BC).copy(alpha = alpha)))

        // Upper Diagonal Arm (Orange/Amber gradient)
        list.add(VoxelCuboid(cx + 6f, cy, hoverZ + 22f, 8f, 8f, 8f, Color(0xFFFF5722).copy(alpha = alpha)))
        list.add(VoxelCuboid(cx + 14f, cy, hoverZ + 28f, 8f, 8f, 10f, Color(0xFFFF9800).copy(alpha = alpha)))

        // Lower Diagonal Arm (Violet/Blue gradient)
        list.add(VoxelCuboid(cx + 6f, cy, hoverZ + 8f, 8f, 8f, 8f, Color(0xFF7F52FF).copy(alpha = alpha)))
        list.add(VoxelCuboid(cx + 14f, cy, hoverZ, 8f, 8f, 8f, Color(0xFF3F51B5).copy(alpha = alpha)))

        // Crown Diode Beacon
        list.add(VoxelCuboid(cx + 2f, cy, hoverZ + 40f, 6f, 6f, 6f, Color(0xFFFFD600).copy(alpha = alpha)))

        return list
    }

    fun emitVoxelCuboids(): List<VoxelCuboid> = getAllVoxelCuboids()

    fun getAllVoxelCuboids(): List<VoxelCuboid> {
        val cuboids = mutableListOf<VoxelCuboid>()

        // 1. Procedural Base Floor Tiles
        cuboids.addAll(proceduralFloorTiles)

        // 2. Pre-baked Floor Traces (Micro-biomes, Highways, Ring Concourse)
        for (i in proceduralFloorTraces.indices) {
            cuboids.add(proceduralFloorTraces[i].cachedCuboid)
        }

        // 3. Floating Holographic Kotlin "K" Voxel Crest at Central CPU Core Plaza (1900f, 1900f)
        cuboids.addAll(emitKotlinCrestCuboids(arenaPulseTime))

        // 4. Procedural Boundary Walls
        cuboids.addAll(proceduralWalls)

        // Holographic Elevator Extraction Pad
        elevatorPad?.let { cuboids.addAll(it.emitVoxelCuboids()) }

        // 5. Data Relay Towers
        dataRelayTowers.forEach { cuboids.addAll(it.emitVoxelCuboids()) }

        // 6. Tech Barricades
        techBarricades.forEach { cuboids.addAll(it.emitVoxelCuboids()) }

        // 7. Server Racks
        serverRacks.forEach { cuboids.addAll(it.emitVoxelCuboids()) }

        // 4. Voxel Trees
        trees.forEach { cuboids.addAll(it.emitVoxelCuboids()) }

        // 5. Destructible Crates
        crates.forEach { cuboids.addAll(it.emitVoxelCuboids()) }

        // Explosive Byte Barrels
        explosiveBarrels.forEach { cuboids.addAll(it.emitVoxelCuboids()) }

        // Overclock Power Conduits
        overclockConduits.forEach { cuboids.addAll(it.emitVoxelCuboids()) }

        // Dynamic Laser Sweep Fences
        laserFences.forEach { cuboids.addAll(it.emitVoxelCuboids()) }

        // District Bounty Beacons
        bountyBeacons.forEach { cuboids.addAll(it.emitVoxelCuboids()) }

        // 6. Dropped Trivia Shards & Gun Drops
        triviaShards.forEach { cuboids.addAll(it.emitVoxelCuboids()) }
        gunDrops.forEach { cuboids.addAll(it.emitVoxelCuboids()) }

        // 7. Gems
        gems.forEach { cuboids.addAll(it.emitVoxelCuboids()) }

        // 8. Turrets
        turrets.forEach { cuboids.addAll(it.emitVoxelCuboids()) }

        // 9. Companion Drone
        drone?.let { cuboids.addAll(it.emitVoxelCuboids()) }

        // 10. Bugs
        bugs.forEach { cuboids.addAll(it.emitVoxelCuboids()) }

        // 11. Boss Titan
        boss?.let { b ->
            if (!b.isDefeated) {
                cuboids.addAll(b.emitVoxelCuboids())
            }
        }

        // 12. Projectiles
        projectiles.forEach { cuboids.addAll(it.emitVoxelCuboids()) }

        // 13. Shatter Debris Particles
        val activeParticles = synchronized(debrisParticles) { debrisParticles.toList() }
        activeParticles.forEach { p ->
            val alpha = (p.lifeMs.toFloat() / p.maxLifeMs.toFloat()).coerceIn(0f, 1f)
            cuboids.add(
                VoxelCuboid(
                    x = p.x,
                    y = p.y,
                    z = p.z,
                    sizeX = p.size,
                    sizeY = p.size,
                    sizeZ = p.size,
                    baseColor = p.color.copy(alpha = alpha)
                )
            )
        }

        // 14. Player
        cuboids.addAll(player.emitVoxelCuboids())

        val px = player.posX
        val py = player.posY

        // 15. Volumetric Cyber-Clouds along the Horizon Perimeter (masks chunk loading/unloading)
        val cloudClusters = 28
        val twoPi = 6.2831853f
        for (i in 0 until cloudClusters) {
            val angle = (i * twoPi / cloudClusters) + cloudAnimTime * (if (i % 2 == 0) 0.5f else -0.35f)
            val rad = 1950f + sin(i * 1.6f + cloudAnimTime * 1.5f) * 160f
            val cx = px + cos(angle) * rad
            val cy = py + sin(angle) * rad
            val cz = 14f + sin(i.toFloat() + cloudAnimTime) * 14f
            val size = 95f + (i % 4) * 24f
            val height = 28f + (i % 3) * 10f

            val cloudColor = when (i % 3) {
                0 -> Color(0x4400E5FF)
                1 -> Color(0x55E0F7FA)
                else -> Color(0x387C4DFF)
            }
            cuboids.add(
                VoxelCuboid(
                    x = cx,
                    y = cy,
                    z = cz,
                    sizeX = size,
                    sizeY = size,
                    sizeZ = height,
                    baseColor = cloudColor
                )
            )
        }

        // Cull cuboids outside camera visibility radius and apply distance-based cloud fog dissolve
        // Scale distance inversely with zoom so zoomed-out view reveals the entire map
        val viewScale = (0.52f / smoothZoom).coerceAtLeast(1.0f)
        val maxDist = 2400f * viewScale
        val fadeStart = 1800f * viewScale
        val maxDistSq = maxDist * maxDist
        val fadeStartSq = fadeStart * fadeStart

        val visibleFloor = ArrayList<VoxelCuboid>(proceduralFloorTiles.size + proceduralFloorTraces.size)
        val visibleDynamic = ArrayList<VoxelCuboid>(160)

        for (i in cuboids.indices) {
            val c = cuboids[i]
            val dx = c.x - px
            val dy = c.y - py
            val distSq = dx * dx + dy * dy
            if (distSq <= maxDistSq) {
                if (c.isFloor) {
                    visibleFloor.add(c)
                } else {
                    if (distSq > fadeStartSq) {
                        val dist = sqrt(distSq)
                        val fogFactor = ((maxDist - dist) / (maxDist - fadeStart)).coerceIn(0f, 1f)
                        val newAlpha = c.baseColor.alpha * fogFactor
                        visibleDynamic.add(
                            c.copy(
                                baseColor = c.baseColor.copy(alpha = newAlpha),
                                topColor = c.topColor.copy(alpha = newAlpha),
                                leftColor = c.leftColor.copy(alpha = newAlpha),
                                rightColor = c.rightColor.copy(alpha = newAlpha)
                            )
                        )
                    } else {
                        visibleDynamic.add(c)
                    }
                }
            }
        }

        visibleDynamic.sortBy { it.depth }
        val result = ArrayList<VoxelCuboid>(visibleFloor.size + visibleDynamic.size)
        result.addAll(visibleFloor)
        result.addAll(visibleDynamic)
        return result
    }

    fun getPlayerPosition(): Pair<Float, Float> = Pair(player.posX, player.posY)

    private fun distance(x1: Float, y1: Float, x2: Float, y2: Float): Float {
        val dx = x1 - x2
        val dy = y1 - y2
        return sqrt(dx * dx + dy * dy)
    }

    companion object {
        const val DEFAULT_ZOOM_MULTIPLIER = 1.95f
    }
}
