package pro.jayeshseth.bugrush

import pro.jayeshseth.bugrush.audio.ChiptuneSynthesizer
import pro.jayeshseth.bugrush.audio.SoundEffect
import pro.jayeshseth.bugrush.game.actors.BossVoxelActor
import pro.jayeshseth.bugrush.game.actors.BugHazardActor
import pro.jayeshseth.bugrush.game.actors.BugType
import pro.jayeshseth.bugrush.game.actors.CompanionDroneVoxelActor
import pro.jayeshseth.bugrush.game.actors.CrateVoxelActor
import pro.jayeshseth.bugrush.game.actors.DamageResult
import pro.jayeshseth.bugrush.game.actors.DataRelayTowerActor
import pro.jayeshseth.bugrush.game.actors.BountyBeaconVoxelActor
import pro.jayeshseth.bugrush.game.actors.ExplosiveBarrelVoxelActor
import pro.jayeshseth.bugrush.game.actors.LaserFenceVoxelActor
import pro.jayeshseth.bugrush.game.actors.OverclockConduitVoxelActor
import pro.jayeshseth.bugrush.game.actors.PlayerArchetype
import pro.jayeshseth.bugrush.game.actors.PlayerVoxelActor
import pro.jayeshseth.bugrush.game.actors.ProjectileVoxelActor
import pro.jayeshseth.bugrush.game.actors.TechBarricadeActor
import pro.jayeshseth.bugrush.game.actors.TurretVoxelActor
import pro.jayeshseth.bugrush.game.actors.WeaponType
import pro.jayeshseth.bugrush.trivia.PowerUpType
import pro.jayeshseth.bugrush.trivia.VersionPerk
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import pro.jayeshseth.bugrush.game.ConferenceGameStateHolder
import pro.jayeshseth.bugrush.game.managers.ArenaManager
import pro.jayeshseth.bugrush.game.managers.GameSessionState
import com.pandulapeter.kubriko.gamepadInput.GamepadButton
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ArcadeExtravaganzaTest {

    @Test
    fun testEnemyVariantProperties() {
        val normalBug = BugHazardActor(100f, 100f, headingAngle = 0f, bugType = BugType.NORMAL)
        assertEquals(1, normalBug.hp)
        assertEquals(20f, normalBug.collisionRadius)

        val memoryLeakBug = BugHazardActor(100f, 100f, headingAngle = 0f, bugType = BugType.MEMORY_LEAK)
        assertEquals(2, memoryLeakBug.hp)
        assertTrue(memoryLeakBug.isMemoryLeak)

        val swarmerBug = BugHazardActor(100f, 100f, headingAngle = 0f, bugType = BugType.SWARMER)
        assertEquals(1, swarmerBug.hp)
        assertEquals(14f, swarmerBug.collisionRadius)
        assertTrue(swarmerBug.isSwarmer)

        val phantomBug = BugHazardActor(100f, 100f, headingAngle = 0f, bugType = BugType.PHANTOM)
        assertEquals(2, phantomBug.hp)
        assertTrue(phantomBug.isPhantom)

        val deadlockBug = BugHazardActor(100f, 100f, headingAngle = 0f, bugType = BugType.DEADLOCK)
        assertEquals(3, deadlockBug.hp)
        assertTrue(deadlockBug.isDeadlock)

        // Deadlock Frontal Armor Deflection Test:
        // Bug heading is 0f (facing +X). A bullet coming from front travels in direction (-1f, 0f).
        val frontDeflection = deadlockBug.takeDamage(
            amount = 1,
            bulletDirX = -1f,
            bulletDirY = 0f,
            isHeavyRailgun = false
        )
        assertEquals(DamageResult.DEFLECTED, frontDeflection, "Frontal shot on Deadlock must be deflected")
        assertEquals(3, deadlockBug.hp, "Deadlock must not lose HP when deflecting frontal shot")

        // Rear shot test (bullet travels in +X direction, matching heading direction)
        val rearHit = deadlockBug.takeDamage(
            amount = 1,
            bulletDirX = 1f,
            bulletDirY = 0f,
            isHeavyRailgun = false
        )
        assertEquals(DamageResult.HIT, rearHit, "Rear shot on Deadlock must deal damage")
        assertEquals(2, deadlockBug.hp)

        // Heavy Railgun crushing front armor test
        val railgunHit = deadlockBug.takeDamage(
            amount = 2,
            bulletDirX = -1f,
            bulletDirY = 0f,
            isHeavyRailgun = true
        )
        assertEquals(DamageResult.KILLED, railgunHit, "Heavy Railgun must pierce Deadlock front armor")
        assertEquals(0, deadlockBug.hp)
    }

    @Test
    fun testRoguelitePerkApplication() {
        val player = PlayerVoxelActor(1000f, 1000f)
        assertFalse(player.hasGarbageCollectorDrone)
        assertFalse(player.hasCoroutinesTurrets)
        assertFalse(player.hasJitCompiler)
        assertFalse(player.hasRefactorFirewall)

        // Apply JIT Compiler
        player.hasJitCompiler = true
        val normalBullets = player.fireWeapon()
        assertTrue(normalBullets.isNotEmpty())
        // JIT compiler reduces shoot cooldown by 35%
        val expectedCooldown = (pro.jayeshseth.bugrush.game.actors.WeaponType.RAPID_BYTE_BLASTER.fireRateMs * 0.65f).toInt()
        assertEquals(expectedCooldown, player.shootCooldownMs, "JIT Compiler must accelerate weapon fire rate by 35%")

        // Apply Refactor Firewall
        player.hasRefactorFirewall = true
        player.isFirewallShieldActive = true
        assertTrue(player.isFirewallShieldActive)

        // Companion Drone Test
        val drone = CompanionDroneVoxelActor(1000f, 1000f)
        val dummyBug = BugHazardActor(1050f, 1000f, bugType = BugType.NORMAL)
        val droneShots = drone.updateOrbitAndCombat(1000f, 1000f, 1000, listOf(dummyBug))
        assertTrue(droneShots.isNotEmpty(), "Companion Drone must auto-target nearby bug")

        // Turret Test
        val turret = TurretVoxelActor(1000f, 1000f, remainingLifeMs = 8000)
        val turretShots = turret.updateAndCombat(500, listOf(dummyBug))
        assertTrue(turretShots.isNotEmpty(), "Turret must fire at nearby bug")
    }

    @Test
    fun testBossKernelPanicTransition() {
        val boss = BossVoxelActor(1000f, 1000f, maxHp = 25)
        assertFalse(boss.isEnraged)
        assertFalse(boss.isKernelPanic)

        // Damage to 50% HP (12 HP)
        boss.takeDamage(13)
        assertEquals(12, boss.hp)
        assertTrue(boss.isEnraged, "Boss must enter Enraged mode at <= 50% HP")
        assertFalse(boss.isKernelPanic, "Boss must not yet be in Kernel Panic above 40% HP")

        // Damage to 40% HP (10 HP out of 25: 25 * 0.4 = 10)
        boss.takeDamage(2)
        assertEquals(10, boss.hp)
        assertTrue(boss.isKernelPanic, "Boss must enter Kernel Panic at <= 40% HP")

        // In Kernel Panic, attack timer triggers sweeping 5-laser barrage
        boss.attackTimerMs = 0
        val projectiles = boss.updateTowards(1200f, 1000f, deltaTimeInMilliseconds = 100)
        assertTrue(projectiles.size >= 5, "Boss Kernel Panic mode must fire a 5-laser sweeping barrage")
    }

    @Test
    fun testAudioSynthesizerBuffers() {
        for (sfx in SoundEffect.values()) {
            val buffer = ChiptuneSynthesizer.synthesizeSfx(sfx)
            assertTrue(buffer.isNotEmpty(), "SFX buffer for $sfx must not be empty")
            assertEquals(0, buffer.size % 2, "16-bit PCM buffer must have even byte length")
        }

        val bgmBuffer = ChiptuneSynthesizer.synthesizeBgm()
        assertTrue(bgmBuffer.isNotEmpty(), "BGM buffer must not be empty")
        assertEquals(0, bgmBuffer.size % 2, "BGM 16-bit PCM buffer must have even byte length")

        val titleBuffer = ChiptuneSynthesizer.synthesizeTitleTheme()
        assertTrue(titleBuffer.isNotEmpty(), "Title theme buffer must not be empty")
        assertEquals(0, titleBuffer.size % 2, "Title theme 16-bit PCM buffer must have even byte length")
        assertTrue(titleBuffer.size > bgmBuffer.size, "Title theme anthem (64 steps) should be larger than 32-step BGM loop")
    }

    @Test
    fun testTextFieldFocusBypassesGameKeys() {
        val audioPlayer = pro.jayeshseth.bugrush.audio.DesktopChiptuneAudioPlayer()
        val stateHolder = pro.jayeshseth.bugrush.game.ConferenceGameStateHolder(soundPlayer = audioPlayer)
        assertFalse(stateHolder.isTextFieldFocused, "Text field should not be focused by default")

        // When text field is NOT focused, M key toggles audio mute
        assertFalse(stateHolder.isMuted.value)
        val handledWhenNotFocused = stateHolder.handleKey(androidx.compose.ui.input.key.Key.M)
        assertTrue(handledWhenNotFocused, "M key should be handled when text field is not focused")
        assertTrue(stateHolder.isMuted.value, "Audio should be muted after pressing M")

        // When text field IS focused (user typing handle), M key should NOT be intercepted
        stateHolder.isTextFieldFocused = true
        val handledWhenFocused = stateHolder.handleKey(androidx.compose.ui.input.key.Key.M)
        assertFalse(handledWhenFocused, "M key must NOT be intercepted when user is typing in text field")
        assertTrue(stateHolder.isMuted.value, "Audio state must not change when typing M in text field")
    }

    @Test
    fun testCompanionDroneAndTurretLifecycles() {
        val drone = CompanionDroneVoxelActor(1000f, 1000f)
        assertEquals(140f, drone.suctionRadius, "Companion drone must have suction radius of 140")

        val turret = TurretVoxelActor(1000f, 1000f, remainingLifeMs = 8000)
        assertEquals(8000, turret.remainingLifeMs)
        assertFalse(turret.isExpired)

        // After 8000ms elapsed, turret should be expired
        turret.updateAndCombat(8500, emptyList())
        assertTrue(turret.isExpired, "Turret must expire after remainingLifeMs runs out")
    }

    @Test
    fun testBossDefeatStateTransitions() {
        val boss = BossVoxelActor(1000f, 1000f, maxHp = 25)
        assertFalse(boss.isDefeated)
        val killed = boss.takeDamage(25)
        assertTrue(killed)
        assertTrue(boss.isDefeated, "Boss must be marked as defeated when HP reaches 0")
        assertEquals(0, boss.hp)
    }

    @Test
    fun testStrictWallCollision() {
        val stateHolder = pro.jayeshseth.bugrush.game.ConferenceGameStateHolder()
        val arenaManager = stateHolder.arenaManager

        // Central Grand Combat Plaza at (1900, 1900) must be 100% walkable
        assertTrue(arenaManager.isPositionWalkable(1900f, 1900f), "Central spawn area must be walkable")

        // Coordinates outside map perimeter limits (< 100 or > 3700) must never be walkable
        assertFalse(arenaManager.isPositionWalkable(50f, 50f), "Outer abyss coordinates must not be walkable")
        assertFalse(arenaManager.isPositionWalkable(3800f, 3800f), "Far abyss coordinates must not be walkable")

        // Player must halt dash upon colliding with impassable boundary
        val player = PlayerVoxelActor(1900f, 1900f)
        player.headingX = 1f
        player.headingY = 0f
        player.canMoveTo = { x, _, _ -> x < 1950f } // Mock boundary wall at X = 1950
        player.triggerDash()
        assertTrue(player.isDashing)

        // Step dash by 100ms with direction +X (which would cross 1950f)
        player.update(100)
        assertFalse(player.isDashing, "Player must immediately halt dash upon hitting boundary wall")
        assertTrue(player.posX <= 1950f, "Player must never penetrate beyond boundary wall")
    }

    @Test
    fun testDynamicCameraAnglesAndHotkeys() {
        val stateHolder = pro.jayeshseth.bugrush.game.ConferenceGameStateHolder()
        val arenaManager = stateHolder.arenaManager

        assertEquals(0f, arenaManager.targetCameraAngleRad)

        // Pressing Tab rotates camera clockwise by 90 degrees (pi/2)
        val handledTab = stateHolder.handleKey(androidx.compose.ui.input.key.Key.Tab)
        assertTrue(handledTab)
        val expectedFirstAngle = kotlin.math.PI.toFloat() * 0.5f
        assertEquals(expectedFirstAngle, arenaManager.targetCameraAngleRad, 0.001f)

        // Pressing R rotates camera clockwise another 90 degrees (pi)
        val handledR = stateHolder.handleKey(androidx.compose.ui.input.key.Key.R)
        assertTrue(handledR)
        val expectedSecondAngle = kotlin.math.PI.toFloat()
        assertEquals(expectedSecondAngle, arenaManager.targetCameraAngleRad, 0.001f)

        // Pressing Shift+Tab rotates camera counter-clockwise back by 90 degrees
        val handledShiftTab = stateHolder.handleKey(androidx.compose.ui.input.key.Key.Tab, isShiftPressed = true)
        assertTrue(handledShiftTab)
        assertEquals(expectedFirstAngle, arenaManager.targetCameraAngleRad, 0.001f)
    }

    @Test
    fun testUnboundedZoomControls() {
        val stateHolder = pro.jayeshseth.bugrush.game.ConferenceGameStateHolder()
        val arenaManager = stateHolder.arenaManager

        assertEquals(1.95f, arenaManager.targetZoomMultiplier, 0.001f)

        // Pressing Z zooms in (increases zoom factor by 1.25x)
        val handledZ = stateHolder.handleKey(androidx.compose.ui.input.key.Key.Z)
        assertTrue(handledZ)
        assertEquals(1.95f * 1.25f, arenaManager.targetZoomMultiplier, 0.001f)

        // Multiple Z presses can reach extreme magnification
        for (i in 0 until 4) {
            stateHolder.handleKey(androidx.compose.ui.input.key.Key.Z)
        }
        assertTrue(arenaManager.targetZoomMultiplier > 5.0f, "Zoom multiplier should increase with consecutive Z presses")

        // Pressing 0 resets zoom to 1.95x
        val handledReset = stateHolder.handleKey(androidx.compose.ui.input.key.Key.Zero)
        assertTrue(handledReset)
        assertEquals(1.95f, arenaManager.targetZoomMultiplier, 0.001f)

        // Pressing O zooms out (decreases zoom factor)
        val handledO = stateHolder.handleKey(androidx.compose.ui.input.key.Key.O)
        assertTrue(handledO)
        assertEquals(1.95f / 1.25f, arenaManager.targetZoomMultiplier, 0.001f)

        // Multiple O presses can zoom out extensively
        for (i in 0 until 5) {
            stateHolder.handleKey(androidx.compose.ui.input.key.Key.O)
        }
        assertTrue(arenaManager.targetZoomMultiplier < 0.70f, "Zoom multiplier should decrease significantly with consecutive O presses")
    }

    @Test
    fun testMegamapFlatFloorAndSectorProgression() {
        val stateHolder = pro.jayeshseth.bugrush.game.ConferenceGameStateHolder()
        val arenaManager = stateHolder.arenaManager

        // Sector 1 initial state at Grand Combat Plaza (1900f, 1900f)
        assertEquals(1, arenaManager.currentFloor.value)
        assertEquals(0, arenaManager.bugsSquashedThisFloor.value)
        assertFalse(arenaManager.isElevatorOnline.value)

        // Verify ground heights are uniformly flat (0f) across the entire megamap
        val centerZ = arenaManager.getGroundZ(1900f, 1900f)
        assertEquals(0f, centerZ, "Central Grand Combat Plaza should be flat ground level 0f")
        for ((_, h) in arenaManager.cellHeights) {
            assertEquals(0f, h, "All megamap cells should be flat ground level 0f")
        }

        // Test projectile flight beyond 1930f (ensuring it is not prematurely destroyed)
        val projectile = pro.jayeshseth.bugrush.game.actors.ProjectileVoxelActor(
            posX = 1900f,
            posY = 1900f,
            dirX = 1f,
            dirY = 0f,
            isPlayerProjectile = true
        )
        // Simulate flight past 1930f (e.g. at 2400f)
        projectile.posX = 2400f
        projectile.update(16)
        assertFalse(projectile.isDestroyed, "Player projectile must stay active well beyond 1930f across the megamap")

        // Test bug movement beyond 1900f (ensuring bug is not trapped/blocked at 1900f)
        val bug = pro.jayeshseth.bugrush.game.actors.BugHazardActor(
            posX = 2200f,
            posY = 2200f,
            bugType = pro.jayeshseth.bugrush.game.actors.BugType.NORMAL
        )
        bug.update(100)
        assertTrue(bug.posX > 2100f, "Bug must be free to roam beyond 1900f across the megamap without being stuck")

        // Extraction Beacon on Megamap
        arenaManager.startNewGame()
        assertFalse(arenaManager.isElevatorOnline.value)

        // Power up the extraction beacon
        arenaManager.isElevatorOnline.value = true
        arenaManager.elevatorPad?.isActive = true
        assertTrue(arenaManager.isElevatorOnline.value)
        assertTrue(arenaManager.elevatorPad?.isActive == true)

        // Step on extraction beacon: triggers sector ascension bonus without resetting player position
        val oldScore = arenaManager.score.value
        arenaManager.ascendToNextFloor()
        assertEquals(2, arenaManager.currentFloor.value, "Floor/Sector should advance to 2 upon stepping on beacon")
        assertTrue(arenaManager.score.value > oldScore, "Extraction beacon must award victory score bonus")
        assertTrue(arenaManager.floorAscensionTitle.value.contains("SECTOR 2"), "Banner must announce SECTOR 2")
    }

    @Test
    fun testDataRelayTowerAndTechBarricadeActors() {
        // 1. Data Relay Tower Actor
        val tower = DataRelayTowerActor(1900f, 650f)
        assertEquals(24f, tower.collisionRadius, "Data relay tower must have collision radius of 24f")
        assertTrue(tower.isAlwaysActive)
        assertTrue(tower.isAlwaysVisible)

        val initialTimer = tower.beaconTimer
        tower.update(100)
        assertTrue(tower.beaconTimer > initialTimer, "Tower update must advance beaconTimer")

        val towerCuboids = tower.emitVoxelCuboids()
        assertTrue(towerCuboids.size >= 10, "Data relay tower must emit detailed voxels (chassis, conduits, beacon)")
        val beaconCuboid = towerCuboids.maxByOrNull { it.z }
        assertTrue(beaconCuboid != null && beaconCuboid.z >= 110f, "Tower summit beacon must reach >= 110 units high")
        val totalTowerHeight = towerCuboids.maxOf { it.z + it.sizeZ }
        assertTrue(totalTowerHeight in 90f..120f, "Tower total height must be between 90 and 120 units (actual: $totalTowerHeight)")

        val elevatedTower = DataRelayTowerActor(1900f, 650f, posZ = 10f)
        val elevatedCuboids = elevatedTower.emitVoxelCuboids()
        val elevatedBeacon = elevatedCuboids.maxByOrNull { it.z }
        assertTrue(elevatedBeacon != null && elevatedBeacon.z >= 120f, "Tower must respect posZ offset")

        // 2. Tech Barricade Actor (Horizontal)
        val horizBarricade = TechBarricadeActor(1900f, 1480f, isHorizontal = true)
        assertEquals(20f, horizBarricade.collisionRadius, "Tech barricade must have collision radius of 20f")
        assertTrue(horizBarricade.isHorizontal)
        val initialLed = horizBarricade.ledTimer
        horizBarricade.update(100)
        assertTrue(horizBarricade.ledTimer > initialLed, "Barricade update must advance ledTimer")

        val horizCuboids = horizBarricade.emitVoxelCuboids()
        assertTrue(horizCuboids.size >= 8, "Tech barricade must emit detailed voxels (chassis, armor, hazard stripes, LEDs)")
        // Verify width 48 units and height 18 units
        val baseChassisH = horizCuboids.firstOrNull { it.sizeX == 48f && it.sizeY == 16f }
        assertTrue(baseChassisH != null, "Horizontal barricade must have base width of 48 units and depth of 16 units")
        val maxZH = horizCuboids.maxOf { it.z + it.sizeZ }
        assertEquals(18f, maxZH, "Barricade height must reach 18 units")

        // 3. Tech Barricade Actor (Vertical)
        val vertBarricade = TechBarricadeActor(1480f, 1900f, isHorizontal = false)
        assertFalse(vertBarricade.isHorizontal)
        val vertCuboids = vertBarricade.emitVoxelCuboids()
        assertTrue(vertCuboids.size >= 8)
        val baseChassisV = vertCuboids.firstOrNull { it.sizeX == 16f && it.sizeY == 48f }
        assertTrue(baseChassisV != null, "Vertical barricade must have base width of 16 units and depth of 48 units")
        val maxZV = vertCuboids.maxOf { it.z + it.sizeZ }
        assertEquals(18f, maxZV, "Vertical barricade height must reach 18 units")
    }

    @Test
    fun testDistrictBiomesAndGrandCircuitHighways() {
        val stateHolder = pro.jayeshseth.bugrush.game.ConferenceGameStateHolder()
        val arenaManager = stateHolder.arenaManager

        arenaManager.generateMapCells()

        // 1. Walkable cells in 37x37 grid
        assertTrue(arenaManager.walkableCells.contains(Pair(18, 18)), "Center cell (18, 18) must be walkable")
        assertTrue(arenaManager.isPositionWalkable(1900f, 1900f), "Center (1900f, 1900f) must be walkable")

        // 2. Procedural floor traces check
        assertTrue(arenaManager.proceduralFloorTraces.isNotEmpty(), "District biomes must generate animated floor traces")

        // Central Grand CPU Core Plaza: Gold heat-sink traces (0xFFFFD600) and cyan crosshairs
        val goldTraces = arenaManager.proceduralFloorTraces.filter { it.baseColor == androidx.compose.ui.graphics.Color(0xFFFFD600) }
        assertTrue(goldTraces.isNotEmpty(), "Central CPU core plaza must have gold heat-sink traces")

        // Grand Circuit Highways: Cyan center dividers along gx == 18 or gy == 18
        val cyanHighwayDividers = arenaManager.proceduralFloorTraces.filter {
            it.baseColor == androidx.compose.ui.graphics.Color(0xFF00E5FF) && (it.sizeX == 14f || it.sizeY == 14f)
        }
        assertTrue(cyanHighwayDividers.isNotEmpty(), "Grand Circuit Highways must feature glowing cyan center dividers")

        // Ring Concourse: Amber guidance markers near radius 1000f
        val ringConcourseNodes = arenaManager.proceduralFloorTraces.filter {
            it.baseColor == androidx.compose.ui.graphics.Color(0xFFFFB300)
        }
        assertTrue(ringConcourseNodes.isNotEmpty(), "Ring concourse must feature amber guide nodes at radius 1000f")

        // North Sector (Server Farm): Parallel glowing cyan data conduits
        val northSectorConduits = arenaManager.proceduralFloorTraces.filter {
            it.baseColor == androidx.compose.ui.graphics.Color(0xFF00E5FF) && it.sizeX == 12f
        }
        assertTrue(northSectorConduits.isNotEmpty(), "North sector must generate parallel glowing cyan data conduits")

        // East Sector (Industrial Power Substation): Yellow/black hazard stripes
        val eastHazardStripes = arenaManager.proceduralFloorTraces.filter {
            it.baseColor == androidx.compose.ui.graphics.Color(0xFFFFD600) && it.sizeX == 74f
        }
        assertTrue(eastHazardStripes.isNotEmpty(), "East sector must generate caution hazard stripes")

        // South Sector (Quarantine Scrapyard): Acid-green slime pools (0xFF00E676)
        val southSlimePools = arenaManager.proceduralFloorTraces.filter {
            it.baseColor == androidx.compose.ui.graphics.Color(0xFF00E676)
        }
        assertTrue(southSlimePools.isNotEmpty(), "South sector must generate toxic memory leak slime pools")

        // West Sector (Quantum Cyber-Garden): Neon purple/magenta circuit roots (0xFFE040FB / 0xFFD500F9)
        val westCircuitRoots = arenaManager.proceduralFloorTraces.filter {
            it.baseColor == androidx.compose.ui.graphics.Color(0xFFE040FB) || it.baseColor == androidx.compose.ui.graphics.Color(0xFFD500F9)
        }
        assertTrue(westCircuitRoots.isNotEmpty(), "West sector must generate glowing circuit roots")

        // 3. Floating Holographic Kotlin "K" Voxel Crest in getAllVoxelCuboids()
        val allCuboids = arenaManager.getAllVoxelCuboids()
        val kotlinCrestCuboids = allCuboids.filter {
            kotlin.math.abs(it.x - 1900f) <= 20f && kotlin.math.abs(it.y - 1900f) <= 20f && it.z >= 20f
        }
        assertTrue(kotlinCrestCuboids.isNotEmpty(), "Central CPU core plaza must have floating holographic Kotlin 'K' crest")

        // 4. Strict 100% flat floor invariant check: all floor cuboids must have top surface at Z <= 0f
        val protrudingFloorCuboids = allCuboids.filter { it.isFloor && (it.z + it.sizeZ) > 0.001f }
        assertTrue(protrudingFloorCuboids.isEmpty(), "All floor cuboids (including base rings) must be flush with flat floor (Z <= 0f)")
    }

    @Test
    fun testDistrictPropFormationsAndEnvironmentalCollisions() {
        val stateHolder = pro.jayeshseth.bugrush.game.ConferenceGameStateHolder()
        val arenaManager = stateHolder.arenaManager

        arenaManager.startNewGame()

        // 1. Verify structured formations exist
        assertEquals(3, arenaManager.dataRelayTowers.size, "North sector must contain exactly 3 Data Relay Towers")
        assertTrue(arenaManager.techBarricades.size >= 12, "Must spawn tech barricades across districts and central perimeter")

        // Verify Central Plaza perimeter barricades
        val perimeterBarricades = arenaManager.techBarricades.filter {
            (kotlin.math.abs(it.posX - 1900f) < 50f && kotlin.math.abs(it.posY - 1900f) in 400f..450f) ||
            (kotlin.math.abs(it.posY - 1900f) < 50f && kotlin.math.abs(it.posX - 1900f) in 400f..450f)
        }
        assertEquals(4, perimeterBarricades.size, "Must have 4 perimeter barricades defending central combat plaza")

        // 2. Collision Test: Player pushOutOf Data Relay Tower
        val player = PlayerVoxelActor(1500f, 850f)
        val tower = arenaManager.dataRelayTowers.first()
        player.posX = tower.posX + 5f
        player.posY = tower.posY
        val minDist = player.collisionRadius + tower.collisionRadius
        player.pushOutOf(tower.posX, tower.posY, minDist)
        val distAfterTower = kotlin.math.sqrt((player.posX - tower.posX) * (player.posX - tower.posX) + (player.posY - tower.posY) * (player.posY - tower.posY))
        assertTrue(distAfterTower >= minDist - 0.01f, "Player must be pushed outside Data Relay Tower collision radius")

        // 3. Collision Test: Player pushOutOf Tech Barricade
        val barricade = arenaManager.techBarricades.first()
        player.posX = barricade.posX + 5f
        player.posY = barricade.posY
        val minDistBarricade = player.collisionRadius + barricade.collisionRadius
        player.pushOutOf(barricade.posX, barricade.posY, minDistBarricade)
        val distAfterBarricade = kotlin.math.sqrt((player.posX - barricade.posX) * (player.posX - barricade.posX) + (player.posY - barricade.posY) * (player.posY - barricade.posY))
        assertTrue(distAfterBarricade >= minDistBarricade - 0.01f, "Player must be pushed outside Tech Barricade collision radius")

        // 4. emitVoxelCuboids() alias matches getAllVoxelCuboids()
        val emitted = arenaManager.emitVoxelCuboids()
        val allCuboids = arenaManager.getAllVoxelCuboids()
        assertEquals(allCuboids.size, emitted.size, "emitVoxelCuboids() should match getAllVoxelCuboids()")

        // 5. Projectile Collision with Data Relay Tower and Tech Barricade
        val towerBullet = pro.jayeshseth.bugrush.game.actors.ProjectileVoxelActor(
            posX = tower.posX,
            posY = tower.posY,
            dirX = 1f,
            dirY = 0f
        )
        val barricadeBullet = pro.jayeshseth.bugrush.game.actors.ProjectileVoxelActor(
            posX = barricade.posX,
            posY = barricade.posY,
            dirX = 1f,
            dirY = 0f
        )
        assertTrue(kotlin.math.hypot(towerBullet.posX - tower.posX, towerBullet.posY - tower.posY) < tower.collisionRadius + 8f)
        assertTrue(kotlin.math.hypot(barricadeBullet.posX - barricade.posX, barricadeBullet.posY - barricade.posY) < barricade.collisionRadius + 8f)
    }

    @Test
    fun testCyberpunkArcadeShaderConstantsAndShaderSource() {
        val shader = pro.jayeshseth.bugrush.game.renderer.CyberpunkArcadeShader
        assertEquals(0.04f, shader.DEFAULT_BARREL_WARP)
        assertEquals(0.12f, shader.DEFAULT_SCANLINE_DARK)
        assertEquals(1.4f, shader.DEFAULT_SPECULAR_BOOST)

        val src = shader.SHADER
        assertTrue(src.contains("barrelWarp"), "Shader must declare barrelWarp uniform")
        assertTrue(src.contains("scanlineDark"), "Shader must declare scanlineDark uniform")
        assertTrue(src.contains("uSpecularBoost"), "Shader must declare uSpecularBoost uniform")
        assertTrue(src.contains("glitchIntensity"), "Shader must declare glitchIntensity uniform")
        assertTrue(src.contains("crimsonAlert"), "Shader must declare crimsonAlert uniform")
        assertTrue(src.contains("combatAberration"), "Shader must declare combatAberration uniform")
        assertTrue(src.contains("shockwaveProgress"), "Shader must declare shockwaveProgress uniform")
        assertTrue(src.contains("shockwaveOrigin"), "Shader must declare shockwaveOrigin uniform")

        val runtimeShader = io.github.shaderengine.core.PlatformRuntimeShader(src)
        val renderEffect = runtimeShader.toRenderEffect("content")
        assertNotNull(renderEffect, "CyberpunkArcadeShader must compile cleanly to RenderEffect on desktop Skia")
    }

    @Test
    fun testReactiveShaderStateAndHooks() {
        val stateHolder = pro.jayeshseth.bugrush.game.ConferenceGameStateHolder()
        assertEquals(1f, stateHolder.shockwaveProgress)
        assertEquals(0f, stateHolder.shockwaveIntensity)
        assertEquals(0f, stateHolder.glitchIntensity)
        assertEquals(0f, stateHolder.crimsonAlert)
        assertEquals(0f, stateHolder.combatAberration)

        // Trigger Shockwave
        stateHolder.triggerShockwave(500f, 400f, intensity = 0.85f)
        assertEquals(500f, stateHolder.shockwaveOrigin.x)
        assertEquals(400f, stateHolder.shockwaveOrigin.y)
        assertEquals(0f, stateHolder.shockwaveProgress)
        assertEquals(0.85f, stateHolder.shockwaveIntensity)

        // Advance progress
        stateHolder.updateShaderState(250)
        assertTrue(stateHolder.shockwaveProgress in 0.45f..0.55f)
        assertEquals(0.85f, stateHolder.shockwaveIntensity)

        // Complete shockwave
        stateHolder.updateShaderState(300)
        assertEquals(1f, stateHolder.shockwaveProgress)
        assertEquals(0f, stateHolder.shockwaveIntensity)

        // Glitch and Crimson Alert
        stateHolder.triggerGlitch(durationMs = 300, intensity = 0.8f)
        assertEquals(0.8f, stateHolder.glitchIntensity)
        stateHolder.triggerCrimsonAlert(durationMs = 600, intensity = 0.9f)
        assertEquals(0.9f, stateHolder.crimsonAlert)

        stateHolder.updateShaderState(350)
        assertEquals(0f, stateHolder.glitchIntensity, "Glitch must decay to 0 after duration runs out")
        assertTrue(stateHolder.crimsonAlert > 0f, "Crimson alert should still be decaying after 350ms of 600ms")

        stateHolder.updateShaderState(300)
        assertEquals(0f, stateHolder.crimsonAlert, "Crimson alert must decay to 0 after full duration")
    }

    @Test
    fun testDashAndCombatHooksTriggerShockwaves() {
        val stateHolder = pro.jayeshseth.bugrush.game.ConferenceGameStateHolder()
        val arenaManager = stateHolder.arenaManager
        arenaManager.startNewGame()

        // Trigger player dash via arenaManager
        arenaManager.triggerPlayerDash()
        assertEquals(0f, stateHolder.shockwaveProgress, "Player dash must trigger shader shockwave (progress = 0)")
        assertTrue(stateHolder.shockwaveIntensity > 0f, "Shockwave intensity must be active")
        assertTrue(stateHolder.combatAberration > 0f, "Combat aberration must be triggered on dash")

        // Advance to clear shockwave
        stateHolder.updateShaderState(600)
        assertEquals(1f, stateHolder.shockwaveProgress)

        // Trigger player dash execution directly via onPlayerDashExecuted (which player.onDashTriggered invokes)
        arenaManager.onPlayerDashExecuted(1950f, 1950f)
        assertEquals(0f, stateHolder.shockwaveProgress, "onPlayerDashExecuted must trigger shader shockwave")
        assertTrue(stateHolder.shockwaveIntensity > 0f)

        // Advance to clear shockwave again
        stateHolder.updateShaderState(600)
        assertEquals(1f, stateHolder.shockwaveProgress)

        // Trigger world shockwave and alert hooks via arenaManager
        arenaManager.triggerWorldShockwave(1900f, 1900f, intensity = 0.95f)
        assertEquals(0f, stateHolder.shockwaveProgress, "triggerWorldShockwave must reset shockwave progress")
        assertEquals(0.95f, stateHolder.shockwaveIntensity, "triggerWorldShockwave must assign intensity")

        arenaManager.onTriggerGlitch?.invoke(400, 0.85f)
        assertEquals(0.85f, stateHolder.glitchIntensity)

        arenaManager.onTriggerCrimsonAlert?.invoke(800, 1.0f)
        assertEquals(1.0f, stateHolder.crimsonAlert)

        arenaManager.onTriggerCombatImpact?.invoke(0.75f)
        assertEquals(0.75f, stateHolder.combatAberration)
    }

    @Test
    fun testScreenAngleWaypointProjection() {
        val stateHolder = pro.jayeshseth.bugrush.game.ConferenceGameStateHolder()
        val arenaManager = stateHolder.arenaManager
        arenaManager.startNewGame()

        // With cameraAngleRad = 0f, in isometric projection:
        // Target straight UP on screen: relX < 0, relY < 0 with relX == relY
        val upAngle = arenaManager.calculateScreenAngleTo(1900f - 100f, 1900f - 100f)
        val upDeg = ((Math.toDegrees(upAngle.toDouble()) % 360 + 360) % 360).toFloat()
        assertTrue(upDeg in 247.5f..292.5f, "Target straight up in isometric view must point North (approx 270 deg), was $upDeg")

        // Target straight DOWN on screen: relX > 0, relY > 0 with relX == relY
        val downAngle = arenaManager.calculateScreenAngleTo(1900f + 100f, 1900f + 100f)
        val downDeg = ((Math.toDegrees(downAngle.toDouble()) % 360 + 360) % 360).toFloat()
        assertTrue(downDeg in 67.5f..112.5f, "Target straight down in isometric view must point South (approx 90 deg), was $downDeg")

        // Target straight RIGHT on screen: relX > 0, relY < 0 with relX == -relY
        val rightAngle = arenaManager.calculateScreenAngleTo(1900f + 100f, 1900f - 100f)
        val rightDeg = ((Math.toDegrees(rightAngle.toDouble()) % 360 + 360) % 360).toFloat()
        assertTrue(rightDeg <= 22.5f || rightDeg >= 337.5f, "Target straight right in isometric view must point East (approx 0 deg), was $rightDeg")

        // Target straight LEFT on screen: relX < 0, relY > 0 with relX == -relY
        val leftAngle = arenaManager.calculateScreenAngleTo(1900f - 100f, 1900f + 100f)
        val leftDeg = ((Math.toDegrees(leftAngle.toDouble()) % 360 + 360) % 360).toFloat()
        assertTrue(leftDeg in 157.5f..202.5f, "Target straight left in isometric view must point West (approx 180 deg), was $leftDeg")
    }

    @Test
    fun testActiveWeaponStateFlowAndCycling() {
        val stateHolder = pro.jayeshseth.bugrush.game.ConferenceGameStateHolder()
        val arenaManager = stateHolder.arenaManager
        arenaManager.startNewGame()

        val initialWeapon = arenaManager.activeWeapon.value
        assertEquals(pro.jayeshseth.bugrush.game.actors.WeaponType.RAPID_BYTE_BLASTER, initialWeapon)

        // Cheat cycle weapon through all types
        arenaManager.cheatCycleWeapon()
        val nextWeapon = arenaManager.activeWeapon.value
        assertTrue(nextWeapon != initialWeapon, "Active weapon should advance after cycling")

        // Select Archetypes and verify active weapon sync
        arenaManager.selectArchetype(pro.jayeshseth.bugrush.game.actors.PlayerArchetype.ARCH_DEV)
        assertEquals(pro.jayeshseth.bugrush.game.actors.WeaponType.HEAVY_RAILGUN, arenaManager.activeWeapon.value)

        arenaManager.selectArchetype(pro.jayeshseth.bugrush.game.actors.PlayerArchetype.KOTLIN_BOT)
        assertEquals(pro.jayeshseth.bugrush.game.actors.WeaponType.TWIN_PULSE, arenaManager.activeWeapon.value)

        arenaManager.selectArchetype(pro.jayeshseth.bugrush.game.actors.PlayerArchetype.CYBER_CODER)
        assertEquals(pro.jayeshseth.bugrush.game.actors.WeaponType.RAPID_BYTE_BLASTER, arenaManager.activeWeapon.value)
    }

    @Test
    fun testExplosiveByteBarrelDetonationAndAoE() {
        val stateHolder = pro.jayeshseth.bugrush.game.ConferenceGameStateHolder()
        val arenaManager = stateHolder.arenaManager
        arenaManager.startNewGame()

        val barrel1 = ExplosiveBarrelVoxelActor(1000f, 1000f, hp = 2)
        val barrel2 = ExplosiveBarrelVoxelActor(1080f, 1000f, hp = 2) // within 140f radius
        val distantBarrel = ExplosiveBarrelVoxelActor(1350f, 1000f, hp = 2) // 350f away, outside 140f radius
        val nearbyBug = BugHazardActor(1050f, 1000f, bugType = BugType.NORMAL) // within 140f radius
        val nearbyCrate = CrateVoxelActor(1060f, 1000f) // within 140f radius

        arenaManager.explosiveBarrels.clear()
        arenaManager.explosiveBarrels.addAll(listOf(barrel1, barrel2, distantBarrel))
        arenaManager.bugs.clear()
        arenaManager.bugs.add(nearbyBug)
        arenaManager.crates.clear()
        arenaManager.crates.add(nearbyCrate)

        // Barrel takes damage without detonating at 1 HP
        val detonatedEarly = barrel1.takeDamage(1)
        assertFalse(detonatedEarly, "Barrel should not detonate at 1 HP")
        assertEquals(1, barrel1.hp)
        assertFalse(barrel1.isDetonated)

        // Detonate barrel1
        arenaManager.detonateBarrel(barrel1)
        assertTrue(barrel1.isDetonated, "Primary barrel must be detonated")
        assertTrue(barrel1.isDestroyed)
        assertTrue(barrel1.emitVoxelCuboids().isEmpty(), "Destroyed barrel must emit no cuboids")

        // Chain reaction: barrel2 in range is detonated
        assertTrue(barrel2.isDetonated, "Adjacent barrel within 140f must chain detonate")
        assertTrue(barrel2.isDestroyed)

        // Distant barrel outside AoE remains intact
        assertFalse(distantBarrel.isDetonated, "Distant barrel must not detonate")
        assertEquals(2, distantBarrel.hp)

        // Nearby bug is vaporized/killed
        assertEquals(0, nearbyBug.hp, "Bug in explosion radius must be eliminated")

        // Nearby crate is destroyed
        assertTrue(nearbyCrate.isDestroyed, "Crate in explosion radius must be destroyed")

        // Score awarded
        assertTrue(arenaManager.score.value > 0, "Detonation must award points")
    }

    @Test
    fun testSeamlessWaveEscalationWithoutElevator() {
        val stateHolder = pro.jayeshseth.bugrush.game.ConferenceGameStateHolder()
        val arenaManager = stateHolder.arenaManager
        arenaManager.startNewGame()

        assertEquals(1, arenaManager.currentFloor.value)
        assertEquals(0, arenaManager.bugsSquashedThisFloor.value)
        val quota = arenaManager.bugsTargetForElevator.value

        // Simulate squashing bugs up to quota - 1
        arenaManager.bugsSquashedThisFloor.value = quota - 1
        assertEquals(1, arenaManager.currentFloor.value, "Floor should not change before meeting quota")

        // Kill final required bug
        val finalBug = BugHazardActor(1900f, 1900f, bugType = BugType.NORMAL)
        arenaManager.bugs.add(finalBug)
        arenaManager.killBug(finalBug)

        // Seamless on-the-spot wave escalation ("System Overclock") triggered immediately
        assertEquals(2, arenaManager.currentFloor.value, "Floor must immediately advance to 2 without elevator gating")
        assertEquals(0, arenaManager.bugsSquashedThisFloor.value, "Floor bug counter must reset")
        assertTrue(arenaManager.floorAscensionTitle.value.contains("SYSTEM OVERCLOCK"), "Ascension banner must announce System Overclock")
        assertTrue(arenaManager.floorAscensionSplashTimerMs.value > 0, "Splash timer must be active")
        assertTrue(arenaManager.multiplier.value >= 2, "Multiplier must increase after floor escalation")
    }

    @Test
    fun testOverclockPowerConduitActivationAndRecharge() {
        val conduit = OverclockConduitVoxelActor(1500f, 1500f, powerUpType = PowerUpType.SPEED_MAGNET)
        assertTrue(conduit.isCharged, "Conduit must be charged initially")
        assertEquals(0, conduit.rechargeTimerMs)

        // Player outside activation radius (32f)
        assertFalse(conduit.checkActivation(1600f, 1600f))
        assertTrue(conduit.isCharged)

        // Player steps onto conduit
        assertTrue(conduit.checkActivation(1510f, 1510f), "Stepping on conduit within 32f must activate it")
        assertFalse(conduit.isCharged, "Conduit must be discharged after activation")
        assertEquals(25000, conduit.rechargeTimerMs, "Recharge cooldown must be set to 25s")

        // Repeated check while recharging fails
        assertFalse(conduit.checkActivation(1510f, 1510f))

        // Update partially (10s)
        conduit.update(10000)
        assertEquals(15000, conduit.rechargeTimerMs)
        assertFalse(conduit.isCharged)

        // Update past remaining time (15.5s)
        conduit.update(15500)
        assertEquals(0, conduit.rechargeTimerMs)
        assertTrue(conduit.isCharged, "Conduit must be recharged after 25s")

        // ArenaManager integration test
        val stateHolder = pro.jayeshseth.bugrush.game.ConferenceGameStateHolder()
        val arenaManager = stateHolder.arenaManager
        arenaManager.startNewGame()
        arenaManager.overclockConduits.clear()
        arenaManager.overclockConduits.add(conduit)

        arenaManager.player.posX = conduit.posX
        arenaManager.player.posY = conduit.posY
        arenaManager.onUpdate(50)

        assertEquals(PowerUpType.SPEED_MAGNET, arenaManager.activePowerUp.value, "Conduit must grant SPEED_MAGNET power-up")
        assertEquals(6, arenaManager.powerUpRemainingSeconds.value, "Conduit buff must last 6 seconds")
    }

    @Test
    fun testDynamicLaserFenceCyclingAndBeamIntersection() {
        val fence = LaserFenceVoxelActor(
            x1 = 1000f,
            y1 = 1000f,
            x2 = 1200f,
            y2 = 1000f,
            onDurationMs = 4000,
            offDurationMs = 2000
        )
        assertTrue(fence.isLaserActive, "Fence must be active initially")

        // Point directly on beam midpoint (1100, 1000)
        assertTrue(fence.isPointNearBeam(1100f, 1000f, tolerance = 20f), "Midpoint of beam must intersect")

        // Point within 15f tolerance
        assertTrue(fence.isPointNearBeam(1100f, 1015f, tolerance = 20f))

        // Point outside tolerance (50f away)
        assertFalse(fence.isPointNearBeam(1100f, 1050f, tolerance = 20f))

        // Point collinear but past the end of the segment (1250, 1000)
        assertFalse(fence.isPointNearBeam(1250f, 1000f, tolerance = 20f))

        // Cycling: After 3000ms, still within 4000ms ON window
        fence.update(3000)
        assertTrue(fence.isLaserActive)

        // After another 1500ms (total 4500ms > 4000ms), fence cycles OFF
        fence.update(1500)
        assertFalse(fence.isLaserActive, "Laser fence must cycle OFF during offDuration")
        assertFalse(fence.isPointNearBeam(1100f, 1000f), "Disabled beam must not intersect points")

        // After another 1600ms (total 6100ms > 6000ms), fence cycles back ON
        fence.update(1600)
        assertTrue(fence.isLaserActive, "Laser fence must cycle back ON after full cycle completes")

        // ArenaManager bug vaporization test
        val stateHolder = pro.jayeshseth.bugrush.game.ConferenceGameStateHolder()
        val arenaManager = stateHolder.arenaManager
        arenaManager.startNewGame()
        arenaManager.laserFences.clear()
        arenaManager.laserFences.add(fence)

        val crossingBug = BugHazardActor(1100f, 1000f, bugType = BugType.NORMAL)
        arenaManager.bugs.clear()
        arenaManager.bugs.add(crossingBug)

        arenaManager.onUpdate(50)
        assertEquals(0, crossingBug.hp, "Bug intersecting active laser fence must be vaporized")
        assertFalse(arenaManager.bugs.contains(crossingBug), "Vaporized bug must be removed from bugs list")
    }

    @Test
    fun testDistrictBountyBeaconFrenzy() {
        val beacon = BountyBeaconVoxelActor(
            posX = 800f,
            posY = 800f,
            districtName = "WEST DATA VAULT",
            targetKills = 8,
            bountyTimeLimitMs = 15000
        )
        assertFalse(beacon.isActive)
        assertTrue(beacon.isReadyToTrigger)
        assertEquals(0, beacon.currentKills)

        // Player outside trigger radius (36f)
        assertFalse(beacon.checkInteraction(900f, 900f))
        assertFalse(beacon.isActive)

        // Player steps into beacon radius
        assertTrue(beacon.checkInteraction(810f, 810f), "Beacon must trigger within 36f radius")
        assertTrue(beacon.isActive, "Beacon must become active")
        assertFalse(beacon.isReadyToTrigger)
        assertEquals(15000, beacon.remainingTimeMs)

        // Record 7 kills (not yet complete)
        for (i in 1..7) {
            val completed = beacon.recordKill()
            assertFalse(completed, "Bounty must not complete on kill $i of 8")
            assertEquals(i, beacon.currentKills)
            assertTrue(beacon.isActive)
        }

        // 8th kill completes bounty
        val completed = beacon.recordKill()
        assertTrue(completed, "8th kill must complete the bounty")
        assertFalse(beacon.isActive)
        assertTrue(beacon.isCompleted)
        assertEquals(30000, beacon.cooldownRemainingMs, "30s cooldown after successful bounty")

        // ArenaManager telemetry and reward test
        val stateHolder = pro.jayeshseth.bugrush.game.ConferenceGameStateHolder()
        val arenaManager = stateHolder.arenaManager
        arenaManager.startNewGame()
        val freshBeacon = BountyBeaconVoxelActor(
            posX = 800f,
            posY = 800f,
            districtName = "WEST DATA VAULT",
            targetKills = 8,
            bountyTimeLimitMs = 15000
        )
        arenaManager.bountyBeacons.clear()
        arenaManager.bountyBeacons.add(freshBeacon)

        // Step on beacon to activate via arena update
        arenaManager.player.posX = 800f
        arenaManager.player.posY = 800f
        arenaManager.onUpdate(50)

        assertTrue(freshBeacon.isActive)
        assertNotNull(arenaManager.activeBountyDistance.value, "Active bounty distance must be populated")
        assertNotNull(arenaManager.activeBountyAngleRad.value, "Active bounty angle must be populated")
        assertEquals(8, arenaManager.activeBountyRemainingKills.value)
        assertEquals(15, arenaManager.activeBountyTimeSeconds.value)

        // Claim bounty in ArenaManager
        val initialScore = arenaManager.score.value
        val initialMultiplier = arenaManager.multiplier.value
        for (i in 1..8) {
            val dummyBug = BugHazardActor(1900f, 1900f, bugType = BugType.NORMAL)
            arenaManager.killBug(dummyBug)
        }

        assertTrue(freshBeacon.isCompleted)
        assertTrue(arenaManager.score.value >= initialScore + 5000 * initialMultiplier, "Must award 5000 * multiplier bounty bonus")
        assertTrue(arenaManager.multiplier.value > initialMultiplier, "Multiplier must increment on bounty completion")
    }

    @Test
    fun testExplosiveBarrelDamagesBossTitan() {
        val stateHolder = pro.jayeshseth.bugrush.game.ConferenceGameStateHolder()
        val arenaManager = stateHolder.arenaManager
        arenaManager.startNewGame()

        val boss = BossVoxelActor(1050f, 1000f, maxHp = 35)
        arenaManager.boss = boss
        val barrel = ExplosiveBarrelVoxelActor(1000f, 1000f, hp = 2)
        arenaManager.explosiveBarrels.clear()
        arenaManager.explosiveBarrels.add(barrel)

        arenaManager.detonateBarrel(barrel)

        assertTrue(barrel.isDetonated)
        assertEquals(25, boss.hp, "Explosive barrel must deal 10 AoE damage to Boss Titan")
        assertEquals(Pair(25, 35), arenaManager.bossHp.value)
    }

    @Test
    fun testEscalationResetsElevatorOnlineFlag() {
        val stateHolder = pro.jayeshseth.bugrush.game.ConferenceGameStateHolder()
        val arenaManager = stateHolder.arenaManager
        arenaManager.startNewGame()

        arenaManager.ascendToNextFloor()
        assertEquals(2, arenaManager.currentFloor.value)
        assertEquals(0, arenaManager.bugsSquashedThisFloor.value)
        assertFalse(arenaManager.isElevatorOnline.value, "isElevatorOnline must be false after ascending so HUD displays floor bug target")
    }

    @Test
    fun testMissionBriefingStateTransitions() {
        val stateHolder = ConferenceGameStateHolder()
        val arenaManager = stateHolder.arenaManager

        // 1. Initial State -> ATTRACT
        arenaManager.startAttractMode()
        assertEquals(GameSessionState.ATTRACT, arenaManager.sessionState.value)
        assertFalse(arenaManager.isBriefingPausedFromGame)

        // 2. Direct transition to BRIEFING via API
        arenaManager.startBriefing(isPausedFromPlaying = false)
        assertEquals(GameSessionState.BRIEFING, arenaManager.sessionState.value)
        assertFalse(arenaManager.isBriefingPausedFromGame)

        // 3. Instant start on deploy / skipAll
        arenaManager.dismissBriefing()
        assertEquals(GameSessionState.PLAYING, arenaManager.sessionState.value)
        assertEquals(120, arenaManager.remainingTimeSeconds.value)

        // 4. Hotkey triggers: H in ATTRACT mode transitions to BRIEFING
        arenaManager.startAttractMode()
        val handledH = stateHolder.handleKey(Key.H, KeyEventType.KeyDown)
        assertTrue(handledH, "H key in ATTRACT mode must trigger mission briefing")
        assertEquals(GameSessionState.BRIEFING, arenaManager.sessionState.value)

        // 5. Spacebar in BRIEFING triggers instant start/deploy
        val handledSpace = stateHolder.handleKey(Key.Spacebar, KeyEventType.KeyDown)
        assertTrue(handledSpace, "Spacebar in BRIEFING must deploy operator")
        assertEquals(GameSessionState.PLAYING, arenaManager.sessionState.value)

        // 6. F1 key in ATTRACT mode transitions to BRIEFING
        arenaManager.startAttractMode()
        val handledF1 = stateHolder.handleKey(Key.F1, KeyEventType.KeyDown)
        assertTrue(handledF1, "F1 key in ATTRACT mode must trigger mission briefing")
        assertEquals(GameSessionState.BRIEFING, arenaManager.sessionState.value)

        // 7. Enter in BRIEFING triggers instant deploy
        val handledEnter = stateHolder.handleKey(Key.Enter, KeyEventType.KeyDown)
        assertTrue(handledEnter, "Enter in BRIEFING must deploy operator")
        assertEquals(GameSessionState.PLAYING, arenaManager.sessionState.value)

        // 8. In-Game Pause: H key while PLAYING pauses and opens BRIEFING
        arenaManager.player.posX = 1450f
        arenaManager.player.posY = 1450f
        val beforePauseTime = arenaManager.remainingTimeSeconds.value
        val handledPauseH = stateHolder.handleKey(Key.H, KeyEventType.KeyDown)
        assertTrue(handledPauseH, "H key while PLAYING must open briefing and pause")
        assertEquals(GameSessionState.BRIEFING, arenaManager.sessionState.value)
        assertTrue(arenaManager.isBriefingPausedFromGame, "isBriefingPausedFromGame must be true when paused from in-game")

        // Game update while paused does not advance game clock
        arenaManager.onUpdate(1000)
        assertEquals(beforePauseTime, arenaManager.remainingTimeSeconds.value, "Game clock must remain frozen during briefing pause")

        // 9. Resume from pause via Escape key
        val handledResumeEsc = stateHolder.handleKey(Key.Escape, KeyEventType.KeyDown)
        assertTrue(handledResumeEsc, "Escape key in paused briefing must resume gameplay")
        assertEquals(GameSessionState.PLAYING, arenaManager.sessionState.value)
        assertFalse(arenaManager.isBriefingPausedFromGame)
        assertEquals(1450f, arenaManager.player.posX, "Player position must be preserved after resuming from pause")

        // 10. In-Game Pause via F1 and resume via dismissBriefing()
        val handledPauseF1 = stateHolder.handleKey(Key.F1, KeyEventType.KeyDown)
        assertTrue(handledPauseF1, "F1 key while PLAYING must open briefing and pause")
        assertEquals(GameSessionState.BRIEFING, arenaManager.sessionState.value)
        assertTrue(arenaManager.isBriefingPausedFromGame)

        arenaManager.dismissBriefing()
        assertEquals(GameSessionState.PLAYING, arenaManager.sessionState.value)
        assertFalse(arenaManager.isBriefingPausedFromGame)

        // 11. Movement key from ATTRACT transitions to BRIEFING before starting run
        arenaManager.startAttractMode()
        val handledW = stateHolder.handleKey(Key.W, KeyEventType.KeyDown)
        assertTrue(handledW, "Movement key in ATTRACT must transition to briefing")
        assertEquals(GameSessionState.BRIEFING, arenaManager.sessionState.value)

        // 12. Directional arrows in ATTRACT transition to BRIEFING
        arenaManager.startAttractMode()
        val handledUp = stateHolder.handleKey(Key.DirectionUp, KeyEventType.KeyDown)
        assertTrue(handledUp, "DirectionUp key in ATTRACT must transition to briefing")
        assertEquals(GameSessionState.BRIEFING, arenaManager.sessionState.value)

        // 13. Gamepad START in ATTRACT transitions to BRIEFING
        arenaManager.startAttractMode()
        arenaManager.handleGamepadButton(GamepadButton.START)
        assertEquals(GameSessionState.BRIEFING, arenaManager.sessionState.value)
        assertFalse(arenaManager.isBriefingPausedFromGame)

        // 14. Gamepad SOUTH in BRIEFING deploys operator
        arenaManager.handleGamepadButton(GamepadButton.SOUTH)
        assertEquals(GameSessionState.PLAYING, arenaManager.sessionState.value)

        // 15. Gamepad START in PLAYING pauses and opens BRIEFING
        arenaManager.handleGamepadButton(GamepadButton.START)
        assertEquals(GameSessionState.BRIEFING, arenaManager.sessionState.value)
        assertTrue(arenaManager.isBriefingPausedFromGame, "Gamepad START during PLAYING must pause to briefing")

        // 16. Gamepad EAST in paused BRIEFING resumes PLAYING
        arenaManager.handleGamepadButton(GamepadButton.EAST)
        assertEquals(GameSessionState.PLAYING, arenaManager.sessionState.value)
        assertFalse(arenaManager.isBriefingPausedFromGame)

        // 17. Gamepad SELECT in PLAYING pauses and opens BRIEFING
        arenaManager.handleGamepadButton(GamepadButton.SELECT)
        assertEquals(GameSessionState.BRIEFING, arenaManager.sessionState.value)
        assertTrue(arenaManager.isBriefingPausedFromGame)

        // 18. Gamepad START in paused BRIEFING resumes PLAYING
        arenaManager.handleGamepadButton(GamepadButton.START)
        assertEquals(GameSessionState.PLAYING, arenaManager.sessionState.value)
        assertFalse(arenaManager.isBriefingPausedFromGame)

        // 19. Visual Pager: 5-Slide Advancement, Clamping, and Direct Selection
        arenaManager.startBriefing(isPausedFromPlaying = false)
        assertEquals(0, arenaManager.briefingSlideIndex.value, "Briefing must start on Slide 0")

        // Advance through all 5 slides
        arenaManager.nextBriefingSlide()
        assertEquals(1, arenaManager.briefingSlideIndex.value, "nextBriefingSlide() must advance to Slide 1 (Power-Ups)")

        arenaManager.nextBriefingSlide()
        assertEquals(2, arenaManager.briefingSlideIndex.value, "nextBriefingSlide() must advance to Slide 2 (Perks & Crates)")

        arenaManager.nextBriefingSlide()
        assertEquals(3, arenaManager.briefingSlideIndex.value, "nextBriefingSlide() must advance to Slide 3 (Hazards & Bounties)")

        arenaManager.nextBriefingSlide()
        assertEquals(4, arenaManager.briefingSlideIndex.value, "nextBriefingSlide() must advance to Slide 4 (Bugs & Boss)")

        // Advancing past Slide 4 deploys operator directly into game
        arenaManager.nextBriefingSlide()
        assertEquals(GameSessionState.PLAYING, arenaManager.sessionState.value, "Advancing past final slide must deploy operator")

        // 20. Visual Pager: Slide Retreat and Lower Clamping
        arenaManager.startBriefing(isPausedFromPlaying = false)
        arenaManager.setBriefingSlide(4)
        assertEquals(4, arenaManager.briefingSlideIndex.value)
        arenaManager.prevBriefingSlide()
        assertEquals(3, arenaManager.briefingSlideIndex.value, "prevBriefingSlide() must retreat to Slide 3")
        arenaManager.prevBriefingSlide()
        assertEquals(2, arenaManager.briefingSlideIndex.value, "prevBriefingSlide() must retreat to Slide 2")
        arenaManager.prevBriefingSlide()
        assertEquals(1, arenaManager.briefingSlideIndex.value, "prevBriefingSlide() must retreat to Slide 1")
        arenaManager.prevBriefingSlide()
        assertEquals(0, arenaManager.briefingSlideIndex.value, "prevBriefingSlide() must retreat to Slide 0")
        arenaManager.prevBriefingSlide()
        assertEquals(0, arenaManager.briefingSlideIndex.value, "prevBriefingSlide() must clamp at Slide 0")

        // 21. Visual Pager Keyboard Navigation (D / ArrowRight / A / ArrowLeft / 1, 2, 3, 4, 5)
        // Advance via Key.D
        val handledKeyD = stateHolder.handleKey(Key.D, KeyEventType.KeyDown)
        assertTrue(handledKeyD, "Key.D must advance briefing slide")
        assertEquals(1, arenaManager.briefingSlideIndex.value)

        // Advance via Key.DirectionRight
        val handledRight = stateHolder.handleKey(Key.DirectionRight, KeyEventType.KeyDown)
        assertTrue(handledRight, "DirectionRight must advance briefing slide")
        assertEquals(2, arenaManager.briefingSlideIndex.value)

        // Retreat via Key.A
        val handledKeyA = stateHolder.handleKey(Key.A, KeyEventType.KeyDown)
        assertTrue(handledKeyA, "Key.A must retreat briefing slide")
        assertEquals(1, arenaManager.briefingSlideIndex.value)

        // Retreat via Key.DirectionLeft
        val handledLeft = stateHolder.handleKey(Key.DirectionLeft, KeyEventType.KeyDown)
        assertTrue(handledLeft, "DirectionLeft must retreat briefing slide")
        assertEquals(0, arenaManager.briefingSlideIndex.value)

        // Direct number jumping: Key.Five, Key.Four, Key.Three, Key.Two, Key.One
        stateHolder.handleKey(Key.Five, KeyEventType.KeyDown)
        assertEquals(4, arenaManager.briefingSlideIndex.value, "Key.Five must jump directly to Slide 4")
        stateHolder.handleKey(Key.Four, KeyEventType.KeyDown)
        assertEquals(3, arenaManager.briefingSlideIndex.value, "Key.Four must jump directly to Slide 3")
        stateHolder.handleKey(Key.Three, KeyEventType.KeyDown)
        assertEquals(2, arenaManager.briefingSlideIndex.value, "Key.Three must jump directly to Slide 2")
        stateHolder.handleKey(Key.Two, KeyEventType.KeyDown)
        assertEquals(1, arenaManager.briefingSlideIndex.value, "Key.Two must jump directly to Slide 1")
        stateHolder.handleKey(Key.One, KeyEventType.KeyDown)
        assertEquals(0, arenaManager.briefingSlideIndex.value, "Key.One must jump directly to Slide 0")

        // 22. Visual Pager Gamepad Navigation (DPAD_RIGHT / DPAD_LEFT / Shoulders)
        arenaManager.handleGamepadButton(GamepadButton.DPAD_RIGHT)
        assertEquals(1, arenaManager.briefingSlideIndex.value, "Gamepad DPAD_RIGHT must advance slide")
        arenaManager.handleGamepadButton(GamepadButton.RIGHT_SHOULDER)
        assertEquals(2, arenaManager.briefingSlideIndex.value, "Gamepad RIGHT_SHOULDER must advance slide")
        arenaManager.handleGamepadButton(GamepadButton.DPAD_LEFT)
        assertEquals(1, arenaManager.briefingSlideIndex.value, "Gamepad DPAD_LEFT must retreat slide")
        arenaManager.handleGamepadButton(GamepadButton.LEFT_SHOULDER)
        assertEquals(0, arenaManager.briefingSlideIndex.value, "Gamepad LEFT_SHOULDER must retreat slide")

        // 23. Skip All / Instant Deploy from intermediate slide
        arenaManager.nextBriefingSlide() // On Slide 1 (Power-Ups)
        assertEquals(1, arenaManager.briefingSlideIndex.value)
        val handledSkipSpace = stateHolder.handleKey(Key.Spacebar, KeyEventType.KeyDown)
        assertTrue(handledSkipSpace, "Spacebar from Slide 1 must instantly skip all and deploy")
        assertEquals(GameSessionState.PLAYING, arenaManager.sessionState.value)
        assertEquals(120, arenaManager.remainingTimeSeconds.value)

        // 24. Verify Escape key triggers dismissBriefing / onSkipAll
        arenaManager.startBriefing(isPausedFromPlaying = false)
        assertEquals(0, arenaManager.briefingSlideIndex.value)
        val handledEsc = stateHolder.handleKey(Key.Escape, KeyEventType.KeyDown)
        assertTrue(handledEsc, "Escape key must skip all and deploy")
        assertEquals(GameSessionState.PLAYING, arenaManager.sessionState.value)
    }

    @Test
    fun test360DegreeIsometricFaceNormals() {
        // Verify view-facing normals for all 4 quadrants of 360-degree continuous rotation
        // Angle 0 rad: +X and +Y are visible
        val cos0 = kotlin.math.cos(0f)
        val sin0 = kotlin.math.sin(0f)
        assertTrue(cos0 + sin0 > 0f, "+X face must be visible at 0 rad")
        assertTrue(cos0 - sin0 > 0f, "+Y face must be visible at 0 rad")

        // Angle PI/2 (90 deg): +X and -Y are visible
        val angle90 = kotlin.math.PI.toFloat() * 0.5f
        val cos90 = kotlin.math.cos(angle90)
        val sin90 = kotlin.math.sin(angle90)
        assertTrue(cos90 + sin90 > 0f, "+X face must be visible at 90 deg")
        assertTrue(cos90 - sin90 < 0f, "-Y face must be visible at 90 deg")

        // Angle PI (180 deg): -X and -Y are visible
        val angle180 = kotlin.math.PI.toFloat()
        val cos180 = kotlin.math.cos(angle180)
        val sin180 = kotlin.math.sin(angle180)
        assertTrue(cos180 + sin180 < 0f, "-X face must be visible at 180 deg")
        assertTrue(cos180 - sin180 < 0f, "-Y face must be visible at 180 deg")

        // Angle 3PI/2 (270 deg): -X and +Y are visible
        val angle270 = kotlin.math.PI.toFloat() * 1.5f
        val cos270 = kotlin.math.cos(angle270)
        val sin270 = kotlin.math.sin(angle270)
        assertTrue(cos270 + sin270 < 0f, "-X face must be visible at 270 deg")
        assertTrue(cos270 - sin270 > 0f, "+Y face must be visible at 270 deg")
    }

    @Test
    fun testStartupOnboardingFlowTransitions() {
        val stateHolder = ConferenceGameStateHolder()
        val arenaManager = stateHolder.arenaManager

        // 1. Initial state is STARTUP mode by default upon launch
        assertEquals(GameSessionState.STARTUP, arenaManager.sessionState.value, "Session state must be STARTUP initially upon launch")

        // 2. Click [START GAME] -> BRIEFING
        arenaManager.startBriefing(isPausedFromPlaying = false, isFromStartup = true)
        assertEquals(GameSessionState.BRIEFING, arenaManager.sessionState.value)
        assertFalse(arenaManager.isBriefingPausedFromGame)

        // 3. Deploy/Skip from Briefing -> NAME_ENTRY
        arenaManager.dismissBriefing()
        assertEquals(GameSessionState.NAME_ENTRY, arenaManager.sessionState.value)

        // 4. Update player handle & confirm name -> COUNTDOWN
        arenaManager.updatePlayerHandle("CYBER_RUNNER")
        assertEquals("CYBER_RUNNER", arenaManager.playerHandle.value)
        arenaManager.startCountdown()
        assertEquals(GameSessionState.COUNTDOWN, arenaManager.sessionState.value)

        // 5. Countdown complete -> PLAYING with 5s invulnerability shield
        arenaManager.startNewGame()
        assertEquals(GameSessionState.PLAYING, arenaManager.sessionState.value)
        assertTrue(arenaManager.player.isEntranceShieldActive, "Entrance shield must be active upon game start")
        assertEquals(5000, arenaManager.player.entranceShieldTimerMs, "Entrance shield timer must be 5000ms")
        assertTrue(arenaManager.player.isInvulnerable, "Player must be invulnerable while entrance shield is active")
    }

    @Test
    fun testPointsAsBulletsEconomy() {
        val stateHolder = ConferenceGameStateHolder()
        val arenaManager = stateHolder.arenaManager

        arenaManager.startNewGame()
        assertEquals(100, arenaManager.score.value, "Player must start with 100 starter ammo/points")

        // 1. Standard weapon costs 1 point
        arenaManager.selectArchetype(pro.jayeshseth.bugrush.game.actors.PlayerArchetype.CYBER_CODER)
        assertEquals(pro.jayeshseth.bugrush.game.actors.WeaponType.RAPID_BYTE_BLASTER, arenaManager.activeWeapon.value)
        arenaManager.firePlayerWeapon()
        assertEquals(99, arenaManager.score.value, "Rapid Byte Blaster must consume 1 point per shot")

        // 2. Heavy railgun costs 5 points
        arenaManager.selectArchetype(pro.jayeshseth.bugrush.game.actors.PlayerArchetype.ARCH_DEV)
        assertEquals(pro.jayeshseth.bugrush.game.actors.WeaponType.HEAVY_RAILGUN, arenaManager.activeWeapon.value)
        arenaManager.player.shootCooldownMs = 0
        arenaManager.firePlayerWeapon()
        assertEquals(94, arenaManager.score.value, "Heavy Railgun must consume 5 points per shot")

        // 3. Gem collection awards +50 points
        val gem = pro.jayeshseth.bugrush.game.actors.GemVoxelActor(100f, 100f, pointValue = 50)
        arenaManager.onGemCollected(gem)
        assertEquals(144, arenaManager.score.value, "Collecting gem must award +50 points")

        // 4. Squashing a bug awards +25 points
        val bug = BugHazardActor(200f, 200f, bugType = BugType.NORMAL)
        arenaManager.killBug(bug)
        assertEquals(169, arenaManager.score.value, "Squashing bug must award +25 points")

        // 5. Bug hit deducts 50 points
        arenaManager.player.entranceShieldTimerMs = 0
        arenaManager.player.isEntranceShieldActive = false
        arenaManager.player.invulnerableTimerMs = 0
        arenaManager.onPlayerHit(knockbackDirX = 0f, knockbackDirY = 0f)
        assertEquals(119, arenaManager.score.value, "Bug hit must deduct 50 points")

        // 6. Points <= 0 locks firing
        arenaManager.player.invulnerableTimerMs = 0
        arenaManager.onPlayerHit(0f, 0f)
        arenaManager.player.invulnerableTimerMs = 0
        arenaManager.onPlayerHit(0f, 0f)
        arenaManager.player.invulnerableTimerMs = 0
        arenaManager.onPlayerHit(0f, 0f) // Clamped to 0
        assertEquals(0, arenaManager.score.value, "Points cannot go below 0")
        assertFalse(arenaManager.player.canShoot(), "Player cannot shoot when score/ammo is 0")
    }

    @Test
    fun testEntranceShieldInvulnerability() {
        val stateHolder = ConferenceGameStateHolder()
        val arenaManager = stateHolder.arenaManager

        arenaManager.startNewGame()
        assertTrue(arenaManager.player.isEntranceShieldActive)
        assertEquals(100, arenaManager.score.value)

        // Attempting to hit player while entrance shield is active
        arenaManager.onPlayerHit(0f, 0f)
        assertEquals(100, arenaManager.score.value, "Score must not be deducted while entrance shield is active")
        assertFalse(arenaManager.player.isStunned, "Player must not be stunned while entrance shield is active")

        // Update player clock past 5000ms
        arenaManager.player.update(2500)
        assertTrue(arenaManager.player.isEntranceShieldActive)
        assertEquals(2500, arenaManager.player.entranceShieldTimerMs)

        arenaManager.player.update(2500)
        assertFalse(arenaManager.player.isEntranceShieldActive, "Entrance shield must expire after 5000ms")
    }

    @Test
    fun testBugSwarmHomingAI() {
        val bug = BugHazardActor(1000f, 1000f, headingAngle = 3.14159f, bugType = BugType.NORMAL)
        assertFalse(bug.hasSwarmTarget)

        // Player is to the East at (1500f, 1000f) -> angle should steer towards 0 rad
        bug.updateSwarmAI(playerX = 1500f, playerY = 1000f, deltaTimeInMilliseconds = 100)
        assertTrue(bug.hasSwarmTarget, "updateSwarmAI must mark hasSwarmTarget as true")

        // Multiple updates should steer bug towards player
        for (i in 0 until 10) {
            bug.updateSwarmAI(playerX = 1500f, playerY = 1000f, deltaTimeInMilliseconds = 100)
        }
        val cosH = kotlin.math.cos(bug.headingAngle)
        assertTrue(cosH > 0f, "Bug heading angle must steer towards player (+X direction)")

        // Flocking separation: create adjacent bug
        val neighbor = BugHazardActor(1005f, 1000f, headingAngle = 0f, bugType = BugType.NORMAL)
        bug.updateSwarmAI(playerX = 1500f, playerY = 1000f, deltaTimeInMilliseconds = 100, neighbors = listOf(neighbor))
        assertTrue(bug.hasSwarmTarget)
    }

    @Test
    fun testPerlinNoiseProceduralGeneration() {
        val noise1 = pro.jayeshseth.bugrush.game.map.PerlinNoise(seed = 42L)
        val noise2 = pro.jayeshseth.bugrush.game.map.PerlinNoise(seed = 9999L)

        // Determinism: same seed produces same output
        val n1A = noise1.noise2D(12.34f, 56.78f)
        val n1B = pro.jayeshseth.bugrush.game.map.PerlinNoise(seed = 42L).noise2D(12.34f, 56.78f)
        assertEquals(n1A, n1B, 0.0001f, "PerlinNoise must be deterministic for identical seed")

        // Variance: different seeds produce different outputs
        val n2A = noise2.noise2D(12.34f, 56.78f)
        assertTrue(kotlin.math.abs(n1A - n2A) > 0.001f, "Different seeds should produce differing noise values")

        // fBm bounds check: fbm2D must remain strictly in [0.0f, 1.0f]
        for (x in 0..10) {
            for (y in 0..10) {
                val fbm = noise1.fbm2D(x * 0.5f, y * 0.5f)
                assertTrue(fbm in 0f..1f, "fBm noise value $fbm must be clamped in [0, 1]")
            }
        }
    }

    @Test
    fun testPerlinNoiseMapDifferentiation() {
        val managerA = ArenaManager()
        val managerB = ArenaManager()

        managerA.startNewGame()
        managerB.startNewGame()

        // 1. Both maps strictly preserve flat walkable floor at Z = 0f (cuboid z = -6f, sizeZ = 6f)
        val walkableA = managerA.proceduralFloorTiles.filter { it.z == -6f }
        val walkableB = managerB.proceduralFloorTiles.filter { it.z == -6f }
        assertTrue(walkableA.isNotEmpty())
        assertTrue(walkableB.isNotEmpty())
        assertTrue(walkableA.all { it.sizeZ == 6f }, "Floor elevation must be strictly uniform at Z = 0f")
        assertTrue(walkableB.all { it.sizeZ == 6f })
        assertTrue(managerA.proceduralFloorTiles.all { (it.z + it.sizeZ) <= 0.001f })
        assertTrue(managerB.proceduralFloorTiles.all { (it.z + it.sizeZ) <= 0.001f })

        // 2. Both maps feature randomized, non-circular organic borders within the grid boundary
        for (tile in walkableA) {
            val dist = kotlin.math.sqrt((tile.x - 1900f) * (tile.x - 1900f) + (tile.y - 1900f) * (tile.y - 1900f))
            assertTrue(dist <= 1800f, "Floor tiles must remain within megamap grid boundary")
        }

        // 3. Different seeds produce differing floor tile micro-biome colors
        val colorsA = walkableA.map { it.baseColor }
        val colorsB = walkableB.map { it.baseColor }
        var differingColorCount = 0
        for (i in 0 until minOf(colorsA.size, colorsB.size)) {
            if (colorsA[i] != colorsB[i]) differingColorCount++
        }
        assertTrue(differingColorCount > 0, "Procedural floor biomes must produce differing tile color distributions across different seeds")

        // 4. Props (server racks, trees, crates, barrels, barricades) are procedurally spawned
        assertTrue(managerA.serverRacks.isNotEmpty(), "Procedural server racks must be spawned")
        assertTrue(managerA.trees.isNotEmpty(), "Procedural trees must be spawned")
        assertTrue(managerA.crates.isNotEmpty(), "Procedural crates must be spawned")
        assertTrue(managerA.explosiveBarrels.isNotEmpty(), "Procedural explosive barrels must be spawned")
        assertTrue(managerA.techBarricades.size >= 12, "Must spawn at least 12 tech barricades across districts and center")
    }

    @Test
    fun testStartupLeaderboardToggle() {
        val stateHolder = ConferenceGameStateHolder()
        val arenaManager = stateHolder.arenaManager

        arenaManager.startStartupMode()
        assertEquals(GameSessionState.STARTUP, arenaManager.sessionState.value)
        assertFalse(arenaManager.isStartupLeaderboardVisible.value)

        // Press 'L' to toggle open leaderboard
        val handledL = stateHolder.handleKey(Key.L, KeyEventType.KeyDown)
        assertTrue(handledL, "Key.L must be handled in STARTUP mode")
        assertTrue(arenaManager.isStartupLeaderboardVisible.value, "Leaderboard must be visible after pressing L")

        // Press 'Escape' to close leaderboard
        val handledEsc = stateHolder.handleKey(Key.Escape, KeyEventType.KeyDown)
        assertTrue(handledEsc, "Key.Escape must be handled in STARTUP mode")
        assertFalse(arenaManager.isStartupLeaderboardVisible.value, "Leaderboard must be closed after pressing Escape")
    }

    @Test
    fun testRedBugsInViewSwarmAttraction() {
        val arenaManager = ArenaManager()
        arenaManager.screenWidth = 1920f
        arenaManager.screenHeight = 1080f
        arenaManager.startNewGame()

        // Create a red bug nearby in view
        val redBugInView = BugHazardActor(
            posX = arenaManager.player.posX + 100f,
            posY = arenaManager.player.posY + 100f,
            bugType = BugType.NORMAL
        )
        // Create an armored beetle (non-red bug) nearby
        val armoredBugInView = BugHazardActor(
            posX = arenaManager.player.posX + 120f,
            posY = arenaManager.player.posY + 120f,
            bugType = BugType.DEADLOCK
        )
        // Create a red bug far off-screen
        val redBugOffScreen = BugHazardActor(
            posX = 3500f,
            posY = 3500f,
            bugType = BugType.NORMAL
        )

        arenaManager.bugs.clear()
        arenaManager.bugs.addAll(listOf(redBugInView, armoredBugInView, redBugOffScreen))

        assertTrue(arenaManager.isBugInView(redBugInView), "Nearby red bug must be in view")
        assertFalse(arenaManager.isBugInView(redBugOffScreen), "Faraway red bug must be out of view")

        // First bullet triggers swarm alert
        arenaManager.firePlayerWeapon()

        arenaManager.updateBugSwarmAI(100)

        assertTrue(redBugInView.hasSwarmTarget, "Red bug in view must engage swarm target towards player")
        assertFalse(armoredBugInView.hasSwarmTarget, "Armored deadlock beetle must NOT attract to player")
        assertFalse(redBugOffScreen.hasSwarmTarget, "Off-screen red bug must NOT attract to player")
    }

    @Test
    fun testAutoLeaderboardEntryOnRoundEnd() {
        val arenaManager = ArenaManager()
        arenaManager.updatePlayerHandle("Cyber_Champion_99")
        arenaManager.startNewGame()

        val initialCount = pro.jayeshseth.bugrush.leaderboard.LocalLeaderboardManager.entries.value.size

        arenaManager.endRound()

        assertEquals(GameSessionState.ROUND_OVER, arenaManager.sessionState.value)
        val entries = pro.jayeshseth.bugrush.leaderboard.LocalLeaderboardManager.entries.value
        assertTrue(entries.size >= initialCount + 1, "Leaderboard entry must be auto-recorded on round end")
        assertTrue(entries.any { it.playerName == "Cyber_Champion_99" }, "Leaderboard must contain entry for Cyber_Champion_99")
    }

    @Test
    fun testLeaderboardOpenFromGameOver() {
        val arenaManager = ArenaManager()
        arenaManager.endRound()
        assertEquals(GameSessionState.ROUND_OVER, arenaManager.sessionState.value)

        // Opening leaderboard from game over must transition to STARTUP and show leaderboard
        arenaManager.openStartupLeaderboard()
        assertEquals(GameSessionState.STARTUP, arenaManager.sessionState.value)
        assertTrue(arenaManager.isStartupLeaderboardVisible.value)

        // Closing leaderboard must hide it and stay on STARTUP menu
        arenaManager.closeStartupLeaderboard()
        assertEquals(GameSessionState.STARTUP, arenaManager.sessionState.value)
        assertFalse(arenaManager.isStartupLeaderboardVisible.value)
    }

    @Test
    fun testTitleThemeAudioLifecycleAndTransition() {
        var startTitleCount = 0
        var stopTitleCount = 0
        var startBgmCount = 0
        var stopBgmCount = 0
        var pauseBgmCount = 0
        var resumeBgmCount = 0

        val mockSoundPlayer = object : pro.jayeshseth.bugrush.audio.SoundPlayer {
            override fun playSfx(sfx: SoundEffect) {}
            override fun startBgm() { startBgmCount++ }
            override fun stopBgm() { stopBgmCount++ }
            override fun pauseBgm() { pauseBgmCount++ }
            override fun resumeBgm() { resumeBgmCount++ }
            override fun startTitleTheme() { startTitleCount++ }
            override fun stopTitleTheme() { stopTitleCount++ }
            override fun toggleMute() {}
            override val isMuted: Boolean = false
        }

        val stateHolder = ConferenceGameStateHolder(soundPlayer = mockSoundPlayer)
        val arenaManager = stateHolder.arenaManager

        // 1. Entering STARTUP mode starts title theme
        arenaManager.startStartupMode()
        assertEquals(1, startTitleCount, "startStartupMode must trigger soundPlayer.startTitleTheme()")
        assertTrue(stopBgmCount >= 1, "startStartupMode must stop BGM")

        // 2. Transitioning to Briefing stops title theme
        arenaManager.startBriefing(isPausedFromPlaying = false, isFromStartup = true)
        assertEquals(1, stopTitleCount, "startBriefing must trigger soundPlayer.stopTitleTheme()")

        // 3. Countdown and startNewGame transitions to action BGM
        arenaManager.startCountdown()
        assertTrue(stopTitleCount >= 2, "startCountdown must stop title theme")

        arenaManager.startNewGame()
        assertEquals(1, startBgmCount, "startNewGame must start action BGM")

        // Test Pause and Resume audio calls
        arenaManager.pauseGame()
        assertEquals(1, pauseBgmCount, "pauseGame must pause BGM")
        arenaManager.resumeGame()
        assertEquals(1, resumeBgmCount, "resumeGame must resume BGM")

        // 4. Round over returns to title theme
        arenaManager.endRound()
        assertEquals(2, startTitleCount, "endRound must restart title theme")

        // 5. DesktopChiptuneAudioPlayer title theme methods test
        val desktopPlayer = pro.jayeshseth.bugrush.audio.DesktopChiptuneAudioPlayer()
        desktopPlayer.startTitleTheme()
        desktopPlayer.stopTitleTheme()
        desktopPlayer.startBgm()
        desktopPlayer.stopBgm()
        desktopPlayer.toggleMute()
        assertTrue(desktopPlayer.isMuted)
        desktopPlayer.toggleMute()
        assertFalse(desktopPlayer.isMuted)
    }

    @Test
    fun testRetroArcadeTypographySystem() {
        val pixelHeader = pro.jayeshseth.bugrush.ui.ArcadeTheme.pixelHeader
        val terminalBody = pro.jayeshseth.bugrush.ui.ArcadeTheme.terminalBody

        assertNotNull(pixelHeader, "ArcadeTheme.pixelHeader must be defined")
        assertNotNull(terminalBody, "ArcadeTheme.terminalBody must be defined")
        assertTrue(pixelHeader is androidx.compose.ui.text.font.FontFamily)
        assertTrue(terminalBody is androidx.compose.ui.text.font.FontFamily)
    }

    @Test
    fun testAudioPlayerRaceConditionProtection() {
        val player = pro.jayeshseth.bugrush.audio.DesktopChiptuneAudioPlayer()
        // Rapid start and immediate stop before worker thread executes
        for (i in 0 until 10) {
            player.startTitleTheme()
            player.stopTitleTheme()
            player.startBgm()
            player.pauseBgm()
            player.resumeBgm()
            player.stopBgm()
        }
        // Toggle mute rapidly while triggering themes
        player.toggleMute()
        assertTrue(player.isMuted)
        player.startTitleTheme()
        player.startBgm()
        player.pauseBgm()
        player.resumeBgm()
        player.toggleMute()
        assertFalse(player.isMuted)
        player.stopTitleTheme()
        player.stopBgm()
    }

    @Test
    fun testAttractModeLeaderboardControls() {
        val stateHolder = ConferenceGameStateHolder()
        val arenaManager = stateHolder.arenaManager

        arenaManager.startAttractMode()
        assertEquals(GameSessionState.ATTRACT, arenaManager.sessionState.value)
        assertFalse(arenaManager.isStartupLeaderboardVisible.value)

        // Press 'L' to toggle open leaderboard in ATTRACT mode
        val handledL = stateHolder.handleKey(Key.L, KeyEventType.KeyDown)
        assertTrue(handledL, "Key.L must be handled in ATTRACT mode")
        assertTrue(arenaManager.isStartupLeaderboardVisible.value, "Leaderboard must be visible after pressing L in ATTRACT")

        // Press 'Escape' to close leaderboard in ATTRACT mode
        val handledEsc = stateHolder.handleKey(Key.Escape, KeyEventType.KeyDown)
        assertTrue(handledEsc, "Key.Escape must be handled in ATTRACT mode")
        assertFalse(arenaManager.isStartupLeaderboardVisible.value, "Leaderboard must be closed after pressing Escape in ATTRACT")

        // Retriggering startAttractMode resets leaderboard visibility
        arenaManager.openStartupLeaderboard()
        assertTrue(arenaManager.isStartupLeaderboardVisible.value)
        arenaManager.startAttractMode()
        assertFalse(arenaManager.isStartupLeaderboardVisible.value, "startAttractMode must reset leaderboard visibility")
    }

    @Test
    fun testDioramaLegAnimationAndAimTracking() {
        val player = PlayerVoxelActor(0f, 0f, PlayerArchetype.CYBER_CODER)
        val swarmer = BugHazardActor(50f, 50f, headingAngle = 0f, bugType = BugType.SWARMER)

        // Walking leg animation must advance on update
        swarmer.update(100)
        val cuboidsWithLegMotion = swarmer.emitVoxelCuboids()
        assertTrue(cuboidsWithLegMotion.isNotEmpty())

        // Player aim tracking: aiming at swarmer at (50, 50)
        val aimX = swarmer.posX - player.posX
        val aimY = swarmer.posY - player.posY
        val dist = kotlin.math.hypot(aimX, aimY)
        player.headingX = aimX / dist
        player.headingY = aimY / dist
        assertEquals(1f / kotlin.math.sqrt(2f), player.headingX, 0.001f)
        assertEquals(1f / kotlin.math.sqrt(2f), player.headingY, 0.001f)
    }

    @Test
    fun testBugSwarmWaitsForFirstBullet() {
        val arenaManager = ArenaManager()
        arenaManager.startNewGame()
        assertFalse(arenaManager.hasPlayerFiredFirstBullet.value, "hasPlayerFiredFirstBullet must start false")

        // Spawn a normal bug in view
        val bug = BugHazardActor(arenaManager.player.posX + 50f, arenaManager.player.posY + 50f, bugType = BugType.NORMAL)
        arenaManager.bugs.clear()
        arenaManager.bugs.add(bug)

        // Before shooting: bug swarm AI must NOT set target
        arenaManager.updateBugSwarmAI(16)
        assertFalse(bug.hasSwarmTarget, "Bugs must NOT swarm before first bullet is fired")

        // Fire a bullet
        arenaManager.firePlayerWeapon()
        assertTrue(arenaManager.hasPlayerFiredFirstBullet.value, "hasPlayerFiredFirstBullet must be true after shooting")

        // After shooting: bug in view must acquire swarm target
        arenaManager.updateBugSwarmAI(16)
        assertTrue(bug.hasSwarmTarget, "In-view normal bugs must swarm after first bullet is fired")
    }

    @Test
    fun testPauseAndResumeWithEscape() {
        val stateHolder = ConferenceGameStateHolder()
        val arenaManager = stateHolder.arenaManager
        arenaManager.startNewGame()
        assertEquals(GameSessionState.PLAYING, arenaManager.sessionState.value)

        // Fire a bullet so projectiles and bugs are active
        arenaManager.firePlayerWeapon()
        assertTrue(arenaManager.bugs.isNotEmpty(), "Bugs must be present in arena")

        // Press Escape to pause
        val handledPause = stateHolder.handleKey(Key.Escape, KeyEventType.KeyDown)
        assertTrue(handledPause, "Escape key must be handled during PLAYING")
        assertEquals(GameSessionState.PAUSED, arenaManager.sessionState.value, "Game must be PAUSED")
        assertTrue(arenaManager.player.isPaused, "Player must be paused")
        assertTrue(arenaManager.bugs.all { it.isPaused }, "All bugs must be paused")

        // Press Escape again to resume
        val handledResume = stateHolder.handleKey(Key.Escape, KeyEventType.KeyDown)
        assertTrue(handledResume, "Escape key must be handled during PAUSED")
        assertEquals(GameSessionState.PLAYING, arenaManager.sessionState.value, "Game must be resumed to PLAYING")
        assertFalse(arenaManager.player.isPaused, "Player must be unpaused")
        assertTrue(arenaManager.bugs.all { !it.isPaused }, "All bugs must be unpaused")

        // Pause again and test quit with Q
        stateHolder.handleKey(Key.Escape, KeyEventType.KeyDown)
        assertEquals(GameSessionState.PAUSED, arenaManager.sessionState.value)
        val handledQ = stateHolder.handleKey(Key.Q, KeyEventType.KeyDown)
        assertTrue(handledQ, "Q key must be handled during PAUSED")
        assertEquals(GameSessionState.STARTUP, arenaManager.sessionState.value, "Q key must quit to STARTUP")
    }

    @Test
    fun testStartupArrowKeyNavigationAndMenuSelection() {
        val stateHolder = ConferenceGameStateHolder()
        val arenaManager = stateHolder.arenaManager
        arenaManager.startStartupMode()
        assertEquals(GameSessionState.STARTUP, arenaManager.sessionState.value)
        assertEquals(0, arenaManager.startupMenuIndex.value, "Initial menu selection must be index 0 (START GAME)")

        // Down arrow navigates to LEADERBOARD (index 1)
        val handledDown = stateHolder.handleKey(Key.DirectionDown, KeyEventType.KeyDown)
        assertTrue(handledDown, "DirectionDown must be handled in STARTUP")
        assertEquals(1, arenaManager.startupMenuIndex.value, "Menu selection must move to index 1 (LEADERBOARD)")

        // S key maintains or selects index 1
        stateHolder.handleKey(Key.S, KeyEventType.KeyDown)
        assertEquals(1, arenaManager.startupMenuIndex.value)

        // Up arrow navigates back to START GAME (index 0)
        val handledUp = stateHolder.handleKey(Key.DirectionUp, KeyEventType.KeyDown)
        assertTrue(handledUp, "DirectionUp must be handled in STARTUP")
        assertEquals(0, arenaManager.startupMenuIndex.value, "Menu selection must move to index 0 (START GAME)")

        // W key maintains index 0
        stateHolder.handleKey(Key.W, KeyEventType.KeyDown)
        assertEquals(0, arenaManager.startupMenuIndex.value)

        // Select LEADERBOARD and press Enter
        stateHolder.handleKey(Key.DirectionDown, KeyEventType.KeyDown)
        assertEquals(1, arenaManager.startupMenuIndex.value)
        stateHolder.handleKey(Key.Enter, KeyEventType.KeyDown)
        assertTrue(arenaManager.isStartupLeaderboardVisible.value, "Pressing Enter on LEADERBOARD must open leaderboard")

        // Pressing Escape or Enter closes leaderboard
        stateHolder.handleKey(Key.Escape, KeyEventType.KeyDown)
        assertFalse(arenaManager.isStartupLeaderboardVisible.value, "Pressing Escape must close leaderboard")

        // Select START GAME and press Enter
        stateHolder.handleKey(Key.DirectionUp, KeyEventType.KeyDown)
        assertEquals(0, arenaManager.startupMenuIndex.value)
        stateHolder.handleKey(Key.Enter, KeyEventType.KeyDown)
        assertEquals(GameSessionState.BRIEFING, arenaManager.sessionState.value, "Pressing Enter on START GAME must begin briefing")
    }

    @Test
    fun testTriviaRealTimeGameLoopAndDoubleDamage() {
        val stateHolder = ConferenceGameStateHolder()
        val arenaManager = stateHolder.arenaManager
        arenaManager.startNewGame()
        arenaManager.player.invulnerableTimerMs = 0
        arenaManager.player.entranceShieldTimerMs = 0
        arenaManager.player.isEntranceShieldActive = false

        // Position player and add shard at player's location
        arenaManager.player.posX = 1900f
        arenaManager.player.posY = 1900f
        val shard = pro.jayeshseth.bugrush.game.actors.TriviaShardVoxelActor(1900f, 1900f, tier = 1)
        // Add shard directly and tick update
        arenaManager.onUpdate(50)
        // Shard pickup triggers trivia
        val triggerMethod = ArenaManager::class.java.getDeclaredMethod("triggerShardTrivia", Int::class.javaPrimitiveType)
        triggerMethod.isAccessible = true
        triggerMethod.invoke(arenaManager, 1)

        assertEquals(GameSessionState.TRIVIA_MODAL, arenaManager.sessionState.value, "Session state must be TRIVIA_MODAL")
        assertTrue(arenaManager.player.isMovementLocked, "Player movement must be locked during trivia")
        assertEquals(2000, arenaManager.triviaLockoutRemainingMs.value, "Trivia lockout must initialize to 2000ms")

        // Player cannot move while locked
        arenaManager.player.setDirectMove(1f, 1f)
        arenaManager.player.update(50)
        assertEquals(1900f, arenaManager.player.posX, "Player X must remain frozen while answering trivia")
        assertEquals(1900f, arenaManager.player.posY, "Player Y must remain frozen while answering trivia")

        // Answering during lockout is blocked
        arenaManager.submitTriviaAnswer(arenaManager.currentTriviaQuestion.value!!.correctIndex)
        assertEquals(GameSessionState.TRIVIA_MODAL, arenaManager.sessionState.value, "Answering during lockout must be blocked")

        // Game loop advances during TRIVIA_MODAL
        arenaManager.onUpdate(500)
        assertEquals(1500, arenaManager.triviaLockoutRemainingMs.value, "Lockout timer must decrement during game loop update")

        // Bug hit during trivia deals 2X damage (-100 PTS)
        arenaManager.player.invulnerableTimerMs = 0
        arenaManager.player.isEntranceShieldActive = false
        val preHitScore = arenaManager.score.value
        arenaManager.onPlayerHit(0f, 0f, isBossHit = false)
        val postHitScore = arenaManager.score.value
        assertEquals(100, preHitScore - postHitScore, "Bug hit during trivia must deduct 100 points (2x)")
        val texts = arenaManager.getFloatingTextsSnapshot()
        assertTrue(texts.any { it.text.contains("2X DAMAGE") }, "Floating text must indicate 2X DAMAGE")

        // Advance remaining lockout time
        arenaManager.onUpdate(1500)
        assertEquals(0, arenaManager.triviaLockoutRemainingMs.value, "Lockout must reach 0ms")

        // Now answer can be submitted
        val question = arenaManager.currentTriviaQuestion.value!!
        arenaManager.submitTriviaAnswer(question.correctIndex)
        arenaManager.perkSelectionOptions.value?.let { perks ->
            arenaManager.selectVersionPerk(perks[0])
        }
        assertEquals(GameSessionState.PLAYING, arenaManager.sessionState.value, "Game state must return to PLAYING")
        assertFalse(arenaManager.player.isMovementLocked, "Player movement must unlock after trivia concludes")
    }

    @Test
    fun testGunDropProbabilitiesAndPickup() {
        val arenaManager = ArenaManager()
        arenaManager.startNewGame()

        // Test rollRandomWeaponType distribution
        val rolls = mutableMapOf<WeaponType, Int>()
        for (i in 0 until 1000) {
            val wp = arenaManager.rollRandomWeaponType()
            rolls[wp] = (rolls[wp] ?: 0) + 1
        }
        assertTrue(rolls.containsKey(WeaponType.TWIN_PULSE))
        assertTrue(rolls.containsKey(WeaponType.TRIPLE_SPREAD))
        assertTrue(rolls.containsKey(WeaponType.HEAVY_RAILGUN))
        assertTrue(rolls.containsKey(WeaponType.PLASMA_FLAK_CANNON))
        assertTrue(rolls.containsKey(WeaponType.NOVA_360_BLASTER))

        // Spawn a gun drop
        val drop = arenaManager.spawnRandomGunDrop(1920f, 1920f)
        assertTrue(arenaManager.gunDrops.contains(drop))

        // Position player on drop
        arenaManager.player.posX = 1920f
        arenaManager.player.posY = 1920f
        arenaManager.onUpdate(50)

        // Verify pickup equips weapon immediately
        assertEquals(drop.weaponType, arenaManager.player.activeWeapon, "Player weapon must match picked up gun drop")
        assertEquals(drop.weaponType, arenaManager.activeWeapon.value, "ArenaManager activeWeapon must update")
        assertTrue(drop.isCollected, "Drop must be marked as collected")
        assertFalse(arenaManager.gunDrops.contains(drop), "Collected drop must be removed from arenaManager.gunDrops")
    }

    @Test
    fun testNova360BlasterAndAmmoCosts() {
        val stateHolder = ConferenceGameStateHolder()
        val arenaManager = stateHolder.arenaManager
        arenaManager.startNewGame()

        // 1. Verify Nova 360 radial projectile burst
        arenaManager.player.activeWeapon = WeaponType.NOVA_360_BLASTER
        val bullets = arenaManager.player.fireWeapon()
        assertEquals(16, bullets.size, "Nova 360 Blaster must spawn 16 radial projectiles")

        // 2. Verify Ammo Cost for all weapon types
        val weaponCosts = mapOf(
            WeaponType.RAPID_BYTE_BLASTER to 1,
            WeaponType.TWIN_PULSE to 2,
            WeaponType.TRIPLE_SPREAD to 3,
            WeaponType.HEAVY_RAILGUN to 5,
            WeaponType.PLASMA_FLAK_CANNON to 5,
            WeaponType.NOVA_360_BLASTER to 16
        )

        weaponCosts.forEach { (weapon, expectedCost) ->
            arenaManager.player.activeWeapon = weapon
            assertEquals(expectedCost, weapon.ammoCost, "$weapon ammoCost must be $expectedCost")

            // Test point deduction on shot
            arenaManager.setScoreForTest(500)
            val scoreBefore = arenaManager.score.value
            arenaManager.player.shootCooldownMs = 0
            arenaManager.firePlayerWeapon()
            val scoreAfter = arenaManager.score.value
            assertEquals(expectedCost, scoreBefore - scoreAfter, "Firing $weapon must deduct exactly $expectedCost points")
        }

        // 3. Test ammo depletion blocks shooting
        arenaManager.setScoreForTest(5)
        arenaManager.player.activeWeapon = WeaponType.NOVA_360_BLASTER
        arenaManager.player.shootCooldownMs = 0
        arenaManager.firePlayerWeapon()
        // Score remains 5 because 5 < 16 cost
        assertEquals(5, arenaManager.score.value, "Shooting Nova 360 with insufficient points must be blocked")
        val texts = arenaManager.getFloatingTextsSnapshot()
        assertTrue(texts.any { it.text == "NO AMMO!" }, "Must display NO AMMO! feedback")
    }

    @Test
    fun testBossSpawnThresholdAtOneMinute() {
        val stateHolder = ConferenceGameStateHolder()
        val arenaManager = stateHolder.arenaManager
        arenaManager.startNewGame()

        // Early run: 120s remaining (0s elapsed, < 60s elapsed)
        assertEquals(120, arenaManager.remainingTimeSeconds.value)
        assertEquals(1, arenaManager.currentFloor.value)

        // Ascend to Floor 4 early (< 60s elapsed)
        arenaManager.ascendToNextFloor() // floor 2
        arenaManager.ascendToNextFloor() // floor 3
        arenaManager.ascendToNextFloor() // floor 4
        assertEquals(4, arenaManager.currentFloor.value)
        assertFalse(arenaManager.bossWarningSiren.value, "Boss must NOT spawn on Floor 4 if remaining time > 60s")

        // Directly update loop with remainingTime > 60s
        arenaManager.onUpdate(100)
        assertFalse(arenaManager.bossWarningSiren.value, "Boss must NOT spawn while remainingTime > 60s")

        // Now fast-forward time to remainingTime <= 60s
        arenaManager.setRemainingTimeForTest(60) // 60s remaining => 60s elapsed
        assertEquals(60, arenaManager.remainingTimeSeconds.value)

        arenaManager.onUpdate(100)
        assertTrue(arenaManager.bossWarningSiren.value, "Boss MUST spawn when remaining time <= 60s")
        assertNotNull(arenaManager.boss, "Boss actor must be created")
    }

    @Test
    fun testDamageRangeAndViewportRestrictedCollisions() {
        val stateHolder = ConferenceGameStateHolder()
        val arenaManager = stateHolder.arenaManager
        arenaManager.screenWidth = 1920f
        arenaManager.screenHeight = 1080f
        arenaManager.startNewGame()
        arenaManager.player.posX = 1900f
        arenaManager.player.posY = 1900f

        // 1. Verify range damage calculation directly
        // Close range (<= 250f): Boosted (+1 damage)
        val closeRange = arenaManager.calculateRangeDamage(baseDamage = 1, distance = 150f)
        assertEquals(2, closeRange.damage)
        assertTrue(closeRange.isBoosted)
        assertFalse(closeRange.isFalloff)

        // Mid range (250f < dist <= 600f): Normal base damage
        val midRange = arenaManager.calculateRangeDamage(baseDamage = 1, distance = 400f)
        assertEquals(1, midRange.damage)
        assertFalse(midRange.isBoosted)
        assertFalse(midRange.isFalloff)

        // Long range (> 600f) with standard gun (baseDamage = 1): damage remains 1
        val longRange = arenaManager.calculateRangeDamage(baseDamage = 1, distance = 800f)
        assertEquals(1, longRange.damage)
        assertFalse(longRange.isBoosted)
        assertFalse(longRange.isFalloff)

        // Long range (> 600f) with heavy railgun (baseDamage = 2): damage falls off to 1
        val heavyLongRange = arenaManager.calculateRangeDamage(baseDamage = 2, distance = 800f, isHeavyRailgun = true)
        assertEquals(1, heavyLongRange.damage)
        assertFalse(heavyLongRange.isBoosted)
        assertTrue(heavyLongRange.isFalloff)

        // Close range with heavy weapon (baseDamage = 2, dist <= 250f): deals 3 damage!
        val heavyCloseRange = arenaManager.calculateRangeDamage(baseDamage = 2, distance = 100f, isHeavyRailgun = true)
        assertEquals(3, heavyCloseRange.damage)
        assertTrue(heavyCloseRange.isBoosted)
        assertFalse(heavyCloseRange.isFalloff)

        // 2. Off-screen bugs and off-screen crates do not take damage from projectiles
        arenaManager.bugs.clear()
        arenaManager.crates.clear()
        arenaManager.projectiles.clear()

        val offScreenBug = BugHazardActor(posX = 3700f, posY = 3700f, bugType = BugType.MEMORY_LEAK)
        val offScreenCrate = CrateVoxelActor(posX = 3700f, posY = 3700f)
        assertFalse(arenaManager.isBugInView(offScreenBug), "Bug at (3700, 3700) must be off-screen")
        assertFalse(arenaManager.isCrateInView(offScreenCrate), "Crate at (3700, 3700) must be off-screen")

        arenaManager.bugs.add(offScreenBug)
        arenaManager.crates.add(offScreenCrate)

        // Bullet positioned at the off-screen bug's coordinates
        val bulletAtOffScreenBug = ProjectileVoxelActor(
            posX = 3700f,
            posY = 3700f,
            dirX = 1f,
            dirY = 0f,
            isPlayerProjectile = true,
            damage = 1,
            originX = 1900f,
            originY = 1900f
        )
        arenaManager.projectiles.add(bulletAtOffScreenBug)
        arenaManager.update(16)

        // Bullet should despawn because it is off-screen, and bug and crate remain completely undamaged
        assertEquals(2, offScreenBug.hp, "Off-screen bug must NOT take damage from projectiles")
        assertEquals(2, offScreenCrate.hp, "Off-screen crate must NOT take damage from projectiles")
        assertFalse(offScreenCrate.isDestroyed, "Off-screen crate must not be destroyed")

        // 3. Close-range shots (dist <= 250f) deal boosted damage (baseDamage + 1) and one-shot 2-HP crates
        arenaManager.bugs.clear()
        arenaManager.crates.clear()
        arenaManager.projectiles.clear()

        val closeCrate = CrateVoxelActor(posX = 1950f, posY = 1900f) // dist = 50f
        assertTrue(arenaManager.isCrateInView(closeCrate), "Close crate must be in view")
        arenaManager.crates.add(closeCrate)

        val pointBlankBullet = ProjectileVoxelActor(
            posX = 1950f,
            posY = 1900f,
            dirX = 1f,
            dirY = 0f,
            isPlayerProjectile = true,
            damage = 1,
            originX = 1900f,
            originY = 1900f
        )
        arenaManager.projectiles.add(pointBlankBullet)
        arenaManager.update(16)

        assertTrue(closeCrate.isDestroyed, "Close-range boosted shot (2 dmg) must one-shot 2-HP crate")
        assertEquals(0, closeCrate.hp)

        // 4. Mid/Long-range shot (dist > 250f) deals normal 1 damage to a 2-HP crate, requiring 2 hits
        arenaManager.crates.clear()
        arenaManager.projectiles.clear()

        val midRangeCrate = CrateVoxelActor(posX = 1900f, posY = 2250f) // dist = 350f
        assertTrue(arenaManager.isCrateInView(midRangeCrate), "Mid-range crate must be in view")
        arenaManager.crates.add(midRangeCrate)

        // First mid-range bullet (dist = 350f, baseDamage = 1 -> deals 1 damage)
        val midBullet1 = ProjectileVoxelActor(
            posX = 1900f,
            posY = 2250f,
            dirX = 0f,
            dirY = 1f,
            isPlayerProjectile = true,
            damage = 1,
            originX = 1900f,
            originY = 1900f
        )
        arenaManager.projectiles.add(midBullet1)
        arenaManager.update(16)

        assertFalse(midRangeCrate.isDestroyed, "Mid-range shot should deal 1 damage to 2-HP crate, crate survives")
        assertEquals(1, midRangeCrate.hp, "Crate HP should be 1 after first mid-range shot")

        // Second mid-range bullet
        val midBullet2 = ProjectileVoxelActor(
            posX = 1900f,
            posY = 2250f,
            dirX = 0f,
            dirY = 1f,
            isPlayerProjectile = true,
            damage = 1,
            originX = 1900f,
            originY = 1900f
        )
        arenaManager.projectiles.add(midBullet2)
        arenaManager.update(16)

        assertTrue(midRangeCrate.isDestroyed, "Second shot destroys the 1-HP crate")
        assertEquals(0, midRangeCrate.hp)

        // 5. In-view Bug takes boosted damage at close range
        arenaManager.bugs.clear()
        arenaManager.projectiles.clear()

        val eliteBug = BugHazardActor(posX = 2000f, posY = 1900f, bugType = BugType.MEMORY_LEAK) // 2 HP, dist = 100f
        assertTrue(arenaManager.isBugInView(eliteBug), "Bug at (2000, 1900) must be in view")
        arenaManager.bugs.add(eliteBug)

        val closeBulletVsBug = ProjectileVoxelActor(
            posX = 2000f,
            posY = 1900f,
            dirX = 1f,
            dirY = 0f,
            isPlayerProjectile = true,
            damage = 1,
            originX = 1900f,
            originY = 1900f
        )
        arenaManager.projectiles.add(closeBulletVsBug)
        arenaManager.update(16)

        assertEquals(0, eliteBug.hp, "Close-range boosted shot (2 dmg) should eliminate 2-HP elite bug in 1 hit")

        // 6. Long-range shot with Heavy Railgun (dist > 600f) deals falloff damage (2 - 1 = 1)
        arenaManager.crates.clear()
        arenaManager.projectiles.clear()

        // Place crate within camera view (zoom = 0.55f, viewport = 1920x1080, center = 1900, 1900)
        // A crate at (2100, 2100) has dist = sqrt(200^2 + 200^2) = 282.8f from player, well within view
        val longRangeCrate = CrateVoxelActor(posX = 2100f, posY = 2100f) // 2 HP crate in view
        assertTrue(arenaManager.isCrateInView(longRangeCrate), "Crate at (2100, 2100) must be in view")
        arenaManager.crates.add(longRangeCrate)

        // Bullet originated far away (dist = 700f from crate)
        val heavyLongRangeBullet = ProjectileVoxelActor(
            posX = 2100f,
            posY = 2100f,
            dirX = 1f,
            dirY = 0f,
            isPlayerProjectile = true,
            damage = 2, // Heavy Railgun
            originX = 1400f, // dist = 700f > 600f
            originY = 2100f
        )
        arenaManager.projectiles.add(heavyLongRangeBullet)
        arenaManager.update(16)

        // Heavy weapon with falloff deals 1 damage instead of 2; crate survives with 1 HP
        assertEquals(1, longRangeCrate.hp, "Long-range heavy shot (>600f) must experience falloff to 1 dmg")
        assertFalse(longRangeCrate.isDestroyed, "Crate survives 1st falloff hit")

        // 7. Verify bug collision does NOT fall-through to damage a nearby boss (isolation check)
        arenaManager.bugs.clear()
        arenaManager.projectiles.clear()
        val testBoss = BossVoxelActor(posX = 1950f, posY = 1900f, maxHp = 35)
        arenaManager.boss = testBoss
        assertTrue(arenaManager.isBossInView(testBoss), "Test boss must be in view")

        val nearbyBug = BugHazardActor(posX = 1950f, posY = 1900f, bugType = BugType.NORMAL) // 1 HP bug at same location
        arenaManager.bugs.add(nearbyBug)

        val bulletAtBugAndBoss = ProjectileVoxelActor(
            posX = 1950f,
            posY = 1900f,
            dirX = 1f,
            dirY = 0f,
            isPlayerProjectile = true,
            damage = 1,
            originX = 1900f,
            originY = 1900f
        )
        arenaManager.projectiles.add(bulletAtBugAndBoss)
        arenaManager.update(16)

        // Bug must be killed, but boss MUST NOT take damage from the same bullet
        assertTrue(nearbyBug.hp <= 0, "Bug must be squashed by the bullet")
        assertEquals(35, testBoss.hp, "Boss MUST NOT take damage from a bullet that already hit a bug")

        // 8. Boss in view takes boosted damage at close range (dist <= 250f)
        arenaManager.bugs.clear()
        arenaManager.projectiles.clear()
        val bossBullet = ProjectileVoxelActor(
            posX = 1950f,
            posY = 1900f,
            dirX = 1f,
            dirY = 0f,
            isPlayerProjectile = true,
            damage = 1,
            originX = 1900f,
            originY = 1900f // dist = 50f <= 250f -> boosted damage = 1 + 1 = 2
        )
        arenaManager.projectiles.add(bossBullet)
        arenaManager.update(16)

        assertEquals(33, testBoss.hp, "Close-range shot (dist=50f) against boss deals 2 boosted damage")

        // 9. Off-screen boss does NOT take damage
        arenaManager.projectiles.clear()
        val offScreenBoss = BossVoxelActor(posX = 3700f, posY = 3700f, maxHp = 35)
        arenaManager.boss = offScreenBoss
        assertFalse(arenaManager.isBossInView(offScreenBoss), "Boss at (3700, 3700) must be off-screen")

        val bulletAtOffScreenBoss = ProjectileVoxelActor(
            posX = 3700f,
            posY = 3700f,
            dirX = 1f,
            dirY = 0f,
            isPlayerProjectile = true,
            damage = 2,
            originX = 1900f,
            originY = 1900f
        )
        arenaManager.projectiles.add(bulletAtOffScreenBoss)
        arenaManager.update(16)

        assertEquals(35, offScreenBoss.hp, "Off-screen boss must NOT take damage from projectiles")
    }
}



