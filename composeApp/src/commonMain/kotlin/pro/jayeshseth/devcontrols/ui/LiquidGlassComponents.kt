package pro.jayeshseth.devcontrols.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import pro.jayeshseth.devcontrols.platform.ShaderBackground

// ---------------------------------------------------------------------------
// Design tokens
// ---------------------------------------------------------------------------

/** Base frosted glass fill — thin white veil. */
val GlassFill = Color(0x14FFFFFF)

/** Subtle 1dp highlight border. */
val GlassBorder = Color(0x26FFFFFF)

/** Fallback solid background colour when the GPU shader isn't available. */
val GlassFallbackBg = Color(0xFF060913)

// ---------------------------------------------------------------------------
// LiquidGlassBackground
// ---------------------------------------------------------------------------

/**
 * Full-screen animated liquid-glass background.
 *
 * Renders an AGSL/SkSL domain-warped FBM fluid shader via the project's existing
 * [ShaderBackground] expect/actual (Android 13+ → AGSL; Desktop → Skia SkSL).
 * On older Android versions it falls back to a solid dark colour.
 *
 * Usage:
 * ```
 * LiquidGlassBackground {
 *     // Your UI here — drawn on top of the animated shader
 * }
 * ```
 */
@Composable
fun LiquidGlassBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    Box(modifier = modifier) {
        // Shader layer — fills the entire Box, drawn below the content.
        ShaderBackground(
            source = LIQUID_FLOW_SHADER,
            modifier = Modifier.fillMaxSize(),
            fallback = {
                // Gradient fallback for Android < 13
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .drawBehind {
                            drawRect(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        Color(0xFF0D1B3E),
                                        Color(0xFF060913)
                                    ),
                                    center = Offset(size.width * 0.3f, size.height * 0.2f),
                                    radius = size.width * 0.9f
                                )
                            )
                        }
                )
            }
        )
        // Content layer — everything on top of the shader.
        content()
    }
}

// ---------------------------------------------------------------------------
// LiquidGlassCard
// ---------------------------------------------------------------------------

/**
 * A frosted-glass card with an animated specular shimmer sweep.
 *
 * The card is visually composed of three layers drawn via [Modifier.drawBehind]:
 *  1. Semi-transparent white fill  (the "frost")
 *  2. Top-left radial highlight     (the "luminance bend" light source)
 *  3. Slow animated horizontal shimmer sweep (moving specular highlight)
 *
 * A 1 dp glassmorphism border is applied outside the clip so it remains fully visible.
 *
 * Works on **all** platforms without any platform-specific code — pure Compose drawing.
 */
@Composable
fun LiquidGlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(24.dp),
    borderColor: Color = GlassBorder,
    shimmerDuration: Int = 5_000,
    content: @Composable BoxScope.() -> Unit
) {
    // Animate a normalised position [−0.5 → 1.5] across the card width.
    val shimmer by rememberInfiniteTransition(label = "glass-shimmer").animateFloat(
        initialValue = -0.5f,
        targetValue  =  1.5f,
        animationSpec = infiniteRepeatable(
            animation  = tween(shimmerDuration, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer-pos"
    )

    Box(
        modifier = modifier
            // Border sits outside the clip so rounded corners don't get cropped.
            .border(BorderStroke(1.dp, borderColor), shape)
            .clip(shape)
            .drawBehind {
                val w = size.width
                val h = size.height

                // ── Layer 1: base frosted-glass fill ──────────────────────
                drawRect(GlassFill)

                // ── Layer 2: top-left radial luminance highlight ───────────
                // Mimics the "bent light" look of Apple Liquid Glass.
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0x12FFFFFF), Color.Transparent),
                        center = Offset(0f, 0f),
                        radius = maxOf(w, h) * 0.65f
                    )
                )

                // ── Layer 3: bottom-right depth shadow ────────────────────
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0x0A000000), Color.Transparent),
                        center = Offset(w, h),
                        radius = maxOf(w, h) * 0.55f
                    )
                )

                // ── Layer 4: animated specular shimmer sweep ──────────────
                val cx = w * shimmer
                val halfBand = w * 0.32f
                drawRect(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color(0x0CFFFFFF),
                            Color(0x18FFFFFF),
                            Color(0x0CFFFFFF),
                            Color.Transparent
                        ),
                        startX = cx - halfBand,
                        endX   = cx + halfBand
                    )
                )
            }
    ) {
        content()
    }
}
