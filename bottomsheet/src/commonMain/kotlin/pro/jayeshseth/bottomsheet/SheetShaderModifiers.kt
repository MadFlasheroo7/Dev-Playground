package pro.jayeshseth.bottomsheet

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.shaderengine.core.ShaderController
import io.github.shaderengine.modifiers.shaderRenderEffect

/**
 * Applies a hardware-accelerated frosted glass shader effect over the bottom sheet.
 * Powered by the project's native `:shader-engine`.
 */
fun Modifier.sheetBackdropBlur(
    controller: ShaderController,
    refraction: Float = 0.35f,
    dispersion: Float = 0.2f,
    roughness: Float = 0.25f
): Modifier = this.shaderRenderEffect(
    controller = controller,
    contentUniformName = "content"
) { _ ->
    float("refraction", refraction)
    float("dispersion", dispersion)
    float("roughness", roughness)
}

/**
 * Draws a luminous neon/specular edge glow along the top rim and handle region of the sheet.
 */
fun Modifier.sheetEdgeGlow(
    color: Color = Color(0xFF64B5F6),
    glowHeight: Dp = 24.dp,
    intensity: Float = 0.75f
): Modifier = this.drawWithCache {
    val h = glowHeight.toPx()
    val glowBrush = Brush.verticalGradient(
        0f to color.copy(alpha = (intensity * 0.7f).coerceIn(0f, 1f)),
        0.4f to color.copy(alpha = (intensity * 0.25f).coerceIn(0f, 1f)),
        1f to Color.Transparent,
        startY = 0f,
        endY = h
    )
    onDrawWithContent {
        drawContent()
        drawRect(brush = glowBrush, size = Size(size.width, h))
    }
}
