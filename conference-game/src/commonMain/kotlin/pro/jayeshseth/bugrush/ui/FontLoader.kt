package pro.jayeshseth.bugrush.ui

import androidx.compose.ui.text.font.FontFamily

/**
 * Multiplatform font loader interface for bundling custom retro arcade fonts.
 */
expect fun loadCustomFont(resourcePath: String, identity: String): FontFamily?
