package pro.jayeshseth.devcontrols.ui.visualfx

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.shaderengine.core.ShaderController
import io.github.shaderengine.modifiers.generativeShaderCanvas
import io.github.shaderengine.modifiers.shaderPointerInput

/**
 * Interactive In-App AGSL & SkSL Live Shader Editor.
 * Allows on-device writing, testing, and compiling of custom GLSL fragment shaders
 * with instant visual preview and compiler error diagnostics.
 */
@Composable
fun ShaderLiveEditorScreen(modifier: Modifier = Modifier) {
    val defaultTemplate = """
        uniform float2 resolution;
        uniform float time;
        uniform float2 pointer;
        uniform float p1;

        half4 main(float2 fragCoord) {
            float2 uv = (fragCoord - resolution * 0.5) / min(resolution.x, resolution.y);
            float d = length(uv);

            float r = sin(d * 18.0 - time * 4.0) * 0.5 + 0.5;
            float g = sin(d * 24.0 + time * 3.0 + 2.0) * 0.5 + 0.5;
            float b = cos(d * 12.0 - time * 5.0 + 4.0) * 0.5 + 0.5;

            float ring = smoothstep(0.4, 0.05, abs(d - p1 * 0.5));
            half3 color = half3(r, g, b) + half3(ring * 0.8);

            return half4(clamp(color, half3(0.0), half3(1.0)), 1.0);
        }
    """.trimIndent()

    var shaderCode by remember { mutableStateOf(defaultTemplate) }
    var compileStatus by remember { mutableStateOf("READY") }
    var compileError by remember { mutableStateOf<String?>(null) }
    var activeController by remember { mutableStateOf<ShaderController?>(null) }

    var p1 by remember { mutableFloatStateOf(0.5f) }
    val scope = rememberCoroutineScope()

    // Compilation routine
    fun compileShader(source: String) {
        try {
            val controller = ShaderController(source, scope)
            // Dry run to check validity
            val brush = controller.platformShader.toBrush()
            if (brush is SolidColor) {
                compileStatus = "FALLBACK / UNSUPPORTED"
                compileError = "Platform fell back to SolidColor. Verify uniform syntax."
                activeController = null
            } else {
                compileStatus = "COMPILED OK"
                compileError = null
                activeController = controller
            }
        } catch (e: Throwable) {
            compileStatus = "COMPILER ERROR"
            compileError = e.message ?: "Unknown shader syntax error."
            activeController = null
        }
    }

    // Run clock loop for active controller
    LaunchedEffect(activeController) {
        activeController?.runClockLoop()
    }

    // Initial compile
    LaunchedEffect(Unit) {
        compileShader(shaderCode)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF07090E))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Row 1: Header & Compile Button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    "LIVE AGSL / SkSL EDITOR",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    if (compileError != null) "ERRORS DETECTED" else "STATUS: $compileStatus",
                    color = if (compileError != null) Color(0xFFEF4444) else Color(0xFF10B981),
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Button(
                onClick = { compileShader(shaderCode) },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Compile & Run", fontSize = 12.sp, fontFamily = FontFamily.Monospace)
            }
        }

        // Row 2: Template Chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = false,
                onClick = {
                    shaderCode = defaultTemplate
                    compileShader(defaultTemplate)
                },
                label = { Text("Plasma Rings", fontSize = 11.sp, fontFamily = FontFamily.Monospace) },
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = Color(0xFF0F172A),
                    labelColor = Color(0xFF94A3B8)
                )
            )
            FilterChip(
                selected = false,
                onClick = {
                    val code = """
                        uniform float2 resolution;
                        uniform float time;
                        uniform float2 pointer;
                        uniform float p1;

                        half4 main(float2 fragCoord) {
                            float2 uv = fragCoord / resolution;
                            float wave = sin(uv.x * 30.0 + time * 6.0) * cos(uv.y * 30.0 - time * 4.0);
                            half3 col = half3(uv.x, uv.y, sin(time) * 0.5 + 0.5) + half3(wave * p1);
                            return half4(clamp(col, half3(0.0), half3(1.0)), 1.0);
                        }
                    """.trimIndent()
                    shaderCode = code
                    compileShader(code)
                },
                label = { Text("Interference Grid", fontSize = 11.sp, fontFamily = FontFamily.Monospace) },
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = Color(0xFF0F172A),
                    labelColor = Color(0xFF94A3B8)
                )
            )
            FilterChip(
                selected = false,
                onClick = {
                    val code = """
                        uniform float2 resolution;
                        uniform float time;
                        uniform float2 pointer;
                        uniform float p1;

                        half4 main(float2 fragCoord) {
                            float2 uv = (fragCoord - pointer) / min(resolution.x, resolution.y);
                            float d = length(uv);
                            float ripple = sin(d * 40.0 - time * 12.0) * exp(-d * 6.0) * p1;
                            half3 col = half3(0.1, 0.5, 1.0) * half(1.0 + ripple * 4.0);
                            return half4(clamp(col, half3(0.0), half3(1.0)), 1.0);
                        }
                    """.trimIndent()
                    shaderCode = code
                    compileShader(code)
                },
                label = { Text("Touch Water Ripple", fontSize = 11.sp, fontFamily = FontFamily.Monospace) },
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = Color(0xFF0F172A),
                    labelColor = Color(0xFF94A3B8)
                )
            )
        }

        // Live Preview Viewport (Top half)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .border(1.dp, Color(0xFF334155), RoundedCornerShape(12.dp)),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF030712)),
            shape = RoundedCornerShape(12.dp)
        ) {
            val controller = activeController
            if (controller != null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .shaderPointerInput(controller)
                        .generativeShaderCanvas(controller) {
                            float("p1", p1)
                        }
                )
            } else if (compileError != null) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text("COMPILATION ERROR", color = Color(0xFFEF4444), fontWeight = FontWeight.Bold, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(compileError ?: "", color = Color(0xFFFCA5A5), fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                }
            } else {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Compiling shader...", color = Color.Gray, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                }
            }
        }

        // Parameter Slider
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Uniform p1: ${((p1 * 100).toInt()) / 100f}", color = Color(0xFF94A3B8), fontSize = 11.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.width(130.dp))
            Slider(
                value = p1,
                onValueChange = { p1 = it },
                valueRange = 0f..2f,
                modifier = Modifier.weight(1f),
                colors = SliderDefaults.colors(
                    thumbColor = Color(0xFF0284C7),
                    activeTrackColor = Color(0xFF0284C7),
                    inactiveTrackColor = Color(0xFF1E293B)
                )
            )
        }

        // Live Code Editor (Bottom half)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(10.dp)),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0A0F1D)),
            shape = RoundedCornerShape(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                BasicTextField(
                    value = shaderCode,
                    onValueChange = { shaderCode = it },
                    textStyle = TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        color = Color(0xFFE2E8F0),
                        lineHeight = 18.sp
                    ),
                    cursorBrush = SolidColor(Color(0xFF38BDF8)),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
