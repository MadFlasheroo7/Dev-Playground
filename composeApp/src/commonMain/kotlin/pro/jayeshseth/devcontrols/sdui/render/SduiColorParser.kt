package pro.jayeshseth.devcontrols.sdui.render

import androidx.compose.ui.graphics.Color

/**
 * Robust Color parser supporting 6-digit hex (#RRGGBB), 8-digit hex (#AARRGGBB),
 * CSS color names, and fallback defaults.
 */
object SduiColorParser {

    fun parse(raw: String?, fallback: Color = Color.Unspecified): Color {
        if (raw.isNullOrBlank()) return fallback
        val clean = raw.trim().lowercase()

        // Named colors
        when (clean) {
            "transparent" -> return Color.Transparent
            "black" -> return Color.Black
            "white" -> return Color.White
            "red" -> return Color.Red
            "green" -> return Color.Green
            "blue" -> return Color.Blue
            "yellow" -> return Color.Yellow
            "cyan" -> return Color.Cyan
            "magenta" -> return Color.Magenta
            "gray", "grey" -> return Color.Gray
            "darkgray", "darkgrey" -> return Color.DarkGray
            "lightgray", "lightgrey" -> return Color.LightGray
        }

        // Hex parsing: #RGB, #RGBA, #RRGGBB, #AARRGGBB
        val hex = if (clean.startsWith("#")) clean.substring(1) else clean
        return try {
            when (hex.length) {
                3 -> {
                    // #RGB -> #RRGGBB
                    val r = hex[0].toString().repeat(2).toInt(16)
                    val g = hex[1].toString().repeat(2).toInt(16)
                    val b = hex[2].toString().repeat(2).toInt(16)
                    Color(r, g, b)
                }
                4 -> {
                    // #ARGB
                    val a = hex[0].toString().repeat(2).toInt(16)
                    val r = hex[1].toString().repeat(2).toInt(16)
                    val g = hex[2].toString().repeat(2).toInt(16)
                    val b = hex[3].toString().repeat(2).toInt(16)
                    Color(r, g, b, a)
                }
                6 -> {
                    // RRGGBB
                    val num = hex.toLong(16)
                    val r = ((num shr 16) and 0xFF).toInt()
                    val g = ((num shr 8) and 0xFF).toInt()
                    val b = (num and 0xFF).toInt()
                    Color(r, g, b)
                }
                8 -> {
                    // AARRGGBB
                    val num = hex.toLong(16)
                    val a = ((num shr 24) and 0xFF).toInt()
                    val r = ((num shr 16) and 0xFF).toInt()
                    val g = ((num shr 8) and 0xFF).toInt()
                    val b = (num and 0xFF).toInt()
                    Color(r, g, b, a)
                }
                else -> fallback
            }
        } catch (_: Exception) {
            fallback
        }
    }
}
