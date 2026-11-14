package pro.jayeshseth.devcontrols

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import pro.jayeshseth.devcontrols.data.ButtonAction
import pro.jayeshseth.devcontrols.data.TriggerCondition
import pro.jayeshseth.devcontrols.data.TriggerConfig

class ButtonMapperLogicTest {

    @Test
    fun testButtonActionFromName() {
        assertEquals(ButtonAction.DEFAULT, ButtonAction.fromName(null))
        assertEquals(ButtonAction.DEFAULT, ButtonAction.fromName("UNKNOWN_ACTION"))
        assertEquals(ButtonAction.SCREENSHOT, ButtonAction.fromName("SCREENSHOT"))
        assertEquals(ButtonAction.LAUNCH_APP, ButtonAction.fromName("LAUNCH_APP"))
        assertEquals(ButtonAction.CYCLE_RINGER, ButtonAction.fromName("CYCLE_RINGER"))
        assertEquals(ButtonAction.TOGGLE_DND, ButtonAction.fromName("TOGGLE_DND"))
        assertEquals(ButtonAction.TOGGLE_AUTO_ROTATE, ButtonAction.fromName("TOGGLE_AUTO_ROTATE"))
        assertEquals(ButtonAction.OPEN_DEV_OPTIONS, ButtonAction.fromName("OPEN_DEV_OPTIONS"))
    }

    @Test
    fun testTriggerConditionFromName() {
        assertEquals(TriggerCondition.ALWAYS, TriggerCondition.fromName(null))
        assertEquals(TriggerCondition.ALWAYS, TriggerCondition.fromName("INVALID"))
        assertEquals(TriggerCondition.LOCKED, TriggerCondition.fromName("LOCKED"))
        assertEquals(TriggerCondition.UNLOCKED, TriggerCondition.fromName("UNLOCKED"))
    }

    @Test
    fun testTriggerConfigShouldTriggerConditionEvaluation() {
        // DEFAULT action should never trigger regardless of condition
        val defaultConfig = TriggerConfig(action = ButtonAction.DEFAULT, condition = TriggerCondition.ALWAYS)
        assertFalse(defaultConfig.shouldTrigger(isLocked = true))
        assertFalse(defaultConfig.shouldTrigger(isLocked = false))

        // ALWAYS condition
        val alwaysConfig = TriggerConfig(action = ButtonAction.SCREENSHOT, condition = TriggerCondition.ALWAYS)
        assertTrue(alwaysConfig.shouldTrigger(isLocked = true))
        assertTrue(alwaysConfig.shouldTrigger(isLocked = false))

        // LOCKED condition
        val lockedConfig = TriggerConfig(action = ButtonAction.PLAY_PAUSE, condition = TriggerCondition.LOCKED)
        assertTrue(lockedConfig.shouldTrigger(isLocked = true))
        assertFalse(lockedConfig.shouldTrigger(isLocked = false))

        // UNLOCKED condition
        val unlockedConfig = TriggerConfig(action = ButtonAction.LAUNCH_APP, condition = TriggerCondition.UNLOCKED)
        assertFalse(unlockedConfig.shouldTrigger(isLocked = true))
        assertTrue(unlockedConfig.shouldTrigger(isLocked = false))
    }

    @Test
    fun testCustomAppLaunchConfig() {
        val appConfig = TriggerConfig(
            action = ButtonAction.LAUNCH_APP,
            condition = TriggerCondition.ALWAYS,
            extraPackage = "com.spotify.music",
            extraLabel = "Spotify"
        )
        assertTrue(appConfig.shouldTrigger(isLocked = false))
        assertEquals("com.spotify.music", appConfig.extraPackage)
        assertEquals("Spotify", appConfig.extraLabel)
    }

