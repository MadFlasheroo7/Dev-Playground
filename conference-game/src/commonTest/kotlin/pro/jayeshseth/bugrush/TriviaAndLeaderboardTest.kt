package pro.jayeshseth.bugrush

import pro.jayeshseth.bugrush.leaderboard.LeaderboardEntry
import pro.jayeshseth.bugrush.leaderboard.LocalLeaderboardManager
import pro.jayeshseth.bugrush.trivia.KotlinQuestionBank
import pro.jayeshseth.bugrush.trivia.QuestionTier
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class TriviaAndLeaderboardTest {

    @Test
    fun testQuestionRetrievalForTiers() {
        val q1 = KotlinQuestionBank.getRandomQuestion(QuestionTier.TIER_1_EASY)
        val q2 = KotlinQuestionBank.getRandomQuestion(QuestionTier.TIER_2_MEDIUM)
        val q3 = KotlinQuestionBank.getRandomQuestion(QuestionTier.TIER_3_HARD)

        assertEquals(QuestionTier.TIER_1_EASY, q1.tier)
        assertEquals(QuestionTier.TIER_2_MEDIUM, q2.tier)
        assertEquals(QuestionTier.TIER_3_HARD, q3.tier)

        assertTrue(q1.options.isNotEmpty())
        assertTrue(q1.correctIndex in 0 until q1.options.size)
        assertNotNull(q1.powerUp)
    }

    @Test
    fun testLeaderboardSortingAndRanking() {
        val entry1 = LeaderboardEntry("PlayerA", 5000, 20, 2, 2, 2, 1000L)
        val entry2 = LeaderboardEntry("PlayerB", 12000, 45, 3, 3, 4, 2000L)

        val rank = LocalLeaderboardManager.addEntry(entry2)
        LocalLeaderboardManager.addEntry(entry1)

        val top = LocalLeaderboardManager.entries.value
        assertTrue(top.isNotEmpty())

        // Verify descending order
        for (i in 0 until top.size - 1) {
            assertTrue(top[i].score >= top[i + 1].score, "Leaderboard should be sorted descending by score")
        }
    }

    @Test
    fun testPerlinNoiseBoundsAndConsistency() {
        val noise = pro.jayeshseth.bugrush.game.map.PerlinNoise(seed = 12345L)
        val val1 = noise.fbm2D(5.5f, 8.2f)
        val val2 = noise.fbm2D(5.5f, 8.2f)
        assertEquals(val1, val2, "Same coordinates must yield deterministic noise")
        assertTrue(val1 in 0f..1f, "fbm2D must be normalized between 0.0 and 1.0")

        val noise2 = pro.jayeshseth.bugrush.game.map.PerlinNoise(seed = 99999L)
        val val3 = noise2.fbm2D(5.5f, 8.2f)
        assertTrue(val3 in 0f..1f)
    }

    @Test
    fun testIsometricProjectionRoundtrip() {
        val proj = pro.jayeshseth.bugrush.game.renderer.IsometricProjection
        val worldX = 1250f
        val worldY = 820f
        val camX = 1000f
        val camY = 1000f
        val screenX = 960f
        val screenY = 540f

        val projected = proj.project(worldX, worldY, 0f, camX, camY, screenX, screenY, zoom = 0.52f)
        val unprojected = proj.unproject(projected.x, projected.y, 0f, camX, camY, screenX, screenY, zoom = 0.52f)

        assertEquals(worldX, unprojected.first, 0.05f, "Unprojected X should match original worldX")
        assertEquals(worldY, unprojected.second, 0.05f, "Unprojected Y should match original worldY")
    }

    @Test
    fun testPlayerInvulnerabilityAndKnockback() {
        val player = pro.jayeshseth.bugrush.game.actors.PlayerVoxelActor(1000f, 1000f)
        assertTrue(!player.isInvulnerable, "Player should start without invulnerability")

        player.triggerHit(stunDurationMs = 190, iFramesMs = 1400)
        assertTrue(player.isStunned, "Player should be stunned")
        assertTrue(player.isInvulnerable, "Player should be invulnerable")

        val prevX = player.posX
        val prevY = player.posY
        player.applyKnockback(1f, 0f, force = 75f)
        assertEquals(prevX + 75f, player.posX, 0.01f, "Knockback should displace player X")
        assertEquals(prevY, player.posY, 0.01f, "Knockback should not displace player Y")
    }

    @Test
    fun testWeaponCyclingCheat() {
        val player = pro.jayeshseth.bugrush.game.actors.PlayerVoxelActor(
            1000f, 1000f, archetype = pro.jayeshseth.bugrush.game.actors.PlayerArchetype.CYBER_CODER
        )
        assertEquals(pro.jayeshseth.bugrush.game.actors.WeaponType.RAPID_BYTE_BLASTER, player.activeWeapon)

        // Cycle through all 6 weapons
        val w1 = player.cycleWeapon()
        val w2 = player.cycleWeapon()
        val w3 = player.cycleWeapon()
        val w4 = player.cycleWeapon()
        val w5 = player.cycleWeapon()
        val w6 = player.cycleWeapon()

        assertEquals(pro.jayeshseth.bugrush.game.actors.WeaponType.RAPID_BYTE_BLASTER, w6, "Should loop back to start")

        // Switch to Nova 360 Blaster and verify 16-way radial projectile burst
        player.activeWeapon = pro.jayeshseth.bugrush.game.actors.WeaponType.NOVA_360_BLASTER
        val novaBullets = player.fireWeapon()
        assertEquals(16, novaBullets.size, "Nova 360 Blaster must fire 16 projectiles")

        // Switch to Plasma Flak Cannon and verify 5-way projectile burst
        player.activeWeapon = pro.jayeshseth.bugrush.game.actors.WeaponType.PLASMA_FLAK_CANNON
        val flakBullets = player.fireWeapon()
        assertEquals(5, flakBullets.size, "Plasma Flak Cannon must fire 5 projectiles")

        // Switch to Twin Pulse and verify 2 parallel projectiles
        player.activeWeapon = pro.jayeshseth.bugrush.game.actors.WeaponType.TWIN_PULSE
        val twinBullets = player.fireWeapon()
        assertEquals(2, twinBullets.size, "Twin Pulse must fire 2 projectiles")
    }

    @Test
    fun testFrustumCulling() {
        val proj = pro.jayeshseth.bugrush.game.renderer.IsometricProjection
        assertTrue(proj.isInViewport(500f, 400f, 1280f, 720f), "Center should be in viewport")
        assertTrue(proj.isInViewport(-50f, 100f, 1280f, 720f), "Within margin should be kept")
        assertTrue(!proj.isInViewport(-300f, 400f, 1280f, 720f), "Far left should be culled")
        assertTrue(!proj.isInViewport(1800f, 400f, 1280f, 720f), "Far right should be culled")
        assertTrue(!proj.isInViewport(500f, 1200f, 1280f, 720f), "Far bottom should be culled")
    }
}
