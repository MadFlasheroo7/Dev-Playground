package pro.jayeshseth.bugrush.ui

import androidx.compose.ui.text.font.FontFamily

/**
 * Centralized typography system for Kotlin Bug Rush.
 *
 * Provides authentic retro-arcade styling across the entire game:
 * - [pixelHeader]: Authentic 8-bit chunky pixel font ("Press Start 2P") for titles,
 *   fanfare banners, score, multiplier, ammo counters ("POINTS == AMMO"), and action buttons.
 * - [terminalBody]: Retro phosphor terminal font ("VT323") for briefing cards,
 *   trivia questions & options, telemetry readouts, and leaderboard table rows.
 *
 * Falls back to [FontFamily.Monospace] if resources cannot be read in headless test environments.
 */
object ArcadeTheme {
    val pixelHeader: FontFamily = loadCustomFont("/fonts/PressStart2P-Regular.ttf", "PressStart2P") ?: FontFamily.Monospace
    val terminalBody: FontFamily = loadCustomFont("/fonts/VT323-Regular.ttf", "VT323") ?: FontFamily.Monospace
}
