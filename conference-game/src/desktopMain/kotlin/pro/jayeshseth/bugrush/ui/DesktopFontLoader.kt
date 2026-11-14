package pro.jayeshseth.bugrush.ui

import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.platform.Font

actual fun loadCustomFont(resourcePath: String, identity: String): FontFamily? {
    return try {
        val stream = ArcadeTheme::class.java.getResourceAsStream(resourcePath) ?: return null
        val bytes = stream.use { it.readBytes() }
        FontFamily(Font(identity = identity, data = bytes))
    } catch (_: Throwable) {
        null
    }
}
