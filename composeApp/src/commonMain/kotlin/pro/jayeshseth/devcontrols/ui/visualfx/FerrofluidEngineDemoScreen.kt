package pro.jayeshseth.devcontrols.ui.visualfx

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.shaderengine.core.rememberShaderController
import io.github.shaderengine.modifiers.shaderPointerInput
import io.github.shaderengine.modifiers.shaderRenderEffect
import io.github.shaderengine.presets.ChromaticFerrofluidPreset

/**
 * Showcase screen demonstrating the cross-platform :shader-engine running the optimized
 * Chromatic Ferrofluid Optical Lens shader.
 *
 * All drag physics, pointer smoothing, instantaneous velocity calculation, and continuous
 * frame timing are handled by [ShaderController] and [Modifier.shaderRenderEffect]
 * with zero Compose recomposition.
 */
@Composable
fun FerrofluidEngineDemoScreen(onBack: () -> Unit) {
    val controller = rememberShaderController(ChromaticFerrofluidPreset.SHADER)

    var dispersion by remember { mutableFloatStateOf(0.55f) }
    var warpIntensity by remember { mutableFloatStateOf(0.15f) }
    var surfaceTension by remember { mutableFloatStateOf(0.10f) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF07090E))
            .shaderPointerInput(controller)
    ) {
        // UI layer refracted by the shader lens
        Box(
            modifier = Modifier
                .fillMaxSize()
                .shaderRenderEffect(controller) {
                    float("dispersion", dispersion)
                    float("warpIntensity", warpIntensity)
                    float("surfaceTension", surfaceTension)
                }
        ) {
            // Background reference grid
            Canvas(modifier = Modifier.fillMaxSize()) {
                val step = 36.dp.toPx()
                val gridColor = Color(0xFF1E293B)
                var x = 0f
                while (x < size.width) {
                    drawLine(gridColor, Offset(x, 0f), Offset(x, size.height), strokeWidth = 1.5f)
                    x += step
                }
                var y = 0f
                while (y < size.height) {
                    drawLine(gridColor, Offset(0f, y), Offset(size.width, y), strokeWidth = 1.5f)
                    y += step
                }
            }

            // Foreground Live UI content that will be optically refracted by the ferrofluid lens
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp, vertical = 52.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = onBack,
                        colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFF38BDF8))
                    ) {
                        Text("← Back", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }

                Text(
                    text = "KMP SHADER ENGINE\nFERROFLUID LENS",
                    color = Color.White,
                    fontSize = 32.sp,
                    lineHeight = 36.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace
                )

                Text(
                    text = "Drag or flick across the canvas to stretch anisotropic velocity metaballs, fuse satellite droplets, and refract live Compose nodes at 0 recompositions.",
                    color = Color(0xFF94A3B8),
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    EngineSpecCard(
                        title = "PIPELINE",
                        metric = "Zero Recomposition",
                        subtitle = "graphicsLayer uniform buffer",
                        accent = Brush.linearGradient(
                            listOf(Color(0xFF6366F1), Color(0xFFEC4899))
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    EngineSpecCard(
                        title = "GPU NORMALS",
                        metric = "Tier 2 Forward ∇H",
                        subtitle = "H0 sample reuse (-50% ALU)",
                        accent = Brush.linearGradient(
                            listOf(Color(0xFF06B6D4), Color(0xFF10B981))
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }

                EngineSpecCard(
                    title = "CROSS-PLATFORM ARCHITECTURE",
                    metric = "Android AGSL (API 33+) ⇄ Skia SkSL",
                    subtitle = "Automated preprocessor • FP16 clamp invariants • Early-out warps",
                    accent = Brush.horizontalGradient(
                        listOf(Color(0xFFF59E0B), Color(0xFFEF4444), Color(0xFF8B5CF6))
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // Floating bottom controls
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(16.dp)
                .background(Color(0xCC0F172A), RoundedCornerShape(20.dp))
                .border(1.dp, Color(0xFF334155), RoundedCornerShape(20.dp))
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            EngineControlSlider("Chromatic Dispersion", dispersion, 0f..1.5f) { dispersion = it }
            EngineControlSlider("fBm Warp Turbulence", warpIntensity, 0f..0.4f) { warpIntensity = it }
            EngineControlSlider("Surface Tension (smin)", surfaceTension, 0.02f..0.25f) { surfaceTension = it }
        }
    }
}

@Composable
private fun EngineSpecCard(
    title: String,
    metric: String,
    subtitle: String,
    accent: Brush,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(Color(0xFF0F172A), RoundedCornerShape(18.dp))
            .border(1.5.dp, accent, RoundedCornerShape(18.dp))
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = title,
            color = Color(0xFF64748B),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
        Text(
            text = metric,
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.ExtraBold,
            fontFamily = FontFamily.Monospace
        )
        Text(
            text = subtitle,
            color = Color(0xFFCBD5E1),
            fontSize = 12.sp
        )
    }
}

@Composable
private fun EngineControlSlider(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, color = Color(0xFFE2E8F0), fontSize = 12.sp, fontFamily = FontFamily.Monospace)
            Text(
                text = ((value * 100).toInt() / 100f).toString(),
                color = Color(0xFF38BDF8),
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = range,
            colors = SliderDefaults.colors(
                thumbColor = Color(0xFF38BDF8),
                activeTrackColor = Color(0xFF0284C7)
            ),
            modifier = Modifier.height(28.dp)
        )
    }
}
