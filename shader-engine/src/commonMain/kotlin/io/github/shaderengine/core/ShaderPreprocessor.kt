package io.github.shaderengine.core

/**
 * Normalizes AGSL shader sources for cross-platform SkSL execution and injects
 * standard library headers and math constants.
 */
object ShaderPreprocessor {
    private val LAYOUT_COLOR_REGEX = Regex("""layout\s*\(\s*color\s*\)\s*""")
    private val PI_DECLARATION_REGEX = Regex("""\b(const\s+float\s+PI|float\s+PI|#define\s+PI)\b""")
    private val PI_USAGE_REGEX = Regex("""\bPI\b""")

    /**
     * Preprocesses an AGSL shader string to run cleanly across Skiko / Skia targets.
     */
    fun preprocessForSkia(agslSource: String): String {
        var processed = agslSource.replace(LAYOUT_COLOR_REGEX, "")
        if (PI_USAGE_REGEX.containsMatchIn(processed) && !PI_DECLARATION_REGEX.containsMatchIn(processed)) {
            processed = "const float PI = 3.14159265359;\n$processed"
        }
        return processed
    }
}
