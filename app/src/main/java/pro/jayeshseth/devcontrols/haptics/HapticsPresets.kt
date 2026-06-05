package pro.jayeshseth.devcontrols.haptics

/**
 * Curated set of vibration patterns. Each preset is either a waveform
 * (off/on/off/on alternation with optional amplitudes) or a composition of
 * primitives. Sourced from real-world UX guidelines + a few "for fun"
 * additions (morse, heartbeat, drumroll …).
 */
data class HapticsPreset(
    val id: String,
    val title: String,
    val subtitle: String,
    val play: (HapticsEngine) -> Unit,
)

object HapticsPresets {

    val all: List<HapticsPreset> = listOf(

        // ---- "UX-style" feedback patterns -----------------------------------

        HapticsPreset(
            id = "soft_tick",
            title = "Soft tick",
            subtitle = "Single light tick, like a checkbox flip",
            play = { it.oneShot(durationMs = 12, amplitude = 80) }
        ),
        HapticsPreset(
            id = "tap",
            title = "Tap",
            subtitle = "Standard touch feedback",
            play = { it.oneShot(durationMs = 20, amplitude = 160) }
        ),
        HapticsPreset(
            id = "double_tap",
            title = "Double tap",
            subtitle = "Two crisp taps",
            play = {
                it.waveform(
                    timings = longArrayOf(0, 25, 60, 25),
                    amplitudes = intArrayOf(0, 180, 0, 180),
                )
            }
        ),
        HapticsPreset(
            id = "confirm",
            title = "Confirm",
            subtitle = "Rising chirp — action accepted",
            play = {
                val supported = it.primitiveSupport()
                val rise = android.os.VibrationEffect.Composition.PRIMITIVE_QUICK_RISE
                val click = android.os.VibrationEffect.Composition.PRIMITIVE_CLICK
                if (supported[rise] == true) {
                    it.composition(listOf(
                        HapticsEngine.CompositionStep(rise, 0.7f, 0),
                        HapticsEngine.CompositionStep(click, 0.9f, 60),
                    ))
                } else {
                    it.waveform(
                        timings = longArrayOf(0, 60, 30, 30),
                        amplitudes = intArrayOf(0, 120, 0, 200),
                    )
                }
            }
        ),
        HapticsPreset(
            id = "reject",
            title = "Reject",
            subtitle = "Falling thud — action denied",
            play = {
                val supported = it.primitiveSupport()
                val fall = android.os.VibrationEffect.Composition.PRIMITIVE_QUICK_FALL
                val thud = android.os.VibrationEffect.Composition.PRIMITIVE_THUD
                val steps = mutableListOf<HapticsEngine.CompositionStep>()
                if (supported[fall] == true) {
                    steps += HapticsEngine.CompositionStep(fall, 0.8f, 0)
                }
                if (supported[thud] == true) {
                    steps += HapticsEngine.CompositionStep(thud, 0.9f, 40)
                }
                if (steps.isNotEmpty()) it.composition(steps)
                else it.waveform(longArrayOf(0, 80), intArrayOf(0, 255))
            }
        ),

        // ---- Notification / alarm bursts ------------------------------------

        HapticsPreset(
            id = "notification_short",
            title = "Notification (short)",
            subtitle = "200 ms strong + 100 ms gap + 200 ms strong",
            play = {
                it.waveform(
                    timings = longArrayOf(0, 200, 100, 200),
                    amplitudes = intArrayOf(0, 200, 0, 200),
                    usage = HapticsEngine.UsageDefaults.NOTIFICATION,
                )
            }
        ),
        HapticsPreset(
            id = "alarm_bursts",
            title = "Alarm bursts",
            subtitle = "5× full-amplitude pulse, NB. uses ALARM usage",
            play = {
                it.waveform(
                    timings = longArrayOf(0, 250, 150, 250, 150, 250, 150, 250, 150, 250),
                    amplitudes = intArrayOf(0, 255, 0, 255, 0, 255, 0, 255, 0, 255),
                    usage = HapticsEngine.UsageDefaults.ALARM,
                )
            }
        ),
        HapticsPreset(
            id = "phone_ring",
            title = "Phone ring",
            subtitle = "Imitates the classic call-incoming pattern",
            play = {
                it.waveform(
                    timings = longArrayOf(0, 1000, 500, 1000, 500),
                    amplitudes = intArrayOf(0, 200, 0, 200, 0),
                    repeat = 1,
                    usage = HapticsEngine.UsageDefaults.RINGTONE,
                )
            }
        ),

        // ---- "Just for fun" -------------------------------------------------

        HapticsPreset(
            id = "heartbeat",
            title = "Heartbeat",
            subtitle = "Lub-dub repeating",
            play = {
                it.waveform(
                    timings = longArrayOf(0, 80, 70, 130, 600),
                    amplitudes = intArrayOf(0, 220, 0, 255, 0),
                    repeat = 0,
                )
            }
        ),
        HapticsPreset(
            id = "morse_sos",
            title = "Morse: SOS",
            subtitle = "... --- ...",
            play = {
                // dot = 80 ms, dash = 240 ms, inter-symbol gap = 80, inter-letter = 240
                val timings = longArrayOf(
                    0,
                    80, 80, 80, 80, 80, 240,         // S: ...
                    240, 80, 240, 80, 240, 240,      // O: ---
                    80, 80, 80, 80, 80,              // S: ...
                )
                val amps = IntArray(timings.size) { i -> if (i % 2 == 1) 255 else 0 }
                it.waveform(timings, amps)
            }
        ),
        HapticsPreset(
            id = "drumroll",
            title = "Drumroll",
            subtitle = "Fast rising ticks → climax",
            play = {
                val timings = mutableListOf<Long>(0)
                val amps = mutableListOf(0)
                var gap = 80L
                repeat(14) {
                    timings += 25L
                    amps += (60 + it * 14).coerceAtMost(220)
                    timings += gap
                    amps += 0
                    if (gap > 18) gap = (gap * 0.85).toLong()
                }
                timings += 120L; amps += 255
                it.waveform(timings.toLongArray(), amps.toIntArray())
            }
        ),
        HapticsPreset(
            id = "knock_knock",
            title = "Knock knock",
            subtitle = "Two sharp knocks, pause, two more",
            play = {
                it.waveform(
                    timings = longArrayOf(0, 40, 90, 40, 380, 40, 90, 40),
                    amplitudes = intArrayOf(0, 255, 0, 255, 0, 255, 0, 255),
                )
            }
        ),
        HapticsPreset(
            id = "ascending",
            title = "Ascending",
            subtitle = "Rising amplitude pulses",
            play = {
                it.waveform(
                    timings = longArrayOf(0, 30, 60, 30, 60, 30, 60, 30, 60, 50),
                    amplitudes = intArrayOf(0, 60, 0, 110, 0, 160, 0, 210, 0, 255),
                )
            }
        ),
        HapticsPreset(
            id = "tap_sequence",
            title = "Telegraph",
            subtitle = "Steady evenly-spaced ticks",
            play = {
                val t = LongArray(21) { if (it == 0) 0 else 60 }
                val a = IntArray(21) { if (it % 2 == 1) 180 else 0 }
                it.waveform(t, a)
            }
        ),
        HapticsPreset(
            id = "explosion",
            title = "Explosion",
            subtitle = "Big bang + decaying rumble",
            play = {
                val timings = longArrayOf(0, 60, 25, 30, 25, 30, 35, 30, 50, 30, 80, 30)
                val amps    = intArrayOf  (0,255,  0,220,  0,180,  0,140,  0,100,  0,  60)
                it.waveform(timings, amps)
            }
        ),
        HapticsPreset(
            id = "level_up",
            title = "Level up",
            subtitle = "Quick rise → strong click",
            play = {
                val supported = it.primitiveSupport()
                val rise = android.os.VibrationEffect.Composition.PRIMITIVE_SLOW_RISE
                val click = android.os.VibrationEffect.Composition.PRIMITIVE_CLICK
                if (supported[rise] == true && supported[click] == true) {
                    it.composition(listOf(
                        HapticsEngine.CompositionStep(rise, 1.0f, 0),
                        HapticsEngine.CompositionStep(click, 1.0f, 200),
                        HapticsEngine.CompositionStep(click, 1.0f, 80),
                    ))
                } else {
                    it.waveform(
                        timings = longArrayOf(0, 200, 80, 40, 40, 40),
                        amplitudes = intArrayOf(0, 150, 0, 255, 0, 255),
                    )
                }
            }
        ),
    )
}