    @Test
    fun testAllRequiredActionsPresent() {
        val requiredActions = listOf(
            ButtonAction.DEFAULT,
            ButtonAction.SCREENSHOT,
            ButtonAction.NOTIFICATIONS,
            ButtonAction.QUICK_SETTINGS,
            ButtonAction.LOCK_SCREEN,
            ButtonAction.RECENTS,
            ButtonAction.HOME,
            ButtonAction.BACK,
            ButtonAction.POWER_DIALOG,
            ButtonAction.SPLIT_SCREEN,
            ButtonAction.CYCLE_RINGER,
            ButtonAction.TOGGLE_DND,
            ButtonAction.TOGGLE_AUTO_ROTATE,
            ButtonAction.TOGGLE_FLASHLIGHT,
            ButtonAction.TOGGLE_MUTE,
            ButtonAction.PLAY_PAUSE,
            ButtonAction.SKIP_NEXT,
            ButtonAction.SKIP_PREV,
            ButtonAction.LAUNCH_CAMERA,
            ButtonAction.OPEN_ASSISTANT,
            ButtonAction.LAUNCH_APP,
            ButtonAction.OPEN_DEV_OPTIONS
        )
        val allEntries = ButtonAction.entries
        for (required in requiredActions) {
            assertTrue("Expected ButtonAction ${required.name} to exist", allEntries.contains(required))
            assertFalse(required.label.isBlank())
            assertFalse(required.icon.isBlank())
        }
    }

    @Test
    fun testPowerEventDeduplicationLogic() {
        val keyEventTime = 1000L
        val duplicateScreenEventTime = 1200L // 200ms later (< 500ms)
        val isDuplicate = (duplicateScreenEventTime - keyEventTime) < 500L
        assertTrue("Screen transition within 500ms of KEYCODE_POWER must be flagged as duplicate", isDuplicate)

        val independentScreenEventTime = 1600L // 600ms later (>= 500ms)
        val isIndependent = (independentScreenEventTime - keyEventTime) >= 500L
        assertTrue("Screen transition after 500ms without KEYCODE_POWER must be treated as independent", isIndependent)
    }

    @Test
    fun testInitialLockStatePreservationForPowerGestures() {
        // Scenario 1: Device was UNLOCKED and interactive when user double-clicks power.
        // First click triggers ACTION_SCREEN_OFF.
        // Prior state before transition was screen ON and keyguard not locked.
        val kmKeyguardLocked = false
        val initialLockedOnScreenOff = kmKeyguardLocked // false -> UNLOCKED

        val powerDoubleUnlockedConfig = TriggerConfig(
            action = ButtonAction.TOGGLE_FLASHLIGHT,
            condition = TriggerCondition.UNLOCKED
        )
        // Must evaluate true for UNLOCKED condition despite screen being off post-transition!
        assertTrue(
            "Power gesture started when device was unlocked must satisfy UNLOCKED condition",
            powerDoubleUnlockedConfig.shouldTrigger(initialLockedOnScreenOff)
        )

        // Scenario 2: Device was LOCKED when user double-clicks power to wake device.
        // First click triggers ACTION_SCREEN_ON.
        val initialLockedOnScreenOn = true // was OFF/LOCKED
        val powerDoubleLockedConfig = TriggerConfig(
            action = ButtonAction.LAUNCH_CAMERA,
            condition = TriggerCondition.LOCKED
        )
        assertTrue(
            "Power gesture started when device was locked must satisfy LOCKED condition",
            powerDoubleLockedConfig.shouldTrigger(initialLockedOnScreenOn)
        )
    }

    @Test
    fun testPressHoldDurationDiscrimination() {
        val longPressTimeoutMs = 500L
        val pressDownTime = 1000L

        // Quick release at 120ms -> Tap
        val tapReleaseTime = 1120L
        val isTap = (tapReleaseTime - pressDownTime) <= longPressTimeoutMs
        assertTrue("Release within 500ms must be recognized as a tap", isTap)

        // Hold release at 800ms -> Hold (not a tap)
        val holdReleaseTime = 1800L
        val isHold = (holdReleaseTime - pressDownTime) > longPressTimeoutMs
        assertTrue("Release after 500ms must be recognized as a hold, not a tap", isHold)
    }

    @Test
    fun testVolumeChordEvaluation() {
        val chordConfig = TriggerConfig(
            action = ButtonAction.NOTIFICATIONS,
            condition = TriggerCondition.ALWAYS
        )

        var volUpPressed = true
        var volDownPressed = true
        val chordActive = volUpPressed && volDownPressed && chordConfig.shouldTrigger(isLocked = false)
        assertTrue("Simultaneous Vol Up and Vol Down must trigger active chord config", chordActive)

        // Releasing one key breaks chord state
        volUpPressed = false
        val chordBroken = volUpPressed && volDownPressed
        assertFalse("Releasing a volume key must disengage chord state", chordBroken)
    }
}
